package com.neilyin.nnowork.data.jobarchive

import com.neilyin.nnowork.data.JsonProvider
import com.neilyin.nnowork.data.backup.BackupDateCodec
import com.neilyin.nnowork.data.local.StampRecordEntity
import com.neilyin.nnowork.data.plan.ResignationPlan
import com.neilyin.nnowork.data.settings.AppSettings
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Types
import java.util.Locale
import java.util.UUID

sealed class JobTransitionException(message: String, cause: Throwable? = null) : IllegalStateException(message, cause) {
    /** 封存只屬於已登入者（§19.1）。 */
    class NotSignedIn : JobTransitionException("Job archive requires a signed-in account")
    /** 後端尚未部署（404 + PGRST202 或 job_archives 不存在）；本機資料都還在。 */
    class ServerNotReady(cause: Throwable? = null) : JobTransitionException("Job archive backend is not ready", cause)
    class Network(cause: Throwable? = null) : JobTransitionException("Job archive request failed", cause)
    class Server(message: String, cause: Throwable? = null) : JobTransitionException(message, cause)
    /** 上傳已成功但本機提交失敗；資料保留在 pending，重試沿用同一 id。 */
    class LocalSaveFailed(cause: Throwable? = null) : JobTransitionException("Local job transition commit failed", cause)
}

interface JobArchiveRemote {
    /** 上傳一筆封存（archive_my_job），回傳伺服器 id。同一 id 重送必須冪等。 */
    suspend fun upload(id: String, payload: JobArchivePayload): String
    /** 拉回目前登入者的全部封存（get_my_job_archives）。 */
    suspend fun fetchAll(): List<JobArchiveRemoteRow>
}

/** 目前工作的快照（讀取）。 */
data class CurrentJobSnapshot(
    val settings: AppSettings,
    val stamps: List<StampRecordEntity>,
    val plan: ResignationPlan,
)

/** 本機儲存的抽象：Room（stamps、archives）＋ DataStore（settings、plan）。 */
interface JobTransitionLocalStore {
    suspend fun readCurrentJob(): CurrentJobSnapshot
    suspend fun archivesForOwner(owner: String): List<JobArchiveEntity>

    /**
     * 在同一個 Room transaction 內：若同擁有者同 id 尚無封存就插入、刪除全部蓋章，
     * 最後以單次 DataStore edit 對「當下」的 settings 與 plan 套用轉換（不覆蓋並行更新的其他欄位）。
     * 任一步失敗整個 rollback。
     */
    suspend fun commitTransition(
        archive: JobArchiveEntity?,
        transformSettings: (AppSettings) -> AppSettings,
        transformPlan: (ResignationPlan) -> ResignationPlan,
    )

    suspend fun insertArchivesIfAbsent(archives: List<JobArchiveEntity>): Int
    suspend fun claimUnowned(ids: Set<String>, owner: String): Int
}

/** 上傳前就寫入、本機提交成功才清除（§19.6）。payloadJson 讓快照在任何刪除前就落地。 */
@JsonClass(generateAdapter = false)
data class JobArchivePendingEntry(
    val id: String,
    /** 本機當時的 currentJobIndex（＝ key：「正在被封存的那份目前工作」）。 */
    val jobIndex: Int,
    val ownerUserId: String,
    val companyName: String = "",
    val stampCount: Int = 0,
    val createdAt: String = BackupDateCodec.EPOCH_ISO,
    val uploaded: Boolean = false,
    /** 本次封存實際使用的編號（payload.jobIndex），重試沿用。 */
    val archiveJobIndex: Int? = null,
    /** 上傳時的完整 payload；Android 額外保存，確保本機清除前快照已持久化。 */
    val payloadJson: String? = null,
)

interface JobArchivePendingStore {
    suspend fun load(): Map<String, JobArchivePendingEntry>
    suspend fun save(entries: Map<String, JobArchivePendingEntry>)
}

object JobArchivePendingKeys {
    fun key(owner: String, jobIndex: Int): String = "${owner.lowercase(Locale.ROOT)}|$jobIndex"

    private val mapAdapter = JsonProvider.moshi.adapter<Map<String, JobArchivePendingEntry>>(
        Types.newParameterizedType(Map::class.java, String::class.java, JobArchivePendingEntry::class.java),
    )

    fun encode(entries: Map<String, JobArchivePendingEntry>): String = mapAdapter.toJson(entries)
    fun decode(json: String?): Map<String, JobArchivePendingEntry> =
        json?.let { runCatching { mapAdapter.fromJson(it) }.getOrNull() }.orEmpty()
}

data class JobTransitionResult(
    val archive: JobArchiveEntity,
    /** 已清除、排過提醒的待辦 id；呼叫端負責取消提醒。 */
    val clearedReminderTodoIds: List<String>,
)

/** 合併拉回資料時要做的事（純計算，方便測試）。 */
data class JobArchiveMergePlan(
    val toInsert: List<JobArchiveRemoteRow>,
    /** 本機已有的那筆剛好是待完成紀錄 → 代表其實已完成，清掉待完成。 */
    val pendingKeysToRemove: Set<String>,
    /** 自己上傳過、本機還沒完成清除的那次換工作：維持待完成、不顯示，標記 uploaded。 */
    val pendingKeysToMarkUploaded: Set<String>,
)

