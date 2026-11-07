package com.neilyin.nnowork.data.backup

import com.squareup.moshi.JsonClass
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

/**
 * 備份匯入的 write-ahead journal。
 *
 * 只保存舊/新 DataStore 主要欄位與 Room 內容指紋，不複製整份資料庫。
 * Room transaction 若因 process death 回滾，下次啟動可以由目前指紋判斷應還原舊
 * preferences，或保留已成功提交的新 preferences。
 */
@JsonClass(generateAdapter = false)
internal data class BackupImportJournal(
    val version: Int = CURRENT_VERSION,
    val token: String,
    val oldPreferences: BackupPreferenceSnapshot,
    val newPreferences: BackupPreferenceSnapshot,
    val oldDatabaseFingerprint: String,
    val newDatabaseFingerprint: String,
) {
    companion object {
        const val CURRENT_VERSION = 2
    }
}

@JsonClass(generateAdapter = false)
internal data class BackupPreferenceSnapshot(
    val settingsJson: String? = null,
    val planJson: String? = null,
    val lastAuthProvider: String? = null,
    val deferredCollectionsJson: String? = null,
)

internal data class BackupFingerprintStamp(
    val id: String,
    val date: Long,
    val reason: String,
    val isEasterEgg: Boolean,
    val roundNumber: Int,
)

internal data class BackupFingerprintLedgerEntry(
    val id: String,
    val amount: Int,
    val type: String,
    val note: String,
    val createdAt: Long,
)

/** 與資料列順序無關的 Room 內容指紋，可在純 JVM 測試。 */
internal object BackupDatabaseFingerprint {
    fun compute(
        stamps: List<BackupFingerprintStamp>,
        ledgerEntries: List<BackupFingerprintLedgerEntry>,
    ): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.putString("nnowork-backup-db-fingerprint-v1")

        val orderedStamps = stamps.sortedBy(BackupFingerprintStamp::id)
        digest.putInt(orderedStamps.size)
        orderedStamps.forEach { stamp ->
            digest.putString(stamp.id)
            digest.putLong(stamp.date)
            digest.putString(stamp.reason)
            digest.putBoolean(stamp.isEasterEgg)
            digest.putInt(stamp.roundNumber)
        }

        val orderedLedger = ledgerEntries.sortedBy(BackupFingerprintLedgerEntry::id)
        digest.putInt(orderedLedger.size)
        orderedLedger.forEach { entry ->
            digest.putString(entry.id)
            digest.putInt(entry.amount)
            digest.putString(entry.type)
            digest.putString(entry.note)
            digest.putLong(entry.createdAt)
        }

        return digest.digest().joinToString(separator = "") { byte -> "%02x".format(byte) }
    }

    private fun MessageDigest.putBoolean(value: Boolean) {
        update((if (value) 1 else 0).toByte())
    }

    private fun MessageDigest.putInt(value: Int) {
        update((value ushr 24).toByte())
        update((value ushr 16).toByte())
        update((value ushr 8).toByte())
        update(value.toByte())
    }

    private fun MessageDigest.putLong(value: Long) {
        update((value ushr 56).toByte())
        update((value ushr 48).toByte())
        update((value ushr 40).toByte())
        update((value ushr 32).toByte())
        update((value ushr 24).toByte())
        update((value ushr 16).toByte())
        update((value ushr 8).toByte())
        update(value.toByte())
    }

    private fun MessageDigest.putString(value: String) {
        val bytes = value.toByteArray(StandardCharsets.UTF_8)
        putInt(bytes.size)
        update(bytes)
    }
}

internal enum class BackupImportRecoveryDecision {
    APPLY_OLD_PREFERENCES,
    APPLY_NEW_PREFERENCES,
    PRESERVE_CURRENT_PREFERENCES,
    UNRESOLVED,
}

/**
 * Room marker 與 stamps/ledger 同 transaction，因此不會被 commit 後的新蓋章或金幣
 * 流水影響。DataStore token 仍存在時代表新 preferences 已寫入；直接保留
 * 當前值，才不會覆蓋 commit 後緊接發生的合法設定更新。
 */
internal object BackupImportCommitPolicy {
    fun decide(
        committedDatabaseToken: String?,
        dataStoreToken: String?,
        journalToken: String,
    ): BackupImportRecoveryDecision = when {
        committedDatabaseToken != journalToken -> BackupImportRecoveryDecision.APPLY_OLD_PREFERENCES
        dataStoreToken == journalToken -> BackupImportRecoveryDecision.PRESERVE_CURRENT_PREFERENCES
        else -> BackupImportRecoveryDecision.APPLY_NEW_PREFERENCES
    }
}

/**
 * Journal 已持久化就代表使用者已確認匯入。當舊/新 DB 指紋相同時，
 * 優先 roll forward 到新 preferences，可完成只改設定、Room 內容未變的匯入。
 */
internal object BackupImportRecoveryPolicy {
    fun decide(
        currentDatabaseFingerprint: String,
        oldDatabaseFingerprint: String,
        newDatabaseFingerprint: String,
    ): BackupImportRecoveryDecision = when (currentDatabaseFingerprint) {
        newDatabaseFingerprint -> BackupImportRecoveryDecision.APPLY_NEW_PREFERENCES
        oldDatabaseFingerprint -> BackupImportRecoveryDecision.APPLY_OLD_PREFERENCES
        else -> BackupImportRecoveryDecision.UNRESOLVED
    }
}
