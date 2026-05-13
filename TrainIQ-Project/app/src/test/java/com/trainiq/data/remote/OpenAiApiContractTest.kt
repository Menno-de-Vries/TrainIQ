package com.trainiq.data.remote

import com.google.gson.Gson
import com.trainiq.data.model.OpenAiInputContent
import com.trainiq.data.model.OpenAiInputMessage
import com.trainiq.data.model.OpenAiResponseRequest
import com.trainiq.data.model.OpenAiTextConfig
import com.trainiq.data.model.OpenAiTextFormat
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.http.Header
import retrofit2.http.Query

class OpenAiApiContractTest {
    @Test
    fun createResponseUsesBearerHeaderInsteadOfQueryAuth() {
        val method = OpenAiApi::class.java.methods.single { it.name == "createResponse" }
        val parameterAnnotations = method.parameterAnnotations.flatten()

        assertFalse(parameterAnnotations.any { it is Query && it.value.contains("key", ignoreCase = true) })
        assertTrue(parameterAnnotations.any { it is Header && it.value == "Authorization" })
    }

    @Test
    fun responseRequestSerializesResponsesStructuredOutputShape() {
        val json = Gson().toJson(
            OpenAiResponseRequest(
                model = "gpt-4.1-mini",
                input = listOf(
                    OpenAiInputMessage(
                        role = "user",
                        content = listOf(
                            OpenAiInputContent(type = "input_text", text = "Geef JSON"),
                            OpenAiInputContent(type = "input_image", imageUrl = "data:image/jpeg;base64,abc"),
                        ),
                    ),
                ),
                text = OpenAiTextConfig(
                    format = OpenAiTextFormat(
                        name = "meal_scan",
                        schema = mapOf("type" to "object", "additionalProperties" to false),
                    ),
                ),
            ),
        )

        assertTrue(json.contains("\"text\""))
        assertTrue(json.contains("\"format\""))
        assertTrue(json.contains("\"type\":\"json_schema\""))
        assertTrue(json.contains("\"strict\":true"))
        assertTrue(json.contains("\"input_image\""))
        assertTrue(json.contains("\"image_url\""))
        assertFalse(json.contains("response_format"))
    }
}
