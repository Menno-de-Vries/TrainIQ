package com.trainiq.core.sleep

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.ServiceCompat
import com.trainiq.domain.sleep.SleepRepeatMillis
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/** Owns a single alarm stream; the notification is silent while this service owns playback. */
@AndroidEntryPoint
class SleepAlarmPlaybackService : Service() {
    companion object {
        // Accessed only on the main looper, and registered only after foreground promotion.
        private var active: SleepAlarmPlaybackService? = null
        @androidx.annotation.MainThread
        internal fun cancelActivePlayback(revision: Long) {
            active?.takeIf { it.scheduler.playbackRevision == revision }?.finishPlayback()
        }
    }
    @Inject lateinit var scheduler: SleepRoutineScheduler
    private var player: MediaPlayer? = null
    private var focus: AudioFocusRequest? = null
    private val handler = Handler(Looper.getMainLooper())
    private val expire = Runnable { finishPlayback() }
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.getLongExtra("revision", -1) != scheduler.playbackRevision || !scheduler.notificationsAllowed()) {
            // stopSelf alone can leave a queued startForegroundService deadline pending,
            // particularly when confirmation races another start on an existing service.
            ServiceCompat.startForeground(this, SleepCancelledPlaybackNotificationId,
                scheduler.cancelledPlaybackNotification(),
                if (Build.VERSION.SDK_INT >= 29) ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK else 0)
            stopForeground(STOP_FOREGROUND_REMOVE)
            finishPlayback(startId)
            return START_NOT_STICKY
        }
        ServiceCompat.startForeground(this, SleepNotificationId,
            scheduler.reminderNotification(intent.getBooleanExtra("escalated", false), silent = true),
            if (Build.VERSION.SDK_INT >= 29) ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK else 0)
        active = this
        // Cancellation may have arrived from a worker during the foreground handshake.
        if (intent.getLongExtra("revision", -1) != scheduler.playbackRevision) {
            finishPlayback(startId)
            return START_NOT_STICKY
        }
        releaseAudio()
        if (!scheduler.soundEnabled()) { finishPlayback(); return START_NOT_STICKY }
        val sound = scheduler.alarmSound() ?: run { finishPlayback(); return START_NOT_STICKY }
        val attributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
        val audio = getSystemService(AudioManager::class.java)
        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            .setAudioAttributes(attributes).setOnAudioFocusChangeListener { change ->
                if (change == AudioManager.AUDIOFOCUS_LOSS || change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) finishPlayback()
            }.build()
        focus = request
        if (audio.requestAudioFocus(request) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            finishPlayback(); return START_NOT_STICKY
        }
        try {
            player = MediaPlayer()
            player?.apply {
                setWakeMode(applicationContext, android.os.PowerManager.PARTIAL_WAKE_LOCK)
                setAudioAttributes(attributes)
                setDataSource(this@SleepAlarmPlaybackService, sound)
                isLooping = true
                setOnPreparedListener { if (player === it) it.start() }
                setOnErrorListener { _, _, _ -> finishPlayback(); true }
                prepareAsync()
            }
            handler.removeCallbacks(expire)
            handler.postDelayed(expire, SleepRepeatMillis)
        } catch (_: Exception) { finishPlayback() }
        return START_NOT_STICKY
    }

    private fun finishPlayback(startId: Int? = null) {
        if (active === this) active = null
        handler.removeCallbacks(expire)
        releaseAudio()
        // Detach before a queued countdown replaces the same notification ID. Letting
        // Android stop the foreground service first can remove that newer notification.
        stopForeground(STOP_FOREGROUND_DETACH)
        if (startId == null) stopSelf() else stopSelf(startId)
    }

    private fun releaseAudio() {
        player?.release()
        player = null
        focus?.let { getSystemService(AudioManager::class.java).abandonAudioFocusRequest(it) }
        focus = null
    }
    override fun onDestroy() {
        if (active === this) active = null
        handler.removeCallbacks(expire)
        releaseAudio()
        stopForeground(STOP_FOREGROUND_DETACH)
        super.onDestroy()
    }
}
