package com.trainiq.ai.services

import com.google.gson.Gson
import com.google.gson.JsonParser
import com.trainiq.BuildConfig
import com.trainiq.core.datastore.AiPreferences
import com.trainiq.core.diagnostics.DiagnosticsTracker
import com.trainiq.data.model.OpenAiErrorEnvelope
import java.io.IOException
import java.net.SocketTimeoutException
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import okhttp3.ResponseBody
import retrofit2.HttpException

enum class AiProvider(val displayName: String) {
    GEMINI("Gemini 2.5 Flash"),
    OPENAI("OpenAI"),
}

enum class AiProviderPreference(val storageValue: String, val label: String) {
    GEMINI_FIRST("gemini_first", "Gemini eerst"),
    OPENAI_FIRST("openai_first", "OpenAI eerst");

    fun orderedProviders(): List<AiProvider> = when (this) {
        GEMINI_FIRST -> listOf(AiProvider.GEMINI, AiProvider.OPENAI)
        OPENAI_FIRST -> listOf(AiProvider.OPENAI, AiProvider.GEMINI)
    }

    companion object {
        fun fromStorageValue(value: String): AiProviderPreference? =
            entries.firstOrNull { it.storageValue == value }
    }
}

data class AiRouteRequest(
    val feature: AiFeature,
    val prompt: String,
    val schemaName: String,
    val responseJsonSchema: Map<String, Any?>,
    val thinkingBudget: Int,
    val imageJpegBytes: ByteArray? = null,
)

data class AiRouteResult(
    val providerUsed: AiProvider,
    val model: String,
    val rawJson: String,
    val fallbackFailures: List<String> = emptyList(),
)

interface AiModelClient {
    val provider: AiProvider
    suspend fun generateJson(apiKey: String, request: AiRouteRequest): AiRouteResult
}

interface AiJsonGenerator {
    suspend fun generateJson(request: AiRouteRequest): AiRouteResult
}

@Singleton
class GeminiModelClient @Inject constructor(
    private val geminiApi: com.trainiq.data.remote.GeminiApi,
) : AiModelClient {
    override val provider: AiProvider = AiProvider.GEMINI

    override suspend fun generateJson(apiKey: String, request: AiRouteRequest): AiRouteResult {
        val parts = buildList {
            add(com.trainiq.data.model.GeminiRequest.Part(text = request.prompt))
            request.imageJpegBytes?.let { imageBytes ->
                add(
                    com.trainiq.data.model.GeminiRequest.Part(
                        inlineData = com.trainiq.data.model.GeminiRequest.InlineData(
                            mimeType = "image/jpeg",
                            data = java.util.Base64.getEncoder().encodeToString(imageBytes),
                        ),
                    ),
                )
            }
        }
        val response = geminiApi.generateContent(
            model = GEMINI_FLASH_MODEL,
            apiKey = apiKey,
            request = com.trainiq.data.model.GeminiRequest(
                contents = listOf(com.trainiq.data.model.GeminiRequest.Content(parts = parts)),
                generationConfig = com.trainiq.data.model.GeminiRequest.GenerationConfig(
                    responseMimeType = "application/json",
                    responseJsonSchema = request.responseJsonSchema,
                    thinkingConfig = com.trainiq.data.model.GeminiRequest.ThinkingConfig(
                        includeThoughts = false,
                        thinkingBudget = request.thinkingBudget,
                    ),
                ),
            ),
        )
        return AiRouteResult(
            providerUsed = provider,
            model = GEMINI_FLASH_MODEL,
            rawJson = response.candidates.firstOrNull()?.content?.parts?.joinToString(" ") { it.text }.orEmpty(),
        )
    }
}

