package com.trainiq.features.nutrition

import com.trainiq.domain.repository.MealEntryRequest
import com.trainiq.domain.repository.MealEntryType
import com.trainiq.domain.model.EnergyBalanceSnapshot
import com.trainiq.domain.model.MealType
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NutritionInputValidationTest {
    @Test
    fun nutritionNumber_acceptsCommaAndPointDecimals() {
        assertEquals(22.8, "22,8".toNutritionNumberOrNull(max = 100.0) ?: -1.0, 0.0)
        assertEquals(22.8, "22.8".toNutritionNumberOrNull(max = 100.0) ?: -1.0, 0.0)
    }

    @Test
    fun foodValidation_rejectsBlankNamesInvalidNumbersAndExtremeValues() {
        val blank = validateFoodInput("   ", "123", "10", "20", "5")
        val negative = validateFoodInput("Banaan", "-1", "10", "20", "5")
        val extreme = validateFoodInput("Banaan", "5000,1", "10", "20", "5")

        assertEquals("Naam is verplicht.", blank.name)
        assertEquals("Vul een niet-negatieve waarde in.", negative.calories)
        assertEquals("Waarde is te hoog.", extreme.calories)
    }

    @Test
    fun foodValidation_acceptsZeroMacroValues() {
        val errors = validateFoodInput("Water", "0", "0", "0", "0")

        assertFalse(errors.hasErrors)
    }

    @Test
    fun recipeValidation_rejectsMissingNameMissingIngredientsAndInvalidCookedGrams() {
        val errors = validateRecipeInput(
            name = " ",
            cookedGrams = "0",
            ingredients = emptyList(),
        )

        assertEquals("Naam is verplicht.", errors.name)
        assertEquals("Voeg minimaal een ingrediënt toe.", errors.ingredients)
        assertEquals("Vul een positief aantal gram in.", errors.cookedGrams)
    }

    @Test
    fun mealValidation_rejectsMissingNameMissingItemsAndZeroGrams() {
        val missingItems = validateMealInput(" ", emptyList())
        val zeroItem = validateMealInput(
            "Lunch",
            listOf(MealEntryRequest(MealEntryType.FOOD, referenceId = 1L, gramsUsed = 0.0)),
        )

        assertEquals("Naam is verplicht.", missingItems.name)
        assertEquals("Voeg minimaal een product of recept toe.", missingItems.items)
        assertEquals("Vul voor elk item een positief aantal gram in.", zeroItem.items)
    }

    @Test
    fun aiValidation_keepsInvalidItemSpecificErrors() {
        val errors = validateEditableAiItem(
            EditableAiItem(
                name = " ",
                grams = "-20",
                calories = "abc",
                protein = "1",
                carbs = "2",
                fat = "3",
                confidence = null,
                notes = null,
            ),
        )

        assertEquals("Naam is verplicht.", errors.name)
        assertEquals("Vul een positief aantal gram in.", errors.grams)
        assertEquals("Vul een geldige waarde in.", errors.calories)
        assertTrue(errors.hasErrors)
    }

    @Test
    fun aiValidation_acceptsLargePortionTotalsBeforePer100GramConversion() {
        val errors = validateEditableAiItem(
            EditableAiItem(
                name = "Familiepan pasta",
                grams = "1000",
                calories = "6000",
                protein = "180",
                carbs = "900",
                fat = "120",
                confidence = null,
                notes = null,
            ),
        )

        assertFalse(errors.hasErrors)
    }

    @Test
    fun pendingGuard_rejectsSecondSubmitAndAllowsAfterFinish() {
        val started = tryStartNutritionSubmit(emptySet(), NutritionSubmitKey.Food)
        assertTrue(started is NutritionSubmitStartResult.Started)

        started as NutritionSubmitStartResult.Started
        assertEquals(NutritionSubmitStartResult.AlreadyPending, tryStartNutritionSubmit(started.pendingKeys, NutritionSubmitKey.Food))
        assertEquals(emptySet<NutritionSubmitKey>(), finishNutritionSubmit(started.pendingKeys, NutritionSubmitKey.Food))
    }

    @Test
    fun aiBatchProgress_finishesOnSuccessAndFailure() {
        val started = startAiBatchSaveProgress(itemCount = 2)

        val afterSuccess = started.finishOne(success = true)
        val afterFailure = afterSuccess.finishOne(success = false)

        assertEquals(0, afterFailure.pendingCount)
        assertFalse(afterFailure.allSucceeded)
        assertTrue(afterFailure.isFinished)
    }

    @Test
    fun aiBatchProgress_ignoresInvalidEmptyBatch() {
        val started = startAiBatchSaveProgress(itemCount = 0)

        assertEquals(0, started.pendingCount)
        assertTrue(started.isFinished)
    }

    @Test
    fun pendingGuard_tracksAiItemsSeparatelyFromManualFoodSubmit() {
        val foodStarted = tryStartNutritionSubmit(emptySet(), NutritionSubmitKey.Food) as NutritionSubmitStartResult.Started
        val aiStarted = tryStartNutritionSubmit(foodStarted.pendingKeys, NutritionSubmitKey.AiItems)

        assertTrue(aiStarted is NutritionSubmitStartResult.Started)
        aiStarted as NutritionSubmitStartResult.Started
        assertEquals(setOf(NutritionSubmitKey.Food, NutritionSubmitKey.AiItems), aiStarted.pendingKeys)
        assertEquals(NutritionSubmitStartResult.AlreadyPending, tryStartNutritionSubmit(aiStarted.pendingKeys, NutritionSubmitKey.AiItems))
    }

    @Test
    fun nutritionMacroSummary_expandsMacroLabelsForScanability() {
        assertEquals("Eiwit 32 g - Kh 55 g - Vet 18 g", nutritionMacroSummary(32.0, 55.0, 18.0))
    }

    @Test
    fun aiMealAnalyzingLabel_namesTheActualOperation() {
        assertEquals("Maaltijd analyseren...", aiMealAnalyzingLabel())
    }

    @Test
    fun nutritionTabTitles_keepOverviewEntryAndAiResultSeparated() {
        assertEquals(
            listOf("Vandaag", "AI-resultaat", "Recepten", "Producten", "Historie"),
            nutritionTabTitles(),
        )
    }

    @Test
    fun nutritionSectionMenuCopy_keepsCompactMenuAccessible() {
        assertEquals("Voeding secties openen", nutritionSectionMenuButtonDescription())
        assertEquals("Secties", nutritionSectionMenuButtonLabel())
    }

    @Test
    fun mealEditAndSaveLabels_distinguishNewDraftsFromExistingMeals() {
        assertEquals("Hoeveelheid wijzigen", mealEditActionLabel())
        assertEquals("Maaltijd opslaan", mealDraftSaveLabel(isEditing = false))
        assertEquals("Wijzigingen opslaan", mealDraftSaveLabel(isEditing = true))
    }

    @Test
    fun nutritionScreen_usesSectionMenuInsteadOfPersistentTabRow() {
        val source = File("src/main/java/com/trainiq/features/nutrition/NutritionScreen.kt").readText()

        assertFalse(source.contains("ScrollableTabRow"))
        assertTrue(source.contains("showSectionMenu"))
        assertTrue(source.contains("nutritionSectionMenuButtonDescription()"))
        assertTrue(source.contains("Icons.Rounded.Menu"))
        assertTrue(source.contains("NutritionSectionTab(\"AI-resultaat\", 2)"))
        assertTrue(source.contains("NutritionSectionTab(\"Recepten\", 3)"))
        assertFalse(source.contains("\"Toevoegen\", 1"))
    }

    @Test
    fun nutritionScreen_keepsMealAddAndEditFlowsContextual() {
        val source = File("src/main/java/com/trainiq/features/nutrition/NutritionScreen.kt").readText()
        val dailyAddBody = source.substringAfter("onAddToMeal = { type ->").substringBefore("onEditMeal = { meal ->")
        val editBody = source.substringAfter("onEditMeal = { meal ->").substringBefore("onDeleteMeal =")

        assertTrue(dailyAddBody.contains("showAddToMealActions = true"))
        assertFalse(dailyAddBody.contains("selectedTab = 1"))
        assertTrue(editBody.contains("editingMealId = meal.id"))
        assertTrue(editBody.contains("mealDraft.clear()"))
        assertTrue(editBody.contains("mealDraft.addAll"))
        assertTrue(editBody.contains("selectedTab = 1"))
    }

    @Test
    fun savedFoodActionLabels_areExplicitForMealAndEditingActions() {
        assertEquals("Aan maaltijd toevoegen", savedFoodAddToMealLabel())
        assertEquals("Bewerken", savedFoodEditLabel(selected = false))
        assertEquals("Wordt bewerkt", savedFoodEditLabel(selected = true))
    }

    @Test
    fun nutritionEnergyProgress_usesEnergyOutWhenAvailableInsteadOfHardcodedTarget() {
        val balance = EnergyBalanceSnapshot(
            caloriesIn = 1_400,
            caloriesOut = 2_000,
            balance = -600,
            bmr = 1_600,
            tefCalories = 140,
            neatCalories = 160,
            workoutCalories = 100,
        )

        assertEquals(0.7f, nutritionEnergyProgressFraction(calories = 1_400.0, energyBalance = balance), 0.0f)
    }

    @Test
    fun nutritionEnergyProgress_withoutEnergyBalanceDoesNotInventFallbackTarget() {
        assertEquals(0f, nutritionEnergyProgressFraction(calories = 1_400.0, energyBalance = null), 0.0f)
    }

    @Test
    fun mealSectionEmptyText_namesMealAndAction() {
        assertEquals(
            "Nog niets gelogd voor ochtend. Gebruik de plusknop om iets te loggen.",
            mealSectionEmptyText(MealType.BREAKFAST),
        )
    }

    @Test
    fun mealSectionAddAction_usesCompactAccessiblePlusButton() {
        assertEquals("Toevoegen aan Ochtend", mealSectionAddContentDescription(MealType.BREAKFAST))

        val source = File("src/main/java/com/trainiq/features/nutrition/NutritionScreen.kt").readText()
        val mealSection = source.substringAfter("private fun MealSectionCard(").substringBefore("private fun MealEntryRow")

        assertTrue(mealSection.contains("IconButton(onClick = onAdd"))
        assertTrue(mealSection.contains("Icons.Rounded.Add"))
        assertTrue(mealSection.contains("mealSectionAddContentDescription(mealType)"))
        assertFalse(mealSection.contains("Text(\"Toevoegen\")"))
    }

    @Test
    fun nutritionBrowsingList_doesNotClearFocusOnInitialDrag() {
        val source = File("src/main/java/com/trainiq/features/nutrition/NutritionScreen.kt").readText()
        val browsingList = source.substringAfter("state = nutritionListState,").substringBefore("contentPadding = PaddingValues(")

        assertFalse(browsingList.contains("clearFocusOnScrollOrDrag()"))
    }

    @Test
    fun nutritionInitialLoading_usesStableBrowsingListInsteadOfScreenSwap() {
        val source = File("src/main/java/com/trainiq/features/nutrition/NutritionScreen.kt").readText()
        val screenBody = source.substringAfter("fun NutritionScreen(\n    uiState: NutritionUiState,").substringBefore("if (showRecipeActions)")
        val browsingListCount = Regex("LazyColumn\\(\\s*\\n\\s*state = nutritionListState,").findAll(screenBody).count()

        assertEquals(1, browsingListCount)
        assertFalse(screenBody.contains("AnimatedContent("))
        assertTrue(screenBody.contains("when (uiState)"))
        assertTrue(screenBody.contains("NutritionUiState.Loading ->"))
        assertTrue(screenBody.contains("items(8)"))
    }

    @Test
    fun nutritionActionSheets_doNotClearFocusOnScrollDrag() {
        val source = File("src/main/java/com/trainiq/features/nutrition/NutritionScreen.kt").readText()
        val recipeSheet = source.substringAfter("private fun RecipeActionBottomSheet(").substringBefore("@Composable\nprivate fun AddToMealActionSheet")
        val addToMealSheet = source.substringAfter("private fun AddToMealActionSheet(").substringBefore("@Composable\nprivate fun AddFoodForm")

        assertFalse(recipeSheet.contains("clearFocusOnScrollOrDrag()"))
        assertFalse(addToMealSheet.contains("clearFocusOnScrollOrDrag()"))
        assertTrue(addToMealSheet.contains("clearFocusOnTapOutside()"))
    }

    @Test
    fun contextualAddActions_preserveOriginMealTypeAndOpenDraftAfterMutation() {
        val source = File("src/main/java/com/trainiq/features/nutrition/NutritionScreen.kt").readText()
        val addSheet = source.substringAfter("AddToMealActionSheet(").substringBefore("pendingDelete?.let")
        val contextualTargetHelper = source.substringAfter("fun preserveContextualMealTarget()").substringBefore("fun finishAiBatchItem")
        val aiMealSave = source.substringAfter("onSaveToDraft = {").substringBefore("},\n                                )")
        val photoMealSave = source.substringAfter("onAddToMeal = {").substringAfter("startAiFoodBatchSave(").substringBefore("},\n                                    )")
        val savedRecipeAdd = source.substringAfter("onUseInMeal = { recipe ->").substringBefore("onDelete = { pendingDelete = PendingNutritionDelete.Recipe")
        val savedFoodAdd = source.substringAfter("onQuickAdd = { food ->").substringBefore("onDelete = { pendingDelete = PendingNutritionDelete.Food")
        val reuseMeal = source.substringAfter("onReuseMeal = { meal ->").substringBefore("onDeleteMeal = { pendingDelete = PendingNutritionDelete.Meal")

        assertTrue(addSheet.contains("mealType = addToMealType"))
        assertTrue(addSheet.contains("pendingAiMealType = addToMealType"))
        assertTrue(contextualTargetHelper.contains("mealType = addToMealType"))
        assertTrue(aiMealSave.contains("selectedTab = 1"))
        assertTrue(photoMealSave.contains("selectedTab = 1"))
        assertTrue(savedRecipeAdd.contains("preserveContextualMealTarget()"))
        assertTrue(savedRecipeAdd.contains("selectedTab = 1"))
        assertTrue(savedFoodAdd.contains("preserveContextualMealTarget()"))
        assertTrue(savedFoodAdd.contains("selectedTab = 1"))
        assertTrue(reuseMeal.contains("mealType = meal.mealType"))
        assertTrue(reuseMeal.contains("selectedTab = 1"))
    }

    @Test
    fun mealHistoryReuse_restoresSnapshotItemsIntoEditableDraft() {
        val source = File("src/main/java/com/trainiq/features/nutrition/NutritionScreen.kt").readText()
        val reuseMeal = source.substringAfter("onReuseMeal = { meal ->").substringBefore("onDeleteMeal = { pendingDelete = PendingNutritionDelete.Meal")
        val mealHistoryCard = source.substringAfter("private fun MealHistoryCard(").substringBefore("@Composable\nprivate fun EditableAiItemCard")

        assertTrue(reuseMeal.contains("mealType = meal.mealType"))
        assertTrue(reuseMeal.contains("mealName = meal.name"))
        assertTrue(reuseMeal.contains("mealNotes = meal.notes.orEmpty()"))
        assertTrue(reuseMeal.contains("mealDraft.clear()"))
        assertTrue(reuseMeal.contains("mealDraft.addAll(meal.items.map"))
        assertTrue(reuseMeal.contains("itemType = if (it.itemType.name == \"FOOD\") MealEntryType.FOOD else MealEntryType.RECIPE"))
        assertTrue(reuseMeal.contains("referenceId = it.referenceId"))
        assertTrue(reuseMeal.contains("gramsUsed = it.gramsUsed"))
        assertTrue(reuseMeal.contains("servingCount = it.servingCount"))
        assertTrue(reuseMeal.contains("notes = it.notes"))
        assertTrue(reuseMeal.contains("selectedTab = 1"))
        assertTrue(mealHistoryCard.contains("Opnieuw gebruiken"))
        assertTrue(mealHistoryCard.contains("voedingssnapshots"))
    }

    @Test
    fun pendingBarcodeResult_populatesCorrectNutritionTargetAndClearsNavigationResult() {
        val source = File("src/main/java/com/trainiq/features/nutrition/NutritionScreen.kt").readText()
        val pendingBarcodeEffect = source.substringAfter("LaunchedEffect(pendingBarcode) {").substringBefore("LaunchedEffect(barcodeLookupResult)")

        assertTrue(pendingBarcodeEffect.contains("if (pendingBarcode != null)"))
        assertTrue(pendingBarcodeEffect.contains("successState?.scanTarget == ScanTarget.RECIPE_DRAFT"))
        assertTrue(pendingBarcodeEffect.contains("quickIngredientBarcode = pendingBarcode"))
        assertTrue(pendingBarcodeEffect.contains("selectedTab = 3"))
        assertTrue(pendingBarcodeEffect.contains("quickIngredientName = quickIngredientName.ifBlank { \"Gescand product\" }"))
        assertTrue(pendingBarcodeEffect.contains("onLookupBarcodeProduct(pendingBarcode, BarcodeLookupTarget.RECIPE_DRAFT)"))
        assertTrue(pendingBarcodeEffect.contains("barcode = pendingBarcode"))
        assertTrue(pendingBarcodeEffect.contains("selectedTab = 4"))
        assertTrue(pendingBarcodeEffect.contains("onLookupBarcodeProduct(pendingBarcode, BarcodeLookupTarget.FOOD_EDITOR)"))
        assertTrue(pendingBarcodeEffect.contains("onSetScanTarget(ScanTarget.FOOD_EDITOR)"))
        assertTrue(pendingBarcodeEffect.contains("onBarcodeClear()"))
    }

    @Test
    fun savedRecipeEditFlow_reusesSelectedRecipeIdAndShowsEditSaveCopy() {
        val source = File("src/main/java/com/trainiq/features/nutrition/NutritionScreen.kt").readText()
        val selectedRecipeEffect = source.substringAfter("LaunchedEffect(selectedRecipe?.id) {").substringBefore("LaunchedEffect(scanResult)")
        val recipeEditor = source.substringAfter("RecipeEditorCard(").substringBefore("onScanBarcodeForRecipe =")
        val savedRecipesCard = source.substringAfter("SavedRecipesCard(").substringBefore("onUseInMeal = { recipe ->")
        val savedRecipesBody = source.substringAfter("private fun SavedRecipesCard(").substringBefore("@Composable\nprivate fun MealDraftReviewCard")
        val recipeEditorBody = source.substringAfter("private fun RecipeEditorCard(").substringBefore("@Composable\nprivate fun RecipeTotalsCard")

        assertTrue(selectedRecipeEffect.contains("recipeName = it.name"))
        assertTrue(selectedRecipeEffect.contains("recipeDraft.clear()"))
        assertTrue(selectedRecipeEffect.contains("recipeDraft.addAll"))
        assertTrue(recipeEditor.contains("isEditing = selectedRecipeId != null"))
        assertTrue(recipeEditor.contains("onSaveRecipe(selectedRecipeId, recipeName, recipeNotes, recipeCookedGrams, recipeDraft.toList())"))
        assertTrue(recipeEditor.contains("selectedRecipeId = null"))
        assertTrue(savedRecipesCard.contains("onSelect = { selectedRecipeId = it }"))
        assertTrue(savedRecipesBody.contains("Text(if (selectedRecipeId == recipe.id) \"Bewerken\" else \"Bewerk\")"))
        assertTrue(recipeEditorBody.contains("if (isEditing) \"Wijzigingen opslaan\" else \"Recept opslaan\""))
    }

    @Test
    fun nutritionEnergyBalanceCopy_namesDeficitAndBreakdownUnits() {
        val balance = EnergyBalanceSnapshot(
            caloriesIn = 1_400,
            caloriesOut = 2_000,
            balance = -600,
            bmr = 1_600,
            tefCalories = 140,
            neatCalories = 160,
            workoutCalories = 100,
        )

        assertEquals("Netto calorieën 600 kcal tekort", nutritionEnergyBalanceSummary(balance))
        assertEquals("TEF 140 kcal - NEAT 160 kcal - Training 100 kcal", nutritionEnergyBreakdownText(balance))
    }
}
