package com.trainiq.domain.model

import java.time.Instant
import java.time.ZoneId
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

enum class BiologicalSex {
    MALE,
    FEMALE,
}

enum class MealType(val label: String) {
    BREAKFAST("Breakfast"),
    LUNCH("Lunch"),
    DINNER("Dinner"),
    SNACK("Snack"),
}

data class GoalBaseline(
    val bmr: Int,
    val activityMultiplier: Double,
    val maintenanceCalories: Int,
    val targetCalories: Int,
    val tefCalories: Int,
    val proteinTarget: Int,
    val carbsTarget: Int,
    val fatTarget: Int,
)

data class EnergyBalanceSnapshot(
    val caloriesIn: Int,
    val caloriesOut: Int,
    val balance: Int,
    val bmr: Int,
    val tefCalories: Int,
    val neatCalories: Int,
    val workoutCalories: Int,
)

private const val TefRate = 0.10

fun String.toActivityMultiplier(): Double = when (trim().lowercase()) {
    "sedentary" -> 1.2
    "lightly active" -> 1.375
    "moderately active" -> 1.55
    "very active" -> 1.725
    "athlete" -> 1.9
    else -> when {
        contains("sedent", ignoreCase = true) -> 1.2
        contains("light", ignoreCase = true) -> 1.375
        contains("very", ignoreCase = true) -> 1.725
        contains("athlete", ignoreCase = true) || contains("extra", ignoreCase = true) -> 1.9
        else -> 1.55
    }
}

fun mifflinStJeorBmr(
    weightKg: Double,
    heightCm: Double,
    age: Int,
    sex: BiologicalSex,
): Int {
    val sexOffset = when (sex) {
        BiologicalSex.MALE -> 5
        BiologicalSex.FEMALE -> -161
    }
    return ((10 * weightKg) + (6.25 * heightCm) - (5 * age) + sexOffset).roundToInt()
}

fun buildGoalBaseline(
    heightCm: Double,
    weightKg: Double,
    bodyFat: Double,
    age: Int,
    sex: BiologicalSex,
    activityLevel: String,
    goal: String,
): GoalBaseline {
    val bmr = mifflinStJeorBmr(weightKg = weightKg, heightCm = heightCm, age = age, sex = sex)
    val activityMultiplier = activityLevel.toActivityMultiplier()
    val maintenanceCalories = (bmr * activityMultiplier).roundToInt()
    val targetCalories = when {
        goal.contains("cut", ignoreCase = true) || goal.contains("fat", ignoreCase = true) || goal.contains("lose", ignoreCase = true) ->
            (maintenanceCalories * 0.90).roundToInt()
        goal.contains("bulk", ignoreCase = true) || goal.contains("gain", ignoreCase = true) ->
            (maintenanceCalories * 1.07).roundToInt()
        goal.contains("recomp", ignoreCase = true) || bodyFat >= 20.0 ->
            (maintenanceCalories * 0.92).roundToInt()
        else -> maintenanceCalories
    }
    val hasUsableBodyFat = bodyFat in 5.0..60.0
    val leanMassKg = if (hasUsableBodyFat) weightKg * (1.0 - bodyFat / 100.0) else weightKg
    val proteinReferenceKg = if (hasUsableBodyFat) leanMassKg else weightKg
    val proteinPerKg = when {
        goal.contains("cut", ignoreCase = true) || goal.contains("fat", ignoreCase = true) || goal.contains("lose", ignoreCase = true) -> 2.2
        goal.contains("bulk", ignoreCase = true) || goal.contains("gain", ignoreCase = true) -> 2.0
        else -> 2.0
    }
    val proteinUpperBound = if (hasUsableBodyFat) (weightKg * 1.9).roundToInt() else (weightKg * 2.2).roundToInt()
    val proteinTarget = (proteinReferenceKg * proteinPerKg)
        .roundToInt()
        .coerceIn(90, proteinUpperBound.coerceAtLeast(90))
    val fatMinimum = max(45, (weightKg * 0.55).roundToInt())
    val fatPreferred = (weightKg * 0.7).roundToInt()
    val fatCalorieCap = (targetCalories * 0.35 / 9.0).roundToInt()
    val fatTarget = min(fatPreferred, fatCalorieCap).coerceAtLeast(fatMinimum)
    val carbsTarget = ((targetCalories - (proteinTarget * 4) - (fatTarget * 9)) / 4.0).roundToInt().coerceAtLeast(0)
    return GoalBaseline(
        bmr = bmr,
        activityMultiplier = activityMultiplier,
        maintenanceCalories = maintenanceCalories,
        targetCalories = targetCalories,
        tefCalories = (targetCalories * TefRate).roundToInt(),
        proteinTarget = proteinTarget,
        carbsTarget = carbsTarget,
        fatTarget = fatTarget,
    )
}

fun estimateStrengthTrainingCalories(durationSeconds: Long): Int =
    ((durationSeconds / 60.0) * 5.0).roundToInt()

fun estimateNeatCaloriesFromSteps(steps: Int): Int = (steps * 0.04).roundToInt()

fun buildEnergyBalance(
    profile: UserProfile,
    caloriesIn: Double,
    steps: Int,
    workoutCalories: Int,
): EnergyBalanceSnapshot {
    val bmr = mifflinStJeorBmr(
        weightKg = profile.weight,
        heightCm = profile.height,
        age = profile.age,
        sex = profile.sex,
    )
    val caloriesInRounded = caloriesIn.roundToInt()
    val tefCalories = (caloriesInRounded * TefRate).roundToInt()
    val neatCalories = estimateNeatCaloriesFromSteps(steps)
    val caloriesOut = bmr + tefCalories + neatCalories + workoutCalories
    return EnergyBalanceSnapshot(
        caloriesIn = caloriesInRounded,
        caloriesOut = caloriesOut,
        balance = caloriesInRounded - caloriesOut,
        bmr = bmr,
        tefCalories = tefCalories,
        neatCalories = neatCalories,
        workoutCalories = workoutCalories,
    )
}

fun suggestMealType(timestampMillis: Long): MealType {
    val hour = Instant.ofEpochMilli(timestampMillis)
        .atZone(ZoneId.systemDefault())
        .hour
    return when (hour) {
        in 5..10 -> MealType.BREAKFAST
        in 11..15 -> MealType.LUNCH
        in 16..21 -> MealType.DINNER
        else -> MealType.SNACK
    }
}
