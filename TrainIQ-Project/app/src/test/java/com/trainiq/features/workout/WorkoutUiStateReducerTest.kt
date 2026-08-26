package com.trainiq.features.workout

import com.trainiq.core.datastore.WorkoutFeedbackPreferences
import com.trainiq.core.ui.ScreenUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutUiStateReducerTest {
    @Test
    fun workoutScreenUiState_wrapsAggregateContentAsSuccess() {
        val content = WorkoutUiContent(
            overview = null,
            workoutFeedbackPreferences = WorkoutFeedbackPreferences(restTimerSoundEnabled = true),
            activeWorkout = ActiveWorkoutUiState(completedSets = 3),
            message = "Set opgeslagen.",
            pendingGeneratedRoutine = null,
            isSavingGeneratedRoutine = false,
            isGeneratingAiRoutine = false,
        )

        val state = workoutScreenUiState(content)

        assertTrue(state is ScreenUiState.Success)
        assertEquals(content, (state as ScreenUiState.Success).content)
    }
}
