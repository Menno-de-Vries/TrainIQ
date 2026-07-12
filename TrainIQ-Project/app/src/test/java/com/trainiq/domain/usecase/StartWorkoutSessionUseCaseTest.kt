package com.trainiq.domain.usecase

import com.trainiq.domain.model.ActiveWorkoutSession
import com.trainiq.domain.model.ActiveWorkoutSetDraft
import com.trainiq.domain.model.Exercise
import com.trainiq.domain.model.ExerciseHistory
import com.trainiq.domain.model.LoggedSet
import com.trainiq.domain.model.ProgressionSuggestion
import com.trainiq.domain.model.ReadinessLevel
import com.trainiq.domain.model.SetType
import com.trainiq.domain.model.WorkoutCompletionResult
import com.trainiq.domain.model.WorkoutCompletionSummary
import com.trainiq.domain.model.WorkoutDay
import com.trainiq.domain.model.WorkoutExercisePlan
import com.trainiq.domain.model.WorkoutLoggingSummary
import com.trainiq.domain.model.WorkoutOverview
import com.trainiq.domain.repository.WorkoutRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class StartWorkoutSessionUseCaseTest {
    @Test
    fun failsBeforeRepositoryStartWhenWorkoutHasNoExercises() = runTest {
        val repository = FakeWorkoutRepository(
            day = WorkoutDay(id = 7L, routineId = 1L, name = "Push", orderIndex = 0, exercises = emptyList()),
        )
        val useCase = StartWorkoutSessionUseCase(repository)

        val error = try {
            useCase(7L)
            fail("Expected empty workouts to fail before repository start")
            return@runTest
        } catch (expected: IllegalStateException) {
            expected
        }

        assertEquals("Voeg eerst oefeningen toe aan deze sessie voordat je start.", error.message)
        assertEquals(0, repository.startedDrafts.size)
    }

    @Test
    fun combinesPlanAndProgressionDraftsBeforeStartingSession() = runTest {
        val bench = Exercise(id = 3L, name = "Bench Press", muscleGroup = "Chest", equipment = "Barbell")
        val day = WorkoutDay(
            id = 7L,
            routineId = 1L,
            name = "Push",
            orderIndex = 0,
            exercises = listOf(
                WorkoutExercisePlan(
                    id = 4L,
                    exercise = bench,
                    targetSets = 3,
                    repRange = "8-10",
                    restSeconds = 120,
                    targetWeightKg = 80.0,
                    targetRpe = 8.0,
                ),
            ),
        )
        val activeSession = ActiveWorkoutSession(
            sessionId = 10L,
            dayId = 7L,
            routineId = 1L,
            startedAt = 1_000L,
            updatedAt = 1_000L,
            loggedSets = emptyList(),
            drafts = emptyMap(),
            collapsedExerciseIds = emptySet(),
            restTimerEndsAt = null,
            restTimerTotalSeconds = 0,
        )
        val repository = FakeWorkoutRepository(
            day = day,
            suggestions = listOf(
                ProgressionSuggestion(
                    exerciseId = 3L,
                    exerciseName = "Bench Press",
                    suggestedWeightKg = 82.5,
                    suggestedReps = "8-10",
                    lastSessionAvgRpe = 7.5f,
                    readinessSignal = ReadinessLevel.INCREASE,
                    lastLoggedWeightKg = 80.0,
                    lastLoggedReps = "8",
                ),
            ),
            session = activeSession,
        )
        val useCase = StartWorkoutSessionUseCase(repository)

        val result = useCase(7L)

        assertEquals(day, result.workout)
        assertEquals(activeSession, result.session)
        assertEquals(listOf(3L), result.progressionSuggestions.map { it.exerciseId })
        assertEquals(ActiveWorkoutSetDraft(weight = "80", reps = "8", rpe = "7.5"), repository.startedDrafts.getValue(4L))
    }

    @Test
    fun discardActiveWorkoutSessionUseCaseTargetsSessionIdInsteadOfRequestedDay() = runTest {
        val repository = FakeWorkoutRepository(day = null)
        val useCase = DiscardActiveWorkoutSessionUseCase(repository)

        useCase(42L)

        assertEquals(42L, repository.discardedSessionId)
    }
}

