package com.trainiq.features.progress

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.trainiq.core.theme.TrainIqTheme
import com.trainiq.core.util.toReadableDate
import com.trainiq.domain.model.BodyMeasurement
import com.trainiq.domain.model.ChartPoint
import com.trainiq.domain.model.ProgressOverview
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class ProgressDeleteConfirmationInstrumentedTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun deletingMeasurementRequiresConfirmationAndCancelPreservesIt() {
        var deletedMeasurementIds = emptyList<Long>()
        val measurement = sampleMeasurement()
        compose.setContent {
            TrainIqTheme {
                ProgressScreen(
                    uiState = ProgressUiState.Success(sampleOverview(measurement)),
                    onAddMeasurement = { _, _, _ -> },
                    onDeleteMeasurement = { deletedMeasurementIds = deletedMeasurementIds + it },
                    onRetry = {},
                    onDismissMessage = {},
                )
            }
        }

        compose.onNodeWithText("Historie").performClick()
        compose.onNodeWithText("Verwijderen").performScrollTo().performClick()

        compose.onNodeWithText("Meting verwijderen?").assertIsDisplayed()
        compose.onNodeWithText("Weet je zeker dat je deze meting wilt verwijderen?")
            .assertIsDisplayed()
        compose.onAllNodesWithText(measurementIdentificationText(measurement)).assertCountEquals(2)
        compose.onNodeWithText("Annuleren").performClick()

        compose.onNodeWithText("${measurement.date.toReadableDate()}: 80.0 kg, 15.0% vet, 40.0 kg spier")
            .performScrollTo()
            .assertIsDisplayed()
        assertEquals(emptyList<Long>(), deletedMeasurementIds)
        compose.onAllNodesWithText("Meting verwijderen?").assertCountEquals(0)
    }

    @Test
    fun confirmingMeasurementDeletionInvokesExistingCallbackWithItsId() {
        var deletedMeasurementIds = emptyList<Long>()
        val olderMeasurement = sampleMeasurement()
        val measurement = sampleMeasurement(
            id = 43L,
            date = Instant.parse("2024-03-15T13:00:00Z").toEpochMilli(),
            weight = 81.2,
            bodyFat = 16.1,
            muscleMass = 39.3,
        )
        compose.setContent {
            TrainIqTheme {
                ProgressScreen(
                    uiState = ProgressUiState.Success(sampleOverview(olderMeasurement, measurement)),
                    onAddMeasurement = { _, _, _ -> },
                    onDeleteMeasurement = { deletedMeasurementIds = deletedMeasurementIds + it },
                    onRetry = {},
                    onDismissMessage = {},
                )
            }
        }

        compose.onNodeWithText("Historie").performClick()
        compose.onAllNodesWithText("Verwijderen")[0].performScrollTo().performClick()
        compose.onNodeWithText("Meting verwijderen?").assertIsDisplayed()
        compose.onAllNodesWithText(measurementIdentificationText(measurement)).assertCountEquals(2)
        compose.onAllNodesWithText(measurementIdentificationText(olderMeasurement)).assertCountEquals(1)
        compose.onNodeWithText("Ja, verwijderen").performClick()

        assertEquals(listOf(measurement.id), deletedMeasurementIds)
    }

    private fun sampleMeasurement(
        id: Long = 42L,
        date: Long = Instant.parse("2024-03-15T12:00:00Z").toEpochMilli(),
        weight: Double = 80.0,
        bodyFat: Double = 15.0,
        muscleMass: Double = 40.0,
    ) = BodyMeasurement(
        id = id,
        date = date,
        weight = weight,
        bodyFat = bodyFat,
        muscleMass = muscleMass,
    )

    private fun sampleOverview(vararg measurements: BodyMeasurement): ProgressOverview {
        val measurement = measurements.first()
        return ProgressOverview(
            measurements = measurements.toList(),
            weightTrend = listOf(ChartPoint(measurement.date.toReadableDate(), measurement.weight)),
            bodyFatTrend = listOf(ChartPoint(measurement.date.toReadableDate(), measurement.bodyFat)),
            muscleMassTrend = listOf(ChartPoint(measurement.date.toReadableDate(), measurement.muscleMass)),
            strengthTrend = emptyList(),
            volumeTrend = emptyList(),
            estimatedOneRepMax = 0.0,
            weeklyLoadRatio = null,
        )
    }
}
