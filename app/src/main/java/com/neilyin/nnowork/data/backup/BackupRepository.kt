package com.neilyin.nnowork.data.backup

import android.content.Context
import android.util.AtomicFile
import android.util.Log
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.room.withTransaction
import com.neilyin.nnowork.data.JsonProvider
import com.neilyin.nnowork.data.coin.CoinLedgerEntryEntity
import com.neilyin.nnowork.data.coin.CoinLedgerPolicy
import com.neilyin.nnowork.data.coin.CoinLedgerRepository
import com.neilyin.nnowork.data.datastore.appDataStore
import com.neilyin.nnowork.data.jobarchive.JobArchiveEntity
import com.neilyin.nnowork.data.jobarchive.toEntity
import com.neilyin.nnowork.data.local.AppDatabase
import com.neilyin.nnowork.data.local.StampRecordEntity
import com.neilyin.nnowork.data.plan.ManualSalaryTimer
import com.neilyin.nnowork.data.plan.HourlyTrackingMode
import com.neilyin.nnowork.data.plan.ResignationPlan
import com.neilyin.nnowork.data.plan.Shift
import com.neilyin.nnowork.data.plan.TodoItem
import com.neilyin.nnowork.data.settings.AppSettings
import com.neilyin.nnowork.data.settings.SettingsRepository
import com.neilyin.nnowork.data.shop.ShopPurchaseEntity
import com.squareup.moshi.JsonClass
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileNotFoundException
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

/**
 * 整合 Room 蓋章資料與 DataStore 設定的備份存取層。
 *
 * 匯入是「全刪全換」：一定先完成 JSON decode + validate + domain mapping，
 * 再進入 Room transaction。DataStore 兩份主資料會在同一次 edit 原子替換；
 * write-ahead journal 會在任何覆蓋前落盤，依 Room 舊/新指紋復原同一版 preferences。
 */
