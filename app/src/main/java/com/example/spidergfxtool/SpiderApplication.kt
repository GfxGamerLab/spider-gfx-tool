package com.example.spidergfxtool

import android.app.Application
import android.util.Log

class SpiderApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Global Uncaught Exception Handler to prevent process death / crash popups
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("SpiderApplication", "Uncaught exception on thread ${thread.name}", throwable)
            try {
                // Emergency hardware state reset if Shizuku is available
                GfxBackendManager.globalReset()
            } catch (_: Throwable) {}

            try {
                defaultHandler?.uncaughtException(thread, throwable)
            } catch (_: Throwable) {
                try {
                    android.os.Process.killProcess(android.os.Process.myPid())
                    System.exit(10)
                } catch (_: Throwable) {}
            }
        }

        try {
            AdsManager.init(this)
        } catch (e: Throwable) {
            Log.e("SpiderApplication", "Ads initialization error", e)
        }

        try {
            SoundEffectsManager.init(this)
        } catch (e: Throwable) {
            Log.e("SpiderApplication", "SoundEffects initialization error", e)
        }

        try {
            GfxBackendManager.initShizukuListener(this)
        } catch (e: Throwable) {
            Log.e("SpiderApplication", "Shizuku listener initialization error", e)
        }
    }
}
