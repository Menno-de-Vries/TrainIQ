package com.trainiq.ai.services

import com.trainiq.domain.model.LoggedSet
import com.trainiq.domain.model.SetType
import com.trainiq.domain.model.WorkoutDebriefSource
import com.trainiq.domain.model.buildWorkoutAnalysisSnapshot
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class WorkoutLocalDebriefTest {
    @Test fun unknownEffortReportsActualFactsWithoutInventingIntensityOrRecovery() {
        val snapshot = buildWorkoutAnalysisSnapshot(sets())
        val result = fallbackWorkoutDebriefResult(9999.0, null, analysisSnapshot = snapshot)

        assertTrue(result.summary.contains("3 sets · 26 herhalingen · totaalvolume 1160 kg"))
        assertTrue(result.summary.contains("Gemiddelde set-RPE onbekend · 0 van 3 sets ingevuld"))
        assertEquals("Nog geen eerdere vergelijkbare training gevonden.", result.progressionFeedback)
        assertTrue(result.recommendation.contains("RPE ontbreekt"))
        assertEquals("MAINTAIN", result.intensitySignal)
        assertNull(result.recoveryScore)
        assertFalse(result.nextLoadTarget.contains("RPE onder 8"))
        assertTrue(result.recoveryAdvice.contains("niet af te leiden"))
    }

    @Test fun partialEffortDescribesCoverageAndComparisonBasisWithoutExtendingMeanToUnknownSets() {
        val snapshot = buildWorkoutAnalysisSnapshot(sets(3.0, 8.0, 0.0))
        val result = fallbackWorkoutDebriefResult(1160.0, 20.0, analysisSnapshot = snapshot)

        assertTrue(result.summary.contains("Gemiddelde set-RPE 5,5 · 2 van 3 sets ingevuld"))
        assertTrue(result.progressionFeedback.contains("Vergelijkingsvolume 960 kg"))
        assertTrue(result.progressionFeedback.contains("20,0%"))
        assertTrue(result.recommendation.contains("RPE is deels ingevuld"))
        assertFalse(result.risks.any { it.contains("herstel") })
        assertEquals("MAINTAIN", result.intensitySignal)
    }

    @Test fun knownHighAndLowEffortStayFactualWithoutHistoryOrReadinessEvidence() {
        for (rpe in listOf(5.0, 9.5)) {
            val snapshot = buildWorkoutAnalysisSnapshot(sets(rpe, rpe, rpe))
            val result = fallbackWorkoutDebriefResult(1160.0, null, analysisSnapshot = snapshot)

            assertTrue(result.summary.contains("Gemiddelde set-RPE ${formatAiPercentNl(rpe)} · 3 van 3 sets ingevuld"))
            assertTrue(result.recommendation.contains("geen eerdere vergelijkbare training"))
            assertEquals("MAINTAIN", result.intensitySignal)
            assertNull(result.recoveryScore)
        }
    }

    @Test fun transportFailureAndInvalidResponseRetainSnapshotFacts() = runTest {
        val snapshot = buildWorkoutAnalysisSnapshot(sets(3.0, 8.0, 0.0))
        val generators = listOf<AiJsonGenerator>(
            object : AiJsonGenerator {
                override suspend fun generateJson(request: AiRouteRequest): AiRouteResult =
                    throw java.io.IOException("private transport detail")
            },
            object : AiJsonGenerator {
                override suspend fun generateJson(request: AiRouteRequest) =
                    AiRouteResult(AiProvider.GEMINI, "gemini-2.5-flash", "invalid json")
            },
        )
        for (generator in generators) {
            val result = WorkoutDebriefService(generator).generateWorkoutDebrief(
                totalVolume = 9999.0, progression = null, distribution = "Borst 1", avgRpe = null,
                topExercises = "Bench", weeklyFrequency = 1, analysisSnapshot = snapshot,
            )
            assertEquals(WorkoutDebriefSource.LOCAL_FALLBACK, result.source)
            assertTrue(result.summary.contains("totaalvolume 1160 kg"))
            assertTrue(result.summary.contains("2 van 3 sets ingevuld"))
            assertFalse(result.summary.contains("private transport detail"))
        }
    }

    @Test fun cancellationPropagatesWithoutReturningFallback() = runTest {
        val cancellation = CancellationException("screen left")
        val generator = object : AiJsonGenerator {
            override suspend fun generateJson(request: AiRouteRequest): AiRouteResult = throw cancellation
        }
        val error = runCatching {
            WorkoutDebriefService(generator).generateWorkoutDebrief(
                totalVolume = 1160.0, progression = null, distribution = "Borst 1", avgRpe = null,
                topExercises = "Bench", weeklyFrequency = 1, analysisSnapshot = buildWorkoutAnalysisSnapshot(sets()),
            )
        }.exceptionOrNull()
        assertSame(cancellation, error)
    }

    private fun sets(vararg rpes: Double) = listOf(
        LoggedSet(exerciseId = 1L, weight = 20.0, reps = 10, rpe = rpes.getOrElse(0) { 0.0 }, setType = SetType.WARM_UP),
        LoggedSet(exerciseId = 1L, weight = 60.0, reps = 8, rpe = rpes.getOrElse(1) { 0.0 }),
        LoggedSet(exerciseId = 1L, weight = 60.0, reps = 8, rpe = rpes.getOrElse(2) { 0.0 }),
        LoggedSet(exerciseId = 1L, weight = 999.0, reps = 10, rpe = 10.0, completed = false),
    )
}
