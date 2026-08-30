package com.trainiq.core.diagnostics

import android.app.Activity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiagnosticsTrackerTest {
    @Test
    fun aiFailure_recordsStructuredSafeMetadataWithoutSensitiveContent() {
        val breadcrumbs = BreadcrumbRingBuffer()
        val telemetry = RecordingTelemetry()
        val tracker = DiagnosticsTracker(breadcrumbs, telemetry, FakePerformanceSessionMonitor)
        val attributes = mapOf(
            "provider" to "openai",
            "feature" to "weekly_report",
            "category" to "authentication",
            "http_status" to "401",
            "error_code" to "invalid_api_key",
            "error_type" to "invalid_request_error",
            "request_id" to "req_safe_123",
            "attempt" to "1",
            "duration_ms" to "725",
            "Authorization" to "Bearer synthetic-secret",
            "prompt" to "private prompt",
        )

        tracker.aiFailure(attributes)

        val event = telemetry.recordedEvents().single()
        assertEquals("ai.remote.failure", event.name)
        assertEquals(
            attributes.filterKeys { it !in setOf("Authorization", "prompt") },
            event.attributes,
        )
        assertTrue(breadcrumbs.snapshot().single().message.contains("req_safe_123"))
        val recorded = event.toString() + breadcrumbs.snapshot().single().message
        assertFalse(recorded.contains("Authorization"))
        assertFalse(recorded.contains("synthetic-secret"))
        assertFalse(recorded.contains("prompt"))
        assertFalse(recorded.contains("private prompt"))
        assertFalse(recorded.contains("image"))
    }

    private object FakePerformanceSessionMonitor : PerformanceSessionMonitor {
        override val sessionId: String = "session"
        override fun start(activity: Activity) = Unit
        override fun setEnabled(enabled: Boolean) = Unit
        override fun updateScreen(screenName: String) = Unit
        override fun stop() = Unit
        override fun latestSummary(): PerformanceSessionSummary = PerformanceSessionSummary.Empty
    }
}
