package com.trainiq.ai.services

import com.google.gson.JsonParser
import com.trainiq.ai.prompts.AiPrompts
import com.trainiq.domain.model.AiFallbackContext
import com.trainiq.domain.model.RoutineGenerationOptions
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException

data class GeneratedRoutine(
    val routineName: String,
    val routineDescription: String,
    val periodizationNote: String = "",
    val estimatedDurationMinutes: Int = 0,
    val source: GeneratedRoutineSource = GeneratedRoutineSource.GEMINI_2_5_FLASH,
    val fallbackContext: AiFallbackContext? = null,
    val days: List<GeneratedDay>,
)

enum class GeneratedRoutineSource {
    GEMINI_2_5_FLASH,
    OPENAI,
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
    val existingExerciseId: Long? = null,
)

@Singleton
class RoutineGeneratorService internal constructor(
    private val aiJsonGenerator: AiJsonGenerator,
    private val readinessProvider: suspend () -> AiReadiness,
) {
    @Inject
    constructor(
        aiProviderRouter: AiProviderRouter,
        aiUsageGate: AiUsageGate,
    ) : this(
        aiJsonGenerator = aiProviderRouter,
        readinessProvider = aiUsageGate::currentReadiness,
    )

    // Provider routing replaces the old direct Gemini-only boundary while preserving bounded AI retry semantics.
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
        existingExercises: List<String> = emptyList(),
        options: RoutineGenerationOptions = RoutineGenerationOptions(),
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
            priorityMuscleGroups = options.priorityMuscleGroups,
            preferredExercises = options.preferredExercises,
            excludedExercises = options.excludedExercises,
            existingExercises = existingExercises,
        )
        val fallbackValid = fallback.respectsRequest(daysPerWeek, equipment, sessionDurationMinutes, options, existingExercises)
        return try {
            val readiness = readinessProvider()
            if (readiness != AiReadiness.CONFIGURED) {
                require(fallbackValid) { "Geen passende routine voor dit materiaal, deze voorkeuren en de beschikbare tijd." }
                val fallbackContext = readiness.toAiFallbackContext()
                return fallback.copy(
                    routineDescription = listOfNotNull(
                        fallbackContext?.safeUserMessage(),
                        fallback.routineDescription,
                    ).joinToString(" "),
                    fallbackContext = fallbackContext,
                ).withSettingsSummary(daysPerWeek, equipment, targetFocus, options)
            }
            val routed = aiJsonGenerator.generateJson(
                AiRouteRequest(
                    feature = AiFeature.ROUTINE_GENERATION,
                    schemaName = "routine_generator",
                    responseJsonSchema = AiJsonSchemas.routineGenerator,
                    thinkingBudget = 1000,
                    prompt = AiPrompts.routineGenerator(
                        goal = promptGoal,
                        targetFocus = promptFocus,
                        daysPerWeek = daysPerWeek,
                        equipment = promptEquipment,
                        experienceLevel = experienceLevel,
                        sessionDurationMinutes = sessionDurationMinutes,
                        includeDeload = includeDeload,
                        existingExercises = existingExercises,
                        priorityMuscleGroups = options.priorityMuscleGroups,
                        preferredExercises = options.preferredExercises,
                        excludedExercises = options.excludedExercises,
                    ),
                ),
            )
            parseGeneratedRoutine(routed.rawJson, fallback, routed.providerUsed)
                .takeIf { it.source != GeneratedRoutineSource.LOCAL_FALLBACK && it.respectsRequest(daysPerWeek, equipment, sessionDurationMinutes, options, existingExercises) }
                .let { it ?: fallback.takeIf { fallbackValid } ?: error("Geen passende routine voor dit materiaal, deze voorkeuren en de beschikbare tijd.") }
                .also { routine ->
                if (routed.providerUsed == AiProvider.OPENAI && routine.source == GeneratedRoutineSource.LOCAL_FALLBACK) {
                    throw AiProviderRequestException(AiProvider.OPENAI, AiFeature.ROUTINE_GENERATION, AiFailureCategory.INVALID_RESPONSE)
                }
            }.withSettingsSummary(daysPerWeek, equipment, targetFocus, options)
        } catch (throwable: Throwable) {
            if (throwable is CancellationException) throw throwable
            val mapped = throwable.asAiRateLimitExceptionIfNeeded()
            if (mapped is AiRateLimitException || mapped is AiFeatureThrottledException) throw mapped
            if (!throwable.allowsDeterministicAiFallback()) throw throwable
            require(fallbackValid) { "Geen passende routine voor dit materiaal, deze voorkeuren en de beschikbare tijd." }
            fallback.copy(
                routineDescription = "${throwable.toSafeAiFallbackMessage("AI-routinegeneratie is nu niet beschikbaar.")} ${fallback.routineDescription}",
                fallbackContext = throwable.toAiFallbackContext(),
            ).withSettingsSummary(daysPerWeek, equipment, targetFocus, options)
        }
    }
}

