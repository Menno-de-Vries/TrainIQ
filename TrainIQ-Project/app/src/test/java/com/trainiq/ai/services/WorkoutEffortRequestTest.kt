package com.trainiq.ai.services

import com.google.gson.Gson
import com.google.gson.JsonParser
import com.trainiq.data.model.GeminiRequest
import com.trainiq.data.model.GeminiResponse
import com.trainiq.data.remote.GeminiApi
import com.trainiq.domain.model.LoggedSet
import com.trainiq.domain.model.WorkoutEffortSummary
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class WorkoutEffortRequestTest {
    @Test fun actualSerializedGeminiRequestCarriesPerExerciseMeanSetDetailsAndExplicitNulls() = runTest {
        var serializedRequest = ""
        val api = object : GeminiApi {
            override suspend fun generateContent(model: String, apiKey: String, request: GeminiRequest): GeminiResponse {
                serializedRequest = Gson().toJson(request)
                return GeminiResponse()
            }
        }
        val sets = listOf(
            set(1, 6.0), set(1, 8.0), set(1, 10.0).copy(completed = false),
            set(2, 9.0), set(2, 0.0), set(3, 0.0),
        )
        WorkoutDebriefService(api) { "synthetic-key" }.generateWorkoutDebrief(
            totalVolume = 1000.0, progression = null, distribution = "Chest, Back",
            avgRpe = WorkoutEffortSummary.averageRpe(sets)?.toFloat(), topExercises = "Bench", weeklyFrequency = 2,
            exerciseEffort = WorkoutEffortSummary.exercises(sets, mapOf(1L to "Bench", 2L to "Row", 3L to "Squat")),
        )
        val root = JsonParser.parseString(serializedRequest).asJsonObject
        val prompt = root.getAsJsonArray("contents")[0].asJsonObject.getAsJsonArray("parts")[0].asJsonObject.get("text").asString
        val jsonLine = prompt.lineSequence().first { it.contains("setdetails (JSON):") }.substringAfter("setdetails (JSON): ")
        val exercises = JsonParser.parseString(jsonLine).asJsonArray
        assertEquals(3, exercises.size())
        assertEquals(7.0, exercises[0].asJsonObject.get("averageRpe").asDouble, 0.0)
        assertEquals(2, exercises[0].asJsonObject.getAsJsonArray("sets").size())
        assertEquals(9.0, exercises[1].asJsonObject.get("averageRpe").asDouble, 0.0)
        assertTrue(exercises[1].asJsonObject.getAsJsonArray("sets")[1].asJsonObject.get("rpe").isJsonNull)
        assertTrue(exercises[2].asJsonObject.get("averageRpe").isJsonNull)
        assertTrue(prompt.contains("Meld RPE niet als ontbrekend"))
        assertEquals("application/json", root.getAsJsonObject("generationConfig").get("responseMimeType").asString)
    }

    private fun set(exerciseId: Long, rpe: Double) = LoggedSet(exerciseId = exerciseId, weight = 60.0, reps = 8, rpe = rpe)
}
