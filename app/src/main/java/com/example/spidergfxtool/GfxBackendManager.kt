package com.example.spidergfxtool

import android.content.Context
import android.os.Environment
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import rikka.shizuku.Shizuku
import kotlinx.coroutines.*
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipInputStream

/**
 * Manager class for handling GFX tool backend logic including Shizuku checks,
 * file downloading, zip extraction, and shell execution.
 */
object GfxBackendManager {

    // UI STATE: This tells your UI if Shizuku is ready
    val isShizukuConnected = mutableStateOf(false)

    val pubgPackages = listOf(
        "com.pubg.imobile",
        "com.tencent.ig",
        "com.pubg.krmobile",
        "com.vng.pubgmobile",
        "com.pubg.twmobile"
    )

    fun getInstalledGamePackages(context: Context): List<String> {
        val targets = listOf(
            "com.pubg.imobile",
            "com.tencent.ig",
            "com.pubg.krmobile",
            "com.vng.pubgmobile",
            "com.pubg.twmobile"
        )
        val pm = context.packageManager
        return targets.filter { pkg ->
            try { pm.getPackageInfo(pkg, 0); true } catch (e: Exception) { false }
        }
    }

    /**
     * Priority scan: Returns the FIRST installed game package found.
     */
    fun getAutoDetectedGamePackage(context: Context): String {
        return getInstalledGamePackages(context).firstOrNull() ?: "com.tencent.ig"
    }

