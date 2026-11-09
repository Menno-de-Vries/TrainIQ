package com.trainiq.features.nutrition

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.navigation.compose.*
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import com.trainiq.core.datastore.AiPreferences
import com.trainiq.core.theme.TrainIqTheme
import com.trainiq.domain.model.*
import com.trainiq.navigation.MealDetail
import com.trainiq.navigation.Nutrition
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Actual typed MealDetail entry + retained fixture VM, using the production batch and departure binding.
 * This deliberately does not claim to construct the Hilt NutritionViewModel or exercise Room/network. */
class NutritionAiBatchOwnerInstrumentedTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test fun retainedMealDetailBatchSurvivesRecreateBlocksDepartureAndRetriesOnlyFailedRecipeRow() {
        val fixture = OwnerFixture()
        ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
            scenario.onActivity { installOwnerFixture(it, fixture) }
            compose.onNodeWithText("Open detail").performClick()
            compose.waitUntil(10_000) { fixture.activeModel != null }
            val original = fixture.activeModel!!
            val originalOwner = fixture.activeOwner
            compose.onNodeWithContentDescription("Voeding secties openen").performClick()
            compose.onNodeWithText("Recepten").performClick()
            compose.onNodeWithText("Foto/AI ingrediënten").performClick()
            compose.onAllNodesWithContentDescription("Naam")[1].performScrollTo().performTextReplacement("Corrected B")
            compose.onAllNodesWithContentDescription("Grammen")[1].performScrollTo().performTextReplacement("180")
            compose.onNodeWithText("Als ingrediënten toevoegen").performScrollTo().performClick()
            compose.waitUntil(10_000) { fixture.writes.size == 1 }
            compose.onNodeWithText("Other tab").performClick()
            compose.runOnIdle { assertEquals(originalOwner, fixture.activeOwner) }
            Espresso.closeSoftKeyboard()
            Espresso.pressBack()
            compose.runOnIdle { assertEquals(originalOwner, fixture.activeOwner); assertEquals(2, fixture.notices) }
            scenario.recreate()
            scenario.onActivity { installOwnerFixture(it, fixture) }
            compose.waitUntil(10_000) { fixture.activeOwner == originalOwner }
            compose.runOnIdle { assertSame(original, fixture.activeModel); assertEquals(1, fixture.writes.size) }
            fixture.release.complete(Unit)
            compose.waitUntil(10_000) { !original.batch.state.value.busy && fixture.writes.size == 2 }
            compose.onNodeWithContentDescription("Naam").assertTextContains("Corrected B")
            compose.onNodeWithContentDescription("Grammen").assertTextContains("180")
            compose.onNodeWithText("Other tab").performClick()
            compose.onNodeWithText("AI-controle verlaten?").assertExists()
            compose.onNodeWithText("Blijven controleren").performClick()
            compose.runOnIdle { fixture.failB = false }
            compose.onNodeWithText("Als ingrediënten toevoegen").performScrollTo().performClick()
            compose.waitUntil(10_000) { fixture.writes.size == 3 && !original.batch.state.value.busy }
            compose.onNodeWithText("Recept opslaan").performScrollTo().performClick()
            compose.runOnIdle {
                assertEquals(listOf("Synthetic A", "Corrected B", "Corrected B"), fixture.writes)
                assertEquals(2, fixture.recipeIngredients.size)
                assertEquals(2, fixture.recipeIngredients.map { it.first }.distinct().size)
                assertEquals(180.0, fixture.recipeIngredients.last().second, 0.0)
            }
            compose.onNodeWithText("Other tab").performClick()
            compose.onNodeWithText("Open detail").assertExists()
            compose.onNodeWithText("Open detail").performClick()
            compose.waitUntil(10_000) { fixture.activeModel !== original }
            compose.onNodeWithText("Als ingrediënten toevoegen").assertDoesNotExist()
            compose.runOnIdle { assertEquals(3, fixture.writes.size) }
        }
    }
}

