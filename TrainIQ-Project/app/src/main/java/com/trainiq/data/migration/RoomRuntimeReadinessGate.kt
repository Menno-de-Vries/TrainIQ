package com.trainiq.data.migration

import com.trainiq.core.database.RoomMirrorImportRunEntity
import com.trainiq.core.database.TrainIqDao
import java.security.MessageDigest

class RoomRuntimeReadinessGate(
    private val latestRun: suspend () -> RoomMirrorImportRunEntity?,
) {
    constructor(dao: TrainIqDao) : this(latestRun = { dao.latestMirrorImportRun() })

    suspend fun evaluate(
        currentJson: String?,
        migrationChainVerified: Boolean,
        nowMillis: Long = System.currentTimeMillis(),
        maxMirrorAgeMillis: Long = DefaultMaxMirrorAgeMillis,
    ): RoomRuntimeReadiness = evaluate(
        currentJson = currentJson,
        migrationChainStatus = if (migrationChainVerified) {
            RoomMigrationChainVerification.VERIFIED
        } else {
            RoomMigrationChainVerification.UNVERIFIED
        },
        nowMillis = nowMillis,
        maxMirrorAgeMillis = maxMirrorAgeMillis,
    )

    suspend fun evaluate(
        currentJson: String?,
        migrationChainStatus: RoomMigrationChainVerification,
        nowMillis: Long = System.currentTimeMillis(),
        maxMirrorAgeMillis: Long = DefaultMaxMirrorAgeMillis,
    ): RoomRuntimeReadiness = evaluate(
        currentJson = currentJson,
        verification = RoomRuntimeReadinessVerification(
            migrationChain = migrationChainStatus,
            liveShapeImport = RoomImportValidationVerification.UNVERIFIED,
        ),
        nowMillis = nowMillis,
        maxMirrorAgeMillis = maxMirrorAgeMillis,
    )

    suspend fun evaluate(
        currentJson: String?,
        verification: RoomRuntimeReadinessVerification,
        nowMillis: Long = System.currentTimeMillis(),
        maxMirrorAgeMillis: Long = DefaultMaxMirrorAgeMillis,
    ): RoomRuntimeReadiness {
        if (currentJson.isNullOrBlank()) {
            return RoomRuntimeReadiness.Blocked(RoomRuntimeReadinessFailure.MISSING_JSON)
        }
        when (verification.migrationChain) {
            RoomMigrationChainVerification.VERIFIED -> Unit
            RoomMigrationChainVerification.UNVERIFIED -> {
                return RoomRuntimeReadiness.Blocked(RoomRuntimeReadinessFailure.MIGRATION_CHAIN_UNVERIFIED)
            }
            RoomMigrationChainVerification.PARTIAL -> {
                return RoomRuntimeReadiness.Blocked(RoomRuntimeReadinessFailure.MIGRATION_CHAIN_PARTIAL)
            }
            RoomMigrationChainVerification.MISSING -> {
                return RoomRuntimeReadiness.Blocked(RoomRuntimeReadinessFailure.MIGRATION_CHAIN_MISSING)
            }
            RoomMigrationChainVerification.STALE -> {
                return RoomRuntimeReadiness.Blocked(RoomRuntimeReadinessFailure.MIGRATION_CHAIN_STALE)
            }
            RoomMigrationChainVerification.FAILED -> {
                return RoomRuntimeReadiness.Blocked(RoomRuntimeReadinessFailure.MIGRATION_CHAIN_FAILED)
            }
            RoomMigrationChainVerification.UNKNOWN -> {
                return RoomRuntimeReadiness.Blocked(RoomRuntimeReadinessFailure.MIGRATION_CHAIN_UNKNOWN)
            }
        }
        when (verification.liveShapeImport) {
            RoomImportValidationVerification.VERIFIED -> Unit
            RoomImportValidationVerification.UNVERIFIED -> {
                return RoomRuntimeReadiness.Blocked(RoomRuntimeReadinessFailure.LIVE_SHAPE_IMPORT_UNVERIFIED)
            }
            RoomImportValidationVerification.PARTIAL -> {
                return RoomRuntimeReadiness.Blocked(RoomRuntimeReadinessFailure.LIVE_SHAPE_IMPORT_PARTIAL)
            }
            RoomImportValidationVerification.MISSING -> {
                return RoomRuntimeReadiness.Blocked(RoomRuntimeReadinessFailure.LIVE_SHAPE_IMPORT_MISSING)
            }
            RoomImportValidationVerification.STALE -> {
                return RoomRuntimeReadiness.Blocked(RoomRuntimeReadinessFailure.LIVE_SHAPE_IMPORT_STALE)
            }
            RoomImportValidationVerification.FAILED -> {
                return RoomRuntimeReadiness.Blocked(RoomRuntimeReadinessFailure.LIVE_SHAPE_IMPORT_FAILED)
            }
            RoomImportValidationVerification.UNKNOWN -> {
                return RoomRuntimeReadiness.Blocked(RoomRuntimeReadinessFailure.LIVE_SHAPE_IMPORT_UNKNOWN)
            }
        }

        val latest = latestRun()
            ?: return RoomRuntimeReadiness.Blocked(RoomRuntimeReadinessFailure.NO_MIRROR_GENERATION)

        val status = latest.status.uppercase()
        when (status) {
            "IN_PROGRESS", "STARTED", "RUNNING" -> {
                return RoomRuntimeReadiness.Blocked(RoomRuntimeReadinessFailure.IMPORT_IN_PROGRESS)
            }
            "FAILED", "FAILURE", "ERROR" -> {
                return RoomRuntimeReadiness.Blocked(RoomRuntimeReadinessFailure.LATEST_GENERATION_FAILED)
            }
            "SUCCESS" -> Unit
            else -> return RoomRuntimeReadiness.Blocked(RoomRuntimeReadinessFailure.UNKNOWN_STATE)
        }

        if (latest.roomAuthoritative || !latest.jsonAuthoritative) {
            return RoomRuntimeReadiness.Blocked(RoomRuntimeReadinessFailure.UNKNOWN_STATE)
        }
        if (latest.mismatchCount != 0) {
            return RoomRuntimeReadiness.Blocked(RoomRuntimeReadinessFailure.MIRROR_MISMATCH)
        }
        if (nowMillis - latest.finishedAt > maxMirrorAgeMillis || latest.finishedAt > nowMillis) {
            return RoomRuntimeReadiness.Blocked(RoomRuntimeReadinessFailure.STALE_MIRROR)
        }
        if (latest.sourceFingerprint != RoomJsonFingerprint.sha256(currentJson)) {
            return RoomRuntimeReadiness.Blocked(RoomRuntimeReadinessFailure.JSON_FINGERPRINT_MISMATCH)
        }

        return RoomRuntimeReadiness.Ready(
            generationId = latest.generationId,
            schemaVersion = latest.schemaVersion,
        )
    }

    private companion object {
        const val DefaultMaxMirrorAgeMillis = 24L * 60L * 60L * 1000L
    }
}

