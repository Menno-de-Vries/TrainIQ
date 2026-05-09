package com.trainiq.data.repository

import com.google.gson.Gson
import com.trainiq.core.database.ActiveWorkoutCollapsedExerciseEntity
import com.trainiq.core.database.ActiveWorkoutDraftEntity
import com.trainiq.core.database.ActiveWorkoutSessionEntity
import com.trainiq.core.database.ActiveWorkoutSetEntity
import com.trainiq.core.database.BodyMeasurementEntity
import com.trainiq.core.database.ExerciseEntity
import com.trainiq.core.database.FoodItemEntity
import com.trainiq.core.database.MealEntity
import com.trainiq.core.database.MealItemEntity
import com.trainiq.core.database.PerformedExerciseEntity
import com.trainiq.core.database.RecipeEntity
import com.trainiq.core.database.RecipeIngredientEntity
import com.trainiq.core.database.RoutineSetEntity
import com.trainiq.core.database.TrainIqDatabase
import com.trainiq.core.database.UserProfileEntity
import com.trainiq.core.database.WorkoutDayEntity
import com.trainiq.core.database.WorkoutExerciseEntity
import com.trainiq.core.database.WorkoutLogEventEntity
import com.trainiq.core.database.WorkoutLogEventSetEntity
import com.trainiq.core.database.WorkoutRoutineEntity
import com.trainiq.core.database.WorkoutSessionEntity
import com.trainiq.core.database.WorkoutSetEntity
import com.trainiq.data.local.ActiveWorkoutDraftStorage
import com.trainiq.data.local.ActiveWorkoutSessionStorage
import com.trainiq.data.local.ActiveWorkoutSetStorage
import com.trainiq.data.local.FoodItemStorage
import com.trainiq.data.local.LoggedMealItemStorage
import com.trainiq.data.local.LoggedMealStorage
import com.trainiq.data.local.RecipeIngredientStorage
import com.trainiq.data.local.RecipeStorage
import com.trainiq.data.local.TrainIqLocalStore
import com.trainiq.data.local.TrainIqStorageState
import com.trainiq.data.local.WorkoutLogEventStorage
import com.trainiq.data.migration.JsonRoomImportPlanner
import com.trainiq.data.migration.RoomJsonImportSink
import com.trainiq.data.migration.RoomMirrorImportRun
import com.trainiq.domain.model.FoodSourceType
import com.trainiq.domain.model.LoggedMealItemType
import com.trainiq.domain.model.MealType
import com.trainiq.domain.model.SetType
import com.trainiq.domain.model.WorkoutLogEventType
import com.trainiq.domain.model.WorkoutSyncStatus
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Singleton
class RoomTrainIqRuntimeStore @Inject constructor(
    private val database: TrainIqDatabase,
    private val legacyStore: TrainIqLocalStore,
) {
    private val dao = database.dao()
    private val gson = Gson()
    private val planner = JsonRoomImportPlanner(gson)
    private val sink = RoomJsonImportSink(database)
    private val mutex = Mutex()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        scope.launch {
            seedRoomFromLegacyJsonIfNeeded()
        }
    }

    val state: StateFlow<TrainIqStorageState> = combine(
        combine(
            dao.observeUserProfile(),
            dao.observeRoutines(),
            dao.observeWorkoutDays(),
            dao.observeExercises(),
            dao.observeWorkoutExercises(),
        ) { profile, routines, days, exercises, workoutExercises ->
            CorePlanTables(profile, routines, days, exercises, workoutExercises)
        },
        combine(
            dao.observeRoutineSets(),
            dao.observeWorkoutSessions(),
            dao.observePerformedExercises(),
            dao.observeWorkoutSets(),
            dao.observeMeasurements(),
        ) { routineSets, sessions, performedExercises, workoutSets, measurements ->
            WorkoutHistoryTables(routineSets, sessions, performedExercises, workoutSets, measurements)
        },
        combine(
            dao.observeFoodItems(),
            dao.observeRecipes(),
            dao.observeRecipeIngredients(),
            dao.observeMeals(),
            dao.observeMealItems(),
        ) { foodItems, recipes, recipeIngredients, meals, mealItems ->
            NutritionTables(foodItems, recipes, recipeIngredients, meals, mealItems)
        },
        combine(
            dao.observeActiveWorkoutSessions(),
            dao.observeActiveWorkoutDrafts(),
            dao.observeActiveWorkoutCollapsedExercises(),
            dao.observeActiveWorkoutSets(),
            dao.observeWorkoutLogEvents(),
        ) { sessions, drafts, collapsed, sets, events ->
            ActiveWorkoutTables(sessions, drafts, collapsed, sets, events)
        },
        dao.observeWorkoutLogEventSets(),
    ) { core, history, nutrition, active, eventSets ->
        TrainIqStorageState(
            profile = core.profile,
            routines = core.routines,
            days = core.days,
            exercises = core.exercises,
            workoutExercises = core.workoutExercises,
            routineSets = history.routineSets,
            foods = nutrition.foodItems.map { it.toStorage() },
            recipes = nutrition.recipes.map { it.toStorage() },
            recipeIngredients = nutrition.recipeIngredients.map { it.toStorage() },
            meals = nutrition.meals.map { it.toStorage() },
            mealItems = nutrition.mealItems.map { it.toStorage() },
            measurements = history.measurements,
            sessions = history.sessions,
            performedExercises = history.performedExercises,
            workoutSets = history.workoutSets,
            activeWorkoutSession = active.toStorage(),
            workoutLogEvents = active.events.map { it.toStorage(eventSets.filter { set -> set.eventId == it.id }) },
        )
    }.stateIn(scope, SharingStarted.Eagerly, TrainIqStorageState())

    suspend fun update(transform: (TrainIqStorageState) -> TrainIqStorageState) {
        mutex.withLock {
            val updated = transform(state.value)
            val updatedJson = gson.toJson(updated)
            val startedAt = System.currentTimeMillis()
            val plan = planner.plan(updatedJson)
            sink.importTransaction(
                plan = plan,
                mirrorRun = RoomMirrorImportRun(
                    generationId = "runtime-$startedAt",
                    sourceFingerprint = updatedJson.sha256(),
                    startedAt = startedAt,
                    finishedAt = System.currentTimeMillis(),
                ),
            )
        }
    }

    suspend fun clearAll() {
        mutex.withLock {
            database.dao().clearMirrorTables()
        }
    }

    suspend fun clearProfile() {
        update { it.copy(profile = null) }
    }

    suspend fun updateActiveWorkoutRestTimer(
        sessionId: Long,
        endsAt: Long?,
        totalSeconds: Int,
        updatedAt: Long,
    ) {
        mutex.withLock {
            dao.updateActiveWorkoutRestTimer(
                sessionId = sessionId,
                endsAt = endsAt,
                totalSeconds = totalSeconds,
                updatedAt = updatedAt,
            )
        }
    }

    suspend fun updateActiveWorkoutDraft(
        sessionId: Long,
        exerciseId: Long,
        draft: ActiveWorkoutDraftStorage,
        updatedAt: Long,
    ) {
        mutex.withLock {
            dao.updateActiveWorkoutDraft(
                sessionId = sessionId,
                draft = ActiveWorkoutDraftEntity(
                    sessionId = sessionId,
                    exerciseId = exerciseId,
                    weight = draft.weight,
                    reps = draft.reps,
                    rpe = draft.rpe,
                    setType = draft.setType.name,
                ),
                updatedAt = updatedAt,
            )
        }
    }

    suspend fun logActiveWorkoutSet(
        active: ActiveWorkoutSessionStorage,
        set: ActiveWorkoutSetStorage,
        draft: ActiveWorkoutDraftStorage,
        event: WorkoutLogEventStorage,
    ) {
        mutex.withLock {
            dao.logActiveWorkoutSet(
                session = ActiveWorkoutSessionEntity(
                    sessionId = active.sessionId,
                    dayId = active.dayId,
                    routineId = active.routineId,
                    startedAt = active.startedAt,
                    updatedAt = active.updatedAt,
                    restTimerEndsAt = active.restTimerEndsAt,
                    restTimerTotalSeconds = active.restTimerTotalSeconds,
                ),
                draft = ActiveWorkoutDraftEntity(
                    sessionId = active.sessionId,
                    exerciseId = set.activeKey,
                    weight = draft.weight,
                    reps = draft.reps,
                    rpe = draft.rpe,
                    setType = draft.setType.name,
                ),
                set = set.toActiveWorkoutSetEntity(sessionId = active.sessionId),
                event = event.toWorkoutLogEventEntity(),
                eventSets = event.toWorkoutLogEventSetEntities(),
                activeKey = set.activeKey,
            )
        }
    }

    suspend fun discardActiveWorkoutSession(sessionId: Long) {
        mutex.withLock {
            dao.discardActiveWorkoutSession(sessionId)
        }
    }

    private suspend fun seedRoomFromLegacyJsonIfNeeded() {
        mutex.withLock {
            if (dao.mirrorRowCount() > 0) return
            val legacyState = legacyStore.exportLegacyState()
            if (legacyState == TrainIqStorageState()) return
            sink.importTransaction(planner.plan(gson.toJson(legacyState)))
        }
    }
}

