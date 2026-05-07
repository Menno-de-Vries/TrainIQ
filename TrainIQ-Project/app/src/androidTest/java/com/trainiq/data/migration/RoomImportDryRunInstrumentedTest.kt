package com.trainiq.data.migration

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.trainiq.core.database.TrainIqDatabase
import com.trainiq.data.local.TrainIqStorageState
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RoomImportDryRunInstrumentedTest {
    private lateinit var database: TrainIqDatabase

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            TrainIqDatabase::class.java,
        ).build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun successfulDryRunPopulatesRoomButDoesNotMakeRoomAuthoritative() = runTest {
        val source = fixture("valid-representative-trainiq-state.json")

        val status = RoomImportDryRun(JsonRoomImportPlanner(), RoomJsonImportSink(database))
            .attempt(sourceJson = source, loadedState = TrainIqStorageState())

        assertTrue(status is RoomImportDryRunStatus.Succeeded)
        status as RoomImportDryRunStatus.Succeeded
        assertTrue(status.jsonAuthoritative)
        assertFalse(status.roomAuthoritative)
        assertEquals(0, status.mismatchCount)
        assertTrue(status.generationId.isNotBlank())
        assertEquals(1, database.dao().foodCount())
        assertEquals(1, database.dao().mealItemCount())
        assertEquals(1, database.dao().activeWorkoutSessionCount())
        assertEquals(1, database.dao().workoutLogEventCount())
        val latestRun = database.dao().latestMirrorImportRun()
        assertEquals(status.generationId, latestRun?.generationId)
        assertEquals("SUCCESS", latestRun?.status)
        assertEquals(0, latestRun?.mismatchCount)
        assertFalse(latestRun?.roomAuthoritative ?: true)
        assertTrue(latestRun?.jsonAuthoritative ?: false)
        assertEquals(source, fixture("valid-representative-trainiq-state.json"))
    }

    @Test
    fun failedDryRunRollsBackRoomAndKeepsJsonAuthoritative() = runTest {
        val source = fixture("valid-representative-trainiq-state.json")
        val successfulStatus = RoomImportDryRun(JsonRoomImportPlanner(), RoomJsonImportSink(database))
            .attempt(sourceJson = source, loadedState = TrainIqStorageState())
        assertTrue(successfulStatus is RoomImportDryRunStatus.Succeeded)

        val status = RoomImportDryRun(
            JsonRoomImportPlanner(),
            RoomJsonImportSink(database, failAfterCoreTables = true),
        ).attempt(sourceJson = source, loadedState = TrainIqStorageState())

        assertTrue(status is RoomImportDryRunStatus.Failed)
        assertTrue(status.jsonAuthoritative)
        assertFalse(status.roomAuthoritative)
        assertEquals(1, database.dao().userProfileCount())
        assertEquals(1, database.dao().foodCount())
        assertEquals(1, database.dao().activeWorkoutSessionCount())
        assertEquals((successfulStatus as RoomImportDryRunStatus.Succeeded).generationId, database.dao().latestMirrorImportRun()?.generationId)
        assertEquals(source, fixture("valid-representative-trainiq-state.json"))
    }

    @Test
    fun successfulDryRunRemovesRecordsMissingFromLaterJson() = runTest {
        val fullSource = fixture("valid-representative-trainiq-state.json")
        val minimalSource = fixture("minimal-valid-trainiq-state.json")
        val dryRun = RoomImportDryRun(JsonRoomImportPlanner(), RoomJsonImportSink(database))

        val first = dryRun.attempt(sourceJson = fullSource, loadedState = TrainIqStorageState())
        assertTrue(first is RoomImportDryRunStatus.Succeeded)
        assertEquals(1, database.dao().foodCount())

        val second = dryRun.attempt(sourceJson = minimalSource, loadedState = TrainIqStorageState())

        assertTrue(second is RoomImportDryRunStatus.Succeeded)
        second as RoomImportDryRunStatus.Succeeded
        assertEquals(0, database.dao().foodCount())
        assertEquals(0, database.dao().mealItemCount())
        assertEquals(0, database.dao().workoutLogEventCount())
        assertTrue(second.staleRowsRemoved > 0)
        assertEquals(second.generationId, database.dao().latestMirrorImportRun()?.generationId)
    }

    private fun fixture(name: String): String =
        InstrumentationRegistry.getInstrumentation()
            .context
            .assets
            .open("room-import/$name")
            .bufferedReader()
            .use { it.readText() }
}
