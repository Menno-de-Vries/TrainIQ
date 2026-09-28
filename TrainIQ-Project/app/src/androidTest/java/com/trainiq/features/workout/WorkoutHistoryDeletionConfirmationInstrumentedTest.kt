package com.trainiq.features.workout

import androidx.compose.ui.test.*
import com.trainiq.core.theme.TrainIqTheme
import com.trainiq.domain.model.WorkoutSessionSummary
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class WorkoutHistoryDeletionConfirmationInstrumentedTest {
    @Test
    fun deleteRequiresConfirmationAndCanBeCancelled() = runComposeUiTest {
        var deletedSessionId: Long? = null
        setContent {
            TrainIqTheme {
                WorkoutHistoryCard(
                    session = WorkoutSessionSummary(
                        id = 42L,
                        date = 1_780_000_000_000L,
                        duration = 3_600L,
                        caloriesBurned = 0,
                        totalVolume = 120.0,
                        workoutName = "Push day QA",
                    ),
                    onDelete = { deletedSessionId = it },
                )
            }
        }

        onNodeWithText("Verwijderen").performClick()
        onNodeWithText("Training verwijderen?").assertIsDisplayed()
        onNodeWithText("Weet je zeker dat je ‘Push day QA’", substring = true).assertIsDisplayed()
        onNodeWithText("Deze actie kan niet ongedaan worden gemaakt.").assertIsDisplayed()
        onNodeWithText("Een al naar de AI-provider verstuurd verzoek kan niet worden teruggehaald.").assertIsDisplayed()
        assertEquals(null, deletedSessionId)

        onNodeWithText("Annuleren").performClick()
        onNodeWithText("Training verwijderen?").assertDoesNotExist()
        assertEquals(null, deletedSessionId)

        onNodeWithText("Verwijderen").performClick()
        onNodeWithText("Ja, verwijderen").performClick()
        assertEquals(42L, deletedSessionId)
    }
}
