package com.trainiq.data.model

import com.google.gson.annotations.SerializedName

data class OpenAiResponseRequest(
    val model: String,
    val input: List<OpenAiInputMessage>,
    val text: OpenAiTextConfig,
)

data class OpenAiInputMessage(
    val role: String,
    val content: List<OpenAiInputContent>,
)

data class OpenAiInputContent(
    val type: String,
    val text: String? = null,
    @SerializedName("image_url") val imageUrl: String? = null,
)

data class OpenAiTextConfig(
    val format: OpenAiTextFormat,
)

data class OpenAiTextFormat(
    val type: String = "json_schema",
    val name: String,
    val schema: Map<String, Any?>,
    val strict: Boolean = true,
)

data class OpenAiResponse(
    val output: List<OpenAiOutput> = emptyList(),
    @SerializedName("output_text") val outputText: String? = null,
)

data class OpenAiOutput(
    val content: List<OpenAiOutputContent> = emptyList(),
)

data class OpenAiOutputContent(
    val type: String = "",
    val text: String? = null,
)
