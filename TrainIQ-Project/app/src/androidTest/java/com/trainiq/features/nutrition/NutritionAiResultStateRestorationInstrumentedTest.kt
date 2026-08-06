package com.trainiq.features.nutrition

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.StateRestorationTester
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.runComposeUiTest
import com.trainiq.core.datastore.AiPreferences
import com.trainiq.core.theme.TrainIqTheme
import com.trainiq.domain.model.MealAnalysisResult
import com.trainiq.domain.model.MealAnalysisSource
import com.trainiq.domain.model.MealScanItem
import com.trainiq.domain.model.MealType
import com.trainiq.domain.model.NutritionFacts
import com.trainiq.domain.model.NutritionOverview
import org.junit.Test

class NutritionAiResultStateRestorationInstrumentedTest {
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun editedAiResultAndValidationErrorSurviveStateRestoration() = runComposeUiTest {
        val restorationTester = StateRestorationTester(this)
        val uiState = syntheticUiState(scanResult = syntheticScanResult())

        restorationTester.setContent {
            SyntheticNutritionScreen(uiState)
        }

        onNodeWithText("AI-items aan maaltijd toevoegen").assertIsDisplayed()
        onNodeWithContentDescription("Naam").performTextReplacement("Bewerkte bowl")
        onNodeWithContentDescription("Grammen").performTextReplacement("180")
        onNodeWithContentDescription("Vet").performTextReplacement("-4")
        onNodeWithContentDescription("Vet").assertTextContains("-4")
        onNodeWithText("AI-items aan maaltijd toevoegen").performScrollTo().performClick()
        onNodeWithContentDescription("Vet").assertTextContains("Vul een niet-negatieve waarde in.")

        restorationTester.emulateSaveAndRestore()

        onNodeWithContentDescription("Naam").assertTextContains("Bewerkte bowl")
        onNodeWithContentDescription("Grammen").assertTextContains("180")
        onNodeWithContentDescription("Vet").assertTextContains("-4")
        onNodeWithContentDescription("Vet").assertTextContains("Vul een niet-negatieve waarde in.")
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun recipeTargetSurvivesAiResultStateRestoration() = runComposeUiTest {
        val restorationTester = StateRestorationTester(this)
        var uiState by mutableStateOf(syntheticUiState(scanResult = null, aiEnabled = true))

        restorationTester.setContent {
            SyntheticNutritionScreen(uiState)
        }

        onNodeWithText("Recepten").performClick()
        onNodeWithText("+ Toevoegen").performClick()
        onNodeWithText("Product via foto/AI toevoegen").performClick()
        runOnIdle {
            uiState = syntheticUiState(scanResult = syntheticScanResult(), aiEnabled = true)
        }
        onNodeWithText("Fotocontrole").assertIsDisplayed()
        onNodeWithText("AI-items aan maaltijd toevoegen").assertDoesNotExist()

        restorationTester.emulateSaveAndRestore()

        onNodeWithText("Fotocontrole").assertIsDisplayed()
        onNodeWithText("AI-items aan maaltijd toevoegen").assertDoesNotExist()
    }
}

@Composable
private fun SyntheticNutritionScreen(uiState: NutritionUiState.Success) {
    TrainIqTheme(dynamicColor = false) {
        NutritionScreen(
            uiState = uiState,
            onSaveFood = { _, _, _, _, _, _, _, _, _, _ -> },
            onSaveRecipe = { _, _, _, _, _, _ -> },
            onSaveMeal = { _, _, _, _, _, _ -> },
            onDeleteMeal = {},
            onDeleteFood = {},
            onDeleteRecipe = {},
            onTryStartAiBatchSave = { true },
            onFinishAiBatchSave = {},
            onSetScanResult = {},
            onSetMessage = {},
            onDismissMessage = {},
            onOpenAiScanner = {},
            onOpenBarcodeScanner = {},
        )
    }
}

private fun syntheticUiState(
    scanResult: MealAnalysisResult?,
    aiEnabled: Boolean = false,
): NutritionUiState.Success = NutritionUiState.Success(
    overview = NutritionOverview(
        foods = emptyList(),
        recipes = emptyList(),
        meals = emptyList(),
        todaysNutrition = NutritionFacts.Zero,
        todaysCalories = 0.0,
        todaysProtein = 0.0,
        todaysCarbs = 0.0,
        todaysFat = 0.0,
        todaysMeals = emptyList(),
        todaysMealsByType = MealType.entries.associateWith { emptyList() },
        todaysWorkoutCalories = 0,
        scannedResult = scanResult,
    ),
    aiPreferences = AiPreferences(
        enabled = aiEnabled,
        apiKey = if (aiEnabled) "synthetic-test-key" else "",
    ),
    scanResult = scanResult,
)

private fun syntheticScanResult(): MealAnalysisResult = MealAnalysisResult(
    items = listOf(
        MealScanItem(
            name = "Originele bowl",
            estimatedGrams = 100.0,
            nutrition = NutritionFacts(
                calories = 500.0,
                protein = 20.0,
                carbs = 30.0,
                fat = 10.0,
            ),
            confidence = "medium",
            notes = "Synthetische fixture",
        ),
    ),
    suggestedMealType = MealType.DINNER,
    notes = "Controleer dit resultaat",
    source = MealAnalysisSource.LOCAL_FALLBACK,
)
