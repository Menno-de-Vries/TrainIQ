package com.trainiq.features.workout

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ActiveWorkoutRestoreInstrumentedTest {
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
        dao.insertRoutines(listOf(WorkoutRoutineEntity(id = 1L, name = "QA Upper", description = "Seeded restore QA", active = true)))
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
    fun activeWorkoutRestoresFromRoomAfterActivityRecreation() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            compose.waitUntil(timeoutMillis = 10_000L) {
                compose.onAllNodesWithText("Training").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithText("Training").assertIsDisplayed()
            compose.onNodeWithText("Training").performClick()
            scenario.recreate()
            compose.waitUntil(timeoutMillis = 10_000L) {
                compose.onAllNodesWithText("Training").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithText("Training").assertIsDisplayed()
            compose.onNodeWithText("Training").performClick()
            compose.waitUntil(timeoutMillis = 30_000L) {
                compose.onAllNodesWithText("QA Upper", substring = true).fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithText("Actieve routine").assertIsDisplayed()
        }
    }

}
