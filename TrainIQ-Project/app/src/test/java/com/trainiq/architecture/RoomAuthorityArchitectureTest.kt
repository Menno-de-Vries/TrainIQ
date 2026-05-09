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
