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
    @Inject lateinit var scheduler: SleepRoutineScheduler
    private var player: MediaPlayer? = null
    private var focus: AudioFocusRequest? = null
    private val handler = Handler(Looper.getMainLooper())
    private val expire = Runnable { stopSelf() }
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.getLongExtra("revision", -1) != scheduler.playbackRevision || !scheduler.notificationsAllowed()) {
            stopSelf()
            return START_NOT_STICKY
        }
        ServiceCompat.startForeground(this, SleepNotificationId,
            scheduler.reminderNotification(intent.getBooleanExtra("escalated", false), silent = true),
            if (Build.VERSION.SDK_INT >= 29) ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK else 0)
        releaseAudio()
        if (!scheduler.soundEnabled()) { stopSelf(); return START_NOT_STICKY }
        val sound = scheduler.alarmSound() ?: run { stopSelf(); return START_NOT_STICKY }
        val attributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
        val audio = getSystemService(AudioManager::class.java)
        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            .setAudioAttributes(attributes).setOnAudioFocusChangeListener { change ->
                if (change == AudioManager.AUDIOFOCUS_LOSS || change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) stopSelf()
            }.build()
        focus = request
        if (audio.requestAudioFocus(request) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            stopSelf(); return START_NOT_STICKY
        }
        try {
            player = MediaPlayer()
            player?.apply {
                setWakeMode(applicationContext, android.os.PowerManager.PARTIAL_WAKE_LOCK)
                setAudioAttributes(attributes)
                setDataSource(this@SleepAlarmPlaybackService, sound)
                isLooping = true
                setOnPreparedListener { if (player === it) it.start() }
                setOnErrorListener { _, _, _ -> stopSelf(); true }
                prepareAsync()
            }
            handler.removeCallbacks(expire)
            handler.postDelayed(expire, SleepRepeatMillis)
        } catch (_: Exception) { stopSelf() }
        return START_NOT_STICKY
    }

    private fun releaseAudio() {
        player?.release()
        player = null
        focus?.let { getSystemService(AudioManager::class.java).abandonAudioFocusRequest(it) }
        focus = null
    }
    override fun onDestroy() {
        handler.removeCallbacks(expire)
        releaseAudio()
        stopForeground(STOP_FOREGROUND_DETACH)
        super.onDestroy()
    }
}
