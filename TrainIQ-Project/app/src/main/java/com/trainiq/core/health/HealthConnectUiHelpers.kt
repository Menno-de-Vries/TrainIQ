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

internal data class HealthConnectRationaleReason(
    val title: String,
    val description: String,
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

internal val HealthConnectRationaleReasons = listOf(
    HealthConnectRationaleReason(
        title = "Stappen",
        description = "Stappen tonen bewegingsvolume en consistentie, zodat TrainIQ een actieve week kan onderscheiden van alleen korte zware trainingsmomenten.",
    ),
    HealthConnectRationaleReason(
        title = "Hartslag",
        description = "Hartslag geeft een signaal voor intensiteit en herstel, zodat adviezen minder hoeven te gokken naar stress, conditie en vermoeidheid.",
    ),
    HealthConnectRationaleReason(
        title = "Slaap",
        description = "Slaap helpt TrainIQ inschatten hoe hersteld je bent en maakt readiness, deload-signalen en sessieadvies concreter.",
    ),
    HealthConnectRationaleReason(
        title = "Actieve calorieen",
        description = "Actieve calorieen geven context voor energieverbruik bovenop rustverbruik en helpen training en voeding realistischer naast elkaar te zetten.",
    ),
    HealthConnectRationaleReason(
        title = "Gewicht",
        description = "Gewichtstrends geven voortgang context. TrainIQ gebruikt ze om prestaties en voeding te verbinden aan echte lichaamsverandering.",
    ),
    HealthConnectRationaleReason(
        title = "Workouts",
        description = "Workouts geven duur en trainingscontext, zodat TrainIQ Health Connect-data kan koppelen aan je trainingsbelasting.",
    ),
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