@Singleton
class OpenAiModelClient @Inject constructor(
    private val openAiApi: com.trainiq.data.remote.OpenAiApi,
) : AiModelClient {
    override val provider: AiProvider = AiProvider.OPENAI

    override suspend fun generateJson(apiKey: String, request: AiRouteRequest): AiRouteResult {
        val inputContent = buildList {
            add(com.trainiq.data.model.OpenAiInputContent(type = "input_text", text = request.prompt))
            request.imageJpegBytes?.let { imageBytes ->
                add(
                    com.trainiq.data.model.OpenAiInputContent(
                        type = "input_image",
                        imageUrl = "data:image/jpeg;base64,${java.util.Base64.getEncoder().encodeToString(imageBytes)}",
                    ),
                )
            }
        }
        val httpResponse = try {
            openAiApi.createResponse(
                authorization = "Bearer $apiKey",
                request = com.trainiq.data.model.OpenAiResponseRequest(
                    model = OPENAI_DEFAULT_MODEL,
                    input = listOf(com.trainiq.data.model.OpenAiInputMessage(role = "user", content = inputContent)),
                    text = com.trainiq.data.model.OpenAiTextConfig(
                        format = com.trainiq.data.model.OpenAiTextFormat(
                            name = request.schemaName,
                            schema = request.responseJsonSchema.toOpenAiStrictSchema(),
                        ),
                    ),
                ),
            )
        } catch (error: CancellationException) {
            throw error
        } catch (error: SocketTimeoutException) {
            throw openAiFailure(request, AiFailureCategory.TIMEOUT, cause = error)
        } catch (error: IOException) {
            throw openAiFailure(request, AiFailureCategory.NETWORK, cause = error)
        }
        val requestId = sanitizeOpenAiMetadata(httpResponse.headers()["x-request-id"])
        if (!httpResponse.isSuccessful) {
            val parsedError = parseOpenAiError(httpResponse.errorBody()?.readBoundedText())
            val retryAfterMillis = parseRetryAfterMillis(httpResponse.headers()["Retry-After"])
            throw openAiFailure(
                request = request,
                category = classifyOpenAiFailure(
                    httpStatus = httpResponse.code(),
                    errorCode = parsedError.code,
                    errorType = parsedError.type,
                    retryAfterMillis = retryAfterMillis,
                ),
                httpStatus = httpResponse.code(),
                errorCode = parsedError.code,
                errorType = parsedError.type,
                requestId = requestId,
                retryAfterMillis = retryAfterMillis,
            )
        }
        val response = httpResponse.body()
            ?: throw openAiFailure(request, AiFailureCategory.INVALID_RESPONSE, httpStatus = httpResponse.code(), requestId = requestId)
        val status = response.status?.lowercase()
        val responseErrorCode = sanitizeOpenAiMetadata(response.error?.code)
        val responseErrorType = sanitizeOpenAiMetadata(response.error?.type)
        if (status == "failed") {
            throw openAiFailure(
                request = request,
                category = classifyOpenAiFailure(httpResponse.code(), responseErrorCode, responseErrorType)
                    .takeUnless { it == AiFailureCategory.UNKNOWN }
                    ?: AiFailureCategory.SERVICE_FAILURE,
                httpStatus = httpResponse.code(),
                errorCode = responseErrorCode,
                errorType = responseErrorType,
                requestId = requestId,
            )
        }
        if (status == "incomplete" || response.incompleteDetails != null) {
            throw openAiFailure(
                request = request,
                category = AiFailureCategory.INCOMPLETE_RESPONSE,
                httpStatus = httpResponse.code(),
                errorCode = responseErrorCode,
                errorType = responseErrorType,
                requestId = requestId,
            )
        }
        if (status != "completed") {
            throw openAiFailure(
                request = request,
                category = AiFailureCategory.INVALID_RESPONSE,
                httpStatus = httpResponse.code(),
                errorCode = responseErrorCode,
                errorType = responseErrorType,
                requestId = requestId,
            )
        }
        if (response.output.flatMap { it.content }.any { it.type == "refusal" || !it.refusal.isNullOrBlank() }) {
            throw openAiFailure(
                request = request,
                category = AiFailureCategory.REFUSAL,
                httpStatus = httpResponse.code(),
                errorCode = responseErrorCode,
                errorType = responseErrorType,
                requestId = requestId,
            )
        }
        val output = response.output
            .flatMap { it.content }
            .filter { it.type == "output_text" }
            .mapNotNull { it.text?.trim() }
            .joinToString("\n")
            .trim()
            .ifBlank { response.outputText?.trim().orEmpty() }
        if (output.isBlank() || runCatching { JsonParser.parseString(output).asJsonObject }.isFailure) {
            throw openAiFailure(
                request = request,
                category = AiFailureCategory.INVALID_RESPONSE,
                httpStatus = httpResponse.code(),
                errorCode = responseErrorCode,
                errorType = responseErrorType,
                requestId = requestId,
            )
        }
        return AiRouteResult(
            providerUsed = provider,
            model = OPENAI_DEFAULT_MODEL,
            rawJson = output,
        )
    }
}

