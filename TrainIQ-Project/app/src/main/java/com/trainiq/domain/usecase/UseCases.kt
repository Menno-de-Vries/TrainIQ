package com.trainiq.domain.usecase

import com.trainiq.domain.model.LoggedSet
import com.trainiq.domain.model.MealScanItem
import com.trainiq.domain.repository.CoachRepository
import com.trainiq.domain.repository.HomeRepository
import com.trainiq.domain.repository.NutritionRepository
import com.trainiq.domain.repository.ProgressRepository
import com.trainiq.domain.repository.WorkoutRepository
import javax.inject.Inject

class ObserveHomeDashboardUseCase @Inject constructor(private val repository: HomeRepository) {
    operator fun invoke() = repository.observeDashboard()
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

class ObserveNutritionUseCase @Inject constructor(private val repository: NutritionRepository) {
    operator fun invoke() = repository.observeNutritionOverview()
}

class AnalyzeMealUseCase @Inject constructor(private val repository: NutritionRepository) {
    suspend operator fun invoke(path: String) = repository.analyzeMealPhoto(path)
}

class SaveScannedMealUseCase @Inject constructor(private val repository: NutritionRepository) {
    suspend operator fun invoke(items: List<MealScanItem>) = repository.saveScannedMeal(items)
}

class ObserveProgressUseCase @Inject constructor(private val repository: ProgressRepository) {
    operator fun invoke() = repository.observeProgressOverview()
}

class ObserveCoachUseCase @Inject constructor(private val repository: CoachRepository) {
    operator fun invoke() = repository.observeCoachOverview()
}

class GenerateGoalAdviceUseCase @Inject constructor(private val repository: CoachRepository) {
    suspend operator fun invoke(height: Double, weight: Double, bodyFat: Double, goal: String) =
        repository.generateGoalAdvice(height, weight, bodyFat, goal)
}
