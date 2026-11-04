package com.trainiq.features.workout

import android.content.Context
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.click
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
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
    @Test
    fun restNotificationOpensValidatedTypedSessionAndBellShowsRealPreferences() {
        val clock = com.trainiq.core.workout.AndroidWorkoutRestTimerClock(context)
        val timer = com.trainiq.domain.workout.newWorkoutRestTimer(12, 7, 120, clock.now(), "notification-open")!!
        val preferences = UserPreferencesRepository(context)
        val originalSound = runBlocking { preferences.workoutFeedbackPreferences.first().restTimerSoundEnabled }
        val notifications = com.trainiq.core.workout.WorkoutRestTimerNotifications(context, preferences)
        val scheduler = com.trainiq.core.workout.WorkoutRestTimerScheduler(context, notifications)
        runBlocking {
            val active = database.dao().readActiveWorkoutSessionsForExport().single()
            database.dao().insertActiveWorkoutSessions(listOf(active.copy(restTimerEndsAt = timer.wallDeadline,
                restTimerTotalSeconds = 120, restTimerGeneration = timer.identity.generation,
                restTimerElapsedDeadline = timer.elapsedDeadline, restTimerBootId = timer.bootId)))
        }
        try {
            ActivityScenario.launch(MainActivity::class.java).use {
                compose.waitUntil(30_000) { compose.onAllNodes((hasText("Training") or hasContentDescription("Training")) and hasClickAction()).fetchSemanticsNodes().isNotEmpty() }
                notifications.notification(timer, silent = true).contentIntent.send()
                compose.waitForText("Actieve training")
                compose.onNodeWithText("Bench Press", useUnmergedTree = true).assertIsDisplayed()
                val bell = hasContentDescription("Rustmelding:", substring = true) and hasClickAction()
                compose.onNode(bell).assertIsDisplayed().performClick()
                compose.waitForText("Rustmelding")
                capture("rest-settings-sheet")
                compose.onNodeWithText("Geluid en meldingen beheren").performScrollTo().assertIsDisplayed()
                compose.onNodeWithContentDescription("Geluid bij afgelopen rust").performScrollTo().performClick()
                compose.waitUntil(10_000) { runBlocking { preferences.workoutFeedbackPreferences.first().restTimerSoundEnabled != originalSound } }
                androidx.test.espresso.Espresso.pressBack()
                compose.onNodeWithContentDescription("Rusttimer overslaan", useUnmergedTree = true).performClick()
                compose.waitUntil(10_000) { runBlocking { database.dao().readActiveWorkoutSessionsForExport().single().restTimerGeneration == null } }
            }
        } finally {
            runBlocking { preferences.setRestTimerSoundEnabled(originalSound) }
            scheduler.cancel(timer.identity)
        }
    }

    @get:Rule
    val compose = createEmptyComposeRule()

    private lateinit var context: Context
    private lateinit var database: TrainIqDatabase

    @Test
    fun appendPreviousSetIsDisabledDuringCorrectionAndKeepsOriginal() {
        ActivityScenario.launch(MainActivity::class.java).use {
            val training = (hasText("Training") or hasContentDescription("Training")) and hasClickAction()
            compose.waitUntil(30_000) { compose.onAllNodes(training).fetchSemanticsNodes().isNotEmpty() }
            compose.onNode(training).performClick()
            compose.waitForText("QA Upper")
            compose.onNodeWithText("Training starten").performClick()
            compose.waitForText("Actieve training")
            compose.onNodeWithContentDescription("Gelogde set corrigeren").performScrollTo().performClick()
            val repeat = compose.onNodeWithText("Opnieuw loggen").performScrollTo().assertIsNotEnabled().fetchSemanticsNode().boundsInRoot
            val primary = compose.onNodeWithText("Wijzig loggen").fetchSemanticsNode().boundsInRoot
            val density = context.resources.displayMetrics.density
            assertTrue("Both actions retain 48dp targets", repeat.height >= 48f * density && primary.height >= 48f * density && repeat.width >= 48f * density && primary.width >= 48f * density)
            assertTrue("Actions do not overlap", repeat.left >= primary.right || repeat.top >= primary.bottom)
            if (repeat.left >= primary.right) {
                assertTrue("Adjacent actions have equal width and height", kotlin.math.abs(repeat.width - primary.width) <= 1f && kotlin.math.abs(repeat.height - primary.height) <= 1f)
            }
            capture("fix1-action-buttons")
            assertEquals(1, runBlocking { database.dao().observeActiveWorkoutSets().first().size })
        }
    }
    @Test
    fun appendPreviousSetCopiesPerformanceWithoutEffortAndCanUndo() {
        ActivityScenario.launch(MainActivity::class.java).use {
            val training = (hasText("Training") or hasContentDescription("Training")) and hasClickAction()
            compose.waitUntil(30_000) { compose.onAllNodes(training).fetchSemanticsNodes().isNotEmpty() }
            compose.onNode(training).performClick()
            compose.waitForText("QA Upper")
            compose.onNodeWithText("Training starten").performClick()
            compose.waitForText("Actieve training")
            compose.onNodeWithContentDescription("Set toevoegen aan Bench Press").performScrollTo().performClick()
            metricInput("Gewicht in kilogram").performScrollTo().performTextReplacement("35")
            compose.onNodeWithText("Opnieuw loggen").performScrollTo().performClick()
            waitForSetCount(2)
            val sets = runBlocking { database.dao().observeActiveWorkoutSets().first() }.sortedBy { it.orderIndex }
            assertEquals(80.0, sets.last().weight, 0.0)
            assertEquals(5, sets.last().reps)
            assertEquals(0.0, sets.last().rpe, 0.0)
            compose.onNodeWithText("Ongedaan maken").performClick()
            waitForSetCount(1)
            assertEquals(80.0, readActiveWorkoutSet().weight, 0.0)
        }
    }

    @Test
    fun appendPreviousSetIsDisabledWithoutCompletedSet() {
        runBlocking { database.dao().deleteActiveWorkoutSetsForSession(12L) }
        ActivityScenario.launch(MainActivity::class.java).use {
            val training = (hasText("Training") or hasContentDescription("Training")) and hasClickAction()
            compose.waitUntil(30_000) { compose.onAllNodes(training).fetchSemanticsNodes().isNotEmpty() }
            compose.onNode(training).performClick()
            compose.waitForText("QA Upper")
            compose.onNodeWithText("Training starten").performClick()
            compose.waitForText("Actieve training")
            compose.onNodeWithText("Opnieuw loggen").performScrollTo().assertIsNotEnabled()
        }
    }
    @Test
    fun alternatingLogActionsAcceptOnlyFirstTap() {
        ActivityScenario.launch(MainActivity::class.java).use {
            val training = (hasText("Training") or hasContentDescription("Training")) and hasClickAction()
            compose.waitUntil(30_000) { compose.onAllNodes(training).fetchSemanticsNodes().isNotEmpty() }
            compose.onNode(training).performClick()
            compose.waitForText("QA Upper")
            compose.onNodeWithText("Training starten").performClick()
            compose.waitForText("Actieve training")
            compose.onNodeWithContentDescription("Set toevoegen aan Bench Press").performScrollTo().performClick()
            metricInput("Gewicht in kilogram").performScrollTo().performTextReplacement("35")
            metricInput("Gewicht in kilogram").performImeAction()
            val normal = compose.onNodeWithText("Extra set loggen").performScrollTo()
            val repeat = compose.onNodeWithText("Opnieuw loggen")
            awaitReachableWorkoutAction(normal, "alternating-actions")
            val normalBounds = normal.fetchSemanticsNode().boundsInRoot
            val repeatBounds = repeat.fetchSemanticsNode().boundsInRoot
            val listBounds = compose.onNode(hasScrollToIndexAction()).fetchSemanticsNode().boundsInRoot
            assertTrue("Both tap points must be inside their buttons and visible list", normalBounds.contains(normalBounds.center) && repeatBounds.contains(repeatBounds.center) && listBounds.contains(normalBounds.center) && listBounds.contains(repeatBounds.center) && normalBounds.right <= repeatBounds.left)
            compose.onRoot().performTouchInput {
                click(normalBounds.center)
                click(repeatBounds.center)
            }
            waitForSetCount(2)
            compose.waitForIdle()
            val sets = runBlocking { database.dao().observeActiveWorkoutSets().first() }.sortedBy { it.orderIndex }
            assertEquals(2, sets.size)
            assertEquals(35.0, sets.last().weight, 0.0)
        }
    }

    @Test
    fun failedRepeatRollsBackAndPreservesDraftForRetry() {
        ActivityScenario.launch(MainActivity::class.java).use {
            val training = (hasText("Training") or hasContentDescription("Training")) and hasClickAction()
            compose.waitUntil(30_000) { compose.onAllNodes(training).fetchSemanticsNodes().isNotEmpty() }
            compose.onNode(training).performClick()
            compose.waitForText("QA Upper")
            compose.onNodeWithText("Training starten").performClick()
            compose.waitForText("Actieve training")
            compose.onNodeWithContentDescription("Set toevoegen aan Bench Press").performScrollTo().performClick()
            metricInput("Gewicht in kilogram").performScrollTo().performTextReplacement("35")
            metricInput("Gewicht in kilogram").performImeAction()
            val beforeEvents = runBlocking { database.dao().readWorkoutLogEventsForExport().size }
            val sql = database.openHelper.writableDatabase
            sql.execSQL("CREATE TRIGGER codex_test_fail_active_set_insert BEFORE INSERT ON active_workout_sets BEGIN SELECT RAISE(ABORT, 'test insert failure'); END")
            try {
                compose.onNodeWithText("Opnieuw loggen").performScrollTo().performClick()
                compose.waitForText("Set loggen is mislukt")
                assertEquals(1, runBlocking { database.dao().observeActiveWorkoutSets().first().size })
                assertEquals(beforeEvents, runBlocking { database.dao().readWorkoutLogEventsForExport().size })
                metricInput("Gewicht in kilogram").performScrollTo().assertTextEquals("35")
            } finally {
                sql.execSQL("DROP TRIGGER IF EXISTS codex_test_fail_active_set_insert")
            }
            compose.onNodeWithText("Opnieuw loggen").performScrollTo().performClick()
            waitForSetCount(2)
            val appended = runBlocking { database.dao().observeActiveWorkoutSets().first() }.maxBy { it.orderIndex }
            assertEquals(80.0, appended.weight, 0.0)
        }
    }
    @Test
    fun runningRestKeepsFinishLabelAndOneTimerControlGroup() {
        runBlocking {
            val now = System.currentTimeMillis()
            database.dao().insertActiveWorkoutSessions(listOf(ActiveWorkoutSessionEntity(
                sessionId = 12L, dayId = 7L, routineId = 1L,
                startedAt = now - 60_000L, updatedAt = now,
                restTimerEndsAt = now + 120_000L, restTimerTotalSeconds = 120,
                restTimerGeneration = "running-rest-ui", restTimerElapsedDeadline = android.os.SystemClock.elapsedRealtime() + 120_000L,
                restTimerBootId = com.trainiq.core.workout.AndroidWorkoutRestTimerClock(context).now().bootId,
            )))
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
            val finishText = compose.onNodeWithText("Training afronden", useUnmergedTree = true)
                .assertTextEquals("Training afronden").assertIsDisplayed()
            capture("single-rest-before-label-check")
            val restStatus = compose.onNode(hasContentDescription("Rusttimer:", substring = true), useUnmergedTree = true)
                .fetchSemanticsNode().boundsInRoot
            val restBell = compose.onNode(hasContentDescription("Rustmelding:", substring = true) and hasClickAction())
                .fetchSemanticsNode().boundsInRoot
            val restCount = compose.onNodeWithText("1 set gelogd", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
            val targetSize = 48f * context.resources.displayMetrics.density
            assertTrue("Bell keeps its 48dp target", restBell.width >= targetSize && restBell.height >= targetSize)
            listOf(restStatus, restCount).forEach { label ->
                assertTrue("Compact fixture puts status and count on the bell's row", label.top < restBell.bottom && label.bottom > restBell.top)
                assertTrue("Footer items on the same row share a vertical center: label=$label bell=$restBell",
                    kotlin.math.abs(label.center.y - restBell.center.y) <= 1f)
            }
            val finishLayouts = mutableListOf<TextLayoutResult>()
            finishText.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(finishLayouts) }
            val finishLayout = finishLayouts.single()
            val lineGeometry = (0 until finishLayout.lineCount).map { line ->
                "line $line: ${finishLayout.getLineLeft(line)}..${finishLayout.getLineRight(line)}, " +
                    "ellipsis=${finishLayout.isLineEllipsized(line)}"
            }
            assertTrue("Finish label height must fit: size=${finishLayout.size}", !finishLayout.didOverflowHeight)
            assertTrue(
                "Finish label glyphs must fit its bounds: size=${finishLayout.size}, $lineGeometry",
                (0 until finishLayout.lineCount).all { line ->
                    !finishLayout.isLineEllipsized(line) && finishLayout.getLineLeft(line) >= -1f &&
                        finishLayout.getLineRight(line) <= finishLayout.size.width + 1f
                },
            )
            assertEquals(
                "Finish label must render every character",
                "Training afronden".length,
                finishLayout.getLineEnd(finishLayout.lineCount - 1, visibleEnd = true),
            )
            compose.onAllNodes(hasContentDescription("Rusttimer overslaan"), useUnmergedTree = true).assertCountEquals(1)
            compose.onAllNodes(hasContentDescription("Rusttimer 30 seconden langer"), useUnmergedTree = true).assertCountEquals(1)
            compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Bench Press"))
            compose.onAllNodes(hasContentDescription("Rusttimer overslaan"), useUnmergedTree = true).assertCountEquals(1)
            compose.onAllNodes(hasContentDescription("Rusttimer 30 seconden langer"), useUnmergedTree = true).assertCountEquals(1)
            exerciseControl("Bench Press", "Rust voor volgende sets 30 seconden langer").assertExists()
            exerciseControl("Bench Press", "Rust voor volgende sets 30 seconden korter").assertExists()
            val finish = compose.onNodeWithContentDescription("Training afronden").assertIsDisplayed()
                .fetchSemanticsNode().boundsInRoot
            val density = context.resources.displayMetrics.density
            assertTrue("Labelled finish action must retain a 48dp target", finish.height >= 48f * density)
            val viewport = compose.onRoot().fetchSemanticsNode().boundsInRoot
            assertTrue("Finish action must stay inside the app viewport", finish.bottom <= viewport.bottom && finish.left >= viewport.left && finish.right <= viewport.right)
            val labelBounds = finishText.fetchSemanticsNode().boundsInRoot
            assertTrue(
                "Entire finish label must stay inside its button: label=$labelBounds button=$finish",
                labelBounds.left >= finish.left && labelBounds.right <= finish.right &&
                    labelBounds.top >= finish.top && labelBounds.bottom <= finish.bottom,
            )
            capture("single-rest-labelled-finish")
        }
    }

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
            compose.waitUntil(10_000) { compose.onAllNodesWithText("Actieve training hersteld.").fetchSemanticsNodes().isEmpty() }
            compose.onNode(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Voltooid"), useUnmergedTree = true).performScrollTo()
            val performance = compose.onNodeWithText("80 kg × 5", useUnmergedTree = true).assertIsDisplayed()
            val setLabel = compose.onNodeWithText("Set 1", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
            val performanceBounds = performance.fetchSemanticsNode().boundsInRoot
            val density = context.resources.displayMetrics.density
            assertTrue("Set label and performance have clear spacing", performanceBounds.left - setLabel.right >= 12f * density - 1f)
            compose.onAllNodesWithText("N").assertCountEquals(0)
            val performanceLayouts = mutableListOf<TextLayoutResult>()
            performance.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(performanceLayouts) }
            val layout = performanceLayouts.single()
            assertTrue("Performance glyphs and all characters must fit: $layout", (0 until layout.lineCount).all { line ->
                !layout.isLineEllipsized(line) && layout.getLineLeft(line) >= -1f &&
                    layout.getLineRight(line) <= layout.size.width + 1f &&
                    layout.getLineBottom(line) <= layout.size.height + 1f
            } && layout.getLineEnd(layout.lineCount - 1, visibleEnd = true) == "80 kg × 5".length)
            compose.onNodeWithContentDescription("Normaal, 80 kilogram, 5 herhalingen", useUnmergedTree = true).assertExists()
            val completedRow = compose.onNode(
                SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Voltooid"), useUnmergedTree = true,
            ).fetchSemanticsNode().boundsInRoot
            val actionBounds = listOf("Gelogde set corrigeren", "Gelogde set acties").map { description ->
                compose.onNodeWithContentDescription(description).fetchSemanticsNode().boundsInRoot.also { bounds ->
                    assertTrue("Set action retains a 48dp target", bounds.width >= 48f * density && bounds.height >= 48f * density)
                }
            }
            assertTrue("Set contents remain within the completed row", listOf(setLabel, performanceBounds, *actionBounds.toTypedArray()).all { bounds ->
                bounds.left >= completedRow.left && bounds.right <= completedRow.right &&
                    bounds.top >= completedRow.top && bounds.bottom <= completedRow.bottom
            })
            assertTrue("Performance and actions do not overlap", performanceBounds.right <= actionBounds[0].left && actionBounds[0].right <= actionBounds[1].left)
            capture("fix1-completed-row")

            compose.onNodeWithContentDescription("Gelogde set acties").performScrollTo().performClick()
            compose.onNodeWithText("Settype wijzigen").performClick()
            compose.waitForText("Warm-up")
            compose.onNodeWithText("Warm-up").performClick()
            compose.waitForText("Warm-up")

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
            compose.waitForText("Warm-up")

            compose.waitUntil(10_000) { compose.onAllNodesWithText("Actieve training hersteld.").fetchSemanticsNodes().isEmpty() }
            val actions = compose.onNodeWithContentDescription("Gelogde set acties").performScrollTo()
            awaitReachableWorkoutAction(actions, "completed-menu").performClick()
            compose.waitForText("Set verwijderen")
            compose.onNodeWithText("Set verwijderen").performClick()
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
            awaitReachableWorkoutAction(logButton, "rest-ime-before-log-check").performClick()
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
            compose.onNodeWithText("Workout-notitie").performScrollTo().performClick()
            compose.onNodeWithText("Notitie").performScrollTo().performTextReplacement("Twee oefeningen, rustig tempo")
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
            exerciseControl("Bench Press", "Rust voor volgende sets 30 seconden langer").performScrollTo().performClick()
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
            compose.onNodeWithText("Training afronden", useUnmergedTree = true).assertIsDisplayed()
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
            compose.waitUntil(10_000) {
                compose.onAllNodesWithText("Actieve training hersteld.").fetchSemanticsNodes().isEmpty() &&
                    compose.onAllNodesWithText("Set 3 gelogd", substring = true).fetchSemanticsNodes().isEmpty()
            }
            assertEquals(3, runBlocking { database.dao().readActiveWorkoutSetsForExport().size })
            compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Seated Row"))
            exerciseControl("Seated Row", "Gewicht in kilogram", input = true).performScrollTo()
                .performTextReplacement("40")
            exerciseControl("Seated Row", "Herh.", input = true).performTextReplacement("8")
            exerciseControl("Seated Row", "Rust in seconden", input = true).performImeAction()
            val finalSetAction = exerciseText("Seated Row", "Set loggen").performScrollTo()
            awaitReachableWorkoutAction(finalSetAction, "resumed-final-set-before-click").performClick()
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
            compose.onNodeWithText("Workout-notitie").performScrollTo().performClick()
            compose.onNodeWithText("Notitie").performScrollTo().performTextReplacement("Rustig opbouwen vandaag")
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
    fun failedFinishPreservesRestTimerAndSuccessfulRetryCancelsAfterCommit() {
        val clock = com.trainiq.core.workout.AndroidWorkoutRestTimerClock(context)
        val timer = com.trainiq.domain.workout.newWorkoutRestTimer(12, 7, 600, clock.now(), "finish-retry")!!
        val preferences = UserPreferencesRepository(context)
        val scheduler = com.trainiq.core.workout.WorkoutRestTimerScheduler(
            context, com.trainiq.core.workout.WorkoutRestTimerNotifications(context, preferences),
        )
        runBlocking {
            preferences.setAiEnabled(false)
            database.dao().insertRoutineSets(listOf(
                RoutineSetEntity(id = 11L, workoutExerciseId = 4L, orderIndex = 1, setType = "NORMAL", targetReps = 5),
            ))
            val active = database.dao().readActiveWorkoutSessionsForExport().single()
            database.dao().insertActiveWorkoutSessions(listOf(active.copy(
                restTimerEndsAt = timer.wallDeadline, restTimerTotalSeconds = timer.totalSeconds,
                restTimerGeneration = timer.identity.generation, restTimerElapsedDeadline = timer.elapsedDeadline,
                restTimerBootId = timer.bootId,
            )))
        }
        fun pendingRestAlarms(): Int {
            val descriptor = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().uiAutomation
                .executeShellCommand("dumpsys alarm")
            val dump = android.os.ParcelFileDescriptor.AutoCloseInputStream(descriptor).use { it.readBytes().decodeToString() }
            return Regex("ELAPSED_WAKEUP #\\d+: Alarm\\{[^\\n]* com\\.trainiq\\}").findAll(dump).count()
        }
        val sql = database.openHelper.writableDatabase
        try {
            ActivityScenario.launch(MainActivity::class.java).use {
                val training = (hasText("Training") or hasContentDescription("Training")) and hasClickAction()
                compose.waitUntil(30_000) { compose.onAllNodes(training).fetchSemanticsNodes().isNotEmpty() }
                compose.onNode(training).performClick()
                compose.waitForText("QA Upper")
                compose.onNodeWithText("Training starten").performClick()
                compose.waitForText("Actieve training")
                compose.waitUntil(10_000) { pendingRestAlarms() == 1 }
                val before = runBlocking { database.dao().readActiveWorkoutSessionsForExport().single() }
                // The existing draft is updated by Room's upsert; active-session timer writes remain permitted.
                sql.execSQL("CREATE TRIGGER codex_test_fail_workout_finish BEFORE UPDATE OF completed ON workout_sessions WHEN NEW.completed = 1 BEGIN SELECT RAISE(ABORT, 'test finish failure'); END")
                compose.onNodeWithContentDescription("Training afronden").performClick()
                compose.waitForText("Training afronden?")
                compose.onNodeWithText("Opslaan").performClick()
                compose.waitForText("Training afronden is mislukt. Probeer opnieuw.")
                val after = runBlocking { database.dao().readActiveWorkoutSessionsForExport().single() }
                assertEquals(before.sessionId, after.sessionId)
                assertEquals(before.startedAt, after.startedAt)
                assertEquals(before.restTimerGeneration, after.restTimerGeneration)
                assertEquals(before.restTimerEndsAt, after.restTimerEndsAt)
                assertEquals(before.restTimerElapsedDeadline, after.restTimerElapsedDeadline)
                assertEquals(before.restTimerBootId, after.restTimerBootId)
                assertEquals(before.restTimerTotalSeconds, after.restTimerTotalSeconds)
                assertEquals(false, after.restTimerHandled)
                assertEquals(1, pendingRestAlarms())
                assertEquals(1, runBlocking { database.dao().readActiveWorkoutSetsForExport().size })
                assertEquals(false, runBlocking { database.dao().readWorkoutSessionsForExport().single().completed })
                // Platform Back also returns to the still-active session from the error route.
                androidx.test.espresso.Espresso.pressBack()
                compose.waitForText("Actieve training")
                compose.onNodeWithContentDescription("Rusttimer overslaan", useUnmergedTree = true).assertIsDisplayed()

                sql.execSQL("DROP TRIGGER IF EXISTS codex_test_fail_workout_finish")
                compose.onNodeWithContentDescription("Training afronden").performClick()
                compose.waitForText("Training afronden?")
                compose.onNodeWithText("Opslaan").performClick()
                compose.waitForText("Voltooid")
                assertEquals(0, runBlocking { database.dao().readActiveWorkoutSessionsForExport().size })
                assertEquals(true, runBlocking { database.dao().readWorkoutSessionsForExport().single().completed })
                assertEquals(1, runBlocking { database.dao().readWorkoutSetsForExport().size })
                assertEquals(0, pendingRestAlarms())
            }
        } finally {
            sql.execSQL("DROP TRIGGER IF EXISTS codex_test_fail_workout_finish")
            scheduler.cancel(timer.identity)
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
            compose.onNodeWithText("Workout-notitie").performScrollTo().performClick()
            compose.onNodeWithText("Notitie").performScrollTo().performTextReplacement("Goede sessie")
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

    private fun awaitReachableWorkoutAction(
        action: SemanticsNodeInteraction,
        evidenceState: String,
    ): SemanticsNodeInteraction {
        var lastGeometry: Triple<Rect, Rect, Rect>? = null
        var stableSince = android.os.SystemClock.elapsedRealtime()
        try {
            compose.waitUntil(5_000) {
                val geometry = Triple(
                    action.fetchSemanticsNode().boundsInRoot,
                    compose.onNode(hasScrollToIndexAction()).fetchSemanticsNode().boundsInRoot,
                    compose.onRoot().fetchSemanticsNode().boundsInRoot,
                )
                val displayed = runCatching { action.assertIsDisplayed() }.isSuccess
                val tapReachable = geometry.first.width > 0f && geometry.first.height > 0f &&
                    geometry.second.contains(geometry.first.center) && geometry.third.contains(geometry.first.center)
                val now = android.os.SystemClock.elapsedRealtime()
                if (geometry != lastGeometry || !displayed || !tapReachable) stableSince = now
                lastGeometry = geometry
                displayed && tapReachable && now - stableSince >= 500
            }
        } catch (failure: Throwable) {
            val captureFailure = runCatching { capture("$evidenceState-unreachable") }.exceptionOrNull()
            throw AssertionError(
                "Set log action did not settle inside the visible list/root: " +
                    "lastButtonListRoot=$lastGeometry, stableForMs=${android.os.SystemClock.elapsedRealtime() - stableSince}, " +
                    "button=${runCatching { action.fetchSemanticsNode().boundsInRoot }.getOrNull()}, " +
                    "list=${compose.onNode(hasScrollToIndexAction()).fetchSemanticsNode().boundsInRoot}, " +
                    "root=${compose.onRoot().fetchSemanticsNode().boundsInRoot}, captureFailure=$captureFailure",
                failure,
            )
        }
        capture(evidenceState)
        try {
            action.assertIsDisplayed()
        } catch (failure: AssertionError) {
            throw AssertionError(
                "Set log action must remain reachable with keyboard and rest timer: " +
                    "button=${action.fetchSemanticsNode().boundsInRoot}, " +
                    "list=${compose.onNode(hasScrollToIndexAction()).fetchSemanticsNode().boundsInRoot}, " +
                    "root=${compose.onRoot().fetchSemanticsNode().boundsInRoot}",
                failure,
            )
        }
        return action
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
