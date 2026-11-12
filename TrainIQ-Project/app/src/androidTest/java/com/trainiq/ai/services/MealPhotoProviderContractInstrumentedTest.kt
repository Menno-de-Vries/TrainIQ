package com.trainiq.ai.services

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.test.core.app.ApplicationProvider
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.trainiq.core.datastore.AiPreferences
import com.trainiq.data.remote.GeminiApi
import com.trainiq.data.remote.OpenAiApi
import com.trainiq.domain.model.MealAnalysisSource
import java.io.File
import java.util.Base64
import java.util.UUID
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/** Real Android JPEG preparation + Retrofit/Gson request/response + router + meal parser.
 * Only the transport response is synthetic; no user image, credential or live provider is used. */
class MealPhotoProviderContractInstrumentedTest {
    @Test fun geminiPhotoAndExtraTextUseTheProductionMediaAndJsonBoundary() = verify(AiProvider.GEMINI)
    @Test fun openAiPhotoAndExtraTextUseTheProductionMediaAndJsonBoundary() = verify(AiProvider.OPENAI)

    private fun verify(provider: AiProvider) = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val file = File(context.cacheDir, "meal-contract-${UUID.randomUUID()}.jpg")
        val bitmap = Bitmap.createBitmap(24, 16, Bitmap.Config.ARGB_8888)
        file.outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it)) }
        bitmap.recycle()
        val extraText = "Rijst 200 g; kip 100 g"
        val meal = """{"items":[{"name":"Rijst","estimatedGrams":100,"calories":130,"protein":2.5,"carbs":28,"fat":0.3},{"name":"kip","estimatedGrams":100,"calories":165,"protein":31,"carbs":0,"fat":3.6}],"suggestedMealType":"LUNCH"}"""
        var imageRequests = 0
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            val request = chain.request()
            val body = if (request.method == "GET") {
                assertEquals(AiProvider.OPENAI, provider)
                assertEquals("/v1/models", request.url.encodedPath)
                """{"data":[{"id":"gpt-6-luna"}]}"""
            } else {
                imageRequests++
                val json = JsonParser.parseString(Buffer().also { request.body!!.writeTo(it) }.readUtf8()).asJsonObject
                val bytes = if (provider == AiProvider.GEMINI) {
                    assertEquals("/v1beta/models/gemini-2.5-flash:generateContent", request.url.encodedPath)
                    assertEquals("fixture-key", request.header("x-goog-api-key"))
                    val parts = json.getAsJsonArray("contents")[0].asJsonObject.getAsJsonArray("parts")
                    assertTrue(parts[0].asJsonObject.get("text").asString.contains(extraText))
                    assertNotNull(json.getAsJsonObject("generationConfig").get("responseJsonSchema"))
                    val inline = parts[1].asJsonObject.getAsJsonObject("inline_data")
                    assertEquals("image/jpeg", inline.get("mime_type").asString)
                    Base64.getDecoder().decode(inline.get("data").asString)
                } else {
                    assertEquals("/v1/responses", request.url.encodedPath)
                    assertEquals("Bearer fixture-key", request.header("Authorization"))
                    val content = json.getAsJsonArray("input")[0].asJsonObject.getAsJsonArray("content")
                    assertTrue(content[0].asJsonObject.get("text").asString.contains(extraText))
                    assertTrue(json.getAsJsonObject("text").getAsJsonObject("format").get("strict").asBoolean)
                    val image = content[1].asJsonObject
                    assertEquals("input_image", image.get("type").asString)
                    val url = image.get("image_url").asString
                    assertTrue(url.startsWith("data:image/jpeg;base64,"))
                    Base64.getDecoder().decode(url.substringAfter(','))
                }
                val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                assertNotNull(decoded)
                assertEquals(24, decoded.width)
                assertEquals(16, decoded.height)
                decoded.recycle()
                if (provider == AiProvider.GEMINI) {
                    """{"candidates":[{"finishReason":"STOP","content":{"parts":[{"text":${Gson().toJson(meal)}}]}}]}"""
                } else {
                    """{"status":"completed","output":[{"content":[{"type":"output_text","text":${Gson().toJson(meal)}}]}]}"""
                }
            }
            Response.Builder().request(request).protocol(Protocol.HTTP_1_1).code(200).message("fixture")
                .body(body.toResponseBody("application/json".toMediaType())).build()
        }.build()
        val retrofit = Retrofit.Builder().baseUrl("https://fixture.invalid/").client(client)
            .addConverterFactory(GsonConverterFactory.create()).build()
        val modelClient: AiModelClient = if (provider == AiProvider.GEMINI) {
            GeminiModelClient(retrofit.create(GeminiApi::class.java))
        } else OpenAiModelClient(retrofit.create(OpenAiApi::class.java))
        val settings = AiPreferences(enabled = true, apiKey = "fixture-key",
            geminiApiKey = if (provider == AiProvider.GEMINI) "fixture-key" else "",
            openAiApiKey = if (provider == AiProvider.OPENAI) "fixture-key" else "",
            preferredProvider = if (provider == AiProvider.OPENAI) AiProviderPreference.OPENAI_FIRST else AiProviderPreference.GEMINI_FIRST)
        val service = MealAnalysisService(
            aiJsonGenerator = object : AiJsonGenerator {
                override suspend fun generateJson(request: AiRouteRequest) = routeAiProviderRequest(settings, request, clientFor = { modelClient })
            }, isAiReady = { true }, imageBytesProvider = { prepareMealScanImageBytes(it) },
        )
        try {
            val result = service.analyzeMealImage(file.absolutePath, extraText, 1_794_268_800_000L)
            assertEquals(1, imageRequests)
            assertEquals(if (provider == AiProvider.OPENAI) MealAnalysisSource.OPENAI else MealAnalysisSource.API, result.source)
            assertEquals(listOf(200.0, 100.0), result.items.map { it.estimatedGrams })
            assertEquals(260.0, result.items.first().nutrition.calories, 0.0)
        } finally { file.delete() }
    }
}
