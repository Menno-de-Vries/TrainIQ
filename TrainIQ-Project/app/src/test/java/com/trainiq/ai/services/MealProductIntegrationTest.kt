package com.trainiq.ai.services

import com.trainiq.data.remote.enrichMealProducts
import com.trainiq.domain.model.BarcodeProductLookupResult
import com.trainiq.domain.model.FoodProviderMode
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class MealProductIntegrationTest {
    @Test fun extractedIdentityIsVerifiedAfterUserAmountNormalization() = runTest {
        val service = MealAnalysisService(
            aiJsonGenerator = object : AiJsonGenerator {
                override suspend fun generateJson(request: AiRouteRequest): AiRouteResult = AiRouteResult(
                    AiProvider.GEMINI, "fixture", """{"suggestedMealType":"SNACK","items":[{"name":"Acme yoghurt","productName":"yoghurt","brand":"Acme","amountUnit":"g","estimatedGrams":100,"calories":99,"protein":1,"carbs":2,"fat":3}]}""")
            },
            isAiReady = { true }, imageBytesProvider = { byteArrayOf(1) },
            productEnricher = { result, mode ->
                assertEquals(FoodProviderMode.OPEN_FOOD_FACTS, mode)
                assertEquals(250.0, result.items.single().estimatedGrams, 0.0)
                enrichMealProducts(result, mode) { item, _ ->
                    assertEquals("Acme", item.brand)
                    BarcodeProductLookupResult("3017620422003", "yoghurt", 80.0, 5.0, 10.0, 2.0)
                }
            },
        )
        val result = service.analyzeMealImage("fixture.jpg", "250 g Acme yoghurt", 0, FoodProviderMode.OPEN_FOOD_FACTS)
        assertEquals(200.0, result.items.single().nutrition.calories, 0.0)
        assertTrue(result.items.single().notes.orEmpty().contains("openfoodfacts.org/product/"))
    }
}
