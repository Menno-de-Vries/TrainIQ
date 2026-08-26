package com.trainiq.core.reminders

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.trainiq.MainActivity
import com.trainiq.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrainIqReminderNotifications @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    fun canPostNotifications(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    fun show(content: ReminderContent) {
        if (!canPostNotifications()) return
        ensureChannel()
        val notification = NotificationCompat.Builder(context, TrainIqReminderChannelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(content.title)
            .setContentText(content.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content.body))
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(publicReminderNotification(content))
            .setContentIntent(appPendingIntent())
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setDefaults(NotificationCompat.DEFAULT_VIBRATE)
            .build()
        NotificationManagerCompat.from(context).notify(content.type.notificationId, notification)
    }

    private fun publicReminderNotification(content: ReminderContent) =
        NotificationCompat.Builder(context, TrainIqReminderChannelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(publicReminderTitle(content.type))
            .setContentText("Open TrainIQ voor je reminder.")
            .setContentIntent(appPendingIntent())
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            TrainIqReminderChannelId,
            "TrainIQ reminders",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Opt-in reminders voor voeding en krachttraining."
            enableVibration(true)
            vibrationPattern = longArrayOf(0L, 120L, 80L, 120L)
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun appPendingIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}

private const val TrainIqReminderChannelId = "trainiq_reminders"
private fun publicReminderTitle(type: ReminderType): String = when (type) {
    ReminderType.MEAL -> "TrainIQ reminder"
    ReminderType.WORKOUT -> "TrainIQ reminder"
}
private val ReminderType.notificationId: Int
    get() = when (this) {
        ReminderType.MEAL -> 2001
        ReminderType.WORKOUT -> 2002
    }
