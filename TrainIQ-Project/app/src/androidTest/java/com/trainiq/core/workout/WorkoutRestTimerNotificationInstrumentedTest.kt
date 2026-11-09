package com.trainiq.core.workout

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.trainiq.core.datastore.UserPreferencesRepository
import com.trainiq.domain.workout.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test

/** Uses the isolated agent profile. Never grants permissions or changes ringer, DND or channel choices. */
class WorkoutRestTimerNotificationInstrumentedTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val notifications = WorkoutRestTimerNotifications(context, UserPreferencesRepository(context))
    private val clock = AndroidWorkoutRestTimerClock(context)
    private fun timer(generation: String) = newWorkoutRestTimer(9001, 9002, 3600, clock.now(), generation)!!

    @Test fun receiverClaimsCurrentRoomGenerationAndDuplicateBroadcastDoesNotRearm() = runBlocking {
        val database = com.trainiq.testing.resetTrainIqAndroidTestDatabase(context)
        val dao = database.dao()
        dao.insertRoutines(listOf(com.trainiq.core.database.WorkoutRoutineEntity(9000, "Rest receiver", "", true)))
        dao.insertWorkoutDays(listOf(com.trainiq.core.database.WorkoutDayEntity(9002, 9000, "Day", 0)))
        val now = clock.now()
        val timer = timer("receiver")
        dao.insertActiveWorkoutSessions(listOf(com.trainiq.core.database.ActiveWorkoutSessionEntity(
            sessionId = 9001, dayId = 9002, routineId = 9000, startedAt = now.wallMillis, updatedAt = now.wallMillis,
            restTimerEndsAt = now.wallMillis - 1, restTimerTotalSeconds = 1, restTimerGeneration = timer.identity.generation,
            restTimerElapsedDeadline = now.elapsedMillis - 1, restTimerBootId = now.bootId)))
        val intent = Intent(context, WorkoutRestTimerReceiver::class.java).setAction(RestTimerAction)
            .putExtra(RestSessionExtra, 9001L).putExtra(RestGenerationExtra, "receiver")
        try {
            context.sendBroadcast(intent)
            withTimeout(10_000) { while (dao.readActiveWorkoutSessionsForExport().single().restTimerHandled.not()) delay(20) }
            context.sendBroadcast(intent)
            val coordinator = dagger.hilt.android.EntryPointAccessors.fromApplication(context, WorkoutRestTimerEntryPoint::class.java).restTimerCoordinator()
            assertEquals(RestTimerCompletion.HANDLED, coordinator.completeIfCurrent(timer.identity))
            assertEquals(0, dao.readActiveWorkoutSessionsForExport().single().restTimerTotalSeconds)
        } finally { notifications.cancel(timer.identity); dao.discardActiveWorkoutSession(9001) }
    }

    @Test fun notificationIsPrivateDismissibleSingleCueAndPerNotificationMutePreservesChannel() {
        val manager = context.getSystemService(NotificationManager::class.java)
        notifications.status()
        val before = manager.getNotificationChannel(RestChannelId)
        val sound = before.sound
        val importance = before.importance
        val normal = notifications.notification(timer("normal"), false)
        val muted = notifications.notification(timer("muted"), true)
        assertEquals(Notification.VISIBILITY_PRIVATE, normal.visibility)
        assertNotNull(normal.publicVersion)
        assertNull(normal.fullScreenIntent)
        assertEquals(0, normal.flags and (Notification.FLAG_INSISTENT or Notification.FLAG_ONGOING_EVENT or Notification.FLAG_FOREGROUND_SERVICE))
        assertTrue(normal.flags and Notification.FLAG_AUTO_CANCEL != 0)
        assertEquals("Verder trainen", normal.actions.single().title.toString())
        assertEquals(Notification.GROUP_ALERT_SUMMARY, muted.groupAlertBehavior)
        assertEquals("Rust voorbij", muted.extras.getString(Notification.EXTRA_TITLE))
        assertEquals("Rusttijd verstreken", notifications.notification(timer("stale"), true, expired = true).extras.getString(Notification.EXTRA_TITLE))
        val after = manager.getNotificationChannel(RestChannelId)
        assertEquals(sound, after.sound); assertEquals(importance, after.importance)
        assertEquals(AudioAttributes.USAGE_NOTIFICATION, after.audioAttributes.usage)
    }

    @Test fun elapsedAlarmReplacementAndGenerationCancellationDoNotTouchSleep() {
        val scheduler = WorkoutRestTimerScheduler(context, notifications)
        val first = timer("adapter-one"); val second = timer("adapter-two")
        try {
            scheduler.schedule(first); scheduler.schedule(first)
            assertEquals(1, pendingRestAlarms())
            scheduler.cancel(first.identity)
            scheduler.schedule(second)
            scheduler.cancel(first.identity)
            assertEquals(1, pendingRestAlarms())
        } finally { scheduler.cancel(first.identity); scheduler.cancel(second.identity) }
        assertEquals(0, pendingRestAlarms())
    }

    @Test fun blockedAppOrChannelCannotPostAndAllowedNotificationIsOrdinary() = runBlocking {
        val timer = timer("delivery")
        val status = notifications.status()
        try {
            val posted = notifications.show(timer, silent = true)
            assertEquals(status.appAllowed && status.channelAllowed, posted)
            if (posted) {
                val manager = context.getSystemService(NotificationManager::class.java)
                val current = manager.activeNotifications.single { it.id == RestNotificationId }.notification
                assertEquals(0, current.flags and Notification.FLAG_INSISTENT)
                assertEquals(RestChannelId, current.channelId)
            }
        } finally { notifications.cancel(timer.identity) }
    }

    private fun pendingRestAlarms(): Int {
        val descriptor = InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand("dumpsys alarm")
        val dump = android.os.ParcelFileDescriptor.AutoCloseInputStream(descriptor).use { it.readBytes().decodeToString() }
        // Count PendingIntent operations in pending alarm records, not historical delivery stats.
        return Regex("ELAPSED_WAKEUP #\\d+: Alarm\\{[^\\n]* com\\.trainiq\\}").findAll(dump).count()
    }
}
