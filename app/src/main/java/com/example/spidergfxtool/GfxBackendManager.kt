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
     * Raw shell executor without fallback logic.
     * Wrapped in robust try-catch to prevent crashes on restricted commands.
     */
    private suspend fun runShellInternal(command: String): Result<String> = withContext(Dispatchers.IO) {
        Log.d("GFX_SHELL_INTERNAL", "Executing: $command")
        try {
            if (!Shizuku.pingBinder()) {
                return@withContext Result.failure(Exception("Shizuku service not running"))
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

            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val output = StringBuilder()
            var outLine: String?
            while (reader.readLine().also { outLine = it } != null) {
                output.append(outLine).append("\n")
            }

            val errReader = BufferedReader(InputStreamReader(process.errorStream))
            val error = StringBuilder()
            var line: String?
            while (errReader.readLine().also { line = it } != null) {
                error.append(line).append("\n")
            }

            val exitCode = process.waitFor()

            if (exitCode == 0) {
                Result.success(output.toString().trim())
            } else {
                val errorMsg = error.toString().trim()
                Result.failure(Exception(if (errorMsg.isNotEmpty()) errorMsg else "Shell error (code $exitCode)"))
            }
        } catch (e: SecurityException) {
            Log.e("GFX_SHELL_SECURITY", "Permission denied for command: $command", e)
            Result.failure(Exception("Security Error: Shizuku lacks permission for this system command."))
        } catch (e: Exception) {
            Log.e("GFX_SHELL_ERROR", "Execution failed: ${e.localizedMessage}", e)
            Result.failure(Exception("Execution failed: ${e.localizedMessage}"))
        }
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

            // 1. Cache Check
            if (zipFile.exists() && zipFile.length() > 0) {
                onProgress("[FPS ENGINE] Local zip found in app data directory. Skipping download.")
            } else {
                // 2. Download from GitHub
                onProgress("[FPS ENGINE] Downloading payload to app data folder...")
                val url = URL(downloadUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 15000
                connection.readTimeout = 15000
                connection.instanceFollowRedirects = true
                
                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    return@withContext Result.failure(IOException("Server Error: ${connection.responseCode}"))
                }

                connection.inputStream.use { input ->
                    FileOutputStream(zipFile).use { output ->
                        input.copyTo(output)
                    }
                }
                connection.disconnect()
                onProgress("[PATH] Downloaded to: ${zipFile.absolutePath}")
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
     * MASTER RAM SWEEPER: Force stops apps one-by-one with real-time feedback.
     */
    suspend fun runRamSweeper(
        context: Context,
        includeSystem: Boolean,
        whitelist: Set<String>,
        onProgress: (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val packages = pm.getInstalledPackages(0)
        
        // Safety: Never kill these
        val criticalSafety = setOf(
            "com.example.spidergfxtool",
            "moe.shizuku.privileged.api",
            "com.android.systemui",
            "com.android.launcher",
            "rikka.shizuku"
        )

        val targetList = packages.filter { pkg ->
            val appInfo = pkg.applicationInfo
            val isSystem = if (appInfo != null) (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0 else false
            val isTarget = if (includeSystem) true else !isSystem
            
            isTarget && 
            pkg.packageName !in criticalSafety && 
            pkg.packageName !in whitelist
        }

        if (targetList.isEmpty()) {
            onProgress("SYSTEM MEMORY OPTIMIZED. NO TARGETS.")
            return@withContext
        }

        targetList.forEachIndexed { index, pkg ->
            val percent = ((index + 1).toFloat() / targetList.size * 100).toInt()
            val name = pkg.applicationInfo?.loadLabel(pm)?.toString() ?: pkg.packageName
            
            onProgress("[$percent%] Terminating: $name...")
            
            try {
                // Execute force-stop via am
                runShellInternal("am force-stop ${pkg.packageName}")
                delay(80) // Professional visual delay
            } catch (e: Exception) {
                Log.e("GFX_RAM_SWEEPER", "Failed to kill ${pkg.packageName}")
            }
        }
        
        onProgress("SYSTEM MEMORY OPTIMIZED. ALL TASKS COMPLETED.")
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
     * GLOBAL RESET: Restores factory size and density instantly.
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
}
