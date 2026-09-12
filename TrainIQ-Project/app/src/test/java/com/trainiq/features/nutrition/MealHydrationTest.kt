package com.trainiq.features.nutrition

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class MealHydrationTest {
    @Test fun checkedGramsArePerServingMlAndFollowEdits() {
        assertEquals(250.0, mealHydrationMl(250.0, true), 0.0)
        assertEquals(500.0, mealHydrationMl(250.0, true) * 2, 0.0)
        assertEquals(375.5, mealHydrationMl(375.5, true), 0.0)
        assertEquals(0.0, mealHydrationMl(375.5, false), 0.0)
    }

    @Test fun invalidGramsCannotBecomeHydration() {
        listOf(0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY, 100_000.1).forEach { grams ->
            assertThrows(IllegalArgumentException::class.java) { mealHydrationMl(grams, true) }
        }
    }
}
