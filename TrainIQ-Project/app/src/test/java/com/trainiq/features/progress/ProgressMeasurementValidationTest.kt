package com.trainiq.features.progress

import com.trainiq.domain.model.BodyMeasurement
import com.trainiq.core.ui.UiMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressMeasurementValidationTest {
    @Test
    fun validateProgressMeasurementInput_withCommaAndDotDecimals_returnsValidatedMeasurement() {
        val result = validateProgressMeasurementInput(
            weight = "80,5",
            bodyFat = "15.25",
            muscleMass = "42,75",
        )

        assertTrue(result is ProgressMeasurementValidationResult.Valid)
        val measurement = (result as ProgressMeasurementValidationResult.Valid).measurement
        assertEquals(80.5, measurement.weight, 0.0)
        assertEquals(15.25, measurement.bodyFat, 0.0)
        assertEquals(42.75, measurement.muscleMass, 0.0)
    }

    @Test
    fun validateProgressMeasurementInput_rejectsEmptyNanInfinityAndOutOfRangeValues() {
        assertValidationError(weight = "", expectedField = ProgressMeasurementField.Weight)
        assertValidationError(weight = "NaN", expectedField = ProgressMeasurementField.Weight)
        assertValidationError(weight = "Infinity", expectedField = ProgressMeasurementField.Weight)
        assertValidationError(weight = "29,9", expectedField = ProgressMeasurementField.Weight)
        assertValidationError(weight = "300,1", expectedField = ProgressMeasurementField.Weight)

        assertValidationError(bodyFat = "", expectedField = ProgressMeasurementField.BodyFat)
        assertValidationError(bodyFat = "NaN", expectedField = ProgressMeasurementField.BodyFat)
        assertValidationError(bodyFat = "Infinity", expectedField = ProgressMeasurementField.BodyFat)
        assertValidationError(bodyFat = "-0,1", expectedField = ProgressMeasurementField.BodyFat)
        assertValidationError(bodyFat = "100,1", expectedField = ProgressMeasurementField.BodyFat)

        assertValidationError(muscleMass = "", expectedField = ProgressMeasurementField.MuscleMass)
        assertValidationError(muscleMass = "NaN", expectedField = ProgressMeasurementField.MuscleMass)
        assertValidationError(muscleMass = "Infinity", expectedField = ProgressMeasurementField.MuscleMass)
        assertValidationError(muscleMass = "0,9", expectedField = ProgressMeasurementField.MuscleMass)
        assertValidationError(muscleMass = "200,1", expectedField = ProgressMeasurementField.MuscleMass)
    }

    @Test
    fun validateProgressMeasurementInput_acceptsInclusiveRanges() {
        val minimum = validateProgressMeasurementInput(weight = "30", bodyFat = "0", muscleMass = "1")
        val maximum = validateProgressMeasurementInput(weight = "300", bodyFat = "100", muscleMass = "200")

        assertTrue(minimum is ProgressMeasurementValidationResult.Valid)
        assertTrue(maximum is ProgressMeasurementValidationResult.Valid)
    }

    @Test
    fun validateProgressMeasurementInput_returnsDutchFieldMessages() {
        assertValidationError(weight = "abc", expectedMessage = "Gewicht moet tussen 30 en 300 kg zijn.")
        assertValidationError(bodyFat = "abc", expectedMessage = "Vetpercentage moet tussen 0 en 100% zijn.")
        assertValidationError(muscleMass = "abc", expectedMessage = "Spiermassa moet tussen 1 en 200 kg zijn.")
    }

    @Test
    fun estimatedOneRepMaxText_keepsUsefulDecimalPrecision() {
        assertEquals("102.6 kg", estimatedOneRepMaxText(102.6))
    }

    @Test
    fun measurementHistoryRows_areSortedOnceNewestFirst() {
        val older = BodyMeasurement(id = 1L, date = 100L, weight = 80.0, bodyFat = 15.0, muscleMass = 40.0)
        val newer = BodyMeasurement(id = 2L, date = 200L, weight = 81.0, bodyFat = 14.8, muscleMass = 40.5)

        assertEquals(listOf(newer, older), sortedMeasurementsForHistory(listOf(older, newer)))
    }

    @Test
    fun latestMeasurementHeroText_usesNewestRealMeasurementWithoutInventingData() {
        val older = BodyMeasurement(id = 1L, date = 100L, weight = 80.0, bodyFat = 15.0, muscleMass = 40.0)
        val newer = BodyMeasurement(id = 2L, date = 200L, weight = 81.2, bodyFat = 14.8, muscleMass = 40.5)

        assertEquals("81.2 kg", latestBodyWeightText(listOf(older, newer)))
        assertEquals("14.8%", latestBodyFatText(listOf(older, newer)))
        assertEquals("40.5 kg", latestMuscleMassText(listOf(older, newer)))
    }

    @Test
    fun latestMeasurementHeroText_usesEmptyPlaceholdersWhenNoMeasurementsExist() {
        assertEquals("-- kg", latestBodyWeightText(emptyList()))
        assertEquals("--%", latestBodyFatText(emptyList()))
        assertEquals("-- kg", latestMuscleMassText(emptyList()))
    }

    @Test
    fun deleteMeasurementActionLabel_isCompactForNarrowRows() {
        assertEquals("Verwijderen", deleteMeasurementActionLabel())
    }

    @Test
    fun weeklyLoadRatioCopyAvoidsFatigueAndRpeClaims() {
        assertEquals("Nog geen vergelijking", weeklyLoadRatioText(null))
        assertEquals("1.20×", weeklyLoadRatioText(1.2))
        assertTrue(weeklyLoadRatioSupportingText(null).contains("twee trainingsweken"))
        assertTrue(weeklyLoadRatioSupportingText(1.2).contains("maximaal drie voorgaande trainingsweken"))

        val source = java.io.File("src/main/java/com/trainiq/features/progress/ProgressScreen.kt").readText()
        assertFalse(source.contains("Vermoeidheidsindex"))
        assertFalse(source.contains("volume + RPE"))
    }

    @Test
    fun progressUiState_usesSingleSealedScreenState() {
        val overview = com.trainiq.domain.model.ProgressOverview(
            measurements = emptyList(),
            weightTrend = emptyList(),
            bodyFatTrend = emptyList(),
            muscleMassTrend = emptyList(),
            strengthTrend = emptyList(),
            volumeTrend = emptyList(),
            estimatedOneRepMax = 0.0,
            weeklyLoadRatio = null,
        )

        val message = UiMessage("Meting opgeslagen.", id = 1L)
        assertEquals(ProgressUiState.Loading, progressUiState(observation = null, message = null))
        assertEquals(
            ProgressUiState.Error("Voortgang kon niet worden geladen."),
            progressUiState(observation = Result.failure(IllegalStateException("Room unavailable")), message = null),
        )
        assertEquals(
            ProgressUiState.Success(overview = overview, message = message),
            progressUiState(observation = Result.success(overview), message = message),
        )
    }

    @Test
    fun progressScreen_importsScalePhotoBeforeOpeningCameraScanner() {
        val source = java.io.File("src/main/java/com/trainiq/features/progress/ProgressScreen.kt").readText()

        assertTrue(source.contains("ActivityResultContracts.PickVisualMedia()"))
        assertTrue(source.contains("copyScannerImageFromUri(context, uri)"))
        assertTrue(source.contains("onAnalyzeImportedScalePhoto(path)"))
        assertTrue(source.contains("Text(scalePhotoImportLabel())"))
    }

    private fun assertValidationError(
        weight: String = "80",
        bodyFat: String = "15",
        muscleMass: String = "40",
        expectedField: ProgressMeasurementField? = null,
        expectedMessage: String? = null,
    ) {
        val result = validateProgressMeasurementInput(
            weight = weight,
            bodyFat = bodyFat,
            muscleMass = muscleMass,
        )

        assertTrue(result is ProgressMeasurementValidationResult.Invalid)
        val error = (result as ProgressMeasurementValidationResult.Invalid).error
        expectedField?.let { assertEquals(it, error.field) }
        expectedMessage?.let { assertEquals(it, error.message) }
    }
}
