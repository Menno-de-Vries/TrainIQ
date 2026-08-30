package com.trainiq.ai.services

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.TimeoutCancellationException
import kotlin.random.Random
import retrofit2.HttpException

internal enum class AiFailureCategory {
    AUTHENTICATION,
    ACCESS,
    REQUEST_CONFIGURATION,
    TEMPORARY_RATE_LIMIT,
    QUOTA_BILLING,
    UNCLASSIFIED_LIMIT,
    TIMEOUT,
    NETWORK,
    SERVICE_FAILURE,
    INCOMPLETE_RESPONSE,
    REFUSAL,
    INVALID_RESPONSE,
    UNKNOWN,
}

internal class AiProviderRequestException(
    val provider: AiProvider,
    val feature: AiFeature,
    val category: AiFailureCategory,
    val httpStatus: Int? = null,
    val errorCode: String? = null,
    val errorType: String? = null,
    val requestId: String? = null,
    val retryAfterMillis: Long? = null,
    val attempt: Int? = null,
    val durationMillis: Long? = null,
    cause: Throwable? = null,
) : RuntimeException(category.safeUserMessage(provider), cause)

internal fun AiProviderRequestException.safeDiagnosticAttributes(): Map<String, String> = buildMap {
    put("provider", provider.name.lowercase())
    put("feature", feature.name.lowercase())
    put("category", category.name.lowercase())
    httpStatus?.let { put("http_status", it.toString()) }
    errorCode?.let { put("error_code", it) }
    errorType?.let { put("error_type", it) }
    requestId?.let { put("request_id", it) }
    retryAfterMillis?.let { put("retry_after_ms", it.toString()) }
    attempt?.let { put("attempt", it.toString()) }
    durationMillis?.let { put("duration_ms", it.toString()) }
}

internal fun Throwable.allowsDeterministicAiFallback(): Boolean = when (this) {
    is AiProviderRequestException -> category in setOf(
        AiFailureCategory.TEMPORARY_RATE_LIMIT,
        AiFailureCategory.TIMEOUT,
        AiFailureCategory.NETWORK,
        AiFailureCategory.SERVICE_FAILURE,
    )
    is AiProviderUnavailableException -> terminalFailure == null && (hasRecoverableFailure || failures.isEmpty())
    else -> true
}

private fun AiFailureCategory.safeUserMessage(provider: AiProvider): String = when (this) {
    AiFailureCategory.AUTHENTICATION -> "${provider.displayName} weigert de API-sleutel of projecttoegang. Controleer je AI-instellingen."
    AiFailureCategory.ACCESS -> "${provider.displayName} staat deze aanvraag niet toe. Controleer de provider- en projectrechten."
    AiFailureCategory.REQUEST_CONFIGURATION -> "TrainIQ kon geen geldige aanvraag naar ${provider.displayName} sturen. Werk de app bij of probeer later opnieuw."
    AiFailureCategory.TEMPORARY_RATE_LIMIT -> "${provider.displayName} is tijdelijk beperkt. Probeer later opnieuw."
    AiFailureCategory.QUOTA_BILLING -> "Het ${provider.displayName}-project heeft geen bruikbaar tegoed of heeft een gebruiks- of bestedingslimiet bereikt. Controleer billing en projectlimieten."
    AiFailureCategory.UNCLASSIFIED_LIMIT -> "${provider.displayName} wees de aanvraag af vanwege een limiet. Controleer billing en projectlimieten of probeer later opnieuw."
    AiFailureCategory.TIMEOUT -> "${provider.displayName} reageerde te langzaam. Controleer je verbinding en probeer opnieuw."
    AiFailureCategory.NETWORK -> "${provider.displayName} kon niet worden bereikt. Controleer je internetverbinding en probeer opnieuw."
    AiFailureCategory.SERVICE_FAILURE -> "${provider.displayName} is tijdelijk niet beschikbaar. Probeer later opnieuw."
    AiFailureCategory.INCOMPLETE_RESPONSE -> "${provider.displayName} gaf geen volledig antwoord. Probeer opnieuw."
    AiFailureCategory.REFUSAL -> "${provider.displayName} kon deze aanvraag niet beantwoorden. Pas de invoer aan en probeer opnieuw."
    AiFailureCategory.INVALID_RESPONSE -> "${provider.displayName} gaf geen bruikbaar antwoord. Probeer opnieuw."
    AiFailureCategory.UNKNOWN -> "${provider.displayName} kon deze aanvraag niet verwerken. Probeer later opnieuw."
}

