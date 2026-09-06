package com.trainiq.features.workout

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.test.espresso.Espresso
import com.trainiq.core.theme.TrainIqTheme
import com.trainiq.domain.model.Exercise
import com.trainiq.domain.model.WorkoutExercisePlan
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class ExercisePlanValidationInstrumentedTest {
    private val bench = Exercise(1L, "Bench Press", "Chest", "Barbell")

    @Test fun editRejectsMalformedValuesThenSavesCorrectedDraft() = runComposeUiTest {
        var submitted: String? = null
        setContent {
            TrainIqTheme {
                ExercisePlanEditDialog(WorkoutExercisePlan(1L, bench, 3, "8-12", 90),
                    onConfirm = { sets, _, _, _, _, _ -> submitted = sets }, onDismiss = {})
            }
        }
        onNode(hasSetTextAction() and hasText("Sets")).performTextReplacement("oops")
        onNodeWithText("Opslaan").assertIsNotEnabled()
        onNodeWithText(PlanValidationMessage).performScrollTo().assertIsDisplayed()
        runOnIdle { assertNull(submitted) }
        onNode(hasSetTextAction() and hasText("Sets")).performScrollTo().performTextReplacement("4")
        onNodeWithText("Opslaan").assertIsEnabled().performClick()
        runOnIdle { assertEquals("4", submitted) }
    }

    @Test fun pickerCannotSubmitInvalidDefaultsAndRecoversWithoutLosingSearch() = runComposeUiTest {
        val sets = mutableStateOf("oops")
        var selected: Exercise? = null
        setContent { TrainIqTheme {
            ExercisePickerSheet(exercises = listOf(bench), showDefaults = true,
                targetSets = sets.value, repRange = "8-12", restSeconds = "90", targetWeightKg = "", targetRpe = "",
                onTargetSetsChange = { sets.value = it }, onRepRangeChange = {}, onRestSecondsChange = {},
                onTargetWeightChange = {}, onTargetRpeChange = {}, onCustomExercise = {},
                onSelect = { selected = it }, onDismiss = {})
        } }
        onNode(hasSetTextAction() and hasText("Oefening zoeken")).performTextReplacement("bench")
        Espresso.closeSoftKeyboard()
        waitForIdle()
        onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Toevoegen"))
        onNodeWithText("Toevoegen").assertIsNotEnabled()
        onNode(hasScrollToIndexAction()).performScrollToNode(hasText(PlanValidationMessage))
        onNodeWithText(PlanValidationMessage).assertIsDisplayed()
        runOnIdle { sets.value = "3" }
        onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Toevoegen"))
        onNodeWithText("Toevoegen").assertIsEnabled().performClick()
        runOnIdle { assertEquals(bench, selected) }
    }

    @Test fun customExerciseKeepsNameWhileInvalidDefaultsAreCorrected() = runComposeUiTest {
        val sets = mutableStateOf("oops")
        var name: String? = null
        setContent { TrainIqTheme {
            CustomExerciseDialog(targetSets = sets.value, repRange = "8-12", restSeconds = "90", targetWeightKg = "", targetRpe = "",
                onTargetSetsChange = { sets.value = it }, onRepRangeChange = {}, onRestSecondsChange = {},
                onTargetWeightChange = {}, onTargetRpeChange = {},
                onConfirm = { entered, _, _ -> name = entered }, onDismiss = {})
        } }
        onNode(hasSetTextAction() and hasText("Oefening")).performScrollTo().performTextReplacement("Eigen press")
        onNode(hasSetTextAction() and hasText("Spiergroep")).performScrollTo().performTextReplacement("Borst")
        onNode(hasSetTextAction() and hasText("Materiaal")).performScrollTo().performTextReplacement("Band")
        onNodeWithText("Toevoegen").assertIsNotEnabled()
        runOnIdle { sets.value = "3" }
        onNodeWithText("Toevoegen").assertIsEnabled().performClick()
        runOnIdle { assertEquals("Eigen press", name) }
    }
}
