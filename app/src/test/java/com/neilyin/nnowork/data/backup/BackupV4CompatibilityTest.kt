package com.neilyin.nnowork.data.backup

import com.neilyin.nnowork.data.plan.HourlyTrackingMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/** 備份 v4（iOS DataBackup.swift schemaVersion 4）：讀 iOS v4、仍讀 v3、Android 匯出 v4。 */
class BackupV4CompatibilityTest {
    private val codec = BackupCodec()

    /**
     * 依 iOS `DataBackupManager.encode`（.iso8601、.prettyPrinted、.sortedKeys）的輸出格式手寫：
     * UUID 為大寫、nil 欄位省略、含 Android 不認識的 manualStopwatch / manualSessions。
     */
    private val iosV4 = """
        {
          "appSettings" : [
            {
              "cardStyle" : 4,
              "coinCount" : 25,
              "commonReasons" : [ "老闆廢話太多", "無效會議" ],
              "companyName" : "新公司",
              "currentJobIndex" : 3,
              "currentRound" : 1,
              "currentRunNumber" : 2,
              "firstStampDate" : "2026-10-06T01:00:00Z",
              "hasMigratedToLedger" : true,
              "isCardLocked" : false,
              "isOnboardingFinished" : true,
              "lastCoinEarnedDate" : "2026-10-06T01:00:00Z",
              "lastSeenNoticeVersion" : "3.7.1",
              "maxStamps" : 10,
              "roundCompletedDates" : [ ],
              "roundStartDates" : [ "2026-10-06T00:00:00Z" ],
              "unlockedStyles" : [ 1, 2, 3, 4 ],
              "widgetRefreshInterval" : 60
            }
          ],
          "coinLedgerEntries" : [
            {
              "amount" : 20,
              "createdAt" : "2026-01-01T00:00:00Z",
              "id" : "0F8FAD5B-D9CB-469F-A165-70867728950E",
              "note" : "",
              "type" : "migration"
            },
            {
              "amount" : 5,
              "createdAt" : "2026-10-06T01:00:00Z",
              "id" : "6F1C4A52-1111-4F5B-9C1D-000000000001",
              "note" : "",
              "type" : "treeHolePostApproved"
            }
          ],
          "exportDate" : "2026-11-07T03:00:00Z",
          "jobArchives" : [
            {
              "id" : "3F2B9C1E-0000-4000-8000-000000000001",
              "ownerUserID" : "9a1e0c4d-1111-4222-8333-444455556666",
              "payload" : {
                "cardCount" : 3,
                "companyName" : "舊公司",
                "createdAt" : "2026-10-05T08:30:00Z",
                "endDate" : "2026-10-05T08:30:00Z",
                "jobIndex" : 2,
                "receipt" : {
                  "companyName" : "舊公司",
                  "completionDate" : "2026-10-01T09:00:00Z",
                  "firstStampDate" : "2026-03-02T01:15:00Z",
                  "issuedAt" : "2026-10-05T08:30:00Z",
                  "kind" : "master",
                  "savedAmount" : 12000,
                  "totalDays" : 213,
                  "totalStamps" : 30
                },
                "savedAmount" : 12000,
                "stampCount" : 30,
                "stamps" : [
                  { "date" : "2026-03-02T01:15:00Z", "isEasterEgg" : false, "reason" : "無效會議", "roundNumber" : 1 }
                ],
                "startDate" : "2026-03-02T01:15:00Z",
                "targetAmount" : 100000,
                "todos" : [ { "isCompleted" : true, "title" : "存錢" } ],
                "topReasons" : [ { "count" : 12, "reason" : "無效會議" } ],
                "version" : 1
              },
              "remoteID" : "3f2b9c1e-0000-4000-8000-000000000001",
              "syncedAt" : "2026-10-05T08:30:05Z"
            },
            {
              "id" : "4A2B9C1E-0000-4000-8000-000000000002",
              "payload" : {
                "endDate" : "2025-12-31T00:00:00Z",
                "jobIndex" : 1,
                "receipt" : {
                  "companyName" : "",
                  "completionDate" : "2025-12-31T00:00:00Z",
                  "issuedAt" : "2025-12-31T00:00:00Z",
                  "kind" : "resignation",
                  "savedAmount" : 0,
                  "totalDays" : 0,
                  "totalStamps" : 0
                }
              }
            }
          ],
          "lastAuthProvider" : "apple",
          "manualSessions" : [ { "endedAt" : "2026-10-06T09:00:00Z", "seconds" : 3600, "startedAt" : "2026-10-06T08:00:00Z" } ],
          "manualStopwatch" : { "accumulatedSeconds" : 120, "isRunning" : false },
          "niuMaConfig" : {
            "currencySymbol" : "NT$",
            "customRestDays" : [ "2026-10-10" ],
            "dayShifts" : { },
            "endMinutes" : 1080,
            "hourlyManualMode" : true,
            "hourlyRate" : 200,
            "lunchMinutes" : 60,
            "lunchUnpaid" : false,
            "monthlyScheduleMode" : 0,
            "monthlySalary" : 0,
            "payMode" : 1,
            "paydayDays" : [ 5 ],
            "shiftUseTemplates" : false,
            "shifts" : [ ],
            "startMinutes" : 540,
            "workDays" : [ 2, 3, 4, 5, 6 ]
          },
          "resignationPlans" : [
            {
              "completedTodoItems" : [ ],
              "completedTodoList" : [ ],
              "currentSaved" : 0,
              "hasResume" : false,
              "targetAmount" : 100000,
              "todoItems" : [ { "id" : "37DB5C14-C445-43C5-B1A6-78B28FEAE851", "notificationScheduled" : false, "title" : "面試" } ],
              "todoList" : [ ]
            }
          ],
          "schemaVersion" : 4,
          "shopPurchaseRecords" : [ ],
          "stampRecords" : [
            { "date" : "2026-10-06T01:00:00Z", "id" : "54157CCB-9CB8-4A9A-A8B7-95D494F5BDA8", "isEasterEgg" : false, "reason" : "同事雷", "roundNumber" : 1 }
          ]
        }
    """.trimIndent()

