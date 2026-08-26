package com.trainiq.features.workout

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.StateRestorationTester
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.runComposeUiTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.trainiq.core.theme.TrainIqTheme
import com.trainiq.domain.model.WorkoutOverview
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WorkoutAiRoutineGenerationStateRestorationInstrumentedTest {
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun inFlightGenerationRemainsBlockedAcrossStateRestoration() = runComposeUiTest {
        val restorationTester = StateRestorationTester(this)
        var message by mutableStateOf<String?>(null)

        restorationTester.setContent {
            SyntheticWorkoutScreen(
                message = message,
                onGenerate = { message = "AI-routine maken..." },
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
    onGenerate: () -> Unit,
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
            onCreateRoutine = { _, _ -> },
            onGenerateAiRoutine = { _, _, _, _, _, _ -> onGenerate() },
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
