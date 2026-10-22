package com.trainiq.ai.services

import com.google.gson.Gson
import com.trainiq.data.model.GeminiRequest
import com.trainiq.data.model.GeminiResponse
import com.trainiq.data.remote.GeminiApi
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class GeminiModelClientTest {
    @Test fun blockedTruncatedEmptyAndMalformedResponsesHaveSafeCategories() = runTest {
        val cases = listOf(
            """{"promptFeedback":{"blockReason":"SAFETY"}}""" to AiFailureCategory.REFUSAL,
            """{"candidates":[{"finishReason":"SAFETY"}]}""" to AiFailureCategory.REFUSAL,
            """{"candidates":[{"finishReason":"MAX_TOKENS","content":{"parts":[{"text":"{}"}]}}]}""" to AiFailureCategory.INCOMPLETE_RESPONSE,
            """{"candidates":[]}""" to AiFailureCategory.INVALID_RESPONSE,
            """{"candidates":null}""" to AiFailureCategory.INVALID_RESPONSE,
            """{"candidates":[{"content":null}]}""" to AiFailureCategory.INVALID_RESPONSE,
            """{"candidates":[{"content":{"parts":null}}]}""" to AiFailureCategory.INVALID_RESPONSE,
            """{"candidates":[{"finishReason":"STOP","content":{"parts":[{"text":"private malformed text"}]}}]}""" to AiFailureCategory.INVALID_RESPONSE,
        )
        for ((raw, expected) in cases) {
            val client = GeminiModelClient(FixtureGeminiApi(raw))
            val error = runCatching { client.generateJson("test-key", request()) }.exceptionOrNull()
            assertEquals(expected.name, expected, (error as? AiProviderRequestException)?.category)
            assertFalse(error?.message.orEmpty().contains("private"))
        }
    }

    @Test fun successfulResponseSkipsThoughtParts() = runTest {
        val client = GeminiModelClient(FixtureGeminiApi("""{"candidates":[{"finishReason":"STOP","content":{"parts":[
            {"text":"private thought","thought":true},{"text":"{\"items\":[]}"}
        ]}}]}"""))
        assertEquals("""{"items":[]}""", client.generateJson("test-key", request()).rawJson)
    }

    @Test fun oversizedMealResponseHasInvalidCategoryWithoutKeepingRawText() = runTest {
        val text = """{"private":"${"x".repeat(64_001)}"}"""
        val raw = Gson().toJson(GeminiResponse(listOf(GeminiResponse.Candidate(
            content = GeminiResponse.Content(listOf(GeminiResponse.Part(text))),
        ))))
        val error = runCatching { GeminiModelClient(FixtureGeminiApi(raw)).generateJson("test-key", request()) }.exceptionOrNull()
        assertEquals(AiFailureCategory.INVALID_RESPONSE, (error as? AiProviderRequestException)?.category)
        assertFalse(error?.message.orEmpty().contains("private"))
    }

    @Test fun nullMealEnvelopeHasSafeInvalidCategory() = runTest {
        // Exercise Retrofit's actual null-body failure, before a DTO can reach the client.
        for ((status, body) in listOf(200 to "null", 204 to "")) {
            val transport = OkHttpClient.Builder().addInterceptor { chain ->
                okhttp3.Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
                    .code(status).message("Fixture")
                    .body(body.toResponseBody("application/json".toMediaType())).build()
            }.build()
            try {
                val api = Retrofit.Builder().baseUrl("http://localhost/").client(transport)
                    .addConverterFactory(GsonConverterFactory.create()).build().create(GeminiApi::class.java)
                val error = runCatching { GeminiModelClient(api).generateJson("test-key", request()) }.exceptionOrNull()
                assertEquals("HTTP $status", AiFailureCategory.INVALID_RESPONSE, (error as? AiProviderRequestException)?.category)
            } finally {
                transport.dispatcher.executorService.shutdownNow()
                transport.connectionPool.evictAll()
            }
        }
    }

    private fun request() = AiRouteRequest(AiFeature.MEAL_SCAN, "fixture", "meal_scan", AiJsonSchemas.mealScan, 0, byteArrayOf(1))
    private class FixtureGeminiApi(private val raw: String) : GeminiApi {
        override suspend fun generateContent(model: String, apiKey: String, request: GeminiRequest): GeminiResponse =
            Gson().fromJson(raw, GeminiResponse::class.java)
    }
}
