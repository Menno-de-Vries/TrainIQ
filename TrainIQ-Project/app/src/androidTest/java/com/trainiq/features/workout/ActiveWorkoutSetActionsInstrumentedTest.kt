package com.trainiq.features.workout

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.trainiq.MainActivity
import com.trainiq.core.datastore.OnboardingPreferences
import com.trainiq.core.datastore.UserPreferencesRepository
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
        UserPreferencesRepository(context).saveOnboardingPreferences(
            OnboardingPreferences(completed = true, guidedTourCompleted = true),
        )
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
            val trainingNavigation = (hasContentDescription("Training") or hasText("Training")) and hasClickAction()
            compose.waitUntil(30_000) { compose.onAllNodes(trainingNavigation).fetchSemanticsNodes().isNotEmpty() }
            compose.onNode(trainingNavigation).performClick()
            compose.waitForText("QA Upper")
            compose.onNodeWithText("Training starten").performClick()
            compose.waitForText("Actieve training")

            compose.waitForText("N")
            compose.onNodeWithText("N")
                .performScrollTo()
            compose.onNodeWithText("N").performSemanticsAction(SemanticsActions.OnClick) { it() }
            compose.waitForText("Warm-up")
            compose.onNodeWithText("Warm-up").performClick()
            compose.waitForText("W")

            compose.onNodeWithContentDescription("Gelogde set corrigeren")
                .performScrollTo()
            compose.onNodeWithContentDescription("Gelogde set corrigeren")
                .performSemanticsAction(SemanticsActions.OnClick) { it() }
            compose.waitForText("Wijzig loggen")

            metricInput("Kg, kg")
                .performScrollTo()
                .performTextReplacement("9999999999999999999999999999999999999999")
            compose.onNodeWithText("Wijzig loggen").performScrollTo()
            compose.onNodeWithText("Wijzig loggen").performSemanticsAction(SemanticsActions.OnClick) { it() }
            compose.onNodeWithContentDescription("Kg, kg").assert(
                SemanticsMatcher.expectValue(SemanticsProperties.Error, "Voer een gewicht tussen 0 en 1000 kg in."),
            )
            assertEquals(80.0, readActiveWorkoutSet().weight, 0.0)

            metricInput("Kg, kg")
                .performScrollTo()
                .performTextReplacement("82.5")
            metricInput("Herh.")
                .performScrollTo()
                .performTextReplacement("6")
            metricInput("RPE")
                .performScrollTo()
                .performTextReplacement("8.5")
            compose.onNodeWithText("Wijzig loggen").performScrollTo()
            compose.onNodeWithText("Wijzig loggen").performSemanticsAction(SemanticsActions.OnClick) { it() }

            compose.waitForText("bijgewerkt")
            val updatedSet = readActiveWorkoutSet()
            assertEquals("WARM_UP", updatedSet.setType)
            assertEquals(82.5, updatedSet.weight, 0.0)
            assertEquals(6, updatedSet.reps)
            assertEquals(8.5, updatedSet.rpe, 0.0)
            compose.waitForText("W")

            compose.onNodeWithContentDescription("Set verwijderen")
                .performScrollTo()
            compose.onNodeWithContentDescription("Set verwijderen")
                .performSemanticsAction(SemanticsActions.OnClick) { it() }
            compose.waitForText("Set verwijderen?")
            compose.onNodeWithText("Verwijderen").performClick()
            compose.waitForNoActiveWorkoutSets()
            compose.waitForText("0 sets gelogd")
        }
    }

    private fun androidx.compose.ui.test.junit4.ComposeTestRule.waitForText(text: String) {
        waitUntil(timeoutMillis = 30_000L) {
            onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun metricInput(description: String): SemanticsNodeInteraction = compose.onNode(
        hasSetTextAction() and hasAnyAncestor(hasContentDescription(description)),
        useUnmergedTree = true,
    )

    private fun androidx.compose.ui.test.junit4.ComposeTestRule.waitForNoActiveWorkoutSets() {
        waitUntil(timeoutMillis = 30_000L) {
            runBlocking { trainIqAndroidTestDatabase(context).dao().observeActiveWorkoutSets().first().isEmpty() }
        }
    }

    private fun readActiveWorkoutSet() = runBlocking {
        trainIqAndroidTestDatabase(context).dao().observeActiveWorkoutSets().first().single()
    }
}
