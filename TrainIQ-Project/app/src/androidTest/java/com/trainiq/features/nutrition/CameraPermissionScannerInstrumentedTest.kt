package com.trainiq.features.nutrition

import android.Manifest
import androidx.camera.core.CameraState
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.trainiq.core.theme.TrainIqTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.TimeUnit

class CameraPermissionScannerInstrumentedTest {
    @get:Rule val compose = createComposeRule()

    @Test fun deniedCameraKeepsBackActionAvailable() {
        var backedOut = false
        compose.setContent {
            TrainIqTheme {
                CameraScannerScreen(
                    uiState = CameraScannerUiState.Preview("", true), scannerMode = ScannerMode.BARCODE,
                    onAnalyze = {}, onDismissError = {}, onScanAgain = {}, onReviewItems = {}, onReviewScaleMeasurement = {},
                    onBack = { backedOut = true }, onBarcodeScanned = {},
                    bindCameraPreview = false, initialCameraPermissionGranted = false,
                )
            }
        }
        compose.onNodeWithText("Cameratoegang nodig").assertIsDisplayed()
        compose.onNodeWithText("Toegang geven").assertIsDisplayed()
        compose.onNodeWithText("Terug").performClick()
        compose.runOnIdle { assertTrue(backedOut) }
    }

    @Test fun alreadyGrantedCameraOpensBarcodeAfterAiCameraAndCanReopen() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        // Revoking our own permission kills the instrumentation process on Android 16.
        // Denial uses the fixture above and the separate system UI smoke.
        instrumentation.uiAutomation.grantRuntimePermission(context.packageName, Manifest.permission.CAMERA)
        val provider = ProcessCameraProvider.getInstance(context).get(15, TimeUnit.SECONDS)
        var mode by mutableStateOf(ScannerMode.AI_MEAL)
        compose.setContent {
            TrainIqTheme {
                key(mode) {
                    CameraScannerScreen(
                        uiState = CameraScannerUiState.Preview("", true), scannerMode = mode,
                        onAnalyze = {}, onDismissError = {}, onScanAgain = {}, onReviewItems = {}, onReviewScaleMeasurement = {},
                        onBack = {}, onBarcodeScanned = {},
                    )
                }
            }
        }
        listOf(ScannerMode.AI_MEAL, ScannerMode.BARCODE, ScannerMode.AI_MEAL, ScannerMode.BARCODE).forEach { next ->
            compose.runOnIdle { mode = next }
            compose.waitUntil(15_000) { provider.availableCameraInfos.any { it.cameraState.value?.type == CameraState.Type.OPEN } }
            compose.onNodeWithText(if (next == ScannerMode.BARCODE) "Barcodescanner" else "Camerascanner").assertIsDisplayed()
            compose.onNodeWithText(scannerCameraBindFailureMessage(next)).assertDoesNotExist()
        }
    }
}
