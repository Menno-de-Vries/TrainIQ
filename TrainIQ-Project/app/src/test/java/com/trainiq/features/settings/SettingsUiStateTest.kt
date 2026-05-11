package com.trainiq.features.settings

import com.trainiq.core.datastore.AiPreferences
import com.trainiq.core.datastore.WorkoutFeedbackPreferences
import com.trainiq.core.theme.ThemeMode
import com.trainiq.domain.model.HealthConnectState
import com.trainiq.domain.model.HealthConnectStatus
import java.io.File
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
    fun geminiApiKeyHelpPointsToGoogleAiStudioWithoutEncouragingCommittedSecrets() {
        assertEquals("Google AI Studio API Keys", geminiApiKeySourceLabel())
        assertEquals("https://aistudio.google.com/app/apikey", geminiApiKeySourceUrl())

        val help = geminiApiKeySetupHelpText()
        assertTrue(help.contains("Google AI Studio"))
        assertTrue(help.contains("plak hem hier"))
        assertTrue(help.contains("Deel je sleutel niet"))
        assertTrue(help.contains("commit hem nooit"))
    }

    @Test
    fun healthConnectSettingsMessageUsesCompactCopyForLowerSettingsSection() {
        assertEquals(
            "Geef toegang om stappen, hartslag, slaap, calorieen, gewicht en workouts te synchroniseren.",
            healthConnectSettingsMessage(
                HealthConnectStatus(
                    state = HealthConnectState.PERMISSION_REQUIRED,
                    message = "Long provider message that should not drive the settings layout.",
                ),
            ),
        )
        assertEquals(
            "Verbonden. TrainIQ synchroniseert toegestane metrics wanneer data beschikbaar is.",
            healthConnectSettingsMessage(
                HealthConnectStatus(
                    state = HealthConnectState.CONNECTED,
                    message = "Long provider message that should not drive the settings layout.",
                ),
            ),
        )
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

    @Test
    fun healthConnectSettingsActionsKeepAccessibilityLabelsForLargeFontClipping() {
        val source = File("src/main/java/com/trainiq/features/settings/SettingsSection.kt").readText()

        assertTrue(source.contains("Health Connect-toegang geven"))
        assertTrue(source.contains("Health Connect-status vernieuwen"))
        assertTrue(source.contains("Health Connect-instellingen openen"))
        assertTrue(source.contains("Health Connect installeren of bijwerken"))
        assertTrue(source.contains("settingsActionLabel"))
    }
}
