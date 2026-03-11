package com.trainiq.data.datasource

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HealthConnectDataSource @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    suspend fun getTodaySteps(): Int = runCatching {
        if (HealthConnectClient.getSdkStatus(context) != HealthConnectClient.SDK_AVAILABLE) {
            return 0
        }
        val client = HealthConnectClient.getOrCreate(context)
        val permission = HealthPermission.getReadPermission(StepsRecord::class)
        val granted = client.permissionController.getGrantedPermissions()
        if (!granted.contains(permission)) {
            return 0
        }
        val start = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant()
        val end = Instant.now()
        client.readRecords(
            ReadRecordsRequest(
                recordType = StepsRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end),
            ),
        ).records.sumOf { it.count.toInt() }
    }.getOrDefault(0)
}
