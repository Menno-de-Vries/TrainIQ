package com.trainiq.data.repository

import com.trainiq.core.database.WorkoutSessionEntity
import com.trainiq.core.database.WorkoutSetEntity
import com.trainiq.domain.model.Exercise
import com.trainiq.domain.model.ReadinessLevel
import com.trainiq.domain.model.SetType
import com.trainiq.domain.model.WorkoutDay
import com.trainiq.domain.model.WorkoutExercisePlan
import org.junit.Assert.assertEquals
import org.junit.Test

class WorkoutProgressionSuggestionCalculatorTest {
    private val calculator = WorkoutProgressionSuggestionCalculator()
    private val day = WorkoutDay(
        id = 7L,
        routineId = 1L,
        name = "Push",
        orderIndex = 0,
        exercises = listOf(
            WorkoutExercisePlan(
                id = 44L,
                exercise = Exercise(id = 3L, name = "Bench Press", muscleGroup = "Chest", equipment = "Barbell"),
                targetSets = 2,
                repRange = "5",
                restSeconds = 120,
            ),
        ),
    )

    @Test
    fun calculate_increasesCompoundWeightWhenRecentSessionsHitTargetsWithRecoveryRoom() {
        val suggestions = calculator.calculate(
            day = day,
            sessions = completedSessions(1_000L, 2_000L),
            sets = listOf(
                completedSet(id = 1L, sessionId = 1L, weight = 100.0, reps = 5, rpe = 7.0, rir = 3),
                completedSet(id = 2L, sessionId = 2L, weight = 102.5, reps = 5, rpe = 7.0, rir = 2),
            ),
        )

        assertEquals(1, suggestions.size)
        assertEquals(ReadinessLevel.INCREASE, suggestions.single().readinessSignal)
        assertEquals(105.0, suggestions.single().suggestedWeightKg, 0.0)
        assertEquals(102.5, suggestions.single().lastLoggedWeightKg!!, 0.0)
    }

    @Test
    fun calculate_deloadsWhenRecentRirIsTooLow() {
        val suggestions = calculator.calculate(
            day = day,
            sessions = completedSessions(1_000L),
            sets = listOf(completedSet(id = 1L, sessionId = 1L, weight = 100.0, reps = 5, rpe = 8.5, rir = 0)),
        )

        assertEquals(ReadinessLevel.DELOAD, suggestions.single().readinessSignal)
        assertEquals(90.0, suggestions.single().suggestedWeightKg, 0.0)
    }

    @Test
    fun calculate_excludesWarmupsDraftSessionsAndIncompleteSetsFromProgressionSignals() {
        val suggestions = calculator.calculate(
            day = day,
            sessions = listOf(
                WorkoutSessionEntity(id = 1L, date = 1_000L, duration = 1_800L, status = "DRAFT", completed = false),
                WorkoutSessionEntity(id = 2L, date = 2_000L, duration = 1_800L, status = "COMPLETED", completed = true),
            ),
            sets = listOf(
                completedSet(id = 1L, sessionId = 1L, weight = 200.0, reps = 5, rpe = 6.0, rir = 4),
                completedSet(id = 2L, sessionId = 2L, weight = 60.0, reps = 5, rpe = 6.0, rir = 4, setType = SetType.WARM_UP),
                completedSet(id = 3L, sessionId = 2L, weight = 80.0, reps = 0, rpe = 6.0, rir = 4),
            ),
        )

        assertEquals(emptyList<Any>(), suggestions)
    }

    @Test
    fun calculate_detectsPlateauWhenThreeRecentEstimatedMaxesAreNearlyFlat() {
        val suggestions = calculator.calculate(
            day = day,
            sessions = completedSessions(1_000L, 2_000L, 3_000L),
            sets = listOf(
                completedSet(id = 1L, sessionId = 1L, weight = 100.0, reps = 5, rpe = 7.0, rir = 3),
                completedSet(id = 2L, sessionId = 2L, weight = 100.2, reps = 5, rpe = 7.0, rir = 3),
                completedSet(id = 3L, sessionId = 3L, weight = 100.4, reps = 5, rpe = 7.0, rir = 3),
            ),
        )

        assertEquals(ReadinessLevel.PLATEAU, suggestions.single().readinessSignal)
        assertEquals(100.4, suggestions.single().suggestedWeightKg, 0.0)
    }

