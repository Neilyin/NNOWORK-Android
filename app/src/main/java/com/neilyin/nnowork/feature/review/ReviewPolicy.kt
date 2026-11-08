package com.neilyin.nnowork.feature.review

/**
 * 評分請求的節流規則（純函式，對應 iOS ReviewManager.request）：
 * - 每個里程碑一生只請求一次；
 * - 每次 App 啟動（process）最多請求一次；
 * - 沒有前景 Activity 時略過且不標記，讓下次再試。
 * 是否真的顯示由 Google Play 決定（有配額），無法強制。
 */
object ReviewPolicy {
    const val DEFAULT_DELAY_MS = 600L

    /** 剛請求過評分的視為「系統視窗可能正要出現或開著」的時間窗（iOS 10 秒）。 */
    const val RECENT_WINDOW_MS = 10_000L

    fun shouldRequest(
        momentAlreadyUsed: Boolean,
        requestedThisSession: Boolean,
        hasForegroundActivity: Boolean,
    ): Boolean = !momentAlreadyUsed && !requestedThisSession && hasForegroundActivity

    fun requestedRecently(lastRequestAtMs: Long?, nowMs: Long): Boolean =
        lastRequestAtMs != null && kotlin.math.abs(nowMs - lastRequestAtMs) < RECENT_WINDOW_MS
}
