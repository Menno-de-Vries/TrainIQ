package com.trainiq.domain.model

/** Explicit metric volume only. Mass is deliberately never interpreted as volume. */
fun explicitVolumeMl(amount: String, unit: String): Double? {
    val multiplier = when (unit.trim().lowercase()) { "ml" -> 1.0; "cl" -> 10.0; "l" -> 1000.0; else -> return null }
    val result = (amount.trim().replace(',', '.').toDoubleOrNull() ?: return null) * multiplier
    return result.takeIf { it.isFinite() && it > 0 && it <= 100_000 }
}

data class HydrationRecord(
    val id: String,
    val timestamp: Long,
    val volumeMl: Double,
    val mealId: Long?,
    val label: String,
)
