package com.trainiq.core.health

import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HealthConnectReadPermissionsTest {
    @Test
    fun readPermissionsRequestActiveCaloriesNotTotalCalories() {
        assertTrue(
            HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class) in HealthConnectReadPermissions,
        )
        assertFalse(
            HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class) in HealthConnectReadPermissions,
        )
    }
}