class BackupRepository(
    context: Context,
    private val database: AppDatabase = AppDatabase.get(context.applicationContext),
    private val coinLedgerRepository: CoinLedgerRepository = CoinLedgerRepository(
        database,
        SettingsRepository(context.applicationContext),
    ),
    private val codec: BackupCodec = BackupCodec(),
    private val nowMillis: () -> Long = System::currentTimeMillis,
) {
    private val appContext = context.applicationContext
    private val stampDao = database.stampDao()
    private val coinLedgerDao = database.coinLedgerDao()
    private val shopPurchaseDao = database.shopPurchaseDao()
    private val jobArchiveDao = database.jobArchiveDao()
    private val settingsAdapter = JsonProvider.adapter<AppSettings>()
    private val planAdapter = JsonProvider.adapter<ResignationPlan>()
    private val deferredAdapter = JsonProvider.adapter<DeferredBackupCollections>()
    private val importJournalAdapter = JsonProvider.adapter<BackupImportJournal>()
    private val importJournalBaseFile = File(appContext.filesDir, IMPORT_JOURNAL_FILE)
    private val importJournalFile = AtomicFile(importJournalBaseFile)
    private val importMarkerDao = database.backupImportMarkerDao()
    private val operationMutex = Mutex()

    /** 蒐集完整快照並編碼成 iOS/Android 共用的 v4 JSON。 */
    suspend fun exportJson(): String = operationMutex.withLock { codec.encode(collectPayloadUnlocked()) }

    suspend fun collectPayload(): BackupPayload = operationMutex.withLock { collectPayloadUnlocked() }

    private suspend fun collectPayloadUnlocked(): BackupPayload {
        // 匯出前先確保舊 coinCount 已安全 seed 到 ledger。
        coinLedgerRepository.initialize()
        val preferences = appContext.appDataStore.data.first()
        val settings = preferences[SETTINGS_KEY]
            ?.let { runCatching { settingsAdapter.fromJson(it) }.getOrNull() }
            ?: AppSettings()
        val plan = preferences[PLAN_KEY]
            ?.let { runCatching { planAdapter.fromJson(it) }.getOrNull() }
            ?: ResignationPlan()
        val deferred = preferences[DEFERRED_COLLECTIONS_KEY]
            ?.let { runCatching { deferredAdapter.fromJson(it) }.getOrNull() }
            ?: DeferredBackupCollections()
        // 蓋章與每日 +1 在同一個 Room transaction 寫入；這裡也必須在同一
        // read transaction 取快照，避免匯出「有新 stamp 但少對應 ledger」的混合狀態。
        val (ledgerEntries, stampRecords, shopPurchases) = database.withTransaction {
            Triple(coinLedgerDao.getAll(), stampDao.getAll(), shopPurchaseDao.getAll())
        }
        // 匯出全部帳號的過往工作（與 iOS 相同）；payload 解不開的那筆無法安全匯出就略過。
        val jobArchives = database.withTransaction { jobArchiveDao.getAll() }
            .mapNotNull(JobArchiveEntity::toBackupOrNull)
        val ledgerBalance = CoinLedgerPolicy.balance(ledgerEntries)
        val mirroredSettings = settings.copy(
            coinCount = ledgerBalance,
            hasMigratedToLedger = true,
        )

        return BackupPayload(
            schemaVersion = BackupPayload.CURRENT_SCHEMA_VERSION,
            exportDate = BackupDateCodec.format(nowMillis()),
            appSettings = listOf(mirroredSettings.toBackup()),
            stampRecords = stampRecords.map(StampRecordEntity::toBackup),
            resignationPlans = listOf(plan.toBackup()),
            niuMaConfig = plan.toNiuMaConfigBackup(deferred.workDays, nowMillis()),
            coinLedgerEntries = ledgerEntries.map(CoinLedgerEntryEntity::toBackup),
            shopPurchaseRecords = (deferred.shopPurchaseRecords + shopPurchases.map(ShopPurchaseEntity::toBackup))
                .associateBy { it.id }
                .values
                .sortedBy { it.purchasedAt },
            jobArchives = jobArchives,
            lastAuthProvider = preferences[LAST_AUTH_PROVIDER_KEY]?.takeIf(String::isNotBlank),
        )
    }

    /** 可供 UI 預檢檔案；只讀且不會改動本機資料。 */
    fun decodeAndValidate(json: String): BackupPayload = codec.decodeAndValidate(json)

    /**
     * 完整驗證後才進行覆蓋匯入。舊備份沒有 lastAuthProvider 時不改動本機提示，
     * 與 iOS 語意一致。
     */
    suspend fun importJson(json: String): BackupImportSummary {
        val payload = codec.decodeAndValidate(json)

        // 所有可能拋錯的日期/domain 轉換都在任何寫入前完成。
        val importedLedgerEntries = payload.ledgerEntriesForImport().map(BackupCoinLedgerEntry::toEntity)
        val importedShopPurchases = payload.shopPurchaseRecordsForImport().map(BackupShopPurchaseRecord::toEntity)
        val importedBalance = CoinLedgerPolicy.balance(importedLedgerEntries)
        val importedSettings = payload.appSettings.single()
            .toDomain(payload.schemaVersion)
            .copy(
                coinCount = importedBalance,
                hasMigratedToLedger = true,
                isOnboardingFinished = true,
            )
        val importedPlan = payload.resignationPlans.single().toDomain(payload.niuMaConfig)
        val importedStamps = payload.stampRecords.map(BackupStampRecord::toEntity)
        val deferredCollections = DeferredBackupCollections(
            shopPurchaseRecords = emptyList(),
            workDays = payload.niuMaConfig.workDays,
        )
        // v4：過往工作採「合併」（封存是唯讀歷史，本機已有的一律保留）；v1–v3 不動本機封存。
        val importedArchives = payload.jobArchivesForImport().map(BackupJobArchive::toEntity)
        val newPlanJson = planAdapter.toJson(importedPlan)
        val newDeferredJson = deferredAdapter.toJson(deferredCollections)
        val newDatabaseFingerprint = databaseFingerprint(importedStamps, importedLedgerEntries)
        val importToken = UUID.randomUUID().toString()

        return operationMutex.withLock {
            // 如果上次在 commit 窗口被系統終止，先完成那次的可重複復原。
            recoverPendingImportUnlocked()

            val oldPreferences = preferenceSnapshot(appContext.appDataStore.data.first())
            // currentJobIndex 只增不減：還原較舊的備份時沿用本機較大的值（含本機封存編號 + 1）。
            val existingArchives = database.withTransaction { jobArchiveDao.getAll() }
            val localSettings = oldPreferences.settingsJson
                ?.let { runCatching { settingsAdapter.fromJson(it) }.getOrNull() }
            val newSettingsJson = settingsAdapter.toJson(
                importedSettings.copy(
                    currentJobIndex = BackupJobArchiveMerge.mergedJobIndex(
                        imported = importedSettings.currentJobIndex,
                        localSettingsJobIndex = localSettings?.currentJobIndex,
                        localArchiveJobIndexes = existingArchives.map { it.jobIndex },
                    ),
                ),
            )
            val newPreferences = BackupPreferenceSnapshot(
                settingsJson = newSettingsJson,
                planJson = newPlanJson,
                lastAuthProvider = payload.lastAuthProvider
                    ?.takeIf(String::isNotBlank)
                    ?: oldPreferences.lastAuthProvider,
                deferredCollectionsJson = newDeferredJson,
            )
            var mergedArchiveCount = 0
            try {
                database.withTransaction {
                    val oldDatabaseFingerprint = databaseFingerprint(
                        stamps = stampDao.getAll(),
                        ledgerEntries = coinLedgerDao.getAll(),
                    )
                    // Write-ahead：journal 必須在 Room/DataStore 任何覆蓋前原子落盤。
                    writeImportJournal(
                        BackupImportJournal(
                            token = importToken,
                            oldPreferences = oldPreferences,
                            newPreferences = newPreferences,
                            oldDatabaseFingerprint = oldDatabaseFingerprint,
                            newDatabaseFingerprint = newDatabaseFingerprint,
                        ),
                    )
                    stampDao.replaceAll(importedStamps)
                    coinLedgerDao.replaceAllForBackup(importedLedgerEntries)
                    shopPurchaseDao.replaceAllForBackup(importedShopPurchases)
                    mergedArchiveCount = BackupJobArchiveMerge.newArchives(
                        imported = importedArchives,
                        existing = jobArchiveDao.getAll(),
                    ).count { jobArchiveDao.insertIfAbsent(it) != -1L }
                    appContext.appDataStore.edit { preferences ->
                        preferences.applySnapshot(newPreferences)
                        preferences[IMPORT_TOKEN_KEY] = importToken
                    }
                    // 必須與 stamps/ledger 同 transaction 提交；這是無視後續新寫入
                    // 仍能準確判定匯入已 commit 的永久依據。
                    importMarkerDao.markCommitted(
                        BackupImportMarkerEntity(token = importToken, committedAt = nowMillis()),
                    )
                }
            } catch (error: Exception) {
                val recoveryFailure = withContext(NonCancellable) {
                    runCatching { recoverPendingImportUnlocked() }.exceptionOrNull()
                }
                recoveryFailure?.let(error::addSuppressed)
                if (error is CancellationException) throw error
                val message = if (recoveryFailure == null) {
                    "Backup import failed; previous data was restored"
                } else {
                    "Backup import failed and its recovery journal could not be resolved"
                }
                throw BackupWriteException(message, error)
            }

            // DB 已提交、preferences 也已是新版；此後即使刪除失敗，下次啟動也會
            // 依 Room commit token 判定已成功，不會受後續新流水影響。
            clearImportJournal()
            // Journal 先清；此處若被系統終止，遺留 token 不影響資料。
            runCatching {
                appContext.appDataStore.edit { preferences ->
                    if (preferences[IMPORT_TOKEN_KEY] == importToken) preferences.remove(IMPORT_TOKEN_KEY)
                }
            }

            BackupImportSummary(
                schemaVersion = payload.schemaVersion,
                stampsImported = importedStamps.size,
                settingsRecordsFound = payload.appSettings.size,
                planRecordsFound = payload.resignationPlans.size,
                coinLedgerEntriesPreserved = importedLedgerEntries.size,
                shopPurchaseRecordsPreserved = importedShopPurchases.size,
                jobArchivesMerged = mergedArchiveCount,
            )
        }
    }

    /** 將早期版本暫存在 DataStore 的 v3 商店資料搬進正式 Room outbox。 */
    suspend fun migrateDeferredShopPurchases() = operationMutex.withLock {
        val preferences = appContext.appDataStore.data.first()
        val deferred = preferences[DEFERRED_COLLECTIONS_KEY]
            ?.let { runCatching { deferredAdapter.fromJson(it) }.getOrNull() }
            ?: return@withLock
        if (deferred.shopPurchaseRecords.isEmpty()) return@withLock

        database.withTransaction {
            shopPurchaseDao.insertAllIfAbsent(deferred.shopPurchaseRecords.map(BackupShopPurchaseRecord::toEntity))
        }
        appContext.appDataStore.edit { prefs ->
            prefs[DEFERRED_COLLECTIONS_KEY] = deferredAdapter.toJson(
                deferred.copy(shopPurchaseRecords = emptyList()),
            )
        }
    }

    /**
     * Application 啟動時、金幣遷移前呼叫。無 journal 時只是一次快速的私有檔案檢查。
     */
    internal suspend fun recoverPendingImportIfNeeded(): BackupImportRecoveryDecision? =
        operationMutex.withLock {
            try {
                recoverPendingImportUnlocked()
            } catch (error: Exception) {
                // AtomicFile 損毀或未知未來版本不能讓 Application 永久 crash-loop。
                // 保留原檔成 quarantine 供診斷，不猜測覆寫當前任何資料。
                quarantineImportJournal()
                Log.e(TAG, "Backup import recovery was quarantined", error)
                BackupImportRecoveryDecision.UNRESOLVED
            }
        }

    private fun preferenceSnapshot(preferences: Preferences) = BackupPreferenceSnapshot(
        settingsJson = preferences[SETTINGS_KEY],
        planJson = preferences[PLAN_KEY],
        lastAuthProvider = preferences[LAST_AUTH_PROVIDER_KEY],
        deferredCollectionsJson = preferences[DEFERRED_COLLECTIONS_KEY],
    )

    private suspend fun recoverPendingImportUnlocked(): BackupImportRecoveryDecision? {
        val journal = readImportJournal() ?: return null
        check(journal.version == BackupImportJournal.CURRENT_VERSION) {
            "Unsupported backup import journal version ${journal.version}"
        }

        val currentPreferences = appContext.appDataStore.data.first()
        val committedToken = database.withTransaction { importMarkerDao.currentToken() }
        val decision = BackupImportCommitPolicy.decide(
            committedDatabaseToken = committedToken,
            dataStoreToken = currentPreferences[IMPORT_TOKEN_KEY],
            journalToken = journal.token,
        )
        val snapshot = when (decision) {
            BackupImportRecoveryDecision.APPLY_OLD_PREFERENCES -> journal.oldPreferences
            BackupImportRecoveryDecision.APPLY_NEW_PREFERENCES -> journal.newPreferences
            BackupImportRecoveryDecision.PRESERVE_CURRENT_PREFERENCES -> null
            BackupImportRecoveryDecision.UNRESOLVED -> error("Commit-token policy cannot be unresolved")
        }

        appContext.appDataStore.edit { preferences ->
            if (snapshot != null) preferences.applySnapshot(snapshot)
        }
        // 一定先清 journal 再移除 token。若反過來，在兩步之間 process death，
        // 下次啟動會把 PRESERVE 誤判成 APPLY_NEW，覆蓋 commit 後的合法設定更新。
        clearImportJournal()
        runCatching {
            appContext.appDataStore.edit { preferences ->
                if (preferences[IMPORT_TOKEN_KEY] == journal.token) preferences.remove(IMPORT_TOKEN_KEY)
            }
        }
        return decision
    }

    private fun databaseFingerprint(
        stamps: List<StampRecordEntity>,
        ledgerEntries: List<CoinLedgerEntryEntity>,
    ): String = BackupDatabaseFingerprint.compute(
        stamps = stamps.map { stamp ->
            BackupFingerprintStamp(
                id = stamp.id,
                date = stamp.date,
                reason = stamp.reason,
                isEasterEgg = stamp.isEasterEgg,
                roundNumber = stamp.roundNumber,
            )
        },
        ledgerEntries = ledgerEntries.map { entry ->
            BackupFingerprintLedgerEntry(
                id = entry.id,
                amount = entry.amount,
                type = entry.type,
                note = entry.note,
                createdAt = entry.createdAt,
            )
        },
    )

    private fun writeImportJournal(journal: BackupImportJournal) {
        val bytes = importJournalAdapter.toJson(journal).toByteArray(StandardCharsets.UTF_8)
        val output = importJournalFile.startWrite()
        try {
            output.write(bytes)
            importJournalFile.finishWrite(output)
        } catch (error: Throwable) {
            importJournalFile.failWrite(output)
            throw error
        }
    }

    private fun readImportJournal(): BackupImportJournal? {
        val json = try {
            importJournalFile.openRead().bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
        } catch (_: FileNotFoundException) {
            return null
        }
        return importJournalAdapter.fromJson(json)
            ?: throw BackupRecoveryException("Backup import journal is empty")
    }

    private fun clearImportJournal() {
        importJournalFile.delete()
    }

    private fun quarantineImportJournal() {
        if (!importJournalBaseFile.exists()) {
            importJournalFile.delete()
            return
        }
        val quarantine = File(appContext.filesDir, "$IMPORT_JOURNAL_FILE.quarantine")
        runCatching { if (quarantine.exists()) quarantine.delete() }
        if (!importJournalBaseFile.renameTo(quarantine)) {
            // 無法改名時仍必須停止重複啟動失敗；當前 DB/DataStore 都不動。
            importJournalFile.delete()
        }
    }

    private fun androidx.datastore.preferences.core.MutablePreferences.applySnapshot(
        snapshot: BackupPreferenceSnapshot,
    ) {
        restore(SETTINGS_KEY, snapshot.settingsJson)
        restore(PLAN_KEY, snapshot.planJson)
        restore(LAST_AUTH_PROVIDER_KEY, snapshot.lastAuthProvider)
        restore(DEFERRED_COLLECTIONS_KEY, snapshot.deferredCollectionsJson)
    }

    private fun androidx.datastore.preferences.core.MutablePreferences.restore(
        key: Preferences.Key<String>,
        value: String?,
    ) {
        if (value == null) remove(key) else this[key] = value
    }

    companion object {
        private val SETTINGS_KEY = stringPreferencesKey("settings_json")
        private val PLAN_KEY = stringPreferencesKey("resignation_plan_json")
        private val LAST_AUTH_PROVIDER_KEY = stringPreferencesKey("last_auth_provider")
        private val IMPORT_TOKEN_KEY = stringPreferencesKey("backup_import_token")
        private const val IMPORT_JOURNAL_FILE = "backup_import_journal.json"
        private const val TAG = "BackupRepository"

        // 舊版曾暫存商店交易；v4 啟動時會搬進 Room outbox，此 key 仍保留 workDays。
        private val DEFERRED_COLLECTIONS_KEY = stringPreferencesKey("backup_deferred_collections_json")

        private val FILE_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter
            .ofPattern("yyyy-MM-dd")
            .withZone(ZoneId.systemDefault())

        fun suggestedFileName(epochMillis: Long = System.currentTimeMillis()): String =
            "NNOWORK-backup-${FILE_DATE_FORMATTER.format(Instant.ofEpochMilli(epochMillis))}.json"
    }
}

