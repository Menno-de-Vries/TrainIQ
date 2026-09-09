package com.trainiq.domain.sleep

import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

const val SleepCountdownMillis = 120_000L
const val SleepRepeatMillis = 10 * 60_000L

/** Only the current routine is retained; confirmation is not evidence of actual sleep. */
data class SleepRoutine(
    val enabled: Boolean = false,
    val minuteOfDay: Int = 22 * 60,
    val nextAt: Long = 0,
    val routineDay: String = "",
    val confirmedAt: Long = 0,
    val completedDay: String = "",
) {
    val active: Boolean get() = enabled && routineDay.isNotEmpty()
    fun configure(enabled: Boolean, minute: Int, now: Long, zone: ZoneId): SleepRoutine {
        require(minute in 0..1439)
        if (this.enabled == enabled && minuteOfDay == minute && (!enabled || nextAt > 0)) return this
        return copy(enabled = enabled, minuteOfDay = minute, routineDay = "", confirmedAt = 0,
            nextAt = if (enabled) nextOccurrence(minute, now, zone, completedDay) else 0)
    }

    fun rebaseTime(now: Long, zone: ZoneId): SleepRoutine = when {
        !enabled || active -> this
        nextAt in 1..now -> advance(now, zone)
        else -> copy(nextAt = nextOccurrence(minuteOfDay, now, zone, completedDay))
    }

    fun confirm(now: Long): SleepRoutine =
        if (active && confirmedAt == 0L) copy(confirmedAt = now, nextAt = now + SleepCountdownMillis) else this

    fun advance(now: Long, zone: ZoneId): SleepRoutine {
        if (!enabled) return this
        if (active) {
            if (confirmedAt > 0 && now >= confirmedAt + SleepCountdownMillis) {
                return copy(routineDay = "", confirmedAt = 0, completedDay = routineDay,
                    nextAt = nextOccurrence(minuteOfDay, now, zone, routineDay))
            }
            return this
        }
        if (nextAt > 0 && now >= nextAt) {
            return copy(routineDay = Instant.ofEpochMilli(nextAt).atZone(zone).toLocalDate().toString())
        }
        return this
    }
}

private fun nextOccurrence(minute: Int, now: Long, zone: ZoneId, completedDay: String): Long {
    var day = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
    val time = LocalTime.of(minute / 60, minute % 60)
    while (true) {
        val candidate = day.atTime(time).atZone(zone).toInstant().toEpochMilli()
        if (candidate > now && day.toString() != completedDay) return candidate
        day = day.plusDays(1)
    }
}
