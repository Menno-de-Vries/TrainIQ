package com.trainiq.navigation

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsProperties
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

class TopLevelSwipeNavigationInstrumentedTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Before fun seed() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        resetTrainIqAndroidTestDatabase(context)
        UserPreferencesRepository(context).saveOnboardingPreferences(OnboardingPreferences(completed = true, guidedTourCompleted = true))
    }

    @Test fun tapAndSwipeShareVisibleDestinationAndStopAtEnds() {
        ActivityScenario.launch(MainActivity::class.java).use {
            val navigationItem = SemanticsMatcher.keyIsDefined(SemanticsProperties.Selected) and hasClickAction()
            val home = (hasText("Start") or hasContentDescription("Start")) and navigationItem
            val training = (hasText("Training") or hasContentDescription("Training")) and navigationItem
            val nutrition = (hasText("Voeding") or hasContentDescription("Voeding")) and navigationItem
            val coach = (hasText("Coach") or hasContentDescription("Coach")) and navigationItem
            val more = (hasText("Meer") or hasContentDescription("Instellingen")) and navigationItem
            compose.waitUntil(30_000) { compose.onAllNodes(training).fetchSemanticsNodes().isNotEmpty() }
            compose.onNode(home).assertIsSelected()
            swipeAcross(fromRight = false)
            compose.onNode(home).assertIsSelected()
            compose.onNode(training).performClick()
            compose.onNode(training).assertIsSelected()
            swipeAcross(fromRight = true)
            compose.onNode(nutrition).assertIsSelected()
            swipeAcross(fromRight = false)
            compose.onNode(training).assertIsSelected()
            swipeAcross(fromRight = true)
            compose.onNode(nutrition).assertIsSelected()
            swipeAcross(fromRight = true)
            compose.onNode(coach).assertIsSelected()
            swipeAcross(fromRight = true)
            compose.onNode(more).assertIsSelected()
            swipeAcross(fromRight = true)
            compose.onNode(more).assertIsSelected()
            swipeAcross(fromRight = false)
            compose.onNode(coach).assertIsSelected()
            swipeAcross(fromRight = false)
            compose.onNode(nutrition).assertIsSelected()
        }
    }

    private fun swipeAcross(fromRight: Boolean) {
        compose.onRoot().performTouchInput {
            val startX = if (fromRight) width * .8f else width * .2f
            val endX = if (fromRight) width * .2f else width * .8f
            // Stay clear of central action buttons; their click targets intentionally own gestures.
            swipe(start = Offset(startX, height * .15f), end = Offset(endX, height * .15f))
        }
    }
}
