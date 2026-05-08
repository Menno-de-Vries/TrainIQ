package com.trainiq.data.repository

import com.trainiq.core.database.WorkoutSessionEntity
import com.trainiq.data.local.ActiveWorkoutDraftStorage
import com.trainiq.data.local.ActiveWorkoutSessionStorage
import com.trainiq.data.local.ActiveWorkoutSetStorage
import com.trainiq.data.local.TrainIqStorageState
import com.trainiq.domain.model.ActiveWorkoutSetDraft
import com.trainiq.domain.model.LoggedSet

internal object ActiveWorkoutSessionMutations {
    data class Result(
        val state: TrainIqStorageState,
        val active: ActiveWorkoutSessionStorage,
    )

    fun startOrResume(
        state: TrainIqStorageState,
        dayId: Long,
        initialDrafts: Map<Long, ActiveWorkoutSetDraft>,
        now: Long,
    ): Result {
        val existing = state.activeWorkoutSession
        if (existing != null && existing.dayId != dayId) {
            error("Rond je actieve training af of verwijder die voordat je een andere training start.")
        }
        val active = if (existing != null && existing.dayId == dayId) {
            existing.copy(updatedAt = now)
        } else {
            val day = state.days.firstOrNull { it.id == dayId }
            ActiveWorkoutSessionStorage(
                sessionId = (state.sessions.maxOfOrNull { it.id } ?: 0L) + 1L,
                dayId = dayId,
                routineId = day?.routineId,
                startedAt = now,
                updatedAt = now,
                drafts = initialDrafts.mapValues { it.value.toStorage() },
            )
        }
        val existingSessionIds = state.sessions.map { it.id }.toSet()
        val draftSession = WorkoutSessionEntity(
            id = active.sessionId,
            date = active.startedAt,
            duration = ((now - active.startedAt) / 1_000).coerceAtLeast(0),
            caloriesBurned = 0,
            routineId = active.routineId,
            workoutDayId = active.dayId,
            startedAt = active.startedAt,
            endedAt = 0L,
            status = "DRAFT",
            completed = false,
        )
        val nextState = state.copy(
            sessions = if (active.sessionId in existingSessionIds) {
                state.sessions.map { if (it.id == active.sessionId) draftSession else it }
            } else {
                listOf(draftSession) + state.sessions
            },
            performedExercises = state.ensurePerformedExercisesForActiveSession(active),
            activeWorkoutSession = active,
        )
        return Result(nextState, active)
    }

    fun logSet(
        state: TrainIqStorageState,
        dayId: Long,
        set: LoggedSet,
        draft: ActiveWorkoutSetDraft,
        restSeconds: Int,
        now: Long,
    ): Result {
        val active = state.activeWorkoutSession?.takeIf { it.dayId == dayId }
            ?: ActiveWorkoutSessionStorage(dayId = dayId, startedAt = now)
        val performedExercise = state.performedExercises.firstOrNull {
            it.sessionId == active.sessionId &&
                it.exerciseId == set.exerciseId &&
                (set.sourceWorkoutExerciseId == null || it.sourceWorkoutExerciseId == set.sourceWorkoutExerciseId)
        }
        val storedSet = ActiveWorkoutSetStorage(
            id = (active.loggedSets.maxOfOrNull { it.id } ?: 0L) + 1L,
            exerciseId = set.exerciseId,
            performedExerciseId = performedExercise?.id ?: set.performedExerciseId,
            sourceWorkoutExerciseId = performedExercise?.sourceWorkoutExerciseId ?: set.sourceWorkoutExerciseId,
            weight = set.weight,
            reps = set.reps,
            rpe = set.rpe,
            repsInReserve = set.repsInReserve,
            setType = set.setType,
            restSeconds = restSeconds.coerceAtLeast(0),
            orderIndex = active.loggedSets.count { it.activeKey == set.activeKey },
            completed = true,
            loggedAt = now,
        )
        val nextState = state
            .copy(
                activeWorkoutSession = active.copy(
                    drafts = active.drafts.toMutableMap().apply { put(set.activeKey, draft.toStorage()) },
                    collapsedExerciseIds = active.collapsedExerciseIds - set.activeKey,
                    restTimerEndsAt = if (restSeconds > 0) now + restSeconds * 1_000L else null,
                    restTimerTotalSeconds = restSeconds.coerceAtLeast(0),
                ),
            )
            .appendWorkoutSetEvent(
                dayId = dayId,
                sessionId = active.sessionId,
                set = storedSet,
                now = now,
            )
        return Result(nextState, requireNotNull(nextState.activeWorkoutSession))
    }
}

internal val ActiveWorkoutSetStorage.activeKey: Long
    get() = sourceWorkoutExerciseId ?: exerciseId

internal val LoggedSet.activeKey: Long
    get() = sourceWorkoutExerciseId ?: exerciseId

internal fun ActiveWorkoutSetDraft.toStorage() = ActiveWorkoutDraftStorage(
    weight = weight,
    reps = reps,
    rpe = rpe,
    setType = setType,
)
