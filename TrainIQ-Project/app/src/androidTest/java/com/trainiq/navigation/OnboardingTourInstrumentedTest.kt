package com.trainiq.navigation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.trainiq.MainActivity
import com.trainiq.core.datastore.OnboardingPreferences
import com.trainiq.core.datastore.UserPreferencesRepository
import com.trainiq.features.onboarding.shouldShowGuidedTour
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OnboardingTourInstrumentedTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test
    fun firstRunShowsAllSixIntroductionsAndCompletionSurvivesRecreation() = withPreferences(OnboardingPreferences()) { preferences ->
        ActivityScenario.launch(MainActivity::class.java).use { activity ->
            waitForText("Welkom bij TrainIQ")
            compose.onNodeWithText("Stap 1 van 6").assertDoesNotExist()
            // The historical primary-setup skip does not skip the separate app tour.
            compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Later afronden"))
            compose.onNodeWithText("Later afronden").performScrollTo().performClick()
            val titles = listOf("Start", "Training", "Voeding", "Voortgang", "Coach", "Instellingen")
            titles.forEachIndexed { index, title ->
                val counter = "Stap ${index + 1} van 6"
                waitForText(counter)
                compose.onNode(hasText(title) and hasAnyAncestor(hasTestTag("guided-tour")))
                    .assertIsDisplayed()
                compose.onNodeWithText(counter).assertIsDisplayed()
                if (index == 2) {
                    activity.recreate()
                    waitForText(counter)
                    compose.onNodeWithText(counter).assertIsDisplayed()
                }
                compose.onNodeWithText(if (index == 5) "Tour afronden" else "Volgende").performClick()
            }
            compose.waitUntil(15_000) { runBlocking { preferences.getOnboardingPreferences().guidedTourCompleted } }
            activity.recreate()
            waitForText("Meer")
            compose.waitForIdle()
            compose.onNodeWithText("Stap 1 van 6").assertDoesNotExist()
            val stored = runBlocking { preferences.getOnboardingPreferences() }
            assertTrue(stored.completed)
            assertTrue(stored.guidedTourCompleted)
            assertFalse(stored.guidedTourSkipped)
        }
    }

    @Test
    fun legacyCompletedSetupOffersTourAndSkippingPersistsUntilExplicitReopen() = withPreferences(
        OnboardingPreferences(completed = true, goal = "Existing profile"),
    ) { preferences ->
        ActivityScenario.launch(MainActivity::class.java).use { activity ->
            waitForText("Stap 1 van 6")
            compose.onNodeWithText("Later afronden").performClick()
            compose.waitUntil(15_000) { runBlocking { preferences.getOnboardingPreferences().guidedTourSkipped } }
            activity.recreate()
            waitForText("TrainIQ")
            compose.waitForIdle()
            compose.onNodeWithText("Stap 1 van 6").assertDoesNotExist()
            assertFalse(shouldShowGuidedTour(runBlocking { preferences.getOnboardingPreferences() }))
            runBlocking { preferences.reopenOnboarding() }
            waitForText("Stap 1 van 6")
            assertTrue(runBlocking { preferences.getOnboardingPreferences().completed })
        }
    }

    private fun waitForText(text: String) {
        compose.waitUntil(30_000) { compose.onAllNodes(hasText(text)).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun withPreferences(initial: OnboardingPreferences, block: (UserPreferencesRepository) -> Unit) {
        val preferences = UserPreferencesRepository(InstrumentationRegistry.getInstrumentation().targetContext)
        val original = runBlocking { preferences.getOnboardingPreferences() }
        try {
            runBlocking { preferences.saveOnboardingPreferences(initial) }
            block(preferences)
        } finally {
            runBlocking { preferences.saveOnboardingPreferences(original) }
        }
    }
}
