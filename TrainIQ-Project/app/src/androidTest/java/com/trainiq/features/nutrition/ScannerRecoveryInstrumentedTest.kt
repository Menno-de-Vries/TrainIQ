package com.trainiq.features.nutrition

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.trainiq.core.theme.TrainIqTheme
import org.junit.Test

class ScannerRecoveryInstrumentedTest {
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
