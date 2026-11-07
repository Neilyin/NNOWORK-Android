package com.neilyin.nnowork.data.jobarchive

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.room.withTransaction
import com.neilyin.nnowork.data.JsonProvider
import com.neilyin.nnowork.data.datastore.appDataStore
import com.neilyin.nnowork.data.local.AppDatabase
import com.neilyin.nnowork.data.plan.ResignationPlan
import com.neilyin.nnowork.data.settings.AppSettings
import com.neilyin.nnowork.data.treehole.PostgrestError
import com.neilyin.nnowork.data.treehole.TreeHoleApi
import com.neilyin.nnowork.data.treehole.UserTokenCaller
import com.neilyin.nnowork.data.treehole.isMissingRpc
import com.neilyin.nnowork.data.treehole.postgrestError
import com.squareup.moshi.JsonWriter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okio.Buffer
import retrofit2.HttpException
import java.io.IOException
import java.util.Locale

/**
 * 換工作封存（android開發.md §19）的 Android 實作：Room 存過往工作、DataStore 存待完成封存，
 * RPC `archive_my_job` / `get_my_job_archives`（只能帶 user token）。
 *
 * UI 入口尚未接上（另一期處理）；本類別只提供資料層。
 */
class JobArchiveRepository internal constructor(
    private val context: Context,
    private val database: AppDatabase,
    private val api: () -> TreeHoleApi,
    private val tokenCaller: UserTokenCaller,
    private val currentOwnerId: () -> String?,
    private val nowMillis: () -> Long = System::currentTimeMillis,
) {
    private val dao = database.jobArchiveDao()
    private val operationMutex = Mutex()
    private val settingsAdapter = JsonProvider.adapter<AppSettings>()
    private val planAdapter = JsonProvider.adapter<ResignationPlan>()

    private val localStore = object : JobTransitionLocalStore {
        override suspend fun readCurrentJob(): CurrentJobSnapshot {
            val prefs = context.appDataStore.data.first()
            val settings = prefs[SETTINGS_KEY]?.let { runCatching { settingsAdapter.fromJson(it) }.getOrNull() }
                ?: AppSettings()
            val plan = prefs[PLAN_KEY]?.let { runCatching { planAdapter.fromJson(it) }.getOrNull() }
                ?: ResignationPlan()
            val stamps = database.withTransaction { database.stampDao().getAll() }
            return CurrentJobSnapshot(settings, stamps, plan)
        }

        override suspend fun archivesForOwner(owner: String): List<JobArchiveEntity> =
            dao.getForOwner(owner.lowercase(Locale.ROOT))

        override suspend fun commitTransition(
            archive: JobArchiveEntity?,
            transformSettings: (AppSettings) -> AppSettings,
            transformPlan: (ResignationPlan) -> ResignationPlan,
        ) {
            database.withTransaction {
                archive?.let { dao.insertIfAbsent(it) }
                database.stampDao().deleteAll()
                // 最後一步、且是單次原子 edit；若拋錯，Room transaction 會連同上面兩步一起 rollback。
                context.appDataStore.edit { prefs ->
                    val settings = prefs[SETTINGS_KEY]?.let { runCatching { settingsAdapter.fromJson(it) }.getOrNull() }
                        ?: AppSettings()
                    val plan = prefs[PLAN_KEY]?.let { runCatching { planAdapter.fromJson(it) }.getOrNull() }
                        ?: ResignationPlan()
                    prefs[SETTINGS_KEY] = settingsAdapter.toJson(transformSettings(settings))
                    prefs[PLAN_KEY] = planAdapter.toJson(transformPlan(plan))
                }
            }
        }

        override suspend fun insertArchivesIfAbsent(archives: List<JobArchiveEntity>): Int =
            if (archives.isEmpty()) 0 else database.withTransaction {
                archives.count { dao.insertIfAbsent(it) != -1L }
            }

        override suspend fun claimUnowned(ids: Set<String>, owner: String): Int = database.withTransaction {
            ids.sumOf { dao.claimUnowned(it.lowercase(Locale.ROOT), owner.lowercase(Locale.ROOT)) }
        }
    }

    private val pendingStore = object : JobArchivePendingStore {
        override suspend fun load(): Map<String, JobArchivePendingEntry> =
            JobArchivePendingKeys.decode(context.appDataStore.data.first()[PENDING_KEY])

        override suspend fun save(entries: Map<String, JobArchivePendingEntry>) {
            context.appDataStore.edit { prefs ->
                if (entries.isEmpty()) prefs.remove(PENDING_KEY)
                else prefs[PENDING_KEY] = JobArchivePendingKeys.encode(entries)
            }
        }
    }

    private val remote = object : JobArchiveRemote {
        override suspend fun upload(id: String, payload: JobArchivePayload): String = mapErrors {
            val body = JobArchiveWire.archiveRequestJson(id, payload).toJsonBody()
            tokenCaller.callSignedIn { authorization -> api().archiveMyJob(authorization, body) }
                ?.id?.takeIf(String::isNotBlank)
                ?: if (tokenCaller.isSignedIn) id else throw JobTransitionException.NotSignedIn()
        }

        override suspend fun fetchAll(): List<JobArchiveRemoteRow> = mapErrors {
            val json = tokenCaller.callSignedIn { authorization ->
                api().getMyJobArchives(authorization, "{}".toJsonBody()).use { it.string() }
            } ?: throw JobTransitionException.NotSignedIn()
            JobArchiveCoding.decodeRemoteRows(json)
        }
    }

    private val coordinator = JobTransitionCoordinator(localStore, remote, pendingStore, nowMillis)

    /** 「過往工作」只顯示目前帳號的封存；未登入時不顯示任何封存。 */
    fun visibleArchives(ownerId: String?): Flow<List<JobArchiveEntity>> =
        dao.observeAll().map { JobArchiveVisibility.visible(it, ownerId) }

    suspend fun archiveAndStartNewJob(
        newCompanyName: String,
        maxStamps: Int = JobTransitionPolicy.DEFAULT_MAX_STAMPS,
    ): JobTransitionResult = operationMutex.withLock {
        coordinator.archiveAndStartNewJob(currentOwnerId(), newCompanyName, maxStamps)
    }

    /** 使用者明確選「不保存，直接開始」時才可呼叫。 */
    suspend fun startNewJobWithoutArchive(
        newCompanyName: String,
        maxStamps: Int = JobTransitionPolicy.DEFAULT_MAX_STAMPS,
    ): List<String> = operationMutex.withLock {
        coordinator.startNewJobWithoutArchive(currentOwnerId(), newCompanyName, maxStamps)
    }

    /** 登入成功當下、以及每次回前景（已登入）時呼叫。失敗靜默（回 0）。 */
    suspend fun pullFromServer(): Int = operationMutex.withLock {
        val owner = currentOwnerId() ?: return@withLock 0
        try {
            coordinator.pullFromServer(owner, currentOwnerId)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            0
        }
    }

    /** 刪除帳號成功後呼叫（owner 要在刪除前取得）；只刪該帳號的本機封存與待完成紀錄。 */
    suspend fun deleteAllLocal(ownerId: String?) = operationMutex.withLock {
        val owner = ownerId?.trim()?.lowercase(Locale.ROOT)?.takeIf(String::isNotEmpty) ?: return@withLock
        pendingStore.save(pendingStore.load().filterValues { it.ownerUserId.lowercase(Locale.ROOT) != owner })
        database.withTransaction { dao.deleteForOwner(owner) }
    }

    /** 送文 job_index 用的周目（不是 currentJobIndex）。 */
    suspend fun currentRunNumber(): Int {
        val settings = context.appDataStore.data.first()[SETTINGS_KEY]
            ?.let { runCatching { settingsAdapter.fromJson(it) }.getOrNull() }
            ?: AppSettings()
        val owner = currentOwnerId()?.lowercase(Locale.ROOT)
        val keys = owner?.let { o -> dao.getForOwner(o).map { it.dedupeKey } }
        return JobTransitionPolicy.runNumber(settings, keys)
    }

    private suspend fun <T> mapErrors(block: suspend () -> T): T = try {
        block()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: JobTransitionException) {
        throw error
    } catch (error: HttpException) {
        throw JobArchiveErrors.map(error.code(), error.postgrestError(), error)
    } catch (error: IOException) {
        throw JobTransitionException.Network(error)
    } catch (error: Exception) {
        throw JobTransitionException.Network(error)
    }

    private companion object {
        val SETTINGS_KEY = stringPreferencesKey("settings_json")
        val PLAN_KEY = stringPreferencesKey("resignation_plan_json")
        val PENDING_KEY = stringPreferencesKey("job_archive_pending_uploads_v1")
        val JSON = "application/json".toMediaType()

        fun String.toJsonBody(): RequestBody = toRequestBody(JSON)
    }
}

