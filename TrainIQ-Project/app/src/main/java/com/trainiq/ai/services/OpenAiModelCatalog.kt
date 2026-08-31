package com.trainiq.ai.services

import com.trainiq.data.model.OpenAiModelDescriptor
import com.trainiq.data.remote.OpenAiApi
import java.security.MessageDigest
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import retrofit2.Response

internal object OpenAiModelSelectionPolicy {
    // Candidates are deliberately pinned IDs, verified for Responses, structured output, and image input.
    private val budgetCandidates = listOf("gpt-5.6-luna", "gpt-5.4-mini")

    val fingerprint: String = budgetCandidates.joinToString(",", prefix = "runtime-model-policy-v1:")

    fun select(models: List<OpenAiModelDescriptor>, excluded: Set<String> = emptySet()): String? {
        val available = models
            .asSequence()
            .filter { it.id.isNotBlank() && it.id !in excluded }
            .filter { descriptor -> descriptor.shutdownDate?.let(::isStillAvailable) ?: true }
            .map { it.id }
            .toSet()
        return budgetCandidates.firstOrNull { it in available }
    }

    private fun isStillAvailable(shutdownDate: String): Boolean =
        runCatching { LocalDate.parse(shutdownDate) >= LocalDate.now() }.getOrDefault(false)
}

internal class OpenAiModelDiscoveryException(
    val response: Response<*>,
) : RuntimeException("OpenAI-modeldetectie mislukt.")

internal class OpenAiNoUsableModelException : RuntimeException("Geen compatibel OpenAI-model beschikbaar.")

@Singleton
class OpenAiModelCatalog internal constructor(
    private val openAiApi: OpenAiApi,
    private val nowMillis: () -> Long,
) {
    @Inject
    constructor(openAiApi: OpenAiApi) : this(openAiApi, System::currentTimeMillis)
    private val cachedModels = mutableMapOf<String, CachedModels>()

    suspend fun select(apiKey: String, excluded: Set<String> = emptySet()): String {
        val fingerprint = apiKey.fingerprint()
        val models = cachedModels[fingerprint]
            ?.takeIf { nowMillis() - it.fetchedAtMillis < CacheTtlMillis }
            ?.models
            ?: refresh(apiKey, fingerprint)
        return OpenAiModelSelectionPolicy.select(models, excluded) ?: throw OpenAiNoUsableModelException()
    }

    fun invalidate(apiKey: String) {
        cachedModels.remove(apiKey.fingerprint())
    }

    private suspend fun refresh(apiKey: String, fingerprint: String): List<OpenAiModelDescriptor> {
        val response = openAiApi.listModels("Bearer $apiKey")
        if (!response.isSuccessful) throw OpenAiModelDiscoveryException(response)
        val models = response.body()?.data.orEmpty()
        cachedModels[fingerprint] = CachedModels(models = models, fetchedAtMillis = nowMillis())
        return models
    }

    private fun String.fingerprint(): String = MessageDigest.getInstance("SHA-256")
        .digest(toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }

    private data class CachedModels(
        val models: List<OpenAiModelDescriptor>,
        val fetchedAtMillis: Long,
    )

    private companion object {
        const val CacheTtlMillis = 6 * 60 * 60 * 1_000L
    }
}
