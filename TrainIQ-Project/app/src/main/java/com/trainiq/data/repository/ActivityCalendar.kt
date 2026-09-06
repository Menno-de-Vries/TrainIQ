package com.trainiq.data.repository

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import com.trainiq.core.database.WorkoutSessionEntity

internal fun completedWorkoutCalories(
    sessions: List<WorkoutSessionEntity>,
    date: LocalDate = LocalDate.now(),
    zone: ZoneId = ZoneId.systemDefault(),
): Int = sessions.filter { it.completed && it.status == "COMPLETED" && it.date.isOnActivityDate(date, zone) }
    .sumOf { it.caloriesBurned }

internal fun Long.isOnActivityDate(
    date: LocalDate = LocalDate.now(),
    zone: ZoneId = ZoneId.systemDefault(),
): Boolean = Instant.ofEpochMilli(this).atZone(zone).toLocalDate() == date

internal fun activityAdherence(
    timestamps: List<Long>,
    today: LocalDate = LocalDate.now(),
    zone: ZoneId = ZoneId.systemDefault(),
): Int {
    val dates = timestamps.map { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }.toSet()
    return ((0L..6L).count { today.minusDays(it) in dates } / 7.0 * 100).toInt()
}

internal fun activityStreak(
    timestamps: List<Long>,
    today: LocalDate = LocalDate.now(),
    zone: ZoneId = ZoneId.systemDefault(),
): Int {
    val dates = timestamps.map { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }.toSet()
    var day = today
    var streak = 0
    while (day in dates) {
        streak++
        day = day.minusDays(1)
    }
    return streak
}
