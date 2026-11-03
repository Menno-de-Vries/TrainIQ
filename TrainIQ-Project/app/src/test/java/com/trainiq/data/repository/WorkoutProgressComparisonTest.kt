package com.trainiq.data.repository

import com.trainiq.core.database.ExerciseEntity
import com.trainiq.core.database.WorkoutDayEntity
import com.trainiq.core.database.WorkoutSessionEntity
import com.trainiq.core.database.WorkoutSetEntity
import com.trainiq.domain.model.LoggedSet
import com.trainiq.domain.model.SetType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WorkoutProgressComparisonTest {
    @Test
    fun completedFallbackSelectionIsSymmetricWhenOnlySpecialSetsWerePerformed() {
        val result = compare(
            current = listOf(
                LoggedSet(exerciseId = 10L, weight = 30.0, reps = 10, rpe = 0.0, setType = SetType.DROP_SET),
                LoggedSet(exerciseId = 10L, weight = 999.0, reps = 10, rpe = 10.0, completed = false),
            ),
            previous = listOf(
                WorkoutSetEntity(1L, 42L, 10L, 20.0, 10, 0.0, setType = "DROP_SET"),
                WorkoutSetEntity(2L, 42L, 10L, 999.0, 10, 10.0, completed = false),
            ),
        )
        assertEquals(200.0, result?.previousVolume ?: -1.0, 0.0)
        assertEquals(300.0, result?.currentVolume ?: -1.0, 0.0)
        assertEquals(50.0, result?.progressionPercent ?: -1.0, 0.0)
    }

    @Test
    fun bothSidesExcludeWarmupAndIncompleteRowsWhenCompletedWorkSetsExist() {
        val result = compare(
            current = listOf(
                LoggedSet(exerciseId = 10L, weight = 60.0, reps = 10, rpe = 0.0),
                LoggedSet(exerciseId = 10L, weight = 20.0, reps = 10, rpe = 3.0, setType = SetType.WARM_UP),
                LoggedSet(exerciseId = 10L, weight = 999.0, reps = 10, rpe = 10.0, completed = false),
            ),
            previous = listOf(
                WorkoutSetEntity(1L, 42L, 10L, 50.0, 10, 0.0, setType = "BACK_OFF"),
                WorkoutSetEntity(2L, 42L, 10L, 20.0, 10, 3.0, setType = "WARM_UP"),
                WorkoutSetEntity(3L, 42L, 10L, 999.0, 10, 10.0, completed = false),
            ),
        )
        assertEquals(500.0, result?.previousVolume ?: -1.0, 0.0)
        assertEquals(600.0, result?.currentVolume ?: -1.0, 0.0)
        assertEquals(20.0, result?.progressionPercent ?: -1.0, 0.0)
    }

    private fun compare(current: List<LoggedSet>, previous: List<WorkoutSetEntity>) = buildWorkoutProgressComparison(
        dayId = 7L, routineId = 1L, startedAt = 4_000L, activeSessionId = 123L,
        currentSets = current, sessions = listOf(completedSession(42L, 7L, 1L, 2_000L)),
        sets = previous, days = listOf(WorkoutDayEntity(7L, 1L, "Push", 0)),
        exercises = listOf(ExerciseEntity(10L, "Bench", "Borst", "Barbell")),
    )

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
        assertEquals("Vergelijking met vorige vergelijkbare training: 500 kg naar 550 kg (10,0%).", result?.summary)
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

    @Test
    fun defaultWorkoutSessionName_usesDutchLabel() {
        assertEquals("Sessie 3", defaultWorkoutSessionName(existingSessionCount = 2))
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
