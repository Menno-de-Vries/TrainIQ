package com.trainiq.ai.services

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.TimeoutCancellationException
import retrofit2.HttpException

internal class AiRateLimitException : RuntimeException("AI-limiet bereikt, probeer later opnieuw.")
internal class AiFeatureThrottledException(
    feature: AiFeature,
    retryAfterMillis: Long,
) : RuntimeException("${feature.label.capitalizedLabel()} is tijdelijk gepauzeerd. Probeer over ${retryAfterMillis.toRetryDelayLabel()} opnieuw.")

internal class AiTimeoutException(
    feature: AiFeature,
) : RuntimeException("AI-aanroep voor ${feature.label} duurde te lang. Lokale fallback is gebruikt.")

enum class AiFeature(
    val label: String,
    val timeoutMillis: Long,
    val throttleCooldownMillis: Long,
) {
    MEAL_SCAN(label = "maaltijdscan", timeoutMillis = 20_000L, throttleCooldownMillis = 30_000L),
    WORKOUT_DEBRIEF(label = "workoutanalyse", timeoutMillis = 15_000L, throttleCooldownMillis = 45_000L),
    GOAL_ADVICE(label = "doeladvies", timeoutMillis = 20_000L, throttleCooldownMillis = 45_000L),
    WEEKLY_REPORT(label = "weekrapport", timeoutMillis = 20_000L, throttleCooldownMillis = 60_000L),
    ROUTINE_GENERATION(label = "routinesuggestie", timeoutMillis = 25_000L, throttleCooldownMillis = 60_000L),
}

internal class AiFeatureThrottle(
    private val nowMillis: () -> Long = { System.currentTimeMillis() },
) {
    private val throttledUntilByFeature = mutableMapOf<AiFeature, Long>()

    fun failureIfThrottled(feature: AiFeature): AiFeatureThrottledException? {
        val retryAfterMillis = (throttledUntilByFeature[feature] ?: return null) - nowMillis()
        return if (retryAfterMillis > 0L) {
            AiFeatureThrottledException(feature, retryAfterMillis)
        } else {
            throttledUntilByFeature.remove(feature)
            null
        }
    }

    fun recordRateLimit(feature: AiFeature) {
        throttledUntilByFeature[feature] = nowMillis() + feature.throttleCooldownMillis
    }
}

private val SharedAiFeatureThrottle = AiFeatureThrottle()

internal fun Throwable.asAiRateLimitExceptionIfNeeded(): Throwable =
    if (this is HttpException && code() == 429) AiRateLimitException() else this

internal fun Throwable.toAiUserMessage(defaultMessage: String): String = when (val mapped = asAiRateLimitExceptionIfNeeded()) {
    is AiRateLimitException -> mapped.message ?: defaultMessage
    is AiFeatureThrottledException -> mapped.message ?: defaultMessage
    is AiTimeoutException -> mapped.message ?: defaultMessage
    else -> mapped.message ?: defaultMessage
}

internal suspend fun <T> callGeminiWithBoundedRetry(
    feature: AiFeature,
    timeoutMillis: Long = feature.timeoutMillis,
    maxAttempts: Int = 2,
    initialBackoffMillis: Long = 350L,
    throttle: AiFeatureThrottle = SharedAiFeatureThrottle,
    block: suspend () -> T,
): T {
    return callAiWithBoundedRetry(feature, timeoutMillis, maxAttempts, initialBackoffMillis, throttle, block)
}

internal suspend fun <T> callAiWithBoundedRetry(
    feature: AiFeature,
    timeoutMillis: Long = feature.timeoutMillis,
    maxAttempts: Int = 2,
    initialBackoffMillis: Long = 350L,
    throttle: AiFeatureThrottle = SharedAiFeatureThrottle,
    block: suspend () -> T,
): T {
    throttle.failureIfThrottled(feature)?.let { throw it }
    var nextDelay = initialBackoffMillis
    var lastError: Throwable? = null
    repeat(maxAttempts.coerceAtLeast(1)) { attempt ->
        try {
            return withTimeout(timeoutMillis) {
                block()
            }
        } catch (error: TimeoutCancellationException) {
            throw AiTimeoutException(feature)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            lastError = error.asAiRateLimitExceptionIfNeeded()
            if (lastError !is AiRateLimitException || attempt == maxAttempts - 1) {
                if (lastError is AiRateLimitException) throttle.recordRateLimit(feature)
                throw lastError ?: error
            }
            delay(nextDelay)
            nextDelay = (nextDelay * 2).coerceAtMost(2_000L)
        }
    }
    if (lastError is AiRateLimitException) throttle.recordRateLimit(feature)
    throw lastError ?: AiRateLimitException()
}

private fun String.capitalizedLabel(): String =
    replaceFirstChar { char -> char.titlecase() }

private fun Long.toRetryDelayLabel(): String =
    if (this < 60_000L) {
        "${((this + 999L) / 1_000L).coerceAtLeast(1L)} seconden"
    } else {
        "${((this + 59_999L) / 60_000L).coerceAtLeast(1L)} minuten"
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
