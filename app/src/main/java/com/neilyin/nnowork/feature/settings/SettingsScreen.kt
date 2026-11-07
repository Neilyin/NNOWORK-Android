package com.neilyin.nnowork.feature.settings

import com.neilyin.nnowork.ui.theme.NnoworkRadius
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import com.neilyin.nnowork.ui.theme.PixelButtonVariant
import com.neilyin.nnowork.ui.theme.NnoworkField
import com.neilyin.nnowork.ui.theme.NnoworkPageTitle
import com.neilyin.nnowork.ui.theme.NnoworkDialogDefaults
import android.content.Intent
import com.neilyin.nnowork.ui.theme.NnoworkSpacing
import com.neilyin.nnowork.ui.theme.NnoworkLine
import com.neilyin.nnowork.ui.theme.NnoworkTheme
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import com.neilyin.nnowork.util.PlayStore
import com.neilyin.nnowork.feature.manual.UserManualSheet
import androidx.compose.ui.semantics.Role
import androidx.compose.material.icons.filled.Star
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.runtime.saveable.rememberSaveable
import com.neilyin.nnowork.ui.theme.NnoworkSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.layout.Layout
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.ui.layout.ParentDataModifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import com.neilyin.nnowork.ui.adaptive.LocalNnoworkWindowInfo
import com.neilyin.nnowork.ui.adaptive.twoPaneSplit
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.neilyin.nnowork.BuildConfig
import com.neilyin.nnowork.analytics.Analytics
import com.neilyin.nnowork.data.auth.AuthRepository
import com.neilyin.nnowork.data.auth.AuthUiState
import com.neilyin.nnowork.data.backup.BackupException
import com.neilyin.nnowork.data.backup.BackupRepository
import com.neilyin.nnowork.data.backup.BackupWriteException
import com.neilyin.nnowork.data.coin.CoinLedgerRepository
import com.neilyin.nnowork.data.coin.ThemePurchaseResult
import com.neilyin.nnowork.data.jobarchive.JobArchiveRepository
import com.neilyin.nnowork.feature.jobarchive.JobTransitionSheet
import com.neilyin.nnowork.data.local.StampDao
import com.neilyin.nnowork.data.plan.PlanRepository
import com.neilyin.nnowork.data.settings.AppSettings
import com.neilyin.nnowork.data.settings.SettingsRepository
import com.neilyin.nnowork.data.treehole.BlockedTreeHoleAuthor
import com.neilyin.nnowork.data.treehole.TreeHoleRepository
import com.neilyin.nnowork.data.treehole.ANDROID_FEEDBACK_SOURCE
import com.neilyin.nnowork.di.ServiceLocator
import com.neilyin.nnowork.ui.theme.CardTheme
import com.neilyin.nnowork.ui.theme.isUnlocked
import com.neilyin.nnowork.ui.theme.appWideTheme
import com.neilyin.nnowork.ui.theme.PixelButton
import com.neilyin.nnowork.ui.theme.NnoworkTextField
import com.neilyin.nnowork.ui.theme.pixelCard
import com.neilyin.nnowork.ui.theme.pixelTextStyle
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.Reader

/**
 * 刪除雲端帳號（iOS AccountAuthView.performDeleteAccount）：deleteAccount 會清掉 session，
 * 所以帳號 id 必須在呼叫**前**記下；伺服器確認刪除後才刪這個帳號的本機過往工作。
 */
internal suspend fun deleteAccountAndLocalArchives(
    currentOwnerId: () -> String?,
    deleteAccount: suspend () -> Boolean,
    deleteLocalArchives: suspend (ownerId: String?) -> Unit,
): Boolean {
    val owner = currentOwnerId()
    if (!deleteAccount()) return false
    // 雲端的 job_archives 由 auth.users ON DELETE CASCADE 刪除；本機這份跟著刪，與確認對話框說明一致。
    runCatching { deleteLocalArchives(owner) }
    return true
}

data class PreparedBackupImport(
    val json: String,
    val schemaVersion: Int,
    val stampCount: Int,
)

data class BackupUiState(
    val inProgress: Boolean = false,
    val pendingImport: PreparedBackupImport? = null,
    val statusMessage: String? = null,
    val statusIsError: Boolean = false,
)

