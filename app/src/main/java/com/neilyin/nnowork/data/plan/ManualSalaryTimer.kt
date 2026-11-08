package com.neilyin.nnowork.data.plan

/**
 * 時薪制手動純計時器的狀態轉移。
 *
 * 狀態保存在 [ResignationPlan]，因此離開畫面、旋轉或 App 被系統回收後仍能續算；
 * UI 只負責每秒提供目前時間，不自行持有商業狀態。
 */
object ManualSalaryTimer {
    const val IDLE = 0
    const val RUNNING = 1
    const val PAUSED = 2

    fun elapsedMillis(plan: ResignationPlan, now: Long): Long {
        val accumulated = plan.manualTimerElapsedMillis.coerceAtLeast(0L)
        if (plan.manualTimerStatus != RUNNING) return accumulated
        val startedAt = plan.manualTimerStartedAt ?: return accumulated
        val segment = if (now > startedAt) now - startedAt else 0L
        return if (segment > Long.MAX_VALUE - accumulated) Long.MAX_VALUE else accumulated + segment
    }

    fun earned(plan: ResignationPlan, now: Long): Double =
        plan.hourlyRate.coerceAtLeast(0.0) * elapsedMillis(plan, now) / MILLIS_PER_HOUR

    /** 暫停後為續跑；已停止或初始狀態則開始全新一輪。 */
    fun start(plan: ResignationPlan, now: Long): ResignationPlan {
        val resuming = plan.manualTimerStatus == PAUSED
        return plan.copy(
            manualTimerStatus = RUNNING,
            manualTimerStartedAt = now,
            manualTimerElapsedMillis = if (resuming) {
                plan.manualTimerElapsedMillis.coerceAtLeast(0L)
            } else {
                0L
            },
        )
    }

    fun pause(plan: ResignationPlan, now: Long): ResignationPlan {
        if (plan.manualTimerStatus != RUNNING) return plan
        return plan.copy(
            manualTimerStatus = PAUSED,
            manualTimerStartedAt = null,
            manualTimerElapsedMillis = elapsedMillis(plan, now),
        )
    }

    fun stop(plan: ResignationPlan, now: Long): ResignationPlan {
        if (plan.manualTimerStatus == IDLE) return plan
        return plan.copy(
            manualTimerStatus = IDLE,
            manualTimerStartedAt = null,
            manualTimerElapsedMillis = elapsedMillis(plan, now),
        )
    }

    private const val MILLIS_PER_HOUR = 3_600_000.0
}