    private val iosV3 = """
        {
          "appSettings" : [ { "cardStyle" : 1, "coinCount" : 3, "commonReasons" : [ ], "companyName" : "A",
            "currentRound" : 2, "hasMigratedToLedger" : true, "isCardLocked" : false, "isOnboardingFinished" : true,
            "lastSeenNoticeVersion" : "3.5.1", "maxStamps" : 10, "roundCompletedDates" : [ "2026-05-01T00:00:00Z" ],
            "roundStartDates" : [ "2026-04-01T00:00:00Z", "2026-05-02T00:00:00Z" ], "unlockedStyles" : [ 1, 2, 3 ],
            "widgetRefreshInterval" : 60 } ],
          "coinLedgerEntries" : [ { "amount" : 3, "createdAt" : "2026-04-01T00:00:00Z",
            "id" : "0F8FAD5B-D9CB-469F-A165-70867728950E", "note" : "", "type" : "dailyStamp" } ],
          "exportDate" : "2026-08-01T00:00:00Z",
          "niuMaConfig" : { "currencySymbol" : "NT$", "customRestDays" : [ ], "dayShifts" : { }, "endMinutes" : 1080,
            "hourlyRate" : 0, "lunchMinutes" : 60, "lunchUnpaid" : false, "monthlyScheduleMode" : 0,
            "monthlySalary" : 40000, "payMode" : 0, "paydayDays" : [ 5 ], "shiftUseTemplates" : false, "shifts" : [ ],
            "startMinutes" : 540, "workDays" : [ 2, 3, 4, 5, 6 ] },
          "resignationPlans" : [ { "completedTodoItems" : [ ], "completedTodoList" : [ ], "currentSaved" : 10,
            "hasResume" : true, "targetAmount" : 100, "todoItems" : [ ], "todoList" : [ ] } ],
          "schemaVersion" : 3,
          "shopPurchaseRecords" : [ ],
          "stampRecords" : [ ]
        }
    """.trimIndent()

