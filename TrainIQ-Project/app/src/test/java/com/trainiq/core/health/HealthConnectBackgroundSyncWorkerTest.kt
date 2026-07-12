package com.trainiq.core.health

import com.trainiq.domain.model.HealthConnectState
import com.trainiq.domain.model.HealthConnectStatus
import com.trainiq.domain.model.HealthMetricStatus
import com.trainiq.domain.model.HealthMetricSyncState
import com.trainiq.domain.model.HealthMetricType
import java.io.IOException
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HealthConnectBackgroundSyncWorkerTest {
    @Test
    fun backgroundSyncRetriesGlobalOrPerMetricFailuresOnly() {
        val connectedWithFailedMetric = HealthConnectStatus(
            state = HealthConnectState.CONNECTED,
            message = "Connected with a partial failure.",
            metricStatuses = listOf(
                HealthMetricStatus(HealthMetricType.STEPS, HealthMetricSyncState.SYNCED),
                HealthMetricStatus(HealthMetricType.HEART_RATE, HealthMetricSyncState.FAILED),
            ),
        )
        val denied = HealthConnectStatus(
            state = HealthConnectState.PERMISSION_REQUIRED,
            message = "Permission denied.",
            metricStatuses = listOf(
                HealthMetricStatus(HealthMetricType.STEPS, HealthMetricSyncState.DENIED),
            ),
        )
        val unavailable = HealthConnectStatus(
            state = HealthConnectState.UNSUPPORTED,
            message = "Health Connect unavailable.",
            metricStatuses = listOf(
                HealthMetricStatus(HealthMetricType.STEPS, HealthMetricSyncState.UNAVAILABLE),
            ),
        )

        assertTrue(shouldRetryHealthConnectBackgroundSync(connectedWithFailedMetric))
        assertTrue(shouldRetryHealthConnectBackgroundSync(HealthConnectStatus(HealthConnectState.ERROR, message = "Transient error.")))
        assertFalse(shouldRetryHealthConnectBackgroundSync(denied))
        assertFalse(shouldRetryHealthConnectBackgroundSync(unavailable))
        assertFalse(shouldRetryHealthConnectBackgroundSync(HealthConnectStatus(HealthConnectState.NO_DATA, message = "No data.")))
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
