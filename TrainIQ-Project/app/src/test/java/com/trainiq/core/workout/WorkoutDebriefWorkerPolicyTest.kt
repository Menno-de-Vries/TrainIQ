package com.trainiq.core.workout

import com.trainiq.ai.services.AiProviderUnavailableException
import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutDebriefWorkerPolicyTest {
    @Test
    fun transientFailuresRetryOnlyBeforeThirdAttempt() {
        assertTrue(shouldRetryWorkoutDebriefFailure(IOException("offline")))
        assertEquals(3, WorkoutDebriefMaxAttempts)
        assertTrue(0 < WorkoutDebriefMaxAttempts - 1)
        assertTrue(1 < WorkoutDebriefMaxAttempts - 1)
        assertFalse(2 < WorkoutDebriefMaxAttempts - 1)
    }

    @Test
    fun missingProviderConfigurationIsPermanent() {
        assertFalse(shouldRetryWorkoutDebriefFailure(AiProviderUnavailableException(emptyList())))
        assertTrue(shouldRetryWorkoutDebriefFailure(AiProviderUnavailableException(listOf("GEMINI:timeout"))))
    }
}
