package com.trainiq.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.trainiq.domain.sleep.SleepRoutine

@Entity(tableName = "sleep_routine")
data class SleepRoutineEntity(
    @PrimaryKey val id: Int = 1,
    val enabled: Boolean,
    val minuteOfDay: Int,
    val nextAt: Long,
    val routineDay: String,
    val confirmedAt: Long,
    val completedDay: String,
)

fun SleepRoutineEntity.toDomain() = SleepRoutine(enabled, minuteOfDay, nextAt, routineDay, confirmedAt, completedDay)
fun SleepRoutine.toEntity() = SleepRoutineEntity(1, enabled, minuteOfDay, nextAt, routineDay, confirmedAt, completedDay)
