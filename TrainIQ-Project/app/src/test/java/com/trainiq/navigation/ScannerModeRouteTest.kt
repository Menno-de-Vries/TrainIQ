package com.trainiq.navigation

import com.trainiq.features.nutrition.ScannerMode
import org.junit.Assert.assertEquals
import org.junit.Test

class ScannerModeRouteTest {
    @Test
    fun cameraScannerRoute_keepsScannerModeTyped() {
        val route = CameraScanner(scannerMode = ScannerMode.BARCODE)

        assertEquals(ScannerMode.BARCODE, route.scannerMode)
    }

    @Test
    fun cameraScannerRoute_defaultsToAiMealMode() {
        assertEquals(ScannerMode.AI_MEAL, CameraScanner().scannerMode)
    }
}
