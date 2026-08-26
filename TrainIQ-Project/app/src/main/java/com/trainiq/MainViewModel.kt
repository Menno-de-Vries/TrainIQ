package com.trainiq

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trainiq.core.datastore.OnboardingPreferences
import com.trainiq.core.datastore.UserPreferencesRepository
import com.trainiq.core.theme.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

sealed interface MainOnboardingState {
    data object Loading : MainOnboardingState
    data class Ready(val preferences: OnboardingPreferences) : MainOnboardingState
}

@HiltViewModel
class MainViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {
    val themeMode: StateFlow<ThemeMode> = userPreferencesRepository.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemeMode.SYSTEM)

    val telemetryOptIn: StateFlow<Boolean> = userPreferencesRepository.telemetryOptIn
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val onboardingState: StateFlow<MainOnboardingState> = userPreferencesRepository.onboardingPreferences
        .map<OnboardingPreferences, MainOnboardingState>(MainOnboardingState::Ready)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MainOnboardingState.Loading)

    fun markGuidedTourCompleted() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            userPreferencesRepository.markGuidedTourCompleted()
        }
    }

    fun markGuidedTourSkipped() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            userPreferencesRepository.markGuidedTourSkipped()
        }
    }
}
