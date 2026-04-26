package com.trainiq.data.local

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.trainiq.core.database.BodyMeasurementEntity
import com.trainiq.core.database.ExerciseEntity
import com.trainiq.core.database.PerformedExerciseEntity
import com.trainiq.core.database.RoutineSetEntity
import com.trainiq.core.database.UserProfileEntity
import com.trainiq.core.database.WorkoutDayEntity
import com.trainiq.core.database.WorkoutExerciseEntity
import com.trainiq.core.database.WorkoutRoutineEntity
import com.trainiq.core.database.WorkoutSessionEntity
import com.trainiq.core.database.WorkoutSetEntity
import com.trainiq.domain.model.SetType
import com.trainiq.domain.model.MealType
import com.trainiq.domain.model.WorkoutLogEventType
import com.trainiq.domain.model.WorkoutSyncStatus
import com.trainiq.domain.model.estimateStrengthTrainingCalories
import com.trainiq.domain.model.suggestMealType
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Singleton
class TrainIqLocalStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val gson = Gson()
    private val mutex = Mutex()
    private val storageFile = context.filesDir.resolve("trainiq-state.json")
    private val _state = MutableStateFlow(loadState())

    val state: StateFlow<TrainIqStorageState> = _state.asStateFlow()

    suspend fun update(transform: (TrainIqStorageState) -> TrainIqStorageState) {
        mutex.withLock {
            val updated = transform(_state.value)
            val tempFile = storageFile.resolveSibling("${storageFile.name}.tmp")
            tempFile.writeText(gson.toJson(updated))
            if (storageFile.exists()) {
                storageFile.delete()
            }
            tempFile.renameTo(storageFile)
            _state.value = updated
        }
    }

    suspend fun clearAll() {
        mutex.withLock {
            if (storageFile.exists()) {
                storageFile.delete()
            }
            _state.value = TrainIqStorageState()
        }
    }

    suspend fun clearProfile() {
        update { it.copy(profile = null) }
    }

    private fun loadState(): TrainIqStorageState {
        if (!storageFile.exists()) return TrainIqStorageState()
        return runCatching {
            val raw = storageFile.readText()
            val parsed = gson.fromJson(raw, TrainIqStorageState::class.java) ?: TrainIqStorageState()
            migrateRoutineSets(migrateProfileAndWorkoutDefaults(migrateLegacyMeals(parsed, raw)))
        }.getOrElse { TrainIqStorageState() }
    }

    private fun migrateLegacyMeals(state: TrainIqStorageState, raw: String): TrainIqStorageState {
        if (state.meals.isNotEmpty() || state.mealItems.isNotEmpty()) return state
        val root = runCatching { JsonParser.parseString(raw).asJsonObject }.getOrNull() ?: return state
        val legacyMeals = root.getAsJsonArray("meals") ?: return state
        if (legacyMeals.size() == 0) return state
        val migratedMeals = mutableListOf<LoggedMealStorage>()
        val migratedItems = mutableListOf<LoggedMealItemStorage>()
        legacyMeals.forEachIndexed { index, element ->
            val obj = element.asJsonObject
            val mealId = obj.get("id")?.asLong ?: (index + 1L)
            migratedMeals += LoggedMealStorage(
                id = mealId,
                timestamp = obj.get("date")?.asLong ?: 0L,
                mealType = suggestMealType(obj.get("date")?.asLong ?: 0L),
                name = "Imported meal",
            )
            migratedItems += LoggedMealItemStorage(
                id = index + 1L,
                mealId = mealId,
                calories = obj.get("calories")?.asDouble ?: 0.0,
                protein = obj.get("protein")?.asDouble ?: 0.0,
                carbs = obj.get("carbs")?.asDouble ?: 0.0,
                fat = obj.get("fat")?.asDouble ?: 0.0,
                name = "Imported nutrition",
                gramsUsed = 100.0,
            )
        }
        return state.copy(meals = migratedMeals, mealItems = migratedItems)
    }

    private fun migrateProfileAndWorkoutDefaults(state: TrainIqStorageState): TrainIqStorageState {
        val migratedProfile = state.profile?.copy(
            age = state.profile.age.takeIf { it > 0 } ?: 30,
            sex = state.profile.sex.takeIf { it.isNotBlank() } ?: "MALE",
        )
        val migratedSessions = state.sessions.map { session ->
            if (session.caloriesBurned > 0) {
                session
            } else {
                session.copy(caloriesBurned = estimateStrengthTrainingCalories(session.duration))
            }
        }
        val migratedMeals = state.meals.map { meal ->
            meal.copy(
                mealType = meal.mealType.takeIf { it in MealType.entries } ?: suggestMealType(meal.timestamp),
                name = meal.name.ifBlank { meal.mealType.label },
            )
        }
        return state.copy(
            profile = migratedProfile,
            sessions = migratedSessions,
            meals = migratedMeals,
        )
    }

    private fun migrateRoutineSets(state: TrainIqStorageState): TrainIqStorageState {
        if (state.workoutExercises.isEmpty()) return state
        val exercisesWithSets = state.routineSets.map { it.workoutExerciseId }.toSet()
        val missingExercises = state.workoutExercises.filterNot { it.id in exercisesWithSets }
        if (missingExercises.isEmpty()) return state
        var nextSetId = (state.routineSets.maxOfOrNull { it.id } ?: 0L) + 1L
        val migratedSets = missingExercises.flatMap { workoutExercise ->
            List(workoutExercise.targetSets.coerceAtLeast(0)) { index ->
                RoutineSetEntity(
                    id = nextSetId++,
                    workoutExerciseId = workoutExercise.id,
                    orderIndex = index,
                    setType = normalizeStoredSetType(workoutExercise.setType),
                    targetReps = parseRepTarget(workoutExercise.repRange),
                    targetWeightKg = workoutExercise.targetWeightKg.coerceAtLeast(0.0),
                    restSeconds = workoutExercise.restSeconds.coerceAtLeast(0),
                    targetRpe = workoutExercise.targetRpe.coerceIn(0.0, 10.0),
                )
            }
        }
        return state.copy(routineSets = state.routineSets + migratedSets)
    }
}

