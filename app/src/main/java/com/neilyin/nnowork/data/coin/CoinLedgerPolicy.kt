package com.neilyin.nnowork.data.coin

/** 無 Android 依賴的流水帳折疊與規則，供 JVM 測試與 Repository 共用。 */
object CoinLedgerPolicy {
    /**
     * 與 iOS CoinWallet 一致：一般流水全部加總，migration 只計 createdAt 最早一筆。
     * CloudKit 雙裝置競態可能合法產生多筆 migration，全加會讓跨平台金幣膨脹。
     */
    fun balance(entries: List<CoinLedgerEntryEntity>): Int {
        var total = 0L
        entries
            .filter { it.type != CoinTransactionType.MIGRATION.rawValue }
            .forEach { entry -> total = Math.addExact(total, entry.amount.toLong()) }
        entries
            .filter { it.type == CoinTransactionType.MIGRATION.rawValue }
            .minByOrNull(CoinLedgerEntryEntity::createdAt)
            ?.let { migration -> total = Math.addExact(total, migration.amount.toLong()) }

        require(total in 0L..Int.MAX_VALUE.toLong()) {
            "Coin balance must stay within 0..${Int.MAX_VALUE}"
        }
        return total.toInt()
    }

    fun canSpend(balance: Int, cost: Int): Boolean = cost > 0 && balance >= cost

    fun legacyMigrationAmount(
        ledgerIsEmpty: Boolean,
        legacyCoinCount: Int,
    ): Int? = legacyCoinCount.takeIf {
        ledgerIsEmpty && it > 0
    }
}
