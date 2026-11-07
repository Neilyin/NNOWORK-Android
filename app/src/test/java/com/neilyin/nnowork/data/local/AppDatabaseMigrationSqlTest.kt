package com.neilyin.nnowork.data.local

import com.neilyin.nnowork.data.JsonProvider
import com.neilyin.nnowork.data.jobarchive.JobArchiveDao
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.sql.Connection
import java.sql.DriverManager

/**
 * JVM 端、不需模擬器：用真的 SQLite 跑 v4 schema（app/schemas/…/4.json）＋ MIGRATION_4_5 的 SQL，
 * 確認舊資料都在，且 job_archives 的欄位／主鍵／索引與 Room 匯出的 5.json 完全一致。
 * （完整的 MigrationTestHelper 版本在 androidTest/AppDatabaseMigrationTest。）
 */
class AppDatabaseMigrationSqlTest {
    private fun schema(version: Int): Map<*, *> {
        val relative = "schemas/com.neilyin.nnowork.data.local.AppDatabase/$version.json"
        val file = listOf(File(relative), File("app/$relative")).first { it.isFile }
        return JsonProvider.moshi.adapter(Map::class.java).fromJson(file.readText())!!["database"] as Map<*, *>
    }

    private fun entities(schema: Map<*, *>): List<Map<*, *>> = (schema["entities"] as List<*>).map { it as Map<*, *> }

    private fun sqlFor(entity: Map<*, *>, key: String = "createSql"): String =
        (entity[key] as String).replace("\${TABLE_NAME}", entity["tableName"] as String)

    private fun createV4(connection: Connection) {
        val v4 = schema(4)
        assertEquals(4.0, v4["version"])
        connection.createStatement().use { statement ->
            entities(v4).forEach { entity ->
                statement.execute(sqlFor(entity))
                (entity["indices"] as List<*>?).orEmpty().forEach { index ->
                    val sql = (index as Map<*, *>)["createSql"] as String
                    statement.execute(sql.replace("\${TABLE_NAME}", entity["tableName"] as String))
                }
            }
            (v4["setupQueries"] as List<*>).forEach { statement.execute(it as String) }
        }
    }

    /** 以字面值帶入 Room 的具名參數（僅測試用）。 */
    private fun bind(sql: String, id: String, owner: String) =
        sql.replace(":owner", "'$owner'").replace(":id", "'$id'")

    @Test
    fun `claiming an unowned backup copy of an already owned archive no longer violates the primary key`() {
        DriverManager.getConnection("jdbc:sqlite::memory:").use { connection ->
            val v5 = entities(schema(5)).single { it["tableName"] == "job_archives" }
            connection.createStatement().use { statement ->
                statement.execute(sqlFor(v5))
                // 帳號 A 已拉回 X；匯入舊 iOS 備份帶進無主的 ("", X) 與 ("", Y)。
                listOf("x" to "user-a", "x" to "", "y" to "").forEach { (id, owner) ->
                    statement.execute(
                        "INSERT INTO job_archives (id, ownerUserId, remoteId, syncedAt, jobIndex, companyName, " +
                            "endDate, createdAt, payloadJson) VALUES ('$id', '$owner', NULL, NULL, 1, '', 1, 1, '{}')",
                    )
                }

                // 修正前的單純 UPDATE 會撞複合主鍵（重現 bug）。
                val legacyFailure = runCatching {
                    statement.execute(bind(JobArchiveDao.CLAIM_UNOWNED_SQL, "x", "user-a"))
                }.exceptionOrNull()
                assertTrue(legacyFailure?.message.orEmpty(), legacyFailure?.message.orEmpty().contains("UNIQUE"))

                // 修正後：同一個 transaction 內先刪重複、再認領（JobArchiveDao.claimUnowned）。
                connection.autoCommit = false
                listOf("x", "y").forEach { id ->
                    statement.execute(bind(JobArchiveDao.DELETE_UNOWNED_DUPLICATE_SQL, id, "user-a"))
                    statement.execute(bind(JobArchiveDao.CLAIM_UNOWNED_SQL, id, "user-a"))
                }
                connection.commit()
                connection.autoCommit = true

                val rows = mutableListOf<Pair<String, String>>()
                statement.executeQuery("SELECT ownerUserId, id FROM job_archives ORDER BY id").use { result ->
                    while (result.next()) rows += result.getString(1) to result.getString(2)
                }
                assertEquals(listOf("user-a" to "x", "user-a" to "y"), rows)

                // 再認領一次是 no-op。
                statement.execute(bind(JobArchiveDao.DELETE_UNOWNED_DUPLICATE_SQL, "x", "user-a"))
                assertEquals(0, statement.executeUpdate(bind(JobArchiveDao.CLAIM_UNOWNED_SQL, "x", "user-a")))
                // 其他帳號的同 id 封存不會被刪。
                statement.execute(
                    "INSERT INTO job_archives VALUES ('x', 'user-b', NULL, NULL, 1, '', 1, 1, '{}')",
                )
                statement.execute(bind(JobArchiveDao.DELETE_UNOWNED_DUPLICATE_SQL, "x", "user-a"))
                statement.executeQuery("SELECT COUNT(*) FROM job_archives WHERE id = 'x'").use { result ->
                    result.next()
                    assertEquals(2, result.getInt(1))
                }
            }
        }
    }

