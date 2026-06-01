package com.trainiq.features.settings

import com.trainiq.core.datastore.AiPreferences
import com.trainiq.core.datastore.WorkoutFeedbackPreferences
import com.trainiq.core.theme.ThemeMode
import com.trainiq.core.ui.UiMessage
import com.trainiq.domain.model.HealthConnectState
import com.trainiq.domain.model.HealthConnectStatus
import com.trainiq.domain.model.HealthMetricStatus
import com.trainiq.domain.model.HealthMetricSyncState
import com.trainiq.domain.model.HealthMetricType
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
            message = UiMessage("Instellingen opgeslagen.", id = 1L),
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
                message = UiMessage("Instellingen opgeslagen.", id = 1L),
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
    fun healthConnectSettingsCopyPreservesPartialPermissionMessage() {
        val partialStatus = HealthConnectStatus(
            state = HealthConnectState.NO_DATA,
            message = "Health Connect is gedeeltelijk verbonden. Toegestane metrics zijn gesynchroniseerd, maar er is nog geen recente data.",
            metricStatuses = listOf(
                HealthMetricStatus(HealthMetricType.ACTIVE_CALORIES, HealthMetricSyncState.SYNCED),
                HealthMetricStatus(HealthMetricType.STEPS, HealthMetricSyncState.DENIED),
            ),
        )

        assertEquals("Gedeeltelijk verbonden", healthStatusLabel(partialStatus))
        assertEquals(partialStatus.message, healthConnectSettingsMessage(partialStatus))
        assertTrue(partialStatus.hasPartialHealthConnectAccess())
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

    @Test
    fun themeModeChipsExposeExplicitAccessibilityLabels() {
        assertEquals("Themamodus: Systeem", themeModeAccessibilityLabel(ThemeMode.SYSTEM))
        assertEquals("Themamodus: Licht", themeModeAccessibilityLabel(ThemeMode.LIGHT))
        assertEquals("Themamodus: Donker", themeModeAccessibilityLabel(ThemeMode.DARK))

        val source = File("src/main/java/com/trainiq/features/settings/SettingsSection.kt").readText()
        assertTrue(source.contains("Modifier.settingsActionLabel(themeModeAccessibilityLabel(mode))"))
    }

    @Test
    fun feedbackAndTelemetrySwitchesExposeStatefulAccessibilityLabels() {
        assertEquals("Rusttimer-geluid: ingeschakeld", settingsToggleAccessibilityLabel("Rusttimer-geluid", true))
        assertEquals("Workouttrillingen: uitgeschakeld", settingsToggleAccessibilityLabel("Workouttrillingen", false))
        assertEquals("Technische telemetrie delen: ingeschakeld", settingsToggleAccessibilityLabel("Technische telemetrie delen", true))

        val source = File("src/main/java/com/trainiq/features/settings/SettingsSection.kt").readText()
        assertTrue(source.contains(".heightIn(min = 48.dp)"))
        assertTrue(source.contains("role = Role.Switch"))
        assertTrue(source.contains("onCheckedChange = null"))
        assertTrue(source.contains(".sizeIn(minWidth = 48.dp, minHeight = 48.dp)"))
        assertTrue(source.contains("settingsActionLabel(settingsToggleAccessibilityLabel(title, checked))"))
    }

    @Test
    fun compactSettingsReadsAsOverflowAndExposesProgressNearTop() {
        assertEquals("Meer", settingsOverflowSectionTitle())
        assertTrue(settingsOverflowSectionBody().contains("Compacte navigatie"))
        assertTrue(settingsOverflowSectionBody().contains("Voortgang"))
        assertEquals("Voortgang openen vanuit Meer", settingsOpenProgressActionLabel())
    }

    @Test
    fun settingsUsesDocumentExportAndSnackbarFeedbackInsteadOfTopMessageItem() {
        val source = File("src/main/java/com/trainiq/features/settings/SettingsSection.kt").readText()

        assertTrue(source.contains("ActivityResultContracts.CreateDocument(\"application/json\")"))
        assertTrue(source.contains("Data exporteren als JSON"))
        assertTrue(source.contains("rememberLazyListState()"))
        assertTrue(source.contains("SnackbarHost"))
        assertFalse(source.contains("message?.let"))
        assertFalse(source.contains("AnimatedScreenState(targetState = uiState)"))
    }

    @Test
    fun transientMessagesUseSnackbarsInsteadOfTopListItemsAcrossMainFlows() {
        val root = File("src/main/java/com/trainiq/features")
        val nutrition = File(root, "nutrition/NutritionScreen.kt").readText()
        val progress = File(root, "progress/ProgressScreen.kt").readText()
        val coach = File(root, "coach/CoachScreen.kt").readText()
        val workout = File(root, "workout/WorkoutScreen.kt").readText()

        assertTrue(nutrition.contains("SnackbarHost"))
        assertTrue(progress.contains("SnackbarHost"))
        assertTrue(coach.contains("SnackbarHost"))
        assertTrue(workout.contains("SnackbarHost"))
        assertFalse(nutrition.contains("message?.let { MessageCard"))
        assertFalse(progress.contains("message?.let { item"))
        assertFalse(coach.contains("state.message?.let"))
        assertFalse(workout.contains("active-workout-message"))
        assertFalse(workout.contains("if (message != null) item"))
    }
}
