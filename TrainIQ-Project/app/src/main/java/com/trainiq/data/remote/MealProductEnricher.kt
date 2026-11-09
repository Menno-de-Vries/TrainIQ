package com.trainiq.data.remote

import com.google.gson.JsonParser
import com.trainiq.domain.model.BarcodeProductLookupResult
import com.trainiq.domain.model.FoodProviderMode
import com.trainiq.domain.model.MealAnalysisResult
import com.trainiq.domain.model.MealScanItem
import com.trainiq.domain.model.NutritionFacts
import com.trainiq.domain.model.normalizedBarcode
import java.net.URLEncoder
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

@Singleton
class MealProductEnricher @Inject constructor(client: OkHttpClient) {
    private val transport = MealProductTransport(client)
    private val cache = linkedMapOf<String, Pair<Long, BarcodeProductLookupResult?>>()

    suspend fun enrich(result: MealAnalysisResult, mode: FoodProviderMode): MealAnalysisResult =
        enrichMealProducts(result, mode) { item, provider ->
            val barcode = item.barcode?.let(::normalizedBarcode)
            val key = "$provider:${barcode ?: listOf(item.brand, item.productName, item.variant).joinToString("|") { it.orEmpty().lowercase(java.util.Locale.ROOT) }}"
            val cached = synchronized(cache) { cache[key]?.takeIf { System.currentTimeMillis() - it.first < 300_000 } }
            if (cached != null) cached.second else {
                val product = try { if (barcode != null) lookupFoodProduct(barcode, provider,
                    primary = { code -> transport.get("https://world.openfoodfacts.org/api/v2/product/$code.json?fields=status,product_name,nutriments,categories_tags")?.let { parseOpenFoodFactsProduct(code, it) } },
                    fallback = { code ->
                        val base = com.trainiq.BuildConfig.FOOD_GATEWAY_URL
                        val gtin = com.trainiq.domain.model.fatSecretGtin13(code)
                        if (base.isBlank() || gtin == null || !base.startsWith("https://")) null
                        else transport.get("${base.trimEnd('/')}/food/barcode/$gtin")?.let { parseFatSecretGatewayProduct(gtin, it) }
                    })
                    else if (provider != FoodProviderMode.FATSECRET) searchExactMealProduct(item, transport) else null
                } catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { null }
                synchronized(cache) {
                    if (cache.size >= 32) cache.remove(cache.keys.first())
                    cache[key] = System.currentTimeMillis() to product
                }
                product
            }
        }
}

internal suspend fun enrichMealProducts(
    result: MealAnalysisResult,
    mode: FoodProviderMode = FoodProviderMode.AUTOMATIC,
    lookup: suspend (MealScanItem, FoodProviderMode) -> BarcodeProductLookupResult?,
): MealAnalysisResult {
    val enriched = result.items.toMutableList()
    // One short budget for the whole meal, not a timeout for every ingredient.
    withTimeoutOrNull(3_000) {
        var requests = 0
        val resolved = mutableMapOf<List<String?>, BarcodeProductLookupResult?>()
        for ((index, item) in result.items.withIndex()) {
            if (item.confidence.equals("low", ignoreCase = true)) continue
            if (item.amountUnit != "g" || !item.estimatedGrams.isFinite() || item.estimatedGrams <= 0) continue
            val barcode = item.barcode?.let(::normalizedBarcode)
            if (barcode == null && (item.brand.isNullOrBlank() || item.productName.isNullOrBlank())) continue
            val identity = listOf(barcode, item.brand, item.productName, item.variant)
            if (!resolved.containsKey(identity)) {
                if (requests++ >= 1) continue
                resolved[identity] = try { lookup(item, mode) }
                catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { null }
            }
            val product = resolved[identity] ?: continue
            // OFF uses _100g keys for liquid bases too; do not invent a density.
            if (product.provider == FoodProviderMode.OPEN_FOOD_FACTS && product.isBeverage) continue
            val factor = item.estimatedGrams / 100.0
            val nutrition = NutritionFacts(product.caloriesPer100g * factor, product.proteinPer100g * factor,
                product.carbsPer100g * factor, product.fatPer100g * factor)
            if (listOf(nutrition.calories, nutrition.protein, nutrition.carbs, nutrition.fat).any { !it.isFinite() || it < 0 }) continue
            val source = if (product.provider == FoodProviderMode.FATSECRET) "FatSecret" else "Open Food Facts"
            enriched[index] = item.copy(nutrition = nutrition, notes = listOfNotNull(item.notes,
                "Productgegevens: $source (barcode ${product.barcode}); omgerekend naar ${item.estimatedGrams} g. Controleer de verpakking.",
                if (product.provider == FoodProviderMode.OPEN_FOOD_FACTS) "https://world.openfoodfacts.org/product/${product.barcode}" else null).joinToString(" "))
        }
    }
    return result.copy(items = enriched)
}

