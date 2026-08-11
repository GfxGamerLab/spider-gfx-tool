package com.example.spidergfxtool

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

object SoundEffectsManager {
    private var soundPool: SoundPool? = null
    private var soundOnId: Int = -1
    private var soundOffId: Int = -1

    var isFeedbackEnabled: Boolean = true

    fun init(context: Context) {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(5)
            .setAudioAttributes(audioAttributes)
            .build()

        // Load sounds - Resource names are switch_on and switch_off
        soundOnId = soundPool?.load(context, R.raw.switch_on, 1) ?: -1
        soundOffId = soundPool?.load(context, R.raw.switch_off, 1) ?: -1
    }

    fun playOn() {
        if (isFeedbackEnabled && soundOnId != -1) {
            soundPool?.play(soundOnId, 1.0f, 1.0f, 1, 0, 1.0f)
        }
    }

    fun playOff() {
        if (isFeedbackEnabled && soundOffId != -1) {
            soundPool?.play(soundOffId, 1.0f, 1.0f, 1, 0, 1.0f)
        }
    }

    fun performHaptic(haptic: HapticFeedback) {
        if (isFeedbackEnabled) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    fun release() {
        soundPool?.release()
        soundPool = null
        soundOnId = -1
        soundOffId = -1
    }
}
