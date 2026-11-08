package com.neilyin.nnowork.widget

import com.neilyin.nnowork.data.plan.HourlyTrackingMode
import com.neilyin.nnowork.data.plan.ManualSalaryTimer
import com.neilyin.nnowork.data.plan.ResignationPlan
import com.neilyin.nnowork.data.plan.SalaryCalculator
import java.util.Calendar

/**
 * 牛馬／薪水計時器 Widget 的顯示狀態（對應 iOS NiuMaEntry.Phase）。
 * 純資料，不含任何 Android／Glance 型別，方便單元測試。薪資數學全部委派 [SalaryCalculator]／[ManualSalaryTimer]。
 */
sealed interface NiuMaState {
    /** 本薪資週期累積已賺。 */
    val cycleEarned: Double

    data object NotConfigured : NiuMaState {
        override val cycleEarned: Double = 0.0
    }

    /** 上班中：今日已賺 + 進度 + 下班倒數。 */
    data class Working(
        val earned: Double,
        override val cycleEarned: Double,
        val progress: Float,
        val offWorkAt: Long,
        val countdownMillis: Long,
    ) : NiuMaState

    data class OffWork(val dailyWage: Double, override val cycleEarned: Double) : NiuMaState

    data class BeforeWork(val startsAt: Long, override val cycleEarned: Double) : NiuMaState

    /** 休息日；[paidAmount] > 0 = 月薪放假照領，0 = 時薪不計薪。 */
    data class RestDay(val paidAmount: Double, override val cycleEarned: Double) : NiuMaState

    /** 國定假日（僅月薪制，放假照領）。 */
    data class Holiday(val dailyWage: Double, override val cycleEarned: Double) : NiuMaState

    enum class ManualStatus { RUNNING, PAUSED, STOPPED }

    /** 時薪制手動計時器。 */
    data class Manual(
        val status: ManualStatus,
        val elapsedMillis: Long,
        val earned: Double,
        override val cycleEarned: Double,
    ) : NiuMaState
}

object NiuMaWidgetState {
    /** 上班中／計時中時的刷新間隔。Glance 無法每秒跳動，金額每分鐘刷新；倒數與計時由 Chronometer 自走。 */
    const val TICK_MILLIS = 60_000L

    private const val MIN_DELAY_MILLIS = 1_000L

    fun select(plan: ResignationPlan, now: Long): NiuMaState {
        val snap = SalaryCalculator.snapshot(plan, now)
        if (!snap.configured) return NiuMaState.NotConfigured

        // 時薪制手動計時（碼表）：與班表無關。
        if (plan.salaryPayMode == 1 &&
            HourlyTrackingMode.normalize(plan.hourlyTrackingMode) == HourlyTrackingMode.MANUAL
        ) {
            val earned = ManualSalaryTimer.earned(plan, now)
            val status = when (plan.manualTimerStatus) {
                ManualSalaryTimer.RUNNING -> NiuMaState.ManualStatus.RUNNING
                ManualSalaryTimer.PAUSED -> NiuMaState.ManualStatus.PAUSED
                else -> NiuMaState.ManualStatus.STOPPED
            }
            return NiuMaState.Manual(
                status = status,
                elapsedMillis = ManualSalaryTimer.elapsedMillis(plan, now),
                earned = earned,
                cycleEarned = SalaryCalculator.earnedSincePayday(plan, now, earned),
            )
        }

        // 夜班：昨天開始、跨過午夜的班次（SalaryCalculator.snapshot 只看「今天」）。
        val today = SalaryCalculator.startOfDay(now)
        val yesterday = SalaryCalculator.startOfDay(today - 60 * 60 * 1000L)
        val nightInterval = SalaryCalculator.workInterval(plan, yesterday)
            ?.takeIf { it.second > today }
        if (nightInterval != null) {
            val (start, end) = nightInterval
            if (now in start until end) {
                val ratio = ((now - start).toDouble() / (end - start).toDouble()).coerceIn(0.0, 1.0)
                val earned = SalaryCalculator.dailyWage(plan, yesterday) * ratio
                return NiuMaState.Working(
                    earned = earned,
                    cycleEarned = SalaryCalculator.earnedSincePayday(plan, now, earned),
                    progress = ratio.toFloat(),
                    offWorkAt = end,
                    countdownMillis = end - now,
                )
            }
            if (now >= end &&
                (snap.phase == SalaryCalculator.Phase.BEFORE_WORK || snap.phase == SalaryCalculator.Phase.REST_DAY)
            ) {
                val wage = SalaryCalculator.dailyWage(plan, yesterday)
                return NiuMaState.OffWork(wage, SalaryCalculator.earnedSincePayday(plan, now, 0.0))
            }
        }

        return when (snap.phase) {
            SalaryCalculator.Phase.NOT_CONFIGURED -> NiuMaState.NotConfigured
            SalaryCalculator.Phase.WORKING -> NiuMaState.Working(
                earned = snap.todayEarned,
                cycleEarned = snap.cycleEarned,
                progress = snap.progress,
                offWorkAt = now + snap.countdownMillis,
                countdownMillis = snap.countdownMillis,
            )
            SalaryCalculator.Phase.AFTER_WORK ->
                NiuMaState.OffWork(SalaryCalculator.dailyWage(plan, today), snap.cycleEarned)
            SalaryCalculator.Phase.BEFORE_WORK -> NiuMaState.BeforeWork(
                startsAt = SalaryCalculator.workInterval(plan, today)?.first ?: now,
                cycleEarned = snap.cycleEarned,
            )
            SalaryCalculator.Phase.REST_DAY, SalaryCalculator.Phase.HOLIDAY ->
                if (plan.salaryPayMode == 0 && SalaryCalculator.isHoliday(today)) {
                    NiuMaState.Holiday(SalaryCalculator.dailyWage(plan, today), snap.cycleEarned)
                } else {
                    NiuMaState.RestDay(snap.todayEarned, snap.cycleEarned)
                }
        }
    }

    /**
     * 下一次需要重新渲染的時間點（epoch millis）；null = 靠一般週期 Worker 即可（未設定／暫停中）。
     * 上班中與計時中每分鐘刷新金額；其餘狀態只在狀態會改變的邊界刷新。
     */
    fun nextRefreshAt(state: NiuMaState, now: Long): Long? {
        val target = when (state) {
            is NiuMaState.Working -> minOf(now + TICK_MILLIS, state.offWorkAt + MIN_DELAY_MILLIS)
            is NiuMaState.BeforeWork -> state.startsAt
            is NiuMaState.OffWork, is NiuMaState.RestDay, is NiuMaState.Holiday -> nextMidnight(now)
            is NiuMaState.Manual ->
                if (state.status == NiuMaState.ManualStatus.RUNNING) now + TICK_MILLIS else return null
            NiuMaState.NotConfigured -> return null
        }
        return target.coerceAtLeast(now + MIN_DELAY_MILLIS)
    }

    private fun nextMidnight(now: Long): Long = Calendar.getInstance().apply {
        timeInMillis = SalaryCalculator.startOfDay(now)
        add(Calendar.DAY_OF_MONTH, 1)
    }.timeInMillis
}
