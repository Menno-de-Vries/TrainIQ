package com.trainiq.features.workout

import androidx.lifecycle.SavedStateHandle
import com.trainiq.domain.usecase.ResumeWorkoutFromRestNotificationUseCase
import com.trainiq.domain.usecase.StartedWorkoutSession
import com.trainiq.domain.workout.WorkoutRestOpenRequest

/** The first open accepts a rest generation; subsequent restoration resumes that session's lifetime. */
internal class WorkoutNotificationResumeState(
    private val savedState: SavedStateHandle,
    private val resumeWorkout: ResumeWorkoutFromRestNotificationUseCase,
) {
    suspend fun resume(request: WorkoutRestOpenRequest): StartedWorkoutSession? {
        val acceptedStartedAt = if (
            savedState.get<Long>(Day) == request.dayId &&
            savedState.get<Long>(Session) == request.sessionId &&
            savedState.get<String>(Generation) == request.generation
        ) savedState.get<Long>(StartedAt) else null
        return resumeWorkout(request, acceptedStartedAt)?.also { result ->
            // Primitive saved-state values survive a new destination ViewModel after process recreation.
            // Bind acceptance to this request as well: a different intent must pass its own generation check.
            savedState[Day] = request.dayId
            savedState[Session] = request.sessionId
            savedState[Generation] = request.generation
            savedState[StartedAt] = result.session.startedAt
        }
    }

    private companion object {
        const val Day = "acceptedRestDay"
        const val Session = "acceptedRestSession"
        const val Generation = "acceptedRestGeneration"
        const val StartedAt = "acceptedRestStartedAt"
    }
}
