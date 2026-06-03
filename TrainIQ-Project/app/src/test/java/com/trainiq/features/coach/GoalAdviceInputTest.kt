package com.trainiq.features.coach

import com.trainiq.domain.model.BiologicalSex
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GoalAdviceInputTest {
    @Test
    fun buildGoalAdviceInput_withEquivalentNumericFormatting_returnsSameInput() {
        val first = buildGoalAdviceInput(
            name = " Sam ",
            height = "180",
            weight = "80",
            bodyFat = "15",
            age = "34",
            sex = BiologicalSex.MALE,
            activityLevel = " Moderately active ",
            goal = " Lean bulk ",
        )
        val second = buildGoalAdviceInput(
            name = "Sam",
            height = "180.0",
            weight = "80.0",
            bodyFat = "15.0",
            age = "34",
            sex = BiologicalSex.MALE,
            activityLevel = "Moderately active",
            goal = "Lean bulk",
        )

        assertEquals(first, second)
    }

    @Test
    fun buildGoalAdviceInput_afterWeightChange_noLongerMatchesAdviceInput() {
        val adviceInput = buildGoalAdviceInput(
            name = "Sam",
            height = "180",
            weight = "80",
            bodyFat = "15",
            age = "34",
            sex = BiologicalSex.MALE,
            activityLevel = "Moderately active",
            goal = "Lean bulk",
        )
        val changedInput = buildGoalAdviceInput(
            name = "Sam",
            height = "180",
            weight = "90",
            bodyFat = "15",
            age = "34",
            sex = BiologicalSex.MALE,
            activityLevel = "Moderately active",
            goal = "Lean bulk",
        )

        assertNotEquals(adviceInput, changedInput)
    }

    @Test
    fun buildGoalAdviceInput_withMissingRequiredField_returnsNull() {
        val input = buildGoalAdviceInput(
            name = "",
            height = "180",
            weight = "80",
            bodyFat = "15",
            age = "34",
            sex = BiologicalSex.MALE,
            activityLevel = "Moderately active",
            goal = "Lean bulk",
        )

        assertNull(input)
    }

    @Test
    fun buildGoalAdviceInput_withDecimalComma_returnsParsedInput() {
        val input = buildGoalAdviceInput(
            name = "Sam",
            height = "180,5",
            weight = "80,2",
            bodyFat = "15,5",
            age = "34",
            sex = BiologicalSex.MALE,
            activityLevel = "Moderately active",
            goal = "Lean bulk",
        )

        assertEquals(180.5, input?.height)
        assertEquals(80.2, input?.weight)
        assertEquals(15.5, input?.bodyFat)
    }

    @Test
    fun buildGoalAdviceInput_withImpossibleProfileValues_returnsNull() {
        val negativeWeight = buildGoalAdviceInput(
            name = "Sam",
            height = "180",
            weight = "-80",
            bodyFat = "15",
            age = "34",
            sex = BiologicalSex.MALE,
            activityLevel = "Moderately active",
            goal = "Lean bulk",
        )
        val impossibleBodyFat = buildGoalAdviceInput(
            name = "Sam",
            height = "180",
            weight = "80",
            bodyFat = "120",
            age = "34",
            sex = BiologicalSex.MALE,
            activityLevel = "Moderately active",
            goal = "Lean bulk",
        )

        assertNull(negativeWeight)
        assertNull(impossibleBodyFat)
    }

    @Test
    fun buildGoalAdviceInput_withInvalidAge_returnsNull() {
        val input = buildGoalAdviceInput(
            name = "Sam",
            height = "180",
            weight = "80",
            bodyFat = "15",
            age = "abc",
            sex = BiologicalSex.MALE,
            activityLevel = "Moderately active",
            goal = "Lean bulk",
        )

        assertNull(input)
    }

    @Test
    fun buildGoalAdviceInput_withNonFiniteNumbers_returnsNull() {
        val nanHeight = buildGoalAdviceInput(
            name = "Sam",
            height = "NaN",
            weight = "80",
            bodyFat = "15",
            age = "34",
            sex = BiologicalSex.MALE,
            activityLevel = "Moderately active",
            goal = "Lean bulk",
        )
        val infiniteWeight = buildGoalAdviceInput(
            name = "Sam",
            height = "180",
            weight = "Infinity",
            bodyFat = "15",
            age = "34",
            sex = BiologicalSex.MALE,
            activityLevel = "Moderately active",
            goal = "Lean bulk",
        )

        assertNull(nanHeight)
        assertNull(infiniteWeight)
    }

    @Test
    fun goalAdviceEnergyDifferenceLabel_usesDutchBalanceLabels() {
        assertEquals("Tekort", goalAdviceEnergyDifferenceLabel(-250))
        assertEquals("Overschot", goalAdviceEnergyDifferenceLabel(250))
        assertEquals("Balans", goalAdviceEnergyDifferenceLabel(0))
    }

    @Test
    fun cleanAdviceBulletText_removesRawMarkdownMarkers() {
        assertEquals("Houd eiwit hoog.", cleanAdviceBulletText("- Houd eiwit hoog."))
        assertEquals("Plan je volgende sessie.", cleanAdviceBulletText("* Plan je volgende sessie."))
    }

    @Test
    fun coachScreen_keepsWeeklyTrainingAndNutritionSectionsVisibleBehindProfileGate() {
        val source = File("src/main/java/com/trainiq/features/coach/CoachScreen.kt").readText()
        val profileReadyBody = source.substringAfter("} else item {").substringBefore("item {\n                        GoalAdviceInputCard")
        val weekReportCard = source.substringAfter("private fun WeekReportCard(").substringBefore("@Composable\nprivate fun BulletAdviceSurface")
        val weeklySourceLabels = source.substringAfter("private fun WeeklyReportSource.label()").substringBefore("internal data class GoalAdviceInput")

        assertTrue(profileReadyBody.contains("WeekReportCard(report = state.generatedReport, fallbackSummary = state.overview.weeklyReport)"))
        assertTrue(profileReadyBody.contains("onGenerateWeeklyReport()"))
        assertTrue(profileReadyBody.contains("if (state.isGeneratingReport) \"Rapport maken...\" else \"Weekrapport maken\""))
        assertTrue(profileReadyBody.contains("Text(\"Trainingsinzichten\""))
        assertTrue(profileReadyBody.contains("state.overview.trainingInsights.ifEmpty"))
        assertTrue(profileReadyBody.contains("Text(\"Voedingscoach\""))
        assertTrue(profileReadyBody.contains("text = state.overview.nutritionCoachMessage"))
        assertTrue(weekReportCard.contains("Text(\"Weekoverzicht\""))
        assertTrue(weekReportCard.contains("report?.source?.label() ?: \"Lokale analyse\""))
        assertTrue(weekReportCard.contains("BulletAdviceSurface(\"Hoogtepunten\", it.wins)"))
        assertTrue(weekReportCard.contains("BulletAdviceSurface(\"Aandachtspunten\", it.risks)"))
        assertTrue(weekReportCard.contains("Text(\"Volgende stap\""))
        assertTrue(weekReportCard.contains("BulletAdviceSurface(\"Onderbouwing\", it.rationaleBullets.take(3))"))
        assertTrue(weeklySourceLabels.contains("WeeklyReportSource.GEMINI_2_5_FLASH -> \"Gemini 2.5 Flash\""))
        assertTrue(weeklySourceLabels.contains("WeeklyReportSource.OPENAI -> \"OpenAI\""))
        assertTrue(weeklySourceLabels.contains("WeeklyReportSource.LOCAL_FALLBACK -> \"Lokale analyse\""))
    }

    @Test
    fun goalAdviceCardUsesWrappingWarmSectionsForMetricOutput() {
        val source = File("src/main/java/com/trainiq/features/coach/CoachScreen.kt").readText()
        val goalAdviceCard = source.substringAfter("private fun GoalAdviceCard(").substringBefore("@Composable\nprivate fun AdviceSurface")
        val adviceSurface = source.substringAfter("private fun AdviceSurface(").substringBefore("@Composable\nprivate fun WeekReportCard")

        assertTrue(goalAdviceCard.contains("FlowRow("))
        assertTrue(goalAdviceCard.contains("MetricPill(\"Eiwit\""))
        assertTrue(goalAdviceCard.contains("MetricPill(\"Onderhoud\""))
        assertFalse(goalAdviceCard.contains("horizontalArrangement = Arrangement.SpaceBetween"))
        assertTrue(adviceSurface.contains("MaterialTheme.trainIqColors.amber"))
    }
}
