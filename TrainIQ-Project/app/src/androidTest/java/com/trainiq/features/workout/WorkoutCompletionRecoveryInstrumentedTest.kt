package com.trainiq.features.workout

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import com.trainiq.core.theme.TrainIqTheme
import com.trainiq.domain.model.*
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class WorkoutCompletionRecoveryInstrumentedTest {
    @Test fun touchingSummaryCancelsAutomaticReturn() = runComposeUiTest {
        var homes = 0
        setContent { TrainIqTheme { WorkoutCompletionScreen(WorkoutCompletionUiState.Success(summary()), {}, { homes++ }) } }
        onNode(hasScrollToIndexAction()).performTouchInput { click() }
        mainClock.advanceTimeBy(13_000)
        runOnIdle { assertEquals(0, homes) }
    }

    @Test fun trainingReadErrorOffersReachableRetry() = runComposeUiTest {
        var retries = 0
        setContent { TrainIqTheme { WorkoutObservationError(
            com.trainiq.core.ui.ScreenUiState.Error("Training niet beschikbaar", "Trainingsgegevens konden niet worden geladen."),
            { retries++ },
        ) } }
        onNodeWithText("Opnieuw proberen").assertIsDisplayed().performClick()
        assertEquals(1, retries)
    }

    @Test fun loadingAndErrorStayVisibleAndRetryIsReachable() = runComposeUiTest {
        var state by mutableStateOf<WorkoutCompletionUiState>(WorkoutCompletionUiState.Loading)
        var homes = 0
        var retries = 0
        setContent { TrainIqTheme { WorkoutCompletionScreen(state, {}, { homes++ }, { retries++ }) } }
        mainClock.advanceTimeBy(13_000)
        assertEquals(0, homes)
        runOnIdle { state = WorkoutCompletionUiState.Error("Opslag tijdelijk niet beschikbaar") }
        mainClock.advanceTimeBy(13_000)
        assertEquals(0, homes)
        onNodeWithText("Opnieuw proberen").performScrollTo().performClick()
        assertEquals(1, retries)
    }

    @Test fun countdownStartsOnlyAfterSummaryIsLoaded() = runComposeUiTest {
        var state by mutableStateOf<WorkoutCompletionUiState>(WorkoutCompletionUiState.Loading)
        var homes = 0
        setContent { TrainIqTheme { WorkoutCompletionScreen(state, {}, { homes++ }) } }
        mainClock.advanceTimeBy(13_000)
        assertEquals(0, homes)
        mainClock.autoAdvance = false
        runOnIdle {
            state = WorkoutCompletionUiState.Success(summary())
        }
        mainClock.advanceTimeByFrame()
        mainClock.advanceTimeBy(11_000)
        assertEquals(0, homes)
        mainClock.advanceTimeBy(2_000)
        runOnIdle { assertEquals(1, homes) }
    }

    private fun summary() = WorkoutCompletionSummary(
        1L, "Training", 0L, 1000L, 1L, 1, 1, 100.0, 0, "100 kg",
        WorkoutDebrief("Opgeslagen", "", "", "", 75, "MAINTAIN"), "Lokaal", "", emptyList())
}
