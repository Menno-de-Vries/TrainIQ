package com.trainiq.core.diagnostics

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.trainiq.BuildConfig
import java.io.ByteArrayOutputStream
import java.util.Locale
import java.util.Random
import java.util.zip.GZIPOutputStream
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

data class TelemetryConfig(
    val enabled: Boolean = false,
    val endpointUrl: String? = null,
    val apiToken: String? = null,
    val sampleRate: Double = 0.0,
    val uploadOnWifiOnly: Boolean = true,
    val maxBatchSize: Int = 20,
    val flushIntervalMillis: Long = 60_000L,
    val perfettoEnabled: Boolean = false,
    val crashContextEnabled: Boolean = true,
) {
    fun canUpload(userOptIn: Boolean): Boolean =
        enabled &&
            userOptIn &&
            !endpointUrl.isNullOrBlank() &&
            sampleRate > 0.0 &&
            maxBatchSize > 0

    companion object {
        fun fromBuildConfig(): TelemetryConfig = TelemetryConfig(
            enabled = BuildConfig.TELEMETRY_ENABLED,
            endpointUrl = BuildConfig.TELEMETRY_ENDPOINT_URL.takeIf { it.isNotBlank() },
            apiToken = BuildConfig.TELEMETRY_API_TOKEN.takeIf { it.isNotBlank() },
            sampleRate = BuildConfig.TELEMETRY_SAMPLE_RATE.coerceIn(0.0, 1.0),
            uploadOnWifiOnly = BuildConfig.TELEMETRY_UPLOAD_WIFI_ONLY,
            maxBatchSize = BuildConfig.TELEMETRY_MAX_BATCH_SIZE.coerceAtLeast(1),
            flushIntervalMillis = BuildConfig.TELEMETRY_FLUSH_INTERVAL_MILLIS.coerceAtLeast(1_000L),
            perfettoEnabled = BuildConfig.TELEMETRY_PERFETTO_ENABLED,
            crashContextEnabled = BuildConfig.TELEMETRY_CRASH_CONTEXT_ENABLED,
        )
    }
}

sealed interface TelemetryPayload {
    data class Event(val event: TelemetryEvent) : TelemetryPayload
    data class Summary(val summary: PerformanceSessionSummary) : TelemetryPayload
}

interface TelemetryExporter {
    fun enqueue(payload: TelemetryPayload)
    fun flush()
    fun setUserOptIn(enabled: Boolean)
}

class NoOpTelemetryExporter : TelemetryExporter {
    override fun enqueue(payload: TelemetryPayload) = Unit
    override fun flush() = Unit
    override fun setUserOptIn(enabled: Boolean) = Unit
}

class LocalDebugExporter : TelemetryExporter {
    private val queue = ArrayDeque<TelemetryPayload>()
    private var userOptIn = false

    override fun enqueue(payload: TelemetryPayload) {
        if (userOptIn) queue.addLast(payload)
    }

    override fun flush() {
        queue.clear()
    }

    override fun setUserOptIn(enabled: Boolean) {
        userOptIn = enabled
        if (!enabled) queue.clear()
    }

    fun queuedPayloads(): List<TelemetryPayload> = queue.toList()
}

object DiagnosticsPrivacyGuard {
    private val allowedEvents = setOf(
        "app.startup",
        "screen.show",
        "tap",
        "state.change",
        "performance.summary",
    )
    private val allowedAttributes = setOf(
        "build_type",
        "screen",
        "target",
        "state",
        "value",
        "sessionId",
        "frame.p99_ms",
        "jank.rate",
        "frame.p50_ms",
        "frame.p90_ms",
        "frames",
        "janky_frames",
    )
    private val allowedCrashKeys = setOf("av", "bt", "sid", "screen", "frame.p99_ms", "jank.rate", "bc")

    fun sanitize(event: TelemetryEvent): TelemetryEvent? {
        if (event.name !in allowedEvents) return null
        val attributes = event.attributes
            .filterKeys { it in allowedAttributes }
            .entries
            .take(MaxContextKeys)
            .associate { (key, value) -> key to sanitizeValue(value) }
        return TelemetryEvent(name = event.name, attributes = attributes)
    }

