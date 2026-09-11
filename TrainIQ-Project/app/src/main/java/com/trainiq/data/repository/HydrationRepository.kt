package com.trainiq.data.repository

import com.trainiq.core.database.TrainIqDatabase
import com.trainiq.core.database.HydrationEntity
import com.trainiq.domain.model.explicitVolumeMl
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HydrationRepository @Inject constructor(database: TrainIqDatabase) {
    private val dao = database.dao()
    fun observe() = dao.observeHydration()
    suspend fun save(id: String, timestamp: Long, amount: String) {
        require(id.isNotBlank() && !id.startsWith("meal:"))
        val volume = requireNotNull(explicitVolumeMl(amount, "ml"))
        dao.saveHydration(HydrationEntity(id, timestamp, volume))
    }
    suspend fun delete(id: String) {
        require(!id.startsWith("meal:"))
        dao.deleteHydration(id)
    }
}
