package com.neilyin.nnowork.data.plan

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * 薪水計時器核心邏輯（對應 iOS §4.2）。全部為純函式，供 UI 與桌面 Widget 共用。
 *
 * 日期一律以「當天 00:00 的 epoch millis」或 `yyyy-MM-dd` key 表示。
 */
object SalaryCalculator {

    /** 月曆可設定範圍上限；超出此日預設休六日。 */
    const val CALENDAR_END_KEY = "2026-12-31"

    /** 2026 國定假日（寫死，對應 iOS）。 */
    val HOLIDAYS_2026: Set<String> = setOf(
        "2026-09-25", // 中秋
        "2026-09-28", // 教師節
        "2026-10-09", // 雙十補
        "2026-10-26", // 光復補
        "2026-12-25", // 行憲
    )

    enum class Phase { NOT_CONFIGURED, REST_DAY, HOLIDAY, BEFORE_WORK, WORKING, AFTER_WORK }

    data class Snapshot(
        val configured: Boolean,
        val todayEarned: Double,
        val cycleEarned: Double,
        val progress: Float,
        val phase: Phase,
        val statusText: String,
        /** 距離下班的毫秒（上班中才有意義，否則 0）。 */
        val countdownMillis: Long,
    )

    private val keyFormat get() = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    fun dateKey(dayMillis: Long): String = keyFormat.format(java.util.Date(dayMillis))

