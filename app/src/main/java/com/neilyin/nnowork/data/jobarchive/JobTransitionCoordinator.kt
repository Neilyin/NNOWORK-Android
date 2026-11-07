package com.neilyin.nnowork.data.jobarchive

import com.neilyin.nnowork.data.JsonProvider
import com.neilyin.nnowork.data.backup.BackupDateCodec
import com.neilyin.nnowork.data.local.StampRecordEntity
import com.neilyin.nnowork.data.plan.ResignationPlan
import com.neilyin.nnowork.data.settings.AppSettings
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Types
import kotlinx.coroutines.CancellationException
import java.util.Locale
import java.util.UUID

sealed class JobTransitionException(message: String, cause: Throwable? = null) : IllegalStateException(message, cause) {
    /** 封存只屬於已登入者（§19.1）。 */
    class NotSignedIn : JobTransitionException("Job archive requires a signed-in account")
    /** 後端尚未部署（404 + PGRST202 或 job_archives 不存在）；本機資料都還在。 */
    class ServerNotReady(cause: Throwable? = null) : JobTransitionException("Job archive backend is not ready", cause)
    class Network(cause: Throwable? = null) : JobTransitionException("Job archive request failed", cause)
    class Server(message: String, cause: Throwable? = null) : JobTransitionException(message, cause)
    /** 上傳已成功但本機提交失敗；資料保留在 pending，下次啟動或下次嘗試會自動接續。 */
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

/**
 * 本機儲存的抽象。Room（stamps、archives）與 DataStore（settings、plan、pending）是兩個儲存，
 * 沒辦法一個 transaction 提交，所以換工作拆成兩個各自原子的階段，並靠 pending 做前滾復原：
 *
 * - Room 階段 [commitRoomPhase]：一個 Room transaction 內插入封存＋刪除「已被封存的那些章」。
 *   「本機已有這筆封存」就是 Room 階段已提交的持久標記。
 * - DataStore 階段 [resetCurrentJobAndClearPending]：一次 DataStore edit 內重設 settings、plan
 *   並刪除 pending。三者在同一個檔案，同一次 edit 是原子的。
 */
interface JobTransitionLocalStore {
    suspend fun readCurrentJob(): CurrentJobSnapshot
    suspend fun archivesForOwner(owner: String): List<JobArchiveEntity>

    /** 單一 Room transaction：archive 非 null 且同擁有者同 id 尚無封存時插入；刪除 [stampIds] 指定的章。 */
    suspend fun commitRoomPhase(archive: JobArchiveEntity?, stampIds: Set<String>)

    /**
     * 單一 DataStore edit：對「當下」的 settings 與 plan 套用 [transform]，並刪除 [pendingKeys]。
     */
    suspend fun resetCurrentJobAndClearPending(
        pendingKeys: Collection<String>,
        transform: (AppSettings, ResignationPlan) -> Pair<AppSettings, ResignationPlan>,
    )

    suspend fun insertArchivesIfAbsent(archives: List<JobArchiveEntity>): Int
    suspend fun claimUnowned(ids: Set<String>, owner: String): Int
}

/** 上傳前就寫入、DataStore 階段完成時才（原子地）清除（§19.6）。 */
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
    /** 完整 payload；Android 額外保存，確保任何清除前快照已持久化，可從它前滾。 */
    val payloadJson: String? = null,
    /** 前滾重設用的參數（使用者當初輸入的新公司、格數，與算好的新周目）。 */
    val newCompanyName: String? = null,
    val maxStamps: Int? = null,
    val newRunNumber: Int? = null,
    /** 新工作的 currentJobIndex；null 時用 archiveJobIndex + 1。 */
    val newJobIndex: Int? = null,
    /** [KIND_ARCHIVE]（換工作並封存）或 [KIND_DISCARD]（使用者明確選「不保存，直接開始」）。 */
    val kind: String = KIND_ARCHIVE,
    /** 被收進這筆封存（或被放棄）的章；Room 階段只刪這些，之後才蓋的章不會被刪。 */
    val archivedStampIds: List<String>? = null,
) {
    companion object {
        const val KIND_ARCHIVE = "archive"
        const val KIND_DISCARD = "discard"
    }
}

