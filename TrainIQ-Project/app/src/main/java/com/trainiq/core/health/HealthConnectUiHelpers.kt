package com.trainiq.core.health

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

internal data class HealthConnectPermissionCopy(
    val permission: String,
    val label: String,
)

internal val HealthConnectReadPermissions = setOf(
    HealthPermission.getReadPermission(StepsRecord::class),
    HealthPermission.getReadPermission(HeartRateRecord::class),
    HealthPermission.getReadPermission(SleepSessionRecord::class),
    HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
    HealthPermission.getReadPermission(WeightRecord::class),
    HealthPermission.getReadPermission(ExerciseSessionRecord::class),
)

internal val HealthConnectPermissionCopyBySignal = listOf(
    HealthConnectPermissionCopy(HealthPermission.getReadPermission(StepsRecord::class), "Stappen"),
    HealthConnectPermissionCopy(HealthPermission.getReadPermission(HeartRateRecord::class), "Hartslag"),
    HealthConnectPermissionCopy(HealthPermission.getReadPermission(SleepSessionRecord::class), "Slaap"),
    HealthConnectPermissionCopy(HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class), "Actieve calorieen"),
    HealthConnectPermissionCopy(HealthPermission.getReadPermission(WeightRecord::class), "Gewicht"),
    HealthConnectPermissionCopy(HealthPermission.getReadPermission(ExerciseSessionRecord::class), "Workouts"),
)

internal fun healthConnectPermissionResultMessage(grantedPermissions: Set<String>): String? {
    val grantedLabels = HealthConnectPermissionCopyBySignal
        .filter { it.permission in grantedPermissions }
        .map { it.label }
    if (grantedLabels.size == HealthConnectPermissionCopyBySignal.size) return null
    val deniedLabels = HealthConnectPermissionCopyBySignal
        .filterNot { it.permission in grantedPermissions }
        .map { it.label }
    return if (grantedLabels.isEmpty()) {
        "Geen Health Connect-signalen gekoppeld. Je kunt TrainIQ blijven gebruiken en later per signaal toegang geven."
    } else {
        "${grantedLabels.joinToString()} zijn gekoppeld. Nog niet gekoppeld: ${deniedLabels.joinToString()}. TrainIQ gebruikt de beschikbare signalen en markeert ontbrekende data apart."
    }
}

@Composable
fun rememberHealthConnectPermissionRequester(onPermissionsResult: () -> Unit): () -> Unit {
    val context = LocalContext.current
    return {
        context.startActivity(Intent(context, HealthConnectPermissionsRationaleActivity::class.java))
    }
}

@Composable
fun HealthConnectRefreshOnResume(onRefresh: () -> Unit, refreshOnFirstResume: Boolean = true) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnRefresh = rememberUpdatedState(onRefresh)
    DisposableEffect(lifecycleOwner, refreshOnFirstResume) {
        var hasSeenFirstResume = false
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (refreshOnFirstResume || hasSeenFirstResume) {
                    currentOnRefresh.value()
                }
                hasSeenFirstResume = true
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}