    @Test
    fun `iOS v4 sample decodes including job archives and job counters`() {
        val decoded = codec.decodeAndValidate(iosV4)
        assertEquals(4, decoded.schemaVersion)
        val settings = decoded.appSettings.single()
        assertEquals(3, settings.currentJobIndex)
        assertEquals(2, settings.currentRunNumber)
        assertEquals(2, decoded.jobArchives.size)

        val first = decoded.jobArchives[0]
        assertEquals("9a1e0c4d-1111-4222-8333-444455556666", first.ownerUserID)
        assertEquals(2, first.payload.jobIndex)
        assertEquals("master", first.payload.receipt.kind)
        assertEquals(1, first.payload.stamps.size)
        val second = decoded.jobArchives[1]
        assertNull(second.ownerUserID)
        assertNull(second.remoteID)
        assertEquals("createdAt falls back to endDate", "2025-12-31T00:00:00Z", second.payload.createdAt)
        assertEquals(2, decoded.jobArchivesForImport().size)
        assertEquals("apple", decoded.lastAuthProvider)
    }

    @Test
    fun `iOS hourly manual mode survives import and export`() {
        val decoded = codec.decodeAndValidate(iosV4)
        assertEquals(true, decoded.niuMaConfig.hourlyManualMode)
        assertEquals(HourlyTrackingMode.MANUAL, decoded.niuMaConfig.effectiveHourlyTrackingMode())
        assertEquals(
            HourlyTrackingMode.MANUAL,
            BackupNiuMaConfig(hourlyTrackingMode = HourlyTrackingMode.MANUAL).effectiveHourlyTrackingMode(),
        )
        assertEquals(
            HourlyTrackingMode.SCHEDULE,
            BackupNiuMaConfig(hourlyTrackingMode = HourlyTrackingMode.MANUAL, hourlyManualMode = false)
                .effectiveHourlyTrackingMode(),
        )
    }

    @Test
    fun `iOS v3 sample still decodes with v4 defaults and imports no archives`() {
        val decoded = codec.decodeAndValidate(iosV3)
        assertEquals(3, decoded.schemaVersion)
        assertEquals(1, decoded.appSettings.single().currentJobIndex)
        assertEquals(0, decoded.appSettings.single().currentRunNumber)
        assertTrue(decoded.jobArchives.isEmpty())
        assertTrue(decoded.jobArchivesForImport().isEmpty())
        assertNull(decoded.niuMaConfig.hourlyManualMode)
        assertEquals(HourlyTrackingMode.SCHEDULE, decoded.niuMaConfig.effectiveHourlyTrackingMode())
    }

    @Test
    fun `android export is v4 and round trips job archives with iOS key names`() {
        val decoded = codec.decodeAndValidate(iosV4)
        val exported = codec.encode(decoded.copy(schemaVersion = 3))
        assertTrue(exported.contains("\"schemaVersion\": 4"))
        assertTrue(exported.contains("\"jobArchives\""))
        assertTrue(exported.contains("\"ownerUserID\""))
        assertTrue(exported.contains("\"remoteID\""))
        assertTrue(exported.contains("\"currentJobIndex\": 3"))
        assertTrue(exported.contains("\"currentRunNumber\": 2"))
        assertFalse("nil values are omitted like iOS", exported.contains("\"ownerUserID\": null"))
        val reDecoded = codec.decodeAndValidate(exported)
        assertEquals(decoded.jobArchives, reDecoded.jobArchives)
        assertEquals(decoded.appSettings, reDecoded.appSettings)
    }

