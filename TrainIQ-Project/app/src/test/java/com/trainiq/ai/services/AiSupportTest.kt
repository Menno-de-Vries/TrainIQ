package com.trainiq.ai.services

import java.io.IOException
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

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

    @Test
    fun callOpenAiWithBoundedRetry_quotaFailureDoesNotRetryOrThrottleNextRequest() = runTest {
        val throttle = AiFeatureThrottle(nowMillis = { testScheduler.currentTime })
        var calls = 0

        val first = runCatching {
            callOpenAiWithBoundedRetry(
                feature = AiFeature.WEEKLY_REPORT,
                throttle = throttle,
            ) {
                calls += 1
                throw failure(AiFailureCategory.QUOTA_BILLING)
            }
        }.exceptionOrNull()
        val second = callOpenAiWithBoundedRetry(
            feature = AiFeature.WEEKLY_REPORT,
            throttle = throttle,
        ) {
            calls += 1
            "ok"
        }

        assertEquals(AiFailureCategory.QUOTA_BILLING, (first as AiProviderRequestException).category)
        assertEquals("ok", second)
        assertEquals(2, calls)
    }

    @Test
    fun callOpenAiWithBoundedRetry_timeoutDoesNotThrottleNextRequest() = runTest {
        val throttle = AiFeatureThrottle(nowMillis = { testScheduler.currentTime })
        var calls = 0

        val first = runCatching {
            callOpenAiWithBoundedRetry(
                feature = AiFeature.WEEKLY_REPORT,
                timeoutMillis = 5L,
                throttle = throttle,
                elapsedRealtimeMillis = { testScheduler.currentTime },
            ) {
                calls += 1
                delay(10L)
            }
        }.exceptionOrNull()
        val second = callOpenAiWithBoundedRetry(
            feature = AiFeature.WEEKLY_REPORT,
            timeoutMillis = 5L,
            throttle = throttle,
            elapsedRealtimeMillis = { testScheduler.currentTime },
        ) {
            calls += 1
            "ok"
        }

        assertEquals(AiFailureCategory.TIMEOUT, (first as AiProviderRequestException).category)
        assertEquals("ok", second)
        assertEquals(2, calls)
    }

    @Test
    fun callOpenAiWithBoundedRetry_doesNotStartRetryBeyondRemainingFeatureBudget() = runTest {
        val throttle = AiFeatureThrottle(nowMillis = { testScheduler.currentTime })
        var calls = 0

        val error = runCatching {
            callOpenAiWithBoundedRetry(
                feature = AiFeature.WEEKLY_REPORT,
                timeoutMillis = 10L,
                throttle = throttle,
                elapsedRealtimeMillis = { testScheduler.currentTime },
            ) {
                calls += 1
                delay(8L)
                throw failure(AiFailureCategory.TEMPORARY_RATE_LIMIT, retryAfterMillis = 5L)
            }
        }.exceptionOrNull() as AiProviderRequestException

        assertEquals(AiFailureCategory.TEMPORARY_RATE_LIMIT, error.category)
        assertEquals(1, error.attempt)
        assertEquals(8L, error.durationMillis)
        assertEquals(1, calls)
        assertEquals(8L, testScheduler.currentTime)
    }

    @Test
    fun callOpenAiWithBoundedRetry_exhaustedTemporaryRateLimitThrottlesWithoutThirdCall() = runTest {
        val throttle = AiFeatureThrottle(nowMillis = { testScheduler.currentTime })
        var calls = 0

        val first = runCatching {
            callOpenAiWithBoundedRetry(
                feature = AiFeature.WEEKLY_REPORT,
                throttle = throttle,
                initialBackoffMillis = 1L,
                jitterMillis = { 0L },
                elapsedRealtimeMillis = { testScheduler.currentTime },
            ) {
                calls += 1
                throw failure(AiFailureCategory.TEMPORARY_RATE_LIMIT)
            }
        }.exceptionOrNull() as AiProviderRequestException
        val second = runCatching {
            callOpenAiWithBoundedRetry(
                feature = AiFeature.WEEKLY_REPORT,
                throttle = throttle,
                elapsedRealtimeMillis = { testScheduler.currentTime },
            ) {
                calls += 1
                "unexpected"
            }
        }.exceptionOrNull() as AiProviderRequestException

        assertEquals(2, first.attempt)
        assertEquals(AiFailureCategory.TEMPORARY_RATE_LIMIT, second.category)
        assertEquals(0, second.attempt)
        assertEquals(2, calls)
    }

    private fun failure(
        category: AiFailureCategory,
        retryAfterMillis: Long? = null,
    ) = AiProviderRequestException(
        provider = AiProvider.OPENAI,
        feature = AiFeature.WEEKLY_REPORT,
        category = category,
        retryAfterMillis = retryAfterMillis,
    )

    @Test
    fun toAiUserMessage_mapsConfigurationPermissionNetworkAndServiceFailuresWithoutResponseBodies() {
        val responseBody = "secret-bearing response body".toResponseBody()

        assertEquals(
            "De API-sleutel is ongeldig of niet geautoriseerd. Controleer de AI-instellingen.",
            HttpException(Response.error<Unit>(401, responseBody)).toAiUserMessage("fallback"),
        )
        assertEquals(
            "De AI-provider heeft geen toestemming voor dit verzoek. Controleer de providerrechten.",
            HttpException(Response.error<Unit>(403, responseBody)).toAiUserMessage("fallback"),
        )
        assertEquals(
            "De AI-provider kan niet worden bereikt. Controleer je internetverbinding en probeer opnieuw.",
            IOException("network diagnostic").toAiUserMessage("fallback"),
        )
        assertEquals(
            "De AI-provider is tijdelijk niet beschikbaar. Probeer later opnieuw.",
            HttpException(Response.error<Unit>(500, responseBody)).toAiUserMessage("fallback"),
        )
    }
}
