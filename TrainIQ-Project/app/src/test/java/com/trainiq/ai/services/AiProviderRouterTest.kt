package com.trainiq.ai.services

import com.trainiq.core.datastore.AiPreferences
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class AiProviderRouterTest {
    @Test
    fun openAiStrictSchema_addsAdditionalPropertiesFalseToNestedObjects() {
        val schema = AiJsonSchemas.routineGenerator.toOpenAiStrictSchema()
        val days = ((schema["properties"] as Map<*, *>)["days"] as Map<*, *>)
        val day = days["items"] as Map<*, *>

        assertEquals(false, schema["additionalProperties"])
        assertEquals(false, day["additionalProperties"])
        assertTrue((schema["required"] as List<*>).contains("routineDescription"))
    }

    @Test
    fun aiPreferences_reportsAnyReadyProvider() {
        assertTrue(
            AiPreferences(
                enabled = true,
                apiKey = "",
                geminiApiKey = "",
                openAiApiKey = "sk-test",
                preferredProvider = AiProviderPreference.OPENAI_FIRST,
            ).hasAnyReadyProvider(),
        )
    }

    @Test
    fun fallbackGenerator_triesPreferredThenSecondProviderOnRateLimit() = runTest {
        val generator = FallbackTestGenerator(
            preference = AiProviderPreference.OPENAI_FIRST,
            keys = mapOf(AiProvider.OPENAI to "openai", AiProvider.GEMINI to "gemini"),
        )

        val result = generator.generateJson(
            AiRouteRequest(
                feature = AiFeature.WEEKLY_REPORT,
                prompt = "Geef JSON",
                schemaName = "weekly_report",
                responseJsonSchema = AiJsonSchemas.weeklyReport,
                thinkingBudget = 1000,
            ),
        )

        assertEquals(AiProvider.GEMINI, result.providerUsed)
        assertEquals(listOf(AiProvider.OPENAI, AiProvider.GEMINI), generator.calls)
    }

    @Test
    fun routeAiProviderRequest_usesPreferredProviderWhenReady() = runTest {
        val gemini = FakeAiModelClient(AiProvider.GEMINI)
        val openAi = FakeAiModelClient(AiProvider.OPENAI)

        val result = routeAiProviderRequest(
            settings = aiSettings(
                preferredProvider = AiProviderPreference.GEMINI_FIRST,
                geminiApiKey = "gemini-key",
                openAiApiKey = "openai-key",
            ),
            request = weeklyRequest(),
            clientFor = { provider -> if (provider == AiProvider.GEMINI) gemini else openAi },
        )

        assertEquals(AiProvider.GEMINI, result.providerUsed)
        assertEquals(listOf("gemini-key"), gemini.apiKeys)
        assertTrue(openAi.apiKeys.isEmpty())
    }

    @Test
    fun routeAiProviderRequest_usesOpenAiWhenGeminiIsPreferredButOnlyOpenAiIsConfigured() = runTest {
        val gemini = FakeAiModelClient(AiProvider.GEMINI)
        val openAi = FakeAiModelClient(AiProvider.OPENAI)

        val result = routeAiProviderRequest(
            settings = aiSettings(
                preferredProvider = AiProviderPreference.GEMINI_FIRST,
                geminiApiKey = "",
                openAiApiKey = "openai-key",
            ),
            request = weeklyRequest(),
            clientFor = { provider -> if (provider == AiProvider.GEMINI) gemini else openAi },
        )

        assertEquals(AiProvider.OPENAI, result.providerUsed)
        assertTrue(gemini.apiKeys.isEmpty())
        assertEquals(listOf("openai-key"), openAi.apiKeys)
    }

    @Test
    fun routeAiProviderRequest_usesGeminiWhenOpenAiIsPreferredButOnlyGeminiIsConfigured() = runTest {
        val gemini = FakeAiModelClient(AiProvider.GEMINI)
        val openAi = FakeAiModelClient(AiProvider.OPENAI)

        val result = routeAiProviderRequest(
            settings = aiSettings(
                preferredProvider = AiProviderPreference.OPENAI_FIRST,
                geminiApiKey = "gemini-key",
                openAiApiKey = "",
            ),
            request = weeklyRequest(),
            clientFor = { provider -> if (provider == AiProvider.GEMINI) gemini else openAi },
        )

        assertEquals(AiProvider.GEMINI, result.providerUsed)
        assertEquals(listOf("gemini-key"), gemini.apiKeys)
        assertTrue(openAi.apiKeys.isEmpty())
    }

    @Test
    fun routeAiProviderRequest_fallsBackToSecondProviderOnTransientFailure() = runTest {
        val openAi = FakeAiModelClient(AiProvider.OPENAI, error = rateLimitError())
        val gemini = FakeAiModelClient(AiProvider.GEMINI)

        val result = routeAiProviderRequest(
            settings = aiSettings(
                preferredProvider = AiProviderPreference.OPENAI_FIRST,
                geminiApiKey = "gemini-key",
                openAiApiKey = "openai-key",
                allowCrossProviderFallback = true,
            ),
            request = weeklyRequest(),
            clientFor = { provider -> if (provider == AiProvider.GEMINI) gemini else openAi },
        )

        assertEquals(AiProvider.GEMINI, result.providerUsed)
        assertEquals(listOf("OPENAI:AiRateLimitException"), result.fallbackFailures)
        assertEquals(listOf("openai-key", "openai-key"), openAi.apiKeys)
        assertEquals(listOf("gemini-key"), gemini.apiKeys)
    }

    @Test
    fun routeAiProviderRequest_doesNotCrossProviderFallbackWithoutExplicitConsent() = runTest {
        val openAi = FakeAiModelClient(AiProvider.OPENAI, error = rateLimitError())
        val gemini = FakeAiModelClient(AiProvider.GEMINI)

        val error = runCatching {
            routeAiProviderRequest(
                settings = aiSettings(
                    preferredProvider = AiProviderPreference.OPENAI_FIRST,
                    geminiApiKey = "gemini-key",
                    openAiApiKey = "openai-key",
                ),
                request = weeklyRequest(),
                clientFor = { provider -> if (provider == AiProvider.GEMINI) gemini else openAi },
            )
        }.exceptionOrNull()

        assertTrue(error is AiProviderUnavailableException)
        assertEquals(listOf("OPENAI:AiRateLimitException"), (error as AiProviderUnavailableException).failures)
        assertEquals(listOf("openai-key", "openai-key"), openAi.apiKeys)
        assertTrue(gemini.apiKeys.isEmpty())
    }

    @Test
    fun routeAiProviderRequest_reusesThrottleToSkipRecentlyRateLimitedProvider() = runTest {
        val openAiThrottle = AiFeatureThrottle(nowMillis = { 1_000L })
        val gemini = FakeAiModelClient(AiProvider.GEMINI)
        val openAi = FakeAiModelClient(AiProvider.OPENAI, error = rateLimitError())
        val settings = aiSettings(
            preferredProvider = AiProviderPreference.OPENAI_FIRST,
            geminiApiKey = "gemini-key",
            openAiApiKey = "openai-key",
            allowCrossProviderFallback = true,
        )

        routeAiProviderRequest(
            settings = settings,
            request = weeklyRequest(),
            throttleForProvider = { provider ->
                if (provider == AiProvider.OPENAI) openAiThrottle else AiFeatureThrottle(nowMillis = { 1_000L })
            },
            clientFor = { provider -> if (provider == AiProvider.GEMINI) gemini else openAi },
        )
        routeAiProviderRequest(
            settings = settings,
            request = weeklyRequest(),
            throttleForProvider = { provider ->
                if (provider == AiProvider.OPENAI) openAiThrottle else AiFeatureThrottle(nowMillis = { 1_000L })
            },
            clientFor = { provider -> if (provider == AiProvider.GEMINI) gemini else openAi },
        )

        assertEquals(listOf("openai-key", "openai-key"), openAi.apiKeys)
        assertEquals(listOf("gemini-key", "gemini-key"), gemini.apiKeys)
    }

    @Test
    fun routeAiProviderRequest_skipsProvidersWithoutKeysAndThrowsWhenNoneReady() = runTest {
        val gemini = FakeAiModelClient(AiProvider.GEMINI)
        val openAi = FakeAiModelClient(AiProvider.OPENAI)

        val error = runCatching {
            routeAiProviderRequest(
                settings = aiSettings(
                    preferredProvider = AiProviderPreference.OPENAI_FIRST,
                    geminiApiKey = "",
                    openAiApiKey = "",
                ),
                request = weeklyRequest(),
                clientFor = { provider -> if (provider == AiProvider.GEMINI) gemini else openAi },
            )
        }.exceptionOrNull()

        assertTrue(error is AiProviderUnavailableException)
        assertTrue((error as AiProviderUnavailableException).failures.isEmpty())
        assertTrue(gemini.apiKeys.isEmpty())
        assertTrue(openAi.apiKeys.isEmpty())
    }

    @Test
    fun routeAiProviderRequest_stopsOnNonTransientProviderFailure() = runTest {
        val openAi = FakeAiModelClient(AiProvider.OPENAI, error = IllegalArgumentException("bad schema"))
        val gemini = FakeAiModelClient(AiProvider.GEMINI)

        val error = runCatching {
            routeAiProviderRequest(
                settings = aiSettings(
                    preferredProvider = AiProviderPreference.OPENAI_FIRST,
                    geminiApiKey = "gemini-key",
                    openAiApiKey = "openai-key",
                ),
                request = weeklyRequest(),
                clientFor = { provider -> if (provider == AiProvider.GEMINI) gemini else openAi },
            )
        }.exceptionOrNull()

        assertTrue(error is IllegalArgumentException)
        assertEquals(listOf("openai-key"), openAi.apiKeys)
        assertTrue(gemini.apiKeys.isEmpty())
    }

    @Test
    fun routeAiProviderRequest_propagatesCancellationWithoutFallback() = runTest {
        val openAi = FakeAiModelClient(AiProvider.OPENAI, error = CancellationException("screen left"))
        val gemini = FakeAiModelClient(AiProvider.GEMINI)

        val error = runCatching {
            routeAiProviderRequest(
                settings = aiSettings(
                    preferredProvider = AiProviderPreference.OPENAI_FIRST,
                    geminiApiKey = "gemini-key",
                    openAiApiKey = "openai-key",
                ),
                request = weeklyRequest(),
                clientFor = { provider -> if (provider == AiProvider.GEMINI) gemini else openAi },
            )
        }.exceptionOrNull()

        assertTrue(error is CancellationException)
        assertEquals("screen left", error?.message)
        assertFalse(gemini.apiKeys.isNotEmpty())
    }

    private class FallbackTestGenerator(
        private val preference: AiProviderPreference,
        private val keys: Map<AiProvider, String>,
    ) : AiJsonGenerator {
        val calls = mutableListOf<AiProvider>()

        override suspend fun generateJson(request: AiRouteRequest): AiRouteResult {
            for (provider in preference.orderedProviders()) {
                if (keys[provider].isNullOrBlank()) continue
                calls += provider
                if (provider == AiProvider.OPENAI) {
                    continue
                }
                return AiRouteResult(provider, "model", """{"summary":"Goed herstel.","nextWeekFocus":"Houd volume stabiel."}""")
            }
            throw HttpException(Response.error<Unit>(429, "rate".toResponseBody()))
        }
    }

    private class FakeAiModelClient(
        override val provider: AiProvider,
        private val error: Throwable? = null,
    ) : AiModelClient {
        val apiKeys = mutableListOf<String>()

        override suspend fun generateJson(apiKey: String, request: AiRouteRequest): AiRouteResult {
            apiKeys += apiKey
            error?.let { throw it }
            return AiRouteResult(
                providerUsed = provider,
                model = "${provider.name.lowercase()}-model",
                rawJson = """{"summary":"Goed herstel.","nextWeekFocus":"Houd volume stabiel."}""",
            )
        }
    }

    private fun aiSettings(
        preferredProvider: AiProviderPreference,
        geminiApiKey: String,
        openAiApiKey: String,
        allowCrossProviderFallback: Boolean = false,
    ): AiPreferences =
        AiPreferences(
            enabled = true,
            apiKey = geminiApiKey,
            geminiApiKey = geminiApiKey,
            openAiApiKey = openAiApiKey,
            preferredProvider = preferredProvider,
            allowCrossProviderFallback = allowCrossProviderFallback,
        )

    private fun weeklyRequest(): AiRouteRequest =
        AiRouteRequest(
            feature = AiFeature.WEEKLY_REPORT,
            prompt = "Geef JSON",
            schemaName = "weekly_report",
            responseJsonSchema = AiJsonSchemas.weeklyReport,
            thinkingBudget = 1000,
        )

    private fun rateLimitError(): HttpException =
        HttpException(Response.error<Unit>(429, "rate".toResponseBody()))
}
