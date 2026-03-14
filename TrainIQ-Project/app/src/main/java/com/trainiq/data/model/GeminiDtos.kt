package com.trainiq.data.model

import com.google.gson.annotations.SerializedName

data class GeminiRequest(
    val contents: List<Content>,
    @SerializedName("thinking_config") val thinkingConfig: ThinkingConfig? = null,
    @SerializedName("generation_config") val generationConfig: GenerationConfig? = null,
) {
    data class Content(val parts: List<Part>)

    data class Part(
        val text: String? = null,
        @SerializedName("inline_data") val inlineData: InlineData? = null,
    )

    data class InlineData(
        @SerializedName("mime_type") val mimeType: String,
        val data: String,
    )

    data class ThinkingConfig(
        @SerializedName("include_thoughts") val includeThoughts: Boolean,
        @SerializedName("thinking_budget") val thinkingBudget: Int,
    )

    data class GenerationConfig(
        @SerializedName("response_mime_type") val responseMimeType: String = "application/json",
    )
}

data class GeminiResponse(
    val candidates: List<Candidate> = emptyList(),
) {
    data class Candidate(val content: Content = Content())
    data class Content(val parts: List<Part> = emptyList())
    data class Part(val text: String = "")
}
