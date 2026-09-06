package com.trainiq.features.workout

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.StateRestorationTester
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.runComposeUiTest
import com.trainiq.core.theme.TrainIqTheme
import com.trainiq.domain.model.Exercise
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class ExerciseEditorRecoveryInstrumentedTest {
    @Test
    fun productionPickerSearchesDutchMetadataRestoresQueryAndShowsNoResults() = runComposeUiTest {
        val restore = StateRestorationTester(this)
        var dismissed = false
        var selected: Exercise? = null
        val bench = Exercise(1L, "Bench Press", "Chest", "Barbell")
        restore.setContent {
            TrainIqTheme {
                ExercisePickerSheet(
                    exercises = listOf(bench), showDefaults = false,
                    targetSets = "3", repRange = "8-12", restSeconds = "90", targetWeightKg = "", targetRpe = "",
                    onTargetSetsChange = {}, onRepRangeChange = {}, onRestSecondsChange = {},
                    onTargetWeightChange = {}, onTargetRpeChange = {}, onCustomExercise = {},
                    onSelect = { selected = it }, onDismiss = { dismissed = true },
                )
            }
        }
        onNode(hasSetTextAction() and hasText("Oefening zoeken")).performTextReplacement("borst halterstang")
        onNodeWithText("Bench Press").performScrollTo().assertIsDisplayed()
        restore.emulateSaveAndRestore()
        onNode(hasSetTextAction() and hasText("Oefening zoeken")).assertTextContains("borst halterstang")
        onNodeWithText("Vervangen").performScrollTo().performClick()
        runOnIdle { assertEquals(bench, selected) }
        onNode(hasSetTextAction() and hasText("Oefening zoeken")).performScrollTo().performTextReplacement("onbekend")
        onNodeWithText(exerciseSearchEmptyText()).performScrollTo().assertIsDisplayed()
        onNodeWithContentDescription("Oefeningkiezer sluiten").performScrollTo().performClick()
        runOnIdle { assertTrue(dismissed) }
    }

    @Test
    fun productionCustomDialogRetainsAllDraftFieldsAndValidatesAfterRestoration() = runComposeUiTest {
        val restore = StateRestorationTester(this)
        var submitted: List<String>? = null
        restore.setContent {
            TrainIqTheme {
                CustomExerciseDialog(
                    targetSets = "3", repRange = "8-12", restSeconds = "90", targetWeightKg = "", targetRpe = "",
                    onTargetSetsChange = {}, onRepRangeChange = {}, onRestSecondsChange = {},
                    onTargetWeightChange = {}, onTargetRpeChange = {},
                    onConfirm = { name, muscle, equipment -> submitted = listOf(name, muscle, equipment) }, onDismiss = {},
                )
            }
        }
        onNodeWithText("Toevoegen").assertIsNotEnabled()
        onNode(hasSetTextAction() and hasText("Oefening")).performScrollTo().performTextReplacement("Eigen press")
        onNode(hasSetTextAction() and hasText("Spiergroep")).performScrollTo().performTextReplacement("Borst")
        onNode(hasSetTextAction() and hasText("Materiaal")).performScrollTo().performTextReplacement("Band")
        restore.emulateSaveAndRestore()
        onNode(hasSetTextAction() and hasText("Oefening")).performScrollTo().assertTextContains("Eigen press")
        onNode(hasSetTextAction() and hasText("Spiergroep")).performScrollTo().assertTextContains("Borst")
        onNode(hasSetTextAction() and hasText("Materiaal")).performScrollTo().assertTextContains("Band")
        onNodeWithText("Toevoegen").assertIsDisplayed().performClick()
        runOnIdle { assertEquals(listOf("Eigen press", "Borst", "Band"), submitted) }
    }

    @Test
    fun customDialogCancelSemanticsDoesNotSubmit() = runComposeUiTest {
        val open = mutableStateOf(true)
        var submitted = false
        setContent {
            TrainIqTheme {
                if (open.value) CustomExerciseDialog(
                    targetSets = "3", repRange = "8-12", restSeconds = "90", targetWeightKg = "", targetRpe = "",
                    onTargetSetsChange = {}, onRepRangeChange = {}, onRestSecondsChange = {},
                    onTargetWeightChange = {}, onTargetRpeChange = {},
                    onConfirm = { _, _, _ -> submitted = true }, onDismiss = { open.value = false },
                )
            }
        }
        onNode(hasSetTextAction() and hasText("Oefening")).performScrollTo().performTextReplacement("Niet opslaan")
        onNodeWithText("Annuleren").assertIsDisplayed().performSemanticsAction(SemanticsActions.OnClick) { it() }
        runOnIdle { assertTrue(!open.value && !submitted) }
    }
}
