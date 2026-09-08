package com.trainiq.domain.sleep

import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.*
import org.junit.Test

class SleepRoutinePolicyTest {
    private val zone = ZoneId.of("Europe/Amsterdam")
    private fun time(value: String) = Instant.parse(value).toEpochMilli()
    private val now = time("2026-09-08T19:00:00Z")

    @Test fun disabledRoutineNeverTriggers() {
        assertFalse(SleepRoutine().advance(now, zone).active)
    }

    @Test fun scheduleTriggersOnlyWhenDueAndRepeatsUntilConfirmed() {
        val planned = SleepRoutine().configure(true, 22 * 60, now, zone)
        assertEquals(time("2026-09-08T20:00:00Z"), planned.nextAt)
        assertFalse(planned.advance(planned.nextAt - 1, zone).active)
        val active = planned.advance(planned.nextAt, zone)
        assertTrue(active.active)
        assertEquals("2026-09-08", active.routineDay)
        assertTrue(active.advance(active.nextAt + 86_400_000, zone).active)
    }

    @Test fun explicitConfirmationIsIdempotentAndFinishesAfterTwoMinutes() {
        val active = SleepRoutine().configure(true, 22 * 60, now, zone).advance(now + 3_600_000, zone)
        val confirmed = active.confirm(now + 3_600_000)
        assertEquals(confirmed, confirmed.confirm(now + 3_610_000))
        assertTrue(confirmed.advance(confirmed.confirmedAt + 119_999, zone).active)
        val completed = confirmed.advance(confirmed.confirmedAt + 120_000, zone)
        assertFalse(completed.active)
        assertTrue(completed.nextAt > confirmed.confirmedAt + 120_000)
        assertEquals("2026-09-08", completed.completedDay)
    }

    @Test fun disableAndTimeChangeCancelActiveRoutine() {
        val active = SleepRoutine().configure(true, 22 * 60, now, zone).advance(now + 3_600_000, zone)
        assertFalse(active.configure(false, 1320, now, zone).active)
        val changed = active.configure(true, 1380, now, zone)
        assertFalse(changed.active)
        assertEquals(time("2026-09-08T21:00:00Z"), changed.nextAt)
    }

    @Test fun daylightSavingGapAndOverlapHaveOneFutureOccurrence() {
        val gap = SleepRoutine().configure(true, 150, time("2026-03-28T23:00:00Z"), zone)
        assertEquals(time("2026-03-29T01:30:00Z"), gap.nextAt)
        val overlap = SleepRoutine().configure(true, 150, time("2026-10-25T00:45:00Z"), zone)
        assertEquals(time("2026-10-26T01:30:00Z"), overlap.nextAt)
    }

    @Test fun midnightConfirmationDoesNotRetriggerSameDay() {
        val active = SleepRoutine().configure(true, 1320, now, zone).advance(now + 3_600_000, zone)
        val confirmed = active.confirm(time("2026-09-08T22:00:00Z"))
        val done = confirmed.advance(confirmed.confirmedAt + 120_000, zone)
        assertEquals(time("2026-09-09T20:00:00Z"), done.nextAt)
    }
}
