package com.trainiq.data.repository

import com.trainiq.core.database.WorkoutDayEntity
import com.trainiq.core.database.WorkoutSessionEntity
import com.trainiq.data.local.ActiveWorkoutDraftStorage
import com.trainiq.data.local.ActiveWorkoutSessionStorage
import com.trainiq.data.local.ActiveWorkoutSetStorage
import com.trainiq.data.local.TrainIqStorageState
import com.trainiq.domain.model.ActiveWorkoutSetDraft
import com.trainiq.domain.model.LoggedSet
import com.trainiq.domain.model.SetType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ActiveWorkoutSessionMutationsTest {
    @Test
    fun startOrResume_createsDraftSessionAndPreservesInitialDraftsInOneStateTransition() {
        val state = TrainIqStorageState(
            days = listOf(WorkoutDayEntity(id = 7L, routineId = 3L, name = "Push", orderIndex = 0)),
            sessions = listOf(WorkoutSessionEntity(id = 5L, date = 1_000L, duration = 0L)),
        )

        val result = ActiveWorkoutSessionMutations.startOrResume(
            state = state,
            dayId = 7L,
            initialDrafts = mapOf(11L to ActiveWorkoutSetDraft(weight = "80", reps = "8", rpe = "7")),
            now = 10_000L,
        )

        assertEquals(6L, result.active.sessionId)
        assertEquals(7L, result.active.dayId)
        assertEquals(3L, result.active.routineId)
        assertEquals(ActiveWorkoutDraftStorage(weight = "80", reps = "8", rpe = "7"), result.active.drafts.getValue(11L))
        assertEquals("DRAFT", result.state.sessions.first { it.id == 6L }.status)
        assertEquals(false, result.state.sessions.first { it.id == 6L }.completed)
        assertEquals(result.active, result.state.activeWorkoutSession)
    }

    @Test
    fun startOrResume_rejectsStartingDifferentDayWhenAnotherWorkoutIsActive() {
        val state = TrainIqStorageState(
            activeWorkoutSession = ActiveWorkoutSessionStorage(sessionId = 4L, dayId = 1L),
        )

        val error = assertThrows(IllegalStateException::class.java) {
            ActiveWorkoutSessionMutations.startOrResume(
                state = state,
                dayId = 2L,
                initialDrafts = emptyMap(),
                now = 10_000L,
            )
        }

        assertEquals("Rond je actieve training af of verwijder die voordat je een andere training start.", error.message)
    }

    @Test
    fun logSet_appendsUndoableEventUpdatesDraftAndRestTimerAtomically() {
        val active = ActiveWorkoutSessionStorage(sessionId = 12L, dayId = 7L, startedAt = 1_000L)
        val state = TrainIqStorageState(activeWorkoutSession = active)

        val result = ActiveWorkoutSessionMutations.logSet(
            state = state,
            dayId = 7L,
            set = LoggedSet(
                exerciseId = 3L,
                weight = 82.5,
                reps = 8,
                rpe = 7.5,
                repsInReserve = 2,
                setType = SetType.NORMAL,
            ),
            draft = ActiveWorkoutSetDraft(weight = "82.5", reps = "8", rpe = "7.5"),
            restSeconds = 90,
            now = 20_000L,
        )

        val updatedActive = result.state.activeWorkoutSession!!
        assertEquals(listOf(1L), updatedActive.loggedSets.map { it.id })
        assertEquals(82.5, updatedActive.loggedSets.single().weight, 0.0)
        assertEquals(110_000L, updatedActive.restTimerEndsAt)
        assertEquals(90, updatedActive.restTimerTotalSeconds)
        assertEquals(ActiveWorkoutDraftStorage(weight = "82.5", reps = "8", rpe = "7.5"), updatedActive.drafts.getValue(3L))
        assertEquals(1, result.state.workoutLogEvents.size)
        assertEquals(ActiveWorkoutSetStorage::class, result.state.workoutLogEvents.single().set!!::class)
        assertEquals(updatedActive, result.active)
    }
}
