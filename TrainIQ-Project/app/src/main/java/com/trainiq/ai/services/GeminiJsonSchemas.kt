package com.trainiq.ai.services

object GeminiJsonSchemas {
    val mealScan: Map<String, Any?> = objectSchema(
        properties = listOf(
            "items" to arraySchema(
                objectSchema(
                    properties = listOf(
                        "name" to stringSchema(),
                        "estimatedGrams" to numberSchema(),
                        "calories" to numberSchema(),
                        "protein" to numberSchema(),
                        "carbs" to numberSchema(),
                        "fat" to numberSchema(),
                        "confidence" to stringSchema(),
                        "notes" to stringSchema(),
                    ),
                    required = listOf("name", "estimatedGrams", "calories", "protein", "carbs", "fat"),
                ),
            ),
            "suggestedMealType" to stringSchema(enum = listOf("BREAKFAST", "LUNCH", "DINNER", "SNACK")),
            "notes" to stringSchema(),
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
            "wins" to arraySchema(stringSchema()),
            "risks" to arraySchema(stringSchema()),
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
            "aandachtspunten" to arraySchema(stringSchema()),
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
            "wins" to arraySchema(stringSchema()),
            "risks" to arraySchema(stringSchema()),
            "nextWeekFocus" to stringSchema(),
            "rationaleBullets" to arraySchema(stringSchema()),
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
            "routineName" to stringSchema(),
            "routineDescription" to stringSchema(),
            "estimatedDurationMinutes" to integerSchema(),
            "periodizationNote" to stringSchema(),
            "days" to arraySchema(
                objectSchema(
                    properties = listOf(
                        "dayName" to stringSchema(),
                        "estimatedDurationMinutes" to integerSchema(),
                        "exercises" to arraySchema(
                            objectSchema(
                                properties = listOf(
                                    "exerciseName" to stringSchema(),
                                    "muscleGroup" to stringSchema(),
                                    "equipment" to stringSchema(),
                                    "targetSets" to integerSchema(),
                                    "repRange" to stringSchema(),
                                    "restSeconds" to integerSchema(),
                                    "targetWeightKg" to numberSchema(),
                                    "targetRpe" to numberSchema(),
                                    "coachingCue" to stringSchema(),
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
                        ),
                    ),
                    required = listOf("dayName", "exercises"),
                ),
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

private fun arraySchema(items: Map<String, Any?>): Map<String, Any?> =
    mapOf("type" to "array", "items" to items)

private fun stringSchema(enum: List<String>? = null): Map<String, Any?> =
    buildMap {
        put("type", "string")
        enum?.let { put("enum", it) }
    }

private fun numberSchema(): Map<String, Any?> = mapOf("type" to "number")

private fun integerSchema(minimum: Int? = null, maximum: Int? = null): Map<String, Any?> =
    buildMap {
        put("type", "integer")
        minimum?.let { put("minimum", it) }
        maximum?.let { put("maximum", it) }
    }
