package com.neilyin.nnowork.data.shop

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/** 本機優先的商店交易 outbox；syncedAt == null 代表尚待上傳。 */
@Entity(
    tableName = "shop_purchase_records",
    indices = [Index("purchasedAt"), Index("syncedAt")],
)
data class ShopPurchaseEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val productType: String,
    val productId: String,
    val productName: String,
    val quantity: Int = 1,
    val unitCoinCost: Int,
    val totalCoinCost: Int,
    val currency: String = "coins",
    val status: String = "completed",
    val source: String = "android",
    val purchasedAt: Long = System.currentTimeMillis(),
    val syncedAt: Long? = null,
)
