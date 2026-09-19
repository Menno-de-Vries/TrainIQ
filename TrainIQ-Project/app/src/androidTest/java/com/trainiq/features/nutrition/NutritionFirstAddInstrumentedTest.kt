package com.trainiq.features.nutrition

import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.*
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.trainiq.MainActivity
import com.trainiq.core.datastore.OnboardingPreferences
import com.trainiq.core.datastore.UserPreferencesRepository
import com.trainiq.domain.model.FoodSourceType
import com.trainiq.domain.model.MealType
import com.trainiq.domain.repository.MealEntryRequest
import com.trainiq.domain.repository.MealEntryType
import com.trainiq.testing.resetTrainIqAndroidTestDatabase
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Real ViewModel -> use cases -> Room -> observed UI state, without camera/network dependencies. */
class NutritionFirstAddInstrumentedTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test fun firstManualMealTapWithKeyboardReachesRoomAndVisibleToday() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        runBlocking {
            UserPreferencesRepository(context).saveOnboardingPreferences(
                OnboardingPreferences(completed = true, guidedTourCompleted = true),
            )
        }
        val db = resetTrainIqAndroidTestDatabase(context)
        ActivityScenario.launch(MainActivity::class.java).use {
            val navigation = (hasContentDescription("Voeding") or hasText("Voeding")) and hasClickAction()
            compose.waitUntil(30_000) { compose.onAllNodes(navigation).fetchSemanticsNodes().isNotEmpty() }
            compose.onNode(navigation).performClick()
            compose.waitUntil(15_000) { compose.onAllNodesWithText("Voedingsdag").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithContentDescription("Toevoegen aan Middag").performScrollTo().performClick()
            compose.onNodeWithText("Handmatig product maken").performClick()
            listOf("Productnaam" to "First meal proof", "kcal / 100g" to "100", "Eiwit / 100g" to "10",
                "Kh / 100g" to "10", "Vet / 100g" to "2").forEach { (label, value) ->
                compose.onNodeWithContentDescription(label).performScrollTo().performTextReplacement(value)
            }
            compose.onNodeWithText("Alleen aan maaltijd toevoegen").performScrollTo().performTouchInput { click() }
            compose.onNodeWithContentDescription("Gram per portie").performScrollTo().performTextReplacement("150")
            compose.onNodeWithText("Maaltijd opslaan").performScrollTo().performTouchInput { click() }
            compose.waitUntil(15_000) { runBlocking { db.dao().readMealsForExport().size == 1 } }
            compose.onNodeWithText("Voedingsdag").assertExists()
            assertEquals(150.0, runBlocking { db.dao().readMealItemsForExport().single().calories }, 0.0)
        }
    }

    @Test fun freshFirstAddRapidTapSequentialAddsAndFailedMealRetryPersistExactlyOnce() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        runBlocking {
            UserPreferencesRepository(context).saveOnboardingPreferences(
                OnboardingPreferences(completed = true, guidedTourCompleted = true),
            )
        }
        val db = resetTrainIqAndroidTestDatabase(context)
        lateinit var vm: NutritionViewModel
        var foodId = 0L
        var foodCallbacks = 0
        var mealCallbacks = 0
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                vm = ViewModelProvider(activity)[NutritionViewModel::class.java]
                activity.lifecycleScope.launch { vm.uiState.collect {} }
                repeat(2) {
                    vm.saveFood(null, "First food", null, "100", "10", "10", "2", "100", FoodSourceType.MANUAL,
                        onSaved = { food -> foodId = food.id; foodCallbacks++ })
                }
            }
            compose.waitUntil(15_000) {
                (vm.uiState.value as? NutritionUiState.Success)?.overview?.foods?.any { it.id == foodId } == true && foodId > 0
            }
            assertEquals(1, foodCallbacks)
            assertEquals(1, runBlocking { db.dao().readFoodItemsForExport().size })
            scenario.onActivity {
                vm.saveFood(null, "Second food", null, "200", "20", "20", "4", "100", FoodSourceType.MANUAL)
            }
            compose.waitUntil(15_000) { (vm.uiState.value as? NutritionUiState.Success)?.overview?.foods?.size == 2 }
            val valid = listOf(MealEntryRequest(MealEntryType.FOOD, foodId, 150.0))
            scenario.onActivity {
                vm.saveMeal(101L, MealType.LUNCH, "First meal", "",
                    listOf(MealEntryRequest(MealEntryType.FOOD, Long.MAX_VALUE, 100.0)))
            }
            compose.waitUntil(15_000) {
                (vm.uiState.value as? NutritionUiState.Success)?.let {
                    it.message?.contains("verwijderd") == true && NutritionSubmitKey.Meal !in it.pendingSubmits
                } == true
            }
            assertTrue(runBlocking { db.dao().readMealsForExport().isEmpty() })
            scenario.onActivity {
                repeat(2) { vm.saveMeal(101L, MealType.LUNCH, "First meal", "", valid) { mealCallbacks++ } }
            }
            compose.waitUntil(15_000) {
                (vm.uiState.value as? NutritionUiState.Success)?.overview?.todaysMeals?.any {
                    it.id == 101L && it.items.size == 1
                } == true
            }
            assertEquals(1, mealCallbacks)
            scenario.onActivity { vm.saveMeal(102L, MealType.LUNCH, "Second meal", "", valid) { mealCallbacks++ } }
            compose.waitUntil(15_000) { (vm.uiState.value as? NutritionUiState.Success)?.overview?.todaysMeals?.size == 2 }
            val saved = runBlocking { db.dao().readMealItemsForExport() }
            assertEquals(2, saved.size)
            assertTrue(saved.all { it.calories == 150.0 && it.gramsUsed == 150.0 })
            scenario.recreate()
            assertEquals(2, runBlocking { db.dao().readMealsForExport().size })
            assertEquals(2, mealCallbacks)
        }
    }
}
