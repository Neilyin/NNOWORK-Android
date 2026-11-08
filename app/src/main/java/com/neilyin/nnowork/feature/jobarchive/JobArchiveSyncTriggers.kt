package com.neilyin.nnowork.feature.jobarchive

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * 過往工作的雲端補拉時機（§19.3，對照 iOS NNOWORKApp scenePhase .active 與 RootView onChange(isSignedIn)）：
 * - App 回到前景且已登入。App 剛啟動那一次由啟動流程（NnoworkApp）拉過，這裡略過，避免連打兩次。
 * - 登入成功當下（未登入 → 已登入）。第一次觀察到的登入狀態只記錄，不拉（同樣已由啟動流程處理）。
 * 呼叫端在主執行緒呼叫；[pull] 自己負責切執行緒、失敗靜默。
 */
class JobArchivePullTrigger(
    private val scope: CoroutineScope,
    private val pull: suspend () -> Unit,
) {
    private var seenFirstForeground = false
    private var lastSignedIn: Boolean? = null

    fun onForeground(isSignedIn: Boolean) {
        if (!seenFirstForeground) {
            seenFirstForeground = true
            return
        }
        if (isSignedIn) launchPull()
    }

    fun onSignedInChanged(isSignedIn: Boolean) {
        val previous = lastSignedIn
        lastSignedIn = isSignedIn
        if (previous == false && isSignedIn) launchPull()
    }

    private fun launchPull() {
        scope.launch { pull() }
    }
}
