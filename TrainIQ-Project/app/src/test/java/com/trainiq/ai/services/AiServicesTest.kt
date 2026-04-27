package com.trainiq.ai.services

import com.trainiq.data.model.GeminiRequest
import com.trainiq.data.model.GeminiResponse
import com.trainiq.data.remote.GeminiApi
import com.trainiq.domain.model.MealAnalysisSource
import java.io.File
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AiServicesTest {
    @Test
    fun analyzeMealImage_withStructuredItems_returnsApiResult() = runTest {
        val api = FakeGeminiApi(
            response = mealScanResponse(
                """
                    {
                      "items": [
                        {
                          "name": "Kwark",
                          "estimatedGrams": 250,
                          "calories": 150,
                          "protein": 24,
                          "carbs": 10,
                          "fat": 1,
                          "confidence": "high"
                        }
                      ],
                      "suggestedMealType": "BREAKFAST",
                      "notes": "Duidelijke foto."
                    }
                """.trimIndent(),
            ),
        )
        val service = MealAnalysisService(api, isAiReady = { true }, apiKeyProvider = { "key" })

        val result = service.analyzeMealImage(tempImagePath(), "ontbijt", 1_800_000L)

        assertEquals(MealAnalysisSource.API, result.source)
        assertEquals(1, result.items.size)
        assertEquals("Kwark", result.items.single().name)
        assertEquals("Duidelijke foto.", result.notes)
    }

    @Test
    fun analyzeMealImage_withStructuredEmptyItems_returnsApiEmptyResult() = runTest {
        val api = FakeGeminiApi(response = mealScanResponse("""{"items":[],"suggestedMealType":"LUNCH"}"""))
        val service = MealAnalysisService(api, isAiReady = { true }, apiKeyProvider = { "key" })

        val result = service.analyzeMealImage(tempImagePath(), "", 43_200_000L)

        assertEquals(MealAnalysisSource.API, result.source)
        assertTrue(result.items.isEmpty())
    }

    @Test
    fun analyzeMealImage_whenApiFails_throwsInsteadOfReturningEmptyFallback() = runTest {
        val api = FakeGeminiApi(error = IllegalStateException("network down"))
        val service = MealAnalysisService(api, isAiReady = { true }, apiKeyProvider = { "key" })

        val error = runCatching {
            service.analyzeMealImage(tempImagePath(), "", 43_200_000L)
        }.exceptionOrNull()

        assertTrue(error is MealAnalysisUnavailableException)
    }

    @Test
    fun analyzeMealImage_withoutApiConfig_returnsExplicitLocalFallback() = runTest {
        val api = FakeGeminiApi()
        val service = MealAnalysisService(api, isAiReady = { false }, apiKeyProvider = { null })

        val result = service.analyzeMealImage(tempImagePath(), "", 43_200_000L)

        assertFalse(api.called)
        assertEquals(MealAnalysisSource.LOCAL_FALLBACK, result.source)
        assertTrue(result.items.isEmpty())
        assertEquals("AI-maaltijdanalyse is nu niet beschikbaar. Je kunt de maaltijd handmatig toevoegen.", result.notes)
    }

    @Test
    fun generateWorkoutDebrief_withStructuredJson_returnsParsedDebrief() = runTest {
        val api = FakeGeminiApi(
            response = GeminiResponse(
                candidates = listOf(
                    GeminiResponse.Candidate(
                        content = GeminiResponse.Content(
                            parts = listOf(
                                GeminiResponse.Part(
                                    text = """
                                        {
                                          "summary": "Solid session.",
                                          "progressionFeedback": "Progression is controlled.",
                                          "recommendation": "Keep the same load and chase one extra rep.",
                                          "nextSessionFocus": "Bench press 82.5kg x 8",
                                          "recoveryScore": 88,
                                          "intensitySignal": "INCREASE",
                                          "wins": ["Top set moved well."],
                                          "risks": ["Volume jumped quickly."],
                                          "nextLoadTarget": "Bench Press: 82.5 kg x 6-8 for 3 working sets",
                                          "recoveryAdvice": "Keep sleep above 7 hours."
                                        }
                                    """.trimIndent(),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )
        val service = WorkoutDebriefService(api) { "key" }

        val result = service.generateWorkoutDebrief(
            totalVolume = 10_000.0,
            progression = 6.5,
            distribution = "Chest 3, Back 2",
            avgRpe = 6.8f,
            topExercises = "Squat 100kg x 5, Bench 80kg x 8",
            weeklyFrequency = 4,
        )

        assertEquals("Solid session.", result.summary)
        assertEquals("Progression is controlled.", result.progressionFeedback)
        assertEquals("Keep the same load and chase one extra rep.", result.recommendation)
        assertEquals("Bench press 82.5kg x 8", result.nextSessionFocus)
        assertEquals(88, result.recoveryScore)
        assertEquals("INCREASE", result.intensitySignal)
        assertEquals(listOf("Top set moved well."), result.wins)
        assertEquals(listOf("Volume jumped quickly."), result.risks)
        assertEquals("Bench Press: 82.5 kg x 6-8 for 3 working sets", result.nextLoadTarget)
        assertEquals("Keep sleep above 7 hours.", result.recoveryAdvice)
        assertNotNull(api.lastRequest)
        assertEquals("application/json", api.lastRequest?.generationConfig?.responseMimeType)
    }

    @Test
    fun generateWorkoutDebrief_withMalformedJson_usesDeterministicFallback() = runTest {
        val api = FakeGeminiApi(
            response = GeminiResponse(
                candidates = listOf(
                    GeminiResponse.Candidate(
                        content = GeminiResponse.Content(
                            parts = listOf(GeminiResponse.Part(text = "not json")),
                        ),
                    ),
                ),
            ),
        )
        val service = WorkoutDebriefService(api) { "key" }

        val result = service.generateWorkoutDebrief(
            totalVolume = 9_250.0,
            progression = -3.2,
            distribution = "Legs 4",
            avgRpe = 9.2f,
            topExercises = "Deadlift 140kg x 3",
            weeklyFrequency = 2,
        )

        assertEquals("Lokale samenvatting: volume 9250 kg.", result.summary)
        assertEquals("Volume veranderde met -3.2% ten opzichte van de vorige sessie.", result.progressionFeedback)
        assertEquals("Houd dezelfde opzet aan en verhoog pas als uitvoering en herstel goed blijven.", result.recommendation)
        assertEquals("Huidige gewichten vasthouden", result.nextSessionFocus)
        assertEquals(75, result.recoveryScore)
        assertEquals("MAINTAIN", result.intensitySignal)
    }

    @Test
    fun generateWorkoutDebrief_whenAiIsDisabled_returnsFallbackWithoutCallingApi() = runTest {
        val api = FakeGeminiApi()
        val service = WorkoutDebriefService(api) { null }

        val result = service.generateWorkoutDebrief(
            totalVolume = 5_000.0,
            progression = 1.0,
            distribution = "Full body",
            avgRpe = 6.0f,
            topExercises = "Squat 80kg x 5",
            weeklyFrequency = 3,
        )

        assertFalse(api.called)
        assertEquals("Huidige gewichten vasthouden", result.nextSessionFocus)
        assertEquals(75, result.recoveryScore)
        assertEquals("MAINTAIN", result.intensitySignal)
    }

    private class FakeGeminiApi(
        private val response: GeminiResponse = GeminiResponse(),
        private val error: Throwable? = null,
    ) : GeminiApi {
        var called = false
            private set
        var lastRequest: GeminiRequest? = null
            private set

        override suspend fun generateContent(
            model: String,
            apiKey: String,
            request: GeminiRequest,
        ): GeminiResponse {
            error?.let { throw it }
            called = true
            lastRequest = request
            return response
        }
    }

    private fun mealScanResponse(text: String): GeminiResponse =
        GeminiResponse(
            candidates = listOf(
                GeminiResponse.Candidate(
                    content = GeminiResponse.Content(parts = listOf(GeminiResponse.Part(text = text))),
                ),
            ),
        )

    private fun tempImagePath(): String =
        File.createTempFile("meal-scan", ".jpg").apply {
            writeBytes(byteArrayOf(1, 2, 3))
            deleteOnExit()
        }.absolutePath
}
