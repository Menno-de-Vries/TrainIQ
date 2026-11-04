package com.trainiq.data.repository

import androidx.room.withTransaction
import com.trainiq.core.database.ActiveWorkoutSessionEntity
import com.trainiq.core.database.TrainIqDatabase
import com.trainiq.data.local.ActiveWorkoutSessionStorage
import com.trainiq.domain.workout.*
import javax.inject.Inject

class RoomWorkoutRestTimerStore @Inject constructor(private val database: TrainIqDatabase) : WorkoutRestTimerStore {
    override suspend fun read(): ScheduledWorkoutRestTimer? = database.dao().readActiveWorkoutSessionsForExport()
        .firstOrNull()?.scheduledRestTimer()

    override suspend fun claim(identity: WorkoutRestTimerIdentity, now: WorkoutRestTimerNow): RestTimerCompletion =
        database.withTransaction {
            val current = database.dao().readActiveWorkoutSessionsForExport().firstOrNull()?.scheduledRestTimer()
                ?.takeIf { it.identity == identity } ?: return@withTransaction RestTimerCompletion.OBSOLETE
            val decision = current.completion(now)
            if (decision == RestTimerCompletion.COMPLETED || decision == RestTimerCompletion.EXPIRED) {
                if (database.dao().claimWorkoutRestTimer(identity.sessionId, identity.generation) != 1)
                    return@withTransaction RestTimerCompletion.HANDLED
            }
            decision
        }
}

internal fun ActiveWorkoutSessionEntity.scheduledRestTimer(): ScheduledWorkoutRestTimer? {
    // A legacy deadline has no trustworthy clock identity: reconcile silently expires it.
    val generation = restTimerGeneration ?: if (restTimerEndsAt != null) "legacy:$sessionId" else return null
    return ScheduledWorkoutRestTimer(WorkoutRestTimerIdentity(sessionId, generation), dayId,
        restTimerEndsAt ?: 0, restTimerElapsedDeadline ?: 0, restTimerBootId.orEmpty(), restTimerTotalSeconds, restTimerHandled)
}

internal fun ActiveWorkoutSessionStorage.withRestTimer(timer: ScheduledWorkoutRestTimer?) = copy(
    restTimerEndsAt = timer?.wallDeadline,
    restTimerTotalSeconds = timer?.totalSeconds ?: 0,
    restTimerGeneration = timer?.identity?.generation,
    restTimerElapsedDeadline = timer?.elapsedDeadline,
    restTimerBootId = timer?.bootId,
    restTimerHandled = timer?.handled ?: false,
)
