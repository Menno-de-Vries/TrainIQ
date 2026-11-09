package com.trainiq.domain.workout

import org.junit.Assert.*
import org.junit.Test

class WorkoutRestTimerPolicyTest {
    private val now = WorkoutRestTimerNow(1_000_000, 10_000, "boot-a")
    private val timer = ScheduledWorkoutRestTimer(WorkoutRestTimerIdentity(7, "generation-a"), 8,
        1_060_000, 70_000, "boot-a", 60)

    @Test fun monotoneDeadlineKeepsPartialSecondsDespiteWallClockChanges() {
        assertEquals(1, timer.remainingSeconds(now.copy(wallMillis = 9_000_000, elapsedMillis = 69_999)))
        assertEquals(0, timer.remainingSeconds(now.copy(elapsedMillis = 70_000)))
        assertEquals(60, timer.remainingSeconds(now.copy(wallMillis = -9_000_000)))
        assertEquals(0, timer.remainingSeconds(now.copy(bootId = "boot-b")))
    }

    @Test fun completionDistinguishesEarlyCurrentStaleHandledAndRebootedTimers() {
        assertEquals(RestTimerCompletion.EARLY, timer.completion(now))
        assertEquals(RestTimerCompletion.COMPLETED, timer.completion(now.copy(elapsedMillis = 70_000)))
        assertEquals(RestTimerCompletion.COMPLETED, timer.completion(now.copy(elapsedMillis = 100_000)))
        assertEquals(RestTimerCompletion.EXPIRED, timer.completion(now.copy(elapsedMillis = 100_001)))
        assertEquals(RestTimerCompletion.EXPIRED, timer.completion(now.copy(bootId = "boot-b")))
        assertEquals(RestTimerCompletion.HANDLED, timer.copy(handled = true).completion(now))
    }

    @Test fun adjustmentUsesRemainingMonotoneDurationAndCreatesANewGeneration() {
        val adjusted = timer.adjusted(30, now.copy(wallMillis = 5_000_000, elapsedMillis = 20_001), "generation-b")
        assertEquals(80, adjusted!!.totalSeconds)
        assertEquals(100_001L, adjusted.elapsedDeadline)
        assertEquals(5_080_000L, adjusted.wallDeadline)
        assertEquals("generation-b", adjusted.identity.generation)
        assertNull(timer.adjusted(-60, now, "cancel"))
    }
}
