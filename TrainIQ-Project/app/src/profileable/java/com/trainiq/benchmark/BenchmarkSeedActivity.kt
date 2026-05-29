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
                        exercises = listOf(
                            ExerciseEntity(id = ExerciseId, name = "Bench press", muscleGroup = "Chest", equipment = "Barbell"),
                        ),
                        workoutExercises = listOf(
                            WorkoutExerciseEntity(
                                id = WorkoutExerciseId,
                                dayId = DayId,
                                exerciseId = ExerciseId,
                                targetSets = 3,
                                repRange = "6-8",
                                restSeconds = 120,
                                targetWeightKg = 80.0,
                                targetRpe = 8.0,
                                orderIndex = 0,
                            ),
                        ),
                        sets = listOf(
                            RoutineSetEntity(
                                id = RoutineSetId,
                                workoutExerciseId = WorkoutExerciseId,
                                orderIndex = 0,
                                setType = "WORKING",
                                targetReps = 8,
                                targetWeightKg = 80.0,
                                restSeconds = 120,
                                targetRpe = 8.0,
                            ),
                        ),
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
                        drafts = listOf(
                            ActiveWorkoutDraftEntity(
                                sessionId = SessionId,
                                exerciseId = ExerciseId,
                                weight = "80",
                                reps = "8",
                                rpe = "8",
                                setType = "WORKING",
                            ),
                        ),
                        performedExercises = listOf(
                            PerformedExerciseEntity(
                                id = PerformedExerciseId,
                                sessionId = SessionId,
                                exerciseId = ExerciseId,
                                sourceWorkoutExerciseId = WorkoutExerciseId,
                                orderIndex = 0,
                            ),
                        ),
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
        const val ExerciseId = 9_003L
        const val WorkoutExerciseId = 9_004L
        const val RoutineSetId = 9_005L
        const val SessionId = 9_006L
        const val PerformedExerciseId = 9_007L
    }
}
