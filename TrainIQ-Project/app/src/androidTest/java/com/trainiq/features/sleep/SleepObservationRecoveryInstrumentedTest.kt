package com.trainiq.features.sleep

import android.content.Context
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.work.WorkManager
import com.trainiq.core.database.TrainIqDao
import com.trainiq.core.database.TrainIqDatabase
import com.trainiq.core.sleep.SleepRoutineScheduler
import com.trainiq.core.theme.TrainIqTheme
import com.trainiq.data.sleep.SleepRoutineRepository
import com.trainiq.domain.sleep.SleepAlarmDelivery
import com.trainiq.domain.sleep.SleepRoutine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.emitAll
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SleepObservationRecoveryInstrumentedTest {
    @get:Rule val compose = createComposeRule()

    @Test fun failedObservationCanRetryWithoutLeavingTheScreen() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = Room.inMemoryDatabaseBuilder(context, TrainIqDatabase::class.java).build()
        val store = ViewModelStore()
        var subscriptions = 0
        val realDao = database.dao()
        val dao = object : TrainIqDao by realDao {
            override fun observeSleepRoutine() = flow {
                subscriptions++
                if (subscriptions == 1) error("synthetic read failure")
                emitAll(realDao.observeSleepRoutine())
            }
        }
        val delivery = object : SleepAlarmDelivery {
            override fun schedule(state: SleepRoutine) = Unit
            override fun showReminder(escalated: Boolean) = Unit
            override fun showCountdown(state: SleepRoutine) = Unit
            override fun cancelNotification() = Unit
        }
        lateinit var viewModel: SleepRoutineViewModel
        try {
            compose.runOnUiThread {
                viewModel = SleepRoutineViewModel(
                    SleepRoutineRepository(dao, delivery),
                    SleepRoutineScheduler(context, WorkManager.getInstance(context)),
                )
                store.put("sleep", viewModel)
            }
            compose.setContent {
                val state by viewModel.uiState.collectAsState()
                TrainIqTheme {
                    SleepRoutineScreen(state, {}, { _, _ -> }, {}, viewModel::refresh, {}, {}, {})
                }
            }
            compose.waitUntil(10_000) { viewModel.uiState.value is SleepRoutineUiState.Error }
            compose.onNodeWithText("Opnieuw proberen").assertIsDisplayed().performClick()
            compose.waitUntil(10_000) { viewModel.uiState.value is SleepRoutineUiState.Success }
            compose.onNodeWithText("Slaapvoorbereiding staat uit.").assertIsDisplayed()
            compose.runOnIdle { assertEquals(2, subscriptions) }
        } finally {
            compose.runOnUiThread { store.clear() }
            database.close()
        }
    }
}
