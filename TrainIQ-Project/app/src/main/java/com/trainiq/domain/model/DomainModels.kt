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
    val setType: SetType = SetType.WORKING,
    val supersetGroupId: Long? = null,
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

data class LoggedSet(
    val exerciseId: Long,
    val weight: Double,
    val reps: Int,
    val rpe: Double,
    val repsInReserve: Int? = null,
    val setType: SetType = SetType.WORKING,
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
    WARMUP,
    WORKING,
    TOP_SET,
    BACKOFF,
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
    val days: List<GeneratedDay>,
)

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

data class MealAnalysisResult(
    val items: List<MealScanItem>,
    val suggestedMealType: MealType? = null,
    val notes: String? = null,
    val rawResponse: String? = null,
)

data class WeeklyReportResult(
    val summary: String,
    val wins: List<String>,
    val risks: List<String>,
    val nextWeekFocus: String,
    val thinkingProcess: List<String> = emptyList(),
    val rawResponse: String? = null,
)

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
    val rawResponse: String? = null,
)

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
