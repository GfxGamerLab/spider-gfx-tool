package com.example.spidergfxtool

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import kotlinx.coroutines.*
import java.io.RandomAccessFile

class HudOverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var serviceJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())

    // --- Watchdog Receiver for Battery Freeze Fix ---
    private val batteryWatchdog = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val prefs = context.getSharedPreferences("spider_gfx_prefs", Context.MODE_PRIVATE)
            when (intent.action) {
                Intent.ACTION_POWER_CONNECTED -> {
                    serviceScope.launch(Dispatchers.IO) {
                        GfxBackendManager.runShell(null, "dumpsys battery reset")
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Safety: Real Battery State Restored for Charging", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                Intent.ACTION_POWER_DISCONNECTED -> {
                    if (prefs.getBoolean("key_fake_temp", false)) {
                        serviceScope.launch(Dispatchers.IO) {
                            GfxBackendManager.runShell(null, "dumpsys battery set temp 250")
                        }
                    }
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        
        // Register Watchdog Dynamically (Bypasses background restrictions)
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
        }
        registerReceiver(batteryWatchdog, filter)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 100
        }

        overlayView = LayoutInflater.from(this).inflate(R.layout.layout_hud_overlay, null)
        windowManager?.addView(overlayView, params)

        startMonitoring()
    }

    private fun startMonitoring() {
        serviceJob = serviceScope.launch {
            val fpsText = overlayView?.findViewById<TextView>(R.id.fpsText)
            val cpuText = overlayView?.findViewById<TextView>(R.id.cpuText)

            while (isActive) {
                fpsText?.text = "FPS: ${60}" 
                cpuText?.text = "CPU: ${getCPUTemp()}°C"
                delay(1000)
            }
        }
    }

    private fun getCPUTemp(): String {
        return try {
            val reader = RandomAccessFile("/sys/class/thermal/thermal_zone0/temp", "r")
            val temp = reader.readLine().toDouble() / 1000
            reader.close()
            String.format("%.1f", temp)
        } catch (e: Exception) {
            "N/A"
        }
    }

    override fun onDestroy() {
        // Force Battery Reset on Shutdown
        serviceScope.launch(Dispatchers.IO) {
            GfxBackendManager.runShell(null, "dumpsys battery reset")
        }

        try {
            unregisterReceiver(batteryWatchdog)
        } catch (e: Exception) { }

        serviceJob?.cancel()
        overlayView?.let { windowManager?.removeView(it) }
        super.onDestroy()
    }
}
