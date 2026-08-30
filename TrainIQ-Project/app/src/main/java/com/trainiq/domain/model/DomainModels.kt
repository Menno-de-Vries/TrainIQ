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

data class ExerciseLibraryItem(
    val exercise: Exercise,
    val completedSessions: Int = 0,
    val score: Double = 0.0,
    val rankLabel: String = "",
    val lastPerformedAt: Long? = null,
    val bestEstimatedOneRepMax: Double = 0.0,
    val totalVolume: Double = 0.0,
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
    val workoutName: String = "Sessie $id",
    val exerciseCount: Int = 0,
    val setsLogged: Int = 0,
    val strongestSetLabel: String = "",
    val debriefSummary: String = "",
    val debriefRecommendation: String = "",
    val debriefNextSessionFocus: String = "",
    val debriefRecoveryScore: Int = 0,
    val debriefIntensitySignal: String = "",
    val debriefSource: WorkoutDebriefSource = WorkoutDebriefSource.LOCAL_FALLBACK,
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
    NOVICE("Nieuw", 75.0),
    INTERMEDIATE("Gemiddeld", 150.0),
    ADVANCED("Gevorderd", 300.0),
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
    val loggedAt: Long = 0L,
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
        loggedAt = loggedAt,
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
    val fallbackContext: AiFallbackContext? = null,
    val days: List<GeneratedDay>,
)

enum class GeneratedRoutineSource {
    GEMINI_2_5_FLASH,
    OPENAI,
    LOCAL_FALLBACK,
}

enum class AiFallbackContext {
    AI_DISABLED,
    NO_DECRYPTABLE_KEY,
    RATE_LIMIT,
    TIMEOUT,
    NETWORK,
    SERVICE_FAILURE,
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
    val existingExerciseId: Long? = null,
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
    val defaultServingGrams: Double = 100.0,
    val sourceType: FoodSourceType,
    val createdAt: Long,
    val updatedAt: Long,
)

data class BarcodeProductLookupResult(
    val barcode: String,
    val name: String,
    val caloriesPer100g: Double,
    val proteinPer100g: Double,
    val carbsPer100g: Double,
    val fatPer100g: Double,
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
    SNAPSHOT,
}

data class LoggedMealItem(
    val id: Long,
    val mealId: Long,
    val itemType: LoggedMealItemType,
    val referenceId: Long,
    val name: String,
    val gramsUsed: Double,
    val servingCount: Int = 1,
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
    GEMINI_2_5_FLASH,
    OPENAI,
    LOCAL_FALLBACK,
}

data class MealAnalysisResult(
    val items: List<MealScanItem>,
    val suggestedMealType: MealType? = null,
    val notes: String? = null,
    val rawResponse: String? = null,
    val source: MealAnalysisSource = MealAnalysisSource.API,
    val fallbackContext: AiFallbackContext? = null,
)

data class WeeklyReportResult(
    val summary: String,
    val wins: List<String>,
    val risks: List<String>,
    val nextWeekFocus: String,
    val rationaleBullets: List<String> = emptyList(),
    val source: WeeklyReportSource = WeeklyReportSource.GEMINI_2_5_FLASH,
    val rawResponse: String? = null,
    val fallbackContext: AiFallbackContext? = null,
)

enum class WeeklyReportSource {
    GEMINI_2_5_FLASH,
    OPENAI,
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

data class BodyMeasurementPhotoResult(
    val weight: Double,
    val bodyFat: Double,
    val muscleMass: Double,
    val confidence: String? = null,
    val notes: String? = null,
    val rawResponse: String? = null,
    val source: BodyMeasurementPhotoSource = BodyMeasurementPhotoSource.GEMINI_2_5_FLASH,
    val fallbackContext: AiFallbackContext? = null,
)

enum class BodyMeasurementPhotoSource {
    GEMINI_2_5_FLASH,
    OPENAI,
    LOCAL_FALLBACK,
}

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
    val coachInsight: String,
)

