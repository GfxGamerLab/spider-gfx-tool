package com.example.spidergfxtool

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Watchdog Receiver: Ensures battery state is reset when charging
 * to prevent IC freezing and percentage stuck issues.
 */
class BatterySafetyReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val prefs = context.getSharedPreferences("spider_gfx_prefs", Context.MODE_PRIVATE)
        val scope = CoroutineScope(Dispatchers.IO)

        when (intent.action) {
            Intent.ACTION_POWER_CONNECTED -> {
                scope.launch {
                    // IMMEDIATELY reset battery state for safe charging
                    GfxBackendManager.runShell(null, "dumpsys battery reset")
                    launch(Dispatchers.Main) {
                        Toast.makeText(context, "Safety: Real Battery State Restored for Charging", Toast.LENGTH_LONG).show()
                    }
                }
            }
            Intent.ACTION_POWER_DISCONNECTED -> {
                val isFakeTempEnabled = prefs.getBoolean("key_fake_temp", false)
                if (isFakeTempEnabled) {
                    scope.launch {
                        // Re-apply fake temp tweak only after unplugging
                        GfxBackendManager.runShell(null, "dumpsys battery set temp 250")
                    }
                }
            }
        }
    }
}
