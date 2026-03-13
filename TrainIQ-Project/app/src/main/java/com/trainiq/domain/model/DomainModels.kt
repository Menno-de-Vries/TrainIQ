package com.trainiq.domain.model

data class UserProfile(
    val id: Long,
    val name: String,
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
    val totalVolume: Double,
)

data class LoggedSet(
    val exerciseId: Long,
    val weight: Double,
    val reps: Int,
    val rpe: Double,
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
    val notes: String? = null,
    val rawResponse: String? = null,
)

data class NutritionOverview(
    val foods: List<FoodItem>,
    val recipes: List<Recipe>,
    val meals: List<LoggedMeal>,
    val todaysCalories: Double,
    val todaysProtein: Double,
    val todaysCarbs: Double,
    val todaysFat: Double,
    val todaysMeals: List<LoggedMeal>,
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
    val calorieProgress: Int,
    val calorieTarget: Int,
    val proteinProgress: Int,
    val proteinTarget: Int,
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
    val calorieTarget: Int,
    val proteinTarget: Int,
    val carbsTarget: Int,
    val fatTarget: Int,
    val trainingFocus: String,
    val summary: String,
)

data class WorkoutDebrief(
    val summary: String,
    val progressionFeedback: String,
    val recommendation: String,
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

data class HealthConnectStatus(
    val state: HealthConnectState,
    val stepsToday: Int? = null,
    val message: String,
    val lastSyncedAt: Long? = null,
)

data class ChartPoint(
    val label: String,
    val value: Double,
)
