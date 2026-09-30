package com.trainiq.features.workout

import android.content.Context
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.click
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasAnyDescendant
import androidx.lifecycle.Lifecycle
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
import org.junit.Assert.assertTrue
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

    @Test
    fun collapsingExerciseHidesSetDetailsWithoutChangingLoggedSet() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            val trainingNavigation = (hasContentDescription("Training") or hasText("Training")) and hasClickAction()
            compose.waitUntil(30_000) { compose.onAllNodes(trainingNavigation).fetchSemanticsNodes().isNotEmpty() }
            compose.onNode(trainingNavigation).performClick()
            compose.waitForText("QA Upper")
            compose.onNodeWithText("Training starten").performClick()
            compose.waitForText("Actieve training")
            compose.onNodeWithContentDescription("Gelogde set corrigeren").assertExists()
            compose.onNodeWithContentDescription("Klap oefening in").performScrollTo()
                .performSemanticsAction(SemanticsActions.OnClick) { it() }
            compose.waitUntil(10_000) { runBlocking { database.dao().readActiveWorkoutCollapsedExercisesForExport().isNotEmpty() } }
            compose.waitUntil(10_000) { compose.onAllNodes(hasContentDescription("Open oefening")).fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithContentDescription("Open oefening").assertExists()
            compose.onAllNodes(hasContentDescription("Gelogde set corrigeren")).assertCountEquals(0)
            compose.onRoot().performTouchInput {
                swipe(start = Offset(width * 0.5f, height * 0.7f), end = Offset(width * 0.5f, height * 0.3f))
            }
            compose.waitUntil(10_000) { compose.onAllNodesWithText("Actieve training hersteld.").fetchSemanticsNodes().isEmpty() }
            capture("collapsed")
            assertEquals(1, runBlocking { database.dao().readActiveWorkoutSetsForExport().size })
            scenario.recreate()
            compose.waitForText("Actieve training")
            compose.onNodeWithContentDescription("Open oefening").performScrollTo()
                .performSemanticsAction(SemanticsActions.OnClick) { it() }
            compose.waitUntil(10_000) { compose.onAllNodes(hasContentDescription("Gelogde set corrigeren")).fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithContentDescription("Gelogde set corrigeren").assertExists()
            compose.onRoot().performTouchInput {
                swipe(start = Offset(width * 0.5f, height * 0.7f), end = Offset(width * 0.5f, height * 0.3f))
            }
            capture("expanded")
            assertEquals(1, runBlocking { database.dao().readActiveWorkoutSetsForExport().size })
        }
    }

    @Test
    fun longExerciseNameRemainsReadableAndLoggerActionsStayAvailable() {
        val longExerciseName = "Custom movement " + "with a deliberately long name ".repeat(4)
        runBlocking {
            database.dao().deleteActiveWorkoutSet(sessionId = 12L, setId = 1L)
            database.dao().insertExercise(
                ExerciseEntity(id = 3L, name = longExerciseName, muscleGroup = "Chest", equipment = "Barbell"),
            )
        }

        ActivityScenario.launch(MainActivity::class.java).use {
            val trainingNavigation = (hasContentDescription("Training") or hasText("Training")) and hasClickAction()
            compose.waitUntil(30_000) { compose.onAllNodes(trainingNavigation).fetchSemanticsNodes().isNotEmpty() }
            compose.onNode(trainingNavigation).performClick()
            compose.waitForText("QA Upper")
            compose.onNodeWithText("Training starten").performClick()
            compose.waitForText("Actieve training")

            compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText(longExerciseName))
            val titleNode = compose.onNodeWithText(longExerciseName, useUnmergedTree = true)
            val titleLayouts = mutableListOf<TextLayoutResult>()
            titleNode.assertTextEquals(longExerciseName)
                .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(titleLayouts) }
            assertEquals(1, titleLayouts.size)
            assertTrue("Long exercise titles must wrap beyond the former three-line cap", titleLayouts.single().lineCount > 3)
            assertTrue(
                "Exercise title must not be ellipsized",
                (0 until titleLayouts.single().lineCount).none(titleLayouts.single()::isLineEllipsized),
            )
            val titleBounds = titleNode.fetchSemanticsNode().boundsInRoot
            val scrollBounds = compose.onNode(hasScrollToIndexAction()).fetchSemanticsNode().boundsInRoot
            assertTrue(
                "Entire exercise title must be inside the visible workout list: title=$titleBounds viewport=$scrollBounds",
                titleBounds.top >= scrollBounds.top && titleBounds.bottom <= scrollBounds.bottom,
            )
            compose.onNodeWithContentDescription("Klap oefening in").assertExists()
            compose.onNodeWithContentDescription("Set toevoegen aan $longExerciseName").performScrollTo().performClick()
            compose.onNodeWithText("Set loggen").performScrollTo().performClick()
            val firstSetTapAt = android.os.SystemClock.elapsedRealtime()

            // Observe the short-lived Undo Snackbar before waiting for persistence/debounce.
            compose.waitUntil(5_000) {
                compose.onAllNodesWithText("Ongedaan maken").fetchSemanticsNodes().isNotEmpty()
            }
            compose.waitUntil(5_000) {
                runBlocking { database.dao().observeActiveWorkoutSets().first().size == 1 } &&
                    compose.onAllNodesWithText("Extra set loggen").fetchSemanticsNodes().isNotEmpty() &&
                    android.os.SystemClock.elapsedRealtime() - firstSetTapAt >= 400L
            }
            val extraSetAction = compose.onNodeWithText("Extra set loggen").performScrollTo()
            extraSetAction.assertIsDisplayed()
            extraSetAction.performClick()
            compose.waitUntil(15_000) {
                runBlocking { database.dao().observeActiveWorkoutSets().first().size == 2 } ||
                    compose.onAllNodesWithText("Set loggen is mislukt", substring = true).fetchSemanticsNodes().isNotEmpty() ||
                    compose.onAllNodesWithText("Deze set wordt al opgeslagen", substring = true).fetchSemanticsNodes().isNotEmpty()
            }
            val loggedSetCount = runBlocking { database.dao().observeActiveWorkoutSets().first().size }
            assertEquals("Both logger actions must persist a set", 2, loggedSetCount)
        }
    }

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
            compose.onNodeWithText("Bench Press", useUnmergedTree = true).assertIsDisplayed()
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
    fun twoExerciseSessionKeepsNotesTimersAndSetsThroughResumeAndFinish() {
        runBlocking {
            val dao = database.dao()
            UserPreferencesRepository(context).setAiEnabled(false)
            dao.insertExercises(listOf(ExerciseEntity(id = 5L, name = "Seated Row", muscleGroup = "Back", equipment = "Cable")))
            dao.insertWorkoutExercises(listOf(WorkoutExerciseEntity(
                id = 6L, dayId = 7L, exerciseId = 5L, targetSets = 1, repRange = "8",
                restSeconds = 90, targetWeightKg = 40.0, targetRpe = 0.0, setType = "NORMAL", orderIndex = 1,
            )))
            dao.insertRoutineSets(listOf(
                RoutineSetEntity(id = 11L, workoutExerciseId = 4L, orderIndex = 1, setType = "NORMAL", targetReps = 5, restSeconds = 120),
                RoutineSetEntity(id = 12L, workoutExerciseId = 4L, orderIndex = 2, setType = "NORMAL", targetReps = 5, restSeconds = 120),
                RoutineSetEntity(id = 20L, workoutExerciseId = 6L, orderIndex = 0, setType = "NORMAL", targetReps = 8, restSeconds = 90),
            ))
            dao.insertPerformedExercises(listOf(PerformedExerciseEntity(
                id = 22L, sessionId = 12L, exerciseId = 5L, sourceWorkoutExerciseId = 6L, orderIndex = 1,
            )))
        }
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            val trainingNavigation = (hasContentDescription("Training") or hasText("Training")) and hasClickAction()
            compose.waitUntil(30_000) { compose.onAllNodes(trainingNavigation).fetchSemanticsNodes().isNotEmpty() }
            compose.onNode(trainingNavigation).performClick()
            compose.waitForText("QA Upper")
            compose.onNodeWithText("Training starten").performClick()
            compose.waitForText("Actieve training")
            compose.onNodeWithText("Workout-notitie").performScrollTo().performTextReplacement("Twee oefeningen, rustig tempo")
            compose.waitUntil(10_000) {
                compose.onAllNodesWithText("Actieve training hersteld.").fetchSemanticsNodes().isEmpty()
            }
            compose.onNodeWithText("Notitie opslaan").performScrollTo().performClick()
            compose.waitUntil(10_000) {
                runBlocking { database.dao().observeActiveWorkoutSessions().first().single().notes == "Twee oefeningen, rustig tempo" }
            }

            compose.waitUntil(10_000) {
                compose.onAllNodesWithText("Actieve training hersteld.").fetchSemanticsNodes().isEmpty()
            }
            exerciseControl("Bench Press", "Rusttimer 30 seconden langer").performScrollTo().performClick()
            capture("rest-control")
            compose.waitUntil(10_000) {
                compose.onAllNodes(hasContentDescription("Rust voor deze oefening: 150s"), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
            }
            exerciseControl("Bench Press", "Gewicht in kilogram", input = true).performScrollTo().performClick()
                .performTextReplacement("82.5")
            exerciseControl("Bench Press", "Herh.", input = true).performClick().performTextReplacement("6")
            exerciseControl("Bench Press", "Herh.", input = true).performImeAction()
            exerciseControl("Bench Press", "Gewicht in kilogram", input = true).assertIsFocused().performImeAction()
            exerciseControl("Bench Press", "Rust in seconden", input = true).assertIsFocused().performImeAction()
            compose.waitUntil(15_000) {
                compose.onAllNodesWithText("Rust voor deze oefening ingesteld", substring = true).fetchSemanticsNodes().isEmpty()
            }
            exerciseText("Bench Press", "Set loggen").performScrollTo().assertIsDisplayed().performClick()
            waitForSetCount(2)
            compose.waitUntil(10_000) {
                runBlocking { database.dao().observeActiveWorkoutSessions().first().single().restTimerTotalSeconds == 150 }
            }
            val bottom = hasAnyAncestor(hasContentDescription("Training afronden", substring = true))
            compose.onNode(hasContentDescription("Rusttimer 30 seconden langer") and bottom, useUnmergedTree = true)
                .assertIsDisplayed().performClick()
            compose.waitUntil(10_000) {
                runBlocking { database.dao().observeActiveWorkoutSessions().first().single().restTimerTotalSeconds > 150 }
            }
            compose.onNode(hasContentDescription("Rusttimer overslaan") and bottom, useUnmergedTree = true).performClick()
            compose.waitUntil(10_000) {
                runBlocking { database.dao().observeActiveWorkoutSessions().first().single().restTimerEndsAt == null }
            }
            compose.waitUntil(15_000) {
                compose.onAllNodesWithText("Set 2 gelogd", substring = true).fetchSemanticsNodes().isEmpty()
                    && compose.onAllNodesWithText("Rusttimer overgeslagen.").fetchSemanticsNodes().isEmpty()
            }
            exerciseText("Bench Press", "Set loggen").performScrollTo().performClick()
            waitForSetCount(3)

            // Resume through both app backgrounding and the existing Training navigation.
            scenario.moveToState(Lifecycle.State.CREATED)
            scenario.moveToState(Lifecycle.State.RESUMED)
            // Resume preserves scroll; the header may be outside the lazy composition.
            compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Actieve training"))
            compose.waitForText("Actieve training")
            compose.onNodeWithContentDescription("Terug naar Training").performScrollTo().performClick()
            compose.waitForText("QA Upper")
            compose.onNodeWithText("Training starten").performClick()
            compose.waitForText("Actieve training")
            assertEquals(3, runBlocking { database.dao().readActiveWorkoutSetsForExport().size })
            compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Seated Row"))
            exerciseControl("Seated Row", "Gewicht in kilogram", input = true).performScrollTo()
                .performTextReplacement("40")
            exerciseControl("Seated Row", "Herh.", input = true).performTextReplacement("8")
            exerciseControl("Seated Row", "Rust in seconden", input = true).performImeAction()
            exerciseText("Seated Row", "Set loggen").performScrollTo().assertIsDisplayed().performClick()
            waitForSetCount(4)
            capture("two-exercise-completed")
            compose.onNodeWithContentDescription("Training afronden").assertIsDisplayed().performClick()
            compose.waitForText("Voltooid")
            runBlocking {
                val dao = trainIqAndroidTestDatabase(context).dao()
                val session = dao.readWorkoutSessionsForExport().single()
                assertTrue(session.completed)
                assertEquals("Twee oefeningen, rustig tempo", session.notes)
                val sets = dao.readWorkoutSetsForExport()
                assertEquals(3, sets.count { it.exerciseId == 3L })
                assertEquals(1, sets.count { it.exerciseId == 5L })
                assertEquals(0, dao.readActiveWorkoutSetsForExport().size)
            }
        }
    }

    private fun exerciseScope(name: String) = hasAnyAncestor(
        SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Uitgeklapt") and hasAnyDescendant(hasText(name)),
    )

    private fun exerciseControl(name: String, description: String, input: Boolean = false): SemanticsNodeInteraction =
        compose.onNode(
            (if (input) hasSetTextAction() and hasAnyAncestor(hasContentDescription(description)) else hasContentDescription(description)) and exerciseScope(name),
            useUnmergedTree = true,
        )

    private fun exerciseText(name: String, text: String): SemanticsNodeInteraction =
        compose.onNode(hasText(text) and exerciseScope(name), useUnmergedTree = true)

    private fun waitForSetCount(count: Int) = compose.waitUntil(15_000) {
        runBlocking { database.dao().readActiveWorkoutSetsForExport().size == count }
    }

    @Test
    fun notesAndCollapsedExerciseSurviveRecreation() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            val trainingNavigation = (hasContentDescription("Training") or hasText("Training")) and hasClickAction()
            compose.waitUntil(30_000) { compose.onAllNodes(trainingNavigation).fetchSemanticsNodes().isNotEmpty() }
            compose.onNode(trainingNavigation).performClick()
            compose.waitForText("QA Upper")
            compose.onNodeWithText("Training starten").performClick()
            compose.waitForText("Actieve training")
            compose.onNodeWithText("Workout-notitie").performScrollTo().performTextReplacement("Rustig opbouwen vandaag")
            compose.waitUntil(10_000) {
                compose.onAllNodesWithText("Actieve training hersteld.").fetchSemanticsNodes().isEmpty()
            }
            compose.onNodeWithText("Notitie opslaan").performScrollTo().performClick()
            compose.waitUntil(10_000) {
                runBlocking { database.dao().observeActiveWorkoutSessions().first().single().notes == "Rustig opbouwen vandaag" }
            }
            compose.onNodeWithContentDescription("Klap oefening in").performScrollTo().performClick()
            compose.onNodeWithContentDescription("Open oefening").assertExists()
            compose.onNodeWithContentDescription("Set toevoegen aan Bench Press").assertDoesNotExist()
            scenario.recreate()
            compose.waitForText("Actieve training")
            compose.onNodeWithText("Rustig opbouwen vandaag").performScrollTo().assertIsDisplayed()
            // The restoration Snackbar can cover the collapsed exercise's toggle on compact screens.
            compose.waitUntil(10_000) {
                compose.onAllNodesWithText("Actieve training hersteld.").fetchSemanticsNodes().isEmpty()
            }
            compose.onNodeWithContentDescription("Open oefening").performScrollTo()
            capture("recreated-before-open")
            compose.onNodeWithContentDescription("Open oefening").performClick()
            capture("recreated-after-open")
            // Collapse changes persist through a coroutine; Compose idleness alone does not await it.
            compose.waitUntil(10_000) {
                compose.onAllNodes(hasContentDescription("Set toevoegen aan Bench Press")).fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithContentDescription("Set toevoegen aan Bench Press").performScrollTo().assertIsDisplayed()
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
            // Finishing must flush an unsaved note before navigating to processing.
            compose.onNodeWithText("Workout-notitie").performScrollTo().performTextReplacement("Goede sessie")
            compose.onNodeWithContentDescription("Training afronden").performClick()
            compose.waitForText("Training afronden?")
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
                assertEquals("Goede sessie", sessions.single().notes)
                assertEquals(1, dao.readWorkoutSetsForExport().size)
                assertEquals(0, dao.observeActiveWorkoutSets().first().size)
            }
            assertSavedOnce()
            scenario.recreate()
            compose.waitForText("Voltooid")
            assertSavedOnce()
        }
    }

    @Test
    fun editingFieldsOnlyChangesDraftAndDoubleTapLogsExactlyOneSet() {
        runBlocking {
            database.dao().insertRoutineSets(
                listOf(
                    RoutineSetEntity(id = 11L, workoutExerciseId = 4L, orderIndex = 1, setType = "NORMAL", targetReps = 5),
                    RoutineSetEntity(id = 12L, workoutExerciseId = 4L, orderIndex = 2, setType = "NORMAL", targetReps = 5),
                ),
            )
        }
        ActivityScenario.launch(MainActivity::class.java).use {
            val trainingNavigation = (hasContentDescription("Training") or hasText("Training")) and hasClickAction()
            compose.waitUntil(30_000) { compose.onAllNodes(trainingNavigation).fetchSemanticsNodes().isNotEmpty() }
            compose.onNode(trainingNavigation).performClick()
            compose.waitForText("QA Upper")
            compose.onNodeWithText("Training starten").performClick()
            compose.waitForText("Actieve training")

            metricInput("Gewicht in kilogram").performScrollTo().performClick()
                .performTextReplacement("82,5")
            metricInput("Herh.").performClick().performTextReplacement("6")
            assertEquals(1, runBlocking { database.dao().observeActiveWorkoutSets().first().size })

            compose.onNodeWithText("Set 2").performScrollTo().performTouchInput { click() }
            assertEquals(1, runBlocking { database.dao().observeActiveWorkoutSets().first().size })

            metricInput("Rust in seconden").performScrollTo().performClick()
            metricInput("Rust in seconden").performTextReplacement("90")
            metricInput("Rust in seconden").performImeAction()
            compose.onRoot().performTouchInput {
                swipe(start = Offset(width * 0.5f, height * 0.7f), end = Offset(width * 0.5f, height * 0.3f))
            }
            assertEquals(1, runBlocking { database.dao().observeActiveWorkoutSets().first().size })

            val submit = compose.onNodeWithText("Set loggen")
            submit.performScrollTo().assertIsDisplayed()
            compose.onAllNodesWithText("Set loggen").assertCountEquals(1)
            submit.performTouchInput { doubleClick() }
            compose.waitUntil(15_000) {
                runBlocking { database.dao().observeActiveWorkoutSets().first().size == 2 }
            }
            compose.waitForIdle()
            assertEquals(2, runBlocking { database.dao().observeActiveWorkoutSets().first().size })
            val finish = compose.onNodeWithContentDescription("Training afronden").assertIsDisplayed()
                .fetchSemanticsNode().boundsInRoot
            val snackbar = compose.onNodeWithText("Set 2 gelogd", substring = true).assertIsDisplayed()
                .fetchSemanticsNode().boundsInRoot
            assertTrue("Snackbar moet boven de bereikbare actiebalk blijven.", snackbar.bottom <= finish.top)
        }
    }

    @Test
    fun compactFirstSetKeepsMetricTapTargetsAndAlignedInputs() {
        runBlocking {
            database.dao().deleteActiveWorkoutSetsForSession(12L)
            database.dao().insertRoutineSets(listOf(
                RoutineSetEntity(id = 11L, workoutExerciseId = 4L, orderIndex = 1, setType = "NORMAL", targetReps = 5, restSeconds = 120),
                RoutineSetEntity(id = 12L, workoutExerciseId = 4L, orderIndex = 2, setType = "NORMAL", targetReps = 5, restSeconds = 120),
            ))
        }
        ActivityScenario.launch(MainActivity::class.java).use {
            val training = (hasText("Training") or hasContentDescription("Training")) and hasClickAction()
            compose.waitUntil(30_000) { compose.onAllNodes(training).fetchSemanticsNodes().isNotEmpty() }
            compose.onNode(training).performClick()
            compose.waitForText("QA Upper")
            compose.onNodeWithText("Training starten").performClick()
            compose.waitForText("Actieve training")
            compose.waitUntil(10_000) {
                compose.onAllNodesWithText("Actieve training hersteld.").fetchSemanticsNodes().isEmpty()
            }
            metricInput("Herh.").performScrollTo()
            val density = context.resources.displayMetrics.density
            val fontScale = context.resources.configuration.fontScale
            val bounds = listOf("Herh.", "Gewicht in kilogram", "Rust in seconden").map { description ->
                compose.onNodeWithContentDescription(description, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
            }
            val compactHeightLimit = (64f + (fontScale - 1f).coerceAtLeast(0f) * 28f) * density
            bounds.forEach { cell ->
                assertTrue("Metric cell must retain a 48dp tap target: $cell", cell.width >= 48f * density && cell.height >= 48f * density)
                assertTrue("Metric cell should remain compact at font scale $fontScale: $cell", cell.height <= compactHeightLimit + 1f)
            }
            assertTrue("Metric heights must match", bounds.maxOf { it.height } - bounds.minOf { it.height } <= 1f)
            assertTrue("Metric labels and values must align", bounds.maxOf { it.top } - bounds.minOf { it.top } <= 1f)
            bounds.zipWithNext().forEach { (left, right) -> assertTrue("Metric cells must not overlap", left.right <= right.left) }
            capture("polish-first-set")
            compose.onAllNodes(hasContentDescription("Geplande set uit deze training verwijderen"))[0]
                .performScrollTo().performClick()
            compose.onNodeWithText("Set 3", useUnmergedTree = true).assertDoesNotExist()
            compose.onNodeWithContentDescription("Set toevoegen aan Bench Press").performScrollTo().performClick()
            compose.onNodeWithText("Set 3", useUnmergedTree = true).assertExists()
            metricInput("Herh.").performScrollTo()
            metricInput("Herh.").performClick().performTextReplacement("7")
            metricInput("Herh.").performImeAction()
            metricInput("Gewicht in kilogram").assertIsFocused().performTextReplacement("27.5")
            metricInput("Gewicht in kilogram").performImeAction()
            metricInput("Rust in seconden").assertIsFocused().performTextReplacement("75")
            metricInput("Rust in seconden").performImeAction()
            val submit = compose.onNode(hasText("Set loggen") and hasClickAction()).performScrollTo().assertIsDisplayed()
            val submitBounds = submit.fetchSemanticsNode().boundsInRoot
            assertTrue("Log action must retain a 48dp target", submitBounds.height >= 48f * density)
            submit.performClick()
            waitForSetCount(1)
            val set = readActiveWorkoutSet()
            assertEquals(7, set.reps)
            assertEquals(27.5, set.weight, 0.0)
            assertEquals(75, set.restSeconds)
            compose.waitUntil(15_000) {
                compose.onAllNodesWithText("Set 1 gelogd", substring = true).fetchSemanticsNodes().isEmpty()
            }
            capture("polish-completed-next")
            val finish = compose.onNodeWithContentDescription("Training afronden").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
            assertTrue("Finish action must retain a 48dp target", finish.width >= 48f * density && finish.height >= 48f * density)
            assertTrue("Finish action must stay inside the app viewport", finish.bottom <= compose.onRoot().fetchSemanticsNode().boundsInRoot.bottom)
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
