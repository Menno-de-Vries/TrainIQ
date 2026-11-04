package com.trainiq.core.workout

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.trainiq.MainActivity
import com.trainiq.R
import com.trainiq.core.datastore.UserPreferencesRepository
import com.trainiq.domain.workout.*
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

const val RestChannelId = "trainiq_workout_rest"
internal const val RestNotificationId = 2020

data class WorkoutRestNotificationStatus(val appAllowed: Boolean, val channelAllowed: Boolean,
    val channelSound: Boolean, val systemSound: Boolean) {
    fun label(soundPreference: Boolean) = when {
        !appAllowed -> "Meldingen niet toegestaan"
        !channelAllowed -> "Rustmeldingen geblokkeerd"
        !soundPreference || !channelSound || !systemSound -> "Stille rustmelding"
        else -> "Geluid en melding aan"
    }
}

@Singleton
class WorkoutRestTimerNotifications @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val preferences: UserPreferencesRepository,
) {
    private val manager get() = context.getSystemService(NotificationManager::class.java)
    private var postedIdentity: WorkoutRestTimerIdentity? = null
    fun status(): WorkoutRestNotificationStatus {
        ensureChannel()
        val channel = manager.getNotificationChannel(RestChannelId)
        val audio = context.getSystemService(AudioManager::class.java)
        val appAllowed = (Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context,
            Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) &&
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        return WorkoutRestNotificationStatus(appAllowed,
            channel.importance != NotificationManager.IMPORTANCE_NONE,
            channel.importance >= NotificationManager.IMPORTANCE_DEFAULT && channel.sound != null,
            audio.ringerMode == AudioManager.RINGER_MODE_NORMAL && audio.getStreamVolume(AudioManager.STREAM_NOTIFICATION) > 0 &&
                manager.currentInterruptionFilter == NotificationManager.INTERRUPTION_FILTER_ALL)
    }

    @SuppressLint("MissingPermission")
    suspend fun show(timer: ScheduledWorkoutRestTimer, silent: Boolean): Boolean {
        val state = status()
        if (!state.appAllowed || !state.channelAllowed) return false
        val soundEnabled = preferences.workoutFeedbackPreferences.first().restTimerSoundEnabled
        val expired = silent || android.os.SystemClock.elapsedRealtime() - timer.elapsedDeadline > 30_000
        val muted = expired || !soundEnabled
        postedIdentity = timer.identity
        manager.notify(RestNotificationId, notification(timer, muted, expired = expired))
        return true
    }

    internal fun notification(timer: ScheduledWorkoutRestTimer, silent: Boolean, expired: Boolean = false): Notification {
        ensureChannel()
        val open = PendingIntent.getActivity(context, RestNotificationId,
            Intent(context, MainActivity::class.java).setAction("com.trainiq.OPEN_WORKOUT_REST")
                .setData(Uri.parse("trainiq-rest-open://${timer.identity.sessionId}/${timer.identity.generation}"))
                .putExtra(RestSessionExtra, timer.identity.sessionId).putExtra(RestGenerationExtra, timer.identity.generation)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(context, RestChannelId).setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(if (expired) "Rusttijd verstreken" else "Rust voorbij")
            .setContentText("Klaar voor je volgende set").setContentIntent(open)
            .addAction(0, "Verder trainen", open).setAutoCancel(true).setOnlyAlertOnce(true)
            .setSilent(silent).setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(NotificationCompat.Builder(context, RestChannelId).setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("TrainIQ").setContentText("Open TrainIQ om verder te gaan.").setSilent(true).build())
            .addExtras(android.os.Bundle().apply {
                putLong(RestSessionExtra, timer.identity.sessionId); putString(RestGenerationExtra, timer.identity.generation)
            }).build()
    }

    fun cancel(identity: WorkoutRestTimerIdentity) {
        if (postedIdentity == identity || manager.activeNotifications.any { it.id == RestNotificationId &&
            it.notification.extras.getLong(RestSessionExtra) == identity.sessionId &&
            it.notification.extras.getString(RestGenerationExtra) == identity.generation }) {
            manager.cancel(RestNotificationId)
            if (postedIdentity == identity) postedIdentity = null
        }
    }

    private fun ensureChannel() {
        if (manager.getNotificationChannel(RestChannelId) != null) return
        manager.createNotificationChannel(NotificationChannel(RestChannelId, "Rusttimer", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "Een korte melding wanneer je rust tussen sets voorbij is."
            setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION), AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
            enableVibration(false)
            lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        })
    }
}
