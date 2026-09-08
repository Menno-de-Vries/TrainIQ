package com.trainiq.features.sleep

import com.trainiq.domain.sleep.SleepRoutine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SleepRoutineStatusTest {
    @Test fun confirmationBetweenClockTicksNeverShowsMoreThanTwoMinutes() {
        val state = SleepRoutine(enabled = true, routineDay = "2026-09-08", confirmedAt = 10_900)
        assertEquals("Bevestigd: nog 2:00 om te gaan slapen.", sleepRoutineStatus(state, 10_000))
    }

    @Test fun countdownEndsAtThePersistedDeadline() {
        val state = SleepRoutine(enabled = true, routineDay = "2026-09-08", confirmedAt = 10_900)
        assertEquals("Bevestigd: nog 0:01 om te gaan slapen.", sleepRoutineStatus(state, 130_899))
        assertTrue(sleepRoutineStatus(state, 130_900).startsWith("Slaaproutine afgehandeld."))
    }
}