data class BackupImportSummary(
    val schemaVersion: Int,
    val stampsImported: Int,
    val settingsRecordsFound: Int,
    val planRecordsFound: Int,
    val coinLedgerEntriesPreserved: Int,
    val shopPurchaseRecordsPreserved: Int,
    /** v4：這次新合併進本機的過往工作筆數（已存在的不重複插入）。 */
    val jobArchivesMerged: Int = 0,
)

/** 寫入/回滾錯誤不是檔案格式錯誤，因此不繼承 BackupException。 */
class BackupWriteException(message: String, cause: Throwable? = null) : IllegalStateException(message, cause)

class BackupRecoveryException(message: String, cause: Throwable? = null) : IllegalStateException(message, cause)

@JsonClass(generateAdapter = false)
internal data class DeferredBackupCollections(
    val shopPurchaseRecords: List<BackupShopPurchaseRecord> = emptyList(),
    /** Android 目前沒有 workDays 狀態，先保留 iOS 匯入值以確保往後匯出不遺失。 */
    val workDays: List<Int>? = null,
)

private fun AppSettings.toBackup() = BackupAppSettings(
    companyName = companyName,
    cardStyle = cardStyle,
    maxStamps = maxStamps,
    currentRound = currentRound,
    isOnboardingFinished = isOnboardingFinished,
    widgetRefreshInterval = widgetRefreshInterval,
    firstStampDate = firstStampDate?.let(BackupDateCodec::format),
    isCardLocked = isCardLocked,
    cardCompletedDate = cardCompletedDate?.let(BackupDateCodec::format),
    roundStartDates = roundStartDates.map(BackupDateCodec::format),
    roundCompletedDates = roundCompletedDates.map(BackupDateCodec::format),
    coinCount = coinCount,
    lastCoinEarnedDate = lastCoinEarnedDate?.let(BackupDateCodec::format),
    lastTreeHoleShareDate = lastTreeHoleShareDate?.let(BackupDateCodec::format),
    unlockedStyles = unlockedStyles,
    lastSeenNoticeVersion = lastSeenNoticeVersion,
    commonReasons = commonReasons,
    hasMigratedToLedger = hasMigratedToLedger,
    currentJobIndex = currentJobIndex,
    currentRunNumber = currentRunNumber,
)

