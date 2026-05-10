package com.trainiq.data.repository

import com.trainiq.ai.services.GoalAdvisorService
import com.trainiq.ai.services.MealAnalysisService
import com.trainiq.ai.services.RoutineGeneratorService
import com.trainiq.ai.services.WeeklyReportService
import com.trainiq.ai.services.WorkoutDebriefService
import com.trainiq.ai.services.fallbackWorkoutDebriefResult
import com.trainiq.analytics.AnalyticsEngine
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
import com.trainiq.core.util.energyBalanceValueText
import com.trainiq.core.util.toReadableDate
import com.trainiq.core.util.todayEpochMillis
import com.trainiq.data.datasource.HealthConnectDataSource
import com.trainiq.data.local.FoodItemStorage
import com.trainiq.data.local.ActiveWorkoutDraftStorage
import com.trainiq.data.local.ActiveWorkoutSessionStorage
import com.trainiq.data.local.ActiveWorkoutSetStorage
import com.trainiq.data.local.LoggedMealItemStorage
import com.trainiq.data.local.LoggedMealStorage
import com.trainiq.data.local.RecipeIngredientStorage
import com.trainiq.data.local.RecipeStorage
import com.trainiq.data.local.TrainIqStorageState
import com.trainiq.data.local.WorkoutLogEventStorage
import com.trainiq.data.mapper.parseSetType
import com.trainiq.data.mapper.toDomain
import com.trainiq.domain.model.BodyMeasurement
import com.trainiq.domain.model.ActiveWorkoutSession
import com.trainiq.domain.model.ActiveWorkoutSetDraft
import com.trainiq.domain.model.ActiveWorkoutSetEntry
import com.trainiq.domain.model.BiologicalSex
import com.trainiq.domain.model.ChartPoint
import com.trainiq.domain.model.CoachOverview
import com.trainiq.domain.model.Exercise
import com.trainiq.domain.model.ExerciseHistory
import com.trainiq.domain.model.ExerciseHistorySession
import com.trainiq.domain.model.ExerciseHistorySet
import com.trainiq.domain.model.ExerciseRank
import com.trainiq.domain.model.ExerciseRankProgress
import com.trainiq.domain.model.ExerciseStats
import com.trainiq.domain.model.FoodItem
import com.trainiq.domain.model.FoodSourceType
import com.trainiq.domain.model.GeneratedDay
import com.trainiq.domain.model.GeneratedExercise
import com.trainiq.domain.model.GeneratedRoutine
import com.trainiq.domain.model.GoalAdvice
import com.trainiq.domain.model.HealthConnectStatus
import com.trainiq.domain.model.HomeDashboard
import com.trainiq.domain.model.LoggedMeal
import com.trainiq.domain.model.LoggedMealItem
import com.trainiq.domain.model.LoggedMealItemType
import com.trainiq.domain.model.LoggedSet
import com.trainiq.domain.model.MealAnalysisResult
import com.trainiq.domain.model.MealScanItem
import com.trainiq.domain.model.MealType
import com.trainiq.domain.model.NutritionFacts
import com.trainiq.domain.model.NutritionOverview
import com.trainiq.domain.model.ProgressOverview
import com.trainiq.domain.model.ProgressionSuggestion
import com.trainiq.domain.model.Recipe
import com.trainiq.domain.model.RecipeIngredient
import com.trainiq.domain.model.RoutineSet
import com.trainiq.domain.model.SetType
import com.trainiq.domain.model.UserProfile
import com.trainiq.domain.model.WeeklyReportResult
import com.trainiq.domain.model.WorkoutDay
import com.trainiq.domain.model.WorkoutDebrief
import com.trainiq.domain.model.WorkoutDebriefSource
import com.trainiq.domain.model.WorkoutCompletionExercise
import com.trainiq.domain.model.WorkoutCompletionResult
import com.trainiq.domain.model.WorkoutCompletionSet
import com.trainiq.domain.model.WorkoutCompletionSummary
import com.trainiq.domain.model.WorkoutLogEventType
import com.trainiq.domain.model.WorkoutLoggingSummary
import com.trainiq.domain.model.WorkoutOverview
import com.trainiq.domain.model.WorkoutSyncStatus
import com.trainiq.domain.model.buildEnergyBalance
import com.trainiq.domain.model.buildGoalBaseline
import com.trainiq.domain.model.estimateStrengthTrainingCalories
import com.trainiq.domain.model.nutritionForGrams
import com.trainiq.domain.model.rounded
import com.trainiq.domain.model.suggestMealType
import com.trainiq.domain.repository.CoachRepository
import com.trainiq.domain.repository.HomeRepository
import com.trainiq.domain.repository.MealEntryRequest
import com.trainiq.domain.repository.MealEntryType
import com.trainiq.domain.repository.NutritionRepository
import com.trainiq.domain.repository.ProgressRepository
import com.trainiq.domain.repository.WorkoutRepository
import javax.inject.Inject
import javax.inject.Singleton
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class TrainIqDataCoordinator @Inject constructor(
    private val runtimeStore: RoomTrainIqRuntimeStore,
    private val healthConnectDataSource: HealthConnectDataSource,
    private val analyticsEngine: AnalyticsEngine,
    private val mealAnalysisService: MealAnalysisService,
    private val workoutDebriefService: WorkoutDebriefService,
    private val goalAdvisorService: GoalAdvisorService,
    private val weeklyReportService: WeeklyReportService,
    private val routineGeneratorService: RoutineGeneratorService,
    private val progressionSuggestionCalculator: WorkoutProgressionSuggestionCalculator,
    private val exerciseLibrarySeeder: ExerciseLibrarySeeder,
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val scannedMealResult = MutableStateFlow<MealAnalysisResult?>(null)
    private val _cachedSteps = MutableStateFlow(0)

    private val snapshotState: StateFlow<RepositorySnapshot> = combine(runtimeStore.state, scannedMealResult) { state, scanned ->
        RepositorySnapshot(
            profile = state.profile?.toDomain(),
            routines = state.routines,
            days = state.days,
            exercises = state.exercises,
            workoutExercises = state.workoutExercises,
            routineSets = state.routineSets,
            sessions = state.sessions,
            performedExercises = state.performedExercises,
            sets = state.workoutSets,
            foods = state.foods.map(::mapFood),
            recipes = buildRecipes(state.foods, state.recipes, state.recipeIngredients),
            meals = buildMeals(state.meals, state.mealItems),
            measurements = state.measurements.map { it.toDomain() },
            scannedMealResult = scanned,
        )
    }.stateIn(scope, SharingStarted.Eagerly, RepositorySnapshot())

    init {
        scope.launch {
            delay(3_000L)
            exerciseLibrarySeeder.ensureSeeded()
        }
    }

    fun observeDashboard(): Flow<HomeDashboard> = combine(snapshotState, _cachedSteps) { snapshot, steps ->
        val activeRoutine = buildActiveRoutine(snapshot)
        val nextWorkout = activeRoutine?.days?.minByOrNull { it.orderIndex }
        val todaysMeals = snapshot.meals.filter { it.timestamp >= todayEpochMillis() }
        val todaysNutrition = todaysMeals.fold(NutritionFacts.Zero) { acc, meal -> acc + meal.totalNutrition }
        val profile = snapshot.profile
        val todaysWorkoutCalories = snapshot.sessions
            .filter { normalizeToDay(it.date) == todayEpochMillis() }
            .sumOf { it.caloriesBurned }
        HomeDashboard(
            profile = profile,
            energyBalance = profile?.let {
                buildEnergyBalance(
                    profile = it,
                    caloriesIn = todaysNutrition.calories,
                    steps = steps,
                    workoutCalories = todaysWorkoutCalories,
                )
            },
            calorieTarget = profile?.calorieTarget ?: 0,
            calorieProgress = todaysNutrition.calories.toInt(),
            proteinProgress = todaysNutrition.protein.toInt(),
            proteinTarget = profile?.proteinTarget ?: 0,
            carbsProgress = todaysNutrition.carbs.toInt(),
            carbsTarget = profile?.carbsTarget ?: 0,
            fatProgress = todaysNutrition.fat.toInt(),
            fatTarget = profile?.fatTarget ?: 0,
            todaysWorkoutCalories = todaysWorkoutCalories,
            steps = steps.takeIf { it > 0 },
            nextWorkout = nextWorkout,
            streak = computeStreak(snapshot.sessions, snapshot.meals),
            aiInsight = buildDashboardInsight(snapshot, nextWorkout),
        )
    }

    suspend fun getHealthConnectStatus(): HealthConnectStatus {
        val status = healthConnectDataSource.getStatus()
        _cachedSteps.value = when (status.state) {
            com.trainiq.domain.model.HealthConnectState.CONNECTED,
            com.trainiq.domain.model.HealthConnectState.NO_DATA -> status.metrics?.stepsToday ?: 0
            else -> 0
        }
        return status
    }

    suspend fun refreshDashboardData() {
        _cachedSteps.value = healthConnectDataSource.getTodayStepsLive()
    }

    fun observeWorkoutOverview(): Flow<WorkoutOverview> = snapshotState.map(::buildWorkoutOverview)

    fun observeWorkoutLoggingSummary(dayId: Long): Flow<WorkoutLoggingSummary> =
        runtimeStore.state.map { state -> state.workoutLoggingSummary(dayId = dayId, now = System.currentTimeMillis()) }

    fun observeExerciseHistory(exerciseId: Long): Flow<ExerciseHistory> =
        snapshotState.map { snapshot -> buildExerciseHistory(snapshot, exerciseId) }

    suspend fun getWorkoutDay(dayId: Long): WorkoutDay? = buildWorkoutDay(snapshotState.value, dayId)

    suspend fun getProgressionSuggestions(dayId: Long): List<ProgressionSuggestion> = withContext(Dispatchers.IO) {
        val snapshot = snapshotState.value
        progressionSuggestionCalculator.calculate(
            day = buildWorkoutDay(snapshot, dayId),
            sessions = snapshot.sessions,
            sets = snapshot.sets,
        )
    }

    suspend fun getNextWorkoutDay(): WorkoutDay? =
        buildWorkoutOverview(snapshotState.value).activeRoutine?.days?.minByOrNull { it.orderIndex }

    suspend fun getOrStartActiveWorkoutSession(
        dayId: Long,
        initialDrafts: Map<Long, ActiveWorkoutSetDraft>,
    ): ActiveWorkoutSession {
        var result: ActiveWorkoutSession? = null
        runtimeStore.update { state ->
            val mutation = ActiveWorkoutSessionMutations.startOrResume(
                state = state,
                dayId = dayId,
                initialDrafts = initialDrafts,
                now = System.currentTimeMillis(),
            )
            result = mutation.active.toDomain()
            mutation.state
        }
        return requireNotNull(result)
    }

    suspend fun updateActiveWorkoutDraft(exerciseId: Long, draft: ActiveWorkoutSetDraft): ActiveWorkoutSession? =
        withContext(Dispatchers.IO) {
            val active = runtimeStore.state.value.activeWorkoutSession ?: return@withContext null
            val now = System.currentTimeMillis()
            val updated = active.copy(
                updatedAt = now,
                drafts = active.drafts.toMutableMap().apply { put(exerciseId, draft.toStorage()) },
            )
            runtimeStore.updateActiveWorkoutDraft(
                sessionId = updated.sessionId,
                exerciseId = exerciseId,
                draft = requireNotNull(updated.drafts[exerciseId]),
                updatedAt = updated.updatedAt,
            )
            updated.toDomain()
        }

    suspend fun logActiveWorkoutSet(
        dayId: Long,
        set: LoggedSet,
        draft: ActiveWorkoutSetDraft,
        restSeconds: Int,
    ): ActiveWorkoutSession {
        val mutation = withContext(Dispatchers.IO) {
            val mutation = ActiveWorkoutSessionMutations.logSet(
                state = runtimeStore.state.value,
                dayId = dayId,
                set = set,
                draft = draft,
                restSeconds = restSeconds,
                now = System.currentTimeMillis(),
            )
            val loggedSet = requireNotNull(mutation.active.loggedSets.lastOrNull()) {
                "Active workout set logging did not produce a stored set."
            }
            val event = requireNotNull(mutation.state.workoutLogEvents.lastOrNull()) {
                "Active workout set logging did not produce a workout log event."
            }
            runtimeStore.logActiveWorkoutSet(
                active = mutation.active,
                set = loggedSet,
                draft = requireNotNull(mutation.active.drafts[loggedSet.activeKey]),
                event = event,
            )
            mutation
        }
        return mutation.active.toDomain()
    }

    suspend fun updateActiveWorkoutSet(
        setId: Long,
        set: LoggedSet,
        draft: ActiveWorkoutSetDraft,
        restSeconds: Int,
    ): ActiveWorkoutSession? {
        val now = System.currentTimeMillis()
        val active = runtimeStore.state.value.activeWorkoutSession ?: return null
        val updatedSet = active.loggedSets.firstOrNull { it.id == setId }?.let { existing ->
            existing.copy(
                exerciseId = set.exerciseId,
                performedExerciseId = set.performedExerciseId.takeIf { it > 0L } ?: existing.performedExerciseId,
                sourceWorkoutExerciseId = set.sourceWorkoutExerciseId ?: existing.sourceWorkoutExerciseId,
                weight = set.weight,
                reps = set.reps,
                rpe = set.rpe,
                repsInReserve = set.repsInReserve,
                setType = set.setType,
                restSeconds = restSeconds.coerceAtLeast(0),
                completed = true,
                loggedAt = now,
            )
        } ?: return null
        val updated = active.updateSetById(setId = setId, set = updatedSet, now = now)
            .copy(
                drafts = active.drafts.toMutableMap().apply {
                    put(updatedSet.activeKey, draft.toStorage())
                },
                restTimerEndsAt = if (restSeconds > 0) now + restSeconds * 1_000L else null,
                restTimerTotalSeconds = restSeconds.coerceAtLeast(0),
            )
        runtimeStore.updateActiveWorkoutSet(
            active = updated,
            set = updatedSet,
            draft = requireNotNull(updated.drafts[updatedSet.activeKey]),
        )
        return updated.toDomain()
    }

    suspend fun updateActiveWorkoutSetType(setId: Long, setType: SetType): ActiveWorkoutSession? {
        val active = runtimeStore.state.value.activeWorkoutSession ?: return null
        if (active.loggedSets.none { it.id == setId }) return null
        val now = System.currentTimeMillis()
        val updated = active.updateSetTypeById(setId = setId, setType = setType, now = now)
        runtimeStore.updateActiveWorkoutSetType(
            sessionId = updated.sessionId,
            setId = setId,
            setType = setType,
            updatedAt = updated.updatedAt,
        )
        return updated.toDomain()
    }

    suspend fun deleteActiveWorkoutSet(setId: Long): ActiveWorkoutSession? {
        val active = runtimeStore.state.value.activeWorkoutSession ?: return null
        if (active.loggedSets.none { it.id == setId }) return null
        val now = System.currentTimeMillis()
        val updated = active.deleteSetById(setId = setId, now = now)
        runtimeStore.deleteActiveWorkoutSet(
            sessionId = updated.sessionId,
            setId = setId,
            updatedAt = updated.updatedAt,
        )
        return updated.toDomain()
    }

    suspend fun undoWorkoutLogEvent(eventId: Long): ActiveWorkoutSession? {
        val updated = runtimeStore.state.value.undoWorkoutSetEvent(
            eventId = eventId,
            now = System.currentTimeMillis(),
        )
        val active = updated.activeWorkoutSession ?: return null
        val undoEvent = updated.workoutLogEvents.lastOrNull {
            it.type == WorkoutLogEventType.UNDO_SET && it.targetEventId == eventId
        } ?: return active.toDomain()
        runtimeStore.undoActiveWorkoutLogEvent(active = active, undoEvent = undoEvent)
        return active.toDomain()
    }

    suspend fun setActiveWorkoutCollapsed(exerciseId: Long, collapsed: Boolean): ActiveWorkoutSession? {
        val active = runtimeStore.state.value.activeWorkoutSession ?: return null
        val now = System.currentTimeMillis()
        val updated = active.copy(
            updatedAt = now,
            collapsedExerciseIds = if (collapsed) {
                active.collapsedExerciseIds + exerciseId
            } else {
                active.collapsedExerciseIds - exerciseId
            },
        )
        runtimeStore.setActiveWorkoutCollapsedExercise(
            sessionId = updated.sessionId,
            exerciseId = exerciseId,
            collapsed = collapsed,
            updatedAt = updated.updatedAt,
        )
        return updated.toDomain()
    }

    suspend fun updateActiveWorkoutRestTimer(endsAt: Long?, totalSeconds: Int): ActiveWorkoutSession? =
        withContext(Dispatchers.IO) {
            val active = runtimeStore.state.value.activeWorkoutSession ?: return@withContext null
            val now = System.currentTimeMillis()
            val updated = active.copy(
                updatedAt = now,
                restTimerEndsAt = endsAt,
                restTimerTotalSeconds = if (endsAt == null) 0 else totalSeconds.coerceAtLeast(0),
            )
            runtimeStore.updateActiveWorkoutRestTimer(
                sessionId = updated.sessionId,
                endsAt = updated.restTimerEndsAt,
                totalSeconds = updated.restTimerTotalSeconds,
                updatedAt = updated.updatedAt,
            )
            updated.toDomain()
        }

    suspend fun finishActiveWorkout(dayId: Long): WorkoutCompletionResult {
        val active = runtimeStore.state.value.activeWorkoutSession?.takeIf { it.dayId == dayId }
        val now = System.currentTimeMillis()
        val durationSeconds = active?.let {
            activeWorkoutDurationSeconds(startedAt = it.startedAt, now = now)
        } ?: 1L
        val result = finishWorkout(
            dayId = dayId,
            durationSeconds = durationSeconds,
            loggedSets = active?.loggedSets.orEmpty().map { it.toDomain().toLoggedSet() },
            activeSessionId = active?.sessionId,
            activeStartedAt = active?.startedAt,
        )
        return result
    }

    suspend fun getWorkoutCompletionSummary(sessionId: Long): WorkoutCompletionSummary? = withContext(Dispatchers.IO) {
        val state = runtimeStore.state.value
        val session = state.sessions.firstOrNull { it.id == sessionId && it.completed && it.status == "COMPLETED" }
            ?: return@withContext null
        buildWorkoutCompletionSummary(
            session = session,
            routines = state.routines,
            days = state.days,
            exercises = state.exercises,
            workoutExercises = state.workoutExercises,
            performedExercises = state.performedExercises,
            sets = state.workoutSets,
            fallbackDebrief = session.toStoredDebrief(),
        )
    }

    suspend fun discardActiveWorkout(dayId: Long) {
        withContext(Dispatchers.IO) {
            val active = runtimeStore.state.value.activeWorkoutSession?.takeIf { it.dayId == dayId }
                ?: return@withContext
            runtimeStore.discardActiveWorkoutSession(active.sessionId)
        }
    }

    suspend fun setActiveRoutine(routineId: Long) {
        runtimeStore.update { state ->
            state.copy(routines = state.routines.map { it.copy(active = it.id == routineId) })
        }
    }

    suspend fun finishWorkout(dayId: Long, durationSeconds: Long, loggedSets: List<LoggedSet>): WorkoutDebrief =
        finishWorkout(dayId = dayId, durationSeconds = durationSeconds, loggedSets = loggedSets, activeSessionId = null, activeStartedAt = null).debrief

    private suspend fun finishWorkout(
        dayId: Long,
        durationSeconds: Long,
        loggedSets: List<LoggedSet>,
        activeSessionId: Long?,
        activeStartedAt: Long?,
    ): WorkoutCompletionResult {
        if (loggedSets.isEmpty()) {
            return WorkoutCompletionResult(
                sessionId = 0L,
                debrief = WorkoutDebrief(
                summary = "Geen sets gelogd.",
                progressionFeedback = "Log minimaal een set om voortgang op te slaan.",
                recommendation = "Voeg tijdens je training sets toe voordat je afrondt.",
                nextSessionFocus = "Huidige gewichten vasthouden",
                recoveryScore = 75,
                intensitySignal = "MAINTAIN",
                source = WorkoutDebriefSource.LOCAL_FALLBACK,
                ),
            )
        }

        val beforeSnapshot = snapshotState.value
        val current = runtimeStore.state.value
        val now = System.currentTimeMillis()
        val sessionId = activeSessionId?.takeIf { it > 0L } ?: ((current.sessions.maxOfOrNull { it.id } ?: 0L) + 1L)
        val startedAt = activeStartedAt?.takeIf { it > 0L } ?: (now - durationSeconds * 1_000L).coerceAtLeast(0L)
        val newSession = WorkoutSessionEntity(
            id = sessionId,
            date = startedAt,
            duration = durationSeconds,
            caloriesBurned = estimateStrengthTrainingCalories(durationSeconds),
            routineId = beforeSnapshot.days.firstOrNull { it.id == dayId }?.routineId,
            workoutDayId = dayId,
            startedAt = startedAt,
            endedAt = now,
            status = "COMPLETED",
            completed = true,
        )
        val performedExercises = current.ensurePerformedExercisesForCompletedSets(dayId, sessionId, loggedSets)
        val newSets = loggedSets.mapIndexed { index, set ->
            val performedExerciseId = set.performedExerciseId.takeIf { it > 0L }
                ?: performedExercises.firstOrNull { it.sessionId == sessionId && it.exerciseId == set.exerciseId }?.id
                ?: 0L
            WorkoutSetEntity(
                id = (current.workoutSets.maxOfOrNull { it.id } ?: 0L) + index + 1L,
                sessionId = sessionId,
                exerciseId = set.exerciseId,
                weight = set.weight,
                reps = set.reps,
                rpe = set.rpe,
                repsInReserve = set.repsInReserve,
                performedExerciseId = performedExerciseId,
                setType = set.setType.name,
                restSeconds = set.restSeconds,
                orderIndex = set.orderIndex.takeIf { it > 0 } ?: index,
                completed = set.completed,
                loggedAt = completedWorkoutSetLoggedAt(set = set, fallback = now + index),
                completedAt = now + index,
            )
        }
        val debriefSets = loggedSets.filter { it.setType.isProgressionType() }.ifEmpty { loggedSets }
        val currentVolume = debriefSets.sumOf { it.weight * it.reps }
        val comparison = buildWorkoutProgressComparison(
            dayId = dayId,
            routineId = newSession.routineId,
            startedAt = startedAt,
            activeSessionId = activeSessionId,
            currentSets = debriefSets,
            sessions = beforeSnapshot.sessions,
            sets = beforeSnapshot.sets,
            days = beforeSnapshot.days,
            exercises = beforeSnapshot.exercises,
        )
        val progression = comparison?.progressionPercent
        val distribution = buildWorkoutDay(beforeSnapshot, dayId)?.exercises
            ?.groupBy { it.exercise.muscleGroup }
            ?.map { "${it.key} ${it.value.size}" }
            ?.joinToString()
            .orEmpty()
        val avgRpe = debriefSets.map { it.rpe }.average().takeIf { !it.isNaN() }?.toFloat() ?: 0f
        val exerciseNameById = buildWorkoutDay(beforeSnapshot, dayId)
            ?.exercises
            ?.associate { it.exercise.id to it.exercise.name }
            .orEmpty() + beforeSnapshot.exercises.associate { it.id to it.name }
        val topExercises = debriefSets
            .sortedByDescending { it.weight * it.reps }
            .take(3)
            .joinToString { set ->
                val exerciseName = exerciseNameById[set.exerciseId] ?: "Oefening ${set.exerciseId}"
                workoutDebriefTopSetText(exerciseName = exerciseName, weightLabel = formatWeight(set.weight), reps = set.reps)
            }
            .ifBlank { workoutDebriefEmptyTopSetsText() }
        val sevenDaysAgo = System.currentTimeMillis() - (7 * 86_400_000L)
        val weeklyFrequency = (beforeSnapshot.sessions + newSession)
            .filter { it.date >= sevenDaysAgo }
            .map { normalizeToDay(it.date) }
            .distinct()
            .count()
        val localDebrief = fallbackWorkoutDebriefResult(currentVolume, progression)
        runtimeStore.finishActiveWorkoutSession(
            session = newSession.withDebrief(localDebrief),
            performedExercises = performedExercises,
            sets = newSets,
            activeSessionId = activeSessionId,
        )
        scope.launch {
            val refreshedDebrief = workoutDebriefService.generateWorkoutDebrief(
                totalVolume = currentVolume,
                progression = progression,
                comparisonSummary = comparison?.summary ?: "Nog geen eerdere vergelijkbare training gevonden.",
                distribution = distribution,
                avgRpe = avgRpe,
                topExercises = topExercises,
                weeklyFrequency = weeklyFrequency,
            )
            runtimeStore.updateWorkoutSessionDebrief(sessionId = sessionId, debrief = refreshedDebrief)
        }
        return WorkoutCompletionResult(sessionId = sessionId, debrief = localDebrief)
    }

    suspend fun createRoutine(name: String, description: String) {
        runtimeStore.createRoutine(name = name, description = description)
    }

    suspend fun updateRoutine(routineId: Long, name: String, description: String) {
        runtimeStore.updateRoutine(routineId = routineId, name = name, description = description)
    }

    suspend fun deleteRoutine(routineId: Long) {
        runtimeStore.deleteRoutine(routineId)
    }

    suspend fun searchExercises(query: String): List<Exercise> = withContext(Dispatchers.IO) {
        val normalizedQuery = query.trim()
        snapshotState.value.exercises
            .asSequence()
            .filter { exercise ->
                normalizedQuery.isBlank() ||
                    exercise.name.contains(normalizedQuery, ignoreCase = true) ||
                    exercise.muscleGroup.contains(normalizedQuery, ignoreCase = true) ||
                    exercise.equipment.contains(normalizedQuery, ignoreCase = true)
            }
            .sortedBy { it.name.lowercase() }
            .map { it.toDomain() }
            .toList()
    }

    suspend fun reorderExercises(dayId: Long, orderedIds: List<Long>) {
        runtimeStore.reorderExercises(dayId = dayId, orderedIds = orderedIds)
    }

    suspend fun setSupersetGroup(workoutExerciseIds: List<Long>, groupId: Long?) {
        val ids = workoutExerciseIds.toSet()
        if (ids.isEmpty()) return
        runtimeStore.update { state ->
            state.copy(
                workoutExercises = state.workoutExercises.map { exercise ->
                    if (exercise.id in ids) {
                        exercise.copy(supersetGroupId = groupId)
                    } else {
                        exercise
                    }
                },
            )
        }
    }

    suspend fun replaceExerciseInPlan(workoutExerciseId: Long, newExerciseId: Long) {
        runtimeStore.update { state ->
            state.withExerciseReplacedInPlan(workoutExerciseId, newExerciseId)
        }
    }

    suspend fun replaceExerciseInActiveWorkout(workoutExerciseId: Long, newExerciseId: Long): ActiveWorkoutSession? {
        var result: ActiveWorkoutSession? = null
        val now = System.currentTimeMillis()
        runtimeStore.update { state ->
            val updated = state.withExerciseReplacedInActiveWorkout(workoutExerciseId, newExerciseId, now)
            result = updated.activeWorkoutSession?.toDomain()
            updated
        }
        return result
    }

    suspend fun updateWorkoutExercisePlan(
        workoutExerciseId: Long,
        targetSets: Int,
        repRange: String,
        restSeconds: Int,
        targetWeightKg: Double,
        targetRpe: Double,
        setType: SetType,
    ) {
        runtimeStore.update { state ->
            val updatedExercises = state.workoutExercises.map { exercise ->
                if (exercise.id == workoutExerciseId) {
                    exercise.copy(
                        targetSets = targetSets.coerceAtLeast(1),
                        repRange = repRange.ifBlank { "8-12" },
                        restSeconds = restSeconds.coerceAtLeast(0),
                        targetWeightKg = targetWeightKg.coerceAtLeast(0.0),
                        targetRpe = targetRpe.coerceIn(0.0, 10.0),
                        setType = setType.name,
                    )
                } else {
                    exercise
                }
            }
            state.copy(
                workoutExercises = state.workoutExercises.map { exercise ->
                    if (exercise.id == workoutExerciseId) {
                        exercise.copy(
                            targetSets = targetSets.coerceAtLeast(1),
                            repRange = repRange.ifBlank { "8-12" },
                            restSeconds = restSeconds.coerceAtLeast(0),
                            targetWeightKg = targetWeightKg.coerceAtLeast(0.0),
                            targetRpe = targetRpe.coerceIn(0.0, 10.0),
                            setType = setType.name,
                        )
                    } else {
                        exercise
                    }
                },
            ).copy(workoutExercises = updatedExercises)
                .withRoutineSetCountSynced(
                    workoutExerciseId = workoutExerciseId,
                    targetSets = targetSets.coerceAtLeast(1),
                    repRange = repRange.ifBlank { "8-12" },
                    restSeconds = restSeconds.coerceAtLeast(0),
                    targetWeightKg = targetWeightKg.coerceAtLeast(0.0),
                    targetRpe = targetRpe.coerceIn(0.0, 10.0),
                    setType = setType,
                )
        }
    }

    suspend fun addSetToExercise(workoutExerciseId: Long) {
        replaceTargetedRoutineSetsForExercise(workoutExerciseId) { state ->
            state.withRoutineSetAdded(workoutExerciseId)
        }
    }

    suspend fun updateRoutineSet(set: RoutineSet) {
        updateTargetedRoutineSet(set.id) { set.toEntity() }
    }

    suspend fun updateRoutineSetType(setId: Long, setType: SetType) {
        updateTargetedRoutineSet(setId) { it.copy(setType = setType.name) }
    }

    suspend fun updateRoutineSetReps(setId: Long, targetReps: Int) {
        updateTargetedRoutineSet(setId) { it.copy(targetReps = targetReps.coerceAtLeast(0)) }
    }

    suspend fun updateRoutineSetWeight(setId: Long, targetWeightKg: Double) {
        updateTargetedRoutineSet(setId) { it.copy(targetWeightKg = targetWeightKg.coerceAtLeast(0.0)) }
    }

    suspend fun updateRoutineSetRestTime(setId: Long, restSeconds: Int) {
        updateTargetedRoutineSet(setId) { it.copy(restSeconds = restSeconds.coerceAtLeast(0)) }
    }

    private suspend fun updateTargetedRoutineSet(
        setId: Long,
        transform: (RoutineSetEntity) -> RoutineSetEntity,
    ) {
        val updated = runtimeStore.state.value.updateRoutineSet(setId = setId, transform = transform)
        val updatedSet = updated.routineSets.firstOrNull { it.id == setId } ?: return
        val updatedExercise = updated.workoutExercises.firstOrNull { it.id == updatedSet.workoutExerciseId } ?: return
        runtimeStore.updateRoutineSet(set = updatedSet, workoutExercise = updatedExercise)
    }

    suspend fun deleteRoutineSet(setId: Long) {
        val workoutExerciseId = runtimeStore.state.value.routineSets
            .firstOrNull { it.id == setId }
            ?.workoutExerciseId
            ?: return
        replaceTargetedRoutineSetsForExercise(workoutExerciseId) { state ->
            state.copy(routineSets = state.routineSets.filterNot { it.id == setId })
                .renumberRoutineSets(workoutExerciseId)
                .withWorkoutExerciseTargetsSynced(workoutExerciseId)
        }
    }

    suspend fun moveRoutineSet(workoutExerciseId: Long, orderedSetIds: List<Long>) {
        replaceTargetedRoutineSetsForExercise(workoutExerciseId) { state ->
            val requestedOrder = orderedSetIds.distinct().withIndex().associate { it.value to it.index }
            val exerciseSets = state.routineSets.filter { it.workoutExerciseId == workoutExerciseId }
            val fallbackOrder = exerciseSets
                .filterNot { it.id in requestedOrder }
                .sortedWith(compareBy<RoutineSetEntity> { it.orderIndex }.thenBy { it.id })
                .withIndex()
                .associate { it.value.id to requestedOrder.size + it.index }
            val nextOrder = requestedOrder + fallbackOrder
            state.copy(
                routineSets = state.routineSets.map { set ->
                    if (set.workoutExerciseId == workoutExerciseId) {
                        set.copy(orderIndex = nextOrder[set.id] ?: set.orderIndex)
                    } else {
                        set
                    }
                },
            ).withWorkoutExerciseTargetsSynced(workoutExerciseId)
        }
    }

    private suspend fun replaceTargetedRoutineSetsForExercise(
        workoutExerciseId: Long,
        transform: (TrainIqStorageState) -> TrainIqStorageState,
    ) {
        val updated = transform(runtimeStore.state.value)
        val updatedExercise = updated.workoutExercises.firstOrNull { it.id == workoutExerciseId } ?: return
        val updatedSets = updated.routineSets
            .filter { it.workoutExerciseId == workoutExerciseId }
            .sortedWith(compareBy<RoutineSetEntity> { it.orderIndex }.thenBy { it.id })
        runtimeStore.replaceRoutineSetsForExercise(
            workoutExerciseId = workoutExerciseId,
            sets = updatedSets,
            workoutExercise = updatedExercise,
        )
    }

    suspend fun addWorkoutDay(routineId: Long, name: String) {
        runtimeStore.update { state ->
            val dayId = (state.days.maxOfOrNull { it.id } ?: 0L) + 1L
            val nextOrder = state.days.count { it.routineId == routineId }
            val sessionName = name.trim().ifBlank { defaultWorkoutSessionName(nextOrder) }
            state.copy(days = state.days + WorkoutDayEntity(dayId, routineId, sessionName, nextOrder))
        }
    }

    suspend fun removeWorkoutDay(dayId: Long) {
        runtimeStore.update { state ->
            state.copy(
                days = state.days.filterNot { it.id == dayId },
                workoutExercises = state.workoutExercises.filterNot { it.dayId == dayId },
                routineSets = state.routineSets.filterNot { routineSet ->
                    state.workoutExercises.any { it.dayId == dayId && it.id == routineSet.workoutExerciseId }
                },
                activeWorkoutSession = state.activeWorkoutSession?.takeUnless { it.dayId == dayId },
            )
        }
    }

    suspend fun addExerciseToDay(
        dayId: Long,
        name: String,
        muscleGroup: String,
        equipment: String,
        targetSets: Int,
        repRange: String,
        restSeconds: Int,
        targetWeightKg: Double,
        targetRpe: Double,
    ) {
        runtimeStore.update { state ->
            state.withExerciseAddedToDay(dayId, name, muscleGroup, equipment, targetSets, repRange, restSeconds, targetWeightKg, targetRpe)
        }
    }

    suspend fun addExerciseToRoutine(
        routineId: Long,
        name: String,
        muscleGroup: String,
        equipment: String,
        targetSets: Int,
        repRange: String,
        restSeconds: Int,
        targetWeightKg: Double,
        targetRpe: Double,
    ) {
        runtimeStore.update { state ->
            state.withExerciseAddedToRoutine(routineId, name, muscleGroup, equipment, targetSets, repRange, restSeconds, targetWeightKg, targetRpe)
        }
    }

    suspend fun removeExerciseFromDay(workoutExerciseId: Long) {
        runtimeStore.update { state ->
            state.withExerciseRemovedFromDay(
                workoutExerciseId = workoutExerciseId,
                now = System.currentTimeMillis(),
            )
        }
    }

    suspend fun deleteWorkoutSession(sessionId: Long) {
        runtimeStore.update { state ->
            state.withWorkoutSessionDeleted(sessionId)
        }
    }

    suspend fun generateAiRoutine(
        daysPerWeek: Int,
        equipment: String,
        targetFocus: String,
        experienceLevel: String,
        sessionDurationMinutes: Int,
        includeDeload: Boolean,
    ): GeneratedRoutine = withContext(Dispatchers.IO) {
        val profile = snapshotState.value.profile
            ?: error(missingProfileForAiRoutineMessage())
        val generated = routineGeneratorService.generateRoutine(
            goal = profile.goal,
            targetFocus = targetFocus.ifBlank { profile.trainingFocus },
            daysPerWeek = daysPerWeek,
            equipment = equipment,
            experienceLevel = experienceLevel,
            sessionDurationMinutes = sessionDurationMinutes,
            includeDeload = includeDeload,
        )

        check(generated.days.isNotEmpty()) {
            generatedRoutineMissingDaysMessage()
        }
        check(generated.days.none { it.exercises.isEmpty() }) {
            generatedRoutineMissingExercisesMessage()
        }
        generated.toDomainGeneratedRoutine()
    }

    suspend fun saveGeneratedRoutine(routine: GeneratedRoutine) = withContext(Dispatchers.IO) {
        check(routine.days.isNotEmpty()) {
            generatedRoutineMissingDaysMessage()
        }
        check(routine.days.none { it.exercises.isEmpty() }) {
            generatedRoutineMissingExercisesMessage()
        }
        runtimeStore.update { state ->
            val routineId = (state.routines.maxOfOrNull { it.id } ?: 0L) + 1L
            val newRoutine = WorkoutRoutineEntity(
                id = routineId,
                name = routine.routineName,
                description = listOf(routine.routineDescription, routine.periodizationNote)
                    .filter { it.isNotBlank() }
                    .joinToString("\n\n"),
                active = state.routines.isEmpty(),
            )

            var nextDayId = (state.days.maxOfOrNull { it.id } ?: 0L) + 1L
            var nextExerciseId = (state.exercises.maxOfOrNull { it.id } ?: 0L) + 1L
            var nextWorkoutExerciseId = (state.workoutExercises.maxOfOrNull { it.id } ?: 0L) + 1L
            var nextRoutineSetId = (state.routineSets.maxOfOrNull { it.id } ?: 0L) + 1L

            val newDays = mutableListOf<WorkoutDayEntity>()
            val newExercises = mutableListOf<ExerciseEntity>()
            val newWorkoutExercises = mutableListOf<WorkoutExerciseEntity>()
            val newRoutineSets = mutableListOf<RoutineSetEntity>()
            // mutable copy so we can track newly added exercises within this transaction
            val allExercises = state.exercises.toMutableList()

            routine.days.forEachIndexed { orderIndex, generatedDay ->
                val dayId = nextDayId++
                newDays += WorkoutDayEntity(
                    id = dayId,
                    routineId = routineId,
                    name = generatedDay.dayName,
                    orderIndex = orderIndex,
                )
                generatedDay.exercises.forEach { generatedExercise ->
                    val existing = allExercises.firstOrNull {
                        it.name.equals(generatedExercise.exerciseName, ignoreCase = true) &&
                            it.equipment.equals(generatedExercise.equipment, ignoreCase = true)
                    }
                    val exerciseId = existing?.id ?: run {
                        val newId = nextExerciseId++
                        val newExercise = ExerciseEntity(
                            id = newId,
                            name = generatedExercise.exerciseName,
                            muscleGroup = generatedExercise.muscleGroup,
                            equipment = generatedExercise.equipment,
                        )
                        newExercises += newExercise
                        allExercises += newExercise
                        newId
                    }
                    val workoutExerciseId = nextWorkoutExerciseId++
                    newWorkoutExercises += WorkoutExerciseEntity(
                        id = workoutExerciseId,
                        dayId = dayId,
                        exerciseId = exerciseId,
                        targetSets = generatedExercise.targetSets,
                        repRange = generatedExercise.repRange,
                        restSeconds = generatedExercise.restSeconds,
                        orderIndex = newWorkoutExercises.count { it.dayId == dayId },
                    )
                    repeat(generatedExercise.targetSets.coerceAtLeast(0)) { setIndex ->
                        newRoutineSets += RoutineSetEntity(
                            id = nextRoutineSetId++,
                            workoutExerciseId = workoutExerciseId,
                            orderIndex = setIndex,
                            setType = SetType.NORMAL.name,
                            targetReps = parseTargetRepTarget(generatedExercise.repRange),
                            restSeconds = generatedExercise.restSeconds.coerceAtLeast(0),
                        )
                    }
                }
            }

            state.copy(
                routines = state.routines + newRoutine,
                days = state.days + newDays,
                exercises = state.exercises + newExercises,
                workoutExercises = state.workoutExercises + newWorkoutExercises,
                routineSets = state.routineSets + newRoutineSets,
            )
        }
    }

    fun observeNutritionOverview(): Flow<NutritionOverview> =
        combine(snapshotState, _cachedSteps) { snapshot, steps -> buildNutritionOverview(snapshot, steps) }

    suspend fun analyzeMealPhoto(path: String, context: String, capturedAtMillis: Long): MealAnalysisResult {
        scannedMealResult.value = null
        return try {
            val result = mealAnalysisService.analyzeMealImage(
                path = path,
                userContext = context,
                capturedAtMillis = capturedAtMillis,
            )
            scannedMealResult.value = result
            result
        } catch (error: Throwable) {
            scannedMealResult.value = null
            throw error
        }
    }

    fun clearLastScanResult() {
        scannedMealResult.value = null
    }

    suspend fun saveFoodItem(
        id: Long?,
        name: String,
        barcode: String?,
        caloriesPer100g: Double,
        proteinPer100g: Double,
        carbsPer100g: Double,
        fatPer100g: Double,
        sourceType: FoodSourceType,
    ): FoodItem {
        val now = System.currentTimeMillis()
        var saved: FoodItemStorage? = null
        runtimeStore.update { state ->
            val existing = state.foods.firstOrNull { it.id == id }
            val duplicateBarcode = barcode?.trim()?.takeIf { it.isNotBlank() }?.let { code ->
                state.foods.firstOrNull { it.barcode == code && it.id != id }
            }
            val foodId = existing?.id ?: duplicateBarcode?.id ?: ((state.foods.maxOfOrNull { it.id } ?: 0L) + 1L)
            val storage = FoodItemStorage(
                id = foodId,
                name = name.trim(),
                barcode = barcode?.trim()?.takeIf { it.isNotBlank() },
                caloriesPer100g = caloriesPer100g,
                proteinPer100g = proteinPer100g,
                carbsPer100g = carbsPer100g,
                fatPer100g = fatPer100g,
                sourceType = sourceType,
                createdAt = existing?.createdAt ?: duplicateBarcode?.createdAt ?: now,
                updatedAt = now,
            )
            saved = storage
            state.copy(foods = state.foods.filterNot { it.id == foodId } + storage)
        }
        return mapFood(requireNotNull(saved))
    }

    suspend fun saveRecipe(
        id: Long?,
        name: String,
        notes: String?,
        totalCookedGrams: Double?,
        ingredients: List<Pair<Long, Double>>,
    ): Recipe {
        val current = runtimeStore.state.value
        val recipeId = id ?: ((current.recipes.maxOfOrNull { it.id } ?: 0L) + 1L)
        val now = System.currentTimeMillis()
        val recipe = RecipeStorage(
            id = recipeId,
            name = name.trim(),
            notes = notes?.trim()?.takeIf { it.isNotBlank() },
            totalCookedGrams = totalCookedGrams,
            createdAt = current.recipes.firstOrNull { it.id == recipeId }?.createdAt ?: now,
            updatedAt = now,
        )
        val ingredientStartId = current.recipeIngredients.maxOfOrNull { it.id } ?: 0L
        val ingredientStorage = ingredients.mapIndexed { index, (foodId, gramsUsed) ->
            RecipeIngredientStorage(
                id = ingredientStartId + index + 1L,
                recipeId = recipeId,
                foodItemId = foodId,
                gramsUsed = gramsUsed,
            )
        }
        runtimeStore.update { state ->
            state.copy(
                recipes = state.recipes.filterNot { it.id == recipeId } + recipe,
                recipeIngredients = state.recipeIngredients.filterNot { it.recipeId == recipeId } + ingredientStorage,
            )
        }
        return buildRecipes(runtimeStore.state.value.foods, runtimeStore.state.value.recipes, runtimeStore.state.value.recipeIngredients)
            .first { it.id == recipeId }
    }

    suspend fun saveMeal(
        id: Long?,
        mealType: MealType,
        name: String,
        notes: String?,
        items: List<MealEntryRequest>,
    ): Long {
        val snapshot = snapshotState.value
        val current = runtimeStore.state.value
        val mealId = id ?: ((current.meals.maxOfOrNull { it.id } ?: 0L) + 1L)
        val timestamp = snapshot.meals.firstOrNull { it.id == mealId }?.timestamp ?: System.currentTimeMillis()
        val mealStorage = LoggedMealStorage(
            id = mealId,
            timestamp = timestamp,
            mealType = mealType,
            name = name.trim().ifBlank { mealType.label },
            notes = notes?.trim()?.takeIf { it.isNotBlank() },
        )
        val startItemId = current.mealItems.maxOfOrNull { it.id } ?: 0L
        val mealItems = buildMealItemSnapshots(
            mealId = mealId,
            startItemId = startItemId,
            requests = items,
            foods = snapshot.foods,
            recipes = snapshot.recipes,
        )
        require(mealItems.isNotEmpty()) { "Deze maaltijd bevat geen beschikbare producten of recepten meer." }
        runtimeStore.saveMeal(meal = mealStorage, items = mealItems)
        scannedMealResult.value = null
        return mealId
    }

    suspend fun deleteMeal(mealId: Long) {
        runtimeStore.deleteMeal(mealId)
    }

    suspend fun deleteFood(foodId: Long) {
        runtimeStore.update { state ->
            if (state.recipeIngredients.any { it.foodItemId == foodId }) {
                throw IllegalStateException("Product wordt nog gebruikt in recepten.")
            }
            state.copy(
                foods = state.foods.filterNot { it.id == foodId },
            )
        }
    }

    suspend fun deleteRecipe(recipeId: Long) {
        runtimeStore.update { state ->
            state.copy(
                recipes = state.recipes.filterNot { it.id == recipeId },
                recipeIngredients = state.recipeIngredients.filterNot { it.recipeId == recipeId },
            )
        }
    }

    fun observeProgressOverview(): Flow<ProgressOverview> = snapshotState.map(::buildProgressOverview)

    suspend fun addMeasurement(weight: Double, bodyFat: Double, muscleMass: Double) {
        val measurementId = (runtimeStore.state.value.measurements.maxOfOrNull { it.id } ?: 0L) + 1L
        runtimeStore.addMeasurement(
            BodyMeasurementEntity(
                id = measurementId,
                date = System.currentTimeMillis(),
                weight = weight,
                bodyFat = bodyFat,
                muscleMass = muscleMass,
            )
        )
    }

    suspend fun deleteMeasurement(measurementId: Long) {
        runtimeStore.deleteMeasurement(measurementId)
    }

    fun observeCoachOverview(): Flow<CoachOverview> = snapshotState.mapLatest { snapshot ->
        val progress = buildProgressOverview(snapshot)
        val nutrition = buildNutritionOverview(snapshot)
        val profile = snapshot.profile
        CoachOverview(
            weeklyReport = buildWeeklySummary(snapshot, progress),
            trainingInsights = buildTrainingInsights(snapshot, progress),
            nutritionCoachMessage = buildNutritionMessage(profile, nutrition),
            profile = profile,
        )
    }

    suspend fun generateGoalAdvice(
        height: Double,
        weight: Double,
        bodyFat: Double,
        age: Int,
        sex: BiologicalSex,
        activityLevel: String,
        goal: String,
    ): GoalAdvice = goalAdvisorService.generateGoalAdvice(
        height = height,
        weight = weight,
        bodyFat = bodyFat,
        age = age,
        sex = sex,
        activityLevel = activityLevel,
        goal = goal,
    )

    suspend fun generateWeeklyReport(): WeeklyReportResult {
        val snapshot = snapshotState.value
        val progress = buildProgressOverview(snapshot)
        return if (snapshot.sessions.isEmpty() && snapshot.meals.isEmpty()) {
            WeeklyReportResult(
                summary = "Log je eerste training of maaltijd om een wekelijkse coachsamenvatting te krijgen.",
                wins = emptyList(),
                risks = emptyList(),
                nextWeekFocus = "Voltooi eerst een training of maaltijdlog.",
                source = com.trainiq.domain.model.WeeklyReportSource.LOCAL_FALLBACK,
            )
        } else {
            weeklyReportService.generateWeeklyReport(
                volume = progress.currentWeekVolume(),
                weightTrend = progress.weightTrend.lastOrNull()?.value ?: snapshot.profile?.weight ?: 0.0,
                adherence = calculateAdherence(snapshot),
            )
        }
    }

    fun observeUserProfile(): Flow<UserProfile?> = snapshotState.map { it.profile }

    suspend fun saveProfile(profile: UserProfile) {
        runtimeStore.saveProfile(
            UserProfileEntity(
                id = profile.id,
                name = profile.name,
                age = profile.age,
                sex = profile.sex.name,
                height = profile.height,
                weight = profile.weight,
                bodyFat = profile.bodyFat,
                activityLevel = profile.activityLevel,
                goal = profile.goal,
                calorieTarget = profile.calorieTarget,
                proteinTarget = profile.proteinTarget,
                carbsTarget = profile.carbsTarget,
                fatTarget = profile.fatTarget,
                trainingFocus = profile.trainingFocus,
            ),
        )
    }

    private fun buildWeeklySummary(snapshot: RepositorySnapshot, progress: ProgressOverview): String {
        if (snapshot.sessions.isEmpty() && snapshot.meals.isEmpty()) {
            return "Log je eerste training of maaltijd om je wekelijkse samenvatting te ontgrendelen."
        }
        val volume = progress.currentWeekVolume().toInt()
        val adherence = calculateAdherence(snapshot)
        return "Deze week: $volume kg trainingsvolume, beste geschatte 1RM ${formatSummaryWeight(progress.estimatedOneRepMax)} kg en $adherence% consistentie."
    }

    private fun buildTrainingInsights(snapshot: RepositorySnapshot, progress: ProgressOverview): List<String> {
        val insights = mutableListOf<String>()
        val activeRoutine = buildWorkoutOverview(snapshot).activeRoutine
        if (activeRoutine == null) {
            insights += "Kies of maak een actieve routine zodat je volgende training gepland kan worden."
        } else {
            insights += "Actieve routine: ${activeRoutine.name} met ${trainingDayCountText(activeRoutine.days.size)}."
        }
        if (snapshot.sessions.isEmpty()) {
            insights += "Er is nog geen trainingshistorie. Rond een workout af om volume en herstel te volgen."
        } else {
            insights += "Je beste geschatte 1RM staat op ${formatSummaryWeight(progress.estimatedOneRepMax)} kg."
            insights += "De huidige fatigue index is ${"%.2f".format(progress.fatigueIndex)}."
        }
        return insights
    }

    private fun buildNutritionMessage(profile: UserProfile?, nutrition: NutritionOverview): String {
        if (profile == null) {
            return "Sla eerst je profiel en doel op om calorie- en macrocoaching te activeren."
        }
        if (nutrition.meals.isEmpty()) {
            return "Voeg je eerste maaltijd toe om te zien hoe je intake zich verhoudt tot ${profile.calorieTarget} kcal."
        }
        val energyBalance = nutrition.energyBalance
        val remainingCalories = profile.calorieTarget - nutrition.todaysCalories.toInt()
        return if (energyBalance == null) {
            "Je zit vandaag nog $remainingCalories kcal onder je doel. Richt je vooral op eiwitten en volwaardige koolhydraten."
        } else if (remainingCalories > 0) {
            "Je intake ligt nog ${remainingCalories.coerceAtLeast(0)} kcal onder je target. TEF is ${energyBalance.tefCalories} kcal en je energiebalans staat op ${energyBalanceValueText(energyBalance.balance)}."
        } else {
            "Je caloriedoel is vandaag gehaald. Houd je eiwitten hoog; je actuele energiebalans staat op ${energyBalanceValueText(energyBalance.balance)}."
        }
    }

    private fun buildWorkoutOverview(snapshot: RepositorySnapshot): WorkoutOverview {
        val dayMap = snapshot.days.associate { it.id to buildWorkoutDay(snapshot, it.id) }
        val routines = snapshot.routines.map { routine ->
            routine.toDomain(
                days = snapshot.days
                    .filter { it.routineId == routine.id }
                    .sortedBy { it.orderIndex }
                    .mapNotNull { dayMap[it.id] },
            )
        }
        val sessionVolumes = snapshot.sets.groupBy { it.sessionId }.mapValues { analyticsEngine.trainingVolume(it.value) }
        val history = snapshot.sessions
            .filter { it.completed && it.status == "COMPLETED" }
            .sortedByDescending { it.date }
            .map { it.toDomain(sessionVolumes[it.id] ?: 0.0) }
        return WorkoutOverview(
            activeRoutine = routines.firstOrNull { it.active },
            routines = routines,
            exercises = snapshot.exercises.map { it.toDomain() }.sortedBy { it.name },
            history = history,
        )
    }

    private fun buildActiveRoutine(snapshot: RepositorySnapshot): com.trainiq.domain.model.WorkoutRoutine? {
        val routine = snapshot.routines.firstOrNull { it.active } ?: return null
        val activeDays = snapshot.days
            .filter { it.routineId == routine.id }
            .sortedBy { it.orderIndex }
            .mapNotNull { buildWorkoutDay(snapshot, it.id) }
        return routine.toDomain(days = activeDays)
    }

    private fun buildWorkoutDay(snapshot: RepositorySnapshot, dayId: Long): WorkoutDay? {
        val day = snapshot.days.firstOrNull { it.id == dayId } ?: return null
        val exercisePlans = snapshot.workoutExercises
            .filter { it.dayId == dayId }
            .sortedWith(compareBy<WorkoutExerciseEntity> { it.orderIndex }.thenBy { it.id })
            .mapNotNull { workoutExercise ->
                snapshot.exercises.firstOrNull { it.id == workoutExercise.exerciseId }
                    ?.toDomain()
                    ?.let { exercise ->
                        val routineSets = snapshot.routineSets
                            .filter { it.workoutExerciseId == workoutExercise.id }
                            .sortedWith(compareBy<RoutineSetEntity> { it.orderIndex }.thenBy { it.id })
                            .map { it.toDomain() }
                        workoutExercise.toDomain(exercise, routineSets)
                    }
            }
        return day.toDomain(exercisePlans)
    }

    private fun buildExerciseHistory(snapshot: RepositorySnapshot, exerciseId: Long): ExerciseHistory {
        val exercise = snapshot.exercises.firstOrNull { it.id == exerciseId }?.toDomain()
        val sessionsById = snapshot.sessions
            .filter { it.completed && it.status == "COMPLETED" }
            .associateBy { it.id }
        val historySessions = snapshot.sets
            .filter { it.exerciseId == exerciseId && it.completed && it.reps > 0 && it.weight >= 0.0 }
            .groupBy { it.sessionId }
            .mapNotNull { (sessionId, sets) ->
                val session = sessionsById[sessionId] ?: return@mapNotNull null
                val performedSets = sets
                    .sortedWith(compareBy<WorkoutSetEntity> { it.orderIndex }.thenBy { it.id })
                    .map { set ->
                        ExerciseHistorySet(
                            orderIndex = set.orderIndex,
                            reps = set.reps,
                            weightKg = set.weight,
                            setType = parseSetType(set.setType),
                            restSeconds = set.restSeconds,
                            rpe = set.rpe,
                            repsInReserve = set.repsInReserve,
                            completed = set.completed,
                        )
                    }
                val totalVolume = performedSets.sumOf { it.weightKg * it.reps }
                val bestWeight = performedSets.maxOfOrNull { it.weightKg } ?: 0.0
                val bestE1rm = performedSets.maxOfOrNull { estimateSimpleOneRepMax(it.weightKg, it.reps) } ?: 0.0
                val avgRpe = performedSets.map { it.rpe }.filter { it > 0.0 }.average().takeIf { !it.isNaN() }
                ExerciseHistorySession(
                    sessionId = sessionId,
                    startedAt = session.startedAt.takeIf { it > 0L } ?: session.date,
                    endedAt = session.endedAt.takeIf { it > 0L } ?: session.date,
                    durationSeconds = session.duration,
                    totalVolume = totalVolume,
                    bestWeightKg = bestWeight,
                    bestEstimatedOneRepMax = bestE1rm,
                    averageRpe = avgRpe,
                    sets = performedSets,
                )
            }
            .sortedByDescending { it.startedAt }
        val chronological = historySessions.sortedBy { it.startedAt }
        val allSets = historySessions.flatMap { it.sets }
        val highestWeight = allSets.maxOfOrNull { it.weightKg } ?: 0.0
        val mostReps = allSets.maxOfOrNull { it.reps } ?: 0
        val bestE1rm = allSets.maxOfOrNull { estimateSimpleOneRepMax(it.weightKg, it.reps) } ?: 0.0
        val bestSet = allSets.maxByOrNull { estimateSimpleOneRepMax(it.weightKg, it.reps) }
        val latest = historySessions.firstOrNull()
        val previous = historySessions.drop(1).firstOrNull()
        val progressPercent = latest?.totalVolume?.let { latestVolume ->
            previous?.totalVolume?.takeIf { it > 0.0 }?.let { ((latestVolume - it) / it) * 100.0 }
        }
        val stats = ExerciseStats(
            lastPerformedAt = latest?.startedAt,
            completedSessions = historySessions.size,
            totalSets = allSets.size,
            highestWeightKg = highestWeight,
            mostReps = mostReps,
            bestEstimatedOneRepMax = bestE1rm,
            bestSetLabel = bestSet?.let { "${formatWeight(it.weightKg)} kg x ${it.reps}" } ?: "-",
            latestPerformanceLabel = latest?.sets?.joinToString { "${formatWeight(it.weightKg)} kg x ${it.reps}" } ?: "-",
            averageRpe = allSets.map { it.rpe }.filter { it > 0.0 }.average().takeIf { !it.isNaN() },
            totalVolume = historySessions.sumOf { it.totalVolume },
            progressSincePreviousPercent = progressPercent,
        )
        val rank = buildExerciseRank(stats)
        return ExerciseHistory(
            exercise = exercise,
            stats = stats,
            sessions = historySessions,
            volumePoints = chronological.map { ChartPoint(it.startedAt.toReadableDate(), it.totalVolume) },
            bestWeightPoints = chronological.map { ChartPoint(it.startedAt.toReadableDate(), it.bestWeightKg) },
            estimatedOneRepMaxPoints = chronological.map { ChartPoint(it.startedAt.toReadableDate(), it.bestEstimatedOneRepMax) },
            rank = rank,
        )
    }

    private fun buildNutritionOverview(snapshot: RepositorySnapshot, steps: Int = 0): NutritionOverview {
        val todaysMeals = snapshot.meals.filter { it.timestamp >= todayEpochMillis() }.sortedByDescending { it.timestamp }
        val totals = todaysMeals.fold(NutritionFacts.Zero) { acc, meal -> acc + meal.totalNutrition }
        val todaysMealsByType = MealType.entries.associateWith { mealType ->
            todaysMeals.filter { it.mealType == mealType }
        }
        val todaysWorkoutCalories = snapshot.sessions
            .filter { normalizeToDay(it.date) == todayEpochMillis() }
            .sumOf { it.caloriesBurned }
        return NutritionOverview(
            foods = snapshot.foods.sortedBy { it.name.lowercase() },
            recipes = snapshot.recipes.sortedBy { it.name.lowercase() },
            meals = snapshot.meals.sortedByDescending { it.timestamp },
            todaysNutrition = totals,
            todaysCalories = totals.calories,
            todaysProtein = totals.protein,
            todaysCarbs = totals.carbs,
            todaysFat = totals.fat,
            todaysMeals = todaysMeals,
            todaysMealsByType = todaysMealsByType,
            todaysWorkoutCalories = todaysWorkoutCalories,
            energyBalance = snapshot.profile?.let {
                buildEnergyBalance(
                    profile = it,
                    caloriesIn = totals.calories,
                    steps = steps,
                    workoutCalories = todaysWorkoutCalories,
                )
            },
            scannedResult = snapshot.scannedMealResult,
        )
    }

    private fun buildProgressOverview(snapshot: RepositorySnapshot): ProgressOverview {
        return buildProgressOverviewFromHistory(
            measurements = snapshot.measurements,
            sessions = snapshot.sessions,
            sets = snapshot.sets,
            analyticsEngine = analyticsEngine,
        )
    }

    private fun buildDashboardInsight(snapshot: RepositorySnapshot, nextWorkout: WorkoutDay?): String {
        val profile = snapshot.profile
        if (profile == null) {
            return "Vul je profiel en doel in om gepersonaliseerde calorie-, macro- en trainingsadviezen te zien."
        }
        if (nextWorkout == null) {
            return "Maak een actieve routine zodat TrainIQ je volgende workout kan plannen."
        }
        val todaysProtein = snapshot.meals
            .filter { it.timestamp >= todayEpochMillis() }
            .sumOf { it.totalNutrition.protein }
        val proteinGap = profile.proteinTarget - todaysProtein.toInt()
        val todaysWorkoutCalories = snapshot.sessions
            .filter { normalizeToDay(it.date) == todayEpochMillis() }
            .sumOf { it.caloriesBurned }
        return when {
            snapshot.sessions.none { it.completed && it.status == "COMPLETED" } -> "Je bent klaar om te starten. Plan ${nextWorkout.name} als eerste sessie voor je doel '${profile.goal}'."
            proteinGap > 20 -> "Je volgende workout is ${nextWorkout.name}. Voeg vandaag nog ongeveer $proteinGap g eiwit toe en houd rekening met ${todaysWorkoutCalories} kcal krachttraining."
            else -> "Je ligt op koers voor ${profile.goal}. Volgende training: ${nextWorkout.name}. Focus op ${profile.trainingFocus.lowercase()}."
        }
    }

    private fun calculateAdherence(snapshot: RepositorySnapshot): Int {
        val today = todayEpochMillis()
        val last7Days = (0..6).map { today - (it * 86_400_000L) }.toSet()
        val workoutDays = snapshot.sessions.filter { it.completed && it.status == "COMPLETED" }.map { normalizeToDay(it.date) }.toSet()
        val mealDays = snapshot.meals.map { normalizeToDay(it.timestamp) }.toSet()
        val activeDays = last7Days.count { it in workoutDays || it in mealDays }
        return (activeDays / 7.0 * 100).toInt()
    }

    private fun computeStreak(sessions: List<WorkoutSessionEntity>, meals: List<LoggedMeal>): Int {
        val activeDays = (sessions.filter { it.completed && it.status == "COMPLETED" }.map { normalizeToDay(it.date) } + meals.map { normalizeToDay(it.timestamp) }).toSet()
        if (activeDays.isEmpty()) return 0
        var streak = 0
        var day = todayEpochMillis()
        while (day in activeDays) {
            streak += 1
            day -= 86_400_000L
        }
        return streak
    }

    private fun normalizeToDay(timestamp: Long): Long =
        java.time.Instant.ofEpochMilli(timestamp)
            .atZone(java.time.ZoneId.systemDefault())
            .toLocalDate()
            .atStartOfDay(java.time.ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

    private fun parseTargetRepTarget(repRange: String): Int =
        repRange.substringAfter('-', repRange).trim().toIntOrNull()
            ?: repRange.filter(Char::isDigit).toIntOrNull()
            ?: 0

    private fun formatWeight(weight: Double): String =
        if (weight % 1.0 == 0.0) weight.toInt().toString() else "%.1f".format(weight)

    private fun estimateSimpleOneRepMax(weight: Double, reps: Int): Double =
        if (reps <= 0 || weight < 0.0) 0.0 else weight * (1.0 + reps / 30.0)

    private fun buildExerciseRank(stats: ExerciseStats): ExerciseRankProgress {
        val score = stats.bestEstimatedOneRepMax + kotlin.math.ln(stats.totalVolume + 1.0) + (stats.completedSessions * 2.0)
        val current = ExerciseRank.entries.lastOrNull { score >= it.threshold } ?: ExerciseRank.BEGINNER
        val next = ExerciseRank.entries.firstOrNull { it.threshold > score }
        val previousThreshold = current.threshold
        val progress = next?.let { ((score - previousThreshold) / (it.threshold - previousThreshold)).toFloat().coerceIn(0f, 1f) } ?: 1f
        val pointsToNext = next?.let { (it.threshold - score).coerceAtLeast(0.0) } ?: 0.0
        val message = next?.let {
            "Nog ${formatWeight(pointsToNext)} punten tot ${it.label}. Voeg volume toe of verbeter je beste set."
        } ?: "Elite bereikt. Blijf consistente sterke sessies loggen."
        return ExerciseRankProgress(
            rank = current,
            score = score,
            nextRank = next,
            progressToNext = progress,
            pointsToNext = pointsToNext,
            message = message,
        )
    }

    private fun mapFood(storage: FoodItemStorage): FoodItem = FoodItem(
        id = storage.id,
        name = storage.name,
        barcode = storage.barcode,
        caloriesPer100g = storage.caloriesPer100g,
        proteinPer100g = storage.proteinPer100g,
        carbsPer100g = storage.carbsPer100g,
        fatPer100g = storage.fatPer100g,
        sourceType = storage.sourceType,
        createdAt = storage.createdAt,
        updatedAt = storage.updatedAt,
    )

    private fun buildRecipes(
        foods: List<FoodItemStorage>,
        recipes: List<RecipeStorage>,
        ingredients: List<RecipeIngredientStorage>,
    ): List<Recipe> {
        val foodsById = foods.associateBy { it.id }.mapValues { mapFood(it.value) }
        return recipes.map { recipe ->
            val recipeIngredients = ingredients
                .filter { it.recipeId == recipe.id }
                .mapNotNull { ingredient ->
                    val food = foodsById[ingredient.foodItemId] ?: return@mapNotNull null
                    RecipeIngredient(
                        id = ingredient.id,
                        recipeId = recipe.id,
                        foodItemId = food.id,
                        foodName = food.name,
                        gramsUsed = ingredient.gramsUsed,
                        nutrition = food.nutritionForGrams(ingredient.gramsUsed),
                    )
                }
            Recipe(
                id = recipe.id,
                name = recipe.name,
                notes = recipe.notes,
                ingredients = recipeIngredients,
                totalCookedGrams = recipe.totalCookedGrams,
                totalNutrition = recipeIngredients.fold(NutritionFacts.Zero) { acc, ingredient -> acc + ingredient.nutrition }.rounded(),
                createdAt = recipe.createdAt,
                updatedAt = recipe.updatedAt,
            )
        }
    }

    private fun buildMeals(
        meals: List<LoggedMealStorage>,
        items: List<LoggedMealItemStorage>,
    ): List<LoggedMeal> = meals.map { meal ->
        val mealItems = items
            .filter { it.mealId == meal.id }
            .map { item ->
                LoggedMealItem(
                    id = item.id,
                    mealId = item.mealId,
                    itemType = item.itemType,
                    referenceId = item.referenceId,
                    name = item.name,
                    gramsUsed = item.gramsUsed,
                    nutritionSnapshot = NutritionFacts(item.calories, item.protein, item.carbs, item.fat).rounded(),
                    notes = item.notes,
                )
            }
        LoggedMeal(
            id = meal.id,
            timestamp = meal.timestamp,
            mealType = meal.mealType,
            name = meal.name,
            notes = meal.notes,
            items = mealItems,
            totalNutrition = mealItems.fold(NutritionFacts.Zero) { acc, item -> acc + item.nutritionSnapshot }.rounded(),
        )
    }

    private suspend fun mutateActiveWorkout(
        transform: (ActiveWorkoutSessionStorage, Long) -> ActiveWorkoutSessionStorage,
    ): ActiveWorkoutSession? {
        var result: ActiveWorkoutSession? = null
        runtimeStore.update { state ->
            val active = state.activeWorkoutSession ?: return@update state
            val next = transform(active, System.currentTimeMillis())
            result = next.toDomain()
            state.copy(activeWorkoutSession = next)
        }
        return result
    }

    private data class RepositorySnapshot(
        val profile: UserProfile? = null,
        val routines: List<WorkoutRoutineEntity> = emptyList(),
        val days: List<WorkoutDayEntity> = emptyList(),
        val exercises: List<ExerciseEntity> = emptyList(),
        val workoutExercises: List<WorkoutExerciseEntity> = emptyList(),
        val routineSets: List<RoutineSetEntity> = emptyList(),
        val sessions: List<WorkoutSessionEntity> = emptyList(),
        val performedExercises: List<PerformedExerciseEntity> = emptyList(),
        val sets: List<WorkoutSetEntity> = emptyList(),
        val foods: List<FoodItem> = emptyList(),
        val recipes: List<Recipe> = emptyList(),
        val meals: List<LoggedMeal> = emptyList(),
        val measurements: List<BodyMeasurement> = emptyList(),
        val scannedMealResult: MealAnalysisResult? = null,
    )

}

internal const val WorkoutUndoWindowMillis: Long = 15_000L

internal fun TrainIqStorageState.appendWorkoutSetEvent(
    dayId: Long,
    sessionId: Long,
    set: ActiveWorkoutSetStorage,
    now: Long,
    undoWindowMillis: Long = WorkoutUndoWindowMillis,
): TrainIqStorageState {
    val active = activeWorkoutSession ?: ActiveWorkoutSessionStorage(
        sessionId = sessionId,
        dayId = dayId,
        startedAt = now,
    )
    val previousLoggedSets = active.loggedSets
    val event = WorkoutLogEventStorage(
        id = (workoutLogEvents.maxOfOrNull { it.id } ?: 0L) + 1L,
        dayId = dayId,
        sessionId = sessionId,
        type = WorkoutLogEventType.ADD_SET,
        syncStatus = WorkoutSyncStatus.PENDING,
        createdAt = now,
        undoExpiresAt = now + undoWindowMillis,
        set = set,
        previousLoggedSets = previousLoggedSets,
    )
    return copy(
        activeWorkoutSession = active.copy(
            sessionId = sessionId,
            dayId = dayId,
            updatedAt = now,
            loggedSets = previousLoggedSets + set,
        ),
        workoutLogEvents = workoutLogEvents + event,
    )
}

internal fun TrainIqStorageState.undoWorkoutSetEvent(eventId: Long, now: Long): TrainIqStorageState {
    val event = workoutLogEvents.firstOrNull { it.id == eventId && it.type == WorkoutLogEventType.ADD_SET }
        ?: return this
    if ((event.undoExpiresAt ?: Long.MIN_VALUE) < now) return this
    if (workoutLogEvents.any { it.type == WorkoutLogEventType.UNDO_SET && it.targetEventId == eventId }) return this
    val active = activeWorkoutSession ?: return this
    val undoEvent = WorkoutLogEventStorage(
        id = (workoutLogEvents.maxOfOrNull { it.id } ?: 0L) + 1L,
        dayId = event.dayId,
        sessionId = event.sessionId,
        type = WorkoutLogEventType.UNDO_SET,
        syncStatus = WorkoutSyncStatus.PENDING,
        createdAt = now,
        targetEventId = eventId,
        previousLoggedSets = active.loggedSets,
    )
    return copy(
        activeWorkoutSession = active.copy(
            updatedAt = now,
            loggedSets = event.previousLoggedSets,
        ),
        workoutLogEvents = workoutLogEvents + undoEvent,
    )
}

internal fun TrainIqStorageState.workoutLoggingSummary(dayId: Long, now: Long): WorkoutLoggingSummary {
    val pendingCount = workoutLogEvents.count { it.dayId == dayId && it.syncStatus == WorkoutSyncStatus.PENDING }
    val undoneEventIds = workoutLogEvents
        .filter { it.type == WorkoutLogEventType.UNDO_SET }
        .mapNotNull { it.targetEventId }
        .toSet()
    val undoable = workoutLogEvents
        .asReversed()
        .firstOrNull { event ->
            event.dayId == dayId &&
                event.type == WorkoutLogEventType.ADD_SET &&
                event.id !in undoneEventIds &&
                (event.undoExpiresAt ?: Long.MIN_VALUE) >= now
        }
    return WorkoutLoggingSummary(
        pendingCount = pendingCount,
        lastUndoableEventId = undoable?.id,
        lastUndoableExpiresAt = undoable?.undoExpiresAt,
    )
}

internal fun TrainIqStorageState.finalizeWorkoutLogEventsForCompletedSession(sessionId: Long): TrainIqStorageState =
    copy(workoutLogEvents = workoutLogEvents.filterNot { it.sessionId == sessionId })

internal fun completedWorkoutSetLoggedAt(set: LoggedSet, fallback: Long): Long =
    set.loggedAt.takeIf { it > 0L } ?: fallback

internal fun workoutDebriefTopSetText(exerciseName: String, weightLabel: String, reps: Int): String =
    "$exerciseName $weightLabel kg x $reps"

internal fun workoutDebriefEmptyTopSetsText(): String = "Geen topsets gelogd"

internal fun buildWorkoutCompletionSummary(
    session: WorkoutSessionEntity,
    routines: List<WorkoutRoutineEntity>,
    days: List<WorkoutDayEntity>,
    exercises: List<ExerciseEntity>,
    workoutExercises: List<WorkoutExerciseEntity>,
    performedExercises: List<PerformedExerciseEntity>,
    sets: List<WorkoutSetEntity>,
    fallbackDebrief: WorkoutDebrief,
): WorkoutCompletionSummary {
    val sessionSets = sets
        .filter { it.sessionId == session.id && it.completed }
        .sortedWith(compareBy<WorkoutSetEntity> { it.performedExerciseId }.thenBy { it.orderIndex }.thenBy { it.id })
    val sessionPerformed = performedExercises
        .filter { it.sessionId == session.id }
        .sortedBy { it.orderIndex }
    val day = session.workoutDayId?.let { dayId -> days.firstOrNull { it.id == dayId } }
    val routine = session.routineId?.let { routineId -> routines.firstOrNull { it.id == routineId } }
        ?: day?.let { workoutDay -> routines.firstOrNull { it.id == workoutDay.routineId } }
    val exerciseById = exercises.associateBy { it.id }
    val plannedOrderByExerciseId = workoutExercises
        .filter { it.dayId == session.workoutDayId }
        .associate { it.exerciseId to it.orderIndex }
    val performedByExerciseId = sessionPerformed.associateBy { it.exerciseId }
    val setsByExercise = sessionSets.groupBy { it.exerciseId }
    val exerciseSummaries = setsByExercise.map { (exerciseId, exerciseSets) ->
        val exercise = exerciseById[exerciseId]
        val orderedSets = exerciseSets.sortedWith(compareBy<WorkoutSetEntity> { it.orderIndex }.thenBy { it.id })
        WorkoutCompletionExercise(
            exerciseId = exerciseId,
            name = exercise?.name ?: "Oefening $exerciseId",
            muscleGroup = exercise?.muscleGroup.orEmpty(),
            sets = orderedSets.mapIndexed { index, set ->
                WorkoutCompletionSet(
                    setNumber = index + 1,
                    weightKg = set.weight,
                    reps = set.reps,
                    rpe = set.rpe,
                    restSeconds = set.restSeconds,
                    setType = parseSetType(set.setType),
                    completed = set.completed,
                )
            },
            totalVolume = orderedSets.sumOf { it.weight * it.reps },
            bestSetLabel = orderedSets.maxWithOrNull(compareBy<WorkoutSetEntity> { it.weight }.thenBy { it.reps })?.let { set ->
                "${formatSummaryWeight(set.weight)} kg x ${set.reps}"
            }.orEmpty(),
        )
    }.sortedWith(
        compareBy<WorkoutCompletionExercise> {
            performedByExerciseId[it.exerciseId]?.orderIndex ?: plannedOrderByExerciseId[it.exerciseId] ?: Int.MAX_VALUE
        }.thenBy { it.name },
    )
    val debrief = session.toStoredDebrief().takeIf { it.summary.isNotBlank() } ?: fallbackDebrief
    val strongestSet = sessionSets.maxWithOrNull(compareBy<WorkoutSetEntity> { it.weight }.thenBy { it.reps })
    val workoutName = listOfNotNull(routine?.name, day?.name)
        .filter { it.isNotBlank() }
        .joinToString(" - ")
        .ifBlank { "Krachttraining" }
    return WorkoutCompletionSummary(
        sessionId = session.id,
        workoutName = workoutName,
        startedAt = session.startedAt.takeIf { it > 0L } ?: session.date,
        endedAt = session.endedAt.takeIf { it > 0L } ?: session.date,
        durationSeconds = session.duration,
        exercisesCompleted = exerciseSummaries.size,
        setsLogged = sessionSets.size,
        totalVolume = sessionSets.sumOf { it.weight * it.reps },
        personalBests = 0,
        strongestSetLabel = strongestSet?.let { "${formatSummaryWeight(it.weight)} kg x ${it.reps}" }.orEmpty(),
        debrief = debrief,
        sourceLabel = when (debrief.source) {
            WorkoutDebriefSource.GEMINI_2_5_FLASH -> "Samenvatting gemaakt met Gemini 2.5 Flash"
            WorkoutDebriefSource.LOCAL_FALLBACK -> "Samenvatting gemaakt op basis van je trainingsdata"
        },
        recommendationLabel = when (debrief.intensitySignal.uppercase(Locale.US)) {
            "INCREASE" -> "Verhogen"
            "DELOAD", "DECREASE" -> "Verlagen"
            "REVIEW", "PLATEAU" -> "Review"
            else -> "Vasthouden"
        },
        exercises = exerciseSummaries,
    )
}

private fun WorkoutSessionEntity.withDebrief(debrief: WorkoutDebrief): WorkoutSessionEntity = copy(
    debriefSummary = debrief.summary,
    debriefProgressionFeedback = debrief.progressionFeedback,
    debriefRecommendation = debrief.recommendation,
    debriefNextSessionFocus = debrief.nextSessionFocus,
    debriefRecoveryScore = debrief.recoveryScore,
    debriefIntensitySignal = debrief.intensitySignal,
    debriefWins = debrief.wins.joinToString("\n"),
    debriefRisks = debrief.risks.joinToString("\n"),
    debriefNextLoadTarget = debrief.nextLoadTarget,
    debriefRecoveryAdvice = debrief.recoveryAdvice,
    debriefSource = debrief.source.name,
)

private fun WorkoutSessionEntity.toStoredDebrief(): WorkoutDebrief = WorkoutDebrief(
    summary = debriefSummary,
    progressionFeedback = debriefProgressionFeedback,
    recommendation = debriefRecommendation,
    nextSessionFocus = debriefNextSessionFocus,
    recoveryScore = debriefRecoveryScore.coerceIn(0, 100),
    intensitySignal = debriefIntensitySignal.ifBlank { "MAINTAIN" },
    wins = debriefWins.lines().map { it.trim() }.filter { it.isNotBlank() },
    risks = debriefRisks.lines().map { it.trim() }.filter { it.isNotBlank() },
    nextLoadTarget = debriefNextLoadTarget,
    recoveryAdvice = debriefRecoveryAdvice,
    source = runCatching { WorkoutDebriefSource.valueOf(debriefSource) }.getOrDefault(WorkoutDebriefSource.LOCAL_FALLBACK),
)

private fun formatSummaryWeight(weight: Double): String =
    if (weight % 1.0 == 0.0) weight.toInt().toString() else String.format(Locale.US, "%.1f", weight)

internal data class WorkoutProgressComparison(
    val previousSessionId: Long,
    val previousVolume: Double,
    val currentVolume: Double,
    val progressionPercent: Double,
    val matchedExerciseCount: Int,
    val summary: String,
)

internal fun buildWorkoutProgressComparison(
    dayId: Long,
    routineId: Long?,
    startedAt: Long,
    activeSessionId: Long?,
    currentSets: List<LoggedSet>,
    sessions: List<WorkoutSessionEntity>,
    sets: List<WorkoutSetEntity>,
    days: List<WorkoutDayEntity>,
    exercises: List<ExerciseEntity>,
): WorkoutProgressComparison? {
    val currentExerciseIds = currentSets.map { it.exerciseId }.toSet()
    if (currentExerciseIds.isEmpty()) return null
    val exerciseNameById = exercises.associate { it.id to it.name.trim().lowercase(Locale.US) }
    val currentExerciseNames = currentExerciseIds.mapNotNull { exerciseNameById[it] }.toSet()
    val routineByDayId = days.associate { it.id to it.routineId }
    val candidates = sessions
        .asSequence()
        .filter { it.completed && it.status == "COMPLETED" }
        .filter { it.id != activeSessionId }
        .filter { (it.startedAt.takeIf { value -> value > 0L } ?: it.date) < startedAt }
        .mapNotNull { session ->
            val sessionSets = sets.filter { it.sessionId == session.id && isProgressionSet(it) }
            val sessionExerciseIds = sessionSets.map { it.exerciseId }.toSet()
            val idOverlap = sessionExerciseIds.intersect(currentExerciseIds).size
            val nameOverlap = sessionExerciseIds.mapNotNull { exerciseNameById[it] }.toSet().intersect(currentExerciseNames).size
            val matchedExerciseCount = maxOf(idOverlap, nameOverlap)
            if (matchedExerciseCount == 0) return@mapNotNull null
            val sessionRoutineId = session.routineId ?: session.workoutDayId?.let { routineByDayId[it] }
            val matchScore = when {
                session.workoutDayId == dayId -> 3
                routineId != null && sessionRoutineId == routineId -> 2
                else -> 1
            }
            if (matchScore == 1 && nameOverlap == 0) return@mapNotNull null
            val volume = sessionSets
                .filter { set ->
                    set.exerciseId in currentExerciseIds || exerciseNameById[set.exerciseId] in currentExerciseNames
                }
                .sumOf { it.weight * it.reps }
                .takeIf { it > 0.0 }
                ?: return@mapNotNull null
            PreviousWorkoutCandidate(
                session = session,
                volume = volume,
                matchedExerciseCount = matchedExerciseCount,
                matchScore = matchScore,
            )
        }
        .sortedWith(
            compareByDescending<PreviousWorkoutCandidate> { it.matchScore }
                .thenByDescending { it.session.startedAt.takeIf { value -> value > 0L } ?: it.session.date },
        )
        .toList()
    val previous = candidates.firstOrNull() ?: return null
    val currentVolume = currentSets
        .filter { it.exerciseId in currentExerciseIds || exerciseNameById[it.exerciseId] in currentExerciseNames }
        .sumOf { it.weight * it.reps }
    if (currentVolume <= 0.0 || previous.volume <= 0.0) return null
    val progression = ((currentVolume - previous.volume) / previous.volume) * 100.0
    return WorkoutProgressComparison(
        previousSessionId = previous.session.id,
        previousVolume = previous.volume,
        currentVolume = currentVolume,
        progressionPercent = progression,
        matchedExerciseCount = previous.matchedExerciseCount,
        summary = "Vergelijking met vorige vergelijkbare training: ${formatSummaryWeight(previous.volume)} kg naar ${formatSummaryWeight(currentVolume)} kg (${formatProgressionPercent(progression)}%).",
    )
}

private data class PreviousWorkoutCandidate(
    val session: WorkoutSessionEntity,
    val volume: Double,
    val matchedExerciseCount: Int,
    val matchScore: Int,
)

internal fun TrainIqStorageState.deleteActiveWorkoutSetById(setId: Long, now: Long): TrainIqStorageState =
    copy(
        activeWorkoutSession = activeWorkoutSession?.deleteSetById(setId = setId, now = now),
        workoutLogEvents = workoutLogEvents.filterNot { event ->
            event.type == WorkoutLogEventType.ADD_SET &&
                event.syncStatus == WorkoutSyncStatus.PENDING &&
                event.set?.id == setId
        },
    )

internal fun TrainIqStorageState.updateActiveWorkoutSetTypeById(
    setId: Long,
    setType: SetType,
    now: Long,
): TrainIqStorageState =
    copy(
        activeWorkoutSession = activeWorkoutSession?.updateSetTypeById(setId = setId, setType = setType, now = now),
        workoutLogEvents = workoutLogEvents.map { event ->
            if (event.set?.id == setId) {
                event.copy(set = event.set.copy(setType = setType))
            } else {
                event
            }
        },
    )

internal fun TrainIqStorageState.updateActiveWorkoutSetById(
    setId: Long,
    set: ActiveWorkoutSetStorage,
    now: Long,
): TrainIqStorageState =
    copy(
        activeWorkoutSession = activeWorkoutSession?.updateSetById(setId = setId, set = set, now = now),
        workoutLogEvents = workoutLogEvents.map { event ->
            if (event.set?.id == setId) event.copy(set = set) else event
        },
    )

private fun ActiveWorkoutSessionStorage.deleteSetById(setId: Long, now: Long): ActiveWorkoutSessionStorage =
    copy(
        updatedAt = now,
        loggedSets = loggedSets.filterNot { it.id == setId },
    )

private fun ActiveWorkoutSessionStorage.updateSetTypeById(
    setId: Long,
    setType: SetType,
    now: Long,
): ActiveWorkoutSessionStorage =
    copy(
        updatedAt = now,
        loggedSets = loggedSets.map { set -> if (set.id == setId) set.copy(setType = setType) else set },
    )

private fun ActiveWorkoutSessionStorage.updateSetById(
    setId: Long,
    set: ActiveWorkoutSetStorage,
    now: Long,
): ActiveWorkoutSessionStorage =
    copy(
        updatedAt = now,
        loggedSets = loggedSets.map { existing -> if (existing.id == setId) set.copy(id = setId) else existing },
    )

internal fun defaultWorkoutSessionName(existingSessionCount: Int): String = "Sessie ${existingSessionCount + 1}"

internal const val MaxActiveWorkoutDurationSeconds: Long = 4 * 60 * 60

internal fun activeWorkoutDurationSeconds(startedAt: Long, now: Long): Long {
    if (startedAt <= 0L || now <= startedAt) return 1L
    return ((now - startedAt) / 1_000L).coerceIn(1L, MaxActiveWorkoutDurationSeconds)
}

internal fun formatProgressionPercent(value: Double): String =
    String.format(Locale.forLanguageTag("nl-NL"), "%.1f", value)

internal fun TrainIqStorageState.ensurePerformedExercisesForActiveSession(
    active: ActiveWorkoutSessionStorage,
): List<PerformedExerciseEntity> {
    if (active.sessionId <= 0L) return performedExercises
    val existingForSession = performedExercises.filter { it.sessionId == active.sessionId }
    if (existingForSession.isNotEmpty()) return performedExercises
    val plans = workoutExercises
        .filter { it.dayId == active.dayId }
        .sortedWith(compareBy<WorkoutExerciseEntity> { it.orderIndex }.thenBy { it.id })
    var nextId = (performedExercises.maxOfOrNull { it.id } ?: 0L) + 1L
    val created = plans.mapIndexed { index, plan ->
        PerformedExerciseEntity(
            id = nextId++,
            sessionId = active.sessionId,
            exerciseId = plan.exerciseId,
            sourceWorkoutExerciseId = plan.id,
            orderIndex = index,
        )
    }
    return performedExercises + created
}

internal fun TrainIqStorageState.ensurePerformedExercisesForCompletedSets(
    dayId: Long,
    sessionId: Long,
    loggedSets: List<LoggedSet>,
): List<PerformedExerciseEntity> {
    val existingForSession = performedExercises.filter { it.sessionId == sessionId }
    val plansByExerciseId = workoutExercises
        .filter { it.dayId == dayId }
        .sortedWith(compareBy<WorkoutExerciseEntity> { it.orderIndex }.thenBy { it.id })
        .associateBy { it.exerciseId }
    var nextId = ((performedExercises.filterNot { it.sessionId == sessionId }.maxOfOrNull { it.id } ?: 0L) + 1L)
        .coerceAtLeast((existingForSession.maxOfOrNull { it.id } ?: 0L) + 1L)
    val created = loggedSets
        .map { it.exerciseId }
        .distinct()
        .mapIndexedNotNull { fallbackIndex, exerciseId ->
            if (existingForSession.any { it.exerciseId == exerciseId }) return@mapIndexedNotNull null
            val plan = plansByExerciseId[exerciseId]
            PerformedExerciseEntity(
                id = nextId++,
                sessionId = sessionId,
                exerciseId = exerciseId,
                sourceWorkoutExerciseId = plan?.id,
                orderIndex = plan?.orderIndex ?: fallbackIndex,
            )
        }
    return existingForSession + created
}

internal fun ActiveWorkoutSessionStorage.toDomain() = ActiveWorkoutSession(
    sessionId = sessionId,
    dayId = dayId,
    routineId = routineId,
    startedAt = startedAt,
    updatedAt = updatedAt,
    loggedSets = loggedSets.map { it.toDomain() },
    drafts = drafts.mapValues { it.value.toDomain() },
    collapsedExerciseIds = collapsedExerciseIds,
    restTimerEndsAt = restTimerEndsAt,
    restTimerTotalSeconds = restTimerTotalSeconds,
)

private fun ActiveWorkoutSetStorage.toDomain() = ActiveWorkoutSetEntry(
    id = id,
    exerciseId = exerciseId,
    weight = weight,
    reps = reps,
    rpe = rpe,
    repsInReserve = repsInReserve,
    setType = setType,
    restSeconds = restSeconds,
    orderIndex = orderIndex,
    completed = completed,
    loggedAt = loggedAt,
    performedExerciseId = performedExerciseId,
    sourceWorkoutExerciseId = sourceWorkoutExerciseId,
)

private fun ActiveWorkoutDraftStorage.toDomain() = ActiveWorkoutSetDraft(
    weight = weight,
    reps = reps,
    rpe = rpe,
    setType = setType,
)

internal fun TrainIqStorageState.withWorkoutSessionDeleted(sessionId: Long): TrainIqStorageState =
    copy(
        sessions = sessions.filterNot { it.id == sessionId },
        workoutSets = workoutSets.filterNot { it.sessionId == sessionId },
        performedExercises = performedExercises.filterNot { it.sessionId == sessionId },
        workoutLogEvents = workoutLogEvents.filterNot { it.sessionId == sessionId },
        activeWorkoutSession = activeWorkoutSession?.takeUnless { it.sessionId == sessionId },
    )

internal fun TrainIqStorageState.withExerciseAddedToRoutine(
    routineId: Long,
    name: String,
    muscleGroup: String,
    equipment: String,
    targetSets: Int,
    repRange: String,
    restSeconds: Int,
    targetWeightKg: Double = 0.0,
    targetRpe: Double = 0.0,
): TrainIqStorageState {
    val routineDays = days.filter { it.routineId == routineId }.sortedBy { it.orderIndex }
    val targetDay = routineDays.firstOrNull()
    val stateWithDay = if (targetDay == null) {
        val dayId = (days.maxOfOrNull { it.id } ?: 0L) + 1L
        copy(
            days = days + WorkoutDayEntity(
                id = dayId,
                routineId = routineId,
                name = defaultWorkoutSessionName(routineDays.size),
                orderIndex = routineDays.size,
            ),
        )
    } else {
        this
    }
    val targetDayId = targetDay?.id ?: stateWithDay.days.maxOf { it.id }
    return stateWithDay.withExerciseAddedToDay(targetDayId, name, muscleGroup, equipment, targetSets, repRange, restSeconds, targetWeightKg, targetRpe)
}

internal fun TrainIqStorageState.withExerciseAddedToDay(
    dayId: Long,
    name: String,
    muscleGroup: String,
    equipment: String,
    targetSets: Int,
    repRange: String,
    restSeconds: Int,
    targetWeightKg: Double = 0.0,
    targetRpe: Double = 0.0,
): TrainIqStorageState {
    val existing = exercises.firstOrNull {
        it.name.equals(name, ignoreCase = true) &&
            it.muscleGroup.equals(muscleGroup, ignoreCase = true) &&
            it.equipment.equals(equipment, ignoreCase = true)
    }
    val exerciseId = existing?.id ?: ((exercises.maxOfOrNull { it.id } ?: 0L) + 1L)
    val exercise = existing ?: ExerciseEntity(exerciseId, name, muscleGroup, equipment)
    val workoutExerciseId = (workoutExercises.maxOfOrNull { it.id } ?: 0L) + 1L
    val nextOrder = workoutExercises.count { it.dayId == dayId }
    val nextSetId = (routineSets.maxOfOrNull { it.id } ?: 0L) + 1L
    val newRoutineSets = List(targetSets.coerceAtLeast(0)) { index ->
        RoutineSetEntity(
            id = nextSetId + index,
            workoutExerciseId = workoutExerciseId,
            orderIndex = index,
            setType = SetType.NORMAL.name,
            targetReps = parseRoutineRepTarget(repRange),
            targetWeightKg = targetWeightKg.coerceAtLeast(0.0),
            restSeconds = restSeconds.coerceAtLeast(0),
            targetRpe = targetRpe.coerceIn(0.0, 10.0),
        )
    }
    return copy(
        exercises = if (existing == null) exercises + exercise else exercises,
        workoutExercises = workoutExercises + WorkoutExerciseEntity(
            id = workoutExerciseId,
            dayId = dayId,
            exerciseId = exerciseId,
            targetSets = targetSets,
            repRange = repRange,
            restSeconds = restSeconds,
            targetWeightKg = targetWeightKg,
            targetRpe = targetRpe,
            orderIndex = nextOrder,
        ),
        routineSets = routineSets + newRoutineSets,
    )
}

internal fun TrainIqStorageState.withRoutineSetAdded(workoutExerciseId: Long): TrainIqStorageState {
    val workoutExercise = workoutExercises.firstOrNull { it.id == workoutExerciseId } ?: return this
    val existingSets = routineSets
        .filter { it.workoutExerciseId == workoutExerciseId }
        .sortedWith(compareBy<RoutineSetEntity> { it.orderIndex }.thenBy { it.id })
    val previous = existingSets.lastOrNull()
    val nextSet = previous?.copy(
        id = (routineSets.maxOfOrNull { it.id } ?: 0L) + 1L,
        orderIndex = existingSets.size,
    ) ?: RoutineSetEntity(
        id = (routineSets.maxOfOrNull { it.id } ?: 0L) + 1L,
        workoutExerciseId = workoutExerciseId,
        orderIndex = existingSets.size,
        setType = parseSetType(workoutExercise.setType).name,
        targetReps = parseRoutineRepTarget(workoutExercise.repRange),
        targetWeightKg = workoutExercise.targetWeightKg.coerceAtLeast(0.0),
        restSeconds = workoutExercise.restSeconds.coerceAtLeast(0),
        targetRpe = workoutExercise.targetRpe.coerceIn(0.0, 10.0),
    )
    return copy(routineSets = routineSets + nextSet).withWorkoutExerciseTargetsSynced(workoutExerciseId)
}

internal fun TrainIqStorageState.withExerciseReplacedInPlan(
    workoutExerciseId: Long,
    newExerciseId: Long,
): TrainIqStorageState {
    if (exercises.none { it.id == newExerciseId }) return this
    return copy(
        workoutExercises = workoutExercises.map { workoutExercise ->
            if (workoutExercise.id == workoutExerciseId) {
                workoutExercise.copy(exerciseId = newExerciseId)
            } else {
                workoutExercise
            }
        },
    )
}

internal fun TrainIqStorageState.withExerciseReplacedInActiveWorkout(
    workoutExerciseId: Long,
    newExerciseId: Long,
    now: Long,
): TrainIqStorageState {
    if (exercises.none { it.id == newExerciseId }) return this
    val currentPlan = workoutExercises.firstOrNull { it.id == workoutExerciseId } ?: return this
    val updated = withExerciseReplacedInPlan(workoutExerciseId, newExerciseId)
    val active = updated.activeWorkoutSession
    return if (active == null || active.dayId != currentPlan.dayId) {
        updated
    } else {
        updated.copy(
            activeWorkoutSession = active.copy(updatedAt = now),
        )
    }
}

internal fun TrainIqStorageState.withExerciseRemovedFromDay(
    workoutExerciseId: Long,
    now: Long,
): TrainIqStorageState {
    val removed = workoutExercises.firstOrNull { it.id == workoutExerciseId }
    val remainingWorkoutExercises = workoutExercises.filterNot { it.id == workoutExerciseId }
    val nextActiveSession = when {
        removed == null || activeWorkoutSession == null -> activeWorkoutSession
        activeWorkoutSession.dayId != removed.dayId -> activeWorkoutSession
        else -> {
            val remainingActivePlanUsesExercise = remainingWorkoutExercises.any {
                it.dayId == removed.dayId && it.exerciseId == removed.exerciseId
            }
            activeWorkoutSession.copy(
                loggedSets = activeWorkoutSession.loggedSets.filterNot { set ->
                    set.sourceWorkoutExerciseId == workoutExerciseId ||
                        (!remainingActivePlanUsesExercise && set.sourceWorkoutExerciseId == null && set.exerciseId == removed.exerciseId)
                },
                drafts = if (remainingActivePlanUsesExercise) {
                    activeWorkoutSession.drafts
                } else {
                    activeWorkoutSession.drafts - removed.exerciseId
                },
                collapsedExerciseIds = if (remainingActivePlanUsesExercise) {
                    activeWorkoutSession.collapsedExerciseIds
                } else {
                    activeWorkoutSession.collapsedExerciseIds - removed.exerciseId
                },
                updatedAt = now,
            )
        }
    }

    return copy(
        workoutExercises = remainingWorkoutExercises,
        routineSets = routineSets.filterNot { it.workoutExerciseId == workoutExerciseId },
        activeWorkoutSession = nextActiveSession,
    )
}

internal fun TrainIqStorageState.updateRoutineSet(
    setId: Long,
    transform: (RoutineSetEntity) -> RoutineSetEntity,
): TrainIqStorageState {
    val current = routineSets.firstOrNull { it.id == setId } ?: return this
    return copy(routineSets = routineSets.map { set ->
        if (set.id == setId) transform(set).sanitizeRoutineSet() else set
    }).withWorkoutExerciseTargetsSynced(current.workoutExerciseId)
}

internal fun TrainIqStorageState.renumberRoutineSets(workoutExerciseId: Long): TrainIqStorageState {
    val orderedIds = routineSets
        .filter { it.workoutExerciseId == workoutExerciseId }
        .sortedWith(compareBy<RoutineSetEntity> { it.orderIndex }.thenBy { it.id })
        .mapIndexed { index, set -> set.id to index }
        .toMap()
    return copy(routineSets = routineSets.map { set ->
        if (set.workoutExerciseId == workoutExerciseId) set.copy(orderIndex = orderedIds[set.id] ?: set.orderIndex) else set
    })
}

internal fun TrainIqStorageState.withRoutineSetCountSynced(
    workoutExerciseId: Long,
    targetSets: Int,
    repRange: String,
    restSeconds: Int,
    targetWeightKg: Double,
    targetRpe: Double,
    setType: SetType,
): TrainIqStorageState {
    val currentSets = routineSets
        .filter { it.workoutExerciseId == workoutExerciseId }
        .sortedWith(compareBy<RoutineSetEntity> { it.orderIndex }.thenBy { it.id })
    if (currentSets.size == targetSets) return withWorkoutExerciseTargetsSynced(workoutExerciseId)
    var nextSetId = (routineSets.maxOfOrNull { it.id } ?: 0L) + 1L
    val adjustedSets = when {
        currentSets.size > targetSets -> currentSets.take(targetSets)
        else -> currentSets + List(targetSets - currentSets.size) { offset ->
            val previous = (currentSets.lastOrNull() ?: currentSets.getOrNull(currentSets.lastIndex + offset))
            previous?.copy(id = nextSetId++, orderIndex = currentSets.size + offset)
                ?: RoutineSetEntity(
                    id = nextSetId++,
                    workoutExerciseId = workoutExerciseId,
                    orderIndex = currentSets.size + offset,
                    setType = setType.name,
                    targetReps = parseRoutineRepTarget(repRange),
                    targetWeightKg = targetWeightKg,
                    restSeconds = restSeconds,
                    targetRpe = targetRpe,
                )
        }
    }
    return copy(
        routineSets = routineSets.filterNot { it.workoutExerciseId == workoutExerciseId } + adjustedSets,
    ).renumberRoutineSets(workoutExerciseId).withWorkoutExerciseTargetsSynced(workoutExerciseId)
}

internal fun TrainIqStorageState.withWorkoutExerciseTargetsSynced(workoutExerciseId: Long): TrainIqStorageState {
    val orderedSets = routineSets
        .filter { it.workoutExerciseId == workoutExerciseId }
        .sortedWith(compareBy<RoutineSetEntity> { it.orderIndex }.thenBy { it.id })
    val first = orderedSets.firstOrNull()
    return copy(workoutExercises = workoutExercises.map { exercise ->
        if (exercise.id == workoutExerciseId) {
            exercise.copy(
                targetSets = orderedSets.size.coerceAtLeast(0),
                repRange = first?.targetReps?.takeIf { it > 0 }?.toString() ?: exercise.repRange,
                restSeconds = first?.restSeconds ?: exercise.restSeconds,
                targetWeightKg = first?.targetWeightKg ?: exercise.targetWeightKg,
                targetRpe = first?.targetRpe ?: exercise.targetRpe,
                setType = first?.setType ?: exercise.setType,
            )
        } else {
            exercise
        }
    })
}

private fun RoutineSet.toEntity() = RoutineSetEntity(
    id = id,
    workoutExerciseId = workoutExerciseId,
    orderIndex = orderIndex,
    setType = setType.name,
    targetReps = targetReps.coerceAtLeast(0),
    targetWeightKg = targetWeightKg.coerceAtLeast(0.0),
    restSeconds = restSeconds.coerceAtLeast(0),
    targetRpe = targetRpe.coerceIn(0.0, 10.0),
    targetRir = targetRir?.coerceAtLeast(0),
)

private fun RoutineSetEntity.sanitizeRoutineSet() = copy(
    setType = parseSetType(setType).name,
    targetReps = targetReps.coerceAtLeast(0),
    targetWeightKg = targetWeightKg.coerceAtLeast(0.0),
    restSeconds = restSeconds.coerceAtLeast(0),
    targetRpe = targetRpe.coerceIn(0.0, 10.0),
    targetRir = targetRir?.coerceAtLeast(0),
)

private fun parseRoutineRepTarget(repRange: String): Int =
    repRange.substringAfter('-', repRange).trim().toIntOrNull()
        ?: repRange.filter(Char::isDigit).toIntOrNull()
        ?: 0

internal fun buildMealItemSnapshots(
    mealId: Long,
    startItemId: Long,
    requests: List<MealEntryRequest>,
    foods: List<FoodItem>,
    recipes: List<Recipe>,
): List<LoggedMealItemStorage> {
    val foodsById = foods.associateBy { it.id }
    val recipesById = recipes.associateBy { it.id }
    var nextItemId = startItemId + 1L
    return requests.map { request ->
        when (request.itemType) {
            MealEntryType.FOOD -> {
                val food = foodsById[request.referenceId] ?: error("Deze maaltijd bevat een verwijderd product of recept.")
                val nutrition = food.nutritionForGrams(request.gramsUsed)
                LoggedMealItemStorage(
                    id = nextItemId++,
                    mealId = mealId,
                    itemType = LoggedMealItemType.FOOD,
                    referenceId = food.id,
                    name = food.name,
                    gramsUsed = request.gramsUsed,
                    calories = nutrition.calories,
                    protein = nutrition.protein,
                    carbs = nutrition.carbs,
                    fat = nutrition.fat,
                    notes = request.notes,
                )
            }

            MealEntryType.RECIPE -> {
                val recipe = recipesById[request.referenceId] ?: error("Deze maaltijd bevat een verwijderd product of recept.")
                val baseGrams = recipe.totalCookedGrams ?: recipe.ingredients.sumOf { it.gramsUsed }
                if (baseGrams <= 0.0 || !baseGrams.isFinite()) error("Deze maaltijd bevat een verwijderd product of recept.")
                val ratio = request.gramsUsed / baseGrams
                val nutrition = NutritionFacts(
                    calories = recipe.totalNutrition.calories * ratio,
                    protein = recipe.totalNutrition.protein * ratio,
                    carbs = recipe.totalNutrition.carbs * ratio,
                    fat = recipe.totalNutrition.fat * ratio,
                ).rounded()
                LoggedMealItemStorage(
                    id = nextItemId++,
                    mealId = mealId,
                    itemType = LoggedMealItemType.RECIPE,
                    referenceId = recipe.id,
                    name = recipe.name,
                    gramsUsed = request.gramsUsed,
                    calories = nutrition.calories,
                    protein = nutrition.protein,
                    carbs = nutrition.carbs,
                    fat = nutrition.fat,
                    notes = request.notes,
                )
            }
        }
    }
}

internal fun missingProfileForAiRoutineMessage(): String =
    "Vul eerst je profiel in voordat je een AI-routine maakt."

internal fun generatedRoutineMissingDaysMessage(): String =
    "AI gaf geen trainingsdagen terug. Probeer een specifiekere trainingsfocus."

internal fun generatedRoutineMissingExercisesMessage(): String =
    "AI gaf een dag zonder oefeningen terug. Probeer andere apparatuur of focus."

internal fun trainingDayCountText(count: Int): String =
    "$count ${if (count == 1) "trainingsdag" else "trainingsdagen"}"

private fun WorkoutSetEntity.progressionSetType(): SetType = parseSetType(setType)

private fun isProgressionSet(set: WorkoutSetEntity): Boolean =
    set.completed && set.reps > 0 && set.weight >= 0.0 && set.progressionSetType().isProgressionType()

private fun SetType.isProgressionType(): Boolean = this == SetType.NORMAL || this == SetType.BACK_OFF

internal fun buildProgressOverviewFromHistory(
    measurements: List<BodyMeasurement>,
    sessions: List<WorkoutSessionEntity>,
    sets: List<WorkoutSetEntity>,
    analyticsEngine: AnalyticsEngine,
): ProgressOverview {
    val completedSessions = sessions
        .filter { it.completed && it.status == "COMPLETED" }
        .sortedBy { it.date }
    val completedSessionIds = completedSessions.map { it.id }.toSet()
    val progressionSets = sets
        .filter { it.sessionId in completedSessionIds }
        .filter(::isProgressionSet)
    val weeklyVolumeTrend = completedSessions
        .groupBy { it.date.weekStartMillis() }
        .toSortedMap()
        .map { (weekStart, weekSessions) ->
            val weekSessionIds = weekSessions.map { it.id }.toSet()
            val weekVolume = analyticsEngine.trainingVolume(progressionSets.filter { it.sessionId in weekSessionIds })
            ChartPoint(weekStart.toReadableDate(), weekVolume)
        }
    val strengthTrend = completedSessions.mapNotNull { session ->
        val bestOneRepMax = progressionSets
            .filter { it.sessionId == session.id }
            .maxOfOrNull { analyticsEngine.estimatedOneRepMax(it.weight, it.reps) }
            ?: return@mapNotNull null
        ChartPoint(session.date.toReadableDate(), bestOneRepMax)
    }
    val estimatedOneRepMax = strengthTrend.maxOfOrNull { it.value } ?: 0.0
    val weeklyVolume = weeklyVolumeTrend.lastOrNull()?.value ?: 0.0
    val baseline = weeklyVolumeTrend
        .dropLast(1)
        .takeLast(3)
        .map { it.value }
        .average()
        .takeIf { !it.isNaN() && it > 0.0 }
        ?: weeklyVolume

    return ProgressOverview(
        measurements = measurements,
        weightTrend = measurements.map { ChartPoint(it.date.toReadableDate(), it.weight) },
        bodyFatTrend = measurements.map { ChartPoint(it.date.toReadableDate(), it.bodyFat) },
        muscleMassTrend = measurements.map { ChartPoint(it.date.toReadableDate(), it.muscleMass) },
        strengthTrend = strengthTrend,
        volumeTrend = weeklyVolumeTrend,
        estimatedOneRepMax = estimatedOneRepMax,
        fatigueIndex = if (weeklyVolume == 0.0 || baseline == 0.0) 0.0 else analyticsEngine.fatigueIndex(weeklyVolume, baseline),
    )
}

private fun ProgressOverview.currentWeekVolume(): Double =
    volumeTrend.lastOrNull()?.value ?: 0.0

private fun Long.weekStartMillis(): Long =
    java.time.Instant.ofEpochMilli(this)
        .atZone(java.time.ZoneId.systemDefault())
        .toLocalDate()
        .with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
        .atStartOfDay(java.time.ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()

private fun com.trainiq.ai.services.GeneratedRoutine.toDomainGeneratedRoutine() = GeneratedRoutine(
    routineName = routineName,
    routineDescription = routineDescription,
    periodizationNote = periodizationNote,
    estimatedDurationMinutes = estimatedDurationMinutes,
    source = when (source) {
        com.trainiq.ai.services.GeneratedRoutineSource.GEMINI_2_5_FLASH -> com.trainiq.domain.model.GeneratedRoutineSource.GEMINI_2_5_FLASH
        com.trainiq.ai.services.GeneratedRoutineSource.LOCAL_FALLBACK -> com.trainiq.domain.model.GeneratedRoutineSource.LOCAL_FALLBACK
    },
    days = days.map { day ->
        GeneratedDay(
            dayName = day.dayName,
            estimatedDurationMinutes = day.estimatedDurationMinutes,
            exercises = day.exercises.map { exercise ->
                GeneratedExercise(
                    exerciseName = exercise.exerciseName,
                    muscleGroup = exercise.muscleGroup,
                    equipment = exercise.equipment,
                    targetSets = exercise.targetSets,
                    repRange = exercise.repRange,
                    restSeconds = exercise.restSeconds,
                    coachingCue = exercise.coachingCue,
                )
            },
        )
    },
)