class SettingsViewModel(
    private val settingsRepository: SettingsRepository = ServiceLocator.settingsRepository,
    private val planRepository: PlanRepository = ServiceLocator.planRepository,
    private val stampDao: StampDao = ServiceLocator.stampDao,
    private val treeHoleRepository: TreeHoleRepository = ServiceLocator.treeHoleRepository,
    private val backupRepository: BackupRepository = ServiceLocator.backupRepository,
    private val coinLedgerRepository: CoinLedgerRepository = ServiceLocator.coinLedgerRepository,
    private val authRepository: AuthRepository = ServiceLocator.authRepository,
    private val jobArchiveRepository: JobArchiveRepository = ServiceLocator.jobArchiveRepository,
) : ViewModel() {
    private val _accountDeleteFailed = MutableStateFlow(false)
    /** 刪除帳號失敗（iOS「刪除失敗」alert）。 */
    val accountDeleteFailed: StateFlow<Boolean> = _accountDeleteFailed.asStateFlow()
    private val _resetFailed = MutableStateFlow(false)
    /** 「全部重來」寫入失敗（iOS showingSaveErrorAlert「存檔失敗」）。 */
    val resetFailed: StateFlow<Boolean> = _resetFailed.asStateFlow()

    fun dismissResetFailure() {
        _resetFailed.value = false
    }

    val settings: StateFlow<AppSettings> = settingsRepository.settings.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        AppSettings(),
    )
    private val _backupUiState = MutableStateFlow(BackupUiState())
    val backupUiState: StateFlow<BackupUiState> = _backupUiState.asStateFlow()
    val authState: StateFlow<AuthUiState> = authRepository.state
    val blockedAuthors: StateFlow<List<BlockedTreeHoleAuthor>> = treeHoleRepository.blockedAuthors.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList(),
    )
    val googleOAuthUri: Uri get() = authRepository.googleOAuthUri

    fun updateCompany(name: String) = update { it.copy(companyName = name.trim()) }
    fun updateWidgetInterval(minutes: Int) {
        update { it.copy(widgetRefreshInterval = minutes) }
        com.neilyin.nnowork.widget.WidgetScheduler.schedule(ServiceLocator.applicationContext, minutes)
    }
    fun selectTheme(theme: CardTheme) {
        viewModelScope.launch {
            val current = settingsRepository.current()
            if (!theme.isUnlocked(current.unlockedStyles)) return@launch
            settingsRepository.update { it.copy(cardStyle = theme.id) }
            com.neilyin.nnowork.widget.WidgetScheduler.refreshNow(ServiceLocator.applicationContext)
            com.neilyin.nnowork.analytics.Analytics.log("theme_changed", "theme_name" to theme.name)
        }
    }
    fun unlockTheme(theme: CardTheme) {
        if (!theme.requiresCoins) {
            selectTheme(theme)
            return
        }
        viewModelScope.launch {
            when (
                coinLedgerRepository.purchaseTheme(
                    themeId = theme.id,
                    productId = theme.shopProductId,
                    themeName = ServiceLocator.applicationContext.getString(theme.titleRes),
                    coinCost = theme.coinCost,
                )
            ) {
                ThemePurchaseResult.PURCHASED -> {
                    ServiceLocator.shopPurchaseRepository.syncPending()
                    com.neilyin.nnowork.widget.WidgetScheduler.refreshNow(ServiceLocator.applicationContext)
                    com.neilyin.nnowork.analytics.Analytics.log(
                        "theme_unlocked",
                        "theme_name" to theme.name,
                        "coin_cost" to theme.coinCost.toString(),
                    )
                }
                ThemePurchaseResult.ALREADY_OWNED,
                ThemePurchaseResult.INSUFFICIENT_FUNDS,
                -> Unit
            }
        }
    }

    fun addReason(reason: String) {
        val trimmed = reason.trim()
        if (trimmed.isEmpty()) return
        update {
            if (it.commonReasons.contains(trimmed)) it else it.copy(commonReasons = it.commonReasons + trimmed)
        }
    }
    fun deleteReason(reason: String) = update { it.copy(commonReasons = it.commonReasons.filterNot { item -> item == reason }) }
    /**
     * 「全部重來」（iOS resetAllForFreshStart）：清掉目前的章與離職計畫、回到新手設定；
     * currentJobIndex 照樣 +1、周目設回 1。保留已解鎖主題、金幣流水帳、今天的每日金幣、常用原因、過往工作。
     */
    fun resetAll() {
        viewModelScope.launch {
            val context = ServiceLocator.applicationContext
            val clearedTodoIds = runCatching {
                jobArchiveRepository.resetAllForFreshStart { styleId, unlocked ->
                    CardTheme.fromId(styleId).isUnlocked(unlocked)
                }
            }.getOrElse {
                _resetFailed.value = true
                return@launch
            }
            runCatching { com.neilyin.nnowork.notify.ReminderScheduler.cancelAll(context, clearedTodoIds) }
            com.neilyin.nnowork.widget.WidgetScheduler.schedule(context, settingsRepository.current().widgetRefreshInterval)
            com.neilyin.nnowork.widget.WidgetScheduler.refreshNow(context)
        }
    }

    fun submitFeedback(content: String, onResult: (String) -> Unit) {
        val trimmed = content.trim()
        if (trimmed.length < 5) {
            onResult("至少寫 5 個字，告訴我們你的想法。")
            return
        }
        viewModelScope.launch {
            runCatching { treeHoleRepository.submitFeedback(trimmed) }
                .onSuccess {
                    onResult("已送出，並已附註「$ANDROID_FEEDBACK_SOURCE」。審核通過後會發放金幣。")
                }
                .onFailure { onResult(it.message?.takeIf { m -> m.isNotBlank() } ?: "送出失敗，稍後再試。") }
        }
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch { authRepository.signIn(email, password) }
    }

    fun signUp(email: String, password: String) {
        viewModelScope.launch { authRepository.signUp(email, password) }
    }

    fun signOut() {
        viewModelScope.launch { authRepository.signOut() }
    }

    fun deleteAuthAccount() {
        viewModelScope.launch {
            val deleted = deleteAccountAndLocalArchives(
                currentOwnerId = { authRepository.currentUserId },
                deleteAccount = { authRepository.deleteAccount() },
                deleteLocalArchives = { owner -> jobArchiveRepository.deleteAllLocal(owner) },
            )
            if (!deleted) _accountDeleteFailed.value = true
        }
    }

    fun dismissAccountDeleteFailure() {
        _accountDeleteFailed.value = false
    }

    fun clearAuthMessage() = authRepository.clearMessage()

    fun unblockTreeHoleAuthor(authorKey: String) {
        viewModelScope.launch { treeHoleRepository.unblockAuthor(authorKey) }
    }

    fun exportBackup(uri: Uri) {
        if (!beginBackupOperation()) return
        viewModelScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    val json = backupRepository.exportJson()
                    val resolver = ServiceLocator.applicationContext.contentResolver
                    resolver.openOutputStream(uri, "wt")?.bufferedWriter(Charsets.UTF_8)?.use { writer ->
                        writer.write(json)
                    } ?: throw IOException("Unable to open backup destination")
                }
            }
            result.onSuccess { Analytics.log("backup_export_succeeded") }
            _backupUiState.value = BackupUiState(
                statusMessage = result.fold(
                    onSuccess = { "備份已匯出，請妥善保存這個 JSON 檔案。" },
                    onFailure = { it.backupErrorMessage("匯出失敗") },
                ),
                statusIsError = result.isFailure,
            )
        }
    }

    fun prepareBackupImport(uri: Uri) {
        if (!beginBackupOperation()) return
        viewModelScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    val resolver = ServiceLocator.applicationContext.contentResolver
                    val json = resolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use {
                        it.readTextLimited(MAX_BACKUP_JSON_CHARS)
                    }
                        ?: throw IOException("Unable to open backup file")
                    val payload = backupRepository.decodeAndValidate(json)
                    PreparedBackupImport(
                        json = json,
                        schemaVersion = payload.schemaVersion,
                        stampCount = payload.stampRecords.size,
                    )
                }
            }
            _backupUiState.value = result.fold(
                onSuccess = { BackupUiState(pendingImport = it) },
                onFailure = {
                    BackupUiState(
                        statusMessage = it.backupErrorMessage("無法讀取備份"),
                        statusIsError = true,
                    )
                },
            )
        }
    }

    fun importBackup() {
        val prepared = _backupUiState.value.pendingImport ?: return
        if (!beginBackupOperation(keepPendingImport = true)) return
        viewModelScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    val previousPlan = planRepository.current()
                    val summary = backupRepository.importJson(prepared.json)
                    previousPlan to summary
                }
            }
            result.onSuccess { (previousPlan, summary) ->
                Analytics.log(
                    "backup_import_succeeded",
                    "schema_version" to summary.schemaVersion.toString(),
                    "stamp_count" to summary.stampsImported.toString(),
                )
                withContext(Dispatchers.IO) {
                    runCatching {
                        val context = ServiceLocator.applicationContext
                        previousPlan.todoItems.forEach { item ->
                            com.neilyin.nnowork.notify.ReminderScheduler.cancel(context, item.id)
                        }
                        val restoredPlan = planRepository.current()
                        restoredPlan.todoItems.forEach { item ->
                            item.reminderAt?.takeIf { item.notificationScheduled }?.let { reminderAt ->
                                com.neilyin.nnowork.notify.ReminderScheduler.schedule(
                                    context = context,
                                    todoId = item.id,
                                    title = item.title,
                                    triggerAtMillis = reminderAt,
                                )
                            }
                        }
                        val restoredSettings = settingsRepository.current()
                        com.neilyin.nnowork.widget.WidgetScheduler.schedule(
                            context,
                            restoredSettings.widgetRefreshInterval,
                        )
                    }
                }
            }
            _backupUiState.value = BackupUiState(
                pendingImport = prepared.takeIf { result.isFailure },
                statusMessage = result.fold(
                    onSuccess = { (_, summary) ->
                        "已還原 ${summary.stampsImported} 筆集點紀錄與備份中的設定。"
                    },
                    onFailure = { it.backupErrorMessage("匯入失敗") },
                ),
                statusIsError = result.isFailure,
            )
        }
    }

    fun dismissPreparedBackupImport() {
        if (_backupUiState.value.inProgress) return
        _backupUiState.value = _backupUiState.value.copy(pendingImport = null)
    }

    private fun beginBackupOperation(keepPendingImport: Boolean = false): Boolean {
        while (true) {
            val current = _backupUiState.value
            if (current.inProgress) return false
            val next = BackupUiState(
                inProgress = true,
                pendingImport = current.pendingImport.takeIf { keepPendingImport },
            )
            if (_backupUiState.compareAndSet(current, next)) return true
        }
    }

    private fun update(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch { settingsRepository.update(transform) }
    }
}

