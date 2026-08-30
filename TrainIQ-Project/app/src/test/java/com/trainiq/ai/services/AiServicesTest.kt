package com.trainiq.ai.services

import com.google.gson.Gson
import com.trainiq.ai.prompts.AiPrompts
import com.trainiq.data.model.GeminiRequest
import com.trainiq.data.model.GeminiResponse
import com.trainiq.data.remote.GeminiApi
import com.trainiq.domain.model.BiologicalSex
import com.trainiq.domain.model.GoalAdviceSource
import com.trainiq.domain.model.MealAnalysisSource
import com.trainiq.domain.model.WeeklyReportSource
import java.io.File
import java.io.RandomAccessFile
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import kotlin.coroutines.CoroutineContext

class AiServicesTest {
    @Test
    fun calculateImageSampleSize_largeImageBoundsAvoidsFullResolutionDecode() {
        assertEquals(4, calculateImageSampleSize(width = 5_120, height = 2_880, maxDimensionPx = 1_280))
    }

    @Test
    fun prepareMealScanImageBytes_usesProvidedBackgroundDispatcher() = runTest {
        var dispatchCount = 0
        val recordingDispatcher = object : CoroutineDispatcher() {
            override fun dispatch(context: CoroutineContext, block: Runnable) {
                dispatchCount += 1
                block.run()
            }
        }
        val file = File.createTempFile("meal-scan-dispatcher", ".jpg").apply {
            writeBytes(byteArrayOf(1, 2, 3))
            deleteOnExit()
        }

        val prepared = prepareMealScanImageBytes(
            file = file,
            ioDispatcher = recordingDispatcher,
            imageCompressor = { byteArrayOf(1, 2, 3) },
        )

        assertEquals(listOf<Byte>(1, 2, 3), prepared?.toList())
        assertTrue("Image preparation should dispatch off the caller context", dispatchCount > 0)
    }

    @Test
    fun prepareMealScanImageBytes_rejectsUndecodableInputInsteadOfUploadingRawBytes() = runTest {
        val file = File.createTempFile("meal-scan-invalid", ".jpg").apply {
            writeBytes(byteArrayOf(1, 2, 3))
            deleteOnExit()
        }

        val prepared = prepareMealScanImageBytes(file)

        assertEquals(null, prepared)
    }

    @Test
    fun geminiRequest_serializesOfficialRestGenerationConfigShape() {
        val json = Gson().toJson(
            GeminiRequest(
                contents = listOf(GeminiRequest.Content(parts = listOf(GeminiRequest.Part(text = "Geef JSON")))),
                generationConfig = GeminiRequest.GenerationConfig(
                    responseMimeType = "application/json",
                    responseJsonSchema = AiJsonSchemas.workoutDebrief,
                    thinkingConfig = GeminiRequest.ThinkingConfig(
                        includeThoughts = false,
                        thinkingBudget = 1000,
                    ),
                ),
            ),
        )

        assertTrue(json.contains("\"generationConfig\""))
        assertTrue(json.contains("\"responseMimeType\":\"application/json\""))
        assertTrue(json.contains("\"responseJsonSchema\""))
        assertTrue(json.contains("\"thinkingConfig\""))
        assertTrue(json.contains("\"includeThoughts\":false"))
        assertTrue(json.contains("\"thinkingBudget\":1000"))
        assertFalse(json.contains("generation_config"))
        assertFalse(json.contains("response_mime_type"))
        assertFalse(json.contains("response_json_schema"))
        assertFalse(json.contains("responseSchema"))
        assertFalse(json.contains("thinking_config"))
    }

