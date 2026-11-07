package com.neilyin.nnowork.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.neilyin.nnowork.data.coin.CoinLedgerDao
import com.neilyin.nnowork.data.coin.CoinLedgerEntryEntity
import com.neilyin.nnowork.data.backup.BackupImportMarkerDao
import com.neilyin.nnowork.data.backup.BackupImportMarkerEntity
import com.neilyin.nnowork.data.shop.ShopPurchaseDao
import com.neilyin.nnowork.data.shop.ShopPurchaseEntity

@Database(
    entities = [StampRecordEntity::class, CoinLedgerEntryEntity::class, BackupImportMarkerEntity::class, ShopPurchaseEntity::class],
    version = 4,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun stampDao(): StampDao
    abstract fun coinLedgerDao(): CoinLedgerDao
    abstract fun backupImportMarkerDao(): BackupImportMarkerDao
    abstract fun shopPurchaseDao(): ShopPurchaseDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "nnowork.db",
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .build()
                    .also { INSTANCE = it }
            }

        /** v1 只有 stamp_records；v2 加入 append-only 金幣流水帳，不刪除任何舊資料。 */
        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `coin_ledger_entries` (
                        `id` TEXT NOT NULL,
                        `amount` INTEGER NOT NULL,
                        `type` TEXT NOT NULL,
                        `note` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_coin_ledger_entries_createdAt` " +
                        "ON `coin_ledger_entries` (`createdAt`)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_coin_ledger_entries_type` " +
                        "ON `coin_ledger_entries` (`type`)",
                )
            }
        }

        /** v3 只加入備份匯入 commit marker，不觸碰任何使用者資料。 */
        val MIGRATION_2_3: Migration = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `backup_import_marker` (
                        `slot` INTEGER NOT NULL,
                        `token` TEXT NOT NULL,
                        `committedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`slot`)
                    )
                    """.trimIndent(),
                )
            }
        }

        /** v4 加入商店交易 outbox；既有金幣與權益完全不變。 */
        val MIGRATION_3_4: Migration = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `shop_purchase_records` (
                        `id` TEXT NOT NULL,
                        `productType` TEXT NOT NULL,
                        `productId` TEXT NOT NULL,
                        `productName` TEXT NOT NULL,
                        `quantity` INTEGER NOT NULL,
                        `unitCoinCost` INTEGER NOT NULL,
                        `totalCoinCost` INTEGER NOT NULL,
                        `currency` TEXT NOT NULL,
                        `status` TEXT NOT NULL,
                        `source` TEXT NOT NULL,
                        `purchasedAt` INTEGER NOT NULL,
                        `syncedAt` INTEGER,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_shop_purchase_records_purchasedAt` " +
                        "ON `shop_purchase_records` (`purchasedAt`)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_shop_purchase_records_syncedAt` " +
                        "ON `shop_purchase_records` (`syncedAt`)",
                )
            }
        }
    }
}