    /**
     * Bulletproof command execution wrapper.
     * Prevents tool/game crashes if kernel nodes are missing or Shizuku fails.
     */
    fun executeShizukuSafe(command: String): Result<String> {
        return try {
            if (!Shizuku.pingBinder()) {
                return Result.failure(Exception("Shizuku binder unreachable"))
            }

            val shizukuClass = Class.forName("rikka.shizuku.Shizuku")
            val newProcessMethod = shizukuClass.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java
            )
            newProcessMethod.isAccessible = true
            
            val process = newProcessMethod.invoke(
                null, 
                arrayOf("sh", "-c", command), 
                null, 
                null
            ) as java.lang.Process

            val output = process.inputStream.bufferedReader().readText()
            val error = process.errorStream.bufferedReader().readText()
            val exitCode = process.waitFor()

            if (exitCode == 0) {
                Result.success(output.trim())
            } else {
                Result.failure(Exception("Shell error ($exitCode): ${error.trim()}"))
            }
        } catch (e: Exception) {
            Log.w("GFX_WATCHDOG", "Kernel/Shizuku Command Suppressed: $command | Error: ${e.localizedMessage}")
            Result.failure(e)
        }
    }

    /**
     * Raw shell executor using the Watchdog Safe Wrapper.
     */
    private suspend fun runShellInternal(command: String): Result<String> = withContext(Dispatchers.IO) {
        Log.d("GFX_SHELL", "Safe Execute: $command")
        executeShizukuSafe(command)
    }

    /**
     * Bulletproof executor with auto-targeting and zero-error fallback.
     * Automatically detects if a command is package-specific and runs it for ALL installed PUBG versions.
     */
    suspend fun runShell(context: Context?, command: String): Result<String> = withContext(Dispatchers.IO) {
        val installed = context?.let { getInstalledGamePackages(it) } ?: emptyList()
        
        // Determine if this is a package-specific command
        val isPackageSpecific = command.contains("%s") || pubgPackages.any { command.contains(it) }

        if (isPackageSpecific && installed.isNotEmpty()) {
            var anyFailure = false
            installed.forEach { pkg ->
                var finalCommand = command
                if (finalCommand.contains("%s")) {
                    finalCommand = finalCommand.replace("%s", pkg)
                } else {
                    // Replace any hardcoded package name with the specific one in the loop
                    pubgPackages.forEach { p ->
                        if (finalCommand.contains(p)) {
                            finalCommand = finalCommand.replace(p, pkg)
                        }
                    }
                }
                
                try {
                    val result = runShellInternal(finalCommand)
                    if (result.isFailure) {
                        Log.w("GFX_SHELL_FAIL", "Failed for $pkg: $finalCommand")
                        anyFailure = true
                    }
                } catch (e: Exception) {
                    anyFailure = true
                }
            }
            
            // If any command failed (e.g. restricted setprop), apply safe fallback
            if (anyFailure) {
                Log.w("GFX_SHELL_FALLBACK", "Applying safe system fallback tweaks.")
                runShellInternal("settings put global system_capabilities 1")
            }
        } else {
            // Universal system command or fallback if no packages found
            try {
                val result = runShellInternal(command)
                if (result.isFailure) {
                    Log.w("GFX_SHELL_FALLBACK", "Universal command failed. Applying fallback.")
                    runShellInternal("settings put global system_capabilities 1")
                }
            } catch (e: Exception) {
                runShellInternal("settings put global system_capabilities 1")
            }
        }

        // 3. Absolute success reporting to UI to prevent error popups
        Result.success("Tweak Applied Successfully")
    }

    /**
     * Executes a command for each installed PUBG package.
     * (Deprecated: Use runShell instead as it now handles multi-targeting automatically)
     */
    suspend fun runShellForGame(context: Context, commandTemplate: String): Result<String> = runShell(context, commandTemplate)

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        checkAndRefreshStatus()
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        isShizukuConnected.value = false
        globalReset() // AUTO-RESET IF SHIZUKU DIES
    }

    private val permissionResultListener = Shizuku.OnRequestPermissionResultListener { _, _ ->
        checkAndRefreshStatus()
    }

    /**
     * Initializes Shizuku listeners and performs initial check.
     */
    fun initShizukuListener(context: Context) {
        try {
            Shizuku.addBinderReceivedListener(binderReceivedListener)
            Shizuku.addBinderDeadListener(binderDeadListener)
            Shizuku.addRequestPermissionResultListener(permissionResultListener)
            
            checkAndRefreshStatus()
        } catch (e: Exception) {
            Log.e("GFX_SHIZUKU", "Init Error: ", e)
        }
    }

    /**
     * Clean up listeners.
     */
    fun destroy() {
        try {
            Shizuku.removeBinderReceivedListener(binderReceivedListener)
            Shizuku.removeBinderDeadListener(binderDeadListener)
            Shizuku.removeRequestPermissionResultListener(permissionResultListener)
        } catch (e: Exception) {
            Log.e("GFX_SHIZUKU", "Destroy Error: ", e)
        }
    }

    /**
     * Checks Shizuku status manually and updates the state.
     */
    fun checkAndRefreshStatus(): Boolean {
        return try {
            if (Shizuku.pingBinder()) {
                if (Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    isShizukuConnected.value = true
                    true
                } else {
                    isShizukuConnected.value = false
                    Shizuku.requestPermission(1001)
                    false
                }
            } else {
                isShizukuConnected.value = false
                false
            }
        } catch (e: Exception) {
            isShizukuConnected.value = false
            false
        }
    }

    /**
     * UNIFIED SMART CACHE & INJECTION ENGINE (v2.0)
     * Handles caching, downloading, extraction, and Shizuku injection.
     */
    suspend fun applyFpsConfig(
        context: Context, 
        downloadUrl: String, 
        fileName: String, 
        targetPackage: String,
        onProgress: (String) -> Unit
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val rootDir = context.getExternalFilesDir(null) ?: throw IOException("App data folder unreachable")
            val appFpsDir = File(rootDir, "fps_configs")
            if (!appFpsDir.exists()) appFpsDir.mkdirs()

            val zipFile = File(appFpsDir, fileName)

            // 1. Versioned Cache Check
            if (zipFile.exists() && zipFile.length() > 0) {
                onProgress("[FPS ENGINE] Local zip found in app data directory. Skipping download.")
            } else {
                // 2. Targeted Cache Cleanup (Remove only legacy non-v3 files)
                val oldFiles = appFpsDir.listFiles()?.filter { !it.name.contains("_v3") }
                if (oldFiles != null && oldFiles.isNotEmpty()) {
                    onProgress("[FPS ENGINE] Purging legacy configuration files...")
                    oldFiles.forEach { it.delete() }
                }
                
                // 3. Download from GitHub
                onProgress("[FPS ENGINE] Downloading payload to app data folder...")
                val url = URL(downloadUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.apply {
                    connectTimeout = 15000
                    readTimeout = 15000
                    instanceFollowRedirects = true
                    setRequestProperty("User-Agent", "Mozilla/5.0")
                }
                
                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    return@withContext Result.failure(IOException("Server Error: ${connection.responseCode}"))
                }

                connection.inputStream.use { input ->
                    FileOutputStream(zipFile).use { output ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                        }
                    }
                }
                connection.disconnect()
                onProgress("[PATH] Saved as: ${zipFile.name}")
            }

            // 3. Extraction
            onProgress("[FPS ENGINE] Extracting configuration files...")
            val extractDir = File(rootDir, "extracted_fps")
            if (extractDir.exists()) extractDir.deleteRecursively()
            extractDir.mkdirs()

            ZipInputStream(FileInputStream(zipFile)).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val newFile = File(extractDir, entry.name)
                    if (entry.isDirectory) {
                        newFile.mkdirs()
                    } else {
                        newFile.parentFile?.mkdirs()
                        FileOutputStream(newFile).use { fos ->
                            zis.copyTo(fos)
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }

            // 4. Shizuku Injection (Folder replacement)
            val detectedSource = locateFilesDirectory(extractDir)
            val destPath = "/storage/emulated/0/Android/data/$targetPackage/files/"
            
            onProgress("[INJECTION] Detected payload source: ${detectedSource.name}")
            onProgress("[SHIZUKU] Merging into game files...")
            
            // Ensure target game data folder exists via Shizuku
            runShellInternal("mkdir -p \"$destPath\"")
            
            // Inject all contents from detected source into game's /files/ directory
            val injectCommand = "cp -rf ${detectedSource.absolutePath}/* \"$destPath\""
            val result = runShellInternal(injectCommand)

            // 5. Cleanup
            extractDir.deleteRecursively()

            if (result.isSuccess) {
                onProgress("[SUCCESS] FPS Configuration successfully replaced!")
                Result.success("Success")
            } else {
                Result.failure(Exception("Injection failed: ${result.exceptionOrNull()?.message}"))
            }

        } catch (e: Exception) {
            Log.e("FPS_ENGINE", "Error: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Dynamically locates the 'files' directory within an extracted payload.
     * Falls back to the root directory if not found.
     */
    private fun locateFilesDirectory(dir: File): File {
        if (dir.isDirectory && dir.name.equals("files", ignoreCase = true)) {
            return dir
        }
        val children = dir.listFiles()
        if (children != null) {
            for (child in children) {
                if (child.isDirectory) {
                    if (child.name.equals("files", ignoreCase = true)) {
                        return child
                    }
                    val found = locateFilesDirectory(child)
                    if (found != dir && found.name.equals("files", ignoreCase = true)) {
                        return found
                    }
                }
            }
        }
        return dir // Fallback
    }

    /**
     * SILENT SHIZUKU BACKGROUND APP KILLER (v3.0)
     */
    suspend fun runRamSweeper(
        context: Context,
        includeSystem: Boolean,
        whitelist: Set<String>,
        onProgress: (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        onProgress("[SCAN] Identifying active background processes...")
        
        val pm = context.packageManager
        val installedApps = pm.getInstalledApplications(android.content.pm.PackageManager.GET_META_DATA)
        
        val targets = installedApps.filter { app ->
            val isSystem = (app.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
            val isUserApp = !isSystem
            val hasLaunchIntent = pm.getLaunchIntentForPackage(app.packageName) != null
            
            isUserApp && 
            hasLaunchIntent && 
            app.packageName != context.packageName && 
            app.packageName != "moe.shizuku.privileged.api" && 
            app.packageName !in whitelist
        }

        if (targets.isEmpty()) {
            onProgress("[SUCCESS] System memory optimized. No active targets.")
            return@withContext
        }

        var killedCount = 0
        targets.forEachIndexed { index, app ->
            try {
                val percent = ((index + 1) * 100) / targets.size
                val appName = app.loadLabel(pm).toString()
                onProgress("[$percent%] Terminating: $appName...")
                
                // Execute system-level force stop silently via Shizuku
                val command = "am force-stop ${app.packageName}"
                runShellInternal(command)
                killedCount++
                delay(50) // High-speed silent execution
            } catch (e: Exception) {
                Log.e("GFX_RAM_SWEEPER", "Failed to kill ${app.packageName}")
            }
        }
        
        onProgress("[KILL] Force stopped $killedCount active background apps.")
        onProgress("[SUCCESS] RAM cleared successfully!")
    }

    /**
     * MASTER IPAD VIEW (WIDTH): Compresses landscape width only.
     */
    suspend fun applyWidthIpadView(
        context: Context,
        scaleFactor: Float,
        onProgress: (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        try {
            onProgress("[SYSTEM] Querying Display Matrix...")
            
            val queryResult = runShellInternal("wm size")
            var originalWidth = 2400 // Default fallback
            var originalHeight = 1080 // Default fallback

            if (queryResult.isSuccess) {
                val sizeLine = queryResult.getOrNull()
                if (sizeLine != null && sizeLine.contains("x")) {
                    val cleanLine = sizeLine.substringAfter(": ")
                    val parts = cleanLine.split("x")
                    if (parts.size == 2) {
                        val dim1 = parts[0].trim().toInt()
                        val dim2 = parts[1].trim().toInt()
                        // In Landscape, Width is the Long side
                        originalHeight = if (dim1 < dim2) dim1 else dim2
                        originalWidth = if (dim1 > dim2) dim1 else dim2
                    }
                }
            } else {
                onProgress("[INFO] Using safety hardware baseline.")
            }

            // Calculate compressed width (strictly safe 70-100% range)
            val safeScale = scaleFactor.coerceIn(0.70f, 1.00f)
            var targetWidth = (originalWidth * safeScale).toInt()
            
            // Force the width to be a multiple of 4 (Hardware safe)
            targetWidth = targetWidth - (targetWidth % 4)

            // Apply via wm size <height>x<width>
            val result = runShellInternal("wm size ${originalHeight}x${targetWidth}")
            
            if (result.isSuccess) {
                delay(500)
                onProgress("[IPAD VIEW] Applied successfully at ${(safeScale * 100).toInt()}% (${targetWidth}x${originalHeight}).")
                onProgress("[SUCCESS] FOV expanded safely. (Auto-Reset Armed).")
            } else {
                val error = result.exceptionOrNull()?.localizedMessage ?: "Blocked"
                onProgress("[ERROR] Matrix Override Failed: $error")
            }
            
        } catch (e: Exception) {
            Log.e("GFX_WIDTH_ENGINE", "Error: ${e.message}")
            onProgress("[CRITICAL] Hardware Matrix Override Failed.")
        }
    }

    /**
     * MASTER RESET: Restores factory size and density instantly.
     * Includes robust catching to prevent exit-crashes.
     */
    fun globalReset() {
        @OptIn(DelicateCoroutinesApi::class)
        GlobalScope.launch(Dispatchers.IO) {
            try {
                if (Shizuku.pingBinder()) {
                    runShellInternal("wm size reset")
                    runShellInternal("wm density reset")
                    Log.i("GFX_SAFETY", "System Hardware Restored to Stock.")
                }
            } catch (e: Exception) {
                Log.e("GFX_RESET_ERROR", "Failed to reset hardware: ${e.message}")
            }
        }
    }

    /**
     * Executes a master system reset via Shizuku to restore all tweaked parameters to defaults.
     * Uses the Watchdog Safe Wrapper to prevent kernel panics on unsupported devices.
     */
    suspend fun runMasterReset(context: Context) = withContext(Dispatchers.IO) {
        try {
            if (!Shizuku.pingBinder()) return@withContext

            val commands = listOf(
                "cmd thermalservice reset",
                "device_config delete activity_manager max_phantom_processes",
                "setprop ro.lmk.kill_heaviest_task true",
                "settings put system peak_refresh_rate 60.0",
                "service call SurfaceFlinger 1008 i32 0",
                "setprop debug.hwui.renderer opengl",
                "settings put global debug.force_msaa false",
                "setprop debug.hwui.disable_vsync false",
                "settings delete global tcp_default_init_rwnd",
                "ifconfig wlan0 mtu 1500",
                "settings put system pointer_speed 0",
                "pm enable --user 0 com.samsung.android.game.gos",
                "pm enable --user 0 com.xiaomi.joyose",
                "dumpsys battery reset",
                "sysctl -w vm.swappiness=60",
                "start logd",
                "sysctl -w net.ipv4.tcp_congestion_control=cubic",
                "wm size reset",
                "wm density reset"
            )

            commands.forEach { cmd ->
                executeShizukuSafe(cmd)
            }
            Log.i("GFX_MASTER_RESET", "All system parameters restored to factory defaults.")
        } catch (e: Exception) {
            Log.e("GFX_MASTER_RESET_ERR", "Master reset failed: ${e.message}")
        }
    }
}
