package com.trainiq.data.repository

import com.trainiq.core.database.ExerciseEntity
import com.trainiq.core.database.WorkoutDayEntity
import com.trainiq.core.database.WorkoutExerciseEntity
import com.trainiq.core.database.WorkoutRoutineEntity
import com.trainiq.data.local.TrainIqStorageState
import com.trainiq.domain.model.ReadinessLevel
import org.junit.Assert.assertEquals
import org.junit.Test

class TrainIqRepositoryTest {
    @Test
    fun withExerciseAddedToRoutine_withoutSession_createsDefaultSessionAndExercise() {
        val state = TrainIqStorageState(
            routines = listOf(WorkoutRoutineEntity(id = 1L, name = "Upper", description = "", active = true)),
        )

        val updated = state.withExerciseAddedToRoutine(
            routineId = 1L,
            name = "Bench Press",
            muscleGroup = "Chest",
            equipment = "Barbell",
            targetSets = 3,
            repRange = "8-12",
            restSeconds = 90,
        )

        assertEquals(1, updated.days.size)
        assertEquals("Session 1", updated.days.first().name)
        assertEquals(1, updated.workoutExercises.size)
        assertEquals(updated.days.first().id, updated.workoutExercises.first().dayId)
        assertEquals("Bench Press", updated.exercises.first().name)
    }

    @Test
    fun withExerciseAddedToRoutine_withExistingSession_preservesSessionAndAppendsExerciseOrder() {
        val state = TrainIqStorageState(
            routines = listOf(WorkoutRoutineEntity(id = 1L, name = "Upper", description = "", active = true)),
            days = listOf(WorkoutDayEntity(id = 7L, routineId = 1L, name = "Push", orderIndex = 0)),
            exercises = listOf(ExerciseEntity(id = 3L, name = "Bench Press", muscleGroup = "Chest", equipment = "Barbell")),
            workoutExercises = listOf(
                WorkoutExerciseEntity(
                    id = 4L,
                    dayId = 7L,
                    exerciseId = 3L,
                    targetSets = 3,
                    repRange = "8-12",
                    restSeconds = 90,
                    orderIndex = 0,
                ),
            ),
        )

        val updated = state.withExerciseAddedToRoutine(
            routineId = 1L,
            name = "Row",
            muscleGroup = "Back",
            equipment = "Cable",
            targetSets = 4,
            repRange = "10-12",
            restSeconds = 75,
        )

        assertEquals(listOf("Push"), updated.days.map { it.name })
        assertEquals(2, updated.workoutExercises.size)
        assertEquals(7L, updated.workoutExercises.last().dayId)
        assertEquals(1, updated.workoutExercises.last().orderIndex)
    }

    @Test
    fun resolveReadiness_withLowRpeAndCompletedSessions_returnsIncrease() {
        assertEquals(
            ReadinessLevel.INCREASE,
            resolveReadiness(lastSessionAvgRpe = 6.9f, recentAverageRir = 2.3f, completedRecentSessions = true),
        )
    }

    @Test
    fun resolveReadiness_withHighRpe_returnsDeload() {
        assertEquals(
            ReadinessLevel.DELOAD,
            resolveReadiness(lastSessionAvgRpe = 9.1f, recentAverageRir = 1.0f, completedRecentSessions = true),
        )
    }

    @Test
    fun resolveReadiness_withBoundaryConditions_returnsMaintain() {
        assertEquals(
            ReadinessLevel.MAINTAIN,
            resolveReadiness(lastSessionAvgRpe = 7.0f, recentAverageRir = 1.5f, completedRecentSessions = true),
        )
        assertEquals(
            ReadinessLevel.MAINTAIN,
            resolveReadiness(lastSessionAvgRpe = 6.2f, recentAverageRir = 2.5f, completedRecentSessions = false),
        )
        assertEquals(
            ReadinessLevel.MAINTAIN,
            resolveReadiness(lastSessionAvgRpe = 9.0f, recentAverageRir = 1.2f, completedRecentSessions = true),
        )
    }

    @Test
    fun resolveReadiness_withVeryLowRir_returnsDeloadEvenWhenRpeIsModerate() {
        assertEquals(
            ReadinessLevel.DELOAD,
            resolveReadiness(lastSessionAvgRpe = 8.4f, recentAverageRir = 0.5f, completedRecentSessions = true),
        )
    }
}
