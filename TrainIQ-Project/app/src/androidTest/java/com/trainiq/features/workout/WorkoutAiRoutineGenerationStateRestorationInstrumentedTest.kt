package com.trainiq.features.workout

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.StateRestorationTester
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
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
    @Test fun generatorErrorAndActionsRemainReachableAtLargeFontWithFocusedInput() = runComposeUiTest {
        var loading by mutableStateOf(false)
        var error by mutableStateOf<String?>("Gemini gaf geen volledig antwoord. Probeer opnieuw.")
        var calls = 0
        setContent {
            TrainIqTheme(dynamicColor = false) {
                CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 1.5f)) {
                    RoutineGeneratorDialog(isLoading = loading, errorMessage = error, onDismiss = {},
                        onGenerate = { _, _, _, _, _, _, _ -> calls++; loading = true; error = null })
                }
            }
        }
        onNode(hasSetTextAction() and hasText("Trainingsfocus / split")).performScrollTo().performClick()
        onNodeWithText("Gemini gaf geen volledig antwoord. Probeer opnieuw.").assertIsDisplayed()
        onNodeWithText("Opnieuw proberen").assertIsDisplayed().performClick()
        assertEquals(1, calls)
        onNodeWithText("Annuleren").assertIsDisplayed().assertIsNotEnabled()
        onNodeWithText("Opnieuw proberen").assertDoesNotExist()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test fun failedGenerationKeepsDraftForRetryAndUnrelatedErrorsDoNotDismissIt() = runComposeUiTest {
        var message by mutableStateOf<String?>(null)
        var generationError by mutableStateOf<String?>(null)
        var requestedFocus: String? = null
        setContent { SyntheticWorkoutScreen(message, {}, { _, _, focus, _, _, _, _ ->
            requestedFocus = focus
            generationError = null
        }, generationError = generationError, onDismissGenerationError = { generationError = null }) }
        onNodeWithContentDescription("Met AI genereren").performScrollTo().performClick()
        onAllNodes(hasSetTextAction())[0].performTextReplacement("Mijn herstelplan")
        runOnIdle { generationError = "Routine genereren is mislukt." }
        onNodeWithText("Routine genereren is mislukt.").assertIsDisplayed()
        onNodeWithText("Opnieuw proberen").assertIsDisplayed()
        onNodeWithText("Annuleren").assertIsDisplayed()
        onNode(hasSetTextAction() and hasText("Mijn herstelplan")).assertExists()
        runOnIdle { message = "Instelling bijwerken mislukt." }
        onNode(hasSetTextAction() and hasText("Mijn herstelplan")).assertExists()
        onNodeWithText("Opnieuw proberen").performClick()
        assertEquals("Mijn herstelplan", requestedFocus)
        onNodeWithText("Routine genereren is mislukt.").assertDoesNotExist()
        onNodeWithText("Genereren").assertIsDisplayed()
        runOnIdle { generationError = "Routine genereren is mislukt." }
        onNodeWithText("Annuleren").performClick()
        assertEquals(null, generationError)
        onNodeWithContentDescription("Met AI genereren").performScrollTo().performClick()
        onNodeWithText("Opnieuw proberen").assertDoesNotExist()
    }
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun manualRoutineNameAndDialogSurviveStateRestoration() = runComposeUiTest {
        val restorationTester = StateRestorationTester(this)
        var createdRoutineName by mutableStateOf<String?>(null)

        restorationTester.setContent {
            SyntheticWorkoutScreen(
                message = null,
                onCreateRoutine = { createdRoutineName = it },
                onGenerate = { _, _, _, _, _, _, _ -> },
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
                onGenerate = { days, equipment, focus, level, duration, includeDeload, options ->
                    generatedRequest = GeneratedRoutineRequest(days, equipment, focus, level, duration, includeDeload, options)
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
        onNodeWithText("Gevorderd").performScrollTo().assertIsDisplayed().performClick()
        onNodeWithText("Gevorderd").assertIsSelected()
        onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo(60f, 30f..90f, 3)))
            .performSemanticsAction(SemanticsActions.SetProgress) { setProgress -> setProgress(75f) }
        onNodeWithContentDescription("Deload-richtlijn opnemen")
            .performScrollTo()
            .performClick()
        onNodeWithContentDescription("Deload-richtlijn opnemen").assertIsOff()
        onNodeWithText("Meer opties").performScrollTo().performClick()
        onNode(hasSetTextAction() and hasText("Spiergroepprioriteiten")).performTextReplacement("Benen")
        onNode(hasSetTextAction() and hasText("Uitgesloten oefeningen")).performTextReplacement("Plank")

        restorationTester.emulateSaveAndRestore()

        onNodeWithText("AI-routine genereren").assertIsDisplayed()
        onNodeWithContentDescription("Deload-richtlijn opnemen").assertIsOff()
        onNodeWithText("Gevorderd").assertIsSelected()
        onNodeWithText("Genereren").performClick()

        assertEquals(
            GeneratedRoutineRequest(
                daysPerWeek = 4,
                equipment = "Dumbbells",
                focus = "Herstelgericht",
                experienceLevel = "advanced",
                sessionDuration = 75,
                includeDeload = false,
                options = com.trainiq.domain.model.RoutineGenerationOptions(
                    priorityMuscleGroups = listOf("Benen"),
                    excludedExercises = listOf("Plank"),
                ),
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
                onGenerate = { _, _, _, _, _, _, _ -> message = "AI-routine maken..." },
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
    onGenerate: (Int, String, String, String, Int, Boolean, com.trainiq.domain.model.RoutineGenerationOptions) -> Unit,
    generationError: String? = null,
    onDismissGenerationError: () -> Unit = {},
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
            routineGenerationError = generationError,
            onDismissRoutineGenerationError = onDismissGenerationError,
            onDismissMessage = {},
            onStartWorkout = {},
            onOpenExerciseHistory = {},
            onCreateRoutine = { name, _ -> onCreateRoutine(name) },
            onGenerateAiRoutine = onGenerate,
            onSaveGeneratedRoutine = {},
            onReplaceGeneratedExercise = { _, _, _ -> },
            onRemoveGeneratedExercise = { _, _ -> },
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
    val options: com.trainiq.domain.model.RoutineGenerationOptions,
)
