package com.trainiq.domain.model

import org.junit.Assert.*
import org.junit.Test

class WorkoutEffortSummaryTest {
    @Test fun singleReportedSetKeepsItsRpe() {
        assertEquals(8.0, WorkoutEffortSummary.averageRpe(listOf(set(8.0)))!!, 0.0)
    }

    @Test fun completedSetsUseUnweightedMeanAndIgnoreIncompleteAndUnknown() {
        val sets = listOf(set(6.0), set(9.0), set(10.0, completed = false), set(0.0))
        assertEquals(7.5, WorkoutEffortSummary.averageRpe(sets)!!, 0.0)
        assertEquals(3, WorkoutEffortSummary.exercises(sets, emptyMap()).single().sets.size)
    }

    @Test fun missingAndInvalidRpeRemainUnknownEvenWithObjectiveDataOrRir() {
        val sets = listOf(set(0.0).copy(repsInReserve = 2), set(Double.NaN), set(11.0), set(-1.0), set(0.5))
        assertNull(WorkoutEffortSummary.averageRpe(sets))
        assertTrue(WorkoutEffortSummary.exercises(sets, emptyMap()).single().sets.all { it.rpe == null })
    }

    @Test fun exercisesKeepSeparateMeansAndIncludeEveryCompletedSetType() {
        val sets = listOf(set(6.0), set(8.0).copy(setType = SetType.WARM_UP), set(9.0, exerciseId = 2), set(0.0, exerciseId = 3))
        val result = WorkoutEffortSummary.exercises(sets, mapOf(1L to "Bench", 2L to "Row", 3L to "Squat"))
        assertEquals(listOf(7.0, 9.0, null), result.map { it.averageRpe })
        assertEquals(listOf("Bench", "Row", "Squat"), result.map { it.exerciseName })
    }

    private fun set(rpe: Double, completed: Boolean = true, exerciseId: Long = 1) =
        LoggedSet(exerciseId = exerciseId, weight = 60.0, reps = 8, rpe = rpe, completed = completed)
}
