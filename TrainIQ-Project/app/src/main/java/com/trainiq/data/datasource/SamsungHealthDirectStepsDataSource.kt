package com.trainiq.data.datasource

import android.app.Activity
import android.content.Context
import android.content.pm.PackageInfo
import android.os.Build
import com.trainiq.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Modifier
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

data class SamsungHealthDirectStepSnapshot(
    val steps: Int? = null,
    val status: String = SamsungHealthDirectStepsDataSource.StatusSdkUnavailable,
    val queriedAt: Long? = null,
)

internal data class SamsungHealthRuntimeReadiness(
    val status: String,
    val canUseDirectSdk: Boolean,
)

@Singleton
class SamsungHealthDirectStepsDataSource @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    suspend fun readTodaySteps(
        start: LocalDateTime,
        end: LocalDateTime,
    ): SamsungHealthDirectStepSnapshot {
        if (!BuildConfig.SAMSUNG_HEALTH_DATA_SDK_AAR_PRESENT) {
            return SamsungHealthDirectStepSnapshot(status = unavailableStatus())
        }
        samsungHealthAndroidRuntimeReadiness()
            .takeIf { readiness -> !readiness.canUseDirectSdk }
            ?.let { readiness -> return SamsungHealthDirectStepSnapshot(status = readiness.status) }
        samsungHealthRuntimeReadiness()
            .takeIf { readiness -> !readiness.canUseDirectSdk }
            ?.let { readiness -> return SamsungHealthDirectStepSnapshot(status = readiness.status) }
        return runCatching {
            val store = samsungHealthDataStore()
            val permissions = setOf(samsungStepsReadPermission())
            val grantedPermissions = invokeSuspend(store, "getGrantedPermissions", permissions)
                .asSamsungPermissionSet()
            if (!grantedPermissions.containsAll(permissions)) {
                return@runCatching SamsungHealthDirectStepSnapshot(status = StatusPermissionMissing)
            }

            val stepCount = readTotalSteps(store, start, end)
            SamsungHealthDirectStepSnapshot(
                steps = samsungDirectStepsFromTotal(stepCount),
                status = if (stepCount > 0) StatusSdkRead else StatusSdkReadNoSteps,
                queriedAt = System.currentTimeMillis(),
            )
        }.getOrElse { throwable ->
            SamsungHealthDirectStepSnapshot(status = throwable.samsungFailureStatus())
        }
    }

    suspend fun requestTodayStepPermission(activity: Activity): SamsungHealthDirectStepSnapshot {
        if (!BuildConfig.SAMSUNG_HEALTH_DATA_SDK_AAR_PRESENT) {
            return SamsungHealthDirectStepSnapshot(status = unavailableStatus())
        }
        samsungHealthAndroidRuntimeReadiness()
            .takeIf { readiness -> !readiness.canUseDirectSdk }
            ?.let { readiness -> return SamsungHealthDirectStepSnapshot(status = readiness.status) }
        samsungHealthRuntimeReadiness()
            .takeIf { readiness -> !readiness.canUseDirectSdk }
            ?.let { readiness -> return SamsungHealthDirectStepSnapshot(status = readiness.status) }
        return runCatching {
            val store = samsungHealthDataStore()
            val permissions = setOf(samsungStepsReadPermission())
            val grantedPermissions = invokeSuspend(store, "getGrantedPermissions", permissions).asSamsungPermissionSet()
            val missingPermissions = permissions - grantedPermissions
            if (missingPermissions.isNotEmpty()) {
                val requestResult = invokeSuspend(store, "requestPermissions", missingPermissions, activity)
                val newlyGranted = requestResult.asSamsungPermissionSet().ifEmpty {
                    invokeSuspend(store, "getGrantedPermissions", permissions).asSamsungPermissionSet()
                }
                if (!newlyGranted.containsAll(permissions)) {
                    return@runCatching SamsungHealthDirectStepSnapshot(status = StatusPermissionMissing)
                }
            }

            val now = LocalDateTime.now()
            readTodaySteps(start = now.toLocalDate().atStartOfDay(), end = now)
        }.getOrElse { throwable ->
            val cause = throwable.unwrapInvocationTarget()
            SamsungHealthDirectStepSnapshot(status = cause.samsungResolutionStatus(activity) ?: cause.samsungFailureStatus())
        }
    }

    private fun samsungHealthDataStore(): Any {
        val serviceClass = samsungClass("com.samsung.android.sdk.health.data.HealthDataService")
        return serviceClass.getMethod("getStore", Context::class.java).invoke(null, context)
            ?: error("Samsung Health Data SDK gaf geen HealthDataStore terug.")
    }

    private fun unavailableStatus(): String =
        if (BuildConfig.SAMSUNG_HEALTH_NON_API_AAR_PRESENT) {
            StatusLegacyAarPresent + " " + samsungHealthAndroidRuntimeReadiness().status + " " + samsungHealthRuntimeReadiness().status
        } else {
            StatusSdkUnavailable + " " + samsungHealthAndroidRuntimeReadiness().status + " " + samsungHealthRuntimeReadiness().status
        }

    private fun samsungHealthAndroidRuntimeReadiness(): SamsungHealthRuntimeReadiness =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            SamsungHealthRuntimeReadiness(
                status = "Android runtime: API ${Build.VERSION.SDK_INT} voldoet aan Samsung Health Data SDK minimum API 29.",
                canUseDirectSdk = true,
            )
        } else {
            SamsungHealthRuntimeReadiness(
                status = StatusAndroidRuntimeTooOld,
                canUseDirectSdk = false,
            )
        }

    private fun samsungHealthRuntimeReadiness(): SamsungHealthRuntimeReadiness {
        val packageInfo = context.samsungHealthPackageInfoOrNull()
            ?: return samsungHealthRuntimeReadinessForVersion(installed = false)
        val versionName = packageInfo.versionName?.takeIf { it.isNotBlank() } ?: "onbekend"
        return samsungHealthRuntimeReadinessForVersion(versionName = versionName)
    }

    private fun samsungStepsReadPermission(): Any {
        val stepsType = samsungStepsDataType()
        val readAccess = samsungReadAccessType()
        return samsungPermissionOf(stepsType, readAccess)
    }

    private fun samsungStepsDataType(): Any {
        val dataTypesClass = samsungClass("com.samsung.android.sdk.health.data.request.DataTypes")
        dataTypesClass.fields
            .firstOrNull { field -> field.name == "STEPS" && Modifier.isStatic(field.modifiers) }
            ?.let { field ->
                return field.get(null)
                    ?: error("Samsung Health Data SDK DataTypes.STEPS gaf geen resultaat terug.")
            }
        dataTypesClass.methods
            .firstOrNull { method ->
                method.name == "getSTEPS" &&
                    method.parameterTypes.isEmpty() &&
                    Modifier.isStatic(method.modifiers)
            }
            ?.let { method ->
                return method.invoke(null)
                    ?: error("Samsung Health Data SDK DataTypes.STEPS gaf geen resultaat terug.")
            }

        val companion = dataTypesClass.fields
            .firstOrNull { field -> field.name == "Companion" && Modifier.isStatic(field.modifiers) }
            ?.get(null)
        if (companion != null) {
            companion.javaClass.fields
                .firstOrNull { field -> field.name == "STEPS" }
                ?.let { field ->
                    return field.get(companion)
                        ?: error("Samsung Health Data SDK DataTypes.STEPS gaf geen resultaat terug.")
                }
            companion.javaClass.methods
                .firstOrNull { method -> method.name == "getSTEPS" && method.parameterTypes.isEmpty() }
                ?.let { method ->
                    return method.invoke(companion)
                        ?: error("Samsung Health Data SDK DataTypes.STEPS gaf geen resultaat terug.")
                }
        }

        error("Samsung Health Data SDK DataTypes.STEPS ontbreekt.")
    }

    private fun samsungReadAccessType(): Any {
        val accessTypeClass = samsungClass("com.samsung.android.sdk.health.data.permission.AccessType")
        accessTypeClass.enumConstants
            ?.firstOrNull { it.toString() == "READ" }
            ?.let { return it }
        accessTypeClass.fields
            .firstOrNull { field -> field.name == "READ" && Modifier.isStatic(field.modifiers) }
            ?.let { field ->
                return field.get(null)
                    ?: error("Samsung Health Data SDK AccessType.READ gaf geen resultaat terug.")
            }
        accessTypeClass.methods
            .firstOrNull { method ->
                method.name == "getREAD" &&
                    method.parameterTypes.isEmpty() &&
                    Modifier.isStatic(method.modifiers)
            }
            ?.let { method ->
                return method.invoke(null)
                    ?: error("Samsung Health Data SDK AccessType.READ gaf geen resultaat terug.")
            }

        val companion = accessTypeClass.fields
            .firstOrNull { field -> field.name == "Companion" && Modifier.isStatic(field.modifiers) }
            ?.get(null)
        if (companion != null) {
            companion.javaClass.fields
                .firstOrNull { field -> field.name == "READ" }
                ?.let { field ->
                    return field.get(companion)
                        ?: error("Samsung Health Data SDK AccessType.READ gaf geen resultaat terug.")
                }
            companion.javaClass.methods
                .firstOrNull { method -> method.name == "getREAD" && method.parameterTypes.isEmpty() }
                ?.let { method ->
                    return method.invoke(companion)
                        ?: error("Samsung Health Data SDK AccessType.READ gaf geen resultaat terug.")
                }
        }

        error("Samsung Health Data SDK AccessType.READ ontbreekt.")
    }

    private fun samsungPermissionOf(dataType: Any, accessType: Any): Any {
        val permissionClass = samsungClass("com.samsung.android.sdk.health.data.permission.Permission")
        permissionClass.methods
            .firstOrNull { method ->
                method.name == "of" &&
                    method.parameterTypes.size == 2 &&
                    Modifier.isStatic(method.modifiers)
            }
            ?.let { method ->
                return method.invoke(null, dataType, accessType)
                    ?: error("Samsung Health Data SDK Permission.of gaf geen resultaat terug.")
            }

        val companion = permissionClass.fields
            .firstOrNull { field -> field.name == "Companion" && Modifier.isStatic(field.modifiers) }
            ?.get(null)
        if (companion != null) {
            companion.javaClass.methods
                .firstOrNull { method -> method.name == "of" && method.parameterTypes.size == 2 }
                ?.let { method ->
                    return method.invoke(companion, dataType, accessType)
                        ?: error("Samsung Health Data SDK Permission.of gaf geen resultaat terug.")
                }
        }

        error("Samsung Health Data SDK Permission.of ontbreekt.")
    }

    private suspend fun readTotalSteps(
        store: Any,
        start: LocalDateTime,
        end: LocalDateTime,
    ): Int {
        val localTimeFilter = localTimeFilter(start, end)
        val aggregateRequest = samsungStepsTotalAggregateRequest(localTimeFilter)
        val response = invokeSuspend(store, "aggregateData", aggregateRequest)
        return response.samsungAggregateDataList()
            .sumOf { aggregatedData -> aggregatedData.samsungAggregateValue() }
            .coerceAtMost(Int.MAX_VALUE.toLong())
            .toInt()
    }

    private fun Any.samsungAggregateDataList(): Iterable<Any> {
        javaClass.methods
            .firstOrNull { method -> method.name == "getDataList" && method.parameterTypes.isEmpty() }
            ?.invoke(this)
            ?.asIterable()
            ?.let { return it }
        javaClass.fields
            .firstOrNull { field -> field.name == "dataList" }
            ?.get(this)
            ?.asIterable()
            ?.let { return it }
        error("Samsung Health Data SDK aggregate response dataList ontbreekt.")
    }

    private fun Any.samsungAggregateValue(): Long {
        javaClass.methods
            .firstOrNull { method -> method.name == "getValue" && method.parameterTypes.isEmpty() }
            ?.let { method -> return method.invoke(this).asLongStepValue() }
        javaClass.fields
            .firstOrNull { field -> field.name == "value" }
            ?.let { field -> return field.get(this).asLongStepValue() }
        error("Samsung Health Data SDK aggregate response value ontbreekt.")
    }

    private fun Any?.asLongStepValue(): Long =
        samsungAggregateStepValue(this)

    private fun localTimeFilter(start: LocalDateTime, end: LocalDateTime): Any {
        val localTimeFilterClass = samsungClass("com.samsung.android.sdk.health.data.request.LocalTimeFilter")
        return invokeKotlinCompanionFactory(localTimeFilterClass, "of", start, end)
    }

    private fun samsungStepsTotalAggregateRequest(localTimeFilter: Any): Any {
        val total = samsungStepsTotalAggregateOperation()
        val requestBuilder = samsungAggregateRequestBuilder(total)
        requestBuilder.setSamsungLocalTimeFilter(localTimeFilter)
        requestBuilder.setSamsungOrderingAscIfAvailable()
        return requestBuilder.javaClass.methods
            .first { method -> method.name == "build" && method.parameterTypes.isEmpty() }
            .invoke(requestBuilder)
            ?: error("Samsung Health Data SDK gaf geen aggregate request terug.")
    }

    private fun Any.setSamsungLocalTimeFilter(localTimeFilter: Any) {
        val groupedFailure = runCatching {
            val localTimeGroup = localTimeGroup()
            javaClass.methods
                .firstOrNull { method -> method.name == "setLocalTimeFilterWithGroup" && method.parameterTypes.size == 2 }
                ?.let { method ->
                    method.invoke(this, localTimeFilter, localTimeGroup)
                    return
                }
        }.exceptionOrNull()
        javaClass.methods
            .firstOrNull { method -> method.name == "setLocalTimeFilter" && method.parameterTypes.size == 1 }
            ?.let { method ->
                method.invoke(this, localTimeFilter)
                return
            }
        error(
            groupedFailure?.message
                ?: "Samsung Health Data SDK LocalTimeFilter builder ontbreekt.",
        )
    }

    private fun localTimeGroup(): Any {
        val groupUnitClass = samsungClass("com.samsung.android.sdk.health.data.request.LocalTimeGroupUnit")
        val groupClass = samsungClass("com.samsung.android.sdk.health.data.request.LocalTimeGroup")
        val hourly = groupUnitClass.samsungGroupUnitOrNull("HOURLY")
        if (hourly != null) {
            return invokeKotlinCompanionFactory(groupClass, "of", hourly, 1)
        }
        val minutely = groupUnitClass.samsungGroupUnitOrNull("MINUTELY")
            ?: error("Samsung Health Data SDK LocalTimeGroupUnit.HOURLY/MINUTELY ontbreekt.")
        return invokeKotlinCompanionFactory(groupClass, "of", minutely, 30)
    }

    private fun Class<*>.samsungGroupUnitOrNull(name: String): Any? =
        enumConstants?.firstOrNull { it.toString() == name }
            ?: runCatching { getField(name).get(null) }.getOrNull()

    private fun Any.setSamsungOrderingAscIfAvailable() {
        val orderingAsc = samsungClassOrNull("com.samsung.android.sdk.health.data.request.Ordering")
            ?.samsungGroupUnitOrNull("ASC")
            ?: return
        javaClass.methods
            .firstOrNull { method -> method.name == "setOrdering" && method.parameterTypes.size == 1 }
            ?.invoke(this, orderingAsc)
    }

    private fun samsungStepsTotalAggregateOperation(): Any {
        val stepsTypeClass = samsungClass("com.samsung.android.sdk.health.data.request.DataType\$StepsType")
        stepsTypeClass.fields
            .firstOrNull { field -> field.name == "TOTAL" && Modifier.isStatic(field.modifiers) }
            ?.let { field ->
                return field.get(null)
                    ?: error("Samsung Health Data SDK DataType.StepsType.TOTAL gaf geen resultaat terug.")
            }
        stepsTypeClass.methods
            .firstOrNull { method ->
                method.name == "getTOTAL" &&
                    method.parameterTypes.isEmpty() &&
                    Modifier.isStatic(method.modifiers)
            }
            ?.let { method ->
                return method.invoke(null)
                    ?: error("Samsung Health Data SDK DataType.StepsType.TOTAL gaf geen resultaat terug.")
            }

        val companion = stepsTypeClass.fields
            .firstOrNull { field -> field.name == "Companion" && Modifier.isStatic(field.modifiers) }
            ?.get(null)
        if (companion != null) {
            companion.javaClass.fields
                .firstOrNull { field -> field.name == "TOTAL" }
                ?.let { field ->
                    return field.get(companion)
                        ?: error("Samsung Health Data SDK DataType.StepsType.TOTAL gaf geen resultaat terug.")
                }
            companion.javaClass.methods
                .firstOrNull { method -> method.name == "getTOTAL" && method.parameterTypes.isEmpty() }
                ?.let { method ->
                    return method.invoke(companion)
                        ?: error("Samsung Health Data SDK DataType.StepsType.TOTAL gaf geen resultaat terug.")
                }
        }

        error("Samsung Health Data SDK DataType.StepsType.TOTAL ontbreekt.")
    }

    private fun samsungAggregateRequestBuilder(aggregateOperation: Any): Any {
        aggregateOperation.javaClass.methods
            .firstOrNull { method -> method.name == "getRequestBuilder" && method.parameterTypes.isEmpty() }
            ?.let { method ->
                return method.invoke(aggregateOperation)
                    ?: error("Samsung Health Data SDK DataType.StepsType.TOTAL.requestBuilder gaf geen resultaat terug.")
            }
        aggregateOperation.javaClass.fields
            .firstOrNull { field -> field.name == "requestBuilder" }
            ?.let { field ->
                return field.get(aggregateOperation)
                    ?: error("Samsung Health Data SDK DataType.StepsType.TOTAL.requestBuilder gaf geen resultaat terug.")
            }

        error("Samsung Health Data SDK DataType.StepsType.TOTAL.requestBuilder ontbreekt.")
    }

    private suspend fun invokeSuspend(
        target: Any,
        methodName: String,
        vararg args: Any,
    ): Any = suspendCoroutine { continuation ->
        val method = target.javaClass.methods.firstOrNull { candidate ->
            candidate.name == methodName &&
                candidate.parameterTypes.size == args.size + 1 &&
                Continuation::class.java.isAssignableFrom(candidate.parameterTypes.last())
        } ?: run {
            continuation.resumeWithException(NoSuchMethodException("${target.javaClass.name}.$methodName"))
            return@suspendCoroutine
        }
        val result = runCatching {
            method.invoke(target, *args, continuation.forwarding())
        }.getOrElse { throwable ->
            continuation.resumeWithException(throwable.unwrapInvocationTarget())
            return@suspendCoroutine
        }
        if (result !== COROUTINE_SUSPENDED) {
            continuation.resume(result)
        }
    }

    private fun Continuation<Any>.forwarding(): Continuation<Any> {
        val downstream = this
        return object : Continuation<Any> {
            override val context: CoroutineContext = downstream.context

            override fun resumeWith(result: Result<Any>) {
                result
                    .onSuccess { value -> downstream.resume(value) }
                    .onFailure { throwable -> downstream.resumeWithException(throwable) }
            }
        }
    }

    private fun invokeKotlinCompanionFactory(
        targetClass: Class<*>,
        methodName: String,
        vararg args: Any,
    ): Any {
        targetClass.methods
            .firstOrNull { method -> method.name == methodName && method.parameterTypes.size == args.size }
            ?.let { method ->
                return method.invoke(null, *args)
                    ?: error("Samsung Health Data SDK $methodName gaf geen resultaat terug.")
            }
        val companion = targetClass.getField("Companion").get(null)
            ?: error("Samsung Health Data SDK ${targetClass.name}.Companion ontbreekt.")
        return companion.javaClass.methods
            .first { method -> method.name == methodName && method.parameterTypes.size == args.size }
            .invoke(companion, *args)
            ?: error("Samsung Health Data SDK $methodName gaf geen resultaat terug.")
    }

    private fun samsungClass(name: String): Class<*> =
        Class.forName(name)

    private fun samsungClassOrNull(name: String): Class<*>? =
        runCatching { Class.forName(name) }.getOrNull()

    private fun Context.samsungHealthPackageInfoOrNull(): PackageInfo? =
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageInfo(
                    SamsungHealthPackageName,
                    android.content.pm.PackageManager.PackageInfoFlags.of(0),
                )
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(SamsungHealthPackageName, 0)
            }
        }.getOrNull()

    private fun Any?.asSamsungPermissionSet(): Set<Any> {
        (this as? Set<*>)?.filterNotNull()?.toSet()?.let { return it }
        if (this == null) return emptySet()
        javaClass.methods
            .firstOrNull { method -> method.name == "getGrantedPermissions" && method.parameterTypes.isEmpty() }
            ?.invoke(this)
            ?.let { granted -> return granted.asSamsungPermissionSet() }
        javaClass.fields
            .firstOrNull { field -> field.name == "grantedPermissions" }
            ?.get(this)
            ?.let { granted -> return granted.asSamsungPermissionSet() }
        return emptySet()
    }

    private fun Any?.asIterable(): Iterable<Any> =
        (this as? Iterable<*>)?.filterNotNull().orEmpty()

    private fun Throwable.unwrapInvocationTarget(): Throwable =
        if (this is InvocationTargetException) targetException ?: this else this

    private fun Throwable.userFacingMessage(): String =
        unwrapInvocationTarget().message?.takeIf { it.isNotBlank() }
            ?: unwrapInvocationTarget().javaClass.simpleName

    private fun Throwable.samsungFailureStatus(): String {
        val cause = unwrapInvocationTarget()
        val className = cause.javaClass.name
        val simpleName = cause.javaClass.simpleName
        val detail = cause.message?.takeIf { it.isNotBlank() }
            ?: simpleName
        return when {
            className.endsWith("ResolvablePlatformException") ->
                if (cause.hasSamsungResolution()) {
                    StatusSdkResolvablePlatform + detail
                } else {
                    StatusSdkPlatformUnavailable + detail
                }

            className.endsWith("AuthorizationException") ->
                StatusSdkAuthorizationFailed + detail

            className.endsWith("InvalidRequestException") ->
                StatusSdkInvalidRequest + detail

            className.endsWith("PlatformInternalException") ->
                StatusSdkPlatformInternal + detail

            className.endsWith("HealthDataException") ->
                StatusSdkHealthDataFailed + detail

            else ->
                StatusSdkReadFailedPrefix + userFacingMessage()
        }
    }

    private fun Throwable.samsungResolutionStatus(activity: Activity): String? {
        val cause = unwrapInvocationTarget()
        if (!cause.javaClass.name.endsWith("ResolvablePlatformException") || !cause.hasSamsungResolution()) {
            return null
        }
        return if (cause.resolveSamsungAction(activity)) {
            StatusSdkResolutionStarted
        } else {
            StatusSdkResolutionFailed + cause.userFacingMessage()
        }
    }

    private fun Throwable.hasSamsungResolution(): Boolean =
        runCatching {
            javaClass.methods
                .firstOrNull { method -> method.name == "getHasResolution" && method.parameterTypes.isEmpty() }
                ?.invoke(this) as? Boolean
                ?: javaClass.methods
                    .firstOrNull { method -> method.name == "hasResolution" && method.parameterTypes.isEmpty() }
                    ?.invoke(this) as? Boolean
                ?: false
        }.getOrDefault(false)

    private fun Throwable.resolveSamsungAction(activity: Activity): Boolean =
        runCatching {
            javaClass.methods
                .firstOrNull { method -> method.name == "resolve" && method.parameterTypes.size == 1 }
                ?.let { method ->
                    method.invoke(this, activity)
                    true
                } ?: false
        }.getOrDefault(false)

    companion object {
        const val SamsungHealthPackageName = "com.sec.android.app.shealth"
        const val RequiredSamsungHealthVersionName = "6.30.2"
        const val StatusAndroidRuntimeTooOld = "Android runtime lager dan API 29; Samsung Health Data SDK direct lezen blijft uitgeschakeld op dit apparaat."
        const val StatusRuntimeMissing = "Samsung Health runtime: app niet gevonden; installeer of update Samsung Health voordat je directe All steps-pariteit test."
        const val StatusSdkUnavailable = "Samsung Health Data SDK API AAR samsung-health-data-api*.aar ontbreekt in app/libs; directe Samsung All steps-bron niet beschikbaar."
        const val StatusLegacyAarPresent = "Er staat een Samsung Health AAR in app/libs, maar niet de benodigde samsung-health-data-api*.aar; directe Samsung All steps-bron blijft uitgeschakeld."
        const val StatusPermissionMissing = "Samsung Health Data SDK API AAR is gebundeld, maar Samsung stappen-toestemming ontbreekt."
        const val StatusSdkRead = "Samsung Health Data SDK direct gelezen."
        const val StatusSdkReadNoSteps = "Samsung Health Data SDK direct gelezen, maar vandaag zijn daar nog geen stappen zichtbaar."
        const val StatusSdkReadFailedPrefix = "Samsung Health Data SDK lezen mislukt: "
        const val StatusSdkResolvablePlatform = "Samsung Health Data SDK heeft een oplosbare Samsung Health-actie nodig: "
        const val StatusSdkPlatformUnavailable = "Samsung Health Data SDK-platform is niet klaar: "
        const val StatusSdkAuthorizationFailed = "Samsung Health Data SDK autorisatie mislukt: "
        const val StatusSdkInvalidRequest = "Samsung Health Data SDK stappenverzoek is ongeldig: "
        const val StatusSdkPlatformInternal = "Samsung Health gaf een interne SDK-fout terug: "
        const val StatusSdkHealthDataFailed = "Samsung Health Data SDK gaf een health-data fout terug: "
        const val StatusSdkResolutionStarted = "Samsung Health Data SDK heeft een Samsung Health-actie geopend. Rond die af en vernieuw daarna TrainIQ."
        const val StatusSdkResolutionFailed = "Samsung Health Data SDK-actie kon niet worden geopend: "
    }
}