private fun BackupAppSettings.toDomain(schemaVersion: Int) = AppSettings(
    companyName = companyName,
    cardStyle = cardStyle,
    maxStamps = maxStamps,
    currentRound = currentRound,
    isOnboardingFinished = isOnboardingFinished,
    widgetRefreshInterval = widgetRefreshInterval,
    firstStampDate = firstStampDate?.let(BackupDateCodec::parse),
    isCardLocked = isCardLocked,
    cardCompletedDate = cardCompletedDate?.let(BackupDateCodec::parse),
    roundStartDates = roundStartDates.map(BackupDateCodec::parse),
    roundCompletedDates = roundCompletedDates.map(BackupDateCodec::parse),
    coinCount = coinCount,
    hasMigratedToLedger = if (schemaVersion < 2) true else hasMigratedToLedger,
    lastCoinEarnedDate = lastCoinEarnedDate?.let(BackupDateCodec::parse),
    lastTreeHoleShareDate = lastTreeHoleShareDate?.let(BackupDateCodec::parse),
    unlockedStyles = unlockedStyles,
    lastSeenNoticeVersion = lastSeenNoticeVersion,
    commonReasons = commonReasons,
    currentJobIndex = currentJobIndex.coerceAtLeast(1),
    currentRunNumber = currentRunNumber.coerceAtLeast(0),
)

