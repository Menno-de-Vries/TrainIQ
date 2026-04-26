package com.trainiq.features.profile

import com.trainiq.domain.model.BiologicalSex

internal data class ValidatedProfileInput(
    val name: String,
    val height: Double,
    val weight: Double,
    val bodyFat: Double,
    val age: Int,
    val sex: BiologicalSex,
    val activityLevel: String,
    val goal: String,
)

internal fun buildValidatedProfileInput(
    name: String,
    height: String,
    weight: String,
    bodyFat: String,
    age: String,
    sex: BiologicalSex,
    activityLevel: String,
    goal: String,
): ValidatedProfileInput? {
    val trimmedName = name.trim()
    val trimmedActivityLevel = activityLevel.trim()
    val trimmedGoal = goal.trim()
    val parsedHeight = height.toProfileDoubleOrNull()
    val parsedWeight = weight.toProfileDoubleOrNull()
    val parsedBodyFat = bodyFat.toProfileDoubleOrNull()
    val parsedAge = age.trim().toIntOrNull()

    if (
        trimmedName.isBlank() ||
        trimmedActivityLevel.isBlank() ||
        trimmedGoal.isBlank() ||
        parsedHeight == null ||
        parsedWeight == null ||
        parsedBodyFat == null ||
        parsedAge == null ||
        parsedHeight <= 0.0 ||
        parsedWeight <= 0.0 ||
        parsedBodyFat !in 0.0..100.0 ||
        parsedAge !in 1..120
    ) {
        return null
    }

    return ValidatedProfileInput(
        name = trimmedName,
        height = parsedHeight,
        weight = parsedWeight,
        bodyFat = parsedBodyFat,
        age = parsedAge,
        sex = sex,
        activityLevel = trimmedActivityLevel,
        goal = trimmedGoal,
    )
}

private fun String.toProfileDoubleOrNull(): Double? {
    val value = trim().replace(',', '.').toDoubleOrNull() ?: return null
    return value.takeIf { !it.isNaN() && !it.isInfinite() }
}
