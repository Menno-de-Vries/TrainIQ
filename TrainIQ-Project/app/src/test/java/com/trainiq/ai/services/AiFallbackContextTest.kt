package com.trainiq.ai.services

import com.trainiq.domain.model.AiFallbackContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AiFallbackContextTest {
    @Test
    fun readinessMapsToExplicitLocalConfigurationCauses() {
        assertEquals(AiFallbackContext.AI_DISABLED, AiReadiness.DISABLED.toAiFallbackContext())
        assertEquals(AiFallbackContext.NO_DECRYPTABLE_KEY, AiReadiness.NO_DECRYPTABLE_KEY.toAiFallbackContext())
        assertNull(AiReadiness.CONFIGURED.toAiFallbackContext())
    }

    @Test
    fun onlyRecoverableRemoteFailuresMapToLocalFallbackCauses() {
        val cases = mapOf(
            AiFailureCategory.TEMPORARY_RATE_LIMIT to AiFallbackContext.RATE_LIMIT,
            AiFailureCategory.TIMEOUT to AiFallbackContext.TIMEOUT,
            AiFailureCategory.NETWORK to AiFallbackContext.NETWORK,
            AiFailureCategory.SERVICE_FAILURE to AiFallbackContext.SERVICE_FAILURE,
        )

        cases.forEach { (category, expected) ->
            assertEquals(expected, failure(category).toAiFallbackContext())
        }
        assertNull(failure(AiFailureCategory.AUTHENTICATION).toAiFallbackContext())
        assertNull(failure(AiFailureCategory.QUOTA_BILLING).toAiFallbackContext())
    }

    @Test
    fun fallbackMessagesAreSpecificAndActionable() {
        assertTrue(AiFallbackContext.AI_DISABLED.safeUserMessage().contains("Instellingen"))
        assertTrue(AiFallbackContext.NO_DECRYPTABLE_KEY.safeUserMessage().contains("sleutel"))
        assertTrue(AiFallbackContext.RATE_LIMIT.safeUserMessage().contains("later opnieuw"))
        assertTrue(AiFallbackContext.TIMEOUT.safeUserMessage().contains("opnieuw"))
    }

    private fun failure(category: AiFailureCategory) = AiProviderRequestException(
        provider = AiProvider.OPENAI,
        feature = AiFeature.GOAL_ADVICE,
        category = category,
    )
}
