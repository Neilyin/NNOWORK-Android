package com.neilyin.nnowork.data.coin

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface CoinLedgerDao {
    @Query("SELECT * FROM coin_ledger_entries ORDER BY createdAt ASC, id ASC")
    fun observeAll(): Flow<List<CoinLedgerEntryEntity>>

    @Query("SELECT * FROM coin_ledger_entries ORDER BY createdAt ASC, id ASC")
    suspend fun getAll(): List<CoinLedgerEntryEntity>

    @Query("SELECT COUNT(*) FROM coin_ledger_entries WHERE type = :type")
    suspend fun countByType(type: String): Int

    @Query("DELETE FROM coin_ledger_entries WHERE type = :type")
    suspend fun deleteByType(type: String): Int

    /** 一般業務 append；id 重複代表程式錯誤，必須拋錯。 */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun append(entry: CoinLedgerEntryEntity)

    /** 有穩定 idempotency id 的獎勵使用，回傳 -1 表示已存在。 */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun appendIfAbsent(entry: CoinLedgerEntryEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun appendAll(entries: List<CoinLedgerEntryEntity>)

    /** 僅限使用者明確「全部重置」或備份全量還原。 */
    @Query("DELETE FROM coin_ledger_entries")
    suspend fun deleteAllForFullReplacement()

    @Transaction
    suspend fun replaceAllForBackup(entries: List<CoinLedgerEntryEntity>) {
        deleteAllForFullReplacement()
        if (entries.isNotEmpty()) appendAll(entries)
    }
}
