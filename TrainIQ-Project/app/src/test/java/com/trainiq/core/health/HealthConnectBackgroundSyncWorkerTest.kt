package com.trainiq.core.health

import com.trainiq.domain.model.HealthConnectState
import java.io.IOException
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HealthConnectBackgroundSyncWorkerTest {
    @Test
    fun backgroundSyncRetriesOnlyTransientHealthConnectFailures() {
        assertTrue(shouldRetryHealthConnectBackgroundSync(HealthConnectState.ERROR))
        assertFalse(shouldRetryHealthConnectBackgroundSync(HealthConnectState.CONNECTED))
        assertFalse(shouldRetryHealthConnectBackgroundSync(HealthConnectState.NO_DATA))
        assertFalse(shouldRetryHealthConnectBackgroundSync(HealthConnectState.PERMISSION_REQUIRED))
        assertFalse(shouldRetryHealthConnectBackgroundSync(HealthConnectState.PROVIDER_MISSING))
        assertFalse(shouldRetryHealthConnectBackgroundSync(HealthConnectState.UNSUPPORTED))
    }

    @Test
    fun backgroundSyncFailureRetryPolicy_retriesTransientFailures() {
        assertTrue(shouldRetryHealthConnectBackgroundSyncFailure(IOException("binder unavailable")))
        assertTrue(shouldRetryHealthConnectBackgroundSyncFailure(IllegalStateException("temporary unavailable")))
    }

    @Test
    fun backgroundSyncFailureRetryPolicy_stopsPermanentPermissionAndConfigurationLoops() {
        assertFalse(shouldRetryHealthConnectBackgroundSyncFailure(SecurityException("permission revoked")))
        assertFalse(shouldRetryHealthConnectBackgroundSyncFailure(IllegalArgumentException("bad request")))
        assertFalse(shouldRetryHealthConnectBackgroundSyncFailure(UnsupportedOperationException("provider unsupported")))
    }
}