interface JobArchivePendingStore {
    suspend fun load(): Map<String, JobArchivePendingEntry>
    suspend fun save(entries: Map<String, JobArchivePendingEntry>)
}

object JobArchivePendingKeys {
    fun key(owner: String, jobIndex: Int): String = "${owner.lowercase(Locale.ROOT)}|$jobIndex"

    /** 「不保存，直接開始」的待完成紀錄；不分帳號（未登入也會用）。 */
    fun discardKey(jobIndex: Int): String = "discard|$jobIndex"

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
    /** 自己上傳過、本機還沒完成的那次換工作：維持待完成、不顯示，標記 uploaded。 */
    val pendingKeysToMarkUploaded: Set<String>,
)

object JobArchiveMergePolicy {
    /**
     * 與 iOS 不同的一點：本機已有、且仍有 pending 的那筆**不清 pending**。
     * Android 的本機提交分兩階段，那代表 Room 階段已完成、DataStore 階段待前滾，交給復原處理。
     */
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
        val markUploaded = mutableSetOf<String>()
        for (row in rows) {
            val key = row.id.lowercase(Locale.ROOT)
            if (key in known) continue
            val pendingKey = pendingById[key]
            if (pendingKey != null) {
                markUploaded += pendingKey
                continue
            }
            toInsert += row
            known += key
        }
        return JobArchiveMergePlan(toInsert, markUploaded)
    }
}

/**
 * 換工作流程（§19.6 資料安全順序，紅線：資料不得遺失）：
 * 1. 只在記憶體產生 payload，先把「待完成封存」（含完整 payload 與重設參數）寫進本機持久儲存
 * 2. 上傳 archive_my_job —— 失敗就停，本機資料完全不動
 * 3. Room 階段：插入本機封存＋刪除被封存的章（單一 transaction）
 * 4. DataStore 階段：重設 settings/plan＋刪除 pending（單一 edit）
 *
 * 3 之後、4 之前被殺 → [recoverPendingTransitions] 會偵測（本機已有封存＋pending 仍在）並前滾 4。
 * 1～3 之間被殺 → 本機沒有任何東西被改，pending 留著等使用者下次換工作時沿用同一 id。
 */