    @Test
    fun geminiSchemas_requireOnlyStableCoreFieldsAndLeaveLowConfidenceNotesOptional() {
        val mealScanRequired = AiJsonSchemas.mealScan["required"] as List<*>
        val mealItemsSchema = (AiJsonSchemas.mealScan["properties"] as Map<*, *>)["items"] as Map<*, *>
        val mealItemSchema = mealItemsSchema["items"] as Map<*, *>
        val mealItemProperties = mealItemSchema["properties"] as Map<*, *>
        val mealItemNameSchema = mealItemProperties["name"] as Map<*, *>
        val mealItemRequired = mealItemSchema["required"] as List<*>
        val weeklyRequired = AiJsonSchemas.weeklyReport["required"] as List<*>
        val goalProperties = AiJsonSchemas.goalAdvice["properties"] as Map<*, *>

        assertEquals(listOf("items", "suggestedMealType"), mealScanRequired)
        assertEquals(20, mealItemsSchema["maxItems"])
        assertEquals(120, mealItemNameSchema["maxLength"])
        assertFalse("Top-level scan notes are optional", "notes" in mealScanRequired)
        assertFalse("Per-item confidence is optional", "confidence" in mealItemRequired)
        assertFalse("Per-item notes are optional", "notes" in mealItemRequired)
        assertFalse("Weekly rationale is optional explanation, not chain-of-thought", "rationaleBullets" in weeklyRequired)
        assertFalse("Local baseline owns calorie targets", "targetCalories" in goalProperties)
        assertFalse("Local baseline owns macro targets", "protein" in goalProperties)
    }

    @Test
    fun analyzeMealImage_retriesGeminiRateLimitOnceBeforeUsingApiResult() = runTest {
        val api = FakeGeminiApi(
            outcomes = ArrayDeque(
                listOf(
                    Result.failure(rateLimitError()),
                    Result.success(mealScanResponse("""{"items":[],"suggestedMealType":"LUNCH"}""")),
                ),
            ),
        )
        val service = MealAnalysisService(
            api = api,
            isAiReady = { true },
            apiKeyProvider = { "key" },
            imageBytesProvider = { file -> testPreparedImageBytes(file) },
        )

        val result = service.analyzeMealImage(tempImagePath(), "", 43_200_000L)

        assertEquals(MealAnalysisSource.API, result.source)
        assertEquals(2, api.callCount)
    }

    @Test
    fun analyzeMealImage_openAiAuthenticationFailure_isNotMaskedAsLocalFallback() = runTest {
        val service = MealAnalysisService(
            aiJsonGenerator = FailingAiJsonGenerator(
                AiProviderRequestException(
                    provider = AiProvider.OPENAI,
                    feature = AiFeature.MEAL_SCAN,
                    category = AiFailureCategory.AUTHENTICATION,
                    httpStatus = 401,
                ),
            ),
            isAiReady = { true },
            imageBytesProvider = { byteArrayOf(1, 2, 3) },
        )

        val error = runCatching { service.analyzeMealImage(tempImagePath(), "", 43_200_000L) }.exceptionOrNull()

        assertTrue(error is AiProviderRequestException)
        assertEquals(AiFailureCategory.AUTHENTICATION, (error as AiProviderRequestException).category)
    }

    @Test
    fun analyzeMealImage_openAiTimeoutFallbackKeepsSpecificSafeCause() = runTest {
        val primary = AiProviderRequestException(
            provider = AiProvider.OPENAI,
            feature = AiFeature.MEAL_SCAN,
            category = AiFailureCategory.TIMEOUT,
            requestId = "req_safe_timeout",
        )
        val service = MealAnalysisService(
            aiJsonGenerator = FailingAiJsonGenerator(
                AiProviderUnavailableException(
                    failures = listOf("OPENAI:AiProviderRequestException"),
                    hasRecoverableFailure = true,
                    primaryFailure = primary,
                ),
            ),
            isAiReady = { true },
            imageBytesProvider = { byteArrayOf(1, 2, 3) },
        )

        val result = service.analyzeMealImage(tempImagePath(), "", 43_200_000L)

        assertEquals(MealAnalysisSource.LOCAL_FALLBACK, result.source)
        assertTrue(result.notes.orEmpty().contains("reageerde te langzaam"))
        assertFalse(result.notes.orEmpty().contains("req_safe_timeout"))
    }

    @Test
    fun generateWorkoutDebrief_openAiServiceFallbackKeepsSpecificSafeCause() = runTest {
        val primary = AiProviderRequestException(
            provider = AiProvider.OPENAI,
            feature = AiFeature.WORKOUT_DEBRIEF,
            category = AiFailureCategory.SERVICE_FAILURE,
        )
        val service = WorkoutDebriefService(
            FailingAiJsonGenerator(
                AiProviderUnavailableException(
                    failures = listOf("OPENAI:AiProviderRequestException"),
                    hasRecoverableFailure = true,
                    primaryFailure = primary,
                ),
            ),
        )

        val result = service.generateWorkoutDebrief(
            totalVolume = 5_000.0,
            progression = 1.0,
            distribution = "Full body",
            avgRpe = 7.0f,
            topExercises = "Squat",
            weeklyFrequency = 3,
        )

        assertEquals(com.trainiq.domain.model.WorkoutDebriefSource.LOCAL_FALLBACK, result.source)
        assertTrue(result.summary.contains("tijdelijk niet beschikbaar"))
    }

