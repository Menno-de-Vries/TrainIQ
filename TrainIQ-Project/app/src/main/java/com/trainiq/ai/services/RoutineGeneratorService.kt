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
    val days: List<GeneratedDay>,
)

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
    GeneratedRoutine(
        routineName = routineName,
        routineDescription = routineDescription,
        periodizationNote = periodizationNote,
        estimatedDurationMinutes = estimatedDurationMinutes,
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
            "Beginner block: linear progression with full-body bias and conservative load jumps.",
        )
        "advanced" -> Triple(
            "4-6",
            150,
            if (includeDeload) {
                "Advanced block: undulating loading at RPE 7-9 with a deload every 4th week."
            } else {
                "Advanced block: undulating loading at RPE 7-9 with fatigue-managed volume."
            },
        )
        else -> Triple(
            "6-10",
            105,
            if (includeDeload) {
                "Intermediate block: upper/lower style progression with weekly overload and deload guidance."
            } else {
                "Intermediate block: weekly overload of roughly +2.5 kg on main lifts when recovery is good."
            },
        )
    }
    val templates = listOf(
        GeneratedExercise("Squat", "Legs", equipment.ifBlank { "Barbell" }, 3, repRange, restSeconds, "Brace and drive evenly through mid-foot."),
        GeneratedExercise("Bench Press", "Chest", equipment.ifBlank { "Barbell" }, 3, repRange, restSeconds, "Lower under control and press with a stacked wrist."),
        GeneratedExercise("Romanian Deadlift", "Hamstrings", equipment.ifBlank { "Barbell" }, 3, "8-10", 90, "Push hips back and keep the bar close."),
        GeneratedExercise("Lat Pulldown", "Back", equipment.ifBlank { "Cable" }, 3, "8-12", 75, "Lead with elbows and pause at the chest."),
        GeneratedExercise("Overhead Press", "Shoulders", equipment.ifBlank { "Barbell" }, 3, repRange, restSeconds, "Squeeze glutes and keep ribs down."),
        GeneratedExercise("Split Squat", "Legs", equipment.ifBlank { "Dumbbell" }, 3, "8-12", 75, "Stay tall and load the front leg."),
        GeneratedExercise("Cable Row", "Back", equipment.ifBlank { "Cable" }, 3, "10-12", 75, "Keep shoulders down and finish with upper-back tension."),
        GeneratedExercise("Plank", "Core", "Bodyweight", 3, "30-45s", 45, "Brace abs and keep a straight line from heel to head."),
    )
    val days = List(safeDays) { index ->
        val rotation = templates.drop(index % templates.size) + templates.take(index % templates.size)
        GeneratedDay(
            dayName = when {
                normalizedLevel == "beginner" -> "Full Body ${index + 1}"
                index % 2 == 0 -> "Upper ${index / 2 + 1}"
                else -> "Lower ${index / 2 + 1}"
            },
            estimatedDurationMinutes = duration,
            exercises = rotation.take(exerciseCount),
        )
    }
    return GeneratedRoutine(
        routineName = "${normalizedLevel.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }} ${targetFocus.ifBlank { "Strength" }} Routine",
        routineDescription = "Fallback routine for $goal with $safeDays sessions per week and about $duration minutes per session.",
        periodizationNote = periodizationNote,
        estimatedDurationMinutes = duration,
        days = days,
    )
}