data class ProgressOverview(
    val measurements: List<BodyMeasurement>,
    val weightTrend: List<ChartPoint>,
    val bodyFatTrend: List<ChartPoint>,
    val muscleMassTrend: List<ChartPoint>,
    val strengthTrend: List<ChartPoint>,
    val volumeTrend: List<ChartPoint>,
    val estimatedOneRepMax: Double,
    val weeklyLoadRatio: Double?,
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
    val fallbackContext: AiFallbackContext? = null,
)

data class SavedGoalAdvice(
    val advice: GoalAdvice,
    val profileFingerprint: String,
    val savedAt: Long,
)

fun UserProfile.goalAdviceProfileFingerprint(): String = listOf(
    name.trim(),
    age.toString(),
    sex.name,
    height.toString(),
    weight.toString(),
    bodyFat.toString(),
    activityLevel.trim(),
    goal.trim(),
    calorieTarget.toString(),
    proteinTarget.toString(),
    carbsTarget.toString(),
    fatTarget.toString(),
    trainingFocus.trim(),
).joinToString(separator = "|")

enum class GoalAdviceSource {
    GEMINI_2_5_FLASH,
    OPENAI,
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
    val fallbackContext: AiFallbackContext? = null,
)

enum class WorkoutDebriefSource {
    GEMINI_2_5_FLASH,
    OPENAI,
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
    val exerciseLibrary: List<ExerciseLibraryItem> = exercises.map { ExerciseLibraryItem(it) },
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

enum class HealthMetricType {
    STEPS,
    HEART_RATE,
    SLEEP,
    ACTIVE_CALORIES,
    WEIGHT,
    WORKOUTS,
}

enum class HealthMetricSyncState {
    UNAVAILABLE,
    DENIED,
    PARTIALLY_GRANTED,
    STALE,
    FAILED,
    SYNCING,
    SYNCED,
}

enum class HealthConnectStepDataFreshness {
    UNKNOWN,
    FRESH,
    STALE_CACHE,
    PERMISSION_MISSING,
    UNAVAILABLE,
    ERROR,
}

enum class HealthConnectStepDiagnosticFreshness {
    FRESH,
    STALE,
}

fun resolveSamsungComparableDisplaySteps(
    healthConnectAggregateSteps: Int,
    samsungHealthVisibleSteps: Int?,
    samsungHealthDirectSteps: Int? = null,
): Int = samsungHealthDirectSteps
    ?.takeIf { it >= 0 }
    ?: samsungHealthVisibleSteps?.takeIf { it > 0 }
    ?: healthConnectAggregateSteps

data class HealthConnectStepDiagnostic(
    val aggregateStepsToday: Int,
    val samsungHealthStepsToday: Int? = null,
    val samsungHealthAggregateStepsToday: Int? = null,
    val samsungRawStepRecordSumToday: Int? = null,
    val samsungHealthDirectStepsToday: Int? = null,
    val samsungHealthDirectStatus: String = "Samsung Health Data SDK API AAR samsung-health-data-api*.aar ontbreekt in app/libs; directe Samsung All steps-bron niet beschikbaar.",
    val displaySteps: Int = resolveSamsungComparableDisplaySteps(
        aggregateStepsToday,
        samsungHealthStepsToday,
        samsungHealthDirectStepsToday,
    ),
    val queriedAt: Long,
    val sourceLabels: List<String> = emptyList(),
    val latestSamsungSourceSeenAt: Long? = null,
    val dayStartLabel: String = "",
    val dayEndLabel: String = "",
    val workoutWindowSteps: Int? = null,
    val workoutWindowSessionCount: Int = 0,
    val workoutWindowTruncated: Boolean = false,
    val samsungHealthDirectFromCache: Boolean = false,
    val displayStepsFromCache: Boolean = false,
) {
    val sourceSummary: String
        get() = sourceLabels.distinct().joinToString(", ").ifBlank { "Geen bronlabels zichtbaar" }

    val hasSamsungHealthSource: Boolean
        get() = sourceLabels.any { it.contains("Samsung", ignoreCase = true) }

    val hasMultipleHealthConnectStepSources: Boolean
        get() = sourceLabels.distinct().size > 1

    val hasFreshDirectStepValue: Boolean
        get() = samsungHealthDirectStepsToday != null &&
            displaySteps == samsungHealthDirectStepsToday &&
            !samsungHealthDirectFromCache

    val usesSamsungRawStepFallback: Boolean
        get() {
            val rawSteps = samsungRawStepRecordSumToday ?: return false
            return samsungHealthDirectStepsToday == null &&
                !displayStepsFromCache &&
                rawSteps > 0 &&
                rawSteps > (samsungHealthAggregateStepsToday ?: 0) &&
                samsungHealthStepsToday == rawSteps &&
                displaySteps == rawSteps
        }

    val queryWindowSummary: String
        get() = if (dayStartLabel.isNotBlank() && dayEndLabel.isNotBlank()) {
            "$dayStartLabel-$dayEndLabel"
        } else {
            "vandaag"
        }

    val aggregateAuthorityLabel: String
        get() = when {
            samsungHealthDirectFromCache && samsungHealthDirectStepsToday != null &&
                displaySteps == samsungHealthDirectStepsToday ->
                "TrainIQ toont tijdelijk de laatst bekende directe Samsung Health Data SDK-waarde uit cache; de huidige directe read is mislukt en deze waarde is niet vers."
            samsungHealthDirectStepsToday != null && displaySteps == samsungHealthDirectStepsToday ->
                "TrainIQ toont de directe Samsung Health Data SDK-waarde om Samsung Health All steps te matchen; Health Connect blijft zichtbaar voor diagnose."
            displayStepsFromCache ->
                "TrainIQ toont tijdelijk het laatst bekende stappentotaal uit cache; de huidige stappenread is mislukt en deze waarde is niet vers."
            usesSamsungRawStepFallback ->
                "TrainIQ toont de hogere officiële Samsung Health raw-export om Samsung Health All steps te matchen; de Health Connect-aggregates blijven zichtbaar voor diagnose."
            samsungHealthStepsToday != null && displaySteps == samsungHealthStepsToday ->
                "TrainIQ toont de Samsung Health-export als leidende Samsung-bron; de algemene Health Connect aggregate blijft zichtbaar voor diagnose."
            else ->
                "TrainIQ toont de Health Connect aggregate als dagtotaal omdat geen bruikbare Samsung Health-export beschikbaar is."
        }

    val samsungHealthComparisonSummary: String
        get() = when {
            samsungHealthDirectFromCache && samsungHealthDirectStepsToday != null &&
                displaySteps == samsungHealthDirectStepsToday ->
                "Laatst bekende directe Samsung Health-waarde $samsungHealthDirectStepsToday wordt tijdelijk uit cache getoond; de huidige directe read is mislukt."
            samsungHealthDirectStepsToday != null && displaySteps == samsungHealthDirectStepsToday ->
                "Directe Samsung Health Data SDK-waarde $samsungHealthDirectStepsToday wordt getoond; Health Connect aggregate $aggregateStepsToday blijft diagnose."
            displayStepsFromCache ->
                "Laatst bekende stappenwaarde $displaySteps wordt tijdelijk uit cache getoond; de huidige stappenread is mislukt."
            usesSamsungRawStepFallback ->
                "Officiële Samsung Health raw-waarde $samsungRawStepRecordSumToday wordt getoond; Samsung aggregate ${samsungHealthAggregateStepsToday ?: 0} en Health Connect aggregate $aggregateStepsToday blijven diagnose."
            samsungHealthStepsToday == null ->
                "Samsung Health publiceerde vandaag nog geen apart stappenaggregate naar Health Connect."
            displaySteps == samsungHealthStepsToday ->
                "Samsung Health-export $samsungHealthStepsToday wordt getoond; Health Connect aggregate $aggregateStepsToday blijft diagnose."
            else ->
                "Health Connect aggregate $aggregateStepsToday wordt getoond omdat die hoger is dan de Samsung Health-export $samsungHealthStepsToday die nu zichtbaar is."
        }

    val stepValueDebugSummary: String
        get() = buildList {
            add(if (displayStepsFromCache) "getoond $displaySteps (cache, niet vers)" else "getoond $displaySteps")
            add(
                if (displayStepsFromCache && displaySteps == aggregateStepsToday) {
                    "Health Connect aggregate $aggregateStepsToday (cache, niet vers)"
                } else {
                    "Health Connect aggregate $aggregateStepsToday"
                },
            )
            add("Samsung export ${samsungHealthStepsToday ?: "niet zichtbaar"}")
            add("Samsung aggregate ${samsungHealthAggregateStepsToday ?: "niet zichtbaar"}")
            add("Samsung raw ${samsungRawStepRecordSumToday ?: "niet zichtbaar"}")
            add(
                when {
                    samsungHealthDirectStepsToday == null -> "Samsung direct niet beschikbaar"
                    samsungHealthDirectFromCache -> "Samsung direct $samsungHealthDirectStepsToday (cache, niet vers)"
                    else -> "Samsung direct $samsungHealthDirectStepsToday"
                },
            )
        }.joinToString(separator = " · ")

    val parityGapSummary: String
        get() = when {
            samsungHealthDirectFromCache && samsungHealthDirectStepsToday != null &&
                displaySteps == samsungHealthDirectStepsToday ->
                "TrainIQ toont tijdelijk een laatst bekende directe Samsung-waarde; gebruik deze niet als verse pariteitsmeting en vernieuw na Samsung Health Sync now."
            samsungHealthDirectStepsToday != null && displaySteps == samsungHealthDirectStepsToday ->
                "Directe Samsung Health Data SDK-waarde is beschikbaar; vergelijk deze met Samsung Health All steps na Sync now."
            displayStepsFromCache ->
                "TrainIQ toont tijdelijk een laatst bekende stappenwaarde; gebruik deze niet als verse pariteitsmeting en vernieuw na Samsung Health Sync now."
            usesSamsungRawStepFallback ->
                "TrainIQ toont de officiële Samsung Health raw-export omdat die hoger is dan de Health Connect-aggregates; dit is de beste beschikbare Samsung All steps-vergelijking zolang directe Data SDK-verificatie niet werkt."
            samsungHealthDirectStatus.contains("samsung-health-data-api", ignoreCase = true) ->
                "Directe Samsung Health All steps-bron ontbreekt nog omdat de Samsung Health Data SDK API AAR niet beschikbaar is; Health Connect kan daardoor lager blijven dan Samsung Health.${healthConnectPriorityHintSuffix()}"
            samsungHealthDirectStatus.contains("toestemming ontbreekt", ignoreCase = true) ->
                "Directe Samsung Health All steps-bron is gebundeld, maar Samsung stappen-toestemming ontbreekt nog."
            samsungHealthDirectStatus.contains("lager dan API 29", ignoreCase = true) ->
                "Directe Samsung Health All steps-bron is geblokkeerd omdat Samsung Health Data SDK Android 10/API 29+ vereist."
            samsungHealthDirectStatus.contains("lager dan Samsung Health Data SDK minimum", ignoreCase = true) ->
                "Directe Samsung Health All steps-bron is geblokkeerd omdat Samsung Health te oud is voor de Data SDK."
            samsungHealthDirectStatus.contains("app niet gevonden", ignoreCase = true) ->
                "Directe Samsung Health All steps-bron is geblokkeerd omdat Samsung Health niet gevonden is."
            samsungHealthStepsToday != null && displaySteps == samsungHealthStepsToday ->
                "TrainIQ toont de hoogste Samsung Health-export die Health Connect zichtbaar maakt; als Samsung Health zelf hoger staat, is directe Samsung Data SDK-verificatie nodig.${healthConnectPriorityHintSuffix()}"
            hasSamsungHealthSource ->
                "Samsung Health is zichtbaar in Health Connect, maar directe Samsung Data SDK-verificatie blijft nodig bij aanhoudende mismatch.${healthConnectPriorityHintSuffix()}"
            else ->
                "Samsung Health is nog niet als Health Connect-bron zichtbaar; directe Samsung Data SDK-verificatie of Samsung Health Sync now is nodig bij aanhoudende mismatch."
        }

    val healthConnectStepPrioritySummary: String
        get() = if (hasMultipleHealthConnectStepSources) {
            "Health Connect heeft meerdere stappenbronnen zichtbaar. Android dedupliceert Activity/Stappen-aggregates op basis van App priorities; zet Samsung Health daar bovenaan als Samsung Health leidend moet zijn."
        } else {
            "Health Connect toont nu een enkele stappenbron; App priorities verklaren deze meting minder snel dan Samsung Health-sync of directe Samsung Data SDK-toegang."
        }

    fun samsungSourceRecencySummary(nowMillis: Long = System.currentTimeMillis()): String =
        latestSamsungSourceSeenAt?.let { seenAt ->
            val ageMinutes = ((nowMillis - seenAt).coerceAtLeast(0L) / 60_000L).toInt()
            when {
                ageMinutes < 1 -> "Samsung-bron net gezien in Health Connect."
                ageMinutes < 60 -> "Samsung-bron $ageMinutes min geleden gezien in Health Connect."
                else -> "Samsung-bron ${ageMinutes / 60} uur geleden gezien in Health Connect."
            }
        } ?: "Samsung-bron vandaag nog niet met timestamp gezien in Health Connect."

    fun samsungStepDebugClipboardText(nowMillis: Long = System.currentTimeMillis()): String =
        listOf(
            "TrainIQ Samsung stappen-diagnose",
            stepValueDebugSummary,
            "Bronnen: $sourceSummary",
            "Venster: $queryWindowSummary",
            "Samsung timing: ${samsungSourceRecencySummary(nowMillis)}",
            "Samsung vergelijking: $samsungHealthComparisonSummary",
            "Samsung direct: $samsungHealthDirectStatus",
            "Pariteit: $parityGapSummary",
            "Health Connect zichtbaar: $healthConnectVisibleStepSummary",
            "Health Connect prioriteit: $healthConnectStepPrioritySummary",
            "Syncadvies: ${samsungHealthSyncGuidance(nowMillis)}",
        ).joinToString(separator = "\n")

    val healthConnectVisibleStepSummary: String
        get() = if (
            samsungHealthDirectFromCache && samsungHealthDirectStepsToday != null &&
            displaySteps == samsungHealthDirectStepsToday
        ) {
            "TrainIQ toont tijdelijk $displaySteps stappen uit de laatst bekende directe Samsung-cache. De huidige Samsung-read is mislukt; Health Connect aggregate blijft de diagnosewaarde."
        } else if (samsungHealthDirectStepsToday != null && displaySteps == samsungHealthDirectStepsToday) {
            "TrainIQ toont nu $displaySteps stappen uit de directe Samsung Health Data SDK-bron. Health Connect aggregate blijft zichtbaar als diagnosewaarde."
        } else if (displayStepsFromCache) {
            "TrainIQ toont tijdelijk $displaySteps stappen uit cache. De huidige stappenread is mislukt; vernieuw na Samsung Health Sync now."
        } else if (usesSamsungRawStepFallback) {
            "TrainIQ toont nu $displaySteps stappen uit raw StepsRecords van de officiële Samsung Health-bron. Samsung aggregate ${samsungHealthAggregateStepsToday ?: 0} en Health Connect aggregate $aggregateStepsToday blijven diagnosewaarden."
        } else if (samsungHealthStepsToday != null && displaySteps == samsungHealthStepsToday) {
            "Health Connect geeft TrainIQ nu $displaySteps Samsung Health-stappen. Als Samsung Health zelf meer toont, zijn die extra stappen nog niet naar Health Connect geschreven."
        } else {
            "Health Connect geeft TrainIQ nu $aggregateStepsToday stappen. Als Samsung Health zelf meer toont, controleer of Samsung Health bij Health Connect > Gegevens en toegang > Activiteit > Stappen als bron met recente vermeldingen staat."
        }

    val workoutWindowSummary: String
        get() = when {
            workoutWindowSteps == null ->
                "Workout-stappen zijn niet apart berekend; geef ook workout-toegang om overlap met geregistreerde trainingen te zien."
            workoutWindowSessionCount == 0 ->
                "Geen Health Connect-workouts vandaag om stappen apart mee te vergelijken."
            workoutWindowTruncated ->
                "$workoutWindowSteps stappen vielen binnen de eerste $workoutWindowSessionCount Health Connect-workoutvensters; dit is diagnose en wordt niet afgetrokken."
            else ->
                "$workoutWindowSteps stappen vielen binnen $workoutWindowSessionCount Health Connect-workoutvenster(s); dit is diagnose en wordt niet afgetrokken."
        }

    fun freshness(nowMillis: Long = System.currentTimeMillis()): HealthConnectStepDiagnosticFreshness =
        if (!displayStepsFromCache && nowMillis - queriedAt < StepDiagnosticFreshMillis) {
            HealthConnectStepDiagnosticFreshness.FRESH
        } else {
            HealthConnectStepDiagnosticFreshness.STALE
        }

    fun samsungHealthSyncGuidance(nowMillis: Long = System.currentTimeMillis()): String = when {
        !hasSamsungHealthSource ->
            "Geen Samsung Health-bron gezien in Health Connect. Controleer Samsung Health > Instellingen > Health Connect > App permissions > Samsung Health, open Samsung Health daarna opnieuw en gebruik Sync now."
        freshness(nowMillis) == HealthConnectStepDiagnosticFreshness.STALE ->
            "Samsung Health is zichtbaar, maar deze stapcheck is niet vers. Gebruik Sync now en vernieuw TrainIQ als Samsung Health All steps hoger blijft."
        else ->
            "Samsung Health-data is zichtbaar in Health Connect."
    }

    private fun healthConnectPriorityHintSuffix(): String =
        if (hasMultipleHealthConnectStepSources) {
            " Controleer ook Health Connect App priorities voor Activity/Stappen; Android kan meerdere bronnen dedupliceren op basis van die volgorde."
        } else {
            ""
        }

    private companion object {
        const val StepDiagnosticFreshMillis = 15 * 60 * 1000L
    }
}

data class HealthMetricStatus(
    val metric: HealthMetricType,
    val state: HealthMetricSyncState,
    val message: String? = null,
    val lastSyncedAt: Long? = null,
)

data class HealthConnectMetrics(
    val stepsToday: Int,
    val averageHeartRateBpm: Int? = null,
    val latestHeartRateBpm: Int? = null,
    val sleepMinutes: Long = 0L,
    val sleepSessionCount: Int = 0,
    val caloriesBurnedToday: Double? = null,
    val latestWeightKg: Double? = null,
    val workoutSessionCountToday: Int = 0,
    val workoutMinutesToday: Long = 0L,
)

data class HealthConnectStatus(
    val state: HealthConnectState,
    val metrics: HealthConnectMetrics? = null,
    val message: String,
    val lastSyncedAt: Long? = null,
    val metricStatuses: List<HealthMetricStatus> = emptyList(),
    val stepDataFreshness: HealthConnectStepDataFreshness = HealthConnectStepDataFreshness.UNKNOWN,
    val stepDataUpdatedAt: Long? = lastSyncedAt,
    val stepDiagnostic: HealthConnectStepDiagnostic? = null,
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

    val workoutSessionCountToday: Int
        get() = metrics?.workoutSessionCountToday ?: 0

    val workoutMinutesToday: Long
        get() = metrics?.workoutMinutesToday ?: 0L
}

data class ChartPoint(
    val label: String,
    val value: Double,
)
