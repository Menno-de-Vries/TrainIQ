package com.trainiq.features.workout

import android.content.Context
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.doubleClick
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
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertCountEquals
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
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            val trainingNavigation = (hasContentDescription("Training") or hasText("Training")) and hasClickAction()
            compose.waitUntil(30_000) { compose.onAllNodes(trainingNavigation).fetchSemanticsNodes().isNotEmpty() }
            compose.onNode(trainingNavigation).performClick()
            compose.waitForText("QA Upper")
            compose.onNodeWithText("Training starten").performClick()
            compose.waitForText("Actieve training")
            capture("normal")

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

            metricInput("Gewicht in kilogram")
                .performScrollTo()
                .performTextReplacement("9999999999999999999999999999999999999999")
            compose.onNodeWithText("Wijzig loggen").performScrollTo()
            compose.onNodeWithText("Wijzig loggen").performSemanticsAction(SemanticsActions.OnClick) { it() }
            compose.onNodeWithContentDescription("Gewicht in kilogram").assert(
                SemanticsMatcher.expectValue(SemanticsProperties.Error, "Voer een gewicht tussen 0 en 1000 kg in."),
            )
            assertEquals(80.0, readActiveWorkoutSet().weight, 0.0)
            compose.onAllNodesWithText("Geschatte 1RM:", substring = true).assertCountEquals(0)
            capture("error")

            metricInput("Gewicht in kilogram").performScrollTo().performClick().performTextReplacement("")
            metricInput("Gewicht in kilogram").assertTextEquals("")
            metricInput("Gewicht in kilogram").performTextInput("82.")
            metricInput("Gewicht in kilogram").assertTextEquals("82.")
            metricInput("Gewicht in kilogram").performTextInput("5")
            metricInput("Gewicht in kilogram").assertTextEquals("82.5")
            compose.waitUntil(10_000) {
                compose.onAllNodesWithText("Voer een gewicht tussen 0 en 1000 kg in.").fetchSemanticsNodes().isEmpty()
            }
            compose.onNodeWithContentDescription("Gewicht in kilogram").performScrollTo()
            capture("input")

            metricInput("Herh.").performClick().performTextReplacement("")
            metricInput("Herh.").assertTextEquals("")
            metricInput("Gewicht in kilogram").performClick()
            metricInput("Herh.").assertTextEquals("5")
            metricInput("Herh.")
                .performScrollTo()
                .performTextReplacement("6")
            metricInput("Herh.").performImeAction()
            metricInput("Gewicht in kilogram").assertIsFocused().performImeAction()
            metricInput("Rust in seconden").assertIsFocused().performImeAction()
            assertEquals(80.0, readActiveWorkoutSet().weight, 0.0)
            compose.onNodeWithText("RPE").assertDoesNotExist()
            compose.onNodeWithText("Wijzig loggen").performScrollTo()
            compose.onNodeWithText("Wijzig loggen").performSemanticsAction(SemanticsActions.OnClick) { it() }

            compose.waitForText("bijgewerkt")
            val updatedSet = readActiveWorkoutSet()
            assertEquals("WARM_UP", updatedSet.setType)
            assertEquals(82.5, updatedSet.weight, 0.0)
            assertEquals(6, updatedSet.reps)
            assertEquals(8.0, updatedSet.rpe, 0.0)
            capture("completed")
            scenario.recreate()
            compose.waitForText("Actieve training")
            assertEquals(82.5, readActiveWorkoutSet().weight, 0.0)
            assertEquals(6, readActiveWorkoutSet().reps)
            compose.waitForText("W")

            compose.onNodeWithContentDescription("Set verwijderen")
                .performScrollTo()
            compose.onNodeWithContentDescription("Set verwijderen")
                .performSemanticsAction(SemanticsActions.OnClick) { it() }
            compose.waitForText("Set verwijderen?")
            compose.onNodeWithText("Verwijderen").performClick()
            compose.waitForNoActiveWorkoutSets()
            compose.waitForText("0 sets gelogd")
            compose.waitUntil(10_000) {
                compose.onAllNodesWithText("Set verwijderd.").fetchSemanticsNodes().isEmpty()
            }
            capture("empty")
            metricInput("Gewicht in kilogram").performScrollTo().performTextReplacement("80")
            metricInput("Herh.").performTextReplacement("5")
            val logButton = compose.onNodeWithText("Set loggen")
            logButton.performScrollTo()
            var bounds = logButton.fetchSemanticsNode().boundsInRoot
            var stableSince = android.os.SystemClock.elapsedRealtime()
            compose.waitUntil(5_000) {
                val next = logButton.fetchSemanticsNode().boundsInRoot
                if (next != bounds) {
                    bounds = next
                    stableSince = android.os.SystemClock.elapsedRealtime()
                }
                android.os.SystemClock.elapsedRealtime() - stableSince >= 500
            }
            logButton.assertIsDisplayed().performClick()
            compose.waitUntil(15_000) {
                runBlocking { database.dao().observeActiveWorkoutSets().first().size == 1 }
            }
            assertEquals(0.0, readActiveWorkoutSet().rpe, 0.0)
            assertEquals(null, readActiveWorkoutSet().repsInReserve)
        }
    }

    @Test
    fun finishConfirmationPersistsOneSessionAndSurvivesRecreation() {
        runBlocking {
            UserPreferencesRepository(context).setAiEnabled(false)
            // An unfinished planned set is required for the existing confirmation policy.
            database.dao().insertRoutineSets(listOf(
                RoutineSetEntity(id = 11L, workoutExerciseId = 4L, orderIndex = 1, setType = "NORMAL", targetReps = 5),
            ))
        }
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            val trainingNavigation = (hasContentDescription("Training") or hasText("Training")) and hasClickAction()
            compose.waitUntil(30_000) { compose.onAllNodes(trainingNavigation).fetchSemanticsNodes().isNotEmpty() }
            compose.onNode(trainingNavigation).performClick()
            compose.waitForText("QA Upper")
            compose.onNodeWithText("Training starten").performClick()
            compose.waitForText("Actieve training")
            compose.onNodeWithContentDescription("Training afronden").performClick()
            compose.onNodeWithText("Training afronden?").assertIsDisplayed()
            compose.onNodeWithText("Opslaan").performTouchInput { doubleClick() }
            compose.waitForText("Voltooid")
            // Inspect the persisted result, not merely the success screen.
            fun assertSavedOnce() = runBlocking {
                val dao = trainIqAndroidTestDatabase(context).dao()
                val sessions = dao.readWorkoutSessionsForExport()
                assertEquals(1, sessions.size)
                assertEquals(12L, sessions.single().id)
                assertEquals(true, sessions.single().completed)
                assertEquals(1, dao.readWorkoutSetsForExport().size)
                assertEquals(0, dao.observeActiveWorkoutSets().first().size)
            }
            assertSavedOnce()
            scenario.recreate()
            compose.waitForText("Voltooid")
            assertSavedOnce()
        }
    }

    private fun capture(state: String) {
        compose.waitForIdle()
        // Compose idleness alone does not wait for platform IME/layout animations.
        var bounds = compose.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().map { it.boundsInRoot }
        var stableSince = android.os.SystemClock.elapsedRealtime()
        compose.waitUntil(5_000) {
            val next = compose.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().map { it.boundsInRoot }
            if (next != bounds) {
                bounds = next
                stableSince = android.os.SystemClock.elapsedRealtime()
            }
            android.os.SystemClock.elapsedRealtime() - stableSince >= 500
        }
        val descriptor = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().uiAutomation
            .executeShellCommand("screencap -p /data/local/tmp/trainiq-workout-$state.png")
        android.os.ParcelFileDescriptor.AutoCloseInputStream(descriptor).use { it.readBytes() }
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
