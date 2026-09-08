package com.trainiq.features.nutrition

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.trainiq.core.datastore.AiPreferences
import com.trainiq.core.theme.TrainIqTheme
import com.trainiq.domain.model.*
import com.trainiq.navigation.BarcodeScanResultKey
import com.trainiq.navigation.CameraScanner
import com.trainiq.navigation.Nutrition
import com.trainiq.navigation.clearBarcodeScanResult
import com.trainiq.navigation.setBarcodeScanResult
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Real nutrition UI and navigation; only camera recognition, lookup transport and storage are faked. */
class BarcodeMealFlowInstrumentedTest {
    @get:Rule val compose = createComposeRule()
    private val savedMeals = mutableListOf<MealType>()
    private var savedFoods = 0
    private var lookupCalls = 0

    @Test fun breakfastScanIsConfirmedIntoBreakfast() = verifyMeal("Ochtend", MealType.BREAKFAST)
    @Test fun lunchScanIsConfirmedIntoLunch() = verifyMeal("Middag", MealType.LUNCH)
    @Test fun dinnerScanIsConfirmedIntoDinner() = verifyMeal("Avond", MealType.DINNER)
    @Test fun snackScanIsConfirmedIntoSnack() = verifyMeal("Snacks", MealType.SNACK)

    @Test fun scannerBackDoesNotChangeTheMealDraft() {
        showFlow()
        openMealScanner("Middag")
        androidx.test.espresso.Espresso.pressBack()
        compose.onNodeWithText("Voedingsdag").assertExists()
        compose.onNodeWithContentDescription("Productnaam").assertDoesNotExist()
        compose.runOnIdle { assertTrue(savedMeals.isEmpty()); assertEquals(0, savedFoods) }
    }