@Composable
fun SettingsScreen(
    onShowUpdateNotice: () -> Unit = {},
    viewModel: SettingsViewModel = viewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val backupUiState by viewModel.backupUiState.collectAsStateWithLifecycle()
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val blockedAuthors by viewModel.blockedAuthors.collectAsStateWithLifecycle()
    val accountDeleteFailed by viewModel.accountDeleteFailed.collectAsStateWithLifecycle()
    val resetFailed by viewModel.resetFailed.collectAsStateWithLifecycle()
    if (resetFailed) {
        AlertDialog(
            onDismissRequest = viewModel::dismissResetFailure,
            containerColor = NnoworkDialogDefaults.containerColor,
            shape = NnoworkDialogDefaults.shape,
            title = { Text(stringResource(com.neilyin.nnowork.R.string.settings_save_failed_title), style = pixelTextStyle(NnoworkTheme.type.title, FontWeight.Bold, NnoworkTheme.colors.onElevated)) },
            text = { Text(stringResource(com.neilyin.nnowork.R.string.settings_save_failed_message), style = pixelTextStyle(NnoworkTheme.type.body, color = NnoworkTheme.colors.onElevated)) },
            confirmButton = {
                PixelButton(onClick = viewModel::dismissResetFailure, backgroundColor = NnoworkTheme.colors.buttonBG) {
                    Text(stringResource(com.neilyin.nnowork.R.string.jobarchive_ok), style = pixelTextStyle(NnoworkTheme.type.body, FontWeight.Bold, NnoworkTheme.colors.onElevated))
                }
            },
        )
    }
    var showJobTransition by rememberSaveable { mutableStateOf(false) }
    if (showJobTransition) {
        JobTransitionSheet(onDismiss = { showJobTransition = false })
    }
    SettingsContent(
        settings = settings,
        backupUiState = backupUiState,
        authState = authState,
        blockedAuthors = blockedAuthors,
        googleOAuthUri = viewModel.googleOAuthUri,
        onUpdateCompany = viewModel::updateCompany,
        onSelectTheme = viewModel::selectTheme,
        onUnlockTheme = viewModel::unlockTheme,
        onAddReason = viewModel::addReason,
        onDeleteReason = viewModel::deleteReason,
        onUpdateWidgetInterval = viewModel::updateWidgetInterval,
        onResetAll = viewModel::resetAll,
        onSubmitFeedback = viewModel::submitFeedback,
        onShowUpdateNotice = onShowUpdateNotice,
        onExportBackup = viewModel::exportBackup,
        onPrepareBackupImport = viewModel::prepareBackupImport,
        onImportBackup = viewModel::importBackup,
        onDismissPreparedBackupImport = viewModel::dismissPreparedBackupImport,
        onAuthSignIn = viewModel::signIn,
        onAuthSignUp = viewModel::signUp,
        onAuthSignOut = viewModel::signOut,
        onDeleteAuthAccount = viewModel::deleteAuthAccount,
        onClearAuthMessage = viewModel::clearAuthMessage,
        onUnblockTreeHoleAuthor = viewModel::unblockTreeHoleAuthor,
        onOpenJobTransition = { showJobTransition = true },
        accountDeleteFailed = accountDeleteFailed,
        onDismissAccountDeleteFailure = viewModel::dismissAccountDeleteFailure,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SettingsContent(
    settings: AppSettings,
    backupUiState: BackupUiState,
    authState: AuthUiState,
    blockedAuthors: List<BlockedTreeHoleAuthor>,
    googleOAuthUri: Uri,
    onUpdateCompany: (String) -> Unit,
    onSelectTheme: (CardTheme) -> Unit,
    onUnlockTheme: (CardTheme) -> Unit,
    onAddReason: (String) -> Unit,
    onDeleteReason: (String) -> Unit,
    onUpdateWidgetInterval: (Int) -> Unit,
    onResetAll: () -> Unit,
    onSubmitFeedback: (String, (String) -> Unit) -> Unit,
    onShowUpdateNotice: () -> Unit,
    onExportBackup: (Uri) -> Unit,
    onPrepareBackupImport: (Uri) -> Unit,
    onImportBackup: () -> Unit,
    onDismissPreparedBackupImport: () -> Unit,
    onAuthSignIn: (String, String) -> Unit,
    onAuthSignUp: (String, String) -> Unit,
    onAuthSignOut: () -> Unit,
    onDeleteAuthAccount: () -> Unit,
    onClearAuthMessage: () -> Unit,
    onUnblockTreeHoleAuthor: (String) -> Unit,
    onOpenJobTransition: () -> Unit = {},
    accountDeleteFailed: Boolean = false,
    onDismissAccountDeleteFailure: () -> Unit = {},
) {
    // 輸入中的公司名稱／新原因、重置確認、主題預覽都用 rememberSaveable，摺疊／展開、旋轉後不消失。
    var company by rememberSaveable(settings.companyName) { mutableStateOf(settings.companyName) }
    var newReason by rememberSaveable { mutableStateOf("") }
    var showReset by rememberSaveable { mutableStateOf(false) }
    var themeToPreviewId by rememberSaveable { mutableStateOf<Int?>(null) }
    val themeToPreview = themeToPreviewId?.let { CardTheme.fromId(it) }
    val twoColumns = LocalNnoworkWindowInfo.current.usesTwoColumnSettings

    themeToPreview?.let { theme ->
        ThemePreviewSheet(
            theme = theme,
            coinBalance = settings.coinCount,
            onBuy = { onUnlockTheme(theme) },
            onDismiss = { didBuy, didStamp ->
                Analytics.log(
                    if (didBuy) "theme_preview_purchased" else "theme_preview_dismissed",
                    "theme_name" to theme.name,
                    "did_stamp" to didStamp.toString(),
                )
                themeToPreviewId = null
            },
        )
    }

    if (showReset) {
        AlertDialog(
            onDismissRequest = { showReset = false },
            containerColor = NnoworkDialogDefaults.containerColor,
            shape = NnoworkDialogDefaults.shape,
            title = { Text(stringResource(com.neilyin.nnowork.R.string.settings_reset_title), style = pixelTextStyle(NnoworkTheme.type.title, FontWeight.Bold, NnoworkTheme.colors.onElevated)) },
            text = { Text(stringResource(com.neilyin.nnowork.R.string.settings_reset_message), style = pixelTextStyle(NnoworkTheme.type.body, color = NnoworkTheme.colors.secondaryText)) },
            confirmButton = {
                PixelButton(
                    onClick = {
                        onResetAll()
                        showReset = false
                    },
                    backgroundColor = NnoworkTheme.colors.danger,
                ) {
                    Text(stringResource(com.neilyin.nnowork.R.string.settings_reset_confirm), style = pixelTextStyle(NnoworkTheme.type.body, FontWeight.Bold, NnoworkTheme.colors.onElevated))
                }
            },
            dismissButton = {
                PixelButton(onClick = { showReset = false }, backgroundColor = NnoworkTheme.colors.disabledBG) {
                    Text(stringResource(com.neilyin.nnowork.R.string.jobarchive_cancel), style = pixelTextStyle(NnoworkTheme.type.body, FontWeight.Bold, NnoworkTheme.colors.primaryText))
                }
            },
        )
    }

    // 手機：單欄（原本順序）。寬螢幕：iOS SettingsView regular 分支的左右兩欄。
    // 兩種版面用同一個 SettingsColumns、同樣的子元件順序，只由 [settingsColumn] 決定放哪一欄，
    // 所以切換版面時各區塊的輸入與展開狀態都保留。
    SettingsColumns(
        twoColumns = twoColumns,
        spacing = NnoworkSpacing.roomy,
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(NnoworkSpacing.page),
    ) {
        NnoworkPageTitle("設定", modifier = Modifier.settingsColumn(SettingsColumn.Header))
        // 年度回顧（限時開放，置頂；雙欄時是左欄第一區）
        com.neilyin.nnowork.feature.yearreview.YearReviewSettingsSection(
            cardTheme = CardTheme.fromId(settings.cardStyle).appWideTheme(settings.unlockedStyles),
        )
        ManualSection()
        AccountSection(
            state = authState,
            onSignIn = onAuthSignIn,
            onSignUp = onAuthSignUp,
            onSignOut = onAuthSignOut,
            onDeleteAccount = onDeleteAuthAccount,
            onClearMessage = onClearAuthMessage,
            googleOAuthUri = googleOAuthUri,
            onExportBackup = onExportBackup,
            deleteFailed = accountDeleteFailed,
            onDismissDeleteFailure = onDismissAccountDeleteFailure,
        )
        CoinSection(settings.coinCount)
        TreeHoleSafetySettingsSection(
            modifier = Modifier.settingsColumn(SettingsColumn.Trailing, order = 1),
            blockedAuthors = blockedAuthors,
            onUnblock = onUnblockTreeHoleAuthor,
        )
        SettingsSection(icon = Icons.Filled.Business, title = "公司資訊") {
            LabeledTextField(
                label = "公司名稱",
                value = company,
                placeholder = "輸入公司名稱",
                onValueChange = { company = it },
            )
            Spacer(Modifier.height(NnoworkSpacing.snug))
            PixelButton(onClick = { onUpdateCompany(company) }, backgroundColor = NnoworkTheme.colors.buttonBG, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Save, null, tint = NnoworkTheme.colors.onElevated)
                Spacer(Modifier.size(NnoworkSpacing.xs))
                Text("儲存公司名稱", style = pixelTextStyle(NnoworkTheme.type.body, FontWeight.Bold, NnoworkTheme.colors.onElevated))
            }
        }
        JobChangeSection(onOpen = onOpenJobTransition)
        SettingsSection(icon = Icons.Filled.Style, title = stringResource(com.neilyin.nnowork.R.string.theme_shop_title)) {
            Text("目標格數：${settings.maxStamps} 格，集滿後可在首頁建立下一張卡時更換。", style = pixelTextStyle(NnoworkTheme.type.small, color = NnoworkTheme.colors.secondaryText))
            Spacer(Modifier.height(NnoworkSpacing.sm))
            ThemeShopGrid(
                selectedThemeId = settings.cardStyle,
                unlockedStyles = settings.unlockedStyles,
                onSelect = onSelectTheme,
                onPreview = { theme ->
                    Analytics.log("theme_preview_opened", "theme_name" to theme.name)
                    themeToPreviewId = theme.id
                },
            )
        }
        SettingsSection(icon = Icons.Filled.Refresh, title = "常用蓋章原因") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                NnoworkField(
                    value = newReason,
                    onValueChange = { newReason = it },
                    placeholder = "輸入新原因...",
                    textStyle = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.size(NnoworkSpacing.xs))
                PixelButton(
                    onClick = {
                        onAddReason(newReason)
                        newReason = ""
                    },
                    enabled = newReason.isNotBlank(),
                    backgroundColor = if (newReason.isBlank()) NnoworkTheme.colors.disabledBG else NnoworkTheme.colors.success,
                    contentPadding = PaddingValues(NnoworkSpacing.sm),
                ) {
                    Icon(Icons.Filled.Add, null, tint = NnoworkTheme.colors.primaryText)
                }
            }
            Spacer(Modifier.height(NnoworkSpacing.snug))
            settings.commonReasons.forEach { reason ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(NnoworkRadius.field))
                        .background(NnoworkTheme.colors.appBG)
                        .border(NnoworkLine.hairline, NnoworkTheme.colors.divider, RoundedCornerShape(NnoworkRadius.field))
                        .padding(NnoworkSpacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(reason, style = pixelTextStyle(NnoworkTheme.type.body, FontWeight.Medium, NnoworkTheme.colors.primaryText), modifier = Modifier.weight(1f))
                    PixelButton(onClick = { onDeleteReason(reason) }, variant = PixelButtonVariant.Tonal, contentPadding = PaddingValues(7.dp)) {
                        Icon(Icons.Filled.Delete, "刪除原因", tint = NnoworkTheme.colors.danger, modifier = Modifier.size(18.dp))
                    }
                }
                Spacer(Modifier.height(NnoworkSpacing.xs))
            }
        }
        SettingsSection(icon = Icons.Filled.Timer, title = "小工具設定", modifier = Modifier.settingsColumn(SettingsColumn.Trailing, order = 0)) {
            Text("語錄刷新頻率", style = pixelTextStyle(NnoworkTheme.type.small, FontWeight.Bold, NnoworkTheme.colors.secondaryText))
            Spacer(Modifier.height(NnoworkSpacing.xs))
            // 雙欄時欄寬只有約 330dp：改成換行排列，不用橫捲才看得到後面的選項。手機維持原本的橫向捲動。
            val intervalButtons: @Composable () -> Unit = {
                listOf(5, 10, 30, 60, 180, 300, 480, 1440).forEach { minutes ->
                    PixelButton(
                        onClick = { onUpdateWidgetInterval(minutes) },
                        backgroundColor = if (settings.widgetRefreshInterval == minutes) NnoworkTheme.colors.highlight else NnoworkTheme.colors.disabledBG,
                        contentPadding = PaddingValues(horizontal = NnoworkSpacing.sm, vertical = NnoworkSpacing.snug),
                    ) {
                        Text(intervalLabel(minutes), style = pixelTextStyle(NnoworkTheme.type.small, FontWeight.Bold, NnoworkTheme.colors.primaryText))
                    }
                }
            }
            if (twoColumns) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(NnoworkSpacing.xs),
                    verticalArrangement = Arrangement.spacedBy(NnoworkSpacing.xs),
                ) { intervalButtons() }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(NnoworkSpacing.xs), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    intervalButtons()
                }
            }
            Spacer(Modifier.height(NnoworkSpacing.xs))
            Text("注意：Android 小工具實際刷新可能受系統省電策略影響。", style = pixelTextStyle(NnoworkTheme.type.small, color = NnoworkTheme.colors.secondaryText))
        }
        FeedbackSection(onSubmit = onSubmitFeedback, modifier = Modifier.settingsColumn(SettingsColumn.Trailing, order = 2))
        RateSection(modifier = Modifier.settingsColumn(SettingsColumn.Trailing, order = 2))
        BackupSection(
            modifier = Modifier.settingsColumn(SettingsColumn.Trailing, order = 3),
            state = backupUiState,
            onExport = onExportBackup,
            onPrepareImport = onPrepareBackupImport,
            onImport = onImportBackup,
            onDismissPreparedImport = onDismissPreparedBackupImport,
        )
        SettingsSection(icon = Icons.Filled.Campaign, title = "更新與公告", modifier = Modifier.settingsColumn(SettingsColumn.Trailing, order = 4)) {
            Text(
                "查看這次的資料保護更新、道歉啟事與後續說明。",
                style = pixelTextStyle(NnoworkTheme.type.small, color = NnoworkTheme.colors.secondaryText),
            )
            Spacer(Modifier.height(NnoworkSpacing.snug))
            PixelButton(
                onClick = onShowUpdateNotice,
                backgroundColor = NnoworkTheme.colors.disabledBG,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("查看本次更新", style = pixelTextStyle(NnoworkTheme.type.body, FontWeight.Bold, NnoworkTheme.colors.primaryText))
            }
        }
        SettingsSection(icon = Icons.Filled.RestartAlt, title = "重置", modifier = Modifier.settingsColumn(SettingsColumn.Trailing, order = 5)) {
            // 頁面上的破壞性入口用 tonal＋紅色 icon；真正的確認鈕在對話框裡才是實心紅。
            PixelButton(onClick = { showReset = true }, backgroundColor = NnoworkTheme.colors.disabledBG, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Delete, null, tint = NnoworkTheme.colors.danger)
                Spacer(Modifier.size(NnoworkSpacing.xs))
                Text(stringResource(com.neilyin.nnowork.R.string.settings_reset_action), style = pixelTextStyle(NnoworkTheme.type.bodyLarge, FontWeight.Bold, NnoworkTheme.colors.onElevated))
            }
        }
        // 彩蛋：點署名紀念 77（為 Android 版打下基底的夥伴）。
        var showTribute by rememberSaveable { mutableStateOf(false) }
        Text(
            "離職集點卡 Android ${BuildConfig.VERSION_NAME}\n" + stringResource(com.neilyin.nnowork.R.string.settings_credit),
            style = pixelTextStyle(NnoworkTheme.type.small, color = NnoworkTheme.colors.secondaryText),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .settingsColumn(SettingsColumn.Trailing, order = 6)
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null,
                ) { showTribute = true }
                .padding(top = NnoworkSpacing.xs, bottom = NnoworkSpacing.md),
        )
        if (showTribute) {
            Tribute77Dialog(onDismiss = { showTribute = false })
        }
    }
}

