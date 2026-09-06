package com.trainiq.features.workout

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import com.trainiq.core.theme.TrainIqTheme
import com.trainiq.domain.model.*
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class PlanDraftRestorationInstrumentedTest {
    @Test fun historyErrorExposesRetryAndBack() = runComposeUiTest {
        var retries = 0
        var backs = 0
        setContent { TrainIqTheme {
            ExerciseHistoryScreen(com.trainiq.core.ui.ScreenUiState.Error("Geschiedenis niet beschikbaar", "Probeer opnieuw."),
                onBack = { backs++ }, onRetry = { retries++ })
        } }
        onNodeWithText("Opnieuw proberen").assertIsDisplayed().performClick()
        onNodeWithText("Terug").assertIsDisplayed().performClick()
        org.junit.Assert.assertEquals(1, retries)
        org.junit.Assert.assertEquals(1, backs)
    }
    @Test fun setDraftKeepsTypeAndWeightAcrossRestoration() = runComposeUiTest {
        val restore = StateRestorationTester(this)
        restore.setContent { TrainIqTheme {
            EditSetBottomSheet(RoutineSet(9L, 4L, 0, targetReps = 5, targetWeightKg = 20.0), 1, {}, {})
        } }
        onNode(hasSetTextAction() and hasText("Gewicht")).performTextReplacement("42.5")
        onNodeWithText("Warm-up").performScrollTo().performClick()
        onNodeWithText("Warm-up").assertIsSelected()
        restore.emulateSaveAndRestore()
        onNodeWithText("Warm-up").assertIsSelected()
        onNode(hasSetTextAction() and hasText("Gewicht")).assertTextContains("42.5")
    }
    @Test fun planDraftKeepsInvalidInputAcrossRestoration() = runComposeUiTest {
        val restore = StateRestorationTester(this)
        restore.setContent { TrainIqTheme {
            ExercisePlanEditDialog(WorkoutExercisePlan(4L, Exercise(3L, "Bench", "Chest", "Barbell"), 3, "8", 90),
                onConfirm = { _, _, _, _, _, _ -> }, onDismiss = {})
        } }
        onNode(hasSetTextAction() and hasText("Sets")).performTextReplacement("oops")
        onNodeWithText("Opslaan").assertIsNotEnabled()
        restore.emulateSaveAndRestore()
        onNode(hasSetTextAction() and hasText("Sets")).assertTextContains("oops")
        onNodeWithText("Opslaan").assertIsNotEnabled()
        onNode(hasSetTextAction() and hasText("Sets")).performTextReplacement("4")
        onNodeWithText("Opslaan").assertIsEnabled()
    }
}
