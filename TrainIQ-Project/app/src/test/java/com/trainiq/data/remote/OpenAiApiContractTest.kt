package com.trainiq.data.remote

import com.google.gson.Gson
import com.trainiq.data.model.OpenAiInputContent
import com.trainiq.data.model.OpenAiInputMessage
import com.trainiq.data.model.OpenAiResponseRequest
import com.trainiq.data.model.OpenAiTextConfig
import com.trainiq.data.model.OpenAiTextFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
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
    fun createResponseExposesHttpStatusAndHeadersToTheOpenAiBoundary() {
        val method = OpenAiApi::class.java.methods.single { it.name == "createResponse" }
        val continuationType = method.genericParameterTypes.last().typeName

        assertTrue(continuationType.contains("retrofit2.Response<com.trainiq.data.model.OpenAiResponse>"))
    }

    @Test
    fun responseDtoModelsStatusErrorIncompleteDetailsAndRefusal() {
        val response = Gson().fromJson(
            """{
                "status":"failed",
                "error":{"code":"project_spend_limit_exceeded","type":"insufficient_quota"},
                "incomplete_details":{"reason":"max_output_tokens"},
                "output":[{"content":[{"type":"refusal","refusal":"safe refusal"}]}]
            }""".trimIndent(),
            Class.forName("com.trainiq.data.model.OpenAiResponse"),
        )
        val responseClass = response.javaClass

        assertEquals("failed", responseClass.getDeclaredField("status").apply { isAccessible = true }.get(response))
        val error = responseClass.getDeclaredField("error").apply { isAccessible = true }.get(response)
        assertNotNull(error)
        assertEquals("insufficient_quota", error!!.javaClass.getDeclaredField("type").apply { isAccessible = true }.get(error))
        assertNotNull(responseClass.getDeclaredField("incompleteDetails").apply { isAccessible = true }.get(response))
        val output = responseClass.getDeclaredField("output").apply { isAccessible = true }.get(response) as List<*>
        val content = output.first()!!.javaClass.getDeclaredField("content").apply { isAccessible = true }.get(output.first()) as List<*>
        assertEquals("safe refusal", content.first()!!.javaClass.getDeclaredField("refusal").apply { isAccessible = true }.get(content.first()))
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
