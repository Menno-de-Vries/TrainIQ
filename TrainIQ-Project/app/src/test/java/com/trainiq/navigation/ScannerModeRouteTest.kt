package com.trainiq.navigation

import com.trainiq.features.nutrition.ScannerMode
import org.junit.Assert.assertEquals
import org.junit.Test

class ScannerModeRouteTest {
    @Test
    fun scannerModeFromRoute_withKnownMode_returnsMode() {
        assertEquals(ScannerMode.BARCODE, scannerModeFromRoute(ScannerMode.BARCODE.name))
    }

    @Test
    fun scannerModeFromRoute_withUnknownMode_fallsBackToAiMeal() {
        assertEquals(ScannerMode.AI_MEAL, scannerModeFromRoute("UNKNOWN"))
    }

    @Test
    fun scannerModeFromRoute_withBlankMode_fallsBackToAiMeal() {
        assertEquals(ScannerMode.AI_MEAL, scannerModeFromRoute(""))
    }
}
