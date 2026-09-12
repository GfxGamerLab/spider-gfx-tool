package com.example.spidergfxtool

import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import rikka.shizuku.Shizuku
import java.io.*
import java.util.zip.ZipInputStream

/**
 * Elite GFX Config Manager for Download, Extract, and Shizuku-based Injection.
 * Refactored for v0.5 with robust local storage and Shizuku safety.
 */
object GfxConfigManager {
    private const val TAG = "GfxConfigManager"
    private const val CONFIG_VERSION_DIR = "0.6"
    
    private val client = OkHttpClient()

    private val fpsUrls = mapOf(
        "120FPS" to "https://raw.githubusercontent.com/GfxGamerLab/gfx-pubg-files/main/0.6version/120FPS.zip",
        "90FPS" to "https://raw.githubusercontent.com/GfxGamerLab/gfx-pubg-files/main/0.6version/90FPS.zip",
        "60FPS" to "https://raw.githubusercontent.com/GfxGamerLab/gfx-pubg-files/main/0.6version/60FPS.zip"
    )

    /**
     * Scans for and removes legacy configuration folders from previous versions.
     */
    private fun cleanupLegacyConfigs(context: Context) {
        try {
            // Cleanup legacy external folders
            val externalDir = context.getExternalFilesDir(null)
            val legacyFolders = listOf("Spider_V4_Configs", "fps_configs", "extracted_fps")
            
            legacyFolders.forEach { folderName ->
                val folder = File(externalDir, folderName)
                if (folder.exists()) {
                    folder.deleteRecursively()
                }
            }

            // Cleanup legacy internal cache folders
            val cacheDir = context.cacheDir
            val legacyInternal = listOf("config_v0_4")
            legacyInternal.forEach { folderName ->
                val folder = File(cacheDir, folderName)
                if (folder.exists()) {
                    folder.deleteRecursively()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Cleanup failed: ${e.message}")
        }
    }

    data class CommandResult(val success: Boolean, val exitCode: Int, val error: String)

    /**
     * Core function to apply FPS configuration.
     * Handles v0.6 local storage caching, unzipping, and robust Shizuku injection.
     */
    suspend fun applyFpsConfig(
        fps: String, 
        targetPackage: String, 
        context: Context,
        onProgress: (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        val downloadUrl = fpsUrls[fps] ?: run {
            onProgress("[ERROR] Invalid FPS selection: $fps")
            return@withContext
        }

        try {
            // 0. PRE-FLIGHT: SHIZUKU CHECK
            if (!Shizuku.pingBinder() || Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                onProgress("[ERROR] Shizuku permission not granted or service offline!")
                return@withContext
            }

            // 1. SETUP LOCAL STORAGE (External Files Dir to bypass Shizuku Permission Denied)
            val localDir = context.getExternalFilesDir(CONFIG_VERSION_DIR) ?: throw IOException("External storage unreachable")
            if (!localDir.exists()) localDir.mkdirs()

            // Cleanup legacy data
            cleanupLegacyConfigs(context)

            val zipFile = File(localDir, "$fps.zip")
            val extractDir = File(localDir, "extracted_$fps")
            
            // 2. SMART CACHE & DOWNLOAD
            val cachedFilesDir = findFilesDirectory(extractDir)
            if (cachedFilesDir != null && cachedFilesDir.exists()) {
                onProgress("> LOCAL CACHE FOUND. SKIPPING DOWNLOAD.")
            } else if (zipFile.exists() && zipFile.length() > 0) {
                onProgress("> LOCAL CACHE FOUND. SKIPPING DOWNLOAD.")
                // Still need to unzip if extracted files don't exist but zip does
                if (extractDir.exists()) extractDir.deleteRecursively()
                extractDir.mkdirs()
                unzip(zipFile, extractDir)
            } else {
                onProgress("> DOWNLOADING v0.6 PAYLOAD...")
                downloadFile(downloadUrl, zipFile)
                
                // VALIDATION 1: DOWNLOAD VALIDATION
                if (!zipFile.exists() || zipFile.length() == 0L) {
                    zipFile.delete()
                    onProgress("> [ERROR] DOWNLOAD FAILED (INVALID URL OR NETWORK)")
                    return@withContext
                }

                if (extractDir.exists()) extractDir.deleteRecursively()
                extractDir.mkdirs()
                unzip(zipFile, extractDir)
            }

            // VALIDATION 2: ZIP EXTRACTION VALIDATION
            val sourceFilesDir = findFilesDirectory(extractDir)
            if (sourceFilesDir == null || !sourceFilesDir.exists()) {
                onProgress("> [ERROR] 'files' DIRECTORY NOT FOUND IN ZIP")
                return@withContext
            }
            
            val gamePackagePath = "/storage/emulated/0/Android/data/$targetPackage"

            onProgress("> INJECTING 'files' INTO PACKAGE DIRECTORY...")

            // a) Ensure target directory exists (the game's files folder)
            val mkdirResult = runShizukuCommand("mkdir -p \"$gamePackagePath/files\"")
            if (!mkdirResult.success) {
                onProgress("> [ERROR] Shizuku: Failed to create game path (${mkdirResult.exitCode})")
                return@withContext
            }
            
            // b) Grant permissions to local extracted files so Shizuku shell can read them
            // Quoting the path to handle potential spaces
            val chmodResult = runShizukuCommand("chmod -R 777 \"${localDir.absolutePath}\"")
            if (!chmodResult.success) {
                Log.w(TAG, "Chmod warning: ${chmodResult.error}")
            }
            
            // c) Force-copy extracted 'files' folder directly into game package root
            // This will merge/overwrite the 'files' directory inside the target game path.
            val injectCommand = "cp -rf '${sourceFilesDir.absolutePath}' '${gamePackagePath}/'"
            val injectResult = runShizukuCommand(injectCommand)

            if (injectResult.success) {
                onProgress("> SUCCESS: SYSTEM OVERDRIVE ACTIVE.")
                showToast(context, "Engine Overdrive Active!")
            } else {
                onProgress("> [ERROR] Shizuku: ${injectResult.error} (Code: ${injectResult.exitCode})")
            }

        } catch (e: Exception) {
            val errorMsg = e.localizedMessage ?: "Unknown Error"
            Log.e(TAG, "Process Failed", e)
            onProgress("> [ERROR] Shizuku: $errorMsg")
        }
    }

    private fun findFilesDirectory(dir: File): File? {
        if (dir.name.equals("Files", ignoreCase = true)) return dir
        dir.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                val found = findFilesDirectory(file)
                if (found != null) return found
            }
        }
        return null
    }

    private fun downloadFile(url: String, targetFile: File) {
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Failed to download: ${response.code}")
            response.body?.byteStream()?.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            }
        }
    }

    private fun unzip(zipFile: File, targetDir: File) {
        ZipInputStream(FileInputStream(zipFile)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val newFile = File(targetDir, entry.name)
                if (entry.isDirectory) {
                    newFile.mkdirs()
                } else {
                    newFile.parentFile?.mkdirs()
                    FileOutputStream(newFile).use { fos -> zis.copyTo(fos) }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
    }

    private fun runShizukuCommand(command: String): CommandResult {
        return try {
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
            ) as Process
            
            val exitCode = process.waitFor()
            val errorStream = process.errorStream.bufferedReader().readText().trim()
            
            if (exitCode != 0) {
                Log.e(TAG, "Command failed ($exitCode): $errorStream")
            }
            CommandResult(exitCode == 0, exitCode, errorStream)
        } catch (e: Exception) {
            Log.e(TAG, "Shizuku Exec Error", e)
            CommandResult(false, -1, e.message ?: "Execution Exception")
        }
    }

    private fun showToast(context: Context, message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
}
