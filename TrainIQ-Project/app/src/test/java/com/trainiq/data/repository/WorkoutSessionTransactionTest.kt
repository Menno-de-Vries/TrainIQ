package com.trainiq.data.repository

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutSessionTransactionTest {
    private val source = File("src/main/java/com/trainiq/data/repository/RoomTrainIqRuntimeStore.kt").readText()

    @Test
    fun runtimeUpdatesAreSerializedAndCommittedThroughRoomImportTransaction() {
        val updateBody = source.substringAfter("suspend fun update(").substringBefore("suspend fun clearAll()")

        assertTrue(updateBody.contains("mutex.withLock"))
        assertTrue(updateBody.contains("sink.importTransaction"))
        assertTrue(updateBody.contains("planner.plan(gson.toJson(updated))"))
    }

    @Test
    fun legacySeedUsesSameRoomTransactionPath() {
        val seedBody = source.substringAfter("private suspend fun seedRoomFromLegacyJsonIfNeeded()").substringBeforeLast("}")

        assertTrue(seedBody.contains("mutex.withLock"))
        assertTrue(seedBody.contains("legacyStore.exportLegacyState()"))
        assertTrue(seedBody.contains("sink.importTransaction"))
    }
}