@Composable
private fun TreeHoleSafetySettingsSection(
    blockedAuthors: List<BlockedTreeHoleAuthor>,
    onUnblock: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val supportEmail = stringResource(com.neilyin.nnowork.R.string.treehole_support_email)

    SettingsSection(
        icon = Icons.Filled.Block,
        title = stringResource(com.neilyin.nnowork.R.string.treehole_blocked_users_title),
        modifier = modifier,
    ) {
        Text(
            stringResource(com.neilyin.nnowork.R.string.treehole_blocked_users_hint),
            style = pixelTextStyle(NnoworkTheme.type.small, color = NnoworkTheme.colors.secondaryText),
        )
        Spacer(Modifier.height(NnoworkSpacing.snug))
        if (blockedAuthors.isEmpty()) {
            Text(
                stringResource(com.neilyin.nnowork.R.string.treehole_blocked_users_empty),
                style = pixelTextStyle(NnoworkTheme.type.body, FontWeight.Bold, NnoworkTheme.colors.primaryText),
            )
        } else {
            blockedAuthors.forEach { author ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(NnoworkRadius.field))
                        .background(NnoworkTheme.colors.appBG)
                        .border(NnoworkLine.hairline, NnoworkTheme.colors.divider, RoundedCornerShape(NnoworkRadius.field))
                        .padding(NnoworkSpacing.snug),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(NnoworkSpacing.xs),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            author.nickname,
                            style = pixelTextStyle(NnoworkTheme.type.body, FontWeight.Bold, NnoworkTheme.colors.primaryText),
                        )
                        Text(
                            author.authorKey.take(8),
                            style = pixelTextStyle(NnoworkTheme.type.caption, color = NnoworkTheme.colors.secondaryText),
                        )
                    }
                    PixelButton(
                        onClick = { onUnblock(author.authorKey) },
                        backgroundColor = NnoworkTheme.colors.disabledBG,
                        contentPadding = PaddingValues(horizontal = NnoworkSpacing.snug, vertical = NnoworkSpacing.xs),
                    ) {
                        Text(
                            stringResource(com.neilyin.nnowork.R.string.treehole_unblock),
                            style = pixelTextStyle(NnoworkTheme.type.small, FontWeight.Bold, NnoworkTheme.colors.primaryText),
                        )
                    }
                }
                Spacer(Modifier.height(NnoworkSpacing.xs))
            }
            Text(
                stringResource(com.neilyin.nnowork.R.string.treehole_blocked_users_notice),
                style = pixelTextStyle(NnoworkTheme.type.caption, color = NnoworkTheme.colors.secondaryText),
            )
        }

        Spacer(Modifier.height(NnoworkSpacing.md))
        Text(
            stringResource(com.neilyin.nnowork.R.string.treehole_support_title),
            style = pixelTextStyle(NnoworkTheme.type.body, FontWeight.Bold, NnoworkTheme.colors.primaryText),
        )
        Text(
            stringResource(com.neilyin.nnowork.R.string.treehole_support_body),
            style = pixelTextStyle(NnoworkTheme.type.small, color = NnoworkTheme.colors.secondaryText),
        )
        Spacer(Modifier.height(NnoworkSpacing.xs))
        PixelButton(
            onClick = {
                runCatching {
                    context.startActivity(
                        Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$supportEmail")),
                    )
                }
            },
            backgroundColor = NnoworkTheme.colors.disabledBG,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Filled.Email, contentDescription = null, tint = NnoworkTheme.colors.onElevated)
            Spacer(Modifier.size(7.dp))
            Text(
                stringResource(com.neilyin.nnowork.R.string.treehole_support_email_action),
                style = pixelTextStyle(NnoworkTheme.type.body, FontWeight.Bold, NnoworkTheme.colors.onElevated),
            )
        }
    }
}

