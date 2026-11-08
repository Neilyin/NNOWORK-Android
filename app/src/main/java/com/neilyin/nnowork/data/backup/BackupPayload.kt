package com.neilyin.nnowork.data.backup

import com.neilyin.nnowork.data.jobarchive.JobArchivePayload
import com.squareup.moshi.JsonClass

/**
 * 與 iOS `DataBackup.swift` 對齊的跨平台 JSON 備份格式。
 *
 * 日期使用 ISO-8601 UTC 字串，而非 Android 內部的 epoch millis。所有後加欄位
 * 都有預設值，確保 v1/v2 或缺少新欄位的備份仍可解碼。
 */
@JsonClass(generateAdapter = false)
data class BackupPayload(
    val schemaVersion: Int = 1,
    val exportDate: String = BackupDateCodec.EPOCH_ISO,
    val appSettings: List<BackupAppSettings> = emptyList(),
    val stampRecords: List<BackupStampRecord> = emptyList(),
    val resignationPlans: List<BackupResignationPlan> = emptyList(),
    val niuMaConfig: BackupNiuMaConfig = BackupNiuMaConfig(),
    val coinLedgerEntries: List<BackupCoinLedgerEntry> = emptyList(),
    val shopPurchaseRecords: List<BackupShopPurchaseRecord> = emptyList(),
    /** v4 新增：換工作封存的過往工作。v1–v3 缺席時為空陣列；匯入採「合併」而非覆蓋。 */
    val jobArchives: List<BackupJobArchive> = emptyList(),
    /** 只是 UX 提示，不是身份憑證；舊備份缺少時為 null。 */
    val lastAuthProvider: String? = null,
) {
    companion object {
        /** 與 iOS DataBackup.swift `currentSchemaVersion = 4` 一致；仍可讀 v1–v3。 */
        const val CURRENT_SCHEMA_VERSION = 4
    }
}

/** iOS `JobArchiveDTO`：封存內容整份以 payload 保存（§19.4 欄位）。 */
@JsonClass(generateAdapter = false)
data class BackupJobArchive(
    val id: String = "",
    val remoteID: String? = null,
    val syncedAt: String? = null,
    val payload: JobArchivePayload,
    /** 擁有者帳號（可選）；缺席時為 null，登入時歸給目前帳號（僅限伺服器確認屬於本帳號者）。 */
    val ownerUserID: String? = null,
)

@JsonClass(generateAdapter = false)
data class BackupStampRecord(
    val id: String = "",
    val date: String = BackupDateCodec.EPOCH_ISO,
    val reason: String = "",
    val isEasterEgg: Boolean = false,
    val roundNumber: Int = 1,
)

@JsonClass(generateAdapter = false)
data class BackupAppSettings(
    val companyName: String = "",
    val cardStyle: Int = 1,
    val maxStamps: Int = 10,
    val currentRound: Int = 1,
    val isOnboardingFinished: Boolean = false,
    val widgetRefreshInterval: Int = 60,
    val firstStampDate: String? = null,
    val isCardLocked: Boolean = false,
    val cardCompletedDate: String? = null,
    val roundStartDates: List<String> = emptyList(),
    val roundCompletedDates: List<String> = emptyList(),
    val coinCount: Int = 0,
    val lastCoinEarnedDate: String? = null,
    val lastTreeHoleShareDate: String? = null,
    val unlockedStyles: List<Int> = listOf(1, 2, 3),
    val lastSeenNoticeVersion: String = "",
    val commonReasons: List<String> = DEFAULT_REASONS,
    /** v2 新增；v1 缺少時必須解碼為 false。 */
    val hasMigratedToLedger: Boolean = false,
    /** v4 新增：封存編號；v1–v3 缺少時視為第 1 份工作。 */
    val currentJobIndex: Int = 1,
    /** v4 內 additive：周目；缺少時 0（＝未記錄，讀取時推估）。 */
    val currentRunNumber: Int = 0,
) {
    companion object {
        val DEFAULT_REASONS = listOf(
            "老闆廢話太多",
            "無效會議",
            "需求一直改",
            "同事雷",
            "加班沒錢",
            "心情不好",
        )
    }
}

