package com.trainiq.data.repository

import com.trainiq.domain.model.ActiveWorkoutSession
import com.trainiq.domain.model.ActiveWorkoutSetDraft
import com.trainiq.domain.model.BiologicalSex
import com.trainiq.domain.model.BodyMeasurementPhotoResult
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
import com.trainiq.domain.model.NutritionOverview
import com.trainiq.domain.model.ProgressOverview
import com.trainiq.domain.model.ProgressionSuggestion
import com.trainiq.domain.model.Recipe
import com.trainiq.domain.model.RoutineSet
import com.trainiq.domain.model.SetType
import com.trainiq.domain.model.UserProfile
import com.trainiq.domain.model.WeeklyReportResult
import com.trainiq.domain.model.WorkoutCompletionResult
import com.trainiq.domain.model.WorkoutCompletionSummary
import com.trainiq.domain.model.WorkoutDay
import com.trainiq.domain.model.WorkoutDebrief
import com.trainiq.domain.model.WorkoutLoggingSummary
import com.trainiq.domain.model.WorkoutOverview
import com.trainiq.domain.repository.CoachRepository
import com.trainiq.domain.repository.HomeRepository
import com.trainiq.domain.repository.MealEntryRequest
import com.trainiq.domain.repository.NutritionRepository
import com.trainiq.domain.repository.ProgressRepository
import com.trainiq.domain.repository.WorkoutRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class RoomHomeRepository @Inject constructor(
    private val delegate: TrainIqDataCoordinator,
) : HomeRepository {
    override fun observeDashboard(): Flow<HomeDashboard> = delegate.observeDashboard()
    override suspend fun getHealthConnectStatus(): HealthConnectStatus = delegate.getHealthConnectStatus()
    override suspend fun refreshDashboardData() = delegate.refreshDashboardData()
}

