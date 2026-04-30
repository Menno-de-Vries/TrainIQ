package com.trainiq.domain.model

data class UserProfile(
    val id: Long,
    val name: String,
    val age: Int,
    val sex: BiologicalSex,
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

data class Exercise(
    val id: Long,
    val name: String,
    val muscleGroup: String,
    val equipment: String,
)

data class WorkoutExercisePlan(
    val id: Long,
    val exercise: Exercise,
    val targetSets: Int,
    val repRange: String,
    val restSeconds: Int,
    val targetWeightKg: Double = 0.0,
    val targetRpe: Double = 0.0,
    val setType: SetType = SetType.NORMAL,
    val supersetGroupId: Long? = null,
    val sets: List<RoutineSet> = emptyList(),
)

data class RoutineSet(
    val id: Long,
    val workoutExerciseId: Long,
    val orderIndex: Int,
    val setType: SetType = SetType.NORMAL,
    val targetReps: Int = 0,
    val targetWeightKg: Double = 0.0,
    val restSeconds: Int = 0,
    val targetRpe: Double = 0.0,
    val targetRir: Int? = null,
)

data class WorkoutDay(
    val id: Long,
    val routineId: Long,
    val name: String,
    val orderIndex: Int,
    val exercises: List<WorkoutExercisePlan>,
)

data class WorkoutRoutine(
    val id: Long,
    val name: String,
    val description: String,
    val active: Boolean,
    val days: List<WorkoutDay>,
)

data class WorkoutSessionSummary(
    val id: Long,
    val date: Long,
    val duration: Long,
    val caloriesBurned: Int,
    val totalVolume: Double,
)

data class ExerciseHistory(
    val exercise: Exercise?,
    val stats: ExerciseStats,
    val sessions: List<ExerciseHistorySession>,
    val volumePoints: List<ChartPoint>,
    val bestWeightPoints: List<ChartPoint>,
    val estimatedOneRepMaxPoints: List<ChartPoint>,
    val rank: ExerciseRankProgress,
)

data class ExerciseHistorySession(
    val sessionId: Long,
    val startedAt: Long,
    val endedAt: Long,
    val durationSeconds: Long,
    val totalVolume: Double,
    val bestWeightKg: Double,
    val bestEstimatedOneRepMax: Double,
    val averageRpe: Double?,
    val sets: List<ExerciseHistorySet>,
)

data class ExerciseHistorySet(
    val orderIndex: Int,
    val reps: Int,
    val weightKg: Double,
    val setType: SetType,
    val restSeconds: Int,
    val rpe: Double,
    val repsInReserve: Int?,
    val completed: Boolean,
)

data class ExerciseStats(
    val lastPerformedAt: Long?,
    val completedSessions: Int,
    val totalSets: Int,
    val highestWeightKg: Double,
    val mostReps: Int,
    val bestEstimatedOneRepMax: Double,
    val bestSetLabel: String,
    val latestPerformanceLabel: String,
    val averageRpe: Double?,
    val totalVolume: Double,
    val progressSincePreviousPercent: Double?,
)

data class ExerciseRankProgress(
    val rank: ExerciseRank,
    val score: Double,
    val nextRank: ExerciseRank?,
    val progressToNext: Float,
    val pointsToNext: Double,
    val message: String,
)

enum class ExerciseRank(val label: String, val threshold: Double) {
    BEGINNER("Beginner", 0.0),
    NOVICE("Novice", 75.0),
    INTERMEDIATE("Intermediate", 150.0),
    ADVANCED("Advanced", 300.0),
    ELITE("Elite", 550.0),
}

data class LoggedSet(
    val id: Long = 0L,
    val exerciseId: Long,
    val weight: Double,
    val reps: Int,
    val rpe: Double,
    val repsInReserve: Int? = null,
    val setType: SetType = SetType.NORMAL,
    val restSeconds: Int = 0,
    val orderIndex: Int = 0,
    val completed: Boolean = true,
    val performedExerciseId: Long = 0L,
    val sourceWorkoutExerciseId: Long? = null,
)

data class ActiveWorkoutSetDraft(
    val weight: String = "",
    val reps: String = "",
    val rpe: String = "",
    val setType: SetType = SetType.NORMAL,
)

data class ActiveWorkoutSetEntry(
    val id: Long,
    val exerciseId: Long,
    val weight: Double,
    val reps: Int,
    val rpe: Double,
    val repsInReserve: Int? = null,
    val setType: SetType = SetType.NORMAL,
    val restSeconds: Int = 0,
    val orderIndex: Int = 0,
    val completed: Boolean = true,
    val loggedAt: Long,
    val performedExerciseId: Long = 0L,
    val sourceWorkoutExerciseId: Long? = null,
) {
    fun toLoggedSet() = LoggedSet(
        id = id,
        exerciseId = exerciseId,
        performedExerciseId = performedExerciseId,
        sourceWorkoutExerciseId = sourceWorkoutExerciseId,
        weight = weight,
        reps = reps,
        rpe = rpe,
        repsInReserve = repsInReserve,
        setType = setType,
        restSeconds = restSeconds,
        orderIndex = orderIndex,
        completed = completed,
    )
}

data class ActiveWorkoutSession(
    val sessionId: Long = 0L,
    val dayId: Long,
    val routineId: Long? = null,
    val startedAt: Long,
    val updatedAt: Long,
    val loggedSets: List<ActiveWorkoutSetEntry>,
    val drafts: Map<Long, ActiveWorkoutSetDraft>,
    val collapsedExerciseIds: Set<Long>,
    val restTimerEndsAt: Long?,
    val restTimerTotalSeconds: Int,
)

enum class WorkoutLogEventType {
    ADD_SET,
    EDIT_SET,
    UNDO_SET,
    DELETE_SET,
}

enum class WorkoutSyncStatus {
    PENDING,
    SYNCED,
    FAILED,
}

data class ActiveWorkoutFocusTarget(
    val exerciseId: Long,
    val setIndex: Int,
)

data class WorkoutLoggingSummary(
    val pendingCount: Int = 0,
    val lastUndoableEventId: Long? = null,
    val lastUndoableExpiresAt: Long? = null,
    val activeFocusTarget: ActiveWorkoutFocusTarget? = null,
)

data class ProgressionSuggestion(
    val exerciseId: Long,
    val exerciseName: String,
    val suggestedWeightKg: Double,
    val suggestedReps: String,
    val lastSessionAvgRpe: Float?,
    val readinessSignal: ReadinessLevel,
    val lastLoggedWeightKg: Double? = null,
    val lastLoggedReps: String? = null,
)

enum class SetType {
    NORMAL,
    WARM_UP,
    DROP_SET,
    FAILURE,
    BACK_OFF,
}

enum class ReadinessLevel {
    INCREASE,
    MAINTAIN,
    DELOAD,
    PLATEAU,
}

data class GeneratedRoutine(
    val routineName: String,
    val routineDescription: String,
    val periodizationNote: String = "",
    val estimatedDurationMinutes: Int = 0,
    val source: GeneratedRoutineSource = GeneratedRoutineSource.GEMINI_2_5_FLASH,
    val days: List<GeneratedDay>,
)

enum class GeneratedRoutineSource {
    GEMINI_2_5_FLASH,
    LOCAL_FALLBACK,
}

data class GeneratedDay(
    val dayName: String,
    val estimatedDurationMinutes: Int = 60,
    val exercises: List<GeneratedExercise>,
)

data class GeneratedExercise(
    val exerciseName: String,
    val muscleGroup: String,
    val equipment: String,
    val targetSets: Int,
    val repRange: String,
    val restSeconds: Int,
    val coachingCue: String = "",
)

data class NutritionFacts(
    val calories: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
) {
    operator fun plus(other: NutritionFacts): NutritionFacts = NutritionFacts(
        calories = calories + other.calories,
        protein = protein + other.protein,
        carbs = carbs + other.carbs,
        fat = fat + other.fat,
    )

    companion object {
        val Zero = NutritionFacts(0.0, 0.0, 0.0, 0.0)
    }
}

enum class FoodSourceType {
    MANUAL,
    BARCODE,
    AI,
    IMPORTED,
}

data class FoodItem(
    val id: Long,
    val name: String,
    val barcode: String? = null,
    val caloriesPer100g: Double,
    val proteinPer100g: Double,
    val carbsPer100g: Double,
    val fatPer100g: Double,
    val sourceType: FoodSourceType,
    val createdAt: Long,
    val updatedAt: Long,
)

data class FoodPortion(
    val foodId: Long,
    val grams: Double,
    val nutrition: NutritionFacts,
)

data class RecipeIngredient(
    val id: Long,
    val recipeId: Long,
    val foodItemId: Long,
    val foodName: String,
    val gramsUsed: Double,
    val nutrition: NutritionFacts,
)

data class Recipe(
    val id: Long,
    val name: String,
    val notes: String? = null,
    val ingredients: List<RecipeIngredient>,
    val totalCookedGrams: Double? = null,
    val totalNutrition: NutritionFacts,
    val createdAt: Long,
    val updatedAt: Long,
)

enum class LoggedMealItemType {
    FOOD,
    RECIPE,
}

data class LoggedMealItem(
    val id: Long,
    val mealId: Long,
    val itemType: LoggedMealItemType,
    val referenceId: Long,
    val name: String,
    val gramsUsed: Double,
    val nutritionSnapshot: NutritionFacts,
    val notes: String? = null,
)

data class LoggedMeal(
    val id: Long,
    val timestamp: Long,
    val mealType: MealType,
    val name: String,
    val notes: String? = null,
    val items: List<LoggedMealItem>,
    val totalNutrition: NutritionFacts,
)

data class MealScanItem(
    val name: String,
    val estimatedGrams: Double,
    val nutrition: NutritionFacts,
    val confidence: String? = null,
    val notes: String? = null,
)

enum class MealAnalysisSource {
    API,
    LOCAL_FALLBACK,
}

data class MealAnalysisResult(
    val items: List<MealScanItem>,
    val suggestedMealType: MealType? = null,
    val notes: String? = null,
    val rawResponse: String? = null,
    val source: MealAnalysisSource = MealAnalysisSource.API,
)

data class WeeklyReportResult(
    val summary: String,
    val wins: List<String>,
    val risks: List<String>,
    val nextWeekFocus: String,
    val thinkingProcess: List<String> = emptyList(),
    val source: WeeklyReportSource = WeeklyReportSource.GEMINI_2_5_FLASH,
    val rawResponse: String? = null,
)

enum class WeeklyReportSource {
    GEMINI_2_5_FLASH,
    LOCAL_FALLBACK,
}

data class NutritionOverview(
    val foods: List<FoodItem>,
    val recipes: List<Recipe>,
    val meals: List<LoggedMeal>,
    val todaysNutrition: NutritionFacts,
    val todaysCalories: Double,
    val todaysProtein: Double,
    val todaysCarbs: Double,
    val todaysFat: Double,
    val todaysMeals: List<LoggedMeal>,
    val todaysMealsByType: Map<MealType, List<LoggedMeal>>,
    val todaysWorkoutCalories: Int,
    val energyBalance: EnergyBalanceSnapshot? = null,
    val scannedResult: MealAnalysisResult? = null,
)

data class BodyMeasurement(
    val id: Long,
    val date: Long,
    val weight: Double,
    val bodyFat: Double,
    val muscleMass: Double,
)

data class HomeDashboard(
    val profile: UserProfile?,
    val energyBalance: EnergyBalanceSnapshot?,
    val calorieTarget: Int,
    val calorieProgress: Int,
    val proteinProgress: Int,
    val proteinTarget: Int,
    val carbsProgress: Int,
    val carbsTarget: Int,
    val fatProgress: Int,
    val fatTarget: Int,
    val todaysWorkoutCalories: Int,
    val steps: Int?,
    val nextWorkout: WorkoutDay?,
    val streak: Int,
    val aiInsight: String,
)

data class ProgressOverview(
    val measurements: List<BodyMeasurement>,
    val weightTrend: List<ChartPoint>,
    val bodyFatTrend: List<ChartPoint>,
    val muscleMassTrend: List<ChartPoint>,
    val strengthTrend: List<ChartPoint>,
    val volumeTrend: List<ChartPoint>,
    val estimatedOneRepMax: Double,
    val fatigueIndex: Double,
)

data class CoachOverview(
    val weeklyReport: String,
    val trainingInsights: List<String>,
    val nutritionCoachMessage: String,
    val profile: UserProfile?,
)

data class GoalAdvice(
    val bmr: Int,
    val maintenanceCalories: Int,
    val activityMultiplier: Double,
    val calorieTarget: Int,
    val proteinTarget: Int,
    val carbsTarget: Int,
    val fatTarget: Int,
    val trainingFocus: String,
    val summary: String,
    val calorieAdvice: String = "",
    val macroAdvice: String = "",
    val activityExplanation: String = "",
    val attentionPoints: List<String> = emptyList(),
    val advice: String = "",
    val dataQuality: String = "",
    val source: GoalAdviceSource = GoalAdviceSource.GEMINI_2_5_FLASH,
    val rawResponse: String? = null,
)

enum class GoalAdviceSource {
    GEMINI_2_5_FLASH,
    LOCAL_CALCULATION,
}

data class WorkoutDebrief(
    val summary: String,
    val progressionFeedback: String,
    val recommendation: String,
    val nextSessionFocus: String,
    val recoveryScore: Int,
    val intensitySignal: String,
    val wins: List<String> = emptyList(),
    val risks: List<String> = emptyList(),
    val nextLoadTarget: String = "",
    val recoveryAdvice: String = "",
    val source: WorkoutDebriefSource = WorkoutDebriefSource.GEMINI_2_5_FLASH,
)

enum class WorkoutDebriefSource {
    GEMINI_2_5_FLASH,
    LOCAL_FALLBACK,
}

data class WorkoutCompletionResult(
    val sessionId: Long,
    val debrief: WorkoutDebrief,
)

sealed interface WorkoutCompletionUiState {
    data object Loading : WorkoutCompletionUiState
    data class Success(val summary: WorkoutCompletionSummary) : WorkoutCompletionUiState
    data class Error(val message: String) : WorkoutCompletionUiState
}

data class WorkoutCompletionSummary(
    val sessionId: Long,
    val workoutName: String,
    val startedAt: Long,
    val endedAt: Long,
    val durationSeconds: Long,
    val exercisesCompleted: Int,
    val setsLogged: Int,
    val totalVolume: Double,
    val personalBests: Int,
    val strongestSetLabel: String,
    val debrief: WorkoutDebrief,
    val sourceLabel: String,
    val recommendationLabel: String,
    val exercises: List<WorkoutCompletionExercise>,
)

data class WorkoutCompletionExercise(
    val exerciseId: Long,
    val name: String,
    val muscleGroup: String,
    val sets: List<WorkoutCompletionSet>,
    val totalVolume: Double,
    val bestSetLabel: String,
)

data class WorkoutCompletionSet(
    val setNumber: Int,
    val weightKg: Double,
    val reps: Int,
    val rpe: Double,
    val restSeconds: Int,
    val setType: SetType,
    val completed: Boolean,
)

data class WorkoutOverview(
    val activeRoutine: WorkoutRoutine?,
    val routines: List<WorkoutRoutine>,
    val exercises: List<Exercise>,
    val history: List<WorkoutSessionSummary>,
)

enum class HealthConnectState {
    UNSUPPORTED,
    PROVIDER_MISSING,
    PERMISSION_REQUIRED,
    CONNECTED,
    NO_DATA,
    ERROR,
}

data class HealthConnectMetrics(
    val stepsToday: Int,
    val averageHeartRateBpm: Int? = null,
    val latestHeartRateBpm: Int? = null,
    val sleepMinutes: Long = 0L,
    val sleepSessionCount: Int = 0,
    val caloriesBurnedToday: Double? = null,
    val latestWeightKg: Double? = null,
)

data class HealthConnectStatus(
    val state: HealthConnectState,
    val metrics: HealthConnectMetrics? = null,
    val message: String,
    val lastSyncedAt: Long? = null,
) {
    val stepsToday: Int?
        get() = metrics?.stepsToday

    val averageHeartRateBpm: Int?
        get() = metrics?.averageHeartRateBpm

    val latestHeartRateBpm: Int?
        get() = metrics?.latestHeartRateBpm

    val sleepMinutes: Long?
        get() = metrics?.sleepMinutes

    val sleepSessionCount: Int
        get() = metrics?.sleepSessionCount ?: 0

    val caloriesBurnedToday: Double?
        get() = metrics?.caloriesBurnedToday

    val latestWeightKg: Double?
        get() = metrics?.latestWeightKg
}

data class ChartPoint(
    val label: String,
    val value: Double,
)
