package com.trainiq.flow

import android.content.Context
import android.content.Intent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.trainiq.MainActivity
import com.trainiq.core.datastore.OnboardingPreferences
import com.trainiq.core.datastore.UserPreferencesRepository
import com.trainiq.testing.resetTrainIqAndroidTestDatabase
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TrainIqFlowSmokeInstrumentedTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    private lateinit var context: Context

    @Before
    fun resetSafeLocalState() {
        context = ApplicationProvider.getApplicationContext()
        resetTrainIqAndroidTestDatabase(context)
        runBlocking {
            UserPreferencesRepository(context).saveOnboardingPreferences(OnboardingPreferences())
            UserPreferencesRepository(context).setAiEnabled(false)
        }
    }

    @Test
    fun cleanFirstRunTopLevelFlowExposesGuidanceAndFallbacks() {
        ActivityScenario.launch<MainActivity>(
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        ).use {
            skipFirstRunSetup()
            assertNavigationItemVisible("Start")
            assertNavigationItemVisible("Training")
            assertNavigationItemVisible("Voeding")
            assertNavigationItemVisible("Coach")
            assertNavigationItemVisible("Instellingen")

            assertAnyVisible("Instellen starten", "Profiel invullen", "Health Connect koppelen")

            tapNavigationItem("Training")
            waitForText("Train", checkpoint = "Training tab content")
            assertVisible("Nieuwe routine")

            tapNavigationItem("Voeding")
            waitForText("Voeding loggen", checkpoint = "Voeding tab content")
            assertAnyVisible("Voedingsdag", "Maaltijdconcept", "Maaltijd scannen")
            tapNavigationItem("Coach")
            waitForText("Coach", checkpoint = "Coach tab content")
            assertVisible("Profiel instellen")

            tapNavigationItem("Instellingen")
            waitForText("Instellingen", checkpoint = "Settings tab content")
            assertExists("Health Connect")
            assertExists("AI / Providers")

            tapNavigationItem("Coach")
            tap("Lichaamsmetingen openen")
            waitForText("Lichaam & voortgang", checkpoint = "Progress via Coach")
            assertVisible("Lichaam & voortgang")
        }
    }

    @Test
    fun progressBackStackAndTopLevelNavigationSurviveActivityRecreation() {
        ActivityScenario.launch<MainActivity>(
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        ).use { scenario ->
            skipFirstRunSetup()

            tapNavigationItem("Coach")
            tap("Lichaamsmetingen openen")
            waitForText("Lichaam & voortgang", checkpoint = "Progress opened from Coach")

            scenario.recreate()
            waitForText("Lichaam & voortgang", checkpoint = "Progress after activity recreation")
            assertVisible("Lichaam & voortgang")

            scenario.onActivity { activity ->
                activity.onBackPressedDispatcher.onBackPressed()
            }
            compose.waitForIdle()
            waitForText("Lichaamsmetingen openen", checkpoint = "back to Coach after recreation")
            assertVisible("Lichaamsmetingen openen")

            tapNavigationItem("Training")
            waitForText("Train", checkpoint = "Training after recreation/back")
            assertVisible("Nieuwe routine")

            scenario.recreate()
            waitForText("Train", checkpoint = "Training after second recreation")
            assertVisible("Nieuwe routine")
        }
    }

    private fun skipFirstRunSetup() {
        waitForText("Welkom bij TrainIQ")
        tap("Later afronden")
        waitForText("Stap 1 van 6")
        compose.onNodeWithText("Later afronden").performClick()
        compose.waitUntil(10_000) {
            compose.onAllNodes(navigationItem("Start")).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun tap(text: String) {
        scrollUntilText(text)
        compose.onAllNodesWithText(text, substring = true)[0].performClick()
    }

    private fun tapNavigationItem(text: String) {
        compose.onNode(navigationItem(text)).performClick()
    }

    private fun navigationItem(text: String) =
        (hasText(if (text == "Instellingen") "Meer" else text) or hasContentDescription(text)) and hasClickAction()

    private fun assertVisible(text: String) {
        scrollUntilText(text)
        compose.onAllNodesWithText(text, substring = true)[0].assertIsDisplayed()
    }

    private fun assertNavigationItemVisible(text: String) {
        compose.onNode(navigationItem(text)).assertIsDisplayed()
    }

    private fun assertExists(text: String) {
        scrollUntilText(text)
        check(compose.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty()) {
            "Text not found: $text"
        }
    }

    private fun assertAnyVisible(vararg texts: String) {
        scrollUntilAnyText(*texts)
        val visibleText = texts.firstOrNull { text ->
            compose.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty()
        }
        check(visibleText != null) { "None of these texts were visible: ${texts.joinToString()}" }
        compose.onAllNodesWithText(visibleText, substring = true)[0].assertIsDisplayed()
    }

    private fun waitForText(text: String, checkpoint: String = text) {
        val found = runCatching {
            compose.waitUntil(timeoutMillis = 10_000L) {
                compose.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty()
            }
            true
        }.getOrElse { false }
        check(found) {
            "Timed out waiting for '$text' at checkpoint '$checkpoint'"
        }
    }

    private fun scrollUntilText(text: String) {
        scrollUntilAnyText(text)
    }

    private fun scrollUntilAnyText(vararg texts: String) {
        texts.forEach { text ->
            runCatching {
                compose.onNodeWithText(text).performScrollTo()
            }
            if (compose.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty()) return
            runCatching {
                compose.onNode(hasScrollAction()).performScrollToNode(hasText(text))
            }
            if (compose.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty()) return
        }
    }
}
