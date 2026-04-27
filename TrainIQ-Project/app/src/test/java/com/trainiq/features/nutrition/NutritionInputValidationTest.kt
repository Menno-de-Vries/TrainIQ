package com.trainiq.features.nutrition

import com.trainiq.domain.repository.MealEntryRequest
import com.trainiq.domain.repository.MealEntryType
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
}
