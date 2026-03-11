package com.trainiq.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val height: Double,
    val weight: Double,
    val bodyFat: Double,
    val activityLevel: String,
    val goal: String,
)

@Entity(tableName = "workout_routines")
data class WorkoutRoutineEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val description: String,
    val active: Boolean,
)

@Entity(tableName = "workout_days")
data class WorkoutDayEntity(
    @PrimaryKey val id: Long,
    val routineId: Long,
    val name: String,
    val orderIndex: Int,
)

@Entity(tableName = "exercises")
data class ExerciseEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val muscleGroup: String,
    val equipment: String,
)

@Entity(tableName = "workout_exercises")
data class WorkoutExerciseEntity(
    @PrimaryKey val id: Long,
    val dayId: Long,
    val exerciseId: Long,
    val targetSets: Int,
    val repRange: String,
    val restSeconds: Int,
)

@Entity(tableName = "workout_sessions")
data class WorkoutSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long,
    val duration: Long,
)

@Entity(tableName = "workout_sets")
data class WorkoutSetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val exerciseId: Long,
    val weight: Double,
    val reps: Int,
    val rpe: Double,
)

@Entity(tableName = "meals")
data class MealEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long,
    val calories: Int,
    val protein: Int,
    val carbs: Int,
    val fat: Int,
)

@Entity(tableName = "body_measurements")
data class BodyMeasurementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long,
    val weight: Double,
    val bodyFat: Double,
    val muscleMass: Double,
)
