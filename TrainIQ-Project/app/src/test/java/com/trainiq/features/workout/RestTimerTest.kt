package com.trainiq.features.workout

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

class RestTimerTest {
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test fun queuedWritesStayOrderedAndFailuresRestoreOnlyConfirmedDeadlines() = runTest {
        val original = RestTimerValue(60_000, 60)
        val first = RestTimerValue(75_000, 75)
        val second = RestTimerValue(90_000, 90)
        var displayed = original
        var failFirst = true
        var gate = CompletableDeferred<Unit>()
        val attempts = mutableListOf<RestTimerValue>()
        var errors = 0
        val writes = RestTimerWrites(backgroundScope, persist = {
            attempts += it
            gate.await()
            if (failFirst || it == second) error("storage unavailable")
        }, publish = { displayed = it }, onFailure = { errors++ })
        writes.submit(displayed, first)
        writes.submit(displayed, second)
        assertEquals(second, displayed)
        runCurrent()
        assertEquals(listOf(first), attempts)
        gate.complete(Unit)
        runCurrent()
        assertEquals(listOf(first, second), attempts)
        assertEquals(original, displayed)
        assertEquals(2, errors)

        failFirst = false
        gate = CompletableDeferred()
        writes.submit(displayed, first)
        writes.submit(displayed, second)
        runCurrent()
        gate.complete(Unit)
        runCurrent()
        assertEquals(first, displayed)
        assertEquals(3, errors)
        writes.submit(displayed, original)
        runCurrent()
        assertEquals(original, displayed)
    }

    @Test fun timerKeepsTheLastPartialSecondUntilItsDeadline() {
        assertEquals(1, remainingRestSeconds(10_000L, 9_001L))
        assertEquals(1, remainingRestSeconds(10_000L, 9_999L))
        assertEquals(0, remainingRestSeconds(10_000L, 10_000L))
        assertEquals(0, remainingRestSeconds(null, 10_000L))
        assertEquals(0, remainingRestSeconds(10_000L, 11_000L))
        assertEquals(60, activeWorkoutClockUiState(0L, 60_000L, 60, 1L).restTimerSeconds)
    }

    @Test fun repeatedAdjustmentsUseEachNewDeadlineAndElapsedTime() {
        val now = 1_000L
        val first = adjustedRestSeconds(61_000L, 15, now)
        val second = adjustedRestSeconds(now + first * 1_000L, 15, now)
        assertEquals(75, first)
        assertEquals(90, second)
        assertEquals(80, adjustedRestSeconds(now + second * 1_000L, 0, now + 10_000L))
        assertEquals(0, adjustedRestSeconds(5_000L, -15, now))
        assertEquals(Int.MAX_VALUE, adjustedRestSeconds(61_000L, Int.MAX_VALUE, now))
    }
}
