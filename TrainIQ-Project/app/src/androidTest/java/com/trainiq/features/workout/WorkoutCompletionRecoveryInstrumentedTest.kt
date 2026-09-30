package com.trainiq.features.workout

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.AccessibilityManager
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.*
import androidx.compose.ui.unit.Density
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.trainiq.core.theme.TrainIqTheme
import com.trainiq.domain.model.*
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalTestApi::class, ExperimentalComposeUiApi::class)
class WorkoutCompletionRecoveryInstrumentedTest {
    @Test fun semanticStayActionPausesWithoutTouchOrScroll() = runComposeUiTest {
        var homes = 0
        setContent { TrainIqTheme { WorkoutCompletionScreen(WorkoutCompletionUiState.Success(summary()), {}, { homes++ }) } }
        onNodeWithText("Op scherm blijven").assertIsDisplayed()
            .performSemanticsAction(SemanticsActions.OnClick) { it() }
        mainClock.advanceTimeBy(13_000)
        runOnIdle { assertEquals(0, homes) }
        onNodeWithText("Op scherm blijven").assertDoesNotExist()
        captureCompletionEvidence("paused")
    }

    @Test fun keyboardInteractionPausesReturn() = runComposeUiTest {
        var homes = 0
        lateinit var inputModeManager: InputModeManager
        setContent {
            inputModeManager = LocalInputModeManager.current
            TrainIqTheme { WorkoutCompletionScreen(WorkoutCompletionUiState.Success(summary()), {}, { homes++ }) }
        }
        val instrumentation = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
        // This touch-first fixture establishes and restores its platform input baseline.
        instrumentation.setInTouchMode(true)
        waitUntil(timeoutMillis = 5_000) { inputModeManager.inputMode == InputMode.Touch }
        val originalInputMode = runOnIdle { inputModeManager.inputMode }
        try {
            runOnIdle { org.junit.Assert.assertTrue(inputModeManager.requestInputMode(InputMode.Keyboard)) }
            onNodeWithText("Op scherm blijven").performSemanticsAction(SemanticsActions.RequestFocus) {
                org.junit.Assert.assertTrue("Stay action must receive keyboard focus before key dispatch", it())
            }
            onNodeWithText("Op scherm blijven").assertIsFocused().performKeyInput { pressKey(Key.DirectionDown) }
            mainClock.advanceTimeBy(13_000)
            runOnIdle { assertEquals(0, homes) }
        } finally {
            // Keyboard mode belongs to this fixture; later touch tests must inherit their original mode.
            instrumentation.setInTouchMode(originalInputMode == InputMode.Touch)
            waitUntil(timeoutMillis = 5_000) { inputModeManager.inputMode == originalInputMode }
        }
    }

    @Test fun accessibilityRecommendedIndefiniteTimeoutKeepsSummary() = runComposeUiTest {
        var homes = 0
        val manager = object : AccessibilityManager {
            override fun calculateRecommendedTimeoutMillis(originalTimeoutMillis: Long, containsIcons: Boolean,
                containsText: Boolean, containsControls: Boolean): Long = Long.MAX_VALUE
        }
        setContent { TrainIqTheme {
            CompositionLocalProvider(LocalAccessibilityManager provides manager) {
                WorkoutCompletionScreen(WorkoutCompletionUiState.Success(summary()), {}, { homes++ })
            }
        } }
        mainClock.advanceTimeBy(13_000)
        runOnIdle { assertEquals(0, homes) }
    }

    @Test fun accessibilityRecommendedReadingTimeIsNeverRoundedDown() = runComposeUiTest {
        var homes = 0
        val manager = object : AccessibilityManager {
            override fun calculateRecommendedTimeoutMillis(originalTimeoutMillis: Long, containsIcons: Boolean,
                containsText: Boolean, containsControls: Boolean): Long = 30_500L
        }
        setContent { TrainIqTheme {
            CompositionLocalProvider(LocalAccessibilityManager provides manager) {
                WorkoutCompletionScreen(WorkoutCompletionUiState.Success(summary()), {}, { homes++ })
            }
        } }
        mainClock.autoAdvance = false
        mainClock.advanceTimeBy(30_000)
        runOnIdle { assertEquals(0, homes) }
        mainClock.advanceTimeBy(2_000)
        runOnIdle { assertEquals(1, homes) }
    }

