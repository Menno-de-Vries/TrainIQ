package com.trainiq.features.nutrition

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.trainiq.core.theme.TrainIqTheme
import org.junit.Test

class ScannerRecoveryInstrumentedTest {
    @OptIn(ExperimentalTestApi::class)
    @Test fun systemBackAfterMissingRetryAndProcessingReturnsToOrigin() = runComposeUiTest {
        var state: CameraScannerUiState by mutableStateOf(CameraScannerUiState.Empty("", "Niet gevonden"))
        var open by mutableStateOf(true)
        var exits = 0
        setContent { TrainIqTheme {
            if (!open) androidx.compose.material3.Text("Receptdraft behouden")
            else CameraScannerScreen(uiState = state, scannerMode = ScannerMode.BARCODE, onAnalyze = {},
                onDismissError = {}, onScanAgain = { state = CameraScannerUiState.Preview("", true) },
                onReviewItems = {}, onReviewScaleMeasurement = {}, onBack = { exits++; open = false },
                onBarcodeScanned = {}, bindCameraPreview = false, initialCameraPermissionGranted = true)
        } }
        onNodeWithText(scannerRetryLabel()).performClick()
        onNodeWithText("Barcodescanner").assertIsDisplayed()
        runOnIdle { state = CameraScannerUiState.Processing }
        onNodeWithText("Product ophalen...").assertIsDisplayed()
        val back = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().uiAutomation
            .executeShellCommand("input keyevent KEYCODE_BACK")
        android.os.ParcelFileDescriptor.AutoCloseInputStream(back).use { it.readBytes() }
        onNodeWithText("Receptdraft behouden").assertIsDisplayed()
        runOnIdle { org.junit.Assert.assertEquals(1, exits) }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test fun cancelIsReachableFromProcessingAndEmpty() = runComposeUiTest {
        var state: CameraScannerUiState by mutableStateOf(CameraScannerUiState.Processing)
        var exits = 0
        setContent { TrainIqTheme {
            CameraScannerScreen(uiState = state, scannerMode = ScannerMode.BARCODE, onAnalyze = {},
                onDismissError = {}, onScanAgain = {}, onReviewItems = {}, onReviewScaleMeasurement = {},
                onBack = { exits++ }, onBarcodeScanned = {}, bindCameraPreview = false,
                initialCameraPermissionGranted = true)
        } }
        onNodeWithTag("scanner-sheet-cancel").performClick()
        runOnIdle { state = CameraScannerUiState.Empty("", "Niet gevonden") }
        onNodeWithTag("scanner-sheet-cancel").performClick()
        runOnIdle { org.junit.Assert.assertEquals(2, exits) }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test fun barcodeResolvingAndMissingResultExposeRetryAndManualActions() = runComposeUiTest {
        var state: CameraScannerUiState by mutableStateOf(CameraScannerUiState.Processing)
        var manual = 0
        setContent { TrainIqTheme {
            CameraScannerScreen(uiState = state, scannerMode = ScannerMode.BARCODE, onAnalyze = {},
                onDismissError = {}, onScanAgain = { state = CameraScannerUiState.Preview("", true) },
                onReviewItems = {}, onReviewScaleMeasurement = {}, onBack = {}, onBarcodeScanned = {},
                onManual = { manual++ }, bindCameraPreview = false, initialCameraPermissionGranted = true)
        } }
        onNodeWithText("Product ophalen...").assertIsDisplayed()
        runOnIdle { state = CameraScannerUiState.Empty("", "Geen bruikbaar product gevonden. Probeer opnieuw of voeg het handmatig toe.") }
        onNodeWithText(scannerEmptyTitle()).assertIsDisplayed()
        onNodeWithText(scannerManualAddLabel()).performClick()
        runOnIdle { org.junit.Assert.assertEquals(1, manual) }
        val capture = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().uiAutomation
            .executeShellCommand("screencap -p /data/local/tmp/trainiq-barcode-missing.png")
        android.os.ParcelFileDescriptor.AutoCloseInputStream(capture).use { it.readBytes() }
        onNodeWithText(scannerRetryLabel()).performClick()
        onNodeWithText(scannerEmptyTitle()).assertDoesNotExist()
        onNodeWithText("Barcodescanner").assertIsDisplayed()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test fun scanErrorCanReturnToPreviewWithoutKeepingTheErrorSheet() = runComposeUiTest {
        var state: CameraScannerUiState by mutableStateOf(CameraScannerUiState.Error("Meal context", "Try this scan again"))
        setContent {
            TrainIqTheme {
                CameraScannerScreen(
                    uiState = state, scannerMode = ScannerMode.AI_MEAL, onAnalyze = {},
                    onDismissError = { state = CameraScannerUiState.Preview("Meal context", true) },
                    onScanAgain = {}, onReviewItems = {}, onReviewScaleMeasurement = {},
                    onBack = {}, onBarcodeScanned = {}, bindCameraPreview = false,
                    initialCameraPermissionGranted = true,
                )
            }
        }
        onNodeWithText("Try this scan again").assertIsDisplayed()
        onNodeWithText(scannerErrorPrimaryActionLabel(ScannerSheetErrorAction.Dismiss)).performClick()
        onNodeWithText("Scan mislukt").assertDoesNotExist()
        onNodeWithText("Camerascanner").assertIsDisplayed()
        onNodeWithText("Meal context").assertIsDisplayed()
    }
}
