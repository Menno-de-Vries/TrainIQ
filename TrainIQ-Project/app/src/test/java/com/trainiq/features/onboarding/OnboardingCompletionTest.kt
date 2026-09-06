package com.trainiq.features.onboarding

import com.trainiq.core.datastore.OnboardingPreferences
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingCompletionTest {
    @Test
    fun loadFailureOffersRetryAndSuccessfulRetryRestoresStoredChoices() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            var reads = 0
            val vm = OnboardingViewModel({ flow {
                if (++reads == 1) error("storage unavailable")
                emit(OnboardingPreferences(goal = "Saved choice"))
            } }, {}, {}, dispatcher)
            runCurrent()
            assertTrue(vm.uiState.value is OnboardingUiState.Error)
            vm.retry()
            runCurrent()
            assertEquals("Saved choice", (vm.uiState.value as OnboardingUiState.Success).content.draft.goal)
        } finally { Dispatchers.resetMain() }
    }

    @Test
    fun draftAndCompletionFailuresPreserveChoicesAndCanBeRetried() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val stored = MutableStateFlow(OnboardingPreferences())
            var failSave = true
            var failComplete = true
            var navigations = 0
            val vm = OnboardingViewModel({ stored }, {
                if (failSave) error("disk full")
                stored.value = it
            }, {
                if (failComplete) error("disk full")
                stored.value = it
            }, dispatcher)
            runCurrent()
            vm.dispatch(OnboardingEvent.SelectGoal("Keep me"))
            runCurrent()
            assertEquals("Keep me", (vm.uiState.value as OnboardingUiState.Success).content.draft.goal)
            assertTrue((vm.uiState.value as OnboardingUiState.Success).saveError != null)
            failSave = false
            vm.retry()
            runCurrent()
            assertEquals("Keep me", stored.value.goal)
            assertEquals(null, (vm.uiState.value as OnboardingUiState.Success).saveError)
            vm.complete { navigations++ }
            runCurrent()
            assertEquals(0, navigations)
            assertFalse((vm.uiState.value as OnboardingUiState.Success).isCompleting)
            assertTrue((vm.uiState.value as OnboardingUiState.Success).saveError != null)
            failComplete = false
            vm.complete { navigations++ }
            runCurrent()
            assertEquals(1, navigations)
            assertTrue(stored.value.completed)
            assertEquals("Keep me", stored.value.goal)
        } finally { Dispatchers.resetMain() }
    }

    @Test
    fun draftEventsKeepOrderAndCompletionIgnoresLateEventsAndDuplicateFinish() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val stored = MutableStateFlow(OnboardingPreferences())
            val releaseFirst = CompletableDeferred<Unit>()
            val writes = mutableListOf<String>()
            var navigations = 0
            val viewModel = OnboardingViewModel(
                { stored },
                { draft ->
                    if (draft.goal == "First") releaseFirst.await()
                    stored.value = draft
                    writes += draft.goal
                },
                { stored.value = it; writes += "complete" },
                dispatcher,
            )
            runCurrent()
            viewModel.dispatch(OnboardingEvent.SelectGoal("First"))
            viewModel.dispatch(OnboardingEvent.SelectGoal("Last"))
            viewModel.complete { navigations++ }
            viewModel.complete { navigations++ }
            viewModel.dispatch(OnboardingEvent.SelectGoal("Late"))
            runCurrent()
            releaseFirst.complete(Unit)
            runCurrent()
            viewModel.dispatch(OnboardingEvent.SkipAll)
            runCurrent()

            assertEquals(listOf("First", "Last", "complete"), writes)
            assertEquals("Last", stored.value.goal)
            assertEquals(1, navigations)
            assertTrue(shouldShowGuidedTour(stored.value))
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun editingExistingSetupPreservesCompletionAndTourDecisions() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            for (initial in listOf(
                OnboardingPreferences(completed = true),
                OnboardingPreferences(completed = true, guidedTourCompleted = true),
                OnboardingPreferences(completed = true, guidedTourSkipped = true),
            )) {
                val stored = MutableStateFlow(initial)
                val viewModel = OnboardingViewModel({ stored }, { stored.value = it }, { stored.value = it }, dispatcher)
                runCurrent()
                viewModel.dispatch(OnboardingEvent.SelectGoal("Updated"))
                runCurrent()
                assertTrue(stored.value.completed)
                viewModel.complete {}
                runCurrent()
                assertEquals(initial.guidedTourCompleted, stored.value.guidedTourCompleted)
                assertEquals(initial.guidedTourSkipped, stored.value.guidedTourSkipped)
                assertEquals(shouldShowGuidedTour(initial), shouldShowGuidedTour(stored.value))
            }
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun finishingAfterSkipWaitsForDelayedDraftAndLeavesTourEligible() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val stored = MutableStateFlow(OnboardingPreferences())
            val releaseDraft = CompletableDeferred<Unit>()
            var navigated = false
            val viewModel = OnboardingViewModel(
                { stored },
                { draft -> releaseDraft.await(); stored.value = draft },
                { completed -> stored.value = completed },
                dispatcher,
            )
            runCurrent()
            viewModel.dispatch(OnboardingEvent.SkipAll)
            runCurrent()
            viewModel.complete { navigated = true }
            runCurrent()
            val navigatedBeforeDraftWasSaved = navigated
            releaseDraft.complete(Unit)
            runCurrent()

            assertFalse("Navigation must wait for earlier draft writes", navigatedBeforeDraftWasSaved)
            assertTrue(navigated)
            assertTrue("A late draft must not suppress the tour", shouldShowGuidedTour(stored.value))
        } finally {
            Dispatchers.resetMain()
        }
    }
}
