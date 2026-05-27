package com.trainiq.flow

import android.content.Context
import android.content.Intent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
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
import com.trainiq.testing.resetTrainIqAndroidTestDatabase
import java.io.File
import org.junit.Assert.assertTrue
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
        deleteAppLocalFile("datastore/trainiq_preferences.preferences_pb")
    }

    @Test
    fun cleanFirstRunTopLevelFlowExposesGuidanceAndFallbacks() {
        ActivityScenario.launch<MainActivity>(
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        ).use { scenario ->
            waitForText("Start", checkpoint = "initial Start tab")
            assertNavigationItemVisible("Start")
            assertNavigationItemVisible("Training")
            assertNavigationItemVisible("Voeding")
            assertNavigationItemVisible("Coach")
            assertNavigationItemVisible("Meer")

            assertAnyVisible("Instellen starten", "Profiel invullen", "Health Connect koppelen")

            tapNavigationItem("Training")
            waitForText("Routine maken", checkpoint = "Training tab content")
            assertAnyVisible("Routine maken", "Start met een lege template")

            tapNavigationItem("Voeding")
            waitForText("Voeding loggen", checkpoint = "Voeding tab content")
            assertAnyVisible("Voedingsdag", "Maaltijdconcept", "Maaltijd scannen")
            tapNavigationItem("Coach")
            waitForText("Coach", checkpoint = "Coach tab content")
            assertAnyVisible("Gemini 2.5 Flash", "Lokale berekening opgeslagen", "Advies")

            tapNavigationItem("Meer")
            waitForText("Instellingen", checkpoint = "Meer tab content")
            assertVisible("Meer")
            assertExists("Voortgang openen")
            assertExists("Health Connect")
            assertExists("AI / Providers")

            tap("Voortgang openen")
            waitForText("Voortgang", checkpoint = "Voortgang via Meer")
            assertVisible("Voortgang")
        }
    }

    private fun deleteAppLocalFile(relativePath: String) {
        val dataRoot = context.filesDir.parentFile ?: return
        val target = File(dataRoot, relativePath).canonicalFile
        val root = dataRoot.canonicalFile
        assertTrue(target.path.startsWith(root.path))
        if (target.exists()) {
            target.delete()
        }
    }

    private fun tap(text: String) {
        scrollUntilText(text)
        compose.onAllNodesWithText(text, substring = true)[0].performClick()
    }

    private fun tapNavigationItem(text: String) {
        compose.onAllNodes(hasText(text, substring = false).and(hasClickAction()))[0].performClick()
    }

    private fun tapLast(text: String) {
        scrollUntilText(text)
        val matches = compose.onAllNodesWithText(text, substring = true).fetchSemanticsNodes()
        check(matches.isNotEmpty()) { "Text not found for tap: $text" }
        compose.onAllNodesWithText(text, substring = true)[matches.lastIndex].performClick()
    }

    private fun assertVisible(text: String) {
        scrollUntilText(text)
        compose.onAllNodesWithText(text, substring = true)[0].assertIsDisplayed()
    }

    private fun assertNavigationItemVisible(text: String) {
        compose.onAllNodes(hasText(text, substring = false).and(hasClickAction()))[0].assertIsDisplayed()
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
