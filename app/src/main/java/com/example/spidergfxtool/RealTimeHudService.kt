package com.example.spidergfxtool

import android.annotation.SuppressLint
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.view.Choreographer
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import androidx.compose.runtime.*
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.*
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.spidergfxtool.ui.components.HudOverlayUI
import kotlinx.coroutines.*
import java.io.RandomAccessFile
import kotlin.math.pow
import kotlin.math.sqrt

class RealTimeHudService : Service(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    // --- Lifecycle & State Owners ---
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val store = ViewModelStore()

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    private lateinit var windowManager: WindowManager
    private var hudView: ComposeView? = null
    private var trashView: ImageView? = null

    // --- Telemetry States ---
    private val fpsState = mutableIntStateOf(0)
    private val tempState = mutableFloatStateOf(0f)
    
    private var frameCount = 0
    private var lastCalcTime = 0L

    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            frameCount++
            val currentTime = System.currentTimeMillis()
            if (lastCalcTime == 0L) lastCalcTime = currentTime
            
            if (currentTime - lastCalcTime >= 1000) {
                fpsState.intValue = frameCount
                frameCount = 0
                lastCalcTime = currentTime
            }
            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    private val thermalReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val temp = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1)
            if (temp != -1) {
                tempState.floatValue = temp / 10f
            } else {
                tempState.floatValue = getFallbackCpuTemp()
            }
        }
    }

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
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        OverlayStateManager.setHudActive(true)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !android.provider.Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }

        showHud()
        setupTrashView()
        
        try {
            Choreographer.getInstance().postFrameCallback(frameCallback)
            registerReceiver(thermalReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        } catch (_: Throwable) {}
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun showHud() {
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
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 100
        }

        val uiContext = ContextThemeWrapper(this, android.R.style.Theme_DeviceDefault_NoActionBar)
        hudView = ComposeView(uiContext).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            this.setViewTreeLifecycleOwner(this@RealTimeHudService)
            this.setViewTreeViewModelStoreOwner(this@RealTimeHudService)
            this.setViewTreeSavedStateRegistryOwner(this@RealTimeHudService)
            
            setContent {
                HudOverlayUI(
                    fps = fpsState.intValue,
                    temp = tempState.floatValue
                )
            }
            
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
                            try { windowManager.updateViewLayout(hudView, params) } catch (_: Throwable) {}
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
        try { windowManager.addView(hudView, params) } catch (_: Throwable) { stopSelf() }
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
        val trashCenterY = screenHeight - 100f - 80f
        val distance = sqrt((rawX - trashCenterX).pow(2) + (rawY - trashCenterY).pow(2))
        return distance < 150f
    }

    private fun getFallbackCpuTemp(): Float {
        return try {
            val reader = RandomAccessFile("/sys/class/thermal/thermal_zone0/temp", "r")
            val temp = reader.readLine().toDouble() / 1000
            reader.close()
            temp.toFloat()
        } catch (_: Exception) { 0f }
    }

    override fun onDestroy() {
        OverlayStateManager.setHudActive(false)
        try { Choreographer.getInstance().removeFrameCallback(frameCallback) } catch (_: Throwable) {}
        try { unregisterReceiver(thermalReceiver) } catch (_: Throwable) {}
        hudView?.let { try { windowManager.removeView(it) } catch (_: Throwable) {} }
        trashView?.let { try { windowManager.removeView(it) } catch (_: Throwable) {} }
        try {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            store.clear()
        } catch (_: Throwable) {}
        super.onDestroy()
    }
}
