package com.example.spidergfxtool

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Watchdog Receiver: Ensures battery state is reset when charging
 * to prevent IC freezing and percentage stuck issues.
 */
class BatterySafetyReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val prefs = context.applicationContext.getSharedPreferences("spider_gfx_prefs", Context.MODE_PRIVATE)
        val scope = CoroutineScope(Dispatchers.IO)

        scope.launch {
            try {
                when (intent.action) {
                    Intent.ACTION_POWER_CONNECTED -> {
                        GfxBackendManager.runShell(null, "dumpsys battery reset")
                    }
                    Intent.ACTION_POWER_DISCONNECTED -> {
                        val isFakeTempEnabled = prefs.getBoolean("key_fake_temp", false)
                        if (isFakeTempEnabled) {
                            GfxBackendManager.runShell(null, "dumpsys battery set temp 250")
                        }
                    }
                }
            } catch (_: Throwable) {
            } finally {
                try { pendingResult.finish() } catch (_: Throwable) {}
            }
        }
    }
}
