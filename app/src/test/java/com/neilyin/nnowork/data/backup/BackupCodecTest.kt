package com.neilyin.nnowork.data.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class BackupCodecTest {
    private val codec = BackupCodec()

    @Test
    fun `current v4 payload round trips every cross-platform section`() {
        val payload = BackupPayload(
            schemaVersion = BackupPayload.CURRENT_SCHEMA_VERSION,
            exportDate = "2026-07-19T08:30:00Z",
            appSettings = listOf(
                BackupAppSettings(
                    companyName = "NNOWORK",
                    cardStyle = 6,
                    maxStamps = 12,
                    currentRound = 4,
                    isOnboardingFinished = true,
                    widgetRefreshInterval = 30,
                    firstStampDate = "2026-07-01T01:02:03Z",
                    isCardLocked = true,
                    cardCompletedDate = "2026-07-18T10:11:12Z",
                    roundStartDates = listOf("2026-07-01T01:02:03Z"),
                    roundCompletedDates = listOf("2026-07-18T10:11:12Z"),
                    coinCount = 99,
                    lastCoinEarnedDate = "2026-07-18T01:00:00Z",
                    lastTreeHoleShareDate = "2026-07-18T02:00:00Z",
                    unlockedStyles = listOf(1, 2, 3, 6),
                    lastSeenNoticeVersion = "3.4.0",
                    commonReasons = listOf("無效會議", "需求一直改"),
                    hasMigratedToLedger = true,
                ),
            ),
            stampRecords = listOf(
                BackupStampRecord(
                    id = "54157ccb-9cb8-4a9a-a8b7-95d494f5bda8",
                    date = "2026-07-17T09:08:07Z",
                    reason = "加班沒錢",
                    isEasterEgg = true,
                    roundNumber = 4,
                ),
            ),
            resignationPlans = listOf(
                BackupResignationPlan(
                    targetAmount = 200_000.0,
                    currentSaved = 80_000.0,
                    hasResume = true,
                    todoItems = listOf(
                        BackupTodoItem(
                            id = "37db5c14-c445-43c5-b1a6-78b28feae851",
                            title = "更新履歷",
                            dueDate = "2026-07-30T12:00:00Z",
                            notificationScheduled = true,
                            createdAt = "2026-07-19T08:00:00Z",
                        ),
                    ),
                    completedTodoItems = listOf(
                        BackupTodoItem(
                            id = "37284387-359a-4072-b666-c598afc0fc30",
                            title = "收集作品",
                        ),
                    ),
                    todoList = listOf("舊格式待辦"),
                    completedTodoList = listOf("舊格式完成"),
                ),
            ),
            niuMaConfig = BackupNiuMaConfig(
                payMode = 1,
                hourlyTrackingMode = 1,
                monthlySalary = 42_000.0,
                hourlyRate = 220.0,
                startMinutes = 480,
                endMinutes = 1_020,
                workDays = listOf(2, 3, 4, 5, 6),
                paydayDays = listOf(5, 20),
                currencySymbol = "NT$",
                lunchUnpaid = true,
                lunchMinutes = 45,
                monthlyScheduleMode = 1,
                customRestDays = listOf("2026-07-20"),
                shifts = listOf(
                    BackupShift(
                        id = "day-shift",
                        name = "早班",
                        startMinutes = 480,
                        endMinutes = 960,
                        colorHex = "#3B8EEA",
                        icon = "sun.horizon.fill",
                    ),
                ),
                dayShifts = mapOf("2026-07-21" to "day-shift"),
                shiftUseTemplates = true,
                manualTimerStatus = 2,
                manualTimerElapsedMillis = 5_400_000L,
            ),
            coinLedgerEntries = listOf(
                BackupCoinLedgerEntry(
                    id = "f0c81029-bb55-480d-ac85-8a1c15888718",
                    amount = 99,
                    type = "migration",
                    note = "legacy balance",
                    createdAt = "2026-07-01T00:00:00Z",
                ),
                BackupCoinLedgerEntry(
                    id = "9c5299cb-fc31-4ac4-a5a8-f432097ea2e7",
                    amount = -30,
                    type = "shopPurchase",
                    note = "theme_6",
                    createdAt = "2026-07-19T08:10:00Z",
                ),
            ),
            shopPurchaseRecords = listOf(
                BackupShopPurchaseRecord(
                    id = "eb414e52-da2f-481e-a472-d496c378d95f",
                    productType = "theme",
                    productID = "theme_6",
                    productName = "Theme 6",
                    quantity = 1,
                    unitCoinCost = 30,
                    totalCoinCost = 30,
                    currency = "coins",
                    status = "completed",
                    source = "shop",
                    purchasedAt = "2026-07-19T08:10:00Z",
                    syncedAt = "2026-07-19T08:11:00Z",
                ),
            ),
            lastAuthProvider = "google",
        )

        val decoded = codec.decodeAndValidate(codec.encode(payload))

        assertEquals(payload, decoded)
        assertEquals(BackupPayload.CURRENT_SCHEMA_VERSION, decoded.schemaVersion)
    }

    @Test
    fun `missing legacy and additive fields receive safe defaults`() {
        val legacyJson =
            """
            {
              "schemaVersion": 1,
              "exportDate": "2025-01-01T00:00:00Z",
              "appSettings": [{"companyName": "Old user"}],
              "stampRecords": [{"reason": "legacy stamp"}],
              "resignationPlans": [{}]
            }
            """.trimIndent()

        val decoded = codec.decodeAndValidate(legacyJson)

        assertEquals(1, decoded.schemaVersion)
        assertEquals(10, decoded.appSettings.single().maxStamps)
        assertFalse(decoded.appSettings.single().hasMigratedToLedger)
        assertTrue(decoded.coinLedgerEntries.isEmpty())
        assertTrue(decoded.shopPurchaseRecords.isEmpty())
        assertEquals(null, decoded.lastAuthProvider)
        assertEquals(0, decoded.niuMaConfig.payMode)
        assertEquals(0, decoded.niuMaConfig.hourlyTrackingMode)
        assertNotEquals("", decoded.stampRecords.single().id)
        assertEquals(BackupDateCodec.EPOCH_ISO, decoded.stampRecords.single().date)
    }

    @Test
    fun `iOS ISO dates parse and Android export omits fractional seconds`() {
        assertEquals(1_752_912_000_000L, BackupDateCodec.parse("2025-07-19T08:00:00Z"))
        assertEquals("2025-07-19T08:00:00Z", BackupDateCodec.format(1_752_912_000_987L))
    }

    @Test
    fun `future schema is rejected before import`() {
        val exception = expectThrows<BackupValidationException> {
            codec.decodeAndValidate(
                """{"schemaVersion":5,"exportDate":"2026-07-19T00:00:00Z"}""",
            )
        }

        assertTrue(exception.issues.any { it.path == "schemaVersion" })
    }

    @Test
    fun `duplicate ids and invalid dates are rejected`() {
        val exception = expectThrows<BackupValidationException> {
            codec.decodeAndValidate(
                """
                {
                  "schemaVersion": 3,
                  "exportDate": "not-a-date",
                  "stampRecords": [
                    {"id":"54157ccb-9cb8-4a9a-a8b7-95d494f5bda8"},
                    {"id":"54157ccb-9cb8-4a9a-a8b7-95d494f5bda8"}
                  ]
                }
                """.trimIndent(),
            )
        }

        assertTrue(exception.issues.any { it.path == "exportDate" })
        assertTrue(exception.issues.any { it.path == "stampRecords" })
    }

    @Test
    fun `non UUID record id is rejected instead of silently rewritten`() {
        val exception = expectThrows<BackupValidationException> {
            codec.decodeAndValidate(
                """
                {
                  "schemaVersion": 3,
                  "exportDate": "2026-07-19T00:00:00Z",
                  "stampRecords": [{"id":"not-a-uuid"}]
                }
                """.trimIndent(),
            )
        }

        assertTrue(exception.issues.any { it.path == "stampRecords[0].id" })
    }

    @Test
    fun `backup requires exactly one settings and one plan record`() {
        val missing = expectThrows<BackupValidationException> {
            codec.decodeAndValidate(
                """{"schemaVersion":3,"exportDate":"2026-07-19T00:00:00Z"}""",
            )
        }
        assertTrue(missing.issues.any { it.path == "appSettings" })
        assertTrue(missing.issues.any { it.path == "resignationPlans" })

        val duplicate = expectThrows<BackupValidationException> {
            codec.decodeAndValidate(
                """
                {
                  "schemaVersion":3,
                  "exportDate":"2026-07-19T00:00:00Z",
                  "appSettings":[{},{}],
                  "resignationPlans":[{}]
                }
                """.trimIndent(),
            )
        }
        assertTrue(duplicate.issues.any { it.path == "appSettings" })
    }

    @Test
    fun `negative or overflowing ledger balance is rejected before import`() {
        fun json(amounts: String) =
            """
            {
              "schemaVersion":3,
              "exportDate":"2026-07-19T00:00:00Z",
              "appSettings":[{}],
              "resignationPlans":[{}],
              "coinLedgerEntries":[$amounts]
            }
            """.trimIndent()

        val negative = expectThrows<BackupValidationException> {
            codec.decodeAndValidate(
                json(
                    """{"id":"00000000-0000-0000-0000-000000000001","amount":-1}""",
                ),
            )
        }
        assertTrue(negative.issues.any { it.path == "coinLedgerEntries" })

        val overflow = expectThrows<BackupValidationException> {
            codec.decodeAndValidate(
                json(
                    """
                    {"id":"00000000-0000-0000-0000-000000000001","amount":2147483647},
                    {"id":"00000000-0000-0000-0000-000000000002","amount":1}
                    """.trimIndent(),
                ),
            )
        }
        assertTrue(overflow.issues.any { it.path == "coinLedgerEntries" })
    }

    @Test
    fun `multiple iOS migration entries count only the earliest one`() {
        val decoded = codec.decodeAndValidate(
            """
            {
              "schemaVersion":3,
              "exportDate":"2026-07-19T00:00:00Z",
              "appSettings":[{}],
              "resignationPlans":[{}],
              "coinLedgerEntries":[
                {"id":"00000000-0000-0000-0000-000000000001","amount":2147483647,"type":"migration","createdAt":"2026-07-20T00:00:00Z"},
                {"id":"00000000-0000-0000-0000-000000000002","amount":3,"type":"migration","createdAt":"2026-07-19T00:00:00Z"},
                {"id":"00000000-0000-0000-0000-000000000003","amount":2,"type":"dailyStamp","createdAt":"2026-07-21T00:00:00Z"}
              ]
            }
            """.trimIndent(),
        )

        assertEquals(2, decoded.coinLedgerEntries.count { it.type == "migration" })
    }

    @Test
    fun `iOS due date without a scheduled notification remains disabled`() {
        val source = BackupTodoItem(
            id = "00000000-0000-0000-0000-000000000010",
            title = "只保留到期日",
            dueDate = "2026-08-01T09:00:00Z",
            notificationScheduled = false,
        )

        val domain = source.toDomain()
        val roundTripped = domain.toBackup()

        assertEquals(BackupDateCodec.parse(requireNotNull(source.dueDate)), domain.reminderAt)
        assertFalse(domain.notificationScheduled)
        assertEquals(source.dueDate, roundTripped.dueDate)
        assertFalse(roundTripped.notificationScheduled)
    }

    private inline fun <reified T : Throwable> expectThrows(block: () -> Unit): T {
        try {
            block()
            fail("Expected ${T::class.java.simpleName}")
        } catch (error: Throwable) {
            if (error is T) return error
            throw error
        }
        throw AssertionError("unreachable")
    }
}
