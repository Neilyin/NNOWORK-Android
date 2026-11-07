package com.neilyin.nnowork.data.coin

import androidx.room.withTransaction
import com.neilyin.nnowork.data.local.AppDatabase
import com.neilyin.nnowork.data.local.StampRecordEntity
import com.neilyin.nnowork.data.reward.RewardEvent
import com.neilyin.nnowork.data.reward.RewardKind
import com.neilyin.nnowork.data.settings.SettingsRepository
import com.neilyin.nnowork.data.shop.ShopPurchaseEntity
import com.neilyin.nnowork.util.DateUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.nio.charset.StandardCharsets
import java.util.UUID

/**
 * 金幣的唯一讀寫入口。餘額永遠由 Room ledger 折疊，AppSettings.coinCount 只是 UI 鏡像。
 */
class CoinLedgerRepository(
    private val database: AppDatabase,
    private val settingsRepository: SettingsRepository,
    private val nowMillis: () -> Long = System::currentTimeMillis,
) {
    private val dao = database.coinLedgerDao()
    private val operationMutex = Mutex()

    val balance: Flow<Int> = dao.observeAll().map(CoinLedgerPolicy::balance)

    /** App 啟動時執行；也可安全重複呼叫以修復 settings 鏡像。 */
    suspend fun initialize(): Int = operationMutex.withLock { migrateAndMirrorLocked() }

    suspend fun currentBalance(): Int = operationMutex.withLock { migrateAndMirrorLocked() }

    /** 版本補償以 campaignId 去重，同一活動在任何啟動次數下都只會入帳一次。 */
    suspend fun grantUpdateCompensation(campaignId: String, amount: Int): Int {
        require(campaignId.isNotBlank()) { "Campaign id must not be blank" }
        require(amount > 0) { "Compensation amount must be positive" }
        return operationMutex.withLock {
            migrateAndMirrorLocked()
            val newBalance = database.withTransaction {
                dao.appendIfAbsent(
                    CoinLedgerEntryEntity(
                        id = stableUuid("updateCompensation|$campaignId"),
                        amount = amount,
                        type = CoinTransactionType.UPDATE_COMPENSATION.rawValue,
                        note = "版本 $campaignId 補償",
                        createdAt = nowMillis(),
                    ),
                )
                CoinLedgerPolicy.balance(dao.getAll())
            }
            mirrorBestEffort(newBalance)
            newBalance
        }
    }

    suspend fun credit(
        amount: Int,
        type: CoinTransactionType,
        note: String = "",
        id: String = UUID.randomUUID().toString(),
        createdAt: Long = nowMillis(),
    ): Int {
        require(amount > 0) { "Credit amount must be positive" }
        return operationMutex.withLock {
            migrateAndMirrorLocked()
            val newBalance = database.withTransaction {
                dao.append(
                    CoinLedgerEntryEntity(
                        id = id,
                        amount = amount,
                        type = type.rawValue,
                        note = note,
                        createdAt = createdAt,
                    ),
                )
                CoinLedgerPolicy.balance(dao.getAll())
            }
            // Ledger 已提交就是事實；鏡像失敗不能讓呼叫端重試而重複入帳。
            mirrorBestEffort(newBalance)
            newBalance
        }
    }

    /**
     * 獎勵事件入帳（§18.3）：流水帳 id = 事件 UUID，Room `INSERT OR IGNORE`。
     * 重播同一事件只會入帳一次；回傳 true 代表這次真的新增。
     */
    suspend fun creditRewardEventIfAbsent(event: RewardEvent): Boolean {
        val entry = RewardLedgerEntries.entryFor(event, nowMillis())
        return operationMutex.withLock {
            migrateAndMirrorLocked()
            val (inserted, newBalance) = database.withTransaction {
                val wasInserted = dao.appendIfAbsent(entry) != -1L
                wasInserted to CoinLedgerPolicy.balance(dao.getAll())
            }
            mirrorBestEffort(newBalance)
            inserted
        }
    }

    /** 在同一個 Room transaction 內重算餘額、檢查與落支出帳。 */
    suspend fun spend(
        amount: Int,
        type: CoinTransactionType,
        note: String = "",
        id: String = UUID.randomUUID().toString(),
        createdAt: Long = nowMillis(),
    ): Boolean {
        require(amount > 0) { "Spend amount must be positive" }
        return operationMutex.withLock {
            migrateAndMirrorLocked()
            val result = database.withTransaction {
                val current = CoinLedgerPolicy.balance(dao.getAll())
                if (!CoinLedgerPolicy.canSpend(current, amount)) return@withTransaction null
                dao.append(
                    CoinLedgerEntryEntity(
                        id = id,
                        amount = -amount,
                        type = type.rawValue,
                        note = note,
                        createdAt = createdAt,
                    ),
                )
                CoinLedgerPolicy.balance(dao.getAll())
            }
            result?.let { mirrorBestEffort(it) }
            result != null
        }
    }

    /**
     * 蓋章與每日 +1 在同一個 Room transaction 提交。eligibility 只看 stable daily entry id，
     * 不信任 DataStore 鏡像；Room 完成後才 best-effort 更新 lastCoinEarnedDate。
     */
    suspend fun insertStampAndRewardDaily(record: StampRecordEntity): Boolean = operationMutex.withLock {
        migrateAndMirrorLocked()
        val dayStart = DateUtils.startOfDay(record.date)
        val (inserted, newBalance) = database.withTransaction {
            database.stampDao().insert(record)
            val wasInserted = dao.appendIfAbsent(
                CoinLedgerEntryEntity(
                    id = stableUuid("dailyStamp|$dayStart"),
                    amount = 1,
                    type = CoinTransactionType.DAILY_STAMP.rawValue,
                    note = "每日蓋章",
                    createdAt = record.date,
                ),
            ) != -1L
            wasInserted to CoinLedgerPolicy.balance(dao.getAll())
        }
        runCatching {
            settingsRepository.update { current ->
                current.copy(
                    coinCount = newBalance,
                    hasMigratedToLedger = true,
                    lastCoinEarnedDate = dayStart,
                )
            }
        }
        inserted
    }

    /**
     * 主題支出先在 Room transaction 檢查餘額並提交，再寫 DataStore 權益。
     * ledger note 保留 theme id，若在兩步之間 process death，下次 initialize 會自癒解鎖。
     */
    suspend fun purchaseTheme(
        themeId: Int,
        productId: String,
        themeName: String,
        coinCost: Int,
    ): ThemePurchaseResult {
        require(themeId > 0) { "Theme id must be positive" }
        require(productId.isNotBlank()) { "Product id must not be blank" }
        require(coinCost > 0) { "Theme coin cost must be positive" }

        return operationMutex.withLock {
            migrateAndMirrorLocked()
            if (themeId in settingsRepository.current().unlockedStyles) {
                return@withLock ThemePurchaseResult.ALREADY_OWNED
            }

            val purchasedAt = nowMillis()
            val newBalance = database.withTransaction {
                val currentBalance = CoinLedgerPolicy.balance(dao.getAll())
                if (!CoinLedgerPolicy.canSpend(currentBalance, coinCost)) return@withTransaction null
                dao.append(
                    CoinLedgerEntryEntity(
                        id = stableUuid("themeUnlock|$themeId"),
                        amount = -coinCost,
                        type = CoinTransactionType.THEME_UNLOCK.rawValue,
                        note = themeNote(themeId, themeName),
                        createdAt = purchasedAt,
                    ),
                )
                database.shopPurchaseDao().insert(
                    ShopPurchaseEntity(
                        productType = "theme",
                        productId = productId,
                        productName = themeName,
                        unitCoinCost = coinCost,
                        totalCoinCost = coinCost,
                        purchasedAt = purchasedAt,
                    ),
                )
                CoinLedgerPolicy.balance(dao.getAll())
            } ?: return@withLock ThemePurchaseResult.INSUFFICIENT_FUNDS

            runCatching {
                settingsRepository.update { current ->
                    current.copy(
                        coinCount = newBalance,
                        hasMigratedToLedger = true,
                        unlockedStyles = (current.unlockedStyles + themeId).distinct(),
                        cardStyle = themeId,
                    )
                }
            }
            ThemePurchaseResult.PURCHASED
        }
    }

    /** 僅供使用者明確的「全部重置」。 */
    suspend fun reset() = operationMutex.withLock {
        // 先清鏡像再清 ledger；若中途 process death，最壞只是重置未完成，不會把已清 ledger 從舊鏡像復活。
        settingsRepository.update { current ->
            current.copy(coinCount = 0, hasMigratedToLedger = true)
        }
        database.withTransaction {
            dao.deleteAllForFullReplacement()
            database.shopPurchaseDao().deleteAllForFullReplacement()
        }
    }

    private suspend fun migrateAndMirrorLocked(): Int {
        val settings = settingsRepository.current()
        val (balance, entries) = database.withTransaction {
            // 舊 Debug 測試版曾寫入一次性測試金幣。先移除並跳過 legacy 鏡像遷移，
            // 避免 DataStore 裡的測試餘額被當成真實舊制金幣重新入帳。
            val removedDebugEntries = dao.deleteByType(CoinTransactionType.DEBUG_SEED.rawValue)
            val existingEntries = dao.getAll()
            if (removedDebugEntries == 0) {
                CoinLedgerPolicy.legacyMigrationAmount(
                    ledgerIsEmpty = existingEntries.isEmpty(),
                    legacyCoinCount = settings.coinCount,
                )?.let { legacyBalance ->
                    dao.append(
                        CoinLedgerEntryEntity(
                            id = stableUuid("legacy-coin-migration"),
                            amount = legacyBalance,
                            type = CoinTransactionType.MIGRATION.rawValue,
                            note = "舊制 coinCount 一次性遷移",
                            createdAt = nowMillis(),
                        ),
                    )
                }
            }
            val updatedEntries = dao.getAll()
            CoinLedgerPolicy.balance(updatedEntries) to updatedEntries
        }
        mirrorLedgerStateBestEffort(balance, entries)
        return balance
    }

    private suspend fun mirrorBestEffort(balance: Int) {
        runCatching {
            settingsRepository.update { current ->
                if (current.coinCount == balance && current.hasMigratedToLedger) current
                else current.copy(coinCount = balance, hasMigratedToLedger = true)
            }
        }
    }

    private suspend fun mirrorLedgerStateBestEffort(
        balance: Int,
        entries: List<CoinLedgerEntryEntity>,
    ) {
        val purchasedThemeIds = entries
            .asSequence()
            .filter { it.type == CoinTransactionType.THEME_UNLOCK.rawValue }
            .mapNotNull { parseThemeId(it.note) }
            .toList()
        runCatching {
            settingsRepository.update { current ->
                val missingIds = purchasedThemeIds.filterNot(current.unlockedStyles::contains)
                current.copy(
                    coinCount = balance,
                    hasMigratedToLedger = true,
                    unlockedStyles = (current.unlockedStyles + missingIds).distinct(),
                    cardStyle = missingIds.lastOrNull() ?: current.cardStyle,
                )
            }
        }
    }

    private fun themeNote(themeId: Int, themeName: String): String =
        "$ANDROID_THEME_NOTE_PREFIX$themeId:$themeName"

    private fun parseThemeId(note: String): Int? = note
        .takeIf { it.startsWith(ANDROID_THEME_NOTE_PREFIX) }
        ?.substringAfter(ANDROID_THEME_NOTE_PREFIX)
        ?.substringBefore(':')
        ?.toIntOrNull()

    private fun stableUuid(seed: String): String = UUID
        .nameUUIDFromBytes(seed.toByteArray(StandardCharsets.UTF_8))
        .toString()

    private companion object {
        const val ANDROID_THEME_NOTE_PREFIX = "androidTheme:"
    }
}

/** 獎勵事件 → 流水帳 entry 的唯一對照（純函式，供測試鎖定冪等鍵）。 */
object RewardLedgerEntries {
    fun entryFor(event: RewardEvent, createdAt: Long): CoinLedgerEntryEntity {
        require(event.amount > 0) { "Reward amount must be positive" }
        return CoinLedgerEntryEntity(
            id = event.id,
            amount = event.amount,
            type = event.kind.transactionType.rawValue,
            note = when (event.kind) {
                RewardKind.TREE_HOLE_POST_APPROVED -> "樹洞貼文審核通過"
                RewardKind.FEEDBACK_REWARD -> "意見回饋獲獎"
            },
            createdAt = createdAt,
        )
    }
}

enum class ThemePurchaseResult {
    PURCHASED,
    ALREADY_OWNED,
    INSUFFICIENT_FUNDS,
}
