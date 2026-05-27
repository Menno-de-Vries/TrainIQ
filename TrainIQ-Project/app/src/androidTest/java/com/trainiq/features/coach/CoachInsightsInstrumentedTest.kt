package com.trainiq.features.coach

import android.content.Context
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.room.Room
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.trainiq.MainActivity
import com.trainiq.core.database.ExerciseEntity
import com.trainiq.core.database.MealEntity
import com.trainiq.core.database.PerformedExerciseEntity
import com.trainiq.core.database.RoutineSetEntity
import com.trainiq.core.database.TrainIqDatabase
import com.trainiq.core.database.TrainIqMigrations
import com.trainiq.core.database.UserProfileEntity
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
class CoachInsightsInstrumentedTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    private lateinit var context: Context
    private lateinit var database: TrainIqDatabase

    @Before
    fun seedCoachContext() = runBlocking {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase("trainiq.db")
        database = Room.databaseBuilder(context, TrainIqDatabase::class.java, "trainiq.db")
            .addMigrations(*TrainIqMigrations.All)
            .build()
        val dao = database.dao()
        val now = System.currentTimeMillis()

        dao.upsertUserProfile(
            UserProfileEntity(
                id = 1L,
                name = "QA Coach",
                age = 30,
                sex = "MALE",
                height = 180.0,
                weight = 82.0,
                bodyFat = 18.0,
                activityLevel = "Gemiddeld actief",
                goal = "spieropbouw",
                calorieTarget = 2_800,
                proteinTarget = 170,
                carbsTarget = 330,
                fatTarget = 80,
                trainingFocus = "Progressieve overload",
            ),
        )
        dao.insertRoutines(listOf(WorkoutRoutineEntity(id = 1L, name = "QA Coach Routine", description = "Seeded coach runtime", active = true)))
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
                    orderIndex = 0,
                ),
            ),
        )
        dao.insertRoutineSets(listOf(RoutineSetEntity(id = 10L, workoutExerciseId = 4L, orderIndex = 0, setType = "WORKING", targetReps = 6)))
        dao.importWorkoutSessions(
            listOf(
                WorkoutSessionEntity(
                    id = 12L,
                    date = now - 60_000L,
                    duration = 3_600L,
                    routineId = 1L,
                    workoutDayId = 7L,
                    startedAt = now - 60_000L,
                    endedAt = now,
                    status = "COMPLETED",
                    completed = true,
                ),
            ),
        )
        dao.insertPerformedExercises(listOf(PerformedExerciseEntity(id = 21L, sessionId = 12L, exerciseId = 3L, sourceWorkoutExerciseId = 4L, orderIndex = 0)))
        dao.importWorkoutSets(
            listOf(
                WorkoutSetEntity(
                    id = 31L,
                    sessionId = 12L,
                    exerciseId = 3L,
                    performedExerciseId = 21L,
                    weight = 90.0,
                    reps = 6,
                    rpe = 8.5,
                    setType = "WORKING",
                    restSeconds = 120,
                    orderIndex = 0,
                    completed = true,
                    loggedAt = now,
                    completedAt = now,
                ),
            ),
        )
        dao.insertMeals(
            listOf(
                MealEntity(id = 41L, date = now, mealType = "LUNCH", name = "QA Coach Lunch", calories = 650, protein = 45, carbs = 70, fat = 18),
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
    fun coachShowsTrainingInsightsAndNutritionCoachForSeededContext() {
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.waitForText("Coach")
            compose.onNodeWithText("Coach").performClick()
            compose.waitForText("Weekrapport maken")
            compose.onNodeWithText("Weekrapport maken").performClick()
            compose.waitForText("Samenvatting bijgewerkt.")
            compose.waitForText("Lokale analyse")
            compose.waitForText("Hoogtepunten")
            compose.waitForText("Volgende stap")
            compose.waitForText("Trainingsinzichten")
            compose.waitForText("Actieve routine: QA Coach Routine")
            compose.waitForText("beste geschatte 1RM")
            compose.scrollUntilText("Voedingscoach")
            compose.waitForText("kcal onder je target")
        }
    }

    private fun androidx.compose.ui.test.junit4.ComposeTestRule.waitForText(text: String) {
        waitUntil(timeoutMillis = 30_000L) {
            onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun androidx.compose.ui.test.junit4.ComposeTestRule.scrollUntilText(text: String) {
        repeat(8) {
            if (onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty()) {
                return
            }
            onRoot().performTouchInput { swipeUp() }
            waitForIdle()
        }
        waitForText(text)
    }
}
