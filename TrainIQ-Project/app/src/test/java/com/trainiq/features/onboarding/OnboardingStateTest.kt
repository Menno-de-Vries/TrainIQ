package com.trainiq.features.onboarding

import com.trainiq.core.datastore.OnboardingPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingStateTest {
    @Test
    fun onboardingReducerMovesForwardBackwardAndPreservesDraft() {
        val initial = OnboardingContentState()

        val goalSelected = reduceOnboardingState(
            initial,
            OnboardingEvent.SelectGoal("Spieropbouw"),
        )
        val next = reduceOnboardingState(goalSelected, OnboardingEvent.Next)
        val back = reduceOnboardingState(next, OnboardingEvent.Back)

        assertEquals("Spieropbouw", back.draft.goal)
        assertEquals(OnboardingStep.WELCOME, back.step)
    }

    @Test
    fun onboardingReducerAllowsSkippingHealthConnectAndAiBeforeCompletion() {
        val configured = OnboardingContentState(step = OnboardingStep.HEALTH_CONNECT)
            .let { reduceOnboardingState(it, OnboardingEvent.SkipHealthConnect) }
            .let { reduceOnboardingState(it.copy(step = OnboardingStep.AI_PRIVACY), OnboardingEvent.SkipAi) }
            .let { reduceOnboardingState(it.copy(step = OnboardingStep.REMINDERS), OnboardingEvent.SetRemindersEnabled(true)) }
            .let { reduceOnboardingState(it, OnboardingEvent.AcceptPrivacy) }

        assertFalse(configured.draft.healthConnectAccepted)
        assertTrue(configured.draft.healthConnectSkipped)
        assertFalse(configured.draft.aiAccepted)
        assertTrue(configured.draft.aiSkipped)
        assertFalse(configured.draft.aiSetupDeferred)
        assertTrue(configured.draft.remindersEnabled)
        assertTrue(configured.canComplete)
    }

    @Test
    fun onboardingReducerDistinguishesDeferredAiSetupFromNoAiChoice() {
        val deferred = reduceOnboardingState(
            OnboardingContentState(step = OnboardingStep.AI_PRIVACY),
            OnboardingEvent.DeferAiSetup,
        )
        val noAi = reduceOnboardingState(
            OnboardingContentState(step = OnboardingStep.AI_PRIVACY),
            OnboardingEvent.ContinueWithoutAi,
        )

        assertTrue(deferred.draft.aiAccepted)
        assertTrue(deferred.draft.aiSetupDeferred)
        assertFalse(deferred.draft.aiSkipped)
        assertFalse(noAi.draft.aiAccepted)
        assertFalse(noAi.draft.aiSetupDeferred)
        assertTrue(noAi.draft.aiSkipped)
    }

    @Test
    fun onboardingReducerSkipAllCanCompleteFromFirstScreen() {
        val skipped = reduceOnboardingState(OnboardingContentState(), OnboardingEvent.SkipAll)

        assertEquals(OnboardingStep.REMINDERS, skipped.step)
        assertTrue(skipped.draft.healthConnectSkipped)
        assertTrue(skipped.draft.aiSkipped)
        assertFalse(skipped.draft.aiSetupDeferred)
        assertTrue(skipped.draft.privacyAcknowledged)
        assertTrue(skipped.canComplete)
    }

    @Test
    fun onboardingPreferencesDefaultToIncompleteAndConvertToContentState() {
        val preferences = OnboardingPreferences()
        val state = preferences.toOnboardingContentState()

        assertFalse(preferences.completed)
        assertEquals(OnboardingStep.WELCOME, state.step)
        assertEquals("", state.draft.goal)
    }

    @Test
    fun onboardingSetupItemsExposeSkippedCapabilitiesForSettingsAndHome() {
        val preferences = OnboardingPreferences(
            completed = true,
            healthConnectSkipped = true,
            aiSkipped = true,
            remindersEnabled = false,
            guidedTourCompleted = false,
        )

        val items = onboardingSetupItems(preferences)

        assertTrue(items.any { it.title == "Health Connect koppelen" })
        assertTrue(items.any { it.title == "AI-coach instellen" })
        assertTrue(items.any { it.title == "Herinneringen kiezen" })
        assertTrue(items.any { it.title == "App-rondleiding afronden" })
    }

    @Test
    fun onboardingCompletionStartsGuidedTourUntilItIsCompletedOrSkipped() {
        assertTrue(shouldShowGuidedTour(OnboardingPreferences(completed = true)))
        assertFalse(shouldShowGuidedTour(OnboardingPreferences(completed = true, guidedTourCompleted = true)))
        assertFalse(shouldShowGuidedTour(OnboardingPreferences(completed = true, guidedTourSkipped = true)))
        assertFalse(shouldShowGuidedTour(OnboardingPreferences(completed = false)))
    }
}
