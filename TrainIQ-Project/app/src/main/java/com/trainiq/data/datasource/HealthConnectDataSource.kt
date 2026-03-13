package com.trainiq.data.datasource

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.trainiq.domain.model.HealthConnectState
import com.trainiq.domain.model.HealthConnectStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HealthConnectDataSource @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private val readStepsPermission = HealthPermission.getReadPermission(StepsRecord::class)

    fun permissions(): Set<String> = setOf(readStepsPermission)

    fun providerInstallIntent(): Intent =
        Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.google.android.apps.healthdata"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    fun settingsIntent(): Intent =
        Intent(HealthConnectClient.ACTION_HEALTH_CONNECT_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    suspend fun getStatus(): HealthConnectStatus {
        return when (HealthConnectClient.getSdkStatus(context)) {
            HealthConnectClient.SDK_UNAVAILABLE -> HealthConnectStatus(
                state = HealthConnectState.UNSUPPORTED,
                message = "Health Connect is not supported on this device.",
            )

            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> HealthConnectStatus(
                state = HealthConnectState.PROVIDER_MISSING,
                message = "Install or update Health Connect before TrainIQ can read steps.",
            )

            HealthConnectClient.SDK_AVAILABLE -> fetchConnectedStatus()
            else -> HealthConnectStatus(
                state = HealthConnectState.ERROR,
                message = "Unable to determine Health Connect availability.",
            )
        }
    }

    private suspend fun fetchConnectedStatus(): HealthConnectStatus {
        return runCatching {
            val client = HealthConnectClient.getOrCreate(context)
            val grantedPermissions = client.permissionController.getGrantedPermissions()
            if (!grantedPermissions.contains(readStepsPermission)) {
                HealthConnectStatus(
                    state = HealthConnectState.PERMISSION_REQUIRED,
                    message = "Grant step permission to connect Health Connect.",
                )
            } else {
                val start = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant()
                val end = Instant.now()
                val records = client.readRecords(
                    ReadRecordsRequest(
                        recordType = StepsRecord::class,
                        timeRangeFilter = TimeRangeFilter.between(start, end),
                    ),
                ).records
                val steps = records.sumOf { it.count.toInt() }
                if (records.isEmpty() || steps <= 0) {
                    HealthConnectStatus(
                        state = HealthConnectState.NO_DATA,
                        stepsToday = 0,
                        message = "Health Connect is connected, but no step data is available for today yet.",
                        lastSyncedAt = System.currentTimeMillis(),
                    )
                } else {
                    HealthConnectStatus(
                        state = HealthConnectState.CONNECTED,
                        stepsToday = steps,
                        message = "Health Connect is connected and today's steps were read successfully.",
                        lastSyncedAt = System.currentTimeMillis(),
                    )
                }
            }
        }.getOrElse { throwable ->
            HealthConnectStatus(
                state = HealthConnectState.ERROR,
                message = throwable.message ?: "Health Connect could not be read right now.",
            )
        }
    }
}