object JobArchiveMergePolicy {
    fun plan(
        rows: List<JobArchiveRemoteRow>,
        existingOwned: List<JobArchiveEntity>,
        owner: String,
        pending: Map<String, JobArchivePendingEntry>,
    ): JobArchiveMergePlan {
        val normalizedOwner = owner.lowercase(Locale.ROOT)
        val known = (existingOwned.map { it.dedupeKey } + existingOwned.map { it.id.lowercase(Locale.ROOT) }).toMutableSet()
        val pendingById = pending.filterValues { it.ownerUserId.lowercase(Locale.ROOT) == normalizedOwner }
            .entries.associateBy({ it.value.id.lowercase(Locale.ROOT) }, { it.key })
        val toInsert = mutableListOf<JobArchiveRemoteRow>()
        val remove = mutableSetOf<String>()
        val markUploaded = mutableSetOf<String>()
        for (row in rows) {
            val key = row.id.lowercase(Locale.ROOT)
            val pendingKey = pendingById[key]
            if (key in known) {
                if (pendingKey != null) remove += pendingKey
                continue
            }
            if (pendingKey != null) {
                markUploaded += pendingKey
                continue
            }
            toInsert += row
            known += key
        }
        return JobArchiveMergePlan(toInsert, remove, markUploaded)
    }
}

/**
 * 換工作流程（§19.6 資料安全順序，紅線：資料不得遺失）：
 * 1. 只在記憶體產生 payload，先把「待完成封存」（含完整 payload）寫進本機持久儲存
 * 2. 上傳 archive_my_job —— 失敗就停，本機資料完全不動
 * 3. 上傳成功 → 單一 Room transaction：插入本機封存 ＋ 清除目前工作，失敗整個 rollback
 * 4. 本機提交成功才刪掉「待完成封存」
 */
