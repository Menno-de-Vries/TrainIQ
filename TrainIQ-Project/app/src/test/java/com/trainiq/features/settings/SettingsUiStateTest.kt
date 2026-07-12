package com.trainiq.features.settings

import com.trainiq.core.datastore.ReminderPreferences
import com.trainiq.core.datastore.WorkoutFeedbackPreferences
import com.trainiq.ai.services.AiProviderPreference
import com.trainiq.core.theme.ThemeMode
import com.trainiq.core.ui.UiMessage
import com.trainiq.domain.model.HealthConnectState
import com.trainiq.domain.model.HealthConnectMetrics
import com.trainiq.domain.model.HealthConnectStatus
import com.trainiq.domain.model.HealthConnectStepDataFreshness
import com.trainiq.domain.model.HealthConnectStepDiagnostic
import com.trainiq.domain.model.HealthMetricStatus
import com.trainiq.domain.model.HealthMetricSyncState
import com.trainiq.domain.model.HealthMetricType
import java.io.File
import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsUiStateTest {
    @Test
    fun settingsUsesOnePrecomposedFiniteScrollContainer() {
        val source = File("src/main/java/com/trainiq/features/settings/SettingsSection.kt").readText()
        val settingsScreen = source.substringAfter("fun SettingsScreen(")
            .substringBefore("pendingDestructiveAction?.let")

        assertTrue(settingsScreen.contains("rememberScrollState()"))
        assertTrue(settingsScreen.contains(".verticalScroll(scrollState)"))
        assertTrue(settingsScreen.contains(".clearFocusOnScrollOrDrag()"))
        assertFalse(settingsScreen.contains("LazyColumn("))
        assertFalse(source.contains("SettingsListItem"))
    }

    @Test
    fun settingsUiState_usesSingleSuccessStateWithMaskedKey() {
        val state = settingsUiState(
            themeMode = ThemeMode.DARK,
            aiStatus = SettingsAiStatus(
                enabled = true,
                preferredProvider = AiProviderPreference.GEMINI_FIRST,
                hasGeminiKey = true,
                hasOpenAiKey = false,
                maskedGeminiKey = "abcd****wxyz",
                maskedOpenAiKey = "Niet ingesteld",
            ),
            telemetryOptIn = true,
            workoutFeedbackPreferences = WorkoutFeedbackPreferences(restTimerSoundEnabled = true),
            reminderPreferences = ReminderPreferences(enabled = true),
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
                aiStatus = SettingsAiStatus(
                    enabled = true,
                    preferredProvider = AiProviderPreference.GEMINI_FIRST,
                    hasGeminiKey = true,
                    hasOpenAiKey = false,
                    maskedGeminiKey = "abcd****wxyz",
                    maskedOpenAiKey = "Niet ingesteld",
                ),
                telemetryOptIn = true,
                workoutFeedbackPreferences = WorkoutFeedbackPreferences(restTimerSoundEnabled = true),
                reminderPreferences = ReminderPreferences(enabled = true),
                profile = null,
                healthStatus = HealthConnectStatus(
                    state = HealthConnectState.CONNECTED,
                    message = "Verbonden.",
                ),
                message = UiMessage("Instellingen opgeslagen.", id = 1L),
            ),
            state,
        )
    }

    @Test
    fun settingsUiStateDoesNotRetainPlaintextAiKeys() {
        val source = File("src/main/java/com/trainiq/features/settings/SettingsSection.kt").readText()
        val successBody = source.substringAfter("data class Success(").substringBefore(") : SettingsUiState")
        val settingsScreenBody = source.substringAfter("fun SettingsScreen(").substringBefore("val listState")

        assertFalse(successBody.contains("AiPreferences"))
        assertFalse(successBody.contains("apiKey"))
        assertFalse(settingsScreenBody.contains("var apiKey by rememberSaveable"))
        assertFalse(settingsScreenBody.contains("var openAiKey by rememberSaveable"))
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
    fun importReaderRejectsOversizedJsonBeforeReadingWholeDocument() {
        val oversized = "x".repeat(32)

        val error = runCatching {
            readTrainIqImportJson(oversized.reader(), maxChars = 16)
        }.exceptionOrNull()

        assertTrue(error is IllegalStateException)
        assertTrue(error?.message.orEmpty().contains("te groot"))
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
            "Geef toegang om stappen, hartslag, slaap, actieve calorieen en workouts te synchroniseren.",
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
    fun healthConnectStepAvailabilityDistinguishesMeasuredZeroFromMissingPermission() {
        val freshZero = HealthConnectStatus(
            state = HealthConnectState.CONNECTED,
            message = "Verbonden",
            metrics = HealthConnectMetrics(stepsToday = 0),
            stepDataFreshness = HealthConnectStepDataFreshness.FRESH,
        )

        assertEquals("0 stappen via Health Connect", healthConnectStepsAvailabilityMessage(freshZero))
        assertNull(
            healthConnectStepsAvailabilityMessage(
                freshZero.copy(stepDataFreshness = HealthConnectStepDataFreshness.PERMISSION_MISSING),
            ),
        )
    }

    @Test
    fun healthConnectCompactSummaryUsesFriendlySamsungSourceAndStaleCopy() {
        val diagnostic = HealthConnectStepDiagnostic(
            aggregateStepsToday = 8,
            samsungHealthStepsToday = 84,
            samsungHealthAggregateStepsToday = 8,
            samsungRawStepRecordSumToday = 84,
            queriedAt = 123L,
        )
        val fresh = HealthConnectStatus(
            state = HealthConnectState.CONNECTED,
            metrics = HealthConnectMetrics(stepsToday = 84),
            message = "Verbonden",
            stepDataFreshness = HealthConnectStepDataFreshness.FRESH,
            stepDiagnostic = diagnostic,
        )

        assertEquals("84 stappen via Samsung Health", healthConnectStepsAvailabilityMessage(fresh))
        assertEquals(
            "Laatst bekend: 84 stappen via Samsung Health",
            healthConnectStepsAvailabilityMessage(
                fresh.copy(stepDataFreshness = HealthConnectStepDataFreshness.STALE_CACHE),
            ),
        )
        assertEquals("Technische details tonen", healthTechnicalDetailsToggleLabel(expanded = false))
        assertEquals("Technische details verbergen", healthTechnicalDetailsToggleLabel(expanded = true))
    }

    @Test
    fun healthConnectLastCheckedUsesCompactTime() {
        val timestamp = Instant.parse("2026-07-10T00:33:00Z").toEpochMilli()

        assertEquals(
            "Laatst gecontroleerd om 00:33",
            healthConnectLastCheckedMessage(timestamp, ZoneId.of("UTC")),
        )
        assertNull(healthConnectLastCheckedMessage(null, ZoneId.of("UTC")))
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
    fun reopeningOnboardingFromSettingsDoesNotMarkFirstRunIncomplete() {
        val source = File("src/main/java/com/trainiq/features/settings/SettingsSection.kt").readText()
        val onboardingOpenBody = source.substringAfter("onOpenOnboarding = {")
            .substringBefore("},")

        assertFalse(onboardingOpenBody.contains("viewModel.reopenOnboarding()"))
        assertTrue(source.contains("onOpenOnboarding()"))
    }

    @Test
    fun settingsShowsStepSourceDiagnosticForSamsungHealthTroubleshooting() {
        val source = File("src/main/java/com/trainiq/features/settings/SettingsSection.kt").readText()

        assertTrue(source.contains("stepDiagnostic"))
        assertTrue(source.contains("Samsung Health"))
        assertTrue(source.contains("Sync now"))
        assertTrue(source.contains("Samsung-vergelijking"))
        assertTrue(source.contains("samsungHealthComparisonSummary"))
        assertTrue(source.contains("Stappenwaarden"))
        assertTrue(source.contains("stepValueDebugSummary"))
        assertTrue(source.contains("Samsung-bron timing"))
        assertTrue(source.contains("samsungSourceRecencySummary"))
        assertTrue(source.contains("Samsung direct"))
        assertTrue(source.contains("samsungHealthDirectStatus"))
        assertTrue(source.contains("Pariteit"))
        assertTrue(source.contains("parityGapSummary"))
        assertTrue(source.contains("requestSamsungHealthStepPermission"))
        assertTrue(source.contains("Samsung Health-stappentoegang kon niet worden geopend"))
        assertTrue(source.contains("SAMSUNG_HEALTH_DATA_SDK_AAR_PRESENT"))
        assertTrue(source.contains("Samsung Health-stappentoegang geven"))
        assertTrue(source.contains("Samsung toegang geven"))
        assertTrue(source.contains("Samsung Health openen voor Sync now"))
        assertTrue(source.contains("Samsung Health openen"))
        assertTrue(source.contains("SamsungHealthPackageName = \"com.sec.android.app.shealth\""))
        assertTrue(source.contains("getLaunchIntentForPackage(SamsungHealthPackageName)"))
        assertTrue(source.contains("market://details?id=\$SamsungHealthPackageName"))
        assertTrue(source.contains("Samsung stappen-diagnose kopieren"))
        assertTrue(source.contains("samsungStepDebugClipboardText"))
        assertTrue(source.contains("Samsung stappen-diagnose gekopieerd."))
        assertTrue(source.contains("Health Connect zichtbaar"))
        assertTrue(source.contains("healthConnectVisibleStepSummary"))
        assertTrue(source.contains("Health Connect-prioriteit"))
        assertTrue(source.contains("healthConnectStepPrioritySummary"))
        assertTrue(source.contains("hasMultipleHealthConnectStepSources"))
        assertTrue(source.contains("Health Connect App priorities openen"))
        assertTrue(source.contains("Prioriteiten openen"))
        assertTrue(source.contains("Workout-overlap"))
        assertTrue(source.contains("workoutWindowSummary"))
    }

    @Test
    fun healthConnectTechnicalDiagnosticsAreCollapsedButPreserved() {
        val source = File("src/main/java/com/trainiq/features/settings/SettingsSection.kt").readText()
        val healthCard = source.substringAfter("SectionCard(title = \"Health Connect\")")
            .substringBefore("SectionCard(title = \"Gegevens / opslag\")")
        val details = source.substringAfter("private fun HealthConnectTechnicalDetails(")
            .substringBefore("internal fun healthStatusLabel")

        assertTrue(source.contains("var showHealthTechnicalDetails by rememberSaveable"))
        assertTrue(healthCard.contains("AnimatedVisibility"))
        assertTrue(healthCard.contains("healthTechnicalDetailsToggleLabel"))
        assertTrue(details.contains("stepValueDebugSummary"))
        assertTrue(details.contains("samsungHealthDirectStatus"))
        assertTrue(details.contains("parityGapSummary"))
        assertTrue(details.contains("samsungStepDebugClipboardText"))
        assertTrue(details.contains("workoutWindowSummary"))
    }

    @Test
    fun themeModeChipsExposeExplicitAccessibilityLabels() {
        assertEquals("Themamodus: Systeem", themeModeAccessibilityLabel(ThemeMode.SYSTEM))
        assertEquals("Themamodus: Licht", themeModeAccessibilityLabel(ThemeMode.LIGHT))
        assertEquals("Themamodus: Donker", themeModeAccessibilityLabel(ThemeMode.DARK))

        val source = File("src/main/java/com/trainiq/features/settings/SettingsSection.kt").readText()
        val displaySection = source.substringAfter("SectionCard(title = \"Weergave\")").substringBefore("SectionCard(title = \"Voorkeuren\")")
        assertTrue(displaySection.contains("FlowRow("))
        assertTrue(displaySection.contains(".settingsActionLabel(themeModeAccessibilityLabel(mode))"))
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
    fun settingsCombinesFeedbackRemindersAndTelemetryIntoPreferencesCard() {
        val source = File("src/main/java/com/trainiq/features/settings/SettingsSection.kt").readText()
        val preferencesSection = source.substringAfter("SectionCard(title = \"Voorkeuren\")")
            .substringBefore("SectionCard(title = \"AI / Providers\")")

        assertTrue(preferencesSection.contains("Rusttimer-geluid"))
        assertTrue(preferencesSection.contains("TrainIQ-reminders"))
        assertTrue(preferencesSection.contains("Technische telemetrie delen"))
        assertFalse(source.contains("SectionCard(title = \"Workoutfeedback\")"))
        assertFalse(source.contains("SectionCard(title = \"Reminders\")"))
        assertFalse(source.contains("SectionCard(title = \"Privacy en telemetrie\")"))
    }

    @Test
    fun compactSettingsReadsAsOverflowWithoutDuplicatingTrendNavigation() {
        assertEquals("Meer", settingsOverflowSectionTitle())
        assertTrue(settingsOverflowSectionBody().contains("Compacte navigatie"))
        assertTrue(settingsOverflowSectionBody().contains("Trend"))

        val source = File("src/main/java/com/trainiq/features/settings/SettingsSection.kt").readText()
        assertFalse(source.contains("Voortgang openen"))
        assertFalse(source.contains("settingsOpenProgressActionLabel"))
    }

    @Test
    fun settingsUsesDocumentExportAndSnackbarFeedbackInsteadOfTopMessageItem() {
        val source = File("src/main/java/com/trainiq/features/settings/SettingsSection.kt").readText()

        assertTrue(source.contains("ActivityResultContracts.CreateDocument(\"application/json\")"))
        assertTrue(source.contains("Data exporteren als JSON"))
        assertTrue(source.contains("ActivityResultContracts.OpenDocument()"))
        assertTrue(source.contains("Data importeren uit JSON"))
        assertTrue(source.contains("JSON-import bevestigen"))
        assertTrue(source.contains("Importeren en vervangen"))
        assertTrue(source.contains("TrainIQ-data exporteren als JSON"))
        assertTrue(source.contains("TrainIQ-data importeren uit JSON"))
        assertTrue(source.contains("Lokale TrainIQ-data wissen"))
        assertTrue(source.contains("previewImportJson"))
        assertTrue(source.contains("confirmImport"))
        assertTrue(source.contains("rememberScrollState()"))
        assertTrue(source.contains(".verticalScroll(scrollState)"))
        assertTrue(source.contains("SnackbarHost"))
        assertFalse(source.contains("message?.let"))
        assertFalse(source.contains("AnimatedScreenState(targetState = uiState)"))
    }

    @Test
    fun importPreviewSummaryNamesDestructiveRestoreCounts() {
        val summary = importPreviewSummary(
            com.trainiq.domain.usecase.AppDataImportPreview(
                format = "trainiq-json-export",
                version = 1,
                exportedAt = "2026-06-03T10:15:30Z",
                rowCount = 42,
                routineCount = 2,
                workoutCount = 3,
                mealCount = 4,
                foodCount = 5,
                measurementCount = 6,
            ),
        )

        assertTrue(summary.contains("trainiq-json-export v1"))
        assertTrue(summary.contains("Rijen: 42"))
        assertTrue(summary.contains("Routines: 2"))
        assertTrue(summary.contains("Workouts: 3"))
        assertTrue(summary.contains("Maaltijden: 4"))
        assertTrue(summary.contains("Producten: 5"))
        assertTrue(summary.contains("Metingen: 6"))
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
