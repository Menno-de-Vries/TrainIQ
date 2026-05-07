package com.trainiq.data.migration

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
import com.trainiq.core.database.RoomMirrorImportRunEntity
import com.trainiq.core.database.TrainIqDatabase
import com.trainiq.core.database.UserProfileEntity
import com.trainiq.core.database.WorkoutDayEntity
import com.trainiq.core.database.WorkoutExerciseEntity
import com.trainiq.core.database.WorkoutLogEventEntity
import com.trainiq.core.database.WorkoutLogEventSetEntity
import com.trainiq.core.database.WorkoutRoutineEntity
import com.trainiq.core.database.WorkoutSessionEntity
import com.trainiq.core.database.WorkoutSetEntity
import com.trainiq.data.local.ActiveWorkoutSetStorage
import com.trainiq.data.local.TrainIqStorageState
import com.trainiq.domain.model.MealType
import androidx.room.withTransaction
import kotlin.math.abs
import kotlin.math.roundToInt

class JsonRoomImportPlanner(
    private val gson: Gson = Gson(),
) {
    fun plan(sourceJson: String): JsonRoomImportPlan {
        val state = gson.fromJson(sourceJson, TrainIqStorageState::class.java) ?: TrainIqStorageState()
        return JsonRoomImportPlan(
            profile = state.profile?.normalized(),
            routines = state.routines.map { it.normalized() },
            days = state.days.map { it.normalized() },
            exercises = state.exercises.map { it.normalized() },
            workoutExercises = state.workoutExercises.map { it.normalized() },
            routineSets = state.routineSets.map { it.normalized() },
            sessions = state.sessions.map { it.normalized() },
            performedExercises = state.performedExercises,
            workoutSets = state.workoutSets.map { it.normalized() },
            meals = state.toMealEntities(),
            foodItems = state.foods.map {
                FoodItemEntity(
                    id = it.id,
                    name = stringOrDefault(it.name),
                    barcode = it.barcode,
                    caloriesPer100g = it.caloriesPer100g,
                    proteinPer100g = it.proteinPer100g,
                    carbsPer100g = it.carbsPer100g,
                    fatPer100g = it.fatPer100g,
                    sourceType = enumNameOrDefault({ it.sourceType.name }, "MANUAL"),
                    createdAt = it.createdAt,
                    updatedAt = it.updatedAt,
                )
            },
            recipes = state.recipes.map {
                RecipeEntity(
                    id = it.id,
                    name = stringOrDefault(it.name),
                    notes = it.notes,
                    totalCookedGrams = it.totalCookedGrams,
                    createdAt = it.createdAt,
                    updatedAt = it.updatedAt,
                )
            },
            recipeIngredients = state.recipeIngredients.mapIndexed { index, ingredient ->
                RecipeIngredientEntity(
                    id = ingredient.id,
                    recipeId = ingredient.recipeId,
                    foodItemId = ingredient.foodItemId,
                    gramsUsed = ingredient.gramsUsed,
                    orderIndex = index,
                )
            },
            mealItems = state.mealItems.mapIndexed { index, item ->
                MealItemEntity(
                    id = item.id,
                    mealId = item.mealId,
                    itemType = enumNameOrDefault({ item.itemType.name }, "FOOD"),
                    referenceId = item.referenceId,
                    name = stringOrDefault(item.name),
                    gramsUsed = item.gramsUsed,
                    calories = item.calories,
                    protein = item.protein,
                    carbs = item.carbs,
                    fat = item.fat,
                    notes = item.notes,
                    orderIndex = index,
                )
            },
            activeWorkoutSessions = state.activeWorkoutSession?.let {
                listOf(
                    ActiveWorkoutSessionEntity(
                        sessionId = it.sessionId,
                        dayId = it.dayId,
                        routineId = it.routineId,
                        startedAt = it.startedAt,
                        updatedAt = it.updatedAt,
                        restTimerEndsAt = it.restTimerEndsAt,
                        restTimerTotalSeconds = it.restTimerTotalSeconds,
                    ),
                )
            }.orEmpty(),
            activeWorkoutDrafts = state.activeWorkoutSession?.let { active ->
                active.drafts.map { (exerciseId, draft) ->
                    ActiveWorkoutDraftEntity(
                        sessionId = active.sessionId,
                        exerciseId = exerciseId,
                        weight = stringOrDefault(draft.weight),
                        reps = stringOrDefault(draft.reps),
                        rpe = stringOrDefault(draft.rpe),
                        setType = enumNameOrDefault({ draft.setType.name }, "NORMAL"),
                    )
                }
            }.orEmpty(),
            activeWorkoutCollapsedExercises = state.activeWorkoutSession?.let { active ->
                active.collapsedExerciseIds.map { exerciseId ->
                    ActiveWorkoutCollapsedExerciseEntity(active.sessionId, exerciseId)
                }
            }.orEmpty(),
            activeWorkoutSets = state.activeWorkoutSession?.let { active ->
                active.loggedSets.map { it.toActiveWorkoutSetEntity(active.sessionId) }
            }.orEmpty(),
            workoutLogEvents = state.workoutLogEvents.map {
                WorkoutLogEventEntity(
                    id = it.id,
                    dayId = it.dayId,
                    sessionId = it.sessionId,
                    type = enumNameOrDefault({ it.type.name }, "ADD_SET"),
                    syncStatus = enumNameOrDefault({ it.syncStatus.name }, "PENDING"),
                    createdAt = it.createdAt,
                    undoExpiresAt = it.undoExpiresAt,
                    targetEventId = it.targetEventId,
                )
            },
            workoutLogEventSets = state.workoutLogEvents.flatMap { event ->
                buildList {
                    event.set?.let { add(it.toWorkoutLogEventSetEntity(event.id, "CURRENT", 0)) }
                    event.previousLoggedSets.forEachIndexed { index, set ->
                        add(set.toWorkoutLogEventSetEntity(event.id, "PREVIOUS", index))
                    }
                }
            },
            measurements = state.measurements,
            schemaParityGaps = state.schemaParityGaps(),
        )
    }

    private fun UserProfileEntity.normalized(): UserProfileEntity = copy(
        name = stringOrDefault(name),
        age = age.takeIf { it > 0 } ?: 30,
        sex = stringOrDefault(sex, "MALE"),
        activityLevel = stringOrDefault(activityLevel, "MODERATE"),
        goal = stringOrDefault(goal, "MAINTENANCE"),
        trainingFocus = stringOrDefault(trainingFocus, "STRENGTH"),
    )

    private fun WorkoutRoutineEntity.normalized(): WorkoutRoutineEntity = copy(
        name = stringOrDefault(name),
        description = stringOrDefault(description),
    )

    private fun WorkoutDayEntity.normalized(): WorkoutDayEntity = copy(
        name = stringOrDefault(name),
    )

    private fun ExerciseEntity.normalized(): ExerciseEntity = copy(
        name = stringOrDefault(name),
        muscleGroup = stringOrDefault(muscleGroup),
        equipment = stringOrDefault(equipment),
    )

    private fun WorkoutExerciseEntity.normalized(): WorkoutExerciseEntity = copy(
        repRange = stringOrDefault(repRange),
        setType = stringOrDefault(setType, "WORKING"),
    )

    private fun RoutineSetEntity.normalized(): RoutineSetEntity = copy(
        setType = stringOrDefault(setType, "NORMAL"),
    )

    private fun WorkoutSessionEntity.normalized(): WorkoutSessionEntity = copy(
        status = stringOrDefault(status, "COMPLETED"),
        debriefSummary = stringOrDefault(debriefSummary),
        debriefProgressionFeedback = stringOrDefault(debriefProgressionFeedback),
        debriefRecommendation = stringOrDefault(debriefRecommendation),
        debriefNextSessionFocus = stringOrDefault(debriefNextSessionFocus),
        debriefIntensitySignal = stringOrDefault(debriefIntensitySignal, "MAINTAIN"),
        debriefWins = stringOrDefault(debriefWins),
        debriefRisks = stringOrDefault(debriefRisks),
        debriefNextLoadTarget = stringOrDefault(debriefNextLoadTarget),
        debriefRecoveryAdvice = stringOrDefault(debriefRecoveryAdvice),
        debriefSource = stringOrDefault(debriefSource, "LOCAL_FALLBACK"),
    )

    private fun WorkoutSetEntity.normalized(): WorkoutSetEntity = copy(
        setType = stringOrDefault(setType, "WORKING"),
    )

    private fun TrainIqStorageState.toMealEntities(): List<MealEntity> =
        meals.map { meal ->
            val items = mealItems.filter { it.mealId == meal.id }
            MealEntity(
                id = meal.id,
                date = meal.timestamp,
                mealType = enumNameOrDefault({ meal.mealType.name }, MealType.LUNCH.name),
                name = stringOrDefault(meal.name),
                notes = runCatching { meal.notes }.getOrNull(),
                calories = items.sumOf { it.calories }.roundToInt(),
                protein = items.sumOf { it.protein }.roundToInt(),
                carbs = items.sumOf { it.carbs }.roundToInt(),
                fat = items.sumOf { it.fat }.roundToInt(),
            )
        }

    private fun TrainIqStorageState.schemaParityGaps(): Set<RoomSchemaParityGap> = buildSet {
        // Populated once a JSON field cannot be represented by the Room import plan.
    }

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
        setType = enumNameOrDefault({ setType.name }, "NORMAL"),
        restSeconds = restSeconds,
        orderIndex = orderIndex,
        completed = completed,
        loggedAt = loggedAt,
    )

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
        setType = enumNameOrDefault({ setType.name }, "NORMAL"),
        restSeconds = restSeconds,
        orderIndex = orderIndex,
        completed = completed,
        loggedAt = loggedAt,
    )

    private fun stringOrDefault(value: String?, default: String = ""): String =
        value?.ifBlank { default } ?: default

    private fun enumNameOrDefault(block: () -> String, default: String): String =
        runCatching(block).getOrNull().orEmpty().ifBlank { default }
}

