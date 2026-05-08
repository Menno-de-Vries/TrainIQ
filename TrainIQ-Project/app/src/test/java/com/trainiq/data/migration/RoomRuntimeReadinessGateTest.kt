package com.trainiq.data.migration

import com.trainiq.core.database.RoomMirrorImportRunEntity
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RoomRuntimeReadinessGateTest {
    @Test
    fun readyOnlyWhenLatestSuccessfulGenerationMatchesJsonWithZeroMismatchesAndVerifiedMigrations() = runTest {
        val json = fixture("valid-representative-trainiq-state.json")
        val run = mirrorRun(
            sourceFingerprint = RoomJsonFingerprint.sha256(json),
            status = "SUCCESS",
            mismatchCount = 0,
        )

        val result = RoomRuntimeReadinessGate(latestRun = { run }).evaluate(
            currentJson = json,
            verification = VerifiedPreconditions,
            nowMillis = Now,
        )

        assertTrue(result is RoomRuntimeReadiness.Ready)
        assertTrue(result.jsonAuthoritative)
        assertFalse(result.roomAuthoritative)
        assertEquals("generation-1", (result as RoomRuntimeReadiness.Ready).generationId)
    }

    @Test
    fun blocksWhenNoMirrorGenerationExists() = runTest {
        val result = RoomRuntimeReadinessGate(latestRun = { null }).evaluate(
            currentJson = "{}",
            verification = VerifiedPreconditions,
            nowMillis = Now,
        )

        assertBlocked(result, RoomRuntimeReadinessFailure.NO_MIRROR_GENERATION)
    }

    @Test
    fun blocksWhenLatestGenerationFailed() = runTest {
        val result = gateWith(
            mirrorRun(status = "FAILED", sourceFingerprint = RoomJsonFingerprint.sha256("{}")),
        ).evaluate("{}", verification = VerifiedPreconditions, nowMillis = Now)

        assertBlocked(result, RoomRuntimeReadinessFailure.LATEST_GENERATION_FAILED)
    }

    @Test
    fun blocksWhenLatestGenerationIsInProgress() = runTest {
        val result = gateWith(
            mirrorRun(status = "IN_PROGRESS", sourceFingerprint = RoomJsonFingerprint.sha256("{}")),
        ).evaluate("{}", verification = VerifiedPreconditions, nowMillis = Now)

        assertBlocked(result, RoomRuntimeReadinessFailure.IMPORT_IN_PROGRESS)
    }

    @Test
    fun blocksWhenJsonFingerprintDiffers() = runTest {
        val result = gateWith(
            mirrorRun(sourceFingerprint = RoomJsonFingerprint.sha256("""{"old":true}""")),
        ).evaluate("""{"new":true}""", verification = VerifiedPreconditions, nowMillis = Now)

        assertBlocked(result, RoomRuntimeReadinessFailure.JSON_FINGERPRINT_MISMATCH)
    }

    @Test
    fun blocksWhenMirrorHasMismatches() = runTest {
        val json = "{}"
        val result = gateWith(
            mirrorRun(sourceFingerprint = RoomJsonFingerprint.sha256(json), mismatchCount = 1),
        ).evaluate(json, verification = VerifiedPreconditions, nowMillis = Now)

        assertBlocked(result, RoomRuntimeReadinessFailure.MIRROR_MISMATCH)
    }

    @Test
    fun blocksWhenMirrorIsStale() = runTest {
        val json = "{}"
        val result = gateWith(
            mirrorRun(sourceFingerprint = RoomJsonFingerprint.sha256(json), finishedAt = Now - TwoDaysMillis),
        ).evaluate(json, verification = VerifiedPreconditions, nowMillis = Now)

        assertBlocked(result, RoomRuntimeReadinessFailure.STALE_MIRROR)
    }

    @Test
    fun blocksWhenMigrationChainIsNotVerified() = runTest {
        val json = "{}"
        val result = gateWith(
            mirrorRun(sourceFingerprint = RoomJsonFingerprint.sha256(json)),
        ).evaluate(json, migrationChainVerified = false, nowMillis = Now)

        assertBlocked(result, RoomRuntimeReadinessFailure.MIGRATION_CHAIN_UNVERIFIED)
    }

    @Test
    fun blocksWhenMigrationChainCoverageIsPartialMissingStaleFailedOrUnknown() = runTest {
        val json = "{}"
        val gate = gateWith(mirrorRun(sourceFingerprint = RoomJsonFingerprint.sha256(json)))

        val cases = mapOf(
            RoomMigrationChainVerification.UNVERIFIED to RoomRuntimeReadinessFailure.MIGRATION_CHAIN_UNVERIFIED,
            RoomMigrationChainVerification.PARTIAL to RoomRuntimeReadinessFailure.MIGRATION_CHAIN_PARTIAL,
            RoomMigrationChainVerification.MISSING to RoomRuntimeReadinessFailure.MIGRATION_CHAIN_MISSING,
            RoomMigrationChainVerification.STALE to RoomRuntimeReadinessFailure.MIGRATION_CHAIN_STALE,
            RoomMigrationChainVerification.FAILED to RoomRuntimeReadinessFailure.MIGRATION_CHAIN_FAILED,
            RoomMigrationChainVerification.UNKNOWN to RoomRuntimeReadinessFailure.MIGRATION_CHAIN_UNKNOWN,
            RoomMigrationChainVerification.NOT_RUN to RoomRuntimeReadinessFailure.MIGRATION_CHAIN_NOT_RUN,
        )

        cases.forEach { (status, expectedReason) ->
            val result = gate.evaluate(
                currentJson = json,
                migrationChainStatus = status,
                nowMillis = Now,
            )
            assertBlocked(result, expectedReason)
        }
    }

    @Test
    fun missingMigrationProviderReportBlocksReadiness() = runTest {
        val json = "{}"
        val result = gateWith(
            mirrorRun(sourceFingerprint = RoomJsonFingerprint.sha256(json)),
        ).evaluate(
            currentJson = json,
            verification = RoomRuntimeReadinessVerification(
                migrationChain = null.toMigrationChainVerification(),
                liveShapeImport = RoomImportValidationVerification.VERIFIED,
            ),
            nowMillis = Now,
        )

        assertBlocked(result, RoomRuntimeReadinessFailure.MIGRATION_CHAIN_MISSING)
    }

    @Test
    fun blocksWhenLiveShapeImportValidationIsNotVerified() = runTest {
        val json = "{}"
        val gate = gateWith(mirrorRun(sourceFingerprint = RoomJsonFingerprint.sha256(json)))

        val cases = mapOf(
            RoomImportValidationVerification.UNVERIFIED to RoomRuntimeReadinessFailure.LIVE_SHAPE_IMPORT_UNVERIFIED,
            RoomImportValidationVerification.PARTIAL to RoomRuntimeReadinessFailure.LIVE_SHAPE_IMPORT_PARTIAL,
            RoomImportValidationVerification.MISSING to RoomRuntimeReadinessFailure.LIVE_SHAPE_IMPORT_MISSING,
            RoomImportValidationVerification.STALE to RoomRuntimeReadinessFailure.LIVE_SHAPE_IMPORT_STALE,
            RoomImportValidationVerification.FAILED to RoomRuntimeReadinessFailure.LIVE_SHAPE_IMPORT_FAILED,
            RoomImportValidationVerification.UNKNOWN to RoomRuntimeReadinessFailure.LIVE_SHAPE_IMPORT_UNKNOWN,
        )

        cases.forEach { (status, expectedReason) ->
            val result = gate.evaluate(
                currentJson = json,
                verification = RoomRuntimeReadinessVerification(
                    migrationChain = RoomMigrationChainVerification.VERIFIED,
                    liveShapeImport = status,
                ),
                nowMillis = Now,
            )
            assertBlocked(result, expectedReason)
        }
    }

    @Test
    fun unknownStatesFailClosed() = runTest {
        val json = "{}"
        val result = gateWith(
            mirrorRun(sourceFingerprint = RoomJsonFingerprint.sha256(json), status = "WEIRD"),
        ).evaluate(json, verification = VerifiedPreconditions, nowMillis = Now)

        assertBlocked(result, RoomRuntimeReadinessFailure.UNKNOWN_STATE)
    }

    @Test
    fun missingJsonFailsClosedAndKeepsJsonAuthoritative() = runTest {
        val result = gateWith(mirrorRun()).evaluate(
            currentJson = null,
            migrationChainVerified = true,
            nowMillis = Now,
        )

        assertBlocked(result, RoomRuntimeReadinessFailure.MISSING_JSON)
    }

    private fun gateWith(run: RoomMirrorImportRunEntity) = RoomRuntimeReadinessGate(latestRun = { run })

    private fun assertBlocked(result: RoomRuntimeReadiness, reason: RoomRuntimeReadinessFailure) {
        assertTrue(result is RoomRuntimeReadiness.Blocked)
        result as RoomRuntimeReadiness.Blocked
        assertEquals(reason, result.reason)
        assertTrue(result.jsonAuthoritative)
        assertFalse(result.roomAuthoritative)
    }

    private fun mirrorRun(
        status: String = "SUCCESS",
        sourceFingerprint: String = "fingerprint",
        finishedAt: Long = Now,
        mismatchCount: Int = 0,
    ) = RoomMirrorImportRunEntity(
        generationId = "generation-1",
        sourceFingerprint = sourceFingerprint,
        startedAt = finishedAt - 100L,
        finishedAt = finishedAt,
        status = status,
        schemaVersion = 12,
        expectedRowCount = 1,
        importedRowCount = 1,
        staleRowCount = 0,
        mismatchCount = mismatchCount,
        jsonAuthoritative = true,
        roomAuthoritative = false,
    )

    private fun fixture(name: String): String =
        requireNotNull(javaClass.classLoader?.getResource("room-import/$name")) { "Missing fixture $name" }
            .readText()

    private companion object {
        const val Now = 100_000_000L
        const val TwoDaysMillis = 48L * 60L * 60L * 1000L
        val VerifiedPreconditions = RoomRuntimeReadinessVerification(
            migrationChain = RoomMigrationChainVerification.VERIFIED,
            liveShapeImport = RoomImportValidationVerification.VERIFIED,
        )
    }
}
