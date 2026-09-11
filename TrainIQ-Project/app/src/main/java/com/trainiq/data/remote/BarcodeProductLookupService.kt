package com.trainiq.data.remote

import com.google.gson.JsonParser
import com.trainiq.domain.model.BarcodeProductLookupResult
import com.trainiq.domain.model.*
import java.io.Reader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.net.URLConnection
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class BarcodeProductLookupService @Inject constructor() {
    suspend fun lookup(barcode: String, mode: FoodProviderMode = FoodProviderMode.AUTOMATIC): BarcodeProductLookupResult? =
        lookupFoodProduct(barcode, mode, { lookupOpenFoodFactsProduct(it) }, { lookupFatSecretGateway(it) })
}

internal suspend fun lookupFoodProduct(
    barcode: String, mode: FoodProviderMode,
    primary: suspend (String) -> BarcodeProductLookupResult?,
    fallback: suspend (String) -> BarcodeProductLookupResult?,
): BarcodeProductLookupResult? {
    val normalized = normalizedBarcode(barcode) ?: throw FoodLookupException(FoodLookupFailure.INVALID_BARCODE)
    if (mode == FoodProviderMode.FATSECRET) return fallback(normalized)
    val result = try { primary(normalized) }
    catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
    catch (error: FoodLookupException) {
        if (mode != FoodProviderMode.AUTOMATIC || error.failure != FoodLookupFailure.NETWORK) throw error
        return fallback(normalized)
    }
    catch (error: java.io.IOException) {
        if (mode != FoodProviderMode.AUTOMATIC) throw FoodLookupException(FoodLookupFailure.NETWORK)
        return fallback(normalized)
    }
    return result ?: if (mode == FoodProviderMode.AUTOMATIC) fallback(normalized) else null
}

private suspend fun lookupFatSecretGateway(barcode: String): BarcodeProductLookupResult? = withContext(Dispatchers.IO) {
    val gtin = fatSecretGtin13(barcode) ?: throw FoodLookupException(FoodLookupFailure.INVALID_BARCODE)
    val base = com.trainiq.BuildConfig.FOOD_GATEWAY_URL
    if (base.isBlank()) throw FoodLookupException(FoodLookupFailure.NOT_CONFIGURED)
    val url = URL("${base.trimEnd('/')}/food/barcode/$gtin")
    require(url.protocol == "https")
    val connection = (url.openConnection() as HttpURLConnection).apply {
        connectTimeout = 5_000; readTimeout = 5_000; instanceFollowRedirects = false
        setRequestProperty("Accept", "application/json")
    }
    try {
        when (connection.responseCode) {
            404 -> return@withContext null
            401, 403 -> throw FoodLookupException(FoodLookupFailure.AUTH)
            503 -> throw FoodLookupException(FoodLookupFailure.NOT_CONFIGURED)
            422 -> throw FoodLookupException(FoodLookupFailure.INVALID_RESPONSE)
            200 -> Unit
            else -> throw FoodLookupException(FoodLookupFailure.NETWORK)
        }
        val json = connection.inputStream.bufferedReader().use { it.readText(MaxOpenFoodFactsResponseChars) }
        parseFatSecretGatewayProduct(gtin, json)
    } catch (error: java.io.IOException) { throw FoodLookupException(FoodLookupFailure.NETWORK) }
    finally { connection.disconnect() }
}

internal fun parseFatSecretGatewayProduct(gtin: String, json: String): BarcodeProductLookupResult = try {
        val root = JsonParser.parseString(json).asJsonObject
        fun number(key: String, max: Double) = root.safeOpenFoodFactsNumber(key, 0.0..max)
            ?: throw FoodLookupException(FoodLookupFailure.INVALID_RESPONSE)
        val name = root.get("name")?.asString?.trim()?.takeIf { it.isNotBlank() }
            ?: throw FoodLookupException(FoodLookupFailure.INVALID_RESPONSE)
        BarcodeProductLookupResult(gtin, name, number("caloriesPer100g", 5000.0),
            number("proteinPer100g", 1000.0), number("carbsPer100g", 1000.0), number("fatPer100g", 1000.0),
            provider = FoodProviderMode.FATSECRET)
    } catch (_: Exception) { throw FoodLookupException(FoodLookupFailure.INVALID_RESPONSE) }

internal suspend fun lookupOpenFoodFactsProduct(
    barcode: String,
    openConnection: (URL) -> URLConnection = URL::openConnection,
): BarcodeProductLookupResult? = withContext(Dispatchers.IO) {
    val cleanBarcode = barcode.filter(Char::isDigit).takeIf { it.length in 8..14 } ?: return@withContext null
    run {
        val encodedBarcode = URLEncoder.encode(cleanBarcode, Charsets.UTF_8.name())
        val url = URL("$OpenFoodFactsBaseUrl$encodedBarcode.json?fields=status,product_name,nutriments,categories_tags,serving_quantity,serving_quantity_unit")
        val connection = (openConnection(url) as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 5_000
            readTimeout = 5_000
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "TrainIQ Android - barcode nutrition lookup")
        }
        try {
            if (connection.responseCode == HttpURLConnection.HTTP_NOT_FOUND) return@run null
            if (connection.responseCode == 429 || connection.responseCode >= 500) throw FoodLookupException(FoodLookupFailure.NETWORK)
            if (connection.responseCode != 200) throw FoodLookupException(FoodLookupFailure.AUTH)
            connection.inputStream.bufferedReader().use { reader ->
                val json = reader.readText(MaxOpenFoodFactsResponseChars)
                val status = runCatching { JsonParser.parseString(json).asJsonObject.get("status").asInt }.getOrNull()
                    ?: throw FoodLookupException(FoodLookupFailure.INVALID_RESPONSE)
                if (status == 0) null else parseOpenFoodFactsProduct(cleanBarcode, json)
                    ?: throw FoodLookupException(FoodLookupFailure.INVALID_RESPONSE)
            }
        } catch (error: java.io.IOException) { throw FoodLookupException(FoodLookupFailure.NETWORK) }
        finally {
            connection.disconnect()
        }
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
        explicitServingMl = product.get("serving_quantity")?.asString?.let { quantity ->
            explicitVolumeMl(quantity, product.get("serving_quantity_unit")?.asString.orEmpty())
        },
        isBeverage = product.getAsJsonArray("categories_tags")?.any { it.asString == "en:beverages" } == true,
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

private fun Reader.readText(maxChars: Int): String {
    val buffer = CharArray(DEFAULT_BUFFER_SIZE)
    val output = StringBuilder()
    while (true) {
        val read = read(buffer)
        if (read == -1) return output.toString()
        if (output.length + read > maxChars) {
            throw OpenFoodFactsResponseTooLargeException()
        }
        output.append(buffer, 0, read)
    }
}

private class OpenFoodFactsResponseTooLargeException : RuntimeException()

private const val MaxOpenFoodFactsResponseChars = 256 * 1024
private const val OpenFoodFactsBaseUrl = "https://world.openfoodfacts.org/api/v2/product/"
