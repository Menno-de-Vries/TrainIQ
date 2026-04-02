package com.trainiq.ai.services

import android.util.Base64
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.trainiq.ai.prompts.GeminiPrompts
import com.trainiq.data.model.GeminiRequest
import com.trainiq.data.remote.GeminiApi
import com.trainiq.domain.model.BiologicalSex
import com.trainiq.domain.model.GoalAdvice
import com.trainiq.domain.model.MealAnalysisResult
import com.trainiq.domain.model.MealScanItem
import com.trainiq.domain.model.MealType
import com.trainiq.domain.model.NutritionFacts
import com.trainiq.domain.model.WeeklyReportResult
import com.trainiq.domain.model.WorkoutDebrief
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import com.trainiq.domain.model.buildGoalBaseline
import com.trainiq.domain.model.suggestMealType

private const val GeminiFlashModel = "gemini-2.5-flash"

@Singleton
class MealAnalysisService @Inject constructor(
    private val api: GeminiApi,
    private val aiUsageGate: AiUsageGate,
) {
    private val gson = Gson()
    private val captureTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    suspend fun analyzeMealImage(path: String, userContext: String, capturedAtMillis: Long): MealAnalysisResult =
        runCatching {
            if (!aiUsageGate.isAiReady()) return fallbackMealScan()
            val apiKey = aiUsageGate.currentApiKeyOrNull() ?: return fallbackMealScan()
            val file = File(path)
            val suggestedMealType = suggestMealType(capturedAtMillis)
            val captureTime = Instant.ofEpochMilli(capturedAtMillis)
                .atZone(ZoneId.systemDefault())
                .format(captureTimeFormatter)
            val scanContext = buildString {
                append("User took this photo at $captureTime. ")
                append("Suggested meal type: ${suggestedMealType.label}. ")
                append(userContext.ifBlank { "Identify the food, estimate portion sizes, and return exact macros." })
            }
            val response = api.generateContent(
                model = GeminiFlashModel,
                apiKey = apiKey,
                request = GeminiRequest(
                    contents = listOf(
                        GeminiRequest.Content(
                            parts = listOf(
                                GeminiRequest.Part(text = GeminiPrompts.mealScanner(scanContext)),
                                GeminiRequest.Part(
                                    inlineData = GeminiRequest.InlineData(
                                        mimeType = "image/jpeg",
                                        data = Base64.encodeToString(file.readBytes(), Base64.NO_WRAP),
                                    ),
                                ),
                            ),
                        ),
                ),
                    generationConfig = GeminiRequest.GenerationConfig(
                        responseMimeType = "application/json",
                    ),
                ),
            )
            val text = response.candidates.firstOrNull()?.content?.parts?.joinToString(" ") { it.text }.orEmpty()
            parseMealScan(text, suggestedMealType)
        }.getOrElse { fallbackMealScan() }

    private fun parseMealScan(text: String, fallbackMealType: MealType): MealAnalysisResult {
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
                suggestedMealType = root.get("suggestedMealType")
                    ?.asString
                    ?.trim()
                    ?.uppercase()
                    ?.let { raw -> MealType.entries.firstOrNull { it.name == raw } }
                    ?: fallbackMealType,
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
                suggestedMealType = fallbackMealType,
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
class WorkoutDebriefService internal constructor(
    private val api: GeminiApi,
    private val apiKeyProvider: suspend () -> String?,
) {
    @Inject
    constructor(
        api: GeminiApi,
        aiUsageGate: AiUsageGate,
    ) : this(
        api = api,
        apiKeyProvider = {
            if (aiUsageGate.isAiReady()) aiUsageGate.currentApiKeyOrNull() else null
        },
    )

    suspend fun generateWorkoutDebrief(
        totalVolume: Double,
        progression: Double,
        distribution: String,
        avgRpe: Float,
        topExercises: String,
        weeklyFrequency: Int,
    ): WorkoutDebrief =
        runCatching {
            val apiKey = apiKeyProvider() ?: return fallbackWorkoutDebriefResult(totalVolume, progression)
            val response = api.generateContent(
                model = GeminiFlashModel,
                apiKey = apiKey,
                request = GeminiRequest(
                    contents = listOf(
                        GeminiRequest.Content(
                            parts = listOf(
                                GeminiRequest.Part(
                                    text = GeminiPrompts.workoutDebrief(
                                        totalVolume = totalVolume,
                                        progression = progression,
                                        distribution = distribution,
                                        avgRpe = avgRpe,
                                        topExercises = topExercises,
                                        weeklyFrequency = weeklyFrequency,
                                    ),
                                ),
                            ),
                        ),
                    ),
                    generationConfig = GeminiRequest.GenerationConfig(
                        responseMimeType = "application/json",
                    ),
                ),
            )
            val text = response.candidates.firstOrNull()?.content?.parts?.joinToString(" ") { it.text }.orEmpty()
            parseWorkoutDebriefResponse(text, totalVolume, progression)
        }.getOrElse { fallbackWorkoutDebriefResult(totalVolume, progression) }
}

@Singleton
class GoalAdvisorService @Inject constructor(
    private val api: GeminiApi,
    private val aiUsageGate: AiUsageGate,
) {
    suspend fun generateGoalAdvice(
        height: Double,
        weight: Double,
        bodyFat: Double,
        age: Int,
        sex: BiologicalSex,
        activityLevel: String,
        goal: String,
    ): GoalAdvice =
        runCatching {
            val baseline = deterministicGoalAdvice(
                height = height,
                weight = weight,
                bodyFat = bodyFat,
                age = age,
                sex = sex,
                activityLevel = activityLevel,
                goal = goal,
            )
            if (!aiUsageGate.isAiReady()) return baseline
            val apiKey = aiUsageGate.currentApiKeyOrNull() ?: return baseline
            val response = api.generateContent(
                model = GeminiFlashModel,
                apiKey = apiKey,
                request = GeminiRequest(
                    contents = listOf(
                        GeminiRequest.Content(
                            parts = listOf(
                                GeminiRequest.Part(
                                    text = GeminiPrompts.goalAdvisor(
                                        height = height,
                                        weight = weight,
                                        bodyFat = bodyFat,
                                        age = age,
                                        sex = sex,
                                        activityLevel = activityLevel,
                                        goal = goal,
                                        baseline = baseline,
                                    ),
                                ),
                            ),
                        ),
                    ),
                    thinkingConfig = GeminiRequest.ThinkingConfig(
                        includeThoughts = false,
                        thinkingBudget = 1000,
                    ),
                    generationConfig = GeminiRequest.GenerationConfig(
                        responseMimeType = "application/json",
                    ),
                ),
            )
            val text = response.candidates.firstOrNull()?.content?.parts?.joinToString(" ") { it.text }.orEmpty()
            parseGoalAdvice(text, baseline)
        }.getOrElse {
            deterministicGoalAdvice(
                height = height,
                weight = weight,
                bodyFat = bodyFat,
                age = age,
                sex = sex,
                activityLevel = activityLevel,
                goal = goal,
            )
        }

    private fun parseGoalAdvice(text: String, baseline: GoalAdvice): GoalAdvice =
        runCatching {
            val root = JsonParser.parseString(text).asJsonObject
            baseline.copy(
                trainingFocus = root.get("trainingFocus")?.asString ?: baseline.trainingFocus,
                summary = root.get("summary")?.asString ?: baseline.summary,
                rawResponse = text,
            )
        }.getOrElse { baseline }

    fun deterministicGoalAdvice(
        height: Double,
        weight: Double,
        bodyFat: Double,
        age: Int,
        sex: BiologicalSex,
        activityLevel: String,
        goal: String,
    ): GoalAdvice {
        val baseline = buildGoalBaseline(
            heightCm = height,
            weightKg = weight,
            bodyFat = bodyFat,
            age = age,
            sex = sex,
            activityLevel = activityLevel,
            goal = goal,
        )
        val trainingFocus = when {
            goal.contains("bulk", ignoreCase = true) -> "Progressive overload on compounds"
            goal.contains("cut", ignoreCase = true) || goal.contains("fat", ignoreCase = true) -> "High adherence, steps, and recovery"
            bodyFat > 20 -> "Body recomposition with consistent strength work"
            height > 0 && weight / ((height / 100.0) * (height / 100.0)) < 22 -> "Build muscle with steady weekly volume"
            else -> "Balanced strength and recovery"
        }
        return GoalAdvice(
            bmr = baseline.bmr,
            maintenanceCalories = baseline.maintenanceCalories,
            activityMultiplier = baseline.activityMultiplier,
            calorieTarget = baseline.targetCalories,
            proteinTarget = baseline.proteinTarget,
            carbsTarget = baseline.carbsTarget,
            fatTarget = baseline.fatTarget,
            trainingFocus = trainingFocus,
            summary = "Baseline: BMR ${baseline.bmr} kcal, maintenance ${baseline.maintenanceCalories} kcal, target ${baseline.targetCalories} kcal. Aim for ${baseline.proteinTarget} g protein and keep training focused on $trainingFocus.",
            rawResponse = null,
        )
    }
}

@Singleton
class WeeklyReportService @Inject constructor(
    private val api: GeminiApi,
    private val aiUsageGate: AiUsageGate,
) {
    suspend fun generateWeeklyReport(volume: Double, weightTrend: Double, adherence: Int): WeeklyReportResult =
        runCatching {
            if (!aiUsageGate.isAiReady()) return fallbackWeeklyReport(adherence)
            val apiKey = aiUsageGate.currentApiKeyOrNull() ?: return fallbackWeeklyReport(adherence)
            val response = api.generateContent(
                model = GeminiFlashModel,
                apiKey = apiKey,
                request = GeminiRequest(
                    contents = listOf(
                        GeminiRequest.Content(
                            parts = listOf(GeminiRequest.Part(text = GeminiPrompts.weeklyReport(volume, weightTrend, adherence))),
                        ),
                    ),
                    thinkingConfig = GeminiRequest.ThinkingConfig(
                        includeThoughts = false,
                        thinkingBudget = 1000,
                    ),
                    generationConfig = GeminiRequest.GenerationConfig(
                        responseMimeType = "application/json",
                    ),
                ),
            )
            val text = response.candidates.firstOrNull()?.content?.parts?.joinToString(" ") { it.text }.orEmpty()
            parseWeeklyReport(text, adherence)
        }.getOrElse { fallbackWeeklyReport(adherence) }

    private fun parseWeeklyReport(text: String, adherence: Int): WeeklyReportResult =
        runCatching {
            val root = JsonParser.parseString(text).asJsonObject
            WeeklyReportResult(
                summary = root.get("summary")?.asString ?: fallbackWeeklyReport(adherence).summary,
                wins = root.getAsJsonArray("wins")?.map { it.asString }.orEmpty(),
                risks = root.getAsJsonArray("risks")?.map { it.asString }.orEmpty(),
                nextWeekFocus = root.get("nextWeekFocus")?.asString ?: "Protect recovery before adding more volume.",
                thinkingProcess = root.getAsJsonArray("thinkingProcess")?.map { it.asString }.orEmpty(),
                rawResponse = text,
            )
        }.getOrElse { fallbackWeeklyReport(adherence) }

    private fun fallbackWeeklyReport(adherence: Int): WeeklyReportResult =
        WeeklyReportResult(
            summary = "Weekly report: training volume is trending up, body weight is stable, and adherence is $adherence%. Prioritize sleep before increasing volume again.",
            wins = listOf("Training consistency is improving."),
            risks = listOf("Recovery may limit progress if sleep stays low."),
            nextWeekFocus = "Hold volume steady and improve sleep quality before pushing load.",
        )
}

internal fun parseWorkoutDebriefResponse(
    text: String,
    totalVolume: Double,
    progression: Double,
): WorkoutDebrief = runCatching {
    val root = JsonParser.parseString(text).asJsonObject
    WorkoutDebrief(
        summary = root.get("summary")?.asString ?: "Great session.",
        progressionFeedback = root.get("progressionFeedback")?.asString ?: "Progression remained stable.",
        recommendation = root.get("recommendation")?.asString
            ?: "Repeat this session and aim for one extra rep on main lifts.",
        nextSessionFocus = root.get("nextSessionFocus")?.asString?.trim().orEmpty()
            .ifBlank { "Maintain current weights" },
        recoveryScore = (root.get("recoveryScore")?.asInt ?: 75).coerceIn(0, 100),
        intensitySignal = root.get("intensitySignal")?.asString?.trim()?.uppercase().orEmpty()
            .ifBlank { "MAINTAIN" },
    )
}.getOrElse { fallbackWorkoutDebriefResult(totalVolume, progression) }

internal fun fallbackWorkoutDebriefResult(totalVolume: Double, progression: Double) = WorkoutDebrief(
    summary = "Great session. Volume reached ${totalVolume.toInt()} kg.",
    progressionFeedback = "Volume changed by ${String.format(Locale.US, "%.1f", progression)}% versus the previous session.",
    recommendation = "Keep the same split and add load to the first compound lift next week.",
    nextSessionFocus = "Maintain current weights",
    recoveryScore = 75,
    intensitySignal = "MAINTAIN",
)
