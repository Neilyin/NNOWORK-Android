package com.neilyin.nnowork.navigation

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.material3.MaterialTheme
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.neilyin.nnowork.data.settings.AppSettings
import com.neilyin.nnowork.data.settings.SettingsRepository
import com.neilyin.nnowork.analytics.Analytics
import com.neilyin.nnowork.di.ServiceLocator
import com.neilyin.nnowork.feature.onboarding.OnboardingScreen
import com.neilyin.nnowork.feature.onboarding.ReturningUserScreen
import com.neilyin.nnowork.feature.onboarding.WelcomeBackScreen
import androidx.compose.ui.Alignment
import com.neilyin.nnowork.feature.update.UpdateBannerHost
import com.neilyin.nnowork.feature.update.UPDATE_NOTICE_VERSION
import com.neilyin.nnowork.feature.update.AuthBindingGuideScreen
import com.neilyin.nnowork.feature.update.UpdateNoticeScreen
import com.neilyin.nnowork.ui.adaptive.ProvideNnoworkWindowInfo
import com.neilyin.nnowork.ui.theme.CardTheme
import com.neilyin.nnowork.ui.theme.NnoworkTheme
import com.neilyin.nnowork.ui.theme.appWideTheme
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.IOException

class RootViewModel(
    private val settingsRepository: SettingsRepository = ServiceLocator.settingsRepository,
) : ViewModel() {
    /** null = 載入中。 */
    val settings: StateFlow<AppSettings?> =
        settingsRepository.settings
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val hasStoredSettings: StateFlow<Boolean?> = settingsRepository.hasStoredSettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    private val _restoreState = MutableStateFlow(RestoreUiState())
    val restoreState = _restoreState.asStateFlow()
    val authState = ServiceLocator.authRepository.state

    fun markUpdateNoticeSeen() {
        viewModelScope.launch {
            settingsRepository.update { it.copy(lastSeenNoticeVersion = UPDATE_NOTICE_VERSION) }
            Analytics.log("version_notice_dismissed", "version" to UPDATE_NOTICE_VERSION)
        }
    }

    fun markAuthGuideSeen() {
        viewModelScope.launch {
            settingsRepository.update { it.copy(authGuideShownForV340 = true) }
        }
    }

    fun importBackup(uri: Uri) {
        if (_restoreState.value.processing) return
        viewModelScope.launch {
            _restoreState.value = RestoreUiState(processing = true)
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    val previousPlan = ServiceLocator.planRepository.current()
                    val resolver = ServiceLocator.applicationContext.contentResolver
                    val json = resolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { reader ->
                        val text = reader.readText()
                        if (text.length > MAX_BACKUP_CHARS) throw IOException("Backup is too large")
                        text
                    } ?: throw IOException("Unable to open backup")
                    previousPlan to ServiceLocator.backupRepository.importJson(json)
                }
            }
            result.onSuccess { (previousPlan, _) ->
                withContext(Dispatchers.IO) {
                    val context = ServiceLocator.applicationContext
                    previousPlan.todoItems.forEach { item ->
                        com.neilyin.nnowork.notify.ReminderScheduler.cancel(context, item.id)
                    }
                    ServiceLocator.planRepository.current().todoItems.forEach { item ->
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
            _restoreState.value = result.fold(
                onSuccess = { RestoreUiState(message = "資料已成功還原。") },
                onFailure = { RestoreUiState(message = "匯入失敗，原有資料沒有變更。", isError = true) },
            )
        }
    }

    private companion object {
        const val MAX_BACKUP_CHARS = 16 * 1_048_576
    }
}

data class RestoreUiState(
    val processing: Boolean = false,
    val message: String? = null,
    val isError: Boolean = false,
)

@Composable
fun AppRoot(viewModel: RootViewModel = viewModel()) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val hasStoredSettings by viewModel.hasStoredSettings.collectAsStateWithLifecycle()
    val restoreState by viewModel.restoreState.collectAsStateWithLifecycle()
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    var manualNotice by rememberSaveable { mutableStateOf(false) }
    var dismissedInSession by rememberSaveable { mutableStateOf(false) }
    var welcomeFlow by rememberSaveable { mutableIntStateOf(0) }
    var initialMainTab by rememberSaveable { mutableIntStateOf(0) }

    LaunchedEffect(authState.isSignedIn, settings?.authGuideShownForV340) {
        if (authState.isSignedIn && settings?.authGuideShownForV340 == false) {
            viewModel.markAuthGuideSeen()
        }
    }

    val current = settings
    val selectedTheme = CardTheme.fromId(current?.cardStyle ?: CardTheme.RPG.id)
    val appTheme = selectedTheme.appWideTheme(current?.unlockedStyles.orEmpty())
    NnoworkTheme(cardTheme = appTheme) {
        ProvideNnoworkWindowInfo {
            when {
                current == null || hasStoredSettings == null -> Box(
                    Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
                )
                hasStoredSettings == false && welcomeFlow == 0 -> WelcomeBackScreen(
                    onReturningUser = { welcomeFlow = 1 },
                    onNewUser = { welcomeFlow = 2 },
                )
                hasStoredSettings == false && welcomeFlow == 1 -> ReturningUserScreen(
                    processing = restoreState.processing,
                    message = restoreState.message,
                    isError = restoreState.isError,
                    onImport = viewModel::importBackup,
                    onSkip = { welcomeFlow = 2 },
                    onBack = { welcomeFlow = 0 },
                )
                else -> when {
                    !current!!.isOnboardingFinished -> OnboardingScreen(onFinished = {})
                    manualNotice || (!dismissedInSession && current.lastSeenNoticeVersion != UPDATE_NOTICE_VERSION) -> {
                        UpdateNoticeScreen(
                            onFinished = {
                                manualNotice = false
                                dismissedInSession = true
                                viewModel.markUpdateNoticeSeen()
                            },
                        )
                    }
                    !current.authGuideShownForV340 && !authState.isSignedIn -> AuthBindingGuideScreen(
                        onOpenSettings = {
                            initialMainTab = 4
                            viewModel.markAuthGuideSeen()
                        },
                        onLater = viewModel::markAuthGuideSeen,
                    )
                    else -> Box(Modifier.fillMaxSize()) {
                        MainScaffold(
                            initialSelected = initialMainTab,
                            onShowUpdateNotice = { manualNotice = true },
                        )
                        // 非強制更新提示（iOS RootView 頂部滑入的 UpdateBanner）
                        UpdateBannerHost(Modifier.align(Alignment.TopCenter))
                    }
                }
            }
        }
    }
}
