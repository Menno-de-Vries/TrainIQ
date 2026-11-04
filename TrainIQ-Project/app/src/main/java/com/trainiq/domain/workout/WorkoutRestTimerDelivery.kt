package com.trainiq.domain.workout

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

interface WorkoutRestTimerClock { fun now(): WorkoutRestTimerNow }
interface WorkoutRestTimerStore {
    suspend fun read(): ScheduledWorkoutRestTimer?
    suspend fun claim(identity: WorkoutRestTimerIdentity, now: WorkoutRestTimerNow): RestTimerCompletion
}
interface WorkoutRestTimerDelivery {
    fun schedule(timer: ScheduledWorkoutRestTimer)
    fun cancel(identity: WorkoutRestTimerIdentity)
    suspend fun show(timer: ScheduledWorkoutRestTimer, silent: Boolean): Boolean
}

data class WorkoutRestTimerCue(val identity: WorkoutRestTimerIdentity, val silent: Boolean, val notificationPosted: Boolean)

/** Serializes persistence and external effects; the database separately fences stale generations. */
class WorkoutRestTimerCoordinator(
    private val store: WorkoutRestTimerStore,
    private val delivery: WorkoutRestTimerDelivery,
    private val clock: WorkoutRestTimerClock,
) {
    private val mutex = Mutex()
    private val _cues = MutableSharedFlow<WorkoutRestTimerCue>(extraBufferCapacity = 1)
    val cues = _cues.asSharedFlow()

    suspend fun <T> mutate(action: suspend () -> T): T = mutex.withLock {
        val before = store.read()
        val result = action() // Never cancel a committed timer when persistence fails.
        val after = store.read()
        if (before?.identity != after?.identity) before?.let { safely { delivery.cancel(it.identity) } }
        reconcileLocked(after)
        result
    }

    suspend fun reconcile() = mutex.withLock { reconcileLocked(store.read()) }

    suspend fun completeIfCurrent(identity: WorkoutRestTimerIdentity): RestTimerCompletion = mutex.withLock {
        val timer = store.read()?.takeIf { it.identity == identity } ?: return@withLock RestTimerCompletion.OBSOLETE
        completeLocked(timer)
    }

    suspend fun isCurrent(identity: WorkoutRestTimerIdentity): Boolean = mutex.withLock {
        store.read()?.identity == identity
    }

    suspend fun current(): ScheduledWorkoutRestTimer? = mutex.withLock { store.read() }

    private suspend fun reconcileLocked(timer: ScheduledWorkoutRestTimer?) {
        timer ?: return
        when (timer.completion(clock.now())) {
            RestTimerCompletion.EARLY -> safely { delivery.schedule(timer) }
            RestTimerCompletion.HANDLED -> Unit
            else -> completeLocked(timer)
        }
    }

    private suspend fun completeLocked(timer: ScheduledWorkoutRestTimer): RestTimerCompletion {
        val now = clock.now()
        val result = store.claim(timer.identity, now)
        if (result != RestTimerCompletion.COMPLETED && result != RestTimerCompletion.EXPIRED) return result
        safely { delivery.cancel(timer.identity) }
        // Unknown/rebooted clocks cannot justify either a fresh cue or an imported alarm.
        if (timer.bootId != now.bootId || timer.bootId.isBlank()) return result
        val silent = result == RestTimerCompletion.EXPIRED || timer.completion(clock.now()) == RestTimerCompletion.EXPIRED
        var posted = false
        safely { posted = delivery.show(timer, silent) }
        _cues.tryEmit(WorkoutRestTimerCue(timer.identity, silent, posted))
        return result
    }

    private suspend fun safely(action: suspend () -> Unit) {
        try { action() } catch (error: CancellationException) { throw error } catch (_: Exception) {
            // Persistence remains authoritative. A subsequent foreground/exact-access reconcile
            // retries future planning; a claimed completion is never replayed after a crash.
        }
    }
}
