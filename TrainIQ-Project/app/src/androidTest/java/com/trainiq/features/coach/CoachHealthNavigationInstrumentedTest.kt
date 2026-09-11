package com.trainiq.features.coach

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.trainiq.MainActivity
import com.trainiq.core.datastore.OnboardingPreferences
import com.trainiq.core.datastore.UserPreferencesRepository
import com.trainiq.testing.resetTrainIqAndroidTestDatabase
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class CoachHealthNavigationInstrumentedTest {
    @get:Rule val compose = createEmptyComposeRule()
    @Before fun seed() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        runBlocking {
            resetTrainIqAndroidTestDatabase(context)
            UserPreferencesRepository(context).saveOnboardingPreferences(OnboardingPreferences(completed = true, guidedTourCompleted = true))
        }
    }

    @Test fun coachOpensSleepAndBodyHistoryAndBackReturnsToCoach() {
        ActivityScenario.launch(MainActivity::class.java).use {
            val coach = (hasText("Coach") or hasContentDescription("Coach")) and hasClickAction()
            compose.waitUntil(30_000) { compose.onAllNodes(coach).fetchSemanticsNodes().isNotEmpty() }
            compose.onNode(coach).performClick()
            compose.onNodeWithText("Lichaamsmetingen openen").performScrollTo().assertIsDisplayed()
            capture("coach-health")
            compose.onNodeWithText("Slaap · alarm en bevestiging").performScrollTo().performClick()
            compose.onNodeWithText("Slaapvoorbereiding").assertIsDisplayed()
            capture("coach-sleep")
            compose.onNodeWithText("Terug").performClick()
            compose.onNodeWithText("Lichaamsmetingen openen").performScrollTo().performClick()
            compose.onNodeWithText("Lichaam & voortgang").assertIsDisplayed()
            compose.waitUntil(10_000) { compose.onAllNodesWithText("Gewicht (kg)").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("Gewicht (kg)").performScrollTo().assertExists()
            capture("coach-weight")
            androidx.test.espresso.Espresso.pressBack()
            compose.onNodeWithText("Slaap · alarm en bevestiging").performScrollTo().assertIsDisplayed()
        }
    }

    private fun capture(name: String) {
        val command = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().uiAutomation
            .executeShellCommand("screencap -p /data/local/tmp/trainiq-$name.png")
        android.os.ParcelFileDescriptor.AutoCloseInputStream(command).use { it.readBytes() }
    }
}
