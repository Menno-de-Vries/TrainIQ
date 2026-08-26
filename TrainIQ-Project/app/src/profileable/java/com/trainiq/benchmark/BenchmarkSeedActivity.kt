package com.trainiq.benchmark

import android.app.Activity
import android.os.Bundle
import androidx.room.Room
import androidx.room.withTransaction
import com.trainiq.core.database.ActiveWorkoutDraftEntity
import com.trainiq.core.database.ActiveWorkoutSessionEntity
import com.trainiq.core.database.ExerciseEntity
import com.trainiq.core.database.PerformedExerciseEntity
import com.trainiq.core.database.RoutineSetEntity
import com.trainiq.core.database.TrainIqDatabase
import com.trainiq.core.database.TrainIqMigrations
import com.trainiq.core.database.UserProfileEntity
import com.trainiq.core.database.WorkoutDayEntity
import com.trainiq.core.database.WorkoutExerciseEntity
import com.trainiq.core.database.WorkoutRoutineEntity
import com.trainiq.core.database.WorkoutSessionEntity
import com.trainiq.core.datastore.OnboardingPreferences
import com.trainiq.core.datastore.UserPreferencesRepository
import kotlinx.coroutines.runBlocking

class BenchmarkSeedActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Thread {
            seedActiveWorkout()
            runOnUiThread { finishAndRemoveTask() }
        }.start()
    }

    private fun seedActiveWorkout() {
        runBlocking {
            UserPreferencesRepository(applicationContext).saveOnboardingPreferences(
                OnboardingPreferences(
                    completed = true,
                    goal = "strength",
                    experience = "advanced",
                    trainingDays = 4,
                    equipment = "barbell",
                    privacyAcknowledged = true,
                    guidedTourCompleted = true,
                    guidedTourSkipped = false,
                ),
            )
        }

        val database = Room.databaseBuilder(
            applicationContext,
            TrainIqDatabase::class.java,
            "trainiq.db",
        )
            .addMigrations(*TrainIqMigrations.All)
            .build()

        try {
            runBlocking {
                database.withTransaction {
                    val dao = database.dao()
                    val now = System.currentTimeMillis()

                    dao.clearMirrorTables()
                    dao.upsertUserProfile(
                        UserProfileEntity(
                            id = 1L,
                            name = "Benchmark",
                            age = 35,
                            sex = "MALE",
                            height = 180.0,
                            weight = 82.0,
                            bodyFat = 15.0,
                            activityLevel = "MODERATE",
                            goal = "STRENGTH",
                            calorieTarget = 2700,
                            proteinTarget = 180,
                            carbsTarget = 280,
                            fatTarget = 80,
                            trainingFocus = "POWERLIFTING",
                        ),
                    )
                    val exercises = (0 until BenchmarkExerciseCount).map { index ->
                        ExerciseEntity(
                            id = ExerciseIdBase + index,
                            name = "Benchmark lift ${index + 1}",
                            muscleGroup = if (index % 2 == 0) "Chest" else "Back",
                            equipment = "Barbell",
                        )
                    }
                    val workoutExercises = exercises.mapIndexed { index, exercise ->
                        WorkoutExerciseEntity(
                            id = WorkoutExerciseIdBase + index,
                            dayId = DayId,
                            exerciseId = exercise.id,
                            targetSets = 4,
                            repRange = "6-8",
                            restSeconds = 120,
                            targetWeightKg = 80.0 + index,
                            targetRpe = 8.0,
                            orderIndex = index,
                        )
                    }
                    val routineSets = workoutExercises.flatMapIndexed { exerciseIndex, workoutExercise ->
                        (0 until BenchmarkSetsPerExercise).map { setIndex ->
                            RoutineSetEntity(
                                id = RoutineSetIdBase + exerciseIndex * BenchmarkSetsPerExercise + setIndex,
                                workoutExerciseId = workoutExercise.id,
                                orderIndex = setIndex,
                                setType = "WORKING",
                                targetReps = 8,
                                targetWeightKg = 80.0 + exerciseIndex,
                                restSeconds = 120,
                                targetRpe = 8.0,
                            )
                        }
                    }
                    dao.insertGeneratedRoutineGraph(
                        routine = WorkoutRoutineEntity(
                            id = RoutineId,
                            name = "Benchmark routine",
                            description = "Profileable active-workout benchmark seed",
                            active = true,
                        ),
                        days = listOf(
                            WorkoutDayEntity(id = DayId, routineId = RoutineId, name = "Benchmark day", orderIndex = 0),
                        ),
                        exercises = exercises,
                        workoutExercises = workoutExercises,
                        sets = routineSets,
                    )
                    dao.startOrResumeActiveWorkoutSession(
                        activeSession = ActiveWorkoutSessionEntity(
                            sessionId = SessionId,
                            dayId = DayId,
                            routineId = RoutineId,
                            startedAt = now,
                            updatedAt = now,
                        ),
                        draftSession = WorkoutSessionEntity(
                            id = SessionId,
                            date = now,
                            duration = 0L,
                            routineId = RoutineId,
                            workoutDayId = DayId,
                            startedAt = now,
                            status = "DRAFT",
                            completed = false,
                        ),
                        drafts = workoutExercises.mapIndexed { index, workoutExercise ->
                            ActiveWorkoutDraftEntity(
                                sessionId = SessionId,
                                exerciseId = workoutExercise.id,
                                weight = "${80 + index}",
                                reps = "8",
                                rpe = "8",
                                setType = "WORKING",
                            )
                        },
                        performedExercises = workoutExercises.mapIndexed { index, workoutExercise ->
                            PerformedExerciseEntity(
                                id = PerformedExerciseIdBase + index,
                                sessionId = SessionId,
                                exerciseId = workoutExercise.exerciseId,
                                sourceWorkoutExerciseId = workoutExercise.id,
                                orderIndex = index,
                            )
                        },
                    )
                }
            }
        } finally {
            database.close()
        }
    }

    private companion object {
        const val RoutineId = 9_001L
        const val DayId = 9_002L
        const val ExerciseIdBase = 9_100L
        const val WorkoutExerciseIdBase = 9_200L
        const val RoutineSetIdBase = 9_300L
        const val SessionId = 9_006L
        const val PerformedExerciseIdBase = 9_400L
        const val BenchmarkExerciseCount = 10
        const val BenchmarkSetsPerExercise = 4
    }
}