private fun StampRecordEntity.toBackup() = BackupStampRecord(
    id = id,
    date = BackupDateCodec.format(date),
    reason = reason,
    isEasterEgg = isEasterEgg,
    roundNumber = roundNumber,
)

private fun BackupStampRecord.toEntity() = StampRecordEntity(
    id = id,
    date = BackupDateCodec.parse(date),
    reason = reason,
    isEasterEgg = isEasterEgg,
    roundNumber = roundNumber,
)

private fun CoinLedgerEntryEntity.toBackup() = BackupCoinLedgerEntry(
    id = id,
    amount = amount,
    type = type,
    note = note,
    createdAt = BackupDateCodec.format(createdAt),
)

private fun BackupCoinLedgerEntry.toEntity() = CoinLedgerEntryEntity(
    id = id,
    amount = amount,
    type = type,
    note = note,
    createdAt = BackupDateCodec.parse(createdAt),
)

private fun ShopPurchaseEntity.toBackup() = BackupShopPurchaseRecord(
    id = id,
    productType = productType,
    productID = productId,
    productName = productName,
    quantity = quantity,
    unitCoinCost = unitCoinCost,
    totalCoinCost = totalCoinCost,
    currency = currency,
    status = status,
    source = source,
    purchasedAt = BackupDateCodec.format(purchasedAt),
    syncedAt = syncedAt?.let(BackupDateCodec::format),
)

