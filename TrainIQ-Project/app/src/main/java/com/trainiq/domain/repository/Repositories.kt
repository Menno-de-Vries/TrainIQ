package com.trainiq.domain.repository

import com.trainiq.domain.model.CoachOverview
import com.trainiq.domain.model.ActiveWorkoutSession
import com.trainiq.domain.model.ActiveWorkoutSetDraft
import com.trainiq.domain.model.BiologicalSex
import com.trainiq.domain.model.Exercise
import com.trainiq.domain.model.ExerciseHistory
import com.trainiq.domain.model.FoodItem
import com.trainiq.domain.model.FoodSourceType
import com.trainiq.domain.model.GeneratedRoutine
import com.trainiq.domain.model.GoalAdvice
import com.trainiq.domain.model.HealthConnectStatus
import com.trainiq.domain.model.HomeDashboard
import com.trainiq.domain.model.LoggedSet
import com.trainiq.domain.model.MealAnalysisResult
import com.trainiq.domain.model.MealType
import com.trainiq.domain.model.NutritionFacts
import com.trainiq.domain.model.NutritionOverview
import com.trainiq.domain.model.ProgressOverview
import com.trainiq.domain.model.ProgressionSuggestion
import com.trainiq.domain.model.RoutineSet
import com.trainiq.domain.model.SetType
import com.trainiq.domain.model.Recipe
import com.trainiq.domain.model.UserProfile
import com.trainiq.domain.model.WeeklyReportResult
import com.trainiq.domain.model.WorkoutDay
import com.trainiq.domain.model.WorkoutDebrief
import com.trainiq.domain.model.WorkoutOverview
import kotlinx.coroutines.flow.Flow

interface HomeRepository {
    fun observeDashboard(): Flow<HomeDashboard>
    suspend fun getHealthConnectStatus(): HealthConnectStatus
    /** Fetches today's steps from the Health Connect aggregate API and updates the dashboard flow. Falls back to the DataStore cache when HC is unavailable. */
    suspend fun refreshDashboardData()
}

interface WorkoutRepository {
    fun observeWorkoutOverview(): Flow<WorkoutOverview>
    fun observeExerciseHistory(exerciseId: Long): Flow<ExerciseHistory>
    suspend fun getWorkoutDay(dayId: Long): WorkoutDay?
    suspend fun getProgressionSuggestions(dayId: Long): List<ProgressionSuggestion>
    suspend fun getNextWorkoutDay(): WorkoutDay?
    suspend fun getOrStartActiveWorkoutSession(dayId: Long, initialDrafts: Map<Long, ActiveWorkoutSetDraft>): ActiveWorkoutSession
    suspend fun updateActiveWorkoutDraft(exerciseId: Long, draft: ActiveWorkoutSetDraft): ActiveWorkoutSession?
    suspend fun logActiveWorkoutSet(dayId: Long, set: LoggedSet, draft: ActiveWorkoutSetDraft, restSeconds: Int): ActiveWorkoutSession
    suspend fun updateActiveWorkoutSetType(exerciseId: Long, setIndex: Int, setType: SetType): ActiveWorkoutSession?
    suspend fun deleteActiveWorkoutSet(exerciseId: Long, setIndex: Int): ActiveWorkoutSession?
    suspend fun setActiveWorkoutCollapsed(exerciseId: Long, collapsed: Boolean): ActiveWorkoutSession?
    suspend fun updateActiveWorkoutRestTimer(endsAt: Long?, totalSeconds: Int): ActiveWorkoutSession?
    suspend fun finishActiveWorkout(dayId: Long): WorkoutDebrief
    suspend fun discardActiveWorkout(dayId: Long)
    suspend fun setActiveRoutine(routineId: Long)
    suspend fun finishWorkout(dayId: Long, durationSeconds: Long, loggedSets: List<LoggedSet>): WorkoutDebrief
    suspend fun createRoutine(name: String, description: String)
    suspend fun updateRoutine(routineId: Long, name: String, description: String)
    suspend fun deleteRoutine(routineId: Long)
    suspend fun searchExercises(query: String): List<Exercise>
    suspend fun reorderExercises(dayId: Long, orderedIds: List<Long>)
    suspend fun setSupersetGroup(workoutExerciseIds: List<Long>, groupId: Long?)
    suspend fun replaceExerciseInPlan(workoutExerciseId: Long, newExerciseId: Long)
    suspend fun updateWorkoutExercisePlan(
        workoutExerciseId: Long,
        targetSets: Int,
        repRange: String,
        restSeconds: Int,
        targetWeightKg: Double,
        targetRpe: Double,
        setType: SetType,
    )
    suspend fun addSetToExercise(workoutExerciseId: Long)
    suspend fun updateRoutineSet(set: RoutineSet)
    suspend fun updateRoutineSetType(setId: Long, setType: SetType)
    suspend fun updateRoutineSetReps(setId: Long, targetReps: Int)
    suspend fun updateRoutineSetWeight(setId: Long, targetWeightKg: Double)
    suspend fun updateRoutineSetRestTime(setId: Long, restSeconds: Int)
    suspend fun deleteRoutineSet(setId: Long)
    suspend fun moveRoutineSet(workoutExerciseId: Long, orderedSetIds: List<Long>)
    suspend fun addWorkoutDay(routineId: Long, name: String)
    suspend fun removeWorkoutDay(dayId: Long)
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
    )
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
    )
    suspend fun removeExerciseFromDay(workoutExerciseId: Long)
    suspend fun deleteWorkoutSession(sessionId: Long)
    suspend fun generateAiRoutine(
        daysPerWeek: Int,
        equipment: String,
        targetFocus: String,
        experienceLevel: String,
        sessionDurationMinutes: Int,
        includeDeload: Boolean,
    ): GeneratedRoutine
    suspend fun saveGeneratedRoutine(routine: GeneratedRoutine)
}

