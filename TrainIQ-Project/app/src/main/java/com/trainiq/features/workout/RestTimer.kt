package com.trainiq.features.workout

import com.trainiq.core.ui.launchUserAction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal data class RestTimerValue(val endsAt: Long?, val seconds: Int)

/** Keep taps responsive while persisting in order; roll back to the last successful write. */
internal class RestTimerWrites(
    private val scope: CoroutineScope,
    private val persist: suspend (RestTimerValue) -> Unit,
    private val publish: (RestTimerValue) -> Unit,
    private val onFailure: (Exception) -> Unit,
) {
    private val mutex = Mutex()
    private var revision = 0L
    private var pending = 0
    private var confirmed = RestTimerValue(null, 0)

    fun submit(previous: RestTimerValue, next: RestTimerValue) {
        if (pending == 0) confirmed = previous
        val token = ++revision
        pending++
        publish(next)
        scope.launchUserAction(onFailure) {
            try {
                mutex.withLock {
                    persist(next)
                    confirmed = next
                }
            } finally {
                pending--
                if (token == revision) publish(confirmed)
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
