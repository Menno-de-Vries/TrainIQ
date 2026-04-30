package com.trainiq.ai.services

import com.google.gson.Gson
import com.trainiq.ai.prompts.GeminiPrompts
import com.trainiq.data.model.GeminiRequest
import com.trainiq.data.model.GeminiResponse
import com.trainiq.data.remote.GeminiApi
import com.trainiq.domain.model.BiologicalSex
import com.trainiq.domain.model.GoalAdviceSource
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
    fun geminiRequest_serializesOfficialRestGenerationConfigShape() {
        val json = Gson().toJson(
            GeminiRequest(
                contents = listOf(GeminiRequest.Content(parts = listOf(GeminiRequest.Part(text = "Geef JSON")))),
                generationConfig = GeminiRequest.GenerationConfig(
                    responseMimeType = "application/json",
                    thinkingConfig = GeminiRequest.ThinkingConfig(
                        includeThoughts = false,
                        thinkingBudget = 1000,
                    ),
                ),
            ),
        )

        assertTrue(json.contains("\"generationConfig\""))
        assertTrue(json.contains("\"responseMimeType\":\"application/json\""))
        assertTrue(json.contains("\"thinkingConfig\""))
        assertTrue(json.contains("\"includeThoughts\":false"))
        assertTrue(json.contains("\"thinkingBudget\":1000"))
        assertFalse(json.contains("generation_config"))
        assertFalse(json.contains("response_mime_type"))
        assertFalse(json.contains("thinking_config"))
    }

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
                                          "summary": "Sterke sessie met controle over de belangrijkste werksets.",
                                          "progressionFeedback": "De progressie is beheerst en bruikbaar voor je volgende training.",
                                          "recommendation": "Houd het gewicht gelijk en mik op een extra herhaling.",
                                          "nextSessionFocus": "Bench press 82,5 kg x 8",
                                          "recoveryScore": 88,
                                          "intensitySignal": "INCREASE",
                                          "wins": ["Topset voelde technisch stabiel."],
                                          "risks": ["Volume steeg snel; let op herstel."],
                                          "nextLoadTarget": "Bench Press: 82,5 kg x 6-8 voor 3 werksets",
                                          "recoveryAdvice": "Houd slaap boven 7 uur voordat je verder verhoogt."
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

        assertEquals("Sterke sessie met controle over de belangrijkste werksets.", result.summary)
        assertEquals("De progressie is beheerst en bruikbaar voor je volgende training.", result.progressionFeedback)
        assertEquals("Houd het gewicht gelijk en mik op een extra herhaling.", result.recommendation)
        assertEquals("Bench press 82,5 kg x 8", result.nextSessionFocus)
        assertEquals(88, result.recoveryScore)
        assertEquals("INCREASE", result.intensitySignal)
        assertEquals(listOf("Topset voelde technisch stabiel."), result.wins)
        assertEquals(listOf("Volume steeg snel; let op herstel."), result.risks)
        assertEquals("Bench Press: 82,5 kg x 6-8 voor 3 werksets", result.nextLoadTarget)
        assertEquals("Houd slaap boven 7 uur voordat je verder verhoogt.", result.recoveryAdvice)
        assertEquals(com.trainiq.domain.model.WorkoutDebriefSource.GEMINI_2_5_FLASH, result.source)
        assertNotNull(api.lastRequest)
        assertEquals("gemini-2.5-flash", api.lastModel)
        assertEquals(GEMINI_FLASH_MODEL, api.lastModel)
        assertEquals("application/json", api.lastRequest?.generationConfig?.responseMimeType)
        assertEquals(1000, api.lastRequest?.generationConfig?.thinkingConfig?.thinkingBudget)
        val prompt = api.lastRequest?.contents?.single()?.parts?.single()?.text.orEmpty()
        assertTrue(prompt.contains("Antwoord altijd in het Nederlands volgens locale nl-NL."))
        assertTrue(prompt.contains("Gebruik geen Engels"))
    }

    @Test
    fun workoutDebriefPrompt_defaultsToDutchLocaleAndStructuredShortFields() {
        val prompt = GeminiPrompts.workoutDebrief(
            totalVolume = 1_500.0,
            progression = 2.0,
            distribution = "Borst 2, Rug 2",
            avgRpe = 7.0f,
            topExercises = "Bench Press 80 kg x 8",
            weeklyFrequency = 3,
        )

        assertTrue(prompt.contains("nl-NL"))
        assertTrue(prompt.contains("summary is 1 korte zin"))
        assertTrue(prompt.contains("\"wins\": [\"string\"]"))
        assertTrue(prompt.contains("\"risks\": [\"string\"]"))
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
        assertEquals(com.trainiq.domain.model.WorkoutDebriefSource.LOCAL_FALLBACK, result.source)
    }

    @Test
    fun generateWorkoutDebrief_withEnglishJson_usesDeterministicFallback() = runTest {
        val api = FakeGeminiApi(
            response = mealScanResponse(
                """
                    {
                      "summary": "Strong session with good control.",
                      "progressionFeedback": "Keep the same loading next time.",
                      "recommendation": "Add more weight next workout.",
                      "nextSessionFocus": "Bench press",
                      "recoveryScore": 80,
                      "intensitySignal": "MAINTAIN",
                      "wins": ["Good form."],
                      "risks": [],
                      "nextLoadTarget": "Bench Press: 80 kg x 8",
                      "recoveryAdvice": "Sleep well."
                    }
                """.trimIndent(),
            ),
        )
        val service = WorkoutDebriefService(api) { "key" }

        val result = service.generateWorkoutDebrief(
            totalVolume = 8_000.0,
            progression = 1.5,
            distribution = "Borst 3, Rug 2",
            avgRpe = 7.0f,
            topExercises = "Bench Press 80kg x 8",
            weeklyFrequency = 3,
        )

        assertEquals(com.trainiq.domain.model.WorkoutDebriefSource.LOCAL_FALLBACK, result.source)
        assertTrue(result.summary.contains("Lokale samenvatting"))
    }

    @Test
    fun generateGoalAdvice_withStructuredDutchJson_returnsFormattedSectionsAndKeepsBaselineMath() = runTest {
        val api = FakeGeminiApi(
            response = mealScanResponse(
                """
                    {
                      "trainingFocus": "Krachttraining behouden met gecontroleerd energietekort.",
                      "korteSamenvatting": "Je onderhoud is realistisch berekend vanuit BMR en activiteit.",
                      "calorieAdvies": "Start met een matig tekort en evalueer na twee weken.",
                      "macroAdvies": "Eiwit is gebaseerd op vetvrije massa; koolhydraten vullen de training aan.",
                      "activiteitUitleg": "Licht actief betekent lichte dagelijkse beweging met beperkte extra training.",
                      "aandachtspunten": ["Vetpercentage en activiteit blijven schattingen."],
                      "advies": "Houd dit doel eerst stabiel en stuur op gewichtstrend.",
                      "dataKwaliteit": "Redelijk: profiel compleet, maar geen gevalideerde TDEE."
                    }
                """.trimIndent(),
            ),
        )
        val service = GoalAdvisorService(api, isAiReady = { true }, apiKeyProvider = { "key" })

        val result = service.generateGoalAdvice(
            height = 195.0,
            weight = 107.2,
            bodyFat = 25.0,
            age = 30,
            sex = BiologicalSex.MALE,
            activityLevel = "Licht actief",
            goal = "fat loss",
        )

        assertEquals(2_951, result.maintenanceCalories)
        assertEquals(2_656, result.calorieTarget)
        assertEquals(177, result.proteinTarget)
        assertEquals("Je onderhoud is realistisch berekend vanuit BMR en activiteit.", result.summary)
        assertEquals("Start met een matig tekort en evalueer na twee weken.", result.calorieAdvice)
        assertEquals("Eiwit is gebaseerd op vetvrije massa; koolhydraten vullen de training aan.", result.macroAdvice)
        assertEquals("Licht actief betekent lichte dagelijkse beweging met beperkte extra training.", result.activityExplanation)
        assertEquals(listOf("Vetpercentage en activiteit blijven schattingen."), result.attentionPoints)
        assertEquals("Houd dit doel eerst stabiel en stuur op gewichtstrend.", result.advice)
        assertEquals("Redelijk: profiel compleet, maar geen gevalideerde TDEE.", result.dataQuality)
        assertEquals(GoalAdviceSource.GEMINI_2_5_FLASH, result.source)
        val prompt = api.lastRequest?.contents?.single()?.parts?.single()?.text.orEmpty()
        assertTrue(prompt.contains("Antwoord altijd in het Nederlands volgens locale nl-NL."))
        assertTrue(prompt.contains("\"korteSamenvatting\""))
        assertTrue(prompt.contains("\"activiteitUitleg\""))
        assertTrue(prompt.contains("korteSamenvatting maximaal 2 korte zinnen"))
        assertTrue(prompt.contains("Wijzig deze calorie- en macrocijfers niet"))
    }

    @Test
    fun generateGoalAdvice_withEnglishJsonReturnsLocalDutchFallback() = runTest {
        val api = FakeGeminiApi(
            response = mealScanResponse(
                """
                    {
                      "trainingFocus": "Keep strength training while cutting.",
                      "korteSamenvatting": "Your maintenance is based on activity level.",
                      "calorieAdvies": "Start with a moderate deficit.",
                      "macroAdvies": "Protein supports muscle recovery.",
                      "activiteitUitleg": "Lightly active means limited extra training.",
                      "aandachtspunten": ["Activity level remains an estimate."],
                      "advies": "Keep this target stable.",
                      "dataKwaliteit": "Good profile data."
                    }
                """.trimIndent(),
            ),
        )
        val service = GoalAdvisorService(api, isAiReady = { true }, apiKeyProvider = { "key" })

        val result = service.generateGoalAdvice(
            height = 195.0,
            weight = 107.2,
            bodyFat = 25.0,
            age = 30,
            sex = BiologicalSex.MALE,
            activityLevel = "Licht actief",
            goal = "vetverlies",
        )

        assertEquals(GoalAdviceSource.LOCAL_CALCULATION, result.source)
        assertTrue(result.summary.contains("Lokale berekening"))
        assertTrue(result.activityExplanation.contains("Activiteitsfactor"))
    }

    @Test
    fun generateGoalAdvice_withMalformedJsonReturnsLocalDutchFallback() = runTest {
        val api = FakeGeminiApi(response = mealScanResponse("not json"))
        val service = GoalAdvisorService(api, isAiReady = { true }, apiKeyProvider = { "key" })

        val result = service.generateGoalAdvice(
            height = 180.0,
            weight = 90.0,
            bodyFat = 30.0,
            age = 40,
            sex = BiologicalSex.MALE,
            activityLevel = "Moderately active",
            goal = "weight loss",
        )

        assertEquals(GoalAdviceSource.LOCAL_CALCULATION, result.source)
        assertTrue(result.summary.contains("Lokale berekening"))
        assertTrue(result.activityExplanation.contains("Activiteitsfactor"))
        assertTrue(result.dataQuality.contains("schatting"))
        assertTrue(result.attentionPoints.isNotEmpty())
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
        assertEquals(com.trainiq.domain.model.WorkoutDebriefSource.LOCAL_FALLBACK, result.source)
    }

    private class FakeGeminiApi(
        private val response: GeminiResponse = GeminiResponse(),
        private val error: Throwable? = null,
    ) : GeminiApi {
        var called = false
            private set
        var lastRequest: GeminiRequest? = null
            private set
        var lastModel: String? = null
            private set

        override suspend fun generateContent(
            model: String,
            apiKey: String,
            request: GeminiRequest,
        ): GeminiResponse {
            error?.let { throw it }
            called = true
            lastModel = model
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