class JobTransitionCoordinator(
    private val local: JobTransitionLocalStore,
    private val remote: JobArchiveRemote,
    private val pendingStore: JobArchivePendingStore,
    private val nowMillis: () -> Long = System::currentTimeMillis,
    private val newUuid: () -> String = { UUID.randomUUID().toString() },
    private val canonicalReason: (String) -> String? = JobTransitionPolicy::defaultCanonicalReason,
) {
    /**
     * 前滾所有「Room 階段已提交、DataStore 階段未完成」的 pending。冪等：再跑一次是 no-op。
     * 尚未進入 Room 階段的 pending（含上傳中被殺）不動——那時本機資料一筆都沒改。
     * App 啟動時、以及每次換工作／拉回之前呼叫。
     */
    suspend fun recoverPendingTransitions(): List<JobTransitionResult> {
        val results = mutableListOf<JobTransitionResult>()
        for ((key, entry) in pendingStore.load()) {
            if (entry.kind == JobArchivePendingEntry.KIND_DISCARD) {
                // 使用者已明確選擇放棄：完成那次「不保存」（只刪當時記下的章；冪等）。
                local.commitRoomPhase(archive = null, stampIds = entry.archivedStampIds.orEmpty().toSet())
                local.resetCurrentJobAndClearPending(listOf(key)) { settings, plan ->
                    if (settings.currentJobIndex > entry.jobIndex) settings to plan
                    else resetFor(entry, entry.jobIndex, settings) to JobTransitionPolicy.resetPlanForNewJob(plan)
                }
                continue
            }
            val owner = entry.ownerUserId.lowercase(Locale.ROOT)
            val archive = local.archivesForOwner(owner)
                .firstOrNull { it.id.lowercase(Locale.ROOT) == entry.id.lowercase(Locale.ROOT) }
                ?: continue
            // 不在這裡刪章：Room 階段已在同一個 transaction 刪掉被封存的章；
            // 之後若還有章，那是封存裡沒有的資料，寧可保留。
            var clearedTodoIds: List<String> = emptyList()
            local.resetCurrentJobAndClearPending(listOf(key)) { settings, plan ->
                if (settings.currentJobIndex > entry.jobIndex) {
                    settings to plan // 已經是下一份工作：只清 pending。
                } else {
                    clearedTodoIds = JobTransitionPolicy.scheduledTodoIds(plan)
                    resetFor(entry, archive.jobIndex, settings) to JobTransitionPolicy.resetPlanForNewJob(plan)
                }
            }
            results += JobTransitionResult(archive, clearedTodoIds)
        }
        return results
    }

    suspend fun archiveAndStartNewJob(
        ownerId: String?,
        newCompanyName: String,
        maxStamps: Int = JobTransitionPolicy.DEFAULT_MAX_STAMPS,
    ): JobTransitionResult {
        val owner = ownerId?.trim()?.lowercase(Locale.ROOT)?.takeIf(String::isNotEmpty)
            ?: throw JobTransitionException.NotSignedIn()
        // 上一次換工作若停在 Room 階段之後，先把它完成；那就是使用者要的這次換工作，不再封存一份空的。
        recoverPendingTransitions().firstOrNull { it.archive.ownerUserId == owner }?.let { return it }

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
        val newRunNumber = JobTransitionPolicy.runNumber(current.settings, ownedArchives.map { it.dedupeKey }) + 1
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
            newCompanyName = newCompanyName,
            maxStamps = maxStamps,
            newRunNumber = newRunNumber,
            archivedStampIds = current.stamps.map { it.id },
        )
        // 1. 待完成封存（含完整快照）先落地。
        upsertPending(pendingKey, pending)

        // 2. 上傳。失敗直接拋出：到這裡為止本機資料沒有任何變更（只多了 pending）。
        val remoteId = remote.upload(archiveId, payload).trim().lowercase(Locale.ROOT).ifEmpty { archiveId }

        // 3. 重讀目前資料（上傳期間可能又蓋了章），以最新資料組本機封存，確保被刪的章都在封存裡。
        val fresh = local.readCurrentJob()
        val finalPayload = if (fresh.stamps.map { it.id }.toSet() == current.stamps.map { it.id }.toSet()) {
            payload
        } else {
            JobTransitionPolicy.makeSnapshot(
                fresh.settings, fresh.stamps, fresh.plan, now, canonicalReason = canonicalReason,
            ).copy(jobIndex = archiveNumber)
        }
        pending = pending.copy(
            uploaded = true,
            payloadJson = JobArchiveCoding.encodePayload(finalPayload),
            stampCount = finalPayload.stampCount,
            archivedStampIds = fresh.stamps.map { it.id },
        )
        upsertPending(pendingKey, pending)

        val alreadyLocal = local.archivesForOwner(owner)
            .firstOrNull { it.dedupeKey == remoteId || it.id.lowercase(Locale.ROOT) == archiveId }
        val archive = alreadyLocal ?: finalPayload.toEntity(
            id = archiveId,
            owner = owner,
            remoteId = remoteId,
            syncedAt = nowMillis(),
        )
        try {
            local.commitRoomPhase(if (alreadyLocal == null) archive else null, fresh.stamps.map { it.id }.toSet())
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            throw JobTransitionException.LocalSaveFailed(error)
        }

        // 4. DataStore 階段：重設＋清 pending 一次完成。失敗時 Room 已提交，復原會前滾。
        try {
            local.resetCurrentJobAndClearPending(listOf(pendingKey)) { settings, plan ->
                resetFor(pending, archiveNumber, settings) to JobTransitionPolicy.resetPlanForNewJob(plan)
            }
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            throw JobTransitionException.LocalSaveFailed(error)
        }
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
        recoverPendingTransitions()
        val owner = ownerId?.trim()?.lowercase(Locale.ROOT)?.takeIf(String::isNotEmpty)
        val current = local.readCurrentJob()
        val owned = owner?.let { local.archivesForOwner(it) }
        val newJobIndex = JobTransitionPolicy.nextArchiveNumber(
            current.settings.currentJobIndex,
            owned?.map { it.jobIndex },
        ) + 1
        val newRunNumber = JobTransitionPolicy.runNumber(current.settings, owned?.map { it.dedupeKey }) + 1
        val jobIndex = current.settings.currentJobIndex
        val discardKey = JobArchivePendingKeys.discardKey(jobIndex)
        val entry = JobArchivePendingEntry(
            id = newUuid().lowercase(Locale.ROOT),
            jobIndex = jobIndex,
            ownerUserId = owner.orEmpty(),
            createdAt = BackupDateCodec.format(nowMillis()),
            newCompanyName = newCompanyName,
            maxStamps = maxStamps,
            newRunNumber = newRunNumber,
            newJobIndex = newJobIndex,
            kind = JobArchivePendingEntry.KIND_DISCARD,
            archivedStampIds = current.stamps.map { it.id },
        )
        // 1. 先落地意圖：之後任一步被殺，啟動時的復原都會把這次「不保存」完成，不會停在
        //    「章已刪、設定還是舊工作」的半套狀態。
        upsertPending(discardKey, entry)
        // 2. Room：只刪當時記下的章。
        local.commitRoomPhase(archive = null, stampIds = entry.archivedStampIds.orEmpty().toSet())
        // 3. DataStore：重設＋清掉這次與這份工作的封存 pending，一次完成。
        val keys = listOfNotNull(discardKey, owner?.let { JobArchivePendingKeys.key(it, jobIndex) })
        local.resetCurrentJobAndClearPending(keys) { settings, plan ->
            resetFor(entry, jobIndex, settings) to JobTransitionPolicy.resetPlanForNewJob(plan)
        }
        return JobTransitionPolicy.scheduledTodoIds(current.plan)
    }

    /** 登入後／回前景拉回過往工作補到本機（以 帳號 + id 去重）。不修改 currentJobIndex。 */
    suspend fun pullFromServer(ownerId: String?, currentOwner: () -> String?): Int {
        recoverPendingTransitions()
        val owner = ownerId?.trim()?.lowercase(Locale.ROOT)?.takeIf(String::isNotEmpty) ?: return 0
        val rows = remote.fetchAll()
        if (rows.isEmpty()) return 0
        // await 期間可能已登出或換帳號：資料只歸給拉取開始時的帳號。
        if (currentOwner()?.lowercase(Locale.ROOT) != owner) return 0
        local.claimUnowned(rows.map { it.id }.toSet(), owner)
        val pending = pendingStore.load()
        val plan = JobArchiveMergePolicy.plan(rows, local.archivesForOwner(owner), owner, pending)
        if (plan.pendingKeysToMarkUploaded.isNotEmpty()) {
            val updated = pending.toMutableMap()
            plan.pendingKeysToMarkUploaded.forEach { key -> updated[key]?.let { updated[key] = it.copy(uploaded = true) } }
            pendingStore.save(updated)
        }
        val now = nowMillis()
        return local.insertArchivesIfAbsent(
            plan.toInsert.map { it.payload.toEntity(id = it.id, owner = owner, remoteId = it.id, syncedAt = now) },
        )
    }

    private fun resetFor(entry: JobArchivePendingEntry, archiveNumber: Int, settings: AppSettings): AppSettings =
        JobTransitionPolicy.resetSettingsForNewJob(
            settings = settings,
            newCompanyName = entry.newCompanyName.orEmpty(),
            maxStamps = entry.maxStamps ?: JobTransitionPolicy.DEFAULT_MAX_STAMPS,
            newJobIndex = entry.newJobIndex ?: (archiveNumber + 1),
            newRunNumber = entry.newRunNumber ?: (JobTransitionPolicy.runNumber(settings, null) + 1),
        )

    private suspend fun upsertPending(key: String, entry: JobArchivePendingEntry) {
        pendingStore.save(pendingStore.load() + (key to entry))
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
