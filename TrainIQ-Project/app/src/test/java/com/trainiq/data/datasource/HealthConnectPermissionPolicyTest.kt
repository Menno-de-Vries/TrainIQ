package com.trainiq.data.datasource

import com.trainiq.core.datastore.HealthConnectSyncPreferences
import com.trainiq.data.mapper.toDomainMetrics
import com.trainiq.domain.model.HealthMetricSyncState
import com.trainiq.domain.model.HealthMetricType
import com.trainiq.domain.model.HealthConnectStepDiagnostic
import com.trainiq.domain.model.HealthConnectStepDiagnosticFreshness
import java.io.File
import java.time.LocalDateTime
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HealthConnectPermissionPolicyTest {
    @Test
    fun canReadStepsWhenOnlyStepPermissionIsGranted() {
        assertTrue(
            hasHealthConnectPermission(
                grantedPermissions = setOf("steps"),
                requiredPermission = "steps",
            ),
        )
    }

    @Test
    fun doesNotRequireUnrelatedPermissionsForStepRefresh() {
        assertFalse(
            hasHealthConnectPermission(
                grantedPermissions = setOf("heart_rate", "sleep"),
                requiredPermission = "steps",
            ),
        )
    }

    @Test
    fun partialMetricPermissionsKeepGrantedMetricsAvailableAndDenyMissingMetrics() {
        val statuses = buildHealthMetricPermissionStatuses(
            grantedPermissions = setOf("steps", "sleep"),
            requiredPermissionsByMetric = mapOf(
                HealthMetricType.STEPS to "steps",
                HealthMetricType.HEART_RATE to "heart_rate",
                HealthMetricType.SLEEP to "sleep",
            ),
            lastSyncedAt = 123L,
        )

        assertEquals(HealthMetricSyncState.STALE, statuses.single { it.metric == HealthMetricType.STEPS }.state)
        assertEquals(HealthMetricSyncState.DENIED, statuses.single { it.metric == HealthMetricType.HEART_RATE }.state)
        assertEquals(HealthMetricSyncState.STALE, statuses.single { it.metric == HealthMetricType.SLEEP }.state)
    }

    @Test
    fun missingPermissionsDoNotReportGrantedMetricsAsDenied() {
        val statuses = buildHealthMetricPermissionStatuses(
            grantedPermissions = setOf("steps"),
            requiredPermissionsByMetric = mapOf(
                HealthMetricType.STEPS to "steps",
                HealthMetricType.ACTIVE_CALORIES to "calories",
            ),
            lastSyncedAt = null,
        )

        assertEquals(HealthMetricSyncState.SYNCING, statuses.single { it.metric == HealthMetricType.STEPS }.state)
        assertEquals(HealthMetricSyncState.DENIED, statuses.single { it.metric == HealthMetricType.ACTIVE_CALORIES }.state)
    }

    @Test
    fun successfulMetricReadsRemainSyncedWhenUnrelatedMetricFails() {
        val statuses = buildHealthMetricSyncStatuses(
            metrics = HealthConnectSyncMetricTypes,
            failedMetrics = mapOf(
                HealthMetricType.HEART_RATE to "Hartslag kan nu niet worden gelezen.",
            ),
            lastSyncedAt = 456L,
        )

        assertEquals(HealthMetricSyncState.SYNCED, statuses.single { it.metric == HealthMetricType.STEPS }.state)
        assertEquals(HealthMetricSyncState.FAILED, statuses.single { it.metric == HealthMetricType.HEART_RATE }.state)
        assertEquals(HealthMetricSyncState.SYNCED, statuses.single { it.metric == HealthMetricType.SLEEP }.state)
        assertEquals("Hartslag kan nu niet worden gelezen.", statuses.single { it.metric == HealthMetricType.HEART_RATE }.message)
    }

    @Test
    fun failedMetricReadsDoNotReceiveLastSyncedTimestamp() {
        val statuses = buildHealthMetricSyncStatuses(
            metrics = listOf(HealthMetricType.STEPS, HealthMetricType.WORKOUTS),
            failedMetrics = mapOf(HealthMetricType.WORKOUTS to "Workouts lezen is mislukt."),
            lastSyncedAt = 789L,
        )

        assertEquals(789L, statuses.single { it.metric == HealthMetricType.STEPS }.lastSyncedAt)
        assertEquals(null, statuses.single { it.metric == HealthMetricType.WORKOUTS }.lastSyncedAt)
    }

    @Test
    fun incrementalFailurePayloadPreservesCachedMetricsAndToken() {
        val cachedState = HealthConnectCacheState(aggregatedStepsToday = 1234L)
        val storedState = HealthConnectSyncPreferences(
            changesToken = "existing-token",
            cacheStateJson = "{}",
            lastSyncedAt = 987L,
        )

        val payload = cachedIncrementalFailurePayload(
            cachedState = cachedState,
            storedState = storedState,
            failureMessage = "Changes API tijdelijk niet beschikbaar.",
        )

        assertEquals(cachedState, payload.cacheState)
        assertEquals("existing-token", payload.nextChangesToken)
        assertEquals(987L, payload.lastSyncedAt)
        assertEquals(
            HealthMetricSyncState.FAILED,
            payload.metricStatuses.single { it.metric == HealthMetricType.STEPS }.state,
        )
        assertEquals(1234, payload.cacheState.toDomainMetrics().stepsToday)
    }

    @Test
    fun fullSyncWithoutChangesTokenReportsFailedMetricsAndDoesNotPretendSynced() {
        val payload = fullSyncTokenFailurePayload(
            cacheState = HealthConnectCacheState(aggregatedStepsToday = 4321L),
            failureMessage = "ChangesToken kon niet worden opgehaald.",
            lastSyncedAt = 654L,
        )

        assertEquals("", payload.nextChangesToken)
        assertEquals(654L, payload.lastSyncedAt)
        assertEquals(4321, payload.cacheState.toDomainMetrics().stepsToday)
        assertTrue(payload.metricStatuses.all { it.state == HealthMetricSyncState.FAILED })
    }

    @Test
    fun metricChangesTokensRoundTripByMetricName() {
        val encoded = encodeMetricChangesTokens(
            mapOf(
                HealthMetricType.STEPS to "steps-token",
                HealthMetricType.HEART_RATE to "heart-rate-token",
                HealthMetricType.SLEEP to "sleep-token",
            ),
        )

        assertEquals(
            mapOf(
                HealthMetricType.STEPS to "steps-token",
                HealthMetricType.HEART_RATE to "heart-rate-token",
                HealthMetricType.SLEEP to "sleep-token",
            ),
            decodeMetricChangesTokens(encoded),
        )
    }

    @Test
    fun blankMetricChangesTokensFallBackToLegacyTokenForExistingInstalls() {
        val storedState = HealthConnectSyncPreferences(
            changesToken = "legacy-all-metrics-token",
            changesTokensJson = "",
            cacheStateJson = "{}",
            lastSyncedAt = 321L,
        )

        val tokens = storedState.resolvedMetricChangesTokens(HealthConnectSyncMetricTypes)

        assertTrue(HealthConnectSyncMetricTypes.all { tokens[it] == "legacy-all-metrics-token" })
        assertFalse(tokens.containsKey(HealthMetricType.WEIGHT))
    }

    @Test
    fun syncPayloadKeepsPerMetricTokensSeparateFromCompatibilityToken() {
        val payload = SyncPayload(
            cacheState = HealthConnectCacheState(aggregatedStepsToday = 777L),
            nextChangesTokens = mapOf(
                HealthMetricType.STEPS to "steps-token",
                HealthMetricType.WORKOUTS to "workouts-token",
            ),
            lastSyncedAt = 555L,
        )

        assertEquals("steps-token", payload.nextChangesTokens[HealthMetricType.STEPS])
        assertEquals("workouts-token", payload.nextChangesTokens[HealthMetricType.WORKOUTS])
        assertEquals("steps-token", payload.nextChangesToken)
    }

    @Test
    fun cacheStateDropsMetricsAfterPermissionRevocation() {
        val cacheState = HealthConnectCacheState(
            aggregatedStepsToday = 777L,
            samsungHealthStepsToday = 888L,
            samsungHealthDirectStepsToday = 999L,
            displayStepsToday = 999L,
            stepRecords = listOf(CachedStepRecord("steps", 1L, 2L, 100)),
            heartRateRecords = listOf(CachedHeartRateRecord("hr", 1L, 2L, 60, 64, 2L, 3)),
            sleepSessionRecords = listOf(CachedSleepSessionRecord("sleep", 1L, 2L, 480)),
            caloriesBurnedRecords = listOf(CachedCaloriesBurnedRecord("cal", 1L, 2L, 300.0)),
            weightRecords = listOf(CachedWeightRecord("weight", 1L, 80.0)),
            exerciseSessionRecords = listOf(CachedExerciseSessionRecord("workout", 1L, 2L, 45, "Squat")),
        )

        val redacted = cacheState.onlyMetrics(setOf(HealthMetricType.STEPS, HealthMetricType.SLEEP))

        assertEquals(777L, redacted.aggregatedStepsToday)
        assertEquals(888L, redacted.samsungHealthStepsToday)
        assertEquals(999L, redacted.samsungHealthDirectStepsToday)
        assertEquals(999L, redacted.displayStepsToday)
        assertEquals(1, redacted.stepRecords.size)
        assertEquals(1, redacted.sleepSessionRecords.size)
        assertTrue(redacted.heartRateRecords.isEmpty())
        assertTrue(redacted.caloriesBurnedRecords.isEmpty())
        assertTrue(redacted.weightRecords.isEmpty())
        assertTrue(redacted.exerciseSessionRecords.isEmpty())

        val withoutSteps = cacheState.onlyMetrics(setOf(HealthMetricType.SLEEP))
        assertEquals(0L, withoutSteps.aggregatedStepsToday)
        assertNull(withoutSteps.samsungHealthStepsToday)
        assertNull(withoutSteps.samsungHealthDirectStepsToday)
        assertNull(withoutSteps.displayStepsToday)
        assertTrue(withoutSteps.stepRecords.isEmpty())
    }

    @Test
    fun revokedMetricTokensAreNotRetainedWhenReEncodingGrantedMetricTokens() {
        val retainedTokens = mapOf(
            HealthMetricType.STEPS to "steps-token",
            HealthMetricType.HEART_RATE to "revoked-heart-token",
            HealthMetricType.SLEEP to "sleep-token",
        ).filterKeys { it in setOf(HealthMetricType.STEPS, HealthMetricType.SLEEP) }

        val decoded = decodeMetricChangesTokens(encodeMetricChangesTokens(retainedTokens))

        assertEquals("steps-token", decoded[HealthMetricType.STEPS])
        assertEquals("sleep-token", decoded[HealthMetricType.SLEEP])
        assertFalse(decoded.containsKey(HealthMetricType.HEART_RATE))
    }

    @Test
    fun domainMetricsPreferAggregateStepsOverRawRecordSum() {
        val metrics = HealthConnectCacheState(
            aggregatedStepsToday = 12820L,
            stepRecords = listOf(
                CachedStepRecord("raw-1", 1L, 2L, 12384),
            ),
        ).toDomainMetrics()

        assertEquals(12820, metrics.stepsToday)
    }

    @Test
    fun domainMetricsPreferSamsungComparableDisplayStepsWhenSamsungHealthIsHigher() {
        val metrics = HealthConnectCacheState(
            aggregatedStepsToday = 12_820L,
            samsungHealthStepsToday = 13_240L,
            displayStepsToday = 13_240L,
        ).toDomainMetrics()

        assertEquals(13_240, metrics.stepsToday)
    }

    @Test
    fun domainMetricsPreferCachedDirectSamsungHealthStepsWhenAvailable() {
        val metrics = HealthConnectCacheState(
            aggregatedStepsToday = 180L,
            samsungHealthStepsToday = 180L,
            samsungHealthDirectStepsToday = 600L,
        ).toDomainMetrics()

        assertEquals(600, metrics.stepsToday)
    }

    @Test
    fun domainMetricsDoNotLetLowerSamsungExportHideHealthConnectAggregate() {
        val metrics = HealthConnectCacheState(
            aggregatedStepsToday = 12_820L,
            samsungHealthStepsToday = 12_500L,
            displayStepsToday = 12_820L,
        ).toDomainMetrics()

        assertEquals(12_820, metrics.stepsToday)
    }

    @Test
    fun domainMetricsIgnoreEmptySamsungAggregateCacheValue() {
        val metrics = HealthConnectCacheState(
            aggregatedStepsToday = 12_820L,
            samsungHealthStepsToday = 0L,
            samsungHealthDirectStepsToday = 0L,
        ).toDomainMetrics()

        assertEquals(12_820, metrics.stepsToday)
    }

    @Test
    fun todayStepAggregateRangeUsesLocalDateTimeDayBoundaries() {
        val now = LocalDateTime.of(2026, 6, 4, 18, 45, 30)
        val range = healthConnectTodayLocalDateTimeRange(now)

        assertEquals(LocalDateTime.of(2026, 6, 4, 0, 0), range.start)
        assertEquals(now, range.end)
    }

    @Test
    fun stepAggregateDoesNotFilterToSingleDataOrigin() {
        val source = java.io.File("src/main/java/com/trainiq/data/datasource/HealthConnectDataSource.kt").readText()
        val aggregateBody = source.substringAfter("private suspend fun aggregateStepsToday")
            .substringBefore("private suspend fun performFullSync")

        assertTrue(aggregateBody.contains("StepsRecord.COUNT_TOTAL"))
        assertTrue(aggregateBody.contains("healthConnectTodayLocalDateTimeRange"))
        assertFalse(aggregateBody.contains("DataOrigin"))
        assertFalse(aggregateBody.contains("dataOriginFilter"))
    }

    @Test
    fun stepWorkoutWindowDiagnosticIsSeparateFromDailyAggregate() {
        val source = java.io.File("src/main/java/com/trainiq/data/datasource/HealthConnectDataSource.kt").readText()
        val diagnosticBody = source.substringAfter("private suspend fun buildStepDiagnostic(")
            .substringBefore("private suspend fun readStepSourceSnapshotToday(")

        assertTrue(diagnosticBody.contains("aggregateStepsToday = stepAggregateSnapshot.healthConnectSteps"))
        assertTrue(diagnosticBody.contains("workoutWindowSteps = workoutWindowSnapshot?.steps"))
        assertTrue(source.contains("private suspend fun aggregateStepsForWorkoutWindowsToday("))
        assertTrue(source.contains("TimeRangeFilter.between(start, end)"))
        assertFalse(diagnosticBody.contains("aggregateStepsToday -"))
    }

    @Test
    fun samsungHealthStepComparisonUsesAggregateDataOriginFilterAndDisplayPolicy() {
        val source = File("src/main/java/com/trainiq/data/datasource/HealthConnectDataSource.kt").readText()
        val samsungAggregateBody = source.substringAfter("private suspend fun aggregateSamsungHealthStepsToday(")
            .substringBefore("private suspend fun aggregateStepsForWorkoutWindowsToday(")

        assertTrue(samsungAggregateBody.contains("AggregateRequest"))
        assertTrue(samsungAggregateBody.contains("StepsRecord.COUNT_TOTAL"))
        assertTrue(samsungAggregateBody.contains("dataOriginFilter"))
        assertTrue(samsungAggregateBody.contains("DataOrigin(packageName)"))
        assertTrue(samsungAggregateBody.contains("samsungPackageNames.mapNotNull"))
        assertTrue(samsungAggregateBody.contains("}.getOrNull()"))
        assertTrue(samsungAggregateBody.contains("}.maxOrNull()"))
        assertTrue(source.contains("KnownSamsungHealthPackageNames = setOf(\"com.sec.android.app.shealth\")"))
        assertTrue(source.contains("sourceSnapshot.samsungPackageNames + KnownSamsungHealthPackageNames"))
        assertTrue(source.contains("samsungRawStepRecordSum += record.count"))
        assertTrue(source.contains("resolveSamsungHealthVisibleSteps("))
        assertTrue(samsungAggregateBody.contains("takeIf { it > 0L }"))
        assertTrue(source.contains("withSamsungHealthLabelWhen(samsungHealthSteps != null)"))
        assertTrue(source.contains("resolveSamsungComparableDisplaySteps("))
        assertFalse(samsungAggregateBody.contains("samsungPackageNames.sumOf"))
        assertFalse(samsungAggregateBody.contains("stepRecords.sumOf"))
    }

    @Test
    fun samsungHealthDataSdkParityPlanRequiresExplicitAarAndPhysicalDevice() {
        val plan = File("../../docs/qa/samsung-health-step-parity-acceptance-2026-06-22.md").readText()
        val script = File("../scripts/collect-samsung-step-parity-evidence.ps1").readText()
        val installScript = File("../scripts/install-samsung-health-data-sdk-aar.ps1").readText()
        val buildScript = File("../scripts/build-samsung-step-parity-debug.ps1").readText()
        val buildGradle = File("build.gradle.kts").readText()
        val versionCatalog = File("../gradle/libs.versions.toml").readText()
        assertTrue(plan.contains("samsung-health-data-api-1.1.0.aar"))
        assertTrue(plan.contains("DataTypes.STEPS"))
        assertTrue(plan.contains("DataType.StepsType.TOTAL"))
        assertTrue(plan.contains("physical Samsung device"))
        assertTrue(plan.contains("not supported on emulators"))
        assertTrue(script.contains("com.sec.android.app.shealth"))
        assertTrue(script.contains("-ieq \".aar\""))
        assertTrue(script.contains("samsung-health-data-api"))
        assertTrue(script.contains("Legacy/other Samsung Health AAR files ignored"))
        assertTrue(script.contains("Samsung toegang geven"))
        assertTrue(script.contains("Diagnose kopieren"))
        assertTrue(script.contains("Samsung Health All steps"))
        assertTrue(script.contains("samsung-health-readiness.txt"))
        assertTrue(script.contains("Samsung Health required for Data SDK: 6.30.2 or later"))
        assertTrue(script.contains("Test-VersionAtLeast"))
        assertTrue(script.contains("versionName="))
        assertTrue(script.contains("parity-result.txt"))
        assertTrue(script.contains("TRAINIQ_UNDER_REPORTS"))
        assertTrue(script.contains("TrainIQ percent of Samsung Health"))
        assertTrue(script.contains("device-readiness.txt"))
        assertTrue(script.contains("Physical Samsung device likely"))
        assertTrue(script.contains("Android 10 or later"))
        assertTrue(script.contains("Device meets Samsung Health Data SDK runtime target"))
        assertTrue(script.contains("acceptance-gates.txt"))
        assertTrue(script.contains("Exact Samsung Health All steps parity proof ready"))
        assertTrue(script.contains("Samsung Health version 6.30.2 or later"))
        assertTrue(script.contains("Samsung and TrainIQ step values match"))
        assertTrue(script.contains("Samsung Health Data SDK emulator support: not supported"))
        assertTrue(script.contains("ro.kernel.qemu"))
        assertTrue(script.contains("logcat-crash-slice.txt"))
        assertTrue(installScript.contains("samsung-health-data-api*.aar"))
        assertTrue(installScript.contains("SourcePath"))
        assertTrue(installScript.contains("HelpSamsungDownload"))
        assertTrue(installScript.contains("Write-SamsungHealthDataSdkDownloadHelp"))
        assertTrue(installScript.contains("developer.samsung.com/health/data/overview.html"))
        assertTrue(installScript.contains("developer.samsung.com/codelab/health/steps-data.html"))
        assertTrue(installScript.contains("developer.samsung.com/health/data/release-note.html"))
        assertTrue(installScript.contains("accept Samsung's SDK terms"))
        assertTrue(installScript.contains("SourcePath is required unless -HelpSamsungDownload is used."))
        assertTrue(installScript.contains("Expand-Archive"))
        assertTrue(installScript.contains("Legacy/other Samsung Health AAR files ignored"))
        assertTrue(installScript.contains("Get-FileHash"))
        assertTrue(installScript.contains("checkSamsungHealthDataSdkReadiness"))
        assertTrue(installScript.contains("assembleSamsungHealthParityDebug"))
        assertTrue(installScript.contains("installSamsungHealthParityDebug"))
        assertTrue(installScript.contains("Destination already has"))
        assertTrue(installScript.contains("No samsung-health-data-api*.aar found"))
        assertTrue(buildScript.contains("build-samsung-step-parity-debug"))
        assertTrue(buildScript.contains("install-samsung-health-data-sdk-aar.ps1"))
        assertTrue(buildScript.contains("collect-samsung-step-parity-evidence.ps1"))
        assertTrue(buildScript.contains("checkSamsungHealthDataSdkReadiness"))
        assertTrue(buildScript.contains(":app:assembleSamsungHealthParityDebug"))
        assertTrue(buildScript.contains(":app:installSamsungHealthParityDebug"))
        assertTrue(buildScript.contains("Physical Samsung device required"))
        assertTrue(buildScript.contains("Samsung Health version 6.30.2 or later"))
        assertTrue(buildScript.contains("com.sec.android.app.shealth"))
        assertTrue(buildScript.contains("ro.kernel.qemu"))
        assertTrue(buildScript.contains("ANDROID_SERIAL"))
        assertTrue(buildScript.contains("Samsung toegang geven"))
        assertTrue(buildScript.contains("Diagnose kopieren"))
        assertTrue(buildScript.contains("Samsung Health Data SDK is not supported on emulators"))
        assertTrue(buildScript.contains("Samsung Health Data SDK API AAR readiness failed"))
        assertTrue(buildGradle.contains("implementation(fileTree(mapOf(\"dir\" to \"libs\", \"include\" to listOf(\"*.aar\"))))"))
        assertTrue(buildGradle.contains("SAMSUNG_HEALTH_DATA_SDK_AAR_PRESENT"))
        assertTrue(buildGradle.contains("SAMSUNG_HEALTH_NON_API_AAR_PRESENT"))
        assertTrue(buildGradle.contains("checkSamsungHealthDataSdkReadiness"))
        assertTrue(buildGradle.contains("trainiq.requireSamsungHealthDataSdk"))
        assertTrue(buildGradle.contains("assembleSamsungHealthParityDebug"))
        assertTrue(buildGradle.contains("installSamsungHealthParityDebug"))
        assertTrue(buildGradle.contains("Samsung Health Data SDK API AAR missing"))
        assertTrue(buildGradle.contains("physical Samsung Health All steps parity builds"))
        assertTrue(buildGradle.contains("file.extension.equals(\"aar\", ignoreCase = true)"))
        assertFalse(buildGradle.contains("file.extension == \"aar\""))
        val dataSdkAarFlagBody = buildGradle
            .substringAfter("\"SAMSUNG_HEALTH_DATA_SDK_AAR_PRESENT\"")
            .substringBefore("\"SAMSUNG_HEALTH_NON_API_AAR_PRESENT\"")
        assertTrue(dataSdkAarFlagBody.contains("file.name.contains(\"samsung-health-data-api\", ignoreCase = true)"))
        assertFalse(dataSdkAarFlagBody.contains("file.name.contains(\"samsung-health\", ignoreCase = true)"))
        assertTrue(buildGradle.contains("!file.name.contains(\"samsung-health-data-api\", ignoreCase = true)"))
        assertTrue(buildGradle.contains("id(\"kotlin-parcelize\")"))
        assertTrue(buildGradle.contains("implementation(libs.gson)"))
        assertTrue(buildGradle.contains("sourceCompatibility = JavaVersion.VERSION_17"))
        assertTrue(buildGradle.contains("targetCompatibility = JavaVersion.VERSION_17"))
        assertTrue(buildGradle.contains("jvmToolchain(17)"))
        assertTrue(versionCatalog.contains("com.google.code.gson"))

        val samsungHealthDataSdkAarPresent = File("libs")
            .takeIf { it.isDirectory }
            ?.walkTopDown()
            ?.any { file ->
                file.isFile &&
                    file.extension.equals("aar", ignoreCase = true) &&
                    file.name.contains("samsung-health-data-api", ignoreCase = true)
            }
            ?: false
        val directSource = File("src/main/java/com/trainiq/data/datasource/SamsungHealthDirectStepsDataSource.kt").readText()
        val appSource = File("src/main/java/com/trainiq").walkTopDown()
            .filter { file -> file.isFile && file.extension == "kt" && file.name != "SamsungHealthDirectStepsDataSource.kt" }
            .joinToString(separator = "\n") { file -> file.readText() }

        if (!samsungHealthDataSdkAarPresent) {
            assertFalse(appSource.contains("com.samsung.android.sdk.health.data"))
            assertFalse(appSource.contains("DataTypes.STEPS"))
        }

        assertTrue(directSource.contains("StatusSdkUnavailable"))
        assertTrue(directSource.contains("StatusLegacyAarPresent"))
        assertTrue(directSource.contains("StatusPermissionMissing"))
        assertTrue(directSource.contains("StatusSdkRead"))
        assertTrue(directSource.contains("samsungFailureStatus"))
        assertTrue(directSource.contains("ResolvablePlatformException"))
        assertTrue(directSource.contains("AuthorizationException"))
        assertTrue(directSource.contains("InvalidRequestException"))
        assertTrue(directSource.contains("PlatformInternalException"))
        assertTrue(directSource.contains("HealthDataException"))
        assertTrue(directSource.contains("StatusSdkResolvablePlatform"))
        assertTrue(directSource.contains("StatusSdkAuthorizationFailed"))
        assertTrue(directSource.contains("StatusSdkInvalidRequest"))
        assertTrue(directSource.contains("StatusSdkPlatformInternal"))
        assertTrue(directSource.contains("suspend fun requestTodayStepPermission(activity: Activity)"))
        assertTrue(directSource.contains(".samsungResolutionStatus(activity)"))
        assertTrue(directSource.contains("resolveSamsungAction(activity)"))
        assertTrue(directSource.contains("StatusSdkResolutionStarted"))
        assertTrue(directSource.contains("StatusSdkResolutionFailed"))
        assertTrue(directSource.contains("BuildConfig.SAMSUNG_HEALTH_DATA_SDK_AAR_PRESENT"))
        assertTrue(directSource.contains("BuildConfig.SAMSUNG_HEALTH_NON_API_AAR_PRESENT"))
        assertTrue(directSource.contains("unavailableStatus()"))
        assertTrue(directSource.contains("SamsungHealthPackageName = \"com.sec.android.app.shealth\""))
        assertTrue(directSource.contains("RequiredSamsungHealthVersionName = \"6.30.2\""))
        assertTrue(directSource.contains("SamsungHealthRuntimeReadiness"))
        assertTrue(directSource.contains("samsungHealthAndroidRuntimeReadiness"))
        assertTrue(directSource.contains("Build.VERSION_CODES.Q"))
        assertTrue(directSource.contains("StatusAndroidRuntimeTooOld"))
        assertTrue(directSource.contains("samsungHealthRuntimeReadiness"))
        assertTrue(directSource.contains("isSamsungHealthVersionAtLeast"))
        assertTrue(directSource.contains("!readiness.canUseDirectSdk"))
        assertTrue(directSource.contains("StatusRuntimeMissing"))
        assertTrue(directSource.contains("lager dan Samsung Health Data SDK minimum"))
        assertTrue(directSource.contains("Samsung Health runtime:"))
        assertTrue(directSource.contains("samsung-health-data-api*.aar ontbreekt"))
        assertTrue(directSource.contains("maar niet de benodigde samsung-health-data-api*.aar"))
        assertTrue(directSource.contains("Samsung Health Data SDK API AAR is gebundeld"))
        assertTrue(directSource.contains("HealthDataService"))
        assertTrue(directSource.contains("DataTypes"))
        assertTrue(directSource.contains("samsungStepsDataType"))
        assertTrue(directSource.contains("DataTypes.STEPS ontbreekt"))
        assertTrue(directSource.contains("samsungReadAccessType"))
        assertTrue(directSource.contains("AccessType.READ ontbreekt"))
        assertTrue(directSource.contains("samsungPermissionOf"))
        assertTrue(directSource.contains("Permission.of ontbreekt"))
        assertTrue(directSource.contains("StepsType"))
        assertTrue(directSource.contains("LocalTimeFilter"))
        assertTrue(directSource.contains("setSamsungLocalTimeFilter"))
        assertTrue(directSource.contains("setLocalTimeFilterWithGroup"))
        assertTrue(directSource.contains("LocalTimeGroup"))
        assertTrue(directSource.contains("LocalTimeGroupUnit"))
        assertTrue(directSource.contains("MINUTELY"))
        assertTrue(directSource.contains("setSamsungOrderingAscIfAvailable"))
        assertTrue(directSource.contains("setOrdering"))
        assertTrue(directSource.contains("\"ASC\""))
        assertTrue(directSource.contains("samsungClassOrNull"))
        assertTrue(directSource.contains("LocalTimeFilter builder ontbreekt"))
        assertTrue(directSource.contains("samsungStepsTotalAggregateOperation"))
        assertTrue(directSource.contains("samsungAggregateRequestBuilder"))
        assertTrue(directSource.contains("DataType.StepsType.TOTAL ontbreekt"))
        assertTrue(directSource.contains("DataType.StepsType.TOTAL.requestBuilder ontbreekt"))
        assertTrue(directSource.contains("aggregateData"))
        assertTrue(directSource.contains("samsungAggregateDataList"))
        assertTrue(directSource.contains("aggregate response dataList ontbreekt"))
        assertTrue(directSource.contains("samsungAggregateValue"))
        assertTrue(directSource.contains("aggregate response value ontbreekt"))
        assertTrue(directSource.contains("asLongStepValue"))
        assertTrue(directSource.contains("aggregate response value is geen getal"))
        assertTrue(directSource.contains("getGrantedPermissions"))
        assertTrue(directSource.contains("requestTodayStepPermission"))
        assertTrue(directSource.contains("requestPermissions"))
        assertTrue(directSource.contains("asSamsungPermissionSet"))
        assertTrue(directSource.contains("getGrantedPermissions"))
        assertTrue(directSource.contains("field.name == \"grantedPermissions\""))
        assertTrue(directSource.contains("readTodaySteps("))
    }

    @Test
    fun samsungHealthDataSdkTotalStepsRequestPrefersDocumentedGroupedAggregate() {
        val directSource = File("src/main/java/com/trainiq/data/datasource/SamsungHealthDirectStepsDataSource.kt").readText()
        val filterBody = directSource
            .substringAfter("private fun Any.setSamsungLocalTimeFilter(localTimeFilter: Any)")
            .substringBefore("private fun localTimeGroup()")
        val groupBody = directSource
            .substringAfter("private fun localTimeGroup()")
            .substringBefore("private fun samsungStepsTotalAggregateOperation()")
        val requestBody = directSource
            .substringAfter("private fun samsungStepsTotalAggregateRequest(localTimeFilter: Any)")
            .substringBefore("private fun Any.setSamsungLocalTimeFilter(localTimeFilter: Any)")

        assertTrue(filterBody.indexOf("setLocalTimeFilterWithGroup") < filterBody.indexOf("setLocalTimeFilter\""))
        assertTrue(filterBody.contains("runCatching"))
        assertTrue(filterBody.contains(".exceptionOrNull()"))
        assertTrue(filterBody.contains("localTimeGroup()"))
        assertTrue(groupBody.contains("samsungGroupUnitOrNull(\"HOURLY\")"))
        assertTrue(groupBody.contains("invokeKotlinCompanionFactory(groupClass, \"of\", hourly, 1)"))
        assertTrue(groupBody.contains("MINUTELY"))
        assertTrue(groupBody.contains("30"))
        assertTrue(requestBody.indexOf("setSamsungLocalTimeFilter") < requestBody.indexOf("setSamsungOrderingAscIfAvailable"))
        assertTrue(requestBody.indexOf("setSamsungOrderingAscIfAvailable") < requestBody.indexOf("build"))
    }

    @Test
    fun samsungHealthDataSdkSuspendBridgeResumesOriginalContinuationOnly() {
        val directSource = File("src/main/java/com/trainiq/data/datasource/SamsungHealthDirectStepsDataSource.kt").readText()
        val forwardingBody = directSource
            .substringAfter("private fun Continuation<Any>.forwarding(): Continuation<Any>")
            .substringBefore("private fun invokeKotlinCompanionFactory(")

        assertTrue(forwardingBody.contains("val downstream = this"))
        assertTrue(forwardingBody.contains("override val context: CoroutineContext = downstream.context"))
        assertTrue(forwardingBody.contains("downstream.resume(value)"))
        assertTrue(forwardingBody.contains("downstream.resumeWithException(throwable)"))
        assertFalse(forwardingBody.contains("::resume"))
        assertFalse(forwardingBody.contains("::resumeWithException"))
        assertFalse(forwardingBody.contains("EmptyCoroutineContext"))
    }

    @Test
    fun samsungHealthRuntimeVersionCheckMatchesDataSdkMinimum() {
        assertEquals(false, isSamsungHealthVersionAtLeast("6.30.1"))
        assertEquals(true, isSamsungHealthVersionAtLeast("6.30.2"))
        assertEquals(true, isSamsungHealthVersionAtLeast("6.30.2.031"))
        assertEquals(true, isSamsungHealthVersionAtLeast("6.31.0"))
        assertEquals(false, isSamsungHealthVersionAtLeast("6.29.9"))
        assertEquals(null, isSamsungHealthVersionAtLeast("onbekend"))
    }

    @Test
    fun samsungHealthRuntimeReadinessBlocksOnlyMissingOrTooOldRuntime() {
        val missing = samsungHealthRuntimeReadinessForVersion(installed = false)
        assertEquals(false, missing.canUseDirectSdk)
        assertTrue(missing.status.contains("app niet gevonden"))

        val tooOld = samsungHealthRuntimeReadinessForVersion("6.30.1")
        assertEquals(false, tooOld.canUseDirectSdk)
        assertTrue(tooOld.status.contains("lager dan Samsung Health Data SDK minimum"))

        val minimum = samsungHealthRuntimeReadinessForVersion("6.30.2")
        assertEquals(true, minimum.canUseDirectSdk)
        assertTrue(minimum.status.contains("voldoet aan Samsung Health Data SDK minimum"))

        val unknown = samsungHealthRuntimeReadinessForVersion("onbekend")
        assertEquals(true, unknown.canUseDirectSdk)
        assertTrue(unknown.status.contains("niet automatisch te bepalen"))
    }

    @Test
    fun stepDiagnosticKeepsAggregateTotalSeparateFromRawSources() {
        val diagnostic = HealthConnectStepDiagnostic(
            aggregateStepsToday = 12_345,
            samsungHealthStepsToday = 13_000,
            displaySteps = 13_000,
            queriedAt = 123L,
            sourceLabels = listOf("Samsung Health", "Jouw telefoon"),
            workoutWindowSteps = 1_250,
            workoutWindowSessionCount = 1,
        )

        assertEquals(13_000, diagnostic.displaySteps)
        assertEquals("Samsung Health, Jouw telefoon", diagnostic.sourceSummary)
        assertTrue(diagnostic.samsungHealthComparisonSummary.contains("Samsung Health-export 13000"))
        assertTrue(diagnostic.workoutWindowSummary.contains("1250 stappen"))
        assertTrue(diagnostic.workoutWindowSummary.contains("niet afgetrokken"))
    }

    @Test
    fun stepDiagnosticKeepsHigherHealthConnectAggregateWhenSamsungExportIsLower() {
        val diagnostic = HealthConnectStepDiagnostic(
            aggregateStepsToday = 12_345,
            samsungHealthStepsToday = 12_000,
            queriedAt = 123L,
            sourceLabels = listOf("Samsung Health"),
        )

        assertEquals(12_345, diagnostic.displaySteps)
        assertTrue(diagnostic.samsungHealthComparisonSummary.contains("Health Connect aggregate 12345 wordt getoond"))
        assertTrue(diagnostic.aggregateAuthorityLabel.contains("Health Connect aggregate"))
    }

    @Test
    fun stepDiagnosticExplainsWhenSamsungHealthShowsMoreThanHealthConnectVisibleSteps() {
        val diagnostic = HealthConnectStepDiagnostic(
            aggregateStepsToday = 180,
            samsungHealthStepsToday = 180,
            queriedAt = 123L,
            sourceLabels = listOf("Samsung Health"),
        )

        assertTrue(diagnostic.healthConnectVisibleStepSummary.contains("180 Samsung Health-stappen"))
        assertTrue(diagnostic.healthConnectVisibleStepSummary.contains("extra stappen nog niet naar Health Connect geschreven"))
    }

    @Test
    fun stepDiagnosticUsesHigherRawSamsungExportWhenAggregateUnderReports() {
        val diagnostic = HealthConnectStepDiagnostic(
            aggregateStepsToday = 180,
            samsungHealthStepsToday = 600,
            samsungHealthAggregateStepsToday = 180,
            samsungRawStepRecordSumToday = 600,
            queriedAt = 123L,
            sourceLabels = listOf("Samsung Health"),
        )

        assertEquals(600, diagnostic.displaySteps)
        assertTrue(diagnostic.samsungHealthComparisonSummary.contains("ruwe Health Connect-records"))
        assertTrue(diagnostic.samsungHealthComparisonSummary.contains("Samsung aggregate was 180"))
        assertTrue(diagnostic.healthConnectVisibleStepSummary.contains("hogere ruwe Samsung-export"))
        assertTrue(diagnostic.stepValueDebugSummary.contains("getoond 600"))
        assertTrue(diagnostic.stepValueDebugSummary.contains("Health Connect aggregate 180"))
        assertTrue(diagnostic.stepValueDebugSummary.contains("Samsung export 600"))
        assertTrue(diagnostic.stepValueDebugSummary.contains("Samsung aggregate 180"))
        assertTrue(diagnostic.stepValueDebugSummary.contains("Samsung raw 600"))
        assertTrue(diagnostic.stepValueDebugSummary.contains("Samsung direct niet beschikbaar"))
        assertTrue(diagnostic.samsungStepDebugClipboardText(nowMillis = 123L).contains("samsung-health-data-api*.aar ontbreekt"))
    }

    @Test
    fun stepDiagnosticCanPreferDirectSamsungHealthDataSdkStepsWhenAvailable() {
        val diagnostic = HealthConnectStepDiagnostic(
            aggregateStepsToday = 180,
            samsungHealthStepsToday = 600,
            samsungHealthAggregateStepsToday = 180,
            samsungRawStepRecordSumToday = 600,
            samsungHealthDirectStepsToday = 620,
            samsungHealthDirectStatus = "Samsung Health Data SDK direct gelezen.",
            queriedAt = 123L,
            sourceLabels = listOf("Samsung Health"),
        )

        assertEquals(620, diagnostic.displaySteps)
        assertTrue(diagnostic.aggregateAuthorityLabel.contains("directe Samsung Health Data SDK-waarde"))
        assertTrue(diagnostic.samsungHealthComparisonSummary.contains("Directe Samsung Health Data SDK-waarde 620"))
        assertTrue(diagnostic.stepValueDebugSummary.contains("Samsung direct 620"))
        assertTrue(diagnostic.healthConnectVisibleStepSummary.contains("directe Samsung Health Data SDK-bron"))
        assertTrue(diagnostic.healthConnectVisibleStepSummary.contains("Health Connect aggregate blijft zichtbaar"))
        assertTrue(diagnostic.parityGapSummary.contains("Directe Samsung Health Data SDK-waarde is beschikbaar"))
        assertTrue(diagnostic.samsungStepDebugClipboardText(nowMillis = 123L).contains("Samsung direct: Samsung Health Data SDK direct gelezen."))
        assertTrue(diagnostic.samsungStepDebugClipboardText(nowMillis = 123L).contains("Pariteit: Directe Samsung Health Data SDK-waarde"))
    }

    @Test
    fun stepDiagnosticExplainsDirectSamsungParityGapCauses() {
        val missingAar = HealthConnectStepDiagnostic(
            aggregateStepsToday = 180,
            samsungHealthStepsToday = 180,
            queriedAt = 123L,
            sourceLabels = listOf("Samsung Health"),
        )
        val permissionMissing = missingAar.copy(
            samsungHealthDirectStatus = "Samsung Health Data SDK API AAR is gebundeld, maar Samsung stappen-toestemming ontbreekt.",
        )
        val oldRuntime = missingAar.copy(
            samsungHealthDirectStatus = "Samsung Health runtime: 6.30.1 (lager dan Samsung Health Data SDK minimum 6.30.2+); update Samsung Health voordat je directe All steps-pariteit test.",
        )
        val oldAndroidRuntime = missingAar.copy(
            samsungHealthDirectStatus = "Android runtime lager dan API 29; Samsung Health Data SDK direct lezen blijft uitgeschakeld op dit apparaat.",
        )
        val runtimeMissing = missingAar.copy(
            samsungHealthDirectStatus = "Samsung Health runtime: app niet gevonden; installeer of update Samsung Health voordat je directe All steps-pariteit test.",
        )

        assertTrue(missingAar.parityGapSummary.contains("Samsung Health Data SDK API AAR niet beschikbaar"))
        assertTrue(permissionMissing.parityGapSummary.contains("Samsung stappen-toestemming ontbreekt"))
        assertTrue(oldAndroidRuntime.parityGapSummary.contains("Android 10/API 29+ vereist"))
        assertTrue(oldRuntime.parityGapSummary.contains("Samsung Health te oud"))
        assertTrue(runtimeMissing.parityGapSummary.contains("Samsung Health niet gevonden"))
        assertTrue(missingAar.samsungStepDebugClipboardText(nowMillis = 123L).contains("Pariteit:"))
    }

    @Test
    fun stepDiagnosticExplainsHealthConnectPriorityWhenMultipleStepSourcesAreVisible() {
        val diagnostic = HealthConnectStepDiagnostic(
            aggregateStepsToday = 180,
            samsungHealthStepsToday = 180,
            queriedAt = 123L,
            sourceLabels = listOf("Jouw telefoon", "Samsung Health"),
        )

        assertTrue(diagnostic.hasMultipleHealthConnectStepSources)
        assertTrue(diagnostic.healthConnectStepPrioritySummary.contains("meerdere stappenbronnen"))
        assertTrue(diagnostic.healthConnectStepPrioritySummary.contains("App priorities"))
        assertTrue(diagnostic.healthConnectStepPrioritySummary.contains("dedupliceert"))
        assertTrue(diagnostic.parityGapSummary.contains("App priorities"))
        assertTrue(diagnostic.samsungStepDebugClipboardText(nowMillis = 123L).contains("Health Connect prioriteit:"))
        assertTrue(diagnostic.samsungStepDebugClipboardText(nowMillis = 123L).contains("Samsung Health daar bovenaan"))
    }

    @Test
    fun stepDiagnosticSummarizesSamsungSourceRecencyForPhysicalComparison() {
        val recent = HealthConnectStepDiagnostic(
            aggregateStepsToday = 600,
            queriedAt = 120_000L,
            sourceLabels = listOf("Samsung Health"),
            latestSamsungSourceSeenAt = 90_000L,
        )
        val older = recent.copy(latestSamsungSourceSeenAt = 3_600_000L)
        val missing = recent.copy(latestSamsungSourceSeenAt = null)

        assertTrue(recent.samsungSourceRecencySummary(nowMillis = 120_000L).contains("net gezien"))
        assertTrue(older.samsungSourceRecencySummary(nowMillis = 3_900_000L).contains("5 min geleden"))
        assertTrue(older.samsungSourceRecencySummary(nowMillis = 10_800_000L).contains("2 uur geleden"))
        assertTrue(missing.samsungSourceRecencySummary(nowMillis = 120_000L).contains("nog niet met timestamp"))

        val clipboardText = older.samsungStepDebugClipboardText(nowMillis = 3_900_000L)
        assertTrue(clipboardText.contains("TrainIQ Samsung stappen-diagnose"))
        assertTrue(clipboardText.contains("getoond 600"))
        assertTrue(clipboardText.contains("Bronnen: Samsung Health"))
        assertTrue(clipboardText.contains("Samsung timing: Samsung-bron 5 min geleden gezien in Health Connect."))
        assertTrue(clipboardText.contains("Syncadvies:"))
    }

    @Test
    fun stepDiagnosticGuidesSamsungHealthSyncWhenSamsungSourceIsMissing() {
        val diagnostic = HealthConnectStepDiagnostic(
            aggregateStepsToday = 4_200,
            queriedAt = 123L,
            sourceLabels = listOf("Jouw telefoon"),
            dayStartLabel = "00:00",
            dayEndLabel = "14:30",
        )

        assertTrue(diagnostic.samsungHealthSyncGuidance().contains("Samsung Health"))
        assertTrue(diagnostic.samsungHealthSyncGuidance().contains("Sync now"))
    }

    @Test
    fun stepDiagnosticIncludesFreshnessWindowAndAggregateAuthorityCopy() {
        val diagnostic = HealthConnectStepDiagnostic(
            aggregateStepsToday = 7_200,
            queriedAt = 1_000L,
            sourceLabels = listOf("Jouw telefoon"),
            dayStartLabel = "00:00",
            dayEndLabel = "12:15",
        )

        assertEquals(HealthConnectStepDiagnosticFreshness.FRESH, diagnostic.freshness(nowMillis = 61_000L))
        assertEquals(HealthConnectStepDiagnosticFreshness.STALE, diagnostic.freshness(nowMillis = 901_000L))
        assertTrue(diagnostic.queryWindowSummary.contains("00:00"))
        assertTrue(diagnostic.queryWindowSummary.contains("12:15"))
        assertTrue(diagnostic.aggregateAuthorityLabel.contains("Health Connect aggregate"))
    }

    @Test
    fun stepDiagnosticGivesSamsungGuidanceForStaleSamsungSource() {
        val diagnostic = HealthConnectStepDiagnostic(
            aggregateStepsToday = 8_400,
            queriedAt = 1_000L,
            sourceLabels = listOf("Samsung Health"),
            latestSamsungSourceSeenAt = 1_000L,
            dayStartLabel = "00:00",
            dayEndLabel = "12:15",
        )

        assertTrue(diagnostic.hasSamsungHealthSource)
        assertTrue(diagnostic.samsungHealthSyncGuidance(nowMillis = 901_000L).contains("Sync now"))
    }
}
