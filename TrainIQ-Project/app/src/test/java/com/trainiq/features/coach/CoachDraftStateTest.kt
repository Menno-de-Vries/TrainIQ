package com.trainiq.features.coach

import androidx.lifecycle.SavedStateHandle
import com.trainiq.domain.model.BiologicalSex
import com.trainiq.domain.model.AiFallbackContext
import com.trainiq.domain.model.CoachOverview
import com.trainiq.domain.model.GoalAdvice
import com.trainiq.domain.model.SavedGoalAdvice
import com.trainiq.domain.model.UserProfile
import com.trainiq.domain.model.WeeklyReportResult
import com.trainiq.domain.model.WeeklyReportSource
import com.trainiq.domain.repository.CoachRepository
import com.trainiq.domain.usecase.GenerateGoalAdviceUseCase
import com.trainiq.domain.usecase.GenerateWeeklyReportUseCase
import com.trainiq.domain.usecase.ObserveCoachUseCase
import com.trainiq.domain.usecase.ObserveSavedGoalAdviceUseCase
import com.trainiq.domain.usecase.ObserveUserProfileUseCase
import com.trainiq.domain.usecase.SaveUserProfileUseCase
import java.io.Serializable
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CoachDraftStateTest {
    private lateinit var mainDispatcher: TestDispatcher

    @Before
    fun setUp() {
        mainDispatcher = UnconfinedTestDispatcher()
        Dispatchers.setMain(mainDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun dirtyProfileDraftSurvivesProfileRefreshAndViewModelRecreation_thenSaveResetsDirtyState() = runTest {
        val repository = FakeCoachRepository(profile = profile(name = "Opgeslagen"))
        val savedStateHandle = SavedStateHandle()
        val first = coachViewModel(repository, savedStateHandle)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { first.uiState.collect {} }
        advanceUntilIdle()

        val hydrated = first.success().profileDraft
        assertEquals("Opgeslagen", hydrated.name)
        assertTrue(savedStateHandle.keys().all { key -> savedStateHandle.get<Any?>(key) is Serializable })
        assertFalse(first.success().isProfileDraftDirty)

        first.updateProfileDraft(hydrated.copy(name = "Mijn concept", weight = "82.5"))
        repository.profile.value = profile(name = "Externe refresh", weight = 90.0)
        advanceUntilIdle()

        assertEquals("Mijn concept", first.success().profileDraft.name)
        assertEquals("82.5", first.success().profileDraft.weight)
        assertTrue(first.success().isProfileDraftDirty)

        val recreated = coachViewModel(repository, savedStateHandle)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { recreated.uiState.collect {} }
        advanceUntilIdle()

        assertEquals("Mijn concept", recreated.success().profileDraft.name)
        assertEquals("82.5", recreated.success().profileDraft.weight)
        assertTrue(recreated.success().isProfileDraftDirty)

        recreated.saveProfile()
        advanceUntilIdle()

        assertEquals("Mijn concept", repository.profile.value?.name)
        assertEquals("Mijn concept", recreated.success().profileDraft.name)
        assertFalse(recreated.success().isProfileDraftDirty)
    }

    @Test
    fun editDuringSaveIsNotOverwrittenWhenOlderSubmittedDraftFinishesSaving() = runTest {
        val repository = FakeCoachRepository(profile = profile(name = "Opgeslagen")).apply {
            saveGate = CompletableDeferred()
        }
        val viewModel = coachViewModel(repository, SavedStateHandle())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val submittedDraft = viewModel.success().profileDraft.copy(name = "Ingediend")
        viewModel.updateProfileDraft(submittedDraft)
        viewModel.saveProfile()
        repository.saveStarted.await()

        viewModel.updateProfileDraft(submittedDraft.copy(name = "Nieuwer concept"))
        repository.saveGate?.complete(Unit)
        advanceUntilIdle()

        assertEquals("Ingediend", repository.profile.value?.name)
        assertEquals("Nieuwer concept", viewModel.success().profileDraft.name)
        assertTrue(viewModel.success().isProfileDraftDirty)
    }

    @Test
    fun weeklyReportLocalFallbackShowsSpecificCauseAndClearsLoadingState() = runTest {
        val safeCause = "OpenAI reageerde te langzaam. Controleer je verbinding en probeer opnieuw."
        val repository = FakeCoachRepository(profile = profile(name = "Opgeslagen")).apply {
            weeklyReportResult = WeeklyReportResult(
                summary = "$safeCause Lokale samenvatting: consistentie blijft leidend.",
                wins = emptyList(),
                risks = emptyList(),
                nextWeekFocus = "Herstel bewaken.",
                source = WeeklyReportSource.LOCAL_FALLBACK,
                fallbackContext = AiFallbackContext.TIMEOUT,
            )
        }
        val viewModel = coachViewModel(repository, SavedStateHandle())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.generateWeeklyReport()
        advanceUntilIdle()

        assertFalse(viewModel.success().isGeneratingReport)
        assertEquals(safeCause, viewModel.success().message)
        assertEquals(WeeklyReportSource.LOCAL_FALLBACK, viewModel.success().generatedReport?.source)
    }

    @Test
    fun weeklyReportGenericLocalFallbackKeepsConciseExistingMessage() = runTest {
        val repository = FakeCoachRepository(profile = profile(name = "Opgeslagen")).apply {
            weeklyReportResult = WeeklyReportResult(
                summary = "Lokale samenvatting: consistentie blijft leidend.",
                wins = emptyList(),
                risks = emptyList(),
                nextWeekFocus = "Herstel bewaken.",
                source = WeeklyReportSource.LOCAL_FALLBACK,
            )
        }
        val viewModel = coachViewModel(repository, SavedStateHandle())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.generateWeeklyReport()
        advanceUntilIdle()

        assertEquals(
            "Lokale samenvatting gemaakt.",
            viewModel.success().message,
        )
        assertFalse(viewModel.success().isGeneratingReport)
    }

    @Test
    fun goalAdviceDisabledAiDoesNotClaimTemporaryProviderFailure() = runTest {
        val disabledMessage = "AI staat uit. Schakel AI in via Instellingen om een provider te gebruiken."
        val repository = FakeCoachRepository(profile = profile(name = "Opgeslagen")).apply {
            goalAdviceResult = goalAdvice(
                summary = "$disabledMessage Lokale berekening blijft beschikbaar.",
                fallbackContext = AiFallbackContext.AI_DISABLED,
            )
        }
        val viewModel = coachViewModel(repository, SavedStateHandle())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.generateGoalAdvice()
        advanceUntilIdle()

        assertFalse(viewModel.success().isGeneratingAdvice)
        assertEquals(disabledMessage, viewModel.success().message)
    }

    @Test
    fun editedInputInvalidatesDelayedAdviceAndNewRequestWins() = runTest {
        val oldResponse = CompletableDeferred<Unit>()
        val repository = FakeCoachRepository(profile = profile("Tester")).apply {
            adviceForWeight = { weight ->
                if (weight == 80.0) withContext(NonCancellable) { oldResponse.await() }
                goalAdvice("Advice for $weight")
            }
        }
        val vm = coachViewModel(repository, SavedStateHandle())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        runCurrent()
        vm.generateGoalAdvice()
        vm.generateGoalAdvice()
        runCurrent()
        assertEquals(1, repository.adviceCalls)
        vm.updateProfileDraft(vm.success().profileDraft.copy(weight = "85"))
        runCurrent()
        assertFalse(vm.success().isGeneratingAdvice)
        assertEquals(null, vm.success().goalAdvice)
        vm.generateGoalAdvice()
        runCurrent()
        assertEquals("Advice for 85.0", vm.success().goalAdvice?.summary)
        oldResponse.complete(Unit)
        runCurrent()
        assertEquals("Advice for 85.0", vm.success().goalAdvice?.summary)
        assertFalse(vm.success().isGeneratingAdvice)
    }

    @Test
    fun failedAdviceRequestCanBeRetried() = runTest {
        val repository = FakeCoachRepository(profile = profile("Tester"))
        val vm = coachViewModel(repository, SavedStateHandle())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        runCurrent()
        vm.generateGoalAdvice()
        runCurrent()
        assertFalse(vm.success().isGeneratingAdvice)
        repository.goalAdviceResult = goalAdvice("Recovered")
        vm.generateGoalAdvice()
        runCurrent()
        assertEquals("Recovered", vm.success().goalAdvice?.summary)
    }

    @Test
    fun reportRefreshKeepsLastSuccessOnFailureAndCanRetry() = runTest {
        val report = WeeklyReportResult(
            summary = "Saved report", wins = emptyList(), risks = emptyList(),
            nextWeekFocus = "Rest", source = WeeklyReportSource.LOCAL_FALLBACK,
        )
        val repository = FakeCoachRepository(profile("Tester")).apply { weeklyReportResult = report }
        val vm = coachViewModel(repository, SavedStateHandle())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        runCurrent()
        vm.generateWeeklyReport()
        runCurrent()
        repository.weeklyReportFailure = IllegalStateException("offline")
        vm.generateWeeklyReport()
        runCurrent()
        assertEquals(report, vm.success().generatedReport)
        assertFalse(vm.success().isGeneratingReport)
        assertTrue(vm.success().message != null)
        repository.weeklyReportFailure = null
        repository.weeklyReportResult = report.copy(summary = "Updated")
        vm.generateWeeklyReport()
        runCurrent()
        assertEquals("Updated", vm.success().generatedReport?.summary)
    }

    @Test
    fun reportRequestIsGuardedBeforeDispatchAndCancellationClearsPending() = runTest {
        Dispatchers.setMain(kotlinx.coroutines.test.StandardTestDispatcher(testScheduler))
        val repository = FakeCoachRepository(profile("Tester")).apply {
            weeklyReportGate = CompletableDeferred()
            weeklyReportFailure = kotlinx.coroutines.CancellationException("cancelled")
        }
        val vm = coachViewModel(repository, SavedStateHandle())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        runCurrent()
        vm.generateWeeklyReport()
        vm.generateWeeklyReport()
        runCurrent()
        assertEquals(1, repository.weeklyReportCalls)
        assertTrue(vm.success().isGeneratingReport)
        repository.weeklyReportGate!!.complete(Unit)
        runCurrent()
        assertFalse(vm.success().isGeneratingReport)
        assertEquals(null, vm.success().message)
    }

    private fun coachViewModel(repository: CoachRepository, savedStateHandle: SavedStateHandle) = CoachViewModel(
        savedStateHandle = savedStateHandle,
        observeCoachUseCase = ObserveCoachUseCase(repository),
        observeUserProfileUseCase = ObserveUserProfileUseCase(repository),
        observeSavedGoalAdviceUseCase = ObserveSavedGoalAdviceUseCase(repository),
        generateGoalAdviceUseCase = GenerateGoalAdviceUseCase(repository),
        generateWeeklyReportUseCase = GenerateWeeklyReportUseCase(repository),
        saveUserProfileUseCase = SaveUserProfileUseCase(repository),
    )

    private fun CoachViewModel.success(): CoachUiState.Success = uiState.value as CoachUiState.Success

    private fun profile(name: String, weight: Double = 80.0) = UserProfile(
        id = 1L,
        name = name,
        age = 34,
        sex = BiologicalSex.MALE,
        height = 180.0,
        weight = weight,
        bodyFat = 15.0,
        activityLevel = "Gemiddeld actief",
        goal = "Sterker worden",
        calorieTarget = 2_500,
        proteinTarget = 160,
        carbsTarget = 300,
        fatTarget = 70,
        trainingFocus = "Kracht",
    )

    private fun goalAdvice(summary: String, fallbackContext: AiFallbackContext? = null) = GoalAdvice(
        bmr = 1_800,
        maintenanceCalories = 2_500,
        activityMultiplier = 1.4,
        calorieTarget = 2_500,
        proteinTarget = 160,
        carbsTarget = 300,
        fatTarget = 70,
        trainingFocus = "Kracht",
        summary = summary,
        source = com.trainiq.domain.model.GoalAdviceSource.LOCAL_CALCULATION,
        fallbackContext = fallbackContext,
    )

    private class FakeCoachRepository(profile: UserProfile?) : CoachRepository {
        val profile = MutableStateFlow(profile)
        val saveStarted = CompletableDeferred<Unit>()
        var saveGate: CompletableDeferred<Unit>? = null
        var goalAdviceResult: GoalAdvice? = null
        var adviceCalls = 0
        var adviceForWeight: (suspend (Double) -> GoalAdvice)? = null
        var weeklyReportResult: WeeklyReportResult? = null
        var weeklyReportGate: CompletableDeferred<Unit>? = null
        var weeklyReportFailure: Throwable? = null
        var weeklyReportCalls = 0
        private val savedAdvice = MutableStateFlow<SavedGoalAdvice?>(null)
        private val overview = MutableStateFlow(
            CoachOverview(
                weeklyReport = "Week stabiel",
                trainingInsights = emptyList(),
                nutritionCoachMessage = "Voeding stabiel",
                profile = this.profile.value,
            ),
        )

        override fun observeCoachOverview(): Flow<CoachOverview> = overview

        override suspend fun generateGoalAdvice(
            height: Double,
            weight: Double,
            bodyFat: Double,
            age: Int,
            sex: BiologicalSex,
            activityLevel: String,
            goal: String,
            manualCalorieTarget: Int?,
        ): GoalAdvice {
            adviceCalls++
            return adviceForWeight?.invoke(weight) ?: goalAdviceResult ?: error("Not used")
        }

        override suspend fun generateWeeklyReport(): WeeklyReportResult {
            weeklyReportCalls++
            weeklyReportGate?.await()
            weeklyReportFailure?.let { throw it }
            return weeklyReportResult ?: error("Not used")
        }

        override fun observeUserProfile(): Flow<UserProfile?> = profile

        override fun observeSavedGoalAdvice(): Flow<SavedGoalAdvice?> = savedAdvice

        override suspend fun saveProfile(profile: UserProfile, savedGoalAdvice: SavedGoalAdvice?) {
            saveStarted.complete(Unit)
            saveGate?.await()
            this.profile.value = profile
            this.savedAdvice.value = savedGoalAdvice
        }
    }
}