@Composable
private fun AccountSection(
    state: AuthUiState,
    googleOAuthUri: Uri,
    onSignIn: (String, String) -> Unit,
    onSignUp: (String, String) -> Unit,
    onSignOut: () -> Unit,
    onDeleteAccount: () -> Unit,
    onClearMessage: () -> Unit,
    onExportBackup: (Uri) -> Unit,
    deleteFailed: Boolean,
    onDismissDeleteFailure: () -> Unit,
) {
    var showAuth by rememberSaveable { mutableStateOf(false) }
    var registerMode by rememberSaveable { mutableStateOf(false) }
    // 兩段式確認（iOS）：第一段講清楚後果（含過往工作會一併刪除）並提供「先匯出備份」，第二段最終確認。
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    var finalConfirmDelete by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    val exportBeforeDelete = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri != null) onExportBackup(uri)
        // 匯出（或取消）後回到刪除確認，讓使用者決定要不要繼續。
        confirmDelete = true
    }

    if (showAuth && !state.isSignedIn) {
        AuthDialog(
            state = state,
            registerMode = registerMode,
            onModeChange = {
                registerMode = it
                onClearMessage()
            },
            onDismiss = {
                showAuth = false
                onClearMessage()
            },
            onSubmit = { email, password ->
                if (registerMode) onSignUp(email, password) else onSignIn(email, password)
            },
            onGoogle = {
                runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, googleOAuthUri)) }
            },
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { if (!state.isProcessing) confirmDelete = false },
            containerColor = NnoworkDialogDefaults.containerColor,
            shape = NnoworkDialogDefaults.shape,
            title = { Text(stringResource(com.neilyin.nnowork.R.string.account_delete_step1_title), style = pixelTextStyle(NnoworkTheme.type.title, FontWeight.Bold, NnoworkTheme.colors.onElevated)) },
            text = {
                Text(
                    stringResource(com.neilyin.nnowork.R.string.account_delete_step1_message),
                    style = pixelTextStyle(NnoworkTheme.type.body, color = NnoworkTheme.colors.onElevated),
                )
            },
            confirmButton = {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(NnoworkSpacing.xs),
                ) {
                    PixelButton(
                        onClick = {
                            confirmDelete = false
                            finalConfirmDelete = true
                        },
                        enabled = !state.isProcessing,
                        backgroundColor = NnoworkTheme.colors.danger,
                    ) {
                        Text(stringResource(com.neilyin.nnowork.R.string.account_delete_continue), style = pixelTextStyle(NnoworkTheme.type.body, FontWeight.Bold, NnoworkTheme.colors.onElevated))
                    }
                    PixelButton(
                        onClick = {
                            confirmDelete = false
                            runCatching { exportBeforeDelete.launch(BackupRepository.suggestedFileName()) }
                        },
                        enabled = !state.isProcessing,
                        variant = PixelButtonVariant.Outlined,
                    ) {
                        Text(stringResource(com.neilyin.nnowork.R.string.account_delete_export_first), style = pixelTextStyle(NnoworkTheme.type.body, FontWeight.Bold, NnoworkTheme.colors.primaryText))
                    }
                    PixelButton(onClick = { confirmDelete = false }, enabled = !state.isProcessing, backgroundColor = NnoworkTheme.colors.disabledBG) {
                        Text(stringResource(com.neilyin.nnowork.R.string.jobarchive_cancel), style = pixelTextStyle(NnoworkTheme.type.body, FontWeight.Bold, NnoworkTheme.colors.primaryText))
                    }
                }
            },
        )
    }

    if (finalConfirmDelete) {
        AlertDialog(
            onDismissRequest = { if (!state.isProcessing) finalConfirmDelete = false },
            containerColor = NnoworkDialogDefaults.containerColor,
            shape = NnoworkDialogDefaults.shape,
            title = { Text(stringResource(com.neilyin.nnowork.R.string.account_delete_step2_title), style = pixelTextStyle(NnoworkTheme.type.title, FontWeight.Bold, NnoworkTheme.colors.onElevated)) },
            text = {
                Text(
                    stringResource(com.neilyin.nnowork.R.string.account_delete_step2_message),
                    style = pixelTextStyle(NnoworkTheme.type.body, color = NnoworkTheme.colors.onElevated),
                )
            },
            confirmButton = {
                PixelButton(
                    onClick = {
                        onDeleteAccount()
                        finalConfirmDelete = false
                    },
                    enabled = !state.isProcessing,
                    backgroundColor = NnoworkTheme.colors.danger,
                ) {
                    Text(stringResource(com.neilyin.nnowork.R.string.account_delete_confirm), style = pixelTextStyle(NnoworkTheme.type.body, FontWeight.Bold, NnoworkTheme.colors.onElevated))
                }
            },
            dismissButton = {
                PixelButton(onClick = { finalConfirmDelete = false }, enabled = !state.isProcessing, backgroundColor = NnoworkTheme.colors.disabledBG) {
                    Text(stringResource(com.neilyin.nnowork.R.string.jobarchive_cancel), style = pixelTextStyle(NnoworkTheme.type.body, FontWeight.Bold, NnoworkTheme.colors.primaryText))
                }
            },
        )
    }

    if (deleteFailed) {
        AlertDialog(
            onDismissRequest = onDismissDeleteFailure,
            containerColor = NnoworkDialogDefaults.containerColor,
            shape = NnoworkDialogDefaults.shape,
            title = { Text(stringResource(com.neilyin.nnowork.R.string.account_delete_failed_title), style = pixelTextStyle(NnoworkTheme.type.title, FontWeight.Bold, NnoworkTheme.colors.onElevated)) },
            text = {
                Text(
                    stringResource(com.neilyin.nnowork.R.string.account_delete_failed_message),
                    style = pixelTextStyle(NnoworkTheme.type.body, color = NnoworkTheme.colors.onElevated),
                )
            },
            confirmButton = {
                PixelButton(onClick = onDismissDeleteFailure, backgroundColor = NnoworkTheme.colors.buttonBG) {
                    Text(stringResource(com.neilyin.nnowork.R.string.jobarchive_ok), style = pixelTextStyle(NnoworkTheme.type.body, FontWeight.Bold, NnoworkTheme.colors.onElevated))
                }
            },
        )
    }

    SettingsSection(icon = Icons.Filled.AccountCircle, title = "帳號與跨裝置") {
        if (state.isSignedIn) {
            Text("已登入", style = pixelTextStyle(NnoworkTheme.type.bodyLarge, FontWeight.Bold, NnoworkTheme.colors.success))
            state.email?.let {
                Spacer(Modifier.height(5.dp))
                Text(maskEmail(it), style = pixelTextStyle(NnoworkTheme.type.body, FontWeight.Bold, NnoworkTheme.colors.primaryText))
            }
            val providers = state.linkedProviders.ifEmpty { listOfNotNull(state.lastProvider) }
            if (providers.isNotEmpty()) {
                Spacer(Modifier.height(5.dp))
                Text(
                    "已綁定：${providers.joinToString("、") { authProviderName(it) }}",
                    style = pixelTextStyle(NnoworkTheme.type.small, color = NnoworkTheme.colors.secondaryText),
                )
            }
            Spacer(Modifier.height(NnoworkSpacing.sm))
            Row(horizontalArrangement = Arrangement.spacedBy(NnoworkSpacing.xs)) {
                PixelButton(onClick = onSignOut, enabled = !state.isProcessing, backgroundColor = NnoworkTheme.colors.disabledBG, modifier = Modifier.weight(1f)) {
                    Icon(Icons.AutoMirrored.Filled.Logout, null, tint = NnoworkTheme.colors.primaryText, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(5.dp))
                    Text("登出", style = pixelTextStyle(NnoworkTheme.type.body, FontWeight.Bold, NnoworkTheme.colors.primaryText))
                }
                PixelButton(onClick = { confirmDelete = true }, enabled = !state.isProcessing, backgroundColor = NnoworkTheme.colors.danger, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.PersonRemove, null, tint = NnoworkTheme.colors.onElevated, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(5.dp))
                    Text("刪除帳號", style = pixelTextStyle(NnoworkTheme.type.small, FontWeight.Bold, NnoworkTheme.colors.onElevated))
                }
            }
        } else {
            Text(
                "登入是為了讓樹洞資料日後能跨裝置找回；你也可以先不登入，其他功能照常使用。",
                style = pixelTextStyle(NnoworkTheme.type.small, color = NnoworkTheme.colors.secondaryText),
            )
            state.lastProvider?.let {
                Spacer(Modifier.height(NnoworkSpacing.tight))
                Text("上次使用 ${authProviderName(it)} 登入", style = pixelTextStyle(NnoworkTheme.type.small, FontWeight.Bold, NnoworkTheme.colors.info))
            }
            Spacer(Modifier.height(NnoworkSpacing.sm))
            PixelButton(onClick = { showAuth = true }, backgroundColor = NnoworkTheme.colors.buttonBG, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.AutoMirrored.Filled.Login, null, tint = NnoworkTheme.colors.onElevated)
                Spacer(Modifier.size(NnoworkSpacing.xs))
                Text("登入或建立帳號", style = pixelTextStyle(NnoworkTheme.type.body, FontWeight.Bold, NnoworkTheme.colors.onElevated))
            }
        }
        state.message?.let { message ->
            Spacer(Modifier.height(NnoworkSpacing.snug))
            Text(
                message,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                style = pixelTextStyle(NnoworkTheme.type.small, FontWeight.Bold, if (state.isError) NnoworkTheme.colors.danger else NnoworkTheme.colors.success),
            )
        }
    }
}

