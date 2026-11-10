package com.trainiq.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class EnergyMathTest {
    @Test
    fun dutchGoalsUseTheSameCalculationAsTheirEnglishEquivalent() {
        listOf("Spieropbouw" to "lean bulk", "Vetverlies" to "fat loss", "Afvallen" to "fat loss")
            .forEach { (dutch, english) ->
                fun baseline(goal: String) = buildGoalBaseline(180.0, 90.0, 25.0, 30,
                    BiologicalSex.MALE, "Gemiddeld actief", goal)
                assertEquals(dutch, baseline(english), baseline(dutch))
            }
    }

    @Test
    fun manualCalorieBudgetCannotBeExceededByProteinAndFatTargets() {
        val result = buildGoalBaseline(180.0, 150.0, 20.0, 30,
            BiologicalSex.MALE, "Gemiddeld actief", "fat loss", 1200)
        assertEquals(1200, result.targetCalories)
        assertTrue(abs(result.targetCalories -
            (result.proteinTarget * 4 + result.fatTarget * 9 + result.carbsTarget * 4)) <= 2)
    }

    @Test
    fun macroBudgetsStayConsistentAcrossAcceptedManualTargetsAndWeights() {
        for (weight in listOf(30.0, 80.0, 150.0, 300.0)) {
            for (bodyFat in listOf(0.0, 20.0, 60.0)) {
                for (target in listOf(1200, 2000, 6000)) {
                    val result = buildGoalBaseline(180.0, weight, bodyFat, 30,
                        BiologicalSex.MALE, "Gemiddeld actief", "fat loss", target)
                    assertTrue(result.proteinTarget >= 0 && result.fatTarget >= 0 && result.carbsTarget >= 0)
                    assertTrue("$weight/$bodyFat/$target", abs(target -
                        (result.proteinTarget * 4 + result.fatTarget * 9 + result.carbsTarget * 4)) <= 2)
                }
            }
        }
    }

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
