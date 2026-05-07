package com.trainiq.data.migration

import com.trainiq.data.local.TrainIqStorageState
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RoomImportDryRunTest {
    @Test
    fun successfulDryRunImportsPlanButKeepsJsonAuthoritative() = runTest {
        val source = fixture("valid-representative-trainiq-state.json")
        val sink = RecordingDryRunSink()

        val status = RoomImportDryRun(JsonRoomImportPlanner(), sink)
            .attempt(sourceJson = source, loadedState = TrainIqStorageState())

        assertTrue(status is RoomImportDryRunStatus.Succeeded)
        status as RoomImportDryRunStatus.Succeeded
        assertFalse(status.roomAuthoritative)
        assertTrue(status.jsonAuthoritative)
        assertTrue(status.generationId.isNotBlank())
        assertTrue(status.importedRowCount > 0)
        assertEquals(0, status.mismatchCount)
        assertEquals(1, sink.committedPlans.size)
        assertEquals(source, fixture("valid-representative-trainiq-state.json"))
    }

    @Test
    fun failedDryRunIsNonFatalAndKeepsJsonAuthoritative() = runTest {
        val source = fixture("valid-representative-trainiq-state.json")
        val sink = RecordingDryRunSink(fail = true)

        val status = RoomImportDryRun(JsonRoomImportPlanner(), sink)
            .attempt(sourceJson = source, loadedState = TrainIqStorageState())

        assertTrue(status is RoomImportDryRunStatus.Failed)
        assertTrue(status.jsonAuthoritative)
        assertFalse(status.roomAuthoritative)
        assertEquals("IllegalStateException", (status as RoomImportDryRunStatus.Failed).errorType)
        assertTrue(sink.committedPlans.isEmpty())
    }

    @Test
    fun missingJsonIsSkippedWithoutImport() = runTest {
        val sink = RecordingDryRunSink()

        val status = RoomImportDryRun(JsonRoomImportPlanner(), sink)
            .attempt(sourceJson = null, loadedState = TrainIqStorageState())

        assertEquals(RoomImportDryRunStatus.SkippedMissingJson, status)
        assertTrue(status.jsonAuthoritative)
        assertFalse(status.roomAuthoritative)
        assertTrue(sink.committedPlans.isEmpty())
    }

    @Test
    fun invalidJsonLoadIsSkippedWithoutImport() = runTest {
        val sink = RecordingDryRunSink()

        val status = RoomImportDryRun(JsonRoomImportPlanner(), sink)
            .attempt(
                sourceJson = fixture("malformed-trainiq-state.json"),
                loadedState = TrainIqStorageState(),
                loadFailure = IllegalArgumentException("invalid json"),
            )

        assertEquals(RoomImportDryRunStatus.SkippedInvalidJson, status)
        assertTrue(status.jsonAuthoritative)
        assertFalse(status.roomAuthoritative)
        assertTrue(sink.committedPlans.isEmpty())
    }

    @Test
    fun repeatedDryRunImportIsStable() = runTest {
        val source = fixture("valid-representative-trainiq-state.json")
        val sink = RecordingDryRunSink()
        val dryRun = RoomImportDryRun(JsonRoomImportPlanner(), sink)

        val first = dryRun.attempt(sourceJson = source, loadedState = TrainIqStorageState())
        val second = dryRun.attempt(sourceJson = source, loadedState = TrainIqStorageState())

        assertTrue(first is RoomImportDryRunStatus.Succeeded)
        assertTrue(second is RoomImportDryRunStatus.Succeeded)
        assertEquals(sink.committedPlans[0], sink.committedPlans[1])
    }

    @Test
    fun dryRunStatusMapsToFailClosedReadinessVerification() {
        val success = RoomImportDryRunStatus.Succeeded(
            generationId = "generation",
            importedRowCount = 4,
            staleRowsRemoved = 0,
            mismatchCount = 0,
            loadedStateWasAvailable = true,
        )
        val mismatched = success.copy(mismatchCount = 1)

        assertEquals(
            RoomRuntimeReadinessVerification(
                migrationChain = RoomMigrationChainVerification.UNVERIFIED,
                liveShapeImport = RoomImportValidationVerification.VERIFIED,
            ),
            success.toReadinessVerification(RoomMigrationChainVerification.UNVERIFIED),
        )
        assertEquals(
            RoomImportValidationVerification.PARTIAL,
            mismatched.toReadinessVerification(RoomMigrationChainVerification.VERIFIED).liveShapeImport,
        )
        assertEquals(
            RoomImportValidationVerification.MISSING,
            RoomImportDryRunStatus.SkippedMissingJson
                .toReadinessVerification(RoomMigrationChainVerification.VERIFIED)
                .liveShapeImport,
        )
        assertEquals(
            RoomImportValidationVerification.FAILED,
            RoomImportDryRunStatus.Failed("IllegalStateException")
                .toReadinessVerification(RoomMigrationChainVerification.VERIFIED)
                .liveShapeImport,
        )
        assertEquals(
            RoomImportValidationVerification.UNVERIFIED,
            RoomImportDryRunStatus.NotAttempted
                .toReadinessVerification(RoomMigrationChainVerification.VERIFIED)
                .liveShapeImport,
        )
    }

    private fun fixture(name: String): String =
        requireNotNull(javaClass.classLoader?.getResource("room-import/$name")) { "Missing fixture $name" }
            .readText()
}

private class RecordingDryRunSink(
    private val fail: Boolean = false,
) : JsonRoomImportSink {
    val committedPlans = mutableListOf<JsonRoomImportPlan>()

    override suspend fun importTransaction(
        plan: JsonRoomImportPlan,
        mirrorRun: RoomMirrorImportRun?,
    ): RoomMirrorImportReport {
        if (fail) error("dry-run import failure")
        committedPlans += plan
        return RoomMirrorImportReport(
            generationId = mirrorRun?.generationId,
            expectedRowCount = plan.importedRowCount(),
            importedRowCount = plan.importedRowCount(),
            staleRowsRemoved = 0,
            mismatchCount = 0,
        )
    }
}
