package com.trainiq.data.repository

import com.trainiq.core.database.WorkoutDayEntity
import com.trainiq.core.database.WorkoutRoutineEntity
import com.trainiq.data.local.ActiveWorkoutSessionStorage
import com.trainiq.data.local.ActiveWorkoutSetStorage
import com.trainiq.data.local.TrainIqStorageState
import com.trainiq.domain.model.SetType
import com.trainiq.domain.model.WorkoutLogEventType
import com.trainiq.domain.model.WorkoutSyncStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WorkoutLogEventTest {
    @Test
    fun appendWorkoutSetEvent_withAddSetEvent_persistsPendingEventAndUpdatesActiveSession() {
        val active = ActiveWorkoutSessionStorage(sessionId = 12L, dayId = 7L, startedAt = 1_000L)
        val state = TrainIqStorageState(
            routines = listOf(WorkoutRoutineEntity(id = 1L, name = "Routine", description = "", active = true)),
            days = listOf(WorkoutDayEntity(id = 7L, routineId = 1L, name = "Push", orderIndex = 0)),
            activeWorkoutSession = active,
        )
        val set = ActiveWorkoutSetStorage(
            id = 1L,
            exerciseId = 3L,
            weight = 80.0,
            reps = 8,
            rpe = 8.0,
            repsInReserve = 2,
            setType = SetType.NORMAL,
            restSeconds = 90,
            orderIndex = 0,
            loggedAt = 2_000L,
        )

        val updated = state.appendWorkoutSetEvent(
            dayId = 7L,
            sessionId = 12L,
            set = set,
            now = 2_000L,
            undoWindowMillis = 15_000L,
        )

        assertEquals(listOf(set), updated.activeWorkoutSession?.loggedSets)
        assertEquals(1, updated.workoutLogEvents.size)
        assertEquals(WorkoutLogEventType.ADD_SET, updated.workoutLogEvents.single().type)
        assertEquals(WorkoutSyncStatus.PENDING, updated.workoutLogEvents.single().syncStatus)
        assertEquals(17_000L, updated.workoutLogEvents.single().undoExpiresAt)
        assertEquals(1, updated.workoutLoggingSummary(dayId = 7L, now = 2_500L).pendingCount)
        assertEquals(updated.workoutLogEvents.single().id, updated.workoutLoggingSummary(dayId = 7L, now = 2_500L).lastUndoableEventId)
    }

    @Test
    fun undoWorkoutSetEvent_withUnexpiredEvent_restoresPreviousSetListAndAppendsUndoEvent() {
        val existingSet = ActiveWorkoutSetStorage(id = 1L, exerciseId = 3L, weight = 75.0, reps = 8, loggedAt = 1_000L)
        val addedSet = ActiveWorkoutSetStorage(id = 2L, exerciseId = 3L, weight = 80.0, reps = 8, loggedAt = 2_000L)
        val state = TrainIqStorageState(
            activeWorkoutSession = ActiveWorkoutSessionStorage(
                sessionId = 12L,
                dayId = 7L,
                startedAt = 1_000L,
                loggedSets = listOf(existingSet),
            ),
        ).appendWorkoutSetEvent(
            dayId = 7L,
            sessionId = 12L,
            set = addedSet,
            now = 2_000L,
            undoWindowMillis = 15_000L,
        )
        val eventId = state.workoutLogEvents.single().id

        val updated = state.undoWorkoutSetEvent(eventId = eventId, now = 10_000L)

        assertEquals(listOf(existingSet), updated.activeWorkoutSession?.loggedSets)
        assertEquals(listOf(WorkoutLogEventType.ADD_SET, WorkoutLogEventType.UNDO_SET), updated.workoutLogEvents.map { it.type })
        assertEquals(eventId, updated.workoutLogEvents.last().targetEventId)
        assertEquals(2, updated.workoutLoggingSummary(dayId = 7L, now = 10_000L).pendingCount)
        assertNull(updated.workoutLoggingSummary(dayId = 7L, now = 10_000L).lastUndoableEventId)
    }

    @Test
    fun undoWorkoutSetEvent_afterUndoWindowExpires_keepsStateUnchanged() {
        val addedSet = ActiveWorkoutSetStorage(id = 1L, exerciseId = 3L, weight = 80.0, reps = 8, loggedAt = 2_000L)
        val state = TrainIqStorageState(
            activeWorkoutSession = ActiveWorkoutSessionStorage(sessionId = 12L, dayId = 7L, startedAt = 1_000L),
        ).appendWorkoutSetEvent(
            dayId = 7L,
            sessionId = 12L,
            set = addedSet,
            now = 2_000L,
            undoWindowMillis = 15_000L,
        )

        val updated = state.undoWorkoutSetEvent(eventId = state.workoutLogEvents.single().id, now = 20_001L)

        assertEquals(state.activeWorkoutSession?.loggedSets, updated.activeWorkoutSession?.loggedSets)
        assertEquals(state.workoutLogEvents, updated.workoutLogEvents)
        assertNull(updated.workoutLoggingSummary(dayId = 7L, now = 20_001L).lastUndoableEventId)
    }
}
