package com.trainiq.data.repository

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WorkoutDebriefGenerationTest {
    @Test
    fun staleGenerationSkipsTheAiServiceCall() = runBlocking {
        var aiServiceCalls = 0

        val result = runWorkoutDebriefForCurrentGeneration(
            currentGeneration = "replacement-generation",
            expectedGeneration = "old-generation",
        ) {
            aiServiceCalls += 1
            "debrief"
        }

        assertNull(result)
        assertEquals(0, aiServiceCalls)
    }

    @Test
    fun currentGenerationRunsTheAiServiceOnce() = runBlocking {
        var aiServiceCalls = 0

        val result = runWorkoutDebriefForCurrentGeneration(
            currentGeneration = "current-generation",
            expectedGeneration = "current-generation",
        ) {
            aiServiceCalls += 1
            "debrief"
        }

        assertEquals("debrief", result)
        assertEquals(1, aiServiceCalls)
    }

    @Test
    fun legacyImportWithoutGenerationDoesNotCallAiService() = runBlocking {
        var aiServiceCalls = 0
        val result = runWorkoutDebriefForCurrentGeneration(
            currentGeneration = "",
            expectedGeneration = null,
        ) {
            aiServiceCalls += 1
            "debrief"
        }
        assertNull(result)
        assertEquals(0, aiServiceCalls)
    }

    @Test
    fun legacyQueuedWorkCannotClaimMigrationBackfilledSessionGeneration() = runBlocking {
        var aiServiceCalls = 0

        val result = runWorkoutDebriefForCurrentGeneration(
            currentGeneration = "migration-backfilled-generation",
            expectedGeneration = null,
        ) {
            aiServiceCalls += 1
            "debrief"
        }

        assertNull(result)
        assertEquals(0, aiServiceCalls)
    }

    @Test
    fun deletedSessionAfterSnapshotSkipsTheAiServiceCall() = runBlocking {
        var aiServiceCalls = 0

        val result = runWorkoutDebriefForCurrentGeneration(
            currentGeneration = "deleted-generation",
            expectedGeneration = "deleted-generation",
            isGenerationCurrent = { false },
        ) {
            aiServiceCalls += 1
            "debrief"
        }

        assertNull(result)
        assertEquals(0, aiServiceCalls)
    }
}
