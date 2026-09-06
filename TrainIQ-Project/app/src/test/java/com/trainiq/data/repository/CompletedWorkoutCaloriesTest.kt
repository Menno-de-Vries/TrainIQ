package com.trainiq.data.repository

import com.trainiq.core.database.WorkoutSessionEntity
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class CompletedWorkoutCaloriesTest {
    @Test fun onlyCompletedSessionsInTheLocalDayContributeEnergy() {
        val zone = ZoneId.of("Europe/Amsterdam")
        val date = LocalDate.of(2026, 3, 29)
        val start = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val completed = WorkoutSessionEntity(id = 1L, date = start, duration = 60L, caloriesBurned = 120)
        val sessions = listOf(completed, completed.copy(id = 2L, date = end - 1L),
            completed.copy(id = 3L, completed = false), completed.copy(id = 4L, status = "DRAFT"),
            completed.copy(id = 5L, date = start - 1L), completed.copy(id = 6L, date = end))
        assertEquals(240, completedWorkoutCalories(sessions, date, zone))
        assertEquals(0, completedWorkoutCalories(emptyList(), date, zone))
    }
}
