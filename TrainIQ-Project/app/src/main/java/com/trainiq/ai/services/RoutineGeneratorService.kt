package com.trainiq.ai.services

import android.util.Log
import com.google.gson.JsonParser
import com.trainiq.ai.prompts.GeminiPrompts
import com.trainiq.data.model.GeminiRequest
import com.trainiq.data.remote.GeminiApi
import javax.inject.Inject
import javax.inject.Singleton
import retrofit2.HttpException

data class GeneratedRoutine(
    val routineName: String,
    val routineDescription: String,
    val periodizationNote: String = "",
    val estimatedDurationMinutes: Int = 0,
    val source: GeneratedRoutineSource = GeneratedRoutineSource.GEMINI_2_5_FLASH,
    val days: List<GeneratedDay>,
)

enum class GeneratedRoutineSource {
    GEMINI_2_5_FLASH,
    LOCAL_FALLBACK,
}

data class GeneratedDay(
    val dayName: String,
    val estimatedDurationMinutes: Int = 60,
    val exercises: List<GeneratedExercise>,
)

data class GeneratedExercise(
    val exerciseName: String,
    val muscleGroup: String,
    val equipment: String,
    val targetSets: Int,
    val repRange: String,
    val restSeconds: Int,
    val coachingCue: String = "",
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
    ): GeneratedRoutine? = generateRoutine(
        goal = goal,
        targetFocus = targetFocus,
        daysPerWeek = daysPerWeek,
        equipment = equipment,
        experienceLevel = "intermediate",
        sessionDurationMinutes = 60,
        includeDeload = false,
    )

    suspend fun generateRoutine(
        goal: String,
        targetFocus: String,
        daysPerWeek: Int,
        equipment: String,
        experienceLevel: String,
        sessionDurationMinutes: Int,
        includeDeload: Boolean,
    ): GeneratedRoutine {
        val promptGoal = goal.toDutchGoalSummary()
        val promptFocus = targetFocus.toDutchRoutineFocus().ifBlank { "kracht" }
        val promptEquipment = equipment.toDutchEquipment()
        val fallback = fallbackGeneratedRoutine(
            goal = promptGoal,
            targetFocus = promptFocus,
            daysPerWeek = daysPerWeek,
            equipment = promptEquipment,
            experienceLevel = experienceLevel,
            sessionDurationMinutes = sessionDurationMinutes,
            includeDeload = includeDeload,
        )
        return try {
            if (!aiUsageGate.isAiReady()) {
                Log.d(RoutineGeneratorLogTag, "Routine AI fallback: AI staat uit of configuratie ontbreekt.")
                return fallback
            }
            val apiKey = aiUsageGate.currentApiKeyOrNull() ?: run {
                Log.d(RoutineGeneratorLogTag, "Routine AI fallback: Gemini API-key ontbreekt.")
                return fallback
            }
            val response = api.generateContent(
                model = GEMINI_FLASH_MODEL,
                apiKey = apiKey,
                request = GeminiRequest(
                    contents = listOf(
                        GeminiRequest.Content(
                            parts = listOf(
                                GeminiRequest.Part(
                                    text = GeminiPrompts.routineGenerator(
                                        goal = promptGoal,
                                        targetFocus = promptFocus,
                                        daysPerWeek = daysPerWeek,
                                        equipment = promptEquipment,
                                        experienceLevel = experienceLevel,
                                        sessionDurationMinutes = sessionDurationMinutes,
                                        includeDeload = includeDeload,
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
            parseGeneratedRoutine(text, fallback).also { routine ->
                if (routine.source == GeneratedRoutineSource.LOCAL_FALLBACK) {
                    Log.d(RoutineGeneratorLogTag, "Routine AI fallback: Gemini-antwoord was leeg, ongeldig of niet Nederlands genoeg.")
                }
            }
        } catch (throwable: Throwable) {
            val mapped = throwable.asAiRateLimitExceptionIfNeeded()
            if (mapped is AiRateLimitException) throw mapped
            val detail = if (throwable is HttpException) "HTTP ${throwable.code()}" else throwable::class.simpleName.orEmpty()
            Log.d(RoutineGeneratorLogTag, "Routine AI fallback: Gemini-aanroep mislukt ($detail).")
            fallback
        }
    }
}

private const val RoutineGeneratorLogTag = "RoutineGenerator"

internal fun parseGeneratedRoutine(text: String, fallback: GeneratedRoutine): GeneratedRoutine = runCatching {
    val root = JsonParser.parseString(text).asJsonObject
    val routineName = root.get("routineName")?.asString?.takeIf { it.isNotBlank() } ?: return fallback
    val routineDescription = root.get("routineDescription")?.asString.orEmpty()
    val periodizationNote = root.get("periodizationNote")?.asString.orEmpty()
    val estimatedDurationMinutes = root.get("estimatedDurationMinutes")?.asInt ?: fallback.estimatedDurationMinutes
    val days = root.getAsJsonArray("days")?.map { dayElement ->
        val dayObj = dayElement.asJsonObject
        val dayName = dayObj.get("dayName")?.asString ?: "Dag"
        val dayDurationMinutes = dayObj.get("estimatedDurationMinutes")?.asInt ?: estimatedDurationMinutes
        val exercises = dayObj.getAsJsonArray("exercises")?.map { exElement ->
            val exObj = exElement.asJsonObject
            GeneratedExercise(
                exerciseName = exObj.get("exerciseName")?.asString ?: "Oefening",
                muscleGroup = exObj.get("muscleGroup")?.asString ?: "Algemeen",
                equipment = exObj.get("equipment")?.asString ?: "Lichaamsgewicht",
                targetSets = exObj.get("targetSets")?.asInt ?: 3,
                repRange = exObj.get("repRange")?.asString ?: "8-12",
                restSeconds = exObj.get("restSeconds")?.asInt ?: 90,
                coachingCue = exObj.get("coachingCue")?.asString.orEmpty(),
            )
        }.orEmpty()
        GeneratedDay(
            dayName = dayName,
            estimatedDurationMinutes = dayDurationMinutes,
            exercises = exercises,
        )
    }.orEmpty()
    if (days.isEmpty() || days.any { it.exercises.isEmpty() }) return fallback
    val textFields = listOf(routineName, routineDescription, periodizationNote) +
        days.flatMap { day ->
            listOf(day.dayName) + day.exercises.flatMap { exercise ->
                listOf(exercise.muscleGroup, exercise.equipment, exercise.coachingCue)
            }
        }
    if (!textFields.isUsableDutchAiText()) return fallback
    GeneratedRoutine(
        routineName = routineName,
        routineDescription = routineDescription,
        periodizationNote = periodizationNote,
        estimatedDurationMinutes = estimatedDurationMinutes,
        source = GeneratedRoutineSource.GEMINI_2_5_FLASH,
        days = days,
    )
}.getOrElse { fallback }

internal fun fallbackGeneratedRoutine(
    goal: String,
    targetFocus: String,
    daysPerWeek: Int,
    equipment: String,
    experienceLevel: String,
    sessionDurationMinutes: Int,
    includeDeload: Boolean,
): GeneratedRoutine {
    val safeDays = daysPerWeek.coerceIn(1, 7)
    val duration = sessionDurationMinutes.coerceIn(30, 90)
    val normalizedLevel = experienceLevel.trim().lowercase().ifBlank { "intermediate" }
    val displayFocus = targetFocus.toDutchRoutineFocus().ifBlank { "kracht" }
    val displayEquipment = equipment.toDutchEquipment().ifBlank { "Halterstang" }
    val exerciseCount = when {
        duration <= 30 -> 4
        duration <= 45 -> 5
        duration <= 60 -> 6
        else -> 7
    }
    val (repRange, restSeconds, periodizationNote) = when (normalizedLevel) {
        "beginner" -> Triple(
            "8-12",
            75,
            "Beginnerblok: lineaire progressie met full-body focus en kleine, beheerste gewichtsstappen.",
        )
        "advanced" -> Triple(
            "4-6",
            150,
            if (includeDeload) {
                "Gevorderd blok: golvende belasting op RPE 7-9 met elke vierde week een deload."
            } else {
                "Gevorderd blok: golvende belasting op RPE 7-9 met volume dat vermoeidheid bewaakt."
            },
        )
        else -> Triple(
            "6-10",
            105,
            if (includeDeload) {
                "Gemiddeld blok: boven/onder-progressie met wekelijkse overbelasting en deload-richtlijn."
            } else {
                "Gemiddeld blok: verhoog hoofdlifts ongeveer 2,5 kg per week als herstel goed blijft."
            },
        )
    }
    val templates = listOf(
        GeneratedExercise("Kniebuiging", "Benen", displayEquipment, 3, repRange, restSeconds, "Span je romp aan en duw gelijkmatig door de middenvoet."),
        GeneratedExercise("Bankdrukken", "Borst", displayEquipment, 3, repRange, restSeconds, "Laat gecontroleerd zakken en houd je pols recht onder de stang."),
        GeneratedExercise("Roemeense deadlift", "Hamstrings", displayEquipment, 3, "8-10", 90, "Duw je heupen naar achteren en houd de stang dicht bij je lichaam."),
        GeneratedExercise("Lat pulldown", "Rug", equipment.toDutchEquipment().ifBlank { "Kabel" }, 3, "8-12", 75, "Trek met je ellebogen en pauzeer kort onderin."),
        GeneratedExercise("Schouderdrukken", "Schouders", displayEquipment, 3, repRange, restSeconds, "Span bilspieren aan en houd je ribben laag."),
        GeneratedExercise("Split squat", "Benen", equipment.toDutchEquipment().ifBlank { "Dumbbell" }, 3, "8-12", 75, "Blijf rechtop en belast vooral het voorste been."),
        GeneratedExercise("Kabelroeien", "Rug", equipment.toDutchEquipment().ifBlank { "Kabel" }, 3, "10-12", 75, "Houd schouders laag en eindig met spanning in je bovenrug."),
        GeneratedExercise("Plank", "Core", "Lichaamsgewicht", 3, "30-45s", 45, "Span je buik aan en houd een rechte lijn van hak tot hoofd."),
    )
    val days = List(safeDays) { index ->
        val rotation = templates.drop(index % templates.size) + templates.take(index % templates.size)
        GeneratedDay(
            dayName = when {
                normalizedLevel == "beginner" -> "Volledig lichaam ${index + 1}"
                index % 2 == 0 -> "Bovenlichaam ${index / 2 + 1}"
                else -> "Onderlichaam ${index / 2 + 1}"
            },
            estimatedDurationMinutes = duration,
            exercises = rotation.take(exerciseCount),
        )
    }
    return GeneratedRoutine(
        routineName = "${normalizedLevel.dutchExperienceLabel()} $displayFocus routine",
        routineDescription = "Lokale routine voor ${goal.toDutchGoalSummary()} met $safeDays sessies per week van ongeveer $duration minuten.",
        periodizationNote = periodizationNote,
        estimatedDurationMinutes = duration,
        source = GeneratedRoutineSource.LOCAL_FALLBACK,
        days = days,
    )
}

private fun String.dutchExperienceLabel(): String = when (this) {
    "beginner" -> "Beginner"
    "advanced" -> "Gevorderde"
    else -> "Gemiddelde"
}

private fun String.toDutchRoutineFocus(): String {
    val translated = trim()
        .replace("Full body", "volledig lichaam", ignoreCase = true)
        .replace("Lower body", "onderlichaam", ignoreCase = true)
        .replace("Upper body", "bovenlichaam", ignoreCase = true)
        .replace("Strength", "kracht", ignoreCase = true)
        .replace("Muscle", "spiermassa", ignoreCase = true)
        .replace("Push/Pull", "push/pull", ignoreCase = true)
        .replace("Upper/Lower", "upper/lower", ignoreCase = true)
        .lowercase()
    val stillEnglish = listOf(
        " training ",
        " progressive ",
        " complemented ",
        " cardiovascular ",
        " exercise",
        " with ",
    ).any { signal -> " $translated ".contains(signal) }
    return if (stillEnglish) "kracht" else translated
}

private fun String.toDutchGoalSummary(): String {
    val value = trim()
    if (value.isBlank()) return "je doel"
    val lowered = value.lowercase()
    val stillEnglish = listOf(" with ", " progressive ", " complemented ", " cardiovascular ", " exercise").any { lowered.contains(it) }
    return if (stillEnglish) "je doel" else value
}

private fun String.toDutchEquipment(): String = trim()
    .replace("Barbell", "Halterstang", ignoreCase = true)
    .replace("Dumbbells", "Dumbbells", ignoreCase = true)
    .replace("Dumbbell", "Dumbbell", ignoreCase = true)
    .replace("Bodyweight", "Lichaamsgewicht", ignoreCase = true)
    .replace("Cable", "Kabel", ignoreCase = true)
    .replace("Mixed", "Gemengd", ignoreCase = true)
