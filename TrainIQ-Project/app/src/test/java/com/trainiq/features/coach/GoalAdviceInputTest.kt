package com.trainiq.features.coach

import com.trainiq.domain.model.BiologicalSex
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
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
}
