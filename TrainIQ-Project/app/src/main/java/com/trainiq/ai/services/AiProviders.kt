package com.trainiq.ai.services

import com.trainiq.BuildConfig
import com.trainiq.core.datastore.AiPreferences
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
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
                        imageUrl = "data:image/jpeg;base64,${android.util.Base64.encodeToString(imageBytes, android.util.Base64.NO_WRAP)}",
                    ),
                )
            }
        }
        val response = openAiApi.createResponse(
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
        return AiRouteResult(
            providerUsed = provider,
            model = OPENAI_DEFAULT_MODEL,
            rawJson = response.output
                .flatMap { it.content }
                .mapNotNull { it.text }
                .joinToString(" ")
                .ifBlank { response.outputText.orEmpty() },
        )
    }
}

@Singleton
class AiProviderRouter @Inject constructor(
    private val geminiClient: GeminiModelClient,
    private val openAiClient: OpenAiModelClient,
    private val aiUsageGate: AiUsageGate,
) : AiJsonGenerator {
    private val providerThrottles = AiProvider.entries.associateWith { AiFeatureThrottle() }

    override suspend fun generateJson(request: AiRouteRequest): AiRouteResult {
        val settings = aiUsageGate.currentSettings()
        return routeAiProviderRequest(
            settings = settings,
            request = request,
            throttleForProvider = providerThrottles::getValue,
            clientFor = { provider -> if (provider == AiProvider.GEMINI) geminiClient else openAiClient },
        )
    }
}

internal class AiProviderUnavailableException(
    val failures: List<String>,
) : RuntimeException("Geen AI-provider beschikbaar.")

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
    val mapped = asAiRateLimitExceptionIfNeeded()
    if (mapped is AiRateLimitException || mapped is AiFeatureThrottledException || mapped is AiTimeoutException) return true
    return this is HttpException && code() in listOf(408, 409, 425, 429, 500, 502, 503, 504)
}

internal suspend fun routeAiProviderRequest(
    settings: AiPreferences,
    request: AiRouteRequest,
    throttleForProvider: (AiProvider) -> AiFeatureThrottle = { AiFeatureThrottle() },
    clientFor: (AiProvider) -> AiModelClient,
): AiRouteResult {
    val failures = mutableListOf<String>()
    for (provider in settings.preferredProvider.orderedProviders()) {
        val apiKey = settings.apiKeyFor(provider) ?: continue
        val client = clientFor(provider)
        try {
            return callAiWithBoundedRetry(feature = request.feature, throttle = throttleForProvider(provider)) {
                client.generateJson(apiKey, request)
            }.copy(fallbackFailures = failures.toList())
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            failures += "${provider.name}:${error::class.simpleName.orEmpty()}"
            if (!error.isTransientAiProviderFailure()) throw error
        }
    }
    throw AiProviderUnavailableException(failures)
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
