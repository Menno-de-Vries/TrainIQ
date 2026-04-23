package com.trainiq.ai.services

import com.trainiq.data.model.GeminiRequest
import com.trainiq.data.model.GeminiResponse
import com.trainiq.data.remote.GeminiApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Test

class AiServicesTest {
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

        assertEquals("Great session. Volume reached 9250 kg.", result.summary)
        assertEquals("Volume changed by -3.2% versus the previous session.", result.progressionFeedback)
        assertEquals("Keep the same split and add load to the first compound lift next week.", result.recommendation)
        assertEquals("Maintain current weights", result.nextSessionFocus)
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
        assertEquals("Maintain current weights", result.nextSessionFocus)
        assertEquals(75, result.recoveryScore)
        assertEquals("MAINTAIN", result.intensitySignal)
    }

    private class FakeGeminiApi(
        private val response: GeminiResponse = GeminiResponse(),
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
            called = true
            lastRequest = request
            return response
        }
    }
}
