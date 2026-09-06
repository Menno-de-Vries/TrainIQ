package com.trainiq.features.nutrition

import android.content.Context
import android.os.ParcelFileDescriptor
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasClickAction
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.trainiq.MainActivity
import com.trainiq.core.datastore.UserPreferencesRepository
import com.trainiq.core.datastore.OnboardingPreferences
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NutritionLongFormImeInstrumentedTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    private lateinit var context: Context
    private var originalFontScale: String = "1.0"
    private lateinit var preferences: UserPreferencesRepository
    private lateinit var originalOnboarding: OnboardingPreferences

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        preferences = UserPreferencesRepository(context)
        runBlocking {
            originalOnboarding = preferences.getOnboardingPreferences()
            preferences.saveOnboardingPreferences(OnboardingPreferences(completed = true, guidedTourCompleted = true))
        }
        originalFontScale = shell("settings get system font_scale").trim().ifBlank { "1.0" }
        shell("settings put system font_scale 1.5")
    }

    @After
    fun tearDown() {
        shell("settings put system font_scale $originalFontScale")
        shell("input keyevent 4")
        runBlocking { preferences.saveOnboardingPreferences(originalOnboarding) }
    }

    @Test
    fun nutritionAddSheetKeepsLongAiContextVisibleAfterImeDismissAtFontScale15() {
        ActivityScenario.launch(MainActivity::class.java).use {
            val nutritionNavigation = (hasContentDescription("Voeding") or hasText("Voeding")) and hasClickAction()
            compose.waitUntil(30_000) { compose.onAllNodes(nutritionNavigation).fetchSemanticsNodes().isNotEmpty() }
            compose.onNode(nutritionNavigation).performClick()
            compose.waitForText("Voedingsdag")
            compose.onNodeWithContentDescription("Toevoegen aan Ochtend").performClick()
            compose.waitForText("Toevoegen aan Ochtend")

            compose.onNodeWithText("AI-context voor foto")
                .performScrollTo()
                .performTextInput("kip rollade kaas wrap saus sla tomaat ui yoghurt knoflook kruiden lange context voor clipping en IME controle")
            shell("input keyevent 4")
            compose.waitForIdle()

            compose.waitForText("AI-context voor foto")
            compose.waitForText("Foto / AI-inschatting")
            compose.waitForText("Sluiten")
        }
    }

    private fun shell(command: String): String =
        InstrumentationRegistry.getInstrumentation()
            .uiAutomation
            .executeShellCommand(command)
            .use { descriptor ->
                ParcelFileDescriptor.AutoCloseInputStream(descriptor)
                    .bufferedReader()
                    .use { it.readText() }
            }

    private fun androidx.compose.ui.test.junit4.ComposeTestRule.waitForText(text: String) {
        waitUntil(timeoutMillis = 30_000L) {
            onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty()
        }
    }
}
