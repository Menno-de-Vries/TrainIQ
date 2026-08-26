package com.trainiq.architecture

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DurableWorkoutDebriefArchitectureTest {
    private val mainSources = File("src/main/java/com/trainiq")

    @Test
    fun workoutCompletionSchedulesUniqueDurableDebriefWorkOutsideRepository() {
        val repository = File(mainSources, "data/repository/TrainIqRepository.kt").readText()
        val useCases = File(mainSources, "domain/usecase/UseCases.kt").readText()
        val workerFile = File(mainSources, "core/workout/WorkoutDebriefWorker.kt")
        assertTrue(workerFile.exists())
        val worker = workerFile.readText()

        val finishBody = repository.substringAfter("private suspend fun finishWorkout(")
            .substringBefore("suspend fun refreshWorkoutDebrief(")
        val finishUseCase = useCases.substringAfter("class FinishActiveWorkoutUseCase")
            .substringBefore("class GetWorkoutCompletionSummaryUseCase")

        assertFalse(finishBody.contains("scope.launch"))
        assertFalse(finishBody.contains("workoutDebriefService.generateWorkoutDebrief"))
        assertTrue(finishUseCase.contains("workoutDebriefScheduler.enqueue(result.sessionId)"))
        assertTrue(worker.contains("enqueueUniqueWork("))
        assertTrue(worker.contains("ExistingWorkPolicy.KEEP"))
        assertTrue(worker.contains("NetworkType.CONNECTED"))
        assertTrue(worker.contains("BackoffPolicy.EXPONENTIAL"))
    }

    @Test
    fun workoutDebriefWorkerIsRestartSafeIdempotentAndAttemptBounded() {
        val workerFile = File(mainSources, "core/workout/WorkoutDebriefWorker.kt")
        assertTrue(workerFile.exists())
        val worker = workerFile.readText()

        assertTrue(worker.contains("runAttemptCount >= WorkoutDebriefMaxAttempts - 1"))
        assertTrue(worker.contains("WorkoutDebriefRefreshOutcome.ALREADY_ENRICHED"))
        assertTrue(worker.contains("WorkoutDebriefRefreshOutcome.SESSION_MISSING"))
        assertTrue(worker.contains("WorkoutDebriefRefreshOutcome.UPDATED"))
        assertTrue(worker.contains("WorkoutDebriefRefreshOutcome.INVALID_RESULT"))
    }

    @Test
    fun workoutDebriefRefreshReadsRoomDirectlyAfterColdProcessStart() {
        val repository = File(mainSources, "data/repository/TrainIqRepository.kt").readText()
        val refreshBody = repository.substringAfter("suspend fun refreshWorkoutDebrief(")
            .substringBefore("suspend fun createRoutine(")
        val runtimeStore = File(mainSources, "data/repository/RoomTrainIqRuntimeStore.kt").readText()

        assertTrue(refreshBody.contains("runtimeStore.getWorkoutDebriefRefreshSnapshot(sessionId)"))
        assertFalse(refreshBody.contains("runtimeStore.state.value"))
        assertFalse(refreshBody.contains("snapshotState.value"))
        assertTrue(runtimeStore.contains("suspend fun getWorkoutDebriefRefreshSnapshot("))
    }

    @Test
    fun completionSummaryDoesNotCreateASecondRetryChain() {
        val useCases = File(mainSources, "domain/usecase/UseCases.kt").readText()
        val summaryUseCase = useCases.substringAfter("class GetWorkoutCompletionSummaryUseCase")
            .substringBefore("class DiscardActiveWorkoutUseCase")

        assertFalse(summaryUseCase.contains("WorkoutDebriefScheduler"))
        assertFalse(summaryUseCase.contains("enqueue("))
    }
}
