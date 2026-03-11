package com.trainiq.data.repository

import com.trainiq.ai.services.GoalAdvisorService
import com.trainiq.ai.services.MealAnalysisService
import com.trainiq.ai.services.WeeklyReportService
import com.trainiq.ai.services.WorkoutDebriefService
import com.trainiq.analytics.AnalyticsEngine
import com.trainiq.core.database.MealEntity
import com.trainiq.core.database.WorkoutSessionEntity
import com.trainiq.core.database.WorkoutSetEntity
import com.trainiq.core.datastore.UserPreferencesRepository
import com.trainiq.core.util.toReadableDate
import com.trainiq.core.util.todayEpochMillis
import com.trainiq.data.datasource.HealthConnectDataSource
import com.trainiq.data.mapper.toDomain
import com.trainiq.domain.model.ChartPoint
import com.trainiq.domain.model.CoachOverview
import com.trainiq.domain.model.GoalAdvice
import com.trainiq.domain.model.HomeDashboard
import com.trainiq.domain.model.LoggedSet
import com.trainiq.domain.model.MealScanItem
import com.trainiq.domain.model.NutritionOverview
import com.trainiq.domain.model.ProgressOverview
import com.trainiq.domain.model.WorkoutDay
import com.trainiq.domain.model.WorkoutDebrief
import com.trainiq.domain.model.WorkoutOverview
import com.trainiq.domain.repository.CoachRepository
import com.trainiq.domain.repository.HomeRepository
import com.trainiq.domain.repository.NutritionRepository
import com.trainiq.domain.repository.ProgressRepository
import com.trainiq.domain.repository.WorkoutRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

