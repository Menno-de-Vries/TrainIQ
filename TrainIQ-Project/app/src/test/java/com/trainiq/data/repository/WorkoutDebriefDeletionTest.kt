package com.trainiq.data.repository

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutDebriefDeletionTest {
    @Test
    fun sessionDeletionWaitsForGenerationWorkCancellation() = runBlocking {
        val cancellationStarted = CompletableDeferred<Unit>()
        val finishCancellation = CompletableDeferred<Unit>()
        val operations = mutableListOf<String>()

        val deletion = async {
            cancelWorkoutDebriefBeforeSessionDelete(
                sessionId = 42L,
                generationId = "generation-42",
                cancel = { sessionId, generationId ->
                    operations += "cancel-start:$sessionId:$generationId"
                    cancellationStarted.complete(Unit)
                    finishCancellation.await()
                    operations += "cancel-complete:$sessionId:$generationId"
                },
                delete = { sessionId -> operations += "delete:$sessionId" },
            )
        }

        cancellationStarted.await()
        assertEquals(listOf("cancel-start:42:generation-42"), operations)

        finishCancellation.complete(Unit)
        deletion.await()

        assertEquals(
            listOf(
                "cancel-start:42:generation-42",
                "cancel-complete:42:generation-42",
                "delete:42",
            ),
            operations,
        )
    }

    @Test
    fun sessionWithoutGenerationSkipsCancellationAndStillDeletes() = runBlocking {
        val operations = mutableListOf<String>()

        cancelWorkoutDebriefBeforeSessionDelete(
            sessionId = 42L,
            generationId = null,
            cancel = { _, _ -> operations += "cancel" },
            delete = { sessionId -> operations += "delete:$sessionId" },
        )

        assertEquals(listOf("delete:42"), operations)
    }

    @Test
    fun deletionCancelsAnInFlightProviderCallBeforeRemovingTheSession() = runBlocking {
        val providerStarted = CompletableDeferred<Unit>()
        var providerCancelled = false
        var sessionDeleted = false
        val providerCall = launch {
            runWorkoutDebriefForCurrentGeneration(
                currentGeneration = "generation-42",
                expectedGeneration = "generation-42",
            ) {
                try {
                    providerStarted.complete(Unit)
                    awaitCancellation()
                } finally {
                    providerCancelled = true
                }
            }
        }

        providerStarted.await()
        cancelWorkoutDebriefBeforeSessionDelete(
            sessionId = 42L,
            generationId = "generation-42",
            cancel = { _, _ -> providerCall.cancelAndJoin() },
            delete = { sessionDeleted = true },
        )

        assertTrue(providerCancelled)
        assertTrue(sessionDeleted)
    }
}
