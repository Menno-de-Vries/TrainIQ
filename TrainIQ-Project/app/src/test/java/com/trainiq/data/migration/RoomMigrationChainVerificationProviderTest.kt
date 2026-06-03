package com.trainiq.data.migration

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RoomMigrationChainVerificationProviderTest {
    @Test
    fun productionDefaultFailsClosedWhenNoTrustedMarkerExists() {
        val report = RoomMigrationChainVerificationProvider(
            markerSource = EmptyMarkerSource,
        ).report(nowMillis = Now)

        assertEquals(RoomMigrationChainVerification.NOT_RUN, report.status)
        assertEquals(RoomMigrationChainVerificationReason.NO_TRUSTED_MARKER, report.reason)
        assertEquals(15, report.currentRoomVersion)
        assertEquals(2, report.requiredStartVersion)
        assertEquals(15, report.requiredEndVersion)
        assertNull(report.coveredStartVersion)
        assertNull(report.coveredEndVersion)
        assertFalse(report.freshEnough)
    }

    @Test
    fun verifiedMarkerAllowsOnlyMigrationChainPartOfGate() {
        val report = providerWith(
            marker = validMarker(verifiedAtMillis = Now - 1_000L),
        ).report(nowMillis = Now)

        assertEquals(RoomMigrationChainVerification.VERIFIED, report.status)
        assertEquals(RoomMigrationChainVerificationReason.VERIFIED, report.reason)
        assertEquals(RoomMigrationChainVerificationProvider.expectedMarker(), report.lastVerificationMarker)
        assertTrue(report.freshEnough)
    }

    @Test
    fun staleMarkerNeverReportsVerified() {
        val report = providerWith(
            marker = validMarker(verifiedAtMillis = Now - RoomMigrationChainVerificationProvider.MaxVerificationAgeMillis - 1L),
        ).report(nowMillis = Now)

        assertEquals(RoomMigrationChainVerification.STALE, report.status)
        assertEquals(RoomMigrationChainVerificationReason.MARKER_STALE, report.reason)
        assertFalse(report.freshEnough)
    }

    @Test
    fun wrongBuildVariantNeverReportsVerified() {
        val report = providerWith(
            marker = validMarker(buildVariant = "release"),
        ).report(nowMillis = Now)

        assertEquals(RoomMigrationChainVerification.STALE, report.status)
        assertEquals(RoomMigrationChainVerificationReason.MARKER_STALE, report.reason)
        assertFalse(report.freshEnough)
    }

    @Test
    fun partialCoverageNeverReportsVerified() {
        val report = providerWith(
            marker = validMarker(coveredStartVersion = 3),
        ).report(nowMillis = Now)

        assertEquals(RoomMigrationChainVerification.PARTIAL, report.status)
        assertEquals(RoomMigrationChainVerificationReason.COVERAGE_PARTIAL, report.reason)
        assertFalse(report.freshEnough)
    }

    @Test
    fun invalidHashNeverReportsVerified() {
        val report = providerWith(
            marker = validMarker().copy(payloadSha256 = "not-a-valid-marker-hash"),
        ).report(nowMillis = Now)

        assertEquals(RoomMigrationChainVerification.FAILED, report.status)
        assertEquals(RoomMigrationChainVerificationReason.FAILED, report.reason)
        assertFalse(report.freshEnough)
    }

    @Test
    fun missingHashNeverReportsVerified() {
        val report = providerWith(
            marker = validMarker(includePayloadHash = false),
        ).report(nowMillis = Now)

        assertEquals(RoomMigrationChainVerification.UNKNOWN, report.status)
        assertEquals(RoomMigrationChainVerificationReason.UNKNOWN_MARKER_STATE, report.reason)
        assertFalse(report.freshEnough)
    }

    @Test
    fun missingProviderReportMapsToMissingStatus() {
        assertEquals(RoomMigrationChainVerification.MISSING, null.toMigrationChainVerification())
    }

    @Test
    fun gradleMarkerGeneratorMatchesProviderVersionContract() {
        val buildScript = File("build.gradle.kts").readText()
        val expectedEndVersion = RoomMigrationChainVerificationProvider.CurrentRoomVersion

        assertTrue(buildScript.contains("trainiq-room-migration-chain-v2-to-v$expectedEndVersion"))
        assertTrue(buildScript.contains("val currentRoomVersion = $expectedEndVersion"))
        assertTrue(buildScript.contains("val requiredEndVersion = $expectedEndVersion"))
        assertTrue(buildScript.contains("val coveredEndVersion = $expectedEndVersion"))
    }

    private fun providerWith(marker: RoomMigrationChainVerificationMarker) =
        RoomMigrationChainVerificationProvider(markerSource = StaticMarkerSource(marker))

    private fun validMarker(
        marker: String = RoomMigrationChainVerificationProvider.expectedMarker(),
        buildVariant: String = "debug",
        testTask: String = "connectedDebugAndroidTest",
        currentRoomVersion: Int = 15,
        requiredStartVersion: Int = 2,
        requiredEndVersion: Int = 15,
        coveredStartVersion: Int = 2,
        coveredEndVersion: Int = 15,
        verifiedAtMillis: Long = Now - 1_000L,
        migrationCount: Int = 10,
        includePayloadHash: Boolean = true,
    ): RoomMigrationChainVerificationMarker {
        val markerPayload = RoomMigrationChainVerificationMarker(
            marker = marker,
            buildVariant = buildVariant,
            testTask = testTask,
            currentRoomVersion = currentRoomVersion,
            requiredStartVersion = requiredStartVersion,
            requiredEndVersion = requiredEndVersion,
            coveredStartVersion = coveredStartVersion,
            coveredEndVersion = coveredEndVersion,
            verifiedAtMillis = verifiedAtMillis,
            migrationCount = migrationCount,
        )
        return if (includePayloadHash) {
            markerPayload.copy(payloadSha256 = markerPayload.verificationPayloadSha256())
        } else {
            markerPayload
        }
    }

    private class StaticMarkerSource(
        private val marker: RoomMigrationChainVerificationMarker,
    ) : RoomMigrationChainVerificationMarkerSource {
        override fun latestMarker(): RoomMigrationChainVerificationMarker = marker
    }

    private object EmptyMarkerSource : RoomMigrationChainVerificationMarkerSource {
        override fun latestMarker(): RoomMigrationChainVerificationMarker? = null
    }

    private companion object {
        const val Now = 4_000_000_000L
    }
}
