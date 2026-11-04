package com.trainiq.domain.workout

data class WorkoutRestTimerIdentity(val sessionId: Long, val generation: String)
data class WorkoutRestTimerNow(val wallMillis: Long, val elapsedMillis: Long, val bootId: String)
data class WorkoutRestTimerChange(val sessionId: Long, val sessionStartedAt: Long,
    val expectedGeneration: String?, val timer: ScheduledWorkoutRestTimer?)
data class ScheduledWorkoutRestTimer(
    val identity: WorkoutRestTimerIdentity,
    val dayId: Long,
    val wallDeadline: Long,
    val elapsedDeadline: Long,
    val bootId: String,
    val totalSeconds: Int,
    val handled: Boolean = false,
)
enum class RestTimerCompletion { OBSOLETE, EARLY, HANDLED, COMPLETED, EXPIRED }

fun ScheduledWorkoutRestTimer.remainingSeconds(now: WorkoutRestTimerNow): Int {
    if (handled || bootId != now.bootId || bootId.isBlank()) return 0
    val remaining = (elapsedDeadline - now.elapsedMillis).coerceAtLeast(0)
    return (remaining / 1_000 + if (remaining % 1_000 > 0) 1 else 0)
        .coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
}

fun ScheduledWorkoutRestTimer.completion(now: WorkoutRestTimerNow): RestTimerCompletion = when {
    handled -> RestTimerCompletion.HANDLED
    bootId.isBlank() || bootId != now.bootId -> RestTimerCompletion.EXPIRED
    now.elapsedMillis < elapsedDeadline -> RestTimerCompletion.EARLY
    now.elapsedMillis - elapsedDeadline > 30_000 -> RestTimerCompletion.EXPIRED
    else -> RestTimerCompletion.COMPLETED
}

fun ScheduledWorkoutRestTimer.adjusted(deltaSeconds: Int, now: WorkoutRestTimerNow, generation: String): ScheduledWorkoutRestTimer? {
    val seconds = (remainingSeconds(now).toLong() + deltaSeconds).coerceIn(0, Int.MAX_VALUE.toLong()).toInt()
    return newWorkoutRestTimer(identity.sessionId, dayId, seconds, now, generation)
}

fun newWorkoutRestTimer(sessionId: Long, dayId: Long, seconds: Int, now: WorkoutRestTimerNow,
    generation: String): ScheduledWorkoutRestTimer? = if (seconds <= 0) null else ScheduledWorkoutRestTimer(
    WorkoutRestTimerIdentity(sessionId, generation), dayId, now.wallMillis + seconds * 1_000L,
    now.elapsedMillis + seconds * 1_000L, now.bootId, seconds)

fun com.trainiq.domain.model.ActiveWorkoutSession.scheduledRestTimer(): ScheduledWorkoutRestTimer? {
    val generation = restTimerGeneration ?: return null
    return ScheduledWorkoutRestTimer(WorkoutRestTimerIdentity(sessionId, generation), dayId,
        restTimerEndsAt ?: 0, restTimerElapsedDeadline ?: 0, restTimerBootId.orEmpty(), restTimerTotalSeconds, restTimerHandled)
}
