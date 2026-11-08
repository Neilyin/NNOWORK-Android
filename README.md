# 離職集點卡 — Android 版

iOS App「離職集點卡（NNOWORK）」的 Android 版本，採 **Kotlin + Jetpack Compose**。

## 最新狀態

更新日期：**2026-09-03**

| 項目 | 目前設定 |
|------|----------|
| App 版本 | 3.0（versionCode 1279） |
| applicationId | `com.chi157.resignationpointscard` |
| 最低版本 | Android 8.0（API 26） |
| 目標版本 | Android 16（API 36） |
| compileSdk | Android 16 QPR（API 36.1） |
| Java | 17 |
| Gradle / AGP | Gradle 8.13 / Android Gradle Plugin 8.13.2 |

目前主要產品流程均已有 Android 實作：集點卡、離職計畫與薪水計時、歷史紀錄與收據、取暖樹洞、帳號、資料備份、金幣與主題商店、桌面小工具、提醒通知，以及版本更新導覽。

## 已實作功能

- **集點卡**：蓋章原因、回合完成、每日金幣、卡片主題、特殊印章與完成動畫。
- **離職計畫**：離職日期、待辦事項、提醒通知、排程／手動薪水計時器。
- **歷史紀錄**：歷次蓋章紀錄、完成狀態、離職收據與圖片分享。
- **取暖樹洞**：貼文、留言、反應、今日心聲、職場問答、我的內容、作者貼文、產業同步與分享卡。
- **樹洞安全機制**：條款 v3 重同意閘門與不當內容零容忍條文；貼文、留言可檢舉，可依 `author_key` 在本機封鎖作者，設定頁可解除封鎖或直接聯絡客服。
- **樹洞分享隱私**：分享卡只輸出內容、產業、職稱與卡章數，不輸出暱稱或公司名稱。
- **帳號**：Supabase Email／密碼與 Google OAuth、登入狀態保存、裝置資料認領、登出與 App 內刪除帳號。
- **裝置身份**：首啟產生並持久化匿名裝置識別雜湊，避免使用廣告 ID 或 SSAID。
- **資料備份**：版本化 JSON v3 匯出／匯入、iOS 備份相容、匯入驗證與中斷復原。
- **金幣與商店**：Room append-only 金幣流水帳、每日獎勵、回饋獎勵、主題解鎖及交易 outbox 重試。
- **主題**：六款全 App 語意配色、付費主題互動預覽、熊貓／樹懶專屬印章。
- **內容與通知**：Supabase 遠端語錄、薪水與語錄 Glance Widget、WorkManager 更新、待辦提醒、FCM 與 Analytics。
- **新舊用戶流程**：首次使用分流、備份找回、分頁更新公告與帳號綁定引導。

## 尚需驗證／後續工作

- 在實機完成 Supabase Auth、OAuth callback、共用 RPC 與樹洞完整端到端驗證。
- 使用正式 `google-services.json` 驗證 FCM 與 Firebase Analytics；未提供設定檔時 Firebase 外掛會停用，App 仍可建置。
- 完成 release keystore 簽章、AAB 產出與 Google Play 測試軌驗證。
- Google Drive App Data 自動同步尚未實作；目前以手動 JSON 備份及 Android 系統備份規則為主。
- Android 省電策略可能延後 Widget 與 WorkManager 的實際刷新時間，仍需多廠牌實機測試。

## 技術棧

| 用途 | 套件／方式 |
|------|------------|
| UI | Jetpack Compose + Material 3 |
| 架構 | MVVM（ViewModel + StateFlow） |
| 本地資料 | Room |
| 設定 / 偏好 | DataStore Preferences + Moshi JSON |
| 網路 / Auth | Retrofit + OkHttp + Moshi；Supabase REST / RPC / OAuth |
| 非同步 | Kotlin Coroutines |
| 桌面 Widget / 背景工作 | Glance + WorkManager |
| 圖片 | Coil；Android Canvas 分享圖 |
| 推播 / 分析 | Firebase Cloud Messaging + Firebase Analytics |
| 測試 | JUnit 4 |

## 開啟與建置

1. 安裝 Android Studio，並準備 JDK 17、Android SDK 36.1。
2. 以 Android Studio 開啟本資料夾 `NNOWORK-Android`，等待 Gradle Sync 完成。
3. 連接裝置或啟動模擬器後執行 `app`。

CLI 可使用 Android Studio 內建 JDK：

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

目前工作站已具備 Android SDK；`assembleDebug` 與 `testDebugUnitTest` 均於 **2026-09-03** 驗證通過（111 tests，0 failures，1 skipped）。

## 選用／發布設定

- `app/google-services.json`：啟用 FCM 與 Firebase Analytics；檔案不進版控。
- `keystore.properties`：提供 release 簽章資訊；檔案不進版控。沒有此檔仍可建立 debug 版本。
- OAuth 回呼：Manifest 已註冊 `tw.neil.nnowork://auth-callback`，Supabase 後台需允許相同 redirect URL。

## 專案結構

```text
app/src/main/java/com/neilyin/nnowork/
├── analytics/                  # Firebase Analytics 包裝層
├── data/
│   ├── auth/                   # Supabase Auth、OAuth callback、加密 session
│   ├── backup/                 # 跨平台 JSON v3 備份、驗證與復原
│   ├── coin/                   # 本機金幣流水帳與規則
│   ├── identity/               # 匿名裝置身份
│   ├── local/                  # Room database 與蓋章紀錄
│   ├── plan/                   # 離職計畫與薪水計算
│   ├── settings/               # App 設定與 DataStore
│   ├── shop/                   # 主題交易 outbox
│   └── treehole/               # 樹洞 API、repository、條款與策略
├── feature/
│   ├── home/                   # 集點卡
│   ├── plan/                   # 離職計畫與薪水計時器
│   ├── history/                # 歷史紀錄、自由頁與收據分享
│   ├── treehole/               # 樹洞、問答、分享與個人內容
│   ├── settings/               # 設定、帳號、備份與主題商店
│   ├── onboarding/             # 新舊用戶與首次設定
│   └── update/                 # 更新公告與帳號綁定引導
├── navigation/                 # Root flow 與底部五分頁
├── notify/                     # 本機提醒與 FCM
├── ui/theme/                   # 像素風格設計系統與全域主題
└── widget/                     # 薪水與語錄 Widget
```

## 關聯文件

- `../../NNOWORK/android開發.md`：**主要真實來源**，目前對齊 iOS 3.5.1 與後端 migration 028（最後更新 2026-09-01）。
- `../../NNOWORK/docs/ANDROID_FEATURE_SPEC.md`：Android 基礎功能與驗收規格。
- [`ANDROID_UPDATE_0719.md`](ANDROID_UPDATE_0719.md)：iOS 3.4.0 歷史交接背景與 Android 最新落地狀態。