@Singleton
class RoomWorkoutRepository @Inject constructor(
    private val delegate: TrainIqDataCoordinator,
) : WorkoutRepository {
    override fun observeWorkoutOverview(): Flow<WorkoutOverview> = delegate.observeWorkoutOverview()
    override fun observeWorkoutLoggingSummary(dayId: Long): Flow<WorkoutLoggingSummary> = delegate.observeWorkoutLoggingSummary(dayId)
    override fun observeExerciseHistory(exerciseId: Long): Flow<ExerciseHistory> = delegate.observeExerciseHistory(exerciseId)
    override suspend fun getWorkoutDay(dayId: Long): WorkoutDay? = delegate.getWorkoutDay(dayId)
    override suspend fun getProgressionSuggestions(dayId: Long): List<ProgressionSuggestion> = delegate.getProgressionSuggestions(dayId)
    override suspend fun getNextWorkoutDay(): WorkoutDay? = delegate.getNextWorkoutDay()
    override suspend fun getOrStartActiveWorkoutSession(dayId: Long, initialDrafts: Map<Long, ActiveWorkoutSetDraft>): ActiveWorkoutSession =
        delegate.getOrStartActiveWorkoutSession(dayId, initialDrafts)
    override suspend fun updateActiveWorkoutDraft(exerciseId: Long, draft: ActiveWorkoutSetDraft): ActiveWorkoutSession? =
        delegate.updateActiveWorkoutDraft(exerciseId, draft)
    override suspend fun logActiveWorkoutSet(dayId: Long, set: LoggedSet, draft: ActiveWorkoutSetDraft, restSeconds: Int): ActiveWorkoutSession =
        delegate.logActiveWorkoutSet(dayId, set, draft, restSeconds)
    override suspend fun updateActiveWorkoutSet(setId: Long, set: LoggedSet, draft: ActiveWorkoutSetDraft, restSeconds: Int): ActiveWorkoutSession? =
        delegate.updateActiveWorkoutSet(setId, set, draft, restSeconds)
    override suspend fun updateActiveWorkoutSetType(setId: Long, setType: SetType): ActiveWorkoutSession? =
        delegate.updateActiveWorkoutSetType(setId, setType)
    override suspend fun deleteActiveWorkoutSet(setId: Long): ActiveWorkoutSession? = delegate.deleteActiveWorkoutSet(setId)
    override suspend fun undoWorkoutLogEvent(eventId: Long): ActiveWorkoutSession? = delegate.undoWorkoutLogEvent(eventId)
    override suspend fun setActiveWorkoutCollapsed(exerciseId: Long, collapsed: Boolean): ActiveWorkoutSession? =
        delegate.setActiveWorkoutCollapsed(exerciseId, collapsed)
    override suspend fun updateActiveWorkoutRestTimer(endsAt: Long?, totalSeconds: Int): ActiveWorkoutSession? =
        delegate.updateActiveWorkoutRestTimer(endsAt, totalSeconds)
    override suspend fun finishActiveWorkout(dayId: Long): WorkoutCompletionResult = delegate.finishActiveWorkout(dayId)
    override suspend fun getWorkoutCompletionSummary(sessionId: Long): WorkoutCompletionSummary? = delegate.getWorkoutCompletionSummary(sessionId)
    override suspend fun discardActiveWorkout(dayId: Long) = delegate.discardActiveWorkout(dayId)
    override suspend fun setActiveRoutine(routineId: Long) = delegate.setActiveRoutine(routineId)
    override suspend fun finishWorkout(dayId: Long, durationSeconds: Long, loggedSets: List<LoggedSet>): WorkoutDebrief =
        delegate.finishWorkout(dayId, durationSeconds, loggedSets)
    override suspend fun createRoutine(name: String, description: String) = delegate.createRoutine(name, description)
    override suspend fun updateRoutine(routineId: Long, name: String, description: String) = delegate.updateRoutine(routineId, name, description)
    override suspend fun deleteRoutine(routineId: Long) = delegate.deleteRoutine(routineId)
    override suspend fun searchExercises(query: String): List<Exercise> = delegate.searchExercises(query)
    override suspend fun reorderExercises(dayId: Long, orderedIds: List<Long>) = delegate.reorderExercises(dayId, orderedIds)
    override suspend fun setSupersetGroup(workoutExerciseIds: List<Long>, groupId: Long?) = delegate.setSupersetGroup(workoutExerciseIds, groupId)
    override suspend fun replaceExerciseInPlan(workoutExerciseId: Long, newExerciseId: Long) = delegate.replaceExerciseInPlan(workoutExerciseId, newExerciseId)
    override suspend fun replaceExerciseInActiveWorkout(workoutExerciseId: Long, newExerciseId: Long): ActiveWorkoutSession? =
        delegate.replaceExerciseInActiveWorkout(workoutExerciseId, newExerciseId)
    override suspend fun updateWorkoutExercisePlan(workoutExerciseId: Long, targetSets: Int, repRange: String, restSeconds: Int, targetWeightKg: Double, targetRpe: Double, setType: SetType) =
        delegate.updateWorkoutExercisePlan(workoutExerciseId, targetSets, repRange, restSeconds, targetWeightKg, targetRpe, setType)
    override suspend fun addSetToExercise(workoutExerciseId: Long) = delegate.addSetToExercise(workoutExerciseId)
    override suspend fun updateRoutineSet(set: RoutineSet) = delegate.updateRoutineSet(set)
    override suspend fun updateRoutineSetType(setId: Long, setType: SetType) = delegate.updateRoutineSetType(setId, setType)
    override suspend fun updateRoutineSetReps(setId: Long, targetReps: Int) = delegate.updateRoutineSetReps(setId, targetReps)
    override suspend fun updateRoutineSetWeight(setId: Long, targetWeightKg: Double) = delegate.updateRoutineSetWeight(setId, targetWeightKg)
    override suspend fun updateRoutineSetRestTime(setId: Long, restSeconds: Int) = delegate.updateRoutineSetRestTime(setId, restSeconds)
    override suspend fun deleteRoutineSet(setId: Long) = delegate.deleteRoutineSet(setId)
    override suspend fun moveRoutineSet(workoutExerciseId: Long, orderedSetIds: List<Long>) = delegate.moveRoutineSet(workoutExerciseId, orderedSetIds)
    override suspend fun addWorkoutDay(routineId: Long, name: String) = delegate.addWorkoutDay(routineId, name)
    override suspend fun removeWorkoutDay(dayId: Long) = delegate.removeWorkoutDay(dayId)
    override suspend fun addExerciseToDay(dayId: Long, name: String, muscleGroup: String, equipment: String, targetSets: Int, repRange: String, restSeconds: Int, targetWeightKg: Double, targetRpe: Double) =
        delegate.addExerciseToDay(dayId, name, muscleGroup, equipment, targetSets, repRange, restSeconds, targetWeightKg, targetRpe)
    override suspend fun addExerciseToRoutine(routineId: Long, name: String, muscleGroup: String, equipment: String, targetSets: Int, repRange: String, restSeconds: Int, targetWeightKg: Double, targetRpe: Double) =
        delegate.addExerciseToRoutine(routineId, name, muscleGroup, equipment, targetSets, repRange, restSeconds, targetWeightKg, targetRpe)
    override suspend fun removeExerciseFromDay(workoutExerciseId: Long) = delegate.removeExerciseFromDay(workoutExerciseId)
    override suspend fun deleteWorkoutSession(sessionId: Long) = delegate.deleteWorkoutSession(sessionId)
    override suspend fun generateAiRoutine(daysPerWeek: Int, equipment: String, targetFocus: String, experienceLevel: String, sessionDurationMinutes: Int, includeDeload: Boolean): GeneratedRoutine =
        delegate.generateAiRoutine(daysPerWeek, equipment, targetFocus, experienceLevel, sessionDurationMinutes, includeDeload)
    override suspend fun saveGeneratedRoutine(routine: GeneratedRoutine) = delegate.saveGeneratedRoutine(routine)
}

