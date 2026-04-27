package com.trainiq.features.nutrition

import com.trainiq.domain.repository.MealEntryRequest

private const val MAX_CALORIES_PER_100G = 5000.0
private const val MAX_MACRO_PER_100G = 1000.0
private const val MAX_GRAMS = 100_000.0
private const val MAX_AI_TOTAL_CALORIES = 100_000.0
private const val MAX_AI_TOTAL_MACRO = 100_000.0

internal data class FoodFieldErrors(
    val name: String? = null,
    val calories: String? = null,
    val protein: String? = null,
    val carbs: String? = null,
    val fat: String? = null,
) {
    val hasErrors: Boolean get() = listOf(name, calories, protein, carbs, fat).any { it != null }
}

internal data class RecipeFieldErrors(
    val name: String? = null,
    val cookedGrams: String? = null,
    val ingredientGrams: String? = null,
    val ingredients: String? = null,
) {
    val hasErrors: Boolean get() = listOf(name, cookedGrams, ingredientGrams, ingredients).any { it != null }
}

internal data class MealFieldErrors(
    val name: String? = null,
    val items: String? = null,
) {
    val hasErrors: Boolean get() = listOf(name, items).any { it != null }
}

internal data class QuickAddFieldErrors(
    val grams: String? = null,
) {
    val hasErrors: Boolean get() = grams != null
}

internal data class AiItemFieldErrors(
    val name: String? = null,
    val grams: String? = null,
    val calories: String? = null,
    val protein: String? = null,
    val carbs: String? = null,
    val fat: String? = null,
) {
    val hasErrors: Boolean get() = listOf(name, grams, calories, protein, carbs, fat).any { it != null }
}

enum class NutritionSubmitKey { Food, Recipe, Meal, QuickIngredient, AiItems, Delete }

internal sealed interface NutritionSubmitStartResult {
    data class Started(val pendingKeys: Set<NutritionSubmitKey>) : NutritionSubmitStartResult
    data object AlreadyPending : NutritionSubmitStartResult
}

internal fun tryStartNutritionSubmit(
    pendingKeys: Set<NutritionSubmitKey>,
    key: NutritionSubmitKey,
): NutritionSubmitStartResult = if (key in pendingKeys) {
    NutritionSubmitStartResult.AlreadyPending
} else {
    NutritionSubmitStartResult.Started(pendingKeys + key)
}

internal fun finishNutritionSubmit(
    pendingKeys: Set<NutritionSubmitKey>,
    key: NutritionSubmitKey,
): Set<NutritionSubmitKey> = pendingKeys - key

internal data class AiBatchSaveProgress(
    val pendingCount: Int,
    val failedCount: Int = 0,
) {
    val isFinished: Boolean get() = pendingCount <= 0
    val allSucceeded: Boolean get() = isFinished && failedCount == 0

    fun finishOne(success: Boolean): AiBatchSaveProgress = copy(
        pendingCount = (pendingCount - 1).coerceAtLeast(0),
        failedCount = failedCount + if (success) 0 else 1,
    )
}

internal fun startAiBatchSaveProgress(itemCount: Int): AiBatchSaveProgress =
    AiBatchSaveProgress(pendingCount = itemCount.coerceAtLeast(0))

internal fun String.toNutritionNumberOrNull(max: Double): Double? {
    val parsed = trim().replace(',', '.').toDoubleOrNull() ?: return null
    return parsed.takeIf { it.isFinite() && it >= 0.0 && it <= max }
}

internal fun validateFoodInput(
    name: String,
    calories: String,
    protein: String,
    carbs: String,
    fat: String,
): FoodFieldErrors = FoodFieldErrors(
    name = requiredNameError(name),
    calories = nutritionNumberError(calories, max = MAX_CALORIES_PER_100G, allowZero = true),
    protein = nutritionNumberError(protein, max = MAX_MACRO_PER_100G, allowZero = true),
    carbs = nutritionNumberError(carbs, max = MAX_MACRO_PER_100G, allowZero = true),
    fat = nutritionNumberError(fat, max = MAX_MACRO_PER_100G, allowZero = true),
)

internal fun validateRecipeInput(
    name: String,
    cookedGrams: String,
    ingredients: List<Pair<Long, Double>>,
): RecipeFieldErrors = RecipeFieldErrors(
    name = requiredNameError(name),
    cookedGrams = optionalPositiveGramError(cookedGrams),
    ingredients = when {
        ingredients.isEmpty() -> "Voeg minimaal een ingrediënt toe."
        ingredients.any { it.second <= 0.0 || !it.second.isFinite() } -> "Vul voor elk ingrediënt een positief aantal gram in."
        else -> null
    },
)

internal fun validateIngredientGrams(grams: String): RecipeFieldErrors =
    RecipeFieldErrors(ingredientGrams = requiredPositiveGramError(grams))

internal fun validateMealInput(name: String, items: List<MealEntryRequest>): MealFieldErrors = MealFieldErrors(
    name = requiredNameError(name),
    items = when {
        items.isEmpty() -> "Voeg minimaal een product of recept toe."
        items.any { it.gramsUsed <= 0.0 || !it.gramsUsed.isFinite() } -> "Vul voor elk item een positief aantal gram in."
        else -> null
    },
)

internal fun validateQuickAddGrams(grams: String): QuickAddFieldErrors =
    QuickAddFieldErrors(grams = requiredPositiveGramError(grams))

internal fun validateEditableAiItem(item: EditableAiItem): AiItemFieldErrors = AiItemFieldErrors(
    name = requiredNameError(item.name),
    grams = requiredPositiveGramError(item.grams),
    calories = nutritionNumberError(item.calories, max = MAX_AI_TOTAL_CALORIES, allowZero = true),
    protein = nutritionNumberError(item.protein, max = MAX_AI_TOTAL_MACRO, allowZero = true),
    carbs = nutritionNumberError(item.carbs, max = MAX_AI_TOTAL_MACRO, allowZero = true),
    fat = nutritionNumberError(item.fat, max = MAX_AI_TOTAL_MACRO, allowZero = true),
)

internal fun requiredPositiveGramError(value: String): String? =
    nutritionNumberError(value, max = MAX_GRAMS, allowZero = false)

private fun optionalPositiveGramError(value: String): String? =
    if (value.isBlank()) null else requiredPositiveGramError(value)

private fun requiredNameError(value: String): String? =
    if (value.isBlank()) "Naam is verplicht." else null

private fun nutritionNumberError(value: String, max: Double, allowZero: Boolean): String? {
    if (value.isBlank()) return if (allowZero) "Vul een waarde in." else "Vul een positief aantal gram in."
    val parsed = value.trim().replace(',', '.').toDoubleOrNull() ?: return "Vul een geldige waarde in."
    if (!parsed.isFinite()) return "Vul een geldige waarde in."
    if (parsed < 0.0) return if (allowZero) "Vul een niet-negatieve waarde in." else "Vul een positief aantal gram in."
    if (!allowZero && parsed == 0.0) return "Vul een positief aantal gram in."
    if (parsed > max) return "Waarde is te hoog."
    return null
}
