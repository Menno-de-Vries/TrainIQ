package com.trainiq.data.datasource

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.trainiq.domain.model.HealthConnectAvailability
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
                availability = HealthConnectAvailability.UNAVAILABLE,
                message = "Health Connect wordt op dit toestel niet ondersteund.",
            )

            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> HealthConnectStatus(
                availability = HealthConnectAvailability.NEEDS_INSTALL,
                message = "Installeer of update Health Connect om stappen te synchroniseren.",
            )

            HealthConnectClient.SDK_AVAILABLE -> fetchConnectedStatus()
            else -> HealthConnectStatus(
                availability = HealthConnectAvailability.ERROR,
                message = "De beschikbaarheid van Health Connect kon niet worden bepaald.",
            )
        }
    }

    private suspend fun fetchConnectedStatus(): HealthConnectStatus {
        return runCatching {
            val client = HealthConnectClient.getOrCreate(context)
            val grantedPermissions = client.permissionController.getGrantedPermissions()
            if (!grantedPermissions.contains(readStepsPermission)) {
                HealthConnectStatus(
                    availability = HealthConnectAvailability.NEEDS_PERMISSION,
                    message = "Geef toegang tot stappen om je dagactiviteit te tonen.",
                )
            } else {
                val start = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant()
                val end = Instant.now()
                val steps = client.readRecords(
                    ReadRecordsRequest(
                        recordType = StepsRecord::class,
                        timeRangeFilter = TimeRangeFilter.between(start, end),
                    ),
                ).records.sumOf { it.count.toInt() }
                HealthConnectStatus(
                    availability = HealthConnectAvailability.CONNECTED,
                    stepsToday = steps,
                    message = "Stappen van vandaag zijn gesynchroniseerd.",
                )
            }
        }.getOrElse { throwable ->
            HealthConnectStatus(
                availability = HealthConnectAvailability.ERROR,
                message = throwable.message ?: "Health Connect kon niet worden gelezen.",
            )
        }
    }
}
