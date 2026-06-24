package com.neilyin.nnowork.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * 對應 iOS SwiftData 的 StampRecord。
 */
@Entity(tableName = "stamp_records")
data class StampRecordEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    /** 蓋章時間（epoch millis） */
    val date: Long = System.currentTimeMillis(),
    val reason: String = "",
    val isEasterEgg: Boolean = false,
    val roundNumber: Int = 1,
)
