package com.trainiq.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class StrengthCalculatorTest {

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