/** 設定頁「換工作」區塊（iOS jobChangeSection）：點進去開換工作畫面。 */
@Composable
private fun JobChangeSection(onOpen: () -> Unit) {
    SettingsSection(icon = Icons.Filled.Work, title = stringResource(com.neilyin.nnowork.R.string.settings_job_change_title)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(NnoworkRadius.field))
                .clickable(role = androidx.compose.ui.semantics.Role.Button, onClick = onOpen)
                .padding(vertical = NnoworkSpacing.xxs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Autorenew, null, tint = NnoworkTheme.colors.primaryText, modifier = Modifier.size(NnoworkSize.icon))
            Spacer(Modifier.size(NnoworkSpacing.sm))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(NnoworkSpacing.xxs)) {
                Text(
                    stringResource(com.neilyin.nnowork.R.string.jobarchive_transition_title),
                    style = pixelTextStyle(NnoworkTheme.type.body, FontWeight.Bold, NnoworkTheme.colors.primaryText),
                )
                Text(
                    stringResource(com.neilyin.nnowork.R.string.settings_job_change_subtitle),
                    style = pixelTextStyle(NnoworkTheme.type.caption, color = NnoworkTheme.colors.secondaryText),
                )
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = NnoworkTheme.colors.secondaryText)
        }
    }
}

@Composable
internal fun AuthDialog(
    state: AuthUiState,
    registerMode: Boolean,
    onModeChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    onSubmit: (String, String) -> Unit,
    onGoogle: () -> Unit,
) {
    var email by rememberSaveable { mutableStateOf("") }
    // 密碼刻意不存進 saved state（Bundle），重建後需重新輸入。
    var password by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = { if (!state.isProcessing) onDismiss() },
        containerColor = NnoworkDialogDefaults.containerColor,
        shape = NnoworkDialogDefaults.shape,
        title = { Text(if (registerMode) "建立雲端帳號" else "登入雲端帳號", style = pixelTextStyle(NnoworkTheme.type.title, FontWeight.Bold, NnoworkTheme.colors.onElevated)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(NnoworkSpacing.sm)) {
                Row(horizontalArrangement = Arrangement.spacedBy(NnoworkSpacing.xs)) {
                    PixelButton(onClick = { onModeChange(false) }, enabled = !state.isProcessing, backgroundColor = if (!registerMode) NnoworkTheme.colors.highlight else NnoworkTheme.colors.disabledBG, modifier = Modifier.weight(1f), contentPadding = PaddingValues(vertical = NnoworkSpacing.snug)) {
                        Text("登入", style = pixelTextStyle(NnoworkTheme.type.small, FontWeight.Bold, NnoworkTheme.colors.primaryText))
                    }
                    PixelButton(onClick = { onModeChange(true) }, enabled = !state.isProcessing, backgroundColor = if (registerMode) NnoworkTheme.colors.highlight else NnoworkTheme.colors.disabledBG, modifier = Modifier.weight(1f), contentPadding = PaddingValues(vertical = NnoworkSpacing.snug)) {
                        Text("註冊", style = pixelTextStyle(NnoworkTheme.type.small, FontWeight.Bold, NnoworkTheme.colors.primaryText))
                    }
                }
                LabeledTextField("Email", email, "name@example.com", { email = it })
                LabeledTextField("密碼", password, "至少 6 個字元", { password = it }, PasswordVisualTransformation())
                state.message?.let {
                    Text(it, style = pixelTextStyle(NnoworkTheme.type.small, FontWeight.Bold, if (state.isError) NnoworkTheme.colors.danger else NnoworkTheme.colors.success))
                }
                PixelButton(onClick = onGoogle, enabled = !state.isProcessing, backgroundColor = NnoworkTheme.colors.elevatedSurface, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.AccountCircle, null, tint = NnoworkTheme.colors.info)
                    Spacer(Modifier.size(NnoworkSpacing.xs))
                    Text("使用 Google 繼續", style = pixelTextStyle(NnoworkTheme.type.body, FontWeight.Bold, NnoworkTheme.colors.primaryText))
                }
            }
        },
        confirmButton = {
            PixelButton(
                onClick = { onSubmit(email, password) },
                enabled = email.isNotBlank() && password.length >= 6 && !state.isProcessing,
                backgroundColor = NnoworkTheme.colors.success,
            ) {
                Text(if (state.isProcessing) "處理中..." else if (registerMode) "建立帳號" else "登入", style = pixelTextStyle(NnoworkTheme.type.body, FontWeight.Bold, NnoworkTheme.colors.primaryText))
            }
        },
        dismissButton = {
            PixelButton(onClick = onDismiss, enabled = !state.isProcessing, backgroundColor = NnoworkTheme.colors.disabledBG) {
                Text("稍後再說", style = pixelTextStyle(NnoworkTheme.type.body, FontWeight.Bold, NnoworkTheme.colors.primaryText))
            }
        },
    )
}

private fun authProviderName(provider: String): String = when (provider.lowercase()) {
    "google" -> "Google"
    else -> "Email"
}

private fun maskEmail(email: String): String {
    val at = email.indexOf('@')
    if (at <= 1) return email
    return email.take(1) + "***" + email.substring(at)
}

@Composable
private fun ManualSection() {
    var showManual by rememberSaveable { mutableStateOf(false) }
    if (showManual) UserManualSheet(onDismiss = { showManual = false })
    SettingsSection(icon = Icons.AutoMirrored.Filled.MenuBook, title = stringResource(com.neilyin.nnowork.R.string.manual_title)) {
        val openDesc = stringResource(com.neilyin.nnowork.R.string.manual_open)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClickLabel = openDesc, role = Role.Button) { showManual = true },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(com.neilyin.nnowork.R.string.manual_entry_hint),
                style = pixelTextStyle(NnoworkTheme.type.body, FontWeight.Bold, NnoworkTheme.colors.primaryText),
                modifier = Modifier.weight(1f),
            )
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = NnoworkTheme.colors.secondaryText)
        }
    }
}