    @Test
    fun analyzeMealImage_openAiMalformedOutput_isNotReportedAsOpenAiSuccess() = runTest {
        val service = MealAnalysisService(
            aiJsonGenerator = SuccessfulAiJsonGenerator(
                AiRouteResult(AiProvider.OPENAI, "gpt-5.4-mini", "not json"),
            ),
            isAiReady = { true },
            imageBytesProvider = { byteArrayOf(1, 2, 3) },
        )

        val error = runCatching { service.analyzeMealImage(tempImagePath(), "", 43_200_000L) }.exceptionOrNull()

        assertTrue(error is AiProviderRequestException)
        assertEquals(AiFailureCategory.INVALID_RESPONSE, (error as AiProviderRequestException).category)
    }

    @Test
    fun callGeminiWithBoundedRetry_whenFeatureTimeoutExceeded_throwsTypedTimeoutWithoutRetrying() = runTest {
        var callCount = 0

        val error = runCatching {
            callGeminiWithBoundedRetry(
                feature = AiFeature.MEAL_SCAN,
                timeoutMillis = 1L,
                initialBackoffMillis = 0L,
            ) {
                callCount += 1
                delay(10L)
            }
        }.exceptionOrNull()

        assertTrue(error is AiTimeoutException)
        assertEquals(1, callCount)
        assertTrue(error?.message.orEmpty().contains("maaltijdscan"))
    }

    @Test
    fun callGeminiWithBoundedRetry_whenCoroutineIsCancelled_propagatesCancellation() = runTest {
        val cancellation = CancellationException("caller left screen")

        val error = runCatching {
            callGeminiWithBoundedRetry(
                feature = AiFeature.GOAL_ADVICE,
                initialBackoffMillis = 0L,
            ) {
                throw cancellation
            }
        }.exceptionOrNull()

        assertTrue(error is CancellationException)
        assertEquals("caller left screen", error?.message)
    }

    @Test
    fun callGeminiWithBoundedRetry_whenRateLimitPersists_throttlesSameFeatureOnly() = runTest {
        var now = 1_000L
        val throttle = AiFeatureThrottle(nowMillis = { now })
        var mealScanCalls = 0

        val firstError = runCatching {
            callGeminiWithBoundedRetry(
                feature = AiFeature.MEAL_SCAN,
                initialBackoffMillis = 0L,
                throttle = throttle,
            ) {
                mealScanCalls += 1
                throw rateLimitError()
            }
        }.exceptionOrNull()
        val secondMealError = runCatching {
            callGeminiWithBoundedRetry(
                feature = AiFeature.MEAL_SCAN,
                initialBackoffMillis = 0L,
                throttle = throttle,
            ) {
                mealScanCalls += 1
                mealScanResponse("""{"items":[],"suggestedMealType":"LUNCH"}""")
            }
        }.exceptionOrNull()

        var goalCalls = 0
        val goalResponse = callGeminiWithBoundedRetry(
            feature = AiFeature.GOAL_ADVICE,
            initialBackoffMillis = 0L,
            throttle = throttle,
        ) {
            goalCalls += 1
            mealScanResponse("""{"items":[],"suggestedMealType":"LUNCH"}""")
        }
        now += AiFeature.MEAL_SCAN.throttleCooldownMillis + 1L
        val mealResponseAfterCooldown = callGeminiWithBoundedRetry(
            feature = AiFeature.MEAL_SCAN,
            initialBackoffMillis = 0L,
            throttle = throttle,
        ) {
            mealScanCalls += 1
            mealScanResponse("""{"items":[],"suggestedMealType":"LUNCH"}""")
        }

        assertTrue(firstError is AiRateLimitException)
        assertTrue(secondMealError is AiFeatureThrottledException)
        assertTrue(secondMealError?.toAiUserMessage("fallback").orEmpty().contains("maaltijdscan", ignoreCase = true))
        assertEquals(3, mealScanCalls)
        assertEquals(1, goalCalls)
        assertTrue(goalResponse.candidates.isNotEmpty())
        assertTrue(mealResponseAfterCooldown.candidates.isNotEmpty())
    }

