package com.trainiq.ai.services

import com.google.gson.Gson
import com.google.gson.JsonParser
import com.trainiq.ai.prompts.GeminiPrompts
import com.trainiq.data.model.GeminiRequest
import com.trainiq.data.remote.GeminiApi
import com.trainiq.domain.model.BiologicalSex
import com.trainiq.domain.model.GoalAdvice
import com.trainiq.domain.model.MealAnalysisResult
import com.trainiq.domain.model.MealAnalysisSource
import com.trainiq.domain.model.MealScanItem
import com.trainiq.domain.model.MealType
import com.trainiq.domain.model.NutritionFacts
import com.trainiq.domain.model.WeeklyReportResult
import com.trainiq.domain.model.WorkoutDebrief
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Base64
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import com.trainiq.domain.model.buildGoalBaseline
import com.trainiq.domain.model.suggestMealType

class MealAnalysisUnavailableException(
    message: String = "Scan mislukt. Probeer opnieuw.",
    cause: Throwable? = null,
) : RuntimeException(message, cause)

@Singleton
class MealAnalysisService internal constructor(
    private val api: GeminiApi,
    private val isAiReady: suspend () -> Boolean,
    private val apiKeyProvider: suspend () -> String?,
) {
    internal constructor(
        api: GeminiApi,
        apiKeyProvider: suspend () -> String?,
    ) : this(
        api = api,
        isAiReady = { apiKeyProvider() != null },
        apiKeyProvider = apiKeyProvider,
    )

    @Inject
    constructor(
        api: GeminiApi,
        aiUsageGate: AiUsageGate,
    ) : this(
        api = api,
        isAiReady = { aiUsageGate.isAiReady() },
        apiKeyProvider = { aiUsageGate.currentApiKeyOrNull() },
    )

    private val gson = Gson()
    private val captureTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    suspend fun analyzeMealImage(path: String, userContext: String, capturedAtMillis: Long): MealAnalysisResult {
        if (!isAiReady()) return fallbackMealScan()
        val apiKey = apiKeyProvider() ?: return fallbackMealScan()
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
        val response = runCatching {
            api.generateContent(
                model = GEMINI_FLASH_MODEL,
                apiKey = apiKey,
                request = GeminiRequest(
                    contents = listOf(
                        GeminiRequest.Content(
                            parts = listOf(
                                GeminiRequest.Part(text = GeminiPrompts.mealScanner(scanContext)),
                                GeminiRequest.Part(
                                    inlineData = GeminiRequest.InlineData(
                                        mimeType = "image/jpeg",
                                        data = Base64.getEncoder().encodeToString(file.readBytes()),
                                    ),
                                ),
                            ),
                        ),
                    ),
                    generationConfig = GeminiRequest.GenerationConfig(
                        responseMimeType = "application/json",
                        thinkingConfig = GeminiRequest.ThinkingConfig(
                            includeThoughts = false,
                            thinkingBudget = 0,
                        ),
                    ),
                ),
            )
        }.getOrElse { error ->
            throw MealAnalysisUnavailableException(cause = error)
        }
        val text = response.candidates.firstOrNull()?.content?.parts?.joinToString(" ") { it.text }.orEmpty()
        return parseMealScan(text, suggestedMealType)
    }

    private fun parseMealScan(text: String, fallbackMealType: MealType): MealAnalysisResult {
        if (text.isBlank()) throw MealAnalysisUnavailableException()
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
        }.getOrElse { error ->
            throw MealAnalysisUnavailableException(cause = error)
        }
    }

    private fun fallbackMealScan(): MealAnalysisResult = MealAnalysisResult(
        items = emptyList(),
        notes = "AI-maaltijdanalyse is nu niet beschikbaar. Je kunt de maaltijd handmatig toevoegen.",
        source = MealAnalysisSource.LOCAL_FALLBACK,
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
                model = GEMINI_FLASH_MODEL,
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
                        thinkingConfig = GeminiRequest.ThinkingConfig(
                            includeThoughts = false,
                            thinkingBudget = 1000,
                        ),
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
                model = GEMINI_FLASH_MODEL,
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
                    generationConfig = GeminiRequest.GenerationConfig(
                        responseMimeType = "application/json",
                        thinkingConfig = GeminiRequest.ThinkingConfig(
                            includeThoughts = false,
                            thinkingBudget = 1000,
                        ),
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
            goal.contains("bulk", ignoreCase = true) -> "Progressieve overload op compoundoefeningen"
            goal.contains("cut", ignoreCase = true) || goal.contains("fat", ignoreCase = true) -> "Consistentie, stappen en herstel"
            bodyFat > 20 -> "Body recomposition met consistente krachttraining"
            height > 0 && weight / ((height / 100.0) * (height / 100.0)) < 22 -> "Spiermassa opbouwen met stabiel weekvolume"
            else -> "Gebalanceerde krachtopbouw en herstel"
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
            summary = "Lokale berekening: BMR ${baseline.bmr} kcal, onderhoud ${baseline.maintenanceCalories} kcal, doel ${baseline.targetCalories} kcal. Richt je op ${baseline.proteinTarget} g eiwit en train met focus op $trainingFocus.",
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
                model = GEMINI_FLASH_MODEL,
                apiKey = apiKey,
                request = GeminiRequest(
                    contents = listOf(
                        GeminiRequest.Content(
                            parts = listOf(GeminiRequest.Part(text = GeminiPrompts.weeklyReport(volume, weightTrend, adherence))),
                        ),
                    ),
                    generationConfig = GeminiRequest.GenerationConfig(
                        responseMimeType = "application/json",
                        thinkingConfig = GeminiRequest.ThinkingConfig(
                            includeThoughts = false,
                            thinkingBudget = 1000,
                        ),
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
                nextWeekFocus = root.get("nextWeekFocus")?.asString ?: "Bescherm herstel voordat je extra volume toevoegt.",
                thinkingProcess = root.getAsJsonArray("thinkingProcess")?.map { it.asString }.orEmpty(),
                rawResponse = text,
            )
        }.getOrElse { fallbackWeeklyReport(adherence) }

    private fun fallbackWeeklyReport(adherence: Int): WeeklyReportResult =
        WeeklyReportResult(
            summary = "Lokale samenvatting: er is nog te weinig betrouwbare context voor een AI-weekrapport. Consistentie staat nu op $adherence%.",
            wins = listOf("Training en voeding blijven lokaal beschikbaar."),
            risks = listOf("Zonder compleet profiel en recente logs kan TrainIQ geen betrouwbaar weekadvies geven."),
            nextWeekFocus = "Vul je profiel aan en log een paar trainingen of maaltijden voordat je het volume verhoogt.",
        )
}

internal fun parseWorkoutDebriefResponse(
    text: String,
    totalVolume: Double,
    progression: Double,
): WorkoutDebrief = runCatching {
    val root = JsonParser.parseString(text).asJsonObject
    WorkoutDebrief(
        summary = root.get("summary")?.asString ?: "Training opgeslagen.",
        progressionFeedback = root.get("progressionFeedback")?.asString ?: "Progressie bleef stabiel.",
        recommendation = root.get("recommendation")?.asString
            ?: "Herhaal deze sessie en mik op een extra herhaling bij de belangrijkste oefeningen.",
        nextSessionFocus = root.get("nextSessionFocus")?.asString?.trim().orEmpty()
            .ifBlank { "Huidige gewichten vasthouden" },
        recoveryScore = (root.get("recoveryScore")?.asInt ?: 75).coerceIn(0, 100),
        intensitySignal = root.get("intensitySignal")?.asString?.trim()?.uppercase().orEmpty()
            .ifBlank { "MAINTAIN" },
        wins = root.getAsJsonArray("wins")?.map { it.asString }.orEmpty(),
        risks = root.getAsJsonArray("risks")?.map { it.asString }.orEmpty(),
        nextLoadTarget = root.get("nextLoadTarget")?.asString?.trim().orEmpty(),
        recoveryAdvice = root.get("recoveryAdvice")?.asString?.trim().orEmpty(),
    )
}.getOrElse { fallbackWorkoutDebriefResult(totalVolume, progression) }

internal fun fallbackWorkoutDebriefResult(totalVolume: Double, progression: Double) = WorkoutDebrief(
    summary = "Lokale samenvatting: volume ${totalVolume.toInt()} kg.",
    progressionFeedback = "Volume veranderde met ${String.format(Locale.US, "%.1f", progression)}% ten opzichte van de vorige sessie.",
    recommendation = "Houd dezelfde opzet aan en verhoog pas als uitvoering en herstel goed blijven.",
    nextSessionFocus = "Huidige gewichten vasthouden",
    recoveryScore = 75,
    intensitySignal = "MAINTAIN",
    wins = listOf("Trainingsvolume lokaal vastgelegd."),
    risks = if (progression > 5.0) listOf("Volume steeg meer dan 5%; let op herstel.") else emptyList(),
    nextLoadTarget = "Herhaal de huidige werkgewichten en voeg alleen herhalingen toe als RPE onder 8 blijft.",
    recoveryAdvice = "Gebruik slaap, stappen en spierpijn om te bepalen of je verhoogt of vasthoudt.",
)
