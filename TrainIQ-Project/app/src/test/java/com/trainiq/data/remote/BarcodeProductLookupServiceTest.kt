package com.trainiq.data.remote

import java.io.File
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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

    @Test
    fun parseOpenFoodFactsProduct_withNotFoundMalformedOrUnsafeValues_returnsNull() {
        assertNull(parseOpenFoodFactsProduct("00000000000000", """{"status":0}"""))
        assertNull(parseOpenFoodFactsProduct("8712345678901", "{not-json"))
        assertNull(
            parseOpenFoodFactsProduct(
                barcode = "8712345678901",
                json = """
                    {
                      "status": 1,
                      "product": {
                        "product_name": "Onveilige macro",
                        "nutriments": {
                          "energy-kcal_100g": 56,
                          "proteins_100g": "NaN",
                          "carbohydrates_100g": 4.0,
                          "fat_100g": 0.2
                        }
                      }
                    }
                """.trimIndent(),
            ),
        )
    }

    @Test
    fun barcodeLookupService_usesBoundedTimeoutsAndNullOnNetworkFailures() {
        val source = File("src/main/java/com/trainiq/data/remote/BarcodeProductLookupService.kt").readText()
        val lookupBody = source.substringAfter("internal suspend fun lookupOpenFoodFactsProduct(").substringBefore("internal fun parseOpenFoodFactsProduct")

        assertTrue(lookupBody.contains("barcode.filter(Char::isDigit)"))
        assertTrue(lookupBody.contains("it.length in 8..14"))
        assertTrue(lookupBody.contains("runCatching"))
        assertTrue(lookupBody.contains("connectTimeout = 5_000"))
        assertTrue(lookupBody.contains("readTimeout = 5_000"))
        assertTrue(lookupBody.contains("MaxOpenFoodFactsResponseChars"))
        assertTrue(lookupBody.contains("getOrNull()"))
    }

    @Test
    fun lookupOpenFoodFactsProduct_withNetworkFailureReturnsNull() = runTest {
        val result = lookupOpenFoodFactsProduct("3017620422003") {
            throw IOException("offline")
        }

        assertNull(result)
    }

    @Test
    fun lookupOpenFoodFactsProduct_withMalformedResponseReturnsNullAndDisconnects() = runTest {
        val connection = FakeHttpConnection("{malformed-json")

        val result = lookupOpenFoodFactsProduct("3017620422003") { connection }

        assertNull(result)
        assertTrue(connection.disconnected)
        assertEquals("GET", connection.requestMethod)
        assertEquals(5_000, connection.connectTimeout)
        assertEquals(5_000, connection.readTimeout)
        assertEquals("application/json", connection.capturedRequestProperties["Accept"])
    }

    @Test
    fun lookupOpenFoodFactsProduct_withOversizedResponseReturnsNullAndDisconnects() = runTest {
        val connection = FakeHttpConnection(" ".repeat(300_000))

        val result = lookupOpenFoodFactsProduct("3017620422003") { connection }

        assertNull(result)
        assertTrue(connection.disconnected)
    }

    @Test
    fun lookupOpenFoodFactsProduct_withSuccessfulResponseReturnsProduct() = runTest {
        val connection = FakeHttpConnection(
            """
                {
                  "status": 1,
                  "product": {
                    "product_name": "Nutella",
                    "nutriments": {
                      "energy-kcal_100g": 539,
                      "proteins_100g": 6.3,
                      "carbohydrates_100g": 57.5,
                      "fat_100g": 30.9
                    }
                  }
                }
            """.trimIndent(),
        )

        val result = lookupOpenFoodFactsProduct("barcode: 3017620422003") { connection }

        requireNotNull(result)
        assertEquals("3017620422003", result.barcode)
        assertEquals("Nutella", result.name)
        assertEquals(539.0, result.caloriesPer100g, 0.0)
    }
}

private class FakeHttpConnection(
    private val body: String,
) : HttpURLConnection(URL("https://example.test/product.json")) {
    var disconnected: Boolean = false
        private set
    val capturedRequestProperties = mutableMapOf<String, String>()

    override fun disconnect() {
        disconnected = true
    }

    override fun usingProxy(): Boolean = false

    override fun connect() = Unit

    override fun getInputStream(): InputStream = ByteArrayInputStream(body.toByteArray())

    override fun setRequestProperty(key: String?, value: String?) {
        if (key != null && value != null) {
            capturedRequestProperties[key] = value
        }
    }
}