    fun sanitizeCrashContext(context: Map<String, String>): Map<String, String> =
        context
            .filterKeys { it in allowedCrashKeys }
            .entries
            .take(MaxContextKeys)
            .associate { (key, value) -> key to sanitizeValue(value) }

    private fun sanitizeValue(value: String): String =
        value
            .replace(Regex("(?i)(bearer|token|api[_-]?key)\\s+[^\\s;|]+"), "$1 redacted")
            .take(MaxValueLength)

    private const val MaxValueLength = 160
    private const val MaxContextKeys = 64
}

interface TelemetrySampler {
    fun shouldSample(sampleRate: Double): Boolean
}

class RandomTelemetrySampler(
    private val random: Random = Random(),
) : TelemetrySampler {
    override fun shouldSample(sampleRate: Double): Boolean =
        random.nextDouble() < sampleRate.coerceIn(0.0, 1.0)
}

interface TelemetryNetworkPolicy {
    fun canUpload(uploadOnWifiOnly: Boolean): Boolean
}

class AndroidTelemetryNetworkPolicy(
    private val context: Context,
) : TelemetryNetworkPolicy {
    override fun canUpload(uploadOnWifiOnly: Boolean): Boolean {
        if (!uploadOnWifiOnly) return true
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }
}

data class TelemetryRetryPolicy(
    val initialBackoffMillis: Long = 1_000L,
    val maxBackoffMillis: Long = 60_000L,
) {
    fun next(current: Long): Long =
        if (current <= 0L) initialBackoffMillis else (current * 2L).coerceAtMost(maxBackoffMillis)
}

data class TelemetryHttpRequest(
    val endpointUrl: String,
    val body: ByteArray,
    val gzip: Boolean,
    val authorization: String?,
) {
    val bodyText: String
        get() = if (gzip) ungzip(body) else body.toString(Charsets.UTF_8)
}

interface TelemetrySender {
    fun send(request: TelemetryHttpRequest): Boolean
}

class OkHttpTelemetrySender(
    private val client: OkHttpClient,
) : TelemetrySender {
    override fun send(request: TelemetryHttpRequest): Boolean {
        val body = request.body.toRequestBody("application/json".toMediaType())
        val httpRequest = Request.Builder()
            .url(request.endpointUrl)
            .post(body)
            .apply {
                if (request.gzip) header("Content-Encoding", "gzip")
                request.authorization?.let { header("Authorization", it) }
            }
            .build()
        return runCatching {
            client.newCall(httpRequest).execute().use { response -> response.isSuccessful }
        }.getOrDefault(false)
    }
}

class RecordingTelemetrySender(
    private var failuresBeforeSuccess: Int = 0,
) : TelemetrySender {
    val requests = mutableListOf<TelemetryHttpRequest>()

    override fun send(request: TelemetryHttpRequest): Boolean {
        requests += request
        if (failuresBeforeSuccess > 0) {
            failuresBeforeSuccess--
            return false
        }
        return true
    }
}

