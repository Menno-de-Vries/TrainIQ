package com.trainiq.core.database

import androidx.room.Database
import androidx.room.AutoMigration
import androidx.room.RoomDatabase

@Database(
    entities = [
        SleepRoutineEntity::class,
        HydrationEntity::class,
        RoomMirrorImportRunEntity::class,
        UserProfileEntity::class,
        SavedGoalAdviceEntity::class,
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
    version = 18,
    autoMigrations = [AutoMigration(from = 16, to = 17), AutoMigration(from = 17, to = 18)],
    exportSchema = true,
)
abstract class TrainIqDatabase : RoomDatabase() {
    abstract fun dao(): TrainIqDao
}
