package com.trainiq.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.roundToInt

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

    @Test
    fun buildGoalBaseline_maintenanceEqualsBmrTimesActivityMultiplier() {
        val result = buildGoalBaseline(
            heightCm = 195.0,
            weightKg = 107.2,
            bodyFat = 25.0,
            age = 30,
            sex = BiologicalSex.MALE,
            activityLevel = "Lightly active",
            goal = "fat loss",
        )

        assertEquals((result.bmr * result.activityMultiplier).roundToInt().toLong(), result.maintenanceCalories.toLong())
    }

    @Test
    fun buildGoalBaseline_supportsDutchActivityLevelLabels() {
        val dutch = buildGoalBaseline(
            heightCm = 180.0,
            weightKg = 82.0,
            bodyFat = 18.0,
            age = 32,
            sex = BiologicalSex.MALE,
            activityLevel = "Gemiddeld actief",
            goal = "recomp",
        )
        val legacyEnglish = buildGoalBaseline(
            heightCm = 180.0,
            weightKg = 82.0,
            bodyFat = 18.0,
            age = 32,
            sex = BiologicalSex.MALE,
            activityLevel = "Moderately active",
            goal = "recomp",
        )

        assertEquals(legacyEnglish.activityMultiplier, dutch.activityMultiplier, 0.0)
        assertEquals(legacyEnglish.maintenanceCalories, dutch.maintenanceCalories)
    }

    @Test
    fun buildGoalBaseline_forFatLossUsesModerateDeficitBelowMaintenance() {
        val result = buildGoalBaseline(
            heightCm = 195.0,
            weightKg = 107.2,
            bodyFat = 25.0,
            age = 30,
            sex = BiologicalSex.MALE,
            activityLevel = "Lightly active",
            goal = "fat loss",
        )

        assertEquals(2_951, result.maintenanceCalories)
        assertEquals(2_656, result.targetCalories)
    }

    @Test
    fun buildGoalBaseline_macroCaloriesApproximatelyMatchTarget() {
        val result = buildGoalBaseline(
            heightCm = 195.0,
            weightKg = 107.2,
            bodyFat = 25.0,
            age = 30,
            sex = BiologicalSex.MALE,
            activityLevel = "Lightly active",
            goal = "fat loss",
        )

        val macroCalories = result.proteinTarget * 4 + result.carbsTarget * 4 + result.fatTarget * 9

        assertTrue(kotlin.math.abs(result.targetCalories - macroCalories) <= 8)
    }

    @Test
    fun buildGoalBaseline_usesLeanMassForProteinWhenBodyFatIsAvailable() {
        val result = buildGoalBaseline(
            heightCm = 195.0,
            weightKg = 107.2,
            bodyFat = 25.0,
            age = 30,
            sex = BiologicalSex.MALE,
            activityLevel = "Lightly active",
            goal = "fat loss",
        )

        assertEquals(177, result.proteinTarget)
    }
}
