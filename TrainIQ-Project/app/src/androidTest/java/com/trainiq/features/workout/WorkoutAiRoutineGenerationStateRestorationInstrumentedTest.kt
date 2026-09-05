package com.trainiq.features.workout

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.StateRestorationTester
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsActions
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.trainiq.core.theme.TrainIqTheme
import com.trainiq.domain.model.WorkoutOverview
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WorkoutAiRoutineGenerationStateRestorationInstrumentedTest {
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun manualRoutineNameAndDialogSurviveStateRestoration() = runComposeUiTest {
        val restorationTester = StateRestorationTester(this)
        var createdRoutineName by mutableStateOf<String?>(null)

        restorationTester.setContent {
            SyntheticWorkoutScreen(
                message = null,
                onCreateRoutine = { createdRoutineName = it },
                onGenerate = { _, _, _, _, _, _ -> },
            )
        }

        onNodeWithContentDescription("Lege routine maken")
            .performScrollTo()
            .performClick()
        onNode(hasSetTextAction() and hasText("Routinenaam"))
            .performTextReplacement("Herstelroutine")

        restorationTester.emulateSaveAndRestore()

        onNodeWithText("Routine maken").assertIsDisplayed()
        onNode(hasSetTextAction() and hasText("Herstelroutine")).performClick()
        onNodeWithText("Maken").performClick()

        assertEquals("Herstelroutine", createdRoutineName)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun aiRoutineDraftFieldsSurviveStateRestorationBeforeGenerate() = runComposeUiTest {
        val restorationTester = StateRestorationTester(this)
        var generatedRequest by mutableStateOf<GeneratedRoutineRequest?>(null)

        restorationTester.setContent {
            SyntheticWorkoutScreen(
                message = null,
                onCreateRoutine = {},
                onGenerate = { days, equipment, focus, level, duration, includeDeload ->
                    generatedRequest = GeneratedRoutineRequest(days, equipment, focus, level, duration, includeDeload)
                },
            )
        }

        onNodeWithContentDescription("Met AI genereren")
            .performScrollTo()
            .performClick()
        val inputs = onAllNodes(hasSetTextAction())
        inputs[0].performTextReplacement("Herstelgericht")
        inputs[1].performTextReplacement("4")
        inputs[2].performTextReplacement("Dumbbells")
        onNodeWithText("Gevorderd").performClick()
        onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo(60f, 30f..90f, 3)))
            .performSemanticsAction(SemanticsActions.SetProgress) { setProgress -> setProgress(75f) }
        onNodeWithContentDescription("Deload-richtlijn opnemen")
            .performScrollTo()
            .performClick()
        onNodeWithContentDescription("Deload-richtlijn opnemen").assertIsOff()

        restorationTester.emulateSaveAndRestore()

        onNodeWithText("AI-routine genereren").assertIsDisplayed()
        onNodeWithContentDescription("Deload-richtlijn opnemen").assertIsOff()
        onNodeWithText("Genereren").performClick()

        assertEquals(
            GeneratedRoutineRequest(
                daysPerWeek = 4,
                equipment = "Dumbbells",
                focus = "Herstelgericht",
                experienceLevel = "advanced",
                sessionDuration = 75,
                includeDeload = false,
            ),
            generatedRequest,
        )
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun inFlightGenerationRemainsBlockedAcrossStateRestoration() = runComposeUiTest {
        val restorationTester = StateRestorationTester(this)
        var message by mutableStateOf<String?>(null)

        restorationTester.setContent {
            SyntheticWorkoutScreen(
                message = message,
                onCreateRoutine = {},
                onGenerate = { _, _, _, _, _, _ -> message = "AI-routine maken..." },
            )
        }

        onNodeWithContentDescription("Met AI genereren")
            .performScrollTo()
            .performClick()
        onNodeWithText("Genereren").performClick()
        onNodeWithText("Genereren").assertDoesNotExist()

        restorationTester.emulateSaveAndRestore()

        onNodeWithText("AI-routine genereren").assertIsDisplayed()
        onNodeWithText("Genereren").assertDoesNotExist()
    }
}

@Composable
private fun SyntheticWorkoutScreen(
    message: String?,
    onCreateRoutine: (String) -> Unit,
    onGenerate: (Int, String, String, String, Int, Boolean) -> Unit,
) {
    TrainIqTheme(dynamicColor = false) {
        WorkoutScreen(
            overview = WorkoutOverview(
                activeRoutine = null,
                routines = emptyList(),
                exercises = emptyList(),
                history = emptyList(),
            ),
            message = message,
            pendingGeneratedRoutine = null,
            isSavingGeneratedRoutine = false,
            isGeneratingAiRoutine = message == "AI-routine maken...",
            onDismissMessage = {},
            onStartWorkout = {},
            onOpenExerciseHistory = {},
            onCreateRoutine = { name, _ -> onCreateRoutine(name) },
            onGenerateAiRoutine = onGenerate,
            onSaveGeneratedRoutine = {},
            onRetryGeneratedRoutine = {},
            onDismissGeneratedRoutine = {},
            onUpdateRoutine = { _, _, _ -> true },
            onDeleteRoutine = {},
            onSetActiveRoutine = {},
            onAddDay = { _, _ -> },
            onRemoveDay = {},
            onAddExercise = { _, _, _, _, _, _, _, _, _ -> },
            onAddExerciseToRoutine = { _, _, _, _, _, _, _, _, _ -> },
            onRemoveExercise = {},
            onReorderExercises = { _, _ -> },
            onSetSupersetGroup = { _, _ -> },
            onReplaceExercise = { _, _ -> },
            onUpdateWorkoutExercisePlan = { _, _, _, _, _, _, _ -> },
            onAddSetToExercise = {},
            onUpdateRoutineSet = {},
            onDeleteRoutineSet = {},
            onMoveRoutineSet = { _, _ -> },
            onDeleteWorkoutSession = {},
        )
    }
}

private data class GeneratedRoutineRequest(
    val daysPerWeek: Int,
    val equipment: String,
    val focus: String,
    val experienceLevel: String,
    val sessionDuration: Int,
    val includeDeload: Boolean,
)
