package com.trainiq.features.nutrition

import androidx.activity.compose.setContent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.trainiq.MainActivity
import com.trainiq.core.datastore.OnboardingPreferences
import com.trainiq.core.datastore.UserPreferencesRepository
import com.trainiq.core.theme.TrainIqTheme
import com.trainiq.domain.model.*
import com.trainiq.testing.resetTrainIqAndroidTestDatabase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Actual NutritionRoute -> Hilt NutritionViewModel (Activity owner) -> use case -> Room.
 * Nav-entry retention is covered separately by NutritionAiBatchOwnerInstrumentedTest. */
class NutritionAiProductionWiringInstrumentedTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test fun controlledSyntheticReviewSavesProductThroughActualRouteAndCannotReplay() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        runBlocking { UserPreferencesRepository(context).saveOnboardingPreferences(OnboardingPreferences(completed = true, guidedTourCompleted = true)) }
        val database = resetTrainIqAndroidTestDatabase(context)
        lateinit var model: NutritionViewModel
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                model = ViewModelProvider(activity)[NutritionViewModel::class.java]
                model.prepareScan(NutritionScanDestination.ProductLibrary, null)
                assertTrue(model.acceptCameraMealResult(model.pendingReviewId,
                    MealAnalysisResult(listOf(MealScanItem("Synthetic review product", 100.0,
                        NutritionFacts(150.0, 18.0, 30.0, 6.0))), notes = "Controleer de synthetische portie.", source = MealAnalysisSource.API),
                    "totaal 100 g"))
                activity.setContent { TrainIqTheme(dynamicColor = false) {
                    NutritionRoute(viewModel = model, onAiScanner = {}, onOpenBarcodeScanner = {})
                } }
            }
            compose.waitUntil(20_000) { compose.onAllNodesWithText("Producten opslaan").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithContentDescription("Grammen").performScrollTo().performTextReplacement("200")
            compose.onNodeWithContentDescription("Calorieën").performScrollTo().performTextReplacement("300")
            compose.onNodeWithContentDescription("Optionele context").performScrollTo().performTextReplacement("totaal 200 g")
            compose.onNodeWithText("Producten opslaan").performScrollTo().assertIsDisplayed()
            captureTask4Evidence("ai-review", compose.onRoot().printToString())
            compose.onNodeWithText("Producten opslaan").performClick()
            compose.waitUntil(20_000) {
                (model.uiState.value as? NutritionUiState.Success)?.let { state ->
                    !state.aiBatch.busy && (
                        (state.overview.foods.size == 1 && state.scanResult == null) ||
                            state.aiBatch.message.orEmpty().contains("overschrijden"))
                } == true
            }
            val state = model.uiState.value as NutritionUiState.Success
            assertEquals(state.aiBatch.message, 1, state.overview.foods.size)
            assertEquals("totaal 100 g", state.reviewContext)
            val saved = (model.uiState.value as NutritionUiState.Success).overview.foods.single()
            assertEquals("Synthetic review product", saved.name)
            assertEquals(200.0, saved.defaultServingGrams, 0.0)
            assertEquals(150.0, saved.caloriesPer100g, 0.0)
            assertEquals(FoodSourceType.AI, saved.sourceType)
            assertFalse(NutritionSubmitKey.AiItems in (model.uiState.value as NutritionUiState.Success).pendingSubmits)
            assertEquals(1, runBlocking { database.dao().readFoodItemsForExport().size })
            assertTrue(runBlocking { database.dao().readMealsForExport().isEmpty() })
            compose.onNodeWithText("Producten opslaan").assertDoesNotExist()
            compose.onNodeWithText("Synthetic review product").assertExists()
            scenario.recreate()
            scenario.onActivity { activity ->
                assertSame(model, ViewModelProvider(activity)[NutritionViewModel::class.java])
                activity.setContent { TrainIqTheme(dynamicColor = false) {
                    NutritionRoute(viewModel = model, onAiScanner = {}, onOpenBarcodeScanner = {})
                } }
            }
            compose.waitForIdle()
            compose.onNodeWithText("Producten opslaan").assertDoesNotExist()
            assertEquals(1, runBlocking { database.dao().readFoodItemsForExport().size })
        }
    }
}
