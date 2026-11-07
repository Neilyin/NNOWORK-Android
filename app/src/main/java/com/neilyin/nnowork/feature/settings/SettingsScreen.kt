package com.neilyin.nnowork.feature.settings

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
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
import androidx.compose.ui.unit.sp
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
import com.neilyin.nnowork.ui.theme.PixelButton
import com.neilyin.nnowork.ui.theme.PixelColors
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
) : ViewModel() {
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
    fun resetAll() {
        viewModelScope.launch {
            stampDao.deleteAll()
            planRepository.reset()
            settingsRepository.update { AppSettings() }
            coinLedgerRepository.reset()
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
        viewModelScope.launch { authRepository.deleteAccount() }
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
    )
}

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
) {
    var company by remember(settings.companyName) { mutableStateOf(settings.companyName) }
    var newReason by remember { mutableStateOf("") }
    var showReset by remember { mutableStateOf(false) }
    var themeToPreview by remember { mutableStateOf<CardTheme?>(null) }

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
                themeToPreview = null
            },
        )
    }

    if (showReset) {
        AlertDialog(
            onDismissRequest = { showReset = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("確定要全部重來？", style = pixelTextStyle(20.sp, FontWeight.Bold, PixelColors.white)) },
            text = { Text("這會刪除集點紀錄、離職計畫與設定。此操作無法復原。", style = pixelTextStyle(14.sp, color = PixelColors.gray)) },
            confirmButton = {
                PixelButton(
                    onClick = {
                        onResetAll()
                        showReset = false
                    },
                    backgroundColor = PixelColors.red,
                ) {
                    Text("確定重置", style = pixelTextStyle(14.sp, FontWeight.Bold, PixelColors.white))
                }
            },
            dismissButton = {
                PixelButton(onClick = { showReset = false }, backgroundColor = PixelColors.gray) {
                    Text("取消", style = pixelTextStyle(14.sp, FontWeight.Bold, PixelColors.black))
                }
            },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Text("設定", style = pixelTextStyle(30.sp, FontWeight.Bold, MaterialTheme.colorScheme.onBackground))
        ManualSection()
        AccountSection(
            state = authState,
            onSignIn = onAuthSignIn,
            onSignUp = onAuthSignUp,
            onSignOut = onAuthSignOut,
            onDeleteAccount = onDeleteAuthAccount,
            onClearMessage = onClearAuthMessage,
            googleOAuthUri = googleOAuthUri,
        )
        CoinSection(settings.coinCount)
        TreeHoleSafetySettingsSection(
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
            Spacer(Modifier.height(10.dp))
            PixelButton(onClick = { onUpdateCompany(company) }, backgroundColor = PixelColors.blue, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Save, null, tint = PixelColors.white)
                Spacer(Modifier.size(8.dp))
                Text("儲存公司名稱", style = pixelTextStyle(15.sp, FontWeight.Bold, PixelColors.white))
            }
        }
        SettingsSection(icon = Icons.Filled.Style, title = stringResource(com.neilyin.nnowork.R.string.theme_shop_title)) {
            Text("目標格數：${settings.maxStamps} 格，集滿後可在首頁建立下一張卡時更換。", style = pixelTextStyle(13.sp, color = PixelColors.gray))
            Spacer(Modifier.height(12.dp))
            ThemeShopGrid(
                selectedThemeId = settings.cardStyle,
                unlockedStyles = settings.unlockedStyles,
                onSelect = onSelectTheme,
                onPreview = { theme ->
                    Analytics.log("theme_preview_opened", "theme_name" to theme.name)
                    themeToPreview = theme
                },
            )
        }
        SettingsSection(icon = Icons.Filled.Refresh, title = "常用蓋章原因") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BasicTextField(
                    value = newReason,
                    onValueChange = { newReason = it },
                    singleLine = true,
                    textStyle = pixelTextStyle(15.sp, FontWeight.Medium, PixelColors.black),
                    cursorBrush = SolidColor(PixelColors.black),
                    modifier = Modifier
                        .weight(1f)
                        .background(PixelColors.white)
                        .border(3.dp, PixelColors.black, RectangleShape)
                        .padding(12.dp),
                    decorationBox = { inner ->
                        if (newReason.isEmpty()) Text("輸入新原因...", style = pixelTextStyle(14.sp, color = PixelColors.gray))
                        inner()
                    },
                )
                Spacer(Modifier.size(8.dp))
                PixelButton(
                    onClick = {
                        onAddReason(newReason)
                        newReason = ""
                    },
                    enabled = newReason.isNotBlank(),
                    backgroundColor = if (newReason.isBlank()) PixelColors.gray else PixelColors.green,
                    contentPadding = PaddingValues(12.dp),
                ) {
                    Icon(Icons.Filled.Add, null, tint = PixelColors.black)
                }
            }
            Spacer(Modifier.height(10.dp))
            settings.commonReasons.forEach { reason ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(PixelColors.white)
                        .border(2.dp, PixelColors.black, RectangleShape)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(reason, style = pixelTextStyle(15.sp, FontWeight.Medium, PixelColors.black), modifier = Modifier.weight(1f))
                    PixelButton(onClick = { onDeleteReason(reason) }, backgroundColor = PixelColors.red, contentPadding = PaddingValues(7.dp)) {
                        Icon(Icons.Filled.Delete, null, tint = PixelColors.white, modifier = Modifier.size(18.dp))
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
        SettingsSection(icon = Icons.Filled.Timer, title = "小工具設定") {
            Text("語錄刷新頻率", style = pixelTextStyle(13.sp, FontWeight.Bold, PixelColors.gray))
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                listOf(5, 10, 30, 60, 180, 300, 480, 1440).forEach { minutes ->
                    PixelButton(
                        onClick = { onUpdateWidgetInterval(minutes) },
                        backgroundColor = if (settings.widgetRefreshInterval == minutes) PixelColors.yellow else PixelColors.gray,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                    ) {
                        Text(intervalLabel(minutes), style = pixelTextStyle(13.sp, FontWeight.Bold, PixelColors.black))
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text("注意：Android 小工具實際刷新可能受系統省電策略影響。", style = pixelTextStyle(12.sp, color = PixelColors.gray))
        }
        FeedbackSection(onSubmit = onSubmitFeedback)
        BackupSection(
            state = backupUiState,
            onExport = onExportBackup,
            onPrepareImport = onPrepareBackupImport,
            onImport = onImportBackup,
            onDismissPreparedImport = onDismissPreparedBackupImport,
        )
        SettingsSection(icon = Icons.Filled.Campaign, title = "更新與公告") {
            Text(
                "查看這次的資料保護更新、道歉啟事與後續說明。",
                style = pixelTextStyle(13.sp, color = PixelColors.gray),
            )
            Spacer(Modifier.height(10.dp))
            PixelButton(
                onClick = onShowUpdateNotice,
                backgroundColor = PixelColors.yellow,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("查看本次更新", style = pixelTextStyle(15.sp, FontWeight.Bold, PixelColors.black))
            }
        }
        SettingsSection(icon = Icons.Filled.RestartAlt, title = "重置") {
            PixelButton(onClick = { showReset = true }, backgroundColor = PixelColors.red, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Delete, null, tint = PixelColors.white)
                Spacer(Modifier.size(8.dp))
                Text("全部重來", style = pixelTextStyle(16.sp, FontWeight.Bold, PixelColors.white))
            }
        }
        Text(
            "離職集點卡 Android ${BuildConfig.VERSION_NAME}\nMade By Neil 尼歐",
            style = pixelTextStyle(12.sp, color = PixelColors.gray),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 16.dp),
        )
    }
}

