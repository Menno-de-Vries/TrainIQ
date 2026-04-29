package com.trainiq.domain.usecase

import com.trainiq.domain.model.LoggedSet
import com.trainiq.domain.model.ActiveWorkoutSetDraft
import com.trainiq.domain.model.BiologicalSex
import com.trainiq.domain.model.GeneratedRoutine
import com.trainiq.domain.model.ExerciseHistory
import com.trainiq.domain.model.MealType
import com.trainiq.domain.model.ProgressionSuggestion
import com.trainiq.domain.model.RoutineSet
import com.trainiq.domain.model.SetType
import com.trainiq.domain.model.UserProfile
import com.trainiq.domain.model.WeeklyReportResult
import com.trainiq.domain.model.WorkoutLoggingSummary
import com.trainiq.domain.repository.MealEntryRequest
import com.trainiq.domain.model.FoodSourceType
import com.trainiq.domain.repository.CoachRepository
import com.trainiq.domain.repository.HomeRepository
import com.trainiq.domain.repository.NutritionRepository
import com.trainiq.domain.repository.ProgressRepository
import com.trainiq.domain.repository.WorkoutRepository
import javax.inject.Inject

class ObserveHomeDashboardUseCase @Inject constructor(private val repository: HomeRepository) {
    operator fun invoke() = repository.observeDashboard()
}

class GetHealthConnectStatusUseCase @Inject constructor(private val repository: HomeRepository) {
    suspend operator fun invoke() = repository.getHealthConnectStatus()
}

class RefreshDashboardDataUseCase @Inject constructor(private val repository: HomeRepository) {
    suspend operator fun invoke() = repository.refreshDashboardData()
}

class ObserveWorkoutOverviewUseCase @Inject constructor(private val repository: WorkoutRepository) {
    operator fun invoke() = repository.observeWorkoutOverview()
}

class ObserveWorkoutLoggingSummaryUseCase @Inject constructor(private val repository: WorkoutRepository) {
    operator fun invoke(dayId: Long): kotlinx.coroutines.flow.Flow<WorkoutLoggingSummary> =
        repository.observeWorkoutLoggingSummary(dayId)
}

class ObserveExerciseHistoryUseCase @Inject constructor(private val repository: WorkoutRepository) {
    operator fun invoke(exerciseId: Long): kotlinx.coroutines.flow.Flow<ExerciseHistory> =
        repository.observeExerciseHistory(exerciseId)
}

class GetWorkoutDayUseCase @Inject constructor(private val repository: WorkoutRepository) {
    suspend operator fun invoke(dayId: Long) = repository.getWorkoutDay(dayId)
}

class GetProgressionSuggestionsUseCase @Inject constructor(private val repository: WorkoutRepository) {
    suspend operator fun invoke(dayId: Long): List<ProgressionSuggestion> =
        repository.getProgressionSuggestions(dayId)
}

class FinishWorkoutUseCase @Inject constructor(private val repository: WorkoutRepository) {
    suspend operator fun invoke(dayId: Long, durationSeconds: Long, loggedSets: List<LoggedSet>) =
        repository.finishWorkout(dayId, durationSeconds, loggedSets)
}

class GetOrStartActiveWorkoutSessionUseCase @Inject constructor(private val repository: WorkoutRepository) {
    suspend operator fun invoke(dayId: Long, initialDrafts: Map<Long, ActiveWorkoutSetDraft>) =
        repository.getOrStartActiveWorkoutSession(dayId, initialDrafts)
}

class UpdateActiveWorkoutDraftUseCase @Inject constructor(private val repository: WorkoutRepository) {
    suspend operator fun invoke(exerciseId: Long, draft: ActiveWorkoutSetDraft) =
        repository.updateActiveWorkoutDraft(exerciseId, draft)
}

class LogActiveWorkoutSetUseCase @Inject constructor(private val repository: WorkoutRepository) {
    suspend operator fun invoke(dayId: Long, set: LoggedSet, draft: ActiveWorkoutSetDraft, restSeconds: Int) =
        repository.logActiveWorkoutSet(dayId, set, draft, restSeconds)
}

class UpdateActiveWorkoutSetUseCase @Inject constructor(private val repository: WorkoutRepository) {
    suspend operator fun invoke(setId: Long, set: LoggedSet, draft: ActiveWorkoutSetDraft, restSeconds: Int) =
        repository.updateActiveWorkoutSet(setId, set, draft, restSeconds)
}

