package com.trainiq.features.coach

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.StateRestorationTester
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
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
    fun existingProfileDraftSurvivesStateRestoration() = runComposeUiTest {
        val restorationTester = StateRestorationTester(this)

        restorationTester.setContent {
            SyntheticCoachScreen(profile = syntheticProfile())
        }

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
    fun restoredDraftIsDiscardedWhenProfileIdChanges() = runComposeUiTest {
        val restorationTester = StateRestorationTester(this)
        var currentProfile = syntheticProfile()

        restorationTester.setContent {
            SyntheticCoachScreen(profile = currentProfile)
        }

        onNode(hasSetTextAction() and hasText("Bestaand profiel"))
            .performScrollTo()
            .performTextReplacement("Onopgeslagen coachnaam")
        currentProfile = syntheticProfile(id = 2L, name = "Ander profiel")

        restorationTester.emulateSaveAndRestore()

        onNode(hasSetTextAction() and hasText("Ander profiel"))
            .performScrollTo()
            .assertTextContains("Ander profiel")
        onNode(hasSetTextAction() and hasText("Onopgeslagen coachnaam"))
            .assertDoesNotExist()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun restoredDraftIsDiscardedWhenSameProfileIdHasHashCollidingContent() = runComposeUiTest {
        val restorationTester = StateRestorationTester(this)
        var currentProfile = syntheticProfile(name = "Aa")

        restorationTester.setContent {
            SyntheticCoachScreen(profile = currentProfile)
        }

        onNode(hasSetTextAction() and hasText("Aa"))
            .performScrollTo()
            .performTextReplacement("Onopgeslagen coachnaam")
        currentProfile = syntheticProfile(name = "BB")

        restorationTester.emulateSaveAndRestore()

        onNode(hasSetTextAction() and hasText("BB"))
            .performScrollTo()
            .assertTextContains("BB")
        onNode(hasSetTextAction() and hasText("Onopgeslagen coachnaam"))
            .assertDoesNotExist()
    }
}

@Composable
private fun SyntheticCoachScreen(profile: UserProfile) {
    TrainIqTheme(dynamicColor = false) {
        CoachScreen(
            uiState = syntheticCoachUiState(profile),
            onGenerateAdvice = { _, _, _, _, _, _, _, _ -> },
            onGenerateWeeklyReport = {},
            onSaveProfile = { _, _, _, _, _, _, _, _ -> },
            onDismissMessage = {},
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

private fun syntheticCoachUiState(profile: UserProfile): CoachUiState.Success =
    CoachUiState.Success(
        overview = CoachOverview(
            weeklyReport = "Synthetisch weekrapport",
            trainingInsights = emptyList(),
            nutritionCoachMessage = "Synthetische voedingsfeedback",
            profile = profile,
        ),
        currentProfile = profile,
    )
