package com.trainiq.data.repository

import com.trainiq.ai.services.GoalAdvisorService
import com.trainiq.ai.services.MealAnalysisService
import com.trainiq.ai.services.RoutineGeneratorService
import com.trainiq.ai.services.WeeklyReportService
import com.trainiq.ai.services.WorkoutDebriefService
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
import com.trainiq.data.local.TrainIqLocalStore
import com.trainiq.data.local.TrainIqStorageState
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
import com.trainiq.domain.model.ReadinessLevel
import com.trainiq.domain.model.Recipe
import com.trainiq.domain.model.RecipeIngredient
import com.trainiq.domain.model.RoutineSet
import com.trainiq.domain.model.SetType
import com.trainiq.domain.model.StrengthCalculator
import com.trainiq.domain.model.UserProfile
import com.trainiq.domain.model.WeeklyReportResult
import com.trainiq.domain.model.WorkoutDay
import com.trainiq.domain.model.WorkoutDebrief
import com.trainiq.domain.model.WorkoutOverview
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class TrainIqRepository @Inject constructor(
    private val localStore: TrainIqLocalStore,
    private val healthConnectDataSource: HealthConnectDataSource,
    private val analyticsEngine: AnalyticsEngine,
    private val mealAnalysisService: MealAnalysisService,
    private val workoutDebriefService: WorkoutDebriefService,
    private val goalAdvisorService: GoalAdvisorService,
    private val weeklyReportService: WeeklyReportService,
    private val routineGeneratorService: RoutineGeneratorService,
) : HomeRepository, WorkoutRepository, NutritionRepository, ProgressRepository, CoachRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val scannedMealResult = MutableStateFlow<MealAnalysisResult?>(null)
    private val _cachedSteps = MutableStateFlow(0)

    private val snapshotState: StateFlow<RepositorySnapshot> = combine(localStore.state, scannedMealResult) { state, scanned ->
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
        scope.launch { ensureExerciseLibrary() }
        scope.launch {
            val persisted = healthConnectDataSource.getStepsFromPersistedCache()
            if (persisted > 0) _cachedSteps.value = persisted
        }
    }

    override fun observeDashboard(): Flow<HomeDashboard> = combine(snapshotState, _cachedSteps) { snapshot, steps ->
        val activeRoutine = buildWorkoutOverview(snapshot).activeRoutine
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

    override suspend fun getHealthConnectStatus(): HealthConnectStatus {
        val status = healthConnectDataSource.getStatus()
        status.metrics?.stepsToday?.let { _cachedSteps.value = it }
        return status
    }

    override suspend fun refreshDashboardData() {
        val live = healthConnectDataSource.getTodayStepsLive()
        if (live > 0) {
            _cachedSteps.value = live
        } else {
            // HC unavailable or permissions not granted — fall back to the last persisted snapshot.
            val cached = healthConnectDataSource.getStepsFromPersistedCache()
            if (cached > 0) _cachedSteps.value = cached
        }
    }

    override fun observeWorkoutOverview(): Flow<WorkoutOverview> = snapshotState.map(::buildWorkoutOverview)

    override fun observeExerciseHistory(exerciseId: Long): Flow<ExerciseHistory> =
        snapshotState.map { snapshot -> buildExerciseHistory(snapshot, exerciseId) }

    override suspend fun getWorkoutDay(dayId: Long): WorkoutDay? = buildWorkoutDay(snapshotState.value, dayId)

    override suspend fun getProgressionSuggestions(dayId: Long): List<ProgressionSuggestion> = withContext(Dispatchers.IO) {
        val snapshot = snapshotState.value
        val day = buildWorkoutDay(snapshot, dayId) ?: return@withContext emptyList()
        val sessionsById = snapshot.sessions
            .filter { it.completed && it.status == "COMPLETED" }
            .associateBy { it.id }
        val targetRepsByExerciseId = day.exercises.associate { it.exercise.id to parseTargetRepTarget(it.repRange) }
        day.exercises.mapNotNull { plan ->
            val exerciseSessions = snapshot.sets
                .filter { it.exerciseId == plan.exercise.id }
                .groupBy { it.sessionId }
                .mapNotNull { (sessionId, sets) ->
                    val session = sessionsById[sessionId] ?: return@mapNotNull null
                    val progressionSets = sets
                        .filter(::isProgressionSet)
                        .sortedByDescending { it.weight * it.reps }
                    if (progressionSets.isEmpty()) return@mapNotNull null
                    ExerciseSessionSnapshot(session.date, progressionSets)
                }
                .sortedByDescending { it.date }
                .take(3)
            val lastSession = exerciseSessions.firstOrNull() ?: return@mapNotNull null
            val lastSessionAvgRpe = lastSession.sets.map { it.rpe }.average().takeIf { !it.isNaN() }?.toFloat()
            val recentAverageRir = exerciseSessions
                .mapNotNull { session ->
                    session.sets
                        .mapNotNull { it.repsInReserve }
                        .average()
                        .takeIf { !it.isNaN() }
                        ?.toFloat()
                }
                .average()
                .takeIf { !it.isNaN() }
                ?.toFloat()
            val completedRecentSessions = exerciseSessions
                .take(2)
                .takeIf { it.size == 2 }
                ?.all { session ->
                    session.sets.isNotEmpty() && session.sets.all { it.reps >= (targetRepsByExerciseId[plan.exercise.id] ?: 0) }
                }
                ?: false
            val referenceWeight = lastSession.sets.maxOfOrNull { it.weight } ?: 0.0
            val plateauDetected = hasPlateaued(exerciseSessions)
            val readiness = resolveReadiness(
                lastSessionAvgRpe = lastSessionAvgRpe,
                recentAverageRir = recentAverageRir,
                completedRecentSessions = completedRecentSessions,
                plateauDetected = plateauDetected,
            )
            val loadStep = progressionLoadStep(plan.exercise.name, plan.exercise.muscleGroup, plan.exercise.equipment)
            val suggestedWeight = when (readiness) {
                ReadinessLevel.INCREASE -> referenceWeight + loadStep
                ReadinessLevel.DELOAD -> referenceWeight * 0.9
                ReadinessLevel.PLATEAU -> referenceWeight
                ReadinessLevel.MAINTAIN -> referenceWeight
            }
            val lastLoggedSet = lastSession.sets.maxByOrNull { it.weight * it.reps }
            ProgressionSuggestion(
                exerciseId = plan.exercise.id,
                exerciseName = plan.exercise.name,
                suggestedWeightKg = suggestedWeight.coerceAtLeast(0.0),
                suggestedReps = plan.repRange,
                lastSessionAvgRpe = lastSessionAvgRpe,
                readinessSignal = readiness,
                lastLoggedWeightKg = lastLoggedSet?.weight,
                lastLoggedReps = lastLoggedSet?.reps?.toString(),
            )
        }
    }

    override suspend fun getNextWorkoutDay(): WorkoutDay? =
        buildWorkoutOverview(snapshotState.value).activeRoutine?.days?.minByOrNull { it.orderIndex }

    override suspend fun getOrStartActiveWorkoutSession(
        dayId: Long,
        initialDrafts: Map<Long, ActiveWorkoutSetDraft>,
    ): ActiveWorkoutSession {
        val now = System.currentTimeMillis()
        var result: ActiveWorkoutSession? = null
        localStore.update { state ->
            val existing = state.activeWorkoutSession
            val active = if (existing != null && existing.dayId == dayId) {
                existing.copy(updatedAt = now)
            } else {
                val day = state.days.firstOrNull { it.id == dayId }
                val routineId = day?.routineId
                val sessionId = (state.sessions.maxOfOrNull { it.id } ?: 0L) + 1L
                ActiveWorkoutSessionStorage(
                    sessionId = sessionId,
                    dayId = dayId,
                    routineId = routineId,
                    startedAt = now,
                    updatedAt = now,
                    drafts = initialDrafts.mapValues { it.value.toStorage() },
                )
            }
            result = active.toDomain()
            val existingSessionIds = state.sessions.map { it.id }.toSet()
            val draftSession = WorkoutSessionEntity(
                id = active.sessionId,
                date = active.startedAt,
                duration = ((now - active.startedAt) / 1_000).coerceAtLeast(0),
                caloriesBurned = 0,
                routineId = active.routineId,
                workoutDayId = active.dayId,
                startedAt = active.startedAt,
                endedAt = 0L,
                status = "DRAFT",
                completed = false,
            )
            val performedExercises = state.ensurePerformedExercisesForActiveSession(active)
            state.copy(
                sessions = if (active.sessionId in existingSessionIds) {
                    state.sessions.map { if (it.id == active.sessionId) draftSession else it }
                } else {
                    listOf(draftSession) + state.sessions
                },
                performedExercises = performedExercises,
                activeWorkoutSession = active,
            )
        }
        return requireNotNull(result)
    }

    override suspend fun updateActiveWorkoutDraft(exerciseId: Long, draft: ActiveWorkoutSetDraft): ActiveWorkoutSession? =
        mutateActiveWorkout { active, now ->
            active.copy(
                updatedAt = now,
                drafts = active.drafts.toMutableMap().apply { put(exerciseId, draft.toStorage()) },
            )
        }

    override suspend fun logActiveWorkoutSet(
        dayId: Long,
        set: LoggedSet,
        draft: ActiveWorkoutSetDraft,
        restSeconds: Int,
    ): ActiveWorkoutSession {
        val now = System.currentTimeMillis()
        var result: ActiveWorkoutSession? = null
        localStore.update { state ->
            val active = state.activeWorkoutSession?.takeIf { it.dayId == dayId }
                ?: ActiveWorkoutSessionStorage(dayId = dayId, startedAt = now)
            val nextSetId = (active.loggedSets.maxOfOrNull { it.id } ?: 0L) + 1L
            val performedExercise = state.performedExercises.firstOrNull {
                it.sessionId == active.sessionId && it.exerciseId == set.exerciseId
            }
            val next = active.copy(
                updatedAt = now,
                loggedSets = active.loggedSets + ActiveWorkoutSetStorage(
                    id = nextSetId,
                    exerciseId = set.exerciseId,
                    performedExerciseId = performedExercise?.id ?: set.performedExerciseId,
                    sourceWorkoutExerciseId = performedExercise?.sourceWorkoutExerciseId ?: set.sourceWorkoutExerciseId,
                    weight = set.weight,
                    reps = set.reps,
                    rpe = set.rpe,
                    repsInReserve = set.repsInReserve,
                    setType = set.setType,
                    restSeconds = restSeconds.coerceAtLeast(0),
                    orderIndex = active.loggedSets.count { it.exerciseId == set.exerciseId },
                    completed = true,
                    loggedAt = now,
                ),
                drafts = active.drafts.toMutableMap().apply { put(set.exerciseId, draft.toStorage()) },
                collapsedExerciseIds = active.collapsedExerciseIds - set.exerciseId,
                restTimerEndsAt = if (restSeconds > 0) now + restSeconds * 1_000L else null,
                restTimerTotalSeconds = restSeconds.coerceAtLeast(0),
            )
            result = next.toDomain()
            state.copy(activeWorkoutSession = next)
        }
        return requireNotNull(result)
    }

    override suspend fun updateActiveWorkoutSetType(exerciseId: Long, setIndex: Int, setType: SetType): ActiveWorkoutSession? =
        mutateActiveWorkout { active, now ->
            active.copy(
                updatedAt = now,
                loggedSets = active.loggedSets.replaceExerciseSet(exerciseId, setIndex) { it.copy(setType = setType) },
            )
        }

    override suspend fun deleteActiveWorkoutSet(exerciseId: Long, setIndex: Int): ActiveWorkoutSession? =
        mutateActiveWorkout { active, now ->
            active.copy(
                updatedAt = now,
                loggedSets = active.loggedSets.filterIndexedForExercise(exerciseId) { index -> index != setIndex },
            )
        }

    override suspend fun setActiveWorkoutCollapsed(exerciseId: Long, collapsed: Boolean): ActiveWorkoutSession? =
        mutateActiveWorkout { active, now ->
            active.copy(
                updatedAt = now,
                collapsedExerciseIds = if (collapsed) active.collapsedExerciseIds + exerciseId else active.collapsedExerciseIds - exerciseId,
            )
        }

    override suspend fun updateActiveWorkoutRestTimer(endsAt: Long?, totalSeconds: Int): ActiveWorkoutSession? =
        mutateActiveWorkout { active, now ->
            active.copy(
                updatedAt = now,
                restTimerEndsAt = endsAt,
                restTimerTotalSeconds = if (endsAt == null) 0 else totalSeconds.coerceAtLeast(0),
            )
        }

    override suspend fun finishActiveWorkout(dayId: Long): WorkoutDebrief {
        val active = localStore.state.value.activeWorkoutSession?.takeIf { it.dayId == dayId }
        val durationSeconds = active?.let {
            ((System.currentTimeMillis() - it.startedAt) / 1_000).coerceAtLeast(1)
        } ?: 1L
        val debrief = finishWorkout(
            dayId = dayId,
            durationSeconds = durationSeconds,
            loggedSets = active?.loggedSets.orEmpty().map { it.toDomain().toLoggedSet() },
            activeSessionId = active?.sessionId,
            activeStartedAt = active?.startedAt,
        )
        localStore.update { state ->
            if (state.activeWorkoutSession?.dayId == dayId) state.copy(activeWorkoutSession = null) else state
        }
        return debrief
    }

    override suspend fun discardActiveWorkout(dayId: Long) {
        localStore.update { state ->
            if (state.activeWorkoutSession?.dayId == dayId) state.copy(activeWorkoutSession = null) else state
        }
    }

    override suspend fun setActiveRoutine(routineId: Long) {
        localStore.update { state ->
            state.copy(routines = state.routines.map { it.copy(active = it.id == routineId) })
        }
    }

    override suspend fun finishWorkout(dayId: Long, durationSeconds: Long, loggedSets: List<LoggedSet>): WorkoutDebrief =
        finishWorkout(dayId = dayId, durationSeconds = durationSeconds, loggedSets = loggedSets, activeSessionId = null, activeStartedAt = null)

    private suspend fun finishWorkout(
        dayId: Long,
        durationSeconds: Long,
        loggedSets: List<LoggedSet>,
        activeSessionId: Long?,
        activeStartedAt: Long?,
    ): WorkoutDebrief {
        if (loggedSets.isEmpty()) {
            return WorkoutDebrief(
                summary = "Geen sets gelogd.",
                progressionFeedback = "Log minimaal een set om voortgang op te slaan.",
                recommendation = "Voeg tijdens je training sets toe voordat je afrondt.",
                nextSessionFocus = "Maintain current weights",
                recoveryScore = 75,
                intensitySignal = "MAINTAIN",
            )
        }

        val beforeSnapshot = snapshotState.value
        val current = localStore.state.value
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
                loggedAt = set.performedExerciseId.takeIf { it > 0L }?.let { now + index } ?: now + index,
                completedAt = now + index,
            )
        }
        localStore.update { state ->
            state.copy(
                sessions = listOf(newSession) + state.sessions.filterNot { it.id == sessionId },
                performedExercises = performedExercises + state.performedExercises.filterNot { it.sessionId == sessionId },
                workoutSets = newSets + state.workoutSets.filterNot { it.sessionId == sessionId },
            )
        }

        val debriefSets = loggedSets.filter { it.setType.isProgressionType() }.ifEmpty { loggedSets }
        val currentVolume = debriefSets.sumOf { it.weight * it.reps }
        val previousVolume = beforeSnapshot.sessions
            .firstOrNull()
            ?.let { latest ->
                beforeSnapshot.sets
                    .filter { it.sessionId == latest.id && isProgressionSet(it) }
                    .sumOf { it.weight * it.reps }
            }
            ?.takeIf { it > 0.0 }
            ?: currentVolume
        val progression = if (previousVolume == 0.0) 0.0 else ((currentVolume - previousVolume) / previousVolume) * 100
        val distribution = buildWorkoutDay(beforeSnapshot, dayId)?.exercises
            ?.groupBy { it.exercise.muscleGroup }
            ?.map { "${it.key} ${it.value.size}" }
            ?.joinToString()
            .orEmpty()
        val avgRpe = debriefSets.map { it.rpe }.average().takeIf { !it.isNaN() }?.toFloat() ?: 0f
        val exerciseNameById = buildWorkoutDay(beforeSnapshot, dayId)
            ?.exercises
            ?.associate { it.exercise.id to it.exercise.name }
            .orEmpty()
        val topExercises = debriefSets
            .sortedByDescending { it.weight * it.reps }
            .take(3)
            .joinToString { set ->
                val exerciseName = exerciseNameById[set.exerciseId] ?: "Exercise ${set.exerciseId}"
                "${exerciseName} ${formatWeight(set.weight)}kgx${set.reps}"
            }
            .ifBlank { "No top sets logged" }
        val sevenDaysAgo = System.currentTimeMillis() - (7 * 86_400_000L)
        val weeklyFrequency = (beforeSnapshot.sessions + newSession)
            .filter { it.date >= sevenDaysAgo }
            .map { normalizeToDay(it.date) }
            .distinct()
            .count()
        return workoutDebriefService.generateWorkoutDebrief(
            totalVolume = currentVolume,
            progression = progression,
            distribution = distribution,
            avgRpe = avgRpe,
            topExercises = topExercises,
            weeklyFrequency = weeklyFrequency,
        )
    }

    override suspend fun createRoutine(name: String, description: String) {
        localStore.update { state ->
            val routineId = (state.routines.maxOfOrNull { it.id } ?: 0L) + 1L
            val routine = WorkoutRoutineEntity(
                id = routineId,
                name = name,
                description = description,
                active = state.routines.isEmpty(),
            )
            state.copy(routines = state.routines + routine)
        }
    }

    override suspend fun updateRoutine(routineId: Long, name: String, description: String) {
        localStore.update { state ->
            state.copy(routines = state.routines.map {
                if (it.id == routineId) it.copy(name = name, description = description) else it
            })
        }
    }

    override suspend fun deleteRoutine(routineId: Long) {
        localStore.update { state ->
            val remainingDays = state.days.filterNot { it.routineId == routineId }
            val removedDayIds = state.days.filter { it.routineId == routineId }.map { it.id }.toSet()
            val remainingRoutines = state.routines.filterNot { it.id == routineId }
            val normalizedRoutines = if (remainingRoutines.none { it.active } && remainingRoutines.isNotEmpty()) {
                remainingRoutines.mapIndexed { index, routine -> routine.copy(active = index == 0) }
            } else {
                remainingRoutines
            }
            state.copy(
                routines = normalizedRoutines,
                days = remainingDays,
                workoutExercises = state.workoutExercises.filterNot { it.dayId in removedDayIds },
                routineSets = state.routineSets.filterNot { routineSet ->
                    state.workoutExercises.any { it.dayId in removedDayIds && it.id == routineSet.workoutExerciseId }
                },
                activeWorkoutSession = state.activeWorkoutSession?.takeUnless { it.dayId in removedDayIds },
            )
        }
    }

    override suspend fun searchExercises(query: String): List<Exercise> = withContext(Dispatchers.IO) {
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

    override suspend fun reorderExercises(dayId: Long, orderedIds: List<Long>) {
        localStore.update { state ->
            val requestedOrder = orderedIds.distinct().withIndex().associate { it.value to it.index }
            val dayExercises = state.workoutExercises.filter { it.dayId == dayId }
            val firstAppendedOrder = requestedOrder.size
            val fallbackOrder = dayExercises
                .filterNot { it.id in requestedOrder }
                .sortedWith(compareBy<WorkoutExerciseEntity> { it.orderIndex }.thenBy { it.id })
                .withIndex()
                .associate { it.value.id to firstAppendedOrder + it.index }
            val nextOrder = requestedOrder + fallbackOrder
            state.copy(
                workoutExercises = state.workoutExercises.map { exercise ->
                    if (exercise.dayId == dayId) {
                        exercise.copy(orderIndex = nextOrder[exercise.id] ?: exercise.orderIndex)
                    } else {
                        exercise
                    }
                },
            )
        }
    }

    override suspend fun setSupersetGroup(workoutExerciseIds: List<Long>, groupId: Long?) {
        val ids = workoutExerciseIds.toSet()
        if (ids.isEmpty()) return
        localStore.update { state ->
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

    override suspend fun replaceExerciseInPlan(workoutExerciseId: Long, newExerciseId: Long) {
        localStore.update { state ->
            state.withExerciseReplacedInPlan(workoutExerciseId, newExerciseId)
        }
    }

    override suspend fun updateWorkoutExercisePlan(
        workoutExerciseId: Long,
        targetSets: Int,
        repRange: String,
        restSeconds: Int,
        targetWeightKg: Double,
        targetRpe: Double,
        setType: SetType,
    ) {
        localStore.update { state ->
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

    override suspend fun addSetToExercise(workoutExerciseId: Long) {
        localStore.update { state -> state.withRoutineSetAdded(workoutExerciseId) }
    }

    override suspend fun updateRoutineSet(set: RoutineSet) {
        localStore.update { state ->
            state.copy(routineSets = state.routineSets.map { existing ->
                if (existing.id == set.id) set.toEntity() else existing
            }).withWorkoutExerciseTargetsSynced(set.workoutExerciseId)
        }
    }

    override suspend fun updateRoutineSetType(setId: Long, setType: SetType) {
        localStore.update { state -> state.updateRoutineSet(setId) { it.copy(setType = setType.name) } }
    }

    override suspend fun updateRoutineSetReps(setId: Long, targetReps: Int) {
        localStore.update { state -> state.updateRoutineSet(setId) { it.copy(targetReps = targetReps.coerceAtLeast(0)) } }
    }

    override suspend fun updateRoutineSetWeight(setId: Long, targetWeightKg: Double) {
        localStore.update { state -> state.updateRoutineSet(setId) { it.copy(targetWeightKg = targetWeightKg.coerceAtLeast(0.0)) } }
    }

    override suspend fun updateRoutineSetRestTime(setId: Long, restSeconds: Int) {
        localStore.update { state -> state.updateRoutineSet(setId) { it.copy(restSeconds = restSeconds.coerceAtLeast(0)) } }
    }

    override suspend fun deleteRoutineSet(setId: Long) {
        localStore.update { state ->
            val removed = state.routineSets.firstOrNull { it.id == setId } ?: return@update state
            state.copy(routineSets = state.routineSets.filterNot { it.id == setId })
                .renumberRoutineSets(removed.workoutExerciseId)
                .withWorkoutExerciseTargetsSynced(removed.workoutExerciseId)
        }
    }

    override suspend fun moveRoutineSet(workoutExerciseId: Long, orderedSetIds: List<Long>) {
        localStore.update { state ->
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

    override suspend fun addWorkoutDay(routineId: Long, name: String) {
        localStore.update { state ->
            val dayId = (state.days.maxOfOrNull { it.id } ?: 0L) + 1L
            val nextOrder = state.days.count { it.routineId == routineId }
            val sessionName = name.trim().ifBlank { defaultWorkoutSessionName(nextOrder) }
            state.copy(days = state.days + WorkoutDayEntity(dayId, routineId, sessionName, nextOrder))
        }
    }

    override suspend fun removeWorkoutDay(dayId: Long) {
        localStore.update { state ->
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

    override suspend fun addExerciseToDay(
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
        localStore.update { state ->
            state.withExerciseAddedToDay(dayId, name, muscleGroup, equipment, targetSets, repRange, restSeconds, targetWeightKg, targetRpe)
        }
    }

    override suspend fun addExerciseToRoutine(
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
        localStore.update { state ->
            state.withExerciseAddedToRoutine(routineId, name, muscleGroup, equipment, targetSets, repRange, restSeconds, targetWeightKg, targetRpe)
        }
    }

    override suspend fun removeExerciseFromDay(workoutExerciseId: Long) {
        localStore.update { state ->
            val removed = state.workoutExercises.firstOrNull { it.id == workoutExerciseId }
            val removedExerciseId = removed?.exerciseId
            val active = state.activeWorkoutSession
            state.copy(
                workoutExercises = state.workoutExercises.filterNot { it.id == workoutExerciseId },
                routineSets = state.routineSets.filterNot { it.workoutExerciseId == workoutExerciseId },
                activeWorkoutSession = if (removedExerciseId != null && active != null) {
                    active.copy(
                        loggedSets = active.loggedSets.filterNot { it.exerciseId == removedExerciseId },
                        drafts = active.drafts - removedExerciseId,
                        collapsedExerciseIds = active.collapsedExerciseIds - removedExerciseId,
                        updatedAt = System.currentTimeMillis(),
                    )
                } else {
                    active
                },
            )
        }
    }

    override suspend fun deleteWorkoutSession(sessionId: Long) {
        localStore.update { state ->
            state.copy(
                sessions = state.sessions.filterNot { it.id == sessionId },
                workoutSets = state.workoutSets.filterNot { it.sessionId == sessionId },
            )
        }
    }

    override suspend fun generateAiRoutine(
        daysPerWeek: Int,
        equipment: String,
        targetFocus: String,
        experienceLevel: String,
        sessionDurationMinutes: Int,
        includeDeload: Boolean,
    ): GeneratedRoutine = withContext(Dispatchers.IO) {
        val profile = snapshotState.value.profile
            ?: error("User profile is required to generate an AI routine. Please complete your profile first.")
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
            "The AI returned a routine with no workout days. Try a more specific training focus."
        }
        check(generated.days.none { it.exercises.isEmpty() }) {
            "The AI returned a day with no exercises. Try again with different equipment or focus."
        }
        generated.toDomainGeneratedRoutine()
    }

    override suspend fun saveGeneratedRoutine(routine: GeneratedRoutine) = withContext(Dispatchers.IO) {
        check(routine.days.isNotEmpty()) {
            "Generated routine has no workout days."
        }
        check(routine.days.none { it.exercises.isEmpty() }) {
            "Generated routine contains a day with no exercises."
        }
        localStore.update { state ->
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

    override fun observeNutritionOverview(): Flow<NutritionOverview> =
        combine(snapshotState, _cachedSteps) { snapshot, steps -> buildNutritionOverview(snapshot, steps) }

    override suspend fun analyzeMealPhoto(path: String, context: String, capturedAtMillis: Long): MealAnalysisResult {
        val result = mealAnalysisService.analyzeMealImage(
            path = path,
            userContext = context,
            capturedAtMillis = capturedAtMillis,
        )
        scannedMealResult.value = result
        return result
    }

    override fun clearLastScanResult() {
        scannedMealResult.value = null
    }

    override suspend fun saveFoodItem(
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
        val current = localStore.state.value
        val existing = current.foods.firstOrNull { it.id == id }
        val duplicateBarcode = barcode?.trim()?.takeIf { it.isNotBlank() }?.let { code ->
            current.foods.firstOrNull { it.barcode == code && it.id != id }
        }
        val foodId = existing?.id ?: duplicateBarcode?.id ?: ((current.foods.maxOfOrNull { it.id } ?: 0L) + 1L)
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
        localStore.update { state ->
            state.copy(foods = state.foods.filterNot { it.id == foodId } + storage)
        }
        return mapFood(storage)
    }

    override suspend fun saveRecipe(
        id: Long?,
        name: String,
        notes: String?,
        totalCookedGrams: Double?,
        ingredients: List<Pair<Long, Double>>,
    ): Recipe {
        val current = localStore.state.value
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
        localStore.update { state ->
            state.copy(
                recipes = state.recipes.filterNot { it.id == recipeId } + recipe,
                recipeIngredients = state.recipeIngredients.filterNot { it.recipeId == recipeId } + ingredientStorage,
            )
        }
        return buildRecipes(localStore.state.value.foods, localStore.state.value.recipes, localStore.state.value.recipeIngredients)
            .first { it.id == recipeId }
    }

    override suspend fun saveMeal(
        id: Long?,
        mealType: MealType,
        name: String,
        notes: String?,
        items: List<MealEntryRequest>,
    ): Long {
        val snapshot = snapshotState.value
        val current = localStore.state.value
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
        val mealItems = items.mapIndexed { index, request ->
            when (request.itemType) {
                MealEntryType.FOOD -> {
                    val food = snapshot.foods.first { it.id == request.referenceId }
                    val nutrition = food.nutritionForGrams(request.gramsUsed)
                    LoggedMealItemStorage(
                        id = startItemId + index + 1L,
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
                    val recipe = snapshot.recipes.first { it.id == request.referenceId }
                    val baseGrams = recipe.totalCookedGrams ?: recipe.ingredients.sumOf { it.gramsUsed }
                    val ratio = request.gramsUsed / baseGrams.coerceAtLeast(1.0)
                    val nutrition = NutritionFacts(
                        calories = recipe.totalNutrition.calories * ratio,
                        protein = recipe.totalNutrition.protein * ratio,
                        carbs = recipe.totalNutrition.carbs * ratio,
                        fat = recipe.totalNutrition.fat * ratio,
                    ).rounded()
                    LoggedMealItemStorage(
                        id = startItemId + index + 1L,
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
        localStore.update { state ->
            state.copy(
                meals = state.meals.filterNot { it.id == mealId } + mealStorage,
                mealItems = state.mealItems.filterNot { it.mealId == mealId } + mealItems,
            )
        }
        scannedMealResult.value = null
        return mealId
    }

    override suspend fun deleteMeal(mealId: Long) {
        localStore.update { state ->
            state.copy(
                meals = state.meals.filterNot { it.id == mealId },
                mealItems = state.mealItems.filterNot { it.mealId == mealId },
            )
        }
    }

    override suspend fun deleteFood(foodId: Long) {
        localStore.update { state ->
            state.copy(
                foods = state.foods.filterNot { it.id == foodId },
                recipeIngredients = state.recipeIngredients.filterNot { it.foodItemId == foodId },
            )
        }
    }

    override suspend fun deleteRecipe(recipeId: Long) {
        localStore.update { state ->
            state.copy(
                recipes = state.recipes.filterNot { it.id == recipeId },
                recipeIngredients = state.recipeIngredients.filterNot { it.recipeId == recipeId },
            )
        }
    }

    override fun observeProgressOverview(): Flow<ProgressOverview> = snapshotState.map(::buildProgressOverview)

    override suspend fun addMeasurement(weight: Double, bodyFat: Double, muscleMass: Double) {
        localStore.update { state ->
            val measurementId = (state.measurements.maxOfOrNull { it.id } ?: 0L) + 1L
            state.copy(
                measurements = state.measurements + BodyMeasurementEntity(
                    id = measurementId,
                    date = System.currentTimeMillis(),
                    weight = weight,
                    bodyFat = bodyFat,
                    muscleMass = muscleMass,
                ),
            )
        }
    }

    override suspend fun deleteMeasurement(measurementId: Long) {
        localStore.update { state ->
            state.copy(measurements = state.measurements.filterNot { it.id == measurementId })
        }
    }

    override fun observeCoachOverview(): Flow<CoachOverview> = snapshotState.mapLatest { snapshot ->
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

    override suspend fun generateGoalAdvice(
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

    override suspend fun generateWeeklyReport(): WeeklyReportResult {
        val snapshot = snapshotState.value
        val progress = buildProgressOverview(snapshot)
        return if (snapshot.sessions.isEmpty() && snapshot.meals.isEmpty()) {
            WeeklyReportResult(
                summary = "Log je eerste training of maaltijd om een wekelijkse coachsamenvatting te krijgen.",
                wins = emptyList(),
                risks = emptyList(),
                nextWeekFocus = "Voltooi eerst een training of maaltijdlog.",
            )
        } else {
            weeklyReportService.generateWeeklyReport(
                volume = progress.volumeTrend.takeLast(7).sumOf { it.value },
                weightTrend = progress.weightTrend.lastOrNull()?.value ?: snapshot.profile?.weight ?: 0.0,
                adherence = calculateAdherence(snapshot),
            )
        }
    }

    override fun observeUserProfile(): Flow<UserProfile?> = snapshotState.map { it.profile }

    override suspend fun saveProfile(profile: UserProfile) {
        localStore.update { state ->
            state.copy(
                profile = UserProfileEntity(
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
    }

    private suspend fun ensureExerciseLibrary() {
        if (localStore.state.value.exercises.size >= 50) return
        localStore.update { state ->
            val existingNames = state.exercises.map { it.name.lowercase() }.toSet()
            val nextId = (state.exercises.maxOfOrNull { it.id } ?: 0L) + 1L
            val canonical = listOf(
                ExerciseEntity(1, "Bench Press", "Chest", "Barbell"),
                ExerciseEntity(2, "Incline Bench Press", "Chest", "Barbell"),
                ExerciseEntity(3, "Dumbbell Fly", "Chest", "Dumbbell"),
                ExerciseEntity(4, "Cable Fly", "Chest", "Cable"),
                ExerciseEntity(5, "Dips", "Chest", "Bodyweight"),
                ExerciseEntity(6, "Deadlift", "Back", "Barbell"),
                ExerciseEntity(7, "Barbell Row", "Back", "Barbell"),
                ExerciseEntity(8, "Dumbbell Row", "Back", "Dumbbell"),
                ExerciseEntity(9, "Lat Pulldown", "Back", "Cable"),
                ExerciseEntity(10, "Cable Row", "Back", "Cable"),
                ExerciseEntity(11, "Pull-up", "Back", "Bodyweight"),
                ExerciseEntity(12, "Face Pull", "Back", "Cable"),
                ExerciseEntity(13, "T-Bar Row", "Back", "Barbell"),
                ExerciseEntity(14, "Overhead Press", "Shoulders", "Barbell"),
                ExerciseEntity(15, "Dumbbell Press", "Shoulders", "Dumbbell"),
                ExerciseEntity(16, "Lateral Raise", "Shoulders", "Dumbbell"),
                ExerciseEntity(17, "Rear Delt Fly", "Shoulders", "Dumbbell"),
                ExerciseEntity(18, "Arnold Press", "Shoulders", "Dumbbell"),
                ExerciseEntity(19, "Squat", "Legs", "Barbell"),
                ExerciseEntity(20, "Romanian Deadlift", "Hamstrings", "Barbell"),
                ExerciseEntity(21, "Leg Press", "Legs", "Machine"),
                ExerciseEntity(22, "Leg Curl", "Hamstrings", "Machine"),
                ExerciseEntity(23, "Leg Extension", "Legs", "Machine"),
                ExerciseEntity(24, "Hip Thrust", "Glutes", "Barbell"),
                ExerciseEntity(25, "Split Squat", "Legs", "Dumbbell"),
                ExerciseEntity(26, "Walking Lunge", "Legs", "Dumbbell"),
                ExerciseEntity(27, "Calf Raise", "Calves", "Machine"),
                ExerciseEntity(28, "Barbell Curl", "Biceps", "Barbell"),
                ExerciseEntity(29, "Dumbbell Curl", "Biceps", "Dumbbell"),
                ExerciseEntity(30, "Hammer Curl", "Biceps", "Dumbbell"),
                ExerciseEntity(31, "Preacher Curl", "Biceps", "Barbell"),
                ExerciseEntity(32, "Cable Curl", "Biceps", "Cable"),
                ExerciseEntity(33, "Tricep Pushdown", "Triceps", "Cable"),
                ExerciseEntity(34, "Skull Crusher", "Triceps", "Barbell"),
                ExerciseEntity(35, "Close-Grip Bench", "Triceps", "Barbell"),
                ExerciseEntity(36, "Overhead Tricep Ext", "Triceps", "Dumbbell"),
                ExerciseEntity(37, "Tricep Kickback", "Triceps", "Dumbbell"),
                ExerciseEntity(38, "Plank", "Core", "Bodyweight"),
                ExerciseEntity(39, "Hanging Leg Raise", "Core", "Bodyweight"),
                ExerciseEntity(40, "Ab Wheel Rollout", "Core", "Bodyweight"),
                ExerciseEntity(41, "Cable Crunch", "Core", "Cable"),
                ExerciseEntity(42, "Russian Twist", "Core", "Bodyweight"),
                ExerciseEntity(43, "Chest Press", "Chest", "Machine"),
                ExerciseEntity(44, "Seated Row", "Back", "Machine"),
                ExerciseEntity(45, "Hack Squat", "Legs", "Machine"),
                ExerciseEntity(46, "Bulgarian Split Squat", "Legs", "Dumbbell"),
                ExerciseEntity(47, "Seated Calf Raise", "Calves", "Machine"),
                ExerciseEntity(48, "EZ-Bar Curl", "Biceps", "Barbell"),
                ExerciseEntity(49, "Rope Overhead Extension", "Triceps", "Cable"),
                ExerciseEntity(50, "Pallof Press", "Core", "Cable"),
                ExerciseEntity(51, "Back Extension", "Back", "Machine"),
                ExerciseEntity(52, "Good Morning", "Hamstrings", "Barbell"),
            )
            val additions = canonical
                .filter { it.name.lowercase() !in existingNames }
                .mapIndexed { index, exercise -> exercise.copy(id = nextId + index) }
            if (additions.isEmpty()) state else state.copy(exercises = state.exercises + additions)
        }
    }

    private fun buildWeeklySummary(snapshot: RepositorySnapshot, progress: ProgressOverview): String {
        if (snapshot.sessions.isEmpty() && snapshot.meals.isEmpty()) {
            return "Log je eerste training of maaltijd om je wekelijkse samenvatting te ontgrendelen."
        }
        val volume = progress.volumeTrend.takeLast(7).sumOf { it.value }.toInt()
        val adherence = calculateAdherence(snapshot)
        return "This week: $volume kg training volume, ${progress.estimatedOneRepMax.toInt()} kg estimated 1RM, and $adherence% adherence."
    }

    private fun buildTrainingInsights(snapshot: RepositorySnapshot, progress: ProgressOverview): List<String> {
        val insights = mutableListOf<String>()
        val activeRoutine = buildWorkoutOverview(snapshot).activeRoutine
        if (activeRoutine == null) {
            insights += "Kies of maak een actieve routine zodat je volgende training gepland kan worden."
        } else {
            insights += "Actieve routine: ${activeRoutine.name} met ${activeRoutine.days.size} trainingsdagen."
        }
        if (snapshot.sessions.isEmpty()) {
            insights += "Er is nog geen trainingshistorie. Rond een workout af om volume en herstel te volgen."
        } else {
            insights += "Je laatste geschatte 1RM staat op ${progress.estimatedOneRepMax.toInt()} kg."
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
            "Je intake ligt nog ${remainingCalories.coerceAtLeast(0)} kcal onder je target. TEF is ${energyBalance.tefCalories} kcal en je energiebalans staat op ${energyBalance.balance} kcal."
        } else {
            "Je caloriedoel is vandaag gehaald. Houd je eiwitten hoog; je actuele energiebalans staat op ${energyBalance.balance} kcal."
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
            .sortedByDescending { it.date }
            .map { it.toDomain(sessionVolumes[it.id] ?: 0.0) }
        return WorkoutOverview(
            activeRoutine = routines.firstOrNull { it.active },
            routines = routines,
            exercises = snapshot.exercises.map { it.toDomain() }.sortedBy { it.name },
            history = history,
        )
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
            latestPerformanceLabel = latest?.sets?.joinToString { "${formatWeight(it.weightKg)}x${it.reps}" } ?: "-",
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
        val weightTrend = snapshot.measurements.map { ChartPoint(it.date.toReadableDate(), it.weight) }
        val bodyFatTrend = snapshot.measurements.map { ChartPoint(it.date.toReadableDate(), it.bodyFat) }
        val progressionSets = snapshot.sets.filter(::isProgressionSet)
        val volumeBySession = progressionSets.groupBy { it.sessionId }.mapValues { analyticsEngine.trainingVolume(it.value) }
        val volumeTrend = snapshot.sessions
            .sortedBy { it.date }
            .map { ChartPoint(it.date.toReadableDate(), volumeBySession[it.id] ?: 0.0) }
        val heaviestSet = progressionSets.maxByOrNull { it.weight }
        val estimatedOneRepMax = heaviestSet?.let { analyticsEngine.estimatedOneRepMax(it.weight, it.reps) } ?: 0.0
        val weeklyVolume = volumeTrend.takeLast(1).sumOf { it.value }
        val baseline = volumeTrend.dropLast(1).takeLast(3).map { it.value }.average().takeIf { !it.isNaN() && it > 0.0 } ?: weeklyVolume
        return ProgressOverview(
            measurements = snapshot.measurements,
            weightTrend = weightTrend,
            bodyFatTrend = bodyFatTrend,
            strengthTrend = volumeTrend.map { ChartPoint(it.label, estimatedOneRepMax) },
            volumeTrend = volumeTrend,
            estimatedOneRepMax = estimatedOneRepMax,
            fatigueIndex = if (weeklyVolume == 0.0 || baseline == 0.0) 0.0 else analyticsEngine.fatigueIndex(weeklyVolume, baseline),
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
            snapshot.sessions.isEmpty() -> "Je bent klaar om te starten. Plan ${nextWorkout.name} als eerste sessie voor je doel '${profile.goal}'."
            proteinGap > 20 -> "Je volgende workout is ${nextWorkout.name}. Voeg vandaag nog ongeveer $proteinGap g eiwit toe en houd rekening met ${todaysWorkoutCalories} kcal strength work."
            else -> "Je ligt op koers voor ${profile.goal}. Volgende training: ${nextWorkout.name}. Focus op ${profile.trainingFocus.lowercase()}."
        }
    }

    private fun calculateAdherence(snapshot: RepositorySnapshot): Int {
        val today = todayEpochMillis()
        val last7Days = (0..6).map { today - (it * 86_400_000L) }.toSet()
        val workoutDays = snapshot.sessions.map { normalizeToDay(it.date) }.toSet()
        val mealDays = snapshot.meals.map { normalizeToDay(it.timestamp) }.toSet()
        val activeDays = last7Days.count { it in workoutDays || it in mealDays }
        return (activeDays / 7.0 * 100).toInt()
    }

    private fun computeStreak(sessions: List<WorkoutSessionEntity>, meals: List<LoggedMeal>): Int {
        val activeDays = (sessions.map { normalizeToDay(it.date) } + meals.map { normalizeToDay(it.timestamp) }).toSet()
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
        localStore.update { state ->
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

internal fun defaultWorkoutSessionName(existingSessionCount: Int): String = "Session ${existingSessionCount + 1}"

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

private fun ActiveWorkoutSetDraft.toStorage() = ActiveWorkoutDraftStorage(
    weight = weight,
    reps = reps,
    rpe = rpe,
    setType = setType,
)

private fun List<ActiveWorkoutSetStorage>.replaceExerciseSet(
    exerciseId: Long,
    setIndex: Int,
    transform: (ActiveWorkoutSetStorage) -> ActiveWorkoutSetStorage,
): List<ActiveWorkoutSetStorage> {
    var exerciseIndex = -1
    return map { set ->
        if (set.exerciseId != exerciseId) {
            set
        } else {
            exerciseIndex += 1
            if (exerciseIndex == setIndex) transform(set) else set
        }
    }
}

private fun List<ActiveWorkoutSetStorage>.filterIndexedForExercise(
    exerciseId: Long,
    predicate: (Int) -> Boolean,
): List<ActiveWorkoutSetStorage> {
    var exerciseIndex = -1
    return filter { set ->
        if (set.exerciseId != exerciseId) {
            true
        } else {
            exerciseIndex += 1
            predicate(exerciseIndex)
        }
    }
}

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

internal fun resolveReadiness(
    lastSessionAvgRpe: Float?,
    recentAverageRir: Float?,
    completedRecentSessions: Boolean,
    plateauDetected: Boolean = false,
): ReadinessLevel {
    val effectiveRir = recentAverageRir
        ?: StrengthCalculator.estimateRepsInReserve(lastSessionAvgRpe?.toDouble() ?: 0.0)?.toFloat()
        ?: 0f
    return when {
        (lastSessionAvgRpe ?: 0f) > 9f -> ReadinessLevel.DELOAD
        recentAverageRir != null && recentAverageRir < 1f -> ReadinessLevel.DELOAD
        plateauDetected && effectiveRir >= 2f -> ReadinessLevel.PLATEAU
        effectiveRir >= 2f && completedRecentSessions -> ReadinessLevel.INCREASE
        else -> ReadinessLevel.MAINTAIN
    }
}

private data class ExerciseSessionSnapshot(
    val date: Long,
    val sets: List<WorkoutSetEntity>,
)

private fun hasPlateaued(sessions: List<ExerciseSessionSnapshot>): Boolean {
    if (sessions.size < 3) return false
    val e1rms = sessions.take(3).map { session ->
        session.sets.maxOfOrNull { StrengthCalculator.estimateOneRepMax(it.weight, it.reps) } ?: 0.0
    }.filter { it > 0.0 }
    if (e1rms.size < 3) return false
    val min = e1rms.minOrNull() ?: return false
    val max = e1rms.maxOrNull() ?: return false
    return min > 0.0 && ((max - min) / min) < 0.01
}

private fun progressionLoadStep(exerciseName: String, muscleGroup: String, equipment: String): Double {
    val compoundPattern = listOf("squat", "bench", "deadlift", "press", "row", "pull-up", "hip thrust")
    val normalized = "$exerciseName $muscleGroup $equipment".lowercase()
    return when {
        equipment.contains("dumbbell", ignoreCase = true) -> 1.0
        normalized.contains("raise") || normalized.contains("curl") || normalized.contains("extension") -> 1.0
        compoundPattern.any { normalized.contains(it) } -> 2.5
        else -> 1.25
    }
}

private fun WorkoutSetEntity.progressionSetType(): SetType = parseSetType(setType)

private fun isProgressionSet(set: WorkoutSetEntity): Boolean =
    set.completed && set.reps > 0 && set.weight >= 0.0 && set.progressionSetType().isProgressionType()

private fun SetType.isProgressionType(): Boolean = this == SetType.NORMAL || this == SetType.BACK_OFF

private fun com.trainiq.ai.services.GeneratedRoutine.toDomainGeneratedRoutine() = GeneratedRoutine(
    routineName = routineName,
    routineDescription = routineDescription,
    periodizationNote = periodizationNote,
    estimatedDurationMinutes = estimatedDurationMinutes,
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
