package com.trainiq.features.progress

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import com.trainiq.core.theme.TrainIqTheme
import com.trainiq.domain.model.ProgressOverview
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ProgressPhotoDraftInstrumentedTest {
    @get:Rule val compose = createComposeRule()

    @Test fun manualEditCancelsPendingPhotoAndSurvivesRestorationWithoutLateOverwrite() {
        val gate = CompletableDeferred<String>()
        lateinit var session: ProgressPhotoImportSession
        var importedWeight by mutableStateOf<String?>(null)
        var delivered = 0
        val released = mutableListOf<String>()
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            val scope = rememberCoroutineScope()
            session = remember { ProgressPhotoImportSession(scope) { released.add(it) } }
            val pending by session.isPending.collectAsState()
            TrainIqTheme {
                ProgressScreen(
                    uiState = ProgressUiState.Success(emptyOverview(), isAnalyzingPhoto = pending),
                    onAddMeasurement = { _, _, _ -> }, onDeleteMeasurement = {}, onRetry = {}, onDismissMessage = {},
                    pendingScaleWeight = importedWeight,
                    onScaleResultConsumed = { importedWeight = null },
                    onCancelScalePhotoAnalysis = session::invalidate,
                )
            }
        }
        compose.runOnIdle {
            session.analyze(session.begin(), "synthetic", { withContext(NonCancellable) { gate.await() } },
                { delivered++; importedWeight = it }, { error("Unexpected failure") })
        }
        compose.onNodeWithText("Fotoanalyse annuleren").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Gewicht (kg)").performScrollTo().performTextInput("81,5")
        compose.onAllNodesWithText("Fotoanalyse annuleren").assertCountEquals(0)
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("Gewicht (kg)").performScrollTo().assertTextContains("81,5")
        compose.runOnIdle { gate.complete("70.0") }
        compose.waitUntil(5_000) { "synthetic" in released }
        compose.runOnIdle { assertEquals(0, delivered) }
        compose.onNodeWithText("Gewicht (kg)").assertTextContains("81,5")
    }

    @Test fun explicitCancelRetainsManualValuesAndNewPhotoCanStart() {
        lateinit var session: ProgressPhotoImportSession
        lateinit var rootView: android.view.View
        compose.setContent {
            rootView = androidx.compose.ui.platform.LocalView.current.rootView
            val scope = rememberCoroutineScope()
            session = remember { ProgressPhotoImportSession(scope) {} }
            val pending by session.isPending.collectAsState()
            TrainIqTheme {
                ProgressScreen(
                    uiState = ProgressUiState.Success(emptyOverview(), isAnalyzingPhoto = pending),
                    onAddMeasurement = { _, _, _ -> }, onDeleteMeasurement = {}, onRetry = {}, onDismissMessage = {},
                    onCancelScalePhotoAnalysis = session::invalidate,
                )
            }
        }
        compose.onNodeWithText("Gewicht (kg)").performScrollTo().performTextInput("82")
        compose.runOnIdle { session.begin() }
        compose.onNodeWithText("Fotoanalyse annuleren").performScrollTo().performClick()
        compose.onAllNodesWithText("Fotoanalyse annuleren").assertCountEquals(0)
        compose.onNodeWithText("Gewicht (kg)").performScrollTo().assertTextContains("82")
        // The real picker returns without an active field/IME. Use the production
        // scroll gesture to clear focus before simulating that next import result.
        // The generic component host can pan while IME is visible, unlike MainActivity.
        compose.onNode(hasScrollToIndexAction()).performTouchInput {
            swipeDown(startY = height - 160f, endY = height - 32f)
        }
        compose.waitUntil(5_000) {
            val insets = androidx.core.view.ViewCompat.getRootWindowInsets(rootView)
            insets?.isVisible(androidx.core.view.WindowInsetsCompat.Type.ime()) != true &&
                compose.onRoot().fetchSemanticsNode().boundsInRoot.top >= 0f
        }
        compose.runOnIdle { session.begin() }
        compose.runOnIdle { org.junit.Assert.assertTrue(session.isPending.value) }
        val cancel = compose.onNodeWithText("Fotoanalyse annuleren").performScrollTo()
        cancel.assertIsDisplayed().performTouchInput { click() }
        compose.onAllNodesWithText("Fotoanalyse annuleren").assertCountEquals(0)
        compose.onNodeWithText("Gewicht (kg)").performScrollTo().assertTextContains("82")
        compose.runOnIdle { org.junit.Assert.assertFalse(session.isPending.value) }
    }

    @Test fun semanticReturnIsAvailableForLoadingErrorAndSuccess() {
        var state by mutableStateOf<ProgressUiState>(ProgressUiState.Loading)
        var returns = 0
        compose.setContent { TrainIqTheme {
            ProgressScreen(state, { _, _, _ -> }, {}, {}, {}, onBack = { returns++ })
        } }
        compose.onNodeWithContentDescription("Terug naar Coach").assertIsDisplayed().performClick()
        compose.runOnIdle { state = ProgressUiState.Error("Tijdelijk niet beschikbaar") }
        compose.onNodeWithContentDescription("Terug naar Coach").assertIsDisplayed().performClick()
        compose.runOnIdle { state = ProgressUiState.Success(emptyOverview()) }
        compose.onNodeWithContentDescription("Terug naar Coach").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(3, returns) }
    }

    private fun emptyOverview() = ProgressOverview(emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), 0.0, null)
}
