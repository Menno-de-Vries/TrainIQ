package com.trainiq.ai.services

import android.util.Base64
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.trainiq.ai.prompts.GeminiPrompts
import com.trainiq.data.model.GeminiRequest
import com.trainiq.data.remote.GeminiApi
import com.trainiq.domain.model.GoalAdvice
import com.trainiq.domain.model.MealAnalysisResult
import com.trainiq.domain.model.MealScanItem
import com.trainiq.domain.model.NutritionFacts
import com.trainiq.domain.model.WorkoutDebrief
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MealAnalysisService @Inject constructor(
    private val api: GeminiApi,
    private val aiUsageGate: AiUsageGate,
) {
    private val gson = Gson()

    suspend fun analyzeMealImage(path: String, userContext: String): MealAnalysisResult =
        runCatching {
            val apiKey = aiUsageGate.currentApiKeyOrNull() ?: return fallbackMealScan()
            val file = File(path)
            val response = api.generateContent(
                model = "gemini-1.5-flash",
                apiKey = apiKey,
                request = GeminiRequest(
                    contents = listOf(
                        GeminiRequest.Content(
                            parts = listOf(
                                GeminiRequest.Part(text = GeminiPrompts.mealScanner(userContext)),
                                GeminiRequest.Part(
                                    inlineData = GeminiRequest.InlineData(
                                        mimeType = "image/jpeg",
                                        data = Base64.encodeToString(file.readBytes(), Base64.NO_WRAP),
                                    ),
                                ),
                            ),
                        ),
                    ),
                ),
            )
            val text = response.candidates.firstOrNull()?.content?.parts?.joinToString(" ") { it.text }.orEmpty()
            parseMealScan(text)
        }.getOrElse { fallbackMealScan() }

    private fun parseMealScan(text: String): MealAnalysisResult {
        if (text.isBlank()) return fallbackMealScan()
        return runCatching {
            val root = JsonParser.parseString(text).asJsonObject
            val items = root.getAsJsonArray("items")?.mapNotNull { element ->
                val obj = element.asJsonObject
                val name = obj.get("name")?.asString?.trim().orEmpty()
                if (name.isBlank()) return@mapNotNull null
                MealScanItem(
                    name = name,
                    estimatedGrams = obj.get("estimatedGrams")?.asDouble ?: 100.0,
                    nutrition = NutritionFacts(
                        calories = obj.get("calories")?.asDouble ?: 0.0,
                        protein = obj.get("protein")?.asDouble ?: 0.0,
                        carbs = obj.get("carbs")?.asDouble ?: 0.0,
                        fat = obj.get("fat")?.asDouble ?: 0.0,
                    ),
                    confidence = obj.get("confidence")?.asString,
                    notes = obj.get("notes")?.asString,
                )
            }.orEmpty()
            MealAnalysisResult(
                items = items,
                notes = root.get("notes")?.asString,
                rawResponse = text,
            )
        }.getOrElse {
            MealAnalysisResult(
                items = listOf(
                    MealScanItem(
                        name = text.lineSequence().firstOrNull()?.take(40).orEmpty().ifBlank { "Meal estimate" },
                        estimatedGrams = 100.0,
                        nutrition = NutritionFacts(0.0, 0.0, 0.0, 0.0),
                        confidence = "low",
                        notes = "The AI response was not structured, so review and edit before saving.",
                    ),
                ),
                notes = "Structured parsing failed. Please review the estimate carefully.",
                rawResponse = text,
            )
        }
    }

    private fun fallbackMealScan(): MealAnalysisResult = MealAnalysisResult(
        items = emptyList(),
        notes = "AI meal analysis is unavailable right now. You can still add the meal manually.",
    )
}

@Singleton
class WorkoutDebriefService @Inject constructor(
    private val api: GeminiApi,
    private val aiUsageGate: AiUsageGate,
) {
    suspend fun generateWorkoutDebrief(totalVolume: Double, progression: Double, distribution: String): WorkoutDebrief =
        runCatching {
            val apiKey = aiUsageGate.currentApiKeyOrNull() ?: return fallbackWorkoutDebrief(totalVolume, progression)
            val response = api.generateContent(
                model = "gemini-1.5-flash",
                apiKey = apiKey,
                request = GeminiRequest(
                    contents = listOf(
                        GeminiRequest.Content(
                            parts = listOf(
                                GeminiRequest.Part(text = GeminiPrompts.workoutDebrief(totalVolume, progression, distribution)),
                            ),
                        ),
                    ),
                ),
            )
            val lines = response.candidates.firstOrNull()?.content?.parts
                ?.joinToString(" ") { it.text }
                .orEmpty()
                .lines()
                .filter { it.isNotBlank() }
            WorkoutDebrief(
                summary = lines.getOrElse(0) { "Great session." },
                progressionFeedback = lines.getOrElse(1) { "Progression remained stable." },
                recommendation = lines.getOrElse(2) { "Repeat this session and aim for one extra rep on main lifts." },
            )
        }.getOrElse { fallbackWorkoutDebrief(totalVolume, progression) }

    private fun fallbackWorkoutDebrief(totalVolume: Double, progression: Double) = WorkoutDebrief(
        summary = "Great session. Volume reached ${totalVolume.toInt()} kg.",
        progressionFeedback = "Volume changed by ${"%.1f".format(progression)}% versus the previous session.",
        recommendation = "Keep the same split and add load to the first compound lift next week.",
    )
}

