package com.trainiq.data.datasource

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HealthConnectPermissionRevocationTest {
    @Test
    fun permissionRequiredBranchClearsCachedHealthMetrics() {
        val source = File("src/main/java/com/trainiq/data/datasource/HealthConnectDataSource.kt").readText()
        val branch = source.substringAfter("if (grantedMetrics.isEmpty()) {").substringBefore("} else {")

        assertTrue(branch.contains("preferencesRepository.clearHealthConnectSyncPreferences()"))
        assertFalse(branch.contains("readMetricsFromStoredState"))
        assertFalse(branch.contains("lastSyncedAt ="))
    }
}
