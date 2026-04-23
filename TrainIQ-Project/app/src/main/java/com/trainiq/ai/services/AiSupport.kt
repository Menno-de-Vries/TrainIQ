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
