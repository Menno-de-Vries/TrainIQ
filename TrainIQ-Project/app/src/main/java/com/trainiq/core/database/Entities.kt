package com.trainiq.core.database

import androidx.room.Entity
import androidx.room.ColumnInfo
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val age: Int = 30,
    val sex: String = "MALE",
    val height: Double,
    val weight: Double,
    val bodyFat: Double,
    val activityLevel: String,
    val goal: String,
    val calorieTarget: Int,
    val proteinTarget: Int,
    val carbsTarget: Int,
    val fatTarget: Int,
    val trainingFocus: String,
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
    @ColumnInfo(name = "target_weight_kg", defaultValue = "0.0") val targetWeightKg: Double = 0.0,
    @ColumnInfo(name = "target_rpe", defaultValue = "0.0") val targetRpe: Double = 0.0,
    @ColumnInfo(name = "set_type", defaultValue = "WORKING") val setType: String = "WORKING",
    @ColumnInfo(name = "superset_group_id") val supersetGroupId: Long? = null,
    @ColumnInfo(name = "order_index", defaultValue = "0") val orderIndex: Int = 0,
)

@Entity(
    tableName = "routine_sets",
    indices = [Index(value = ["workoutExerciseId", "order_index"])],
)
data class RoutineSetEntity(
    @PrimaryKey val id: Long,
    val workoutExerciseId: Long,
    @ColumnInfo(name = "order_index", defaultValue = "0") val orderIndex: Int,
    @ColumnInfo(name = "set_type", defaultValue = "NORMAL") val setType: String = "NORMAL",
    @ColumnInfo(name = "target_reps", defaultValue = "0") val targetReps: Int = 0,
    @ColumnInfo(name = "target_weight_kg", defaultValue = "0.0") val targetWeightKg: Double = 0.0,
    @ColumnInfo(name = "rest_seconds", defaultValue = "0") val restSeconds: Int = 0,
    @ColumnInfo(name = "target_rpe", defaultValue = "0.0") val targetRpe: Double = 0.0,
    @ColumnInfo(name = "target_rir") val targetRir: Int? = null,
)

@Entity(
    tableName = "workout_sessions",
    indices = [
        Index(value = ["routine_id"]),
        Index(value = ["workout_day_id"]),
        Index(value = ["status", "date"]),
    ],
)
data class WorkoutSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long,
    val duration: Long,
    val caloriesBurned: Int = 0,
    @ColumnInfo(name = "routine_id") val routineId: Long? = null,
    @ColumnInfo(name = "workout_day_id") val workoutDayId: Long? = null,
    @ColumnInfo(name = "started_at", defaultValue = "0") val startedAt: Long = 0L,
    @ColumnInfo(name = "ended_at", defaultValue = "0") val endedAt: Long = 0L,
    @ColumnInfo(name = "status", defaultValue = "COMPLETED") val status: String = "COMPLETED",
    @ColumnInfo(name = "completed", defaultValue = "1") val completed: Boolean = true,
    @ColumnInfo(name = "debrief_summary", defaultValue = "") val debriefSummary: String = "",
    @ColumnInfo(name = "debrief_progression_feedback", defaultValue = "") val debriefProgressionFeedback: String = "",
    @ColumnInfo(name = "debrief_recommendation", defaultValue = "") val debriefRecommendation: String = "",
    @ColumnInfo(name = "debrief_next_session_focus", defaultValue = "") val debriefNextSessionFocus: String = "",
    @ColumnInfo(name = "debrief_recovery_score", defaultValue = "75") val debriefRecoveryScore: Int = 75,
    @ColumnInfo(name = "debrief_intensity_signal", defaultValue = "MAINTAIN") val debriefIntensitySignal: String = "MAINTAIN",
    @ColumnInfo(name = "debrief_wins", defaultValue = "") val debriefWins: String = "",
    @ColumnInfo(name = "debrief_risks", defaultValue = "") val debriefRisks: String = "",
    @ColumnInfo(name = "debrief_next_load_target", defaultValue = "") val debriefNextLoadTarget: String = "",
    @ColumnInfo(name = "debrief_recovery_advice", defaultValue = "") val debriefRecoveryAdvice: String = "",
    @ColumnInfo(name = "debrief_source", defaultValue = "LOCAL_FALLBACK") val debriefSource: String = "LOCAL_FALLBACK",
)

@Entity(
    tableName = "performed_exercises",
    indices = [
        Index(value = ["session_id", "order_index"]),
        Index(value = ["exercise_id"]),
        Index(value = ["source_workout_exercise_id"]),
    ],
)
data class PerformedExerciseEntity(
    @PrimaryKey val id: Long,
    @ColumnInfo(name = "session_id") val sessionId: Long,
    @ColumnInfo(name = "exercise_id") val exerciseId: Long,
    @ColumnInfo(name = "source_workout_exercise_id") val sourceWorkoutExerciseId: Long? = null,
    @ColumnInfo(name = "order_index", defaultValue = "0") val orderIndex: Int = 0,
)

@Entity(
    tableName = "workout_sets",
    indices = [
        Index(value = ["sessionId", "order_index"]),
        Index(value = ["exerciseId"]),
        Index(value = ["performed_exercise_id", "order_index"]),
    ],
)
data class WorkoutSetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val exerciseId: Long,
    val weight: Double,
    val reps: Int,
    val rpe: Double,
    val repsInReserve: Int? = null,
    @ColumnInfo(name = "performed_exercise_id", defaultValue = "0") val performedExerciseId: Long = 0L,
    @ColumnInfo(name = "set_type", defaultValue = "WORKING") val setType: String = "WORKING",
    @ColumnInfo(name = "rest_seconds", defaultValue = "0") val restSeconds: Int = 0,
    @ColumnInfo(name = "order_index", defaultValue = "0") val orderIndex: Int = 0,
    @ColumnInfo(name = "completed", defaultValue = "1") val completed: Boolean = true,
    @ColumnInfo(name = "logged_at", defaultValue = "0") val loggedAt: Long = 0L,
    @ColumnInfo(name = "completed_at", defaultValue = "0") val completedAt: Long = 0L,
)

@Entity(tableName = "meals")
data class MealEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long,
    val mealType: String = "LUNCH",
    val name: String = "",
    val notes: String? = null,
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
