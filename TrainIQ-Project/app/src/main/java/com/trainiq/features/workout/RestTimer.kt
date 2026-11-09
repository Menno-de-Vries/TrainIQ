package com.trainiq.features.workout

import com.trainiq.core.ui.launchUserAction
import com.trainiq.domain.model.ActiveWorkoutSession
import com.trainiq.domain.model.ActiveWorkoutSetEntry
import com.trainiq.domain.workout.scheduledRestTimer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal data class RestTimerValue(val endsAt: Long?, val seconds: Int,
    val timer: com.trainiq.domain.workout.ScheduledWorkoutRestTimer? = null,
    val sessionId: Long = 0, val sessionStartedAt: Long = 0,
    val raw: RestTimerMetadata? = null,
) {
    val metadata: RestTimerMetadata get() = raw ?: RestTimerMetadata(endsAt, seconds,
        timer?.identity?.generation, timer?.elapsedDeadline, timer?.bootId, timer?.handled ?: false)
}

internal data class RestTimerMetadata(val endsAt: Long?, val seconds: Int, val generation: String?,
    val elapsedDeadline: Long?, val bootId: String?, val handled: Boolean)

internal fun ActiveWorkoutSession.timerValue() = RestTimerValue(restTimerEndsAt, restTimerTotalSeconds,
    scheduledRestTimer(), sessionId, startedAt, RestTimerMetadata(restTimerEndsAt, restTimerTotalSeconds,
        restTimerGeneration, restTimerElapsedDeadline, restTimerBootId, restTimerHandled))

private data class TimerVersion(val sessionId: Long, val startedAt: Long, val metadata: RestTimerMetadata)
private fun RestTimerValue.version() = TimerVersion(sessionId, sessionStartedAt, metadata)
private data class TimerSource(val sessionId: Long, val startedAt: Long,
    val sets: List<ActiveWorkoutSetEntry>, val metadata: RestTimerMetadata)

/** Keep taps responsive while persisting in order; roll back to the last successful write. */
internal class RestTimerWrites(
    private val scope: CoroutineScope,
    private val persist: suspend (RestTimerValue, RestTimerValue) -> Unit,
    private val publish: (RestTimerValue) -> Unit,
    private val onFailure: (Exception) -> Unit,
) {
    private val mutex = Mutex()
    private var revision = 0L
    private var pending = 0
    private var confirmed = RestTimerValue(null, 0)
    private var visible = confirmed
    private var epoch = 0L
    // Correlation lives for the current queue only; no cap silently drops rapid valid taps.
    private val ownVersions = mutableSetOf<TimerVersion>()
    private var source: TimerSource? = null

    fun supersede(value: RestTimerValue, force: Boolean = false): RestTimerValue {
        if (!force && (value.version() == confirmed.version() || value.version() in ownVersions)) return visible
        epoch++
        revision++
        confirmed = value
        visible = value
        ownVersions.clear()
        return value
    }

    /** Merge fresh non-timer fields without replacing a deliberate pending timer adjustment. */
    fun mergeSession(session: ActiveWorkoutSession): ActiveWorkoutSession {
        val value = session.timerValue()
        val incoming = TimerSource(session.sessionId, session.startedAt, session.loggedSets.toList(), value.metadata)
        val previous = source
        val replacement = previous == null || previous.sessionId != incoming.sessionId ||
            previous.startedAt != incoming.startedAt || previous.sets != incoming.sets
        val sameTimer = previous?.metadata == incoming.metadata
        val effective = if (!replacement && sameTimer) visible else supersede(value, force = replacement ||
            (value.version() != confirmed.version() && value.version() !in ownVersions))
        source = incoming
        val metadata = effective.metadata
        return session.copy(restTimerEndsAt = metadata.endsAt, restTimerTotalSeconds = metadata.seconds,
            restTimerGeneration = metadata.generation, restTimerElapsedDeadline = metadata.elapsedDeadline,
            restTimerBootId = metadata.bootId, restTimerHandled = metadata.handled)
    }

    fun submit(previous: RestTimerValue, next: RestTimerValue) {
        if (pending == 0) confirmed = previous
        ownVersions += previous.version()
        ownVersions += next.version()
        val token = ++revision
        val submittedEpoch = epoch
        pending++
        visible = next
        publish(next)
        scope.launchUserAction(onFailure) {
            try {
                mutex.withLock {
                    if (submittedEpoch == epoch) {
                        persist(confirmed, next)
                        if (submittedEpoch == epoch) confirmed = next
                    }
                }
            } finally {
                pending--
                if (token == revision) {
                    visible = confirmed
                    publish(confirmed)
                }
                if (pending == 0) ownVersions.clear()
            }
        }
    }
}

internal fun remainingRestSeconds(endsAt: Long?, now: Long): Int {
    val remaining = endsAt?.let { (it - now).coerceAtLeast(0L) } ?: return 0
    return (remaining / 1_000L + if (remaining % 1_000L > 0L) 1L else 0L)
        .coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
}

internal fun adjustedRestSeconds(endsAt: Long?, deltaSeconds: Int, now: Long): Int =
    (remainingRestSeconds(endsAt, now).toLong() + deltaSeconds)
        .coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()
