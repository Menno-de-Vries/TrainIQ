package com.trainiq.core.util

import org.junit.Assert.assertEquals
import org.junit.Test

class FormattersTest {
    @Test
    fun energyBalanceValueText_formatsDeficitWithKcalAndStatus() {
        assertEquals("2583 kcal tekort", energyBalanceValueText(-2583))
    }

    @Test
    fun energyBalanceValueText_formatsSurplusWithKcalAndStatus() {
        assertEquals("420 kcal overschot", energyBalanceValueText(420))
    }

    @Test
    fun energyBalanceMetaText_usesDutchUnitsAndSeparators() {
        assertEquals(
            "In 1800 kcal - Uit 2300 kcal - Doel 2500 kcal",
            energyBalanceMetaText(caloriesIn = 1800, caloriesOut = 2300, calorieTarget = 2500),
        )
    }
}
