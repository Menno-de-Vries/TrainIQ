package com.trainiq.data.local

import com.trainiq.domain.model.FoodSourceType
import com.trainiq.domain.model.LoggedMealItemType

data class FoodItemStorage(
    val id: Long = 0L,
    val name: String = "",
    val barcode: String? = null,
    val caloriesPer100g: Double = 0.0,
    val proteinPer100g: Double = 0.0,
    val carbsPer100g: Double = 0.0,
    val fatPer100g: Double = 0.0,
    val sourceType: FoodSourceType = FoodSourceType.MANUAL,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
)

data class RecipeStorage(
    val id: Long = 0L,
    val name: String = "",
    val notes: String? = null,
    val totalCookedGrams: Double? = null,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
)

data class RecipeIngredientStorage(
    val id: Long = 0L,
    val recipeId: Long = 0L,
    val foodItemId: Long = 0L,
    val gramsUsed: Double = 0.0,
)

data class LoggedMealStorage(
    val id: Long = 0L,
    val timestamp: Long = 0L,
    val name: String = "",
    val notes: String? = null,
)

data class LoggedMealItemStorage(
    val id: Long = 0L,
    val mealId: Long = 0L,
    val itemType: LoggedMealItemType = LoggedMealItemType.FOOD,
    val referenceId: Long = 0L,
    val name: String = "",
    val gramsUsed: Double = 0.0,
    val calories: Double = 0.0,
    val protein: Double = 0.0,
    val carbs: Double = 0.0,
    val fat: Double = 0.0,
    val notes: String? = null,
)