    @Test
    fun analyzeMealImage_withOversizedImageReturnsFallbackWithoutCallingGemini() = runTest {
        val api = FakeGeminiApi(response = mealScanResponse("""{"items":[],"suggestedMealType":"LUNCH"}"""))
        val service = MealAnalysisService(
            api = api,
            isAiReady = { true },
            apiKeyProvider = { "key" },
            imageBytesProvider = { file -> testPreparedImageBytes(file) },
        )

        val result = service.analyzeMealImage(oversizedImagePath(), "", 43_200_000L)

        assertEquals(MealAnalysisSource.LOCAL_FALLBACK, result.source)
        assertEquals(0, api.callCount)
    }

    @Test
    fun aiRawResponseGuardRejectsOversizedModelOutput() {
        val error = runCatching {
            requireAiRawResponseWithinLimit("x".repeat(64_001))
        }.exceptionOrNull()

        assertTrue(error is AiRawResponseTooLargeException)
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
        val service = MealAnalysisService(
            api = api,
            isAiReady = { true },
            apiKeyProvider = { "key" },
            imageBytesProvider = { file -> testPreparedImageBytes(file) },
        )

        val result = service.analyzeMealImage(tempImagePath(), "ontbijt", 1_800_000L)

        assertEquals(MealAnalysisSource.API, result.source)
        assertEquals(1, result.items.size)
        assertEquals("Kwark", result.items.single().name)
        assertEquals("Duidelijke foto.", result.notes)
        val prompt = api.lastRequest?.contents?.single()?.parts?.first()?.text.orEmpty()
        assertTrue(prompt.contains("De gebruiker nam deze foto om"))
        assertTrue(prompt.contains("Voorgesteld maaltijdtype: Snack."))
        assertFalse(prompt.contains("User took this photo"))
        assertFalse(prompt.contains("Suggested meal type"))
    }

    @Test
    fun analyzeMealImage_capsModelItemCountBeforeUiState() = runTest {
        val items = (1..25).joinToString(",") { index ->
            """
                {
                  "name": "Product $index",
                  "estimatedGrams": 100,
                  "calories": 120,
                  "protein": 8,
                  "carbs": 12,
                  "fat": 4
                }
            """.trimIndent()
        }
        val api = FakeGeminiApi(
            response = mealScanResponse("""{"items":[$items],"suggestedMealType":"LUNCH"}"""),
        )
        val service = MealAnalysisService(
            api = api,
            isAiReady = { true },
            apiKeyProvider = { "key" },
            imageBytesProvider = { file -> testPreparedImageBytes(file) },
        )

        val result = service.analyzeMealImage(tempImagePath(), "", 43_200_000L)

        assertEquals(20, result.items.size)
        assertEquals("Product 1", result.items.first().name)
        assertEquals("Product 20", result.items.last().name)
    }

    @Test
    fun analyzeMealImage_withExplicitContextWeight_keepsUserWeightAsTruth() = runTest {
        val api = FakeGeminiApi(
            response = mealScanResponse(
                """
                    {
                      "items": [
                        {
                          "name": "Kip",
                          "estimatedGrams": 120,
                          "calories": 198,
                          "protein": 36,
                          "carbs": 0,
                          "fat": 4
                        }
                      ],
                      "suggestedMealType": "DINNER"
                    }
                """.trimIndent(),
            ),
        )
        val service = MealAnalysisService(
            api = api,
            isAiReady = { true },
            apiKeyProvider = { "key" },
            imageBytesProvider = { file -> testPreparedImageBytes(file) },
        )

        val result = service.analyzeMealImage(tempImagePath(), "kip 200g", 72_000_000L)

        val item = result.items.single()
        assertEquals(200.0, item.estimatedGrams, 0.0)
        assertEquals(330.0, item.nutrition.calories, 0.01)
        assertTrue(item.notes.orEmpty().contains("Gebruikerscontext"))
    }

