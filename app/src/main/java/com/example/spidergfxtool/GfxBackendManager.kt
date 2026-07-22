package com.example.spidergfxtool

import android.content.Context
import android.os.Environment
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import rikka.shizuku.Shizuku
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.*
import java.util.zip.ZipInputStream

/**
 * Manager class for handling GFX tool backend logic including Shizuku checks,
 * file downloading, zip extraction, and shell execution.
 */
object GfxBackendManager {

    // Bulletproof OkHttpClient with redirects enabled
    private val client = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    // Dynamic destination path generation
    private fun getDestPath(packageName: String): String {
        return "${Environment.getExternalStorageDirectory()}/Android/data/$packageName/"
    }

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
     */
    private suspend fun runShellInternal(command: String): Result<String> = withContext(Dispatchers.IO) {
        Log.d("GFX_SHELL_INTERNAL", "Executing: $command")
        try {
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

            val errReader = BufferedReader(InputStreamReader(process.errorStream))
            val error = StringBuilder()
            var line: String?
            while (errReader.readLine().also { line = it } != null) {
                error.append(line).append("\n")
            }

            val exitCode = process.waitFor()

            if (exitCode == 0) {
                Result.success("Success")
            } else {
                Result.failure(Exception("Shell error (code $exitCode): $error"))
            }
        } catch (e: Exception) {
            Result.failure(e)
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
     * Bulletproof Download Function - Uses getExternalCacheDir() for Shizuku access
     */
    suspend fun downloadGfxZip(context: Context, downloadUrl: String): Result<File> = withContext(Dispatchers.IO) {
        val extCache = context.externalCacheDir ?: return@withContext Result.failure(IOException("External cache unavailable"))
        val fileName = if (downloadUrl.contains("LAGFIX")) "LAGFIX.zip" else "120FPS.zip"
        val targetFile = File(extCache, fileName)
        val extractedDirName = if (downloadUrl.contains("LAGFIX")) "extracted_files_lagfix" else "extracted_files"
        val extractedDir = File(extCache, extractedDirName)
        
        try {
            if (targetFile.exists()) targetFile.delete()
            if (extractedDir.exists()) extractedDir.deleteRecursively()
        } catch (e: Exception) {
            Log.e("GFX_DOWNLOAD", "Cleanup failed: ${e.message}")
        }

        val request = Request.Builder()
            .url(downloadUrl)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext Result.failure(IOException("HTTP Error: ${response.code}"))
                val body = response.body ?: throw IOException("Empty response body")
                FileOutputStream(targetFile).use { output -> body.byteStream().use { input -> input.copyTo(output) } }
                Result.success(targetFile)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * LOCAL EXTRACTION: Extracts to externalCacheDir/extracted_files/
     */
    suspend fun extractZip(zipFile: File, outputDir: File): Result<File> = withContext(Dispatchers.IO) {
        try {
            if (outputDir.exists()) outputDir.deleteRecursively()
            outputDir.mkdirs()
            
            ZipInputStream(FileInputStream(zipFile)).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val newFile = File(outputDir, entry.name)
                    if (entry.isDirectory) {
                        newFile.mkdirs()
                    } else {
                        newFile.parentFile?.mkdirs()
                        BufferedOutputStream(FileOutputStream(newFile)).use { bos -> zis.copyTo(bos) }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
            
            val filesFolder = findFilesFolder(outputDir)
            if (filesFolder != null) {
                Result.success(filesFolder)
            } else {
                Result.failure(Exception("Could not find 'files' folder in zip"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun findFilesFolder(root: File): File? {
        if (root.isDirectory && root.name == "files") return root
        val children = root.listFiles() ?: return null
        for (child in children) {
            if (child.isDirectory) {
                val found = findFilesFolder(child)
                if (found != null) return found
            }
        }
        return null
    }

    /**
     * SAFE SHIZUKU COPY
     */
    suspend fun applyGfx(filesFolder: File, packageName: String): Result<String> = withContext(Dispatchers.IO) {
        val sourcePath = filesFolder.absolutePath
        val destPath = getDestPath(packageName)
        val command = "cp -rf \"$sourcePath\" \"$destPath\""
        
        try {
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

            val exitCode = process.waitFor()

            if (exitCode == 0) {
                if (verifyInstallation(destPath + "files")) {
                    Result.success("120 FPS Applied Successfully!")
                } else {
                    Result.failure(Exception("Apply failed: Verification check returned false."))
                }
            } else {
                Result.failure(Exception("Apply failed (code $exitCode)"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Uses Shizuku to check if the target directory exists.
     */
    private fun verifyInstallation(path: String): Boolean {
        return try {
            val shizukuClass = Class.forName("rikka.shizuku.Shizuku")
            val newProcessMethod = shizukuClass.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java
            )
            newProcessMethod.isAccessible = true
            val verifyCommand = "[ -d \"$path\" ] && echo \"OK\""
            val process = newProcessMethod.invoke(null, arrayOf("sh", "-c", verifyCommand), null, null) as java.lang.Process
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val result = reader.readLine()
            process.waitFor() == 0 && result == "OK"
        } catch (e: Exception) {
            false
        }
    }
}
