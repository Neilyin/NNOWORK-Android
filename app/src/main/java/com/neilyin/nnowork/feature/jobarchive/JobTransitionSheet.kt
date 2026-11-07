package com.neilyin.nnowork.feature.jobarchive

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.SyncProblem
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.neilyin.nnowork.R
import com.neilyin.nnowork.analytics.Analytics
import com.neilyin.nnowork.data.auth.AuthRepository
import com.neilyin.nnowork.data.auth.AuthUiState
import com.neilyin.nnowork.data.jobarchive.JobArchiveRepository
import com.neilyin.nnowork.data.local.StampDao
import com.neilyin.nnowork.data.settings.SettingsRepository
import com.neilyin.nnowork.di.ServiceLocator
import com.neilyin.nnowork.feature.settings.AuthDialog
import com.neilyin.nnowork.notify.ReminderScheduler
import com.neilyin.nnowork.ui.theme.NnoworkDialogDefaults
import com.neilyin.nnowork.ui.theme.NnoworkLine
import com.neilyin.nnowork.ui.theme.NnoworkRadius
import com.neilyin.nnowork.ui.theme.NnoworkSheetDefaults
import com.neilyin.nnowork.ui.theme.NnoworkSize
import com.neilyin.nnowork.ui.theme.NnoworkSpacing
import com.neilyin.nnowork.ui.theme.NnoworkTextField
import com.neilyin.nnowork.ui.theme.NnoworkTheme
import com.neilyin.nnowork.ui.theme.PixelButton
import com.neilyin.nnowork.ui.theme.PixelButtonVariant
import com.neilyin.nnowork.ui.theme.pixelCard
import com.neilyin.nnowork.ui.theme.pixelTextStyle
import com.neilyin.nnowork.widget.WidgetScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 目前這份工作的摘要（「目前的工作」卡片與不保存確認文案用）。 */
data class CurrentJobSummary(
    val companyName: String = "",
    val stampCount: Int = 0,
    val cardCount: Int = 0,
)

