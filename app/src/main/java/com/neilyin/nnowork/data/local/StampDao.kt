package com.neilyin.nnowork.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface StampDao {

    @Query("SELECT * FROM stamp_records ORDER BY date ASC")
    fun observeAll(): Flow<List<StampRecordEntity>>

    @Query("SELECT * FROM stamp_records ORDER BY date ASC")
    suspend fun getAll(): List<StampRecordEntity>

    @Query("SELECT COUNT(*) FROM stamp_records")
    fun observeCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM stamp_records WHERE roundNumber = :round")
    suspend fun countForRound(round: Int): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: StampRecordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<StampRecordEntity>)

    @Update
    suspend fun update(record: StampRecordEntity)

    @Delete
    suspend fun delete(record: StampRecordEntity)

    @Query("DELETE FROM stamp_records")
    suspend fun deleteAll()

    /** 備份還原的「全刪全換」語意；中途失敗時 Room 會整筆回滾。 */
    @Transaction
    suspend fun replaceAll(records: List<StampRecordEntity>) {
        deleteAll()
        if (records.isNotEmpty()) insertAll(records)
    }
}