    @Test fun mealCategorySurvivesRestorationWhileScannerIsOpen() {
        val restoration = showFlow()
        openMealScanner("Snacks")
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("Herken barcode").performClick()
        compose.onNodeWithText("Alleen aan maaltijd toevoegen").performScrollTo().performClick()
        compose.onNodeWithText("Maaltijd opslaan").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(listOf(MealType.SNACK), savedMeals) }
    }

    private fun verifyMeal(label: String, type: MealType) {
        showFlow()
        openMealScanner(label)
        compose.onNodeWithText("Herken barcode").performClick()
        compose.onNodeWithContentDescription("Productnaam").performScrollTo().assertTextContains("Test kwark")
        assertTrue(savedMeals.isEmpty())
        compose.onNodeWithText("Alleen aan maaltijd toevoegen").performScrollTo().performClick()
        assertTrue(savedMeals.isEmpty())
        compose.onNodeWithText("Maaltijd opslaan").performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals(listOf(type), savedMeals)
            assertEquals(0, savedFoods)
        }
    }

    @Test fun cancellingScannerDoesNotOpenAnEditorOrSaveAProduct() {
        showFlow()
        openMealScanner("Ochtend")
        compose.onNodeWithText("Annuleren").performClick()
        compose.onNodeWithContentDescription("Productnaam").assertDoesNotExist()
        compose.onNodeWithText("Voedingsdag").assertExists()
        compose.runOnIdle { assertTrue(savedMeals.isEmpty()); assertEquals(0, savedFoods) }
    }

    @Test fun unknownBarcodeKeepsManualEntryAndDoesNotAddAnything() {
        showFlow(found = false)
        openMealScanner("Ochtend")
        compose.onNodeWithText("Herken barcode").performClick()
        compose.onNodeWithText("Product niet gevonden of voedingswaarden ontbreken. Vul het product handmatig in of scan opnieuw.")
            .performScrollTo().assertExists()
        compose.onNodeWithContentDescription("Productnaam").performScrollTo().assertTextContains("")
        compose.runOnIdle { assertTrue(savedMeals.isEmpty()); assertEquals(0, savedFoods) }
    }

    @Test fun networkFailureIsVisibleAndRetryRecovers() {
        showFlow(failFirstLookup = true)
        openMealScanner("Ochtend")
        compose.onNodeWithText("Herken barcode").performClick()
        compose.onNodeWithText("Product ophalen mislukt.", substring = true).performScrollTo().assertExists()
        compose.onNodeWithText("Product opnieuw ophalen").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Productnaam").performScrollTo().assertTextContains("Test kwark")
        compose.runOnIdle { assertTrue(savedMeals.isEmpty()) }
    }

    @Test fun restoringAnInFlightLookupRestartsItAndKeepsTheMealTarget() {
        val restoration = showFlow(stallFirstLookup = true)
        openMealScanner("Middag")
        compose.onNodeWithText("Herken barcode").performClick()
        compose.runOnIdle { assertEquals(1, lookupCalls) }
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithContentDescription("Productnaam").performScrollTo().assertTextContains("Test kwark")
        compose.onNodeWithText("Alleen aan maaltijd toevoegen").performScrollTo().performClick()
        compose.onNodeWithText("Maaltijd opslaan").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(2, lookupCalls); assertEquals(listOf(MealType.LUNCH), savedMeals) }
    }

    private fun openMealScanner(label: String) {
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasContentDescription("Toevoegen aan $label"))
        compose.onNodeWithContentDescription("Toevoegen aan $label").performScrollTo().performClick()
        compose.onNodeWithText("Barcode scannen").performScrollTo().performClick()
    }

    private fun showFlow(found: Boolean = true, failFirstLookup: Boolean = false, stallFirstLookup: Boolean = false): StateRestorationTester {
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            var state by remember { mutableStateOf(barcodeFlowState()) }
            var attempts by remember { mutableStateOf(0) }
            val nav = rememberNavController()
            TrainIqTheme(dynamicColor = false) {
                NavHost(nav, startDestination = Nutrition) {
                    composable<Nutrition> { entry ->
                        val barcode by entry.savedStateHandle.getStateFlow(BarcodeScanResultKey, "").collectAsStateWithLifecycle()
                        NutritionScreen(
                            uiState = state,
                            onSaveFood = { _, _, _, _, _, _, _, _, _, _, _ -> savedFoods++ },
                            onSaveRecipe = { _, _, _, _, _, _ -> },
                            onSaveMeal = { _, type, _, _, entries, done ->
                                assertEquals(1, entries.size)
                                savedMeals += type
                                done()
                            },
                            onDeleteMeal = {}, onDeleteFood = {}, onDeleteRecipe = {},
                            onTryStartAiBatchSave = { true }, onFinishAiBatchSave = {},
                            onSetScanResult = {}, onSetMessage = {}, onDismissMessage = {}, onRetry = {}, onAiScanner = {},
                            onSetScanTarget = { state = state.copy(scanTarget = it) },
                            onOpenBarcodeScanner = { nav.navigate(CameraScanner(scannerMode = ScannerMode.BARCODE)) },
                            pendingBarcode = barcode.takeIf { it.isNotBlank() },
                            onBarcodeClear = { entry.clearBarcodeScanResult() },
                            onClearBarcodeLookupResult = { state = state.copy(barcodeLookupResult = null) },
                            onLookupBarcodeProduct = { code, target ->
                                lookupCalls++
                                val failed = failFirstLookup && attempts++ == 0
                                if (!stallFirstLookup || lookupCalls > 1) {
                                    state = state.copy(barcodeLookupResult = BarcodeLookupUiResult(
                                        target, if (found && !failed) BarcodeProductLookupResult(code, "Test kwark", 60.0, 10.0, 4.0, 0.5) else null,
                                        code, failed,
                                    ))
                                }
                            },
                        )
                    }
                    composable<CameraScanner> {
                        Column {
                            Button(onClick = {
                                nav.previousBackStackEntry?.setBarcodeScanResult("3017620422003")
                                nav.popBackStack()
                            }) { Text("Herken barcode") }
                            Button(onClick = { nav.popBackStack() }) { Text("Annuleren") }
                        }
                    }
                }
            }
        }
        return restoration
    }
}

private fun barcodeFlowState() = NutritionUiState.Success(
    overview = NutritionOverview(
        foods = emptyList(), recipes = emptyList(), meals = emptyList(), todaysNutrition = NutritionFacts.Zero,
        todaysCalories = 0.0, todaysProtein = 0.0, todaysCarbs = 0.0, todaysFat = 0.0,
        todaysMeals = emptyList(), todaysMealsByType = MealType.entries.associateWith { emptyList() }, todaysWorkoutCalories = 0,
    ),
    aiPreferences = AiPreferences(false, ""),
)
