package com.trainiq.data.repository

import com.trainiq.core.database.ExerciseEntity
import com.trainiq.core.database.PerformedExerciseEntity
import com.trainiq.core.database.RoutineSetEntity
import com.trainiq.core.database.WorkoutSessionEntity
import com.trainiq.core.database.WorkoutSetEntity
import com.trainiq.core.database.WorkoutDayEntity
import com.trainiq.core.database.WorkoutExerciseEntity
import com.trainiq.core.database.WorkoutRoutineEntity
import com.trainiq.data.local.ActiveWorkoutDraftStorage
import com.trainiq.data.local.ActiveWorkoutSessionStorage
import com.trainiq.data.local.ActiveWorkoutSetStorage
import com.trainiq.data.local.TrainIqStorageState
import com.trainiq.domain.model.ReadinessLevel
import com.trainiq.domain.model.SetType
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
            targetWeightKg = 80.0,
            targetRpe = 8.0,
        )

        assertEquals(1, updated.days.size)
        assertEquals("Session 1", updated.days.first().name)
        assertEquals(1, updated.workoutExercises.size)
        assertEquals(updated.days.first().id, updated.workoutExercises.first().dayId)
        assertEquals("Bench Press", updated.exercises.first().name)
        assertEquals(80.0, updated.workoutExercises.first().targetWeightKg, 0.0)
        assertEquals(8.0, updated.workoutExercises.first().targetRpe, 0.0)
        assertEquals(3, updated.routineSets.size)
        assertEquals(updated.workoutExercises.first().id, updated.routineSets.first().workoutExerciseId)
        assertEquals(12, updated.routineSets.first().targetReps)
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
        assertEquals(4, updated.routineSets.count { it.workoutExerciseId == updated.workoutExercises.last().id })
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

    @Test
    fun activeWorkoutSessionStorage_toDomain_preservesDraftsSetsAndTimer() {
        val storage = ActiveWorkoutSessionStorage(
            dayId = 7L,
            startedAt = 100L,
            updatedAt = 200L,
            loggedSets = listOf(
                ActiveWorkoutSetStorage(
                    id = 1L,
                    exerciseId = 3L,
                    performedExerciseId = 9L,
                    sourceWorkoutExerciseId = 4L,
                    weight = 100.0,
                    reps = 5,
                    rpe = 8.0,
                    repsInReserve = 2,
                    setType = SetType.FAILURE,
                    loggedAt = 150L,
                ),
            ),
            drafts = mapOf(3L to ActiveWorkoutDraftStorage(weight = "100", reps = "5", rpe = "8", setType = SetType.FAILURE)),
            collapsedExerciseIds = setOf(4L),
            restTimerEndsAt = 1_000L,
            restTimerTotalSeconds = 120,
        )

        val domain = storage.toDomain()

        assertEquals(7L, domain.dayId)
        assertEquals(1, domain.loggedSets.size)
        assertEquals(SetType.FAILURE, domain.loggedSets.first().setType)
        assertEquals(9L, domain.loggedSets.first().performedExerciseId)
        assertEquals(4L, domain.loggedSets.first().sourceWorkoutExerciseId)
        assertEquals("100", domain.drafts.getValue(3L).weight)
        assertEquals(setOf(4L), domain.collapsedExerciseIds)
        assertEquals(1_000L, domain.restTimerEndsAt)
        assertEquals(120, domain.restTimerTotalSeconds)
    }

    @Test
    fun withRoutineSetAdded_withExistingSets_copiesPreviousValues() {
        val state = TrainIqStorageState(
            workoutExercises = listOf(
                WorkoutExerciseEntity(
                    id = 4L,
                    dayId = 7L,
                    exerciseId = 3L,
                    targetSets = 1,
                    repRange = "5",
                    restSeconds = 120,
                ),
            ),
            routineSets = listOf(
                RoutineSetEntity(
                    id = 10L,
                    workoutExerciseId = 4L,
                    orderIndex = 0,
                    setType = SetType.WARM_UP.name,
                    targetReps = 8,
                    targetWeightKg = 60.0,
                    restSeconds = 90,
                    targetRpe = 6.0,
                ),
            ),
        )

        val updated = state.withRoutineSetAdded(4L)

        assertEquals(2, updated.routineSets.size)
        assertEquals(1, updated.routineSets.last().orderIndex)
        assertEquals(SetType.WARM_UP.name, updated.routineSets.last().setType)
        assertEquals(60.0, updated.routineSets.last().targetWeightKg, 0.0)
        assertEquals(2, updated.workoutExercises.first().targetSets)
    }

    @Test
    fun withExerciseReplacedInPlan_preservesPlanSetsOrderSupersetAndHistory() {
        val completedSession = WorkoutSessionEntity(
            id = 30L,
            date = 1_000L,
            duration = 1_800L,
            startedAt = 1_000L,
            endedAt = 2_800L,
            status = "COMPLETED",
            completed = true,
        )
        val state = TrainIqStorageState(
            exercises = listOf(
                ExerciseEntity(id = 3L, name = "Bench Press", muscleGroup = "Chest", equipment = "Barbell"),
                ExerciseEntity(id = 9L, name = "Incline Press", muscleGroup = "Chest", equipment = "Dumbbell"),
            ),
            workoutExercises = listOf(
                WorkoutExerciseEntity(
                    id = 4L,
                    dayId = 7L,
                    exerciseId = 3L,
                    targetSets = 2,
                    repRange = "8-12",
                    restSeconds = 90,
                    supersetGroupId = 11L,
                    orderIndex = 2,
                ),
            ),
            routineSets = listOf(
                RoutineSetEntity(id = 10L, workoutExerciseId = 4L, orderIndex = 0, targetReps = 8),
                RoutineSetEntity(id = 11L, workoutExerciseId = 4L, orderIndex = 1, targetReps = 10),
            ),
            sessions = listOf(completedSession),
            performedExercises = listOf(
                PerformedExerciseEntity(id = 21L, sessionId = 30L, exerciseId = 3L, sourceWorkoutExerciseId = 4L, orderIndex = 0),
            ),
            workoutSets = listOf(
                WorkoutSetEntity(
                    id = 40L,
                    sessionId = 30L,
                    exerciseId = 3L,
                    performedExerciseId = 21L,
                    weight = 80.0,
                    reps = 8,
                    rpe = 8.0,
                    setType = SetType.NORMAL.name,
                    completed = true,
                ),
            ),
        )

        val updated = state.withExerciseReplacedInPlan(workoutExerciseId = 4L, newExerciseId = 9L)

        val plan = updated.workoutExercises.single()
        assertEquals(4L, plan.id)
        assertEquals(9L, plan.exerciseId)
        assertEquals(7L, plan.dayId)
        assertEquals(2, plan.orderIndex)
        assertEquals(11L, plan.supersetGroupId)
        assertEquals(listOf(10L, 11L), updated.routineSets.map { it.id })
        assertEquals(listOf(4L, 4L), updated.routineSets.map { it.workoutExerciseId })
        assertEquals(state.sessions, updated.sessions)
        assertEquals(state.performedExercises, updated.performedExercises)
        assertEquals(state.workoutSets, updated.workoutSets)
    }

    @Test
    fun updateRoutineSetAndRenumber_afterDeleteKeepsExerciseTargetsInSync() {
        val state = TrainIqStorageState(
            workoutExercises = listOf(
                WorkoutExerciseEntity(
                    id = 4L,
                    dayId = 7L,
                    exerciseId = 3L,
                    targetSets = 2,
                    repRange = "5",
                    restSeconds = 120,
                ),
            ),
            routineSets = listOf(
                RoutineSetEntity(id = 10L, workoutExerciseId = 4L, orderIndex = 0, targetReps = 5),
                RoutineSetEntity(id = 11L, workoutExerciseId = 4L, orderIndex = 1, targetReps = 8),
            ),
        )

        val updated = state.copy(routineSets = state.routineSets.filterNot { it.id == 10L })
            .renumberRoutineSets(4L)
            .withWorkoutExerciseTargetsSynced(4L)

        assertEquals(0, updated.routineSets.single().orderIndex)
        assertEquals(1, updated.workoutExercises.first().targetSets)
        assertEquals("8", updated.workoutExercises.first().repRange)
    }

    @Test
    fun ensurePerformedExercisesForActiveSession_createsStableExerciseLogRows() {
        val state = TrainIqStorageState(
            workoutExercises = listOf(
                WorkoutExerciseEntity(id = 4L, dayId = 7L, exerciseId = 3L, targetSets = 3, repRange = "5", restSeconds = 120, orderIndex = 0),
                WorkoutExerciseEntity(id = 5L, dayId = 7L, exerciseId = 6L, targetSets = 2, repRange = "8", restSeconds = 90, orderIndex = 1),
            ),
            activeWorkoutSession = ActiveWorkoutSessionStorage(sessionId = 20L, dayId = 7L, startedAt = 100L),
        )

        val updated = state.copy(performedExercises = state.ensurePerformedExercisesForActiveSession(state.activeWorkoutSession!!))

        assertEquals(2, updated.performedExercises.size)
        assertEquals(20L, updated.performedExercises.first().sessionId)
        assertEquals(3L, updated.performedExercises.first().exerciseId)
        assertEquals(4L, updated.performedExercises.first().sourceWorkoutExerciseId)
    }

    @Test
    fun completedHistoryInputs_excludeDraftSessionsAndInvalidSets() {
        val completedSession = WorkoutSessionEntity(
            id = 1L,
            date = 1_000L,
            duration = 1_800L,
            startedAt = 1_000L,
            endedAt = 2_800L,
            status = "COMPLETED",
            completed = true,
        )
        val draftSession = completedSession.copy(id = 2L, status = "DRAFT", completed = false)
        val sets = listOf(
            WorkoutSetEntity(
                id = 1L,
                sessionId = 1L,
                exerciseId = 3L,
                performedExerciseId = 10L,
                weight = 100.0,
                reps = 5,
                rpe = 8.0,
                setType = SetType.NORMAL.name,
                completed = true,
            ),
            WorkoutSetEntity(
                id = 2L,
                sessionId = 2L,
                exerciseId = 3L,
                performedExerciseId = 11L,
                weight = 120.0,
                reps = 5,
                rpe = 8.0,
                setType = SetType.NORMAL.name,
                completed = true,
            ),
            WorkoutSetEntity(
                id = 3L,
                sessionId = 1L,
                exerciseId = 3L,
                performedExerciseId = 10L,
                weight = 80.0,
                reps = 0,
                rpe = 0.0,
                setType = SetType.NORMAL.name,
                completed = true,
            ),
        )

        val validSets = sets.filter { set ->
            val session = listOf(completedSession, draftSession).firstOrNull { it.id == set.sessionId }
            session?.completed == true && session.status == "COMPLETED" && set.completed && set.reps > 0 && set.weight >= 0.0
        }

        assertEquals(listOf(1L), validSets.map { it.id })
    }
}