    fun startOfDay(timeMillis: Long): Long = Calendar.getInstance().apply {
        timeInMillis = timeMillis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    fun isWeekend(dayMillis: Long): Boolean {
        val dow = Calendar.getInstance().apply { timeInMillis = dayMillis }.get(Calendar.DAY_OF_WEEK)
        return dow == Calendar.SATURDAY || dow == Calendar.SUNDAY
    }

    fun isHoliday(dayMillis: Long): Boolean = dateKey(dayMillis) in HOLIDAYS_2026

    private fun daysInMonth(dayMillis: Long): Int =
        Calendar.getInstance().apply { timeInMillis = dayMillis }.getActualMaximum(Calendar.DAY_OF_MONTH)

    val ResignationPlan.isConfigured: Boolean
        get() {
            val hasPay = if (salaryPayMode == 0) monthlySalary > 0 else hourlyRate > 0
            val needsScheduledHours = salaryPayMode == 0 ||
                HourlyTrackingMode.normalize(hourlyTrackingMode) == HourlyTrackingMode.SCHEDULE
            // 排班＋多班別：各班別自有時間，不需要全域上下班（iOS canSave 的 timeOK）。
            return hasPay && (!needsScheduledHours || isShiftTemplateMode || workStartMinutes != workEndMinutes)
        }

    /** 是否為排班「多班別」模式。 */
    private val ResignationPlan.isShiftTemplateMode: Boolean
        get() = monthlyScheduleMode == 1 && shiftUseTemplates

    /**
     * isRestDay：
     *  - 排班 + 多班別 → 沒指派班表 = 休
     *  - 排班固定工時 / 見紅休 → 在 customRestDays 內 = 休（超出月曆範圍：週末 = 休）
     */
    fun isRestDay(plan: ResignationPlan, dayMillis: Long): Boolean {
        val key = dateKey(dayMillis)
        return if (plan.isShiftTemplateMode) {
            plan.dayShifts[key] == null
        } else {
            if (key <= CALENDAR_END_KEY) key in plan.customRestDays
            else isWeekend(dayMillis)
        }
    }

    /** 上班區間（start, end 的 epoch millis）；休息日回傳 null。夜班 end 落到隔天。 */
    fun workInterval(plan: ResignationPlan, dayMillis: Long): Pair<Long, Long>? {
        if (isRestDay(plan, dayMillis)) return null
        val dayStart = startOfDay(dayMillis)
        val key = dateKey(dayMillis)
        val shift = if (plan.isShiftTemplateMode) plan.shifts.firstOrNull { it.id == plan.dayShifts[key] } else null
        val startM = shift?.startMinutes ?: plan.workStartMinutes
        val endM = shift?.endMinutes ?: plan.workEndMinutes
        val start = dayStart + startM * 60_000L
        val end = if (endM <= startM) dayStart + (1_440 + endM) * 60_000L else dayStart + endM * 60_000L
        return start to end
    }

    /** 當天計薪分鐘（班表時距或全域 − 午休）。 */
    private fun paidMinutes(plan: ResignationPlan, dayMillis: Long): Int {
        val interval = workInterval(plan, dayMillis) ?: return 0
        val span = ((interval.second - interval.first) / 60_000L).toInt()
        return (span - if (plan.lunchUnpaid) plan.lunchMinutes else 0).coerceAtLeast(0)
    }

    /**
     * dailyWage：
     *  - 月薪 = monthlySalary / 當月總天數（放假也有錢）
     *  - 時薪 = hourlyRate × 計薪分鐘 / 60
     */
    fun dailyWage(plan: ResignationPlan, dayMillis: Long): Double =
        if (plan.salaryPayMode == 0) {
            plan.monthlySalary / daysInMonth(dayMillis).coerceAtLeast(1)
        } else {
            plan.hourlyRate * paidMinutes(plan, dayMillis) / 60.0
        }

    /** 今天(含)以前最近的一個發薪日，作為週期起點（回傳 start-of-day millis）。 */
    fun lastPayday(plan: ResignationPlan, now: Long): Long {
        val today = startOfDay(now)
        if (plan.paydayDays.isEmpty()) return today
        // 從本月與上月的發薪日中，挑出 <= today 的最大者。
        val candidates = mutableListOf<Long>()
        for (monthOffset in 0 downTo -1) {
            val cal = Calendar.getInstance().apply {
                timeInMillis = today
                add(Calendar.MONTH, monthOffset)
            }
            val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
            plan.paydayDays.forEach { d ->
                val cc = (cal.clone() as Calendar).apply {
                    set(Calendar.DAY_OF_MONTH, d.coerceAtMost(maxDay))
                    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                }
                if (cc.timeInMillis <= today) candidates.add(cc.timeInMillis)
            }
        }
        return candidates.maxOrNull() ?: today
    }

    /**
     * earnedSincePayday：月薪累加每一天日薪；時薪只累加實際上班日；再加今天已賺部分。
     */
    fun earnedSincePayday(plan: ResignationPlan, now: Long, todayEarned: Double): Double {
        val today = startOfDay(now)
        var cursor = lastPayday(plan, now)
        var total = 0.0
        while (cursor < today) {
            if (plan.salaryPayMode == 0) {
                total += dailyWage(plan, cursor)
            } else if (!isRestDay(plan, cursor)) {
                total += dailyWage(plan, cursor)
            }
            cursor = Calendar.getInstance().apply {
                timeInMillis = cursor
                add(Calendar.DAY_OF_MONTH, 1)
            }.timeInMillis
        }
        return total + todayEarned
    }

    fun snapshot(plan: ResignationPlan, now: Long): Snapshot {
        if (!plan.isConfigured) {
            return Snapshot(false, 0.0, 0.0, 0f, Phase.NOT_CONFIGURED, "尚未設定薪資", 0)
        }
        val today = startOfDay(now)
        val rest = isRestDay(plan, today)
        val holiday = isHoliday(today)
        val daily = dailyWage(plan, today)
        val interval = workInterval(plan, today)

        val phase: Phase
        val todayEarned: Double
        val progress: Float
        val statusText: String
        var countdown = 0L

        when {
            rest && plan.salaryPayMode == 0 -> {
                phase = Phase.REST_DAY
                todayEarned = daily
                progress = 1f
                statusText = if (holiday) "國定假日，月薪照領" else "今天休息，月薪照樣進帳"
            }
            rest -> {
                phase = Phase.REST_DAY
                todayEarned = 0.0
                progress = 0f
                statusText = "今天休息，時薪制不計薪"
            }
            interval != null && now < interval.first -> {
                phase = Phase.BEFORE_WORK
                todayEarned = 0.0
                progress = 0f
                statusText = "還沒上班，${timeShort.format(java.util.Date(interval.first))} 開始"
            }
            interval != null && now in interval.first until interval.second -> {
                phase = Phase.WORKING
                val ratio = ((now - interval.first).toDouble() / (interval.second - interval.first).toDouble()).coerceIn(0.0, 1.0)
                todayEarned = daily * ratio
                progress = ratio.toFloat()
                countdown = interval.second - now
                statusText = "上班中，${timeShort.format(java.util.Date(interval.second))} 下班"
            }
            else -> {
                phase = Phase.AFTER_WORK
                todayEarned = daily
                progress = 1f
                statusText = "今天已下班"
            }
        }

        return Snapshot(
            configured = true,
            todayEarned = todayEarned,
            cycleEarned = earnedSincePayday(plan, now, todayEarned),
            progress = progress,
            phase = phase,
            statusText = statusText,
            countdownMillis = countdown,
        )
    }

    /** 見紅休模式：產生本月起至月曆上限的「週末 + 國定假日」預選休假日。 */
    fun defaultRedRestDays(now: Long): Set<String> {
        val out = mutableSetOf<String>()
        var cursor = Calendar.getInstance().apply {
            timeInMillis = startOfDay(now)
            set(Calendar.DAY_OF_MONTH, 1)
        }.timeInMillis
        while (dateKey(cursor) <= CALENDAR_END_KEY) {
            if (isWeekend(cursor) || isHoliday(cursor)) out.add(dateKey(cursor))
            cursor = Calendar.getInstance().apply {
                timeInMillis = cursor
                add(Calendar.DAY_OF_MONTH, 1)
            }.timeInMillis
        }
        return out
    }

    /** 排班固定工時：預設休週末。 */
    fun defaultWeekendRestDays(now: Long): Set<String> {
        val out = mutableSetOf<String>()
        var cursor = Calendar.getInstance().apply {
            timeInMillis = startOfDay(now)
            set(Calendar.DAY_OF_MONTH, 1)
        }.timeInMillis
        while (dateKey(cursor) <= CALENDAR_END_KEY) {
            if (isWeekend(cursor)) out.add(dateKey(cursor))
            cursor = Calendar.getInstance().apply {
                timeInMillis = cursor
                add(Calendar.DAY_OF_MONTH, 1)
            }.timeInMillis
        }
        return out
    }

    private val timeShort = SimpleDateFormat("HH:mm", Locale.TAIWAN)

    fun formatMoney(plan: ResignationPlan, value: Double): String =
        plan.currencySymbol + java.text.NumberFormat.getNumberInstance(Locale.TAIWAN).format(value.toLong())

    fun formatCountdown(millis: Long): String {
        if (millis <= 0) return "00:00:00"
        val totalSec = millis / 1000
        val h = totalSec / 3600
        val m = (totalSec % 3600) / 60
        val s = totalSec % 60
        return "%02d:%02d:%02d".format(h, m, s)
    }
}
