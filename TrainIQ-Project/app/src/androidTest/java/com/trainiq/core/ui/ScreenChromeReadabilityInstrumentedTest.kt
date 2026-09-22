package com.trainiq.core.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.trainiq.core.theme.TrainIqTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

class ScreenChromeReadabilityInstrumentedTest {
    @get:Rule val compose = createComposeRule()

    @Test fun recoveryMessageRemainsUntilExplicitDismissal() {
        var dismissals = 0
        compose.setContent {
            TrainIqTheme { MessageCard("Nog niet gekoppeld: slaap en hartslag.", onDismiss = { dismissals++ }) }
        }
        compose.mainClock.advanceTimeBy(10_000)
        compose.runOnIdle { assertEquals(0, dismissals) }
        compose.onNodeWithText("Sluiten").performClick()
        compose.runOnIdle { assertEquals(1, dismissals) }
    }

    @Test fun recoveryInstructionsWrapAtLargeFontInsteadOfHidingTheNextAction() {
        val message = "Deze maaltijd bevat een verwijderd product of recept. Verwijder het item uit je concept en probeer opnieuw."
        compose.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, fontScale = 1.5f)) {
                TrainIqTheme {
                    Column(Modifier.width(320.dp).safeDrawingPadding()) { MessageCard(message, onDismiss = {}) }
                }
            }
        }
        capture("message")
        assertCompleteText(message)
        compose.onNodeWithText("Sluiten").assertIsDisplayed()
    }

    @Test fun compactScreenHeaderKeepsItsFullTitleAndExplanation() {
        val title = "Lichaam & voortgang"
        val subtitle = "Bekijk je metingen en voortgang over meerdere weken."
        compose.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, fontScale = 1.5f)) {
                TrainIqTheme {
                    Column(Modifier.width(240.dp).safeDrawingPadding()) { AppScreenHeader(title, subtitle) }
                }
            }
        }
        capture("header")
        assertCompleteText(title)
        assertCompleteText(subtitle)
    }

    @Test fun sectionTabsKeepFullLabelsAndSelectionAtLargeFont() {
        var selected = "routines"
        val tabs = listOf(
            CompactSectionTabItem("routines", "Routines"),
            CompactSectionTabItem("library", "Bibliotheek"),
            CompactSectionTabItem("history", "Geschiedenis"),
        )
        compose.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, fontScale = 1.5f)) {
                TrainIqTheme {
                    Column(Modifier.width(320.dp).safeDrawingPadding()) {
                        CompactSectionTabs(selected, tabs, { selected = it.key })
                    }
                }
            }
        }
        capture("tabs")
        tabs.forEach { assertCompleteText(it.label, singleLine = true) }
        compose.onNodeWithText("Geschiedenis").performClick()
        compose.runOnIdle { assertEquals("history", selected) }
    }

    private fun assertCompleteText(text: String, singleLine: Boolean = false) {
        val layouts = mutableListOf<TextLayoutResult>()
        compose.onNodeWithText(text).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertEquals(1, layouts.size)
        val layout = layouts.single()
        // FlowRow can reuse a paragraph measured against a wider parent constraint.
        // Check the rendered lines, not that cached paragraph's unused width.
        assertFalse("Text height must fit: $text", layout.multiParagraph.height > layout.size.height + 1)
        if (singleLine) assertEquals("Keep complete short labels together", 1, layout.lineCount)
        for (line in 0 until layouts.single().lineCount) {
            assertFalse("Text line must fit: $text", layout.getLineRight(line) - layout.getLineLeft(line) > layout.size.width + 1)
            assertFalse("Text must not be ellipsized: $text", layouts.single().isLineEllipsized(line))
        }
    }

    private fun capture(name: String) {
        compose.waitForIdle()
        val descriptor = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().uiAutomation
            .executeShellCommand("screencap -p /data/local/tmp/trainiq-polish-$name.png")
        android.os.ParcelFileDescriptor.AutoCloseInputStream(descriptor).use { it.readBytes() }
    }
}
