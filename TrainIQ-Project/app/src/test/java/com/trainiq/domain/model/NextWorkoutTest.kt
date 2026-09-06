package com.trainiq.domain.model

import org.junit.Assert.*
import org.junit.Test

class NextWorkoutTest {
    @Test fun startableSelectionHandlesUnsortedDaysAndEmptyRoutines() {
        val plan = WorkoutExercisePlan(1L, Exercise(1L, "Bench", "Chest", "Barbell"), 3, "8", 90)
        val empty = WorkoutDay(1L, 1L, "Empty", 0, emptyList())
        val ready = WorkoutDay(2L, 1L, "Ready", 2, listOf(plan))
        val later = ready.copy(id = 3L, orderIndex = 5)
        val routine = WorkoutRoutine(1L, "Plan", "", true, listOf(later, empty, ready))
        assertEquals(2L, routine.nextStartableWorkoutDay()?.id)
        assertNull(routine.copy(days = listOf(empty)).nextStartableWorkoutDay())
        assertNull(routine.copy(days = emptyList()).nextStartableWorkoutDay())
    }
}
