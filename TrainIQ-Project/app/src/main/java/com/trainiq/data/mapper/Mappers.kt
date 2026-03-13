package com.trainiq.data.mapper

import com.trainiq.core.database.BodyMeasurementEntity
import com.trainiq.core.database.ExerciseEntity
import com.trainiq.core.database.UserProfileEntity
import com.trainiq.core.database.WorkoutDayEntity
import com.trainiq.core.database.WorkoutExerciseEntity
import com.trainiq.core.database.WorkoutRoutineEntity
import com.trainiq.core.database.WorkoutSessionEntity
import com.trainiq.domain.model.BodyMeasurement
import com.trainiq.domain.model.Exercise
import com.trainiq.domain.model.UserProfile
import com.trainiq.domain.model.WorkoutDay
import com.trainiq.domain.model.WorkoutExercisePlan
import com.trainiq.domain.model.WorkoutRoutine
import com.trainiq.domain.model.WorkoutSessionSummary

fun UserProfileEntity.toDomain() = UserProfile(
    id = id,
    name = name,
    height = height,
    weight = weight,
    bodyFat = bodyFat,
    activityLevel = activityLevel,
    goal = goal,
    calorieTarget = calorieTarget,
    proteinTarget = proteinTarget,
    carbsTarget = carbsTarget,
    fatTarget = fatTarget,
    trainingFocus = trainingFocus,
)

fun ExerciseEntity.toDomain() = Exercise(id, name, muscleGroup, equipment)

fun BodyMeasurementEntity.toDomain() = BodyMeasurement(id, date, weight, bodyFat, muscleMass)

fun WorkoutSessionEntity.toDomain(totalVolume: Double) = WorkoutSessionSummary(id, date, duration, totalVolume)

fun WorkoutDayEntity.toDomain(exercises: List<WorkoutExercisePlan>) = WorkoutDay(id, routineId, name, orderIndex, exercises)

fun WorkoutRoutineEntity.toDomain(days: List<WorkoutDay>) = WorkoutRoutine(id, name, description, active, days)

fun WorkoutExerciseEntity.toDomain(exercise: Exercise) =
    WorkoutExercisePlan(id, exercise, targetSets, repRange, restSeconds)
