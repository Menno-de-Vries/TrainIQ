package com.trainiq.data.migration

import com.trainiq.data.local.TrainIqStorageState
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomImportDryRun @Inject constructor(
    private val planner: JsonRoomImportPlanner,
    private val sink: JsonRoomImportSink,
) {
    suspend fun attempt(
        sourceJson: String?,
        loadedState: TrainIqStorageState,
        loadFailure: Throwable? = null,
    ): RoomImportDryRunStatus {
        if (sourceJson.isNullOrBlank()) return RoomImportDryRunStatus.SkippedMissingJson
        if (loadFailure != null) return RoomImportDryRunStatus.SkippedInvalidJson

        return runCatching {
            val plan = planner.plan(sourceJson)
            if (plan.schemaParityGaps.isNotEmpty()) {
                RoomImportDryRunStatus.SkippedRoomSchemaBlocker(
                    parityGapCount = plan.schemaParityGaps.size,
                )
            } else {
                val startedAt = System.currentTimeMillis()
                val fingerprint = RoomJsonFingerprint.sha256(sourceJson)
                val generationId = "dryrun-$startedAt-${fingerprint.take(12)}"
                val report = sink.importTransaction(
                    plan = plan,
                    mirrorRun = RoomMirrorImportRun(
                        generationId = generationId,
                        sourceFingerprint = fingerprint,
                        startedAt = startedAt,
                        finishedAt = System.currentTimeMillis(),
                    ),
                )
                RoomImportDryRunStatus.Succeeded(
                    generationId = generationId,
                    importedRowCount = report.importedRowCount,
                    staleRowsRemoved = report.staleRowsRemoved,
                    mismatchCount = report.mismatchCount,
                    loadedStateWasAvailable = loadedState != TrainIqStorageState(),
                )
            }
        }.getOrElse { throwable ->
            RoomImportDryRunStatus.Failed(
                errorType = throwable::class.simpleName ?: "UnknownError",
            )
        }
    }

}

sealed interface RoomImportDryRunStatus {
    val jsonAuthoritative: Boolean
        get() = true

    val roomAuthoritative: Boolean
        get() = false

    data object NotAttempted : RoomImportDryRunStatus
    data object SkippedMissingJson : RoomImportDryRunStatus
    data object SkippedInvalidJson : RoomImportDryRunStatus

    data class SkippedRoomSchemaBlocker(
        val parityGapCount: Int,
    ) : RoomImportDryRunStatus

    data class Succeeded(
        val generationId: String,
        val importedRowCount: Int,
        val staleRowsRemoved: Int,
        val mismatchCount: Int,
        val loadedStateWasAvailable: Boolean,
    ) : RoomImportDryRunStatus

    data class Failed(
        val errorType: String,
    ) : RoomImportDryRunStatus
}

fun RoomImportDryRunStatus.toReadinessVerification(
    migrationChain: RoomMigrationChainVerification,
): RoomRuntimeReadinessVerification = RoomRuntimeReadinessVerification(
    migrationChain = migrationChain,
    liveShapeImport = when (this) {
        RoomImportDryRunStatus.NotAttempted -> RoomImportValidationVerification.UNVERIFIED
        RoomImportDryRunStatus.SkippedMissingJson -> RoomImportValidationVerification.MISSING
        RoomImportDryRunStatus.SkippedInvalidJson -> RoomImportValidationVerification.FAILED
        is RoomImportDryRunStatus.SkippedRoomSchemaBlocker -> RoomImportValidationVerification.PARTIAL
        is RoomImportDryRunStatus.Succeeded -> {
            if (mismatchCount == 0) {
                RoomImportValidationVerification.VERIFIED
            } else {
                RoomImportValidationVerification.PARTIAL
            }
        }
        is RoomImportDryRunStatus.Failed -> RoomImportValidationVerification.FAILED
    },
)
