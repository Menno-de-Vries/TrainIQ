package com.trainiq.core.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TelemetryExportPipelineTest {
    @Test
    fun defaultConfig_disablesUploadWithoutExplicitEndpointAndOptIn() {
        val config = TelemetryConfig()

        assertFalse(config.canUpload(userOptIn = true))
        assertFalse(config.canUpload(userOptIn = false))
        assertEquals(0.0, config.sampleRate, 0.0)
        assertTrue(config.uploadOnWifiOnly)
        assertFalse(config.perfettoEnabled)
    }

    @Test
    fun config_requiresEnabledEndpointAndOptInBeforeUpload() {
        val config = TelemetryConfig(
            enabled = true,
            endpointUrl = "https://telemetry.example.test/events",
            sampleRate = 1.0,
        )

        assertTrue(config.canUpload(userOptIn = true))
        assertFalse(config.canUpload(userOptIn = false))
        assertFalse(config.copy(endpointUrl = "").canUpload(userOptIn = true))
    }

    @Test
    fun privacyGuard_allowsOnlyTechnicalKeysAndLimitsContext() {
        val event = TelemetryEvent(
            name = "screen.show",
            attributes = mapOf(
                "screen" to "Home",
                "mealName" to "Personal lunch",
                "routineName" to "My private routine",
                "target" to "Nav:Training",
            ),
        )

        val sanitized = DiagnosticsPrivacyGuard.sanitize(event)

        assertEquals("screen.show", sanitized?.name)
        assertEquals(mapOf("screen" to "Home", "target" to "Nav:Training"), sanitized?.attributes)
    }

    @Test
    fun exporterQueuesBatchesAndDoesNotSendWhenDisabled() {
        val sender = RecordingTelemetrySender()
        val exporter = HttpTelemetryExporter(
            config = TelemetryConfig(enabled = false, endpointUrl = "https://telemetry.example.test", sampleRate = 1.0),
            sender = sender,
            sampler = FixedTelemetrySampler(allowed = true),
            networkPolicy = FixedNetworkPolicy(canUpload = true),
        )

        exporter.setUserOptIn(true)
        exporter.enqueue(TelemetryPayload.Event(TelemetryEvent("screen.show", mapOf("screen" to "Home"))))
        exporter.flush()

        assertEquals(0, sender.requests.size)
    }

    @Test
    fun exporterBatchesCompressesAndRetriesWithBackoff() {
        val sender = RecordingTelemetrySender(failuresBeforeSuccess = 1)
        val exporter = HttpTelemetryExporter(
            config = TelemetryConfig(
                enabled = true,
                endpointUrl = "https://telemetry.example.test/events",
                apiToken = "token",
                sampleRate = 1.0,
                uploadOnWifiOnly = true,
                maxBatchSize = 2,
                flushIntervalMillis = 1_000L,
            ),
            sender = sender,
            sampler = FixedTelemetrySampler(allowed = true),
            networkPolicy = FixedNetworkPolicy(canUpload = true),
            retryPolicy = TelemetryRetryPolicy(initialBackoffMillis = 100L, maxBackoffMillis = 1_000L),
        )

        exporter.setUserOptIn(true)
        exporter.enqueue(TelemetryPayload.Event(TelemetryEvent("screen.show", mapOf("screen" to "Home"))))
        exporter.enqueue(TelemetryPayload.Event(TelemetryEvent("tap", mapOf("target" to "Nav:Training"))))

        assertEquals(2, sender.requests.size)
        assertTrue(sender.requests.last().gzip)
        assertEquals("Bearer token", sender.requests.last().authorization)
        assertEquals(100L, exporter.nextBackoffMillis)
        assertTrue(sender.requests.last().bodyText.contains("screen.show"))
        assertTrue(sender.requests.last().bodyText.contains("tap"))
    }

    @Test
    fun crashReporterAdapterSanitizesContextAndToleratesMissingProvider() {
        val crashReporter = DelegatingCrashReporter(
            config = TelemetryConfig(enabled = true, crashContextEnabled = true),
            delegate = null,
        )

        crashReporter.recordNonFatal(
            throwable = IllegalStateException("boom"),
            context = (1..80).associate { "key$it" to "value$it" } + mapOf(
                "sid" to "session",
                "screen" to "ActiveWorkout",
                "frame.p99_ms" to "55.0",
                "jank.rate" to "0.4",
                "note" to "private note",
            ),
        )

        assertEquals(0, crashReporter.recordedDelegations)
    }
}

private class FixedTelemetrySampler(private val allowed: Boolean) : TelemetrySampler {
    override fun shouldSample(sampleRate: Double): Boolean = allowed
}

private class FixedNetworkPolicy(private val canUpload: Boolean) : TelemetryNetworkPolicy {
    override fun canUpload(uploadOnWifiOnly: Boolean): Boolean = canUpload
}

