package com.trainiq.ai.services

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class AiSupportTest {
    @Test
    fun toAiUserMessage_mapsOpenAiFailuresToDistinctActionableCauses() {
        val authentication = failure(AiFailureCategory.AUTHENTICATION).toAiUserMessage("generic")
        val quota = failure(AiFailureCategory.QUOTA_BILLING).toAiUserMessage("generic")
        val rateLimit = failure(AiFailureCategory.TEMPORARY_RATE_LIMIT).toAiUserMessage("generic")
        val schema = failure(AiFailureCategory.REQUEST_CONFIGURATION).toAiUserMessage("generic")

        assertTrue(authentication.contains("AI-instellingen"))
        assertTrue(quota.contains("billing"))
        assertTrue(rateLimit.contains("later opnieuw"))
        assertTrue(schema.contains("Werk de app bij"))
        assertFalse(schema.contains("API-sleutel"))
    }

    @Test
    fun callOpenAiWithBoundedRetry_withoutRetryAfter_usesBoundedExponentialBackoffWithJitter() = runTest {
        var calls = 0

        val result = callOpenAiWithBoundedRetry(
            feature = AiFeature.WEEKLY_REPORT,
            throttle = AiFeatureThrottle(nowMillis = { testScheduler.currentTime }),
            initialBackoffMillis = 350L,
            jitterMillis = { maximum -> maximum },
        ) {
            calls += 1
            if (calls == 1) throw failure(AiFailureCategory.TEMPORARY_RATE_LIMIT)
            "ok"
        }

        assertTrue(result == "ok")
        assertTrue(calls == 2)
        assertTrue(testScheduler.currentTime == 437L)
    }

    private fun failure(category: AiFailureCategory) = AiProviderRequestException(
        provider = AiProvider.OPENAI,
        feature = AiFeature.WEEKLY_REPORT,
        category = category,
    )
}
