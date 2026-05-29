package com.trainiq.navigation

import com.trainiq.features.nutrition.ScannerMode
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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

    @Test
    fun scannerSavedStateHandleContractsUseStableKeysAndClearAfterConsumption() {
        val source = File("src/main/java/com/trainiq/navigation/TrainIqNav.kt").readText()
        val nutritionDestination = source.substringAfter("composable<Nutrition>").substringBefore("composable<Progress>")
        val progressDestination = source.substringAfter("composable<Progress>").substringBefore("composable<Coach>")
        val scannerDestination = source.substringAfter("composable<CameraScanner>").substringBefore("composable<Settings>")
        val barcodeHelpers = source.substringAfter("internal const val BarcodeScanResultKey").substringBefore("internal fun NavBackStackEntry.setScaleMeasurementResult")
        val scaleHelpers = source.substringAfter("internal const val ScaleWeightResultKey")

        assertTrue(source.contains("internal const val BarcodeScanResultKey = \"scanned_barcode\""))
        assertTrue(source.contains("internal const val ScaleWeightResultKey = \"scale_weight\""))
        assertTrue(source.contains("internal const val ScaleBodyFatResultKey = \"scale_body_fat\""))
        assertTrue(source.contains("internal const val ScaleMuscleMassResultKey = \"scale_muscle_mass\""))
        assertTrue(source.contains("internal const val ScaleNotesResultKey = \"scale_notes\""))
        assertTrue(nutritionDestination.contains("pendingBarcode by entry.savedStateHandle"))
        assertTrue(nutritionDestination.contains("onBarcodeClear = { entry.clearBarcodeScanResult() }"))
        assertTrue(progressDestination.contains("pendingScaleWeight by entry.savedStateHandle"))
        assertTrue(progressDestination.contains("onScaleResultConsumed = { entry.clearScaleMeasurementResult() }"))
        assertTrue(scannerDestination.contains("previousBackStackEntry?.setBarcodeScanResult(barcode)"))
        assertTrue(scannerDestination.contains("previousBackStackEntry?.setScaleMeasurementResult(result)"))
        assertTrue(barcodeHelpers.contains("savedStateHandle[BarcodeScanResultKey] = barcode"))
        assertTrue(barcodeHelpers.contains("savedStateHandle[BarcodeScanResultKey] = \"\""))
        assertTrue(scaleHelpers.contains("savedStateHandle[ScaleWeightResultKey]"))
        assertTrue(scaleHelpers.contains("savedStateHandle[ScaleNotesResultKey] = \"\""))
    }
}
