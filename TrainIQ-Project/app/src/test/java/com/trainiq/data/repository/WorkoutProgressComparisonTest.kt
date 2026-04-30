package com.trainiq.data.repository

import com.trainiq.core.database.ExerciseEntity
import com.trainiq.core.database.WorkoutDayEntity
import com.trainiq.core.database.WorkoutSessionEntity
import com.trainiq.core.database.WorkoutSetEntity
import com.trainiq.domain.model.LoggedSet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WorkoutProgressComparisonTest {
    @Test
    fun buildWorkoutProgressComparison_usesLatestComparableWorkoutDayInsteadOfFirstSession() {
        val currentSets = listOf(
            LoggedSet(
                id = 1L,
                exerciseId = 10L,
                performedExerciseId = 0L,
                weight = 110.0,
                reps = 5,
                rpe = 8.0,
                repsInReserve = 2,
            ),
        )
        val sessions = listOf(
            completedSession(id = 99L, dayId = 2L, routineId = 1L, startedAt = 3_000L),
            completedSession(id = 42L, dayId = 7L, routineId = 1L, startedAt = 2_000L),
            completedSession(id = 7L, dayId = 7L, routineId = 1L, startedAt = 1_000L),
        )
        val sets = listOf(
            WorkoutSetEntity(id = 1L, sessionId = 99L, exerciseId = 10L, weight = 300.0, reps = 10, rpe = 8.0),
            WorkoutSetEntity(id = 2L, sessionId = 42L, exerciseId = 10L, weight = 100.0, reps = 5, rpe = 8.0),
            WorkoutSetEntity(id = 3L, sessionId = 7L, exerciseId = 10L, weight = 90.0, reps = 5, rpe = 8.0),
        )

        val result = buildWorkoutProgressComparison(
            dayId = 7L,
            routineId = 1L,
            startedAt = 4_000L,
            activeSessionId = 123L,
            currentSets = currentSets,
            sessions = sessions,
            sets = sets,
            days = listOf(WorkoutDayEntity(7L, 1L, "Push", 0)),
            exercises = listOf(ExerciseEntity(10L, "Bench Press", "Borst", "Barbell")),
        )

        assertEquals(10.0, result?.progressionPercent ?: 0.0, 0.01)
        assertEquals(500.0, result?.previousVolume ?: 0.0, 0.0)
    }

    @Test
    fun buildWorkoutProgressComparison_returnsNullWhenNoComparablePreviousWorkoutExists() {
        val result = buildWorkoutProgressComparison(
            dayId = 7L,
            routineId = 1L,
            startedAt = 4_000L,
            activeSessionId = 123L,
            currentSets = listOf(
                LoggedSet(
                    id = 1L,
                    exerciseId = 10L,
                    performedExerciseId = 0L,
                    weight = 110.0,
                    reps = 5,
                    rpe = 8.0,
                    repsInReserve = 2,
                ),
            ),
            sessions = listOf(completedSession(id = 99L, dayId = 2L, routineId = 2L, startedAt = 3_000L)),
            sets = listOf(WorkoutSetEntity(id = 1L, sessionId = 99L, exerciseId = 11L, weight = 100.0, reps = 5, rpe = 8.0)),
            days = listOf(WorkoutDayEntity(7L, 1L, "Push", 0)),
            exercises = listOf(ExerciseEntity(10L, "Bench Press", "Borst", "Barbell")),
        )

        assertNull(result)
    }

    private fun completedSession(id: Long, dayId: Long, routineId: Long, startedAt: Long) = WorkoutSessionEntity(
        id = id,
        date = startedAt,
        duration = 3_600L,
        routineId = routineId,
        workoutDayId = dayId,
        startedAt = startedAt,
        endedAt = startedAt + 3_600_000L,
        status = "COMPLETED",
        completed = true,
    )
}
