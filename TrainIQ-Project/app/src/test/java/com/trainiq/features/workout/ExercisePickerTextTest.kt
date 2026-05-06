package com.trainiq.features.workout

import org.junit.Assert.assertEquals
import org.junit.Test

class ExercisePickerTextTest {
    @Test
    fun exercisePickerChromeUsesDutchLabels() {
        assertEquals("Oefening kiezen", exercisePickerTitle())
        assertEquals("Sluiten", exercisePickerCloseLabel())
        assertEquals("Oefeningen zoeken", exercisePickerSearchLabel())
        assertEquals("Aangepaste oefening toevoegen", exercisePickerAddCustomLabel(showCustomForm = false))
        assertEquals("Aangepaste oefening verbergen", exercisePickerAddCustomLabel(showCustomForm = true))
        assertEquals("Geen passende oefeningen gevonden.", exercisePickerEmptyText())
    }

    @Test
    fun customExerciseFormUsesDutchLabels() {
        assertEquals("Aangepaste oefening", customExerciseFormTitle())
        assertEquals("Naam oefening", customExerciseNameLabel())
        assertEquals("Spiergroep", customExerciseMuscleLabel())
        assertEquals("Materiaal", customExerciseEquipmentLabel())
        assertEquals("Aangepaste oefening gebruiken", customExerciseSubmitLabel())
    }

    @Test
    fun exercisePickerRowMetadataLocalizesCommonEnglishValues() {
        assertEquals("Schouders - Dumbbells", exercisePickerMetadataText("Shoulders", "Dumbbells"))
        assertEquals("Borst - Halterstang", exercisePickerMetadataText("Chest", "Barbell"))
        assertEquals("Hele lichaam - Lichaamsgewicht", exercisePickerMetadataText("Full body", "Bodyweight"))
    }
}