private val OpenAiQuotaCodes = setOf(
    "credit_balance_exhausted",
    "insufficient_quota",
    "organization_usage_limit_exceeded",
    "organization_spend_limit_exceeded",
    "project_spend_limit_exceeded",
)

private val OpenAiAuthenticationCodes = setOf("invalid_api_key", "invalid_authentication")
private val OpenAiAccessCodes = setOf("permission_denied", "model_not_found", "project_not_found")
private val OpenAiRateLimitCodes = setOf("rate_limit_exceeded")
private val OpenAiRequestErrorCodes = setOf("invalid_json_schema", "invalid_request", "invalid_request_error")

private fun classifyOpenAiFailure(
    httpStatus: Int,
    errorCode: String?,
    errorType: String? = null,
    retryAfterMillis: Long? = null,
): AiFailureCategory = when {
    httpStatus == 401 || errorCode in OpenAiAuthenticationCodes || errorType in OpenAiAuthenticationCodes -> AiFailureCategory.AUTHENTICATION
    httpStatus == 403 || errorCode in OpenAiAccessCodes || errorType in OpenAiAccessCodes -> AiFailureCategory.ACCESS
    errorCode in OpenAiQuotaCodes || errorType in OpenAiQuotaCodes -> AiFailureCategory.QUOTA_BILLING
    errorCode in OpenAiRateLimitCodes || errorType in OpenAiRateLimitCodes || (httpStatus == 429 && retryAfterMillis != null) -> AiFailureCategory.TEMPORARY_RATE_LIMIT
    httpStatus == 429 -> AiFailureCategory.UNCLASSIFIED_LIMIT
    errorCode in OpenAiRequestErrorCodes || errorType in OpenAiRequestErrorCodes -> AiFailureCategory.REQUEST_CONFIGURATION
    httpStatus == 408 -> AiFailureCategory.TIMEOUT
    httpStatus in listOf(400, 404, 409, 422) -> AiFailureCategory.REQUEST_CONFIGURATION
    httpStatus in 500..599 || errorCode in setOf("server_error", "service_unavailable") || errorType in setOf("server_error", "service_unavailable") -> AiFailureCategory.SERVICE_FAILURE
    else -> AiFailureCategory.UNKNOWN
}

private data class ParsedOpenAiError(val code: String? = null, val type: String? = null)

private fun parseOpenAiError(rawBody: String?): ParsedOpenAiError {
    val boundedBody = rawBody ?: return ParsedOpenAiError()
    val error = runCatching {
        Gson().fromJson(boundedBody, OpenAiErrorEnvelope::class.java).error
    }.getOrNull()
    return ParsedOpenAiError(
        code = sanitizeOpenAiMetadata(error?.code),
        type = sanitizeOpenAiMetadata(error?.type),
    )
}

private fun ResponseBody.readBoundedText(): String? = runCatching {
    charStream().use { reader ->
        val result = StringBuilder()
        val buffer = CharArray(4_096)
        while (result.length < MaxOpenAiErrorBodyChars) {
            val count = reader.read(buffer, 0, minOf(buffer.size, MaxOpenAiErrorBodyChars - result.length))
            if (count <= 0) break
            result.append(buffer, 0, count)
        }
        result.toString()
    }
}.getOrNull()