class JobTransitionCoordinator(
    private val local: JobTransitionLocalStore,
    private val remote: JobArchiveRemote,
    private val pendingStore: JobArchivePendingStore,
    private val nowMillis: () -> Long = System::currentTimeMillis,
    private val newUuid: () -> String = { UUID.randomUUID().toString() },
    private val canonicalReason: (String) -> String? = JobTransitionPolicy::defaultCanonicalReason,
) {
    suspend fun archiveAndStartNewJob(
        ownerId: String?,
        newCompanyName: String,
        maxStamps: Int = JobTransitionPolicy.DEFAULT_MAX_STAMPS,
    ): JobTransitionResult {
        val owner = ownerId?.trim()?.lowercase(Locale.ROOT)?.takeIf(String::isNotEmpty)
            ?: throw JobTransitionException.NotSignedIn()
        val now = nowMillis()
        val current = local.readCurrentJob()
        val localJobIndex = current.settings.currentJobIndex
        val pendingKey = JobArchivePendingKeys.key(owner, localJobIndex)
        val ownedArchives = local.archivesForOwner(owner)

        val existingPending = pendingStore.load()[pendingKey]
        val archiveNumber = existingPending?.archiveJobIndex
            ?: JobTransitionPolicy.nextArchiveNumber(localJobIndex, ownedArchives.map { it.jobIndex })
        val payload = JobTransitionPolicy.makeSnapshot(
            current.settings, current.stamps, current.plan, now, canonicalReason = canonicalReason,
        ).copy(jobIndex = archiveNumber)
        val archiveId = (existingPending?.id ?: newUuid()).lowercase(Locale.ROOT)
        var pending = (existingPending ?: JobArchivePendingEntry(
            id = archiveId,
            jobIndex = localJobIndex,
            ownerUserId = owner,
            createdAt = BackupDateCodec.format(now),
        )).copy(
            archiveJobIndex = archiveNumber,
            companyName = payload.companyName,
            stampCount = payload.stampCount,
            payloadJson = JobArchiveCoding.encodePayload(payload),
        )
        upsertPending(pendingKey, pending)

        // 2. 上傳。失敗直接拋出：到這裡為止本機資料沒有任何變更（只多了 pending）。
        val remoteId = remote.upload(archiveId, payload).trim().lowercase(Locale.ROOT).ifEmpty { archiveId }
        pending = pending.copy(uploaded = true)
        upsertPending(pendingKey, pending)

        // 3. 重讀目前資料（上傳期間可能又蓋了章），以最新資料組本機封存，確保被刪的章都在封存裡。
        val fresh = local.readCurrentJob()
        val previousRunNumber = JobTransitionPolicy.runNumber(fresh.settings, ownedArchives.map { it.dedupeKey })
        val finalPayload = if (fresh.stamps.size == payload.stampCount) {
            payload
        } else {
            JobTransitionPolicy.makeSnapshot(
                fresh.settings, fresh.stamps, fresh.plan, now, canonicalReason = canonicalReason,
            ).copy(jobIndex = archiveNumber)
        }
        val alreadyLocal = ownedArchives.firstOrNull { it.dedupeKey == remoteId || it.id.lowercase(Locale.ROOT) == archiveId }
        val archive = alreadyLocal ?: finalPayload.toEntity(
            id = archiveId,
            owner = owner,
            remoteId = remoteId,
            syncedAt = nowMillis(),
        )
        try {
            local.commitTransition(
                archive = if (alreadyLocal == null) archive else null,
                transformSettings = { settings ->
                    JobTransitionPolicy.resetSettingsForNewJob(
                        settings = settings,
                        newCompanyName = newCompanyName,
                        maxStamps = maxStamps,
                        newJobIndex = archiveNumber + 1,
                        newRunNumber = previousRunNumber + 1,
                    )
                },
                transformPlan = JobTransitionPolicy::resetPlanForNewJob,
            )
        } catch (error: Exception) {
            if (error is kotlinx.coroutines.CancellationException) throw error
            throw JobTransitionException.LocalSaveFailed(error)
        }

        // 4. 本機提交成功才清除待完成紀錄。
        removePending(pendingKey)
        return JobTransitionResult(archive, JobTransitionPolicy.scheduledTodoIds(fresh.plan))
    }

    /**
     * 未綁帳號且使用者**明確選擇**「不保存，直接開始」：清除目前工作、不建立封存。
     * 呼叫端（UI）必須先取得使用者確認；這是唯一會不經封存就刪除蓋章的路徑。
     */
    suspend fun startNewJobWithoutArchive(
        ownerId: String?,
        newCompanyName: String,
        maxStamps: Int = JobTransitionPolicy.DEFAULT_MAX_STAMPS,
    ): List<String> {
        val owner = ownerId?.trim()?.lowercase(Locale.ROOT)?.takeIf(String::isNotEmpty)
        val current = local.readCurrentJob()
        val owned = owner?.let { local.archivesForOwner(it) }
        val newJobIndex = JobTransitionPolicy.nextArchiveNumber(
            current.settings.currentJobIndex,
            owned?.map { it.jobIndex },
        ) + 1
        val newRunNumber = JobTransitionPolicy.runNumber(current.settings, owned?.map { it.dedupeKey }) + 1
        local.commitTransition(
            archive = null,
            transformSettings = { settings ->
                JobTransitionPolicy.resetSettingsForNewJob(settings, newCompanyName, maxStamps, newJobIndex, newRunNumber)
            },
            transformPlan = JobTransitionPolicy::resetPlanForNewJob,
        )
        if (owner != null) removePending(JobArchivePendingKeys.key(owner, current.settings.currentJobIndex))
        return JobTransitionPolicy.scheduledTodoIds(current.plan)
    }

    /** 登入後／回前景拉回過往工作補到本機（以 帳號 + id 去重）。不修改 currentJobIndex。 */
    suspend fun pullFromServer(ownerId: String?, currentOwner: () -> String?): Int {
        val owner = ownerId?.trim()?.lowercase(Locale.ROOT)?.takeIf(String::isNotEmpty) ?: return 0
        val rows = remote.fetchAll()
        if (rows.isEmpty()) return 0
        // await 期間可能已登出或換帳號：資料只歸給拉取開始時的帳號。
        if (currentOwner()?.lowercase(Locale.ROOT) != owner) return 0
        local.claimUnowned(rows.map { it.id }.toSet(), owner)
        val pending = pendingStore.load()
        val plan = JobArchiveMergePolicy.plan(rows, local.archivesForOwner(owner), owner, pending)
        if (plan.pendingKeysToRemove.isNotEmpty() || plan.pendingKeysToMarkUploaded.isNotEmpty()) {
            val updated = pending.toMutableMap()
            plan.pendingKeysToRemove.forEach(updated::remove)
            plan.pendingKeysToMarkUploaded.forEach { key -> updated[key]?.let { updated[key] = it.copy(uploaded = true) } }
            pendingStore.save(updated)
        }
        val now = nowMillis()
        return local.insertArchivesIfAbsent(
            plan.toInsert.map { it.payload.toEntity(id = it.id, owner = owner, remoteId = it.id, syncedAt = now) },
        )
    }

    private suspend fun upsertPending(key: String, entry: JobArchivePendingEntry) {
        pendingStore.save(pendingStore.load() + (key to entry))
    }

    private suspend fun removePending(key: String) {
        pendingStore.save(pendingStore.load() - key)
    }
}

fun JobArchivePayload.toEntity(id: String, owner: String, remoteId: String?, syncedAt: Long?): JobArchiveEntity {
    val normalized = JobArchiveCoding.normalize(this)
    return JobArchiveEntity(
        id = id.lowercase(Locale.ROOT),
        ownerUserId = owner.lowercase(Locale.ROOT),
        remoteId = remoteId?.lowercase(Locale.ROOT),
        syncedAt = syncedAt,
        jobIndex = normalized.jobIndex,
        companyName = normalized.companyName,
        endDate = BackupDateCodec.parse(normalized.endDate),
        createdAt = BackupDateCodec.parse(normalized.effectiveCreatedAt),
        payloadJson = JobArchiveCoding.encodePayload(normalized),
    )
}
