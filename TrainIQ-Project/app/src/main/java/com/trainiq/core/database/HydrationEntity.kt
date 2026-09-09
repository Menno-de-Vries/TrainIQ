package com.trainiq.core.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Manual entries have no food record and use a caller-stable key for retry safety. */
@Entity(tableName = "hydration_entries", indices = [Index(value = ["timestamp"])])
data class HydrationEntity(
    @PrimaryKey val id: String,
    val timestamp: Long,
    val volumeMl: Double,
)
