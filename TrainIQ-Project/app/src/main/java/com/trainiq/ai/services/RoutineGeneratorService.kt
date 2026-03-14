package com.trainiq.ai.services

import com.google.gson.JsonParser
import com.trainiq.ai.prompts.GeminiPrompts
import com.trainiq.data.model.GeminiRequest
import com.trainiq.data.remote.GeminiApi
import javax.inject.Inject
import javax.inject.Singleton

data class GeneratedRoutine(
    val routineName: String,
    val routineDescription: String,
    val days: List<GeneratedDay>,
)

data class GeneratedDay(
    val dayName: String,
    val exercises: List<GeneratedExercise>,
)

data class GeneratedExercise(
    val exerciseName: String,
    val muscleGroup: String,
    val equipment: String,
    val targetSets: Int,
    val repRange: String,
    val restSeconds: Int,
)

@Singleton
class RoutineGeneratorService @Inject constructor(
    private val api: GeminiApi,
    private val aiUsageGate: AiUsageGate,
) {
    suspend fun generateRoutine(
        goal: String,
        targetFocus: String,
        daysPerWeek: Int,
        equipment: String,
    ): GeneratedRoutine? = runCatching {
        val apiKey = aiUsageGate.currentApiKeyOrNull() ?: return null
        val response = api.generateContent(
            model = "gemini-2.5-flash",
            apiKey = apiKey,
            request = GeminiRequest(
                contents = listOf(
                    GeminiRequest.Content(
                        parts = listOf(
                            GeminiRequest.Part(
                                text = GeminiPrompts.routineGenerator(
                                    goal = goal,
                                    targetFocus = targetFocus,
                                    daysPerWeek = daysPerWeek,
                                    equipment = equipment,
                                ),
                            ),
                        ),
                    ),
                ),
                generationConfig = GeminiRequest.GenerationConfig(),
            ),
        )
        val text = response.candidates.firstOrNull()?.content?.parts?.joinToString(" ") { it.text }.orEmpty()
        parseRoutine(text)
    }.getOrNull()

    private fun parseRoutine(text: String): GeneratedRoutine? = runCatching {
        val root = JsonParser.parseString(text).asJsonObject
        val routineName = root.get("routineName")?.asString?.takeIf { it.isNotBlank() } ?: return null
        val routineDescription = root.get("routineDescription")?.asString.orEmpty()
        val days = root.getAsJsonArray("days")?.map { dayElement ->
            val dayObj = dayElement.asJsonObject
            val dayName = dayObj.get("dayName")?.asString ?: "Day"
            val exercises = dayObj.getAsJsonArray("exercises")?.map { exElement ->
                val exObj = exElement.asJsonObject
                GeneratedExercise(
                    exerciseName = exObj.get("exerciseName")?.asString ?: "Exercise",
                    muscleGroup = exObj.get("muscleGroup")?.asString ?: "General",
                    equipment = exObj.get("equipment")?.asString ?: "Bodyweight",
                    targetSets = exObj.get("targetSets")?.asInt ?: 3,
                    repRange = exObj.get("repRange")?.asString ?: "8-12",
                    restSeconds = exObj.get("restSeconds")?.asInt ?: 90,
                )
            }.orEmpty()
            GeneratedDay(dayName = dayName, exercises = exercises)
        }.orEmpty()
        GeneratedRoutine(
            routineName = routineName,
            routineDescription = routineDescription,
            days = days,
        )
    }.getOrNull()
}