    @Test
    fun `v4 to v5 migration keeps rows and matches the exported v5 schema`() {
        DriverManager.getConnection("jdbc:sqlite::memory:").use { connection ->
            createV4(connection)
            connection.createStatement().use { statement ->
                statement.execute("INSERT INTO stamp_records VALUES ('s1', 1760000000000, '無效會議', 0, 2)")
                statement.execute("INSERT INTO coin_ledger_entries VALUES ('c1', 5, 'treeHolePostApproved', '', 1)")
                statement.execute("INSERT INTO backup_import_marker VALUES (0, 'token-1', 3)")

                // MIGRATION_4_5 執行的就是這兩句。
                statement.execute(AppDatabase.JOB_ARCHIVES_CREATE_SQL)
                statement.execute(AppDatabase.JOB_ARCHIVES_END_DATE_INDEX_SQL)

                statement.executeQuery("SELECT reason, roundNumber FROM stamp_records WHERE id = 's1'").use { rows ->
                    assertTrue(rows.next())
                    assertEquals("無效會議", rows.getString(1))
                    assertEquals(2, rows.getInt(2))
                }
                statement.executeQuery("SELECT amount FROM coin_ledger_entries").use { rows ->
                    assertTrue(rows.next())
                    assertEquals(5, rows.getInt(1))
                }
                statement.executeQuery("SELECT COUNT(*) FROM job_archives").use { rows ->
                    assertTrue(rows.next())
                    assertEquals(0, rows.getInt(1))
                }
            }

            // 與 Room 匯出的 v5 job_archives 定義逐字相同（Room 開檔驗證比的就是這些欄位）。
            val v5 = entities(schema(5)).single { it["tableName"] == "job_archives" }
            assertEquals(sqlFor(v5), AppDatabase.JOB_ARCHIVES_CREATE_SQL)
            val v5Index = (v5["indices"] as List<*>).single() as Map<*, *>
            assertEquals(
                (v5Index["createSql"] as String).replace("\${TABLE_NAME}", "job_archives"),
                AppDatabase.JOB_ARCHIVES_END_DATE_INDEX_SQL,
            )

            val expectedColumns = (v5["fields"] as List<*>).associate { field ->
                field as Map<*, *>
                field["columnName"] as String to (field["affinity"] as String to (field["notNull"] as Boolean))
            }
            val actualColumns = mutableMapOf<String, Pair<String, Boolean>>()
            val primaryKey = sortedMapOf<Int, String>()
            connection.createStatement().use { statement ->
                statement.executeQuery("PRAGMA table_info(job_archives)").use { rows ->
                    while (rows.next()) {
                        val name = rows.getString("name")
                        actualColumns[name] = rows.getString("type") to (rows.getInt("notnull") == 1)
                        rows.getInt("pk").takeIf { it > 0 }?.let { primaryKey[it] = name }
                    }
                }
            }
            assertEquals(expectedColumns, actualColumns)
            val expectedPk = ((v5["primaryKey"] as Map<*, *>)["columnNames"] as List<*>).map { it as String }
            assertEquals(expectedPk, primaryKey.values.toList())

            // 其餘 v4 表在 v5 沒有任何變動（新版不需要動舊表）。
            val v4Tables = entities(schema(4)).associate { it["tableName"] to sqlFor(it) }
            val v5Tables = entities(schema(5)).filter { it["tableName"] != "job_archives" }.associate { it["tableName"] to sqlFor(it) }
            assertEquals(v4Tables, v5Tables)
        }
    }
}