sealed interface RoomRuntimeReadiness {
    val jsonAuthoritative: Boolean
        get() = true

    val roomAuthoritative: Boolean
        get() = false

    data class Ready(
        val generationId: String,
        val schemaVersion: Int,
    ) : RoomRuntimeReadiness

    data class Blocked(
        val reason: RoomRuntimeReadinessFailure,
    ) : RoomRuntimeReadiness
}

enum class RoomRuntimeReadinessFailure {
    MISSING_JSON,
    NO_MIRROR_GENERATION,
    LATEST_GENERATION_FAILED,
    IMPORT_IN_PROGRESS,
    JSON_FINGERPRINT_MISMATCH,
    MIRROR_MISMATCH,
    STALE_MIRROR,
    MIGRATION_CHAIN_UNVERIFIED,
    MIGRATION_CHAIN_PARTIAL,
    MIGRATION_CHAIN_MISSING,
    MIGRATION_CHAIN_STALE,
    MIGRATION_CHAIN_FAILED,
    MIGRATION_CHAIN_UNKNOWN,
    LIVE_SHAPE_IMPORT_UNVERIFIED,
    LIVE_SHAPE_IMPORT_PARTIAL,
    LIVE_SHAPE_IMPORT_MISSING,
    LIVE_SHAPE_IMPORT_STALE,
    LIVE_SHAPE_IMPORT_FAILED,
    LIVE_SHAPE_IMPORT_UNKNOWN,
    UNKNOWN_STATE,
}

enum class RoomMigrationChainVerification {
    VERIFIED,
    UNVERIFIED,
    PARTIAL,
    MISSING,
    STALE,
    FAILED,
    UNKNOWN,
}

enum class RoomImportValidationVerification {
    VERIFIED,
    UNVERIFIED,
    PARTIAL,
    MISSING,
    STALE,
    FAILED,
    UNKNOWN,
}

data class RoomRuntimeReadinessVerification(
    val migrationChain: RoomMigrationChainVerification = RoomMigrationChainVerification.UNVERIFIED,
    val liveShapeImport: RoomImportValidationVerification = RoomImportValidationVerification.UNVERIFIED,
)

object RoomJsonFingerprint {
    fun sha256(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
        return digest.joinToString(separator = "") { "%02x".format(it) }
    }
}
