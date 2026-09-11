package com.trainiq.core.sleep

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.PeriodicWorkRequest
import com.trainiq.R
import com.trainiq.domain.sleep.SleepCountdownMillis
import com.trainiq.domain.sleep.SleepRoutine
import com.trainiq.domain.sleep.SleepRepeatMillis
import com.trainiq.domain.sleep.SleepAlarmDelivery
import com.trainiq.features.sleep.SleepRoutineActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import javax.inject.Inject
import javax.inject.Singleton

const val SleepChannelId = "trainiq_sleep_alarm_v2"
private const val LegacySleepChannelId = "trainiq_sleep_preparation"
internal const val SleepNotificationId = 2010

@Singleton
class SleepRoutineScheduler @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val workManager: WorkManager,
) : SleepAlarmDelivery {
    private val alarms get() = context.getSystemService(AlarmManager::class.java)
    private val notifications get() = context.getSystemService(NotificationManager::class.java)
    fun exactAllowed() = Build.VERSION.SDK_INT < 31 || alarms.canScheduleExactAlarms()
    fun fullScreenAllowed() = Build.VERSION.SDK_INT < 34 || notifications.canUseFullScreenIntent()

    fun notificationsAllowed(): Boolean {
        ensureChannel()
        return (Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context,
            Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) &&
            NotificationManagerCompat.from(context).areNotificationsEnabled() &&
            notifications.getNotificationChannel(SleepChannelId).importance != NotificationManager.IMPORTANCE_NONE
    }

    fun soundEnabled(): Boolean {
        ensureChannel()
        val channel = notifications.getNotificationChannel(SleepChannelId)
        return channel.importance >= NotificationManager.IMPORTANCE_DEFAULT && channel.sound != null
    }

    @SuppressLint("ScheduleExactAlarm")
    override fun schedule(state: SleepRoutine) {
        val pending = alarmIntent()
        if (!state.enabled) {
            alarms.cancel(pending)
            workManager.cancelUniqueWork("trainiq_sleep_recovery")
            return
        }
        // A durable fallback also repairs alarms removed when exact-alarm access is revoked.
        workManager.enqueueUniquePeriodicWork("trainiq_sleep_recovery", ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<SleepRoutineWorker>(Duration.ofMillis(PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS)).build())
        if (state.nextAt <= 0) return
        try {
            // Reusing the PendingIntent atomically replaces the previous alarm. Do not cancel
            // first: a failed replacement must not erase an already scheduled successor.
            if (exactAllowed() && state.confirmedAt == 0L) {
                alarms.setAlarmClock(AlarmManager.AlarmClockInfo(state.nextAt, screenIntent()), pending)
            } else if (exactAllowed()) alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, state.nextAt, pending)
            else alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, state.nextAt, pending)
        } catch (_: SecurityException) {
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, state.nextAt, pending)
        }
    }

    internal val playbackRevision get() = revision.get()
    private companion object { val revision = java.util.concurrent.atomic.AtomicLong(0) }

    @SuppressLint("MissingPermission")
    override fun showReminder(escalated: Boolean) {
        if (!notificationsAllowed()) return
        revision.incrementAndGet()
        try {
            ContextCompat.startForegroundService(context, Intent(context, SleepAlarmPlaybackService::class.java)
                .putExtra("revision", playbackRevision).putExtra("escalated", escalated))
        } catch (_: IllegalStateException) {
            // Recovery workers/inexact deliveries may lack the background-start exemption.
            notifications.notify(SleepNotificationId, reminderNotification(escalated, silent = false))
        } catch (_: SecurityException) {
            notifications.notify(SleepNotificationId, reminderNotification(escalated, silent = false))
        }
    }

    internal fun reminderNotification(escalated: Boolean, silent: Boolean): Notification = builder()
            .setSilent(silent)
            .setContentTitle(if (escalated) "Slaapalarm: bevestiging nodig" else "Slaapalarm: tijd om te gaan slapen")
            .setContentText("Tik hier: Ik ga binnen 2 minuten slapen")
            .setStyle(NotificationCompat.BigTextStyle().bigText("Open en kies ‘Ik ga binnen 2 minuten slapen’. Zonder bevestiging volgt elke ${SleepRepeatMillis / 60_000} minuten een nieuw alarm."))
            .setOngoing(true)
            .addAction(0, "Open slaapbevestiging", screenIntent())
            .apply { if (fullScreenAllowed()) setFullScreenIntent(screenIntent(), true) }
            .build()

    @SuppressLint("MissingPermission")
    override fun showCountdown(state: SleepRoutine) {
        if (!notificationsAllowed()) return
        val end = state.confirmedAt + SleepCountdownMillis
        val remaining = end - System.currentTimeMillis()
        if (remaining <= 0) return
        notifications.notify(SleepNotificationId, builder()
            .setContentTitle("Bevestigd: binnen 2 minuten slapen")
            .setContentText("Je voorbereiding is na de countdown afgehandeld.")
            .setSilent(true).setWhen(end).setUsesChronometer(true).setChronometerCountDown(true)
            .setTimeoutAfter(remaining).build())
    }

    override fun cancelNotification() {
        revision.incrementAndGet()
        context.stopService(Intent(context, SleepAlarmPlaybackService::class.java))
        notifications.cancel(SleepNotificationId)
    }

    internal fun alarmSound() = notifications.getNotificationChannel(SleepChannelId)?.sound

    private fun builder() = NotificationCompat.Builder(context, SleepChannelId)
        .setSmallIcon(R.mipmap.ic_launcher).setContentIntent(screenIntent())
        .setCategory(NotificationCompat.CATEGORY_ALARM).setPriority(NotificationCompat.PRIORITY_HIGH)
        .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
        .setPublicVersion(NotificationCompat.Builder(context, SleepChannelId).setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("TrainIQ herinnering").setContentText("Open TrainIQ om verder te gaan.").build())

    private fun screenIntent() = PendingIntent.getActivity(context, SleepNotificationId,
        Intent(context, SleepRoutineActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    private fun alarmIntent() = PendingIntent.getBroadcast(context, SleepNotificationId,
        Intent(context, SleepRoutineReceiver::class.java).setAction("com.trainiq.SLEEP_ROUTINE"),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    private fun ensureChannel() {
        if (notifications.getNotificationChannel(SleepChannelId) != null) return
        val legacy = notifications.getNotificationChannel(LegacySleepChannelId)
        val sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        notifications.createNotificationChannel(NotificationChannel(SleepChannelId,
            "Slaapalarm", legacy?.importance ?: NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Dagelijkse slaapvoorbereiding en herhaling tot bewuste bevestiging."
            // Preserve an existing mute/custom ringtone; migration must not bypass user choices.
            setSound(if (legacy != null) legacy.sound else sound,
                AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
            enableVibration(legacy?.shouldVibrate() ?: true)
            lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        })
    }
}
