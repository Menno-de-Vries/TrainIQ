package com.trainiq.features.workout

import android.content.Context
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.room.Room
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.trainiq.MainActivity
import com.trainiq.core.database.ExerciseEntity
import com.trainiq.core.database.PerformedExerciseEntity
import com.trainiq.core.database.RoutineSetEntity
import com.trainiq.core.database.TrainIqDatabase
import com.trainiq.core.database.TrainIqMigrations
import com.trainiq.core.database.WorkoutDayEntity
import com.trainiq.core.database.WorkoutExerciseEntity
import com.trainiq.core.database.WorkoutRoutineEntity
import com.trainiq.core.database.WorkoutSessionEntity
import com.trainiq.core.database.WorkoutSetEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExerciseHistoryInstrumentedTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    private lateinit var context: Context
    private lateinit var database: TrainIqDatabase

    @Before
    fun seedExerciseHistory() = runBlocking {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase("trainiq.db")
        database = Room.databaseBuilder(context, TrainIqDatabase::class.java, "trainiq.db")
            .addMigrations(*TrainIqMigrations.All)
            .build()
        val dao = database.dao()
        val now = System.currentTimeMillis()
        val firstSessionStart = now - 172_800_000L
        val secondSessionStart = now - 86_400_000L

        dao.insertRoutines(listOf(WorkoutRoutineEntity(id = 1L, name = "QA History Routine", description = "Seeded exercise history QA", active = true)))
        dao.insertWorkoutDays(listOf(WorkoutDayEntity(id = 7L, routineId = 1L, name = "Push", orderIndex = 0)))
        dao.insertExercises(listOf(ExerciseEntity(id = 3L, name = "Bench Press", muscleGroup = "Chest", equipment = "Barbell")))
        dao.insertWorkoutExercises(
            listOf(
                WorkoutExerciseEntity(
                    id = 4L,
                    dayId = 7L,
                    exerciseId = 3L,
                    targetSets = 3,
                    repRange = "5-8",
                    restSeconds = 120,
                    targetWeightKg = 90.0,
                    targetRpe = 8.0,
                    setType = "WORKING",
                    orderIndex = 0,
                ),
            ),
        )
        dao.insertRoutineSets(
            listOf(
                RoutineSetEntity(id = 10L, workoutExerciseId = 4L, orderIndex = 0, setType = "WORKING", targetReps = 8, targetWeightKg = 90.0, restSeconds = 120, targetRpe = 8.0),
            ),
        )
        dao.importWorkoutSessions(
            listOf(
                WorkoutSessionEntity(
                    id = 12L,
                    date = firstSessionStart,
                    duration = 3_600L,
                    routineId = 1L,
                    workoutDayId = 7L,
                    startedAt = firstSessionStart,
                    endedAt = firstSessionStart + 3_600_000L,
                    status = "COMPLETED",
                    completed = true,
                ),
                WorkoutSessionEntity(
                    id = 13L,
                    date = secondSessionStart,
                    duration = 3_900L,
                    routineId = 1L,
                    workoutDayId = 7L,
                    startedAt = secondSessionStart,
                    endedAt = secondSessionStart + 3_900_000L,
                    status = "COMPLETED",
                    completed = true,
                ),
            ),
        )
        dao.insertPerformedExercises(
            listOf(
                PerformedExerciseEntity(id = 21L, sessionId = 12L, exerciseId = 3L, sourceWorkoutExerciseId = 4L, orderIndex = 0),
                PerformedExerciseEntity(id = 22L, sessionId = 13L, exerciseId = 3L, sourceWorkoutExerciseId = 4L, orderIndex = 0),
            ),
        )
        dao.importWorkoutSets(
            listOf(
                WorkoutSetEntity(id = 31L, sessionId = 12L, exerciseId = 3L, performedExerciseId = 21L, weight = 82.5, reps = 8, rpe = 8.0, setType = "WORKING", restSeconds = 120, orderIndex = 0, completed = true, loggedAt = firstSessionStart + 120_000L, completedAt = firstSessionStart + 120_000L),
                WorkoutSetEntity(id = 32L, sessionId = 13L, exerciseId = 3L, performedExerciseId = 22L, weight = 90.0, reps = 6, rpe = 8.5, setType = "WORKING", restSeconds = 150, orderIndex = 0, completed = true, loggedAt = secondSessionStart + 120_000L, completedAt = secondSessionStart + 120_000L),
            ),
        )
        database.close()
    }

    @After
    fun closeDatabase() {
        if (::database.isInitialized && database.isOpen) {
            database.close()
        }
    }

    @Test
    fun seededExerciseHistoryOpensFromTrainingAndShowsProgress() {
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.waitForText("Training")
            compose.onNodeWithText("Training").performClick()
            compose.waitForText("QA History Routine")
            compose.onNodeWithText("Training starten")
                .performScrollTo()
                .performClick()
            compose.waitForText("Actieve training")
            compose.onNodeWithContentDescription("Open geschiedenis voor Bench Press")
                .performScrollTo()
                .performClick()

            compose.waitForText("Volume per sessie")
            compose.waitForText("Bench Press")
            compose.waitForText("Sessies")
            compose.waitForText("2")
            compose.waitForText("Beste kg")
            compose.waitForText("90")
        }
    }

    private fun androidx.compose.ui.test.junit4.ComposeTestRule.waitForText(text: String) {
        waitUntil(timeoutMillis = 30_000L) {
            onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty()
        }
    }
}
