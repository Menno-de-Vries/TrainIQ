package com.trainiq.core.workout

import com.trainiq.ai.services.AiProviderUnavailableException
import com.trainiq.ai.services.AiFailureCategory
import com.trainiq.ai.services.AiFeature
import com.trainiq.ai.services.AiProvider
import com.trainiq.ai.services.AiProviderRequestException
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

    @Test
    fun typedOpenAiFailuresRetryOnlyWhenTemporary() {
        assertTrue(shouldRetryWorkoutDebriefFailure(openAiFailure(AiFailureCategory.TEMPORARY_RATE_LIMIT)))
        assertTrue(shouldRetryWorkoutDebriefFailure(openAiFailure(AiFailureCategory.NETWORK)))
        assertTrue(shouldRetryWorkoutDebriefFailure(openAiFailure(AiFailureCategory.SERVICE_FAILURE)))
        assertFalse(shouldRetryWorkoutDebriefFailure(openAiFailure(AiFailureCategory.AUTHENTICATION)))
        assertFalse(shouldRetryWorkoutDebriefFailure(openAiFailure(AiFailureCategory.QUOTA_BILLING)))
        assertFalse(shouldRetryWorkoutDebriefFailure(openAiFailure(AiFailureCategory.REQUEST_CONFIGURATION)))
    }

    private fun openAiFailure(category: AiFailureCategory) = AiProviderRequestException(
        provider = AiProvider.OPENAI,
        feature = AiFeature.WORKOUT_DEBRIEF,
        category = category,
    )
}