data class JsonRoomImportPlan(
    val profile: UserProfileEntity? = null,
    val routines: List<WorkoutRoutineEntity> = emptyList(),
    val days: List<WorkoutDayEntity> = emptyList(),
    val exercises: List<ExerciseEntity> = emptyList(),
    val workoutExercises: List<WorkoutExerciseEntity> = emptyList(),
    val routineSets: List<RoutineSetEntity> = emptyList(),
    val sessions: List<WorkoutSessionEntity> = emptyList(),
    val performedExercises: List<PerformedExerciseEntity> = emptyList(),
    val workoutSets: List<WorkoutSetEntity> = emptyList(),
    val meals: List<MealEntity> = emptyList(),
    val foodItems: List<FoodItemEntity> = emptyList(),
    val recipes: List<RecipeEntity> = emptyList(),
    val recipeIngredients: List<RecipeIngredientEntity> = emptyList(),
    val mealItems: List<MealItemEntity> = emptyList(),
    val activeWorkoutSessions: List<ActiveWorkoutSessionEntity> = emptyList(),
    val activeWorkoutDrafts: List<ActiveWorkoutDraftEntity> = emptyList(),
    val activeWorkoutCollapsedExercises: List<ActiveWorkoutCollapsedExerciseEntity> = emptyList(),
    val activeWorkoutSets: List<ActiveWorkoutSetEntity> = emptyList(),
    val workoutLogEvents: List<WorkoutLogEventEntity> = emptyList(),
    val workoutLogEventSets: List<WorkoutLogEventSetEntity> = emptyList(),
    val measurements: List<BodyMeasurementEntity> = emptyList(),
    val schemaParityGaps: Set<RoomSchemaParityGap> = emptySet(),
)