private fun BackupShopPurchaseRecord.toEntity() = ShopPurchaseEntity(
    id = id,
    productType = productType,
    productId = productID,
    productName = productName,
    quantity = quantity,
    unitCoinCost = unitCoinCost,
    totalCoinCost = totalCoinCost,
    currency = currency,
    status = status,
    source = source,
    purchasedAt = BackupDateCodec.parse(purchasedAt),
    syncedAt = syncedAt?.let(BackupDateCodec::parse),
)

private fun ResignationPlan.toBackup() = BackupResignationPlan(
    targetAmount = targetAmount,
    currentSaved = currentSaved,
    hasResume = hasResume,
    todoItems = todoItems.map(TodoItem::toBackup),
    completedTodoItems = completedTodoItems.map(TodoItem::toBackup),
)

internal fun TodoItem.toBackup() = BackupTodoItem(
    id = id,
    title = title,
    dueDate = reminderAt?.let(BackupDateCodec::format),
    notificationScheduled = notificationScheduled,
    createdAt = BackupDateCodec.format(createdAt),
)

private fun BackupResignationPlan.toDomain(config: BackupNiuMaConfig): ResignationPlan {
    val activeTodos = todoItems.takeIf { it.isNotEmpty() }
        ?.map(BackupTodoItem::toDomain)
        ?: todoList.mapIndexed { index, title -> legacyTodo("active", index, title) }
    val completedTodos = completedTodoItems.takeIf { it.isNotEmpty() }
        ?.map(BackupTodoItem::toDomain)
        ?: completedTodoList.mapIndexed { index, title -> legacyTodo("completed", index, title) }

    return ResignationPlan(
        targetAmount = targetAmount,
        currentSaved = currentSaved,
        hasResume = hasResume,
        todoItems = activeTodos,
        completedTodoItems = completedTodos,
        salaryPayMode = config.payMode,
        hourlyTrackingMode = config.effectiveHourlyTrackingMode(),
        monthlySalary = config.monthlySalary,
        hourlyRate = config.hourlyRate,
        currencySymbol = config.currencySymbol,
        workStartMinutes = config.startMinutes,
        workEndMinutes = config.endMinutes,
        lunchUnpaid = config.lunchUnpaid,
        lunchMinutes = config.lunchMinutes,
        paydayDays = config.paydayDays.toSet(),
        monthlyScheduleMode = config.monthlyScheduleMode,
        customRestDays = config.customRestDays.toSet(),
        shiftUseTemplates = config.shiftUseTemplates,
        shifts = config.shifts.map(BackupShift::toDomain),
        dayShifts = config.dayShifts,
        // 備份匯入不應在背景偷偷續跑；running 一律以 paused 恢復。
        manualTimerStatus = if (
            config.manualTimerStatus == ManualSalaryTimer.RUNNING ||
            config.manualTimerStatus == ManualSalaryTimer.PAUSED
        ) {
            ManualSalaryTimer.PAUSED
        } else {
            ManualSalaryTimer.IDLE
        },
        manualTimerStartedAt = null,
        manualTimerElapsedMillis = config.manualTimerElapsedMillis.coerceAtLeast(0L),
    )
}

