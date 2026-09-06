package com.trainiq.core.testing

import com.trainiq.core.database.TrainIqDatabase
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface TrainIqDebugDatabaseEntryPoint {
    fun trainIqDatabase(): TrainIqDatabase
    fun dataCoordinator(): com.trainiq.data.repository.TrainIqDataCoordinator
}
