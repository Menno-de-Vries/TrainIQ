package com.trainiq.data.repository

import com.trainiq.core.database.WorkoutSessionEntity
import com.trainiq.core.database.WorkoutSetEntity
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class CurrentWeekTrainingVolumeTest {
    private val zone = ZoneId.of("Europe/Amsterdam")
    private val today = LocalDate.of(2026, 1, 1)
    private fun session(id: Long, day: String) = WorkoutSessionEntity(
        id = id, date = LocalDate.parse(day).atStartOfDay(zone).toInstant().toEpochMilli(), duration = 60,
    )
    private fun set(sessionId: Long) = WorkoutSetEntity(
        sessionId = sessionId, exerciseId = 1, weight = 100.0, reps = 5, rpe = 8.0, setType = "NORMAL",
    )

    @Test fun currentCalendarWeekCrossesYearButExcludesOldAndFutureDays() {
        val sessions = listOf(session(1, "2025-12-28"), session(2, "2025-12-29"),
            session(3, "2026-01-01"), session(4, "2026-01-02"), session(5, "2026-01-05"))
        assertEquals(1000.0, currentWeekTrainingVolume(sessions, sessions.map { set(it.id) }, today, zone), 0.0)
    }

    @Test fun historicalVolumeDoesNotBecomeThisWeeksVolume() {
        assertEquals(0.0, currentWeekTrainingVolume(listOf(session(1, "2025-12-28")), listOf(set(1)), today, zone), 0.0)
    }

    @Test fun onlyCompletedProgressionWorkCounts() {
        val sessions = listOf(session(1, "2025-12-29"), session(2, "2025-12-29").copy(completed = false),
            session(3, "2025-12-29").copy(status = "ABANDONED"))
        val sets = listOf(set(1), set(1).copy(setType = "BACK_OFF"), set(1).copy(setType = "WARM_UP"),
            set(1).copy(completed = false), set(1).copy(reps = 0), set(2), set(3))
        assertEquals(1000.0, currentWeekTrainingVolume(sessions, sets, today, zone), 0.0)
    }
}