data class TrainIqStorageState(
    val profile: UserProfileEntity? = null,
    val routines: List<WorkoutRoutineEntity> = emptyList(),
    val days: List<WorkoutDayEntity> = emptyList(),
    val exercises: List<ExerciseEntity> = emptyList(),
    val workoutExercises: List<WorkoutExerciseEntity> = emptyList(),
    val routineSets: List<RoutineSetEntity> = emptyList(),
    val foods: List<FoodItemStorage> = emptyList(),
    val recipes: List<RecipeStorage> = emptyList(),
    val recipeIngredients: List<RecipeIngredientStorage> = emptyList(),
    val meals: List<LoggedMealStorage> = emptyList(),
    val mealItems: List<LoggedMealItemStorage> = emptyList(),
    val measurements: List<BodyMeasurementEntity> = emptyList(),
    val sessions: List<WorkoutSessionEntity> = emptyList(),
    val performedExercises: List<PerformedExerciseEntity> = emptyList(),
    val workoutSets: List<WorkoutSetEntity> = emptyList(),
    val activeWorkoutSession: ActiveWorkoutSessionStorage? = null,
    val workoutLogEvents: List<WorkoutLogEventStorage> = emptyList(),
)

data class WorkoutLogEventStorage(
    val id: Long = 0L,
    val dayId: Long = 0L,
    val sessionId: Long = 0L,
    val type: WorkoutLogEventType = WorkoutLogEventType.ADD_SET,
    val syncStatus: WorkoutSyncStatus = WorkoutSyncStatus.PENDING,
    val createdAt: Long = 0L,
    val undoExpiresAt: Long? = null,
    val targetEventId: Long? = null,
    val set: ActiveWorkoutSetStorage? = null,
    val previousLoggedSets: List<ActiveWorkoutSetStorage> = emptyList(),
)

data class ActiveWorkoutSessionStorage(
    val sessionId: Long = 0L,
    val dayId: Long = 0L,
    val routineId: Long? = null,
    val startedAt: Long = 0L,
    val updatedAt: Long = 0L,
    val loggedSets: List<ActiveWorkoutSetStorage> = emptyList(),
    val drafts: Map<Long, ActiveWorkoutDraftStorage> = emptyMap(),
    val collapsedExerciseIds: Set<Long> = emptySet(),
    val restTimerEndsAt: Long? = null,
    val restTimerTotalSeconds: Int = 0,
)

data class ActiveWorkoutSetStorage(
    val id: Long = 0L,
    val exerciseId: Long = 0L,
    val performedExerciseId: Long = 0L,
    val sourceWorkoutExerciseId: Long? = null,
    val weight: Double = 0.0,
    val reps: Int = 0,
    val rpe: Double = 0.0,
    val repsInReserve: Int? = null,
    val setType: SetType = SetType.NORMAL,
    val restSeconds: Int = 0,
    val orderIndex: Int = 0,
    val completed: Boolean = true,
    val loggedAt: Long = 0L,
)

data class ActiveWorkoutDraftStorage(
    val weight: String = "",
    val reps: String = "",
    val rpe: String = "",
    val setType: SetType = SetType.NORMAL,
)

private fun parseRepTarget(repRange: String): Int =
    repRange.substringAfter('-', repRange).trim().toIntOrNull()
        ?: repRange.filter(Char::isDigit).toIntOrNull()
        ?: 0

private fun normalizeStoredSetType(value: String?): String = when (value?.trim()?.uppercase()) {
    "WARMUP", "WARM_UP" -> "WARM_UP"
    "WORKING", "TOP_SET", "NORMAL" -> "NORMAL"
    "BACKOFF", "BACKOFF_SET", "BACK_OFF" -> "BACK_OFF"
    "DROP", "DROP_SET" -> "DROP_SET"
    "FAILURE" -> "FAILURE"
    else -> "NORMAL"
}
