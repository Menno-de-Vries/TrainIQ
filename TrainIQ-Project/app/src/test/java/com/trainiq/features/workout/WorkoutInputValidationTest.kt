package com.trainiq.features.workout

import com.trainiq.domain.model.Exercise
import com.trainiq.domain.model.WorkoutDay
import com.trainiq.domain.model.WorkoutExercisePlan
import com.trainiq.domain.model.WorkoutRoutine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutInputValidationTest {
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
