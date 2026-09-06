package com.trainiq.features.nutrition

import com.trainiq.domain.model.LoggedMealItem
import com.trainiq.domain.model.LoggedMealItemType
import com.trainiq.domain.model.NutritionFacts
import com.trainiq.domain.repository.MealEntryRequest
import com.trainiq.domain.repository.MealEntrySnapshot
import com.trainiq.domain.repository.MealEntryType
import org.junit.Assert.*
import org.junit.Test

class MealPortionsTest {
    @Test fun resizingSnapshotScalesEveryNutrientWithoutCompounding() {
        val request = MealEntryRequest(MealEntryType.SNAPSHOT, 0L, 100.0, 2,
            snapshot = MealEntrySnapshot("Haver", 200.0, 10.0, 30.0, 5.0))
        val doubled = request.withGrams(200.0)
        assertEquals(MealEntrySnapshot("Haver", 400.0, 20.0, 60.0, 10.0), doubled.snapshot)
        assertEquals(request, doubled.withGrams(100.0))
        assertEquals(2, doubled.servingCount)
        assertEquals(doubled, doubled.withGrams(200.0))
    }

    @Test fun libraryReferencesKeepTheirIdentityAndRejectInvalidGrams() {
        for (type in listOf(MealEntryType.FOOD, MealEntryType.RECIPE)) {
            val request = MealEntryRequest(type, 12L, 100.0)
            assertEquals(request.copy(gramsUsed = 50.0), request.withGrams(50.0))
            for (grams in listOf(0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY)) {
                assertThrows(IllegalArgumentException::class.java) { request.withGrams(grams) }
            }
        }
    }

    @Test fun editingLoggedSnapshotsRestoresPerServingValues() {
        for (servings in listOf(1, 2, 3)) {
            val item = LoggedMealItem(1L, 2L, LoggedMealItemType.SNAPSHOT, 0L, "Haver", 100.0,
                servings, NutritionFacts(200.0 * servings, 10.0 * servings, 30.0 * servings, 5.0 * servings))
            assertEquals(MealEntrySnapshot("Haver", 200.0, 10.0, 30.0, 5.0), item.toMealEntrySnapshot())
        }
    }
}
