package com.trainiq.features.sleep

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.work.WorkManager
import com.trainiq.core.database.TrainIqDatabase
import com.trainiq.core.database.toDomain
import com.trainiq.core.database.toEntity
import com.trainiq.core.sleep.SleepRoutineScheduler
import com.trainiq.core.theme.TrainIqTheme
import com.trainiq.data.sleep.SleepRoutineRepository
import com.trainiq.domain.sleep.SleepRoutine
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class SleepRoutineInstrumentedTest {
    @get:Rule val compose = createComposeRule()

    @Test fun blockedPermissionsRemainActionableAndConfirmationIsExplicit() {
        var confirmed = 0
        var permissions = 0
        val active = SleepRoutine(enabled = true, routineDay = "2026-09-08")
        compose.setContent { TrainIqTheme(dynamicColor = false) {
            SleepRoutineScreen(SleepRoutineUiState.Success(active, sleepRoutineStatus(active, 1)),
                {}, { _, _ -> }, { confirmed++ }, {}, { permissions++ }, {}, {})
        } }
        compose.onNodeWithText("Ik ga binnen 2 minuten slapen").performScrollTo().assertHasClickAction().performClick()
        compose.onNodeWithText("Meldingen toestaan").performScrollTo().performClick()
        compose.onNodeWithText("Exacte alarms instellen").performScrollTo().assertHasClickAction()
        compose.runOnIdle { assertEquals(1, confirmed); assertEquals(1, permissions) }
    }

    @Test fun confirmedCountdownHasNoSecondConfirmationAction() {
        val confirmed = SleepRoutine(enabled = true, routineDay = "2026-09-08", confirmedAt = 1000)
        compose.setContent { TrainIqTheme(dynamicColor = false) {
            SleepRoutineScreen(SleepRoutineUiState.Success(confirmed, sleepRoutineStatus(confirmed, 61_000)),
                {}, { _, _ -> }, {}, {}, {}, {}, {})
        } }
        compose.onNodeWithText("Bevestigd: nog 1:00 om te gaan slapen.").performScrollTo().assertExists()
        compose.onNodeWithText("Ik ga binnen 2 minuten slapen").assertDoesNotExist()
    }

    @Test fun roomReopenPreservesConfirmationAndReconciliationPlansNextDay() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "sleep-routine-contract.db"
        context.deleteDatabase(name)
        fun open() = Room.databaseBuilder(context, TrainIqDatabase::class.java, name).build()
        val scheduler = SleepRoutineScheduler(context, WorkManager.getInstance(context))
        var database = open()
        try {
            var repo = SleepRoutineRepository(database.dao(), scheduler)
            repo.configure(true, 1320)
            assertTrue(database.dao().getSleepRoutine()!!.nextAt > System.currentTimeMillis())
            database.dao().saveSleepRoutine(SleepRoutine(enabled = true, nextAt = System.currentTimeMillis() - 1000).toEntity())
            repo.reconcile()
            val active = database.dao().getSleepRoutine()!!.toDomain()
            assertTrue(active.active)
            assertEquals(0L, active.confirmedAt)
            repo.confirm()
            val confirmed = database.dao().getSleepRoutine()!!
            assertTrue(confirmed.confirmedAt > 0)
            repo.confirm()
            assertEquals(confirmed.confirmedAt, database.dao().getSleepRoutine()!!.confirmedAt)
            database.close()
            database = open()
            repo = SleepRoutineRepository(database.dao(), scheduler)
            assertEquals(confirmed, database.dao().getSleepRoutine())
            database.dao().saveSleepRoutine(confirmed.copy(confirmedAt = System.currentTimeMillis() - 120_001))
            repo.reconcile()
            val done = database.dao().getSleepRoutine()!!.toDomain()
            assertFalse(done.active)
            assertTrue(done.nextAt > System.currentTimeMillis())
            repo.configure(false, 1320)
            repo.reconcile()
            assertFalse(database.dao().getSleepRoutine()!!.enabled)
            assertEquals(0L, database.dao().getSleepRoutine()!!.nextAt)
        } finally {
            scheduler.schedule(SleepRoutine())
            scheduler.cancelNotification()
            database.close()
            context.deleteDatabase(name)
        }
    }
}
