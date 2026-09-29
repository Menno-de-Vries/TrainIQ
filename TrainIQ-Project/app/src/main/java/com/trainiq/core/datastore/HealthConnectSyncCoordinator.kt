package com.trainiq.core.datastore

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** One owner for the complete provider read/cache commit and local private-data reset. */
internal class HealthConnectSyncCoordinator {
    private val mutex = Mutex()

    suspend fun <T> sync(block: suspend () -> T): T = mutex.withLock { block() }

    // Reset follows any active read/commit, so its cleared data cannot be overwritten
    // later by that operation. New explicit syncs remain possible without revoking access.
    suspend fun reset(clear: suspend () -> Unit) = mutex.withLock { clear() }
}
