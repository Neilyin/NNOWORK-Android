# Android 歷史更新交接 — iOS 3.3.2 → 3.4.0（2026-07-19）

> **2026-09-03 狀態更新**：本文件保留為 iOS 3.4.0 歷史對齊背景。Android 目前已對齊主要真實來源 `../../NNOWORK/android開發.md`（iOS **3.5.1**、後端 migration **028**、最後更新 2026-09-01）：加入條款 v3、貼文／留言檢舉、依 `author_key` 本機封鎖、封鎖名單／解除封鎖、客服信箱及 banned／rate-limit 明確錯誤。目前 Android 版本為 **3.0（1279）**。Google Drive App Data 自動同步尚未實作，正式 Supabase／OAuth／FCM 流程、release 簽章及 Google Play 上架仍需實機或測試軌驗證。最新專案概況以 `README.md` 為準。
>
> 對照文件：`ANDROID_FEATURE_SPEC.md`（基礎功能規格）。本檔只講 **3.3.2 之後新增的東西**與 Android 對應待辦。
> 後端 Supabase 共用（project ref `tcgncqgbmlenwrghkzvv`），`db/001~008` 全部已上線執行（003 號不存在，非漏檔），Android 直接接同一套。
> ⚠️ **db/ 目錄不是後端全貌**：App 實際使用的金幣 RPC `add_coins`（寫入，iOS 呼叫點 TreeHoleService.swift）**不在任何 db/*.sql 中**——它是早期直接在 Supabase Dashboard 建的。Android 對接金幣寫入前，先到 Supabase Dashboard（Database → Functions）確認 `add_coins` 的實際簽名。另有 `get_coin_balance` 同樣未落檔，但它在 iOS 已是死代碼（金幣止血後「只寫不讀」，勿在 Android 使用）。
> 注意：iOS 3.3.3 從未上架，其內容已併入 3.4.0（build 2）一次發佈。

---

## 1. iOS 這段期間出了什麼（依主題整理）

### 1.1 資料備份與還原（原 3.3.3 批）
- **JSON 手動備份**：`DataBackup.swift` 的 `BackupPayload`，版本化格式（現為 v3），涵蓋 AppSettings／集點記錄／離職計畫／金幣流水帳／商店交易／薪水設定，外加可選欄位 `lastAuthProvider`。**匯入語意 = 全刪全換**。舊版備份檔必須永遠可解碼（decodeIfPresent 容錯）。
- **iCloud 自動同步**：SwiftData + CloudKit（私人資料）；金幣另有 iCloud KVS 備援。
- **新舊用戶分流**：首啟且本機無資料 → `WelcomeBackView`（我是老用戶找回資料／新用戶）→ 等待雲端還原或匯入 JSON。
- **Android 對應**：
  - CloudKit 無對應 → 用 **Google Drive App Data 自動備份** 或先只做 JSON 手動備份。
  - **機會點：JSON 備份格式做成跨平台相容**（同 schema），用戶 iOS ⇄ Android 搬家靠一個檔案。實作前先照 iOS `DataBackup.swift` 的 DTO 欄位逐一對齊。
  - 分流頁照抄流程即可。

### 1.2 帳號系統（Stage 3，兩波）
- **Email/密碼**：Supabase Auth REST（無 SDK，純 HTTP）。註冊可選不強制；產品定調文案「註冊是為了後續更好的體驗（金幣與記錄跨裝置保存），你也可以先不註冊」。
- **App 內刪帳號**：RPC `delete_my_auth_account`（JWT 驗證，免密碼重驗，先解除 device_hash 關聯再刪 auth.users）。**上架政策硬要求，Android 也必做**。
- **認領機制**：登入成功 → RPC `claim_device_data`（只認領 `auth_user_id IS NULL` 的 profile，不搶奪）。
- **雙身份收斂（db/007 v2）**：登入用戶跨裝置找回資料，RPC：`my_device_hashes`、`get_my_posts_v2`、`get_my_comments_v2`、`check_new_approvals_v2`、`check_new_feedback_rewards_v2`。
- **三路徑登入（3.4.0）**：Apple + Google + Email。
  - Apple：**Android 免做**（使用者指示）。注意 Google Play 沒有對應強制。
  - Google：iOS 走 Supabase OAuth web flow（`/auth/v1/authorize?provider=google` + 自訂 scheme 回跳，implicit flow 從 URL fragment 取 token）。**Android 建議改用 Credential Manager（原生 Google 登入）拿 id_token 後打 `/auth/v1/token?grant_type=id_token`，體驗更好**；不想接 SDK 才用 web flow。
  - Supabase 端 Google provider 已配置完成（Web client credentials），Android 只需在 Google Cloud 同專案加 Android client（SHA-1 指紋）。
- **登入後統一處理**（iOS `AuthService.finishSignIn`，Android 照此順序）：存 session → 記 `lastAuthProvider` → `claim_device_data` → 拉 `/auth/v1/user` 的 identities。**認領呼叫點全 App 只能有一個**。
- **設定頁綁定顯示**：讀 identities 顯示「已綁定：Google／Email（遮罩信箱）」。
- **上次登入方式提示**：iOS 存 iCloud KVS key `last_auth_provider` → Android 用 DataStore（+ 寫進 JSON 備份欄位），登入頁顯示「上次使用 XX 登入」。
- **產品模型**：一人一種登入方式、不做 linking UI；Supabase 同信箱自動合併行為保留。

### 1.3 金幣系統（Stage 4 + 止血）
- **金幣止血原則：伺服器只寫不讀**——金幣餘額以**本機流水帳**為準，`tree_hole_profiles.coin_balance` 只當寫入鏡像，**絕不能拿伺服器值覆蓋本機**（iOS 曾因此吃過重裝歸零 bug）。Android 必須遵守同一原則。
- **CoinLedgerEntry 流水帳**：append-only，餘額 = 摺疊流水帳；Android 用 Room 建同構表。
- **意見回饋領幣**：RPC `check_new_feedback_rewards_v2`（審核通過後進樹洞自動領）。

### 1.4 樹洞（取暖樹洞）
- **分享卡（3.4.0）**：貼文 → 分享卡圖（`TreeHolePostShareView`）。**只渲染：貼文內容、產業別、職稱、集點卡張數與印章數；絕不渲染暱稱與公司名稱**（條款寫死了這個承諾，Android 實作不得多露欄位）。三個入口：貼文詳情、主列表、今日心聲卡堆（按鈕列正中間）。
- **今日心聲按鈕列**：加油／QQ／**分享**／留言／按讚 五顆。
- **條款 v2（重要）**：免責聲明新增「用戶內容之分享與使用授權」節、隱私權「資料用途」同步（條文全文見 iOS `Localizable.strings` 的 `treehole_disclaimer_full_text`／`treehole_privacy_full_text`，**Android 逐字沿用**）。
  - **版本機制**：常數 `currentTermsVersion = 2`，本機 key `treeHoleTermsAcceptedVersion`。已加入但版本 < 2 的用戶進樹洞先擋「條款已更新」重同意畫面；拒絕 → 退出樹洞區、資料不刪、下次再問；新用戶 onboarding 完成直接寫 2。**Android 上線即用 v2，同樣要做重同意 gate**（Android 用戶若曾在……不適用，Android 是新裝，全部走新用戶流程即可，但機制要建好，v3 時會用到）。
- **後台**：`TreeHoleAdminView` 加了商店銷售統計（RPC `get_shop_sales_admin`）——後台功能 Android 可不做（iOS 管理端已夠用）。

### 1.5 主題與商店（3.4.0）
- **六款主題全 App 套用**：主題 = `CardTheme`，`AppThemeManager` 發全域語意色。Android 對應 Compose theme 切換。
- **金幣商店**：解鎖主題扣金幣。交易走 **outbox 模式**：本機先落（交易記錄 + 金幣流水帳 + 權益）→ 非同步送 Supabase RPC `record_shop_purchase`；`purchase_id` 由 App 端產生為主鍵、重送 `ON CONFLICT DO NOTHING` 防重複計數；`syncedAt == nil` 代表待重試。Android 照此協議實作（表 `shop_purchases` 不開放直讀寫，只能走 SECURITY DEFINER RPC）。

### 1.6 引導與說明（3.4.0 產品慣例）
- **更新說明 = 分頁導覽**：每頁一個功能（圖示＋標題＋說明＋「在哪裡用」真實路徑），最後頁 CTA。
- **帳號綁定引導卡**：更新說明關閉後串接顯示（全螢幕覆蓋，不用半高 sheet——會透出底層 UI）；只給未登入且沒看過的人一次；本機 key `authGuideShownForV340`。文案不施壓、給退路（「稍後再說」）。
- **語錄資料庫化**：quotes 搬 Supabase（表 `quotes` + `app_content_meta` 版本號，客戶端靜默增量更新）。Android 同接。

### 1.7 GA 事件（Android 對齊命名）
既有＋本次新增，Android 用同名事件同參數（Firebase Analytics）：
- 帳號：`account_signup_succeeded`、`account_signin_succeeded`、`account_deleted`
- 樹洞：`treehole_reaction_sent`（reaction: like/cheer/cry）、`treehole_comment_submitted`、`treehole_share_tapped`（**source: card_stack / post_detail / feed_list**）
- 商店/金幣：`theme_unlocked`、`theme_unlock_cancelled`、`theme_changed`、`coin_reward_claimed`
- 備份：`backup_export_succeeded`、`backup_import_succeeded`
- 其他：`stamp_added`、`stamp_round_completed`、`salary_settings_saved`、`tab_switched` 等（完整清單見 iOS `AnalyticsManager.swift`）

---

## 2. Android 落地狀態（原建議順序）

1. ✅ **裝置身份**：首啟 UUID、SHA-256、跨重啟持久化與舊版身份相容遷移已實作；全新安裝不使用 SSAID 或廣告 ID。
2. ✅ **Email + Google 登入**：Supabase OAuth web flow、統一登入收尾、`claim_device_data`、session 加密保存與 App 內刪帳號已實作；待正式環境端到端驗證。
3. ✅ **條款 v3**：同意版本、重同意 gate、兩步條款流程與不當內容零容忍條文已實作。
4. ✅ **金幣流水帳**：Room append-only ledger、每日蓋章獎勵、審核與回饋獎勵已實作；餘額維持本機為準。
5. ✅ **主題商店**：全 App 主題、互動預覽、解鎖扣幣與 `record_shop_purchase` outbox 已實作。
6. ✅ **樹洞分享卡**：欄位白名單、列表／卡堆／詳情分享入口與 Analytics 事件已實作。
7. ◐ **資料備份**：跨平台 JSON v3、舊版容錯、預檢與交易式復原已實作；Google Drive App Data 自動同步尚未實作。
8. ✅ **更新／帳號引導**：分頁更新說明、新舊用戶分流與帳號綁定全螢幕引導已實作。
9. ◐ **Google Play 發布**：UGC 檢舉、封鎖、解除封鎖、24 小時審視提示與客服入口已實作；仍需完成正式簽章、AAB／測試軌、實機端到端測試、UGC 內容分級與 Data Safety 表單驗證。

---

## 3. 紅線（與 iOS 共同遵守）

- **用戶資料不得遺失**：金幣以本機流水帳為準；伺服器金幣欄位只寫不讀。
- **device_hash 一經產生永不改變**（Android 端 UUID 產生後必須持久化，禁止每次啟動重算）。
- **分享卡欄位白名單**：內容／產業別／職稱／卡章數，**暱稱與公司名稱永不外露**——這是條款對用戶的承諾。
- **條款版本常數兩平台同步**：改條文必升版本、必觸發重同意。
- **後端不分平台**：所有 RPC 已為兩平台設計（SECURITY DEFINER + JWT/device_hash 驗證），Android 不需要也不應該新開繞過 RPC 的直讀寫路徑。
