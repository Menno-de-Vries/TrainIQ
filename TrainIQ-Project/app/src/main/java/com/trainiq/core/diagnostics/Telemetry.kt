package com.trainiq.core.diagnostics

interface Telemetry {
    fun event(name: String, attributes: Map<String, String> = emptyMap())
    fun increment(name: String, value: Long = 1L, attributes: Map<String, String> = emptyMap()) {
        event(name, attributes + ("value" to value.toString()))
    }
}

object NoOpTelemetry : Telemetry {
    override fun event(name: String, attributes: Map<String, String>) = Unit
}

class RecordingTelemetry : Telemetry {
    private val events = mutableListOf<TelemetryEvent>()

    override fun event(name: String, attributes: Map<String, String>) {
        events += TelemetryEvent(name = name, attributes = attributes.toMap())
    }

    fun recordedEvents(): List<TelemetryEvent> = events.toList()

    fun clear() {
        events.clear()
    }
}

data class TelemetryEvent(
    val name: String,
    val attributes: Map<String, String>,
)