class UpdateActiveWorkoutSetTypeUseCase @Inject constructor(private val repository: WorkoutRepository) {
    suspend operator fun invoke(setId: Long, setType: SetType) =
        repository.updateActiveWorkoutSetType(setId, setType)
}

class DeleteActiveWorkoutSetUseCase @Inject constructor(private val repository: WorkoutRepository) {
    suspend operator fun invoke(setId: Long) =
        repository.deleteActiveWorkoutSet(setId)
}

class UndoWorkoutLogEventUseCase @Inject constructor(private val repository: WorkoutRepository) {
    suspend operator fun invoke(eventId: Long) = repository.undoWorkoutLogEvent(eventId)
}

class SetActiveWorkoutCollapsedUseCase @Inject constructor(private val repository: WorkoutRepository) {
    suspend operator fun invoke(exerciseId: Long, collapsed: Boolean) =
        repository.setActiveWorkoutCollapsed(exerciseId, collapsed)
}

class UpdateActiveWorkoutRestTimerUseCase @Inject constructor(private val repository: WorkoutRepository) {
    suspend operator fun invoke(endsAt: Long?, totalSeconds: Int) =
        repository.updateActiveWorkoutRestTimer(endsAt, totalSeconds)
}

class FinishActiveWorkoutUseCase @Inject constructor(private val repository: WorkoutRepository) {
    suspend operator fun invoke(dayId: Long) = repository.finishActiveWorkout(dayId)
}

class DiscardActiveWorkoutUseCase @Inject constructor(private val repository: WorkoutRepository) {
    suspend operator fun invoke(dayId: Long) = repository.discardActiveWorkout(dayId)
}

class CreateRoutineUseCase @Inject constructor(private val repository: WorkoutRepository) {
    suspend operator fun invoke(name: String, description: String) = repository.createRoutine(name, description)
}

class UpdateRoutineUseCase @Inject constructor(private val repository: WorkoutRepository) {
    suspend operator fun invoke(routineId: Long, name: String, description: String) =
        repository.updateRoutine(routineId, name, description)
}

class DeleteRoutineUseCase @Inject constructor(private val repository: WorkoutRepository) {
    suspend operator fun invoke(routineId: Long) = repository.deleteRoutine(routineId)
}

class SearchExercisesUseCase @Inject constructor(private val repository: WorkoutRepository) {
    suspend operator fun invoke(query: String) = repository.searchExercises(query)
}

class ReorderExercisesUseCase @Inject constructor(private val repository: WorkoutRepository) {
    suspend operator fun invoke(dayId: Long, orderedIds: List<Long>) = repository.reorderExercises(dayId, orderedIds)
}

class SetSupersetGroupUseCase @Inject constructor(private val repository: WorkoutRepository) {
    suspend operator fun invoke(workoutExerciseIds: List<Long>, groupId: Long?) =
        repository.setSupersetGroup(workoutExerciseIds, groupId)
}

class ReplaceExerciseInPlanUseCase @Inject constructor(private val repository: WorkoutRepository) {
    suspend operator fun invoke(workoutExerciseId: Long, newExerciseId: Long) =
        repository.replaceExerciseInPlan(workoutExerciseId, newExerciseId)
}

class ReplaceExerciseInActiveWorkoutUseCase @Inject constructor(private val repository: WorkoutRepository) {
    suspend operator fun invoke(workoutExerciseId: Long, newExerciseId: Long) =
        repository.replaceExerciseInActiveWorkout(workoutExerciseId, newExerciseId)
}

class UpdateWorkoutExercisePlanUseCase @Inject constructor(private val repository: WorkoutRepository) {
    suspend operator fun invoke(
        workoutExerciseId: Long,
        targetSets: Int,
        repRange: String,
        restSeconds: Int,
        targetWeightKg: Double,
        targetRpe: Double,
        setType: SetType,
    ) = repository.updateWorkoutExercisePlan(workoutExerciseId, targetSets, repRange, restSeconds, targetWeightKg, targetRpe, setType)
}

class AddSetToExerciseUseCase @Inject constructor(private val repository: WorkoutRepository) {
    suspend operator fun invoke(workoutExerciseId: Long) = repository.addSetToExercise(workoutExerciseId)
}

