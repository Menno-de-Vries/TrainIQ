package com.trainiq.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
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

    @Upsert
    suspend fun upsertUserProfile(profile: UserProfileEntity)

    @Upsert
    suspend fun insertRoutines(routines: List<WorkoutRoutineEntity>)

    @Upsert
    suspend fun insertWorkoutDays(days: List<WorkoutDayEntity>)

    @Upsert
    suspend fun insertExercises(exercises: List<ExerciseEntity>)

    @Upsert
    suspend fun insertWorkoutExercises(exercises: List<WorkoutExerciseEntity>)

    @Upsert
    suspend fun insertRoutineSets(sets: List<RoutineSetEntity>)

    @Upsert
    suspend fun insertMeals(meals: List<MealEntity>)

    @Upsert
    suspend fun insertMeasurements(measurements: List<BodyMeasurementEntity>)

    @Upsert
    suspend fun importWorkoutSessions(sessions: List<WorkoutSessionEntity>)

    @Upsert
    suspend fun importWorkoutSets(sets: List<WorkoutSetEntity>)

    @Upsert
    suspend fun importMeals(meals: List<MealEntity>)

    @Upsert
    suspend fun importMeasurements(measurements: List<BodyMeasurementEntity>)

    @Upsert
    suspend fun insertFoodItems(foodItems: List<FoodItemEntity>)

    @Upsert
    suspend fun insertRecipes(recipes: List<RecipeEntity>)

    @Upsert
    suspend fun insertRecipeIngredients(ingredients: List<RecipeIngredientEntity>)

    @Upsert
    suspend fun insertMealItems(items: List<MealItemEntity>)

    @Upsert
    suspend fun insertActiveWorkoutSessions(sessions: List<ActiveWorkoutSessionEntity>)

    @Upsert
    suspend fun insertActiveWorkoutDrafts(drafts: List<ActiveWorkoutDraftEntity>)

    @Upsert
    suspend fun insertActiveWorkoutCollapsedExercises(collapsedExercises: List<ActiveWorkoutCollapsedExerciseEntity>)

    @Upsert
    suspend fun insertActiveWorkoutSets(sets: List<ActiveWorkoutSetEntity>)

    @Upsert
    suspend fun insertWorkoutLogEvents(events: List<WorkoutLogEventEntity>)

    @Upsert
    suspend fun insertWorkoutLogEventSets(sets: List<WorkoutLogEventSetEntity>)

    @Upsert
    suspend fun insertRoutine(routine: WorkoutRoutineEntity)

    @Upsert
    suspend fun insertWorkoutDay(day: WorkoutDayEntity)

    @Upsert
    suspend fun insertExercise(exercise: ExerciseEntity)

    @Upsert
    suspend fun insertWorkoutExercise(exercise: WorkoutExerciseEntity)

    @Insert
    suspend fun insertWorkoutSession(session: WorkoutSessionEntity): Long

    @Upsert
    suspend fun insertPerformedExercises(exercises: List<PerformedExerciseEntity>)

    @Insert
    suspend fun insertWorkoutSets(sets: List<WorkoutSetEntity>)

    @Insert
    suspend fun insertMeal(meal: MealEntity)

    @Insert
    suspend fun insertMeasurement(measurement: BodyMeasurementEntity)

    @Query("DELETE FROM body_measurements WHERE id = :measurementId")
    suspend fun deleteMeasurement(measurementId: Long)

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

    @Query("SELECT * FROM food_items ORDER BY name ASC")
    fun observeFoodItems(): Flow<List<FoodItemEntity>>

    @Query("SELECT * FROM recipes ORDER BY name ASC")
    fun observeRecipes(): Flow<List<RecipeEntity>>

    @Query("SELECT * FROM recipe_ingredients ORDER BY recipe_id ASC, order_index ASC")
    fun observeRecipeIngredients(): Flow<List<RecipeIngredientEntity>>

    @Query("SELECT * FROM meal_items ORDER BY meal_id ASC, order_index ASC")
    fun observeMealItems(): Flow<List<MealItemEntity>>

    @Query("SELECT * FROM active_workout_sessions ORDER BY updatedAt DESC LIMIT 1")
    fun observeActiveWorkoutSessions(): Flow<List<ActiveWorkoutSessionEntity>>

    @Query(
        """
        UPDATE active_workout_sessions
        SET restTimerEndsAt = :endsAt,
            restTimerTotalSeconds = :totalSeconds,
            updatedAt = :updatedAt
        WHERE sessionId = :sessionId
        """,
    )
    suspend fun updateActiveWorkoutRestTimer(
        sessionId: Long,
        endsAt: Long?,
        totalSeconds: Int,
        updatedAt: Long,
    )

    @Query("UPDATE active_workout_sessions SET updatedAt = :updatedAt WHERE sessionId = :sessionId")
    suspend fun updateActiveWorkoutSessionUpdatedAt(sessionId: Long, updatedAt: Long)

    @Transaction
    suspend fun updateActiveWorkoutDraft(
        sessionId: Long,
        draft: ActiveWorkoutDraftEntity,
        updatedAt: Long,
    ) {
        insertActiveWorkoutDrafts(listOf(draft))
        updateActiveWorkoutSessionUpdatedAt(sessionId = sessionId, updatedAt = updatedAt)
    }

    @Query("SELECT * FROM active_workout_drafts ORDER BY session_id ASC, exercise_id ASC")
    fun observeActiveWorkoutDrafts(): Flow<List<ActiveWorkoutDraftEntity>>

    @Query("SELECT * FROM active_workout_collapsed_exercises ORDER BY session_id ASC, exercise_id ASC")
    fun observeActiveWorkoutCollapsedExercises(): Flow<List<ActiveWorkoutCollapsedExerciseEntity>>

    @Query("SELECT * FROM active_workout_sets ORDER BY session_id ASC, order_index ASC, id ASC")
    fun observeActiveWorkoutSets(): Flow<List<ActiveWorkoutSetEntity>>

    @Query("SELECT * FROM workout_log_events ORDER BY created_at ASC")
    fun observeWorkoutLogEvents(): Flow<List<WorkoutLogEventEntity>>

    @Query("DELETE FROM active_workout_collapsed_exercises WHERE session_id = :sessionId AND exercise_id = :exerciseId")
    suspend fun deleteActiveWorkoutCollapsedExercise(sessionId: Long, exerciseId: Long)

    @Transaction
    suspend fun logActiveWorkoutSet(
        session: ActiveWorkoutSessionEntity,
        draft: ActiveWorkoutDraftEntity,
        set: ActiveWorkoutSetEntity,
        event: WorkoutLogEventEntity,
        eventSets: List<WorkoutLogEventSetEntity>,
        activeKey: Long,
    ) {
        insertActiveWorkoutSessions(listOf(session))
        insertActiveWorkoutDrafts(listOf(draft))
        deleteActiveWorkoutCollapsedExercise(sessionId = session.sessionId, exerciseId = activeKey)
        insertActiveWorkoutSets(listOf(set))
        insertWorkoutLogEvents(listOf(event))
        insertWorkoutLogEventSets(eventSets)
    }

    @Query(
        """
        UPDATE workout_log_event_sets
        SET exercise_id = :exerciseId,
            performed_exercise_id = :performedExerciseId,
            source_workout_exercise_id = :sourceWorkoutExerciseId,
            weight = :weight,
            reps = :reps,
            rpe = :rpe,
            reps_in_reserve = :repsInReserve,
            set_type = :setType,
            rest_seconds = :restSeconds,
            order_index = :orderIndex,
            completed = :completed,
            logged_at = :loggedAt
        WHERE id = :setId AND snapshot_role = 'CURRENT'
        """,
    )
    suspend fun updateWorkoutLogCurrentSetSnapshot(
        setId: Long,
        exerciseId: Long,
        performedExerciseId: Long,
        sourceWorkoutExerciseId: Long?,
        weight: Double,
        reps: Int,
        rpe: Double,
        repsInReserve: Int?,
        setType: String,
        restSeconds: Int,
        orderIndex: Int,
        completed: Boolean,
        loggedAt: Long,
    )

    @Transaction
    suspend fun updateActiveWorkoutSet(
        sessionId: Long,
        draft: ActiveWorkoutDraftEntity,
        set: ActiveWorkoutSetEntity,
        restTimerEndsAt: Long?,
        restTimerTotalSeconds: Int,
        updatedAt: Long,
    ) {
        insertActiveWorkoutDrafts(listOf(draft))
        insertActiveWorkoutSets(listOf(set))
        updateWorkoutLogCurrentSetSnapshot(
            setId = set.id,
            exerciseId = set.exerciseId,
            performedExerciseId = set.performedExerciseId,
            sourceWorkoutExerciseId = set.sourceWorkoutExerciseId,
            weight = set.weight,
            reps = set.reps,
            rpe = set.rpe,
            repsInReserve = set.repsInReserve,
            setType = set.setType,
            restSeconds = set.restSeconds,
            orderIndex = set.orderIndex,
            completed = set.completed,
            loggedAt = set.loggedAt,
        )
        updateActiveWorkoutRestTimer(
            sessionId = sessionId,
            endsAt = restTimerEndsAt,
            totalSeconds = restTimerTotalSeconds,
            updatedAt = updatedAt,
        )
    }

    @Query("SELECT * FROM workout_log_event_sets ORDER BY event_id ASC, snapshot_role ASC, snapshot_index ASC")
    fun observeWorkoutLogEventSets(): Flow<List<WorkoutLogEventSetEntity>>

    @Query("DELETE FROM workout_log_event_sets WHERE event_id IN (SELECT id FROM workout_log_events WHERE session_id = :sessionId)")
    suspend fun deleteWorkoutLogEventSetsForSession(sessionId: Long)

    @Query("DELETE FROM workout_log_events WHERE session_id = :sessionId")
    suspend fun deleteWorkoutLogEventsForSession(sessionId: Long)

    @Query("DELETE FROM active_workout_sessions WHERE sessionId = :sessionId")
    suspend fun deleteActiveWorkoutSession(sessionId: Long)

    @Query("DELETE FROM performed_exercises WHERE session_id = :sessionId")
    suspend fun deletePerformedExercisesForSession(sessionId: Long)

    @Query("DELETE FROM workout_sessions WHERE id = :sessionId AND completed = 0 AND status = 'DRAFT'")
    suspend fun deleteDraftWorkoutSession(sessionId: Long)

    @Transaction
    suspend fun discardActiveWorkoutSession(sessionId: Long) {
        deleteWorkoutLogEventSetsForSession(sessionId)
        deleteWorkoutLogEventsForSession(sessionId)
        deleteActiveWorkoutSession(sessionId)
        deletePerformedExercisesForSession(sessionId)
        deleteDraftWorkoutSession(sessionId)
    }

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
