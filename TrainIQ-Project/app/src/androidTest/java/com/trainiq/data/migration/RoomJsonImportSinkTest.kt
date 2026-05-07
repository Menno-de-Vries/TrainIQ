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

class RoomJsonImportSinkTest {
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
    fun successfulRepresentativeImportPopulatesRoomTables() = runTest {
        val source = fixture("valid-representative-trainiq-state.json")

        val outcome = JsonRoomImportCoordinator(JsonRoomImportPlanner(), RoomJsonImportSink(database))
            .tryImport(sourceJson = source, fallbackState = TrainIqStorageState())

        assertTrue((outcome as? JsonRoomImportOutcome.Failed)?.cause?.message.orEmpty(), outcome is JsonRoomImportOutcome.Imported)
        assertTrue(outcome.roomTrusted)
        assertEquals(1, database.dao().userProfileCount())
        assertEquals(1, database.dao().foodCount())
        assertEquals(1, database.dao().recipeCount())
        assertEquals(1, database.dao().recipeIngredientCount())
        assertEquals(1, database.dao().mealItemCount())
        assertEquals(1, database.dao().activeWorkoutSessionCount())
        assertEquals(1, database.dao().workoutLogEventCount())
        assertEquals(1, database.dao().activeWorkoutSetCount())
        assertEquals(1, database.dao().workoutLogEventCurrentSetCount())
        assertTrue(JsonRoomImportPlanner().plan(source).schemaParityGaps.isEmpty())
        assertEquals(source, fixture("valid-representative-trainiq-state.json"))
    }

    @Test
    fun successfulCurrentLiveShapeImportPopulatesRoomMirrorTables() = runTest {
        val source = fixture("live-shape-current-trainiq-state.json")

        val outcome = JsonRoomImportCoordinator(JsonRoomImportPlanner(), RoomJsonImportSink(database))
            .tryImport(sourceJson = source, fallbackState = TrainIqStorageState())

        assertTrue((outcome as? JsonRoomImportOutcome.Failed)?.cause?.message.orEmpty(), outcome is JsonRoomImportOutcome.Imported)
        outcome as JsonRoomImportOutcome.Imported
        assertTrue(outcome.roomTrusted)
        assertTrue(outcome.plan.importedRowCount() > 25)
        assertTrue(outcome.plan.schemaParityGaps.isEmpty())
        assertEquals(1, database.dao().userProfileCount())
        assertEquals(2, database.dao().exerciseCount())
        assertEquals(1, database.dao().routineCount())
        assertEquals(2, database.dao().foodCount())
        assertEquals(1, database.dao().recipeCount())
        assertEquals(2, database.dao().recipeIngredientCount())
        assertEquals(1, database.dao().mealItemCount())
        assertEquals(1, database.dao().activeWorkoutSessionCount())
        assertEquals(1, database.dao().activeWorkoutSetCount())
        assertEquals(1, database.dao().workoutLogEventCount())
        assertEquals(1, database.dao().workoutLogEventCurrentSetCount())
        assertEquals(source, fixture("live-shape-current-trainiq-state.json"))
    }

    @Test
    fun failedImportRollsBackRoomWritesAndKeepsJsonFallbackUntrusted() = runTest {
        val source = fixture("valid-representative-trainiq-state.json")
        val fallbackState = TrainIqStorageState()

        val outcome = JsonRoomImportCoordinator(
            JsonRoomImportPlanner(),
            RoomJsonImportSink(database, failAfterCoreTables = true),
        ).tryImport(sourceJson = source, fallbackState = fallbackState)

        assertTrue(outcome is JsonRoomImportOutcome.Failed)
        outcome as JsonRoomImportOutcome.Failed
        assertFalse(outcome.roomTrusted)
        assertEquals(source, outcome.sourceJson)
        assertEquals(fallbackState, outcome.fallbackState)
        assertEquals(0, database.dao().userProfileCount())
        assertEquals(0, database.dao().foodCount())
        assertEquals(0, database.dao().activeWorkoutSessionCount())
        assertEquals(0, database.dao().workoutLogEventCount())
        assertEquals(source, fixture("valid-representative-trainiq-state.json"))
    }

    @Test
    fun repeatedImportIsStableForFixtureIds() = runTest {
        val source = fixture("valid-representative-trainiq-state.json")
        val coordinator = JsonRoomImportCoordinator(JsonRoomImportPlanner(), RoomJsonImportSink(database))

        val first = coordinator.tryImport(sourceJson = source, fallbackState = TrainIqStorageState())
        val second = coordinator.tryImport(sourceJson = source, fallbackState = TrainIqStorageState())

        assertTrue((first as? JsonRoomImportOutcome.Failed)?.cause?.message.orEmpty(), first is JsonRoomImportOutcome.Imported)
        assertTrue((second as? JsonRoomImportOutcome.Failed)?.cause?.message.orEmpty(), second is JsonRoomImportOutcome.Imported)
        assertEquals(1, database.dao().foodCount())
        assertEquals(1, database.dao().mealItemCount())
        assertEquals(1, database.dao().activeWorkoutSessionCount())
        assertEquals(1, database.dao().workoutLogEventCount())
    }

    private fun fixture(name: String): String =
        InstrumentationRegistry.getInstrumentation()
            .context
            .assets
            .open("room-import/$name")
            .bufferedReader()
            .use { it.readText() }
}