    @Test
    fun `archive entity mapping normalizes ids and keeps payload`() {
        val archive = codec.decodeAndValidate(iosV4).jobArchives[0]
        val entity = archive.toEntity()
        assertEquals("3f2b9c1e-0000-4000-8000-000000000001", entity.id)
        assertEquals("9a1e0c4d-1111-4222-8333-444455556666", entity.ownerUserId)
        assertEquals(2, entity.jobIndex)
        assertEquals(BackupDateCodec.parse("2026-10-05T08:30:00Z"), entity.endDate)
        val back = entity.toBackupOrNull()!!
        assertEquals(archive.payload, back.payload)
        assertEquals(archive.syncedAt, back.syncedAt)
        assertEquals("", codec.decodeAndValidate(iosV4).jobArchives[1].toEntity().ownerUserId)
        assertNull(codec.decodeAndValidate(iosV4).jobArchives[1].toEntity().toBackupOrNull()!!.ownerUserID)
    }

    @Test
    fun `restore merges archives by owner and id and keeps the larger job index`() {
        val decoded = codec.decodeAndValidate(iosV4)
        val imported = decoded.jobArchives.map { it.toEntity() }
        val existingSameRemote = imported[0].copy(id = "aaaaaaaa-0000-4000-8000-000000000009")
        val merged = BackupJobArchiveMerge.newArchives(imported, existing = listOf(existingSameRemote))
        assertEquals(listOf(imported[1].id), merged.map { it.id })
        assertEquals(imported, BackupJobArchiveMerge.newArchives(imported, existing = emptyList()))
        // 同 id 不同帳號互不吃掉。
        val otherOwner = imported[0].copy(ownerUserId = "someone-else", remoteId = null)
        assertEquals(2, BackupJobArchiveMerge.newArchives(imported, existing = listOf(otherOwner)).size)
        // 重複匯入同一份備份不會重複插入。
        assertTrue(BackupJobArchiveMerge.newArchives(imported, existing = imported).isEmpty())

        assertEquals(3, BackupJobArchiveMerge.mergedJobIndex(3, localSettingsJobIndex = 1, localArchiveJobIndexes = emptyList()))
        assertEquals(6, BackupJobArchiveMerge.mergedJobIndex(3, localSettingsJobIndex = 2, localArchiveJobIndexes = listOf(5)))
        assertEquals(4, BackupJobArchiveMerge.mergedJobIndex(1, localSettingsJobIndex = 4, localArchiveJobIndexes = emptyList()))
        assertEquals(1, BackupJobArchiveMerge.mergedJobIndex(1, localSettingsJobIndex = null, localArchiveJobIndexes = emptyList()))
    }

    @Test
    fun `invalid archive id or payload date is rejected before import`() {
        val badId = iosV4.replace("\"id\" : \"4A2B9C1E-0000-4000-8000-000000000002\"", "\"id\" : \"not-a-uuid\"")
        val badDate = iosV4.replace("\"endDate\" : \"2025-12-31T00:00:00Z\"", "\"endDate\" : \"2025-12-31\"")
        listOf(badId to "jobArchives[1].id", badDate to "jobArchives[1].payload").forEach { (json, path) ->
            try {
                codec.decodeAndValidate(json)
                fail("Expected validation failure at $path")
            } catch (error: BackupValidationException) {
                assertTrue(error.issues.toString(), error.issues.any { it.path == path })
            }
        }
        // 缺 receipt 的 payload：iOS 也會整份解碼失敗。
        val missingReceipt = iosV4.replace(Regex("\"receipt\" : \\{[^}]*\"kind\" : \"resignation\"[^}]*\\}"), "\"receiptX\" : {}")
        try {
            codec.decodeAndValidate(missingReceipt)
            fail("Expected decoding failure")
        } catch (_: BackupDecodingException) {
        }
    }
}
