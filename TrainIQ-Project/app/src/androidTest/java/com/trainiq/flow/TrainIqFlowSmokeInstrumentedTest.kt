package com.trainiq.flow

import android.content.Context
import android.content.Intent
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.compose.ui.semantics.SemanticsActions
import com.trainiq.MainActivity
import com.trainiq.features.workout.routineDetailsTestTag
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
    fun settingsProfileValidationErrorSurvivesActivityRecreation() {
        ActivityScenario.launch<MainActivity>(
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        ).use { scenario ->
            waitForText("Meer")
            tap("Meer")
            waitForText("Instellingen")
            tap("Profiel opslaan")
            waitForText("Naam is verplicht.")
            compose.onNode(hasText("Naam") and hasSetTextAction())
                .performTextReplacement("Rotatie-instellingen")
            compose.onNode(hasText("Leeftijd") and hasSetTextAction())
                .performTextReplacement("0")
            tap("Profiel opslaan")
            waitForText("Leeftijd moet tussen 1 en 120 zijn.")

            scenario.recreate()

            scrollUntilText("Profiel opslaan")
            compose.onNode(hasText("Naam") and hasSetTextAction())
                .assertTextContains("Rotatie-instellingen")
            compose.onNode(hasText("Leeftijd") and hasSetTextAction())
                .assertTextContains("0")
            assertExists("Leeftijd moet tussen 1 en 120 zijn.")
        }
    }

    @Test
    fun manualRoutineDraftSurvivesActivityRecreationBeforeCreate() {
        ActivityScenario.launch<MainActivity>(
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        ).use { scenario ->
            waitForText("Training")
            tap("Training")
            waitForText("Lege routine maken")
            tap("Lege routine maken")
            waitForText("Routinenaam")

            compose.onNode(hasText("Routinenaam") and hasSetTextAction())
                .performTextReplacement("Rotatieroutine")

            scenario.recreate()

            waitForText("Routinenaam")
            compose.onNode(hasText("Routinenaam") and hasSetTextAction())
                .assertTextContains("Rotatieroutine")
        }
    }

    @Test
    fun customExerciseDraftSurvivesActivityRecreationBeforeAdd() {
        val routineName = "Rotatie eigen oefening ${System.nanoTime()}"
        ActivityScenario.launch<MainActivity>(
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        ).use { scenario ->
            waitForText("Training")
            tap("Training")
            waitForText("Lege routine maken")
            tap("Lege routine maken")
            waitForText("Routinenaam")
            compose.onNode(hasText("Routinenaam") and hasSetTextAction())
                .performTextReplacement(routineName)
            tap("Maken")

            waitForText("Routine aangemaakt.")
            assertVisible(routineName)
            val detailsTag = routineDetailsTestTag(routineName)
            scrollUntilTag(detailsTag)
            compose.onNodeWithTag(detailsTag, useUnmergedTree = true)
                .performClick()
            waitForText("Eerste oefening toevoegen")
            tap("Eerste oefening toevoegen")
            waitForText("Voeg eigen oefening toe")
            tap("Voeg eigen oefening toe")
            waitForText("Spiergroep")

            compose.onNode(hasText("Oefening") and hasSetTextAction())
                .performTextReplacement("Rotatie squat")
            compose.onNode(hasText("Spiergroep") and hasSetTextAction())
                .performTextReplacement("Benen")
            compose.onNode(hasText("Materiaal") and hasSetTextAction())
                .performTextReplacement("Halters")

            scenario.recreate()

            waitForText("Voeg eigen oefening toe")
            compose.onNode(hasText("Oefening") and hasSetTextAction())
                .assertTextContains("Rotatie squat")
            compose.onNode(hasText("Spiergroep") and hasSetTextAction())
                .assertTextContains("Benen")
            compose.onNode(hasText("Materiaal") and hasSetTextAction())
                .assertTextContains("Halters")
        }
    }

    @Test
    fun exercisePlanEditorDraftSurvivesActivityRecreationBeforeSave() {
        val routineName = "Rotatie oefenplan ${System.nanoTime()}"
        val exerciseName = "Rotatie press"
        ActivityScenario.launch<MainActivity>(
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        ).use { scenario ->
            createRoutineWithCustomExercise(routineName, exerciseName)

            scrollUntilContentDescription("Oefening acties")
            compose.onNode(hasContentDescription("Oefening acties") and hasClickAction())
                .performSemanticsAction(SemanticsActions.OnClick)
            waitForText("Bewerken")
            compose.onNodeWithText("Bewerken").performClick()
            waitForText("Rest s")

            compose.onNode(hasText("Sets") and hasSetTextAction()).performTextReplacement("5")
            compose.onNode(hasText("Reps") and hasSetTextAction()).performTextReplacement("6-8")
            compose.onNode(hasText("Rest s") and hasSetTextAction()).performTextReplacement("150")
            compose.onNode(hasText("Kg") and hasSetTextAction()).performTextReplacement("72.5")
            compose.onNode(hasText("RPE") and hasSetTextAction()).performTextReplacement("8.5")
            tap("Drop set")
            waitForSelectedText("Drop set")

            scenario.recreate()

            waitForText("Rest s")
            compose.onNode(hasText("Sets") and hasSetTextAction()).assertTextContains("5")
            compose.onNode(hasText("Reps") and hasSetTextAction()).assertTextContains("6-8")
            compose.onNode(hasText("Rest s") and hasSetTextAction()).assertTextContains("150")
            compose.onNode(hasText("Kg") and hasSetTextAction()).assertTextContains("72.5")
            compose.onNode(hasText("RPE") and hasSetTextAction()).assertTextContains("8.5")
            waitForSelectedText("Drop set")
        }
    }

    @Test
    fun routineSetEditorDraftSurvivesActivityRecreationBeforeSave() {
        val routineName = "Rotatie seteditor ${System.nanoTime()}"
        val exerciseName = "Rotatie row"
        ActivityScenario.launch<MainActivity>(
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        ).use { scenario ->
            createRoutineWithCustomExercise(routineName, exerciseName)

            scrollUntilText("#1")
            compose.onNode(hasText("#1") and hasClickAction())
                .performSemanticsAction(SemanticsActions.OnClick)
            waitForText("Set #1 bewerken")

            compose.onNode(hasText("Reps") and hasSetTextAction()).performTextReplacement("7")
            compose.onNode(hasText("Gewicht") and hasSetTextAction()).performTextReplacement("67.5")
            compose.onNode(hasText("Rust") and hasSetTextAction()).performScrollTo().performTextReplacement("135")
            compose.onNode(hasText("RPE") and hasSetTextAction()).performScrollTo().performTextReplacement("9")
            compose.onNodeWithText("Failure").performScrollTo().performClick()
            waitForSelectedText("Failure")

            scenario.recreate()

            waitForText("Set #1 bewerken")
            compose.onNode(hasText("Reps") and hasSetTextAction()).assertTextContains("7")
            compose.onNode(hasText("Gewicht") and hasSetTextAction()).assertTextContains("67.5")
            compose.onNode(hasText("Rust") and hasSetTextAction()).performScrollTo().assertTextContains("135")
            compose.onNode(hasText("RPE") and hasSetTextAction()).performScrollTo().assertTextContains("9")
            waitForSelectedText("Failure")
        }
    }

    @Test
    fun aiRoutineDraftSurvivesActivityRecreationBeforeGenerate() {
        ActivityScenario.launch<MainActivity>(
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        ).use { scenario ->
            waitForText("Training")
            tap("Training")
            waitForText("Met AI genereren")
            tap("Met AI genereren")
            waitForText("AI-routine genereren")

            compose.onNode(hasText("Trainingsfocus / split") and hasSetTextAction())
                .performTextReplacement("Rotatie upper/lower")
            compose.onNode(hasText("Dagen per week") and hasSetTextAction())
                .performTextReplacement("4")
            compose.onNode(hasText("Beschikbaar materiaal") and hasSetTextAction())
                .performScrollTo()
                .performTextReplacement("Dumbbells en bank")
            compose.onNodeWithText("Gevorderd")
                .performScrollTo()
                .performClick()
            compose.onNodeWithContentDescription("Deload-richtlijn opnemen")
                .performScrollTo()
                .performClick()

            scenario.recreate()

            waitForText("AI-routine genereren")
            compose.onNode(hasText("Trainingsfocus / split") and hasSetTextAction())
                .assertTextContains("Rotatie upper/lower")
            compose.onNode(hasText("Dagen per week") and hasSetTextAction())
                .assertTextContains("4")
            compose.onNode(hasText("Beschikbaar materiaal") and hasSetTextAction())
                .performScrollTo()
                .assertTextContains("Dumbbells en bank")
            compose.onNodeWithText("Gevorderd")
                .performScrollTo()
                .assertIsSelected()
            compose.onNodeWithContentDescription("Deload-richtlijn opnemen")
                .performScrollTo()
                .assertIsOff()
        }
    }

    @Test
    fun manualFoodDraftSurvivesActivityRecreationBeforeSave() {
        ActivityScenario.launch<MainActivity>(
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        ).use { scenario ->
            waitForText("Voeding")
            tap("Voeding")
            waitForText("Producten")
            tap("Producten")
            waitForText("Productnaam")

            val draftFields = compose.onAllNodes(hasSetTextAction())
            draftFields[0].performTextReplacement("Rotatiehavermout")
            draftFields[1].performTextReplacement("8712345678901")
            draftFields[2].performTextReplacement("370")
            draftFields[3].performTextReplacement("13")
            draftFields[4].performTextReplacement("60")
            draftFields[5].performTextReplacement("-7")
            tap("Product opslaan")
            waitForText("Vul een niet-negatieve waarde in.")

            scenario.recreate()

            waitForText("Productnaam")
            compose.onAllNodes(hasSetTextAction())[0].assertTextContains("Rotatiehavermout")
            compose.onAllNodes(hasSetTextAction())[1].assertTextContains("8712345678901")
            compose.onAllNodes(hasSetTextAction())[2].assertTextContains("370")
            compose.onAllNodes(hasSetTextAction())[3].assertTextContains("13")
            compose.onAllNodes(hasSetTextAction())[4].assertTextContains("60")
            compose.onAllNodes(hasSetTextAction())[5].assertTextContains("-7")
            assertVisible("Vul een niet-negatieve waarde in.")
        }
    }

    @Test
    fun recipeDraftSurvivesActivityRecreationBeforeSave() {
        ActivityScenario.launch<MainActivity>(
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        ).use { scenario ->
            waitForText("Voeding")
            tap("Voeding")
            waitForText("Recepten")
            tap("Recepten")
            waitForText("Receptnaam")

            var draftFields = compose.onAllNodes(hasSetTextAction())
            draftFields[0].performTextReplacement("Rotatierecept")
            draftFields[1].performTextReplacement("Bewaar dit concept")
            draftFields[2].performTextReplacement("450")
            draftFields[3].performTextReplacement("80")
            draftFields[4].performTextReplacement("Rotatiehavermout")
            draftFields[5].performTextReplacement("8712345678901")
            draftFields[6].performTextReplacement("370")
            draftFields[7].performTextReplacement("13")
            draftFields[8].performTextReplacement("60")
            draftFields[9].performTextReplacement("7")
            tap("Ingrediënt opslaan en toevoegen")
            waitForText("80g gebruikt")

            draftFields = compose.onAllNodes(hasSetTextAction())
            draftFields[3].performTextReplacement("75")
            draftFields[4].performTextReplacement("Rotatiecacao")
            draftFields[5].performTextReplacement("8712345678902")
            draftFields[6].performTextReplacement("400")
            draftFields[7].performTextReplacement("20")
            draftFields[8].performTextReplacement("50")
            draftFields[9].performTextReplacement("-4")
            tap("Ingrediënt opslaan en toevoegen")
            waitForText("Vul een niet-negatieve waarde in.")

            scenario.recreate()

            waitForText("Receptnaam")
            draftFields = compose.onAllNodes(hasSetTextAction())
            draftFields[0].assertTextContains("Rotatierecept")
            draftFields[1].assertTextContains("Bewaar dit concept")
            draftFields[2].assertTextContains("450")
            draftFields[3].assertTextContains("75")
            draftFields[4].assertTextContains("Rotatiecacao")
            draftFields[5].assertTextContains("8712345678902")
            draftFields[6].assertTextContains("400")
            draftFields[7].assertTextContains("20")
            draftFields[8].assertTextContains("50")
            draftFields[9].assertTextContains("-4")
            assertExists("80g gebruikt")
            assertVisible("Vul een niet-negatieve waarde in.")
        }
    }

    @Test
    fun mealDraftSurvivesActivityRecreationBeforeSave() {
        ActivityScenario.launch<MainActivity>(
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        ).use { scenario ->
            waitForText("Voeding")
            tap("Voeding")
            waitForText("Producten")
            tap("Producten")
            waitForText("Productnaam")

            val productFields = compose.onAllNodes(hasSetTextAction())
            productFields[0].performTextReplacement("Rotatiemaaltijdproduct")
            productFields[1].performTextReplacement("8712345678903")
            productFields[2].performTextReplacement("250")
            productFields[3].performTextReplacement("12")
            productFields[4].performTextReplacement("30")
            productFields[5].performTextReplacement("8")
            tap("Product opslaan")
            waitForText("Rotatiemaaltijdproduct opgeslagen.")
            assertVisible("Aan maaltijd toevoegen")

            compose.onNodeWithContentDescription("Gram bij toevoegen aan maaltijd")
                .performTextReplacement("125")
            tapLast("Aan maaltijd toevoegen")
            waitForText("Maaltijd controleren")

            tap("Avond")
            waitForSelectedText("Avond")
            compose.onNodeWithContentDescription("Maaltijdnaam").performTextReplacement("")
            compose.onNodeWithContentDescription("Notities").performTextReplacement("Bewaar dit maaltijdconcept")
            compose.onNodeWithContentDescription("Gram").performTextReplacement("175")
            tap("Maaltijd opslaan")
            waitForText("Naam is verplicht.")

            scenario.recreate()

            waitForText("Maaltijd controleren")
            waitForSelectedText("Avond")
            compose.onNodeWithContentDescription("Notities").assertTextContains("Bewaar dit maaltijdconcept")
            compose.onNodeWithContentDescription("Gram").assertTextContains("175")
            assertVisible("Rotatiemaaltijdproduct")
            assertVisible("Naam is verplicht.")
        }
    }

    @Test
    fun aiMealContextSurvivesActivityRecreationBeforeScan() {
        ActivityScenario.launch<MainActivity>(
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        ).use { scenario ->
            waitForText("Voeding")
            tap("Voeding")
            waitForText("AI-resultaat")
            tap("AI-resultaat")
            waitForText("Optionele context")

            compose.onNodeWithContentDescription("Optionele context")
                .performTextReplacement("Vegetarische maaltijd na krachttraining")

            scenario.recreate()

            waitForText("Optionele context")
            compose.onNodeWithContentDescription("Optionele context")
                .assertTextContains("Vegetarische maaltijd na krachttraining")
        }
    }

    private fun createRoutineWithCustomExercise(routineName: String, exerciseName: String) {
        waitForText("Training")
        tap("Training")
        waitForText("Lege routine maken")
        tap("Lege routine maken")
        waitForText("Routinenaam")
        compose.onNode(hasText("Routinenaam") and hasSetTextAction())
            .performTextReplacement(routineName)
        tap("Maken")

        waitForText("Routine aangemaakt.")
        val detailsTag = routineDetailsTestTag(routineName)
        scrollUntilTag(detailsTag)
        compose.onNodeWithTag(detailsTag, useUnmergedTree = true).performClick()
        waitForText("Eerste oefening toevoegen")
        tap("Eerste oefening toevoegen")
        waitForText("Voeg eigen oefening toe")
        tap("Voeg eigen oefening toe")
        waitForText("Spiergroep")

        compose.onNode(hasText("Oefening") and hasSetTextAction()).performTextReplacement(exerciseName)
        compose.onNode(hasText("Spiergroep") and hasSetTextAction()).performTextReplacement("Schouders")
        compose.onNode(hasText("Materiaal") and hasSetTextAction()).performTextReplacement("Dumbbells")
        val addButtons = compose.onAllNodesWithText("Toevoegen", substring = false).fetchSemanticsNodes()
        check(addButtons.isNotEmpty()) { "Toevoegen action not found" }
        compose.onAllNodesWithText("Toevoegen", substring = false)[addButtons.lastIndex].performClick()
        compose.waitUntil(timeoutMillis = 10_000L) {
            compose.onAllNodes(hasText("Spiergroep") and hasSetTextAction()).fetchSemanticsNodes().isEmpty()
        }
        waitForText(exerciseName)
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
        compose.onAllNodesWithText(text, substring = true)[matches.lastIndex].performScrollTo().performClick()
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

    private fun scrollUntilTag(tag: String) {
        val matcher = hasTestTag(tag)
        if (compose.onAllNodes(matcher, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()) return
        val scrollNodeCount = compose.onAllNodes(hasScrollAction()).fetchSemanticsNodes().size
        repeat(scrollNodeCount) { index ->
            runCatching {
                compose.onAllNodes(hasScrollAction())[index].performScrollToNode(matcher)
            }
            if (compose.onAllNodes(matcher, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()) return
        }
        error("Test tag not found after scrolling: $tag")
    }

    private fun scrollUntilContentDescription(description: String) {
        val matcher = hasContentDescription(description)
        if (compose.onAllNodes(matcher, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()) return
        val scrollNodeCount = compose.onAllNodes(hasScrollAction()).fetchSemanticsNodes().size
        repeat(scrollNodeCount) { index ->
            runCatching {
                compose.onAllNodes(hasScrollAction())[index].performScrollToNode(matcher)
            }
            if (compose.onAllNodes(matcher, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()) return
        }
        error("Content description not found after scrolling: $description")
    }

    private fun scrollUntilAnyText(vararg texts: String) {
        texts.forEach { text ->
            runCatching {
                compose.onNodeWithText(text).performScrollTo()
            }
            if (compose.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty()) return
            val scrollNodeCount = compose.onAllNodes(hasScrollAction()).fetchSemanticsNodes().size
            repeat(scrollNodeCount) { index ->
                runCatching {
                    compose.onAllNodes(hasScrollAction())[index].performScrollToNode(hasText(text))
                }
                if (compose.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty()) return
            }
        }
    }
}
