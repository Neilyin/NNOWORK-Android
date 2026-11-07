package com.neilyin.nnowork.data.plan

import com.squareup.moshi.JsonClass
import java.util.UUID

@JsonClass(generateAdapter = false)
data class TodoItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    /** 選填到期／提醒時間（epoch millis）；null = 沒有設定時間。 */
    val reminderAt: Long? = null,
    /**
     * iOS 允許保留 dueDate 但取消通知排程；獨立保存才能跨平台保真。
     * 舊 Android 資料沒有此欄位時，以原本 reminderAt 行為為預設。
     */
    val notificationScheduled: Boolean = reminderAt != null,
)

/** 班表模板（對應 iOS Shift）。icon 為邏輯鍵，UI 端對應到 Material icon。 */
@JsonClass(generateAdapter = false)
data class Shift(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val startMinutes: Int = 9 * 60,
    val endMinutes: Int = 18 * 60,
    val colorHex: String = "#4A90D9",
    val icon: String = "sun", // sunrise(早) / sun(午) / moon(晚) / star(大夜)
)

/** 時薪制的計時方式；用 Int 保持 DataStore／JSON 向後相容。 */
object HourlyTrackingMode {
    const val SCHEDULE = 0
    const val MANUAL = 1

    fun normalize(value: Int): Int = if (value == MANUAL) MANUAL else SCHEDULE
}

@JsonClass(generateAdapter = false)
data class ResignationPlan(
    val targetAmount: Double = 0.0,
    val currentSaved: Double = 0.0,
    val hasResume: Boolean = false,
    val todoItems: List<TodoItem> = emptyList(),
    val completedTodoItems: List<TodoItem> = emptyList(),

    // ── 薪水計時器 §4 ──
    val salaryPayMode: Int = 0, // 0 = 月薪制, 1 = 時薪制
    /** 時薪制下：0 = 時段制，1 = 手動計時。舊版資料預設維持原本時段制。 */
    val hourlyTrackingMode: Int = HourlyTrackingMode.SCHEDULE,
    val monthlySalary: Double = 0.0,
    val hourlyRate: Double = 0.0,
    val currencySymbol: String = "NT$",

    /** 手動純計時器：0 = 未開始／已停止、1 = 計時中、2 = 已暫停。 */
    val manualTimerStatus: Int = ManualSalaryTimer.IDLE,
    /** 當前計時片段起點；只有 [ManualSalaryTimer.RUNNING] 時應有值。 */
    val manualTimerStartedAt: Long? = null,
    /** 已完成片段的累積毫秒；停止後保留，讓使用者能查看本次結果。 */
    val manualTimerElapsedMillis: Long = 0L,

    /** 全域上下班（0 點起算分鐘）。end <= start 視為夜班跨日。 */
    val workStartMinutes: Int = 9 * 60,
    val workEndMinutes: Int = 18 * 60,

    val lunchUnpaid: Boolean = false,
    val lunchMinutes: Int = 60,

    /** 發薪日，可多選 1–30。 */
    val paydayDays: Set<Int> = setOf(5),

    /** 0 = 見紅休, 1 = 排班制。 */
    val monthlyScheduleMode: Int = 0,

    /** 休假日（yyyy-MM-dd）。見紅休：預選週末+國定假日；排班固定工時：自訂。 */
    val customRestDays: Set<String> = emptySet(),

    /** 排班制：false = 固定工時、true = 多種班別。 */
    val shiftUseTemplates: Boolean = false,

    /** 班表模板。 */
    val shifts: List<Shift> = emptyList(),

    /** 每天指派的班別（yyyy-MM-dd -> shiftId）。 */
    val dayShifts: Map<String, String> = emptyMap(),
)
