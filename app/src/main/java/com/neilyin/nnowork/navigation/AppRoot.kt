package com.neilyin.nnowork.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.neilyin.nnowork.data.settings.SettingsRepository
import com.neilyin.nnowork.di.ServiceLocator
import com.neilyin.nnowork.feature.onboarding.OnboardingScreen
import com.neilyin.nnowork.ui.theme.PixelColors
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class RootViewModel(
    settingsRepository: SettingsRepository = ServiceLocator.settingsRepository,
) : ViewModel() {
    /** null = 載入中。 */
    val onboardingFinished: StateFlow<Boolean?> =
        settingsRepository.settings
            .map { it.isOnboardingFinished }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

@Composable
fun AppRoot(viewModel: RootViewModel = viewModel()) {
    val finished by viewModel.onboardingFinished.collectAsStateWithLifecycle()
    when (finished) {
        null -> Box(Modifier.fillMaxSize().background(PixelColors.darkGray))
        false -> OnboardingScreen(onFinished = {})
        true -> MainScaffold()
    }
}
