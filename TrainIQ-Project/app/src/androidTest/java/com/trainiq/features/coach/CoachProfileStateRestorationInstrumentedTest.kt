package com.trainiq.features.coach

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.StateRestorationTester
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.runComposeUiTest
import com.trainiq.core.theme.TrainIqTheme
import com.trainiq.domain.model.BiologicalSex
import com.trainiq.domain.model.CoachOverview
import com.trainiq.domain.model.UserProfile
import org.junit.Test

class CoachProfileStateRestorationInstrumentedTest {
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun previousReportRemainsReadableDuringRefreshAndFailureAllowsRetry() = runComposeUiTest {
        val profile = syntheticProfile()
        var state by mutableStateOf(syntheticCoachUiState(profile, profile.toSyntheticDraft()).copy(
            generatedReport = com.trainiq.domain.model.WeeklyReportResult(
                summary = "Previously saved report", wins = emptyList(), risks = emptyList(),
                nextWeekFocus = "Keep recovering", source = com.trainiq.domain.model.WeeklyReportSource.LOCAL_FALLBACK,
            ),
            isGeneratingReport = true,
        ))
        var retries = 0
        setContent {
            TrainIqTheme {
                CoachScreen(state, {}, { retries++ }, {}, {}, {}, {})
            }
        }
        onNodeWithText("Previously saved report").performScrollTo().assertExists()
        onNodeWithText("Rapport maken...").performScrollTo().assertIsNotEnabled()
        runOnIdle { state = state.copy(isGeneratingReport = false, message = "Refresh failed; try again") }
        onNodeWithText("Previously saved report").performScrollTo().assertExists()
        onNodeWithText("Refresh failed; try again").assertExists()
        onNodeWithText("Weekrapport maken").performScrollTo().performClick()
        runOnIdle { org.junit.Assert.assertEquals(1, retries) }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun existingProfileDraftSurvivesStateRestoration() = runComposeUiTest {
        val restorationTester = StateRestorationTester(this)

        restorationTester.setContent {
            SyntheticCoachScreen(profile = syntheticProfile())
        }

        onNodeWithText("Doelen").performClick()
        onNode(hasSetTextAction() and hasText("Bestaand profiel"))
            .performScrollTo()
            .performTextReplacement("Onopgeslagen coachnaam")
        onNode(hasSetTextAction() and hasText("Onopgeslagen coachnaam"))
            .assertTextContains("Onopgeslagen coachnaam")

        restorationTester.emulateSaveAndRestore()

        onNode(hasSetTextAction() and hasText("Onopgeslagen coachnaam"))
            .performScrollTo()
            .assertTextContains("Onopgeslagen coachnaam")
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun dirtyDraftSurvivesStateRestorationWhenProfileIdChanges() = runComposeUiTest {
        val restorationTester = StateRestorationTester(this)
        var currentProfile = syntheticProfile()

        restorationTester.setContent {
            SyntheticCoachScreen(profile = currentProfile)
        }

        onNodeWithText("Doelen").performClick()
        onNode(hasSetTextAction() and hasText("Bestaand profiel"))
            .performScrollTo()
            .performTextReplacement("Onopgeslagen coachnaam")
        currentProfile = syntheticProfile(id = 2L, name = "Ander profiel")

        restorationTester.emulateSaveAndRestore()

        onNode(hasSetTextAction() and hasText("Onopgeslagen coachnaam"))
            .performScrollTo()
            .assertTextContains("Onopgeslagen coachnaam")
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun dirtyDraftSurvivesStateRestorationWhenProfileContentChanges() = runComposeUiTest {
        val restorationTester = StateRestorationTester(this)
        var currentProfile = syntheticProfile(name = "Aa")

        restorationTester.setContent {
            SyntheticCoachScreen(profile = currentProfile)
        }

        onNodeWithText("Doelen").performClick()
        onNode(hasSetTextAction() and hasText("Aa"))
            .performScrollTo()
            .performTextReplacement("Onopgeslagen coachnaam")
        currentProfile = syntheticProfile(name = "BB")

        restorationTester.emulateSaveAndRestore()

        onNode(hasSetTextAction() and hasText("Onopgeslagen coachnaam"))
            .performScrollTo()
            .assertTextContains("Onopgeslagen coachnaam")
    }
}

@Composable
private fun SyntheticCoachScreen(profile: UserProfile) {
    var profileDraft by rememberSaveable { mutableStateOf(profile.toSyntheticDraft()) }
    TrainIqTheme(dynamicColor = false) {
        CoachScreen(
            uiState = syntheticCoachUiState(profile, profileDraft),
            onGenerateAdvice = {},
            onGenerateWeeklyReport = {},
            onSaveProfile = {},
            onProfileDraftChange = { profileDraft = it },
            onDismissMessage = {},
            onRetry = {},
        )
    }
}

private fun syntheticProfile(
    id: Long = 1L,
    name: String = "Bestaand profiel",
): UserProfile = UserProfile(
    id = id,
    name = name,
    age = 34,
    sex = BiologicalSex.FEMALE,
    height = 172.0,
    weight = 68.0,
    bodyFat = 24.0,
    activityLevel = "Moderately active",
    goal = "Sterker worden",
    calorieTarget = 2_100,
    proteinTarget = 130,
    carbsTarget = 240,
    fatTarget = 70,
    trainingFocus = "Progressieve overload",
)

private fun syntheticCoachUiState(
    profile: UserProfile,
    profileDraft: CoachProfileDraft,
): CoachUiState.Success =
    CoachUiState.Success(
        overview = CoachOverview(
            weeklyReport = "Synthetisch weekrapport",
            trainingInsights = emptyList(),
            nutritionCoachMessage = "Synthetische voedingsfeedback",
            profile = profile,
        ),
        currentProfile = profile,
        profileDraft = profileDraft,
        isProfileDraftDirty = profileDraft != profile.toSyntheticDraft(),
    )

private fun UserProfile.toSyntheticDraft() = CoachProfileDraft(
    name = name,
    age = age.toString(),
    sex = sex,
    height = height.toString(),
    weight = weight.toString(),
    bodyFat = bodyFat.toString(),
    activityLevel = activityLevel,
    goal = goal,
    manualCalorieTarget = calorieTarget.toString(),
)
