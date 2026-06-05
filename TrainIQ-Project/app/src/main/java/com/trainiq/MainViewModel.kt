package com.trainiq

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trainiq.core.datastore.OnboardingPreferences
import com.trainiq.core.datastore.UserPreferencesRepository
import com.trainiq.core.theme.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class MainViewModel @Inject constructor(
    userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {
    val themeMode: StateFlow<ThemeMode> = userPreferencesRepository.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemeMode.SYSTEM)

    val telemetryOptIn: StateFlow<Boolean> = userPreferencesRepository.telemetryOptIn
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val onboardingPreferences: StateFlow<OnboardingPreferences> = userPreferencesRepository.onboardingPreferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), OnboardingPreferences())
}
