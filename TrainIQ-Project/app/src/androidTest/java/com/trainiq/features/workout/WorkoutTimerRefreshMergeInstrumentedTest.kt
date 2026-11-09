package com.trainiq.features.workout

import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.trainiq.MainActivity
import com.trainiq.core.datastore.OnboardingPreferences
import com.trainiq.core.datastore.UserPreferencesRepository
import com.trainiq.domain.model.ActiveWorkoutSession
import com.trainiq.testing.resetTrainIqAndroidTestDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

/** Actual Hilt WorkoutViewModel/applyActiveSession; no provider, sensor or clock sleeps. */
class WorkoutTimerRefreshMergeInstrumentedTest {
    @Test fun unrelatedSessionRefreshMergesCollapseWithoutUndoingOptimisticTimer() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        runBlocking { UserPreferencesRepository(context).saveOnboardingPreferences(
            OnboardingPreferences(completed = true, guidedTourCompleted = true)) }
        resetTrainIqAndroidTestDatabase(context)
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val model = ViewModelProvider(activity)[WorkoutViewModel::class.java]
                val apply = WorkoutViewModel::class.java.getDeclaredMethod("applyActiveSession", ActiveWorkoutSession::class.java)
                    .apply { isAccessible = true }
                @Suppress("UNCHECKED_CAST")
                val state = WorkoutViewModel::class.java.getDeclaredField("_activeSession").apply { isAccessible = true }
                    .get(model) as MutableStateFlow<ActiveWorkoutSession?>
                val initial = ActiveWorkoutSession(sessionId = 7, dayId = 8, startedAt = 1, updatedAt = 1,
                    loggedSets = emptyList(), drafts = emptyMap(), collapsedExerciseIds = emptySet(),
                    restTimerEndsAt = null, restTimerTotalSeconds = 0)
                apply.invoke(model, initial)
                model.adjustRestTimer(30)
                val adjusted = state.value!!
                assertNotNull(adjusted.restTimerGeneration)
                apply.invoke(model, initial.copy(collapsedExerciseIds = setOf(3), updatedAt = 2))
                assertEquals(setOf(3L), state.value!!.collapsedExerciseIds)
                assertEquals(adjusted.restTimerGeneration, state.value!!.restTimerGeneration)
                assertEquals(adjusted.restTimerEndsAt, state.value!!.restTimerEndsAt)
                assertEquals(adjusted.restTimerElapsedDeadline, state.value!!.restTimerElapsedDeadline)
            }
        }
    }
}
