package com.trainiq.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class StrengthCalculatorTest {
    @Test(timeout = 1000) fun platePreviewRejectsUnboundedInputsAndInvalidInventories() {
        listOf(Float.POSITIVE_INFINITY, Float.NaN, Float.MAX_VALUE, 1001f).forEach {
            assertEquals(emptyList<Float>(), StrengthCalculator.calculatePlates(it))
        }
        assertEquals(listOf(20f, 20f), StrengthCalculator.calculatePlates(100f, availablePlates = listOf(0f, -5f, Float.NaN, Float.POSITIVE_INFINITY, 20f)))
        assertEquals(emptyList<Float>(), StrengthCalculator.calculatePlates(100f, availablePlates = listOf(Float.MIN_VALUE)))
        assertEquals(emptyList<Float>(), StrengthCalculator.calculatePlates(100f, barWeight = Float.NaN))
        assertEquals(listOf(20f, 10f, 1.25f), StrengthCalculator.calculatePlates(82.5f))
        assertEquals(490f, StrengthCalculator.calculatePlates(1000f).sum(), 0f)
    }

    @Test
    fun estimateOneRepMax_withBrzyckiRange_usesBrzyckiFormula() {
        val oneRepMax = StrengthCalculator.estimateOneRepMax(weight = 100.0, reps = 5)

        assertEquals(112.5, oneRepMax, 0.2)
    }

    @Test
    fun estimateOneRepMax_withHighRepRange_usesEpleyFormula() {
        val oneRepMax = StrengthCalculator.estimateOneRepMax(weight = 60.0, reps = 12)

        assertEquals(84.0, oneRepMax, 0.2)
    }

    @Test
    fun estimateOneRepMax_withInvalidInputs_returnsZero() {
        assertEquals(0.0, StrengthCalculator.estimateOneRepMax(weight = -100.0, reps = 5), 0.0)
        assertEquals(0.0, StrengthCalculator.estimateOneRepMax(weight = 100.0, reps = 0), 0.0)
    }

    @Test
    fun estimateRepsInReserve_fromRpe_mapsToRoundedRir() {
        assertEquals(2, StrengthCalculator.estimateRepsInReserve(8.0))
        assertEquals(1, StrengthCalculator.estimateRepsInReserve(8.8))
        assertEquals(null, StrengthCalculator.estimateRepsInReserve(0.0))
    }

    @Test
    fun calculatePlates_returnsPerSideBreakdown() {
        val plates = StrengthCalculator.calculatePlates(targetWeight = 100f)

        assertEquals(listOf(20f, 20f), plates)
    }
}