@Singleton
class GoalAdvisorService @Inject constructor(
    private val api: GeminiApi,
    private val aiUsageGate: AiUsageGate,
) {
    suspend fun generateGoalAdvice(height: Double, weight: Double, bodyFat: Double, goal: String): GoalAdvice =
        runCatching {
            val apiKey = aiUsageGate.currentApiKeyOrNull() ?: return deterministicGoalAdvice(height, weight, bodyFat, goal)
            val response = api.generateContent(
                model = "gemini-1.5-flash",
                apiKey = apiKey,
                request = GeminiRequest(
                    contents = listOf(
                        GeminiRequest.Content(
                            parts = listOf(GeminiRequest.Part(text = GeminiPrompts.goalAdvisor(height, weight, bodyFat, goal))),
                        ),
                    ),
                ),
            )
            GoalAdvice(
                calorieTarget = 2700,
                proteinTarget = (weight * 2.1).toInt(),
                carbsTarget = (weight * 3.4).toInt(),
                fatTarget = (weight * 0.8).toInt(),
                trainingFocus = "Upper chest, back thickness, and weekly step consistency",
                summary = response.candidates.firstOrNull()?.content?.parts?.joinToString(" ") { it.text }
                    ?: "Structured nutrition and progressive training will support your goal.",
            )
        }.getOrElse { deterministicGoalAdvice(height, weight, bodyFat, goal) }

    fun deterministicGoalAdvice(height: Double, weight: Double, bodyFat: Double, goal: String): GoalAdvice {
        val isCut = goal.contains("cut", ignoreCase = true) || goal.contains("fat", ignoreCase = true)
        val calories = if (isCut) {
            (weight * 27).toInt().coerceAtLeast(1800)
        } else {
            (weight * 35).toInt().coerceAtLeast(2200)
        }
        val protein = (weight * 2.2).toInt().coerceAtLeast(120)
        val fat = (weight * 0.8).toInt().coerceAtLeast(45)
        val carbs = (calories - protein * 4 - fat * 9).div(4).coerceAtLeast(120)
        val trainingFocus = when {
            goal.contains("bulk", ignoreCase = true) -> "Progressive overload on compounds"
            goal.contains("cut", ignoreCase = true) -> "High adherence, steps, and recovery"
            bodyFat > 20 -> "Body recomposition with consistent strength work"
            height > 0 && weight / ((height / 100.0) * (height / 100.0)) < 22 -> "Build muscle with steady weekly volume"
            else -> "Balanced strength and recovery"
        }
        return GoalAdvice(
            calorieTarget = calories,
            proteinTarget = protein,
            carbsTarget = carbs,
            fatTarget = fat,
            trainingFocus = trainingFocus,
            summary = "Aim for $calories kcal with $protein g protein and keep training focused on $trainingFocus.",
        )
    }
}

@Singleton
class WeeklyReportService @Inject constructor(
    private val api: GeminiApi,
    private val aiUsageGate: AiUsageGate,
) {
    suspend fun generateWeeklyReport(volume: Double, weightTrend: Double, adherence: Int): String =
        runCatching {
            val apiKey = aiUsageGate.currentApiKeyOrNull() ?: return fallbackWeeklyReport(adherence)
            val response = api.generateContent(
                model = "gemini-1.5-flash",
                apiKey = apiKey,
                request = GeminiRequest(
                    contents = listOf(
                        GeminiRequest.Content(
                            parts = listOf(GeminiRequest.Part(text = GeminiPrompts.weeklyReport(volume, weightTrend, adherence))),
                        ),
                    ),
                ),
            )
            response.candidates.firstOrNull()?.content?.parts?.joinToString(" ") { it.text }
                ?: fallbackWeeklyReport(adherence)
        }.getOrElse { fallbackWeeklyReport(adherence) }

    private fun fallbackWeeklyReport(adherence: Int): String =
        "Weekly report: training volume is trending up, body weight is stable, and adherence is $adherence%. Prioritize sleep before increasing volume again."
}
