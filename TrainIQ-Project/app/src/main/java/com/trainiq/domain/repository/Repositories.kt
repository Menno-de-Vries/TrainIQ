package com.trainiq.domain.repository

import com.trainiq.domain.model.CoachOverview
import com.trainiq.domain.model.GoalAdvice
import com.trainiq.domain.model.HealthConnectStatus
import com.trainiq.domain.model.HomeDashboard
import com.trainiq.domain.model.LoggedSet
import com.trainiq.domain.model.MealScanItem
import com.trainiq.domain.model.NutritionOverview
import com.trainiq.domain.model.ProgressOverview
import com.trainiq.domain.model.UserProfile
import com.trainiq.domain.model.WorkoutDay
import com.trainiq.domain.model.WorkoutDebrief
import com.trainiq.domain.model.WorkoutOverview
import kotlinx.coroutines.flow.Flow

interface HomeRepository {
    fun observeDashboard(): Flow<HomeDashboard>
    suspend fun getHealthConnectStatus(): HealthConnectStatus
}

interface WorkoutRepository {
    fun observeWorkoutOverview(): Flow<WorkoutOverview>
    suspend fun getWorkoutDay(dayId: Long): WorkoutDay?
    suspend fun getNextWorkoutDay(): WorkoutDay?
    suspend fun setActiveRoutine(routineId: Long)
    suspend fun finishWorkout(dayId: Long, durationSeconds: Long, loggedSets: List<LoggedSet>): WorkoutDebrief
    suspend fun createRoutine(name: String, description: String)
    suspend fun updateRoutine(routineId: Long, name: String, description: String)
    suspend fun deleteRoutine(routineId: Long)
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
    )
    suspend fun removeExerciseFromDay(workoutExerciseId: Long)
    suspend fun deleteWorkoutSession(sessionId: Long)
}

interface NutritionRepository {
    fun observeNutritionOverview(): Flow<NutritionOverview>
    suspend fun analyzeMealPhoto(path: String): NutritionOverview
    suspend fun saveScannedMeal(items: List<MealScanItem>)
    suspend fun addMeal(calories: Int, protein: Int, carbs: Int, fat: Int)
    suspend fun updateMeal(mealId: Long, calories: Int, protein: Int, carbs: Int, fat: Int)
    suspend fun deleteMeal(mealId: Long)
}

interface ProgressRepository {
    fun observeProgressOverview(): Flow<ProgressOverview>
    suspend fun addMeasurement(weight: Double, bodyFat: Double, muscleMass: Double)
    suspend fun deleteMeasurement(measurementId: Long)
}

interface CoachRepository {
    fun observeCoachOverview(): Flow<CoachOverview>
    suspend fun generateGoalAdvice(height: Double, weight: Double, bodyFat: Double, goal: String): GoalAdvice
    suspend fun generateWeeklyReport(): String
    fun observeUserProfile(): Flow<UserProfile?>
    suspend fun saveProfile(profile: UserProfile)
}
