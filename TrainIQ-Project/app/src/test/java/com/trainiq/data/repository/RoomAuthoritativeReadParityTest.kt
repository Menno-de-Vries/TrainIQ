package com.trainiq.data.repository

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RoomAuthoritativeReadParityTest {
    private val root = File(System.getProperty("user.dir"), "src/main/java/com/trainiq")

    @Test
    fun repositoryReadsRuntimeStateFromRoomRuntimeStoreOnly() {
        val repository = File(root, "data/repository/TrainIqRepository.kt").readText()

        assertTrue(repository.contains("combine(runtimeStore.state, scannedMealResult)"))
        assertFalse(repository.contains("TrainIqLocalStore"))
        assertFalse(repository.contains(".json"))
    }

    @Test
    fun roomRuntimeStoreSeedsRoomFromLegacyJsonWhenDatabaseIsEmpty() {
        val store = File(root, "data/repository/RoomTrainIqRuntimeStore.kt").readText()

        assertTrue(store.contains("seedRoomFromLegacyJsonIfNeeded()"))
        assertTrue(store.contains("legacyStore.exportLegacyState()"))
        assertTrue(store.contains("if (dao.mirrorRowCount() > 0) return"))
        assertTrue(store.contains("sink.importTransaction"))
    }
}
