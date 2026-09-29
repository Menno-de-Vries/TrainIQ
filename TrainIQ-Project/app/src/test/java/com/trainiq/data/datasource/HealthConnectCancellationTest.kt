package com.trainiq.data.datasource

import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HealthConnectCancellationTest {
    @Test
    fun cancellationDuringSuspendedOperationPropagatesWithoutRunningFailureFallback() = runBlocking {
        val operationStarted = CompletableDeferred<Unit>()
        val fallbackInvoked = AtomicBoolean(false)
        val operation = async {
            healthConnectResultOf {
                operationStarted.complete(Unit)
                awaitCancellation()
            }.getOrElse {
                fallbackInvoked.set(true)
                "fallback"
            }
        }

        operationStarted.await()
        operation.cancelAndJoin()

        assertTrue(operation.isCancelled)
        assertFalse(fallbackInvoked.get())
    }

    @Test
    fun ordinaryProviderFailureRemainsAFailedResultForFallbackHandling() = runBlocking {
        val result = healthConnectResultOf<String> {
            throw IOException("Health Connect temporarily unavailable")
        }

        assertTrue(result.isFailure)
        assertEquals("Health Connect temporarily unavailable", result.exceptionOrNull()?.message)
        assertEquals("cached result", result.getOrElse { "cached result" })
    }
}
