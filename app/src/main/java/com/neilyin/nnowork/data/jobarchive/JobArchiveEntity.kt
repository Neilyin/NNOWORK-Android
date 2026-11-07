package com.neilyin.nnowork.data.jobarchive

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * 本機「過往工作」。封存後唯讀：只會新增（換工作、登入拉回、備份合併），
 * 刪除只發生在刪除帳號時（只刪該帳號的）。
 *
 * - (ownerUserId, id) 為主鍵：不同帳號的同 id 封存互不吃掉（iOS ownerScopedKey）。
 * - ownerUserId 為 Supabase auth user id（小寫）；空字串 = 尚無擁有者（例如舊備份匯入）。
 * - payloadJson 是完整 JobArchivePayload；顯示一律以 payload 為準，其餘欄位只供排序/去重。
 */
@Entity(
    tableName = "job_archives",
    primaryKeys = ["ownerUserId", "id"],
    indices = [Index("endDate")],
)
data class JobArchiveEntity(
    val id: String,
    val ownerUserId: String = "",
    val remoteId: String? = null,
    val syncedAt: Long? = null,
    val jobIndex: Int,
    val companyName: String,
    val endDate: Long,
    val createdAt: Long,
    val payloadJson: String,
) {
    /** 去重用的穩定鍵：伺服器 id 優先，沒有就用本機 id（iOS dedupeKey）。 */
    val dedupeKey: String get() = remoteId?.lowercase()?.takeIf(String::isNotEmpty) ?: id.lowercase()

    val payload: JobArchivePayload? get() = JobArchiveCoding.decodePayload(payloadJson)
}

@Dao
interface JobArchiveDao {
    @Query("SELECT * FROM job_archives ORDER BY endDate DESC")
    fun observeAll(): Flow<List<JobArchiveEntity>>

    @Query("SELECT * FROM job_archives ORDER BY endDate DESC")
    suspend fun getAll(): List<JobArchiveEntity>

    @Query("SELECT * FROM job_archives WHERE ownerUserId = :owner ORDER BY endDate DESC")
    suspend fun getForOwner(owner: String): List<JobArchiveEntity>

    /** 已存在（同擁有者同 id）就略過，回傳 -1。 */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(entity: JobArchiveEntity): Long

    @Query("UPDATE job_archives SET ownerUserId = :owner WHERE ownerUserId = '' AND id = :id")
    suspend fun claimUnowned(id: String, owner: String): Int

    @Query("DELETE FROM job_archives WHERE ownerUserId = :owner")
    suspend fun deleteForOwner(owner: String): Int
}
