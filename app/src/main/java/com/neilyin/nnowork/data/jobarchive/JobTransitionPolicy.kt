package com.neilyin.nnowork.data.jobarchive

import com.neilyin.nnowork.data.backup.BackupDateCodec
import com.neilyin.nnowork.data.local.StampRecordEntity
import com.neilyin.nnowork.data.plan.HourlyTrackingMode
import com.neilyin.nnowork.data.plan.ResignationPlan
import com.neilyin.nnowork.data.settings.AppSettings
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/** 換工作的純函式（對照 iOS `JobTransitionService` 的 makeSnapshot / nextArchiveNumber / runNumber / applyNewJobReset）。 */
object JobTransitionPolicy {
    const val DEFAULT_MAX_STAMPS = 10
    const val TOP_REASON_LIMIT = 3
    private val ALLOWED_MAX_STAMPS = setOf(10, 20, 30)

    /** iOS BuiltInStampReason.canonicalValues；自訂原因原樣保留。 */
    val BUILT_IN_REASONS = listOf("老闆廢話太多", "無效會議", "需求一直改", "同事雷", "加班沒錢", "心情不好")

    fun defaultCanonicalReason(stored: String): String? = stored.takeIf { it in BUILT_IN_REASONS }

    /** 依目前資料產生封存快照，不修改任何東西。 */
    fun makeSnapshot(
        settings: AppSettings,
        stamps: List<StampRecordEntity>,
        plan: ResignationPlan?,
        nowMillis: Long,
        zone: ZoneId = ZoneId.systemDefault(),
        canonicalReason: (String) -> String? = ::defaultCanonicalReason,
    ): JobArchivePayload {
        val sorted = stamps.sortedBy { it.date }
        val archivedStamps = sorted.map {
            JobArchivedStamp(
                date = BackupDateCodec.format(it.date),
                reason = it.reason,
                roundNumber = it.roundNumber,
                isEasterEgg = it.isEasterEgg,
            )
        }
        val startMillis = sorted.firstOrNull()?.date
            ?: settings.firstStampDate
            ?: settings.roundStartDates.minOrNull()
        val cardCount = sorted.map { it.roundNumber }.toSet().size

        val counts = LinkedHashMap<String, Int>()
        sorted.forEach { stamp ->
            val key = canonicalReason(stamp.reason) ?: stamp.reason
            counts[key] = (counts[key] ?: 0) + 1
        }
        // LinkedHashMap 保留首次出現順序；sortedByDescending 是穩定排序，同次數維持首次出現先後。
        val topReasons = counts.entries
            .sortedByDescending { it.value }
            .take(TOP_REASON_LIMIT)
            .map { JobReasonCount(reason = it.key, count = it.value) }

        val saved = plan?.currentSaved ?: 0.0
        val target = plan?.targetAmount ?: 0.0
        val salary = plan?.let {
            JobSalarySnapshot(
                payMode = it.salaryPayMode,
                monthlySalary = it.monthlySalary,
                hourlyRate = it.hourlyRate,
                currencySymbol = it.currencySymbol,
                hourlyManualMode = HourlyTrackingMode.normalize(it.hourlyTrackingMode) == HourlyTrackingMode.MANUAL,
                startMinutes = it.workStartMinutes,
                endMinutes = it.workEndMinutes,
            )
        }?.takeIf(JobSalarySnapshot::isConfigured)

        val isMaster = settings.currentRound >= 3 && settings.isCardLocked
        val completionMillis = if (settings.isCardLocked) settings.cardCompletedDate ?: nowMillis else nowMillis
        val receiptStart = sorted.firstOrNull()?.date
        val receipt = JobReceiptSnapshot(
            kind = if (isMaster) JobReceiptSnapshot.KIND_MASTER else JobReceiptSnapshot.KIND_RESIGNATION,
            companyName = settings.companyName,
            firstStampDate = receiptStart?.let(BackupDateCodec::format),
            completionDate = BackupDateCodec.format(completionMillis),
            totalDays = dayCount(receiptStart ?: completionMillis, completionMillis, zone),
            totalStamps = sorted.size,
            savedAmount = saved,
            issuedAt = BackupDateCodec.format(nowMillis),
        )

        val todos = plan?.todoItems.orEmpty().map { JobArchivedTodo(it.title, isCompleted = false) } +
            plan?.completedTodoItems.orEmpty().map { JobArchivedTodo(it.title, isCompleted = true) }

        return JobArchivePayload(
            jobIndex = settings.currentJobIndex,
            companyName = settings.companyName,
            startDate = startMillis?.let(BackupDateCodec::format),
            endDate = BackupDateCodec.format(nowMillis),
            cardCount = cardCount,
            stampCount = sorted.size,
            topReasons = topReasons,
            savedAmount = saved,
            targetAmount = target,
            salary = salary,
            receipt = receipt,
            stamps = archivedStamps,
            todos = todos,
            createdAt = BackupDateCodec.format(nowMillis),
        )
    }