class HttpTelemetryExporter(
    private val config: TelemetryConfig,
    private val sender: TelemetrySender,
    private val sampler: TelemetrySampler = RandomTelemetrySampler(),
    private val networkPolicy: TelemetryNetworkPolicy,
    private val retryPolicy: TelemetryRetryPolicy = TelemetryRetryPolicy(),
) : TelemetryExporter {
    private val queue = ArrayDeque<TelemetryPayload>()
    private var userOptIn = false
    var nextBackoffMillis: Long = 0L
        private set

    override fun enqueue(payload: TelemetryPayload) {
        if (!config.canUpload(userOptIn)) return
        if (!sampler.shouldSample(config.sampleRate)) return
        sanitized(payload)?.let { queue.addLast(it) }
        if (queue.size >= config.maxBatchSize) flush()
    }

    override fun flush() {
        if (!config.canUpload(userOptIn)) {
            queue.clear()
            return
        }
        if (!networkPolicy.canUpload(config.uploadOnWifiOnly) || queue.isEmpty()) return
        val batch = buildList {
            repeat(config.maxBatchSize.coerceAtMost(queue.size)) {
                add(queue.removeFirst())
            }
        }
        val request = TelemetryHttpRequest(
            endpointUrl = config.endpointUrl.orEmpty(),
            body = gzip(batch.toJson().toByteArray(Charsets.UTF_8)),
            gzip = true,
            authorization = config.apiToken?.takeIf { it.isNotBlank() }?.let { "Bearer $it" },
        )
        if (sender.send(request)) {
            nextBackoffMillis = 0L
        } else {
            nextBackoffMillis = retryPolicy.next(nextBackoffMillis)
            if (sender.send(request)) {
                return
            }
            batch.asReversed().forEach { queue.addFirst(it) }
        }
    }

    override fun setUserOptIn(enabled: Boolean) {
        userOptIn = enabled
        if (!enabled) queue.clear()
    }

    private fun sanitized(payload: TelemetryPayload): TelemetryPayload? = when (payload) {
        is TelemetryPayload.Event -> DiagnosticsPrivacyGuard.sanitize(payload.event)?.let(TelemetryPayload::Event)
        is TelemetryPayload.Summary -> TelemetryPayload.Summary(payload.summary)
    }
}

class ExportingTelemetry(
    private val exporter: TelemetryExporter,
) : Telemetry {
    override fun event(name: String, attributes: Map<String, String>) {
        exporter.enqueue(TelemetryPayload.Event(TelemetryEvent(name = name, attributes = attributes)))
    }
}

class DelegatingCrashReporter(
    private val config: TelemetryConfig,
    private val delegate: CrashReporter?,
) : CrashReporter {
    var recordedDelegations: Int = 0
        private set

    override fun recordNonFatal(throwable: Throwable, context: Map<String, String>) {
        if (!config.crashContextEnabled || delegate == null) return
        delegate.recordNonFatal(throwable, DiagnosticsPrivacyGuard.sanitizeCrashContext(context))
        recordedDelegations++
    }
}

private fun List<TelemetryPayload>.toJson(): String =
    joinToString(prefix = "{\"batch\":[", postfix = "]}", separator = ",") { payload ->
        when (payload) {
            is TelemetryPayload.Event -> payload.event.toJson()
            is TelemetryPayload.Summary -> payload.summary.toTelemetryEvent().toJson()
        }
    }

private fun PerformanceSessionSummary.toTelemetryEvent(): TelemetryEvent = TelemetryEvent(
    name = "performance.summary",
    attributes = mapOf(
        "sessionId" to sessionId,
        "screen" to screenName,
        "frames" to frameCount.toString(),
        "janky_frames" to jankyFrameCount.toString(),
        "frame.p50_ms" to p50FrameMs.formatMetric(),
        "frame.p90_ms" to p90FrameMs.formatMetric(),
        "frame.p99_ms" to p99FrameMs.formatMetric(),
        "jank.rate" to jankyFrameRatio.formatMetric(),
    ),
)

private fun TelemetryEvent.toJson(): String =
    "{\"name\":\"${name.escapeJson()}\",\"attributes\":{${attributes.toJsonProperties()}}}"

private fun Map<String, String>.toJsonProperties(): String =
    entries.joinToString(",") { (key, value) -> "\"${key.escapeJson()}\":\"${value.escapeJson()}\"" }

private fun Double.formatMetric(): String = String.format(Locale.US, "%.2f", this)

private fun String.escapeJson(): String =
    buildString {
        this@escapeJson.forEach { char ->
            when (char) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> append(char)
            }
        }
    }

private fun gzip(bytes: ByteArray): ByteArray {
    val output = ByteArrayOutputStream()
    GZIPOutputStream(output).use { it.write(bytes) }
    return output.toByteArray()
}

private fun ungzip(bytes: ByteArray): String =
    java.util.zip.GZIPInputStream(bytes.inputStream()).use { input ->
        input.readBytes().toString(Charsets.UTF_8)
    }