private const val MaxGeneratedRoutineRawResponseChars = 64_000
private const val MaxGeneratedRoutineDays = 7
private const val MaxGeneratedExercisesPerDay = 12
private const val MaxGeneratedRoutineNameChars = 120
private const val MaxGeneratedRoutineTextChars = 600
private const val MaxGeneratedRoutineMetaChars = 40

internal fun parseGeneratedRoutine(text: String, fallback: GeneratedRoutine): GeneratedRoutine =
    parseGeneratedRoutine(text, fallback, AiProvider.GEMINI)

internal fun parseGeneratedRoutine(text: String, fallback: GeneratedRoutine, provider: AiProvider): GeneratedRoutine = runCatching {
    if (text.length > MaxGeneratedRoutineRawResponseChars) return fallback
    val root = JsonParser.parseString(text).asJsonObject
    val routineName = root.get("routineName").boundedString(MaxGeneratedRoutineNameChars) ?: return fallback
    val routineDescription = root.get("routineDescription").boundedString(MaxGeneratedRoutineTextChars).orEmpty()
    val periodizationNote = root.get("periodizationNote").boundedString(MaxGeneratedRoutineTextChars).orEmpty()
    val days = root.getAsJsonArray("days")?.take(MaxGeneratedRoutineDays)?.map { dayElement ->
        val dayObj = dayElement.asJsonObject
        val dayName = dayObj.get("dayName").boundedString(MaxGeneratedRoutineNameChars) ?: "Dag"
        val exercises = dayObj.getAsJsonArray("exercises")?.take(MaxGeneratedExercisesPerDay)?.map { exElement ->
            val exObj = exElement.asJsonObject
            GeneratedExercise(
                exerciseName = exObj.get("exerciseName").boundedString(MaxGeneratedRoutineNameChars) ?: "Oefening",
                muscleGroup = exObj.get("muscleGroup").boundedString(MaxGeneratedRoutineNameChars) ?: "Algemeen",
                equipment = exObj.get("equipment").boundedString(MaxGeneratedRoutineNameChars) ?: "Lichaamsgewicht",
                targetSets = (exObj.get("targetSets")?.asInt ?: 3).coerceIn(1, 10),
                repRange = exObj.get("repRange").boundedString(MaxGeneratedRoutineMetaChars) ?: "8-12",
                restSeconds = (exObj.get("restSeconds")?.asInt ?: 90).coerceIn(30, 300),
                coachingCue = exObj.get("coachingCue").boundedString(MaxGeneratedRoutineTextChars).orEmpty(),
                existingExerciseId = exObj.get("existingExerciseId")?.takeIf { !it.isJsonNull }?.asLong,
            )
        }.orEmpty()
        GeneratedDay(
            dayName = dayName,
            estimatedDurationMinutes = estimatedRoutineDurationMinutes(exercises),
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
        estimatedDurationMinutes = days.maxOf { it.estimatedDurationMinutes },
        source = when (provider) {
            AiProvider.GEMINI -> GeneratedRoutineSource.GEMINI_2_5_FLASH
            AiProvider.OPENAI -> GeneratedRoutineSource.OPENAI
        },
        days = days,
    )
}.getOrElse { fallback }

private fun com.google.gson.JsonElement?.boundedString(maxChars: Int): String? =
    this
        ?.takeIf { !it.isJsonNull }
        ?.let { element -> runCatching { element.asString.trim().take(maxChars) }.getOrNull() }
        ?.takeIf { it.isNotBlank() }

internal fun fallbackGeneratedRoutine(
    goal: String,
    targetFocus: String,
    daysPerWeek: Int,
    equipment: String,
    experienceLevel: String,
    sessionDurationMinutes: Int,
    includeDeload: Boolean,
    fallbackContext: AiFallbackContext? = null,
    priorityMuscleGroups: List<String> = emptyList(),
    preferredExercises: List<String> = emptyList(),
    excludedExercises: List<String> = emptyList(),
    existingExercises: List<String> = emptyList(),
): GeneratedRoutine {
    val safeDays = daysPerWeek.coerceIn(1, 7)
    val duration = sessionDurationMinutes.coerceIn(30, 90)
    val normalizedLevel = experienceLevel.trim().lowercase().ifBlank { "intermediate" }
    val displayFocus = targetFocus.toDutchRoutineFocus().ifBlank { "kracht" }
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
    val available = equipment.split(',', ';').map { it.toDutchEquipment().trim() }.filter { it.isNotBlank() }.toSet()
        .ifEmpty { setOf("Lichaamsgewicht") }
    val excluded = excludedExercises.map { it.routineKey() }.toSet()
    val preferred = preferredExercises.map { it.routineKey() }.toSet()
    val priority = priorityMuscleGroups.map { it.routineKey() }.toSet()
    val libraryCatalog = existingExercises.mapNotNull { descriptor ->
        val parts = descriptor.substringAfter(':', "").split('|')
        if (descriptor.substringBefore(':').trim().toLongOrNull() == null || parts.size < 3) return@mapNotNull null
        val name = parts[0].trim()
        val muscle = parts[1].trim().toRoutineMuscleGroup()
        val requiredEquipment = parts[2].trim().toDutchEquipment()
        if (name.isBlank() || muscle.isBlank() || requiredEquipment.isBlank()) null
        else Triple(name, muscle, requiredEquipment)
    }
    val templates = (libraryCatalog + fallbackExerciseCatalog)
        .distinctBy { it.first.routineKey() to it.third.routineKey() }
        .filter { (name, _, requiredEquipment) ->
            (requiredEquipment == "Lichaamsgewicht" || "Gemengd" in available || requiredEquipment in available) &&
                name.routineKey() !in excluded
        }
        .sortedWith(compareByDescending<Triple<String, String, String>> { it.first.routineKey() in preferred }
            .thenByDescending { it.second.routineKey() in priority }
            .thenBy { it.third == "Lichaamsgewicht" })
        .map { (name, muscle, requiredEquipment) ->
            GeneratedExercise(name, muscle, requiredEquipment, 3, repRange, restSeconds)
        }
    val days = List(safeDays) { index ->
        val dayFocus = when {
            displayFocus.contains("onderlichaam") -> "lower"
            displayFocus.contains("bovenlichaam") -> "upper"
            displayFocus.contains("push/pull/legs") -> listOf("push", "pull", "lower")[index % 3]
            displayFocus.contains("upper/lower") -> if (index % 2 == 0) "upper" else "lower"
            normalizedLevel == "beginner" || safeDays <= 3 -> "full"
            else -> if (index % 2 == 0) "upper" else "lower"
        }
        val focused = templates.filter { it.muscleGroup.matchesRoutineDayFocus(dayFocus) }
        val rotation = focused.drop(index % focused.size.coerceAtLeast(1)) + focused.take(index % focused.size.coerceAtLeast(1))
        val selected = rotation.take(exerciseCount).takeWhileWithinRoutineDuration(duration)
        GeneratedDay(
            dayName = when {
                dayFocus == "full" -> "Volledig lichaam ${index + 1}"
                dayFocus == "lower" -> "Onderlichaam ${index + 1}"
                dayFocus == "upper" -> "Bovenlichaam ${index + 1}"
                dayFocus == "push" -> "Duwen ${index + 1}"
                else -> "Trekken ${index + 1}"
            },
            estimatedDurationMinutes = estimatedRoutineDurationMinutes(selected),
            exercises = selected,
        )
    }
    val actualDuration = days.maxOfOrNull { it.estimatedDurationMinutes } ?: 0
    return GeneratedRoutine(
        routineName = "${normalizedLevel.dutchExperienceLabel()} $displayFocus routine",
        routineDescription = "Lokale routine voor ${goal.toDutchGoalSummary()} met $safeDays sessies per week van ongeveer $actualDuration minuten.",
        periodizationNote = periodizationNote,
        estimatedDurationMinutes = actualDuration,
        source = GeneratedRoutineSource.LOCAL_FALLBACK,
        fallbackContext = fallbackContext,
        days = days,
    )
}

private fun GeneratedRoutine.withSettingsSummary(
    daysPerWeek: Int,
    equipment: String,
    targetFocus: String,
    options: RoutineGenerationOptions,
): GeneratedRoutine {
    val settings = buildList {
        add("$daysPerWeek dagen")
        add("materiaal: ${equipment.ifBlank { "lichaamsgewicht" }}")
        add("split: ${targetFocus.ifBlank { "automatisch" }}")
        if (options.priorityMuscleGroups.isNotEmpty()) add("prioriteit: ${options.priorityMuscleGroups.joinToString()}")
        if (options.preferredExercises.isNotEmpty()) add("gewenst: ${options.preferredExercises.joinToString()}")
        if (options.excludedExercises.isNotEmpty()) add("uitgesloten: ${options.excludedExercises.joinToString()}")
    }.joinToString("; ")
    return copy(routineDescription = "$routineDescription Instellingen: $settings.")
}

internal fun GeneratedRoutine.respectsRequest(
    daysPerWeek: Int,
    equipment: String,
    durationMinutes: Int,
    options: RoutineGenerationOptions,
    existingExercises: List<String>,
): Boolean {
    if (days.size != daysPerWeek || days.any { it.exercises.isEmpty() }) return false
    val available = equipment.split(',', ';').map { it.toDutchEquipment().routineKey() }.filter { it.isNotBlank() }.toSet()
        .ifEmpty { setOf("lichaamsgewicht") }
    val knownExercises = existingExercises.mapNotNull { descriptor ->
        val id = descriptor.substringBefore(':').trim().toLongOrNull() ?: return@mapNotNull null
        val details = descriptor.substringAfter(':', "").split('|')
        if (details.size < 3) return@mapNotNull null
        id to (details[0].routineKey() to details[2].toDutchEquipment().routineKey())
    }.toMap()
    val excluded = options.excludedExercises.map { it.routineKey() }.toSet()
    val generatedExercises = days.flatMap { it.exercises }
    val generatedNames = generatedExercises.map { it.exerciseName.routineKey() }.toSet()
    val generatedMuscles = generatedExercises.map { it.muscleGroup.toRoutineMuscleGroup().routineKey() }.toSet()
    if (options.preferredExercises.any { it.routineKey() !in generatedNames }) return false
    if (options.priorityMuscleGroups.any { it.toRoutineMuscleGroup().routineKey() !in generatedMuscles }) return false
    return days.all { day ->
        estimatedRoutineDurationMinutes(day.exercises) <= durationMinutes + 5 &&
            day.exercises.map { it.exerciseName.routineKey() }.distinct().size == day.exercises.size &&
            day.exercises.all { exercise ->
                val required = exercise.equipment.toDutchEquipment().routineKey()
                (required == "lichaamsgewicht" || "gemengd" in available || required in available) &&
                    exercise.exerciseName.routineKey() !in excluded &&
                    (exercise.existingExerciseId == null || knownExercises[exercise.existingExerciseId] ==
                        (exercise.exerciseName.routineKey() to required))
            }
    }
}

private val fallbackExerciseCatalog = listOf(
    Triple("Kniebuiging", "Benen", "Halterstang"),
    Triple("Bankdrukken", "Borst", "Halterstang"),
    Triple("Roemeense deadlift", "Hamstrings", "Halterstang"),
    Triple("Barbell row", "Rug", "Halterstang"),
    Triple("Schouderdrukken", "Schouders", "Halterstang"),
    Triple("Goblet squat", "Benen", "Dumbbells"),
    Triple("Dumbbell bankdrukken", "Borst", "Dumbbells"),
    Triple("Dumbbell row", "Rug", "Dumbbells"),
    Triple("Dumbbell split squat", "Benen", "Dumbbells"),
    Triple("Dumbbell deadlift", "Hamstrings", "Dumbbells"),
    Triple("Lat pulldown", "Rug", "Kabel"),
    Triple("Kabelroeien", "Rug", "Kabel"),
    Triple("Cable chest press", "Borst", "Kabel"),
    Triple("Air squat", "Benen", "Lichaamsgewicht"),
    Triple("Uitvalspas", "Benen", "Lichaamsgewicht"),
    Triple("Glute bridge", "Bilspieren", "Lichaamsgewicht"),
    Triple("Good morning", "Hamstrings", "Lichaamsgewicht"),
    Triple("Push-up", "Borst", "Lichaamsgewicht"),
    Triple("Pike push-up", "Schouders", "Lichaamsgewicht"),
    Triple("Superman", "Rug", "Lichaamsgewicht"),
    Triple("Plank", "Core", "Lichaamsgewicht"),
)

private fun String.routineKey(): String = lowercase(Locale.ROOT).trim().replace(Regex("""\s+"""), " ")

private fun String.toRoutineMuscleGroup(): String = when (routineKey()) {
    "chest" -> "Borst"
    "back" -> "Rug"
    "legs", "quadriceps" -> "Benen"
    "shoulders" -> "Schouders"
    "glutes" -> "Bilspieren"
    "arms" -> "Armen"
    else -> trim()
}

private fun String.matchesRoutineDayFocus(focus: String): Boolean = when (focus) {
    "upper" -> this in setOf("Borst", "Rug", "Schouders", "Armen", "Core")
    "lower" -> this in setOf("Benen", "Hamstrings", "Bilspieren")
    "push" -> this in setOf("Borst", "Schouders", "Benen")
    "pull" -> this in setOf("Rug", "Hamstrings", "Bilspieren")
    else -> true
}

internal fun estimatedRoutineDurationMinutes(exercises: List<GeneratedExercise>): Int =
    kotlin.math.ceil(exercises.sumOf { it.targetSets * (it.restSeconds + 45) + 60 }.toDouble() / 60.0).toInt()

private fun List<GeneratedExercise>.takeWhileWithinRoutineDuration(limitMinutes: Int): List<GeneratedExercise> {
    val selected = mutableListOf<GeneratedExercise>()
    for (exercise in this) {
        if (estimatedRoutineDurationMinutes(selected + exercise) > limitMinutes) break
        selected += exercise
    }
    return selected
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

private fun String.toDutchEquipment(): String {
    val normalized = trim()
    return when (normalized.lowercase(Locale.ROOT)) {
        "barbell" -> "Halterstang"
        "dumbbell", "dumbbells" -> "Dumbbells"
        "bodyweight", "body weight" -> "Lichaamsgewicht"
        "cable" -> "Kabel"
        "mixed" -> "Gemengd"
        else -> normalized
    }
}
