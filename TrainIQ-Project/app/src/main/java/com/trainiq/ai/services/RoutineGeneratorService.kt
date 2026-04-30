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
        val fallback = fallbackGeneratedRoutine(
            goal = goal,
            targetFocus = targetFocus,
            daysPerWeek = daysPerWeek,
            equipment = equipment,
            experienceLevel = experienceLevel,
            sessionDurationMinutes = sessionDurationMinutes,
            includeDeload = includeDeload,
        )
        return try {
            if (!aiUsageGate.isAiReady()) return fallback
            val apiKey = aiUsageGate.currentApiKeyOrNull() ?: return fallback
            val response = api.generateContent(
                model = GEMINI_FLASH_MODEL,
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
            parseGeneratedRoutine(text, fallback)
        } catch (throwable: Throwable) {
            val mapped = throwable.asAiRateLimitExceptionIfNeeded()
            if (mapped is AiRateLimitException) throw mapped
            fallback
        }
    }
}

internal fun parseGeneratedRoutine(text: String, fallback: GeneratedRoutine): GeneratedRoutine = runCatching {
    val root = JsonParser.parseString(text).asJsonObject
    val routineName = root.get("routineName")?.asString?.takeIf { it.isNotBlank() } ?: return fallback
    val routineDescription = root.get("routineDescription")?.asString.orEmpty()
    val periodizationNote = root.get("periodizationNote")?.asString.orEmpty()
    val estimatedDurationMinutes = root.get("estimatedDurationMinutes")?.asInt ?: fallback.estimatedDurationMinutes
    val days = root.getAsJsonArray("days")?.map { dayElement ->
        val dayObj = dayElement.asJsonObject
        val dayName = dayObj.get("dayName")?.asString ?: "Day"
        val dayDurationMinutes = dayObj.get("estimatedDurationMinutes")?.asInt ?: estimatedDurationMinutes
        val exercises = dayObj.getAsJsonArray("exercises")?.map { exElement ->
            val exObj = exElement.asJsonObject
            GeneratedExercise(
                exerciseName = exObj.get("exerciseName")?.asString ?: "Exercise",
                muscleGroup = exObj.get("muscleGroup")?.asString ?: "General",
                equipment = exObj.get("equipment")?.asString ?: "Bodyweight",
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
                "Intermediate blok: upper/lower progressie met wekelijkse overload en deload-richtlijn."
            } else {
                "Intermediate blok: verhoog hoofdlifts ongeveer 2,5 kg per week als herstel goed blijft."
            },
        )
    }
    val templates = listOf(
        GeneratedExercise("Squat", "Benen", equipment.ifBlank { "Halterstang" }, 3, repRange, restSeconds, "Span je romp aan en duw gelijkmatig door de middenvoet."),
        GeneratedExercise("Bench Press", "Borst", equipment.ifBlank { "Halterstang" }, 3, repRange, restSeconds, "Laat gecontroleerd zakken en houd je pols recht onder de stang."),
        GeneratedExercise("Romanian Deadlift", "Hamstrings", equipment.ifBlank { "Halterstang" }, 3, "8-10", 90, "Duw je heupen naar achteren en houd de stang dicht bij je lichaam."),
        GeneratedExercise("Lat Pulldown", "Rug", equipment.ifBlank { "Kabel" }, 3, "8-12", 75, "Trek met je ellebogen en pauzeer kort onderin."),
        GeneratedExercise("Overhead Press", "Schouders", equipment.ifBlank { "Halterstang" }, 3, repRange, restSeconds, "Span bilspieren aan en houd je ribben laag."),
        GeneratedExercise("Split Squat", "Benen", equipment.ifBlank { "Dumbbell" }, 3, "8-12", 75, "Blijf rechtop en belast vooral het voorste been."),
        GeneratedExercise("Cable Row", "Rug", equipment.ifBlank { "Kabel" }, 3, "10-12", 75, "Houd schouders laag en eindig met spanning in je bovenrug."),
        GeneratedExercise("Plank", "Core", "Lichaamsgewicht", 3, "30-45s", 45, "Span je buik aan en houd een rechte lijn van hak tot hoofd."),
    )
    val days = List(safeDays) { index ->
        val rotation = templates.drop(index % templates.size) + templates.take(index % templates.size)
        GeneratedDay(
            dayName = when {
                normalizedLevel == "beginner" -> "Full body ${index + 1}"
                index % 2 == 0 -> "Upper ${index / 2 + 1}"
                else -> "Lower ${index / 2 + 1}"
            },
            estimatedDurationMinutes = duration,
            exercises = rotation.take(exerciseCount),
        )
    }
    return GeneratedRoutine(
        routineName = "${normalizedLevel.dutchExperienceLabel()} ${targetFocus.ifBlank { "kracht" }} routine",
        routineDescription = "Lokale routine voor $goal met $safeDays sessies per week van ongeveer $duration minuten.",
        periodizationNote = periodizationNote,
        estimatedDurationMinutes = duration,
        source = GeneratedRoutineSource.LOCAL_FALLBACK,
        days = days,
    )
}

private fun String.dutchExperienceLabel(): String = when (this) {
    "beginner" -> "Beginner"
    "advanced" -> "Gevorderde"
    else -> "Intermediate"
}
