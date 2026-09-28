package com.trainiq.features.onboarding

import androidx.compose.ui.test.*
import com.trainiq.core.theme.TrainIqTheme
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class OnboardingAiPrivacyDisclosureInstrumentedTest {
    @Test
    fun aiPrivacyStepDisclosesDeferredWorkoutDebriefProcessing() = runComposeUiTest {
        setContent {
            TrainIqTheme {
                AiPrivacyStep(draft = OnboardingDraft(), onEvent = {})
            }
        }

        onNodeWithText("AI staat standaard uit.", substring = true).assertIsDisplayed()
        onNodeWithText("Na afronden van een workout", substring = true).assertIsDisplayed()
        onAllNodesWithText("op de achtergrond worden verwerkt.", substring = true).assertCountEquals(2)
        onNodeWithText("Met ingeschakelde AI en een opgeslagen providersleutel", substring = true).assertIsDisplayed()
        onNodeWithText("AI aanstaat en een providersleutel is opgeslagen", substring = true).assertIsDisplayed()
        onNodeWithText("starten verzoeken alleen door expliciete acties", substring = true).assertDoesNotExist()
    }
}
