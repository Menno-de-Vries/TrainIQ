package com.trainiq.data.remote

import com.google.gson.JsonParser
import com.trainiq.domain.model.BarcodeProductLookupResult
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class BarcodeProductLookupService @Inject constructor() {
    suspend fun lookup(barcode: String): BarcodeProductLookupResult? = withContext(Dispatchers.IO) {
        val cleanBarcode = barcode.filter(Char::isDigit).takeIf { it.length in 8..14 } ?: return@withContext null
        runCatching {
            val encodedBarcode = URLEncoder.encode(cleanBarcode, Charsets.UTF_8.name())
            val url = URL("$OpenFoodFactsBaseUrl$encodedBarcode.json?fields=status,product_name,nutriments")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 5_000
                readTimeout = 5_000
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "TrainIQ Android - barcode nutrition lookup")
            }
            connection.inputStream.bufferedReader().use { reader ->
                parseOpenFoodFactsProduct(cleanBarcode, reader.readText())
            }
        }.getOrNull()
    }
}

internal fun parseOpenFoodFactsProduct(barcode: String, json: String): BarcodeProductLookupResult? {
    val root = runCatching { JsonParser.parseString(json).asJsonObject }.getOrNull() ?: return null
    if ((root.get("status")?.asInt ?: 0) != 1) return null
    val product = root.getAsJsonObject("product") ?: return null
    val name = product.get("product_name")?.asString?.trim().orEmpty()
    if (name.isBlank()) return null
    val nutriments = product.getAsJsonObject("nutriments") ?: return null
    val calories = nutriments.safeOpenFoodFactsNumber("energy-kcal_100g", 0.0..5000.0) ?: return null
    val protein = nutriments.safeOpenFoodFactsNumber("proteins_100g", 0.0..1000.0) ?: return null
    val carbs = nutriments.safeOpenFoodFactsNumber("carbohydrates_100g", 0.0..1000.0) ?: return null
    val fat = nutriments.safeOpenFoodFactsNumber("fat_100g", 0.0..1000.0) ?: return null
    return BarcodeProductLookupResult(
        barcode = barcode,
        name = name,
        caloriesPer100g = calories,
        proteinPer100g = protein,
        carbsPer100g = carbs,
        fatPer100g = fat,
    )
}

private fun com.google.gson.JsonObject.safeOpenFoodFactsNumber(
    key: String,
    range: ClosedFloatingPointRange<Double>,
): Double? {
    val value = get(key) ?: return null
    val parsed = runCatching {
        if (value.isJsonPrimitive && value.asJsonPrimitive.isString) {
            value.asString.replace(',', '.').toDouble()
        } else {
            value.asDouble
        }
    }.getOrNull() ?: return null
    return parsed.takeIf { it.isFinite() && it in range }
}

private const val OpenFoodFactsBaseUrl = "https://world.openfoodfacts.org/api/v2/product/"
