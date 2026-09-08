package com.trainiq.features.sleep

import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.WorkManager
import com.trainiq.core.database.toEntity
import com.trainiq.core.sleep.SleepChannelId
import com.trainiq.core.sleep.SleepRoutineScheduler
import com.trainiq.data.sleep.SleepRoutineRepository
import com.trainiq.domain.sleep.SleepRoutine
import com.trainiq.domain.sleep.SleepAlarmDelivery
import com.trainiq.testing.trainIqAndroidTestDatabase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class SleepAlarmRegressionTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val scheduler = SleepRoutineScheduler(context, WorkManager.getInstance(context))

    @Test fun notificationFailureCannotRemoveTheNextAlarm() = runBlocking {
        val dao = trainIqAndroidTestDatabase(context).dao()
        val delivery = object : SleepAlarmDelivery by scheduler {
            override fun showReminder(escalated: Boolean) { throw SecurityException("Notification access revoked") }
        }
        val repository = SleepRoutineRepository(dao, delivery)
        try {
            dao.saveSleepRoutine(SleepRoutine(enabled = true, nextAt = System.currentTimeMillis() - 1).toEntity())
            repeat(3) {
                try { repository.reconcile(); fail("Expected the delivery failure") } catch (_: SecurityException) { }
                val state = dao.getSleepRoutine()!!
                assertTrue(state.routineDay.isNotEmpty())
                assertEquals(0L, state.confirmedAt)
                assertTrue(state.nextAt > System.currentTimeMillis())
                assertEquals("A delivery failure must leave one OS successor", 1, pendingSleepAlarms())
                dao.saveSleepRoutine(state.copy(nextAt = System.currentTimeMillis() - 1))
            }
        } finally { repository.clear() }
    }

    @Test fun reminderSoundUsesAlarmStream() {
        scheduler.notificationsAllowed()
        val channel = context.getSystemService(NotificationManager::class.java).getNotificationChannel(SleepChannelId)
        assertEquals(AudioAttributes.USAGE_ALARM, channel.audioAttributes.usage)
    }

    @Test fun unconfirmedRoutineKeepsThreeTenMinuteSuccessors() = runBlocking {
        val dao = trainIqAndroidTestDatabase(context).dao()
        val repository = SleepRoutineRepository(dao, scheduler)
        try {
            dao.saveSleepRoutine(SleepRoutine(enabled = true, nextAt = System.currentTimeMillis() - 1000).toEntity())
            repeat(3) {
                val before = System.currentTimeMillis()
                repository.reconcile()
                val state = dao.getSleepRoutine()!!
                assertTrue(state.routineDay.isNotEmpty())
                assertEquals(0L, state.confirmedAt)
                assertTrue("Next alert must be ten minutes away: ${state.nextAt - before}",
                    state.nextAt in (before + 600_000)..(System.currentTimeMillis() + 600_000))
                repository.reconcile()
                assertEquals(state.nextAt, dao.getSleepRoutine()!!.nextAt)
                assertEquals("Reconciliation must not duplicate or lose the OS alarm", 1, pendingSleepAlarms())
                dao.saveSleepRoutine(state.copy(nextAt = System.currentTimeMillis() - 1))
            }
            repository.confirm()
            val confirmed = dao.getSleepRoutine()!!
            assertTrue(confirmed.confirmedAt > 0)
            repository.confirm()
            assertEquals(confirmed, dao.getSleepRoutine())
            assertEquals(1, pendingSleepAlarms()) // countdown replaces escalation
            repository.configure(false, confirmed.minuteOfDay)
            assertEquals(0, pendingSleepAlarms())
            repository.configure(true, confirmed.minuteOfDay)
            assertEquals(1, pendingSleepAlarms())
            repository.configure(true, (confirmed.minuteOfDay + 60) % 1440)
            assertEquals(1, pendingSleepAlarms())
        } finally { repository.clear() }
    }

    /** Inspect actual pending RTC records, not historical cancellation/wakeup summaries. */
    private fun pendingSleepAlarms(): Int {
        val descriptor = InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand("dumpsys alarm")
        val dump = android.os.ParcelFileDescriptor.AutoCloseInputStream(descriptor).use { it.readBytes().decodeToString() }
        return Regex("RTC_WAKEUP #\\d+: Alarm\\{[^\\n]* com\\.trainiq\\}").findAll(dump).count()
    }
}
