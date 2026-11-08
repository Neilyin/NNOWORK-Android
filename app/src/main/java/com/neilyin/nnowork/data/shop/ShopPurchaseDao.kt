package com.neilyin.nnowork.data.shop

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface ShopPurchaseDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(record: ShopPurchaseEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(records: List<ShopPurchaseEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllIfAbsent(records: List<ShopPurchaseEntity>): List<Long>

    @Query("SELECT * FROM shop_purchase_records ORDER BY purchasedAt ASC, id ASC")
    suspend fun getAll(): List<ShopPurchaseEntity>

    @Query("SELECT * FROM shop_purchase_records WHERE syncedAt IS NULL ORDER BY purchasedAt ASC, id ASC LIMIT :limit")
    suspend fun getPending(limit: Int = 100): List<ShopPurchaseEntity>

    @Query("UPDATE shop_purchase_records SET syncedAt = :syncedAt WHERE id = :id AND syncedAt IS NULL")
    suspend fun markSynced(id: String, syncedAt: Long): Int

    @Query("DELETE FROM shop_purchase_records")
    suspend fun deleteAllForFullReplacement()

    @Transaction
    suspend fun replaceAllForBackup(records: List<ShopPurchaseEntity>) {
        deleteAllForFullReplacement()
        if (records.isNotEmpty()) insertAll(records)
    }
}