internal fun BackupTodoItem.toDomain() = TodoItem(
    id = id,
    title = title,
    createdAt = createdAt?.let(BackupDateCodec::parse)
        ?: dueDate?.let(BackupDateCodec::parse)
        ?: 0L,
    reminderAt = dueDate?.let(BackupDateCodec::parse),
    notificationScheduled = notificationScheduled,
)

private fun legacyTodo(bucket: String, index: Int, title: String) = TodoItem(
    id = stableUuid("legacy-todo|$bucket|$index|$title"),
    title = title,
    createdAt = 0L,
)

private fun ResignationPlan.toNiuMaConfigBackup(
    preservedWorkDays: List<Int>?,
    exportedAt: Long,
) = BackupNiuMaConfig(
    payMode = salaryPayMode,
    hourlyTrackingMode = HourlyTrackingMode.normalize(hourlyTrackingMode),
    // iOS NiuMaConfigDTO 的 key；兩個都寫，iOS 還原時才不會靜默退回固定工時。
    hourlyManualMode = HourlyTrackingMode.normalize(hourlyTrackingMode) == HourlyTrackingMode.MANUAL,
    monthlySalary = monthlySalary,
    hourlyRate = hourlyRate,
    startMinutes = workStartMinutes,
    endMinutes = workEndMinutes,
    // Android 現有計算器尚無 workDays 狀態；優先原樣帶回 iOS 匯入值。
    workDays = preservedWorkDays ?: listOf(2, 3, 4, 5, 6),
    paydayDays = paydayDays.sorted(),
    currencySymbol = currencySymbol,
    lunchUnpaid = lunchUnpaid,
    lunchMinutes = lunchMinutes,
    monthlyScheduleMode = monthlyScheduleMode,
    customRestDays = customRestDays.filterNot { it.endsWith("-00-00") }.sorted(), // 去掉「這一年已設定」標記（不是日期）
    shifts = shifts.map(Shift::toBackup),
    dayShifts = dayShifts,
    shiftUseTemplates = shiftUseTemplates,
    // 匯出瞬間凍結正在跑的片段，避免數天後匯入時把離線時間也算進去。
    manualTimerStatus = if (manualTimerStatus == ManualSalaryTimer.IDLE) {
        ManualSalaryTimer.IDLE
    } else {
        ManualSalaryTimer.PAUSED
    },
    manualTimerElapsedMillis = ManualSalaryTimer.elapsedMillis(this, exportedAt),
)

