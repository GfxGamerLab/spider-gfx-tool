package com.example.spidergfxtool

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import kotlin.math.pow
import kotlin.math.sqrt

class CrosshairService : Service() {

    private lateinit var windowManager: WindowManager
    private var crosshairView: ImageView? = null
    private var trashView: ImageView? = null
    
    private val trashParams = WindowManager.LayoutParams(
        160, 160,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        y = 100
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        OverlayStateManager.setCrosshairActive(true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !android.provider.Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }

        showCrosshair()
        setupTrashView()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun showCrosshair() {
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        crosshairView = ImageView(this).apply {
            setImageResource(R.drawable.ic_crosshair_v7)
            setOnTouchListener(object : View.OnTouchListener {
                private var initialX: Int = 0
                private var initialY: Int = 0
                private var initialTouchX: Float = 0f
                private var initialTouchY: Float = 0f

                override fun onTouch(v: View, event: MotionEvent): Boolean {
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            initialX = params.x
                            initialY = params.y
                            initialTouchX = event.rawX
                            initialTouchY = event.rawY
                            trashView?.visibility = View.VISIBLE
                            return true
                        }
                        MotionEvent.ACTION_MOVE -> {
                            params.x = initialX + (event.rawX - initialTouchX).toInt()
                            params.y = initialY + (event.rawY - initialTouchY).toInt()
                            try { windowManager.updateViewLayout(crosshairView, params) } catch (_: Throwable) {}
                            
                            updateTrashAnimation(event.rawX, event.rawY)
                            return true
                        }
                        MotionEvent.ACTION_UP -> {
                            if (isOverTrash(event.rawX, event.rawY)) {
                                stopSelf()
                            } else {
                                trashView?.visibility = View.GONE
                            }
                            return true
                        }
                    }
                    return false
                }
            })
        }
        try { windowManager.addView(crosshairView, params) } catch (_: Throwable) { stopSelf() }
    }

    private fun setupTrashView() {
        trashView = ImageView(this).apply {
            setImageResource(R.drawable.ic_trash_zone)
            visibility = View.GONE
        }
        try { windowManager.addView(trashView, trashParams) } catch (_: Throwable) {}
    }

    private fun updateTrashAnimation(rawX: Float, rawY: Float) {
        if (isOverTrash(rawX, rawY)) {
            trashView?.animate()?.scaleX(1.5f)?.scaleY(1.5f)?.setDuration(100)?.start()
        } else {
            trashView?.animate()?.scaleX(1.0f)?.scaleY(1.0f)?.setDuration(100)?.start()
        }
    }

    private fun isOverTrash(rawX: Float, rawY: Float): Boolean {
        val screenWidth = resources.displayMetrics.widthPixels
        val screenHeight = resources.displayMetrics.heightPixels
        val trashCenterX = screenWidth / 2f
        val trashCenterY = screenHeight - 100f - 80f // y offset + half height
        
        val distance = sqrt((rawX - trashCenterX).pow(2) + (rawY - trashCenterY).pow(2))
        return distance < 150f
    }

    override fun onDestroy() {
        OverlayStateManager.setCrosshairActive(false)
        crosshairView?.let { try { windowManager.removeView(it) } catch (_: Throwable) {} }
        trashView?.let { try { windowManager.removeView(it) } catch (_: Throwable) {} }
        super.onDestroy()
    }
}
