package com.trainiq.ai.services

import com.trainiq.data.model.OpenAiError
import com.trainiq.data.model.OpenAiIncompleteDetails
import com.trainiq.data.model.OpenAiOutput
import com.trainiq.data.model.OpenAiOutputContent
import com.trainiq.data.model.OpenAiResponse
import com.trainiq.data.model.OpenAiResponseRequest
import com.trainiq.data.remote.OpenAiApi
import kotlinx.coroutines.test.runTest
import java.io.IOException
import java.net.SocketTimeoutException
import okhttp3.Headers.Companion.toHeaders
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class OpenAiModelClientTest {
    @Test
    fun generateJson_completedStructuredOutput_returnsOpenAiResult() = runTest {
        val api = FakeOpenAiApi(
            Response.success(
                OpenAiResponse(
                    status = "completed",
                    output = listOf(
                        OpenAiOutput(
                            content = listOf(
                                OpenAiOutputContent(type = "output_text", text = "{\"summary\":\"Goed herstel.\"}"),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val result = OpenAiModelClient(api).generateJson("synthetic-secret", weeklyRequest())

        assertEquals(AiProvider.OPENAI, result.providerUsed)
        assertEquals("{\"summary\":\"Goed herstel.\"}", result.rawJson)
        assertEquals(1, api.calls)
    }

    @Test
    fun generateJson_httpAuthenticationFailure_preservesOnlySafeMetadata() = runTest {
        val api = FakeOpenAiApi(
            errorResponse(
                status = 401,
                code = "invalid_api_key",
                requestId = "req_safe_123",
                rawMessage = "synthetic-secret prompt-private",
            ),
        )

        val error = runCatching {
            OpenAiModelClient(api).generateJson("synthetic-secret", weeklyRequest())
        }.exceptionOrNull()

        assertFailure(error, category = "AUTHENTICATION", status = 401, code = "invalid_api_key", requestId = "req_safe_123")
        assertFalse(error.toString().contains("synthetic-secret"))
        assertFalse(error.toString().contains("prompt-private"))
    }

    @Test
    fun generateJson_quota429_isDistinctFromTemporaryRateLimit() = runTest {
        val quotaCodes = listOf(
            "credit_balance_exhausted",
            "organization_usage_limit_exceeded",
            "organization_spend_limit_exceeded",
            "project_spend_limit_exceeded",
        )
        val rateLimit = runCatching {
            OpenAiModelClient(
                FakeOpenAiApi(errorResponse(429, "rate_limit_exceeded", "req_rate", retryAfter = "3")),
            ).generateJson("synthetic-secret", weeklyRequest())
        }.exceptionOrNull()

        quotaCodes.forEach { code ->
            val quota = runCatching {
                OpenAiModelClient(
                    FakeOpenAiApi(errorResponse(429, code, "req_quota")),
                ).generateJson("synthetic-secret", weeklyRequest())
            }.exceptionOrNull()
            assertFailure(quota, category = "QUOTA_BILLING", status = 429, code = code, requestId = "req_quota")
        }
        assertFailure(rateLimit, category = "TEMPORARY_RATE_LIMIT", status = 429, code = "rate_limit_exceeded", requestId = "req_rate")
        assertEquals(3_000L, failureField(rateLimit!!, "retryAfterMillis"))
    }

    @Test
    fun generateJson_failedIncompleteRefusalAndNoOutput_areNotSuccessfulResponses() = runTest {
        val cases = listOf(
            OpenAiResponse(status = "failed", error = OpenAiError(code = "server_error")) to "SERVICE_FAILURE",
            OpenAiResponse(status = "incomplete", incompleteDetails = OpenAiIncompleteDetails(reason = "max_output_tokens")) to "INCOMPLETE_RESPONSE",
            OpenAiResponse(
                status = "completed",
                output = listOf(OpenAiOutput(content = listOf(OpenAiOutputContent(type = "refusal", refusal = "cannot comply")))),
            ) to "REFUSAL",
            OpenAiResponse(status = "completed") to "INVALID_RESPONSE",
        )

        cases.forEach { (body, expectedCategory) ->
            val error = runCatching {
                OpenAiModelClient(FakeOpenAiApi(Response.success(body))).generateJson("synthetic-secret", weeklyRequest())
            }.exceptionOrNull()

            assertFailure(error, category = expectedCategory, status = 200, code = body.error?.code, requestId = null)
        }
    }

    @Test
    fun generateJson_requestServiceNetworkAndTimeoutFailures_areClassifiedWithoutRetryMetadata() = runTest {
        val requestFailure = runCatching {
            OpenAiModelClient(FakeOpenAiApi(errorResponse(400, "invalid_json_schema", "req_request")))
                .generateJson("synthetic-secret", weeklyRequest())
        }.exceptionOrNull()
        val serviceFailure = runCatching {
            OpenAiModelClient(FakeOpenAiApi(errorResponse(503, "service_unavailable", "req_service")))
                .generateJson("synthetic-secret", weeklyRequest())
        }.exceptionOrNull()
        val networkFailure = runCatching {
            OpenAiModelClient(ThrowingOpenAiApi(IOException("offline prompt-private")))
                .generateJson("synthetic-secret", weeklyRequest())
        }.exceptionOrNull()
        val timeoutFailure = runCatching {
            OpenAiModelClient(ThrowingOpenAiApi(SocketTimeoutException("timed out prompt-private")))
                .generateJson("synthetic-secret", weeklyRequest())
        }.exceptionOrNull()

        assertFailure(requestFailure, "REQUEST_CONFIGURATION", 400, "invalid_json_schema", "req_request")
        assertFailure(serviceFailure, "SERVICE_FAILURE", 503, "service_unavailable", "req_service")
        assertFailure(networkFailure, "NETWORK", status = null, code = null, requestId = null)
        assertFailure(timeoutFailure, "TIMEOUT", status = null, code = null, requestId = null)
        assertFalse(networkFailure.toString().contains("prompt-private"))
        assertFalse(timeoutFailure.toString().contains("prompt-private"))
    }

    private fun assertFailure(
        error: Throwable?,
        category: String,
        status: Int?,
        code: String?,
        requestId: String?,
    ) {
        assertNotNull(error)
        assertEquals("AiProviderRequestException", error!!::class.simpleName)
        assertEquals(category, (failureField(error, "category") as Enum<*>).name)
        assertEquals(status, failureField(error, "httpStatus"))
        assertEquals(code, failureField(error, "errorCode"))
        assertEquals(requestId, failureField(error, "requestId"))
    }

    private fun failureField(error: Throwable, name: String): Any? =
        error.javaClass.getDeclaredField(name).apply { isAccessible = true }.get(error)

    private class FakeOpenAiApi(
        private val response: Response<OpenAiResponse>,
    ) : OpenAiApi {
        var calls: Int = 0

        override suspend fun createResponse(
            authorization: String,
            request: OpenAiResponseRequest,
        ): Response<OpenAiResponse> {
            calls += 1
            return response
        }
    }

    private class ThrowingOpenAiApi(
        private val error: IOException,
    ) : OpenAiApi {
        override suspend fun createResponse(
            authorization: String,
            request: OpenAiResponseRequest,
        ): Response<OpenAiResponse> = throw error
    }

    private fun errorResponse(
        status: Int,
        code: String,
        requestId: String,
        rawMessage: String = "safe",
        retryAfter: String? = null,
    ): Response<OpenAiResponse> {
        val headers = buildMap {
            put("x-request-id", requestId)
            retryAfter?.let { put("Retry-After", it) }
        }.toHeaders()
        val rawResponse = okhttp3.Response.Builder()
            .request(Request.Builder().url("https://api.openai.com/v1/responses").build())
            .protocol(Protocol.HTTP_1_1)
            .code(status)
            .message("OpenAI error")
            .headers(headers)
            .build()
        val body = """{"error":{"code":"$code","message":"$rawMessage"}}"""
            .toResponseBody("application/json".toMediaType())
        return Response.error(body, rawResponse)
    }

    private fun weeklyRequest() = AiRouteRequest(
        feature = AiFeature.WEEKLY_REPORT,
        prompt = "Geef JSON",
        schemaName = "weekly_report",
        responseJsonSchema = AiJsonSchemas.weeklyReport,
        thinkingBudget = 1000,
    )
}