class UpdateRoutineSetUseCase @Inject constructor(private val repository: WorkoutRepository) {
    suspend operator fun invoke(set: RoutineSet) = repository.updateRoutineSet(set)
}

class UpdateRoutineSetTypeUseCase @Inject constructor(private val repository: WorkoutRepository) {
    suspend operator fun invoke(setId: Long, setType: SetType) = repository.updateRoutineSetType(setId, setType)
}

class UpdateRoutineSetRepsUseCase @Inject constructor(private val repository: WorkoutRepository) {
    suspend operator fun invoke(setId: Long, targetReps: Int) = repository.updateRoutineSetReps(setId, targetReps)
}

class UpdateRoutineSetWeightUseCase @Inject constructor(private val repository: WorkoutRepository) {
    suspend operator fun invoke(setId: Long, targetWeightKg: Double) = repository.updateRoutineSetWeight(setId, targetWeightKg)
}

class UpdateRoutineSetRestTimeUseCase @Inject constructor(private val repository: WorkoutRepository) {
    suspend operator fun invoke(setId: Long, restSeconds: Int) = repository.updateRoutineSetRestTime(setId, restSeconds)
}

class DeleteRoutineSetUseCase @Inject constructor(private val repository: WorkoutRepository) {
    suspend operator fun invoke(setId: Long) = repository.deleteRoutineSet(setId)
}

class MoveRoutineSetUseCase @Inject constructor(private val repository: WorkoutRepository) {
    suspend operator fun invoke(workoutExerciseId: Long, orderedSetIds: List<Long>) =
        repository.moveRoutineSet(workoutExerciseId, orderedSetIds)
}

class SetActiveRoutineUseCase @Inject constructor(private val repository: WorkoutRepository) {
    suspend operator fun invoke(routineId: Long) = repository.setActiveRoutine(routineId)
}

class AddWorkoutDayUseCase @Inject constructor(private val repository: WorkoutRepository) {
    suspend operator fun invoke(routineId: Long, name: String) = repository.addWorkoutDay(routineId, name)
}

class RemoveWorkoutDayUseCase @Inject constructor(private val repository: WorkoutRepository) {
    suspend operator fun invoke(dayId: Long) = repository.removeWorkoutDay(dayId)
}

class AddExerciseToDayUseCase @Inject constructor(private val repository: WorkoutRepository) {
    suspend operator fun invoke(
        dayId: Long,
        name: String,
        muscleGroup: String,
        equipment: String,
        targetSets: Int,
        repRange: String,
        restSeconds: Int,
        targetWeightKg: Double,
        targetRpe: Double,
    ) = repository.addExerciseToDay(dayId, name, muscleGroup, equipment, targetSets, repRange, restSeconds, targetWeightKg, targetRpe)
}

class AddExerciseToRoutineUseCase @Inject constructor(private val repository: WorkoutRepository) {
    suspend operator fun invoke(
        routineId: Long,
        name: String,
        muscleGroup: String,
        equipment: String,
        targetSets: Int,
        repRange: String,
        restSeconds: Int,
        targetWeightKg: Double,
        targetRpe: Double,
    ) = repository.addExerciseToRoutine(routineId, name, muscleGroup, equipment, targetSets, repRange, restSeconds, targetWeightKg, targetRpe)
}

class RemoveExerciseFromDayUseCase @Inject constructor(private val repository: WorkoutRepository) {
    suspend operator fun invoke(workoutExerciseId: Long) = repository.removeExerciseFromDay(workoutExerciseId)
}

class DeleteWorkoutSessionUseCase @Inject constructor(private val repository: WorkoutRepository) {
    suspend operator fun invoke(sessionId: Long) = repository.deleteWorkoutSession(sessionId)
}

class GenerateAiRoutineUseCase @Inject constructor(private val repository: WorkoutRepository) {
    suspend operator fun invoke(
        daysPerWeek: Int,
        equipment: String,
        targetFocus: String,
        experienceLevel: String,
        sessionDurationMinutes: Int,
        includeDeload: Boolean,
    ) = repository.generateAiRoutine(
        daysPerWeek = daysPerWeek,
        equipment = equipment,
        targetFocus = targetFocus,
        experienceLevel = experienceLevel,
        sessionDurationMinutes = sessionDurationMinutes,
        includeDeload = includeDeload,
    )
}