enum class RoomSchemaParityGap {
    FOOD_ITEMS,
    RECIPES,
    RECIPE_INGREDIENTS,
    LOGGED_MEAL_ITEMS,
    ACTIVE_WORKOUT_SESSION,
    WORKOUT_LOG_EVENTS,
}

interface JsonRoomImportSink {
    suspend fun importTransaction(
        plan: JsonRoomImportPlan,
        mirrorRun: RoomMirrorImportRun? = null,
    ): RoomMirrorImportReport
}

class RoomJsonImportSink(
    private val database: TrainIqDatabase,
    private val failAfterCoreTables: Boolean = false,
) : JsonRoomImportSink {
    override suspend fun importTransaction(
        plan: JsonRoomImportPlan,
        mirrorRun: RoomMirrorImportRun?,
    ): RoomMirrorImportReport =
        database.withTransaction {
            val dao = database.dao()
            val staleRows = if (mirrorRun == null) 0 else dao.mirrorRowCount()
            if (mirrorRun != null) dao.clearMirrorTables()
            plan.profile?.let { dao.upsertUserProfile(it) }
            dao.insertRoutines(plan.routines)
            dao.insertWorkoutDays(plan.days)
            dao.insertExercises(plan.exercises)
            dao.insertWorkoutExercises(plan.workoutExercises)
            dao.insertRoutineSets(plan.routineSets)
            if (failAfterCoreTables) error("Forced import failure after core tables")
            dao.importWorkoutSessions(plan.sessions)
            dao.insertPerformedExercises(plan.performedExercises)
            dao.importWorkoutSets(plan.workoutSets)
            dao.importMeals(plan.meals)
            dao.insertFoodItems(plan.foodItems)
            dao.insertRecipes(plan.recipes)
            dao.insertRecipeIngredients(plan.recipeIngredients)
            dao.insertMealItems(plan.mealItems)
            dao.insertActiveWorkoutSessions(plan.activeWorkoutSessions)
            dao.insertActiveWorkoutDrafts(plan.activeWorkoutDrafts)
            dao.insertActiveWorkoutCollapsedExercises(plan.activeWorkoutCollapsedExercises)
            dao.insertActiveWorkoutSets(plan.activeWorkoutSets)
            dao.insertWorkoutLogEvents(plan.workoutLogEvents)
            dao.insertWorkoutLogEventSets(plan.workoutLogEventSets)
            dao.importMeasurements(plan.measurements)
            val importedRows = if (mirrorRun == null) plan.importedRowCount() else dao.mirrorRowCount()
            val mismatchCount = abs(plan.importedRowCount() - importedRows)
            mirrorRun?.let {
                dao.insertMirrorImportRun(
                    RoomMirrorImportRunEntity(
                        generationId = it.generationId,
                        sourceFingerprint = it.sourceFingerprint,
                        startedAt = it.startedAt,
                        finishedAt = it.finishedAt,
                        status = "SUCCESS",
                        schemaVersion = TrainIqDatabaseVersion,
                        expectedRowCount = plan.importedRowCount(),
                        importedRowCount = importedRows,
                        staleRowCount = staleRows,
                        mismatchCount = mismatchCount,
                        jsonAuthoritative = true,
                        roomAuthoritative = false,
                    ),
                )
            }
            RoomMirrorImportReport(
                generationId = mirrorRun?.generationId,
                expectedRowCount = plan.importedRowCount(),
                importedRowCount = importedRows,
                staleRowsRemoved = staleRows,
                mismatchCount = mismatchCount,
            )
        }
}

