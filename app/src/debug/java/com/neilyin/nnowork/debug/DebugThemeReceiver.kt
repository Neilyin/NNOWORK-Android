package com.neilyin.nnowork.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.neilyin.nnowork.di.ServiceLocator
import com.neilyin.nnowork.feature.update.UPDATE_NOTICE_VERSION
import com.neilyin.nnowork.ui.theme.CardTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Debug-only：讓截圖腳本直接切換主題，不需要走金幣購買流程。
 *
 * adb shell am broadcast -a com.neilyin.nnowork.DEBUG_THEME \
 *   -n com.chi157.resignationpointscard/com.neilyin.nnowork.debug.DebugThemeReceiver --ei theme 10
 *
 * 加 `--ez skipIntro true` 會順便標記引導／更新說明已看過，方便直接進主畫面。
 * 加 `--ei lock 10` 會把該付費主題從已解鎖清單移除（截「未購買」狀態用）。
 * 只存在 src/debug，release 建置不包含。
 */
class DebugThemeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val themeId = intent.getIntExtra("theme", -1)
        val skipIntro = intent.getBooleanExtra("skipIntro", false)
        val lockId = intent.getIntExtra("lock", -1)
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                ServiceLocator.settingsRepository.update { current ->
                    var next = current
                    if (themeId in CardTheme.entries.map { it.id }) {
                        next = next.copy(
                            cardStyle = themeId,
                            unlockedStyles = (next.unlockedStyles + themeId).distinct(),
                        )
                    }
                    if (lockId > 0) {
                        next = next.copy(unlockedStyles = next.unlockedStyles - lockId)
                    }
                    if (skipIntro) {
                        next = next.copy(
                            isOnboardingFinished = true,
                            lastSeenNoticeVersion = UPDATE_NOTICE_VERSION,
                            authGuideShownForV340 = true,
                        )
                    }
                    next
                }
            } finally {
                pending.finish()
            }
        }
    }
}
