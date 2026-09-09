package com.trainiq.data.remote

import com.trainiq.domain.model.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class FoodProviderPolicyTest {
    @Test fun gatewayPayloadPreservesProviderAndRejectsInvalidNutrition() {
        val json = """{"name":"Fixture","caloriesPer100g":60,"proteinPer100g":10,"carbsPer100g":4,"fatPer100g":0.5}"""
        val result = parseFatSecretGatewayProduct("0012345678905", json)
        assertEquals(FoodProviderMode.FATSECRET, result.provider)
        assertEquals(60.0, result.caloriesPer100g, 0.0)
        listOf("{", "{}", json.replace("60", "-1")).forEach { invalid ->
            try { parseFatSecretGatewayProduct("0012345678905", invalid); fail() }
            catch (error: FoodLookupException) { assertEquals(FoodLookupFailure.INVALID_RESPONSE, error.failure) }
        }
    }
    private val barcode = "012345678905"
    private val product = BarcodeProductLookupResult(barcode, "Fixture", 100.0, 2.0, 3.0, 4.0)
    @Test fun primaryHitNeverCallsFallback() = runTest {
        assertEquals(product, lookupFoodProduct(barcode, FoodProviderMode.AUTOMATIC, { product }, { error("Unexpected fallback") }))
    }
    @Test fun missFallsBackAndBothMissStayNotFound() = runTest {
        assertEquals(product, lookupFoodProduct(barcode, FoodProviderMode.AUTOMATIC, { null }, { product }))
        assertNull(lookupFoodProduct(barcode, FoodProviderMode.AUTOMATIC, { null }, { null }))
    }
    @Test fun retryableErrorFallsBackAndManualSelectionBypassesOtherProvider() = runTest {
        assertEquals(product, lookupFoodProduct(barcode, FoodProviderMode.AUTOMATIC, { throw FoodLookupException(FoodLookupFailure.NETWORK) }, { product }))
        assertEquals(product, lookupFoodProduct(barcode, FoodProviderMode.FATSECRET, { error("Primary called") }, { product }))
        assertNull(lookupFoodProduct(barcode, FoodProviderMode.OPEN_FOOD_FACTS, { null }, { error("Fallback called") }))
    }
    @Test fun invalidAndAuthenticationStayDistinct() = runTest {
        try { lookupFoodProduct("bad", FoodProviderMode.AUTOMATIC, { error("called") }, { error("called") }); fail() }
        catch (error: FoodLookupException) { assertEquals(FoodLookupFailure.INVALID_BARCODE, error.failure) }
        try { lookupFoodProduct(barcode, FoodProviderMode.AUTOMATIC, { null }, { throw FoodLookupException(FoodLookupFailure.AUTH) }); fail() }
        catch (error: FoodLookupException) { assertEquals(FoodLookupFailure.AUTH, error.failure) }
        assertEquals("0012345678905", fatSecretGtin13(barcode))
        assertEquals("0000096385074", fatSecretGtin13("96385074"))
        assertNull(normalizedBarcode("abc012345678905"))
    }
}