    @Test
    fun analyzeMealImage_withKipRolladeAndKaasContext_keepsSeparateItemIdentity() = runTest {
        val api = FakeGeminiApi(
            response = mealScanResponse(
                """
                    {
                      "items": [
                        {
                          "name": "Kaas",
                          "estimatedGrams": 30,
                          "calories": 110,
                          "protein": 7,
                          "carbs": 0,
                          "fat": 9
                        },
                        {
                          "name": "Kaas",
                          "estimatedGrams": 30,
                          "calories": 110,
                          "protein": 7,
                          "carbs": 0,
                          "fat": 9
                        }
                      ],
                      "suggestedMealType": "LUNCH",
                      "notes": "Foto lijkt op lunch."
                    }
                """.trimIndent(),
            ),
        )
        val service = MealAnalysisService(
            api = api,
            isAiReady = { true },
            apiKeyProvider = { "key" },
            imageBytesProvider = { file -> testPreparedImageBytes(file) },
        )

        val result = service.analyzeMealImage(
            tempImagePath(),
            "kip rollade 80g, kaas 30g",
            43_200_000L,
        )

        assertEquals(listOf("kip rollade", "kaas"), result.items.map { it.name })
        assertEquals(80.0, result.items[0].estimatedGrams, 0.0)
        assertEquals(30.0, result.items[1].estimatedGrams, 0.0)
        assertTrue(result.notes.orEmpty().contains("Expliciete gebruikerscontext"))
    }

    @Test
    fun analyzeMealImage_withFiveContextComponents_preservesFiveComponents() = runTest {
        val api = FakeGeminiApi(
            response = mealScanResponse(
                """
                    {
                      "items": [
                        {"name":"Kiprollade","estimatedGrams":100,"calories":150,"protein":22,"carbs":1,"fat":6},
                        {"name":"Kaas","estimatedGrams":50,"calories":180,"protein":12,"carbs":1,"fat":15},
                        {"name":"Wrap","estimatedGrams":80,"calories":240,"protein":7,"carbs":42,"fat":5},
                        {"name":"Kaas","estimatedGrams":50,"calories":180,"protein":12,"carbs":1,"fat":15},
                        {"name":"Kaas","estimatedGrams":50,"calories":180,"protein":12,"carbs":1,"fat":15}
                      ],
                      "suggestedMealType": "LUNCH"
                    }
                """.trimIndent(),
            ),
        )
        val service = MealAnalysisService(
            api = api,
            isAiReady = { true },
            apiKeyProvider = { "key" },
            imageBytesProvider = { file -> testPreparedImageBytes(file) },
        )

        val result = service.analyzeMealImage(
            tempImagePath(),
            "kip rollade 80g, kaas 30g, wrap 60g, saus 15g, sla 20g",
            43_200_000L,
        )

        assertEquals(
            listOf("kip rollade", "kaas", "wrap", "saus", "sla"),
            result.items.map { it.name },
        )
        assertEquals(5, result.items.size)
        assertTrue(result.notes.orEmpty().contains("meerdere onderdelen lijken samengevoegd"))
        assertTrue(result.items[3].notes.orEmpty().contains("Gebruikerscontext"))
        assertTrue(result.items[4].notes.orEmpty().contains("Gebruikerscontext"))
    }

    @Test
    fun analyzeMealImage_whenContextItemMissing_addsLowConfidenceReviewItem() = runTest {
        val api = FakeGeminiApi(
            response = mealScanResponse(
                """
                    {
                      "items": [
                        {"name":"Kaas","estimatedGrams":30,"calories":110,"protein":7,"carbs":0,"fat":9}
                      ],
                      "suggestedMealType": "LUNCH"
                    }
                """.trimIndent(),
            ),
        )
        val service = MealAnalysisService(
            api = api,
            isAiReady = { true },
            apiKeyProvider = { "key" },
            imageBytesProvider = { file -> testPreparedImageBytes(file) },
        )

        val result = service.analyzeMealImage(
            tempImagePath(),
            "kip rollade 80g, kaas 30g",
            43_200_000L,
        )

        assertEquals(listOf("kip rollade", "kaas"), result.items.map { it.name })
        assertEquals("low", result.items[1].confidence)
        assertTrue(result.items[1].notes.orEmpty().contains("AI leverde geen apart betrouwbaar component terug"))
    }

    @Test
    fun parseBodyMeasurementContextOverrides_acceptsPartialScaleContext() {
        val overrides = parseBodyMeasurementContextOverrides("weegschaal toont 82.4 kg, vet 18,1%, spier 63.0 kg")

        assertEquals(82.4, overrides.weight ?: 0.0, 0.0)
        assertEquals(18.1, overrides.bodyFat ?: 0.0, 0.0)
        assertEquals(63.0, overrides.muscleMass ?: 0.0, 0.0)
    }

