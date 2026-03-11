package com.trainiq.domain.repository

import com.trainiq.domain.model.CoachOverview
import com.trainiq.domain.model.GoalAdvice
import com.trainiq.domain.model.HomeDashboard
import com.trainiq.domain.model.LoggedSet
import com.trainiq.domain.model.MealScanItem
import com.trainiq.domain.model.NutritionOverview
import com.trainiq.domain.model.ProgressOverview
import com.trainiq.domain.model.WorkoutDay
import com.trainiq.domain.model.WorkoutDebrief
import com.trainiq.domain.model.WorkoutOverview
import kotlinx.coroutines.flow.Flow

interface HomeRepository {
    fun observeDashboard(): Flow<HomeDashboard>
}

interface WorkoutRepository {
    fun observeWorkoutOverview(): Flow<WorkoutOverview>
    suspend fun getWorkoutDay(dayId: Long): WorkoutDay?
    suspend fun getNextWorkoutDay(): WorkoutDay?
    suspend fun setActiveRoutine(routineId: Long)
    suspend fun finishWorkout(dayId: Long, durationSeconds: Long, loggedSets: List<LoggedSet>): WorkoutDebrief
}

interface NutritionRepository {
    fun observeNutritionOverview(): Flow<NutritionOverview>
    suspend fun analyzeMealPhoto(path: String): NutritionOverview
    suspend fun saveScannedMeal(items: List<MealScanItem>)
}

interface ProgressRepository {
    fun observeProgressOverview(): Flow<ProgressOverview>
}

interface CoachRepository {
    fun observeCoachOverview(): Flow<CoachOverview>
    suspend fun generateGoalAdvice(height: Double, weight: Double, bodyFat: Double, goal: String): GoalAdvice
}