    @Test fun backgroundDoesNotExitAndResumeProvidesFreshReadingTime() = runComposeUiTest {
        var homes = 0
        val owner = object : LifecycleOwner {
            override val lifecycle = LifecycleRegistry.createUnsafe(this)
        }
        runOnIdle { owner.lifecycle.currentState = Lifecycle.State.RESUMED }
        setContent { TrainIqTheme {
            CompositionLocalProvider(LocalLifecycleOwner provides owner) {
                WorkoutCompletionScreen(WorkoutCompletionUiState.Success(summary()), {}, { homes++ })
            }
        } }
        mainClock.autoAdvance = false
        mainClock.advanceTimeBy(5_000)
        runOnIdle { owner.lifecycle.currentState = Lifecycle.State.CREATED }
        mainClock.advanceTimeBy(20_000)
        runOnIdle { assertEquals(0, homes); owner.lifecycle.currentState = Lifecycle.State.RESUMED }
        mainClock.advanceTimeByFrame()
        mainClock.advanceTimeBy(11_000)
        runOnIdle { assertEquals(0, homes) }
        mainClock.advanceTimeBy(2_000)
        runOnIdle { assertEquals(1, homes); owner.lifecycle.currentState = Lifecycle.State.DESTROYED }
    }

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

    @Test fun completionActionsHaveEqualBoundsAtLargeFontScale() = runComposeUiTest {
        setContent {
            TrainIqTheme {
                CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 1.5f)) {
                    WorkoutCompletionScreen(WorkoutCompletionUiState.Success(summary()), {}, {})
                }
            }
        }
        onNode(hasScrollToIndexAction()).performScrollToIndex(4)
        onNodeWithText("Terug naar krachttraining").performScrollTo()
        val training = onNodeWithText("Terug naar krachttraining").fetchSemanticsNode().boundsInRoot
        val home = onNodeWithText("Naar start").fetchSemanticsNode().boundsInRoot
        assertEquals(training.width, home.width, 1f)
        assertEquals(training.height, home.height, 1f)
        captureCompletionEvidence("large-font-actions")
    }

    @Test fun completionMetricsRemainCompleteAtLargeFontScale() = runComposeUiTest {
        setContent {
            TrainIqTheme {
                CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 1.5f)) {
                    WorkoutCompletionScreen(WorkoutCompletionUiState.Success(summary()), {}, {})
                }
            }
        }
        onNode(hasScrollToIndexAction()).performScrollToIndex(2)
        listOf("Oefeningen" to "1", "Volume" to "100 kg").forEach { (label, value) ->
            val text = if (label == "Oefeningen") label else value
            val metric = hasAnyAncestor(hasContentDescription(statusMetricContentDescription(label, value)))
            val node = onNode(hasText(text) and metric, useUnmergedTree = true).performScrollTo().assertIsDisplayed()
            val layouts = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
            node.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
            assertEquals(1, layouts.size)
            val layout = layouts.single()
            assertEquals("Complete short metric stays on one line: $text", 1, layout.lineCount)
            org.junit.Assert.assertFalse("Metric height fits: $text", layout.multiParagraph.height > layout.size.height + 1)
            org.junit.Assert.assertFalse("Metric width fits: $text", layout.getLineRight(0) - layout.getLineLeft(0) > layout.size.width + 1)
            org.junit.Assert.assertFalse("Metric is not ellipsized: $text", layout.isLineEllipsized(0))
        }
        captureCompletionEvidence("large-font-metrics")
    }

    private fun captureCompletionEvidence(name: String) {
        val descriptor = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().uiAutomation
            .executeShellCommand("screencap -p /data/local/tmp/trainiq-completion-$name.png")
        android.os.ParcelFileDescriptor.AutoCloseInputStream(descriptor).use { it.readBytes() }
    }

    private fun summary() = WorkoutCompletionSummary(
        1L, "Training", 0L, 1000L, 1L, 1, 1, 100.0, 0, "100 kg",
        WorkoutDebrief("Opgeslagen", "", "", "", 75, "MAINTAIN"), "Lokaal", "", emptyList())
}
