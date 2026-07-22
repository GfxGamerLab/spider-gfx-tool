package com.example.spidergfxtool

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object AudioPlayerManager {
    private var mediaPlayer: MediaPlayer? = null

    fun playIntroMusic(context: Context) {
        if (mediaPlayer == null) {
            try {
                mediaPlayer = MediaPlayer.create(context, R.raw.spider_intro).apply {
                    isLooping = false
                    setVolume(1.0f, 1.0f)
                    start()
                }
            } catch (e: Exception) {
                Log.e("AudioPlayerManager", "Error playing music: ${e.message}")
            }
        }
    }

    fun fadeOutAndStop(durationMs: Long = 1000L) {
        val player = mediaPlayer ?: return
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val steps = 20
                val interval = durationMs / steps
                for (i in steps downTo 0) {
                    val volume = i.toFloat() / steps
                    if (player.isPlaying) {
                        player.setVolume(volume, volume)
                    }
                    delay(interval)
                }
                stopAndRelease()
            } catch (e: Exception) {
                stopAndRelease()
            }
        }
    }

    fun stopAndRelease() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            }
        } catch (e: Exception) {
            Log.e("AudioPlayerManager", "Error releasing player: ${e.message}")
        } finally {
            mediaPlayer = null
        }
    }
}
