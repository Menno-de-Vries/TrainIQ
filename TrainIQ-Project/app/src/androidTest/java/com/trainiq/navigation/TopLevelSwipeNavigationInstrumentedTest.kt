package com.trainiq.navigation

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.geometry.Offset
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
            val training = (hasText("Training") or hasContentDescription("Training")) and hasClickAction()
            val nutrition = (hasText("Voeding") or hasContentDescription("Voeding")) and hasClickAction()
            compose.waitUntil(30_000) { compose.onAllNodes(training).fetchSemanticsNodes().isNotEmpty() }
            compose.onNode(training).performClick()
            compose.onNode(training).assertIsSelected()
            compose.onRoot().performTouchInput {
                swipe(start = Offset(width * 0.8f, height * 0.5f), end = Offset(width * 0.2f, height * 0.5f))
            }
            compose.onNode(nutrition).assertIsSelected()
            compose.onRoot().performTouchInput {
                swipe(start = Offset(width * 0.2f, height * 0.5f), end = Offset(width * 0.8f, height * 0.5f))
            }
            compose.onNode(training).assertIsSelected()
        }
    }
}
