package com.trainiq.data.local

import com.trainiq.core.database.BodyMeasurementEntity
import com.trainiq.core.database.ExerciseEntity
import com.trainiq.core.database.MealEntity
import com.trainiq.core.database.UserProfileEntity
import com.trainiq.core.database.WorkoutDayEntity
import com.trainiq.core.database.WorkoutExerciseEntity
import com.trainiq.core.database.WorkoutRoutineEntity
import com.trainiq.core.database.WorkoutSessionEntity
import com.trainiq.core.database.WorkoutSetEntity
import com.trainiq.core.util.todayEpochMillis

object SampleData {
    private val today = todayEpochMillis()
    private const val day = 86_400_000L

    val user = UserProfileEntity(1, "Alex Carter", 180.0, 81.5, 16.2, "High", "Lean bulk")

    val exercises = listOf(
        ExerciseEntity(1, "Bench Press", "Chest", "Barbell"),
        ExerciseEntity(2, "Incline Dumbbell Press", "Chest", "Dumbbell"),
        ExerciseEntity(3, "Lat Pulldown", "Back", "Cable"),
        ExerciseEntity(4, "Barbell Squat", "Legs", "Barbell"),
        ExerciseEntity(5, "Romanian Deadlift", "Hamstrings", "Barbell"),
        ExerciseEntity(6, "Overhead Press", "Shoulders", "Barbell"),
        ExerciseEntity(7, "Cable Fly", "Chest", "Cable"),
        ExerciseEntity(8, "Walking Lunge", "Legs", "Dumbbell"),
    )

    val routines = listOf(
        WorkoutRoutineEntity(1, "Push Pull Legs", "Balanced hypertrophy split", true),
        WorkoutRoutineEntity(2, "Upper Lower", "Strength-focused 4-day split", false),
    )

    val days = listOf(
        WorkoutDayEntity(1, 1, "Push", 0),
        WorkoutDayEntity(2, 1, "Pull", 1),
        WorkoutDayEntity(3, 1, "Legs", 2),
        WorkoutDayEntity(4, 2, "Upper", 0),
        WorkoutDayEntity(5, 2, "Lower", 1),
    )

    val workoutExercises = listOf(
        WorkoutExerciseEntity(1, 1, 1, 4, "6-8", 120),
        WorkoutExerciseEntity(2, 1, 2, 3, "8-10", 90),
        WorkoutExerciseEntity(3, 1, 6, 3, "6-8", 120),
        WorkoutExerciseEntity(4, 1, 7, 2, "12-15", 60),
        WorkoutExerciseEntity(5, 2, 3, 4, "8-10", 90),
        WorkoutExerciseEntity(6, 3, 4, 4, "5-8", 150),
        WorkoutExerciseEntity(7, 3, 5, 3, "8-10", 120),
        WorkoutExerciseEntity(8, 3, 8, 3, "10-12", 75),
    )

    val meals = listOf(
        MealEntity(date = today, calories = 620, protein = 42, carbs = 55, fat = 18),
        MealEntity(date = today, calories = 810, protein = 53, carbs = 91, fat = 24),
        MealEntity(date = today - day, calories = 730, protein = 47, carbs = 80, fat = 20),
    )

    val measurements = listOf(
        BodyMeasurementEntity(date = today - day * 21, weight = 82.6, bodyFat = 17.3, muscleMass = 38.1),
        BodyMeasurementEntity(date = today - day * 14, weight = 82.1, bodyFat = 16.9, muscleMass = 38.3),
        BodyMeasurementEntity(date = today - day * 7, weight = 81.8, bodyFat = 16.5, muscleMass = 38.5),
        BodyMeasurementEntity(date = today, weight = 81.5, bodyFat = 16.2, muscleMass = 38.8),
    )

    val sessions = listOf(
        WorkoutSessionEntity(id = 1, date = today - day * 7, duration = 4200),
        WorkoutSessionEntity(id = 2, date = today - day * 3, duration = 4500),
    )

    val sets = listOf(
        WorkoutSetEntity(id = 1, sessionId = 1, exerciseId = 1, weight = 80.0, reps = 8, rpe = 8.0),
        WorkoutSetEntity(id = 2, sessionId = 1, exerciseId = 1, weight = 82.5, reps = 7, rpe = 8.5),
        WorkoutSetEntity(id = 3, sessionId = 1, exerciseId = 2, weight = 30.0, reps = 10, rpe = 8.0),
        WorkoutSetEntity(id = 4, sessionId = 2, exerciseId = 4, weight = 110.0, reps = 6, rpe = 8.0),
        WorkoutSetEntity(id = 5, sessionId = 2, exerciseId = 5, weight = 95.0, reps = 8, rpe = 8.5),
    )
}
