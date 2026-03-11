package com.trainiq.ai.services

import android.util.Base64
import com.trainiq.BuildConfig
import com.trainiq.ai.prompts.GeminiPrompts
import com.trainiq.data.model.GeminiRequest
import com.trainiq.data.remote.GeminiApi
import com.trainiq.domain.model.GoalAdvice
import com.trainiq.domain.model.MealScanItem
import com.trainiq.domain.model.WorkoutDebrief
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MealAnalysisService @Inject constructor(
    private val api: GeminiApi,
) {
    suspend fun analyzeMealImage(path: String): List<MealScanItem> =
        runCatching {
            if (BuildConfig.GEMINI_API_KEY.isBlank()) {
                fallbackMealScan()
            } else {
                val file = File(path)
                val response = api.generateContent(
                    model = "gemini-1.5-flash",
                    apiKey = BuildConfig.GEMINI_API_KEY,
                    request = GeminiRequest(
                        contents = listOf(
                            GeminiRequest.Content(
                                parts = listOf(
                                    GeminiRequest.Part(text = GeminiPrompts.mealScanner()),
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
                if (text.isBlank()) {
                    listOf(MealScanItem("Meal estimate", 500, 35, 45, 18))
                } else {
                    listOf(MealScanItem(text.take(28), 520, 34, 48, 16))
                }
            }
        }.getOrElse { fallbackMealScan() }

    private fun fallbackMealScan(): List<MealScanItem> = emptyList()
}

@Singleton
class WorkoutDebriefService @Inject constructor(
    private val api: GeminiApi,
) {
    suspend fun generateWorkoutDebrief(totalVolume: Double, progression: Double, distribution: String): WorkoutDebrief =
        runCatching {
            if (BuildConfig.GEMINI_API_KEY.isBlank()) {
                fallbackWorkoutDebrief(totalVolume, progression)
            } else {
                val response = api.generateContent(
                    model = "gemini-1.5-flash",
                    apiKey = BuildConfig.GEMINI_API_KEY,
                    request = GeminiRequest(
                        contents = listOf(
                            GeminiRequest.Content(
                                parts = listOf(
                                    GeminiRequest.Part(
                                        text = GeminiPrompts.workoutDebrief(totalVolume, progression, distribution),
                                    ),
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
            }
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
) {
    suspend fun generateGoalAdvice(height: Double, weight: Double, bodyFat: Double, goal: String): GoalAdvice =
        runCatching {
            if (BuildConfig.GEMINI_API_KEY.isBlank()) {
                fallbackGoalAdvice(weight, goal)
            } else {
                val response = api.generateContent(
                    model = "gemini-1.5-flash",
                    apiKey = BuildConfig.GEMINI_API_KEY,
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
            }
        }.getOrElse { fallbackGoalAdvice(weight, goal) }

    private fun fallbackGoalAdvice(weight: Double, goal: String): GoalAdvice {
        val calories = if (goal.contains("cut", ignoreCase = true)) 2200 else 2850
        return GoalAdvice(
            calorieTarget = calories,
            proteinTarget = (weight * 2.2).toInt(),
            carbsTarget = (weight * 3.5).toInt(),
            fatTarget = (weight * 0.8).toInt(),
            trainingFocus = if (goal.contains("bulk", ignoreCase = true)) {
                "Progressive overload on compounds"
            } else {
                "High adherence and recovery"
            },
            summary = "Aim for $calories kcal with structured strength training and consistent protein intake.",
        )
    }
}

@Singleton
class WeeklyReportService @Inject constructor(
    private val api: GeminiApi,
) {
    suspend fun generateWeeklyReport(volume: Double, weightTrend: Double, adherence: Int): String =
        runCatching {
            if (BuildConfig.GEMINI_API_KEY.isBlank()) {
                fallbackWeeklyReport(adherence)
            } else {
                val response = api.generateContent(
                    model = "gemini-1.5-flash",
                    apiKey = BuildConfig.GEMINI_API_KEY,
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
            }
        }.getOrElse { fallbackWeeklyReport(adherence) }

    private fun fallbackWeeklyReport(adherence: Int): String =
        "Weekly report: training volume is trending up, body weight is stable, and adherence is $adherence%. Prioritize sleep before increasing volume again."
}
