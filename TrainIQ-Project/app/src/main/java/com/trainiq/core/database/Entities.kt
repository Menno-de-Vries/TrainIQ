package com.trainiq.core.database

import androidx.room.Entity
import androidx.room.ColumnInfo
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "room_mirror_import_runs",
    indices = [
        Index(value = ["status", "finished_at"]),
        Index(value = ["source_fingerprint"]),
    ],
)
data class RoomMirrorImportRunEntity(
    @PrimaryKey @ColumnInfo(name = "generation_id") val generationId: String,
    @ColumnInfo(name = "source_fingerprint") val sourceFingerprint: String,
    @ColumnInfo(name = "started_at") val startedAt: Long,
    @ColumnInfo(name = "finished_at") val finishedAt: Long,
    val status: String,
    @ColumnInfo(name = "schema_version") val schemaVersion: Int,
    @ColumnInfo(name = "expected_row_count") val expectedRowCount: Int,
    @ColumnInfo(name = "imported_row_count") val importedRowCount: Int,
    @ColumnInfo(name = "stale_row_count") val staleRowCount: Int,
    @ColumnInfo(name = "mismatch_count") val mismatchCount: Int,
    @ColumnInfo(name = "json_authoritative") val jsonAuthoritative: Boolean = true,
    @ColumnInfo(name = "room_authoritative") val roomAuthoritative: Boolean = false,
    @ColumnInfo(name = "error_type") val errorType: String? = null,
)

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