    @Test
    fun analyzeMealImage_withStructuredEmptyItems_returnsApiEmptyResult() = runTest {
        val api = FakeGeminiApi(response = mealScanResponse("""{"items":[],"suggestedMealType":"LUNCH"}"""))
        val service = MealAnalysisService(
            api = api,
            isAiReady = { true },
            apiKeyProvider = { "key" },
            imageBytesProvider = { file -> testPreparedImageBytes(file) },
        )

        val result = service.analyzeMealImage(tempImagePath(), "", 43_200_000L)

        assertEquals(MealAnalysisSource.API, result.source)
        assertTrue(result.items.isEmpty())
    }

    @Test
    fun analyzeMealImage_discardsImpossibleNumericItemsBeforeReview() = runTest {
        val api = FakeGeminiApi(
            response = mealScanResponse(
                """
                    {
                      "items": [
                        {
                          "name": "Onmogelijke maaltijd",
                          "estimatedGrams": -20,
                          "calories": 9999999,
                          "protein": -4,
                          "carbs": 10,
                          "fat": 2
                        },
                        {
                          "name": "Kwark",
                          "estimatedGrams": 250,
                          "calories": 150,
                          "protein": 24,
                          "carbs": 10,
                          "fat": 1
                        }
                      ],
                      "suggestedMealType": "BREAKFAST"
                    }
                """.trimIndent(),
            ),
        )
        val service = MealAnalysisService(
            api = api,
            isAiReady = { true },
            apiKeyProvider = { "key" },
            imageBytesProvider = { file -> testPreparedImageBytes(file) },
        )

        val result = service.analyzeMealImage(tempImagePath(), "", 1_800_000L)

        assertEquals(1, result.items.size)
        assertEquals("Kwark", result.items.single().name)
    }

    @Test
    fun analyzeMealImage_whenApiFails_returnsExplicitLocalFallback() = runTest {
        val api = FakeGeminiApi(error = IllegalStateException("network down"))
        val service = MealAnalysisService(
            api = api,
            isAiReady = { true },
            apiKeyProvider = { "key" },
            imageBytesProvider = { file -> testPreparedImageBytes(file) },
        )

        val result = service.analyzeMealImage(tempImagePath(), "", 43_200_000L)

        assertEquals(MealAnalysisSource.LOCAL_FALLBACK, result.source)
        assertTrue(result.items.isEmpty())
        assertEquals("AI-maaltijdanalyse is nu niet beschikbaar. Je kunt de maaltijd handmatig toevoegen.", result.notes)
    }

    @Test
    fun analyzeMealImage_whenCallerCancels_propagatesCancellationInsteadOfFallback() = runTest {
        val api = FakeGeminiApi(error = CancellationException("screen stopped"))
        val service = MealAnalysisService(
            api = api,
            isAiReady = { true },
            apiKeyProvider = { "key" },
            imageBytesProvider = { file -> testPreparedImageBytes(file) },
        )

        val error = runCatching {
            service.analyzeMealImage(tempImagePath(), "", 43_200_000L)
        }.exceptionOrNull()

        assertTrue(error is CancellationException)
        assertEquals("screen stopped", error?.message)
    }

    @Test
    fun analyzeMealImage_withoutApiConfig_returnsExplicitLocalFallback() = runTest {
        val api = FakeGeminiApi()
        val service = MealAnalysisService(
            api = api,
            isAiReady = { false },
            apiKeyProvider = { null },
            imageBytesProvider = { file -> testPreparedImageBytes(file) },
        )

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
        assertEquals(AiJsonSchemas.workoutDebrief, api.lastRequest?.generationConfig?.responseJsonSchema)
        assertEquals(1000, api.lastRequest?.generationConfig?.thinkingConfig?.thinkingBudget)
        val prompt = api.lastRequest?.contents?.single()?.parts?.single()?.text.orEmpty()
        assertTrue(prompt.contains("Antwoord altijd in het Nederlands volgens locale nl-NL."))
        assertTrue(prompt.contains("Gebruik geen Engels"))
    }

