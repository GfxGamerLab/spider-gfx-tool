package com.example.spidergfxtool

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.Toast
import androidx.compose.runtime.*
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.app.NotificationCompat
import androidx.lifecycle.*
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.spidergfxtool.ui.components.GamingSpaceOverlay
import kotlinx.coroutines.*

/**
 * Advanced Gaming Space Service hosting Compose inside WindowManager.
 * Implements Lifecycle, ViewModelStore, and SavedStateRegistry owners directly.
 * Includes a dynamic Hardware Safety Watchdog for battery protection.
 */
class GamingSpaceService : Service(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    // --- Lifecycle & State Owners ---
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val store = ViewModelStore()

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    // --- Window Manager & UI ---
    private lateinit var windowManager: WindowManager
    private var rootView: TouchPassthroughLayout? = null
    
    // --- Business Logic ---
    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)
    private val telemetryViewModel = SystemTelemetryViewModel()

    // --- Watchdog Receiver ---
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

    companion object {
        const val ACTION_STOP_SERVICE = "STOP_GAMING_SPACE"
        const val NOTIFICATION_ID = 2001
    }

    class TouchPassthroughLayout(context: Context) : FrameLayout(context) {
        var expanded: Boolean = false

        override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
            val density = resources.displayMetrics.density
            if (!expanded) {
                if (ev.x > 60 * density) return false
            } else {
                val panelWidth = 200 * density
                if (ev.x > panelWidth) return false
            }
            return super.dispatchTouchEvent(ev)
        }
    }

    override fun onCreate() {
        super.onCreate()
        
        // 1. Initialize Lifecycle Owners
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        
        // 2. Register Watchdog
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
        }
        registerReceiver(batteryWatchdog, filter)

        // 3. Start Foreground Service
        createNotificationChannel()
        startServiceForeground()
        
        // 4. Start Engines
        telemetryViewModel.startMonitoring(this)
        showOverlay()
        startAutoBoostLoop()
    }

    private fun startServiceForeground() {
        val notification = createNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun showOverlay() {
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or 
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }

        val uiContext = ContextThemeWrapper(this, android.R.style.Theme_DeviceDefault_NoActionBar)
        val layout = TouchPassthroughLayout(uiContext)
        rootView = layout

        val composeView = ComposeView(uiContext).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            
            // Attach Owners directly from Service
            this.setViewTreeLifecycleOwner(this@GamingSpaceService)
            this.setViewTreeViewModelStoreOwner(this@GamingSpaceService)
            this.setViewTreeSavedStateRegistryOwner(this@GamingSpaceService)
            
            setContent {
                var isExpandedInternal by remember { mutableStateOf(false) }
                val telemetryState by telemetryViewModel.state
                
                var isReady by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) {
                    delay(250)
                    isReady = true
                }

                LaunchedEffect(isExpandedInternal) {
                    layout.expanded = isExpandedInternal
                }

                if (isReady) {
                    GamingSpaceOverlay(
                        isExpanded = isExpandedInternal,
                        telemetry = telemetryState,
                        onToggleExpand = { newState: Boolean -> isExpandedInternal = newState },
                        onExit = { 
                            val stopIntent = Intent(this@GamingSpaceService, GamingSpaceService::class.java).apply {
                                action = ACTION_STOP_SERVICE
                            }
                            startService(stopIntent)
                        }
                    )
                }
            }
        }
        
        layout.addView(composeView)
        windowManager.addView(layout, params)
    }

    private fun startAutoBoostLoop() {
        serviceScope.launch(Dispatchers.IO) {
            while (isActive) {
                delay(20000)
                try {
                    GfxBackendManager.runShell(null, "echo 3 > /proc/sys/vm/drop_caches")
                } catch (e: Exception) { }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_SERVICE) {
            stopSelf()
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        // --- WATCHDOG: Master Reset on Shutdown ---
        serviceScope.launch(Dispatchers.IO) {
            GfxBackendManager.runShell(null, "dumpsys battery reset")
            GfxBackendManager.runMasterReset(this@GamingSpaceService)
        }

        // 1. Unregister Watchdog
        try {
            unregisterReceiver(batteryWatchdog)
        } catch (e: Exception) { }

        // 2. Cleanup Engines
        telemetryViewModel.stopMonitoring()
        serviceJob.cancel()
        
        // 3. Remove Overlay
        rootView?.let {
            windowManager.removeView(it)
            rootView = null
        }
        
        // 4. Cleanup Lifecycle Owners
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        store.clear()
        
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "gaming_space_channel",
                "Spider Gaming Space",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, "gaming_space_channel")
            .setContentTitle("Spider Gaming Space Active")
            .setContentText("Hacker HUD is running. Swipe to expand.")
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