@Entity(
    tableName = "workout_days",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutRoutineEntity::class,
            parentColumns = ["id"],
            childColumns = ["routineId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["routineId"])],
)
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

@Entity(
    tableName = "workout_exercises",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutDayEntity::class,
            parentColumns = ["id"],
            childColumns = ["dayId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index(value = ["dayId"]),
        Index(value = ["exerciseId"]),
    ],
)
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
    foreignKeys = [
        ForeignKey(
            entity = WorkoutExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["workoutExerciseId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
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
    foreignKeys = [
        ForeignKey(
            entity = WorkoutRoutineEntity::class,
            parentColumns = ["id"],
            childColumns = ["routine_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
        ForeignKey(
            entity = WorkoutDayEntity::class,
            parentColumns = ["id"],
            childColumns = ["workout_day_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
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
    foreignKeys = [
        ForeignKey(
            entity = WorkoutSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["session_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exercise_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = WorkoutExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["source_workout_exercise_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
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
    foreignKeys = [
        ForeignKey(
            entity = WorkoutSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
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

@Entity(tableName = "food_items")
data class FoodItemEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val barcode: String? = null,
    @ColumnInfo(name = "calories_per_100g") val caloriesPer100g: Double,
    @ColumnInfo(name = "protein_per_100g") val proteinPer100g: Double,
    @ColumnInfo(name = "carbs_per_100g") val carbsPer100g: Double,
    @ColumnInfo(name = "fat_per_100g") val fatPer100g: Double,
    @ColumnInfo(name = "source_type") val sourceType: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)

@Entity(tableName = "recipes")
data class RecipeEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val notes: String? = null,
    @ColumnInfo(name = "total_cooked_grams") val totalCookedGrams: Double? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)

@Entity(
    tableName = "recipe_ingredients",
    foreignKeys = [
        ForeignKey(
            entity = RecipeEntity::class,
            parentColumns = ["id"],
            childColumns = ["recipe_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = FoodItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["food_item_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index(value = ["recipe_id", "order_index"]),
        Index(value = ["food_item_id"]),
    ],
)
data class RecipeIngredientEntity(
    @PrimaryKey val id: Long,
    @ColumnInfo(name = "recipe_id") val recipeId: Long,
    @ColumnInfo(name = "food_item_id") val foodItemId: Long,
    @ColumnInfo(name = "grams_used") val gramsUsed: Double,
    @ColumnInfo(name = "order_index") val orderIndex: Int = 0,
)

@Entity(
    tableName = "meal_items",
    foreignKeys = [
        ForeignKey(
            entity = MealEntity::class,
            parentColumns = ["id"],
            childColumns = ["meal_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["meal_id", "order_index"]),
        Index(value = ["reference_id"]),
    ],
)
data class MealItemEntity(
    @PrimaryKey val id: Long,
    @ColumnInfo(name = "meal_id") val mealId: Long,
    @ColumnInfo(name = "item_type") val itemType: String,
    @ColumnInfo(name = "reference_id") val referenceId: Long,
    val name: String,
    @ColumnInfo(name = "grams_used") val gramsUsed: Double,
    @ColumnInfo(name = "serving_count", defaultValue = "1") val servingCount: Int = 1,
    val calories: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val notes: String? = null,
    @ColumnInfo(name = "order_index") val orderIndex: Int = 0,
)

@Entity(
    tableName = "active_workout_sessions",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutDayEntity::class,
            parentColumns = ["id"],
            childColumns = ["dayId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = WorkoutRoutineEntity::class,
            parentColumns = ["id"],
            childColumns = ["routineId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index(value = ["dayId"]),
        Index(value = ["routineId"]),
    ],
)
data class ActiveWorkoutSessionEntity(
    @PrimaryKey val sessionId: Long,
    val dayId: Long,
    val routineId: Long? = null,
    val startedAt: Long,
    val updatedAt: Long,
    val restTimerEndsAt: Long? = null,
    val restTimerTotalSeconds: Int = 0,
)

@Entity(
    tableName = "active_workout_drafts",
    foreignKeys = [
        ForeignKey(
            entity = ActiveWorkoutSessionEntity::class,
            parentColumns = ["sessionId"],
            childColumns = ["session_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    primaryKeys = ["session_id", "exercise_id"],
    indices = [
        Index(value = ["session_id"]),
        Index(value = ["exercise_id"]),
    ],
)
data class ActiveWorkoutDraftEntity(
    @ColumnInfo(name = "session_id") val sessionId: Long,
    @ColumnInfo(name = "exercise_id") val exerciseId: Long,
    val weight: String,
    val reps: String,
    val rpe: String,
    @ColumnInfo(name = "set_type") val setType: String,
)

@Entity(
    tableName = "active_workout_collapsed_exercises",
    foreignKeys = [
        ForeignKey(
            entity = ActiveWorkoutSessionEntity::class,
            parentColumns = ["sessionId"],
            childColumns = ["session_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    primaryKeys = ["session_id", "exercise_id"],
    indices = [
        Index(value = ["session_id"]),
        Index(value = ["exercise_id"]),
    ],
)
data class ActiveWorkoutCollapsedExerciseEntity(
    @ColumnInfo(name = "session_id") val sessionId: Long,
    @ColumnInfo(name = "exercise_id") val exerciseId: Long,
)

@Entity(
    tableName = "active_workout_sets",
    foreignKeys = [
        ForeignKey(
            entity = ActiveWorkoutSessionEntity::class,
            parentColumns = ["sessionId"],
            childColumns = ["session_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exercise_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    primaryKeys = ["session_id", "id"],
    indices = [
        Index(value = ["session_id", "order_index"]),
        Index(value = ["exercise_id"]),
    ],
)
data class ActiveWorkoutSetEntity(
    @ColumnInfo(name = "session_id") val sessionId: Long,
    val id: Long,
    @ColumnInfo(name = "exercise_id") val exerciseId: Long,
    @ColumnInfo(name = "performed_exercise_id") val performedExerciseId: Long,
    @ColumnInfo(name = "source_workout_exercise_id") val sourceWorkoutExerciseId: Long? = null,
    val weight: Double,
    val reps: Int,
    val rpe: Double,
    @ColumnInfo(name = "reps_in_reserve") val repsInReserve: Int? = null,
    @ColumnInfo(name = "set_type") val setType: String,
    @ColumnInfo(name = "rest_seconds") val restSeconds: Int,
    @ColumnInfo(name = "order_index") val orderIndex: Int,
    val completed: Boolean,
    @ColumnInfo(name = "logged_at") val loggedAt: Long,
)

@Entity(
    tableName = "workout_log_events",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutDayEntity::class,
            parentColumns = ["id"],
            childColumns = ["day_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["day_id", "created_at"]),
        Index(value = ["session_id", "created_at"]),
        Index(value = ["sync_status"]),
    ],
)
data class WorkoutLogEventEntity(
    @PrimaryKey val id: Long,
    @ColumnInfo(name = "day_id") val dayId: Long,
    @ColumnInfo(name = "session_id") val sessionId: Long,
    val type: String,
    @ColumnInfo(name = "sync_status") val syncStatus: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "undo_expires_at") val undoExpiresAt: Long? = null,
    @ColumnInfo(name = "target_event_id") val targetEventId: Long? = null,
)

@Entity(
    tableName = "workout_log_event_sets",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutLogEventEntity::class,
            parentColumns = ["id"],
            childColumns = ["event_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exercise_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    primaryKeys = ["event_id", "snapshot_role", "snapshot_index"],
    indices = [
        Index(value = ["event_id"]),
        Index(value = ["exercise_id"]),
    ],
)
data class WorkoutLogEventSetEntity(
    @ColumnInfo(name = "event_id") val eventId: Long,
    @ColumnInfo(name = "snapshot_role") val snapshotRole: String,
    @ColumnInfo(name = "snapshot_index") val snapshotIndex: Int,
    val id: Long,
    @ColumnInfo(name = "exercise_id") val exerciseId: Long,
    @ColumnInfo(name = "performed_exercise_id") val performedExerciseId: Long,
    @ColumnInfo(name = "source_workout_exercise_id") val sourceWorkoutExerciseId: Long? = null,
    val weight: Double,
    val reps: Int,
    val rpe: Double,
    @ColumnInfo(name = "reps_in_reserve") val repsInReserve: Int? = null,
    @ColumnInfo(name = "set_type") val setType: String,
    @ColumnInfo(name = "rest_seconds") val restSeconds: Int,
    @ColumnInfo(name = "order_index") val orderIndex: Int,
    val completed: Boolean,
    @ColumnInfo(name = "logged_at") val loggedAt: Long,
)

@Entity(tableName = "body_measurements")
data class BodyMeasurementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long,
    val weight: Double,
    val bodyFat: Double,
    val muscleMass: Double,
)
