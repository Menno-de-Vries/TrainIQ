package com.trainiq.features.workout

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.trainiq.MainActivity
import com.trainiq.core.database.ActiveWorkoutSessionEntity
import com.trainiq.core.database.ActiveWorkoutSetEntity
import com.trainiq.core.database.ExerciseEntity
import com.trainiq.core.database.PerformedExerciseEntity
import com.trainiq.core.database.RoutineSetEntity
import com.trainiq.core.database.TrainIqDatabase
import com.trainiq.core.database.WorkoutDayEntity
import com.trainiq.core.database.WorkoutExerciseEntity
import com.trainiq.core.database.WorkoutRoutineEntity
import com.trainiq.core.database.WorkoutSessionEntity
import com.trainiq.testing.resetTrainIqAndroidTestDatabase
import com.trainiq.testing.trainIqAndroidTestDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ActiveWorkoutSetActionsInstrumentedTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    private lateinit var context: Context
    private lateinit var database: TrainIqDatabase

    @Before
    fun seedActiveWorkout() = runBlocking {
        context = ApplicationProvider.getApplicationContext()
        database = resetTrainIqAndroidTestDatabase(context)
        val dao = database.dao()
        val now = System.currentTimeMillis()
        dao.insertRoutines(listOf(WorkoutRoutineEntity(id = 1L, name = "QA Upper", description = "Seeded set action QA", active = true)))
        dao.insertWorkoutDays(listOf(WorkoutDayEntity(id = 7L, routineId = 1L, name = "Push", orderIndex = 0)))
        dao.insertExercises(listOf(ExerciseEntity(id = 3L, name = "Bench Press", muscleGroup = "Chest", equipment = "Barbell")))
        dao.insertWorkoutExercises(
            listOf(
                WorkoutExerciseEntity(
                    id = 4L,
                    dayId = 7L,
                    exerciseId = 3L,
                    targetSets = 3,
                    repRange = "5",
                    restSeconds = 120,
                    targetWeightKg = 80.0,
                    targetRpe = 8.0,
                    setType = "NORMAL",
                    orderIndex = 0,
                ),
            ),
        )
        dao.insertRoutineSets(
            listOf(RoutineSetEntity(id = 10L, workoutExerciseId = 4L, orderIndex = 0, setType = "NORMAL", targetReps = 5)),
        )
        dao.insertWorkoutSession(
            WorkoutSessionEntity(
                id = 12L,
                date = now - 60_000L,
                duration = 60,
                routineId = 1L,
                workoutDayId = 7L,
                startedAt = now - 60_000L,
                endedAt = 0L,
                status = "DRAFT",
                completed = false,
            ),
        )
        dao.insertPerformedExercises(
            listOf(PerformedExerciseEntity(id = 21L, sessionId = 12L, exerciseId = 3L, sourceWorkoutExerciseId = 4L, orderIndex = 0)),
        )
        dao.insertActiveWorkoutSessions(
            listOf(ActiveWorkoutSessionEntity(sessionId = 12L, dayId = 7L, routineId = 1L, startedAt = now - 60_000L, updatedAt = now)),
        )
        dao.insertActiveWorkoutSets(
            listOf(
                ActiveWorkoutSetEntity(
                    sessionId = 12L,
                    id = 1L,
                    exerciseId = 3L,
                    performedExerciseId = 21L,
                    sourceWorkoutExerciseId = 4L,
                    weight = 80.0,
                    reps = 5,
                    rpe = 8.0,
                    setType = "NORMAL",
                    restSeconds = 120,
                    orderIndex = 0,
                    completed = true,
                    loggedAt = now,
                ),
            ),
        )
    }

    @Test
    fun loggedSetCanEnterCorrectionModeAndBeDeletedFromActiveWorkout() {
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.waitForText("Training")
            compose.onNodeWithText("Training").performClick()
            compose.waitForText("QA Upper")
            compose.onNodeWithText("Training starten").performClick()
            compose.waitForText("Actieve training")

            compose.onNodeWithContentDescription("Set 1 type Normaal, wijzig naar Warm-up")
                .performScrollTo()
                .performClick()
            compose.waitForText("Set 1 - Warm-up")

            compose.onNodeWithContentDescription("Gelogde set corrigeren")
                .performScrollTo()
                .performClick()
            compose.waitForText("Wijzig loggen")

            compose.onNodeWithContentDescription("Gewicht, kg")
                .performScrollTo()
                .performTextReplacement("82.5")
            compose.onNodeWithContentDescription("Reps")
                .performScrollTo()
                .performTextReplacement("6")
            compose.onNodeWithContentDescription("RPE")
                .performScrollTo()
                .performTextReplacement("8.5")
            compose.onNodeWithText("Wijzig loggen").performScrollTo().performClick()

            compose.waitForText("bijgewerkt")
            val updatedSet = readActiveWorkoutSet()
            assertEquals("WARM_UP", updatedSet.setType)
            assertEquals(82.5, updatedSet.weight, 0.0)
            assertEquals(6, updatedSet.reps)
            assertEquals(8.5, updatedSet.rpe, 0.0)
            compose.waitForText("Set 1 - Warm-up")

            compose.onNodeWithContentDescription("Set verwijderen")
                .performScrollTo()
                .performClick()
            compose.waitForText("Set verwijderen?")
            compose.onNodeWithText("Verwijderen").performClick()
            compose.waitForText("Set verwijderd.")
            compose.waitForText("0 sets gelogd")
        }
    }

    private fun androidx.compose.ui.test.junit4.ComposeTestRule.waitForText(text: String) {
        waitUntil(timeoutMillis = 30_000L) {
            onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun readActiveWorkoutSet() = runBlocking {
        trainIqAndroidTestDatabase(context).dao().observeActiveWorkoutSets().first().single()
    }
}
