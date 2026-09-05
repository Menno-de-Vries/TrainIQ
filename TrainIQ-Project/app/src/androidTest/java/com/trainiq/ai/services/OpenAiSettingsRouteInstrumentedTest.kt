package com.trainiq.ai.services

import android.app.Activity
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.trainiq.core.datastore.UserPreferencesRepository
import com.trainiq.core.diagnostics.BreadcrumbRingBuffer
import com.trainiq.core.diagnostics.DiagnosticsTracker
import com.trainiq.core.diagnostics.NoOpTelemetry
import com.trainiq.core.diagnostics.PerformanceSessionMonitor
import com.trainiq.core.diagnostics.PerformanceSessionSummary
import com.trainiq.core.security.AndroidKeystoreOpenAiKeyStore
import com.trainiq.core.security.GeminiEncryptedKeyStore
import com.trainiq.core.security.GeminiKeyMigration
import com.trainiq.core.security.OpenAiKeyStore
import com.trainiq.data.model.GeminiRequest
import com.trainiq.data.model.GeminiResponse
import com.trainiq.data.model.OpenAiModelDescriptor
import com.trainiq.data.model.OpenAiModelsResponse
import com.trainiq.data.model.OpenAiOutput
import com.trainiq.data.model.OpenAiOutputContent
import com.trainiq.data.model.OpenAiResponse
import com.trainiq.data.model.OpenAiResponseRequest
import com.trainiq.data.remote.GeminiApi
import com.trainiq.data.remote.OpenAiApi
import com.trainiq.domain.model.BiologicalSex
import com.trainiq.domain.model.GoalAdviceSource
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import retrofit2.Response

@RunWith(AndroidJUnit4::class)
class OpenAiSettingsRouteInstrumentedTest {
    @Test
    fun storedOpenAiSettings_surviveGateReconstructionAndRouteGoalAdviceToOpenAi() = runBlocking {
        val isolatedContext = InstrumentationRegistry.getInstrumentation()
            .targetContext
            .createDeviceProtectedStorageContext()
        val preferences = UserPreferencesRepository(isolatedContext)
        val openAiKeys = OpenAiKeyStore(AndroidKeystoreOpenAiKeyStore(isolatedContext))
        val firstGate = newGate(preferences, openAiKeys)
        val syntheticKey = "synthetic-instrumented-openai-key"

        try {
            preferences.setAiEnabled(true)
            preferences.setAiProviderPreference(AiProviderPreference.OPENAI_FIRST)
            assertTrue(firstGate.saveOpenAiApiKey(syntheticKey))

            val reconstructedGate = newGate(
                preferences = preferences,
                openAiKeys = OpenAiKeyStore(AndroidKeystoreOpenAiKeyStore(isolatedContext)),
            )
            val api = FakeOpenAiApi()
            val router = AiProviderRouter(
                geminiClient = GeminiModelClient(UnusedGeminiApi),
                openAiClient = OpenAiModelClient(api),
                aiUsageGate = reconstructedGate,
                diagnosticsTracker = DiagnosticsTracker(
                    BreadcrumbRingBuffer(),
                    NoOpTelemetry,
                    NoOpPerformanceSessionMonitor,
                ),
            )

            val settings = reconstructedGate.currentSettings()
            assertTrue(settings.enabled)
            assertEquals(AiProviderPreference.OPENAI_FIRST, settings.preferredProvider)
            assertEquals(syntheticKey, settings.openAiApiKey)

            val advice = GoalAdvisorService(router, reconstructedGate).generateGoalAdvice(
                height = 180.0,
                weight = 90.0,
                bodyFat = 25.0,
                age = 40,
                sex = BiologicalSex.MALE,
                activityLevel = "Licht actief",
                goal = "vetverlies",
            )

            assertEquals(1, api.modelDiscoveryCalls)
            assertEquals(1, api.responseCalls)
            assertEquals("Bearer $syntheticKey", api.lastAuthorization)
            assertEquals("gpt-5.6-luna", api.lastRequest?.model)
            assertEquals("goal_advice", api.lastRequest?.text?.format?.name)
            assertEquals(GoalAdviceSource.OPENAI, advice.source)
            assertEquals("OpenAI-testadvies in het Nederlands.", advice.summary)
        } finally {
            openAiKeys.clearEncryptedKey()
            preferences.clearOpenAiVerificationSnapshot()
            preferences.setAiEnabled(false)
            preferences.setAiProviderPreference(AiProviderPreference.GEMINI_FIRST)
        }
    }

    private fun newGate(
        preferences: UserPreferencesRepository,
        openAiKeys: OpenAiKeyStore,
    ) = AiUsageGate(
        preferencesRepository = preferences,
        geminiKeyMigration = GeminiKeyMigration(EmptyGeminiKeyStore),
        openAiKeyStore = openAiKeys,
        openAiVerificationRecorder = OpenAiVerificationRecorder(preferences),
    )

    private object EmptyGeminiKeyStore : GeminiEncryptedKeyStore {
        override suspend fun readKey(): String? = null

        override suspend fun writeKey(apiKey: String): Boolean = false

        override suspend fun clearKey() = Unit
    }

    private object UnusedGeminiApi : GeminiApi {
        override suspend fun generateContent(
            model: String,
            apiKey: String,
            request: GeminiRequest,
        ): GeminiResponse = error("Gemini must not be called by the OpenAI route test")
    }

    private class FakeOpenAiApi : OpenAiApi {
        var modelDiscoveryCalls = 0
        var responseCalls = 0
        var lastAuthorization: String? = null
        var lastRequest: OpenAiResponseRequest? = null

        override suspend fun listModels(authorization: String): Response<OpenAiModelsResponse> {
            modelDiscoveryCalls += 1
            lastAuthorization = authorization
            return Response.success(
                OpenAiModelsResponse(
                    data = listOf(OpenAiModelDescriptor(id = "gpt-5.6-luna")),
                ),
            )
        }

        override suspend fun createResponse(
            authorization: String,
            request: OpenAiResponseRequest,
        ): Response<OpenAiResponse> {
            responseCalls += 1
            lastAuthorization = authorization
            lastRequest = request
            return Response.success(
                OpenAiResponse(
                    status = "completed",
                    output = listOf(
                        OpenAiOutput(
                            content = listOf(
                                OpenAiOutputContent(
                                    type = "output_text",
                                    text = """
                                        {"trainingFocus":"Krachttraining rustig opbouwen.","korteSamenvatting":"OpenAI-testadvies in het Nederlands.","calorieAdvies":"Houd je calorie doel twee weken stabiel.","macroAdvies":"Eiwit en koolhydraten ondersteunen herstel.","activiteitUitleg":"Lichte dagelijkse beweging vult je training aan.","aandachtspunten":["Volg je gewichtstrend."],"advies":"Evalueer rustig na twee weken.","dataKwaliteit":"Profielgegevens zijn beschikbaar."}
                                    """.trimIndent(),
                                ),
                            ),
                        ),
                    ),
                ),
            )
        }
    }

    private object NoOpPerformanceSessionMonitor : PerformanceSessionMonitor {
        override val sessionId: String = "instrumented-test"

        override fun start(activity: Activity) = Unit

        override fun setEnabled(enabled: Boolean) = Unit

        override fun updateScreen(screenName: String) = Unit

        override fun stop() = Unit

        override fun latestSummary(): PerformanceSessionSummary = PerformanceSessionSummary.Empty
    }
}
