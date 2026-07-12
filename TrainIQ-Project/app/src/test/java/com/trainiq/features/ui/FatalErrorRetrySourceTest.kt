package com.trainiq.features.ui

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class FatalErrorRetrySourceTest {
    @Test
    fun fatalErrorsExposeAccessibleRetryActionsBackedByViewModelReloads() {
        val nutrition = File("src/main/java/com/trainiq/features/nutrition/NutritionScreen.kt").readText()
        val coach = File("src/main/java/com/trainiq/features/coach/CoachScreen.kt").readText()
        val settings = File("src/main/java/com/trainiq/features/settings/SettingsSection.kt").readText()

        assertRetryContract(nutrition, "Nutrition")
        assertRetryContract(coach, "Coach")
        assertRetryContract(settings, "Settings")
        assertTrue(nutrition.contains("preferencesRepository.aiPreferences") && nutrition.contains("reloadableObservation(reloads)"))
        assertTrue(coach.contains("observeUserProfileUseCase()") && coach.contains("observeSavedGoalAdviceUseCase()"))
        assertTrue(coach.split("reloadableObservation(reloads)").size >= 4)
        assertTrue(settings.contains("externalInputs == null -> SettingsUiState.Loading"))
        assertTrue(settings.contains("externalInputs.isFailure -> SettingsUiState.Error"))
        assertTrue(settings.split("reloadableObservation(reloads)").size == 2)
    }

    private fun assertRetryContract(source: String, screen: String) {
        assertTrue("$screen route must delegate retry to its ViewModel", source.contains("viewModel::retry"))
        assertTrue("$screen fatal error must label the action", source.contains("Text(\"Opnieuw proberen\")"))
        assertTrue("$screen retry must have a 48dp minimum touch target", source.contains("heightIn(min = 48.dp)"))
    }
}
