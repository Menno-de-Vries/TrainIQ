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

data class Meal(
    val id: Long,
    val date: Long,
    val calories: Int,
    val protein: Int,
    val carbs: Int,
    val fat: Int,
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

data class NutritionOverview(
    val meals: List<Meal>,
    val todaysCalories: Int,
    val todaysProtein: Int,
    val todaysCarbs: Int,
    val todaysFat: Int,
    val recipes: List<String>,
    val scannedItems: List<MealScanItem> = emptyList(),
)

data class MealScanItem(
    val name: String,
    val calories: Int,
    val protein: Int,
    val carbs: Int,
    val fat: Int,
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

enum class HealthConnectAvailability {
    UNAVAILABLE,
    NEEDS_INSTALL,
    NEEDS_PERMISSION,
    CONNECTED,
    ERROR,
}

data class HealthConnectStatus(
    val availability: HealthConnectAvailability,
    val stepsToday: Int? = null,
    val message: String,
)

data class ChartPoint(
    val label: String,
    val value: Double,
)
