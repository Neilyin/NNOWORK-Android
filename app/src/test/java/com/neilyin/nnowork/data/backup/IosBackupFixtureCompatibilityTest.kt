package com.neilyin.nnowork.data.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

/**
 * 以 IOS_BACKUP_FIXTURE 指定真實 iOS JSON 時執行跨平台相容測試。
 * 備份檔只在原位讀取，不會複製到專案或印出私人內容。
 */
class IosBackupFixtureCompatibilityTest {
    @Test
    fun `real iOS backup decodes and survives an Android v3 migration round trip`() {
        val fixturePath = System.getenv("IOS_BACKUP_FIXTURE")
        assumeTrue("Set IOS_BACKUP_FIXTURE to run the real iOS fixture test", !fixturePath.isNullOrBlank())

        val fixture = File(requireNotNull(fixturePath))
        assertTrue("iOS backup fixture must be a regular file", fixture.isFile)
        assertTrue("iOS backup fixture exceeds the supported size", fixture.length() <= MAX_FIXTURE_BYTES)

        val codec = BackupCodec()
        val decoded = codec.decodeAndValidate(fixture.readText(Charsets.UTF_8))

        assertTrue(decoded.schemaVersion in 1..BackupPayload.CURRENT_SCHEMA_VERSION)
        assertEquals(1, decoded.appSettings.size)
        assertEquals(1, decoded.resignationPlans.size)

        val migratedLedger = decoded.ledgerEntriesForImport()
        val legacyCoinCount = decoded.appSettings.single().coinCount
        if (decoded.schemaVersion < 2 && legacyCoinCount > 0) {
            assertEquals(1, migratedLedger.size)
            assertEquals(legacyCoinCount, migratedLedger.single().amount)
            assertEquals("migration", migratedLedger.single().type)
        }

        val upgradedPayload = decoded.copy(
            schemaVersion = BackupPayload.CURRENT_SCHEMA_VERSION,
            appSettings = decoded.appSettings.map {
                it.copy(hasMigratedToLedger = true, coinCount = legacyCoinCount)
            },
            coinLedgerEntries = migratedLedger,
        )
        val upgraded = codec.decodeAndValidate(codec.encode(upgradedPayload))
        assertEquals(upgradedPayload, upgraded)
    }

    private companion object {
        const val MAX_FIXTURE_BYTES = 16L * 1_048_576L
    }
}
