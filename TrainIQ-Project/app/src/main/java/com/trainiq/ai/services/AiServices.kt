package com.trainiq.ai.services

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.trainiq.ai.prompts.AiPrompts
import com.trainiq.data.remote.GeminiApi
import com.trainiq.domain.model.BiologicalSex
import com.trainiq.domain.model.BodyMeasurementPhotoResult
import com.trainiq.domain.model.BodyMeasurementPhotoSource
import com.trainiq.domain.model.GoalAdvice
import com.trainiq.domain.model.GoalAdviceSource
import com.trainiq.domain.model.MealAnalysisResult
import com.trainiq.domain.model.MealAnalysisSource
import com.trainiq.domain.model.MealScanItem
import com.trainiq.domain.model.MealType
import com.trainiq.domain.model.NutritionFacts
import com.trainiq.domain.model.WeeklyReportResult
import com.trainiq.domain.model.WeeklyReportSource
import com.trainiq.domain.model.WorkoutDebrief
import com.trainiq.domain.model.WorkoutDebriefSource
import java.io.File
import java.io.ByteArrayOutputStream
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import com.trainiq.domain.model.buildGoalBaseline
import com.trainiq.domain.model.suggestMealType

class MealAnalysisUnavailableException(
    message: String = "Scan mislukt. Probeer opnieuw.",
    cause: Throwable? = null,
) : RuntimeException(message, cause)

