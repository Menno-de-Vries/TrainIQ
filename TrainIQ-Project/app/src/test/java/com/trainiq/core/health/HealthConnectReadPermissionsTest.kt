package com.trainiq.core.health

import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import java.io.File
import org.junit.Assert.assertEquals
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

    @Test
    fun permissionResultMessageCelebratesPartialSuccessAndNamesDeniedSignals() {
        val message = healthConnectPermissionResultMessage(
            grantedPermissions = setOf(HealthPermission.getReadPermission(StepsRecord::class)),
        ).orEmpty()

        assertTrue(message.contains("Stappen zijn gekoppeld"))
        assertTrue(message.contains("Nog niet gekoppeld"))
        assertFalse(message.contains("alle zes Health Connect-signalen samen nodig"))
    }

    @Test
    fun permissionResultMessageReturnsNullWhenAllSignalsAreGranted() {
        assertEquals(
            null,
            healthConnectPermissionResultMessage(grantedPermissions = HealthConnectReadPermissions),
        )
    }

    @Test
    fun rationaleReasonsExplainEachRequestedSignalWithoutMakingAllMandatory() {
        assertEquals(
            HealthConnectPermissionCopyBySignal.map { it.label },
            HealthConnectRationaleReasons.map { it.title },
        )
        assertEquals(HealthConnectReadPermissions.size, HealthConnectRationaleReasons.size)
        assertTrue(HealthConnectRationaleReasons.all { it.description.isNotBlank() })
        assertFalse(
            HealthConnectRationaleReasons
                .joinToString(" ") { "${it.title} ${it.description}" }
                .contains("verplicht", ignoreCase = true),
        )
    }

    @Test
    fun manifestDeclaresHealthConnectProviderVisibility() {
        val manifest = File("src/main/AndroidManifest.xml").readText()

        assertTrue(manifest.contains("""<package android:name="com.google.android.apps.healthdata" />"""))
        assertTrue(manifest.contains("""<package android:name="com.google.android.healthconnect.controller" />"""))
        assertTrue(manifest.contains("""<package android:name="com.android.vending" />"""))
    }

    @Test
    fun manifestDeclaresBackgroundReadPermissionUsedBySchedulerGate() {
        val manifest = File("src/main/AndroidManifest.xml").readText()

        assertTrue(manifest.contains("""android.permission.health.READ_HEALTH_DATA_IN_BACKGROUND"""))
    }
}