/** iOS `TodoItem` 欄位；createdAt 是 Android 可選擇擴充，iOS 會忽略未知欄位。 */
@JsonClass(generateAdapter = false)
data class BackupTodoItem(
    val id: String = "",
    val title: String = "",
    val dueDate: String? = null,
    val notificationScheduled: Boolean = false,
    val createdAt: String? = null,
)

@JsonClass(generateAdapter = false)
data class BackupResignationPlan(
    val targetAmount: Double = 0.0,
    val currentSaved: Double = 0.0,
    val hasResume: Boolean = false,
    val todoItems: List<BackupTodoItem> = emptyList(),
    val completedTodoItems: List<BackupTodoItem> = emptyList(),
    /** iOS 舊版字串清單，保留解碼相容。 */
    val todoList: List<String> = emptyList(),
    val completedTodoList: List<String> = emptyList(),
)

@JsonClass(generateAdapter = false)
data class BackupShift(
    val id: String = "",
    val name: String = "",
    val startMinutes: Int = 9 * 60,
    val endMinutes: Int = 18 * 60,
    val colorHex: String = "#4A90D9",
    /** iOS 舊檔可能缺 icon。 */
    val icon: String = "sun.max.fill",
)

@JsonClass(generateAdapter = false)
data class BackupNiuMaConfig(
    val payMode: Int = 0,
    /** Android 時薪計時模式：0 = 時段制，1 = 手動計時；iOS 可安全忽略。 */
    val hourlyTrackingMode: Int = 0,
    val monthlySalary: Double = 0.0,
    val hourlyRate: Double = 0.0,
    val startMinutes: Int = 9 * 60,
    val endMinutes: Int = 18 * 60,
    val workDays: List<Int> = listOf(2, 3, 4, 5, 6),
    val paydayDays: List<Int> = listOf(5),
    val currencySymbol: String = "NT$",
    val lunchUnpaid: Boolean = false,
    val lunchMinutes: Int = 60,
    val monthlyScheduleMode: Int = 0,
    val customRestDays: List<String> = emptyList(),
    val shifts: List<BackupShift> = emptyList(),
    val dayShifts: Map<String, String> = emptyMap(),
    val shiftUseTemplates: Boolean = false,
    /**
     * iOS NiuMaConfigDTO 的時薪子模式（true = 手動計時）。舊 Android 匯出沒有這個 key（null），
     * 匯入時以 [hourlyTrackingMode] 為準；iOS 備份只有這個 key，匯入時以它為準。
     */
    val hourlyManualMode: Boolean? = null,
    /** Android 手動純計時器的可選擴充；iOS 會安全忽略未知欄位。 */
    val manualTimerStatus: Int = 0,
    val manualTimerElapsedMillis: Long = 0L,
)

/** v2 金幣流水帳 DTO；與 Android Room `coin_ledger_entries` 對應。 */
@JsonClass(generateAdapter = false)
data class BackupCoinLedgerEntry(
    val id: String = "",
    val amount: Int = 0,
    val type: String = "",
    val note: String = "",
    val createdAt: String = BackupDateCodec.EPOCH_ISO,
)

/** v3 商店交易 DTO；Android Room outbox 與 iOS SwiftData 共用欄位。 */
@JsonClass(generateAdapter = false)
data class BackupShopPurchaseRecord(
    val id: String = "",
    val productType: String = "",
    val productID: String = "",
    val productName: String = "",
    val quantity: Int = 1,
    val unitCoinCost: Int = 0,
    val totalCoinCost: Int = 0,
    /** iOS 與 record_shop_purchase RPC 的固定值是複數 `coins`。 */
    val currency: String = "coins",
    val status: String = "",
    val source: String = "",
    val purchasedAt: String = BackupDateCodec.EPOCH_ISO,
    val syncedAt: String? = null,
)
