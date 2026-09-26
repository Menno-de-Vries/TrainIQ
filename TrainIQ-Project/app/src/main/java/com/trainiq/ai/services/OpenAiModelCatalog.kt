package com.trainiq.ai.services

import com.trainiq.data.model.OpenAiModelDescriptor
import com.trainiq.data.remote.OpenAiApi
import java.security.MessageDigest
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Response

internal object OpenAiModelSelectionPolicy {
    // Candidates are deliberately pinned IDs, verified for Responses, structured output, and image input.
    private val budgetCandidates = listOf("gpt-6-luna", "gpt-5.6-luna", "gpt-5.4-mini")

    val fingerprint: String = budgetCandidates.joinToString(",", prefix = "runtime-model-policy-v2-medium-meal-routine:")

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

internal class OpenAiModelDiscoveryException(response: Response<*>) : RuntimeException("OpenAI-modeldetectie mislukt.") {
    // Retrofit error bodies are one-shot streams. Keep a bounded replayable copy for a short outage backoff.
    private val rawResponse = response.raw()
    private val contentType = response.errorBody()?.contentType()
    private val errorBody = response.errorBody()?.charStream()?.use { reader ->
        val buffer = CharArray(8_192)
        val count = reader.read(buffer)
        if (count > 0) String(buffer, 0, count) else ""
    }.orEmpty()
    val response: Response<*> get() = Response.error<Any>(errorBody.toResponseBody(contentType), rawResponse)
}

internal class OpenAiNoUsableModelException : RuntimeException("Geen compatibel OpenAI-model beschikbaar.")

@Singleton
class OpenAiModelCatalog internal constructor(
    private val openAiApi: OpenAiApi,
    private val nowMillis: () -> Long,
) {
    @Inject
    constructor(openAiApi: OpenAiApi) : this(openAiApi, System::currentTimeMillis)
    private val cacheMutex = Mutex()
    private val cachedModels = mutableMapOf<String, CachedModels>()
    private val rejectedModels = mutableMapOf<String, RejectedModels>()
    private val cachedFailures = mutableMapOf<String, CachedFailure>()

    suspend fun select(apiKey: String, excluded: Set<String> = emptySet()): String {
        val fingerprint = apiKey.fingerprint()
        cacheMutex.lock()
        try {
            val cached = cachedModels[fingerprint]
            val models = cached
                ?.takeIf { it.isFresh(nowMillis()) }
                ?.models
                ?: run {
                    cachedFailures[fingerprint]?.takeIf { it.isFresh(nowMillis()) }?.let { throw it.error }
                    refresh(apiKey, fingerprint)
                }
            val rejected = rejectedModels[fingerprint]
                ?.takeIf { it.isFresh(nowMillis()) }
                ?.models
                .orEmpty()
            return OpenAiModelSelectionPolicy.select(models, excluded + rejected)
                ?: throw OpenAiNoUsableModelException()
        } finally {
            cacheMutex.unlock()
        }
    }

    suspend fun invalidate(apiKey: String, rejectedModel: String? = null) {
        val fingerprint = apiKey.fingerprint()
        cacheMutex.withLock {
            if (rejectedModel != null) {
                val previous = rejectedModels[fingerprint]
                    ?.takeIf { it.isFresh(nowMillis()) }
                    ?.models
                    .orEmpty()
                // A concurrent call may have already refreshed after the same access failure.
                if (rejectedModel in previous) return@withLock
                rejectedModels[fingerprint] = RejectedModels(previous + rejectedModel, nowMillis())
            }
            cachedModels.remove(fingerprint)
            cachedFailures.remove(fingerprint)
        }
    }

    private suspend fun refresh(apiKey: String, fingerprint: String): List<OpenAiModelDescriptor> {
        try {
            val response = openAiApi.listModels("Bearer $apiKey")
            if (!response.isSuccessful) throw OpenAiModelDiscoveryException(response)
            val models = response.body()?.data.orEmpty()
            cachedModels[fingerprint] = CachedModels(models = models, fetchedAtMillis = nowMillis())
            cachedFailures.remove(fingerprint)
            return models
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            cachedFailures[fingerprint] = CachedFailure(error, nowMillis())
            throw error
        }
    }

    private fun String.fingerprint(): String = MessageDigest.getInstance("SHA-256")
        .digest(toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }

    private data class CachedModels(
        val models: List<OpenAiModelDescriptor>,
        val fetchedAtMillis: Long,
    ) {
        fun isFresh(now: Long): Boolean {
            val age = now - fetchedAtMillis
            val ttl = if (OpenAiModelSelectionPolicy.select(models) == PreferredModel) PreferredCacheTtlMillis else FallbackCacheTtlMillis
            return age >= 0 && age < ttl
        }
    }

    private data class RejectedModels(
        val models: Set<String>,
        val rejectedAtMillis: Long,
    ) {
        fun isFresh(now: Long): Boolean {
            val age = now - rejectedAtMillis
            return age >= 0 && age < RejectedModelTtlMillis
        }
    }

    private data class CachedFailure(val error: Exception, val failedAtMillis: Long) {
        fun isFresh(now: Long): Boolean = now >= failedAtMillis && now - failedAtMillis < FailureCacheTtlMillis
    }

    private companion object {
        const val PreferredModel = "gpt-6-luna"
        const val PreferredCacheTtlMillis = 6 * 60 * 60 * 1_000L
        const val FallbackCacheTtlMillis = 15 * 60 * 1_000L
        const val RejectedModelTtlMillis = 15 * 60 * 1_000L
        const val FailureCacheTtlMillis = 30_000L
    }
}
