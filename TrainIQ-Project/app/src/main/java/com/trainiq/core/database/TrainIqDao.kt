package com.trainiq.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutine(routine: WorkoutRoutineEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkoutDay(day: WorkoutDayEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercise(exercise: ExerciseEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkoutExercise(exercise: WorkoutExerciseEntity)

    @Insert
    suspend fun insertWorkoutSession(session: WorkoutSessionEntity): Long

    @Insert
    suspend fun insertWorkoutSets(sets: List<WorkoutSetEntity>)

    @Insert
    suspend fun insertMeal(meal: MealEntity)

    @Insert
    suspend fun insertMeasurement(measurement: BodyMeasurementEntity)

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

    @Query("SELECT * FROM workout_exercises ORDER BY order_index ASC, id ASC")
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

    @Query("SELECT * FROM workout_exercises WHERE dayId = :dayId ORDER BY order_index ASC, id ASC")
    suspend fun getWorkoutExercisesForDay(dayId: Long): List<WorkoutExerciseEntity>

    @Query("SELECT * FROM exercises WHERE id = :exerciseId LIMIT 1")
    suspend fun getExercise(exerciseId: Long): ExerciseEntity?

    @Query(
        """
        SELECT * FROM exercises
        WHERE :query = ''
            OR name LIKE '%' || :query || '%'
            OR muscleGroup LIKE '%' || :query || '%'
            OR equipment LIKE '%' || :query || '%'
        ORDER BY name ASC
        """,
    )
    suspend fun searchExercises(query: String): List<ExerciseEntity>

    @Query("UPDATE workout_routines SET active = CASE WHEN id = :routineId THEN 1 ELSE 0 END")
    suspend fun setActiveRoutine(routineId: Long)

    @Query("SELECT COUNT(*) FROM workout_routines")
    suspend fun routineCount(): Int

    @Query("SELECT MAX(id) FROM workout_routines")
    suspend fun getMaxRoutineId(): Long?

    @Query("SELECT MAX(id) FROM workout_days")
    suspend fun getMaxWorkoutDayId(): Long?

    @Query("SELECT MAX(id) FROM exercises")
    suspend fun getMaxExerciseId(): Long?

    @Query("SELECT MAX(id) FROM workout_exercises")
    suspend fun getMaxWorkoutExerciseId(): Long?

    @Query("DELETE FROM workout_exercises WHERE id = :workoutExerciseId")
    suspend fun deleteWorkoutExercise(workoutExerciseId: Long)

    @Query("DELETE FROM workout_exercises WHERE dayId = :dayId")
    suspend fun deleteWorkoutExercisesForDay(dayId: Long)

    @Query("DELETE FROM workout_days WHERE id = :dayId")
    suspend fun deleteWorkoutDay(dayId: Long)

    @Query("DELETE FROM workout_days WHERE routineId = :routineId")
    suspend fun deleteWorkoutDaysForRoutine(routineId: Long)

    @Query("DELETE FROM workout_routines WHERE id = :routineId")
    suspend fun deleteRoutine(routineId: Long)

    @Query("UPDATE workout_routines SET name = :name, description = :description WHERE id = :routineId")
    suspend fun updateRoutine(routineId: Long, name: String, description: String)

    @Query("UPDATE workout_exercises SET order_index = :orderIndex WHERE dayId = :dayId AND id = :workoutExerciseId")
    suspend fun updateWorkoutExerciseOrder(dayId: Long, workoutExerciseId: Long, orderIndex: Int)

    @Transaction
    suspend fun reorderExercises(dayId: Long, orderedIds: List<Long>) {
        orderedIds.distinct().forEachIndexed { index, workoutExerciseId ->
            updateWorkoutExerciseOrder(dayId, workoutExerciseId, index)
        }
    }

    @Transaction
    suspend fun deleteWorkoutDayCascade(dayId: Long) {
        deleteWorkoutExercisesForDay(dayId)
        deleteWorkoutDay(dayId)
    }

    @Transaction
    suspend fun deleteRoutineCascade(routineId: Long) {
        observeWorkoutDaysSnapshot(routineId).forEach { day ->
            deleteWorkoutExercisesForDay(day.id)
        }
        deleteWorkoutDaysForRoutine(routineId)
        deleteRoutine(routineId)
    }

    @Query("SELECT * FROM workout_days WHERE routineId = :routineId")
    suspend fun observeWorkoutDaysSnapshot(routineId: Long): List<WorkoutDayEntity>
}
