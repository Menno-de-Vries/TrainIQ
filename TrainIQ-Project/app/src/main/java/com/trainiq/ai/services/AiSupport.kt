package com.trainiq.ai.services

import retrofit2.HttpException

internal const val GEMINI_FLASH_MODEL = "gemini-2.5-flash"

internal class AiRateLimitException : RuntimeException("AI-limiet bereikt, probeer later opnieuw.")

internal fun Throwable.asAiRateLimitExceptionIfNeeded(): Throwable =
    if (this is HttpException && code() == 429) AiRateLimitException() else this

internal fun Throwable.toAiUserMessage(defaultMessage: String): String = when (val mapped = asAiRateLimitExceptionIfNeeded()) {
    is AiRateLimitException -> mapped.message ?: defaultMessage
    else -> mapped.message ?: defaultMessage
}

internal fun List<String>.isUsableDutchAiText(): Boolean {
    val joined = joinToString(" ")
        .lowercase()
        .replace("\n", " ")
    if (joined.isBlank()) return false
    val englishSignals = listOf(
        " strong ",
        " session ",
        " workout ",
        " recovery ",
        " build ",
        " muscle ",
        " progressive ",
        " overload ",
        " keep ",
        " add weight ",
        " sleep ",
        " good form ",
    )
    return englishSignals.none { signal -> joined.contains(signal) }
}
