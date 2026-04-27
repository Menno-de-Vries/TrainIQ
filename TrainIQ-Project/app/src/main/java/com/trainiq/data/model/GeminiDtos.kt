package com.trainiq.data.model

import com.google.gson.annotations.SerializedName

data class GeminiRequest(
    val contents: List<Content>,
    @SerializedName("generationConfig") val generationConfig: GenerationConfig? = null,
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
        @SerializedName("includeThoughts") val includeThoughts: Boolean,
        @SerializedName("thinkingBudget") val thinkingBudget: Int,
    )

    data class GenerationConfig(
        @SerializedName("responseMimeType") val responseMimeType: String = "application/json",
        @SerializedName("thinkingConfig") val thinkingConfig: ThinkingConfig? = null,
    )
}

data class GeminiResponse(
    val candidates: List<Candidate> = emptyList(),
) {
    data class Candidate(val content: Content = Content())
    data class Content(val parts: List<Part> = emptyList())
    data class Part(val text: String = "")
}