    @Test
    fun workoutDebriefPrompt_defaultsToDutchLocaleAndStructuredShortFields() {
        val prompt = AiPrompts.workoutDebrief(
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
        assertEquals("Volume veranderde met -3,2% ten opzichte van de vorige sessie.", result.progressionFeedback)
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
        assertEquals(AiJsonSchemas.goalAdvice, api.lastRequest?.generationConfig?.responseJsonSchema)
        val prompt = api.lastRequest?.contents?.single()?.parts?.single()?.text.orEmpty()
        assertTrue(prompt.contains("Antwoord altijd in het Nederlands volgens locale nl-NL."))
        assertTrue(prompt.contains("\"korteSamenvatting\""))
        assertTrue(prompt.contains("\"activiteitUitleg\""))
        assertTrue(prompt.contains("korteSamenvatting maximaal 2 korte zinnen"))
        assertTrue(prompt.contains("Wijzig deze calorie- en macrocijfers niet"))
    }

    @Test
    fun generateGoalAdvice_withManualCalorieTargetPassesFixedTargetsToGeminiPrompt() = runTest {
        val api = FakeGeminiApi(
            response = mealScanResponse(
                """
                    {
                      "trainingFocus": "Rustige opbouw met stabiele voedingsinname.",
                      "korteSamenvatting": "Je gebruikt bewust een hoger calorie doel.",
                      "calorieAdvies": "Houd dit doel twee weken vast en evalueer trend en training.",
                      "macroAdvies": "Auto macro's verdelen extra energie vooral over koolhydraten.",
                      "activiteitUitleg": "Onderhoud blijft berekend vanuit BMR en activiteit.",
                      "aandachtspunten": ["Gebruik gewichtstrend om bij te sturen."],
                      "advies": "Stuur pas bij na voldoende meetdagen.",
                      "dataKwaliteit": "Redelijk: profiel compleet met handmatig calorie doel."
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
            manualCalorieTarget = 3_050,
        )

        assertEquals(3_050, result.calorieTarget)
        assertEquals(177, result.proteinTarget)
        assertEquals(GoalAdviceSource.GEMINI_2_5_FLASH, result.source)
        val prompt = api.lastRequest?.contents?.single()?.parts?.single()?.text.orEmpty()
        assertTrue(prompt.contains("3050 kcal"))
        assertTrue(prompt.contains("Jouw calorie doel is handmatig ingesteld"))
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
    fun generateGoalAdvice_withOversizedJsonReturnsLocalFallbackWithoutRetainingRawResponse() = runTest {
        val longDutch = "Trainingsweek blijft stabiel en herstel blijft leidend. ".repeat(1_400)
        val api = FakeGeminiApi(
            response = mealScanResponse(
                """
                    {
                      "trainingFocus": "$longDutch",
                      "korteSamenvatting": "Je onderhoud is lokaal berekend.",
                      "calorieAdvies": "Houd je vaste doel stabiel.",
                      "macroAdvies": "Auto macro's blijven leidend.",
                      "activiteitUitleg": "Activiteit blijft een schatting.",
                      "aandachtspunten": ["Gebruik trenddata voorzichtig."],
                      "advies": "Evalueer pas na voldoende meetdagen.",
                      "dataKwaliteit": "Redelijk."
                    }
                """.trimIndent(),
            ),
        )
        val service = GoalAdvisorService(api, isAiReady = { true }, apiKeyProvider = { "key" })

        val result = service.generateGoalAdvice(
            height = 180.0,
            weight = 90.0,
            bodyFat = 20.0,
            age = 35,
            sex = BiologicalSex.MALE,
            activityLevel = "Licht actief",
            goal = "spiermassa",
        )

        assertEquals(GoalAdviceSource.LOCAL_CALCULATION, result.source)
        assertEquals(null, result.rawResponse)
    }

    @Test
    fun localAiFallbackFormatting_usesDutchDecimalAndActivityLabel() {
        assertEquals("2,5", formatAiPercentNl(2.5))
        assertEquals("1,375", formatActivityMultiplierNl(1.375))
        assertEquals("matig actief", "Moderately active".toDutchGoalActivityLabel())
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

    @Test
    fun parseWeeklyReportResponse_withEnglishJsonReturnsLocalDutchFallback() {
        val result = parseWeeklyReportResponse(
            text = """
                {
                  "summary": "Keep training volume stable this week.",
                  "wins": ["Good adherence."],
                  "risks": ["Sleep is low."],
                  "nextWeekFocus": "Add weight only when recovery is good.",
                  "rationaleBullets": ["Recovery is good enough to progress."]
                }
            """.trimIndent(),
            adherence = 72,
        )

        assertEquals(WeeklyReportSource.LOCAL_FALLBACK, result.source)
        assertTrue(result.summary.contains("Lokale samenvatting"))
    }

    @Test
    fun parseWeeklyReportResponse_usesRationaleBulletsWithoutThinkingProcess() {
        val result = parseWeeklyReportResponse(
            text = """
                {
                  "summary": "Trainingsweek was stabiel en herstel blijft leidend.",
                  "wins": ["Je hield drie sessies consistent vast."],
                  "risks": ["Slaapdata is nog beperkt."],
                  "nextWeekFocus": "Houd volume gelijk en verhoog pas na betere slaap.",
                  "rationaleBullets": ["Hersteldata is beperkt, dus progressie blijft conservatief."]
                }
            """.trimIndent(),
            adherence = 82,
        )

        assertEquals(WeeklyReportSource.GEMINI_2_5_FLASH, result.source)
        assertEquals(listOf("Hersteldata is beperkt, dus progressie blijft conservatief."), result.rationaleBullets)
    }

    @Test
    fun parseWeeklyReportResponse_withEmptyJsonReturnsLocalFallbackInsteadOfSyntheticGemini() {
        val result = parseWeeklyReportResponse(text = "{}", adherence = 64)

        assertEquals(WeeklyReportSource.LOCAL_FALLBACK, result.source)
        assertTrue(result.summary.contains("Lokale samenvatting"))
    }

    @Test
    fun parseWeeklyReportResponse_withOversizedJsonReturnsLocalFallbackWithoutRawResponse() {
        val longDutch = "Herstel blijft leidend en training blijft stabiel. ".repeat(1_500)

        val result = parseWeeklyReportResponse(
            text = """
                {
                  "summary": "$longDutch",
                  "wins": ["Je hield drie sessies vast."],
                  "risks": ["Slaapdata blijft beperkt."],
                  "nextWeekFocus": "Houd volume gelijk."
                }
            """.trimIndent(),
            adherence = 82,
        )

        assertEquals(WeeklyReportSource.LOCAL_FALLBACK, result.source)
        assertEquals(null, result.rawResponse)
    }

    @Test
    fun parseWorkoutDebriefResponse_withOversizedJsonReturnsLocalFallback() {
        val longDutch = "Training opgeslagen en herstel blijft de eerste limiter. ".repeat(1_500)

        val result = parseWorkoutDebriefResponse(
            text = """
                {
                  "summary": "$longDutch",
                  "progressionFeedback": "Volume bleef stabiel.",
                  "recommendation": "Herhaal de sessie rustig.",
                  "nextSessionFocus": "Techniek vasthouden"
                }
            """.trimIndent(),
            totalVolume = 4_000.0,
            progression = 1.0,
        )

        assertEquals(com.trainiq.domain.model.WorkoutDebriefSource.LOCAL_FALLBACK, result.source)
    }

    private class FakeGeminiApi(
        private val response: GeminiResponse = GeminiResponse(),
        private val error: Throwable? = null,
        private val outcomes: ArrayDeque<Result<GeminiResponse>> = ArrayDeque(),
    ) : GeminiApi {
        var called = false
            private set
        var callCount = 0
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
            callCount += 1
            outcomes.removeFirstOrNull()?.let { return it.getOrThrow() }
            error?.let { throw it }
            called = true
            lastModel = model
            lastRequest = request
            return response
        }
    }

    private class FailingAiJsonGenerator(
        private val error: Throwable,
    ) : AiJsonGenerator {
        override suspend fun generateJson(request: AiRouteRequest): AiRouteResult = throw error
    }

    private class SuccessfulAiJsonGenerator(
        private val result: AiRouteResult,
    ) : AiJsonGenerator {
        override suspend fun generateJson(request: AiRouteRequest): AiRouteResult = result
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

    private fun testPreparedImageBytes(file: File): ByteArray? =
        file.takeIf { it.exists() && it.length() in 1..(6L * 1024L * 1024L) }
            ?.let { byteArrayOf(1, 2, 3) }

    private fun oversizedImagePath(): String =
        File.createTempFile("meal-scan-oversized", ".jpg").apply {
            RandomAccessFile(this, "rw").use { it.setLength(8L * 1024L * 1024L) }
            deleteOnExit()
        }.absolutePath

    private fun rateLimitError(): HttpException =
        HttpException(Response.error<Unit>(429, "rate limit".toResponseBody()))
}
