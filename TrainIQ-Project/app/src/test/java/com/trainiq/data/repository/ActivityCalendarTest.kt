package com.trainiq.data.repository

import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.*
import org.junit.Test

class ActivityCalendarTest {
    private val zone = ZoneId.of("Europe/Amsterdam")
    private fun millis(date: LocalDate) = date.atTime(12, 0).atZone(zone).toInstant().toEpochMilli()

    @Test fun sevenCalendarDaysRemainSevenAcrossBothClockChanges() {
        listOf(LocalDate.of(2026, 3, 31), LocalDate.of(2026, 10, 27)).forEach { today ->
            val days = (0L..6L).map { millis(today.minusDays(it)) }
            assertEquals(100, activityAdherence(days + days, today, zone))
            assertEquals(7, activityStreak(days, today, zone))
            assertEquals(85, activityAdherence(days.dropLast(1), today, zone))
        }
    }

    @Test fun futureAndOldActivityDoNotFillGapsOrStartTodaysStreak() {
        val today = LocalDate.of(2026, 3, 31)
        val dates = listOf(today.plusDays(1), today, today.minusDays(1), today.minusDays(3), today.minusDays(7))
        assertEquals(42, activityAdherence(dates.map(::millis), today, zone))
        assertEquals(2, activityStreak(dates.map(::millis), today, zone))
        assertEquals(0, activityStreak(listOf(millis(today.minusDays(1))), today, zone))
        assertEquals(0, activityAdherence(emptyList(), today, zone))
    }

    @Test fun dailyMealsUseLocalMidnightIncludingShortAndLongDays() {
        listOf(LocalDate.of(2026, 3, 29), LocalDate.of(2026, 10, 25)).forEach { day ->
            val start = day.atStartOfDay(zone).toInstant().toEpochMilli()
            val end = day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
            assertFalse((start - 1).isOnActivityDate(day, zone))
            assertTrue(start.isOnActivityDate(day, zone))
            assertTrue((end - 1).isOnActivityDate(day, zone))
            assertFalse(end.isOnActivityDate(day, zone))
            assertFalse(millis(day.plusDays(10)).isOnActivityDate(day, zone))
        }
    }
}
