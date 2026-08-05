package com.trainiq.flow

import android.content.Context
import android.content.Intent
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.trainiq.MainActivity
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
        context.deleteDatabase("trainiq.db")
        deleteAppLocalFile("datastore/trainiq_preferences.preferences_pb")
    }

    @Test
    fun cleanFirstRunTopLevelFlowExposesGuidanceAndFallbacks() {
        ActivityScenario.launch<MainActivity>(
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        ).use { scenario ->
            waitForText("Start")
            assertVisible("Start")
            assertVisible("Training")
            assertVisible("Voeding")
            assertVisible("Coach")
            assertVisible("Meer")

            assertAnyVisible("Instellen starten", "Profiel invullen", "Health Connect koppelen")

            tap("Training")
            waitForText("Routine maken")
            assertAnyVisible("Routine maken", "Start met een lege template")

            tap("Voeding")
            waitForText("Toevoegen")
            assertVisible("Toevoegen")
            tap("Coach")
            waitForText("Coach")
            assertAnyVisible("Gemini 2.5 Flash", "Lokale berekening opgeslagen", "Advies")

            tap("Meer")
            waitForText("Instellingen")
            assertVisible("Meer")
            assertVisible("Voortgang openen")
            assertVisible("Health Connect")
            assertVisible("AI / Gemini")

            tap("Voortgang openen")
            waitForText("Voortgang")
            assertVisible("Voortgang")
        }
    }

    @Test
    fun profileDraftSurvivesActivityRecreationBeforeSave() {
        ActivityScenario.launch<MainActivity>(
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        ).use { scenario ->
            waitForText("Instellen starten")
            tap("Instellen starten")
            waitForText("Doeladvies")

            compose.onAllNodes(hasSetTextAction())[0].performTextReplacement("Rotatieprofiel")
            compose.onAllNodes(hasSetTextAction())[0].assertTextContains("Rotatieprofiel")
            compose.onNodeWithText("Vrouw").performClick()
            waitForSelectedText("Vrouw")

            scenario.recreate()

            waitForText("Doeladvies")
            compose.onAllNodes(hasSetTextAction())[0].assertTextContains("Rotatieprofiel")
            compose.onNodeWithText("Vrouw").assertIsSelected()
        }
    }

    @Test
    fun aiRoutineDraftSurvivesActivityRecreationBeforeGenerate() {
        ActivityScenario.launch<MainActivity>(
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        ).use { scenario ->
            waitForText("Training")
            tap("Training")
            waitForText("Routine maken")
            tap("Met AI genereren")
            waitForText("AI-routine genereren")

            val draftFields = compose.onAllNodes(hasSetTextAction())
            draftFields[0].performTextReplacement("Herstelgericht")
            draftFields[1].performTextReplacement("4")
            draftFields[2].performTextReplacement("Dumbbells")

            scenario.recreate()

            waitForText("AI-routine genereren")
            compose.onAllNodes(hasSetTextAction())[0].assertTextContains("Herstelgericht")
            compose.onAllNodes(hasSetTextAction())[1].assertTextContains("4")
            compose.onAllNodes(hasSetTextAction())[2].assertTextContains("Dumbbells")
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

    private fun waitForText(text: String) {
        compose.waitUntil(timeoutMillis = 10_000L) {
            compose.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun waitForSelectedText(text: String) {
        compose.waitUntil(timeoutMillis = 10_000L) {
            compose.onAllNodes(hasText(text) and isSelected()).fetchSemanticsNodes().isNotEmpty()
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
