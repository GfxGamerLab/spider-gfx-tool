package com.example.spidergfxtool

import android.content.Context
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
 */
object GfxConfigManager {
    private const val TAG = "GfxConfigManager"
    private const val CONFIG_DIR_NAME = "Spider_V4_Configs"
    
    private val client = OkHttpClient()

    private val fpsUrls = mapOf(
        "120FPS" to "https://github.com/GfxGamerLab/gfx-pubg-files/releases/download/v4.0-configs/120FPS.zip",
        "90FPS" to "https://github.com/GfxGamerLab/gfx-pubg-files/releases/download/v4.0-configs/90FPS.zip",
        "60FPS" to "https://github.com/GfxGamerLab/gfx-pubg-files/releases/download/v4.0-configs/60FPS.zip"
    )

    /**
     * Core function to apply FPS configuration.
     * Handles caching, downloading, unzipping, and Shizuku injection.
     */
    suspend fun applyFpsConfig(fps: String, targetPackage: String, context: Context) = withContext(Dispatchers.IO) {
        val downloadUrl = fpsUrls[fps] ?: run {
            showToast(context, "Invalid FPS selection: $fps")
            return@withContext
        }

        try {
            val rootDir = context.getExternalFilesDir(null) ?: throw IOException("External storage unreachable")
            val configDir = File(rootDir, CONFIG_DIR_NAME)
            if (!configDir.exists()) configDir.mkdirs()

            val zipFile = File(configDir, "$fps.zip")
            
            // 1. SMART CACHING: Check if ZIP already exists
            if (zipFile.exists() && zipFile.length() > 0) {
                Log.d(TAG, "Using cached config: ${zipFile.absolutePath}")
            } else {
                // 2. DOWNLOAD: Fetch from GitHub
                Log.d(TAG, "Downloading config from: $downloadUrl")
                downloadFile(downloadUrl, zipFile)
            }

            // 3. EXTRACTION: Unzip to temp folder
            val extractDir = File(configDir, "extracted_$fps")
            if (extractDir.exists()) extractDir.deleteRecursively()
            extractDir.mkdirs()

            unzip(zipFile, extractDir)

            // 4. SHIZUKU INJECTION: Bypass Scoped Storage
            // ZIP structure: <fps>/Files/ -> /sdcard/Android/data/<pkg>/
            val sourcePath = File(extractDir, "$fps/Files").absolutePath
            // Using /sdcard/ is standard for shell commands in this context
            val destPath = "/sdcard/Android/data/$targetPackage/"

            if (!File(sourcePath).exists()) {
                throw IOException("Extracted Files directory not found at $sourcePath")
            }

            Log.d(TAG, "Injecting via Shizuku: $sourcePath -> $destPath")
            // Ensure target directory exists
            runShizukuCommand("mkdir -p \"$destPath\"")
            
            val injectResult = runShizukuCommand("cp -rf \"$sourcePath\"* \"$destPath\"")

            // 5. CLEANUP: Remove temp folder
            extractDir.deleteRecursively()

            if (injectResult) {
                showToast(context, "Successfully applied $fps config to $targetPackage")
            } else {
                showToast(context, "Failed to inject config via Shizuku")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error applying config", e)
            showToast(context, "Error: ${e.localizedMessage}")
        }
    }

    private fun downloadFile(url: String, targetFile: File) {
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Failed to download file: $response")
            
            response.body?.byteStream()?.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            } ?: throw IOException("Response body is null")
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
                    FileOutputStream(newFile).use { fos ->
                        zis.copyTo(fos)
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
    }

    private fun runShizukuCommand(command: String): Boolean {
        return try {
            if (!Shizuku.pingBinder()) return false
            
            // Using reflection to access newProcess as done in GfxBackendManager
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
            
            if (exitCode != 0) {
                val errorMsg = process.errorStream.bufferedReader().readText()
                Log.e(TAG, "Shizuku command failed (exit $exitCode): $errorMsg")
            }
            
            exitCode == 0
        } catch (e: Exception) {
            Log.e(TAG, "Shizuku execution error", e)
            false
        }
    }

    private fun showToast(context: Context, message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }
}