internal fun isSamsungHealthVersionAtLeast(
    versionName: String,
    minimumVersionName: String = SamsungHealthDirectStepsDataSource.RequiredSamsungHealthVersionName,
): Boolean? {
    val current = versionName.semanticVersionParts()
    val minimum = minimumVersionName.semanticVersionParts()
    if (current.isEmpty() || minimum.isEmpty()) return null
    val maxLength = maxOf(current.size, minimum.size)
    for (index in 0 until maxLength) {
        val currentPart = current.getOrElse(index) { 0 }
        val minimumPart = minimum.getOrElse(index) { 0 }
        if (currentPart != minimumPart) return currentPart > minimumPart
    }
    return true
}

internal fun samsungHealthRuntimeReadinessForVersion(
    versionName: String = "onbekend",
    installed: Boolean = true,
): SamsungHealthRuntimeReadiness {
    if (!installed) {
        return SamsungHealthRuntimeReadiness(
            status = SamsungHealthDirectStepsDataSource.StatusRuntimeMissing,
            canUseDirectSdk = false,
        )
    }
    return when (isSamsungHealthVersionAtLeast(versionName)) {
        true -> SamsungHealthRuntimeReadiness(
            status = "Samsung Health runtime: $versionName (voldoet aan Samsung Health Data SDK minimum ${SamsungHealthDirectStepsDataSource.RequiredSamsungHealthVersionName}+).",
            canUseDirectSdk = true,
        )
        false -> SamsungHealthRuntimeReadiness(
            status = "Samsung Health runtime: $versionName (lager dan Samsung Health Data SDK minimum ${SamsungHealthDirectStepsDataSource.RequiredSamsungHealthVersionName}+); update Samsung Health voordat je directe All steps-pariteit test.",
            canUseDirectSdk = false,
        )
        null -> SamsungHealthRuntimeReadiness(
            status = "Samsung Health runtime: $versionName (minimum ${SamsungHealthDirectStepsDataSource.RequiredSamsungHealthVersionName}+ niet automatisch te bepalen).",
            canUseDirectSdk = true,
        )
    }
}

private fun String.semanticVersionParts(): List<Int> =
    split(".", "-", "_")
        .mapNotNull { part -> part.takeWhile { char -> char.isDigit() }.toIntOrNull() }

internal fun samsungAggregateStepValue(value: Any?): Long {
    if (value == null) return 0L
    val number = value as? Number
        ?: error("Samsung Health Data SDK aggregate response value is geen getal.")
    return number.toLong()
}

internal fun samsungDirectStepsFromTotal(stepCount: Int): Int =
    stepCount.coerceAtLeast(0)