    /** 與離職收據相同的天數算法（以日曆日計，不為負）。 */
    fun dayCount(startMillis: Long, endMillis: Long, zone: ZoneId = ZoneId.systemDefault()): Int {
        val start = Instant.ofEpochMilli(startMillis).atZone(zone).toLocalDate()
        val end = Instant.ofEpochMilli(endMillis).atZone(zone).toLocalDate()
        return ChronoUnit.DAYS.between(start, end).coerceAtLeast(0).toInt()
    }

    /** 封存編號 = max(currentJobIndex, 本帳號已知封存最大 jobIndex + 1)；未登入時就是目前值。 */
    fun nextArchiveNumber(currentJobIndex: Int, ownerArchiveJobIndexes: List<Int>?): Int {
        if (ownerArchiveJobIndexes == null) return currentJobIndex
        val knownMax = ownerArchiveJobIndexes.maxOrNull() ?: 0
        return maxOf(currentJobIndex, knownMax + 1)
    }

    /**
     * 周目（送文 job_index 用）。已記錄（≥ 1）直接用；舊資料（0）推估：
     * 第一份工作 → 1；否則 = 本帳號過往工作數 + 1（上限 currentJobIndex）。
     */
    fun runNumber(settings: AppSettings, ownerArchiveDedupeKeys: List<String>?): Int {
        if (settings.currentRunNumber >= 1) return settings.currentRunNumber
        if (settings.currentJobIndex <= 1 || ownerArchiveDedupeKeys == null) return 1
        val archiveCount = ownerArchiveDedupeKeys.toSet().size
        return maxOf(1, minOf(settings.currentJobIndex, archiveCount + 1))
    }

    /** 清除「目前工作」的設定欄位；不碰主題、金幣、每日金幣日期、常用原因、onboarding 等。 */
    fun resetSettingsForNewJob(
        settings: AppSettings,
        newCompanyName: String,
        maxStamps: Int,
        newJobIndex: Int?,
        newRunNumber: Int?,
    ): AppSettings = settings.copy(
        companyName = newCompanyName.trim(),
        maxStamps = if (maxStamps in ALLOWED_MAX_STAMPS) maxStamps else DEFAULT_MAX_STAMPS,
        currentRound = 1,
        isCardLocked = false,
        firstStampDate = null,
        cardCompletedDate = null,
        roundStartDates = emptyList(),
        roundCompletedDates = emptyList(),
        currentJobIndex = maxOf(newJobIndex ?: 0, settings.currentJobIndex + 1),
        currentRunNumber = newRunNumber?.let { maxOf(1, it) } ?: settings.currentRunNumber,
    )

    /** 存款進度與待辦屬於上一份工作；目標金額與薪水設定保留（與 iOS 一致）。 */
    fun resetPlanForNewJob(plan: ResignationPlan): ResignationPlan = plan.copy(
        currentSaved = 0.0,
        hasResume = false,
        todoItems = emptyList(),
        completedTodoItems = emptyList(),
    )

    /** 清除前排過提醒的待辦 id，呼叫端負責取消對應提醒。 */
    fun scheduledTodoIds(plan: ResignationPlan?): List<String> =
        (plan?.todoItems.orEmpty() + plan?.completedTodoItems.orEmpty())
            .filter { it.notificationScheduled }
            .map { it.id }
}
