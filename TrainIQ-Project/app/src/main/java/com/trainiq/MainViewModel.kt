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
    private val restTimers: com.trainiq.domain.workout.WorkoutRestTimerCoordinator,
) : ViewModel() {
    private val _restTarget = kotlinx.coroutines.flow.MutableStateFlow<com.trainiq.domain.workout.WorkoutRestOpenRequest?>(null)
    val restTarget: StateFlow<com.trainiq.domain.workout.WorkoutRestOpenRequest?> = _restTarget
    fun openRestTimer(sessionId: Long, generation: String) {
        viewModelScope.launch {
            val timer = restTimers.current()
            _restTarget.value = timer?.takeIf { it.identity.sessionId == sessionId && it.identity.generation == generation }
                ?.let { com.trainiq.domain.workout.WorkoutRestOpenRequest(it.dayId, sessionId, generation) }
        }
    }
    fun consumeRestTarget(request: com.trainiq.domain.workout.WorkoutRestOpenRequest) { _restTarget.compareAndSet(request, null) }

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
