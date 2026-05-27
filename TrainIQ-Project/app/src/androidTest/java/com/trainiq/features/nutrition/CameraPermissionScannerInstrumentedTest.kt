package com.trainiq.features.nutrition

import android.Manifest
import android.content.Context
import android.os.ParcelFileDescriptor
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.trainiq.MainActivity
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CameraPermissionScannerInstrumentedTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun barcodeScannerShowsDeniedFallbackThenGrantedCameraCopy() {
        setCameraPermission(granted = false)
        ActivityScenario.launch(MainActivity::class.java).use {
            openBarcodeScanner()
            compose.waitForText("Cameratoegang nodig")
            compose.waitForText("Toegang geven")
            compose.waitForText("Terug")
        }

        setCameraPermission(granted = true)
        ActivityScenario.launch(MainActivity::class.java).use {
            openBarcodeScanner()
            compose.waitForText("Barcodescanner")
            compose.waitForText("Richt de camera op de barcode van het product.")
            compose.waitForText("Annuleren")
        }
    }

    private fun openBarcodeScanner() {
        compose.waitForText("Voeding")
        compose.onNodeWithText("Voeding").performClick()
        compose.onNodeWithContentDescription("Voeding secties openen").performClick()
        compose.waitForText("Voeding secties")
        compose.onNodeWithText("Producten").performClick()
        compose.waitForText("Producten")
        compose.onNodeWithText("Barcode scannen")
            .performScrollTo()
            .performClick()
    }

    private fun setCameraPermission(granted: Boolean) {
        val action = if (granted) "grant" else "revoke"
        shell("pm $action ${context.packageName} ${Manifest.permission.CAMERA}")
    }

    private fun shell(command: String) {
        InstrumentationRegistry.getInstrumentation()
            .uiAutomation
            .executeShellCommand(command)
            .use { descriptor ->
                ParcelFileDescriptor.AutoCloseInputStream(descriptor)
                    .bufferedReader()
                    .use { it.readText() }
            }
    }

    private fun androidx.compose.ui.test.junit4.ComposeTestRule.waitForText(text: String) {
        waitUntil(timeoutMillis = 30_000L) {
            onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty()
        }
    }
}
