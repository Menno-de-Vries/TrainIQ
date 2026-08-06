package com.trainiq.features.coach

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
            TrainIqTheme(dynamicColor = false) {
                CoachScreen(
                    uiState = syntheticCoachUiState(),
                    onGenerateAdvice = { _, _, _, _, _, _, _, _ -> },
                    onGenerateWeeklyReport = {},
                    onSaveProfile = { _, _, _, _, _, _, _, _ -> },
                    onDismissMessage = {},
                )
            }
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
}

private fun syntheticCoachUiState(): CoachUiState.Success {
    val profile = UserProfile(
        id = 1L,
        name = "Bestaand profiel",
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
    return CoachUiState.Success(
        overview = CoachOverview(
            weeklyReport = "Synthetisch weekrapport",
            trainingInsights = emptyList(),
            nutritionCoachMessage = "Synthetische voedingsfeedback",
            profile = profile,
        ),
        currentProfile = profile,
    )
}
