package com.trainiq.architecture

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RoomAuthorityArchitectureTest {
    private val projectRoot = File(System.getProperty("user.dir") ?: ".")
    private val mainSources = File(projectRoot, "src/main/java/com/trainiq")

    @Test
    fun trainIqRepositoryDoesNotDependOnLegacyLocalStore() {
        val repository = File(mainSources, "data/repository/TrainIqRepository.kt").readText()

        assertFalse(repository.contains("TrainIqLocalStore"))
        assertFalse(repository.contains("localStore.state"))
        assertFalse(repository.contains("localStore.update"))
        assertTrue(repository.contains("RoomTrainIqRuntimeStore"))
    }

    @Test
    fun settingsViewModelUsesUseCasesForProfileResetAndDataClear() {
        val settings = File(mainSources, "features/settings/SettingsSection.kt").readText()
        val viewModelConstructor = settings.substringAfter("class SettingsViewModel @Inject constructor(")
            .substringBefore(") : ViewModel()")

        assertFalse(viewModelConstructor.contains("TrainIqLocalStore"))
        assertTrue(viewModelConstructor.contains("ResetProfileUseCase"))
        assertTrue(viewModelConstructor.contains("ClearAppDataUseCase"))
        assertTrue(settings.contains("resetProfileUseCase()"))
        assertTrue(settings.contains("clearAppDataUseCase()"))
    }

    @Test
    fun legacyLocalStoreDoesNotExposeRuntimeStateFlow() {
        val localStore = File(mainSources, "data/local/TrainIqLocalStore.kt").readText()

        assertFalse(localStore.contains("val state: StateFlow<TrainIqStorageState>"))
        assertFalse(localStore.contains("val state ="))
    }

    @Test
    fun runtimeStoreUpdatesUseFullReplacementSemanticsInsteadOfUpsertOnlyMirrorWrites() {
        val runtimeStore = File(mainSources, "data/repository/RoomTrainIqRuntimeStore.kt").readText()
        val updateBody = runtimeStore.substringAfter("suspend fun update(transform:")
            .substringBefore("suspend fun clearAll()")

        assertTrue(updateBody.contains("mirrorRun ="))
        assertFalse(updateBody.contains("sink.importTransaction(planner.plan(gson.toJson(updated)))"))
    }

    @Test
    fun workoutCompletionPersistsLocalDebriefBeforeAsyncGeminiRefresh() {
        val repository = File(mainSources, "data/repository/TrainIqRepository.kt").readText()
        val finishBody = repository.substringAfter("private suspend fun finishWorkout(")
            .substringBefore("suspend fun createRoutine(")
        val localDebriefIndex = finishBody.indexOf("fallbackWorkoutDebriefResult")
        val asyncRefreshIndex = finishBody.indexOf("scope.launch")
        val geminiDebriefIndex = finishBody.indexOf("workoutDebriefService.generateWorkoutDebrief")
        val returnIndex = finishBody.lastIndexOf("return WorkoutCompletionResult")

        assertTrue(localDebriefIndex >= 0)
        assertTrue(asyncRefreshIndex > localDebriefIndex)
        assertTrue(geminiDebriefIndex > asyncRefreshIndex)
        assertTrue(returnIndex > localDebriefIndex)
    }

    @Test
    fun activeWorkoutRestTimerUsesTargetedRoomUpdate() {
        val repository = File(mainSources, "data/repository/TrainIqRepository.kt").readText()
        val restTimerBody = repository.substringAfter("suspend fun updateActiveWorkoutRestTimer(")
            .substringBefore("suspend fun finishActiveWorkout(")
        val runtimeStore = File(mainSources, "data/repository/RoomTrainIqRuntimeStore.kt").readText()
        val dao = File(mainSources, "core/database/TrainIqDao.kt").readText()

        assertTrue(restTimerBody.contains("runtimeStore.updateActiveWorkoutRestTimer("))
        assertFalse(restTimerBody.contains("runtimeStore.update {"))
        assertTrue(runtimeStore.contains("suspend fun updateActiveWorkoutRestTimer("))
        assertTrue(dao.contains("UPDATE active_workout_sessions"))
    }

    @Test
    fun activeWorkoutDraftUsesTargetedRoomUpdate() {
        val repository = File(mainSources, "data/repository/TrainIqRepository.kt").readText()
        val draftBody = repository.substringAfter("suspend fun updateActiveWorkoutDraft(")
            .substringBefore("suspend fun logActiveWorkoutSet(")
        val runtimeStore = File(mainSources, "data/repository/RoomTrainIqRuntimeStore.kt").readText()
        val dao = File(mainSources, "core/database/TrainIqDao.kt").readText()

        assertTrue(draftBody.contains("runtimeStore.updateActiveWorkoutDraft("))
        assertFalse(draftBody.contains("mutateActiveWorkout"))
        assertFalse(draftBody.contains("runtimeStore.update {"))
        assertTrue(runtimeStore.contains("suspend fun updateActiveWorkoutDraft("))
        assertTrue(dao.contains("suspend fun updateActiveWorkoutDraft("))
        assertTrue(dao.contains("insertActiveWorkoutDrafts(listOf(draft))"))
    }

    @Test
    fun activeWorkoutSetLoggingUsesTargetedRoomWrite() {
        val repository = File(mainSources, "data/repository/TrainIqRepository.kt").readText()
        val logSetBody = repository.substringAfter("suspend fun logActiveWorkoutSet(")
            .substringBefore("suspend fun updateActiveWorkoutSet(")
        val runtimeStore = File(mainSources, "data/repository/RoomTrainIqRuntimeStore.kt").readText()
        val dao = File(mainSources, "core/database/TrainIqDao.kt").readText()

        assertTrue(logSetBody.contains("runtimeStore.logActiveWorkoutSet("))
        assertFalse(logSetBody.contains("runtimeStore.update {"))
        assertTrue(runtimeStore.contains("suspend fun logActiveWorkoutSet("))
        assertTrue(dao.contains("suspend fun logActiveWorkoutSet("))
        assertTrue(dao.contains("insertActiveWorkoutSets(listOf(set))"))
        assertTrue(dao.contains("insertWorkoutLogEvents(listOf(event))"))
        assertTrue(dao.contains("insertWorkoutLogEventSets(eventSets)"))
    }

    @Test
    fun activeWorkoutSetEditingUsesTargetedRoomWrite() {
        val repository = File(mainSources, "data/repository/TrainIqRepository.kt").readText()
        val updateSetBody = repository.substringAfter("suspend fun updateActiveWorkoutSet(")
            .substringBefore("suspend fun updateActiveWorkoutSetType(")
        val runtimeStore = File(mainSources, "data/repository/RoomTrainIqRuntimeStore.kt").readText()
        val dao = File(mainSources, "core/database/TrainIqDao.kt").readText()

        assertTrue(updateSetBody.contains("runtimeStore.updateActiveWorkoutSet("))
        assertFalse(updateSetBody.contains("runtimeStore.update {"))
        assertTrue(runtimeStore.contains("suspend fun updateActiveWorkoutSet("))
        assertTrue(dao.contains("suspend fun updateActiveWorkoutSet("))
        assertTrue(dao.contains("insertActiveWorkoutSets(listOf(set))"))
        assertTrue(dao.contains("updateWorkoutLogCurrentSetSnapshot("))
    }

    @Test
    fun activeWorkoutCollapseUsesTargetedRoomWrite() {
        val repository = File(mainSources, "data/repository/TrainIqRepository.kt").readText()
        val collapseBody = repository.substringAfter("suspend fun setActiveWorkoutCollapsed(")
            .substringBefore("suspend fun updateActiveWorkoutRestTimer(")
        val runtimeStore = File(mainSources, "data/repository/RoomTrainIqRuntimeStore.kt").readText()
        val dao = File(mainSources, "core/database/TrainIqDao.kt").readText()

        assertTrue(collapseBody.contains("runtimeStore.setActiveWorkoutCollapsedExercise("))
        assertFalse(collapseBody.contains("mutateActiveWorkout"))
        assertFalse(collapseBody.contains("runtimeStore.update {"))
        assertTrue(runtimeStore.contains("suspend fun setActiveWorkoutCollapsedExercise("))
        assertTrue(dao.contains("suspend fun setActiveWorkoutCollapsedExercise("))
        assertTrue(dao.contains("insertActiveWorkoutCollapsedExercises(listOf(collapsedExercise))"))
        assertTrue(dao.contains("deleteActiveWorkoutCollapsedExercise("))
    }

    @Test
    fun activeWorkoutSetTypeEditingUsesTargetedRoomWrite() {
        val repository = File(mainSources, "data/repository/TrainIqRepository.kt").readText()
        val setTypeBody = repository.substringAfter("suspend fun updateActiveWorkoutSetType(")
            .substringBefore("suspend fun deleteActiveWorkoutSet(")
        val runtimeStore = File(mainSources, "data/repository/RoomTrainIqRuntimeStore.kt").readText()
        val dao = File(mainSources, "core/database/TrainIqDao.kt").readText()

        assertTrue(setTypeBody.contains("runtimeStore.updateActiveWorkoutSetType("))
        assertFalse(setTypeBody.contains("mutateActiveWorkout"))
        assertFalse(setTypeBody.contains("runtimeStore.update {"))
        assertTrue(runtimeStore.contains("suspend fun updateActiveWorkoutSetType("))
        assertTrue(dao.contains("suspend fun updateActiveWorkoutSetType("))
        assertTrue(dao.contains("UPDATE active_workout_sets SET set_type = :setType"))
        assertTrue(dao.contains("updateWorkoutLogCurrentSetSnapshotType("))
    }

    @Test
    fun activeWorkoutSetDeleteUsesTargetedRoomWrite() {
        val repository = File(mainSources, "data/repository/TrainIqRepository.kt").readText()
        val deleteSetBody = repository.substringAfter("suspend fun deleteActiveWorkoutSet(")
            .substringBefore("suspend fun undoWorkoutLogEvent(")
        val runtimeStore = File(mainSources, "data/repository/RoomTrainIqRuntimeStore.kt").readText()
        val dao = File(mainSources, "core/database/TrainIqDao.kt").readText()

        assertTrue(deleteSetBody.contains("runtimeStore.deleteActiveWorkoutSet("))
        assertFalse(deleteSetBody.contains("mutateActiveWorkout"))
        assertFalse(deleteSetBody.contains("runtimeStore.update {"))
        assertTrue(runtimeStore.contains("suspend fun deleteActiveWorkoutSet("))
        assertTrue(dao.contains("suspend fun deleteActiveWorkoutSet("))
        assertTrue(dao.contains("DELETE FROM active_workout_sets WHERE session_id = :sessionId AND id = :setId"))
        assertTrue(dao.contains("deletePendingAddSetWorkoutLogEvent("))
    }

    @Test
    fun activeWorkoutUndoUsesTargetedRoomWrite() {
        val repository = File(mainSources, "data/repository/TrainIqRepository.kt").readText()
        val undoBody = repository.substringAfter("suspend fun undoWorkoutLogEvent(")
            .substringBefore("suspend fun setActiveWorkoutCollapsed(")
        val runtimeStore = File(mainSources, "data/repository/RoomTrainIqRuntimeStore.kt").readText()
        val dao = File(mainSources, "core/database/TrainIqDao.kt").readText()

        assertTrue(undoBody.contains("runtimeStore.undoActiveWorkoutLogEvent("))
        assertFalse(undoBody.contains("runtimeStore.update {"))
        assertTrue(runtimeStore.contains("suspend fun undoActiveWorkoutLogEvent("))
        assertTrue(dao.contains("suspend fun undoActiveWorkoutLogEvent("))
        assertTrue(dao.contains("deleteActiveWorkoutSetsForSession(sessionId)"))
        assertTrue(dao.contains("insertWorkoutLogEvents(listOf(undoEvent))"))
        assertTrue(dao.contains("insertWorkoutLogEventSets(undoEventSets)"))
    }

    @Test
    fun activeWorkoutDiscardUsesTargetedRoomDelete() {
        val repository = File(mainSources, "data/repository/TrainIqRepository.kt").readText()
        val discardBody = repository.substringAfter("suspend fun discardActiveWorkout(")
            .substringBefore("suspend fun setActiveRoutine(")
        val runtimeStore = File(mainSources, "data/repository/RoomTrainIqRuntimeStore.kt").readText()
        val dao = File(mainSources, "core/database/TrainIqDao.kt").readText()

        assertTrue(discardBody.contains("runtimeStore.discardActiveWorkoutSession("))
        assertFalse(discardBody.contains("runtimeStore.update {"))
        assertTrue(runtimeStore.contains("suspend fun discardActiveWorkoutSession("))
        assertTrue(dao.contains("suspend fun discardActiveWorkoutSession("))
        assertTrue(dao.contains("deleteActiveWorkoutSession(sessionId)"))
        assertTrue(dao.contains("deleteDraftWorkoutSession(sessionId)"))
        assertTrue(dao.contains("deleteWorkoutLogEventsForSession(sessionId)"))
    }

    @Test
    fun activeWorkoutFinishUsesTargetedRoomWrite() {
        val repository = File(mainSources, "data/repository/TrainIqRepository.kt").readText()
        val activeFinishBody = repository.substringAfter("suspend fun finishActiveWorkout(")
            .substringBefore("suspend fun getWorkoutCompletionSummary(")
        val finishBody = repository.substringAfter("private suspend fun finishWorkout(")
            .substringBefore("suspend fun createRoutine(")
        val runtimeStore = File(mainSources, "data/repository/RoomTrainIqRuntimeStore.kt").readText()
        val dao = File(mainSources, "core/database/TrainIqDao.kt").readText()

        assertTrue(finishBody.contains("runtimeStore.finishActiveWorkoutSession("))
        assertTrue(finishBody.contains("runtimeStore.updateWorkoutSessionDebrief("))
        assertFalse(activeFinishBody.contains("runtimeStore.update {"))
        assertFalse(finishBody.contains("runtimeStore.update {"))
        assertTrue(runtimeStore.contains("suspend fun finishActiveWorkoutSession("))
        assertTrue(runtimeStore.contains("suspend fun updateWorkoutSessionDebrief("))
        assertTrue(dao.contains("suspend fun finishActiveWorkoutSession("))
        assertTrue(dao.contains("importWorkoutSessions(listOf(session))"))
        assertTrue(dao.contains("deleteActiveWorkoutSession(sessionId)"))
        assertTrue(dao.contains("suspend fun updateWorkoutSessionDebrief("))
    }

    @Test
    fun bodyMeasurementsUseTargetedRoomWrites() {
        val repository = File(mainSources, "data/repository/TrainIqRepository.kt").readText()
        val measurementBody = repository.substringAfter("suspend fun addMeasurement(")
            .substringBefore("fun observeCoachOverview()")
        val runtimeStore = File(mainSources, "data/repository/RoomTrainIqRuntimeStore.kt").readText()
        val dao = File(mainSources, "core/database/TrainIqDao.kt").readText()

        assertTrue(measurementBody.contains("runtimeStore.addMeasurement("))
        assertTrue(measurementBody.contains("runtimeStore.deleteMeasurement("))
        assertFalse(measurementBody.contains("runtimeStore.update {"))
        assertTrue(runtimeStore.contains("suspend fun addMeasurement("))
        assertTrue(runtimeStore.contains("suspend fun deleteMeasurement("))
        assertTrue(dao.contains("suspend fun insertMeasurement(measurement: BodyMeasurementEntity)"))
        assertTrue(dao.contains("DELETE FROM body_measurements WHERE id = :measurementId"))
    }

    @Test
    fun mealsUseTargetedRoomWrites() {
        val repository = File(mainSources, "data/repository/TrainIqRepository.kt").readText()
        val mealBody = repository.substringAfter("suspend fun saveMeal(")
            .substringBefore("suspend fun deleteFood(")
        val runtimeStore = File(mainSources, "data/repository/RoomTrainIqRuntimeStore.kt").readText()
        val dao = File(mainSources, "core/database/TrainIqDao.kt").readText()

        assertTrue(mealBody.contains("runtimeStore.saveMeal("))
        assertTrue(mealBody.contains("runtimeStore.deleteMeal("))
        assertFalse(mealBody.contains("runtimeStore.update {"))
        assertTrue(runtimeStore.contains("suspend fun saveMeal(meal: LoggedMealStorage"))
        assertTrue(runtimeStore.contains("suspend fun deleteMeal(mealId: Long)"))
        assertTrue(dao.contains("suspend fun saveMeal(meal: MealEntity, items: List<MealItemEntity>)"))
        assertTrue(dao.contains("DELETE FROM meal_items WHERE meal_id = :mealId"))
        assertTrue(dao.contains("DELETE FROM meals WHERE id = :mealId"))
    }

    @Test
    fun profileWritesUseTargetedRoomWrites() {
        val repository = File(mainSources, "data/repository/TrainIqRepository.kt").readText()
        val profileBody = repository.substringAfter("suspend fun saveProfile(")
            .substringBefore("private fun buildWeeklySummary(")
        val runtimeStore = File(mainSources, "data/repository/RoomTrainIqRuntimeStore.kt").readText()
        val clearProfileBody = runtimeStore.substringAfter("suspend fun clearProfile()")
            .substringBefore("suspend fun saveProfile(profile: UserProfileEntity)")
        val dao = File(mainSources, "core/database/TrainIqDao.kt").readText()

        assertTrue(profileBody.contains("runtimeStore.saveProfile("))
        assertFalse(profileBody.contains("runtimeStore.update {"))
        assertTrue(runtimeStore.contains("suspend fun saveProfile(profile: UserProfileEntity)"))
        assertTrue(clearProfileBody.contains("dao.clearMirrorUserProfile()"))
        assertFalse(clearProfileBody.contains("update {"))
        assertTrue(dao.contains("suspend fun upsertUserProfile(profile: UserProfileEntity)"))
        assertTrue(dao.contains("DELETE FROM user_profile"))
    }

    @Test
    fun routineSetEditingUsesTargetedRoomWrites() {
        val repository = File(mainSources, "data/repository/TrainIqRepository.kt").readText()
        val routineSetBody = repository.substringAfter("suspend fun updateRoutineSet(")
            .substringBefore("suspend fun deleteRoutineSet(")
        val runtimeStore = File(mainSources, "data/repository/RoomTrainIqRuntimeStore.kt").readText()
        val dao = File(mainSources, "core/database/TrainIqDao.kt").readText()

        assertTrue(routineSetBody.contains("updateTargetedRoutineSet("))
        assertTrue(routineSetBody.contains("runtimeStore.updateRoutineSet("))
        assertFalse(routineSetBody.contains("runtimeStore.update {"))
        assertTrue(runtimeStore.contains("suspend fun updateRoutineSet(set: RoutineSetEntity"))
        assertTrue(dao.contains("suspend fun updateRoutineSet(set: RoutineSetEntity, workoutExercise: WorkoutExerciseEntity)"))
        assertTrue(dao.contains("insertRoutineSets(listOf(set))"))
        assertTrue(dao.contains("insertWorkoutExercise(workoutExercise)"))
    }

    @Test
    fun routineCoreMutationsUseTargetedRoomWrites() {
        val repository = File(mainSources, "data/repository/TrainIqRepository.kt").readText()
        val routineBody = repository.substringAfter("suspend fun createRoutine(")
            .substringBefore("suspend fun searchExercises(")
        val runtimeStore = File(mainSources, "data/repository/RoomTrainIqRuntimeStore.kt").readText()
        val dao = File(mainSources, "core/database/TrainIqDao.kt").readText()

        assertTrue(routineBody.contains("runtimeStore.createRoutine("))
        assertTrue(routineBody.contains("runtimeStore.updateRoutine("))
        assertTrue(routineBody.contains("runtimeStore.deleteRoutine("))
        assertFalse(routineBody.contains("runtimeStore.update {"))
        assertTrue(runtimeStore.contains("suspend fun createRoutine("))
        assertTrue(runtimeStore.contains("suspend fun updateRoutine("))
        assertTrue(runtimeStore.contains("suspend fun deleteRoutine("))
        assertTrue(dao.contains("suspend fun insertRoutine(routine: WorkoutRoutineEntity)"))
        assertTrue(dao.contains("suspend fun deleteRoutineAndNormalizeActive("))
        assertTrue(dao.contains("setActiveRoutine(routine.id)"))
    }

    @Test
    fun routineExerciseReorderUsesTargetedRoomWrites() {
        val repository = File(mainSources, "data/repository/TrainIqRepository.kt").readText()
        val reorderBody = repository.substringAfter("suspend fun reorderExercises(")
            .substringBefore("suspend fun setSupersetGroup(")
        val runtimeStore = File(mainSources, "data/repository/RoomTrainIqRuntimeStore.kt").readText()
        val dao = File(mainSources, "core/database/TrainIqDao.kt").readText()

        assertTrue(reorderBody.contains("runtimeStore.reorderExercises("))
        assertFalse(reorderBody.contains("runtimeStore.update {"))
        assertTrue(runtimeStore.contains("suspend fun reorderExercises("))
        assertTrue(runtimeStore.contains("getWorkoutExercisesForDay(dayId)"))
        assertTrue(dao.contains("suspend fun reorderExercises(dayId: Long, orderedIds: List<Long>)"))
        assertTrue(dao.contains("updateWorkoutExerciseOrder(dayId, workoutExerciseId, index)"))
    }

    @Test
    fun roomImportReportsUseCurrentRoomSchemaVersion() {
        val database = File(mainSources, "core/database/TrainIqDatabase.kt").readText()
        val importPlanner = File(mainSources, "data/migration/JsonRoomImportPlanner.kt").readText()

        val roomVersion = database.substringAfter("version = ").substringBefore(",").trim()

        assertTrue(importPlanner.contains("private const val TrainIqDatabaseVersion = $roomVersion"))
    }

    @Test
    fun releaseShrinkingKeepsGsonRoomStateTypeMetadata() {
        val rules = File(projectRoot, "proguard-rules.pro").readText()

        assertTrue(rules.contains("-keepattributes Signature"))
        assertTrue(rules.contains("com.trainiq.data.local.**"))
        assertTrue(rules.contains("com.trainiq.core.database.**"))
    }
}
