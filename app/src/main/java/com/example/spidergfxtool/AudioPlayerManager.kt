package com.example.spidergfxtool

import android.content.Context
import android.media.MediaPlayer
import android.util.Log

object AudioPlayerManager {
    private var mediaPlayer: MediaPlayer? = null

    fun playIntroMusic(context: Context) {
        if (mediaPlayer == null) {
            try {
                mediaPlayer = MediaPlayer.create(context, R.raw.spider_intro).apply {
                    isLooping = false
                    start()
                }
            } catch (e: Exception) {
                Log.e("AudioPlayerManager", "Error playing music: ${e.message}")
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
