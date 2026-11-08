package com.neilyin.nnowork.data.coin

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * 本機金幣流水帳。正數為進帳、負數為支出；餘額只能由全部 entry 折疊得到。
 *
 * 除了使用者明確的「全部重置」與備份全量還原外，業務邏輯只能 append。
 */
@Entity(
    tableName = "coin_ledger_entries",
    indices = [Index("createdAt"), Index("type")],
)
data class CoinLedgerEntryEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val amount: Int,
    val type: String,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)

enum class CoinTransactionType(val rawValue: String) {
    DAILY_STAMP("dailyStamp"),
    TREE_HOLE_POST_APPROVED("treeHolePostApproved"),
    FEEDBACK_REWARD("feedbackReward"),
    THEME_UNLOCK("themeUnlock"),
    UPDATE_COMPENSATION("updateCompensation"),
    MIGRATION("migration"),
    /** 舊測試版的一次性 seed；初始化時會自動移除。 */
    DEBUG_SEED("debugSeed"),
}
