package com.trainiq.features.workout

import com.trainiq.core.datastore.WorkoutFeedbackPreferences
import com.trainiq.core.ui.ScreenUiState
import com.trainiq.domain.model.WorkoutOverview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class WorkoutObservationsTest {
    @Test fun readFailureRetriesBothInputsAndKeepsObservingChanges() = runTest {
        var reads = 0
        val preferences = MutableStateFlow(WorkoutFeedbackPreferences())
        val overview = WorkoutOverview(null, emptyList(), emptyList(), history = emptyList())
        val observations = WorkoutObservations(backgroundScope, {
            flow { reads++; if (reads == 1) error("storage read"); emit(overview) }
        }, { preferences })
        var error: ScreenUiState.Error? = null
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { observations.error.collect { error = it } }
        runCurrent()
        assertNotNull(error)
        observations.retry()
        runCurrent()
        assertNull(error)
        assertEquals(2, reads)
        assertEquals(overview, observations.overview.value?.getOrNull())
        preferences.value = preferences.value.copy(workoutHapticsEnabled = false)
        runCurrent()
        assertEquals(false, observations.preferences.value?.getOrNull()?.workoutHapticsEnabled)
    }

    @Test fun preferenceFailureHasItsOwnRecoverableMessage() = runTest {
        val observations = WorkoutObservations(backgroundScope,
            { flowOf(WorkoutOverview(null, emptyList(), emptyList(), history = emptyList())) },
            { flow { error("preferences") } })
        var error: ScreenUiState.Error? = null
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { observations.error.collect { error = it } }
        runCurrent()
        assertEquals("Trainingsvoorkeuren konden niet worden geladen.", error?.message)
    }
}
