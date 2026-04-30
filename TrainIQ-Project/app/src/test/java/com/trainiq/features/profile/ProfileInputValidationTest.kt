package com.trainiq.features.profile

import com.trainiq.domain.model.BiologicalSex
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileInputValidationTest {
    @Test
    fun validateProfileInput_withInvalidFields_returnsSpecificUserFacingMessages() {
        assertValidationError(name = "", expectedMessage = "Naam is verplicht.")
        assertValidationError(age = "0", expectedMessage = "Leeftijd moet tussen 1 en 120 zijn.")
        assertValidationError(height = "79.9", expectedMessage = "Lengte moet tussen 80 en 250 cm zijn.")
        assertValidationError(weight = "300.1", expectedMessage = "Gewicht moet tussen 30 en 300 kg zijn.")
        assertValidationError(bodyFat = "NaN", expectedMessage = "Vetpercentage moet tussen 0 en 100% zijn.")
        assertValidationError(activityLevel = "Mostly desk, sometimes gym", expectedMessage = "Kies een activiteitsniveau.")
        assertValidationError(goal = " ", expectedMessage = "Doel is verplicht.")
    }

    @Test
    fun validateProfileInput_withInvalidFields_returnsFieldIdentifiers() {
        assertValidationError(name = "", expectedField = ProfileInputField.Name)
        assertValidationError(age = "0", expectedField = ProfileInputField.Age)
        assertValidationError(height = "79.9", expectedField = ProfileInputField.Height)
        assertValidationError(weight = "300.1", expectedField = ProfileInputField.Weight)
        assertValidationError(bodyFat = "NaN", expectedField = ProfileInputField.BodyFat)
        assertValidationError(activityLevel = "Mostly desk, sometimes gym", expectedField = ProfileInputField.ActivityLevel)
        assertValidationError(goal = " ", expectedField = ProfileInputField.Goal)
    }

    @Test
    fun validateProfileInput_withValidInput_returnsValidatedInput() {
        val result = validateProfileInput(
            name = " Sam ",
            height = "180,5",
            weight = "80,2",
            bodyFat = "15,5",
            age = "34",
            sex = BiologicalSex.MALE,
            activityLevel = " Gemiddeld actief ",
            goal = " Lean bulk ",
        )

        assertTrue(result is ProfileInputValidationResult.Valid)
        val input = (result as ProfileInputValidationResult.Valid).input
        assertEquals("Sam", input.name)
        assertEquals(180.5, input.height, 0.0)
        assertEquals("Gemiddeld actief", input.activityLevel)
        assertEquals("Lean bulk", input.goal)
    }

    @Test
    fun buildValidatedProfileInput_withKnownActivityLevel_returnsTrimmedInput() {
        val input = validInput(activityLevel = " Gemiddeld actief ")

        assertNotNull(input)
        assertEquals("Gemiddeld actief", input?.activityLevel)
    }

    @Test
    fun buildValidatedProfileInput_acceptsLegacyEnglishActivityLevelForSavedProfiles() {
        val input = validInput(activityLevel = " Moderately active ")

        assertNotNull(input)
        assertEquals("Moderately active", input?.activityLevel)
    }

    @Test
    fun buildValidatedProfileInput_withUnknownActivityLevel_returnsNull() {
        val input = validInput(activityLevel = "Mostly desk, sometimes gym")

        assertNull(input)
    }

    @Test
    fun buildValidatedProfileInput_acceptsInclusiveRealisticRanges() {
        val minimum = validInput(height = "80,0", weight = "30,0", bodyFat = "0,0", age = "1")
        val maximum = validInput(height = "250.0", weight = "300.0", bodyFat = "100.0", age = "120")

        assertNotNull(minimum)
        assertNotNull(maximum)
    }

    @Test
    fun buildValidatedProfileInput_rejectsValuesOutsideRealisticRanges() {
        assertNull(validInput(height = "79.9"))
        assertNull(validInput(height = "250.1"))
        assertNull(validInput(weight = "29.9"))
        assertNull(validInput(weight = "300.1"))
        assertNull(validInput(bodyFat = "-0.1"))
        assertNull(validInput(bodyFat = "100.1"))
        assertNull(validInput(age = "0"))
        assertNull(validInput(age = "121"))
    }

    @Test
    fun buildValidatedProfileInput_rejectsNonFiniteNumbers() {
        assertNull(validInput(height = "NaN"))
        assertNull(validInput(weight = "Infinity"))
    }

    private fun assertValidationError(
        name: String = "Sam",
        height: String = "180",
        weight: String = "80",
        bodyFat: String = "15",
        age: String = "34",
        sex: BiologicalSex = BiologicalSex.MALE,
        activityLevel: String = "Moderately active",
        goal: String = "Lean bulk",
        expectedMessage: String? = null,
        expectedField: ProfileInputField? = null,
    ) {
        val result = validateProfileInput(
            name = name,
            height = height,
            weight = weight,
            bodyFat = bodyFat,
            age = age,
            sex = sex,
            activityLevel = activityLevel,
            goal = goal,
        )

        assertTrue(result is ProfileInputValidationResult.Invalid)
        val error = (result as ProfileInputValidationResult.Invalid).error
        expectedMessage?.let { assertEquals(it, error.message) }
        expectedField?.let { assertEquals(it, error.field) }
    }

    private fun validInput(
        name: String = "Sam",
        height: String = "180",
        weight: String = "80",
        bodyFat: String = "15",
        age: String = "34",
        sex: BiologicalSex = BiologicalSex.MALE,
        activityLevel: String = "Moderately active",
        goal: String = "Lean bulk",
    ) = buildValidatedProfileInput(
        name = name,
        height = height,
        weight = weight,
        bodyFat = bodyFat,
        age = age,
        sex = sex,
        activityLevel = activityLevel,
        goal = goal,
    )
}
