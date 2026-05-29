package com.trainiq.features.nutrition

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.trainiq.core.theme.TrainIqTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AiCameraScannerModesInstrumentedTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun aiMealScannerPreviewOpensWithCaptureControls() {
        compose.setContent {
            TrainIqTheme {
                CameraScannerScreen(
                    uiState = CameraScannerUiState.Preview(
                        contextHint = "",
                        message = "",
                        isEnabled = true,
                    ),
                    scannerMode = ScannerMode.AI_MEAL,
                    onAnalyze = {},
                    onDismissError = {},
                    onScanAgain = {},
                    onReviewItems = {},
                    onReviewScaleMeasurement = {},
                    onBack = {},
                    onBarcodeScanned = {},
                    bindCameraPreview = false,
                    initialCameraPermissionGranted = true,
                )
            }
        }

        compose.onNodeWithText("Camerascanner").assertIsDisplayed()
        compose.onNodeWithText("Zet het volledige bord of de verpakking duidelijk in beeld", substring = true)
            .assertIsDisplayed()
        compose.onNodeWithText("Foto maken").assertIsDisplayed()
        compose.onNodeWithText("Terug").assertIsDisplayed()
    }

    @Test
    fun smartScaleScannerPreviewOpensWithImportAndCaptureControls() {
        compose.setContent {
            TrainIqTheme {
                CameraScannerScreen(
                    uiState = CameraScannerUiState.Preview(
                        contextHint = "Lees gewicht, vetpercentage en spiermassa uit van de smart-weegschaal.",
                        message = "",
                        isEnabled = true,
                    ),
                    scannerMode = ScannerMode.AI_SCALE,
                    onAnalyze = {},
                    onDismissError = {},
                    onScanAgain = {},
                    onReviewItems = {},
                    onReviewScaleMeasurement = {},
                    onBack = {},
                    onBarcodeScanned = {},
                    bindCameraPreview = false,
                    initialCameraPermissionGranted = true,
                )
            }
        }

        compose.onNodeWithText("Camerascanner").assertIsDisplayed()
        compose.onNodeWithText("Lees gewicht, vetpercentage en spiermassa", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Foto maken").assertIsDisplayed()
        compose.onNodeWithText(scalePhotoImportLabel()).assertIsDisplayed()
        compose.onNodeWithText("Terug").assertIsDisplayed()
    }

}