interface NutritionRepository {
    fun observeNutritionOverview(): Flow<NutritionOverview>
    suspend fun analyzeMealPhoto(path: String, context: String, capturedAtMillis: Long): MealAnalysisResult
    fun clearLastScanResult()
    suspend fun saveFoodItem(
        id: Long?,
        name: String,
        barcode: String?,
        caloriesPer100g: Double,
        proteinPer100g: Double,
        carbsPer100g: Double,
        fatPer100g: Double,
        sourceType: FoodSourceType,
    ): FoodItem
    suspend fun saveRecipe(
        id: Long?,
        name: String,
        notes: String?,
        totalCookedGrams: Double?,
        ingredients: List<Pair<Long, Double>>,
    ): Recipe
    suspend fun saveMeal(
        id: Long?,
        mealType: MealType,
        name: String,
        notes: String?,
        items: List<MealEntryRequest>,
    ): Long
    suspend fun deleteMeal(mealId: Long)
    suspend fun deleteFood(foodId: Long)
    suspend fun deleteRecipe(recipeId: Long)
}

data class MealEntryRequest(
    val itemType: MealEntryType,
    val referenceId: Long,
    val gramsUsed: Double,
    val notes: String? = null,
)

enum class MealEntryType {
    FOOD,
    RECIPE,
}

interface ProgressRepository {
    fun observeProgressOverview(): Flow<ProgressOverview>
    suspend fun addMeasurement(weight: Double, bodyFat: Double, muscleMass: Double)
    suspend fun deleteMeasurement(measurementId: Long)
}

interface CoachRepository {
    fun observeCoachOverview(): Flow<CoachOverview>
    suspend fun generateGoalAdvice(
        height: Double,
        weight: Double,
        bodyFat: Double,
        age: Int,
        sex: BiologicalSex,
        activityLevel: String,
        goal: String,
    ): GoalAdvice
    suspend fun generateWeeklyReport(): WeeklyReportResult
    fun observeUserProfile(): Flow<UserProfile?>
    suspend fun saveProfile(profile: UserProfile)
}
