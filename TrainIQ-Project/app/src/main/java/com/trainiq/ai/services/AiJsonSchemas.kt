package com.trainiq.ai.services

object AiJsonSchemas {
    val mealScan: Map<String, Any?> = objectSchema(
        properties = listOf(
            "items" to arraySchema(
                objectSchema(
                    properties = listOf(
                        "name" to stringSchema(maxLength = MaxMealScanNameChars),
                        "estimatedGrams" to numberSchema(),
                        "calories" to numberSchema(),
                        "protein" to numberSchema(),
                        "carbs" to numberSchema(),
                        "fat" to numberSchema(),
                        "confidence" to stringSchema(maxLength = MaxAiMetaChars),
                        "notes" to stringSchema(maxLength = MaxAiNotesChars),
                    ),
                    required = listOf("name", "estimatedGrams", "calories", "protein", "carbs", "fat"),
                ),
                maxItems = MaxMealScanItems,
            ),
            "suggestedMealType" to stringSchema(enum = listOf("BREAKFAST", "LUNCH", "DINNER", "SNACK")),
            "notes" to stringSchema(maxLength = MaxAiNotesChars),
        ),
        required = listOf("items", "suggestedMealType"),
    )

    val workoutDebrief: Map<String, Any?> = objectSchema(
        properties = listOf(
            "summary" to stringSchema(),
            "progressionFeedback" to stringSchema(),
            "recommendation" to stringSchema(),
            "nextSessionFocus" to stringSchema(),
            "recoveryScore" to integerSchema(minimum = 0, maximum = 100),
            "intensitySignal" to stringSchema(enum = listOf("INCREASE", "MAINTAIN", "DELOAD")),
            "wins" to arraySchema(stringSchema(maxLength = MaxAiNotesChars), maxItems = MaxAiBulletItems),
            "risks" to arraySchema(stringSchema(maxLength = MaxAiNotesChars), maxItems = MaxAiBulletItems),
            "nextLoadTarget" to stringSchema(),
            "recoveryAdvice" to stringSchema(),
        ),
        required = listOf("summary", "recommendation", "nextSessionFocus", "recoveryScore", "intensitySignal"),
    )

    val goalAdvice: Map<String, Any?> = objectSchema(
        properties = listOf(
            "trainingFocus" to stringSchema(),
            "calorieAdvies" to stringSchema(),
            "korteSamenvatting" to stringSchema(),
            "macroAdvies" to stringSchema(),
            "activiteitUitleg" to stringSchema(),
            "aandachtspunten" to arraySchema(stringSchema(maxLength = MaxAiNotesChars), maxItems = MaxAiBulletItems),
            "advies" to stringSchema(),
            "dataKwaliteit" to stringSchema(),
        ),
        required = listOf(
            "trainingFocus",
            "calorieAdvies",
            "korteSamenvatting",
        ),
    )

    val weeklyReport: Map<String, Any?> = objectSchema(
        properties = listOf(
            "summary" to stringSchema(),
            "wins" to arraySchema(stringSchema(maxLength = MaxAiNotesChars), maxItems = MaxAiBulletItems),
            "risks" to arraySchema(stringSchema(maxLength = MaxAiNotesChars), maxItems = MaxAiBulletItems),
            "nextWeekFocus" to stringSchema(),
            "rationaleBullets" to arraySchema(stringSchema(maxLength = MaxAiNotesChars), maxItems = MaxAiBulletItems),
        ),
        required = listOf("summary", "nextWeekFocus"),
    )

    val bodyMeasurementPhoto: Map<String, Any?> = objectSchema(
        properties = listOf(
            "weight" to numberSchema(),
            "bodyFat" to numberSchema(),
            "muscleMass" to numberSchema(),
            "unit" to stringSchema(enum = listOf("kg")),
            "confidence" to stringSchema(enum = listOf("high", "medium", "low")),
            "notes" to stringSchema(),
        ),
        required = listOf("weight", "bodyFat", "muscleMass", "unit", "confidence"),
    )

    val routineGenerator: Map<String, Any?> = objectSchema(
        properties = listOf(
            "routineName" to stringSchema(maxLength = MaxGeneratedRoutineNameChars),
            "routineDescription" to stringSchema(maxLength = MaxGeneratedRoutineTextChars),
            "estimatedDurationMinutes" to integerSchema(),
            "periodizationNote" to stringSchema(maxLength = MaxGeneratedRoutineTextChars),
            "days" to arraySchema(
                objectSchema(
                    properties = listOf(
                        "dayName" to stringSchema(maxLength = MaxGeneratedRoutineNameChars),
                        "estimatedDurationMinutes" to integerSchema(),
                        "exercises" to arraySchema(
                            objectSchema(
                                properties = listOf(
                                    "exerciseName" to stringSchema(maxLength = MaxGeneratedRoutineNameChars),
                                    "muscleGroup" to stringSchema(maxLength = MaxGeneratedRoutineNameChars),
                                    "equipment" to stringSchema(maxLength = MaxGeneratedRoutineNameChars),
                                    "targetSets" to integerSchema(),
                                    "repRange" to stringSchema(maxLength = MaxAiMetaChars),
                                    "restSeconds" to integerSchema(),
                                    "targetWeightKg" to numberSchema(),
                                    "targetRpe" to numberSchema(),
                                    "coachingCue" to stringSchema(maxLength = MaxGeneratedRoutineTextChars),
                                    "existingExerciseId" to integerSchema(),
                                ),
                                required = listOf(
                                    "exerciseName",
                                    "muscleGroup",
                                    "equipment",
                                    "targetSets",
                                    "repRange",
                                    "restSeconds",
                                ),
                            ),
                            maxItems = MaxGeneratedExercisesPerDay,
                        ),
                    ),
                    required = listOf("dayName", "exercises"),
                ),
                maxItems = MaxGeneratedRoutineDays,
            ),
        ),
        required = listOf("routineName", "days"),
    )
}

private fun objectSchema(
    properties: List<Pair<String, Map<String, Any?>>>,
    required: List<String> = properties.map { it.first },
): Map<String, Any?> =
    mapOf(
        "type" to "object",
        "properties" to properties.toMap(),
        "required" to required,
    )

private fun arraySchema(items: Map<String, Any?>, maxItems: Int? = null): Map<String, Any?> =
    buildMap {
        put("type", "array")
        put("items", items)
        maxItems?.let { put("maxItems", it) }
    }

private fun stringSchema(enum: List<String>? = null, maxLength: Int? = null): Map<String, Any?> =
    buildMap {
        put("type", "string")
        enum?.let { put("enum", it) }
        maxLength?.let { put("maxLength", it) }
    }

private fun numberSchema(): Map<String, Any?> = mapOf("type" to "number")

private fun integerSchema(minimum: Int? = null, maximum: Int? = null): Map<String, Any?> =
    buildMap {
        put("type", "integer")
        minimum?.let { put("minimum", it) }
        maximum?.let { put("maximum", it) }
    }

private const val MaxMealScanItems = 20
private const val MaxMealScanNameChars = 120
private const val MaxAiMetaChars = 40
private const val MaxAiNotesChars = 600
private const val MaxAiBulletItems = 8
private const val MaxGeneratedRoutineDays = 7
private const val MaxGeneratedExercisesPerDay = 12
private const val MaxGeneratedRoutineNameChars = 120
private const val MaxGeneratedRoutineTextChars = 600
