package com.trainiq.features.settings

import com.trainiq.core.datastore.AiPreferences
import com.trainiq.core.datastore.WorkoutFeedbackPreferences
import com.trainiq.core.theme.ThemeMode
import com.trainiq.domain.model.HealthConnectState
import com.trainiq.domain.model.HealthConnectStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsUiStateTest {
    @Test
    fun settingsUiState_usesSingleSuccessStateWithMaskedKey() {
        val state = settingsUiState(
            themeMode = ThemeMode.DARK,
            aiPreferences = AiPreferences(enabled = true, apiKey = "abcd1234wxyz"),
            telemetryOptIn = true,
            workoutFeedbackPreferences = WorkoutFeedbackPreferences(restTimerSoundEnabled = true),
            profile = null,
            healthStatus = HealthConnectStatus(
                state = HealthConnectState.CONNECTED,
                message = "Verbonden.",
            ),
            message = "Instellingen opgeslagen.",
        )

        assertEquals(
            SettingsUiState.Success(
                themeMode = ThemeMode.DARK,
                aiPreferences = AiPreferences(enabled = true, apiKey = "abcd1234wxyz"),
                telemetryOptIn = true,
                workoutFeedbackPreferences = WorkoutFeedbackPreferences(restTimerSoundEnabled = true),
                profile = null,
                healthStatus = HealthConnectStatus(
                    state = HealthConnectState.CONNECTED,
                    message = "Verbonden.",
                ),
                message = "Instellingen opgeslagen.",
                maskedApiKey = "abcd****wxyz",
            ),
            state,
        )
    }

    @Test
    fun maskedSettingsApiKey_neverExposesShortKeys() {
        assertEquals("Niet ingesteld", maskedSettingsApiKey(""))
        assertEquals("********", maskedSettingsApiKey("short"))
        assertEquals("abcd****wxyz", maskedSettingsApiKey("abcd1234wxyz"))
    }

    @Test
    fun geminiApiKeyInput_isAlwaysMaskedWhileTyping() {
        assertEquals(true, shouldMaskGeminiApiKeyInput())
    }

    @Test
    fun destructiveSettingsDialogCopyNamesScopeAndKeepsCancelAvailable() {
        assertEquals("API-sleutel verwijderen?", destructiveSettingsActionTitle(PendingDestructiveSettingsAction.CLEAR_API_KEY))
        assertEquals("Profiel resetten?", destructiveSettingsActionTitle(PendingDestructiveSettingsAction.RESET_PROFILE))
        assertEquals("Alle lokale appdata wissen?", destructiveSettingsActionTitle(PendingDestructiveSettingsAction.CLEAR_ALL_DATA))

        val clearAllDataBody = destructiveSettingsActionBody(PendingDestructiveSettingsAction.CLEAR_ALL_DATA)
        assertTrue(clearAllDataBody.contains("lokale trainingen"))
        assertTrue(clearAllDataBody.contains("Health Connect-cache"))
        assertTrue(clearAllDataBody.contains("Health Connect-permissies zelf beheer je in Android"))
        assertTrue(clearAllDataBody.contains("niet automatisch ongedaan"))
        assertFalse(clearAllDataBody.contains("cloud", ignoreCase = true))
    }
}