@Composable
private fun TreeHoleSafetySettingsSection(
    blockedAuthors: List<BlockedTreeHoleAuthor>,
    onUnblock: (String) -> Unit,
) {
    val context = LocalContext.current
    val supportEmail = stringResource(com.neilyin.nnowork.R.string.treehole_support_email)

    SettingsSection(
        icon = Icons.Filled.Block,
        title = stringResource(com.neilyin.nnowork.R.string.treehole_blocked_users_title),
    ) {
        Text(
            stringResource(com.neilyin.nnowork.R.string.treehole_blocked_users_hint),
            style = pixelTextStyle(13.sp, color = PixelColors.gray),
        )
        Spacer(Modifier.height(10.dp))
        if (blockedAuthors.isEmpty()) {
            Text(
                stringResource(com.neilyin.nnowork.R.string.treehole_blocked_users_empty),
                style = pixelTextStyle(14.sp, FontWeight.Bold, PixelColors.black),
            )
        } else {
            blockedAuthors.forEach { author ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(PixelColors.white)
                        .border(2.dp, PixelColors.black, RectangleShape)
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            author.nickname,
                            style = pixelTextStyle(14.sp, FontWeight.Bold, PixelColors.black),
                        )
                        Text(
                            author.authorKey.take(8),
                            style = pixelTextStyle(10.sp, color = PixelColors.gray),
                        )
                    }
                    PixelButton(
                        onClick = { onUnblock(author.authorKey) },
                        backgroundColor = PixelColors.gray,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                    ) {
                        Text(
                            stringResource(com.neilyin.nnowork.R.string.treehole_unblock),
                            style = pixelTextStyle(12.sp, FontWeight.Bold, PixelColors.black),
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
            Text(
                stringResource(com.neilyin.nnowork.R.string.treehole_blocked_users_notice),
                style = pixelTextStyle(11.sp, color = PixelColors.gray),
            )
        }

        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(com.neilyin.nnowork.R.string.treehole_support_title),
            style = pixelTextStyle(15.sp, FontWeight.Bold, PixelColors.black),
        )
        Text(
            stringResource(com.neilyin.nnowork.R.string.treehole_support_body),
            style = pixelTextStyle(12.sp, color = PixelColors.gray),
        )
        Spacer(Modifier.height(8.dp))
        PixelButton(
            onClick = {
                runCatching {
                    context.startActivity(
                        Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$supportEmail")),
                    )
                }
            },
            backgroundColor = PixelColors.blue,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Filled.Email, contentDescription = null, tint = PixelColors.white)
            Spacer(Modifier.size(7.dp))
            Text(
                stringResource(com.neilyin.nnowork.R.string.treehole_support_email_action),
                style = pixelTextStyle(14.sp, FontWeight.Bold, PixelColors.white),
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
) {
    var showAuth by remember { mutableStateOf(false) }
    var registerMode by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val context = LocalContext.current

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
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("刪除雲端帳號？", style = pixelTextStyle(20.sp, FontWeight.Bold, PixelColors.white)) },
            text = {
                Text(
                    "Supabase 登入身份會永久刪除。本機集點紀錄會保留，樹洞資料會解除帳號綁定並回到匿名裝置身份。",
                    style = pixelTextStyle(14.sp, color = PixelColors.white),
                )
            },
            confirmButton = {
                PixelButton(
                    onClick = {
                        onDeleteAccount()
                        confirmDelete = false
                    },
                    enabled = !state.isProcessing,
                    backgroundColor = PixelColors.red,
                ) {
                    Text("永久刪除帳號", style = pixelTextStyle(14.sp, FontWeight.Bold, PixelColors.white))
                }
            },
            dismissButton = {
                PixelButton(onClick = { confirmDelete = false }, enabled = !state.isProcessing, backgroundColor = PixelColors.gray) {
                    Text("取消", style = pixelTextStyle(14.sp, FontWeight.Bold, PixelColors.black))
                }
            },
        )
    }

    SettingsSection(icon = Icons.Filled.AccountCircle, title = "帳號與跨裝置") {
        if (state.isSignedIn) {
            Text("已登入", style = pixelTextStyle(16.sp, FontWeight.Bold, PixelColors.green))
            state.email?.let {
                Spacer(Modifier.height(5.dp))
                Text(maskEmail(it), style = pixelTextStyle(14.sp, FontWeight.Bold, PixelColors.black))
            }
            val providers = state.linkedProviders.ifEmpty { listOfNotNull(state.lastProvider) }
            if (providers.isNotEmpty()) {
                Spacer(Modifier.height(5.dp))
                Text(
                    "已綁定：${providers.joinToString("、") { authProviderName(it) }}",
                    style = pixelTextStyle(13.sp, color = PixelColors.gray),
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PixelButton(onClick = onSignOut, enabled = !state.isProcessing, backgroundColor = PixelColors.gray, modifier = Modifier.weight(1f)) {
                    Icon(Icons.AutoMirrored.Filled.Logout, null, tint = PixelColors.black, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(5.dp))
                    Text("登出", style = pixelTextStyle(14.sp, FontWeight.Bold, PixelColors.black))
                }
                PixelButton(onClick = { confirmDelete = true }, enabled = !state.isProcessing, backgroundColor = PixelColors.red, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.PersonRemove, null, tint = PixelColors.white, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(5.dp))
                    Text("刪除帳號", style = pixelTextStyle(13.sp, FontWeight.Bold, PixelColors.white))
                }
            }
        } else {
            Text(
                "登入是為了讓樹洞資料日後能跨裝置找回；你也可以先不登入，其他功能照常使用。",
                style = pixelTextStyle(13.sp, color = PixelColors.gray),
            )
            state.lastProvider?.let {
                Spacer(Modifier.height(6.dp))
                Text("上次使用 ${authProviderName(it)} 登入", style = pixelTextStyle(12.sp, FontWeight.Bold, PixelColors.blue))
            }
            Spacer(Modifier.height(12.dp))
            PixelButton(onClick = { showAuth = true }, backgroundColor = PixelColors.blue, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.AutoMirrored.Filled.Login, null, tint = PixelColors.white)
                Spacer(Modifier.size(8.dp))
                Text("登入或建立帳號", style = pixelTextStyle(15.sp, FontWeight.Bold, PixelColors.white))
            }
        }
        state.message?.let { message ->
            Spacer(Modifier.height(10.dp))
            Text(
                message,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                style = pixelTextStyle(13.sp, FontWeight.Bold, if (state.isError) PixelColors.red else PixelColors.green),
            )
        }
    }
}

