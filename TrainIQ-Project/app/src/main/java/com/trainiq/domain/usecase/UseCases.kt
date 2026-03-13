package com.trainiq.domain.usecase

import com.trainiq.domain.model.LoggedSet
import com.trainiq.domain.model.UserProfile
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

class ObserveWorkoutOverviewUseCase @Inject constructor(private val repository: WorkoutRepository) {
    operator fun invoke() = repository.observeWorkoutOverview()
}

class GetWorkoutDayUseCase @Inject constructor(private val repository: WorkoutRepository) {
    suspend operator fun invoke(dayId: Long) = repository.getWorkoutDay(dayId)
}

class FinishWorkoutUseCase @Inject constructor(private val repository: WorkoutRepository) {
    suspend operator fun invoke(dayId: Long, durationSeconds: Long, loggedSets: List<LoggedSet>) =
        repository.finishWorkout(dayId, durationSeconds, loggedSets)
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
    ) = repository.addExerciseToDay(dayId, name, muscleGroup, equipment, targetSets, repRange, restSeconds)
}

class RemoveExerciseFromDayUseCase @Inject constructor(private val repository: WorkoutRepository) {
    suspend operator fun invoke(workoutExerciseId: Long) = repository.removeExerciseFromDay(workoutExerciseId)
}

class DeleteWorkoutSessionUseCase @Inject constructor(private val repository: WorkoutRepository) {
    suspend operator fun invoke(sessionId: Long) = repository.deleteWorkoutSession(sessionId)
}

class ObserveNutritionUseCase @Inject constructor(private val repository: NutritionRepository) {
    operator fun invoke() = repository.observeNutritionOverview()
}

class AnalyzeMealUseCase @Inject constructor(private val repository: NutritionRepository) {
    suspend operator fun invoke(path: String, context: String) = repository.analyzeMealPhoto(path, context)
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
        name: String,
        notes: String?,
        items: List<MealEntryRequest>,
    ) = repository.saveMeal(id, name, notes, items)
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
    suspend operator fun invoke(height: Double, weight: Double, bodyFat: Double, goal: String) =
        repository.generateGoalAdvice(height, weight, bodyFat, goal)
}

class GenerateWeeklyReportUseCase @Inject constructor(private val repository: CoachRepository) {
    suspend operator fun invoke() = repository.generateWeeklyReport()
}

class ObserveUserProfileUseCase @Inject constructor(private val repository: CoachRepository) {
    operator fun invoke() = repository.observeUserProfile()
}

class SaveUserProfileUseCase @Inject constructor(private val repository: CoachRepository) {
    suspend operator fun invoke(profile: UserProfile) = repository.saveProfile(profile)
}