@Singleton
class TrainIqRepository @Inject constructor(
    private val healthConnectDataSource: HealthConnectDataSource,
    private val preferencesRepository: UserPreferencesRepository,
    private val analyticsEngine: AnalyticsEngine,
    private val mealAnalysisService: MealAnalysisService,
    private val workoutDebriefService: WorkoutDebriefService,
    private val goalAdvisorService: GoalAdvisorService,
    private val weeklyReportService: WeeklyReportService,
) : HomeRepository, WorkoutRepository, NutritionRepository, ProgressRepository, CoachRepository {

    private var routines = mutableListOf<com.trainiq.core.database.WorkoutRoutineEntity>()
    private var days = mutableListOf<com.trainiq.core.database.WorkoutDayEntity>()
    private var exercises = mutableListOf<com.trainiq.core.database.ExerciseEntity>()
    private var workoutExercises = mutableListOf<com.trainiq.core.database.WorkoutExerciseEntity>()
    private var meals = mutableListOf<MealEntity>()
    private var measurements = mutableListOf<com.trainiq.core.database.BodyMeasurementEntity>()
    private var sessions = mutableListOf<WorkoutSessionEntity>()
    private var workoutSets = mutableListOf<WorkoutSetEntity>()
    private var scannedMealItems = emptyList<MealScanItem>()

    private val workoutOverviewState = MutableStateFlow(buildWorkoutOverview())
    private val nutritionOverviewState = MutableStateFlow(buildNutritionOverview())
    private val progressOverviewState = MutableStateFlow(buildProgressOverview())

    private fun refresh() {
        workoutOverviewState.value = buildWorkoutOverview()
        nutritionOverviewState.value = buildNutritionOverview()
        progressOverviewState.value = buildProgressOverview()
    }

    private fun buildWorkoutDay(dayId: Long): WorkoutDay? =
        days.firstOrNull { it.id == dayId }?.toDomain(
            workoutExercises.filter { it.dayId == dayId }.mapNotNull { entry ->
                exercises.firstOrNull { it.id == entry.exerciseId }?.toDomain()?.let(entry::toDomain)
            },
        )

    private fun buildWorkoutOverview(): WorkoutOverview {
        val dayMap = days.associate { it.id to buildWorkoutDay(it.id) }
        val routinesDomain = routines.map { routine ->
            routine.toDomain(
                days.filter { it.routineId == routine.id }
                    .sortedBy { it.orderIndex }
                    .mapNotNull { dayMap[it.id] },
            )
        }
        val sessionVolumes = workoutSets.groupBy { it.sessionId }.mapValues { analyticsEngine.trainingVolume(it.value) }
        return WorkoutOverview(
            activeRoutine = routinesDomain.firstOrNull { it.active },
            routines = routinesDomain,
            exercises = exercises.map { it.toDomain() },
            history = sessions.map { it.toDomain(sessionVolumes[it.id] ?: 0.0) },
        )
    }

    private fun buildNutritionOverview(): NutritionOverview {
        val today = todayEpochMillis()
        val todaysMeals = meals.filter { it.date >= today }.map { it.toDomain() }
        return NutritionOverview(
            meals = meals.map { it.toDomain() },
            todaysCalories = todaysMeals.sumOf { it.calories },
            todaysProtein = todaysMeals.sumOf { it.protein },
            todaysCarbs = todaysMeals.sumOf { it.carbs },
            todaysFat = todaysMeals.sumOf { it.fat },
            recipes = emptyList(),
            scannedItems = scannedMealItems,
        )
    }

    private fun buildProgressOverview(): ProgressOverview {
        val weightTrend = measurements.map { ChartPoint(it.date.toReadableDate(), it.weight) }
        val bodyFatTrend = measurements.map { ChartPoint(it.date.toReadableDate(), it.bodyFat) }
        val volumeBySession = workoutSets.groupBy { it.sessionId }.mapValues { analyticsEngine.trainingVolume(it.value) }
        val volumeTrend = sessions.map { ChartPoint(it.date.toReadableDate(), volumeBySession[it.id] ?: 0.0) }
        val heaviestSet = workoutSets.maxByOrNull { it.weight }
        val estimatedOneRepMax = heaviestSet?.let { analyticsEngine.estimatedOneRepMax(it.weight, it.reps) } ?: 0.0
        val weeklyVolume = volumeTrend.takeLast(1).sumOf { it.value }
        val baseline = volumeTrend.dropLast(1).takeLast(3).map { it.value }.average().takeIf { !it.isNaN() && it > 0.0 } ?: weeklyVolume
        return ProgressOverview(
            weightTrend = weightTrend,
            bodyFatTrend = bodyFatTrend,
            strengthTrend = volumeTrend.map { ChartPoint(it.label, estimatedOneRepMax) },
            volumeTrend = volumeTrend,
            estimatedOneRepMax = estimatedOneRepMax,
            fatigueIndex = if (weeklyVolume == 0.0) 0.0 else analyticsEngine.fatigueIndex(weeklyVolume, baseline),
        )
    }

    override fun observeDashboard(): Flow<HomeDashboard> = preferencesRepository.streakCount.map { streak ->
        val today = todayEpochMillis()
        val todaysMeals = meals.filter { it.date >= today }
        val activeRoutine = routines.firstOrNull { it.active }
        val nextWorkout = activeRoutine?.let { routine ->
            days.filter { it.routineId == routine.id }.sortedBy { it.orderIndex }.firstOrNull()?.let { buildWorkoutDay(it.id) }
        }
        HomeDashboard(
            calorieProgress = todaysMeals.sumOf { it.calories },
            calorieTarget = 0,
            proteinProgress = todaysMeals.sumOf { it.protein },
            proteinTarget = 0,
            steps = healthConnectDataSource.getTodaySteps(),
            nextWorkout = nextWorkout,
            streak = streak,
            aiInsight = "",
        )
    }

    override fun observeWorkoutOverview(): Flow<WorkoutOverview> = workoutOverviewState.asStateFlow()

    override suspend fun getWorkoutDay(dayId: Long): WorkoutDay? = buildWorkoutDay(dayId)

    override suspend fun getNextWorkoutDay(): WorkoutDay? =
        routines.firstOrNull { it.active }?.let { routine ->
            days.filter { it.routineId == routine.id }.sortedBy { it.orderIndex }.firstOrNull()?.let { buildWorkoutDay(it.id) }
        }

    override suspend fun setActiveRoutine(routineId: Long) {
        routines = routines.map { it.copy(active = it.id == routineId) }.toMutableList()
        refresh()
    }

    override suspend fun finishWorkout(dayId: Long, durationSeconds: Long, loggedSets: List<LoggedSet>): WorkoutDebrief {
        val nextSessionId = (sessions.maxOfOrNull { it.id } ?: 0) + 1
        sessions.add(0, WorkoutSessionEntity(id = nextSessionId, date = System.currentTimeMillis(), duration = durationSeconds))
        loggedSets.forEachIndexed { index, set ->
            workoutSets.add(
                0,
                WorkoutSetEntity(
                    id = (workoutSets.maxOfOrNull { it.id } ?: 0) + index + 1,
                    sessionId = nextSessionId,
                    exerciseId = set.exerciseId,
                    weight = set.weight,
                    reps = set.reps,
                    rpe = set.rpe,
                ),
            )
        }
        refresh()
        val currentVolume = loggedSets.sumOf { it.weight * it.reps }
        val previousVolume = workoutSets
            .filter { it.sessionId != nextSessionId }
            .groupBy { it.sessionId }
            .maxByOrNull { it.key }
            ?.value
            ?.let(analyticsEngine::trainingVolume)
            ?: currentVolume
        val progression = if (previousVolume == 0.0) 0.0 else ((currentVolume - previousVolume) / previousVolume) * 100
        val distribution = buildWorkoutDay(dayId)?.exercises
            ?.groupBy { it.exercise.muscleGroup }
            ?.map { "${it.key} ${it.value.size}" }
            ?.joinToString()
            .orEmpty()
        return workoutDebriefService.generateWorkoutDebrief(currentVolume, progression, distribution)
    }

    override fun observeNutritionOverview(): Flow<NutritionOverview> = nutritionOverviewState.asStateFlow()

    override suspend fun analyzeMealPhoto(path: String): NutritionOverview {
        scannedMealItems = mealAnalysisService.analyzeMealImage(path)
        refresh()
        return nutritionOverviewState.value
    }

    override suspend fun saveScannedMeal(items: List<MealScanItem>) {
        if (items.isEmpty()) return
        meals.add(
            0,
            MealEntity(
                id = (meals.maxOfOrNull { it.id } ?: 0) + 1,
                date = System.currentTimeMillis(),
                calories = items.sumOf { it.calories },
                protein = items.sumOf { it.protein },
                carbs = items.sumOf { it.carbs },
                fat = items.sumOf { it.fat },
            ),
        )
        scannedMealItems = emptyList()
        refresh()
    }

    override fun observeProgressOverview(): Flow<ProgressOverview> = progressOverviewState.asStateFlow()

    override fun observeCoachOverview(): Flow<CoachOverview> = progressOverviewState.map { progress ->
        if (progress.weightTrend.isEmpty() && progress.volumeTrend.isEmpty()) {
            CoachOverview(
                weeklyReport = "",
                trainingInsights = emptyList(),
                nutritionCoachMessage = "",
            )
        } else {
            CoachOverview(
                weeklyReport = weeklyReportService.generateWeeklyReport(
                    progress.volumeTrend.takeLast(1).sumOf { it.value },
                    progress.weightTrend.lastOrNull()?.value ?: 0.0,
                    0,
                ),
                trainingInsights = emptyList(),
                nutritionCoachMessage = "",
            )
        }
    }

    override suspend fun generateGoalAdvice(height: Double, weight: Double, bodyFat: Double, goal: String): GoalAdvice =
        goalAdvisorService.generateGoalAdvice(height, weight, bodyFat, goal)
}
