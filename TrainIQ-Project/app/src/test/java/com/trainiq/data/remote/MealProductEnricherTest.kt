package com.trainiq.data.remote

import com.trainiq.domain.model.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import java.util.concurrent.atomic.AtomicInteger

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class MealProductEnricherTest {
    private val item = MealScanItem("Acme yoghurt vanille", 250.0, NutritionFacts(99.0, 1.0, 2.0, 3.0),
        productName = "yoghurt", brand = "Acme", variant = "vanille", amountUnit = "g")
    private val product = BarcodeProductLookupResult("3017620422003", "Yoghurt vanille", 80.0, 5.0, 10.0, 2.0)
    private fun result(value: MealScanItem = item) = MealAnalysisResult(listOf(value))

    @Test fun brandedProductIsNormalizedToEnteredGramsAndAttributed() = runTest {
        val actual = enrichMealProducts(result()) { _, _ -> product }.items.single()
        assertEquals(200.0, actual.nutrition.calories, 0.0)
        assertEquals(12.5, actual.nutrition.protein, 0.0)
        assertEquals(250.0, actual.estimatedGrams, 0.0)
        assertTrue(actual.notes.orEmpty().contains("Open Food Facts"))
    }

    @Test fun missingAndFailedLookupKeepOriginalEstimation() = runTest {
        assertEquals(result(), enrichMealProducts(result()) { _, _ -> null })
        assertEquals(result(), enrichMealProducts(result()) { _, _ -> throw java.io.IOException("offline") })
    }

    @Test fun timeoutKeepsEstimateAndBoundsWholeMeal() = runTest {
        assertEquals(result(), enrichMealProducts(result()) { _, _ -> delay(10_000); product })
        assertEquals(3_000L, testScheduler.currentTime)
    }

    @Test fun callerCancellationPropagates() = runTest {
        val error = runCatching { enrichMealProducts(result()) { _, _ -> throw CancellationException("cancel") } }.exceptionOrNull()
        assertTrue(error is CancellationException)
    }

    @Test fun volumeUnknownUnitAndGenericFoodsNeverQuery() = runTest {
        for (value in listOf(item.copy(amountUnit = "ml"), item.copy(amountUnit = null), item.copy(brand = null))) {
            assertEquals(result(value), enrichMealProducts(result(value)) { _, _ -> fail("Unexpected lookup"); null })
        }
    }

    @Test fun selectedProviderIsPassedToBarcodeLookup() = runTest {
        enrichMealProducts(result(item.copy(barcode = "3017620422003")), FoodProviderMode.FATSECRET) { _, mode ->
            assertEquals(FoodProviderMode.FATSECRET, mode); null
        }
    }

    @Test fun duplicateProductsReuseLookupAndKeepIndependentAmounts() = runTest {
        var calls = 0
        val actual = enrichMealProducts(MealAnalysisResult(listOf(item, item.copy(estimatedGrams = 100.0)))) { _, _ -> calls++; product }
        assertEquals(1, calls)
        assertEquals(listOf(200.0, 80.0), actual.items.map { it.nutrition.calories })
    }

    @Test fun beverageBasisDoesNotInventGramToMlConversion() = runTest {
        assertEquals(result(), enrichMealProducts(result()) { _, _ -> product.copy(isBeverage = true) })
    }

    @Test fun liquidNonBeverageRetainsEditableEstimateInsteadOfInventingDensity() = runTest {
        val oil = parseOpenFoodFactsProduct("3017620422003", """{"status":1,"product":{"product_name":"Olijfolie","product_quantity":500,"product_quantity_unit":"ml","categories_tags":["en:olive-oils"],"nutriments":{"energy-kcal_100g":824,"proteins_100g":0,"carbohydrates_100g":0,"fat_100g":92}}}""")
        requireNotNull(oil)
        assertFalse(oil.isBeverage)
        assertEquals(result(), enrichMealProducts(result()) { _, _ -> oil })
    }

    @Test fun uncertainExtractedIdentityRetainsEstimateWithoutLookup() = runTest {
        val uncertain = result(item.copy(confidence = "low", barcode = "3017620422003"))
        assertEquals(uncertain, enrichMealProducts(uncertain) { _, _ -> fail("Uncertain barcode must not query"); null })
    }

    @Test fun repeatedScanUsesCachedProductButRecalculatesEnteredAmount() = kotlinx.coroutines.runBlocking {
        val calls = AtomicInteger()
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            calls.incrementAndGet()
            assertFalse(chain.request().url.toString().contains("private workout note"))
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200).message("OK")
                .body(searchJson("Yoghurt vanille", "Acme").toResponseBody()).build()
        }.build()
        val enricher = MealProductEnricher(client)
        assertEquals(200.0, enricher.enrich(result(item.copy(notes = "private workout note")), FoodProviderMode.OPEN_FOOD_FACTS).items.single().nutrition.calories, 0.0)
        assertEquals(80.0, enricher.enrich(result(item.copy(estimatedGrams = 100.0)), FoodProviderMode.OPEN_FOOD_FACTS).items.single().nutrition.calories, 0.0)
        assertEquals(1, calls.get())
    }

    @Test fun exactBrandAndVariantRequiredAndAmbiguousIdentityIsRejected() {
        assertEquals(product, selectExactMealProduct(item, searchJson("Yoghurt vanille", "Acme")))
        assertNull(selectExactMealProduct(item, searchJson("Yoghurt aardbei", "Acme")))
        assertNull(selectExactMealProduct(item, searchJson("Yoghurt vanille", "Other")))
        val duplicate = searchJson("Yoghurt vanille", "Acme").replace("]}", "," + searchJson("Yoghurt vanille", "Acme").substringAfter("[").substringBeforeLast("]") + "]}")
        assertNull(selectExactMealProduct(item, duplicate))
    }

    private fun searchJson(name: String, brand: String) = """{"products":[{"code":"3017620422003","product_name":"$name","brands":"$brand","nutriments":{"energy-kcal_100g":80,"proteins_100g":5,"carbohydrates_100g":10,"fat_100g":2}}]}"""
}