/** 給評分：開 Google Play 商店頁（iOS rateSection → App Store 評分頁）。 */
@Composable
private fun RateSection(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val openDesc = stringResource(com.neilyin.nnowork.R.string.settings_rate_open_desc)
    SettingsSection(icon = Icons.Filled.Star, title = stringResource(com.neilyin.nnowork.R.string.review_title), modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClickLabel = openDesc, role = Role.Button) { PlayStore.openListing(context) },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(NnoworkSpacing.xxs)) {
                Text(
                    stringResource(com.neilyin.nnowork.R.string.review_action),
                    style = pixelTextStyle(NnoworkTheme.type.body, FontWeight.Bold, NnoworkTheme.colors.primaryText),
                )
                Text(
                    stringResource(com.neilyin.nnowork.R.string.review_message),
                    style = pixelTextStyle(NnoworkTheme.type.small, color = NnoworkTheme.colors.secondaryText),
                )
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = NnoworkTheme.colors.secondaryText)
        }
    }
}

@Composable
private fun FeedbackSection(onSubmit: (String, (String) -> Unit) -> Unit, modifier: Modifier = Modifier) {
    var text by rememberSaveable { mutableStateOf("") }
    var message by rememberSaveable { mutableStateOf<String?>(null) }
    var sending by remember { mutableStateOf(false) }
    SettingsSection(icon = Icons.Filled.Favorite, title = "分享使用心得", modifier = modifier) {
        Text("把對 App 的想法或建議丟給我，審核通過後會送你金幣。", style = pixelTextStyle(NnoworkTheme.type.small, color = NnoworkTheme.colors.secondaryText))
        Spacer(Modifier.height(NnoworkSpacing.snug))
        NnoworkField(
            value = text,
            onValueChange = { text = it.take(300) },
            placeholder = "想說什麼都可以...",
            singleLine = false,
            minHeight = 90.dp,
            textStyle = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(NnoworkSpacing.tight))
        Text(
            "送出時會在回饋最後自動加上「$ANDROID_FEEDBACK_SOURCE」",
            style = pixelTextStyle(NnoworkTheme.type.small, FontWeight.Bold, NnoworkTheme.colors.secondaryText),
        )
        Spacer(Modifier.height(NnoworkSpacing.xs))
        message?.let {
            Text(it, style = pixelTextStyle(NnoworkTheme.type.small, FontWeight.Bold, if (it.startsWith("已")) NnoworkTheme.colors.success else NnoworkTheme.colors.highlight))
            Spacer(Modifier.height(NnoworkSpacing.xs))
        }
        PixelButton(
            onClick = {
                sending = true
                onSubmit(text) { result ->
                    message = result
                    sending = false
                    if (result.startsWith("已")) text = ""
                }
            },
            enabled = text.trim().length >= 5 && !sending,
            backgroundColor = if (text.trim().length >= 5) NnoworkTheme.colors.success else NnoworkTheme.colors.disabledBG,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.AutoMirrored.Filled.Send, null, tint = NnoworkTheme.colors.primaryText, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(NnoworkSpacing.xs))
            Text(if (sending) "送出中..." else "送出心得", style = pixelTextStyle(NnoworkTheme.type.body, FontWeight.Bold, NnoworkTheme.colors.primaryText))
        }
    }
}

@Composable
private fun BackupSection(
    state: BackupUiState,
    onExport: (Uri) -> Unit,
    onPrepareImport: (Uri) -> Unit,
    onImport: () -> Unit,
    onDismissPreparedImport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri != null) onExport(uri)
    }
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) onPrepareImport(uri)
    }

    state.pendingImport?.let { prepared ->
        AlertDialog(
            onDismissRequest = { if (!state.inProgress) onDismissPreparedImport() },
            containerColor = NnoworkDialogDefaults.containerColor,
            shape = NnoworkDialogDefaults.shape,
            title = {
                Text("確定要匯入這份備份？", style = pixelTextStyle(NnoworkTheme.type.title, FontWeight.Bold, NnoworkTheme.colors.onElevated))
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(NnoworkSpacing.snug)) {
                    Text(
                        "備份格式 v${prepared.schemaVersion}，包含 ${prepared.stampCount} 筆集點紀錄。",
                        style = pixelTextStyle(NnoworkTheme.type.body, color = NnoworkTheme.colors.onElevated),
                    )
                    Text(
                        "匯入會以備份內容取代目前的集點紀錄、離職計畫與 App 設定。裝置身份與樹洞遠端貼文不會被取代。匯入進行時請保持 App 開啟。",
                        style = pixelTextStyle(NnoworkTheme.type.small, color = NnoworkTheme.colors.secondaryText),
                    )
                }
            },
            confirmButton = {
                PixelButton(
                    onClick = onImport,
                    enabled = !state.inProgress,
                    backgroundColor = NnoworkTheme.colors.danger,
                ) {
                    Text(
                        if (state.inProgress) "匯入中..." else "取代並匯入",
                        style = pixelTextStyle(NnoworkTheme.type.body, FontWeight.Bold, NnoworkTheme.colors.onElevated),
                    )
                }
            },
            dismissButton = {
                PixelButton(
                    onClick = onDismissPreparedImport,
                    enabled = !state.inProgress,
                    backgroundColor = NnoworkTheme.colors.disabledBG,
                ) {
                    Text("取消", style = pixelTextStyle(NnoworkTheme.type.body, FontWeight.Bold, NnoworkTheme.colors.primaryText))
                }
            },
        )
    }

    SettingsSection(icon = Icons.Filled.FileDownload, title = "資料備份與移轉", modifier = modifier) {
        Text(
            "將集點紀錄、離職計畫、薪水設定與 App 設定存成版本化 JSON 檔案。",
            style = pixelTextStyle(NnoworkTheme.type.small, color = NnoworkTheme.colors.secondaryText),
        )
        Spacer(Modifier.height(NnoworkSpacing.snug))
        PixelButton(
            onClick = { exportLauncher.launch(BackupRepository.suggestedFileName()) },
            enabled = !state.inProgress,
            backgroundColor = NnoworkTheme.colors.success,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Filled.FileDownload, contentDescription = null, tint = NnoworkTheme.colors.primaryText)
            Spacer(Modifier.size(NnoworkSpacing.xs))
            Text(
                if (state.inProgress) "處理中..." else "匯出備份",
                style = pixelTextStyle(NnoworkTheme.type.body, FontWeight.Bold, NnoworkTheme.colors.primaryText),
            )
        }
        Spacer(Modifier.height(9.dp))
        PixelButton(
            onClick = { importLauncher.launch(arrayOf("application/json", "text/plain")) },
            enabled = !state.inProgress,
            backgroundColor = NnoworkTheme.colors.disabledBG,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Filled.FileUpload, contentDescription = null, tint = NnoworkTheme.colors.onElevated)
            Spacer(Modifier.size(NnoworkSpacing.xs))
            Text("匯入備份", style = pixelTextStyle(NnoworkTheme.type.body, FontWeight.Bold, NnoworkTheme.colors.onElevated))
        }
        state.statusMessage?.let { message ->
            Spacer(Modifier.height(NnoworkSpacing.snug))
            Text(
                message,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                style = pixelTextStyle(
                    NnoworkTheme.type.small,
                    FontWeight.Bold,
                    if (state.statusIsError) NnoworkTheme.colors.danger else NnoworkTheme.colors.success,
                ),
            )
        }
        Spacer(Modifier.height(NnoworkSpacing.xs))
        Text(
            "提醒：匯入會全刪全換目前資料。備份檔可能含公司名稱與私人記錄，請先匯出現況並妥善保管。",
            style = pixelTextStyle(NnoworkTheme.type.small, color = NnoworkTheme.colors.secondaryText),
        )
    }
}

private fun Throwable.backupErrorMessage(prefix: String): String = when (this) {
    is BackupException -> "$prefix：備份檔格式或內容不正確，原有資料沒有變更。"
    is BackupWriteException -> "$prefix：寫入未完成，App 已嘗試保留原有資料。"
    is IOException -> "$prefix：無法存取所選檔案，請確認空間或檔案權限。"
    else -> "$prefix：${message?.takeIf { it.isNotBlank() } ?: "請稍後再試"}。"
}

private const val MAX_BACKUP_JSON_CHARS = 16 * 1_048_576