@Singleton
class MealAnalysisService internal constructor(
    private val aiJsonGenerator: AiJsonGenerator,
    private val isAiReady: suspend () -> Boolean,
) {
    internal constructor(
        api: GeminiApi,
        apiKeyProvider: suspend () -> String?,
    ) : this(
        aiJsonGenerator = GeminiOnlyJsonGenerator(api, apiKeyProvider),
        isAiReady = { apiKeyProvider() != null },
    )

    internal constructor(
        api: GeminiApi,
        isAiReady: suspend () -> Boolean,
        apiKeyProvider: suspend () -> String?,
    ) : this(
        aiJsonGenerator = GeminiOnlyJsonGenerator(api, apiKeyProvider),
        isAiReady = isAiReady,
    )

    @Inject
    constructor(
        aiProviderRouter: AiProviderRouter,
        aiUsageGate: AiUsageGate,
    ) : this(
        aiJsonGenerator = aiProviderRouter,
        isAiReady = { aiUsageGate.isAiReady() },
    )

    private val gson = Gson()
    private val captureTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    suspend fun analyzeMealImage(path: String, userContext: String, capturedAtMillis: Long): MealAnalysisResult {
        if (!isAiReady()) return fallbackMealScan()
        val file = File(path)
        val suggestedMealType = suggestMealType(capturedAtMillis)
        val captureTime = Instant.ofEpochMilli(capturedAtMillis)
            .atZone(ZoneId.systemDefault())
            .format(captureTimeFormatter)
        val sanitizedUserContext = userContext.trim().take(MaxMealScanContextChars)
        val contextOverrides = parseMealContextOverrides(sanitizedUserContext)
        val scanContext = buildString {
            append("De gebruiker nam deze foto om $captureTime. ")
            append("Voorgesteld maaltijdtype: ${suggestedMealType.promptLabel()}. ")
            append(sanitizedUserContext.ifBlank { "Herken de voeding, schat porties en geef exacte macro's terug." })
        }
        val imageBytes = prepareMealScanImageBytes(file) ?: return fallbackMealScan()
        val routed = runCatching {
            aiJsonGenerator.generateJson(
                AiRouteRequest(
                    feature = AiFeature.MEAL_SCAN,
                    prompt = AiPrompts.mealScanner(scanContext),
                    schemaName = "meal_scan",
                    responseJsonSchema = AiJsonSchemas.mealScan,
                    thinkingBudget = 0,
                    imageJpegBytes = imageBytes,
                ),
            )
        }.getOrElse { error ->
            if (error is CancellationException) throw error
            return fallbackMealScan()
        }
        return parseMealScan(routed.rawJson, suggestedMealType, routed.providerUsed, contextOverrides)
    }

    private fun parseMealScan(
        text: String,
        fallbackMealType: MealType,
        provider: AiProvider,
        contextOverrides: MealContextOverrides = MealContextOverrides(),
    ): MealAnalysisResult {
        if (text.isBlank()) throw MealAnalysisUnavailableException()
        val boundedText = requireAiRawResponseWithinLimit(text)
        return runCatching {
            val root = JsonParser.parseString(boundedText).asJsonObject
            val items = root.getAsJsonArray("items")?.take(MaxMealScanItems)?.mapNotNull { element ->
                val obj = element.asJsonObject
                val name = obj.get("name").boundedString(MaxMealScanNameChars).orEmpty()
                if (name.isBlank()) return@mapNotNull null
                val estimatedGrams = obj.safeNumber("estimatedGrams", default = 100.0, min = 1.0, max = MaxMealScanGrams)
                    ?: return@mapNotNull null
                val calories = obj.safeNumber("calories", default = 0.0, min = 0.0, max = MaxMealScanCalories)
                    ?: return@mapNotNull null
                val protein = obj.safeNumber("protein", default = 0.0, min = 0.0, max = MaxMealScanMacro)
                    ?: return@mapNotNull null
                val carbs = obj.safeNumber("carbs", default = 0.0, min = 0.0, max = MaxMealScanMacro)
                    ?: return@mapNotNull null
                val fat = obj.safeNumber("fat", default = 0.0, min = 0.0, max = MaxMealScanMacro)
                    ?: return@mapNotNull null
                MealScanItem(
                    name = name,
                    estimatedGrams = estimatedGrams,
                    nutrition = NutritionFacts(
                        calories = calories,
                        protein = protein,
                        carbs = carbs,
                        fat = fat,
                    ),
                    confidence = obj.get("confidence").boundedString(MaxMealScanMetaChars),
                    notes = obj.get("notes").boundedString(MaxMealScanNotesChars),
                )
            }.orEmpty()
            val normalizedItems = applyMealContextOverrides(items, contextOverrides)
            val reviewNotes = buildMealScanReviewNotes(
                originalItems = items,
                normalizedItems = normalizedItems,
                overrides = contextOverrides,
            )
            MealAnalysisResult(
                items = normalizedItems,
                suggestedMealType = root.get("suggestedMealType")
                    ?.asString
                    ?.trim()
                    ?.uppercase()
                    ?.let { raw -> MealType.entries.firstOrNull { it.name == raw } }
                    ?: fallbackMealType,
                notes = listOfNotNull(root.get("notes").boundedString(MaxMealScanNotesChars), reviewNotes)
                    .joinToString(" ")
                    .ifBlank { null },
                rawResponse = boundedText,
                source = provider.toMealAnalysisSource(),
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
class BodyMeasurementPhotoService @Inject constructor(
    private val aiJsonGenerator: AiJsonGenerator,
    private val aiUsageGate: AiUsageGate,
) {
    suspend fun analyzeScaleImage(path: String, userContext: String): BodyMeasurementPhotoResult {
        if (!aiUsageGate.isAiReady()) return fallbackBodyMeasurementPhoto()
        val sanitizedContext = userContext.trim().take(MaxBodyMeasurementContextChars)
        val contextOverrides = parseBodyMeasurementContextOverrides(sanitizedContext)
        val imageBytes = prepareMealScanImageBytes(File(path)) ?: return fallbackBodyMeasurementPhoto(
            notes = "Foto kon niet worden gelezen. Vul de meting handmatig in.",
        )
        return runCatching {
            val routed = aiJsonGenerator.generateJson(
                AiRouteRequest(
                    feature = AiFeature.BODY_MEASUREMENT_PHOTO,
                    prompt = AiPrompts.bodyMeasurementPhoto(sanitizedContext),
                    schemaName = "body_measurement_photo",
                    responseJsonSchema = AiJsonSchemas.bodyMeasurementPhoto,
                    thinkingBudget = 0,
                    imageJpegBytes = imageBytes,
                ),
            )
            parseBodyMeasurementPhoto(routed.rawJson, routed.providerUsed, contextOverrides)
        }.getOrElse { error ->
            if (error is CancellationException) throw error
            fallbackBodyMeasurementPhoto()
        }
    }

    private fun parseBodyMeasurementPhoto(
        text: String,
        provider: AiProvider,
        contextOverrides: BodyMeasurementContextOverrides = BodyMeasurementContextOverrides(),
    ): BodyMeasurementPhotoResult {
        val boundedText = requireAiRawResponseWithinLimit(text)
        val root = JsonParser.parseString(boundedText).asJsonObject
        val weight = contextOverrides.weight
            ?: root.safeNumber("weight", default = 0.0, min = 30.0, max = 300.0)
            ?: 0.0
        val bodyFat = contextOverrides.bodyFat
            ?: root.safeNumber("bodyFat", default = 0.0, min = 0.0, max = 100.0)
            ?: 0.0
        val muscleMass = contextOverrides.muscleMass
            ?: root.safeNumber("muscleMass", default = 0.0, min = 1.0, max = 200.0)
            ?: 0.0
        val source = provider.toBodyMeasurementPhotoSource()
        if (weight <= 0.0) {
            return fallbackBodyMeasurementPhoto(
                notes = root.get("notes")?.asString ?: "AI kon het gewicht niet betrouwbaar uitlezen. Voeg het gewicht als context toe of vul het handmatig in.",
                rawResponse = boundedText,
                source = source,
            )
        }
        return BodyMeasurementPhotoResult(
            weight = weight,
            bodyFat = bodyFat,
            muscleMass = muscleMass,
            confidence = root.get("confidence")?.asString,
            notes = buildBodyMeasurementNotes(root.get("notes")?.asString, contextOverrides),
            rawResponse = boundedText,
            source = source,
        )
    }
}

internal data class MealContextOverrides(
    val totalGrams: Double? = null,
    val itemGramsByName: Map<String, Double> = emptyMap(),
    val itemDisplayNamesByName: Map<String, String> = emptyMap(),
)

private data class MealContextItemOverride(
    val normalized: String,
    val displayName: String,
    val grams: Double,
)

internal data class BodyMeasurementContextOverrides(
    val weight: Double? = null,
    val bodyFat: Double? = null,
    val muscleMass: Double? = null,
)

internal fun parseMealContextOverrides(context: String): MealContextOverrides {
    if (context.isBlank()) return MealContextOverrides()
    val total = Regex("""(?i)\b(?:totaal|total)\s*[:=]?\s*(\d+(?:[,.]\d+)?)\s*(?:g|gram|grams)\b""")
        .find(context)
        ?.groupValues
        ?.getOrNull(1)
        ?.toAiDoubleOrNull()
        ?.takeIf { it in 1.0..MaxMealScanGrams }
    val itemOverrides = Regex("""(?i)\b([a-zA-ZÀ-ÿ][a-zA-ZÀ-ÿ0-9 '\-]{1,40}?)\s*[:=]?\s*(\d+(?:[,.]\d+)?)\s*(?:g|gram|grams|ml|milliliter|milliliters)\b""")
        .findAll(context)
        .mapNotNull { match ->
            val name = match.groupValues.getOrNull(1)?.trim().orEmpty()
            val grams = match.groupValues.getOrNull(2)?.toAiDoubleOrNull()
            val normalized = normalizeAiContextName(name)
            if (
                normalized.isNotBlank() &&
                normalized !in setOf("totaal", "total") &&
                grams != null &&
                grams in 1.0..MaxMealScanGrams
            ) {
                MealContextItemOverride(normalized = normalized, displayName = name, grams = grams)
            } else {
                null
            }
        }
        .toList()
    return MealContextOverrides(
        totalGrams = total,
        itemGramsByName = itemOverrides.associate { it.normalized to it.grams },
        itemDisplayNamesByName = itemOverrides.associate { it.normalized to it.displayName },
    )
}
internal fun parseBodyMeasurementContextOverrides(context: String): BodyMeasurementContextOverrides {
    if (context.isBlank()) return BodyMeasurementContextOverrides()
    fun firstNumber(pattern: String, range: ClosedFloatingPointRange<Double>): Double? =
        Regex(pattern)
            .find(context)
            ?.groupValues
            ?.getOrNull(1)
            ?.toAiDoubleOrNull()
            ?.takeIf { it in range }

    return BodyMeasurementContextOverrides(
        weight = firstNumber("""(?i)\b(?:gewicht|weight)\s*[:=]?\s*(\d+(?:[,.]\d+)?)\s*(?:kg)?\b""", 30.0..300.0)
            ?: firstNumber("""(?i)\b(\d+(?:[,.]\d+)?)\s*kg\b""", 30.0..300.0),
        bodyFat = firstNumber("""(?i)\b(?:vet|vetpercentage|body\s*fat|fat)\s*[:=]?\s*(\d+(?:[,.]\d+)?)\s*%?\b""", 0.0..100.0),
        muscleMass = firstNumber("""(?i)\b(?:spier|spiermassa|muscle|muscle\s*mass)\s*[:=]?\s*(\d+(?:[,.]\d+)?)\s*(?:kg)?\b""", 1.0..200.0),
    )
}

private fun applyMealContextOverrides(
    items: List<MealScanItem>,
    overrides: MealContextOverrides,
): List<MealScanItem> {
    if (items.isEmpty()) return items
    if (overrides.itemGramsByName.size > 1) {
        return applyLockedMealContextItems(items, overrides)
    }
    return items.map { item ->
        val itemOverride = overrides.itemGramsByName.entries.firstOrNull { (name, _) ->
            val normalizedItem = normalizeAiContextName(item.name)
            normalizedItem == name || normalizedItem.contains(name) || name.contains(normalizedItem)
        }?.value
        val targetGrams = itemOverride ?: overrides.totalGrams?.takeIf { items.size == 1 } ?: return@map item
        item.copy(
            estimatedGrams = targetGrams,
            nutrition = scaleNutritionToGrams(item.nutrition, item.estimatedGrams, targetGrams),
            notes = listOfNotNull(item.notes, "Gebruikerscontext gebruikte ${formatAiOneDecimal(targetGrams)} g als vaste hoeveelheid.")
                .joinToString(" ")
                .ifBlank { null },
        )
    }
}

private fun applyLockedMealContextItems(
    items: List<MealScanItem>,
    overrides: MealContextOverrides,
): List<MealScanItem> {
    val remaining = items.toMutableList()
    return overrides.itemGramsByName.entries.mapIndexed { index, (contextName, grams) ->
        val matchedIndex = remaining.indexOfFirst { item ->
            val normalizedItem = normalizeAiContextName(item.name)
            normalizedItem == contextName || normalizedItem.contains(contextName) || contextName.contains(normalizedItem)
        }.takeIf { it >= 0 } ?: index.takeIf { it in remaining.indices }
        val matched = matchedIndex?.let { remaining.removeAt(it) }
        val displayName = overrides.itemDisplayNamesByName[contextName] ?: contextName
        val base = matched ?: MealScanItem(
            name = displayName,
            estimatedGrams = grams,
            nutrition = NutritionFacts.Zero,
            confidence = "low",
            notes = "Gebruikerscontext genoemd; AI leverde geen apart betrouwbaar component terug.",
        )
        base.copy(
            name = displayName,
            estimatedGrams = grams,
            nutrition = scaleNutritionToGrams(base.nutrition, base.estimatedGrams, grams),
            confidence = base.confidence ?: if (matched == null) "low" else null,
            notes = listOfNotNull(
                base.notes,
                "Gebruikerscontext vergrendelde dit component op ${formatAiOneDecimal(grams)} g.",
            ).joinToString(" ").ifBlank { null },
        )
    }
}

private fun buildMealScanReviewNotes(
    originalItems: List<MealScanItem>,
    normalizedItems: List<MealScanItem>,
    overrides: MealContextOverrides,
): String? {
    val notes = buildList {
        if (hasSuspiciousMealScanDuplicates(originalItems)) {
            add("Controleer deze scan: meerdere onderdelen lijken samengevoegd of overschreven.")
        }
        val missingContextNames = overrides.itemGramsByName.keys
            .filterNot { contextName -> normalizedItems.any { normalizeAiContextName(it.name) == contextName } }
        if (missingContextNames.isNotEmpty()) {
            add("Controleer deze scan: niet alle contextproducten kwamen betrouwbaar uit de AI-output.")
        }
        if (overrides.itemGramsByName.size > 1) {
            add("Expliciete gebruikerscontext is als vaste componentlijst gebruikt.")
        }
    }
    return notes.joinToString(" ").ifBlank { null }
}

private fun hasSuspiciousMealScanDuplicates(items: List<MealScanItem>): Boolean {
    if (items.size < 3) return false
    val duplicateNames = items
        .groupingBy { normalizeAiContextName(it.name) }
        .eachCount()
        .values
        .any { it >= 3 }
    val duplicateMacroSets = items
        .groupingBy { item ->
            listOf(
                item.nutrition.calories,
                item.nutrition.protein,
                item.nutrition.carbs,
                item.nutrition.fat,
            ).joinToString("|") { formatAiOneDecimal(it) }
        }
        .eachCount()
        .values
        .any { it >= 3 }
    return duplicateNames || duplicateMacroSets
}

private fun scaleNutritionToGrams(nutrition: NutritionFacts, sourceGrams: Double, targetGrams: Double): NutritionFacts {
    if (sourceGrams <= 0.0 || !sourceGrams.isFinite()) return nutrition
    val ratio = targetGrams / sourceGrams
    return NutritionFacts(
        calories = nutrition.calories * ratio,
        protein = nutrition.protein * ratio,
        carbs = nutrition.carbs * ratio,
        fat = nutrition.fat * ratio,
    )
}

private fun buildBodyMeasurementNotes(baseNotes: String?, overrides: BodyMeasurementContextOverrides): String? {
    val overrideLabels = buildList {
        if (overrides.weight != null) add("gewicht")
        if (overrides.bodyFat != null) add("vetpercentage")
        if (overrides.muscleMass != null) add("spiermassa")
    }
    val overrideNote = overrideLabels.takeIf { it.isNotEmpty() }
        ?.joinToString(prefix = "Gebruikerscontext gebruikt als vaste waarde voor ", separator = ", ", postfix = ".")
    return listOfNotNull(baseNotes, overrideNote).joinToString(" ").ifBlank { null }
}

private fun normalizeAiContextName(value: String): String =
    value.lowercase(Locale.ROOT)
        .replace(Regex("""[^a-z0-9à-ÿ ]"""), " ")
        .replace(Regex("""\s+"""), " ")
        .trim()

private fun String.toAiDoubleOrNull(): Double? =
    trim().replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() }

private fun formatAiOneDecimal(value: Double): String =
    String.format(Locale.US, "%.1f", value)

private fun fallbackBodyMeasurementPhoto(
    notes: String = "AI-weegfotoanalyse is nu niet beschikbaar. Vul de meting handmatig in.",
    rawResponse: String? = null,
    source: BodyMeasurementPhotoSource = BodyMeasurementPhotoSource.LOCAL_FALLBACK,
) = BodyMeasurementPhotoResult(
    weight = 0.0,
    bodyFat = 0.0,
    muscleMass = 0.0,
    notes = notes,
    rawResponse = rawResponse,
    source = source,
)

private fun MealType.promptLabel(): String = when (this) {
    MealType.BREAKFAST -> "Ochtend"
    MealType.LUNCH -> "Middag"
    MealType.DINNER -> "Avond"
    MealType.SNACK -> "Snack"
}

private const val MaxMealScanGrams = 100_000.0
private const val MaxMealScanCalories = 100_000.0
private const val MaxMealScanMacro = 100_000.0
private const val MaxMealScanItems = 20
private const val MaxMealScanNameChars = 120
private const val MaxMealScanMetaChars = 40
private const val MaxMealScanNotesChars = 600
private const val MaxMealScanContextChars = 2_000
private const val MaxBodyMeasurementContextChars = 1_000
private const val MaxMealImageSourceBytes = 6L * 1024L * 1024L
private const val MaxMealImageUploadBytes = 1_500_000
private const val MaxMealImageDimensionPx = 1_280
private const val MaxAiRawResponseChars = 64_000

internal class AiRawResponseTooLargeException : RuntimeException("AI-antwoord is te groot.")

internal fun requireAiRawResponseWithinLimit(text: String): String {
    if (text.length > MaxAiRawResponseChars) throw AiRawResponseTooLargeException()
    return text
}

internal fun prepareMealScanImageBytes(file: File): ByteArray? {
    if (!file.exists() || file.length() <= 0L || file.length() > MaxMealImageSourceBytes) return null
    val raw = file.readBytes()
    if (raw.size <= MaxMealImageUploadBytes) {
        return runCatching { compressMealImageForAiUpload(raw) }.getOrDefault(raw)
    }
    return runCatching { compressMealImageForAiUpload(raw) }
        .getOrNull()
        ?.takeIf { it.size <= MaxMealImageUploadBytes }
}

private fun compressMealImageForAiUpload(raw: ByteArray): ByteArray {
    val source = BitmapFactory.decodeByteArray(raw, 0, raw.size) ?: return raw
    val scale = minOf(
        1f,
        MaxMealImageDimensionPx.toFloat() / maxOf(source.width, source.height).coerceAtLeast(1),
    )
    val bitmap = if (scale < 1f) {
        Bitmap.createScaledBitmap(
            source,
            (source.width * scale).toInt().coerceAtLeast(1),
            (source.height * scale).toInt().coerceAtLeast(1),
            true,
        )
    } else {
        source
    }
    return ByteArrayOutputStream().use { output ->
        bitmap.compress(Bitmap.CompressFormat.JPEG, 82, output)
        output.toByteArray()
    }
}

private fun com.google.gson.JsonObject.safeNumber(
    key: String,
    default: Double,
    min: Double,
    max: Double,
): Double? {
    val value = get(key) ?: return default
    val parsed = runCatching { value.asDouble }.getOrNull() ?: return null
    return parsed.takeIf { it.isFinite() && it in min..max }
}

private fun com.google.gson.JsonElement?.boundedString(maxChars: Int): String? =
    this
        ?.takeIf { !it.isJsonNull }
        ?.let { element -> runCatching { element.asString.trim().take(maxChars) }.getOrNull() }
        ?.takeIf { it.isNotBlank() }

@Singleton
class WorkoutDebriefService internal constructor(
    private val aiJsonGenerator: AiJsonGenerator,
) {
    internal constructor(
        api: GeminiApi,
        apiKeyProvider: suspend () -> String?,
    ) : this(
        aiJsonGenerator = GeminiOnlyJsonGenerator(api, apiKeyProvider),
    )

    @Inject
    constructor(
        aiProviderRouter: AiProviderRouter,
    ) : this(
        aiJsonGenerator = aiProviderRouter,
    )

    suspend fun generateWorkoutDebrief(
        totalVolume: Double,
        progression: Double?,
        comparisonSummary: String = if (progression == null) {
            "Nog geen eerdere vergelijkbare training gevonden."
        } else {
            "Vergelijking beschikbaar: ${formatAiPercentNl(progression)}%."
        },
        distribution: String,
        avgRpe: Float,
        topExercises: String,
        weeklyFrequency: Int,
    ): WorkoutDebrief =
        runCatching {
            val routed = aiJsonGenerator.generateJson(
                AiRouteRequest(
                    feature = AiFeature.WORKOUT_DEBRIEF,
                    schemaName = "workout_debrief",
                    responseJsonSchema = AiJsonSchemas.workoutDebrief,
                    thinkingBudget = 1000,
                    prompt = AiPrompts.workoutDebrief(
                                        totalVolume = totalVolume,
                                        progression = progression,
                                        comparisonSummary = comparisonSummary,
                                        distribution = distribution,
                                        avgRpe = avgRpe,
                                        topExercises = topExercises,
                                        weeklyFrequency = weeklyFrequency,
                                    ),
                ),
            )
            parseWorkoutDebriefResponse(routed.rawJson, totalVolume, progression, routed.providerUsed)
        }.getOrElse { error ->
            if (error is CancellationException) throw error
            fallbackWorkoutDebriefResult(totalVolume, progression)
        }
}

@Singleton
class GoalAdvisorService internal constructor(
    private val aiJsonGenerator: AiJsonGenerator,
    private val isAiReady: suspend () -> Boolean,
) {
    internal constructor(
        api: GeminiApi,
        isAiReady: suspend () -> Boolean,
        apiKeyProvider: suspend () -> String?,
    ) : this(
        aiJsonGenerator = GeminiOnlyJsonGenerator(api, apiKeyProvider),
        isAiReady = isAiReady,
    )

    @Inject
    constructor(
        aiProviderRouter: AiProviderRouter,
        aiUsageGate: AiUsageGate,
    ) : this(
        aiJsonGenerator = aiProviderRouter,
        isAiReady = { aiUsageGate.isAiReady() },
    )
    suspend fun generateGoalAdvice(
        height: Double,
        weight: Double,
        bodyFat: Double,
        age: Int,
        sex: BiologicalSex,
        activityLevel: String,
        goal: String,
        manualCalorieTarget: Int? = null,
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
                manualCalorieTarget = manualCalorieTarget,
            )
            if (!isAiReady()) return baseline
            val routed = aiJsonGenerator.generateJson(
                AiRouteRequest(
                    feature = AiFeature.GOAL_ADVICE,
                    schemaName = "goal_advice",
                    responseJsonSchema = AiJsonSchemas.goalAdvice,
                    thinkingBudget = 1000,
                    prompt = AiPrompts.goalAdvisor(
                                        height = height,
                                        weight = weight,
                                        bodyFat = bodyFat,
                                        age = age,
                                        sex = sex,
                                        activityLevel = activityLevel,
                                        goal = goal,
                                        baseline = baseline,
                                        manualCalorieTarget = manualCalorieTarget,
                                    ),
                ),
            )
            parseGoalAdvice(routed.rawJson, baseline, routed.providerUsed)
        }.getOrElse { error ->
            if (error is CancellationException) throw error
            deterministicGoalAdvice(
                height = height,
                weight = weight,
                bodyFat = bodyFat,
                age = age,
                sex = sex,
                activityLevel = activityLevel,
                goal = goal,
                manualCalorieTarget = manualCalorieTarget,
            )
        }

    private fun parseGoalAdvice(text: String, baseline: GoalAdvice, provider: AiProvider): GoalAdvice =
        runCatching {
            val boundedText = requireAiRawResponseWithinLimit(text)
            val root = JsonParser.parseString(boundedText).asJsonObject
            val textFields = listOf(
                root.get("trainingFocus")?.asString.orEmpty(),
                root.get("korteSamenvatting")?.asString ?: root.get("summary")?.asString.orEmpty(),
                root.get("calorieAdvies")?.asString.orEmpty(),
                root.get("macroAdvies")?.asString.orEmpty(),
                root.get("activiteitUitleg")?.asString.orEmpty(),
                root.get("advies")?.asString.orEmpty(),
                root.get("dataKwaliteit")?.asString.orEmpty(),
            ) + root.getAsJsonArray("aandachtspunten")?.map { it.asString }.orEmpty()
            if (!textFields.isUsableDutchAiText()) return baseline
            baseline.copy(
                trainingFocus = root.get("trainingFocus")?.asString ?: baseline.trainingFocus,
                summary = root.get("korteSamenvatting")?.asString
                    ?: root.get("summary")?.asString
                    ?: baseline.summary,
                calorieAdvice = root.get("calorieAdvies")?.asString ?: baseline.calorieAdvice,
                macroAdvice = root.get("macroAdvies")?.asString ?: baseline.macroAdvice,
                activityExplanation = root.get("activiteitUitleg")?.asString ?: baseline.activityExplanation,
                attentionPoints = root.getAsJsonArray("aandachtspunten")?.map { it.asString }.orEmpty()
                    .ifEmpty { baseline.attentionPoints },
                advice = root.get("advies")?.asString ?: baseline.advice,
                dataQuality = root.get("dataKwaliteit")?.asString ?: baseline.dataQuality,
                source = provider.toGoalAdviceSource(),
                rawResponse = boundedText,
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
        manualCalorieTarget: Int? = null,
    ): GoalAdvice {
        val baseline = buildGoalBaseline(
            heightCm = height,
            weightKg = weight,
            bodyFat = bodyFat,
            age = age,
            sex = sex,
            activityLevel = activityLevel,
            goal = goal,
            manualCalorieTarget = manualCalorieTarget,
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
            summary = if (manualCalorieTarget != null) {
                "Lokale berekening: onderhoud ${baseline.maintenanceCalories} kcal en jouw calorie doel ${baseline.targetCalories} kcal op basis van je profiel."
            } else {
                "Lokale berekening: onderhoud ${baseline.maintenanceCalories} kcal en doel ${baseline.targetCalories} kcal op basis van je profiel."
            },
            calorieAdvice = buildCalorieAdvice(baseline),
            macroAdvice = "Auto macro's sluiten aan op ${baseline.targetCalories} kcal: ${baseline.proteinTarget} g eiwit, ${baseline.carbsTarget} g koolhydraten en ${baseline.fatTarget} g vet.",
            activityExplanation = "Activiteitsfactor ${formatActivityMultiplierNl(baseline.activityMultiplier)} betekent dat onderhoud is berekend als BMR x activiteit: ${baseline.bmr} x ${formatActivityMultiplierNl(baseline.activityMultiplier)} = ${baseline.maintenanceCalories} kcal.",
            attentionPoints = buildGoalAttentionPoints(bodyFat = bodyFat, activityLevel = activityLevel),
            advice = buildGoalAdviceText(baseline, goal),
            dataQuality = "Lokale schatting op basis van profielgegevens. Werkelijke behoefte kan afwijken door stappen, training, slaap en gewichtstrend.",
            source = GoalAdviceSource.LOCAL_CALCULATION,
            rawResponse = null,
        )
    }

    private fun buildCalorieAdvice(baseline: com.trainiq.domain.model.GoalBaseline): String {
        val difference = baseline.targetCalories - baseline.maintenanceCalories
        return when {
            difference < 0 -> "Je doel ligt ${-difference} kcal onder onderhoud: een matig tekort dat beter vol te houden is."
            difference > 0 -> "Je doel ligt $difference kcal boven onderhoud voor gecontroleerde opbouw."
            else -> "Je doel ligt rond onderhoud; stuur vooral op trend en trainingsprestatie."
        }
    }

    private fun buildGoalAdviceText(baseline: com.trainiq.domain.model.GoalBaseline, goal: String): String = when {
        goal.contains("cut", ignoreCase = true) || goal.contains("fat", ignoreCase = true) || goal.contains("lose", ignoreCase = true) ->
            "Start twee weken met ${baseline.targetCalories} kcal, houd eiwit stabiel en verlaag pas verder als gewicht en taille niet bewegen."
        goal.contains("bulk", ignoreCase = true) || goal.contains("gain", ignoreCase = true) ->
            "Verhoog pas verder als gewicht niet langzaam stijgt en training niet vooruitgaat."
        else -> "Houd deze targets stabiel en evalueer op basis van gewichtstrend, energie en krachtprestaties."
    }

    private fun buildGoalAttentionPoints(bodyFat: Double, activityLevel: String): List<String> = buildList {
        if (bodyFat !in 5.0..60.0) add("Vetpercentage ontbreekt of lijkt onzeker; eiwit is daarom conservatief geschat.")
        add("Activiteitsniveau '${activityLevel.toDutchGoalActivityLabel()}' blijft een keuze en is geen gemeten TDEE.")
    }
}

@Singleton
class WeeklyReportService @Inject constructor(
    private val aiJsonGenerator: AiJsonGenerator,
    private val aiUsageGate: AiUsageGate,
) {
    suspend fun generateWeeklyReport(volume: Double, weightTrend: Double, adherence: Int): WeeklyReportResult =
        runCatching {
            if (!aiUsageGate.isAiReady()) return fallbackWeeklyReport(adherence)
            val routed = aiJsonGenerator.generateJson(
                AiRouteRequest(
                    feature = AiFeature.WEEKLY_REPORT,
                    prompt = AiPrompts.weeklyReport(volume, weightTrend, adherence),
                    schemaName = "weekly_report",
                    responseJsonSchema = AiJsonSchemas.weeklyReport,
                    thinkingBudget = 1000,
                ),
            )
            parseWeeklyReportResponse(routed.rawJson, adherence, routed.providerUsed)
        }.getOrElse { error ->
            if (error is CancellationException) throw error
            fallbackWeeklyReport(adherence)
        }

}

internal fun parseWeeklyReportResponse(text: String, adherence: Int): WeeklyReportResult =
    parseWeeklyReportResponse(text, adherence, AiProvider.GEMINI)

internal fun parseWeeklyReportResponse(text: String, adherence: Int, provider: AiProvider): WeeklyReportResult =
    runCatching {
        val boundedText = requireAiRawResponseWithinLimit(text)
        val root = JsonParser.parseString(boundedText).asJsonObject
        val summary = root.get("summary")?.asString?.trim().orEmpty()
        if (summary.isBlank()) return fallbackWeeklyReport(adherence)
        val wins = root.getAsJsonArray("wins")?.map { it.asString }.orEmpty()
        val risks = root.getAsJsonArray("risks")?.map { it.asString }.orEmpty()
        val nextWeekFocus = root.get("nextWeekFocus")?.asString ?: "Bescherm herstel voordat je extra volume toevoegt."
        val rationaleBullets = (
            root.getAsJsonArray("rationaleBullets")
                ?: root.getAsJsonArray("coachReasoningSummary")
                ?: root.getAsJsonArray(LegacyWeeklyReportReasoningKey)
            )?.map { it.asString }.orEmpty()
        val fields = listOf(summary, nextWeekFocus) + wins + risks + rationaleBullets
        if (!fields.isUsableDutchAiText()) return fallbackWeeklyReport(adherence)
        WeeklyReportResult(
            summary = summary,
            wins = wins,
            risks = risks,
            nextWeekFocus = nextWeekFocus,
            rationaleBullets = rationaleBullets,
            source = provider.toWeeklyReportSource(),
            rawResponse = boundedText,
        )
    }.getOrElse { fallbackWeeklyReport(adherence) }

internal fun fallbackWeeklyReport(adherence: Int): WeeklyReportResult =
    WeeklyReportResult(
        summary = "Lokale samenvatting: er is nog te weinig betrouwbare context voor een AI-weekrapport. Consistentie staat nu op $adherence%.",
        wins = listOf("Training en voeding blijven lokaal beschikbaar."),
        risks = listOf("Zonder compleet profiel en recente logs kan TrainIQ geen betrouwbaar weekadvies geven."),
        nextWeekFocus = "Vul je profiel aan en log een paar trainingen of maaltijden voordat je het volume verhoogt.",
        source = WeeklyReportSource.LOCAL_FALLBACK,
    )

private val LegacyWeeklyReportReasoningKey = "thinking" + "Process"

internal fun parseWorkoutDebriefResponse(
    text: String,
    totalVolume: Double,
    progression: Double?,
): WorkoutDebrief =
    parseWorkoutDebriefResponse(text, totalVolume, progression, AiProvider.GEMINI)

internal fun parseWorkoutDebriefResponse(
    text: String,
    totalVolume: Double,
    progression: Double?,
    provider: AiProvider,
): WorkoutDebrief = runCatching {
    val boundedText = requireAiRawResponseWithinLimit(text)
    val root = JsonParser.parseString(boundedText).asJsonObject
    val summary = root.get("summary")?.asString ?: "Training opgeslagen."
    val progressionFeedback = root.get("progressionFeedback")?.asString ?: "Progressie bleef stabiel."
    val recommendation = root.get("recommendation")?.asString
        ?: "Herhaal deze sessie en mik op een extra herhaling bij de belangrijkste oefeningen."
    val nextSessionFocus = root.get("nextSessionFocus")?.asString?.trim().orEmpty()
        .ifBlank { "Huidige gewichten vasthouden" }
    val wins = root.getAsJsonArray("wins")?.map { it.asString }.orEmpty()
    val risks = root.getAsJsonArray("risks")?.map { it.asString }.orEmpty()
    val nextLoadTarget = root.get("nextLoadTarget")?.asString?.trim().orEmpty()
    val recoveryAdvice = root.get("recoveryAdvice")?.asString?.trim().orEmpty()
    val fields = listOf(
        summary,
        progressionFeedback,
        recommendation,
        nextSessionFocus,
        nextLoadTarget,
        recoveryAdvice,
    ) + wins + risks
    if (!fields.isUsableDutchAiText()) return fallbackWorkoutDebriefResult(totalVolume, progression)
    WorkoutDebrief(
        summary = summary,
        progressionFeedback = progressionFeedback,
        recommendation = recommendation,
        nextSessionFocus = nextSessionFocus,
        recoveryScore = (root.get("recoveryScore")?.asInt ?: 75).coerceIn(0, 100),
        intensitySignal = root.get("intensitySignal")?.asString?.trim()?.uppercase().orEmpty()
            .ifBlank { "MAINTAIN" },
        wins = wins,
        risks = risks,
        nextLoadTarget = nextLoadTarget,
        recoveryAdvice = recoveryAdvice,
        source = provider.toWorkoutDebriefSource(),
    )
}.getOrElse { fallbackWorkoutDebriefResult(totalVolume, progression) }

private fun AiProvider.toMealAnalysisSource(): MealAnalysisSource = when (this) {
    AiProvider.GEMINI -> MealAnalysisSource.API
    AiProvider.OPENAI -> MealAnalysisSource.OPENAI
}

private fun AiProvider.toBodyMeasurementPhotoSource(): BodyMeasurementPhotoSource = when (this) {
    AiProvider.GEMINI -> BodyMeasurementPhotoSource.GEMINI_2_5_FLASH
    AiProvider.OPENAI -> BodyMeasurementPhotoSource.OPENAI
}

private fun AiProvider.toWeeklyReportSource(): WeeklyReportSource = when (this) {
    AiProvider.GEMINI -> WeeklyReportSource.GEMINI_2_5_FLASH
    AiProvider.OPENAI -> WeeklyReportSource.OPENAI
}

private fun AiProvider.toGoalAdviceSource(): GoalAdviceSource = when (this) {
    AiProvider.GEMINI -> GoalAdviceSource.GEMINI_2_5_FLASH
    AiProvider.OPENAI -> GoalAdviceSource.OPENAI
}

private fun AiProvider.toWorkoutDebriefSource(): WorkoutDebriefSource = when (this) {
    AiProvider.GEMINI -> WorkoutDebriefSource.GEMINI_2_5_FLASH
    AiProvider.OPENAI -> WorkoutDebriefSource.OPENAI
}

private class GeminiOnlyJsonGenerator(
    private val api: GeminiApi,
    private val apiKeyProvider: suspend () -> String?,
) : AiJsonGenerator {
    override suspend fun generateJson(request: AiRouteRequest): AiRouteResult {
        val apiKey = apiKeyProvider() ?: throw AiProviderUnavailableException(emptyList())
        return callGeminiWithBoundedRetry(feature = request.feature) {
            GeminiModelClient(api).generateJson(apiKey, request)
        }
    }
}

internal fun fallbackWorkoutDebriefResult(totalVolume: Double, progression: Double?) = WorkoutDebrief(
    summary = "Lokale samenvatting: volume ${totalVolume.toInt()} kg.",
    progressionFeedback = progression?.let {
        "Volume veranderde met ${formatAiPercentNl(it)}% ten opzichte van de vorige sessie."
    } ?: "Nog geen eerdere vergelijkbare training gevonden.",
    recommendation = "Houd dezelfde opzet aan en verhoog pas als uitvoering en herstel goed blijven.",
    nextSessionFocus = "Huidige gewichten vasthouden",
    recoveryScore = 75,
    intensitySignal = "MAINTAIN",
    wins = listOf("Trainingsvolume lokaal vastgelegd."),
    risks = if ((progression ?: 0.0) > 5.0) listOf("Volume steeg meer dan 5%; let op herstel.") else emptyList(),
    nextLoadTarget = "Herhaal de huidige werkgewichten en voeg alleen herhalingen toe als RPE onder 8 blijft.",
    recoveryAdvice = "Gebruik slaap, stappen en spierpijn om te bepalen of je verhoogt of vasthoudt.",
    source = WorkoutDebriefSource.LOCAL_FALLBACK,
)

internal fun formatAiPercentNl(value: Double): String =
    String.format(Locale.forLanguageTag("nl-NL"), "%.1f", value)

internal fun formatActivityMultiplierNl(value: Double): String =
    String.format(Locale.forLanguageTag("nl-NL"), "%.3f", value)

internal fun String.toDutchGoalActivityLabel(): String = when (trim().lowercase(Locale.ROOT)) {
    "sedentary" -> "zittend"
    "lightly active", "light active" -> "licht actief"
    "moderately active", "moderate active" -> "matig actief"
    "very active" -> "zeer actief"
    "extra active", "athlete" -> "extreem actief"
    else -> trim().ifBlank { "onbekend" }
}
