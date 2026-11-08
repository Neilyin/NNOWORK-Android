package com.neilyin.nnowork.data.jobarchive

import com.neilyin.nnowork.data.JsonProvider
import com.neilyin.nnowork.data.backup.BackupDateCodec
import com.squareup.moshi.JsonClass
import com.squareup.moshi.JsonReader
import okio.Buffer

/*
 * 換工作封存的 payload（android開發.md §19.4，對照 iOS `JobArchive.swift` 的 JobArchivePayload）。
 *
 * - key 一律 camelCase（Swift Codable 預設），與 RPC 參數的 snake_case 不同。
 * - 日期是 ISO-8601 字串、**沒有毫秒**（iOS `.iso8601` 不收小數秒）；以 String 保存，
 *   與 BackupPayload 相同，避免時區或精度在往返時漂移。
 * - 寬鬆解碼：除了 endDate 與 receipt，缺欄位都給預設值（與 iOS init(from:) 一致）。
 *   巢狀 struct 沒有自訂 decoder，iOS 是全部欄位必填；這裡也照做（不給預設值）。
 * - 新增欄位只能追加、不能改名。
 */

@JsonClass(generateAdapter = false)
data class JobReasonCount(
    val reason: String,
    val count: Int,
)

/** iOS JobSalarySnapshot：沒設定過薪水（月薪與時薪皆 0）時整個不帶。 */
@JsonClass(generateAdapter = false)
data class JobSalarySnapshot(
    /** 0 月薪、1 時薪 */
    val payMode: Int,
    val monthlySalary: Double,
    val hourlyRate: Double,
    val currencySymbol: String,
    val hourlyManualMode: Boolean,
    val startMinutes: Int,
    val endMinutes: Int,
) {
    val isConfigured: Boolean get() = monthlySalary > 0 || hourlyRate > 0
}

@JsonClass(generateAdapter = false)
data class JobReceiptSnapshot(
    /** `resignation` 或 `master` */
    val kind: String,
    val companyName: String,
    val firstStampDate: String? = null,
    val completionDate: String,
    val totalDays: Int,
    val totalStamps: Int,
    val savedAmount: Double,
    val issuedAt: String,
) {
    companion object {
        const val KIND_RESIGNATION = "resignation"
        const val KIND_MASTER = "master"
    }
}

@JsonClass(generateAdapter = false)
data class JobArchivedStamp(
    val date: String,
    val reason: String,
    val roundNumber: Int,
    val isEasterEgg: Boolean,
)

@JsonClass(generateAdapter = false)
data class JobArchivedTodo(
    val title: String,
    val isCompleted: Boolean,
)

@JsonClass(generateAdapter = false)
data class JobArchivePayload(
    val version: Int = CURRENT_VERSION,
    val jobIndex: Int = 1,
    val companyName: String = "",
    val startDate: String? = null,
    /** 必填：換工作那一刻。 */
    val endDate: String,
    val cardCount: Int = 0,
    val stampCount: Int = 0,
    val topReasons: List<JobReasonCount> = emptyList(),
    val savedAmount: Double = 0.0,
    val targetAmount: Double = 0.0,
    val salary: JobSalarySnapshot? = null,
    /** 必填：收據快照。 */
    val receipt: JobReceiptSnapshot,
    val stamps: List<JobArchivedStamp> = emptyList(),
    val todos: List<JobArchivedTodo> = emptyList(),
    /** 缺值時用 endDate（見 [JobArchiveCoding.decodePayload]）。 */
    val createdAt: String? = null,
) {
    /** iOS：createdAt 缺值用 endDate。 */
    val effectiveCreatedAt: String get() = createdAt ?: endDate

    companion object {
        const val CURRENT_VERSION = 1
    }
}

/** get_my_job_archives 回傳的一列：只有 id 與 payload（§19.3）。 */
data class JobArchiveRemoteRow(
    val id: String,
    val payload: JobArchivePayload,
)

object JobArchiveCoding {
    private val payloadAdapter = JsonProvider.adapter<JobArchivePayload>()

    /** 輸出時省略 null（與 iOS JSONEncoder 對 nil 的行為一致），並補上 createdAt。 */
    fun encodePayload(payload: JobArchivePayload): String =
        payloadAdapter.toJson(normalize(payload))

    /** 解不開（缺 endDate/receipt、日期格式錯）就回 null。 */
    fun decodePayload(json: String): JobArchivePayload? =
        runCatching { payloadAdapter.fromJson(json) }.getOrNull()?.let(::validOrNull)

    fun normalize(payload: JobArchivePayload): JobArchivePayload =
        payload.copy(createdAt = payload.effectiveCreatedAt)

    /** 日期都必須是 ISO-8601；否則視為解不開。 */
    fun validOrNull(payload: JobArchivePayload): JobArchivePayload? {
        val dates = listOfNotNull(
            payload.startDate,
            payload.endDate,
            payload.createdAt,
            payload.receipt.firstStampDate,
            payload.receipt.completionDate,
            payload.receipt.issuedAt,
        ) + payload.stamps.map { it.date }
        if (dates.any { runCatching { BackupDateCodec.parse(it) }.isFailure }) return null
        return normalize(payload)
    }

    /**
     * 解析 get_my_job_archives 回應陣列。單筆 payload 解不開（例如未來格式）就略過那一筆，
     * 不讓整批失敗；整體不是 JSON 陣列才拋錯。
     */
    fun decodeRemoteRows(json: String): List<JobArchiveRemoteRow> {
        val reader = JsonReader.of(Buffer().writeUtf8(json))
        val rows = mutableListOf<JobArchiveRemoteRow>()
        reader.beginArray()
        while (reader.hasNext()) {
            val row = runCatching { decodeRow(reader.peekJson()) }.getOrNull()
            reader.skipValue()
            if (row != null) rows += row
        }
        reader.endArray()
        return rows
    }

    private fun decodeRow(reader: JsonReader): JobArchiveRemoteRow? {
        var id: String? = null
        var payload: JobArchivePayload? = null
        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "id" -> id = reader.nextString()
                "payload" -> payload = payloadAdapter.fromJson(reader)?.let(::validOrNull)
                else -> reader.skipValue()
            }
        }
        reader.endObject()
        val normalizedId = id?.trim()?.lowercase()?.takeIf(String::isNotEmpty) ?: return null
        return payload?.let { JobArchiveRemoteRow(normalizedId, it) }
    }
}
