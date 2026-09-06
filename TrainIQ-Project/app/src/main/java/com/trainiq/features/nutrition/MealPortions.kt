package com.trainiq.features.nutrition

import com.trainiq.domain.model.LoggedMealItem
import com.trainiq.domain.repository.MealEntryRequest
import com.trainiq.domain.repository.MealEntrySnapshot

/** Snapshot nutrients describe one serving at gramsUsed, unlike library references. */
internal fun MealEntryRequest.withGrams(grams: Double): MealEntryRequest {
    require(grams.isFinite() && grams > 0.0)
    val ratio = if (gramsUsed.isFinite() && gramsUsed > 0.0) grams / gramsUsed else 1.0
    return copy(gramsUsed = grams, snapshot = snapshot?.scaledBy(ratio))
}

private fun MealEntrySnapshot.scaledBy(ratio: Double) = copy(
    calories = calories * ratio,
    protein = protein * ratio,
    carbs = carbs * ratio,
    fat = fat * ratio,
)

internal fun LoggedMealItem.toMealEntrySnapshot(): MealEntrySnapshot = MealEntrySnapshot(
    name = name,
    calories = nutritionSnapshot.calories,
    protein = nutritionSnapshot.protein,
    carbs = nutritionSnapshot.carbs,
    fat = nutritionSnapshot.fat,
).scaledBy(1.0 / servingCount.coerceAtLeast(1))
