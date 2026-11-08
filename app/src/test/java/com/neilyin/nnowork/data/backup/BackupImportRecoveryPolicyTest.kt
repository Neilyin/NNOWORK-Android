package com.neilyin.nnowork.data.backup

import com.neilyin.nnowork.data.JsonProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class BackupImportRecoveryPolicyTest {
    @Test
    fun `journal preserves complete nullable old and new preference snapshots`() {
        val adapter = JsonProvider.adapter<BackupImportJournal>()
        val journal = BackupImportJournal(
            token = "import-token",
            oldPreferences = BackupPreferenceSnapshot(),
            newPreferences = BackupPreferenceSnapshot(
                settingsJson = "{\"companyName\":\"new\"}",
                planJson = "{\"targetAmount\":100}",
                lastAuthProvider = "google",
                deferredCollectionsJson = "{\"workDays\":[2,3,4,5,6]}",
            ),
            oldDatabaseFingerprint = "old",
            newDatabaseFingerprint = "new",
        )

        assertEquals(journal, adapter.fromJson(adapter.toJson(journal)))
    }

    @Test
    fun `database fingerprint is stable across query order`() {
        val stamps = listOf(
            stamp("00000000-0000-0000-0000-000000000002", date = 20L),
            stamp("00000000-0000-0000-0000-000000000001", date = 10L),
        )
        val ledger = listOf(
            ledger("00000000-0000-0000-0000-000000000004", amount = -1),
            ledger("00000000-0000-0000-0000-000000000003", amount = 5),
        )

        assertEquals(
            BackupDatabaseFingerprint.compute(stamps, ledger),
            BackupDatabaseFingerprint.compute(stamps.reversed(), ledger.reversed()),
        )
    }

    @Test
    fun `database fingerprint covers every persisted field`() {
        val baseStamp = stamp("00000000-0000-0000-0000-000000000001", date = 10L)
        val baseLedger = ledger("00000000-0000-0000-0000-000000000002", amount = 5)
        val baseline = BackupDatabaseFingerprint.compute(listOf(baseStamp), listOf(baseLedger))

        val mutations = listOf(
            BackupDatabaseFingerprint.compute(listOf(baseStamp.copy(date = 11L)), listOf(baseLedger)),
            BackupDatabaseFingerprint.compute(listOf(baseStamp.copy(reason = "changed")), listOf(baseLedger)),
            BackupDatabaseFingerprint.compute(listOf(baseStamp.copy(isEasterEgg = true)), listOf(baseLedger)),
            BackupDatabaseFingerprint.compute(listOf(baseStamp.copy(roundNumber = 2)), listOf(baseLedger)),
            BackupDatabaseFingerprint.compute(listOf(baseStamp), listOf(baseLedger.copy(amount = 6))),
            BackupDatabaseFingerprint.compute(listOf(baseStamp), listOf(baseLedger.copy(type = "changed"))),
            BackupDatabaseFingerprint.compute(listOf(baseStamp), listOf(baseLedger.copy(note = "changed"))),
            BackupDatabaseFingerprint.compute(listOf(baseStamp), listOf(baseLedger.copy(createdAt = 2L))),
        )

        mutations.forEach { fingerprint -> assertNotEquals(baseline, fingerprint) }
    }

    @Test
    fun `recovery selects preferences matching committed database side`() {
        assertEquals(
            BackupImportRecoveryDecision.APPLY_OLD_PREFERENCES,
            BackupImportRecoveryPolicy.decide(
                currentDatabaseFingerprint = "old",
                oldDatabaseFingerprint = "old",
                newDatabaseFingerprint = "new",
            ),
        )
        assertEquals(
            BackupImportRecoveryDecision.APPLY_NEW_PREFERENCES,
            BackupImportRecoveryPolicy.decide(
                currentDatabaseFingerprint = "new",
                oldDatabaseFingerprint = "old",
                newDatabaseFingerprint = "new",
            ),
        )
        assertEquals(
            BackupImportRecoveryDecision.UNRESOLVED,
            BackupImportRecoveryPolicy.decide(
                currentDatabaseFingerprint = "other",
                oldDatabaseFingerprint = "old",
                newDatabaseFingerprint = "new",
            ),
        )
    }

    @Test
    fun `identical old and new database rolls forward confirmed import`() {
        assertEquals(
            BackupImportRecoveryDecision.APPLY_NEW_PREFERENCES,
            BackupImportRecoveryPolicy.decide(
                currentDatabaseFingerprint = "same",
                oldDatabaseFingerprint = "same",
                newDatabaseFingerprint = "same",
            ),
        )
    }

    @Test
    fun `transaction marker decides commit even after later database writes`() {
        assertEquals(
            BackupImportRecoveryDecision.PRESERVE_CURRENT_PREFERENCES,
            BackupImportCommitPolicy.decide(
                committedDatabaseToken = "import-2",
                dataStoreToken = "import-2",
                journalToken = "import-2",
            ),
        )
        assertEquals(
            BackupImportRecoveryDecision.APPLY_NEW_PREFERENCES,
            BackupImportCommitPolicy.decide(
                committedDatabaseToken = "import-2",
                dataStoreToken = null,
                journalToken = "import-2",
            ),
        )
    }

    @Test
    fun `missing transaction marker rolls preferences back`() {
        assertEquals(
            BackupImportRecoveryDecision.APPLY_OLD_PREFERENCES,
            BackupImportCommitPolicy.decide(
                committedDatabaseToken = "older-import",
                dataStoreToken = "import-2",
                journalToken = "import-2",
            ),
        )
    }

    private fun stamp(id: String, date: Long) = BackupFingerprintStamp(
        id = id,
        date = date,
        reason = "reason",
        isEasterEgg = false,
        roundNumber = 1,
    )

    private fun ledger(id: String, amount: Int) = BackupFingerprintLedgerEntry(
        id = id,
        amount = amount,
        type = "migration",
        note = "note",
        createdAt = 1L,
    )
}
