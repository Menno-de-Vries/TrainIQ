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
import com.trainiq.R
import com.trainiq.domain.sleep.SleepCountdownMillis
import com.trainiq.domain.sleep.SleepRoutine
import com.trainiq.features.sleep.SleepRoutineActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import javax.inject.Inject
import javax.inject.Singleton

const val SleepChannelId = "trainiq_sleep_preparation"
private const val SleepNotificationId = 2010

@Singleton
class SleepRoutineScheduler @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val workManager: WorkManager,
) {
    private val alarms get() = context.getSystemService(AlarmManager::class.java)
    private val notifications get() = context.getSystemService(NotificationManager::class.java)
    fun exactAllowed() = Build.VERSION.SDK_INT < 31 || alarms.canScheduleExactAlarms()

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
    fun schedule(state: SleepRoutine) {
        val pending = alarmIntent()
        alarms.cancel(pending)
        if (!state.enabled) {
            workManager.cancelUniqueWork("trainiq_sleep_recovery")
            return
        }
        // A durable fallback also repairs alarms removed when exact-alarm access is revoked.
        workManager.enqueueUniquePeriodicWork("trainiq_sleep_recovery", ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<SleepRoutineWorker>(Duration.ofMinutes(15)).build())
        if (state.nextAt <= 0) return
        try {
            if (exactAllowed()) alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, state.nextAt, pending)
            else alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, state.nextAt, pending)
        } catch (_: SecurityException) {
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, state.nextAt, pending)
        }
    }

    @SuppressLint("MissingPermission")
    fun showReminder(escalated: Boolean) {
        if (!notificationsAllowed()) return
        val notification = builder()
            .setContentTitle("Tijd om je klaar te maken om te slapen")
            .setContentText("Open de slaaproutine en bevestig bewust dat je binnen 2 minuten gaat slapen.")
            .setStyle(NotificationCompat.BigTextStyle().bigText("Open de slaaproutine en bevestig bewust dat je binnen 2 minuten gaat slapen. Zonder bevestiging wordt de melding ongeveer elke 15 minuten herhaald; Android kan dit vertragen."))
            .setOngoing(true)
            .addAction(0, "Slaaproutine openen", screenIntent())
            .build().apply { if (escalated) flags = flags or Notification.FLAG_INSISTENT }
        notifications.notify(SleepNotificationId, notification)
    }

    @SuppressLint("MissingPermission")
    fun showCountdown(state: SleepRoutine) {
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

    fun cancelNotification() = notifications.cancel(SleepNotificationId)

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
        val sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        notifications.createNotificationChannel(NotificationChannel(SleepChannelId,
            "Slaapvoorbereiding", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Dagelijkse slaapvoorbereiding en herhaling tot bewuste bevestiging."
            setSound(sound, AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT).build())
            enableVibration(true)
            lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        })
    }
}
