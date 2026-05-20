package com.trainiq.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BarcodeProductLookupServiceTest {
    @Test
    fun parseOpenFoodFactsProduct_withNutritionPer100g_returnsProduct() {
        val product = parseOpenFoodFactsProduct(
            barcode = "8712345678901",
            json = """
                {
                  "status": 1,
                  "product": {
                    "product_name": "Magere kwark",
                    "nutriments": {
                      "energy-kcal_100g": 56,
                      "proteins_100g": "10,2",
                      "carbohydrates_100g": 4.0,
                      "fat_100g": 0.2
                    }
                  }
                }
            """.trimIndent(),
        )

        requireNotNull(product)
        assertEquals("8712345678901", product.barcode)
        assertEquals("Magere kwark", product.name)
        assertEquals(56.0, product.caloriesPer100g, 0.0)
        assertEquals(10.2, product.proteinPer100g, 0.0)
        assertEquals(4.0, product.carbsPer100g, 0.0)
        assertEquals(0.2, product.fatPer100g, 0.0)
    }

    @Test
    fun parseOpenFoodFactsProduct_withoutCompleteNutrition_returnsNull() {
        val product = parseOpenFoodFactsProduct(
            barcode = "8712345678901",
            json = """
                {
                  "status": 1,
                  "product": {
                    "product_name": "Onvolledig product",
                    "nutriments": {
                      "energy-kcal_100g": 56
                    }
                  }
                }
            """.trimIndent(),
        )

        assertNull(product)
    }
}
