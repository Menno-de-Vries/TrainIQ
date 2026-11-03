package com.trainiq.features.workout

import com.trainiq.domain.model.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import kotlinx.coroutines.withContext
import org.junit.Assert.*
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class WorkoutCompletionLoaderTest {
    @Test fun savedDebriefAfterFifteenSecondsRemainsVisibleOnOpenScreen() = runTest {
        val local = summary(1L, WorkoutDebriefSource.LOCAL_FALLBACK)
        val generated = summary(1L)
        val saved = MutableStateFlow(local)
        val loader = WorkoutCompletionLoader(backgroundScope) { saved }
        backgroundScope.launch { loader.uiState.collect() }
        loader.load(1L)
        runCurrent()
        assertEquals(WorkoutCompletionUiState.Success(local), loader.uiState.value)
        advanceTimeBy(15_000L)
        saved.value = generated
        runCurrent()
        assertEquals(WorkoutCompletionUiState.Success(generated), loader.uiState.value)
    }

    @Test fun initialFailureCanRetryAndRefreshFailureKeepsSavedSummary() = runTest {
        var failing = true
        val refresh = CompletableDeferred<Unit>()
        val expected = summary(1L, WorkoutDebriefSource.LOCAL_FALLBACK)
        val loader = WorkoutCompletionLoader(backgroundScope) {
            flow {
                if (failing) error("storage unavailable")
                emit(expected)
                refresh.await()
                error("storage unavailable after cached success")
            }
        }
        backgroundScope.launch { loader.uiState.collect() }
        loader.load(1L)
        runCurrent()
        assertTrue(loader.uiState.value is WorkoutCompletionUiState.Error)
        failing = false
        loader.load(1L)
        runCurrent()
        assertEquals(WorkoutCompletionUiState.Success(expected), loader.uiState.value)
        refresh.complete(Unit)
        runCurrent()
        assertEquals(WorkoutCompletionUiState.Success(expected), loader.uiState.value)
    }

    @Test fun lateOlderReadCannotReplaceCurrentSession() = runTest {
        val old = CompletableDeferred<Unit>()
        val loader = WorkoutCompletionLoader(backgroundScope) { id ->
            flow {
                if (id == 1L) withContext(NonCancellable) { old.await() }
                emit(summary(id))
            }
        }
        backgroundScope.launch { loader.uiState.collect() }
        loader.load(1L)
        runCurrent()
        loader.load(2L)
        runCurrent()
        assertEquals(2L, (loader.uiState.value as WorkoutCompletionUiState.Success).summary.sessionId)
        old.complete(Unit)
        runCurrent()
        assertEquals(2L, (loader.uiState.value as WorkoutCompletionUiState.Success).summary.sessionId)
    }

    @Test fun deletionReplacesCachedSuccessAndRetryCanReadReplacement() = runTest {
        val saved = MutableStateFlow<WorkoutCompletionSummary?>(summary(1L))
        val loader = WorkoutCompletionLoader(backgroundScope) { saved }
        backgroundScope.launch { loader.uiState.collect() }
        loader.load(1L)
        runCurrent()
        assertTrue(loader.uiState.value is WorkoutCompletionUiState.Success)
        saved.value = null
        runCurrent()
        assertTrue(loader.uiState.value is WorkoutCompletionUiState.Error)
        loader.load(1L)
        runCurrent()
        assertTrue(loader.uiState.value is WorkoutCompletionUiState.Error)
        saved.value = summary(1L, WorkoutDebriefSource.LOCAL_FALLBACK)
        runCurrent()
        assertEquals(WorkoutCompletionUiState.Success(saved.value!!), loader.uiState.value)
    }

    @Test fun unobservedScreenDoesNoWorkAndCancelsUntilResubscribed() = runTest {
        var subscriptions = 0
        var cancellations = 0
        val loader = WorkoutCompletionLoader(backgroundScope) {
            flow {
                subscriptions++
                try {
                    emit(summary(1L))
                    awaitCancellation()
                } finally {
                    cancellations++
                }
            }
        }
        loader.load(1L)
        runCurrent()
        assertEquals(0, subscriptions)
        val observer = backgroundScope.launch { loader.uiState.collect() }
        runCurrent()
        assertEquals(1, subscriptions)
        observer.cancel()
        runCurrent()
        assertEquals(1, cancellations)
        advanceTimeBy(30_000L)
        runCurrent()
        assertEquals(1, subscriptions)
        backgroundScope.launch { loader.uiState.collect() }
        runCurrent()
        assertEquals(2, subscriptions)
    }

    @Test fun missingSessionShowsRecoverableError() = runTest {
        val loader = WorkoutCompletionLoader(backgroundScope) { flowOf(null) }
        backgroundScope.launch { loader.uiState.collect() }
        loader.load(99L)
        runCurrent()
        assertTrue(loader.uiState.value is WorkoutCompletionUiState.Error)
    }

    private fun summary(id: Long, source: WorkoutDebriefSource = WorkoutDebriefSource.GEMINI_2_5_FLASH) =
        WorkoutCompletionSummary(id, "Training", 0L, 1000L, 1L, 1, 1, 100.0, 0, "100 kg",
            WorkoutDebrief("Opgeslagen", "", "", "", 75, "MAINTAIN", source = source), "Lokaal", "", emptyList())
}
