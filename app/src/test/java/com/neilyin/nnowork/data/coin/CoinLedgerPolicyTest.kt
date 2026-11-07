package com.neilyin.nnowork.data.coin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class CoinLedgerPolicyTest {
    @Test
    fun `balance folds non migration entries and only the earliest migration`() {
        val entries = listOf(
            entry("00000000-0000-0000-0000-000000000001", 12, CoinTransactionType.MIGRATION, createdAt = 20),
            entry("00000000-0000-0000-0000-000000000002", 3, CoinTransactionType.MIGRATION, createdAt = 10),
            entry("00000000-0000-0000-0000-000000000003", 5, CoinTransactionType.TREE_HOLE_POST_APPROVED),
            entry("00000000-0000-0000-0000-000000000004", -4, CoinTransactionType.THEME_UNLOCK),
        )

        assertEquals(4, CoinLedgerPolicy.balance(entries))
    }

    @Test
    fun `negative and overflowing balances are rejected`() {
        expectThrows<IllegalArgumentException> {
            CoinLedgerPolicy.balance(
                listOf(entry("00000000-0000-0000-0000-000000000001", -1, CoinTransactionType.THEME_UNLOCK)),
            )
        }
        expectThrows<IllegalArgumentException> {
            CoinLedgerPolicy.balance(
                listOf(
                    entry("00000000-0000-0000-0000-000000000001", Int.MAX_VALUE, CoinTransactionType.MIGRATION),
                    entry("00000000-0000-0000-0000-000000000002", 1, CoinTransactionType.DAILY_STAMP),
                ),
            )
        }
    }

    @Test
    fun `spend requires a positive affordable cost`() {
        assertTrue(CoinLedgerPolicy.canSpend(balance = 10, cost = 10))
        assertFalse(CoinLedgerPolicy.canSpend(balance = 9, cost = 10))
        assertFalse(CoinLedgerPolicy.canSpend(balance = 10, cost = 0))
    }

    @Test
    fun `legacy balance seeds only when ledger is completely empty`() {
        assertEquals(8, CoinLedgerPolicy.legacyMigrationAmount(ledgerIsEmpty = true, legacyCoinCount = 8))
        assertNull(CoinLedgerPolicy.legacyMigrationAmount(ledgerIsEmpty = false, legacyCoinCount = 8))
        assertNull(CoinLedgerPolicy.legacyMigrationAmount(ledgerIsEmpty = true, legacyCoinCount = 0))
    }

    private fun entry(
        id: String,
        amount: Int,
        type: CoinTransactionType,
        createdAt: Long = 0,
    ) = CoinLedgerEntryEntity(
        id = id,
        amount = amount,
        type = type.rawValue,
        createdAt = createdAt,
    )

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