internal class AiRateLimitException : RuntimeException("AI-limiet bereikt, probeer later opnieuw.")
internal class AiFeatureThrottledException(
    feature: AiFeature,
    val retryAfterMillis: Long,
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
    BODY_MEASUREMENT_PHOTO(label = "weegfoto", timeoutMillis = 20_000L, throttleCooldownMillis = 30_000L),
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

    fun recordRateLimit(feature: AiFeature, retryAfterMillis: Long? = null) {
        val cooldownMillis = maxOf(feature.throttleCooldownMillis, retryAfterMillis ?: 0L)
        throttledUntilByFeature[feature] = nowMillis() + cooldownMillis
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

internal fun Throwable.toSafeAiFallbackMessage(defaultMessage: String): String = when (this) {
    is AiProviderRequestException -> message ?: defaultMessage
    is AiProviderUnavailableException -> primaryFailure?.message ?: defaultMessage
    is AiRateLimitException, is AiFeatureThrottledException, is AiTimeoutException -> message ?: defaultMessage
    else -> defaultMessage
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

internal suspend fun <T> callOpenAiWithBoundedRetry(
    feature: AiFeature,
    timeoutMillis: Long = feature.timeoutMillis,
    maxAttempts: Int = 2,
    initialBackoffMillis: Long = 350L,
    throttle: AiFeatureThrottle,
    jitterMillis: (Long) -> Long = { maximum ->
        if (maximum <= 0L) 0L else Random.nextLong(maximum + 1L)
    },
    elapsedRealtimeMillis: () -> Long = { System.nanoTime() / 1_000_000L },
    block: suspend () -> T,
): T {
    throttle.failureIfThrottled(feature)?.let { throttled ->
        throw AiProviderRequestException(
            provider = AiProvider.OPENAI,
            feature = feature,
            category = AiFailureCategory.TEMPORARY_RATE_LIMIT,
            retryAfterMillis = throttled.retryAfterMillis,
            attempt = 0,
            durationMillis = 0L,
            cause = throttled,
        )
    }
    val startedAtMillis = elapsedRealtimeMillis()
    val totalAttempts = maxAttempts.coerceAtLeast(1)
    var currentAttempt = 0
    var lastRateLimit: AiProviderRequestException? = null
    return try {
        withTimeout(timeoutMillis) {
            repeat(totalAttempts) { attemptIndex ->
                currentAttempt = attemptIndex + 1
                try {
                    return@withTimeout block()
                } catch (error: CancellationException) {
                    throw error
                } catch (error: AiProviderRequestException) {
                    val enriched = error.withExecutionMetadata(
                        attempt = currentAttempt,
                        durationMillis = elapsedSince(startedAtMillis, elapsedRealtimeMillis),
                    )
                    if (enriched.category != AiFailureCategory.TEMPORARY_RATE_LIMIT) throw enriched
                    lastRateLimit = enriched
                    if (attemptIndex == totalAttempts - 1) {
                        throttle.recordRateLimit(feature, enriched.retryAfterMillis)
                        throw enriched
                    }
                    val retryDelay = enriched.retryAfterMillis ?: boundedOpenAiBackoffMillis(
                        attempt = attemptIndex,
                        initialBackoffMillis = initialBackoffMillis,
                        jitterMillis = jitterMillis,
                    )
                    val remainingBudgetMillis = timeoutMillis - elapsedSince(startedAtMillis, elapsedRealtimeMillis)
                    if (remainingBudgetMillis <= 0L || retryDelay >= remainingBudgetMillis) {
                        throttle.recordRateLimit(feature, enriched.retryAfterMillis)
                        throw enriched
                    }
                    delay(retryDelay)
                }
            }
            throw lastRateLimit ?: AiProviderRequestException(
                provider = AiProvider.OPENAI,
                feature = feature,
                category = AiFailureCategory.UNKNOWN,
            )
        }
    } catch (error: TimeoutCancellationException) {
        lastRateLimit?.let {
            throttle.recordRateLimit(feature, it.retryAfterMillis)
            throw it.withExecutionMetadata(
                attempt = currentAttempt,
                durationMillis = elapsedSince(startedAtMillis, elapsedRealtimeMillis),
            )
        }
        throw AiProviderRequestException(
            provider = AiProvider.OPENAI,
            feature = feature,
            category = AiFailureCategory.TIMEOUT,
            attempt = currentAttempt.coerceAtLeast(1),
            durationMillis = elapsedSince(startedAtMillis, elapsedRealtimeMillis),
            cause = error,
        )
    }
}

private fun AiProviderRequestException.withExecutionMetadata(
    attempt: Int,
    durationMillis: Long,
): AiProviderRequestException = AiProviderRequestException(
    provider = provider,
    feature = feature,
    category = category,
    httpStatus = httpStatus,
    errorCode = errorCode,
    errorType = errorType,
    requestId = requestId,
    retryAfterMillis = retryAfterMillis,
    attempt = attempt,
    durationMillis = durationMillis.coerceAtLeast(0L),
    cause = cause,
)

private fun elapsedSince(startedAtMillis: Long, elapsedRealtimeMillis: () -> Long): Long =
    (elapsedRealtimeMillis() - startedAtMillis).coerceAtLeast(0L)

private fun boundedOpenAiBackoffMillis(
    attempt: Int,
    initialBackoffMillis: Long,
    jitterMillis: (Long) -> Long,
): Long {
    val exponent = attempt.coerceIn(0, 10)
    val base = (initialBackoffMillis.coerceAtLeast(0L) * (1L shl exponent)).coerceAtMost(MaxOpenAiBackoffMillis)
    val jitterMaximum = minOf(base / 4L, MaxOpenAiBackoffMillis - base)
    return (base + jitterMillis(jitterMaximum).coerceIn(0L, jitterMaximum)).coerceAtMost(MaxOpenAiBackoffMillis)
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

private const val MaxOpenAiBackoffMillis = 2_000L

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
