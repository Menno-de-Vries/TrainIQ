package com.trainiq.data.repository

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutSessionTransactionTest {
    private val source = File("src/main/java/com/trainiq/data/repository/RoomTrainIqRuntimeStore.kt").readText()

    @Test
    fun runtimeStoreDoesNotExposeBroadFullStateUpdateTransaction() {
        assertFalse(source.contains("suspend fun update(transform:"))
        assertFalse(source.contains("val updatedJson = gson.toJson(updated)"))
        assertFalse(source.contains("mirrorRun ="))
    }

    @Test
    fun legacySeedUsesSameRoomTransactionPath() {
        val seedBody = source.substringAfter("private suspend fun seedRoomFromLegacyJsonIfNeeded()").substringBeforeLast("}")

        assertTrue(seedBody.contains("mutex.withLock"))
        assertTrue(seedBody.contains("legacyStore.exportLegacyState()"))
        assertTrue(seedBody.contains("sink.importTransaction"))
    }
}
