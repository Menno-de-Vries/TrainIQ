package com.trainiq.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TrainIqDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUserProfile(profile: UserProfileEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutines(routines: List<WorkoutRoutineEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkoutDays(days: List<WorkoutDayEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercises(exercises: List<ExerciseEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkoutExercises(exercises: List<WorkoutExerciseEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeals(meals: List<MealEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeasurements(measurements: List<BodyMeasurementEntity>)

    @Insert
    suspend fun insertWorkoutSession(session: WorkoutSessionEntity): Long

    @Insert
    suspend fun insertWorkoutSets(sets: List<WorkoutSetEntity>)

    @Insert
    suspend fun insertMeal(meal: MealEntity)

    @Query("SELECT COUNT(*) FROM exercises")
    suspend fun exerciseCount(): Int

    @Query("SELECT * FROM user_profile LIMIT 1")
    fun observeUserProfile(): Flow<UserProfileEntity?>

    @Query("SELECT * FROM workout_routines ORDER BY active DESC, id ASC")
    fun observeRoutines(): Flow<List<WorkoutRoutineEntity>>

    @Query("SELECT * FROM workout_days ORDER BY orderIndex ASC")
    fun observeWorkoutDays(): Flow<List<WorkoutDayEntity>>

    @Query("SELECT * FROM exercises ORDER BY name ASC")
    fun observeExercises(): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM workout_exercises ORDER BY id ASC")
    fun observeWorkoutExercises(): Flow<List<WorkoutExerciseEntity>>

    @Query("SELECT * FROM workout_sessions ORDER BY date DESC")
    fun observeWorkoutSessions(): Flow<List<WorkoutSessionEntity>>

    @Query("SELECT * FROM workout_sets ORDER BY id DESC")
    fun observeWorkoutSets(): Flow<List<WorkoutSetEntity>>

    @Query("SELECT * FROM meals ORDER BY date DESC")
    fun observeMeals(): Flow<List<MealEntity>>

    @Query("SELECT * FROM body_measurements ORDER BY date ASC")
    fun observeMeasurements(): Flow<List<BodyMeasurementEntity>>

    @Query("SELECT * FROM workout_days WHERE id = :dayId LIMIT 1")
    suspend fun getWorkoutDay(dayId: Long): WorkoutDayEntity?

    @Query("SELECT * FROM workout_exercises WHERE dayId = :dayId ORDER BY id ASC")
    suspend fun getWorkoutExercisesForDay(dayId: Long): List<WorkoutExerciseEntity>

    @Query("SELECT * FROM exercises WHERE id = :exerciseId LIMIT 1")
    suspend fun getExercise(exerciseId: Long): ExerciseEntity?

    @Query("UPDATE workout_routines SET active = CASE WHEN id = :routineId THEN 1 ELSE 0 END")
    suspend fun setActiveRoutine(routineId: Long)
}
