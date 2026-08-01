package com.example.spidergfxtool

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.StatFs
import android.view.Choreographer
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Locale

data class SystemTelemetryState(
    val brand: String = Build.MANUFACTURER,
    val model: String = Build.MODEL,
    val androidVersion: String = Build.VERSION.RELEASE,
    val sdkInt: Int = Build.VERSION.SDK_INT,
    val cpuArch: String = Build.SUPPORTED_ABIS.firstOrNull() ?: "Unknown",
    val totalRamGb: Double = 0.0,
    val availRamGb: Double = 0.0,
    val ramUsagePercent: Float = 0f,
    val totalStorageGb: Long = 0,
    val freeStorageGb: Long = 0,
    val batteryPercent: Int = 0,
    val batteryTemp: Float = 0f,
    val liveFps: Int = 0,
    val livePingMs: Int = 0
)

class SystemTelemetryViewModel : ViewModel() {
    private val _state = mutableStateOf(SystemTelemetryState())
    val state: State<SystemTelemetryState> = _state

    private var monitorJob: Job? = null
    private var fpsCallback: Choreographer.FrameCallback? = null
    private var lastFrameTimeNanos: Long = 0

    fun startMonitoring(context: Context) {
        if (monitorJob?.isActive == true) return

        // 1. Initial Static Data
        updateStaticSpecs(context)

        // 2. Continuous Monitoring
        monitorJob = viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                updateDynamicSpecs(context)
                delay(2000) // Update every 2 seconds
            }
        }

        // 3. Start FPS Counter
        startFpsCounter()
    }

    fun stopMonitoring() {
        monitorJob?.cancel()
        fpsCallback?.let { Choreographer.getInstance().removeFrameCallback(it) }
    }

    private fun updateStaticSpecs(context: Context) {
        val memoryInfo = ActivityManager.MemoryInfo()
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        activityManager.getMemoryInfo(memoryInfo)

        val totalRam = memoryInfo.totalMem.toDouble() / (1024 * 1024 * 1024)
        
        val stat = StatFs(context.filesDir.absolutePath)
        val totalStorage = (stat.blockCountLong * stat.blockSizeLong) / (1024 * 1024 * 1024)

        _state.value = _state.value.copy(
            totalRamGb = String.format(Locale.US, "%.2f", totalRam).toDouble(),
            totalStorageGb = totalStorage
        )
    }

    private suspend fun updateDynamicSpecs(context: Context) {
        // RAM Update
        val memoryInfo = ActivityManager.MemoryInfo()
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        activityManager.getMemoryInfo(memoryInfo)
        val availRam = memoryInfo.availMem.toDouble() / (1024 * 1024 * 1024)
        val usedRam = memoryInfo.totalMem - memoryInfo.availMem
        val usagePercent = (usedRam.toFloat() / memoryInfo.totalMem.toFloat())

        // Storage Update
        val stat = StatFs(context.filesDir.absolutePath)
        val freeStorage = (stat.availableBlocksLong * stat.blockSizeLong) / (1024 * 1024 * 1024)

        // Battery Update
        val batteryStatus: Intent? = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val temp = (batteryStatus?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0) / 10f

        // Ping Update
        val ping = getPingLatency()

        withContext(Dispatchers.Main) {
            _state.value = _state.value.copy(
                availRamGb = String.format(Locale.US, "%.2f", availRam).toDouble(),
                ramUsagePercent = usagePercent,
                freeStorageGb = freeStorage,
                batteryPercent = level,
                batteryTemp = temp,
                livePingMs = ping
            )
        }
    }

    private fun startFpsCounter() {
        fpsCallback = object : Choreographer.FrameCallback {
            override fun doFrame(frameTimeNanos: Long) {
                if (lastFrameTimeNanos != 0L) {
                    val diff = frameTimeNanos - lastFrameTimeNanos
                    val fps = (1_000_000_000L / diff).toInt()
                    _state.value = _state.value.copy(liveFps = fps.coerceIn(0, 144))
                }
                lastFrameTimeNanos = frameTimeNanos
                Choreographer.getInstance().postFrameCallback(this)
            }
        }
        Choreographer.getInstance().postFrameCallback(fpsCallback!!)
    }

    private fun getPingLatency(): Int {
        return try {
            val process = Runtime.getRuntime().exec("ping -c 1 8.8.8.8")
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            var latency = -1
            while (reader.readLine().also { line = it } != null) {
                line?.let { l ->
                    if (l.contains("time=")) {
                        val index = l.indexOf("time=")
                        val end = l.indexOf(" ms", index)
                        latency = l.substring(index + 5, end).toDouble().toInt()
                    }
                }
            }
            process.waitFor()
            if (latency == -1) 0 else latency
        } catch (e: Exception) {
            0
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopMonitoring()
    }
}