private fun parseRetryAfterMillis(value: String?, nowMillis: Long = System.currentTimeMillis()): Long? {
    val trimmed = value?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    val delayMillis = trimmed.toLongOrNull()?.let { seconds ->
        if (seconds < 0L) null else seconds.coerceAtMost(MaxRetryAfterMillis / 1_000L) * 1_000L
    } ?: runCatching {
        ZonedDateTime.parse(trimmed, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().toEpochMilli() - nowMillis
    }.getOrNull()?.coerceAtLeast(0L)?.coerceAtMost(MaxRetryAfterMillis)
    return delayMillis
}

private fun sanitizeOpenAiMetadata(value: String?): String? =
    value?.trim()
        ?.take(MaxOpenAiMetadataChars)
        ?.map { char -> if (char.isLetterOrDigit() || char in "-_.:") char else '_' }
        ?.joinToString("")
        ?.takeIf { it.isNotBlank() }

private fun openAiFailure(
    request: AiRouteRequest,
    category: AiFailureCategory,
    httpStatus: Int? = null,
    errorCode: String? = null,
    errorType: String? = null,
    requestId: String? = null,
    retryAfterMillis: Long? = null,
    cause: Throwable? = null,
) = AiProviderRequestException(
    provider = AiProvider.OPENAI,
    feature = request.feature,
    category = category,
    httpStatus = httpStatus,
    errorCode = sanitizeOpenAiMetadata(errorCode),
    errorType = sanitizeOpenAiMetadata(errorType),
    requestId = sanitizeOpenAiMetadata(requestId),
    retryAfterMillis = retryAfterMillis,
    cause = cause,
)

private const val MaxOpenAiErrorBodyChars = 64_000
private const val MaxOpenAiMetadataChars = 128
private const val MaxRetryAfterMillis = 86_400_000L

@Singleton
class AiProviderRouter @Inject constructor(
    private val geminiClient: GeminiModelClient,
    private val openAiClient: OpenAiModelClient,
    private val aiUsageGate: AiUsageGate,
    private val diagnosticsTracker: DiagnosticsTracker,
) : AiJsonGenerator {
    private val providerThrottles = AiProvider.entries.associateWith { AiFeatureThrottle() }

    override suspend fun generateJson(request: AiRouteRequest): AiRouteResult {
        val settings = aiUsageGate.currentSettings()
        return routeAiProviderRequest(
            settings = settings,
            request = request,
            throttleForProvider = providerThrottles::getValue,
            clientFor = { provider -> if (provider == AiProvider.GEMINI) geminiClient else openAiClient },
            onFailureDiagnostic = diagnosticsTracker::aiFailure,
        )
    }
}

internal class AiProviderUnavailableException(
    val failures: List<String>,
    val hasRecoverableFailure: Boolean = false,
    val primaryFailure: AiProviderRequestException? = null,
    val terminalFailure: AiProviderRequestException? = null,
) : RuntimeException(
    terminalFailure?.message ?: primaryFailure?.message ?: "Geen AI-provider beschikbaar.",
    primaryFailure ?: terminalFailure,
)

internal const val GEMINI_FLASH_MODEL = "gemini-2.5-flash"
internal val OPENAI_DEFAULT_MODEL: String = BuildConfig.OPENAI_MODEL

internal fun AiPreferences.apiKeyFor(provider: AiProvider): String? =
    when (provider) {
        AiProvider.GEMINI -> geminiApiKey
        AiProvider.OPENAI -> openAiApiKey
    }.takeIf { enabled && it.isNotBlank() }

internal fun AiPreferences.hasAnyReadyProvider(): Boolean =
    enabled && (geminiApiKey.isNotBlank() || openAiApiKey.isNotBlank())

private fun Throwable.isTransientAiProviderFailure(): Boolean {
    if (this is AiProviderRequestException) {
        return category in setOf(
            AiFailureCategory.TEMPORARY_RATE_LIMIT,
            AiFailureCategory.TIMEOUT,
            AiFailureCategory.NETWORK,
            AiFailureCategory.SERVICE_FAILURE,
        )
    }
    val mapped = asAiRateLimitExceptionIfNeeded()
    if (mapped is AiRateLimitException || mapped is AiFeatureThrottledException || mapped is AiTimeoutException) return true
    return this is HttpException && code() in listOf(408, 409, 425, 429, 500, 502, 503, 504)
}

private fun Throwable.toSafeProviderFailure(
    provider: AiProvider,
    feature: AiFeature,
): AiProviderRequestException {
    if (this is AiProviderRequestException) return this
    val category = when {
        this is AiRateLimitException || this is AiFeatureThrottledException -> AiFailureCategory.TEMPORARY_RATE_LIMIT
        this is AiTimeoutException -> AiFailureCategory.TIMEOUT
        this is HttpException && code() == 401 -> AiFailureCategory.AUTHENTICATION
        this is HttpException && code() == 403 -> AiFailureCategory.ACCESS
        this is HttpException && code() in setOf(400, 404, 422) -> AiFailureCategory.REQUEST_CONFIGURATION
        this is HttpException && code() == 408 -> AiFailureCategory.TIMEOUT
        this is HttpException && code() == 429 -> AiFailureCategory.TEMPORARY_RATE_LIMIT
        this is HttpException && code() in 500..599 -> AiFailureCategory.SERVICE_FAILURE
        this is IOException -> AiFailureCategory.NETWORK
        else -> AiFailureCategory.UNKNOWN
    }
    return AiProviderRequestException(
        provider = provider,
        feature = feature,
        category = category,
        httpStatus = (this as? HttpException)?.code(),
        retryAfterMillis = (this as? AiFeatureThrottledException)?.retryAfterMillis,
    )
}

internal suspend fun routeAiProviderRequest(
    settings: AiPreferences,
    request: AiRouteRequest,
    throttleForProvider: (AiProvider) -> AiFeatureThrottle = { AiFeatureThrottle() },
    clientFor: (AiProvider) -> AiModelClient,
    onFailureDiagnostic: (Map<String, String>) -> Unit = {},
): AiRouteResult {
    val failures = mutableListOf<String>()
    var hasRecoverableFailure = false
    var primaryFailure: AiProviderRequestException? = null
    val configuredProviders = settings.preferredProvider.orderedProviders()
        .mapNotNull { provider -> settings.apiKeyFor(provider)?.let { apiKey -> provider to apiKey } }
    val providers = if (settings.allowCrossProviderFallback) configuredProviders else configuredProviders.take(1)
    for ((provider, apiKey) in providers) {
        val client = clientFor(provider)
        try {
            val result = when (provider) {
                AiProvider.GEMINI -> callAiWithBoundedRetry(feature = request.feature, throttle = throttleForProvider(provider)) {
                    client.generateJson(apiKey, request)
                }
                AiProvider.OPENAI -> callOpenAiWithBoundedRetry(feature = request.feature, throttle = throttleForProvider(provider)) {
                    client.generateJson(apiKey, request)
                }
            }
            return result.copy(fallbackFailures = failures.toList())
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            val safeFailure = error.toSafeProviderFailure(provider, request.feature)
            if (primaryFailure == null) primaryFailure = safeFailure
            if (error is AiProviderRequestException) {
                onFailureDiagnostic(error.safeDiagnosticAttributes())
            }
            failures += "${provider.name}:${error::class.simpleName.orEmpty()}"
            if (!error.isTransientAiProviderFailure()) {
                if (hasRecoverableFailure) {
                    throw AiProviderUnavailableException(
                        failures = failures,
                        hasRecoverableFailure = true,
                        primaryFailure = primaryFailure,
                        terminalFailure = safeFailure,
                    )
                }
                throw error
            }
            hasRecoverableFailure = true
        }
    }
    throw AiProviderUnavailableException(failures, hasRecoverableFailure, primaryFailure)
}

@Suppress("UNCHECKED_CAST")
internal fun Map<String, Any?>.toOpenAiStrictSchema(): Map<String, Any?> {
    fun strict(value: Any?): Any? = when (value) {
        is Map<*, *> -> {
            val mapped = value.entries.associate { (key, child) -> key.toString() to strict(child) }.toMutableMap()
            mapped.remove("minimum")
            mapped.remove("maximum")
            if (mapped["type"] == "object") {
                val properties = mapped["properties"] as? Map<String, Any?> ?: emptyMap()
                mapped["additionalProperties"] = false
                mapped["required"] = properties.keys.toList()
            }
            mapped
        }
        is List<*> -> value.map(::strict)
        else -> value
    }
    return strict(this) as Map<String, Any?>
}
