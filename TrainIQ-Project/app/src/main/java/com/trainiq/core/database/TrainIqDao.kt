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
    suspend fun insertMirrorImportRun(run: RoomMirrorImportRunEntity)

    @Query("SELECT * FROM room_mirror_import_runs ORDER BY finished_at DESC LIMIT 1")
    suspend fun latestMirrorImportRun(): RoomMirrorImportRunEntity?

    @Query(
        """
        SELECT
            (SELECT COUNT(*) FROM user_profile) +
            (SELECT COUNT(*) FROM workout_routines) +
            (SELECT COUNT(*) FROM workout_days) +
            (SELECT COUNT(*) FROM exercises) +
            (SELECT COUNT(*) FROM workout_exercises) +
            (SELECT COUNT(*) FROM routine_sets) +
            (SELECT COUNT(*) FROM workout_sessions) +
            (SELECT COUNT(*) FROM performed_exercises) +
            (SELECT COUNT(*) FROM workout_sets) +
            (SELECT COUNT(*) FROM meals) +
            (SELECT COUNT(*) FROM food_items) +
            (SELECT COUNT(*) FROM recipes) +
            (SELECT COUNT(*) FROM recipe_ingredients) +
            (SELECT COUNT(*) FROM meal_items) +
            (SELECT COUNT(*) FROM active_workout_sessions) +
            (SELECT COUNT(*) FROM active_workout_drafts) +
            (SELECT COUNT(*) FROM active_workout_collapsed_exercises) +
            (SELECT COUNT(*) FROM active_workout_sets) +
            (SELECT COUNT(*) FROM workout_log_events) +
            (SELECT COUNT(*) FROM workout_log_event_sets) +
            (SELECT COUNT(*) FROM body_measurements)
        """
    )
    suspend fun mirrorRowCount(): Int

    @Query("DELETE FROM user_profile")
    suspend fun clearMirrorUserProfile()

    @Query("DELETE FROM workout_routines")
    suspend fun clearMirrorRoutines()

    @Query("DELETE FROM workout_days")
    suspend fun clearMirrorWorkoutDays()

    @Query("DELETE FROM exercises")
    suspend fun clearMirrorExercises()

    @Query("DELETE FROM workout_exercises")
    suspend fun clearMirrorWorkoutExercises()

    @Query("DELETE FROM routine_sets")
    suspend fun clearMirrorRoutineSets()

    @Query("DELETE FROM workout_sessions")
    suspend fun clearMirrorWorkoutSessions()

    @Query("DELETE FROM performed_exercises")
    suspend fun clearMirrorPerformedExercises()

    @Query("DELETE FROM workout_sets")
    suspend fun clearMirrorWorkoutSets()

    @Query("DELETE FROM meals")
    suspend fun clearMirrorMeals()

    @Query("DELETE FROM food_items")
    suspend fun clearMirrorFoodItems()

    @Query("DELETE FROM recipes")
    suspend fun clearMirrorRecipes()

    @Query("DELETE FROM recipe_ingredients")
    suspend fun clearMirrorRecipeIngredients()

    @Query("DELETE FROM meal_items")
    suspend fun clearMirrorMealItems()

    @Query("DELETE FROM active_workout_sessions")
    suspend fun clearMirrorActiveWorkoutSessions()

    @Query("DELETE FROM active_workout_drafts")
    suspend fun clearMirrorActiveWorkoutDrafts()

    @Query("DELETE FROM active_workout_collapsed_exercises")
    suspend fun clearMirrorActiveWorkoutCollapsedExercises()

    @Query("DELETE FROM active_workout_sets")
    suspend fun clearMirrorActiveWorkoutSets()

    @Query("DELETE FROM workout_log_events")
    suspend fun clearMirrorWorkoutLogEvents()

    @Query("DELETE FROM workout_log_event_sets")
    suspend fun clearMirrorWorkoutLogEventSets()

    @Query("DELETE FROM body_measurements")
    suspend fun clearMirrorMeasurements()

    @Transaction
    suspend fun clearMirrorTables() {
        clearMirrorWorkoutLogEventSets()
        clearMirrorWorkoutLogEvents()
        clearMirrorActiveWorkoutSets()
        clearMirrorActiveWorkoutCollapsedExercises()
        clearMirrorActiveWorkoutDrafts()
        clearMirrorActiveWorkoutSessions()
        clearMirrorMealItems()
        clearMirrorRecipeIngredients()
        clearMirrorRecipes()
        clearMirrorFoodItems()
        clearMirrorMeasurements()
        clearMirrorMeals()
        clearMirrorWorkoutSets()
        clearMirrorPerformedExercises()
        clearMirrorWorkoutSessions()
        clearMirrorRoutineSets()
        clearMirrorWorkoutExercises()
        clearMirrorExercises()
        clearMirrorWorkoutDays()
        clearMirrorRoutines()
        clearMirrorUserProfile()
    }

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
    suspend fun insertRoutineSets(sets: List<RoutineSetEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeals(meals: List<MealEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeasurements(measurements: List<BodyMeasurementEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun importWorkoutSessions(sessions: List<WorkoutSessionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun importWorkoutSets(sets: List<WorkoutSetEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun importMeals(meals: List<MealEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun importMeasurements(measurements: List<BodyMeasurementEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFoodItems(foodItems: List<FoodItemEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecipes(recipes: List<RecipeEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecipeIngredients(ingredients: List<RecipeIngredientEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMealItems(items: List<MealItemEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActiveWorkoutSessions(sessions: List<ActiveWorkoutSessionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActiveWorkoutDrafts(drafts: List<ActiveWorkoutDraftEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActiveWorkoutCollapsedExercises(collapsedExercises: List<ActiveWorkoutCollapsedExerciseEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActiveWorkoutSets(sets: List<ActiveWorkoutSetEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkoutLogEvents(events: List<WorkoutLogEventEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkoutLogEventSets(sets: List<WorkoutLogEventSetEntity>)

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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPerformedExercises(exercises: List<PerformedExerciseEntity>)

    @Insert
    suspend fun insertWorkoutSets(sets: List<WorkoutSetEntity>)

    @Insert
    suspend fun insertMeal(meal: MealEntity)

    @Insert
    suspend fun insertMeasurement(measurement: BodyMeasurementEntity)

    @Query("SELECT COUNT(*) FROM exercises")
    suspend fun exerciseCount(): Int

    @Query("SELECT COUNT(*) FROM user_profile")
    suspend fun userProfileCount(): Int

    @Query("SELECT COUNT(*) FROM food_items")
    suspend fun foodCount(): Int

    @Query("SELECT COUNT(*) FROM recipes")
    suspend fun recipeCount(): Int

    @Query("SELECT COUNT(*) FROM recipe_ingredients")
    suspend fun recipeIngredientCount(): Int

    @Query("SELECT COUNT(*) FROM meal_items")
    suspend fun mealItemCount(): Int

    @Query("SELECT COUNT(*) FROM active_workout_sessions")
    suspend fun activeWorkoutSessionCount(): Int

    @Query("SELECT COUNT(*) FROM active_workout_sets")
    suspend fun activeWorkoutSetCount(): Int

    @Query("SELECT COUNT(*) FROM workout_log_events")
    suspend fun workoutLogEventCount(): Int

    @Query("SELECT COUNT(*) FROM workout_log_event_sets WHERE snapshot_role = 'CURRENT'")
    suspend fun workoutLogEventCurrentSetCount(): Int

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

    @Query("SELECT * FROM routine_sets ORDER BY workoutExerciseId ASC, order_index ASC, id ASC")
    fun observeRoutineSets(): Flow<List<RoutineSetEntity>>

    @Query("SELECT * FROM workout_sessions ORDER BY date DESC")
    fun observeWorkoutSessions(): Flow<List<WorkoutSessionEntity>>

    @Query("SELECT * FROM performed_exercises ORDER BY session_id DESC, order_index ASC")
    fun observePerformedExercises(): Flow<List<PerformedExerciseEntity>>

    @Query("SELECT * FROM performed_exercises WHERE exercise_id = :exerciseId ORDER BY session_id DESC, order_index ASC")
    fun observePerformedExercisesForExercise(exerciseId: Long): Flow<List<PerformedExerciseEntity>>

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

    @Query("DELETE FROM routine_sets WHERE workoutExerciseId = :workoutExerciseId")
    suspend fun deleteRoutineSetsForExercise(workoutExerciseId: Long)

    @Query("DELETE FROM workout_exercises WHERE dayId = :dayId")
    suspend fun deleteWorkoutExercisesForDay(dayId: Long)

    @Query("DELETE FROM routine_sets WHERE workoutExerciseId IN (SELECT id FROM workout_exercises WHERE dayId = :dayId)")
    suspend fun deleteRoutineSetsForDay(dayId: Long)

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
        deleteRoutineSetsForDay(dayId)
        deleteWorkoutExercisesForDay(dayId)
        deleteWorkoutDay(dayId)
    }

    @Transaction
    suspend fun deleteRoutineCascade(routineId: Long) {
        observeWorkoutDaysSnapshot(routineId).forEach { day ->
            deleteRoutineSetsForDay(day.id)
            deleteWorkoutExercisesForDay(day.id)
        }
        deleteWorkoutDaysForRoutine(routineId)
        deleteRoutine(routineId)
    }

    @Query("SELECT * FROM workout_days WHERE routineId = :routineId")
    suspend fun observeWorkoutDaysSnapshot(routineId: Long): List<WorkoutDayEntity>
}