class JsonRoomImportCoordinator(
    private val planner: JsonRoomImportPlanner,
    private val sink: JsonRoomImportSink,
) {
    suspend fun tryImport(
        sourceJson: String,
        fallbackState: TrainIqStorageState,
    ): JsonRoomImportOutcome = runCatching {
        val plan = planner.plan(sourceJson)
        sink.importTransaction(plan)
        JsonRoomImportOutcome.Imported(plan = plan)
    }.getOrElse { throwable ->
        JsonRoomImportOutcome.Failed(
            sourceJson = sourceJson,
            fallbackState = fallbackState,
            cause = throwable,
        )
    }
}

data class RoomMirrorImportRun(
    val generationId: String,
    val sourceFingerprint: String,
    val startedAt: Long,
    val finishedAt: Long,
)

data class RoomMirrorImportReport(
    val generationId: String?,
    val expectedRowCount: Int,
    val importedRowCount: Int,
    val staleRowsRemoved: Int,
    val mismatchCount: Int,
)

fun JsonRoomImportPlan.importedRowCount(): Int =
    (if (profile == null) 0 else 1) +
        routines.size +
        days.size +
        exercises.size +
        workoutExercises.size +
        routineSets.size +
        sessions.size +
        performedExercises.size +
        workoutSets.size +
        meals.size +
        foodItems.size +
        recipes.size +
        recipeIngredients.size +
        mealItems.size +
        activeWorkoutSessions.size +
        activeWorkoutDrafts.size +
        activeWorkoutCollapsedExercises.size +
        activeWorkoutSets.size +
        workoutLogEvents.size +
        workoutLogEventSets.size +
        measurements.size

private const val TrainIqDatabaseVersion = 11

sealed interface JsonRoomImportOutcome {
    val roomTrusted: Boolean

    data class Imported(
        val plan: JsonRoomImportPlan,
    ) : JsonRoomImportOutcome {
        override val roomTrusted: Boolean = true
    }

    data class Failed(
        val sourceJson: String,
        val fallbackState: TrainIqStorageState,
        val cause: Throwable,
    ) : JsonRoomImportOutcome {
        override val roomTrusted: Boolean = false
    }
}