private fun Shift.toBackup() = BackupShift(
    id = id,
    name = name,
    startMinutes = startMinutes,
    endMinutes = endMinutes,
    colorHex = colorHex,
    icon = icon,
)

private fun BackupShift.toDomain() = Shift(
    id = id,
    name = name,
    startMinutes = startMinutes,
    endMinutes = endMinutes,
    colorHex = colorHex,
    icon = icon,
)

internal fun BackupPayload.ledgerEntriesForImport(): List<BackupCoinLedgerEntry> {
    if (schemaVersion >= 2) return coinLedgerEntries

    // iOS v1 相容語意：舊檔沒有 ledger，以 coinCount 建立 migration entry。
    return appSettings.mapIndexedNotNull { index, settings ->
        settings.coinCount.takeIf { it > 0 }?.let { amount ->
            BackupCoinLedgerEntry(
                id = stableUuid("v1-ledger|$index|$amount|$exportDate"),
                amount = amount,
                type = "migration",
                note = "v1 備份匯入相容遷移",
                createdAt = exportDate,
            )
        }
    }
}

private fun BackupPayload.shopPurchaseRecordsForImport(): List<BackupShopPurchaseRecord> =
    if (schemaVersion >= 3) shopPurchaseRecords else emptyList()

internal fun BackupPayload.jobArchivesForImport(): List<BackupJobArchive> =
    if (schemaVersion >= 4) jobArchives else emptyList()

/** iOS 備份只有 hourlyManualMode；Android 舊匯出只有 hourlyTrackingMode。 */
internal fun BackupNiuMaConfig.effectiveHourlyTrackingMode(): Int = when (hourlyManualMode) {
    true -> HourlyTrackingMode.MANUAL
    false -> HourlyTrackingMode.SCHEDULE
    null -> HourlyTrackingMode.normalize(hourlyTrackingMode)
}

internal fun BackupJobArchive.toEntity(): JobArchiveEntity = payload.toEntity(
    id = id,
    owner = ownerUserID.orEmpty(),
    remoteId = remoteID,
    syncedAt = syncedAt?.let(BackupDateCodec::parse),
)

internal fun JobArchiveEntity.toBackupOrNull(): BackupJobArchive? {
    val decoded = payload ?: return null
    return BackupJobArchive(
        id = id,
        remoteID = remoteId,
        syncedAt = syncedAt?.let(BackupDateCodec::format),
        payload = decoded,
        ownerUserID = ownerUserId.takeIf(String::isNotEmpty),
    )
}

/** 備份匯入時過往工作的合併規則（iOS DataBackupManager.restore 的 v4 段）。 */
internal object BackupJobArchiveMerge {
    fun mergedJobIndex(
        imported: Int,
        localSettingsJobIndex: Int?,
        localArchiveJobIndexes: List<Int>,
    ): Int {
        val localMax = (listOfNotNull(localSettingsJobIndex) + localArchiveJobIndexes.map { it + 1 }).maxOrNull() ?: 1
        return maxOf(imported, localMax, 1)
    }

    /** 以「擁有者＋id」與「擁有者＋remoteID」去重；不同帳號同 id 互不吃掉。 */
    fun newArchives(imported: List<JobArchiveEntity>, existing: List<JobArchiveEntity>): List<JobArchiveEntity> {
        val known = mutableSetOf<String>()
        existing.forEach { archive ->
            known += "${archive.ownerUserId}|${archive.id.lowercase()}"
            known += "${archive.ownerUserId}|${archive.dedupeKey}"
        }
        val result = mutableListOf<JobArchiveEntity>()
        for (archive in imported) {
            val idKey = "${archive.ownerUserId}|${archive.id.lowercase()}"
            val remoteKey = archive.remoteId?.let { "${archive.ownerUserId}|${it.lowercase()}" }
            if (idKey in known || (remoteKey != null && remoteKey in known)) continue
            result += archive
            known += idKey
            remoteKey?.let(known::add)
        }
        return result
    }
}

private fun stableUuid(seed: String): String = UUID
    .nameUUIDFromBytes(seed.toByteArray(StandardCharsets.UTF_8))
    .toString()
