package com.trainiq.core.datastore

import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HealthConnectSyncCoordinatorTest {
    @Test
    fun competingSyncReadsTheCommittedPredecessorInsteadOfOverwritingIt() = runTest {
        val coordinator = HealthConnectSyncCoordinator()
        val started = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        var cache = 0
        var secondStarted = false
        val first = async {
            coordinator.sync {
                val snapshot = cache
                started.complete(Unit)
                release.await()
                cache = snapshot + 1
            }
        }
        started.await()
        val second = async {
            coordinator.sync {
                secondStarted = true
                cache += 1
            }
        }
        runCurrent()
        assertFalse(secondStarted)
        release.complete(Unit)
        first.await()
        second.await()
        assertEquals(2, cache)
    }

    @Test
    fun privateResetClearsAfterSuspendedSyncAndCannotBeRepopulatedByIt() = runTest {
        val coordinator = HealthConnectSyncCoordinator()
        val started = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        var cache = "before"
        val sync = async {
            coordinator.sync {
                started.complete(Unit)
                release.await()
                cache = "provider result"
            }
        }
        started.await()
        val reset = async { coordinator.reset { cache = "" } }
        runCurrent()
        assertFalse(reset.isCompleted)
        release.complete(Unit)
        sync.await()
        reset.await()
        assertEquals("", cache)
        assertEquals("", coordinator.sync { cache })
    }

    @Test
    fun canceledWaiterNeverStartsProviderOperation() = runTest {
        val coordinator = HealthConnectSyncCoordinator()
        val started = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        var waiterStarted = false
        val owner = async { coordinator.sync { started.complete(Unit); release.await() } }
        started.await()
        val waiter = async { coordinator.sync { waiterStarted = true } }
        runCurrent()
        waiter.cancelAndJoin()
        release.complete(Unit)
        owner.await()
        assertFalse(waiterStarted)
        assertTrue(waiter.isCancelled)
    }

    @Test
    fun canceledOwnerReleasesOwnershipForResetAndNextSync() = runTest {
        val coordinator = HealthConnectSyncCoordinator()
        val started = CompletableDeferred<Unit>()
        val owner = async { coordinator.sync { started.complete(Unit); awaitCancellation() } }
        started.await()
        owner.cancelAndJoin()
        var cleared = false
        coordinator.reset { cleared = true }
        assertTrue(cleared)
        assertEquals("next", coordinator.sync { "next" })
    }

    @Test
    fun providerAndResetFailuresReleaseOwnershipForRecovery() = runTest {
        val coordinator = HealthConnectSyncCoordinator()
        assertTrue(runCatching { coordinator.sync<Unit> { throw IOException("provider unavailable") } }.isFailure)
        assertTrue(runCatching { coordinator.reset { throw IOException("storage unavailable") } }.isFailure)
        assertEquals("recovered", coordinator.sync { "recovered" })
    }
}
