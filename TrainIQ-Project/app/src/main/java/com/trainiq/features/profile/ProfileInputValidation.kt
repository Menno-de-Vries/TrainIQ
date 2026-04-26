package com.trainiq.features.profile

import com.trainiq.domain.model.BiologicalSex

internal val ProfileActivityLevels = listOf(
    "Sedentary",
    "Lightly active",
    "Moderately active",
    "Very active",
    "Athlete",
)

internal const val DefaultProfileActivityLevel = "Moderately active"

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

internal sealed interface ProfileInputValidationResult {
    data class Valid(val input: ValidatedProfileInput) : ProfileInputValidationResult
    data class Invalid(val error: ProfileInputValidationError) : ProfileInputValidationResult
}

internal enum class ProfileInputField {
    Name,
    Age,
    Height,
    Weight,
    BodyFat,
    ActivityLevel,
    Goal,
}

internal enum class ProfileInputValidationError(
    val message: String,
    val field: ProfileInputField,
) {
    NameRequired("Naam is verplicht.", ProfileInputField.Name),
    InvalidAge("Leeftijd moet tussen 1 en 120 zijn.", ProfileInputField.Age),
    InvalidHeight("Lengte moet tussen 80 en 250 cm zijn.", ProfileInputField.Height),
    InvalidWeight("Gewicht moet tussen 30 en 300 kg zijn.", ProfileInputField.Weight),
    InvalidBodyFat("Vetpercentage moet tussen 0 en 100% zijn.", ProfileInputField.BodyFat),
    InvalidActivityLevel("Kies een activiteitsniveau.", ProfileInputField.ActivityLevel),
    GoalRequired("Doel is verplicht.", ProfileInputField.Goal),
}

internal fun buildValidatedProfileInput(
    name: String,
    height: String,
    weight: String,
    bodyFat: String,
    age: String,
    sex: BiologicalSex,
    activityLevel: String,
    goal: String,
): ValidatedProfileInput? = when (
    val result = validateProfileInput(name, height, weight, bodyFat, age, sex, activityLevel, goal)
) {
    is ProfileInputValidationResult.Valid -> result.input
    is ProfileInputValidationResult.Invalid -> null
}

internal fun validateProfileInput(
    name: String,
    height: String,
    weight: String,
    bodyFat: String,
    age: String,
    sex: BiologicalSex,
    activityLevel: String,
    goal: String,
): ProfileInputValidationResult {
    val trimmedName = name.trim()
    val trimmedActivityLevel = activityLevel.trim()
    val trimmedGoal = goal.trim()
    val parsedHeight = height.toProfileDoubleOrNull()
    val parsedWeight = weight.toProfileDoubleOrNull()
    val parsedBodyFat = bodyFat.toProfileDoubleOrNull()
    val parsedAge = age.trim().toIntOrNull()

    val error = when {
        trimmedName.isBlank() -> ProfileInputValidationError.NameRequired
        parsedAge == null || parsedAge !in 1..120 -> ProfileInputValidationError.InvalidAge
        parsedHeight == null || parsedHeight !in 80.0..250.0 -> ProfileInputValidationError.InvalidHeight
        parsedWeight == null || parsedWeight !in 30.0..300.0 -> ProfileInputValidationError.InvalidWeight
        parsedBodyFat == null || parsedBodyFat !in 0.0..100.0 -> ProfileInputValidationError.InvalidBodyFat
        trimmedActivityLevel.isBlank() || trimmedActivityLevel !in ProfileActivityLevels -> ProfileInputValidationError.InvalidActivityLevel
        trimmedGoal.isBlank() -> ProfileInputValidationError.GoalRequired
        else -> null
    }

    if (error != null) return ProfileInputValidationResult.Invalid(error)

    return ProfileInputValidationResult.Valid(
        ValidatedProfileInput(
            name = trimmedName,
            height = parsedHeight!!,
            weight = parsedWeight!!,
            bodyFat = parsedBodyFat!!,
            age = parsedAge!!,
            sex = sex,
            activityLevel = trimmedActivityLevel,
            goal = trimmedGoal,
        ),
    )
}

private fun String.toProfileDoubleOrNull(): Double? {
    val value = trim().replace(',', '.').toDoubleOrNull() ?: return null
    return value.takeIf { !it.isNaN() && !it.isInfinite() }
}
