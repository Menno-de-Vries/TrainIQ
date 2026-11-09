package com.trainiq.ai.services

import com.google.gson.Gson
import com.google.gson.JsonParser
import com.trainiq.data.model.GeminiRequest
import com.trainiq.data.model.GeminiResponse
import com.trainiq.data.remote.GeminiApi
import com.trainiq.domain.model.LoggedSet
import com.trainiq.domain.model.WorkoutEffortSummary
import com.trainiq.domain.model.SetType
import com.trainiq.domain.model.buildWorkoutAnalysisSnapshot
import com.trainiq.core.database.ExerciseEntity
import com.trainiq.data.local.ActiveWorkoutSessionStorage
import com.trainiq.data.local.ActiveWorkoutSetStorage
import com.trainiq.data.local.TrainIqStorageState
import com.trainiq.data.repository.buildWorkoutDebriefAnalysis
import com.trainiq.data.repository.deleteActiveWorkoutSetById
import com.trainiq.data.repository.updateActiveWorkoutSetById
import com.trainiq.data.repository.toDomain
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class WorkoutEffortRequestTest {
    @Test fun correctedAndDeletedStoredSetsReachActualRequestWithoutStaleValuesOrPlannedExercises() = runTest {
        val original = ActiveWorkoutSetStorage(id = 1L, exerciseId = 1L, weight = 60.0, reps = 8, rpe = 9.0)
        val state = TrainIqStorageState(activeWorkoutSession = ActiveWorkoutSessionStorage(
            dayId = 7L, notes = "PRIVATE_SESSION_NOTE", loggedSets = listOf(original,
                ActiveWorkoutSetStorage(id = 2L, exerciseId = 2L, weight = 999.0, reps = 10, rpe = 10.0),
                ActiveWorkoutSetStorage(id = 3L, exerciseId = 99L, weight = 0.0, reps = 12),
                ActiveWorkoutSetStorage(id = 4L, exerciseId = 2L, weight = 888.0, reps = 10, completed = false)),
        ))
        val updated = state.updateActiveWorkoutSetById(1L, original.copy(weight = 50.0, reps = 6, rpe = 0.0), 100L)
            .deleteActiveWorkoutSetById(2L, 200L)
        val analysis = buildWorkoutDebriefAnalysis(
            updated.activeWorkoutSession!!.toDomain().loggedSets.map { it.toLoggedSet() },
            listOf(ExerciseEntity(1L, "Bench", "Borst", "Barbell"), ExerciseEntity(2L, "Unused row", "Rug", "Cable")),
        )
        assertEquals(2, analysis.snapshot.completedSetCount)
        assertEquals(300.0, analysis.snapshot.totalVolumeKg, 0.0)
        assertEquals("Borst 1, Onbekend 1", analysis.distribution)
        var serializedRequest = ""
        val api = object : GeminiApi {
            override suspend fun generateContent(model: String, apiKey: String, request: GeminiRequest): GeminiResponse {
                serializedRequest = Gson().toJson(request)
                return GeminiResponse()
            }
        }
        WorkoutDebriefService(api) { "synthetic-key" }.generateWorkoutDebrief(
            totalVolume = analysis.snapshot.totalVolumeKg, progression = null, distribution = analysis.distribution,
            avgRpe = analysis.snapshot.reportedMeanRpe?.toFloat(), topExercises = analysis.topExercises,
            weeklyFrequency = 1, exerciseEffort = analysis.exerciseEffort, analysisSnapshot = analysis.snapshot,
        )
        val root = JsonParser.parseString(serializedRequest).asJsonObject
        val prompt = root.getAsJsonArray("contents")[0].asJsonObject.getAsJsonArray("parts")[0].asJsonObject.get("text").asString
        val details = JsonParser.parseString(prompt.lineSequence().first { it.contains("setdetails (JSON):") }
            .substringAfter("setdetails (JSON): ")).asJsonArray
        assertEquals(2, details.size())
        val bench = details[0].asJsonObject
        assertTrue(bench.get("averageRpe").isJsonNull)
        val set = bench.getAsJsonArray("sets").single().asJsonObject
        assertEquals(50.0, set.get("weightKg").asDouble, 0.0)
        assertEquals(6, set.get("reps").asInt)
        assertTrue(set.get("rpe").isJsonNull)
        assertEquals("Oefening 99", details[1].asJsonObject.get("exerciseName").asString)
        assertFalse(prompt.contains("Unused row"))
        assertFalse(prompt.contains("999"))
        assertFalse(prompt.contains("888"))
        assertFalse(prompt.contains("PRIVATE_SESSION_NOTE"))
        assertTrue(prompt.contains("Totaalvolume: 300.0kg"))
    }

    @Test fun snapshotFactsAndCoverageReachSerializedRequestWithSeparateComparisonVolume() = runTest {
        var serializedRequest = ""
        val api = object : GeminiApi {
            override suspend fun generateContent(model: String, apiKey: String, request: GeminiRequest): GeminiResponse {
                serializedRequest = Gson().toJson(request)
                return GeminiResponse()
            }
        }
        val sets = listOf(
            set(1, 3.0).copy(weight = 20.0, reps = 10, setType = SetType.WARM_UP),
            set(1, 8.0), set(1, 0.0), set(1, 10.0).copy(completed = false),
        )
        WorkoutDebriefService(api) { "synthetic-key" }.generateWorkoutDebrief(
            totalVolume = 9999.0, progression = 20.0, distribution = "Borst 1", avgRpe = 10f,
            topExercises = "Bench", weeklyFrequency = 1,
            exerciseEffort = WorkoutEffortSummary.exercises(sets, mapOf(1L to "Bench")),
            analysisSnapshot = buildWorkoutAnalysisSnapshot(sets),
        )
        val root = JsonParser.parseString(serializedRequest).asJsonObject
        val prompt = root.getAsJsonArray("contents")[0].asJsonObject.getAsJsonArray("parts")[0].asJsonObject.get("text").asString
        assertTrue(prompt.contains("Totaalvolume: 1160.0kg"))
        assertTrue(prompt.contains("Voltooide sets: 3; herhalingen: 26"))
        assertTrue(prompt.contains("Vergelijkingsvolume: 960.0kg; vergelijkingssets: 2"))
        assertTrue(prompt.contains("Gemiddelde set-RPE: 5.5 / 10"))
        assertTrue(prompt.contains("RPE-dekking: 2 van 3 sets ingevuld"))
        assertTrue(prompt.contains("De volumeprogressie betreft uitsluitend het vergelijkingsvolume"))
        assertTrue(prompt.contains("Een ongewogen gemiddelde over alle settypen is geen gemeten sessie-RPE"))
        assertFalse(prompt.contains("Als gemiddelde RPE < 6"))
        assertFalse(prompt.contains("Als gemiddelde RPE > 8,5"))
        assertFalse(prompt.contains("9999"))
        assertEquals(1000, root.getAsJsonObject("generationConfig").getAsJsonObject("thinkingConfig").get("thinkingBudget").asInt)
        assertEquals("application/json", root.getAsJsonObject("generationConfig").get("responseMimeType").asString)
        assertNotNull(root.getAsJsonObject("generationConfig").get("responseJsonSchema"))
    }

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
