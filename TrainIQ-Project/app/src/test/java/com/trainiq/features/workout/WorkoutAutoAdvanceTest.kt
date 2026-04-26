package com.trainiq.features.workout

import com.trainiq.domain.model.ActiveWorkoutFocusTarget
import com.trainiq.domain.model.Exercise
import com.trainiq.domain.model.LoggedSet
import com.trainiq.domain.model.WorkoutExercisePlan
import org.junit.Assert.assertEquals
import org.junit.Test

class WorkoutAutoAdvanceTest {
    @Test
    fun resolveNextFocusTarget_withRemainingSetsInSameExercise_targetsNextSet() {
        val bench = plan(id = 10L, exerciseId = 1L, targetSets = 3)
        val row = plan(id = 11L, exerciseId = 2L, targetSets = 3)

        val target = resolveNextFocusTarget(
            plans = listOf(bench, row),
            loggedSetsByExerciseId = mapOf(1L to listOf(loggedSet(1L))),
            justLoggedExerciseId = 1L,
        )

        assertEquals(ActiveWorkoutFocusTarget(exerciseId = 1L, setIndex = 1), target)
    }

    @Test
    fun resolveNextFocusTarget_whenExerciseComplete_targetsNextExercise() {
        val bench = plan(id = 10L, exerciseId = 1L, targetSets = 1)
        val row = plan(id = 11L, exerciseId = 2L, targetSets = 3)

        val target = resolveNextFocusTarget(
            plans = listOf(bench, row),
            loggedSetsByExerciseId = mapOf(1L to listOf(loggedSet(1L))),
            justLoggedExerciseId = 1L,
        )

        assertEquals(ActiveWorkoutFocusTarget(exerciseId = 2L, setIndex = 0), target)
    }

    @Test
    fun resolveNextFocusTarget_withSupersetAlternatesToPartnerBeforeReturning() {
        val bench = plan(id = 10L, exerciseId = 1L, targetSets = 2, supersetGroupId = 100L)
        val row = plan(id = 11L, exerciseId = 2L, targetSets = 2, supersetGroupId = 100L)
        val press = plan(id = 12L, exerciseId = 3L, targetSets = 2)

        val firstTarget = resolveNextFocusTarget(
            plans = listOf(bench, row, press),
            loggedSetsByExerciseId = mapOf(1L to listOf(loggedSet(1L))),
            justLoggedExerciseId = 1L,
        )
        val secondTarget = resolveNextFocusTarget(
            plans = listOf(bench, row, press),
            loggedSetsByExerciseId = mapOf(
                1L to listOf(loggedSet(1L)),
                2L to listOf(loggedSet(2L)),
            ),
            justLoggedExerciseId = 2L,
        )

        assertEquals(ActiveWorkoutFocusTarget(exerciseId = 2L, setIndex = 0), firstTarget)
        assertEquals(ActiveWorkoutFocusTarget(exerciseId = 1L, setIndex = 1), secondTarget)
    }

    private fun plan(
        id: Long,
        exerciseId: Long,
        targetSets: Int,
        supersetGroupId: Long? = null,
    ) = WorkoutExercisePlan(
        id = id,
        exercise = Exercise(id = exerciseId, name = "Exercise $exerciseId", muscleGroup = "Group", equipment = "Barbell"),
        targetSets = targetSets,
        repRange = "8-12",
        restSeconds = 90,
        supersetGroupId = supersetGroupId,
    )

    private fun loggedSet(exerciseId: Long) = LoggedSet(
        exerciseId = exerciseId,
        weight = 80.0,
        reps = 8,
        rpe = 8.0,
    )
}
