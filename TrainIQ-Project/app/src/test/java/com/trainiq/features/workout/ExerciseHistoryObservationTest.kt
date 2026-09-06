package com.trainiq.features.workout

import com.trainiq.core.ui.ScreenUiState
import com.trainiq.domain.model.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ExerciseHistoryObservationTest {
    @Test fun historyFailureCanRetryAndObserveLaterUpdates() = runTest {
        var attempts = 0
        val history = ExerciseHistory(null, ExerciseStats(null, 0, 0, 0.0, 0, 0.0, "", "", null, 0.0, null),
            emptyList(), emptyList(), emptyList(), emptyList(),
            ExerciseRankProgress(ExerciseRank.BEGINNER, 0.0, null, 0f, 0.0, ""))
        val source = MutableStateFlow(history)
        val observation = ExerciseHistoryObservation(backgroundScope) {
            flow { if (++attempts == 1) error("storage"); emitAll(source) }
        }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { observation.uiState.collect() }
        runCurrent()
        assertTrue(observation.uiState.value is ScreenUiState.Error)
        observation.retry()
        runCurrent()
        assertEquals(history, (observation.uiState.value as ScreenUiState.Success).content)
        source.value = history.copy(exercise = Exercise(3L, "Bench", "Chest", "Barbell"))
        runCurrent()
        assertEquals("Bench", (observation.uiState.value as ScreenUiState.Success).content.exercise?.name)
        assertEquals(2, attempts)
    }
}
