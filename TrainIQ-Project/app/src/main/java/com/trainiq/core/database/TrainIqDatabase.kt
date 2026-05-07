package com.trainiq.core.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        RoomMirrorImportRunEntity::class,
        UserProfileEntity::class,
        WorkoutRoutineEntity::class,
        WorkoutDayEntity::class,
        ExerciseEntity::class,
        WorkoutExerciseEntity::class,
        RoutineSetEntity::class,
        WorkoutSessionEntity::class,
        PerformedExerciseEntity::class,
        WorkoutSetEntity::class,
        MealEntity::class,
        FoodItemEntity::class,
        RecipeEntity::class,
        RecipeIngredientEntity::class,
        MealItemEntity::class,
        ActiveWorkoutSessionEntity::class,
        ActiveWorkoutDraftEntity::class,
        ActiveWorkoutCollapsedExerciseEntity::class,
        ActiveWorkoutSetEntity::class,
        WorkoutLogEventEntity::class,
        WorkoutLogEventSetEntity::class,
        BodyMeasurementEntity::class,
    ],
    version = 11,
    exportSchema = true,
)
abstract class TrainIqDatabase : RoomDatabase() {
    abstract fun dao(): TrainIqDao
}
