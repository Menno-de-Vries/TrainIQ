package com.trainiq.features

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.*
import androidx.compose.ui.graphics.asAndroidBitmap
import com.trainiq.core.theme.TrainIqTheme
import com.trainiq.core.ui.UiMessage
import com.trainiq.domain.model.ProgressOverview
import com.trainiq.features.home.HomeScreen
import com.trainiq.features.home.HomeUiState
import com.trainiq.features.onboarding.OnboardingContentState
import com.trainiq.features.onboarding.OnboardingScreen
import com.trainiq.features.onboarding.OnboardingUiState
import com.trainiq.features.progress.ProgressScreen
import com.trainiq.features.progress.ProgressUiState
import com.trainiq.features.progress.ValidatedProgressMeasurement
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class FeatureRecoveryInstrumentedTest {
    @Test fun macroProgressHasTargetsAndSafeMissingGoalSemantics() = runComposeUiTest {
        setContent { TrainIqTheme {
            com.trainiq.core.util.MacroBreakdownCard(80, 160, 300, 260, 20, 0)
        } }
        onNodeWithText("80 / 160 g").assertExists()
        onNodeWithText("300 / 260 g").assertExists()
        onNodeWithText("20 g - Geen doel ingesteld").assertExists()
        fun assertProgress(label: String, value: Float) {
            onNodeWithContentDescription("$label voortgang").assert(
                SemanticsMatcher.expectValue(androidx.compose.ui.semantics.SemanticsProperties.ProgressBarRangeInfo,
                    androidx.compose.ui.semantics.ProgressBarRangeInfo(value, 0f..1f)),
            )
        }
        assertProgress("Eiwit", 0.5f)
        assertProgress("Koolhydraten", 1f)
        assertProgress("Vet", 0f)
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        java.io.File(context.getExternalFilesDir(null), "macro-targets.png").outputStream().use {
            onRoot().captureToImage().asAndroidBitmap().compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    @Test
    fun homeKeepsEnergyBalanceWithoutMealOverview() = runComposeUiTest {
        var dashboard by mutableStateOf(com.trainiq.domain.model.HomeDashboard(null, null, 0, 0, 0, 0, 0, 0, 0, 0, 0, null, null, 0, ""))
        val health = com.trainiq.domain.model.HealthConnectStatus(
            state = com.trainiq.domain.model.HealthConnectState.ERROR, message = "Niet verbonden",
        )
        setContent { TrainIqTheme { HomeScreen(HomeUiState.Success(dashboard, health), {}, {}, {}, {}, {}, {}) } }
        fun assertNoNutritionOverview() {
            listOf("Voedingsdag", "Ochtend", "Middag", "Avond", "Snacks")
                .forEach { onNodeWithText(it).assertDoesNotExist() }
        }
        onNodeWithText("Ontdekmodus").assertExists()
        onNodeWithText("Energiekompas").assertDoesNotExist()
        assertNoNutritionOverview()
        runOnIdle {
            dashboard = dashboard.copy(profile = com.trainiq.domain.model.UserProfile(
                1, "Test", 30, com.trainiq.domain.model.BiologicalSex.MALE, 180.0, 80.0, 20.0,
                "moderate", "maintain", 2400, 160, 260, 80, "strength",
            ), calorieTarget = 2400, energyBalance = com.trainiq.domain.model.EnergyBalanceSnapshot(
                caloriesIn = 1500, caloriesOut = 2100, balance = -600,
                bmr = 1600, tefCalories = 150, neatCalories = 100, workoutCalories = 250,
            ))
        }
        onNodeWithText("Energiekompas").assertIsDisplayed()
        onNodeWithText("600 kcal tekort").assertIsDisplayed()
        onNodeWithText("In 1500 kcal - Uit 2100 kcal - Doel 2400 kcal").assertExists()
        onNodeWithText("Macro's vandaag").performScrollTo().assertIsDisplayed()
        listOf("Eiwit", "Koolhydraten", "Vet").forEach { onNodeWithText(it).assertExists() }
        val capture = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().uiAutomation
            .executeShellCommand("screencap -p /data/local/tmp/trainiq-home-energy.png")
        android.os.ParcelFileDescriptor.AutoCloseInputStream(capture).use { it.readBytes() }
        onNodeWithText("Bekijk verbranding").performScrollTo().performClick()
        listOf("BMR", "TEF", "Stappen", "Training", "Profielschatting", "Uit gelogde intake", "Krachttraining")
            .forEach { onAllNodesWithText(it).onFirst().assertExists() }
        runOnIdle {
            dashboard = dashboard.copy(energyBalance = dashboard.energyBalance!!.copy(
                caloriesIn = 1800, caloriesOut = 2200, balance = -400,
            ))
        }
        onNodeWithText("400 kcal tekort").assertExists()
        onNodeWithText("In 1800 kcal - Uit 2200 kcal - Doel 2400 kcal").assertExists()
        onNodeWithText("Momentum").assertExists()
        assertNoNutritionOverview()
    }

    @Test
    fun homeWithoutProfileHasNoHealthRegistrationAndCoachExposesBothRoutes() = runComposeUiTest {
        var coach by mutableStateOf(false)
        var sleep = 0
        var weight = 0
        val dashboard = com.trainiq.domain.model.HomeDashboard(null, null, 0, 0, 0, 0, 0, 0, 0, 0, 0, null, null, 0, "")
        val health = com.trainiq.domain.model.HealthConnectStatus(state = com.trainiq.domain.model.HealthConnectState.ERROR, message = "Niet verbonden")
        setContent { TrainIqTheme {
            if (coach) com.trainiq.features.coach.CoachScreen(com.trainiq.features.coach.CoachUiState.Loading,
                {}, {}, {}, {}, {}, {}, onOpenSleep = { sleep++ }, onOpenBodyProgress = { weight++ })
            else HomeScreen(HomeUiState.Success(dashboard, health), {}, {}, {}, {}, {}, {})
        } }
        onNodeWithText("Slaap · alarm en bevestiging").assertDoesNotExist()
        onNodeWithText("Gewicht · bijhouden en voortgang").assertDoesNotExist()
        runOnIdle { coach = true }
        onNodeWithText("Slaap · alarm en bevestiging").performScrollTo().performClick()
        onNodeWithText("Lichaamsmetingen openen").performScrollTo().performClick()
        assertEquals(1, sleep)
        assertEquals(1, weight)
    }

    @Test
    fun homeErrorHasReachableRetryAction() = runComposeUiTest {
        var retries = 0
        setContent {
            TrainIqTheme {
                HomeScreen(HomeUiState.Error("Lezen mislukt"), {}, {}, {}, {}, {}, { retries++ })
            }
        }
        onNodeWithText("Lezen mislukt").assertIsDisplayed()
        onNodeWithText("Opnieuw proberen").assertIsDisplayed().performClick()
        assertEquals(1, retries)
    }

    @Test
    fun onboardingLoadAndCompletionErrorsOfferCorrectRetry() = runComposeUiTest {
        var state by mutableStateOf<OnboardingUiState>(OnboardingUiState.Error("Lezen mislukt"))
        var reads = 0
        var finishes = 0
        setContent {
            TrainIqTheme {
                OnboardingScreen(state, {}, {}, { finishes++ }, { reads++ })
            }
        }
        onNodeWithText("Opnieuw proberen").performClick()
        assertEquals(1, reads)
        runOnIdle {
            state = OnboardingUiState.Success(OnboardingContentState(), saveError = "Opslaan mislukt", completionFailed = true)
        }
        onNodeWithText("Opnieuw proberen").performScrollTo().performClick()
        assertEquals(1, finishes)
        runOnIdle { state = OnboardingUiState.Success(OnboardingContentState(), isCompleting = true) }
        onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Setup opslaan..."))
        onNodeWithText("Setup opslaan...").performScrollTo().assertIsNotEnabled()
        onNodeWithText("Later afronden").assertIsNotEnabled()
    }

    @Test
    fun measurementSaveDisablesRepeatSubmitAndPreservesNewerDraftAcrossRestoration() = runComposeUiTest {
        val overview = ProgressOverview(emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), 0.0, null)
        var state by mutableStateOf(ProgressUiState.Success(overview))
        var submissions = 0
        val restoration = StateRestorationTester(this)
        restoration.setContent {
            TrainIqTheme {
                ProgressScreen(state, { _, _, _ -> submissions++; state = state.copy(isSaving = true) }, {}, {}, {})
            }
        }
        fun field(label: String) = onNode(hasSetTextAction() and hasText(label))
        field("Gewicht (kg)").performScrollTo().performTextReplacement("80")
        field("Vetpercentage (%)").performScrollTo().performTextReplacement("20")
        field("Spiermassa (kg)").performScrollTo().performTextReplacement("40")
        onNodeWithText("Meting opslaan").performScrollTo().performClick()
        onNodeWithText("Meting opslaan...").assertIsNotEnabled()
        assertEquals(1, submissions)
        field("Gewicht (kg)").performScrollTo().performTextReplacement("81")
        runOnIdle {
            state = state.copy(isSaving = false, savedMeasurement = ValidatedProgressMeasurement(80.0, 20.0, 40.0), message = UiMessage("Meting opgeslagen."))
        }
        field("Gewicht (kg)").assertTextContains("81")
        restoration.emulateSaveAndRestore()
        field("Gewicht (kg)").performScrollTo().assertTextContains("81")
        runOnIdle { state = state.copy(message = UiMessage("Meting opslaan mislukt. Probeer opnieuw.")) }
        onNodeWithText("Meting opslaan").performScrollTo().assertIsEnabled()
        field("Gewicht (kg)").performScrollTo().assertTextContains("81")
    }
}
