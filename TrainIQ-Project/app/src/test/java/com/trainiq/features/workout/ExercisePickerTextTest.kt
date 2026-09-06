package com.trainiq.features.workout

import org.junit.Assert.assertEquals
import org.junit.Test

class ExercisePickerTextTest {
    @Test
    fun searchMatchesDisplayedDutchAndOriginalMetadataWithoutChangingOrder() {
        val bench = com.trainiq.domain.model.Exercise(1L, "Bench Press", "Chest", "Barbell")
        val squat = com.trainiq.domain.model.Exercise(2L, "Squat", "Legs", "Barbell")
        val rows = listOf(bench, squat)
        assertEquals(listOf(bench), filterPickerExercises(rows, "  BORST halterstang "))
        assertEquals(listOf(bench), filterPickerExercises(rows, "chest bench"))
        assertEquals(rows, filterPickerExercises(rows, "halterstang"))
        assertEquals(rows, filterPickerExercises(rows, "  "))
        assertEquals(emptyList<Any>(), filterPickerExercises(rows, "borst dumbbell"))
        assertEquals(true, matchesExerciseSearch(bench, exerciseSearchTerms("borst beginner"), "Beginner"))
        assertEquals(false, matchesExerciseSearch(bench, exerciseSearchTerms("rug beginner"), "Beginner"))
    }

    @Test
    fun exercisePickerChromeUsesDutchLabels() {
        assertEquals("Oefening kiezen", exercisePickerTitle())
        assertEquals("Sluiten", exercisePickerCloseLabel())
        assertEquals("Oefeningen zoeken", exercisePickerSearchLabel())
        assertEquals("Aangepaste oefening toevoegen", exercisePickerAddCustomLabel(showCustomForm = false))
        assertEquals("Aangepaste oefening verbergen", exercisePickerAddCustomLabel(showCustomForm = true))
        assertEquals(
            "Geen passende oefeningen gevonden. Voeg eventueel een aangepaste oefening toe.",
            exercisePickerEmptyText(),
        )
    }

    @Test
    fun customExerciseFormUsesDutchLabels() {
        assertEquals("Aangepaste oefening", customExerciseFormTitle())
        assertEquals("Naam oefening", customExerciseNameLabel())
        assertEquals("Spiergroep", customExerciseMuscleLabel())
        assertEquals("Materiaal", customExerciseEquipmentLabel())
        assertEquals("Aangepaste oefening gebruiken", customExerciseSubmitLabel())
        assertEquals(true, customExerciseFieldsStackVertically())
    }

    @Test
    fun exercisePickerRowMetadataLocalizesCommonEnglishValues() {
        assertEquals("Schouders - Dumbbells", exercisePickerMetadataText("Shoulders", "Dumbbells"))
        assertEquals("Borst - Halterstang", exercisePickerMetadataText("Chest", "Barbell"))
        assertEquals("Hele lichaam - Lichaamsgewicht", exercisePickerMetadataText("Full body", "Bodyweight"))
        assertEquals("Hamstrings - Dumbbells", exercisePickerMetadataText("Hamstrings", "Dumbbell"))
        assertEquals("Bilspieren - Kettlebells", exercisePickerMetadataText("Glutes", "Kettlebells"))
        assertEquals("Core - EZ-stang", exercisePickerMetadataText("Core", "EZ-Bar"))
    }
}
