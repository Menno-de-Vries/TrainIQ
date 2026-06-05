package com.trainiq

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MainOnboardingStateTest {
    @Test
    fun mainViewModelExposesLoadingBeforeOnboardingPreferencesAreRead() {
        val source = File("src/main/java/com/trainiq/MainViewModel.kt").readText()

        assertTrue(source.contains("sealed interface MainOnboardingState"))
        assertTrue(source.contains("data object Loading : MainOnboardingState"))
        assertTrue(source.contains("data class Ready"))
        assertTrue(source.contains("val onboardingState: StateFlow<MainOnboardingState>"))
        assertTrue(source.contains("MainOnboardingState.Loading"))
        assertFalse(source.contains("stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), OnboardingPreferences())"))
    }

    @Test
    fun mainActivityDoesNotRenderTrainIqAppUntilOnboardingStateIsReady() {
        val source = File("src/main/java/com/trainiq/MainActivity.kt").readText()
        val setContentBody = source.substringAfter("setContent")

        assertTrue(setContentBody.contains("MainOnboardingState.Loading"))
        assertTrue(setContentBody.contains("MainOnboardingState.Ready"))
        assertTrue(setContentBody.contains("TrainIqStartupGate"))
        assertTrue(setContentBody.contains("onboardingPreferences = onboardingState.preferences"))
        assertFalse(setContentBody.contains("onboardingCompleted = onboardingPreferences.completed"))
    }
}
