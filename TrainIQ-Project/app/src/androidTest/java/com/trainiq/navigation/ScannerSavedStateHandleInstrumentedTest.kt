package com.trainiq.navigation

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.trainiq.features.nutrition.ScannerMode
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScannerSavedStateHandleInstrumentedTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun barcodeScannerResultReturnsToNutritionAndClearsAfterConsumption() {
        compose.setContent {
            val navController = rememberNavController()

            NavHost(navController = navController, startDestination = Nutrition) {
                composable<Nutrition> { entry ->
                    val pendingBarcode by entry.savedStateHandle
                        .getStateFlow(BarcodeScanResultKey, "")
                        .collectAsStateWithLifecycle()
                    if (pendingBarcode.isBlank()) {
                        Button(onClick = { navController.navigate(CameraScanner(scannerMode = ScannerMode.BARCODE)) }) {
                            Text("Open barcode scanner")
                        }
                    } else {
                        Text("Barcode ontvangen: $pendingBarcode")
                        Button(onClick = { entry.clearBarcodeScanResult() }) {
                            Text("Barcode verwerkt")
                        }
                    }
                }
                composable<CameraScanner> {
                    Button(
                        onClick = {
                            navController.previousBackStackEntry?.setBarcodeScanResult("3017620422003")
                            navController.popBackStack()
                        },
                    ) {
                        Text("Simuleer barcode resultaat")
                    }
                }
            }
        }

        compose.onNodeWithText("Open barcode scanner").performClick()
        compose.onNodeWithText("Simuleer barcode resultaat").performClick()
        compose.onNodeWithText("Barcode ontvangen: 3017620422003").assertExists()
        compose.onNodeWithText("Barcode verwerkt").performClick()
        compose.onNodeWithText("Open barcode scanner").assertExists()
    }
}
