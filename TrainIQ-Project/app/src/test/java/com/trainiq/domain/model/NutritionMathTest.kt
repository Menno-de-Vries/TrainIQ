package com.trainiq.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class NutritionMathTest {

    @Test
    fun nutritionForGrams_withDecimalAmount_scalesPer100gValues() {
        val food = FoodItem(
            id = 1L,
            name = "Brood",
            caloriesPer100g = 250.0,
            proteinPer100g = 9.0,
            carbsPer100g = 48.0,
            fatPer100g = 3.2,
            sourceType = FoodSourceType.MANUAL,
            createdAt = 0L,
            updatedAt = 0L,
        )

        val result = food.nutritionForGrams(62.5)

        assertEquals(156.2, result.calories, 0.0)
        assertEquals(5.6, result.protein, 0.0)
        assertEquals(30.0, result.carbs, 0.0)
        assertEquals(2.0, result.fat, 0.0)
    }

    @Test
    fun nutritionFactsPlus_combinesMealSectionTotals() {
        val breakfast = NutritionFacts(calories = 250.0, protein = 20.0, carbs = 30.0, fat = 5.0)
        val drink = NutritionFacts(calories = 85.0, protein = 8.5, carbs = 10.0, fat = 0.5)

        val result = breakfast + drink

        assertEquals(335.0, result.calories, 0.0)
        assertEquals(28.5, result.protein, 0.0)
        assertEquals(40.0, result.carbs, 0.0)
        assertEquals(5.5, result.fat, 0.0)
    }

    @Test
    fun rounded_roundsAllNutritionValuesToOneDecimal() {
        val result = NutritionFacts(
            calories = 123.456,
            protein = 12.34,
            carbs = 45.65,
            fat = 7.04,
        ).rounded()

        assertEquals(123.5, result.calories, 0.0)
        assertEquals(12.3, result.protein, 0.0)
        assertEquals(45.6, result.carbs, 0.0)
        assertEquals(7.0, result.fat, 0.0)
    }
}