class JobTransitionViewModel(
    private val jobArchiveRepository: JobArchiveRepository = ServiceLocator.jobArchiveRepository,
    private val authRepository: AuthRepository = ServiceLocator.authRepository,
    settingsRepository: SettingsRepository = ServiceLocator.settingsRepository,
    stampDao: StampDao = ServiceLocator.stampDao,
) : ViewModel() {
    private val controller = JobTransitionController(
        actions = object : JobTransitionActions {
            override suspend fun archiveAndStartNewJob(newCompanyName: String, maxStamps: Int): List<String> =
                jobArchiveRepository.archiveAndStartNewJob(newCompanyName, maxStamps).clearedReminderTodoIds

            override suspend fun startNewJobWithoutArchive(newCompanyName: String, maxStamps: Int): List<String> =
                jobArchiveRepository.startNewJobWithoutArchive(newCompanyName, maxStamps)

            override suspend fun hasUnfinishedUploadedArchive(): Boolean =
                jobArchiveRepository.hasUnfinishedUploadedArchive()
        },
        cancelReminders = { ids -> ReminderScheduler.cancelAll(ServiceLocator.applicationContext, ids) },
    )
    val state: StateFlow<JobTransitionUiState> = controller.state
    val authState: StateFlow<AuthUiState> = authRepository.state
    val googleOAuthUri: Uri get() = authRepository.googleOAuthUri
    val currentJob: StateFlow<CurrentJobSummary> =
        combine(settingsRepository.settings, stampDao.observeAll()) { settings, stamps ->
            CurrentJobSummary(
                companyName = settings.companyName,
                stampCount = stamps.size,
                cardCount = stamps.map { it.roundNumber }.toSet().size,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CurrentJobSummary())

    /** 未登入時按「綁定帳號並保存這段紀錄」：登入成功後自動接續封存（iOS handleAuthSheetDismissed）。 */
    private var archiveAfterSignIn = false

    init {
        viewModelScope.launch {
            authRepository.state.map { it.isSignedIn }.distinctUntilChanged().collect { signedIn ->
                if (signedIn && archiveAfterSignIn) {
                    archiveAfterSignIn = false
                    controller.load()
                    archive()
                }
            }
        }
    }

    /** 每次打開畫面：重新讀「上次換工作未完成」提示。 */
    fun onOpened() {
        viewModelScope.launch { controller.load() }
    }

    /** 畫面關閉（取消或完成）：清掉輸入與待接續的登入，下次打開是全新的畫面。 */
    fun reset() {
        archiveAfterSignIn = false
        controller.reset()
    }

    fun updateCompanyName(name: String) = controller.updateCompanyName(name)
    fun selectMaxStamps(count: Int) = controller.selectMaxStamps(count)
    fun requestDiscard() = controller.requestDiscard()
    fun dismissDiscard() = controller.dismissDiscard()
    fun dismissError() = controller.dismissError()

    fun confirmDiscard() {
        viewModelScope.launch {
            controller.confirmDiscard()
            onFinishedIfCompleted("discard")
        }
    }

    fun archive() {
        viewModelScope.launch {
            controller.archive()
            onFinishedIfCompleted("archive")
        }
    }

    fun requestSignInThenArchive() {
        archiveAfterSignIn = true
        authRepository.clearMessage()
    }

    fun cancelSignIn() {
        archiveAfterSignIn = false
        authRepository.clearMessage()
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch { authRepository.signIn(email, password) }
    }

    fun signUp(email: String, password: String) {
        viewModelScope.launch { authRepository.signUp(email, password) }
    }

    private suspend fun onFinishedIfCompleted(kind: String) {
        if (!controller.state.value.completed) return
        Analytics.log("job_transition_completed", "kind" to kind)
        runCatching { WidgetScheduler.refreshNow(ServiceLocator.applicationContext) }
    }
}

/**
 * 「我換工作了」畫面（iOS `JobTransitionSheet`）。設定頁「換工作」與自由畫面「重新集點（新公司）」共用。
 * 新工作開始（封存或不保存）後呼叫 [onCompleted]；[onDismiss] 是使用者取消。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobTransitionSheet(
    onDismiss: () -> Unit,
    onCompleted: () -> Unit = onDismiss,
    // 固定 key：Activity 只會留一個實例，關閉時 reset()；旋轉螢幕時輸入仍保留。
    viewModel: JobTransitionViewModel = viewModel(key = JOB_TRANSITION_VIEW_MODEL_KEY),
) {
    val close: () -> Unit = {
        viewModel.reset()
        onDismiss()
    }
    val complete: () -> Unit = {
        viewModel.reset()
        onCompleted()
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val currentJob by viewModel.currentJob.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showAuth by remember { mutableStateOf(false) }
    var registerMode by remember { mutableStateOf(false) }
    val processing by rememberUpdatedState(state.processing)
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { it != SheetValue.Hidden || !processing },
    )

    var showVerifyHint by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { viewModel.onOpened() }
    LaunchedEffect(state.completed) {
        if (state.completed) complete()
    }
    LaunchedEffect(authState.isSignedIn) {
        if (authState.isSignedIn) showAuth = false
    }

    ModalBottomSheet(
        onDismissRequest = { if (!state.processing) close() },
        sheetState = sheetState,
        shape = NnoworkSheetDefaults.shape,
        containerColor = NnoworkSheetDefaults.containerColor,
        scrimColor = NnoworkSheetDefaults.scrimColor,
        dragHandle = { NnoworkSheetDefaults.DragHandle() },
    ) {
        JobTransitionContent(
            state = state,
            currentJob = currentJob,
            isSignedIn = authState.isSignedIn,
            onCompanyNameChange = viewModel::updateCompanyName,
            onSelectMaxStamps = viewModel::selectMaxStamps,
            onArchive = viewModel::archive,
            onLinkAccount = {
                registerMode = false
                viewModel.requestSignInThenArchive()
                showAuth = true
            },
            onRequestDiscard = viewModel::requestDiscard,
            onCancel = close,
            modifier = Modifier.fillMaxHeight(0.92f),
        )
    }

    if (state.showDiscardConfirm) {
        JobTransitionDiscardDialog(
            companyName = currentJob.companyName,
            stampCount = currentJob.stampCount,
            onConfirm = viewModel::confirmDiscard,
            onDismiss = viewModel::dismissDiscard,
        )
    }
    if (showVerifyHint) {
        AlertDialog(
            onDismissRequest = { showVerifyHint = false },
            containerColor = NnoworkDialogDefaults.containerColor,
            shape = NnoworkDialogDefaults.shape,
            title = { Text(stringResource(R.string.jobarchive_verify_email_title), style = pixelTextStyle(NnoworkTheme.type.title, FontWeight.Bold, NnoworkTheme.colors.onElevated)) },
            text = { Text(stringResource(R.string.jobarchive_verify_email_message), style = pixelTextStyle(NnoworkTheme.type.body, color = NnoworkTheme.colors.onElevated)) },
            confirmButton = {
                PixelButton(onClick = { showVerifyHint = false }, variant = PixelButtonVariant.Primary) {
                    Text(stringResource(R.string.jobarchive_ok), style = pixelTextStyle(NnoworkTheme.type.body, FontWeight.Bold, NnoworkTheme.colors.primaryText))
                }
            },
        )
    }
    state.error?.let { error ->
        JobTransitionErrorDialog(error = error, serverMessage = state.serverMessage, onDismiss = viewModel::dismissError)
    }
    if (showAuth && !authState.isSignedIn) {
        AuthDialog(
            state = authState,
            registerMode = registerMode,
            onModeChange = { registerMode = it },
            onDismiss = {
                showAuth = false
                // iOS handleAuthSheetDismissed：Email 註冊後還沒驗證 → 提示驗證完再按一次（目前的卡都還在）。
                if (authState.awaitingEmailVerification) showVerifyHint = true
                viewModel.cancelSignIn()
            },
            onSubmit = { email, password ->
                if (registerMode) viewModel.signUp(email, password) else viewModel.signIn(email, password)
            },
            onGoogle = {
                runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, viewModel.googleOAuthUri)) }
            },
        )
    }
}

@Composable
fun JobTransitionContent(
    state: JobTransitionUiState,
    currentJob: CurrentJobSummary,
    isSignedIn: Boolean,
    onCompanyNameChange: (String) -> Unit,
    onSelectMaxStamps: (Int) -> Unit,
    onArchive: () -> Unit,
    onLinkAccount: () -> Unit,
    onRequestDiscard: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = NnoworkTheme.colors
    Box(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(start = NnoworkSpacing.page, end = NnoworkSpacing.page, bottom = NnoworkSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(NnoworkSpacing.md),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.jobarchive_transition_title),
                    style = pixelTextStyle(NnoworkTheme.type.title, FontWeight.Bold, colors.onElevated),
                    modifier = Modifier.weight(1f),
                )
                PixelButton(
                    onClick = onCancel,
                    enabled = !state.processing,
                    variant = PixelButtonVariant.Tonal,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = NnoworkSpacing.md,
                        vertical = NnoworkSpacing.xs,
                    ),
                ) {
                    Text(stringResource(R.string.jobarchive_cancel), style = pixelTextStyle(NnoworkTheme.type.body, FontWeight.Bold, NnoworkTheme.colors.primaryText))
                }
            }

            // 說明
            Row(
                modifier = Modifier.fillMaxWidth().pixelCard().padding(NnoworkSpacing.md),
                verticalAlignment = Alignment.Top,
            ) {
                Icon(Icons.Filled.Work, null, tint = colors.accentForeground, modifier = Modifier.size(NnoworkSize.icon))
                Spacer(Modifier.size(NnoworkSpacing.sm))
                Column(verticalArrangement = Arrangement.spacedBy(NnoworkSpacing.tight)) {
                    Text(stringResource(R.string.jobarchive_intro_title), style = pixelTextStyle(NnoworkTheme.type.subtitle, FontWeight.Bold, NnoworkTheme.colors.primaryText))
                    Text(stringResource(R.string.jobarchive_intro_body), style = pixelTextStyle(NnoworkTheme.type.small, color = colors.secondaryText))
                }
            }

            // 目前的工作
            Column(
                modifier = Modifier.fillMaxWidth().pixelCard().padding(NnoworkSpacing.md),
                verticalArrangement = Arrangement.spacedBy(NnoworkSpacing.xs),
            ) {
                Text(stringResource(R.string.jobarchive_current_job), style = pixelTextStyle(NnoworkTheme.type.small, FontWeight.Bold, colors.secondaryText))
                Text(
                    currentJob.companyName.ifBlank { stringResource(R.string.jobarchive_company_fallback) },
                    style = pixelTextStyle(NnoworkTheme.type.subtitle, FontWeight.Bold, NnoworkTheme.colors.primaryText),
                )
                Text(
                    stringResource(R.string.jobarchive_current_job_counts, currentJob.stampCount, currentJob.cardCount),
                    style = pixelTextStyle(NnoworkTheme.type.small, color = colors.secondaryText),
                )
            }

            // 新工作
            Column(
                modifier = Modifier.fillMaxWidth().pixelCard().padding(NnoworkSpacing.md),
                verticalArrangement = Arrangement.spacedBy(NnoworkSpacing.sm),
            ) {
                NnoworkTextField(
                    value = state.companyName,
                    onValueChange = onCompanyNameChange,
                    label = stringResource(R.string.jobarchive_new_company),
                    placeholder = stringResource(R.string.jobarchive_new_company_placeholder),
                    enabled = !state.processing,
                )
                Text(stringResource(R.string.jobarchive_new_card_size), style = pixelTextStyle(NnoworkTheme.type.small, FontWeight.Bold, colors.secondaryText))
                Row(horizontalArrangement = Arrangement.spacedBy(NnoworkSpacing.snug)) {
                    JobTransitionController.MAX_STAMP_OPTIONS.forEach { count ->
                        val selected = state.maxStamps == count
                        PixelButton(
                            onClick = { onSelectMaxStamps(count) },
                            enabled = !state.processing,
                            variant = if (selected) PixelButtonVariant.Primary else PixelButtonVariant.Tonal,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(
                                stringResource(R.string.jobarchive_slots, count),
                                style = pixelTextStyle(NnoworkTheme.type.body, FontWeight.Bold, NnoworkTheme.colors.primaryText),
                            )
                        }
                    }
                }
            }

            // 動作
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(NnoworkSpacing.sm),
            ) {
                if (isSignedIn) {
                    if (state.hasUnfinishedArchive) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(colors.accentSoft, RoundedCornerShape(NnoworkRadius.field))
                                .border(NnoworkLine.control, colors.accent, RoundedCornerShape(NnoworkRadius.field))
                                .padding(NnoworkSpacing.sm),
                            verticalAlignment = Alignment.Top,
                        ) {
                            Icon(Icons.Filled.SyncProblem, null, tint = NnoworkTheme.colors.primaryText, modifier = Modifier.size(NnoworkSize.compactIcon))
                            Spacer(Modifier.size(NnoworkSpacing.xs))
                            Text(stringResource(R.string.jobarchive_unfinished_hint), style = pixelTextStyle(NnoworkTheme.type.small, FontWeight.Bold, NnoworkTheme.colors.primaryText))
                        }
                    }
                    Text(
                        stringResource(R.string.jobarchive_archive_needs_network),
                        style = pixelTextStyle(NnoworkTheme.type.caption, color = colors.secondaryText),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    PixelButton(
                        onClick = onArchive,
                        enabled = state.canProceed,
                        variant = PixelButtonVariant.Primary,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Filled.Inventory2, null, tint = NnoworkTheme.colors.primaryText, modifier = Modifier.size(NnoworkSize.compactIcon))
                        Spacer(Modifier.size(NnoworkSpacing.xs))
                        Text(stringResource(R.string.jobarchive_archive_action), style = pixelTextStyle(NnoworkTheme.type.bodyLarge, FontWeight.Bold, NnoworkTheme.colors.primaryText))
                    }
                    // 已登入者也保留「不保存」的退路，但做成次要的文字按鈕（iOS discardTextButton）。
                    PixelButton(
                        onClick = onRequestDiscard,
                        enabled = state.canProceed,
                        backgroundColor = androidx.compose.ui.graphics.Color.Transparent,
                        borderColor = androidx.compose.ui.graphics.Color.Transparent,
                        variant = PixelButtonVariant.Custom,
                    ) {
                        Text(
                            stringResource(R.string.jobarchive_discard_action),
                            style = pixelTextStyle(NnoworkTheme.type.small, FontWeight.Bold, colors.danger)
                                .copy(textDecoration = TextDecoration.Underline),
                        )
                    }
                } else {
                    Text(
                        stringResource(R.string.jobarchive_link_account_hint),
                        style = pixelTextStyle(NnoworkTheme.type.caption, color = colors.secondaryText),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    PixelButton(
                        onClick = onLinkAccount,
                        enabled = state.canProceed,
                        variant = PixelButtonVariant.Primary,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Login, null, tint = NnoworkTheme.colors.primaryText, modifier = Modifier.size(NnoworkSize.compactIcon))
                        Spacer(Modifier.size(NnoworkSpacing.xs))
                        Text(stringResource(R.string.jobarchive_link_account_action), style = pixelTextStyle(NnoworkTheme.type.body, FontWeight.Bold, NnoworkTheme.colors.primaryText))
                    }
                    PixelButton(
                        onClick = onRequestDiscard,
                        enabled = state.canProceed,
                        variant = PixelButtonVariant.Outlined,
                        borderColor = colors.danger,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Filled.Delete, null, tint = colors.danger, modifier = Modifier.size(NnoworkSize.compactIcon))
                        Spacer(Modifier.size(NnoworkSpacing.xs))
                        Text(stringResource(R.string.jobarchive_discard_action), style = pixelTextStyle(NnoworkTheme.type.body, FontWeight.Bold, colors.danger))
                    }
                }
                if (state.trimmedName.isEmpty()) {
                    Text(stringResource(R.string.jobarchive_enter_company_first), style = pixelTextStyle(NnoworkTheme.type.caption, color = colors.secondaryText))
                }
            }
        }

        if (state.processing) {
            Box(
                modifier = Modifier.matchParentSize().background(colors.scrim),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier.pixelCard(backgroundColor = colors.elevatedSurface).padding(NnoworkSpacing.lg),
                ) {
                    CircularProgressIndicator(color = colors.accentForeground)
                }
            }
        }
    }
}

/** 「確定不保存這段紀錄？」——唯一一條不經封存就刪章的路徑，必須使用者明確按下確認。 */
@Composable
fun JobTransitionDiscardDialog(
    companyName: String,
    stampCount: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val company = companyName.ifBlank { stringResource(R.string.jobarchive_company_fallback) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NnoworkDialogDefaults.containerColor,
        shape = NnoworkDialogDefaults.shape,
        title = { Text(stringResource(R.string.jobarchive_discard_confirm_title), style = pixelTextStyle(NnoworkTheme.type.title, FontWeight.Bold, NnoworkTheme.colors.onElevated)) },
        text = {
            Text(
                stringResource(R.string.jobarchive_discard_confirm_message, company, stampCount),
                style = pixelTextStyle(NnoworkTheme.type.body, color = NnoworkTheme.colors.onElevated),
            )
        },
        confirmButton = {
            PixelButton(onClick = onConfirm, variant = PixelButtonVariant.Danger) {
                Text(stringResource(R.string.jobarchive_discard_action), style = pixelTextStyle(NnoworkTheme.type.body, FontWeight.Bold, NnoworkTheme.colors.onElevated))
            }
        },
        dismissButton = {
            PixelButton(onClick = onDismiss, variant = PixelButtonVariant.Tonal) {
                Text(stringResource(R.string.jobarchive_cancel), style = pixelTextStyle(NnoworkTheme.type.body, FontWeight.Bold, NnoworkTheme.colors.primaryText))
            }
        },
    )
}

@Composable
private fun JobTransitionErrorDialog(error: JobTransitionError, serverMessage: String?, onDismiss: () -> Unit) {
    val message = when (error) {
        JobTransitionError.NETWORK -> stringResource(R.string.jobarchive_error_network)
        JobTransitionError.NOT_SIGNED_IN -> stringResource(R.string.jobarchive_error_not_signed_in)
        JobTransitionError.SERVER_NOT_READY -> stringResource(R.string.jobarchive_error_server_not_ready)
        JobTransitionError.SERVER -> stringResource(R.string.jobarchive_error_server, serverMessage.orEmpty())
        JobTransitionError.LOCAL_SAVE -> stringResource(R.string.jobarchive_error_local_save)
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NnoworkDialogDefaults.containerColor,
        shape = NnoworkDialogDefaults.shape,
        title = { Text(stringResource(R.string.jobarchive_error_title), style = pixelTextStyle(NnoworkTheme.type.title, FontWeight.Bold, NnoworkTheme.colors.onElevated)) },
        text = { Text(message, style = pixelTextStyle(NnoworkTheme.type.body, color = NnoworkTheme.colors.onElevated)) },
        confirmButton = {
            PixelButton(onClick = onDismiss, variant = PixelButtonVariant.Primary) {
                Text(stringResource(R.string.jobarchive_ok), style = pixelTextStyle(NnoworkTheme.type.body, FontWeight.Bold, NnoworkTheme.colors.primaryText))
            }
        },
    )
}

private const val JOB_TRANSITION_VIEW_MODEL_KEY = "job-transition"
