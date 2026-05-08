package com.trainiq.ai.services

import kotlinx.coroutines.delay
import retrofit2.HttpException

internal const val GEMINI_FLASH_MODEL = "gemini-2.5-flash"

internal class AiRateLimitException : RuntimeException("AI-limiet bereikt, probeer later opnieuw.")

internal fun Throwable.asAiRateLimitExceptionIfNeeded(): Throwable =
    if (this is HttpException && code() == 429) AiRateLimitException() else this

internal fun Throwable.toAiUserMessage(defaultMessage: String): String = when (val mapped = asAiRateLimitExceptionIfNeeded()) {
    is AiRateLimitException -> mapped.message ?: defaultMessage
    else -> mapped.message ?: defaultMessage
}

internal suspend fun <T> callGeminiWithBoundedRetry(
    maxAttempts: Int = 2,
    initialBackoffMillis: Long = 350L,
    block: suspend () -> T,
): T {
    var nextDelay = initialBackoffMillis
    var lastError: Throwable? = null
    repeat(maxAttempts.coerceAtLeast(1)) { attempt ->
        try {
            return block()
        } catch (error: Throwable) {
            lastError = error.asAiRateLimitExceptionIfNeeded()
            if (lastError !is AiRateLimitException || attempt == maxAttempts - 1) {
                throw lastError ?: error
            }
            delay(nextDelay)
            nextDelay = (nextDelay * 2).coerceAtMost(2_000L)
        }
    }
    throw lastError ?: AiRateLimitException()
}

internal fun List<String>.isUsableDutchAiText(): Boolean {
    val joined = joinToString(" ")
        .lowercase()
        .replace("\n", " ")
    if (joined.isBlank()) return false
    val englishSignals = listOf(
        " build muscle ",
        " muscle ",
        " progressive ",
        " keep ",
        " add weight ",
        " sleep ",
        " good form ",
        " recovery is good ",
        " when recovery ",
    )
    return englishSignals.none { signal -> joined.contains(signal) }
}