object JobArchiveVisibility {
    fun visible(archives: List<JobArchiveEntity>, ownerId: String?): List<JobArchiveEntity> {
        val owner = ownerId?.trim()?.lowercase(Locale.ROOT)?.takeIf(String::isNotEmpty) ?: return emptyList()
        val seen = mutableSetOf<String>()
        return archives.filter { it.ownerUserId == owner && seen.add(it.dedupeKey) }
    }
}

object JobArchiveErrors {
    fun map(httpStatus: Int, error: PostgrestError?, cause: Throwable? = null): JobTransitionException {
        if (isMissingRpc(httpStatus, error)) return JobTransitionException.ServerNotReady(cause)
        if (httpStatus == 401) return JobTransitionException.NotSignedIn()
        val message = listOfNotNull(error?.message, error?.details, error?.hint).joinToString(" ")
        val lower = message.lowercase(Locale.ROOT)
        if ("job_archives" in lower || "archive_my_job" in lower || "does not exist" in lower) {
            return JobTransitionException.ServerNotReady(cause)
        }
        return JobTransitionException.Server(message.ifBlank { "HTTP $httpStatus" }, cause)
    }
}

/** archive_my_job 的 wire body（§19.2）。p_start_date 沒章時必須是明確的 null。 */
object JobArchiveWire {
    fun archiveRequestJson(id: String, payload: JobArchivePayload): String {
        val normalized = JobArchiveCoding.normalize(payload)
        val buffer = Buffer()
        JsonWriter.of(buffer).use { writer ->
            writer.serializeNulls = true
            writer.beginObject()
            writer.name("p_id").value(id.trim().lowercase(Locale.ROOT))
            writer.name("p_job_index").value(normalized.jobIndex.toLong())
            writer.name("p_company_name").value(normalized.companyName)
            writer.name("p_start_date")
            if (normalized.startDate == null) writer.nullValue() else writer.value(normalized.startDate)
            writer.name("p_end_date").value(normalized.endDate)
            writer.name("p_payload")
            writer.valueSink().use { sink -> sink.writeUtf8(JobArchiveCoding.encodePayload(normalized)) }
            writer.endObject()
        }
        return buffer.readUtf8()
    }
}
