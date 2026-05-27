package com.trainiq.testing

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.trainiq.core.testing.TrainIqDebugDatabaseEntryPoint
import dagger.hilt.android.EntryPointAccessors
import com.trainiq.core.database.TrainIqDatabase

fun trainIqAndroidTestDatabase(
    context: Context = ApplicationProvider.getApplicationContext(),
): TrainIqDatabase =
    EntryPointAccessors.fromApplication(
        context.applicationContext,
        TrainIqDebugDatabaseEntryPoint::class.java,
    ).trainIqDatabase()

fun resetTrainIqAndroidTestDatabase(
    context: Context = ApplicationProvider.getApplicationContext(),
): TrainIqDatabase =
    trainIqAndroidTestDatabase(context).also { database ->
        database.clearAllTables()
    }
