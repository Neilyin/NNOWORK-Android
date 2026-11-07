package com.neilyin.nnowork

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.neilyin.nnowork.analytics.Analytics
import com.neilyin.nnowork.di.ServiceLocator
import com.neilyin.nnowork.notify.Notifications
import com.neilyin.nnowork.widget.WidgetScheduler
import com.neilyin.nnowork.data.RemoteQuotesStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class NnoworkApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ServiceLocator.init(this)
        Notifications.ensureChannels(this)
        Analytics.init(this)

        // 匯入可能在 Room 與 DataStore 跨儲存提交期間被系統終止。啟動時必須
        // 先以 durable journal 復原到完整舊版或新版，才允許 coin migration/UI 讀取。
        runBlocking(Dispatchers.IO) {
            ServiceLocator.backupRepository.recoverPendingImportIfNeeded()
            ServiceLocator.backupRepository.migrateDeferredShopPurchases()
        }

        // 依設定排程桌面 Widget 定時刷新。
        CoroutineScope(Dispatchers.IO).launch {
            ServiceLocator.authRepository.initialize()
            // 任何 UI 顯示/使用金幣前，先完成舊 coinCount 的一次性 ledger 遷移。
            ServiceLocator.coinLedgerRepository.initialize()
            ServiceLocator.coinLedgerRepository.grantUpdateCompensation(
                campaignId = "android-2.0",
                amount = 15,
            )
            ServiceLocator.shopPurchaseRepository.syncPending()
            RemoteQuotesStore.refreshIfNeeded(this@NnoworkApp)
            val settings = ServiceLocator.settingsRepository.settings.first()
            WidgetScheduler.schedule(this@NnoworkApp, settings.widgetRefreshInterval)
        }

        // 有設定 Firebase 時，取得 FCM token 並綁定 device_hash。
        if (FirebaseApp.getApps(this).isNotEmpty()) {
            runCatching {
                FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
                    CoroutineScope(Dispatchers.IO).launch {
                        runCatching { ServiceLocator.treeHoleRepository.registerPushToken(token) }
                    }
                }
            }
        }
    }
}
