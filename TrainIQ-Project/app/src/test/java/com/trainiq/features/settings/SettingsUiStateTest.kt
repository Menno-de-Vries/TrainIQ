package com.trainiq.features.settings

import com.trainiq.core.datastore.AiPreferences
import com.trainiq.core.datastore.WorkoutFeedbackPreferences
import com.trainiq.core.theme.ThemeMode
import com.trainiq.domain.model.HealthConnectState
import com.trainiq.domain.model.HealthConnectStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsUiStateTest {
    @Test
    fun settingsUiState_usesSingleSuccessStateWithMaskedKey() {
        val state = settingsUiState(
            themeMode = ThemeMode.DARK,
            aiPreferences = AiPreferences(enabled = true, apiKey = "abcd1234wxyz"),
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
}
