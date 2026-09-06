package com.trainiq.features.workout

import org.junit.Assert.*
import org.junit.Test

class ExercisePlanValidationTest {
    @Test fun blanksRetainDefaultsAndCommaDecimalsRemainSupported() {
        assertEquals(ExercisePlanInput(3, "8-12", 90, 0.0, 0.0), parseExercisePlanInput(" ", "", "", "", ""))
        assertEquals(ExercisePlanInput(20, "AMRAP", 900, 1000.0, 10.0), parseExercisePlanInput("20", "AMRAP", "900", "1000", "10"))
        assertEquals(82.5, parseExercisePlanInput("1", "5", "0", "82,5", "7,5")!!.targetWeightKg, 0.0)
    }

    @Test fun malformedNonblankValuesNeverBecomeDefaults() {
        listOf("oops", "99999999999999999999999").forEach { invalid ->
            assertNull(parseExercisePlanInput(invalid, "8-12", "90", "80", "8"))
            assertNull(parseExercisePlanInput("3", "8-12", invalid, "80", "8"))
        }
        listOf("oops", "80..5", "NaN", "Infinity", "-1", "1001").forEach {
            assertNull(parseExercisePlanInput("3", "8-12", "90", it, "8"))
        }
        listOf("oops", "7,,5", "NaN", "Infinity", "-1", "11").forEach {
            assertNull(parseExercisePlanInput("3", "8-12", "90", "80", it))
        }
        assertNull(parseExercisePlanInput("21", "8-12", "90", "80", "8"))
        assertNull(parseExercisePlanInput("3", "8-12", "901", "80", "8"))
    }

    @Test fun platePreviewUsesEnteredCommaWeightAndNeverReplacesInvalidDraftWithSuggestion() {
        assertEquals(82.5f, platePreviewWeight("82,5", 60.0))
        assertEquals(60f, platePreviewWeight("", 60.0))
        listOf("bad", "1e99", "Infinity", "NaN", "1001", "-1").forEach {
            assertNull(platePreviewWeight(it, 60.0))
        }
        assertNull(platePreviewWeight("", Double.POSITIVE_INFINITY))
    }
}