@Singleton
class RoomNutritionRepository @Inject constructor(
    private val delegate: TrainIqDataCoordinator,
) : NutritionRepository {
    override fun observeNutritionOverview(): Flow<NutritionOverview> = delegate.observeNutritionOverview()
    override suspend fun analyzeMealPhoto(path: String, context: String, capturedAtMillis: Long): MealAnalysisResult =
        delegate.analyzeMealPhoto(path, context, capturedAtMillis)
    override suspend fun lookupBarcodeProduct(barcode: String) = delegate.lookupBarcodeProduct(barcode)
    override fun clearLastScanResult() = delegate.clearLastScanResult()
    override suspend fun saveFoodItem(id: Long?, name: String, barcode: String?, caloriesPer100g: Double, proteinPer100g: Double, carbsPer100g: Double, fatPer100g: Double, defaultServingGrams: Double, sourceType: FoodSourceType): FoodItem =
        delegate.saveFoodItem(id, name, barcode, caloriesPer100g, proteinPer100g, carbsPer100g, fatPer100g, defaultServingGrams, sourceType)
    override suspend fun saveRecipe(id: Long?, name: String, notes: String?, totalCookedGrams: Double?, ingredients: List<Pair<Long, Double>>): Recipe =
        delegate.saveRecipe(id, name, notes, totalCookedGrams, ingredients)
    override suspend fun saveMeal(id: Long?, mealType: MealType, name: String, notes: String?, items: List<MealEntryRequest>): Long =
        delegate.saveMeal(id, mealType, name, notes, items)
    override suspend fun deleteMeal(mealId: Long) = delegate.deleteMeal(mealId)
    override suspend fun deleteFood(foodId: Long) = delegate.deleteFood(foodId)
    override suspend fun deleteRecipe(recipeId: Long) = delegate.deleteRecipe(recipeId)
}

@Singleton
class RoomProgressRepository @Inject constructor(
    private val delegate: TrainIqDataCoordinator,
) : ProgressRepository {
    override fun observeProgressOverview(): Flow<ProgressOverview> = delegate.observeProgressOverview()
    override suspend fun analyzeBodyMeasurementPhoto(path: String, context: String): BodyMeasurementPhotoResult =
        delegate.analyzeBodyMeasurementPhoto(path, context)
    override suspend fun addMeasurement(weight: Double, bodyFat: Double, muscleMass: Double) = delegate.addMeasurement(weight, bodyFat, muscleMass)
    override suspend fun deleteMeasurement(measurementId: Long) = delegate.deleteMeasurement(measurementId)
}

@Singleton
class RoomCoachRepository @Inject constructor(
    private val delegate: TrainIqDataCoordinator,
) : CoachRepository {
    override fun observeCoachOverview() = delegate.observeCoachOverview()
    override suspend fun generateGoalAdvice(height: Double, weight: Double, bodyFat: Double, age: Int, sex: BiologicalSex, activityLevel: String, goal: String): GoalAdvice =
        delegate.generateGoalAdvice(height, weight, bodyFat, age, sex, activityLevel, goal)
    override suspend fun generateWeeklyReport(): WeeklyReportResult = delegate.generateWeeklyReport()
    override fun observeUserProfile(): Flow<UserProfile?> = delegate.observeUserProfile()
    override suspend fun saveProfile(profile: UserProfile) = delegate.saveProfile(profile)
}
