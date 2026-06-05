package com.trainiq.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class EnergyMathTest {
    @Test
    fun buildGoalBaseline_withManualCalorieTargetKeepsBmrAndMaintenanceButRecalculatesMacros() {
        val automatic = buildGoalBaseline(
            heightCm = 195.0,
            weightKg = 107.2,
            bodyFat = 25.0,
            age = 30,
            sex = BiologicalSex.MALE,
            activityLevel = "Licht actief",
            goal = "fat loss",
        )

        val manual = buildGoalBaseline(
            heightCm = 195.0,
            weightKg = 107.2,
            bodyFat = 25.0,
            age = 30,
            sex = BiologicalSex.MALE,
            activityLevel = "Licht actief",
            goal = "fat loss",
            manualCalorieTarget = 3_050,
        )

        assertEquals(automatic.bmr, manual.bmr)
        assertEquals(automatic.maintenanceCalories, manual.maintenanceCalories)
        assertEquals(3_050, manual.targetCalories)
        assertEquals(automatic.proteinTarget, manual.proteinTarget)
        assertTrue(manual.carbsTarget > automatic.carbsTarget)
        assertTrue(abs(manual.targetCalories - (manual.proteinTarget * 4 + manual.carbsTarget * 4 + manual.fatTarget * 9)) <= 12)
    }

    @Test
    fun buildGoalBaseline_withBlankManualCalorieTargetUsesAutomaticGoalTarget() {
        val automatic = buildGoalBaseline(
            heightCm = 180.0,
            weightKg = 82.0,
            bodyFat = 16.0,
            age = 28,
            sex = BiologicalSex.MALE,
            activityLevel = "Gemiddeld actief",
            goal = "lean bulk",
        )

        val blankManual = buildGoalBaseline(
            heightCm = 180.0,
            weightKg = 82.0,
            bodyFat = 16.0,
            age = 28,
            sex = BiologicalSex.MALE,
            activityLevel = "Gemiddeld actief",
            goal = "lean bulk",
            manualCalorieTarget = null,
        )

        assertEquals(automatic.targetCalories, blankManual.targetCalories)
        assertEquals(automatic.carbsTarget, blankManual.carbsTarget)
    }
}