internal suspend fun searchExactMealProduct(item: MealScanItem, transport: MealProductTransport): BarcodeProductLookupResult? {
    val query = listOfNotNull(item.brand, item.productName, item.variant).joinToString(" ").take(200)
    val url = "https://world.openfoodfacts.org/cgi/search.pl?search_terms=${URLEncoder.encode(query, "UTF-8")}&search_simple=1&action=process&json=1&page_size=5&fields=code,product_name,brands,nutriments,categories_tags"
    return transport.get(url)?.let { selectExactMealProduct(item, it) }
}

internal class MealProductTransport(client: OkHttpClient, timeoutMillis: Long = 1_200) {
    private val client = client.newBuilder().callTimeout(timeoutMillis, TimeUnit.MILLISECONDS)
        .connectTimeout(timeoutMillis, TimeUnit.MILLISECONDS).readTimeout(timeoutMillis, TimeUnit.MILLISECONDS)
        .followRedirects(false).followSslRedirects(false).retryOnConnectionFailure(false).build()

    suspend fun get(url: String): String? = suspendCancellableCoroutine { continuation ->
        val call = client.newCall(Request.Builder().url(url).header("Accept", "application/json")
            .header("User-Agent", "TrainIQ Android - barcode nutrition lookup").build())
        continuation.invokeOnCancellation { call.cancel() }
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (continuation.isActive) continuation.resumeWith(Result.failure(e))
            }
            override fun onResponse(call: Call, response: Response) {
                val result = runCatching {
                    response.use {
                        if (it.code == 404) return@use null
                        if (!it.isSuccessful) throw IOException("Food lookup unavailable")
                        val body = it.body ?: return@use null
                        body.charStream().use { reader ->
                            val chars = CharArray(256 * 1024 + 1)
                            var size = 0
                            while (size < chars.size) {
                                val read = reader.read(chars, size, chars.size - size)
                                if (read < 0) break
                                size += read
                            }
                            if (size == chars.size) throw IOException("Food response too large")
                            String(chars, 0, size)
                        }
                    }
                }
                if (continuation.isActive) continuation.resumeWith(result)
            }
        })
    }
}

internal fun selectExactMealProduct(item: MealScanItem, json: String): BarcodeProductLookupResult? {
    fun tokens(value: String) = value.lowercase(java.util.Locale.ROOT).split(Regex("[^\\p{L}\\p{N}]+" )).filter(String::isNotBlank).toSet()
    val brand = tokens(item.brand.orEmpty())
    val name = tokens(listOfNotNull(item.productName, item.variant).joinToString(" "))
    if (brand.isEmpty() || name.isEmpty()) return null
    val root = JsonParser.parseString(json).asJsonObject
    val matches = root.getAsJsonArray("products")?.mapNotNull { element ->
        val product = element.asJsonObject
        val candidateBrand = tokens(product.get("brands")?.asString.orEmpty())
        val candidateName = tokens(product.get("product_name")?.asString.orEmpty())
        if (candidateBrand != brand || (candidateName - brand) != (name - brand)) return@mapNotNull null
        val code = product.get("code")?.asString?.let(::normalizedBarcode) ?: return@mapNotNull null
        parseOpenFoodFactsProduct(code, "{\"status\":1,\"product\":$product}")
    }.orEmpty()
    return matches.singleOrNull()
}
