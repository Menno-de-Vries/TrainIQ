package com.trainiq.ai.services

import com.google.gson.Gson
import com.google.gson.JsonParser
import com.trainiq.data.model.OpenAiError
import com.trainiq.data.model.OpenAiIncompleteDetails
import com.trainiq.data.model.OpenAiOutput
import com.trainiq.data.model.OpenAiOutputContent
import com.trainiq.data.model.OpenAiResponse
import com.trainiq.data.model.OpenAiResponseRequest
import com.trainiq.data.remote.OpenAiApi
import com.trainiq.core.datastore.AiPreferences
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
    fun allSixFeaturesRouteTheActualResponsesContractWithImagesOnlyForPhotoFeatures() = runTest {
        featureContracts().forEach { contract ->
            val api = FakeOpenAiApi(
                Response.success(OpenAiResponse(status = "completed", outputText = "{\"ok\":true}")),
            )
            val client = OpenAiModelClient(api)

            val result = routeAiProviderRequest(
                settings = openAiOnlySettings(),
                request = contract.request,
                clientFor = { client },
            )

            assertEquals(AiProvider.OPENAI, result.providerUsed)
            assertEquals("Bearer synthetic-secret", api.lastAuthorization)
            val json = JsonParser.parseString(Gson().toJson(api.lastRequest)).asJsonObject
            assertEquals(OPENAI_DEFAULT_MODEL, json["model"].asString)
            val content = json["input"].asJsonArray.single().asJsonObject["content"].asJsonArray
            assertEquals("input_text", content[0].asJsonObject["type"].asString)
            assertEquals("prompt-${contract.request.feature.name}", content[0].asJsonObject["text"].asString)
            assertEquals(contract.hasImage, content.any { it.asJsonObject["type"].asString == "input_image" })
            val format = json["text"].asJsonObject["format"].asJsonObject
            assertEquals("json_schema", format["type"].asString)
            assertEquals(contract.request.schemaName, format["name"].asString)
            assertTrue(format["strict"].asBoolean)
            assertFalse(json.has("response_format"))
        }
    }

    @Test
    fun allSixFeaturesPropagateTypedOpenAiBoundaryFailuresThroughRouter() = runTest {
        featureContracts().forEach { contract ->
            val api = FakeOpenAiApi(
                errorResponse(
                    status = 403,
                    code = "model_not_found",
                    requestId = "req_${contract.request.feature.name.lowercase()}",
                    rawMessage = "The requested model is not enabled",
                ),
            )

            val error = runCatching {
                routeAiProviderRequest(
                    settings = openAiOnlySettings(),
                    request = contract.request,
                    clientFor = { OpenAiModelClient(api) },
                )
            }.exceptionOrNull()

            assertFailure(
                error = error,
                category = "MODEL_ACCESS",
                status = 403,
                code = "model_not_found",
                requestId = "req_${contract.request.feature.name.lowercase()}",
            )
            assertEquals(1, api.calls)
        }
    }

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
    fun generateJson_typeOnlyInsufficientQuota429_isBillingFailureWithoutRateThrottleClassification() = runTest {
        val error = runCatching {
            OpenAiModelClient(
                FakeOpenAiApi(
                    errorResponse(
                        status = 429,
                        code = null,
                        type = "insufficient_quota",
                        requestId = "req_type_quota",
                    ),
                ),
            ).generateJson("synthetic-secret", weeklyRequest())
        }.exceptionOrNull()

        assertFailure(error, category = "QUOTA_BILLING", status = 429, code = null, requestId = "req_type_quota")
        assertEquals("insufficient_quota", failureField(error!!, "errorType"))
    }

    @Test
    fun generateJson_access403_isNotReportedAsInvalidApiKey() = runTest {
        val error = runCatching {
            OpenAiModelClient(
                FakeOpenAiApi(errorResponse(403, "permission_denied", "req_access")),
            ).generateJson("synthetic-secret", weeklyRequest())
        }.exceptionOrNull()

        assertFailure(error, category = "ACCESS", status = 403, code = "permission_denied", requestId = "req_access")
        assertTrue(error?.message.orEmpty().contains("projectrechten"))
        assertFalse(error?.message.orEmpty().contains("API-sleutel"))
    }

    @Test
    fun generateJson_accessFailuresDistinguishProjectModelAndResponsesPermission() = runTest {
        val cases = listOf(
            Triple("project_not_found", "Project membership is required", "PROJECT_ACCESS"),
            Triple("model_not_found", "The requested model is not enabled", "MODEL_ACCESS"),
            Triple("permission_denied", "Missing write permission for /v1/responses", "ENDPOINT_PERMISSION"),
        )

        cases.forEach { (code, rawMessage, expectedCategory) ->
            val error = runCatching {
                OpenAiModelClient(
                    FakeOpenAiApi(errorResponse(403, code, "req_access", rawMessage = rawMessage)),
                ).generateJson("synthetic-secret", weeklyRequest())
            }.exceptionOrNull()

            assertFailure(error, category = expectedCategory, status = 403, code = code, requestId = "req_access")
            assertFalse(error.toString().contains(rawMessage))
        }
    }

    @Test
    fun generateJson_unknown429WithoutRetryEvidence_isNonRetryableLimitFailure() = runTest {
        val error = runCatching {
            OpenAiModelClient(
                FakeOpenAiApi(errorResponse(429, null, "req_unknown_limit")),
            ).generateJson("synthetic-secret", weeklyRequest())
        }.exceptionOrNull()

        assertFailure(error, category = "UNCLASSIFIED_LIMIT", status = 429, code = null, requestId = "req_unknown_limit")
    }

    @Test
    fun generateJson_failedResponseUsesErrorTypeClassification() = runTest {
        val body = Gson().fromJson(
            """{"status":"failed","error":{"type":"insufficient_quota"}}""",
            OpenAiResponse::class.java,
        )
        val error = runCatching {
            OpenAiModelClient(FakeOpenAiApi(Response.success(body)))
                .generateJson("synthetic-secret", weeklyRequest())
        }.exceptionOrNull()

        assertFailure(error, category = "QUOTA_BILLING", status = 200, code = null, requestId = null)
        assertEquals("insufficient_quota", failureField(error!!, "errorType"))
    }

    @Test
    fun generateJson_failedResponseRequestErrorIsNotRecoverableServiceFailure() = runTest {
        val body = OpenAiResponse(
            status = "failed",
            error = OpenAiError(code = "invalid_json_schema", type = "invalid_request_error"),
        )
        val error = runCatching {
            OpenAiModelClient(FakeOpenAiApi(Response.success(body)))
                .generateJson("synthetic-secret", weeklyRequest())
        }.exceptionOrNull()

        assertFailure(error, category = "REQUEST_CONFIGURATION", status = 200, code = "invalid_json_schema", requestId = null)
        assertEquals("invalid_request_error", failureField(error!!, "errorType"))
        assertFalse(error.allowsDeterministicAiFallback())
    }

    @Test
    fun generateJson_serializesTheActualResponsesRequestContract() = runTest {
        val api = FakeOpenAiApi(
            Response.success(
                OpenAiResponse(status = "completed", outputText = "{\"summary\":\"ok\"}"),
            ),
        )
        val request = weeklyRequest().copy(imageJpegBytes = byteArrayOf(1, 2, 3))

        OpenAiModelClient(api).generateJson("synthetic-secret", request)

        assertEquals("Bearer synthetic-secret", api.lastAuthorization)
        val json = JsonParser.parseString(Gson().toJson(api.lastRequest)).asJsonObject
        assertEquals(OPENAI_DEFAULT_MODEL, json["model"].asString)
        val input = json["input"].asJsonArray.single().asJsonObject
        assertEquals("user", input["role"].asString)
        val content = input["content"].asJsonArray
        assertEquals("input_text", content[0].asJsonObject["type"].asString)
        assertEquals("Geef JSON", content[0].asJsonObject["text"].asString)
        assertEquals("input_image", content[1].asJsonObject["type"].asString)
        assertTrue(content[1].asJsonObject["image_url"].asString.startsWith("data:image/jpeg;base64,"))
        val format = json["text"].asJsonObject["format"].asJsonObject
        assertEquals("json_schema", format["type"].asString)
        assertEquals("weekly_report", format["name"].asString)
        assertTrue(format["strict"].asBoolean)
        assertFalse(json.has("response_format"))
        assertEquals(false, format["schema"].asJsonObject["additionalProperties"].asBoolean)
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
        var lastAuthorization: String? = null
        var lastRequest: OpenAiResponseRequest? = null

        override suspend fun createResponse(
            authorization: String,
            request: OpenAiResponseRequest,
        ): Response<OpenAiResponse> {
            calls += 1
            lastAuthorization = authorization
            lastRequest = request
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
        code: String?,
        requestId: String,
        rawMessage: String = "safe",
        retryAfter: String? = null,
        type: String? = null,
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
        val codeJson = code?.let { Gson().toJson(it) } ?: "null"
        val typeJson = type?.let { Gson().toJson(it) } ?: "null"
        val body = """{"error":{"code":$codeJson,"type":$typeJson,"message":${Gson().toJson(rawMessage)}}}"""
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

    private fun openAiOnlySettings() = AiPreferences(
        enabled = true,
        apiKey = "",
        preferredProvider = AiProviderPreference.OPENAI_FIRST,
        geminiApiKey = "",
        openAiApiKey = "synthetic-secret",
    )

    private fun featureContracts(): List<FeatureContract> = listOf(
        FeatureContract(AiFeature.MEAL_SCAN, "meal_scan", AiJsonSchemas.mealScan, hasImage = true),
        FeatureContract(AiFeature.BODY_MEASUREMENT_PHOTO, "body_measurement_photo", AiJsonSchemas.bodyMeasurementPhoto, hasImage = true),
        FeatureContract(AiFeature.WORKOUT_DEBRIEF, "workout_debrief", AiJsonSchemas.workoutDebrief),
        FeatureContract(AiFeature.GOAL_ADVICE, "goal_advice", AiJsonSchemas.goalAdvice),
        FeatureContract(AiFeature.WEEKLY_REPORT, "weekly_report", AiJsonSchemas.weeklyReport),
        FeatureContract(AiFeature.ROUTINE_GENERATION, "routine_generator", AiJsonSchemas.routineGenerator),
    )

    private data class FeatureContract(
        val request: AiRouteRequest,
        val hasImage: Boolean,
    ) {
        constructor(
            feature: AiFeature,
            schemaName: String,
            schema: Map<String, Any?>,
            hasImage: Boolean = false,
        ) : this(
            request = AiRouteRequest(
                feature = feature,
                prompt = "prompt-${feature.name}",
                schemaName = schemaName,
                responseJsonSchema = schema,
                thinkingBudget = if (hasImage) 0 else 1_000,
                imageJpegBytes = if (hasImage) byteArrayOf(1, 2, 3) else null,
            ),
            hasImage = hasImage,
        )
    }
}
