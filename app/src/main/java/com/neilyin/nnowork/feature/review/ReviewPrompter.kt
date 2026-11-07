package com.neilyin.nnowork.feature.review

import android.app.Activity
import com.google.android.play.core.ktx.launchReview
import com.google.android.play.core.ktx.requestReview
import com.google.android.play.core.review.ReviewManagerFactory
import com.neilyin.nnowork.data.settings.PromptPrefsRepository
import com.neilyin.nnowork.data.settings.ReviewMoment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Google Play In-App Review 入口（對應 iOS ReviewManager.request）。
 *
 * 在「正向時刻」呼叫 [onPositiveMoment]。節流見 [ReviewPolicy]；失敗一律靜默，不影響使用。
 * 手動「給評分」走 Play 商店頁（設定頁，見 PlayStore.openListing）。
 */
object ReviewPrompter {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var requestedThisSession = false
    private var lastRequestAtMs: Long? = null

    /** 剛請求過評分：其他啟動提示可據此讓路。 */
    val requestedRecently: Boolean
        get() = ReviewPolicy.requestedRecently(lastRequestAtMs, System.currentTimeMillis())

    /**
     * @param activity 目前前景的 Activity。
     * @param delayMs 稍延遲，避開 sheet 關閉／蓋章動畫（iOS：首次蓋章 600ms，其餘 900ms）。
     */
    fun onPositiveMoment(
        activity: Activity,
        moment: ReviewMoment,
        delayMs: Long = ReviewPolicy.DEFAULT_DELAY_MS,
    ) {
        if (requestedThisSession) return
        scope.launch {
            val used = withContext(Dispatchers.IO) { PromptPrefsRepository.instance.isReviewMomentUsed(moment) }
            val alive = !activity.isFinishing && !activity.isDestroyed
            // 檢查與設旗標之間沒有 suspend，主執行緒上不會有兩個請求同時通過。
            if (!ReviewPolicy.shouldRequest(used, requestedThisSession, alive)) return@launch
            requestedThisSession = true
            lastRequestAtMs = System.currentTimeMillis() + delayMs
            withContext(Dispatchers.IO) { PromptPrefsRepository.instance.markReviewMomentUsed(moment) }
            delay(delayMs)
            if (activity.isFinishing || activity.isDestroyed) return@launch
            runCatching {
                val manager = ReviewManagerFactory.create(activity)
                val info = manager.requestReview()
                manager.launchReview(activity, info)
            }
        }
    }
}
