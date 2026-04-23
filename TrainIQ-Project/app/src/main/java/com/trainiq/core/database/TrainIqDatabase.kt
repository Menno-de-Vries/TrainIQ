package com.trainiq.core.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        UserProfileEntity::class,
        WorkoutRoutineEntity::class,
        WorkoutDayEntity::class,
        ExerciseEntity::class,
        WorkoutExerciseEntity::class,
        WorkoutSessionEntity::class,
        WorkoutSetEntity::class,
        MealEntity::class,
        BodyMeasurementEntity::class,
    ],
    version = 4,
    exportSchema = false,
)
abstract class TrainIqDatabase : RoomDatabase() {
    abstract fun dao(): TrainIqDao
}