private class OwnerFixture {
    val release = CompletableDeferred<Unit>()
    val foods = MutableStateFlow<List<FoodItem>>(emptyList())
    val writes = java.util.concurrent.CopyOnWriteArrayList<String>()
    var notices = 0
    var failB = true
    var activeModel: OwnerModel? = null
    var activeOwner: String? = null
    var recipeIngredients = emptyList<Pair<Long, Double>>()
    var review: MealAnalysisResult? = MealAnalysisResult(listOf(
        MealScanItem("Synthetic A", 100.0, NutritionFacts(100.0, 10.0, 10.0, 2.0)),
        MealScanItem("Synthetic B", 150.0, NutritionFacts(150.0, 15.0, 15.0, 3.0))))
    suspend fun save(input: ValidatedAiFood): FoodItem {
        writes += input.row.name
        if (input.row.name.endsWith("A")) release.await()
        if (input.row.name.endsWith("B") && failB) error("synthetic write failure")
        val food = FoodItem(foods.value.size + 1L, input.row.name, caloriesPer100g = input.caloriesPer100g,
            proteinPer100g = input.proteinPer100g, carbsPer100g = input.carbsPer100g, fatPer100g = input.fatPer100g,
            sourceType = FoodSourceType.AI, createdAt = 1L, updatedAt = 1L)
        foods.value = foods.value + food
        return food
    }
}

private class OwnerModel(val fixture: OwnerFixture) : ViewModel() {
    val review = MutableStateFlow(fixture.review)
    val batch = NutritionAiBatch(viewModelScope, fixture::save)
    init { batch.beginReview("fixture-review") }
}

private fun installOwnerFixture(activity: ComponentActivity, fixture: OwnerFixture) {
    activity.setContent {
        TrainIqTheme(dynamicColor = false) {
            val nav = rememberNavController()
            val gate = remember { NutritionNavigationGate() }
            NavHost(navController = nav, startDestination = Nutrition) {
                composable<Nutrition> {
                    Button(onClick = { nav.navigate(MealDetail(MealType.LUNCH)) }) { Text("Open detail") }
                }
                composable<MealDetail> { entry ->
                    val model = remember(entry) { ViewModelProvider(entry, object : ViewModelProvider.Factory {
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            @Suppress("UNCHECKED_CAST") return OwnerModel(fixture) as T
                        }
                    })[OwnerModel::class.java] }
                    val lease = rememberNutritionNavigationLease(entry.id, MealDetail::class.qualifiedName.orEmpty(), gate) { model.batch.state.value.busy }
                    val batch by model.batch.state.collectAsState()
                    val review by model.review.collectAsState()
                    val foods by fixture.foods.collectAsState()
                    var navigationState by remember { mutableStateOf(AiNavigationState()) }
                    SideEffect { fixture.activeModel = model; fixture.activeOwner = entry.id }
                    val leave: () -> Unit = { gate.request(Nutrition::class.qualifiedName.orEmpty()) { nav.popBackStack() }; Unit }
                    NutritionOwnerBackHandler(navigationState, leave)
                    Column {
                        Button(onClick = leave) { Text("Other tab") }
                        NutritionScreen(uiState = ownerUiState(review, foods, batch),
                            onSaveFood = { _, _, _, _, _, _, _, _, _, _, _ -> error("Batch must use direct suspend writer") },
                            onSaveRecipe = { _, _, _, _, ingredients, saved -> fixture.recipeIngredients = ingredients; saved() },
                            onSaveMeal = { _, _, _, _, _, _ -> }, onDeleteMeal = {}, onDeleteFood = {}, onDeleteRecipe = {},
                            onSetScanResult = { model.review.value = it; fixture.review = it }, onSetMessage = {}, onDismissMessage = {},
                            onRetry = {}, onAiScanner = {}, onOpenBarcodeScanner = {},
                            onSaveAiBatch = { model.batch.submit(it) }, onAcknowledgeAiIntegration = model.batch::acknowledgeIntegration,
                            onAcknowledgeAiCompletion = model.batch::acknowledgeCompletion, onRejectAiIntegration = model.batch::rejectIntegration,
                            onAiNavigationStateChanged = { state, discard -> navigationState = state; gate.update(lease, state, discard, { fixture.notices++ }) })
                    }
                }
            }
            NutritionLeaveReviewDialog(gate)
        }
    }
}

private fun ownerUiState(review: MealAnalysisResult?, foods: List<FoodItem>, batch: AiFoodBatchState) = NutritionUiState.Success(
    overview = NutritionOverview(foods, emptyList(), emptyList(), NutritionFacts.Zero, 0.0, 0.0, 0.0, 0.0,
        emptyList(), MealType.entries.associateWith { emptyList() }, 0),
    aiPreferences = AiPreferences(true, "synthetic"), scanResult = review, aiBatch = batch,
    reviewId = "fixture-review", reviewContext = "", scanMealType = MealType.LUNCH)
