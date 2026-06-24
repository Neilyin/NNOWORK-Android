# 離職集點卡 — Android 版

iOS App「離職集點卡（NNOWORK）」的 Android 移植版，採 **Kotlin + Jetpack Compose**。

## 技術棧

| 用途 | 套件 |
|------|------|
| UI | Jetpack Compose + Material 3 |
| 架構 | MVVM（ViewModel + StateFlow） |
| 本地資料（蓋章紀錄） | Room |
| 設定 / 偏好 | DataStore (Preferences) + Moshi JSON |
| 網路（取暖樹洞 Supabase） | Retrofit + OkHttp + Moshi |
| 非同步 | Kotlin Coroutines |
| 桌面 Widget | Glance + WorkManager |
| 圖片 | Coil |

最低 Android 版本：**API 26 (Android 8.0)**，目標 API 35。

## 開啟與建置

> ⚠️ 本機目前**未安裝 Android SDK / Android Studio**，無法直接 `./gradlew build`。

1. 安裝 [Android Studio](https://developer.android.com/studio)（內含 SDK 與 Gradle）。
2. 以 Android Studio 開啟本資料夾 `NNOWORK-Android`。
3. 首次開啟會自動產生 `local.properties`（指向 SDK）並 Gradle Sync。
4. 連接裝置或啟動模擬器後按 ▶ Run。

若已設定好 `ANDROID_HOME` 環境變數，亦可用 CLI：

```bash
./gradlew assembleDebug
```

Gradle wrapper（8.11.1）已內含，無需另外安裝 Gradle。

## 專案結構

```
app/src/main/java/com/neilyin/nnowork/
├── MainActivity.kt              # 進入點
├── NnoworkApp.kt                # Application（初始化 ServiceLocator）
├── di/ServiceLocator.kt         # 輕量依賴注入
├── ui/theme/                    # 像素風格設計系統
│   ├── PixelColors.kt           #   調色盤 + hex 工具
│   ├── CardTheme.kt             #   6 種集點卡主題
│   ├── PixelStyle.kt            #   PixelButton / Badge / 字體 / Modifier
│   └── Theme.kt                 #   Material3 主題 + 等寬字體
├── data/
│   ├── local/                   # Room（StampRecord）
│   ├── settings/                # AppSettings + SettingsRepository（DataStore）
│   └── datastore/               # DataStore 定義
├── navigation/                  # AppRoot（引導閘門）+ MainScaffold（底部 5 分頁）
├── feature/
│   ├── home/                    # ✅ 集點卡（蓋章、回合、完成、彩蛋、金幣）
│   ├── onboarding/              # ◐ 引導（最小可用版）
│   ├── plan/                    # ⬜ 離職計畫 + 薪水計時器
│   ├── history/                 # ⬜ 離職紀錄 + 收據
│   ├── treehole/                # ⬜ 取暖樹洞
│   └── settings/                # ⬜ 設定
└── widget/SalaryWidget.kt       # ◐ 薪水計時器 Widget（基礎樁）
```

## 進度

- ✅ 專案骨架、Gradle 設定、像素設計系統
- ✅ 資料層（Room 蓋章 + DataStore 設定）
- ✅ 集點卡主畫面（蓋章、卡片格子、回合、每日金幣、完成對話框、大師彩蛋、動畫）
- ◐ 引導（最小可用：公司名 + 格數 + 風格）
- ⬜ 離職計畫 / 薪水計時器（NiuMaTimer）
- ⬜ 離職紀錄 / 收據（一般 + 大師級）
- ⬜ 取暖樹洞（Supabase RPC，API 規格見 iOS repo 的 `android開發.md`）
- ⬜ 設定（卡片風格解鎖、版本通知）
- ⬜ 完整 Widget / 推播 / 名言佳句

## 對應 iOS 來源

iOS 原始碼位於同層的 `NNOWORK/` repo；取暖樹洞與金幣系統的後端 API 規格詳見 `NNOWORK/android開發.md`。