private fun Reader.readTextLimited(maxChars: Int): String {
    val output = StringBuilder(minOf(maxChars, 64 * 1_024))
    val buffer = CharArray(8 * 1_024)
    while (true) {
        val count = read(buffer)
        if (count < 0) return output.toString()
        if (output.length + count > maxChars) {
            throw IOException("Backup file exceeds the supported size")
        }
        output.append(buffer, 0, count)
    }
}

@Composable
private fun CoinSection(coins: Int) {
    SettingsSection(icon = Icons.Filled.AttachMoney, title = "離職金幣") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(NnoworkRadius.card))
                    .background(NnoworkTheme.colors.accentSoft),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.MonetizationOn,
                    contentDescription = "離職金幣",
                    tint = NnoworkTheme.colors.accentForeground,
                    modifier = Modifier.size(30.dp),
                )
            }
            Spacer(Modifier.size(NnoworkSpacing.snug))
            Column {
                Text(coins.toString(), style = pixelTextStyle(NnoworkTheme.type.pageTitle, FontWeight.Bold, NnoworkTheme.colors.primaryText, monospace = true))
                Text("每日蓋章 +1，一天上限一枚", style = pixelTextStyle(NnoworkTheme.type.small, color = NnoworkTheme.colors.secondaryText))
            }
        }
    }
}

@Composable
private fun SettingsSection(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(NnoworkSpacing.xs)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = NnoworkTheme.colors.accentForeground, modifier = Modifier.size(22.dp))
            Spacer(Modifier.size(NnoworkSpacing.xs))
            Text(
                title,
                style = pixelTextStyle(NnoworkTheme.type.subtitle, FontWeight.Bold, NnoworkTheme.colors.primaryText),
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .pixelCard()
                .padding(NnoworkSpacing.md),
            content = content,
        )
    }
}

@Composable
private fun LabeledTextField(
    label: String,
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    visualTransformation: VisualTransformation = VisualTransformation.None,
) {
    NnoworkTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        placeholder = placeholder,
        visualTransformation = visualTransformation,
    )
}

private fun intervalLabel(minutes: Int): String = when (minutes) {
    60 -> "1 小時"
    180 -> "3 小時"
    300 -> "5 小時"
    480 -> "8 小時"
    1440 -> "每天"
    else -> "$minutes 分鐘"
}

@Preview(name = "設定", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun SettingsContentPreview() {
    SettingsContent(
        settings = AppSettings(
            companyName = "血汗科技",
            cardStyle = CardTheme.SAKURA.id,
            maxStamps = 20,
            widgetRefreshInterval = 60,
            coinCount = 22,
            unlockedStyles = listOf(CardTheme.RPG.id, CardTheme.CYBERPUNK.id, CardTheme.VACATION.id, CardTheme.SAKURA.id),
            commonReasons = listOf("無效會議", "需求一直改", "加班沒錢", "老闆廢話太多"),
        ),
        backupUiState = BackupUiState(),
        authState = AuthUiState(),
        blockedAuthors = emptyList(),
        googleOAuthUri = Uri.parse("https://example.com"),
        onUpdateCompany = {},
        onSelectTheme = {},
        onUnlockTheme = {},
        onAddReason = {},
        onDeleteReason = {},
        onUpdateWidgetInterval = {},
        onResetAll = {},
        onSubmitFeedback = { _, _ -> },
        onShowUpdateNotice = {},
        onExportBackup = {},
        onPrepareBackupImport = {},
        onImportBackup = {},
        onDismissPreparedBackupImport = {},
        onAuthSignIn = { _, _ -> },
        onAuthSignUp = { _, _ -> },
        onAuthSignOut = {},
        onDeleteAuthAccount = {},
        onClearAuthMessage = {},
        onUnblockTreeHoleAuthor = {},
    )
}

/**
 * 寬螢幕設定分兩欄時，區塊放哪一欄（iOS SettingsView：左＝帳號、公司、卡片、主題、原因；右＝小工具、安全、回饋、備份、重置、版本）。
 * Header＝橫跨兩欄的頁面標題，兩欄都從標題下方開始。
 */
private enum class SettingsColumn { Header, Leading, Trailing }

private data class SettingsColumnData(val column: SettingsColumn, val order: Int) : ParentDataModifier {
    override fun Density.modifyParentData(parentData: Any?): Any = this@SettingsColumnData
}

/** 標記區塊在雙欄時的欄位與欄內順序；單欄時忽略，維持原本順序。 */
private fun Modifier.settingsColumn(column: SettingsColumn, order: Int = 0): Modifier =
    this.then(SettingsColumnData(column, order))

/**
 * 設定頁的欄位排版：單欄＝像 Column(spacedBy) 依序往下排（手機外觀不變）；
 * 雙欄＝Header 橫跨頂端、未標記的放左欄、標記 Trailing 的依 order 放右欄，左右兩欄並排、一起捲動。
 * 兩種版面共用同一棵 composition，切換版面時各區塊內的 rememberSaveable 狀態不會遺失。
 * 分界走 [twoPaneSplit]（有摺痕時對準摺痕）。
 */
@Composable
private fun SettingsColumns(
    twoColumns: Boolean,
    spacing: Dp,
    modifier: Modifier = Modifier,
    columnGap: Dp = NnoworkSpacing.xl,
    content: @Composable () -> Unit,
) {
    val fold = LocalNnoworkWindowInfo.current.fold
    var containerStart by remember { mutableStateOf(0.dp) }
    val density = LocalDensity.current
    Layout(
        content = content,
        modifier = modifier.onGloballyPositioned { coords ->
            containerStart = with(density) { coords.positionInWindow().x.toDp() }
        },
    ) { measurables, constraints ->
        val gapPx = spacing.roundToPx()
        val itemConstraints = constraints.copy(minWidth = 0, minHeight = 0)
        if (!twoColumns) {
            val placeables = measurables.map { it.measure(itemConstraints) }
            val height = placeables.sumOf { it.height } + gapPx * (placeables.size - 1).coerceAtLeast(0)
            return@Layout layout(constraints.maxWidth, height.coerceAtLeast(constraints.minHeight)) {
                var y = 0
                placeables.forEach { placeable ->
                    placeable.placeRelative(0, y)
                    y += placeable.height + gapPx
                }
            }
        }
        val split = twoPaneSplit(
            containerWidth = constraints.maxWidth.toDp(),
            containerStart = containerStart,
            spacing = columnGap,
            fold = fold,
        )
        val leadingWidth = split.leading.roundToPx()
        val trailingX = leadingWidth + split.gap.roundToPx()
        val trailingWidth = (constraints.maxWidth - trailingX).coerceAtLeast(0)
        val indexed = measurables.mapIndexed { index, m -> index to m }
        fun columnOf(m: androidx.compose.ui.layout.Measurable) =
            (m.parentData as? SettingsColumnData)?.column ?: SettingsColumn.Leading
        val headerPlaceables = indexed
            .filter { columnOf(it.second) == SettingsColumn.Header }
            .map { it.second.measure(itemConstraints) }
        val leading = indexed.filter { columnOf(it.second) == SettingsColumn.Leading }
        val trailing = indexed
            .filter { (it.second.parentData as? SettingsColumnData)?.column == SettingsColumn.Trailing }
            .sortedBy { (it.second.parentData as SettingsColumnData).order }
        val leadingPlaceables = leading.map { it.second.measure(itemConstraints.copy(maxWidth = leadingWidth)) }
        val trailingPlaceables = trailing.map { it.second.measure(itemConstraints.copy(maxWidth = trailingWidth)) }
        fun columnHeight(list: List<androidx.compose.ui.layout.Placeable>) =
            list.sumOf { it.height } + gapPx * (list.size - 1).coerceAtLeast(0)
        val headerHeight = headerPlaceables.sumOf { it.height + gapPx }
        val height = headerHeight + maxOf(columnHeight(leadingPlaceables), columnHeight(trailingPlaceables))
        layout(constraints.maxWidth, height.coerceAtLeast(constraints.minHeight)) {
            var y = 0
            headerPlaceables.forEach { placeable ->
                placeable.placeRelative(0, y)
                y += placeable.height + gapPx
            }
            leadingPlaceables.forEach { placeable ->
                placeable.placeRelative(0, y)
                y += placeable.height + gapPx
            }
            y = headerHeight
            trailingPlaceables.forEach { placeable ->
                placeable.placeRelative(trailingX, y)
                y += placeable.height + gapPx
            }
        }
    }
}
