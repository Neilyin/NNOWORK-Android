package com.neilyin.nnowork.data.backup

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

/**
 * 與備份匯入的 stamps/ledger 在同一個 Room transaction 提交。
 * 後續即使又有蓋章或金幣流水，仍能靠 token 判定當時的匯入是否已 commit。
 */
@Entity(tableName = "backup_import_marker")
data class BackupImportMarkerEntity(
    @PrimaryKey val slot: Int = SINGLETON_SLOT,
    val token: String,
    val committedAt: Long,
) {
    companion object {
        const val SINGLETON_SLOT = 1
    }
}

@Dao
interface BackupImportMarkerDao {
    @Query("SELECT token FROM backup_import_marker WHERE slot = :slot LIMIT 1")
    suspend fun currentToken(slot: Int = BackupImportMarkerEntity.SINGLETON_SLOT): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun markCommitted(marker: BackupImportMarkerEntity)
}
