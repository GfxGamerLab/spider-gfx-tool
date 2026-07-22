package com.example.spidergfxtool

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import kotlinx.coroutines.*
import java.io.RandomAccessFile

class HudOverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var serviceJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
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
        serviceJob = CoroutineScope(Dispatchers.Main).launch {
            val fpsText = overlayView?.findViewById<TextView>(R.id.fpsText)
            val cpuText = overlayView?.findViewById<TextView>(R.id.cpuText)

            while (isActive) {
                fpsText?.text = "FPS: ${60}" // Mock FPS as real-time FPS requires surface callbacks
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
        super.onDestroy()
        serviceJob?.cancel()
        overlayView?.let { windowManager?.removeView(it) }
    }
}
