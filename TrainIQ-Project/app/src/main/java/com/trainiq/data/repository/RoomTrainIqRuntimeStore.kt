package com.trainiq.data.repository

import com.google.gson.Gson
import androidx.room.withTransaction
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
import com.trainiq.core.database.SavedGoalAdviceEntity
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
import com.trainiq.data.mapper.toDomain
import com.trainiq.domain.model.FoodSourceType
import com.trainiq.domain.model.LoggedMealItemType
import com.trainiq.domain.model.MealType
import com.trainiq.domain.model.SetType
import com.trainiq.domain.model.WorkoutDebrief
import com.trainiq.domain.model.WorkoutLogEventType
import com.trainiq.domain.model.WorkoutSyncStatus
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class SavedRecipeSnapshot(
    val recipe: RecipeStorage,
    val ingredients: List<RecipeIngredientStorage>,
    val foods: List<FoodItemStorage>,
)

@Singleton
class RoomTrainIqRuntimeStore internal constructor(
    private val database: TrainIqDatabase,
    private val legacyStore: TrainIqLocalStore,
    private val scope: CoroutineScope,
) {
    @Inject
    constructor(database: TrainIqDatabase, legacyStore: TrainIqLocalStore) :
        this(database, legacyStore, CoroutineScope(SupervisorJob() + Dispatchers.IO))

    private val dao = database.dao()
    private val gson = Gson()
    private val planner = JsonRoomImportPlanner(gson)
    private val sink = RoomJsonImportSink(database)
    private val mutex = Mutex()

    init {
        scope.launch {
            runCatching {
                seedRoomFromLegacyJsonIfNeeded()
            }
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

    suspend fun readExportSnapshot(): TrainIqStorageState = mutex.withLock {
        database.withTransaction {
            val active = ActiveWorkoutTables(
                sessions = dao.readActiveWorkoutSessionsForExport(),
                drafts = dao.readActiveWorkoutDraftsForExport(),
                collapsed = dao.readActiveWorkoutCollapsedExercisesForExport(),
                sets = dao.readActiveWorkoutSetsForExport(),
                events = dao.readWorkoutLogEventsForExport(),
            )
            val eventSets = dao.readWorkoutLogEventSetsForExport()
            TrainIqStorageState(
                profile = dao.readUserProfileForExport(),
                routines = dao.readRoutinesForExport(),
                days = dao.readWorkoutDaysForExport(),
                exercises = dao.readExercisesForExport(),
                workoutExercises = dao.readWorkoutExercisesForExport(),
                routineSets = dao.readRoutineSetsForExport(),
                foods = dao.readFoodItemsForExport().map { it.toStorage() },
                recipes = dao.readRecipesForExport().map { it.toStorage() },
                recipeIngredients = dao.readRecipeIngredientsForExport().map { it.toStorage() },
                meals = dao.readMealsForExport().map { it.toStorage() },
                mealItems = dao.readMealItemsForExport().map { it.toStorage() },
                measurements = dao.readMeasurementsForExport(),
                sessions = dao.readWorkoutSessionsForExport(),
                performedExercises = dao.readPerformedExercisesForExport(),
                workoutSets = dao.readWorkoutSetsForExport(),
                activeWorkoutSession = active.toStorage(),
                workoutLogEvents = active.events.map { event ->
                    event.toStorage(eventSets.filter { set -> set.eventId == event.id })
                },
            )
        }
    }

    suspend fun clearAll() {
        mutex.withLock {
            database.dao().clearMirrorTables()
        }
    }

    suspend fun clearProfile() {
        mutex.withLock {
            database.withTransaction {
                dao.clearSavedGoalAdvice()
                dao.clearMirrorUserProfile()
            }
        }
    }

    fun observeSavedGoalAdvice() = dao.observeSavedGoalAdvice().map { it?.toDomain() }

    suspend fun saveProfile(profile: UserProfileEntity, savedGoalAdvice: SavedGoalAdviceEntity? = null) {
        mutex.withLock {
            database.withTransaction {
                dao.upsertUserProfile(profile)
                if (savedGoalAdvice != null) {
                    dao.upsertSavedGoalAdvice(savedGoalAdvice)
                }
            }
        }
    }

    suspend fun updateRoutineSet(set: RoutineSetEntity, workoutExercise: WorkoutExerciseEntity) {
        mutex.withLock {
            dao.updateRoutineSet(set = set, workoutExercise = workoutExercise)
        }
    }

    internal suspend fun updateRoutineSet(setId: Long, transform: (RoutineSetEntity) -> RoutineSetEntity) {
        mutex.withLock {
            database.withTransaction {
                val updated = readCurrentPlan().updateRoutineSet(setId, transform)
                val set = updated.routineSets.firstOrNull { it.id == setId } ?: return@withTransaction
                dao.updateRoutineSet(set, updated.workoutExercises.first { it.id == set.workoutExerciseId })
            }
        }
    }

    internal suspend fun replaceRoutineSetsForExercise(workoutExerciseId: Long, transform: (TrainIqStorageState) -> TrainIqStorageState) {
        mutex.withLock {
            database.withTransaction {
                val updated = transform(readCurrentPlan())
                val plan = updated.workoutExercises.firstOrNull { it.id == workoutExerciseId } ?: return@withTransaction
                dao.replaceRoutineSetsForExercise(workoutExerciseId, updated.routineSets.filter { it.workoutExerciseId == workoutExerciseId }, plan)
            }
        }
    }

    internal suspend fun saveWorkoutExercise(workoutExerciseId: Long, newExerciseId: Long) {
        mutex.withLock {
            database.withTransaction {
                require(dao.getExercise(newExerciseId) != null) { "Oefening bestaat niet meer." }
                dao.readWorkoutExercisesForExport().firstOrNull { it.id == workoutExerciseId }?.let {
                    dao.insertWorkoutExercise(it.copy(exerciseId = newExerciseId))
                }
            }
        }
    }

    internal suspend fun deleteRoutineSet(setId: Long) {
        mutex.withLock {
            database.withTransaction {
                val current = readCurrentPlan()
                val planId = current.routineSets.firstOrNull { it.id == setId }?.workoutExerciseId ?: return@withTransaction
                val updated = current.copy(routineSets = current.routineSets.filterNot { it.id == setId })
                    .renumberRoutineSets(planId).withWorkoutExerciseTargetsSynced(planId)
                dao.replaceRoutineSetsForExercise(planId, updated.routineSets.filter { it.workoutExerciseId == planId },
                    updated.workoutExercises.first { it.id == planId })
            }
        }
    }

    internal suspend fun replaceWorkoutExerciseInActiveWorkout(workoutExerciseId: Long, newExerciseId: Long): ActiveWorkoutSessionStorage? =
        mutex.withLock {
            database.withTransaction {
                val current = readCurrentPlan().copy(activeWorkoutSession = readCurrentActiveWorkout())
                require(current.exercises.any { it.id == newExerciseId }) { "Oefening bestaat niet meer." }
                val updated = current.withExerciseReplacedInActiveWorkout(workoutExerciseId, newExerciseId, System.currentTimeMillis())
                val plan = updated.workoutExercises.firstOrNull { it.id == workoutExerciseId } ?: return@withTransaction current.activeWorkoutSession
                val active = updated.activeWorkoutSession
                val affected = active?.takeIf { it.dayId == plan.dayId }
                dao.replaceWorkoutExerciseInActiveWorkout(plan, affected?.sessionId, affected?.updatedAt)
                active
            }
        }

    internal suspend fun addWorkoutDay(routineId: Long, name: String) {
        mutex.withLock {
            database.withTransaction {
                val days = dao.observeWorkoutDaysSnapshot(routineId)
                val order = (days.maxOfOrNull { it.orderIndex } ?: -1) + 1
                dao.insertWorkoutDay(WorkoutDayEntity((dao.getMaxWorkoutDayId() ?: 0L) + 1L, routineId,
                    name.trim().ifBlank { defaultWorkoutSessionName(order) }, order))
            }
        }
    }

    internal suspend fun addWorkoutExerciseToDay(transform: (TrainIqStorageState) -> TrainIqStorageState) {
        mutex.withLock {
            database.withTransaction {
                val current = readCurrentPlan()
                val updated = transform(current)
                val plan = updated.workoutExercises.firstOrNull { candidate -> current.workoutExercises.none { it.id == candidate.id } } ?: return@withTransaction
                dao.addWorkoutExerciseToDay(
                    updated.days.firstOrNull { candidate -> current.days.none { it.id == candidate.id } },
                    updated.exercises.first { it.id == plan.exerciseId }, plan,
                    updated.routineSets.filter { it.workoutExerciseId == plan.id },
                )
            }
        }
    }

    suspend fun replaceRoutineSetsForExercise(
        workoutExerciseId: Long,
        sets: List<RoutineSetEntity>,
        workoutExercise: WorkoutExerciseEntity,
    ) {
        mutex.withLock {
            dao.replaceRoutineSetsForExercise(
                workoutExerciseId = workoutExerciseId,
                sets = sets,
                workoutExercise = workoutExercise,
            )
        }
    }

    suspend fun createRoutine(name: String, description: String) {
        mutex.withLock {
            database.withTransaction {
                require(name.isNotBlank() && dao.getRoutinesSnapshot().none { it.name.equals(name.trim(), ignoreCase = true) }) {
                    "Een routine met deze naam bestaat al of de naam is leeg."
                }
                val routineId = (dao.getMaxRoutineId() ?: 0L) + 1L
                dao.insertRoutine(
                    WorkoutRoutineEntity(
                        id = routineId,
                        name = name.trim(),
                        description = description,
                        active = dao.routineCount() == 0,
                    ),
                )
            }
        }
    }

    suspend fun updateRoutine(routineId: Long, name: String, description: String) {
        mutex.withLock {
            database.withTransaction {
                require(name.isNotBlank() && dao.getRoutinesSnapshot().none {
                    it.id != routineId && it.name.equals(name.trim(), ignoreCase = true)
                }) { "Een routine met deze naam bestaat al of de naam is leeg." }
                dao.updateRoutine(routineId = routineId, name = name.trim(), description = description)
            }
        }
    }

    suspend fun deleteRoutine(routineId: Long) {
        mutex.withLock {
            database.withTransaction {
                val dayIds = dao.observeWorkoutDaysSnapshot(routineId).map { it.id }.toSet()
                if (dao.readActiveWorkoutSessionsForExport().any { it.dayId in dayIds })
                    throw com.trainiq.domain.repository.ActiveWorkoutPlanInUseException()
                dao.deleteRoutineAndNormalizeActive(routineId)
            }
        }
    }

    suspend fun setActiveRoutine(routineId: Long) {
        mutex.withLock {
            database.withTransaction {
                require(dao.getRoutinesSnapshot().any { it.id == routineId }) { "Routine bestaat niet meer." }
                dao.setActiveRoutine(routineId)
            }
        }
    }

    suspend fun reorderExercises(dayId: Long, orderedIds: List<Long>) {
        mutex.withLock {
            val requestedOrder = orderedIds.distinct()
            val fallbackOrder = dao.getWorkoutExercisesForDay(dayId)
                .filterNot { it.id in requestedOrder }
                .sortedWith(compareBy<WorkoutExerciseEntity> { it.orderIndex }.thenBy { it.id })
                .map { it.id }
            dao.reorderExercises(dayId = dayId, orderedIds = requestedOrder + fallbackOrder)
        }
    }

    suspend fun setSupersetGroup(workoutExerciseIds: List<Long>, groupId: Long?) {
        val ids = workoutExerciseIds.distinct()
        if (ids.isEmpty()) return
        mutex.withLock {
            dao.setSupersetGroup(workoutExerciseIds = ids, groupId = groupId)
        }
    }

    suspend fun saveWorkoutExercise(workoutExercise: WorkoutExerciseEntity) {
        mutex.withLock {
            dao.insertWorkoutExercise(workoutExercise)
        }
    }

    suspend fun seedExerciseLibrary(exercises: List<ExerciseEntity>) {
        if (exercises.isEmpty()) return
        mutex.withLock {
            dao.insertExercises(exercises)
        }
    }

    suspend fun replaceWorkoutExerciseInActiveWorkout(
        workoutExercise: WorkoutExerciseEntity,
        activeSessionId: Long?,
        updatedAt: Long?,
    ) {
        mutex.withLock {
            dao.replaceWorkoutExerciseInActiveWorkout(
                workoutExercise = workoutExercise,
                activeSessionId = activeSessionId,
                updatedAt = updatedAt,
            )
        }
    }

    suspend fun addWorkoutDay(day: WorkoutDayEntity) {
        mutex.withLock {
            dao.insertWorkoutDay(day)
        }
    }

    suspend fun removeWorkoutDay(dayId: Long) {
        mutex.withLock {
            database.withTransaction {
                if (dao.readActiveWorkoutSessionsForExport().any { it.dayId == dayId })
                    throw com.trainiq.domain.repository.ActiveWorkoutPlanInUseException()
                dao.deleteWorkoutDayCascade(dayId)
            }
        }
    }

    suspend fun addWorkoutExerciseToDay(
        day: WorkoutDayEntity?,
        exercise: ExerciseEntity,
        workoutExercise: WorkoutExerciseEntity,
        sets: List<RoutineSetEntity>,
    ) {
        mutex.withLock {
            dao.addWorkoutExerciseToDay(
                day = day,
                exercise = exercise,
                workoutExercise = workoutExercise,
                sets = sets,
            )
        }
    }

    suspend fun removeWorkoutExerciseFromDay(
        workoutExerciseId: Long,
    ) {
        mutex.withLock {
            database.withTransaction {
                val active = readCurrentPlan().copy(activeWorkoutSession = readCurrentActiveWorkout())
                    .withExerciseRemovedFromDay(workoutExerciseId, System.currentTimeMillis()).activeWorkoutSession
                dao.deleteWorkoutExerciseCascade(
                    workoutExerciseId = workoutExerciseId,
                    activeSession = active?.toActiveWorkoutSessionEntity(),
                    activeDrafts = active?.drafts?.map { (exerciseId, draft) ->
                        ActiveWorkoutDraftEntity(
                            sessionId = active.sessionId,
                            exerciseId = exerciseId,
                            weight = draft.weight,
                            reps = draft.reps,
                            rpe = draft.rpe,
                            setType = draft.setType.name,
                        )
                    }.orEmpty(),
                    activeCollapsedExercises = active?.collapsedExerciseIds?.map { exerciseId ->
                        ActiveWorkoutCollapsedExerciseEntity(
                            sessionId = active.sessionId,
                            exerciseId = exerciseId,
                        )
                    }.orEmpty(),
                    activeSets = active?.loggedSets?.map {
                        it.toActiveWorkoutSetEntity(sessionId = active.sessionId)
                    }.orEmpty(),
                )
            }
        }
    }

    suspend fun deleteWorkoutSession(sessionId: Long) {
        mutex.withLock {
            dao.deleteWorkoutSessionCascade(sessionId)
        }
    }

    suspend fun saveGeneratedRoutine(
        routine: WorkoutRoutineEntity,
        days: List<WorkoutDayEntity>,
        exercises: List<ExerciseEntity>,
        workoutExercises: List<WorkoutExerciseEntity>,
        sets: List<RoutineSetEntity>,
    ) {
        mutex.withLock {
            dao.insertGeneratedRoutineGraph(
                routine = routine,
                days = days,
                exercises = exercises,
                workoutExercises = workoutExercises,
                sets = sets,
            )
        }
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

    suspend fun startOrResumeActiveWorkoutSession(
        active: ActiveWorkoutSessionStorage,
        draftSession: WorkoutSessionEntity,
        performedExercises: List<PerformedExerciseEntity>,
    ) {
        mutex.withLock {
            dao.startOrResumeActiveWorkoutSession(
                activeSession = active.toActiveWorkoutSessionEntity(),
                draftSession = draftSession,
                drafts = active.drafts.map { (exerciseId, draft) ->
                    ActiveWorkoutDraftEntity(
                        sessionId = active.sessionId,
                        exerciseId = exerciseId,
                        weight = draft.weight,
                        reps = draft.reps,
                        rpe = draft.rpe,
                        setType = draft.setType.name,
                    )
                },
                performedExercises = performedExercises,
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

    suspend fun updateActiveWorkoutSet(
        active: ActiveWorkoutSessionStorage,
        set: ActiveWorkoutSetStorage,
        draft: ActiveWorkoutDraftStorage,
    ) {
        mutex.withLock {
            dao.updateActiveWorkoutSet(
                sessionId = active.sessionId,
                draft = ActiveWorkoutDraftEntity(
                    sessionId = active.sessionId,
                    exerciseId = set.activeKey,
                    weight = draft.weight,
                    reps = draft.reps,
                    rpe = draft.rpe,
                    setType = draft.setType.name,
                ),
                set = set.toActiveWorkoutSetEntity(sessionId = active.sessionId),
                restTimerEndsAt = active.restTimerEndsAt,
                restTimerTotalSeconds = active.restTimerTotalSeconds,
                updatedAt = active.updatedAt,
            )
        }
    }

    suspend fun setActiveWorkoutCollapsedExercise(
        sessionId: Long,
        exerciseId: Long,
        collapsed: Boolean,
        updatedAt: Long,
    ) {
        mutex.withLock {
            dao.setActiveWorkoutCollapsedExercise(
                collapsedExercise = ActiveWorkoutCollapsedExerciseEntity(
                    sessionId = sessionId,
                    exerciseId = exerciseId,
                ),
                collapsed = collapsed,
                updatedAt = updatedAt,
            )
        }
    }

    suspend fun updateActiveWorkoutSetType(
        sessionId: Long,
        setId: Long,
        setType: SetType,
        updatedAt: Long,
    ) {
        mutex.withLock {
            dao.updateActiveWorkoutSetType(
                sessionId = sessionId,
                setId = setId,
                setType = setType.name,
                updatedAt = updatedAt,
            )
        }
    }

    suspend fun deleteActiveWorkoutSet(
        sessionId: Long,
        setId: Long,
        updatedAt: Long,
    ) {
        mutex.withLock {
            dao.deleteActiveWorkoutSet(
                sessionId = sessionId,
                setId = setId,
                updatedAt = updatedAt,
            )
        }
    }

    suspend fun undoActiveWorkoutLogEvent(
        active: ActiveWorkoutSessionStorage,
        undoEvent: WorkoutLogEventStorage,
    ) {
        mutex.withLock {
            dao.undoActiveWorkoutLogEvent(
                sessionId = active.sessionId,
                targetSetId = requireNotNull(undoEvent.set).id,
                undoEvent = undoEvent.toWorkoutLogEventEntity(),
                undoEventSets = undoEvent.toWorkoutLogEventSetEntities(),
                updatedAt = active.updatedAt,
            )
        }
    }

    suspend fun saveMeal(meal: LoggedMealStorage, items: List<LoggedMealItemStorage>): Long = mutex.withLock {
        database.withTransaction {
            persistMeal(meal, items)
        }
    }

    internal suspend fun saveMeal(
        meal: LoggedMealStorage,
        buildItems: (List<FoodItemStorage>, List<RecipeStorage>, List<RecipeIngredientStorage>) -> List<LoggedMealItemStorage>,
    ): Long = mutex.withLock {
        database.withTransaction {
            val items = buildItems(
                dao.readFoodItemsForExport().map { it.toStorage() },
                dao.readRecipesForExport().map { it.toStorage() },
                dao.readRecipeIngredientsForExport().map { it.toStorage() },
            )
            persistMeal(meal, items)
        }
    }

    private suspend fun persistMeal(meal: LoggedMealStorage, items: List<LoggedMealItemStorage>): Long {
        val mealId = meal.id.takeIf { it > 0L } ?: ((dao.getMaxMealId() ?: 0L) + 1L)
        val firstItemId = (dao.getMaxMealItemId() ?: 0L) + 1L
        val persistedItems = items.mapIndexed { index, item ->
            item.copy(id = firstItemId + index, mealId = mealId)
        }
        dao.saveMeal(
            meal = meal.copy(id = mealId, timestamp = dao.getMeal(mealId)?.date ?: meal.timestamp).toMealEntity(persistedItems),
            items = persistedItems.mapIndexed { index, item -> item.toMealItemEntity(orderIndex = index) },
        )
        return mealId
    }

    suspend fun deleteMeal(mealId: Long) {
        mutex.withLock {
            dao.deleteMealWithItems(mealId)
        }
    }

    suspend fun saveFood(food: FoodItemStorage): FoodItemStorage = mutex.withLock {
        database.withTransaction {
            val existing = food.id.takeIf { it > 0L }?.let { dao.getFoodItem(it) }
            require(food.id <= 0L || existing != null) { "Dit product bestaat niet meer. Maak een nieuw product aan." }
            val duplicateBarcode = food.barcode
                ?.takeIf { it.isNotBlank() }
                ?.let { dao.getFoodItemByBarcode(it) }
                ?.takeIf { it.id != food.id }
            val matched = existing ?: duplicateBarcode
            require(existing == null || duplicateBarcode == null) {
                "Deze barcode hoort al bij een ander product. Pas de barcode aan."
            }
            val persisted = food.copy(
                id = matched?.id ?: ((dao.getMaxFoodItemId() ?: 0L) + 1L),
                createdAt = matched?.createdAt ?: food.createdAt,
            )
            dao.insertFoodItems(listOf(persisted.toFoodItemEntity()))
            persisted
        }
    }

    suspend fun deleteFood(foodId: Long) {
        mutex.withLock {
            dao.deleteFoodItem(foodId)
        }
    }

    suspend fun saveRecipe(
        recipe: RecipeStorage,
        ingredients: List<RecipeIngredientStorage>,
    ): SavedRecipeSnapshot = mutex.withLock {
        database.withTransaction {
            val persisted = recipe.copy(
                id = recipe.id.takeIf { it > 0L } ?: ((dao.getMaxRecipeId() ?: 0L) + 1L),
                createdAt = dao.getRecipe(recipe.id)?.createdAt ?: recipe.createdAt,
            )
            val firstIngredientId = (dao.getMaxRecipeIngredientId() ?: 0L) + 1L
            val persistedIngredients = ingredients.mapIndexed { index, ingredient ->
                ingredient.copy(id = firstIngredientId + index, recipeId = persisted.id)
            }
            dao.saveRecipe(
                recipe = persisted.toRecipeEntity(),
                ingredients = persistedIngredients.mapIndexed { index, ingredient ->
                    ingredient.toRecipeIngredientEntity(orderIndex = index)
                },
            )
            SavedRecipeSnapshot(
                recipe = persisted,
                ingredients = persistedIngredients,
                foods = dao.getFoodsByIds(persistedIngredients.map { it.foodItemId }.distinct()).map { it.toStorage() },
            )
        }
    }

    suspend fun deleteRecipe(recipeId: Long) {
        mutex.withLock {
            dao.deleteRecipeWithIngredients(recipeId)
        }
    }

    suspend fun discardActiveWorkoutSession(sessionId: Long) {
        mutex.withLock {
            dao.discardActiveWorkoutSession(sessionId)
        }
    }

    suspend fun finishActiveWorkoutSession(
        session: WorkoutSessionEntity,
        performedExercises: List<PerformedExerciseEntity>,
        sets: List<WorkoutSetEntity>,
        activeSessionId: Long?,
    ) {
        mutex.withLock {
            dao.finishActiveWorkoutSession(
                session = session,
                performedExercises = performedExercises,
                sets = sets,
                activeSessionId = activeSessionId,
            )
        }
    }

    suspend fun getWorkoutDebriefRefreshSnapshot(sessionId: Long): WorkoutDebriefRefreshSnapshot =
        database.withTransaction {
            WorkoutDebriefRefreshSnapshot(
                session = dao.getCompletedWorkoutSession(sessionId),
                sessions = dao.getWorkoutSessions(),
                sets = dao.getWorkoutSets(),
                days = dao.getWorkoutDays(),
                exercises = dao.getExercises(),
                workoutExercises = dao.getWorkoutExercises(),
            )
        }

    suspend fun updateWorkoutSessionDebrief(sessionId: Long, debrief: WorkoutDebrief): Int =
        mutex.withLock {
            dao.updateWorkoutSessionDebrief(
                sessionId = sessionId,
                summary = debrief.summary,
                progressionFeedback = debrief.progressionFeedback,
                recommendation = debrief.recommendation,
                nextSessionFocus = debrief.nextSessionFocus,
                recoveryScore = debrief.recoveryScore,
                intensitySignal = debrief.intensitySignal,
                wins = debrief.wins.joinToString("\n"),
                risks = debrief.risks.joinToString("\n"),
                nextLoadTarget = debrief.nextLoadTarget,
                recoveryAdvice = debrief.recoveryAdvice,
                source = debrief.source.name,
            )
        }

    suspend fun addMeasurement(measurement: BodyMeasurementEntity) {
        mutex.withLock {
            dao.insertMeasurement(measurement)
        }
    }

    suspend fun deleteMeasurement(measurementId: Long) {
        mutex.withLock {
            dao.deleteMeasurement(measurementId)
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

    /** Planning inputs only; history, nutrition and JSON mirrors are never read or rewritten. */
    private suspend fun readCurrentPlan() = TrainIqStorageState(
        routines = dao.readRoutinesForExport(),
        days = dao.readWorkoutDaysForExport(),
        exercises = dao.readExercisesForExport(),
        workoutExercises = dao.readWorkoutExercisesForExport(),
        routineSets = dao.readRoutineSetsForExport(),
    )

    private suspend fun readCurrentActiveWorkout() = ActiveWorkoutTables(
        sessions = dao.readActiveWorkoutSessionsForExport(),
        drafts = dao.readActiveWorkoutDraftsForExport(),
        collapsed = dao.readActiveWorkoutCollapsedExercisesForExport(),
        sets = dao.readActiveWorkoutSetsForExport(),
        events = emptyList(),
    ).toStorage()
}

data class WorkoutDebriefRefreshSnapshot(
    val session: WorkoutSessionEntity?,
    val sessions: List<WorkoutSessionEntity>,
    val sets: List<WorkoutSetEntity>,
    val days: List<WorkoutDayEntity>,
    val exercises: List<ExerciseEntity>,
    val workoutExercises: List<WorkoutExerciseEntity>,
)

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
    defaultServingGrams = defaultServingGrams.normalizedDefaultServingGrams(),
    sourceType = sourceType.toEnum(FoodSourceType.MANUAL),
    createdAt = createdAt,
    updatedAt = updatedAt,
)

private fun FoodItemStorage.toFoodItemEntity() = FoodItemEntity(
    id = id,
    name = name,
    barcode = barcode,
    caloriesPer100g = caloriesPer100g,
    proteinPer100g = proteinPer100g,
    carbsPer100g = carbsPer100g,
    fatPer100g = fatPer100g,
    defaultServingGrams = defaultServingGrams.normalizedDefaultServingGrams(),
    sourceType = sourceType.name,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

private fun Double.normalizedDefaultServingGrams(): Double =
    takeIf { it.isFinite() && it > 0.0 } ?: 100.0

private fun RecipeEntity.toStorage() = RecipeStorage(
    id = id,
    name = name,
    notes = notes,
    totalCookedGrams = totalCookedGrams,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

private fun RecipeStorage.toRecipeEntity() = RecipeEntity(
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

private fun RecipeIngredientStorage.toRecipeIngredientEntity(orderIndex: Int) = RecipeIngredientEntity(
    id = id,
    recipeId = recipeId,
    foodItemId = foodItemId,
    gramsUsed = gramsUsed,
    orderIndex = orderIndex,
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
    servingCount = servingCount.coerceAtLeast(1),
    calories = calories,
    protein = protein,
    carbs = carbs,
    fat = fat,
    notes = notes,
)

private fun LoggedMealStorage.toMealEntity(items: List<LoggedMealItemStorage>) = MealEntity(
    id = id,
    date = timestamp,
    mealType = mealType.name,
    name = name,
    notes = notes,
    calories = items.sumOf { it.calories }.roundToInt(),
    protein = items.sumOf { it.protein }.roundToInt(),
    carbs = items.sumOf { it.carbs }.roundToInt(),
    fat = items.sumOf { it.fat }.roundToInt(),
)

private fun LoggedMealItemStorage.toMealItemEntity(orderIndex: Int) = MealItemEntity(
    id = id,
    mealId = mealId,
    itemType = itemType.name,
    referenceId = referenceId,
    name = name,
    gramsUsed = gramsUsed,
    servingCount = servingCount.coerceAtLeast(1),
    calories = calories,
    protein = protein,
    carbs = carbs,
    fat = fat,
    notes = notes,
    orderIndex = orderIndex,
)

private fun ActiveWorkoutSessionStorage.toActiveWorkoutSessionEntity() = ActiveWorkoutSessionEntity(
    sessionId = sessionId,
    dayId = dayId,
    routineId = routineId,
    startedAt = startedAt,
    updatedAt = updatedAt,
    restTimerEndsAt = restTimerEndsAt,
    restTimerTotalSeconds = restTimerTotalSeconds,
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
