package com.neilyin.nnowork.feature.treehole

import com.neilyin.nnowork.data.reward.RewardSyncSummary
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 金幣獎勵（§18.3 delivery + ACK）的觸發時機，對照 iOS `TreeHoleView.collectRewards()`：
 * - 進樹洞時（已建立身份且已同意最新條款）
 * - 每次成功重新載入「我的貼文」之後（iOS `rewardRefreshRevision`）
 * 同時只跑一個；失敗靜默，事件留在 journal／伺服器等下次。有新入帳才回呼 [onCredited]。
 */
class TreeHoleRewardSync(
    private val scope: CoroutineScope,
    private val canSync: () -> Boolean,
    private val synchronize: suspend () -> RewardSyncSummary,
    private val onCredited: (RewardSyncSummary) -> Unit,
) {
    private val running = AtomicBoolean(false)

    fun onScreenEntered() = trigger()

    fun onMyPostsReloaded() = trigger()

    private fun trigger() {
        if (!canSync()) return
        if (!running.compareAndSet(false, true)) return
        scope.launch {
            try {
                val summary = try {
                    synchronize()
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    null
                }
                if (summary != null && summary.hasCredits) onCredited(summary)
            } finally {
                running.set(false)
            }
        }
    }
}

val RewardSyncSummary.hasCredits: Boolean
    get() = approvedPostCoins > 0 || feedbackRewardCoins > 0
