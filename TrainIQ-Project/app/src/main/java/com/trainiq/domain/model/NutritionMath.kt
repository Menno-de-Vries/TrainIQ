package com.trainiq.domain.model

import kotlin.math.round

fun FoodItem.nutritionForGrams(grams: Double): NutritionFacts {
    val multiplier = grams / 100.0
    return NutritionFacts(
        calories = caloriesPer100g * multiplier,
        protein = proteinPer100g * multiplier,
        carbs = carbsPer100g * multiplier,
        fat = fatPer100g * multiplier,
    ).rounded()
}

fun NutritionFacts.rounded(): NutritionFacts = NutritionFacts(
    calories = calories.round1(),
    protein = protein.round1(),
    carbs = carbs.round1(),
    fat = fat.round1(),
)

fun Double.round1(): Double = round(this * 10.0) / 10.0