    @Test
    fun unknownOrInvalidRepTargetsNeverCountAsAchievedForAnIncrease() {
        listOf("AMRAP", "30 sec", "0", "12-8", "8/12", "999999999999").forEach { target ->
            assertEquals(target, ReadinessLevel.MAINTAIN, suggestionForTarget(target).readinessSignal)
        }
    }

    @Test
    fun numericRangesUseTheUpperBoundIncludingTypographicDashes() {
        listOf("5", "3-5", "3–5", "3 – 5").forEach { target ->
            assertEquals(target, ReadinessLevel.INCREASE, suggestionForTarget(target).readinessSignal)
        }
        assertEquals(ReadinessLevel.MAINTAIN, suggestionForTarget("5-6").readinessSignal)
    }

    @Test
    fun unknownTargetStillAllowsFatigueDeload() {
        assertEquals(ReadinessLevel.DELOAD, suggestionForTarget("AMRAP", rpe = 10.0).readinessSignal)
    }

    @Test
    fun duplicateExercisePlansUseTheirOwnRepTargets() {
        val plan = day.exercises.single()
        val suggestions = calculator.calculate(
            day.copy(exercises = listOf(plan.copy(repRange = "5"), plan.copy(id = 45L, repRange = "10"))),
            completedSessions(1_000L, 2_000L),
            listOf(completedSet(1L, 1L, 100.0, 5, 7.0, 3), completedSet(2L, 2L, 102.5, 5, 7.0, 3)),
        )
        assertEquals(listOf(ReadinessLevel.INCREASE, ReadinessLevel.MAINTAIN), suggestions.map { it.readinessSignal })
    }

    @Test
    fun unratedSetsDoNotDiluteRecordedHighEffort() {
        val suggestions = calculator.calculate(day, completedSessions(1_000L), listOf(
            completedSet(1L, 1L, 100.0, 5, 10.0, null),
            completedSet(2L, 1L, 100.0, 5, 0.0, null),
        ))
        assertEquals(10f, suggestions.single().lastSessionAvgRpe)
        assertEquals(ReadinessLevel.DELOAD, suggestions.single().readinessSignal)
    }

    @Test
    fun allUnratedSetsKeepEffortUnknown() {
        val suggestions = calculator.calculate(day, completedSessions(1_000L),
            listOf(completedSet(1L, 1L, 100.0, 5, 0.0, null)))
        assertEquals(null, suggestions.single().lastSessionAvgRpe)
        assertEquals(ReadinessLevel.MAINTAIN, suggestions.single().readinessSignal)
    }

    private fun suggestionForTarget(target: String, rpe: Double = 7.0) = calculator.calculate(
        day.copy(exercises = day.exercises.map { it.copy(repRange = target) }),
        completedSessions(1_000L, 2_000L),
        listOf(
            completedSet(1L, 1L, 100.0, 5, rpe, 3),
            completedSet(2L, 2L, 102.5, 5, rpe, 3),
        ),
    ).single()

    private fun completedSessions(vararg dates: Long): List<WorkoutSessionEntity> =
        dates.mapIndexed { index, date ->
            WorkoutSessionEntity(
                id = index + 1L,
                date = date,
                duration = 1_800L,
                status = "COMPLETED",
                completed = true,
            )
        }

    private fun completedSet(
        id: Long,
        sessionId: Long,
        weight: Double,
        reps: Int,
        rpe: Double,
        rir: Int?,
        setType: SetType = SetType.NORMAL,
    ) = WorkoutSetEntity(
        id = id,
        sessionId = sessionId,
        exerciseId = 3L,
        weight = weight,
        reps = reps,
        rpe = rpe,
        repsInReserve = rir,
        setType = setType.name,
        completed = true,
    )
}
