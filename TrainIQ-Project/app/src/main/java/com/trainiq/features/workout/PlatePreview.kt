package com.trainiq.features.workout

import com.trainiq.domain.model.StrengthCalculator

internal fun platePreviewWeight(input: String, suggestedWeight: Double?): Float? {
    val weight = if (input.isBlank()) suggestedWeight else input.trim().replace(',', '.').toDoubleOrNull()
    return weight?.takeIf { it.isFinite() && it in 0.0..StrengthCalculator.MaxWeightKg }?.toFloat()
}
