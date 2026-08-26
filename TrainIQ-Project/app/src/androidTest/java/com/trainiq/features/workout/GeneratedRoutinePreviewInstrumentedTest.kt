package com.trainiq.features.workout

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.trainiq.core.theme.TrainIqTheme
import com.trainiq.domain.model.GeneratedDay
import com.trainiq.domain.model.GeneratedExercise
import com.trainiq.domain.model.GeneratedRoutine
import com.trainiq.domain.model.GeneratedRoutineSource
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GeneratedRoutinePreviewInstrumentedTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun generatedRoutinePreviewKeepsBottomActionsReachableAndDispatchesClicks() {
        var saveClicks = 0
        var retryClicks = 0
        var dismissClicks = 0

        compose.setContent {
            TrainIqTheme {
                GeneratedRoutinePreviewDialog(
                    routine = sampleGeneratedRoutine(),
                    isSaving = false,
                    onSave = { saveClicks += 1 },
                    onRetry = { retryClicks += 1 },
                    onDismiss = { dismissClicks += 1 },
                )
            }
        }

        compose.onNodeWithText("QA hypertrofie blok").assertIsDisplayed()
        compose.onNodeWithText("Opslaan")
            .assertIsDisplayed()
            .assertIsEnabled()
            .performClick()
        compose.onNodeWithText("Opnieuw proberen")
            .assertIsDisplayed()
            .assertIsEnabled()
            .performClick()
        compose.onNodeWithText("Annuleren")
            .assertIsDisplayed()
            .assertIsEnabled()
            .performClick()

        assertEquals(1, saveClicks)
        assertEquals(1, retryClicks)
        assertEquals(1, dismissClicks)
    }

    @Test
    fun generatedRoutinePreviewDisablesSaveOnlyWhileSaving() {
        var retryClicks = 0
        var dismissClicks = 0

        compose.setContent {
            TrainIqTheme {
                GeneratedRoutinePreviewDialog(
                    routine = sampleGeneratedRoutine(),
                    isSaving = true,
                    onSave = {},
                    onRetry = { retryClicks += 1 },
                    onDismiss = { dismissClicks += 1 },
                )
            }
        }

        compose.onNodeWithText("Opslaan...")
            .assertIsDisplayed()
            .assertIsNotEnabled()
        compose.onNodeWithText("Opnieuw proberen")
            .assertIsDisplayed()
            .assertIsEnabled()
            .performClick()
        compose.onNodeWithText("Annuleren")
            .assertIsDisplayed()
            .assertIsEnabled()
            .performClick()

        assertEquals(1, retryClicks)
        assertEquals(1, dismissClicks)
    }

    private fun sampleGeneratedRoutine(): GeneratedRoutine =
        GeneratedRoutine(
            routineName = "QA hypertrofie blok",
            routineDescription = "Vierdaagse routine met voldoende inhoud om de preview-scroll en vaste acties te belasten.",
            periodizationNote = "Bouw drie weken op en houd week vier lichter.",
            estimatedDurationMinutes = 65,
            source = GeneratedRoutineSource.GEMINI_2_5_FLASH,
            days = List(5) { dayIndex ->
                GeneratedDay(
                    dayName = "Dag ${dayIndex + 1}",
                    estimatedDurationMinutes = 60 + dayIndex,
                    exercises = List(6) { exerciseIndex ->
                        GeneratedExercise(
                            exerciseName = "Oefening ${dayIndex + 1}.${exerciseIndex + 1}",
                            muscleGroup = "Full body",
                            equipment = "Halters",
                            targetSets = 3,
                            repRange = "8-12",
                            restSeconds = 90,
                            coachingCue = "Houd twee reps in reserve.",
                        )
                    },
                )
            },
        )
}
