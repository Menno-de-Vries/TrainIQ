package com.trainiq.core.health

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HealthConnectBackgroundSyncPolicyTest {
    @Test
    fun backgroundSyncUsesStableUniqueWorkNameAndWorkManagerSafeInterval() {
        assertEquals("health_connect_background_sync", HealthConnectBackgroundSyncWorkName)
        assertTrue(HealthConnectBackgroundSyncInterval.toMinutes() >= 15)
        assertTrue(HealthConnectBackgroundBackoff.toMinutes() >= 10)
    }
}
