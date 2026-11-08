package com.neilyin.nnowork.data.local

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Room v4 → v5（新增 job_archives）。schema 來源：app/schemas（androidTest assets）。
 * 4.json 由 v4 的 Room 產生碼重建（identity hash b8568539…），與已上線 v4 完全相同。
 */
@RunWith(AndroidJUnit4::class)
class AppDatabaseMigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
    )

    @Test
    fun migrate4To5KeepsEveryRowAndCreatesJobArchives() {
        helper.createDatabase(DB_NAME, 4).apply {
            execSQL(
                "INSERT INTO stamp_records (id, date, reason, isEasterEgg, roundNumber) " +
                    "VALUES ('s1', 1760000000000, '無效會議', 0, 2), ('s2', 1760000100000, '同事雷', 1, 3)",
            )
            execSQL(
                "INSERT INTO coin_ledger_entries (id, amount, type, note, createdAt) " +
                    "VALUES ('c1', 20, 'migration', '', 1), ('c2', 5, 'treeHolePostApproved', 'x', 2)",
            )
            execSQL("INSERT INTO backup_import_marker (slot, token, committedAt) VALUES (0, 'token-1', 3)")
            execSQL(
                "INSERT INTO shop_purchase_records (id, productType, productId, productName, quantity, " +
                    "unitCoinCost, totalCoinCost, currency, status, source, purchasedAt, syncedAt) " +
                    "VALUES ('p1', 'theme', 'theme.7', '主題', 1, 30, 30, 'coins', 'completed', 'shop', 4, NULL)",
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(DB_NAME, 5, true, AppDatabase.MIGRATION_4_5)

        db.query("SELECT id, reason, isEasterEgg, roundNumber FROM stamp_records ORDER BY id").use { cursor ->
            assertEquals(2, cursor.count)
            cursor.moveToFirst()
            assertEquals("s1", cursor.getString(0))
            assertEquals("無效會議", cursor.getString(1))
            assertEquals(2, cursor.getInt(3))
        }
        db.query("SELECT SUM(amount) FROM coin_ledger_entries").use { cursor ->
            cursor.moveToFirst()
            assertEquals(25, cursor.getInt(0))
        }
        db.query("SELECT token FROM backup_import_marker").use { cursor ->
            cursor.moveToFirst()
            assertEquals("token-1", cursor.getString(0))
        }
        db.query("SELECT COUNT(*) FROM shop_purchase_records").use { cursor ->
            cursor.moveToFirst()
            assertEquals(1, cursor.getInt(0))
        }
        db.query("SELECT COUNT(*) FROM job_archives").use { cursor ->
            cursor.moveToFirst()
            assertEquals(0, cursor.getInt(0))
        }
        // 同 id、不同擁有者可以並存（複合主鍵）。
        db.execSQL(
            "INSERT INTO job_archives (id, ownerUserId, remoteId, syncedAt, jobIndex, companyName, endDate, " +
                "createdAt, payloadJson) VALUES ('a1', 'u1', NULL, NULL, 1, '舊公司', 5, 5, '{}'), " +
                "('a1', 'u2', NULL, NULL, 1, '舊公司', 5, 5, '{}')",
        )
        db.close()

        // 用真的 AppDatabase（含全部 migration）開啟：Room 會再驗證一次 schema。
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val room = Room.databaseBuilder(context, AppDatabase::class.java, DB_NAME)
            .addMigrations(
                AppDatabase.MIGRATION_1_2,
                AppDatabase.MIGRATION_2_3,
                AppDatabase.MIGRATION_3_4,
                AppDatabase.MIGRATION_4_5,
            )
            .build()
        try {
            runBlocking {
                assertEquals(2, room.stampDao().getAll().size)
                assertEquals(2, room.jobArchiveDao().getAll().size)
                assertTrue(room.jobArchiveDao().getForOwner("u1").single().id == "a1")
            }
        } finally {
            room.close()
        }
    }

    private companion object {
        const val DB_NAME = "migration-4-5-test.db"
    }
}
