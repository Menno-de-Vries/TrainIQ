package com.trainiq.core.audio

import android.media.AudioManager
import android.media.ToneGenerator
import javax.inject.Inject

class RestTimerSoundPlayer @Inject constructor() {
    private var toneGenerator: ToneGenerator? = null

    fun play() {
        runCatching {
            val player = toneGenerator ?: ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80).also {
                toneGenerator = it
            }
            player.startTone(ToneGenerator.TONE_PROP_ACK, 180)
        }
    }

    fun release() {
        runCatching {
            toneGenerator?.release()
        }
        toneGenerator = null
    }
}
