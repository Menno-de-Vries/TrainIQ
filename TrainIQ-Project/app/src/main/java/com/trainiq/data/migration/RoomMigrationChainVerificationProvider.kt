package com.trainiq.data.migration

import android.content.Context
import com.google.gson.Gson
import com.trainiq.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomMigrationChainVerificationProvider @Inject constructor(
    private val markerSource: RoomMigrationChainVerificationMarkerSource,
) {
    fun report(nowMillis: Long = System.currentTimeMillis()): RoomMigrationChainVerificationReport {
        val marker = markerSource.latestMarker()
            ?: return RoomMigrationChainVerificationReport(
                currentRoomVersion = CurrentRoomVersion,
                requiredStartVersion = RequiredMigrationStartVersion,
                requiredEndVersion = CurrentRoomVersion,
                coveredStartVersion = null,
                coveredEndVersion = null,
                status = RoomMigrationChainVerification.NOT_RUN,
                reason = RoomMigrationChainVerificationReason.NO_TRUSTED_MARKER,
                lastVerificationMarker = null,
                verifiedAtMillis = null,
                freshEnough = false,
            )

        val expectedMarker = expectedMarker()
        val status = when {
            marker.payloadSha256.isNullOrBlank() -> RoomMigrationChainVerification.UNKNOWN
            marker.payloadSha256 != marker.verificationPayloadSha256() -> RoomMigrationChainVerification.FAILED
            marker.buildVariant != BuildConfig.BUILD_TYPE -> RoomMigrationChainVerification.STALE
            marker.marker != expectedMarker -> RoomMigrationChainVerification.STALE
            marker.currentRoomVersion != CurrentRoomVersion -> RoomMigrationChainVerification.STALE
            marker.requiredStartVersion != RequiredMigrationStartVersion -> RoomMigrationChainVerification.PARTIAL
            marker.requiredEndVersion != CurrentRoomVersion -> RoomMigrationChainVerification.PARTIAL
            marker.coveredStartVersion > RequiredMigrationStartVersion -> RoomMigrationChainVerification.PARTIAL
            marker.coveredEndVersion < CurrentRoomVersion -> RoomMigrationChainVerification.PARTIAL
            marker.verifiedAtMillis <= 0L -> RoomMigrationChainVerification.UNKNOWN
            nowMillis - marker.verifiedAtMillis > MaxVerificationAgeMillis -> RoomMigrationChainVerification.STALE
            marker.verifiedAtMillis > nowMillis -> RoomMigrationChainVerification.STALE
            else -> RoomMigrationChainVerification.VERIFIED
        }
        return RoomMigrationChainVerificationReport(
            currentRoomVersion = CurrentRoomVersion,
            requiredStartVersion = RequiredMigrationStartVersion,
            requiredEndVersion = CurrentRoomVersion,
            coveredStartVersion = marker.coveredStartVersion,
            coveredEndVersion = marker.coveredEndVersion,
            status = status,
            reason = status.toReason(),
            lastVerificationMarker = marker.marker,
            verifiedAtMillis = marker.verifiedAtMillis,
            freshEnough = status == RoomMigrationChainVerification.VERIFIED,
        )
    }

    companion object {
        const val RequiredMigrationStartVersion = 2
        const val CurrentRoomVersion = 16
        const val MaxVerificationAgeMillis = 30L * 24L * 60L * 60L * 1000L

        fun expectedMarker(): String =
            "trainiq-room-migration-chain-v$RequiredMigrationStartVersion-to-v$CurrentRoomVersion"
    }
}

interface RoomMigrationChainVerificationMarkerSource {
    fun latestMarker(): RoomMigrationChainVerificationMarker?
}

@Singleton
class AssetRoomMigrationChainVerificationMarkerSource @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : RoomMigrationChainVerificationMarkerSource {
    private val gson = Gson()

    override fun latestMarker(): RoomMigrationChainVerificationMarker? = runCatching {
        context.assets.open(MarkerAssetName).bufferedReader().use { reader ->
            gson.fromJson(reader, RoomMigrationChainVerificationMarker::class.java)
        }?.takeIf { marker -> marker.buildVariant == BuildConfig.BUILD_TYPE }
    }.getOrNull()
}

data class RoomMigrationChainVerificationMarker(
    val marker: String,
    val buildVariant: String,
    val testTask: String,
    val currentRoomVersion: Int,
    val requiredStartVersion: Int,
    val requiredEndVersion: Int,
    val coveredStartVersion: Int,
    val coveredEndVersion: Int,
    val verifiedAtMillis: Long,
    val migrationCount: Int,
    val payloadSha256: String? = null,
)

fun RoomMigrationChainVerificationMarker.verificationPayloadSha256(): String {
    val payload = listOf(
        marker,
        buildVariant,
        testTask,
        currentRoomVersion.toString(),
        requiredStartVersion.toString(),
        requiredEndVersion.toString(),
        coveredStartVersion.toString(),
        coveredEndVersion.toString(),
        verifiedAtMillis.toString(),
        migrationCount.toString(),
    ).joinToString(separator = "|")
    return MessageDigest.getInstance("SHA-256")
        .digest(payload.toByteArray(Charsets.UTF_8))
        .joinToString(separator = "") { "%02x".format(it) }
}

data class RoomMigrationChainVerificationReport(
    val currentRoomVersion: Int,
    val requiredStartVersion: Int,
    val requiredEndVersion: Int,
    val coveredStartVersion: Int?,
    val coveredEndVersion: Int?,
    val status: RoomMigrationChainVerification,
    val reason: RoomMigrationChainVerificationReason,
    val lastVerificationMarker: String?,
    val verifiedAtMillis: Long?,
    val freshEnough: Boolean,
)

enum class RoomMigrationChainVerificationReason {
    VERIFIED,
    NO_TRUSTED_MARKER,
    MARKER_STALE,
    COVERAGE_PARTIAL,
    UNKNOWN_MARKER_STATE,
    FAILED,
}

fun RoomMigrationChainVerificationReport?.toMigrationChainVerification(): RoomMigrationChainVerification =
    this?.status ?: RoomMigrationChainVerification.MISSING

private fun RoomMigrationChainVerification.toReason(): RoomMigrationChainVerificationReason = when (this) {
    RoomMigrationChainVerification.VERIFIED -> RoomMigrationChainVerificationReason.VERIFIED
    RoomMigrationChainVerification.UNVERIFIED,
    RoomMigrationChainVerification.NOT_RUN,
    RoomMigrationChainVerification.MISSING -> RoomMigrationChainVerificationReason.NO_TRUSTED_MARKER
    RoomMigrationChainVerification.PARTIAL -> RoomMigrationChainVerificationReason.COVERAGE_PARTIAL
    RoomMigrationChainVerification.STALE -> RoomMigrationChainVerificationReason.MARKER_STALE
    RoomMigrationChainVerification.FAILED -> RoomMigrationChainVerificationReason.FAILED
    RoomMigrationChainVerification.UNKNOWN -> RoomMigrationChainVerificationReason.UNKNOWN_MARKER_STATE
}

internal const val MarkerAssetName = "room_migration_chain_verification_marker.json"