@Composable
private fun AuthDialog(
    state: AuthUiState,
    registerMode: Boolean,
    onModeChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    onSubmit: (String, String) -> Unit,
    onGoogle: () -> Unit,
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = { if (!state.isProcessing) onDismiss() },
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text(if (registerMode) "建立雲端帳號" else "登入雲端帳號", style = pixelTextStyle(20.sp, FontWeight.Bold, PixelColors.white)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PixelButton(onClick = { onModeChange(false) }, enabled = !state.isProcessing, backgroundColor = if (!registerMode) PixelColors.yellow else PixelColors.gray, modifier = Modifier.weight(1f), contentPadding = PaddingValues(vertical = 10.dp)) {
                        Text("登入", style = pixelTextStyle(13.sp, FontWeight.Bold, PixelColors.black))
                    }
                    PixelButton(onClick = { onModeChange(true) }, enabled = !state.isProcessing, backgroundColor = if (registerMode) PixelColors.yellow else PixelColors.gray, modifier = Modifier.weight(1f), contentPadding = PaddingValues(vertical = 10.dp)) {
                        Text("註冊", style = pixelTextStyle(13.sp, FontWeight.Bold, PixelColors.black))
                    }
                }
                LabeledTextField("Email", email, "name@example.com", { email = it })
                LabeledTextField("密碼", password, "至少 6 個字元", { password = it }, PasswordVisualTransformation())
                state.message?.let {
                    Text(it, style = pixelTextStyle(13.sp, FontWeight.Bold, if (state.isError) PixelColors.red else PixelColors.green))
                }
                PixelButton(onClick = onGoogle, enabled = !state.isProcessing, backgroundColor = PixelColors.white, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.AccountCircle, null, tint = PixelColors.blue)
                    Spacer(Modifier.size(8.dp))
                    Text("使用 Google 繼續", style = pixelTextStyle(14.sp, FontWeight.Bold, PixelColors.black))
                }
            }
        },
        confirmButton = {
            PixelButton(
                onClick = { onSubmit(email, password) },
                enabled = email.isNotBlank() && password.length >= 6 && !state.isProcessing,
                backgroundColor = PixelColors.green,
            ) {
                Text(if (state.isProcessing) "處理中..." else if (registerMode) "建立帳號" else "登入", style = pixelTextStyle(14.sp, FontWeight.Bold, PixelColors.black))
            }
        },
        dismissButton = {
            PixelButton(onClick = onDismiss, enabled = !state.isProcessing, backgroundColor = PixelColors.gray) {
                Text("稍後再說", style = pixelTextStyle(14.sp, FontWeight.Bold, PixelColors.black))
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

private val manualChapters = listOf(
    "集點卡" to "每天蓋一個章，記下想離職的原因。一張卡有 N 格（可在下方設定），集滿後可選擇「我要離職」產生離職收據，或「再來一張」。當天蓋章後會出現隱藏彩蛋，可額外多蓋（不計每日上限）。每天蓋章 +1 金幣。",
    "離職計畫" to "設定自由預備金目標與目前已存，追蹤進度；標記履歷是否就緒；新增待辦事項並可設定提醒（到時間會發本機通知）。內含薪水計時器。",
    "薪水計時器（BETA）" to "設定月薪／時薪、上下班時間、午休、發薪日（可多選）。排休方式可選「見紅休」或「排班制」；排班制可用固定工時或多種班別，並在月曆上逐日指派。即時顯示今日已賺、下班倒數與本期累積。",
    "離職紀錄" to "查看總蓋章數、使用卡片數與最常見原因，並依每張卡片列出蓋章日期與原因。",
    "取暖樹洞" to "匿名職場心情社群。先建立樹洞身份（暱稱、產業、職稱）。蓋章後可選擇把原因分享到樹洞，審核通過後公開並 +5 金幣。每天可玩「今日心聲」翻卡：右滑讚、左滑加油、上滑哭、下滑跳過。",
    "金幣與主題" to "金幣可在「集點卡設定」的卡片風格商店解鎖付費主題。賺金幣：每天蓋章、樹洞貼文通過審核、分享使用心得通過審核。",
    "桌面小工具" to "可在桌面加入薪水計時器與離職語錄小工具。語錄刷新頻率可於「小工具設定」調整（實際刷新受系統省電策略影響）。",
)

@Composable
private fun ManualSection() {
    var expanded by remember { mutableStateOf(false) }
    var openChapter by remember { mutableStateOf(-1) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.AutoMirrored.Filled.MenuBook, null, tint = PixelColors.yellow, modifier = Modifier.size(22.dp))
            Spacer(Modifier.size(8.dp))
            Text("使用手冊", style = pixelTextStyle(18.sp, FontWeight.Bold, MaterialTheme.colorScheme.onBackground))
        }
        Column(
            modifier = Modifier.fillMaxWidth().pixelCard().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("第一次用？點開看看每個功能怎麼玩", style = pixelTextStyle(14.sp, FontWeight.Bold, PixelColors.black), modifier = Modifier.weight(1f))
                Icon(if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, null, tint = PixelColors.black)
            }
            if (expanded) {
                manualChapters.forEachIndexed { index, (title, body) ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .pixelCard(
                                backgroundColor = PixelColors.white,
                                borderColor = PixelColors.black,
                                borderWidth = 2.dp,
                                elevation = 2.dp,
                            )
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { openChapter = if (openChapter == index) -1 else index },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(title, style = pixelTextStyle(14.sp, FontWeight.Bold, PixelColors.black), modifier = Modifier.weight(1f))
                            Icon(if (openChapter == index) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, null, tint = PixelColors.gray, modifier = Modifier.size(18.dp))
                        }
                        if (openChapter == index) {
                            Text(body, style = pixelTextStyle(13.sp, color = PixelColors.darkGray))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FeedbackSection(onSubmit: (String, (String) -> Unit) -> Unit) {
    var text by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var sending by remember { mutableStateOf(false) }
    SettingsSection(icon = Icons.Filled.Favorite, title = "分享使用心得") {
        Text("把對 App 的想法或建議丟給我，審核通過後會送你金幣。", style = pixelTextStyle(13.sp, color = PixelColors.gray))
        Spacer(Modifier.height(10.dp))
        BasicTextField(
            value = text,
            onValueChange = { text = it.take(300) },
            textStyle = pixelTextStyle(15.sp, FontWeight.Medium, PixelColors.black),
            cursorBrush = SolidColor(PixelColors.black),
            modifier = Modifier.fillMaxWidth().height(90.dp).background(PixelColors.white).border(3.dp, PixelColors.black, RectangleShape).padding(12.dp),
            decorationBox = { inner -> if (text.isEmpty()) Text("想說什麼都可以...", style = pixelTextStyle(14.sp, color = PixelColors.gray)); inner() },
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "送出時會在回饋最後自動加上「$ANDROID_FEEDBACK_SOURCE」",
            style = pixelTextStyle(12.sp, FontWeight.Bold, PixelColors.gray),
        )
        Spacer(Modifier.height(8.dp))
        message?.let {
            Text(it, style = pixelTextStyle(13.sp, FontWeight.Bold, if (it.startsWith("已")) PixelColors.green else PixelColors.yellow))
            Spacer(Modifier.height(8.dp))
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
            backgroundColor = if (text.trim().length >= 5) PixelColors.green else PixelColors.gray,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.AutoMirrored.Filled.Send, null, tint = PixelColors.black, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(8.dp))
            Text(if (sending) "送出中..." else "送出心得", style = pixelTextStyle(15.sp, FontWeight.Bold, PixelColors.black))
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
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text("確定要匯入這份備份？", style = pixelTextStyle(20.sp, FontWeight.Bold, PixelColors.white))
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "備份格式 v${prepared.schemaVersion}，包含 ${prepared.stampCount} 筆集點紀錄。",
                        style = pixelTextStyle(14.sp, color = PixelColors.white),
                    )
                    Text(
                        "匯入會以備份內容取代目前的集點紀錄、離職計畫與 App 設定。裝置身份與樹洞遠端貼文不會被取代。匯入進行時請保持 App 開啟。",
                        style = pixelTextStyle(13.sp, color = PixelColors.gray),
                    )
                }
            },
            confirmButton = {
                PixelButton(
                    onClick = onImport,
                    enabled = !state.inProgress,
                    backgroundColor = PixelColors.red,
                ) {
                    Text(
                        if (state.inProgress) "匯入中..." else "取代並匯入",
                        style = pixelTextStyle(14.sp, FontWeight.Bold, PixelColors.white),
                    )
                }
            },
            dismissButton = {
                PixelButton(
                    onClick = onDismissPreparedImport,
                    enabled = !state.inProgress,
                    backgroundColor = PixelColors.gray,
                ) {
                    Text("取消", style = pixelTextStyle(14.sp, FontWeight.Bold, PixelColors.black))
                }
            },
        )
    }

    SettingsSection(icon = Icons.Filled.FileDownload, title = "資料備份與移轉") {
        Text(
            "將集點紀錄、離職計畫、薪水設定與 App 設定存成版本化 JSON 檔案。",
            style = pixelTextStyle(13.sp, color = PixelColors.gray),
        )
        Spacer(Modifier.height(10.dp))
        PixelButton(
            onClick = { exportLauncher.launch(BackupRepository.suggestedFileName()) },
            enabled = !state.inProgress,
            backgroundColor = PixelColors.green,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Filled.FileDownload, contentDescription = null, tint = PixelColors.black)
            Spacer(Modifier.size(8.dp))
            Text(
                if (state.inProgress) "處理中..." else "匯出備份",
                style = pixelTextStyle(15.sp, FontWeight.Bold, PixelColors.black),
            )
        }
        Spacer(Modifier.height(9.dp))
        PixelButton(
            onClick = { importLauncher.launch(arrayOf("application/json", "text/plain")) },
            enabled = !state.inProgress,
            backgroundColor = PixelColors.blue,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Filled.FileUpload, contentDescription = null, tint = PixelColors.white)
            Spacer(Modifier.size(8.dp))
            Text("匯入備份", style = pixelTextStyle(15.sp, FontWeight.Bold, PixelColors.white))
        }
        state.statusMessage?.let { message ->
            Spacer(Modifier.height(10.dp))
            Text(
                message,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                style = pixelTextStyle(
                    13.sp,
                    FontWeight.Bold,
                    if (state.statusIsError) PixelColors.red else PixelColors.green,
                ),
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "提醒：匯入會全刪全換目前資料。備份檔可能含公司名稱與私人記錄，請先匯出現況並妥善保管。",
            style = pixelTextStyle(12.sp, color = PixelColors.gray),
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
                    .pixelCard(
                        backgroundColor = PixelColors.yellow,
                        borderColor = PixelColors.black,
                        elevation = 3.dp,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.MonetizationOn,
                    contentDescription = "離職金幣",
                    tint = PixelColors.black,
                    modifier = Modifier.size(30.dp),
                )
            }
            Spacer(Modifier.size(10.dp))
            Column {
                Text(coins.toString(), style = pixelTextStyle(28.sp, FontWeight.Bold, PixelColors.black))
                Text("每日蓋章 +1，一天上限一枚", style = pixelTextStyle(12.sp, color = PixelColors.gray))
            }
        }
    }
}

@Composable
private fun SettingsSection(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(22.dp))
            Spacer(Modifier.size(8.dp))
            Text(
                title,
                style = pixelTextStyle(18.sp, FontWeight.Bold, MaterialTheme.colorScheme.onBackground),
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .pixelCard()
                .padding(16.dp),
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
