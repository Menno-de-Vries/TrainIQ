package com.trainiq.ai.services

import com.trainiq.core.datastore.AiPreferences
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class AiProviderRouterTest {
    @Test
    fun openAiStrictSchema_addsAdditionalPropertiesFalseToNestedObjects() {
        val schema = GeminiJsonSchemas.routineGenerator.toOpenAiStrictSchema()
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
                responseJsonSchema = GeminiJsonSchemas.weeklyReport,
                thinkingBudget = 1000,
            ),
        )

        assertEquals(AiProvider.GEMINI, result.providerUsed)
        assertEquals(listOf(AiProvider.OPENAI, AiProvider.GEMINI), generator.calls)
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
}
