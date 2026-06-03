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
    fun legacyRoomPreflightCannotCrashStartupLoadJob() {
        val localStore = File(mainSources, "data/local/TrainIqLocalStore.kt").readText()
        val loadJobBody = localStore.substringAfter("private val loadJob = scope.launch {")
            .substringBefore("private val legacyState")

        assertTrue(loadJobBody.contains("loadState()"))
        assertTrue(loadJobBody.contains("runCatching"))
        assertTrue(loadJobBody.contains("updateRoomPreflightStatus("))
    }

    @Test
    fun runtimeStoreDoesNotExposeBroadFullStateUpdateApi() {
        val runtimeStore = File(mainSources, "data/repository/RoomTrainIqRuntimeStore.kt").readText()
        val legacySeedBody = runtimeStore.substringAfter("private suspend fun seedRoomFromLegacyJsonIfNeeded()")
            .substringBefore("private data class CorePlanTables")
        val initBody = runtimeStore.substringAfter("init {")
            .substringBefore("val state:")

        assertFalse(runtimeStore.contains("suspend fun update(transform:"))
        assertFalse(runtimeStore.contains("RoomMirrorImportRun"))
        assertTrue(initBody.contains("runCatching"))
        assertTrue(legacySeedBody.contains("legacyStore.exportLegacyState()"))
        assertTrue(legacySeedBody.contains("sink.importTransaction(planner.plan(gson.toJson(legacyState)))"))
    }

    @Test
    fun workoutCompletionPersistsLocalDebriefBeforeAsyncGeminiRefresh() {
        val repository = File(mainSources, "data/repository/TrainIqRepository.kt").readText()
        val finishBody = repository.substringAfter("private suspend fun finishWorkout(")
            .substringBefore("suspend fun createRoutine(")
        val localDebriefIndex = finishBody.indexOf("fallbackWorkoutDebriefResult")
        val asyncRefreshIndex = finishBody.indexOf("scope.launch")
        val runCatchingIndex = finishBody.indexOf("runCatching", asyncRefreshIndex)
        val geminiDebriefIndex = finishBody.indexOf("workoutDebriefService.generateWorkoutDebrief")
        val returnIndex = finishBody.lastIndexOf("return WorkoutCompletionResult")

        assertTrue(localDebriefIndex >= 0)
        assertTrue(asyncRefreshIndex > localDebriefIndex)
        assertTrue(runCatchingIndex > asyncRefreshIndex)
        assertTrue(geminiDebriefIndex > runCatchingIndex)
        assertTrue(returnIndex > localDebriefIndex)
    }

    @Test
    fun delayedExerciseLibrarySeedingCannotCrashRepositoryScope() {
        val repository = File(mainSources, "data/repository/TrainIqRepository.kt").readText()
        val initBody = repository.substringAfter("init {")
            .substringBefore("fun observeDashboard()")

        assertTrue(initBody.contains("delay(3_000L)"))
        assertTrue(initBody.contains("runCatching"))
        assertTrue(initBody.contains("exerciseLibrarySeeder.ensureSeeded()"))
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
    fun activeWorkoutStartUsesTargetedRoomWrite() {
        val repository = File(mainSources, "data/repository/TrainIqRepository.kt").readText()
        val startBody = repository.substringAfter("suspend fun getOrStartActiveWorkoutSession(")
            .substringBefore("suspend fun updateActiveWorkoutDraft(")
        val runtimeStore = File(mainSources, "data/repository/RoomTrainIqRuntimeStore.kt").readText()
        val dao = File(mainSources, "core/database/TrainIqDao.kt").readText()

        assertTrue(startBody.contains("runtimeStore.startOrResumeActiveWorkoutSession("))
        assertFalse(startBody.contains("runtimeStore.update {"))
        assertTrue(runtimeStore.contains("suspend fun startOrResumeActiveWorkoutSession("))
        assertTrue(dao.contains("suspend fun startOrResumeActiveWorkoutSession("))
        assertTrue(dao.contains("insertActiveWorkoutSessions(listOf(activeSession))"))
        assertTrue(dao.contains("insertActiveWorkoutDrafts(drafts)"))
        assertTrue(dao.contains("insertPerformedExercises(performedExercises)"))
        assertTrue(dao.contains("importWorkoutSessions(listOf(draftSession))"))
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
    fun routineSetAddDeleteMoveUseTargetedRoomWrites() {
        val repository = File(mainSources, "data/repository/TrainIqRepository.kt").readText()
        val routineSetAddBody = repository.substringAfter("suspend fun addSetToExercise(")
            .substringBefore("suspend fun updateRoutineSet(")
        val routineSetDeleteMoveBody = repository.substringAfter("suspend fun deleteRoutineSet(")
            .substringBefore("suspend fun addWorkoutDay(")
        val runtimeStore = File(mainSources, "data/repository/RoomTrainIqRuntimeStore.kt").readText()
        val dao = File(mainSources, "core/database/TrainIqDao.kt").readText()

        assertTrue(routineSetAddBody.contains("replaceTargetedRoutineSetsForExercise("))
        assertTrue(routineSetDeleteMoveBody.contains("replaceTargetedRoutineSetsForExercise("))
        assertFalse(routineSetAddBody.contains("runtimeStore.update {"))
        assertFalse(routineSetDeleteMoveBody.contains("runtimeStore.update {"))
        assertTrue(runtimeStore.contains("suspend fun replaceRoutineSetsForExercise("))
        assertTrue(dao.contains("suspend fun replaceRoutineSetsForExercise("))
        assertTrue(dao.contains("deleteRoutineSetsForExercise(workoutExerciseId)"))
        assertTrue(dao.contains("insertRoutineSets(sets)"))
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
    fun activeRoutineSelectionUsesTargetedRoomWrite() {
        val repository = File(mainSources, "data/repository/TrainIqRepository.kt").readText()
        val activeRoutineBody = repository.substringAfter("suspend fun setActiveRoutine(")
            .substringBefore("suspend fun finishWorkout(")
        val runtimeStore = File(mainSources, "data/repository/RoomTrainIqRuntimeStore.kt").readText()
        val dao = File(mainSources, "core/database/TrainIqDao.kt").readText()

        assertTrue(activeRoutineBody.contains("runtimeStore.setActiveRoutine(routineId)"))
        assertFalse(activeRoutineBody.contains("runtimeStore.update {"))
        assertTrue(runtimeStore.contains("suspend fun setActiveRoutine(routineId: Long)"))
        assertTrue(dao.contains("UPDATE workout_routines SET active = CASE WHEN id = :routineId THEN 1 ELSE 0 END"))
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
    fun supersetGroupUsesTargetedRoomWrite() {
        val repository = File(mainSources, "data/repository/TrainIqRepository.kt").readText()
        val supersetBody = repository.substringAfter("suspend fun setSupersetGroup(")
            .substringBefore("suspend fun replaceExerciseInPlan(")
        val runtimeStore = File(mainSources, "data/repository/RoomTrainIqRuntimeStore.kt").readText()
        val dao = File(mainSources, "core/database/TrainIqDao.kt").readText()

        assertTrue(supersetBody.contains("runtimeStore.setSupersetGroup("))
        assertFalse(supersetBody.contains("runtimeStore.update {"))
        assertTrue(runtimeStore.contains("suspend fun setSupersetGroup("))
        assertTrue(dao.contains("UPDATE workout_exercises SET superset_group_id = :groupId WHERE id IN (:workoutExerciseIds)"))
    }

    @Test
    fun workoutExercisePlanUpdateUsesTargetedRoomWrite() {
        val repository = File(mainSources, "data/repository/TrainIqRepository.kt").readText()
        val planBody = repository.substringAfter("suspend fun updateWorkoutExercisePlan(")
            .substringBefore("suspend fun addSetToExercise(")
        val runtimeStore = File(mainSources, "data/repository/RoomTrainIqRuntimeStore.kt").readText()
        val dao = File(mainSources, "core/database/TrainIqDao.kt").readText()

        assertTrue(planBody.contains("runtimeStore.replaceRoutineSetsForExercise("))
        assertFalse(planBody.contains("runtimeStore.update {"))
        assertTrue(runtimeStore.contains("suspend fun replaceRoutineSetsForExercise("))
        assertTrue(dao.contains("suspend fun replaceRoutineSetsForExercise("))
        assertTrue(dao.contains("deleteRoutineSetsForExercise(workoutExerciseId)"))
        assertTrue(dao.contains("insertRoutineSets(sets)"))
        assertTrue(dao.contains("insertWorkoutExercise(workoutExercise)"))
    }

    @Test
    fun replaceExerciseInPlanUsesTargetedRoomWrite() {
        val repository = File(mainSources, "data/repository/TrainIqRepository.kt").readText()
        val replaceBody = repository.substringAfter("suspend fun replaceExerciseInPlan(")
            .substringBefore("suspend fun replaceExerciseInActiveWorkout(")
        val runtimeStore = File(mainSources, "data/repository/RoomTrainIqRuntimeStore.kt").readText()
        val dao = File(mainSources, "core/database/TrainIqDao.kt").readText()

        assertTrue(replaceBody.contains("runtimeStore.saveWorkoutExercise("))
        assertFalse(replaceBody.contains("runtimeStore.update {"))
        assertTrue(runtimeStore.contains("suspend fun saveWorkoutExercise("))
        assertTrue(dao.contains("suspend fun insertWorkoutExercise(exercise: WorkoutExerciseEntity)"))
    }

    @Test
    fun replaceExerciseInActiveWorkoutUsesTargetedRoomWrite() {
        val repository = File(mainSources, "data/repository/TrainIqRepository.kt").readText()
        val replaceBody = repository.substringAfter("suspend fun replaceExerciseInActiveWorkout(")
            .substringBefore("suspend fun updateWorkoutExercisePlan(")
        val runtimeStore = File(mainSources, "data/repository/RoomTrainIqRuntimeStore.kt").readText()
        val dao = File(mainSources, "core/database/TrainIqDao.kt").readText()

        assertTrue(replaceBody.contains("runtimeStore.replaceWorkoutExerciseInActiveWorkout("))
        assertFalse(replaceBody.contains("runtimeStore.update {"))
        assertTrue(runtimeStore.contains("suspend fun replaceWorkoutExerciseInActiveWorkout("))
        assertTrue(dao.contains("suspend fun replaceWorkoutExerciseInActiveWorkout("))
        assertTrue(dao.contains("insertWorkoutExercise(workoutExercise)"))
        assertTrue(dao.contains("updateActiveWorkoutSessionUpdatedAt(sessionId = activeSessionId, updatedAt = updatedAt)"))
    }

    @Test
    fun workoutDayAddRemoveUseTargetedRoomWrites() {
        val repository = File(mainSources, "data/repository/TrainIqRepository.kt").readText()
        val dayBody = repository.substringAfter("suspend fun addWorkoutDay(")
            .substringBefore("suspend fun addExerciseToDay(")
        val runtimeStore = File(mainSources, "data/repository/RoomTrainIqRuntimeStore.kt").readText()
        val dao = File(mainSources, "core/database/TrainIqDao.kt").readText()

        assertTrue(dayBody.contains("runtimeStore.addWorkoutDay("))
        assertTrue(dayBody.contains("runtimeStore.removeWorkoutDay("))
        assertFalse(dayBody.contains("runtimeStore.update {"))
        assertTrue(runtimeStore.contains("suspend fun addWorkoutDay(day: WorkoutDayEntity)"))
        assertTrue(runtimeStore.contains("suspend fun removeWorkoutDay(dayId: Long)"))
        assertTrue(dao.contains("suspend fun insertWorkoutDay(day: WorkoutDayEntity)"))
        assertTrue(dao.contains("suspend fun deleteWorkoutDayCascade(dayId: Long)"))
    }

    @Test
    fun workoutExerciseAddRemoveUseTargetedRoomWrites() {
        val repository = File(mainSources, "data/repository/TrainIqRepository.kt").readText()
        val addBody = repository.substringAfter("suspend fun addExerciseToDay(")
            .substringBefore("suspend fun addExerciseToRoutine(")
        val addRoutineBody = repository.substringAfter("suspend fun addExerciseToRoutine(")
            .substringBefore("suspend fun removeExerciseFromDay(")
        val removeBody = repository.substringAfter("suspend fun removeExerciseFromDay(")
            .substringBefore("suspend fun deleteWorkoutSession(")
        val runtimeStore = File(mainSources, "data/repository/RoomTrainIqRuntimeStore.kt").readText()
        val dao = File(mainSources, "core/database/TrainIqDao.kt").readText()

        assertTrue(addBody.contains("runtimeStore.addWorkoutExerciseToDay("))
        assertTrue(addRoutineBody.contains("runtimeStore.addWorkoutExerciseToDay("))
        assertTrue(removeBody.contains("runtimeStore.removeWorkoutExerciseFromDay("))
        assertFalse(addBody.contains("runtimeStore.update {"))
        assertFalse(addRoutineBody.contains("runtimeStore.update {"))
        assertFalse(removeBody.contains("runtimeStore.update {"))
        assertTrue(runtimeStore.contains("suspend fun addWorkoutExerciseToDay("))
        assertTrue(runtimeStore.contains("suspend fun removeWorkoutExerciseFromDay("))
        assertTrue(dao.contains("suspend fun addWorkoutExerciseToDay("))
        assertTrue(dao.contains("insertWorkoutDay(day)"))
        assertTrue(dao.contains("suspend fun deleteWorkoutExerciseCascade("))
        assertTrue(dao.contains("deleteRoutineSetsForExercise(workoutExerciseId)"))
        assertTrue(dao.contains("deleteWorkoutExercise(workoutExerciseId)"))
    }

    @Test
    fun workoutSessionDeleteUsesTargetedRoomWrites() {
        val repository = File(mainSources, "data/repository/TrainIqRepository.kt").readText()
        val deleteBody = repository.substringAfter("suspend fun deleteWorkoutSession(")
            .substringBefore("suspend fun generateAiRoutine(")
        val runtimeStore = File(mainSources, "data/repository/RoomTrainIqRuntimeStore.kt").readText()
        val dao = File(mainSources, "core/database/TrainIqDao.kt").readText()

        assertTrue(deleteBody.contains("runtimeStore.deleteWorkoutSession(sessionId)"))
        assertFalse(deleteBody.contains("runtimeStore.update {"))
        assertTrue(runtimeStore.contains("suspend fun deleteWorkoutSession(sessionId: Long)"))
        assertTrue(dao.contains("suspend fun deleteWorkoutSessionCascade(sessionId: Long)"))
        assertTrue(dao.contains("deleteWorkoutLogEventSetsForSession(sessionId)"))
        assertTrue(dao.contains("deleteWorkoutSetsForSession(sessionId)"))
        assertTrue(dao.contains("deletePerformedExercisesForSession(sessionId)"))
        assertTrue(dao.contains("deleteWorkoutSessionById(sessionId)"))
    }

    @Test
    fun generatedRoutineSaveUsesTargetedRoomWrites() {
        val repository = File(mainSources, "data/repository/TrainIqRepository.kt").readText()
        val saveBody = repository.substringAfter("suspend fun saveGeneratedRoutine(")
            .substringBefore("fun observeNutritionOverview(")
        val runtimeStore = File(mainSources, "data/repository/RoomTrainIqRuntimeStore.kt").readText()
        val dao = File(mainSources, "core/database/TrainIqDao.kt").readText()

        assertTrue(saveBody.contains("runtimeStore.saveGeneratedRoutine("))
        assertFalse(saveBody.contains("runtimeStore.update {"))
        assertTrue(runtimeStore.contains("suspend fun saveGeneratedRoutine("))
        assertTrue(dao.contains("suspend fun insertGeneratedRoutineGraph("))
        assertTrue(dao.contains("insertRoutines(listOf(routine))"))
        assertTrue(dao.contains("insertWorkoutDays(days)"))
        assertTrue(dao.contains("insertWorkoutExercises(workoutExercises)"))
        assertTrue(dao.contains("insertRoutineSets(sets)"))
    }

    @Test
    fun foodAndRecipeMutationsUseTargetedRoomWrites() {
        val repository = File(mainSources, "data/repository/TrainIqRepository.kt").readText()
        val nutritionBody = repository.substringAfter("suspend fun saveFoodItem(")
            .substringBefore("fun observeProgressOverview(")
        val runtimeStore = File(mainSources, "data/repository/RoomTrainIqRuntimeStore.kt").readText()
        val dao = File(mainSources, "core/database/TrainIqDao.kt").readText()

        assertTrue(nutritionBody.contains("runtimeStore.saveFood("))
        assertTrue(nutritionBody.contains("runtimeStore.saveRecipe("))
        assertTrue(nutritionBody.contains("runtimeStore.deleteFood(foodId)"))
        assertTrue(nutritionBody.contains("runtimeStore.deleteRecipe(recipeId)"))
        assertFalse(nutritionBody.contains("runtimeStore.update {"))
        assertTrue(runtimeStore.contains("suspend fun saveFood(food: FoodItemStorage)"))
        assertTrue(runtimeStore.contains("suspend fun saveRecipe(recipe: RecipeStorage"))
        assertTrue(dao.contains("suspend fun deleteFoodItem(foodId: Long)"))
        assertTrue(dao.contains("suspend fun saveRecipe(recipe: RecipeEntity"))
        assertTrue(dao.contains("suspend fun deleteRecipeWithIngredients(recipeId: Long)"))
    }

    @Test
    fun exerciseLibrarySeedingUsesTargetedRoomInsert() {
        val seeder = File(mainSources, "data/repository/ExerciseLibrarySeeder.kt").readText()
        val ensureSeededBody = seeder.substringAfter("suspend fun ensureSeeded()")
            .substringBefore("internal fun shouldSkipExerciseLibrarySeed")
        val runtimeStore = File(mainSources, "data/repository/RoomTrainIqRuntimeStore.kt").readText()
        val dao = File(mainSources, "core/database/TrainIqDao.kt").readText()

        assertTrue(ensureSeededBody.contains("missingCanonicalExercises(runtimeStore.state.value)"))
        assertTrue(ensureSeededBody.contains("runtimeStore.seedExerciseLibrary(additions)"))
        assertFalse(ensureSeededBody.contains("runtimeStore.update"))
        assertTrue(runtimeStore.contains("suspend fun seedExerciseLibrary(exercises: List<ExerciseEntity>)"))
        assertTrue(runtimeStore.contains("dao.insertExercises(exercises)"))
        assertTrue(dao.contains("suspend fun insertExercises(exercises: List<ExerciseEntity>)"))
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
