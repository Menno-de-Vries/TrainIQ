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

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
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
    fun openAiStrictSchemas_keepSupportedConstraintsAndExcludeUnsupportedCompositionKeywords() {
        val schemas = listOf(
            AiJsonSchemas.mealScan,
            AiJsonSchemas.bodyMeasurementPhoto,
            AiJsonSchemas.workoutDebrief,
            AiJsonSchemas.goalAdvice,
            AiJsonSchemas.weeklyReport,
            AiJsonSchemas.routineGenerator,
        ).map { it.toOpenAiStrictSchema() }
        val keys = mutableSetOf<String>()

        fun collect(value: Any?) {
            when (value) {
                is Map<*, *> -> value.forEach { (key, child) ->
                    keys += key.toString()
                    collect(child)
                }
                is List<*> -> value.forEach(::collect)
            }
        }
        schemas.forEach(::collect)

        assertTrue("maxLength" in keys)
        assertTrue("maxItems" in keys)
        assertTrue(setOf("allOf", "not", "dependentRequired", "dependentSchemas", "if", "then", "else").none(keys::contains))
    }

    @Test
    fun routeAiProviderRequest_usesConfiguredOpenAiWhenGeminiIsPreferredButMissing() = runTest {
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
    fun routeAiProviderRequest_usesConfiguredGeminiWhenOpenAiIsPreferredButMissing() = runTest {
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
        assertEquals(listOf("OPENAI:AiProviderRequestException"), result.fallbackFailures)
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
        assertEquals(listOf("OPENAI:AiProviderRequestException"), (error as AiProviderUnavailableException).failures)
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
    fun routeAiProviderRequest_openAiTemporary429_respectsRetryAfterThenSucceeds() = runTest {
        val openAi = SequencedAiModelClient(
            provider = AiProvider.OPENAI,
            outcomes = ArrayDeque(
                listOf(
                    Result.failure(
                        AiProviderRequestException(
                            provider = AiProvider.OPENAI,
                            feature = AiFeature.WEEKLY_REPORT,
                            category = AiFailureCategory.TEMPORARY_RATE_LIMIT,
                            httpStatus = 429,
                            errorCode = "rate_limit_exceeded",
                            retryAfterMillis = 3_000L,
                        ),
                    ),
                    Result.success(AiRouteResult(AiProvider.OPENAI, "openai-model", "{}")),
                ),
            ),
        )

        val result = routeAiProviderRequest(
            settings = aiSettings(AiProviderPreference.OPENAI_FIRST, geminiApiKey = "", openAiApiKey = "openai-key"),
            request = weeklyRequest(),
            clientFor = { openAi },
        )

        assertEquals(AiProvider.OPENAI, result.providerUsed)
        assertEquals(2, openAi.calls)
        assertEquals(3_000L, testScheduler.currentTime)
    }

    @Test
    fun routeAiProviderRequest_openAiTemporary429_throttlesOnlyAfterRetryBudgetIsExhausted() = runTest {
        val throttle = AiFeatureThrottle(nowMillis = { testScheduler.currentTime })
        val diagnostics = mutableListOf<Map<String, String>>()
        val temporaryRateLimit = AiProviderRequestException(
            provider = AiProvider.OPENAI,
            feature = AiFeature.WEEKLY_REPORT,
            category = AiFailureCategory.TEMPORARY_RATE_LIMIT,
            httpStatus = 429,
            errorCode = "rate_limit_exceeded",
        )
        val openAi = FakeAiModelClient(AiProvider.OPENAI, error = temporaryRateLimit)
        val settings = aiSettings(AiProviderPreference.OPENAI_FIRST, geminiApiKey = "", openAiApiKey = "openai-key")

        runCatching {
            routeAiProviderRequest(
                settings = settings,
                request = weeklyRequest(),
                throttleForProvider = { throttle },
                clientFor = { openAi },
                onFailureDiagnostic = { diagnostics += it },
            )
        }
        assertEquals(listOf("openai-key", "openai-key"), openAi.apiKeys)

        runCatching {
            routeAiProviderRequest(
                settings = settings,
                request = weeklyRequest(),
                throttleForProvider = { throttle },
                clientFor = { openAi },
                onFailureDiagnostic = { diagnostics += it },
            )
        }
        assertEquals(listOf("openai-key", "openai-key"), openAi.apiKeys)
        assertEquals(2, diagnostics.size)
        assertEquals("temporary_rate_limit", diagnostics.last()["category"])
        assertTrue(diagnostics.last()["retry_after_ms"]?.toLongOrNull() ?: 0L > 0L)
    }

    @Test
    fun routeAiProviderRequest_openAiQuotaAndAuthenticationNeverRetryOrThrottleNextCalculation() = runTest {
        val terminalFailures = listOf(AiFailureCategory.QUOTA_BILLING, AiFailureCategory.AUTHENTICATION)

        terminalFailures.forEach { category ->
            val throttle = AiFeatureThrottle(nowMillis = { testScheduler.currentTime })
            val openAi = FakeAiModelClient(
                AiProvider.OPENAI,
                error = AiProviderRequestException(
                    provider = AiProvider.OPENAI,
                    feature = AiFeature.WEEKLY_REPORT,
                    category = category,
                    httpStatus = if (category == AiFailureCategory.AUTHENTICATION) 401 else 429,
                ),
            )
            val settings = aiSettings(AiProviderPreference.OPENAI_FIRST, geminiApiKey = "", openAiApiKey = "openai-key")

            repeat(2) {
                runCatching {
                    routeAiProviderRequest(
                        settings = settings,
                        request = weeklyRequest(),
                        throttleForProvider = { throttle },
                        clientFor = { openAi },
                    )
                }
            }

            assertEquals("$category must reach OpenAI once per calculation", 2, openAi.apiKeys.size)
        }
    }

    @Test
    fun routeAiProviderRequest_reportsOnlySanitizedFailureMetadata() = runTest {
        val diagnostics = mutableListOf<Map<String, String>>()
        val openAi = FakeAiModelClient(
            AiProvider.OPENAI,
            error = AiProviderRequestException(
                provider = AiProvider.OPENAI,
                feature = AiFeature.WEEKLY_REPORT,
                category = AiFailureCategory.AUTHENTICATION,
                httpStatus = 401,
                errorCode = "invalid_api_key",
                requestId = "req_safe_123",
                cause = IllegalArgumentException("openai-key prompt-private image-private"),
            ),
        )

        runCatching {
            routeAiProviderRequest(
                settings = aiSettings(AiProviderPreference.OPENAI_FIRST, geminiApiKey = "", openAiApiKey = "openai-key"),
                request = weeklyRequest(),
                clientFor = { openAi },
                onFailureDiagnostic = { diagnostic -> diagnostics.add(diagnostic) },
            )
        }

        assertEquals(
            mapOf(
                "provider" to "openai",
                "feature" to "weekly_report",
                "category" to "authentication",
                "http_status" to "401",
                "error_code" to "invalid_api_key",
                "request_id" to "req_safe_123",
            ),
            diagnostics.single(),
        )
        val recorded = diagnostics.single().toString()
        assertFalse(recorded.contains("openai-key"))
        assertFalse(recorded.contains("prompt-private"))
        assertFalse(recorded.contains("image-private"))
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

    private class SequencedAiModelClient(
        override val provider: AiProvider,
        private val outcomes: ArrayDeque<Result<AiRouteResult>>,
    ) : AiModelClient {
        var calls: Int = 0

        override suspend fun generateJson(apiKey: String, request: AiRouteRequest): AiRouteResult {
            calls += 1
            return outcomes.removeFirst().getOrThrow()
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

    private fun rateLimitError(): AiProviderRequestException =
        AiProviderRequestException(
            provider = AiProvider.OPENAI,
            feature = AiFeature.WEEKLY_REPORT,
            category = AiFailureCategory.TEMPORARY_RATE_LIMIT,
            httpStatus = 429,
            errorCode = "rate_limit_exceeded",
        )
}
