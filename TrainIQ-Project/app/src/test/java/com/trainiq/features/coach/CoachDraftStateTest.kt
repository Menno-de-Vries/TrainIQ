package com.trainiq.features.coach

import androidx.lifecycle.SavedStateHandle
import com.trainiq.domain.model.BiologicalSex
import com.trainiq.domain.model.CoachOverview
import com.trainiq.domain.model.GoalAdvice
import com.trainiq.domain.model.SavedGoalAdvice
import com.trainiq.domain.model.UserProfile
import com.trainiq.domain.model.WeeklyReportResult
import com.trainiq.domain.repository.CoachRepository
import com.trainiq.domain.usecase.GenerateGoalAdviceUseCase
import com.trainiq.domain.usecase.GenerateWeeklyReportUseCase
import com.trainiq.domain.usecase.ObserveCoachUseCase
import com.trainiq.domain.usecase.ObserveSavedGoalAdviceUseCase
import com.trainiq.domain.usecase.ObserveUserProfileUseCase
import com.trainiq.domain.usecase.SaveUserProfileUseCase
import java.io.Serializable
import kotlinx.coroutines.CompletableDeferred
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

    private class FakeCoachRepository(profile: UserProfile?) : CoachRepository {
        val profile = MutableStateFlow(profile)
        val saveStarted = CompletableDeferred<Unit>()
        var saveGate: CompletableDeferred<Unit>? = null
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
        ): GoalAdvice = error("Not used")

        override suspend fun generateWeeklyReport(): WeeklyReportResult = error("Not used")

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
