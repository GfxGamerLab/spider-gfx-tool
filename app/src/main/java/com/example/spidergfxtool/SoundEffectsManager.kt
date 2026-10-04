package com.example.spidergfxtool

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.util.Log
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

object SoundEffectsManager {
    private const val TAG = "SoundEffectsManager"
    private var soundPool: SoundPool? = null
    private var soundOnId: Int = -1
    private var soundOffId: Int = -1
    private var soundCorrectId: Int = -1
    private var soundWrongId: Int = -1
    private var activeStreamId: Int = -1

    var isFeedbackEnabled: Boolean = true

    fun init(context: Context) {
        if (soundPool != null) return

        try {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()

            soundPool = SoundPool.Builder()
                .setMaxStreams(6)
                .setAudioAttributes(audioAttributes)
                .build()

            soundOnId = soundPool?.load(context, R.raw.switch_on, 1) ?: -1
            soundOffId = soundPool?.load(context, R.raw.switch_off, 1) ?: -1
            soundCorrectId = soundPool?.load(context, R.raw.correct_key, 1) ?: -1
            soundWrongId = soundPool?.load(context, R.raw.wrong_key, 1) ?: -1
        } catch (e: Exception) {
            Log.e(TAG, "SoundPool init failed: ${e.message}")
        }
    }

    private fun playStream(soundId: Int) {
        if (!isFeedbackEnabled || soundPool == null || soundId == -1) return
        try {
            if (activeStreamId != -1) {
                soundPool?.stop(activeStreamId)
            }
            val streamId = soundPool?.play(soundId, 1.0f, 1.0f, 1, 0, 1.0f) ?: -1
            if (streamId != 0) {
                activeStreamId = streamId
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play sound $soundId: ${e.message}")
        }
    }

    /**
     * Played when a user manually turns a switch ON.
     */
    fun playOn() {
        playStream(soundOnId)
    }

    /**
     * Played when a user manually turns a switch OFF.
     */
    fun playOff() {
        playStream(soundOffId)
    }

    /**
     * Played for standard action button clicks.
     */
    fun playClick() {
        playOn()
    }

    /**
     * Played specifically for successful protocol key activation.
     */
    fun playSuccessKey() {
        playStream(soundCorrectId)
    }

    /**
     * Played ONLY when a feature is unsupported and auto-reverts.
     */
    fun playError() {
        playStream(soundWrongId)
    }

    fun performHaptic(haptic: HapticFeedback) {
        if (isFeedbackEnabled) {
            try {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            } catch (_: Exception) {
            }
        }
    }

    fun release() {
        try {
            if (activeStreamId != -1) {
                soundPool?.stop(activeStreamId)
            }
            soundPool?.release()
        } catch (e: Exception) {
            Log.e(TAG, "SoundPool release error: ${e.message}")
        } finally {
            soundPool = null
            soundOnId = -1
            soundOffId = -1
            soundCorrectId = -1
            soundWrongId = -1
            activeStreamId = -1
        }
    }
}