private class FakeWorkoutRepository(
    private val day: WorkoutDay?,
    private val suggestions: List<ProgressionSuggestion> = emptyList(),
    private val session: ActiveWorkoutSession? = null,
) : WorkoutRepository {
    override suspend fun refreshWorkoutDebrief(sessionId: Long) =
        com.trainiq.domain.repository.WorkoutDebriefRefreshOutcome.SESSION_MISSING
    var startedDrafts: Map<Long, ActiveWorkoutSetDraft> = emptyMap()
    var discardedSessionId: Long? = null

    override suspend fun getWorkoutDay(dayId: Long): WorkoutDay? = day
    override suspend fun getProgressionSuggestions(dayId: Long): List<ProgressionSuggestion> = suggestions
    override suspend fun getCurrentActiveWorkoutSession(): ActiveWorkoutSession? = session
    override suspend fun getOrStartActiveWorkoutSession(
        dayId: Long,
        initialDrafts: Map<Long, ActiveWorkoutSetDraft>,
    ): ActiveWorkoutSession {
        startedDrafts = initialDrafts
        return requireNotNull(session)
    }

    override fun observeWorkoutOverview(): Flow<WorkoutOverview> = emptyFlow()
    override fun observeWorkoutLoggingSummary(dayId: Long): Flow<WorkoutLoggingSummary> = emptyFlow()
    override fun observeExerciseHistory(exerciseId: Long): Flow<ExerciseHistory> = emptyFlow()
    override suspend fun getNextWorkoutDay(): WorkoutDay? = null
    override suspend fun updateActiveWorkoutDraft(exerciseId: Long, draft: ActiveWorkoutSetDraft): ActiveWorkoutSession? = null
    override suspend fun logActiveWorkoutSet(dayId: Long, set: LoggedSet, draft: ActiveWorkoutSetDraft, restSeconds: Int): ActiveWorkoutSession = error("unused")
    override suspend fun updateActiveWorkoutSet(setId: Long, set: LoggedSet, draft: ActiveWorkoutSetDraft, restSeconds: Int): ActiveWorkoutSession? = null
    override suspend fun updateActiveWorkoutSetType(setId: Long, setType: SetType): ActiveWorkoutSession? = null
    override suspend fun deleteActiveWorkoutSet(setId: Long): ActiveWorkoutSession? = null
    override suspend fun undoWorkoutLogEvent(eventId: Long): ActiveWorkoutSession? = null
    override suspend fun setActiveWorkoutCollapsed(exerciseId: Long, collapsed: Boolean): ActiveWorkoutSession? = null
    override suspend fun updateActiveWorkoutRestTimer(endsAt: Long?, totalSeconds: Int): ActiveWorkoutSession? = null
    override suspend fun finishActiveWorkout(dayId: Long): WorkoutCompletionResult = error("unused")
    override suspend fun getWorkoutCompletionSummary(sessionId: Long): WorkoutCompletionSummary? = null
    override suspend fun discardActiveWorkout(dayId: Long) = Unit
    override suspend fun discardActiveWorkoutSession(sessionId: Long) {
        discardedSessionId = sessionId
    }
    override suspend fun setActiveRoutine(routineId: Long) = Unit
    override suspend fun finishWorkout(dayId: Long, durationSeconds: Long, loggedSets: List<LoggedSet>) = error("unused")
    override suspend fun createRoutine(name: String, description: String) = Unit
    override suspend fun updateRoutine(routineId: Long, name: String, description: String) = Unit
    override suspend fun deleteRoutine(routineId: Long) = Unit
    override suspend fun searchExercises(query: String) = emptyList<Exercise>()
    override suspend fun reorderExercises(dayId: Long, orderedIds: List<Long>) = Unit
    override suspend fun setSupersetGroup(workoutExerciseIds: List<Long>, groupId: Long?) = Unit
    override suspend fun replaceExerciseInPlan(workoutExerciseId: Long, newExerciseId: Long) = Unit
    override suspend fun replaceExerciseInActiveWorkout(workoutExerciseId: Long, newExerciseId: Long): ActiveWorkoutSession? = null
    override suspend fun updateWorkoutExercisePlan(workoutExerciseId: Long, targetSets: Int, repRange: String, restSeconds: Int, targetWeightKg: Double, targetRpe: Double, setType: SetType) = Unit
    override suspend fun addSetToExercise(workoutExerciseId: Long) = Unit
    override suspend fun updateRoutineSet(set: com.trainiq.domain.model.RoutineSet) = Unit
    override suspend fun updateRoutineSetType(setId: Long, setType: SetType) = Unit
    override suspend fun updateRoutineSetReps(setId: Long, targetReps: Int) = Unit
    override suspend fun updateRoutineSetWeight(setId: Long, targetWeightKg: Double) = Unit
    override suspend fun updateRoutineSetRestTime(setId: Long, restSeconds: Int) = Unit
    override suspend fun deleteRoutineSet(setId: Long) = Unit
    override suspend fun moveRoutineSet(workoutExerciseId: Long, orderedSetIds: List<Long>) = Unit
    override suspend fun addWorkoutDay(routineId: Long, name: String) = Unit
    override suspend fun removeWorkoutDay(dayId: Long) = Unit
    override suspend fun addExerciseToDay(dayId: Long, name: String, muscleGroup: String, equipment: String, targetSets: Int, repRange: String, restSeconds: Int, targetWeightKg: Double, targetRpe: Double) = Unit
    override suspend fun addExerciseToRoutine(routineId: Long, name: String, muscleGroup: String, equipment: String, targetSets: Int, repRange: String, restSeconds: Int, targetWeightKg: Double, targetRpe: Double) = Unit
    override suspend fun removeExerciseFromDay(workoutExerciseId: Long) = Unit
    override suspend fun deleteWorkoutSession(sessionId: Long) = Unit
    override suspend fun generateAiRoutine(daysPerWeek: Int, equipment: String, targetFocus: String, experienceLevel: String, sessionDurationMinutes: Int, includeDeload: Boolean) = error("unused")
    override suspend fun saveGeneratedRoutine(routine: com.trainiq.domain.model.GeneratedRoutine) = Unit
}
