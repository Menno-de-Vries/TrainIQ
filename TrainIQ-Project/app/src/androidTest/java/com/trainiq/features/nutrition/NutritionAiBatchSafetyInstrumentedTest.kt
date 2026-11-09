package com.trainiq.features.nutrition

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.test.*
import com.trainiq.core.datastore.AiPreferences
import com.trainiq.core.theme.TrainIqTheme
import com.trainiq.domain.model.*
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class NutritionAiBatchSafetyInstrumentedTest {
    @Test fun replacementAnalysisCannotApplyOrEditOldReview() = runComposeUiTest {
        var state by mutableStateOf(aiSafetyState(aiSafetyScan()))
        setContent { AiSafetyScreen(state) }
        onNodeWithText("Aan maaltijd toevoegen").performScrollTo().assertExists()
        runOnIdle { state = state.copy(scanResult = null, isAnalyzing = true,
            overview = state.overview.copy(scannedResult = null)) }
        onNodeWithText("Aan maaltijd toevoegen").assertDoesNotExist()
        onNodeWithContentDescription("Naam").assertDoesNotExist()
        onNodeWithText(aiMealAnalyzingLabel()).assertExists()
    }

    @Test fun savingBatchCannotEditOrDeleteItsSubmittedRow() = runComposeUiTest {
        var state by mutableStateOf(aiSafetyState(null))
        var writeCalls = 0
        setContent { AiSafetyScreen(state, onWrite = { writeCalls++ }) }
        onNodeWithContentDescription("Voeding secties openen").performClick()
        onNodeWithText("Producten").performClick()
        onNodeWithText("Foto/AI product").performClick()
        runOnIdle { state = aiSafetyState(aiSafetyScan()) }
        onNodeWithText("Producten opslaan").performScrollTo().performClick()
        waitUntil(timeoutMillis = 10_000) { writeCalls == 1 }
        onNodeWithContentDescription("Naam").performScrollTo().assertIsNotEnabled()
        onNodeWithText("Item verwijderen").performScrollTo().assertIsNotEnabled()
        onNodeWithText("Scanner openen").performScrollTo().assertIsNotEnabled()
        onNodeWithText("Foto importeren").performScrollTo().assertIsNotEnabled()
        onNodeWithContentDescription("Optionele context").performScrollTo().assertIsNotEnabled()
    }
}

@Composable
private fun AiSafetyScreen(state: NutritionUiState.Success, onWrite: () -> Unit = {}) {
    val scope = rememberCoroutineScope()
    val batch = remember { NutritionAiBatch(scope) { onWrite(); kotlinx.coroutines.CompletableDeferred<FoodItem>().await() } }
    val batchState by batch.state.collectAsState()
    LaunchedEffect(state.scanResult) { state.scanResult?.let { batch.beginReview(state.reviewId ?: it.hashCode().toString()) } }
    TrainIqTheme(dynamicColor = false) {
        NutritionScreen(uiState = state.copy(aiBatch = batchState),
            onSaveFood = { _, _, _, _, _, _, _, _, _, _, _ -> onWrite() },
            onSaveRecipe = { _, _, _, _, _, _ -> }, onSaveMeal = { _, _, _, _, _, _ -> },
            onDeleteMeal = {}, onDeleteFood = {}, onDeleteRecipe = {},
            onSetScanResult = {},
            onSetMessage = {}, onDismissMessage = {}, onRetry = {}, onAiScanner = {}, onOpenBarcodeScanner = {},
            onSaveAiBatch = { batch.submit(it) }, onAcknowledgeAiIntegration = batch::acknowledgeIntegration,
            onAcknowledgeAiCompletion = batch::acknowledgeCompletion, onRejectAiIntegration = batch::rejectIntegration)
    }
}

private fun aiSafetyState(result: MealAnalysisResult?) = NutritionUiState.Success(
    overview = NutritionOverview(foods = emptyList(), recipes = emptyList(), meals = emptyList(),
        todaysNutrition = NutritionFacts.Zero, todaysCalories = 0.0, todaysProtein = 0.0, todaysCarbs = 0.0,
        todaysFat = 0.0, todaysMeals = emptyList(), todaysMealsByType = MealType.entries.associateWith { emptyList() },
        todaysWorkoutCalories = 0, scannedResult = result),
    aiPreferences = AiPreferences(enabled = true, apiKey = "synthetic-key"), scanResult = result)

private fun aiSafetyScan() = MealAnalysisResult(
    items = listOf(MealScanItem("Synthetic bowl", 100.0, NutritionFacts(100.0, 10.0, 10.0, 2.0))),
    suggestedMealType = MealType.LUNCH)