class SaveGeneratedRoutineUseCase @Inject constructor(private val repository: WorkoutRepository) {
    suspend operator fun invoke(routine: GeneratedRoutine) = repository.saveGeneratedRoutine(routine)
}

class ObserveNutritionUseCase @Inject constructor(private val repository: NutritionRepository) {
    operator fun invoke() = repository.observeNutritionOverview()
}

class AnalyzeMealUseCase @Inject constructor(private val repository: NutritionRepository) {
    suspend operator fun invoke(path: String, context: String, capturedAtMillis: Long) =
        repository.analyzeMealPhoto(path, context, capturedAtMillis)
}

class SaveFoodItemUseCase @Inject constructor(private val repository: NutritionRepository) {
    suspend operator fun invoke(
        id: Long?,
        name: String,
        barcode: String?,
        caloriesPer100g: Double,
        proteinPer100g: Double,
        carbsPer100g: Double,
        fatPer100g: Double,
        sourceType: FoodSourceType,
    ) = repository.saveFoodItem(id, name, barcode, caloriesPer100g, proteinPer100g, carbsPer100g, fatPer100g, sourceType)
}

class SaveRecipeUseCase @Inject constructor(private val repository: NutritionRepository) {
    suspend operator fun invoke(
        id: Long?,
        name: String,
        notes: String?,
        totalCookedGrams: Double?,
        ingredients: List<Pair<Long, Double>>,
    ) = repository.saveRecipe(id, name, notes, totalCookedGrams, ingredients)
}

class SaveMealUseCase @Inject constructor(private val repository: NutritionRepository) {
    suspend operator fun invoke(
        id: Long?,
        mealType: MealType,
        name: String,
        notes: String?,
        items: List<MealEntryRequest>,
    ) = repository.saveMeal(id, mealType, name, notes, items)
}

class DeleteMealUseCase @Inject constructor(private val repository: NutritionRepository) {
    suspend operator fun invoke(mealId: Long) = repository.deleteMeal(mealId)
}

class DeleteFoodUseCase @Inject constructor(private val repository: NutritionRepository) {
    suspend operator fun invoke(foodId: Long) = repository.deleteFood(foodId)
}

class DeleteRecipeUseCase @Inject constructor(private val repository: NutritionRepository) {
    suspend operator fun invoke(recipeId: Long) = repository.deleteRecipe(recipeId)
}

class ClearLastScanResultUseCase @Inject constructor(private val repository: NutritionRepository) {
    operator fun invoke() = repository.clearLastScanResult()
}

class ObserveProgressUseCase @Inject constructor(private val repository: ProgressRepository) {
    operator fun invoke() = repository.observeProgressOverview()
}

class AddMeasurementUseCase @Inject constructor(private val repository: ProgressRepository) {
    suspend operator fun invoke(weight: Double, bodyFat: Double, muscleMass: Double) =
        repository.addMeasurement(weight, bodyFat, muscleMass)
}

class DeleteMeasurementUseCase @Inject constructor(private val repository: ProgressRepository) {
    suspend operator fun invoke(measurementId: Long) = repository.deleteMeasurement(measurementId)
}

class ObserveCoachUseCase @Inject constructor(private val repository: CoachRepository) {
    operator fun invoke() = repository.observeCoachOverview()
}

class GenerateGoalAdviceUseCase @Inject constructor(private val repository: CoachRepository) {
    suspend operator fun invoke(
        height: Double,
        weight: Double,
        bodyFat: Double,
        age: Int,
        sex: BiologicalSex,
        activityLevel: String,
        goal: String,
    ) = repository.generateGoalAdvice(height, weight, bodyFat, age, sex, activityLevel, goal)
}

class GenerateWeeklyReportUseCase @Inject constructor(private val repository: CoachRepository) {
    suspend operator fun invoke(): WeeklyReportResult = repository.generateWeeklyReport()
}

class ObserveUserProfileUseCase @Inject constructor(private val repository: CoachRepository) {
    operator fun invoke() = repository.observeUserProfile()
}

class SaveUserProfileUseCase @Inject constructor(private val repository: CoachRepository) {
    suspend operator fun invoke(profile: UserProfile) = repository.saveProfile(profile)
}
