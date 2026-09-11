package com.trainiq.features.sleep

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.assertIsDisplayed
import androidx.work.WorkManager
import com.trainiq.core.sleep.SleepRoutineScheduler
import com.trainiq.domain.sleep.SleepRoutine
import com.trainiq.core.database.toEntity
import com.trainiq.core.sleep.SleepRoutineEntryPoint
import com.trainiq.core.sleep.SleepRoutineReceiver
import com.trainiq.testing.trainIqAndroidTestDatabase
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Runs only on the isolated local test target; never changes DND, volume or alarm access. */
class SleepNotificationInstrumentedTest {
    @get:Rule val compose = createComposeRule()
    @Test fun alarmStreamPlaysInVibrateModeAndCancelReleasesIt() {
        compose.setContent { androidx.compose.material3.Text("Alarm playback contract") }
        val context = ApplicationProvider.getApplicationContext<Context>()
        if (Build.VERSION.SDK_INT >= 33) InstrumentationRegistry.getInstrumentation().uiAutomation
            .grantRuntimePermission(context.packageName, Manifest.permission.POST_NOTIFICATIONS)
        val audio = context.getSystemService(android.media.AudioManager::class.java)
        val originalMode = audio.ringerMode
        val scheduler = SleepRoutineScheduler(context, WorkManager.getInstance(context))
        try {
            audio.ringerMode = android.media.AudioManager.RINGER_MODE_VIBRATE
            assertEquals(android.media.AudioManager.RINGER_MODE_VIBRATE, audio.ringerMode)
            scheduler.showReminder(false)
            compose.waitUntil(10_000) { audio.activePlaybackConfigurations.any {
                it.audioAttributes.usage == android.media.AudioAttributes.USAGE_ALARM
            } }
            scheduler.cancelNotification()
            compose.waitUntil(10_000) { audio.activePlaybackConfigurations.none {
                it.audioAttributes.usage == android.media.AudioAttributes.USAGE_ALARM
            } }
        } finally { scheduler.cancelNotification(); audio.ringerMode = originalMode }
    }

    @Test fun reminderEscalatesAndNotificationOpensRealSleepScreen() {
        compose.setContent { androidx.compose.material3.Text("Notification contract") }
        val context = ApplicationProvider.getApplicationContext<Context>()
        if (Build.VERSION.SDK_INT >= 33) InstrumentationRegistry.getInstrumentation().uiAutomation
            .grantRuntimePermission(context.packageName, Manifest.permission.POST_NOTIFICATIONS)
        val scheduler = SleepRoutineScheduler(context, WorkManager.getInstance(context))
        val manager = context.getSystemService(NotificationManager::class.java)
        val database = trainIqAndroidTestDatabase(context)
        val repository = EntryPointAccessors.fromApplication(context, SleepRoutineEntryPoint::class.java).sleepRoutineRepository()
        runBlocking {
            database.dao().saveSleepRoutine(SleepRoutine(enabled = true,
                nextAt = System.currentTimeMillis() - 1000).toEntity())
        }
        try {
            context.sendBroadcast(Intent(context, SleepRoutineReceiver::class.java).setAction("com.trainiq.SLEEP_ROUTINE"))
            compose.waitUntil(10_000) { manager.activeNotifications.any { it.id == 2010 } }
            val first = manager.activeNotifications.single { it.id == 2010 }.notification
            assertEquals(0, first.flags and Notification.FLAG_INSISTENT)
            assertEquals(Notification.CATEGORY_ALARM, first.category)
            assertEquals(Notification.VISIBILITY_PRIVATE, first.visibility)
            // Notification delivery precedes the receiver's durable transition. Wait for that
            // public state before advancing the next synthetic trigger.
            compose.waitUntil(10_000) { runBlocking {
                database.dao().getSleepRoutine()?.let { it.routineDay.isNotEmpty() && it.nextAt > System.currentTimeMillis() } == true
            } }
            runBlocking {
                val active = database.dao().getSleepRoutine()!!
                database.dao().saveSleepRoutine(active.copy(nextAt = System.currentTimeMillis() - 1000))
            }
            context.sendBroadcast(Intent(context, SleepRoutineReceiver::class.java).setAction("com.trainiq.SLEEP_ROUTINE"))
            compose.waitUntil(10_000) { manager.activeNotifications.any {
                it.id == 2010 && it.notification.extras.getString(Notification.EXTRA_TITLE) == "Slaapalarm: bevestiging nodig"
            } }
            val escalated = manager.activeNotifications.single { it.id == 2010 }.notification
            assertEquals(0, escalated.flags and Notification.FLAG_INSISTENT)
            assertEquals(Notification.CATEGORY_ALARM, escalated.category)
            escalated.contentIntent.send()
            compose.waitUntil(10_000) {
                compose.onAllNodesWithText("Slaapvoorbereiding")
                    .fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty()
            }
            compose.onNodeWithText("Slaapvoorbereiding").assertExists()
            compose.waitUntil(10_000) { compose.onAllNodesWithText("Ik ga binnen 2 minuten slapen").fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty() }
            compose.onNodeWithText("Ik ga binnen 2 minuten slapen").performScrollTo()
            captureSleepEvidence("sleep-active")
            compose.onNodeWithText("Ik ga binnen 2 minuten slapen").performClick()
            compose.waitUntil(10_000) { manager.activeNotifications.any { it.id == 2010 && it.notification.extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER) } }
            val countdown = manager.activeNotifications.single { it.id == 2010 }.notification
            assertEquals(0, countdown.flags and Notification.FLAG_INSISTENT)
            assertTrue(countdown.extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER))
            assertTrue(countdown.timeoutAfter in 1..120_000)
            compose.waitUntil(10_000) { compose.onAllNodesWithText("Bevestigd: nog", substring = true).fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("Bevestigd: nog", substring = true).performScrollTo().assertIsDisplayed()
            compose.waitForIdle()
            captureSleepEvidence("sleep-countdown")
            runBlocking {
                val saved = database.dao().getSleepRoutine()!!
                assertTrue(saved.confirmedAt > 0)
                database.dao().saveSleepRoutine(saved.copy(confirmedAt = System.currentTimeMillis() - 120_001))
                repository.reconcile()
                assertEquals("", database.dao().getSleepRoutine()!!.routineDay)
            }
            androidx.test.espresso.Espresso.pressBack()
        } finally {
            runBlocking { repository.clear() }
            scheduler.cancelNotification()
        }
    }
}

private fun captureSleepEvidence(name: String) {
    val command = InstrumentationRegistry.getInstrumentation().uiAutomation
        .executeShellCommand("screencap -p /data/local/tmp/trainiq-$name.png")
    android.os.ParcelFileDescriptor.AutoCloseInputStream(command).use { it.readBytes() }
}