private data class CorePlanTables(
    val profile: UserProfileEntity?,
    val routines: List<WorkoutRoutineEntity>,
    val days: List<WorkoutDayEntity>,
    val exercises: List<ExerciseEntity>,
    val workoutExercises: List<WorkoutExerciseEntity>,
)

private data class WorkoutHistoryTables(
    val routineSets: List<RoutineSetEntity>,
    val sessions: List<WorkoutSessionEntity>,
    val performedExercises: List<PerformedExerciseEntity>,
    val workoutSets: List<WorkoutSetEntity>,
    val measurements: List<BodyMeasurementEntity>,
)

private data class NutritionTables(
    val foodItems: List<FoodItemEntity>,
    val recipes: List<RecipeEntity>,
    val recipeIngredients: List<RecipeIngredientEntity>,
    val meals: List<MealEntity>,
    val mealItems: List<MealItemEntity>,
)

private data class ActiveWorkoutTables(
    val sessions: List<ActiveWorkoutSessionEntity>,
    val drafts: List<ActiveWorkoutDraftEntity>,
    val collapsed: List<ActiveWorkoutCollapsedExerciseEntity>,
    val sets: List<ActiveWorkoutSetEntity>,
    val events: List<WorkoutLogEventEntity>,
) {
    fun toStorage(): ActiveWorkoutSessionStorage? {
        val session = sessions.firstOrNull() ?: return null
        return ActiveWorkoutSessionStorage(
            sessionId = session.sessionId,
            dayId = session.dayId,
            routineId = session.routineId,
            startedAt = session.startedAt,
            updatedAt = session.updatedAt,
            loggedSets = sets.filter { it.sessionId == session.sessionId }.map { it.toStorage() },
            drafts = drafts.filter { it.sessionId == session.sessionId }.associate { draft ->
                draft.exerciseId to ActiveWorkoutDraftStorage(
                    weight = draft.weight,
                    reps = draft.reps,
                    rpe = draft.rpe,
                    setType = draft.setType.toEnum(SetType.NORMAL),
                )
            },
            collapsedExerciseIds = collapsed.filter { it.sessionId == session.sessionId }.map { it.exerciseId }.toSet(),
            restTimerEndsAt = session.restTimerEndsAt,
            restTimerTotalSeconds = session.restTimerTotalSeconds,
        )
    }
}

