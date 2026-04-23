package com.trainiq.domain.model

import kotlin.math.roundToInt

object StrengthCalculator {
    private val defaultPlateOptions = listOf(20f, 10f, 5f, 2.5f, 1.25f)

    fun estimateOneRepMax(weight: Double, reps: Int): Double {
        if (weight <= 0.0 || reps <= 0) return 0.0
        return if (reps <= 10) {
            weight / (1.0278 - (0.0278 * reps))
        } else {
            weight * (1.0 + reps / 30.0)
        }
    }

    fun estimateRepsInReserve(rpe: Double): Int? {
        if (rpe <= 0.0) return null
        return (10.0 - rpe).roundToInt().coerceAtLeast(0)
    }

    fun calculatePlates(
        targetWeight: Float,
        barWeight: Float = 20f,
        availablePlates: List<Float> = defaultPlateOptions,
    ): List<Float> {
        if (targetWeight <= barWeight) return emptyList()
        var remainingPerSide = ((targetWeight - barWeight) / 2f).coerceAtLeast(0f)
        val result = mutableListOf<Float>()
        for (plate in availablePlates.sortedDescending()) {
            while (remainingPerSide + 0.0001f >= plate) {
                result += plate
                remainingPerSide -= plate
            }
        }
        return result
    }
}
