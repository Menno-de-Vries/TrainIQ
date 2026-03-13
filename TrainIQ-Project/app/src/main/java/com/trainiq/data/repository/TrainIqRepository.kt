package com.trainiq.data.repository

import com.trainiq.ai.services.GoalAdvisorService
import com.trainiq.ai.services.MealAnalysisService
import com.trainiq.ai.services.WeeklyReportService
import com.trainiq.ai.services.WorkoutDebriefService
import com.trainiq.analytics.AnalyticsEngine
import com.trainiq.core.database.BodyMeasurementEntity
import com.trainiq.core.database.ExerciseEntity
import com.trainiq.core.database.MealEntity
import com.trainiq.core.database.UserProfileEntity
import com.trainiq.core.database.WorkoutDayEntity
import com.trainiq.core.database.WorkoutExerciseEntity
import com.trainiq.core.database.WorkoutRoutineEntity
import com.trainiq.core.database.WorkoutSessionEntity
import com.trainiq.core.database.WorkoutSetEntity
import com.trainiq.core.util.toReadableDate
import com.trainiq.core.util.todayEpochMillis
import com.trainiq.data.datasource.HealthConnectDataSource
import com.trainiq.data.local.TrainIqLocalStore
import com.trainiq.data.mapper.toDomain
import com.trainiq.domain.model.BodyMeasurement
import com.trainiq.domain.model.ChartPoint
import com.trainiq.domain.model.CoachOverview
import com.trainiq.domain.model.GoalAdvice
import com.trainiq.domain.model.HomeDashboard
import com.trainiq.domain.model.LoggedSet
import com.trainiq.domain.model.Meal
import com.trainiq.domain.model.MealScanItem
import com.trainiq.domain.model.NutritionOverview
import com.trainiq.domain.model.ProgressOverview
import com.trainiq.domain.model.UserProfile
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
) : HomeRepository, WorkoutRepository, NutritionRepository, ProgressRepository, CoachRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val scannedMealItems = MutableStateFlow<List<MealScanItem>>(emptyList())

    private val snapshotState: StateFlow<RepositorySnapshot> = combine(localStore.state, scannedMealItems) { state, scanned ->
        RepositorySnapshot(
            profile = state.profile?.toDomain(),
            routines = state.routines,
            days = state.days,
            exercises = state.exercises,
            workoutExercises = state.workoutExercises,
            sessions = state.sessions,
            sets = state.workoutSets,
            meals = state.meals.map { it.toDomain() },
            measurements = state.measurements.map { it.toDomain() },
            scannedMealItems = scanned,
        )
    }.stateIn(scope, SharingStarted.Eagerly, RepositorySnapshot())

    init {
        scope.launch { ensureExerciseLibrary() }
    }

    override fun observeDashboard(): Flow<HomeDashboard> = snapshotState.map { snapshot ->
        val activeRoutine = buildWorkoutOverview(snapshot).activeRoutine
        val nextWorkout = activeRoutine?.days?.minByOrNull { it.orderIndex }
        val todaysMeals = snapshot.meals.filter { it.date >= todayEpochMillis() }
        val profile = snapshot.profile
        HomeDashboard(
            profile = profile,
            calorieProgress = todaysMeals.sumOf { it.calories },
            calorieTarget = profile?.calorieTarget ?: 0,
            proteinProgress = todaysMeals.sumOf { it.protein },
            proteinTarget = profile?.proteinTarget ?: 0,
            steps = null,
            nextWorkout = nextWorkout,
            streak = computeStreak(snapshot.sessions, snapshot.meals),
            aiInsight = buildDashboardInsight(snapshot, nextWorkout),
        )
    }

    override suspend fun getHealthConnectStatus() = healthConnectDataSource.getStatus()

    override fun observeWorkoutOverview(): Flow<WorkoutOverview> = snapshotState.map(::buildWorkoutOverview)

    override suspend fun getWorkoutDay(dayId: Long): WorkoutDay? = buildWorkoutDay(snapshotState.value, dayId)

    override suspend fun getNextWorkoutDay(): WorkoutDay? =
        buildWorkoutOverview(snapshotState.value).activeRoutine?.days?.minByOrNull { it.orderIndex }

    override suspend fun setActiveRoutine(routineId: Long) {
        localStore.update { state ->
            state.copy(routines = state.routines.map { it.copy(active = it.id == routineId) })
        }
    }

    override suspend fun finishWorkout(dayId: Long, durationSeconds: Long, loggedSets: List<LoggedSet>): WorkoutDebrief {
        if (loggedSets.isEmpty()) {
            return WorkoutDebrief(
                summary = "Geen sets gelogd.",
                progressionFeedback = "Log minimaal een set om voortgang op te slaan.",
                recommendation = "Voeg tijdens je training sets toe voordat je afrondt.",
            )
        }

        val current = localStore.state.value
        val sessionId = (current.sessions.maxOfOrNull { it.id } ?: 0L) + 1L
        val newSession = WorkoutSessionEntity(id = sessionId, date = System.currentTimeMillis(), duration = durationSeconds)
        val newSets = loggedSets.mapIndexed { index, set ->
            WorkoutSetEntity(
                id = (current.workoutSets.maxOfOrNull { it.id } ?: 0L) + index + 1L,
                sessionId = sessionId,
                exerciseId = set.exerciseId,
                weight = set.weight,
                reps = set.reps,
                rpe = set.rpe,
            )
        }
        localStore.update { state ->
            state.copy(
                sessions = listOf(newSession) + state.sessions,
                workoutSets = newSets + state.workoutSets,
            )
        }

        val snapshot = snapshotState.value
        val currentVolume = loggedSets.sumOf { it.weight * it.reps }
        val previousVolume = snapshot.sessions
            .firstOrNull()
            ?.let { latest ->
                snapshot.sets.filter { it.sessionId == latest.id }.sumOf { it.weight * it.reps }
            }
            ?.takeIf { it > 0.0 }
            ?: currentVolume
        val progression = if (previousVolume == 0.0) 0.0 else ((currentVolume - previousVolume) / previousVolume) * 100
        val distribution = buildWorkoutDay(snapshot, dayId)?.exercises
            ?.groupBy { it.exercise.muscleGroup }
            ?.map { "${it.key} ${it.value.size}" }
            ?.joinToString()
            .orEmpty()
        return workoutDebriefService.generateWorkoutDebrief(currentVolume, progression, distribution)
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
            )
        }
    }

    override suspend fun addWorkoutDay(routineId: Long, name: String) {
        localStore.update { state ->
            val dayId = (state.days.maxOfOrNull { it.id } ?: 0L) + 1L
            val nextOrder = state.days.count { it.routineId == routineId }
            state.copy(days = state.days + WorkoutDayEntity(dayId, routineId, name, nextOrder))
        }
    }

    override suspend fun removeWorkoutDay(dayId: Long) {
        localStore.update { state ->
            state.copy(
                days = state.days.filterNot { it.id == dayId },
                workoutExercises = state.workoutExercises.filterNot { it.dayId == dayId },
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
    ) {
        localStore.update { state ->
            val existing = state.exercises.firstOrNull {
                it.name.equals(name, ignoreCase = true) &&
                    it.muscleGroup.equals(muscleGroup, ignoreCase = true) &&
                    it.equipment.equals(equipment, ignoreCase = true)
            }
            val exerciseId = existing?.id ?: ((state.exercises.maxOfOrNull { it.id } ?: 0L) + 1L)
            val exercise = existing ?: ExerciseEntity(exerciseId, name, muscleGroup, equipment)
            val workoutExerciseId = (state.workoutExercises.maxOfOrNull { it.id } ?: 0L) + 1L
            state.copy(
                exercises = if (existing == null) state.exercises + exercise else state.exercises,
                workoutExercises = state.workoutExercises + WorkoutExerciseEntity(
                    id = workoutExerciseId,
                    dayId = dayId,
                    exerciseId = exerciseId,
                    targetSets = targetSets,
                    repRange = repRange,
                    restSeconds = restSeconds,
                ),
            )
        }
    }

    override suspend fun removeExerciseFromDay(workoutExerciseId: Long) {
        localStore.update { state ->
            state.copy(workoutExercises = state.workoutExercises.filterNot { it.id == workoutExerciseId })
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

    override fun observeNutritionOverview(): Flow<NutritionOverview> = snapshotState.map(::buildNutritionOverview)

    override suspend fun analyzeMealPhoto(path: String): NutritionOverview {
        scannedMealItems.value = mealAnalysisService.analyzeMealImage(path)
        return buildNutritionOverview(snapshotState.value.copy(scannedMealItems = scannedMealItems.value))
    }

    override suspend fun saveScannedMeal(items: List<MealScanItem>) {
        if (items.isEmpty()) return
        addMeal(
            calories = items.sumOf { it.calories },
            protein = items.sumOf { it.protein },
            carbs = items.sumOf { it.carbs },
            fat = items.sumOf { it.fat },
        )
        scannedMealItems.value = emptyList()
    }

    override suspend fun addMeal(calories: Int, protein: Int, carbs: Int, fat: Int) {
        localStore.update { state ->
            val mealId = (state.meals.maxOfOrNull { it.id } ?: 0L) + 1L
            state.copy(
                meals = listOf(
                    MealEntity(
                        id = mealId,
                        date = System.currentTimeMillis(),
                        calories = calories,
                        protein = protein,
                        carbs = carbs,
                        fat = fat,
                    ),
                ) + state.meals,
            )
        }
    }

    override suspend fun updateMeal(mealId: Long, calories: Int, protein: Int, carbs: Int, fat: Int) {
        localStore.update { state ->
            state.copy(
                meals = state.meals.map { meal ->
                    if (meal.id == mealId) {
                        meal.copy(calories = calories, protein = protein, carbs = carbs, fat = fat)
                    } else {
                        meal
                    }
                },
            )
        }
    }

    override suspend fun deleteMeal(mealId: Long) {
        localStore.update { state ->
            state.copy(meals = state.meals.filterNot { it.id == mealId })
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

    override suspend fun generateGoalAdvice(height: Double, weight: Double, bodyFat: Double, goal: String): GoalAdvice =
        goalAdvisorService.generateGoalAdvice(height, weight, bodyFat, goal)

    override suspend fun generateWeeklyReport(): String {
        val snapshot = snapshotState.value
        val progress = buildProgressOverview(snapshot)
        return if (snapshot.sessions.isEmpty() && snapshot.meals.isEmpty()) {
            "Log je eerste training of maaltijd om een wekelijkse coachsamenvatting te krijgen."
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
        if (localStore.state.value.exercises.isNotEmpty()) return
        localStore.update { state ->
            state.copy(
                exercises = listOf(
                    ExerciseEntity(1, "Squat", "Legs", "Barbell"),
                    ExerciseEntity(2, "Bench Press", "Chest", "Barbell"),
                    ExerciseEntity(3, "Deadlift", "Back", "Barbell"),
                    ExerciseEntity(4, "Overhead Press", "Shoulders", "Barbell"),
                    ExerciseEntity(5, "Lat Pulldown", "Back", "Cable"),
                    ExerciseEntity(6, "Leg Press", "Legs", "Machine"),
                    ExerciseEntity(7, "Romanian Deadlift", "Hamstrings", "Barbell"),
                    ExerciseEntity(8, "Dumbbell Row", "Back", "Dumbbell"),
                ),
            )
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
        val remainingCalories = profile.calorieTarget - nutrition.todaysCalories
        return if (remainingCalories > 0) {
            "Je zit vandaag nog $remainingCalories kcal onder je doel. Richt je vooral op eiwitten en volwaardige koolhydraten."
        } else {
            "Je caloriedoel is vandaag gehaald. Houd je eiwitten hoog en plan je volgende maaltijd licht."
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
            .mapNotNull { workoutExercise ->
                snapshot.exercises.firstOrNull { it.id == workoutExercise.exerciseId }
                    ?.toDomain()
                    ?.let(workoutExercise::toDomain)
            }
        return day.toDomain(exercisePlans)
    }

    private fun buildNutritionOverview(snapshot: RepositorySnapshot): NutritionOverview {
        val todaysMeals = snapshot.meals.filter { it.date >= todayEpochMillis() }
        return NutritionOverview(
            meals = snapshot.meals.sortedByDescending { it.date },
            todaysCalories = todaysMeals.sumOf { it.calories },
            todaysProtein = todaysMeals.sumOf { it.protein },
            todaysCarbs = todaysMeals.sumOf { it.carbs },
            todaysFat = todaysMeals.sumOf { it.fat },
            recipes = emptyList(),
            scannedItems = snapshot.scannedMealItems,
        )
    }

    private fun buildProgressOverview(snapshot: RepositorySnapshot): ProgressOverview {
        val weightTrend = snapshot.measurements.map { ChartPoint(it.date.toReadableDate(), it.weight) }
        val bodyFatTrend = snapshot.measurements.map { ChartPoint(it.date.toReadableDate(), it.bodyFat) }
        val volumeBySession = snapshot.sets.groupBy { it.sessionId }.mapValues { analyticsEngine.trainingVolume(it.value) }
        val volumeTrend = snapshot.sessions
            .sortedBy { it.date }
            .map { ChartPoint(it.date.toReadableDate(), volumeBySession[it.id] ?: 0.0) }
        val heaviestSet = snapshot.sets.maxByOrNull { it.weight }
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
        val todaysProtein = snapshot.meals.filter { it.date >= todayEpochMillis() }.sumOf { it.protein }
        val proteinGap = profile.proteinTarget - todaysProtein
        return when {
            snapshot.sessions.isEmpty() -> "Je bent klaar om te starten. Plan ${nextWorkout.name} als eerste sessie voor je doel '${profile.goal}'."
            proteinGap > 20 -> "Je volgende workout is ${nextWorkout.name}. Voeg vandaag nog ongeveer $proteinGap g eiwit toe voor beter herstel."
            else -> "Je ligt op koers voor ${profile.goal}. Volgende training: ${nextWorkout.name}. Focus op ${profile.trainingFocus.lowercase()}."
        }
    }

    private fun calculateAdherence(snapshot: RepositorySnapshot): Int {
        val today = todayEpochMillis()
        val last7Days = (0..6).map { today - (it * 86_400_000L) }.toSet()
        val workoutDays = snapshot.sessions.map { normalizeToDay(it.date) }.toSet()
        val mealDays = snapshot.meals.map { normalizeToDay(it.date) }.toSet()
        val activeDays = last7Days.count { it in workoutDays || it in mealDays }
        return (activeDays / 7.0 * 100).toInt()
    }

    private fun computeStreak(sessions: List<WorkoutSessionEntity>, meals: List<Meal>): Int {
        val activeDays = (sessions.map { normalizeToDay(it.date) } + meals.map { normalizeToDay(it.date) }).toSet()
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

    private data class RepositorySnapshot(
        val profile: UserProfile? = null,
        val routines: List<WorkoutRoutineEntity> = emptyList(),
        val days: List<WorkoutDayEntity> = emptyList(),
        val exercises: List<ExerciseEntity> = emptyList(),
        val workoutExercises: List<WorkoutExerciseEntity> = emptyList(),
        val sessions: List<WorkoutSessionEntity> = emptyList(),
        val sets: List<WorkoutSetEntity> = emptyList(),
        val meals: List<Meal> = emptyList(),
        val measurements: List<BodyMeasurement> = emptyList(),
        val scannedMealItems: List<MealScanItem> = emptyList(),
    )
}
