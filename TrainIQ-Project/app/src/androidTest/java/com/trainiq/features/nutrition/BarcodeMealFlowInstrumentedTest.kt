package com.trainiq.features.nutrition

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.*
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.trainiq.core.datastore.AiPreferences
import com.trainiq.core.theme.TrainIqTheme
import com.trainiq.domain.model.*
import com.trainiq.domain.repository.MealEntryRequest
import com.trainiq.navigation.BarcodeScanResultKey
import com.trainiq.navigation.CameraScanner
import com.trainiq.navigation.Nutrition
import com.trainiq.navigation.clearBarcodeScanResult
import com.trainiq.navigation.setBarcodeScanResult
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import kotlinx.coroutines.flow.first

/** Real nutrition UI and navigation; only camera recognition, lookup transport and storage are faked. */
class BarcodeMealFlowInstrumentedTest {
    @get:Rule val compose = createComposeRule()
    private val savedMeals = mutableListOf<MealType>()
    private var savedFoods = 0
    private var lookupCalls = 0

    @Test fun firstAddAfterDayChangeUsesTodayAndNotRestoredYesterday() {
        var today = java.time.LocalDate.of(2026, 9, 19)
        val dates = mutableListOf<java.time.LocalDate>()
        val restoration = showFlow(currentDate = { today }, saveEntries = { _, entries ->
            dates += java.time.Instant.ofEpochMilli(requireNotNull(entries.single().loggedAt))
                .atZone(java.time.ZoneId.systemDefault()).toLocalDate()
        })
        today = today.plusDays(1)
        restoration.emulateSavedInstanceStateRestore()
        openMealScanner("Middag")
        compose.onNodeWithText("Herken barcode").performClick()
        compose.onNodeWithText("Alleen aan maaltijd toevoegen").performScrollTo().performClick()
        compose.onNodeWithText("Maaltijd opslaan").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(listOf(today), dates) }
    }

    @Test fun midnightWhileDraftIsOpenUsesTodayButExplicitDateSurvivesRestoration() {
        var today = java.time.LocalDate.of(2026, 9, 19)
        val dates = mutableListOf<java.time.LocalDate>()
        val restoration = showFlow(currentDate = { today }, saveEntries = { _, entries ->
            dates += java.time.Instant.ofEpochMilli(requireNotNull(entries.single().loggedAt))
                .atZone(java.time.ZoneId.systemDefault()).toLocalDate()
        })
        openMealScanner("Middag")
        compose.onNodeWithText("Herken barcode").performClick()
        compose.onNodeWithText("Alleen aan maaltijd toevoegen").performScrollTo().performClick()
        today = today.plusDays(1)
        compose.onNodeWithText("Maaltijd opslaan").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(listOf(today), dates) }

        openMealScanner("Middag")
        compose.onNodeWithText("Herken barcode").performClick()
        compose.onNodeWithText("Alleen aan maaltijd toevoegen").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Datum (jjjj-mm-dd)").performScrollTo().performTextReplacement("2026-09-10")
        androidx.test.espresso.Espresso.closeSoftKeyboard()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("Maaltijd opslaan").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(listOf(today, java.time.LocalDate.of(2026, 9, 10)), dates) }
    }

    @Test fun firstManualAddWithKeyboardAddsExactlyOneItem() {
        showFlow()
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasContentDescription("Toevoegen aan Middag"))
        compose.onNodeWithContentDescription("Toevoegen aan Middag").performClick()
        compose.onNodeWithText("Handmatig product maken").performClick()
        listOf("Productnaam" to "Eerste product", "kcal / 100g" to "100", "Eiwit / 100g" to "10",
            "Kh / 100g" to "10", "Vet / 100g" to "2", "Standaard hoeveelheid (gram)" to "150").forEach { (label, value) ->
            compose.onNodeWithContentDescription(label).performScrollTo().performTextReplacement(value)
        }
        compose.onNodeWithText("Alleen aan maaltijd toevoegen").performScrollTo().performTouchInput { click() }
        compose.onNodeWithText("Maaltijd opslaan").performScrollTo().performTouchInput { click() }
        compose.runOnIdle { assertEquals(listOf(MealType.LUNCH), savedMeals) }
    }

    @Test fun scannedDrinkReachesPersistentHydrationTotal() {
        val db = androidx.room.Room.inMemoryDatabaseBuilder(
            androidx.test.core.app.ApplicationProvider.getApplicationContext(),
            com.trainiq.core.database.TrainIqDatabase::class.java,
        ).build()
        try {
            var total by mutableStateOf(0.0)
            showFlow(drink = true, hydrationContent = { Text("Geregistreerd vocht: ${total.toLong()} ml") }, saveEntries = { id, entries ->
                kotlinx.coroutines.runBlocking {
                    db.dao().saveMeal(
                        com.trainiq.core.database.MealEntity(id = id, date = 1000, name = "Drank", calories = 0, protein = 0, carbs = 0, fat = 0),
                        entries.mapIndexed { index, entry ->
                            com.trainiq.core.database.MealItemEntity(id = index.toLong() + 1, mealId = id, itemType = "SNAPSHOT", referenceId = 0,
                                name = "Drank", gramsUsed = entry.gramsUsed, calories = 0.0, protein = 0.0, carbs = 0.0, fat = 0.0,
                                hydrationMl = entry.hydrationMl, servingCount = entry.servingCount)
                        },
                    )
                    total = db.dao().observeHydration().first().sumOf { it.volumeMl }
                }
            })
            openMealScanner("Middag")
            compose.onNodeWithText("Herken barcode").performClick()
            compose.onNodeWithText("Alleen aan maaltijd toevoegen").performScrollTo().performClick()
            compose.onNodeWithText("Volume per portie (ml)").assertDoesNotExist()
            compose.onNodeWithText("Gram per portie").performScrollTo().performTextReplacement("250")
            compose.onNodeWithText("250 ml per portie - gram telt 1-op-1 als vocht").assertExists()
            androidx.test.espresso.Espresso.closeSoftKeyboard()
            val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
            java.io.File(context.getExternalFilesDir(null), "hydration-draft.png").outputStream().use {
                compose.onRoot().captureToImage().asAndroidBitmap().compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
            }
            compose.onNodeWithText("Maaltijd opslaan").performScrollTo().performClick()
            compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Geregistreerd vocht: 250 ml"))
            compose.onNodeWithText("Geregistreerd vocht: 250 ml").assertExists()
        } finally { db.close() }
    }

    @Test fun optionalSaveDefaultsOffAndSurvivesRestoration() {
        val restoration = showFlow()
        openMealScanner("Middag")
        compose.onNodeWithText("Herken barcode").performClick()
        compose.onNodeWithText("Opslaan bij mijn producten").performScrollTo().assertIsOff().performClick()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("Opslaan bij mijn producten").performScrollTo().assertIsOn()
        val capture = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().uiAutomation
            .executeShellCommand("screencap -p /data/local/tmp/trainiq-barcode-choice.png")
        android.os.ParcelFileDescriptor.AutoCloseInputStream(capture).use { it.readBytes() }
        compose.onNodeWithText("Aan maaltijd toevoegen en product opslaan").performScrollTo().performClick()
        compose.onNodeWithText("Maaltijd opslaan").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(1, savedFoods); assertEquals(listOf(MealType.LUNCH), savedMeals) }
        compose.onNodeWithContentDescription("Voeding secties openen").performClick()
        compose.onNodeWithText("Producten").assertIsDisplayed().performClick()
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Aan maaltijd toevoegen"))
        compose.onNodeWithText("Test kwark").assertExists()
        compose.onNodeWithText("Aan maaltijd toevoegen").performScrollTo().performClick()
        compose.onNodeWithText("Maaltijd opslaan").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(2, savedMeals.size); assertEquals(1, savedFoods); assertEquals(1, lookupCalls) }
    }

    @Test fun optionalSaveFailureKeepsMealAndExplainsFailure() {
        showFlow(failFoodSave = true)
        openMealScanner("Avond")
        compose.onNodeWithText("Herken barcode").performClick()
        compose.onNodeWithText("Opslaan bij mijn producten").performScrollTo().performClick()
        compose.onNodeWithText("Aan maaltijd toevoegen en product opslaan").performScrollTo().performClick()
        compose.onNodeWithText("Product aan de maaltijd toegevoegd, maar opslaan bij mijn producten is mislukt.", substring = true).assertExists()
        compose.onNodeWithText("Maaltijd opslaan").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(listOf(MealType.DINNER), savedMeals) }
    }

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

    private fun showFlow(found: Boolean = true, failFirstLookup: Boolean = false, stallFirstLookup: Boolean = false, failFoodSave: Boolean = false,
        currentDate: () -> java.time.LocalDate = { java.time.LocalDate.now() },
        drink: Boolean = false, hydrationContent: @androidx.compose.runtime.Composable () -> Unit = {},
        saveEntries: (Long, List<MealEntryRequest>) -> Unit = { _, _ -> },
    ): StateRestorationTester {
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
                            currentDate = currentDate,
                            hydrationContent = hydrationContent,
                            uiState = state,
                            onSaveFood = { _, name, code, kcal, protein, carbs, fat, grams, source, done, failed ->
                                savedFoods++
                                if (failFoodSave) failed(IllegalStateException("Synthetic failure"))
                                else {
                                    val food = FoodItem(1, name, code, kcal.toDouble(), protein.toDouble(), carbs.toDouble(), fat.toDouble(), grams.toDouble(), source, 0, 0)
                                    state = state.copy(overview = state.overview.copy(foods = listOf(food)))
                                    done(food)
                                }
                            },
                            onSaveRecipe = { _, _, _, _, _, _ -> },
                            onSaveMeal = { id, type, _, _, entries, done ->
                                assertEquals(1, entries.size)
                                saveEntries(requireNotNull(id), entries)
                                savedMeals += type
                                done()
                            },
                            onDeleteMeal = {}, onDeleteFood = {}, onDeleteRecipe = {},
                            onTryStartAiBatchSave = { true }, onFinishAiBatchSave = {},
                            onSetScanResult = {}, onSetMessage = { state = state.copy(message = it) }, onDismissMessage = {}, onRetry = {}, onAiScanner = {},
                            onOpenBarcodeScanner = { nav.navigate(CameraScanner(scannerMode = ScannerMode.BARCODE)) },
                            pendingBarcode = barcode.takeIf { it.isNotBlank() },
                            onBarcodeClear = { entry.clearBarcodeScanResult() },
                            onClearBarcodeLookupResult = { state = state.copy(barcodeLookupResult = null) },
                            onLookupBarcodeProduct = { code, target ->
                                lookupCalls++
                                val failed = failFirstLookup && attempts++ == 0
                                if (!stallFirstLookup || lookupCalls > 1) {
                                    state = state.copy(barcodeLookupResult = BarcodeLookupUiResult(
                                        target, if (found && !failed) BarcodeProductLookupResult(code, "Test kwark", 60.0, 10.0, 4.0, 0.5,
                                            explicitServingMl = if (drink) 250.0 else null, isBeverage = drink) else null,
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
