package com.trainiq.features.workout

import com.trainiq.domain.model.Exercise
import com.trainiq.domain.model.LoggedSet
import com.trainiq.domain.model.RoutineSet
import com.trainiq.domain.model.SetType
import com.trainiq.domain.model.WorkoutDay
import com.trainiq.domain.model.WorkoutExercisePlan
import com.trainiq.domain.model.WorkoutRoutine
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutInputValidationTest {
    @Test
    fun `routine set metric cells keep fixed scan columns with placeholders`() {
        val set = RoutineSet(
            id = 1,
            workoutExerciseId = 10,
            orderIndex = 0,
            setType = SetType.NORMAL,
            targetReps = 8,
            targetWeightKg = 80.0,
            restSeconds = -1,
            targetRpe = 0.0,
        )

        assertEquals(
            listOf("8", "80 kg", "-", "-"),
            routineSetMetricCells(set).map { it.value },
        )
    }

    @Test
    fun `routine set metric layout uses one row when enough width is available`() {
        assertEquals(RoutineSetMetricLayout.OneRow, routineSetMetricLayoutForWidth(260.dp))
    }

    @Test
    fun `routine set metric layout falls back to balanced grid on narrow widths`() {
        assertEquals(RoutineSetMetricLayout.BalancedGrid, routineSetMetricLayoutForWidth(220.dp))
    }

    @Test
    fun `active set metric cells prefer logged values and keep routine editor labels`() {
        val loggedSet = LoggedSet(
            exerciseId = 1,
            weight = 50.0,
            reps = 12,
            rpe = 6.5,
            restSeconds = 90,
        )

        assertEquals(
            listOf("Reps" to "12", "Kg" to "50 kg", "Rust" to "90s", "RPE" to "6.5"),
            activeSetMetricCells(repRange = "8-12", plannedSet = null, loggedSet = loggedSet)
                .map { it.label to it.value },
        )
    }

    @Test
    fun `active set metric cells use planned values with placeholders`() {
        val plannedSet = RoutineSet(
            id = 1,
            workoutExerciseId = 10,
            orderIndex = 0,
            targetReps = 12,
            targetWeightKg = 0.0,
            restSeconds = 90,
            targetRpe = 0.0,
        )

        assertEquals(
            listOf("12", "-", "90s", "-"),
            activeSetMetricCells(repRange = "8-12", plannedSet = plannedSet, loggedSet = null)
                .map { it.value },
        )
    }

    @Test
    fun `set log start rejects duplicate pending submit`() {
        val started = tryStartSetLog(emptySet(), exerciseId = 10L)

        assertTrue(started is SetLogStartResult.Started)
        started as SetLogStartResult.Started
        assertEquals(SetLogStartResult.AlreadyPending, tryStartSetLog(started.pendingExerciseIds, exerciseId = 10L))
    }

    @Test
    fun `set log start allows submit after completion`() {
        val started = tryStartSetLog(emptySet(), exerciseId = 10L) as SetLogStartResult.Started
        val finished = finishSetLog(started.pendingExerciseIds, exerciseId = 10L)

        assertTrue(tryStartSetLog(finished, exerciseId = 10L) is SetLogStartResult.Started)
    }

    @Test
    fun `set log start allows different exercise while another is pending`() {
        val started = tryStartSetLog(emptySet(), exerciseId = 10L) as SetLogStartResult.Started
        val next = tryStartSetLog(started.pendingExerciseIds, exerciseId = 20L)

        assertTrue(next is SetLogStartResult.Started)
        next as SetLogStartResult.Started
        assertEquals(setOf(10L, 20L), next.pendingExerciseIds)
    }

    @Test
    fun `pending correction keeps logger visible and changes primary action label`() {
        assertTrue(shouldShowActiveSetLogger(isSessionFinished = false, loggedSetCount = 3, activeSetTargetCount = 3, hasPendingCorrection = true))
        assertEquals("Wijzig loggen", activeSetLogButtonLabel(isLogPending = false, hasPendingCorrection = true, loggedSetCount = 3, plannedSetCount = 3))
    }

    @Test
    fun `decimal filter keeps digits and a single separator with limited decimals`() {
        assertEquals("12.34", filterDecimalInput("1a2,3.456", maxDecimals = 2))
    }

    @Test
    fun `decimal filter supports leading separator and rpe precision`() {
        assertEquals(".5", filterDecimalInput(",56", maxDecimals = 1))
    }

    @Test
    fun `integer filter keeps digits only`() {
        assertEquals("123", filterIntegerInput("1a2,3"))
    }

    @Test
    fun `set input accepts comma decimal and blank rpe`() {
        val result = validateSetInput(SetInputDraft(weight = "80,5", reps = "8", rpe = ""))

        assertTrue(result is SetLogValidationResult.Valid)
        result as SetLogValidationResult.Valid
        assertEquals(80.5, result.weight, 0.0)
        assertEquals(8, result.reps)
        assertEquals(0.0, result.rpe, 0.0)
    }

    @Test
    fun `set input accepts zero weight for bodyweight sets`() {
        val result = validateSetInput(SetInputDraft(weight = "0", reps = "12", rpe = "7"))

        assertTrue(result is SetLogValidationResult.Valid)
        result as SetLogValidationResult.Valid
        assertEquals(0.0, result.weight, 0.0)
        assertEquals(12, result.reps)
        assertEquals(7.0, result.rpe, 0.0)
    }

    @Test
    fun `set input rejects non numeric rpe`() {
        val result = validateSetInput(SetInputDraft(weight = "80", reps = "8", rpe = "abc"))

        assertTrue(result is SetLogValidationResult.Invalid)
    }

    @Test
    fun `set input rejects rpe above ten`() {
        val result = validateSetInput(SetInputDraft(weight = "80", reps = "8", rpe = "10,5"))

        assertTrue(result is SetLogValidationResult.Invalid)
    }

    @Test
    fun `invalid rpe maps to field specific error feedback`() {
        val result = validateSetInput(SetInputDraft(weight = "80", reps = "8", rpe = "11.5"))

        assertTrue(result is SetLogValidationResult.Invalid)
        result as SetLogValidationResult.Invalid
        assertEquals("RPE moet leeg zijn of tussen 0 en 10 liggen.", result.fieldErrors.rpe)
        assertEquals(null, result.fieldErrors.weight)
        assertEquals(null, result.fieldErrors.reps)
    }

    @Test
    fun `first startable day skips empty days`() {
        val emptyDay = WorkoutDay(id = 1, routineId = 1, name = "Empty", orderIndex = 0, exercises = emptyList())
        val exercise = Exercise(id = 1, name = "Bench press", muscleGroup = "Chest", equipment = "Barbell")
        val startableDay = WorkoutDay(
            id = 2,
            routineId = 1,
            name = "Push",
            orderIndex = 1,
            exercises = listOf(
                WorkoutExercisePlan(
                    id = 10,
                    exercise = exercise,
                    targetSets = 3,
                    repRange = "8-12",
                    restSeconds = 90,
                ),
            ),
        )
        val routine = WorkoutRoutine(
            id = 1,
            name = "Routine",
            description = "",
            active = true,
            days = listOf(emptyDay, startableDay),
        )

        assertEquals(startableDay, routine.firstStartableDay())
    }

    @Test
    fun `selected routine id is cleared when restored routine no longer exists`() {
        val routine = WorkoutRoutine(
            id = 1,
            name = "Routine",
            description = "",
            active = true,
            days = emptyList(),
        )

        assertEquals(1L, resolveSelectedRoutineId(1L, listOf(routine)))
        assertEquals(null, resolveSelectedRoutineId(2L, listOf(routine)))
        assertEquals(null, resolveSelectedRoutineId(null, listOf(routine)))
    }

    @Test
    fun `workout list keys do not collide with saved routine ids`() {
        val routine = WorkoutRoutine(
            id = 1,
            name = "Routine",
            description = "",
            active = true,
            days = emptyList(),
        )
        val keys = workoutOverviewListKeys(
            routines = listOf(routine),
            exercises = listOf(Exercise(id = 1, name = "Bench press", muscleGroup = "Chest", equipment = "Barbell")),
            historySessionIds = listOf(1),
            hasMessage = true,
        )

        assertEquals(keys.distinct(), keys)
    }

    @Test
    fun `exercise picker handle dismiss requires clear downward drag threshold`() {
        val thresholdPx = 96f

        assertEquals(false, shouldDismissExercisePickerFromHandleDrag(verticalDragPx = 24f, thresholdPx = thresholdPx))
        assertEquals(false, shouldDismissExercisePickerFromHandleDrag(verticalDragPx = 95f, thresholdPx = thresholdPx))
        assertEquals(true, shouldDismissExercisePickerFromHandleDrag(verticalDragPx = 96f, thresholdPx = thresholdPx))
        assertEquals(true, shouldDismissExercisePickerFromHandleDrag(verticalDragPx = 140f, thresholdPx = thresholdPx))
    }

    @Test
    fun `exercise edit trailing scroll padding includes ime in dialog set editor`() {
        val padding = exerciseEditTrailingScrollPadding(
            imeBottomPadding = 280.dp,
            minimumTrailingPadding = 24.dp,
        )

        assertEquals(304.dp, padding)
    }

    @Test
    fun `set editor uses dialog backed surface instead of material bottom sheet`() {
        assertEquals(true, setEditorUsesDialogBackedSurface())
    }

    @Test
    fun `set editor handle dismiss requires clear downward drag threshold`() {
        val thresholdPx = 96f

        assertEquals(false, shouldDismissSetEditorFromHandleDrag(verticalDragPx = 24f, thresholdPx = thresholdPx))
        assertEquals(false, shouldDismissSetEditorFromHandleDrag(verticalDragPx = 95f, thresholdPx = thresholdPx))
        assertEquals(true, shouldDismissSetEditorFromHandleDrag(verticalDragPx = 96f, thresholdPx = thresholdPx))
        assertEquals(true, shouldDismissSetEditorFromHandleDrag(verticalDragPx = 140f, thresholdPx = thresholdPx))
    }

    @Test
    fun `set editor focused input reveal scrolls only enough to clear visible viewport`() {
        assertEquals(
            148f,
            focusedInputRevealScrollDelta(
                fieldTop = 1223f,
                fieldBottom = 1391f,
                viewportTop = 444f,
                viewportBottom = 1291f,
                marginPx = 48f,
            ),
        )
        assertEquals(
            0f,
            focusedInputRevealScrollDelta(
                fieldTop = 900f,
                fieldBottom = 1050f,
                viewportTop = 444f,
                viewportBottom = 1291f,
                marginPx = 48f,
            ),
        )
    }

    @Test
    fun `set editor visible viewport bottom accounts for ime overlay`() {
        assertEquals(
            1291f,
            setEditorVisibleViewportBottom(
                viewportBottom = 1849f,
                rootHeight = 2400f,
                imeBottomPx = 1109f,
            ),
        )
        assertEquals(
            1849f,
            setEditorVisibleViewportBottom(
                viewportBottom = 1849f,
                rootHeight = 2400f,
                imeBottomPx = 0f,
            ),
        )
    }

    @Test
    fun `set editor reveal verifies focused field against ime-reduced viewport`() {
        val viewport = Rect(left = 0f, top = 444f, right = 1079f, bottom = 1849f)
        val hiddenByIme = Rect(left = 555f, top = 1223f, right = 1016f, bottom = 1391f)
        val visibleAfterScroll = Rect(left = 555f, top = 1000f, right = 1016f, bottom = 1168f)

        assertEquals(
            false,
            isFocusedInputVisibleInSetEditor(
                field = hiddenByIme,
                viewport = viewport,
                visibleViewportBottom = 1291f,
                marginPx = 48f,
            ),
        )
        assertEquals(
            true,
            isFocusedInputVisibleInSetEditor(
                field = visibleAfterScroll,
                viewport = viewport,
                visibleViewportBottom = 1291f,
                marginPx = 48f,
            ),
        )
    }

    @Test
    fun `set log guard rejects second tap while exercise is pending`() {
        val firstTap = tryStartSetLog(pendingExerciseIds = emptySet(), exerciseId = 42L)
        assertTrue(firstTap is SetLogStartResult.Started)

        val secondTap = tryStartSetLog(
            pendingExerciseIds = (firstTap as SetLogStartResult.Started).pendingExerciseIds,
            exerciseId = 42L,
        )

        assertTrue(secondTap is SetLogStartResult.AlreadyPending)
    }

    @Test
    fun `set log guard can clear pending exercise after save finishes`() {
        val firstTap = tryStartSetLog(pendingExerciseIds = emptySet(), exerciseId = 42L) as SetLogStartResult.Started

        assertEquals(emptySet<Long>(), finishSetLog(firstTap.pendingExerciseIds, exerciseId = 42L))
    }
}