private fun FoodItemEntity.toStorage() = FoodItemStorage(
    id = id,
    name = name,
    barcode = barcode,
    caloriesPer100g = caloriesPer100g,
    proteinPer100g = proteinPer100g,
    carbsPer100g = carbsPer100g,
    fatPer100g = fatPer100g,
    sourceType = sourceType.toEnum(FoodSourceType.MANUAL),
    createdAt = createdAt,
    updatedAt = updatedAt,
)

private fun RecipeEntity.toStorage() = RecipeStorage(
    id = id,
    name = name,
    notes = notes,
    totalCookedGrams = totalCookedGrams,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

private fun RecipeIngredientEntity.toStorage() = RecipeIngredientStorage(
    id = id,
    recipeId = recipeId,
    foodItemId = foodItemId,
    gramsUsed = gramsUsed,
)

private fun MealEntity.toStorage() = LoggedMealStorage(
    id = id,
    timestamp = date,
    mealType = mealType.toEnum(MealType.LUNCH),
    name = name,
    notes = notes,
)

private fun MealItemEntity.toStorage() = LoggedMealItemStorage(
    id = id,
    mealId = mealId,
    itemType = itemType.toEnum(LoggedMealItemType.FOOD),
    referenceId = referenceId,
    name = name,
    gramsUsed = gramsUsed,
    calories = calories,
    protein = protein,
    carbs = carbs,
    fat = fat,
    notes = notes,
)

private fun ActiveWorkoutSetEntity.toStorage() = ActiveWorkoutSetStorage(
    id = id,
    exerciseId = exerciseId,
    performedExerciseId = performedExerciseId,
    sourceWorkoutExerciseId = sourceWorkoutExerciseId,
    weight = weight,
    reps = reps,
    rpe = rpe,
    repsInReserve = repsInReserve,
    setType = setType.toEnum(SetType.NORMAL),
    restSeconds = restSeconds,
    orderIndex = orderIndex,
    completed = completed,
    loggedAt = loggedAt,
)

private fun ActiveWorkoutSetStorage.toActiveWorkoutSetEntity(sessionId: Long) = ActiveWorkoutSetEntity(
    sessionId = sessionId,
    id = id,
    exerciseId = exerciseId,
    performedExerciseId = performedExerciseId,
    sourceWorkoutExerciseId = sourceWorkoutExerciseId,
    weight = weight,
    reps = reps,
    rpe = rpe,
    repsInReserve = repsInReserve,
    setType = setType.name,
    restSeconds = restSeconds,
    orderIndex = orderIndex,
    completed = completed,
    loggedAt = loggedAt,
)

private fun WorkoutLogEventStorage.toWorkoutLogEventEntity() = WorkoutLogEventEntity(
    id = id,
    dayId = dayId,
    sessionId = sessionId,
    type = type.name,
    syncStatus = syncStatus.name,
    createdAt = createdAt,
    undoExpiresAt = undoExpiresAt,
    targetEventId = targetEventId,
)

private fun WorkoutLogEventStorage.toWorkoutLogEventSetEntities(): List<WorkoutLogEventSetEntity> = buildList {
    set?.let { add(it.toWorkoutLogEventSetEntity(eventId = id, snapshotRole = "CURRENT", snapshotIndex = 0)) }
    previousLoggedSets.forEachIndexed { index, previous ->
        add(previous.toWorkoutLogEventSetEntity(eventId = id, snapshotRole = "PREVIOUS", snapshotIndex = index))
    }
}

private fun ActiveWorkoutSetStorage.toWorkoutLogEventSetEntity(
    eventId: Long,
    snapshotRole: String,
    snapshotIndex: Int,
) = WorkoutLogEventSetEntity(
    eventId = eventId,
    snapshotRole = snapshotRole,
    snapshotIndex = snapshotIndex,
    id = id,
    exerciseId = exerciseId,
    performedExerciseId = performedExerciseId,
    sourceWorkoutExerciseId = sourceWorkoutExerciseId,
    weight = weight,
    reps = reps,
    rpe = rpe,
    repsInReserve = repsInReserve,
    setType = setType.name,
    restSeconds = restSeconds,
    orderIndex = orderIndex,
    completed = completed,
    loggedAt = loggedAt,
)

private fun WorkoutLogEventEntity.toStorage(sets: List<WorkoutLogEventSetEntity>) = WorkoutLogEventStorage(
    id = id,
    dayId = dayId,
    sessionId = sessionId,
    type = type.toEnum(WorkoutLogEventType.ADD_SET),
    syncStatus = syncStatus.toEnum(WorkoutSyncStatus.PENDING),
    createdAt = createdAt,
    undoExpiresAt = undoExpiresAt,
    targetEventId = targetEventId,
    set = sets.firstOrNull { it.snapshotRole == "CURRENT" }?.toActiveStorage(),
    previousLoggedSets = sets.filter { it.snapshotRole == "PREVIOUS" }.map { it.toActiveStorage() },
)

private fun WorkoutLogEventSetEntity.toActiveStorage() = ActiveWorkoutSetStorage(
    id = id,
    exerciseId = exerciseId,
    performedExerciseId = performedExerciseId,
    sourceWorkoutExerciseId = sourceWorkoutExerciseId,
    weight = weight,
    reps = reps,
    rpe = rpe,
    repsInReserve = repsInReserve,
    setType = setType.toEnum(SetType.NORMAL),
    restSeconds = restSeconds,
    orderIndex = orderIndex,
    completed = completed,
    loggedAt = loggedAt,
)

private inline fun <reified T : Enum<T>> String.toEnum(default: T): T =
    runCatching { enumValueOf<T>(trim().uppercase()) }.getOrDefault(default)

private fun String.sha256(): String =
    MessageDigest.getInstance("SHA-256")
        .digest(toByteArray(Charsets.UTF_8))
        .joinToString(separator = "") { byte -> "%02x".format(byte) }
