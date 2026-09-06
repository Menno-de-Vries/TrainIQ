package com.trainiq.features.workout

import com.trainiq.domain.model.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.test.*
import kotlinx.coroutines.withContext
import org.junit.Assert.*
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class WorkoutCompletionLoaderTest {
    @Test fun initialFailureCanRetryAndRefreshFailureKeepsSavedSummary() = runTest {
        var failing = true
        var reads = 0
        val expected = summary(1L, WorkoutDebriefSource.LOCAL_FALLBACK)
        val loader = WorkoutCompletionLoader(backgroundScope) {
            reads++
            if (failing || reads > 2) error("storage unavailable")
            expected
        }
        loader.load(1L)
        runCurrent()
        assertTrue(loader.uiState.value is WorkoutCompletionUiState.Error)
        failing = false
        loader.load(1L)
        runCurrent()
        assertEquals(WorkoutCompletionUiState.Success(expected), loader.uiState.value)
        advanceTimeBy(2_001)
        runCurrent()
        assertEquals(WorkoutCompletionUiState.Success(expected), loader.uiState.value)
    }

    @Test fun lateOlderReadCannotReplaceCurrentSession() = runTest {
        val old = CompletableDeferred<Unit>()
        val loader = WorkoutCompletionLoader(backgroundScope) { id ->
            if (id == 1L) withContext(NonCancellable) { old.await() }
            summary(id)
        }
        loader.load(1L)
        runCurrent()
        loader.load(2L)
        runCurrent()
        old.complete(Unit)
        runCurrent()
        assertEquals(2L, (loader.uiState.value as WorkoutCompletionUiState.Success).summary.sessionId)
    }

    @Test fun missingSessionShowsRecoverableError() = runTest {
        val loader = WorkoutCompletionLoader(backgroundScope) { null }
        loader.load(99L)
        runCurrent()
        assertTrue(loader.uiState.value is WorkoutCompletionUiState.Error)
    }

    private fun summary(id: Long, source: WorkoutDebriefSource = WorkoutDebriefSource.GEMINI_2_5_FLASH) =
        WorkoutCompletionSummary(id, "Training", 0L, 1000L, 1L, 1, 1, 100.0, 0, "100 kg",
            WorkoutDebrief("Opgeslagen", "", "", "", 75, "MAINTAIN", source = source), "Lokaal", "", emptyList())
}
