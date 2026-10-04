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
 * Refactored for v0.7 with External Secure Storage and Shizuku safety.
 */
object GfxConfigManager {
    private const val TAG = "GfxConfigManager"
    private const val VERSION_FOLDER = "0.7_Version"
    
    private val client = OkHttpClient()

    private val fpsUrls = mapOf(
        "120FPS" to "https://raw.githubusercontent.com/GfxGamerLab/gfx-pubg-files/main/0.7/%20Version/120FPS.zip",
        "90FPS" to "https://raw.githubusercontent.com/GfxGamerLab/gfx-pubg-files/main/0.7/%20Version/90FPS.zip",
        "60FPS" to "https://raw.githubusercontent.com/GfxGamerLab/gfx-pubg-files/main/0.7/%20Version/60FPS.zip"
    )

    data class CommandResult(val success: Boolean, val exitCode: Int, val error: String)

    /**
     * Cleans up any old or mismatched version folders from the external files directory.
     */
    private fun cleanupOldVersions(context: Context) {
        try {
            val root = context.getExternalFilesDir(null) ?: return
            root.listFiles()?.forEach { file ->
                if (file.isDirectory && file.name != VERSION_FOLDER) {
                    Log.d(TAG, "Cleaning up old version: ${file.name}")
                    file.deleteRecursively()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Cleanup failed: ${e.message}")
        }
    }

    /**
     * Core function to apply FPS configuration.
     * Handles v0.7 external storage caching, unzipping, and robust Shizuku injection.
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
                onProgress("[ERROR] Shizuku engine offline. Please start Shizuku via Wireless Debugging.")
                return@withContext
            }

            // 1. SELF-CLEANING & VERSION CONTROL
            cleanupOldVersions(context)

            // 2. SETUP STORAGE (getExternalFilesDir to avoid internal permission blocks)
            val baseDir = File(context.getExternalFilesDir(null), VERSION_FOLDER)
            if (!baseDir.exists()) baseDir.mkdirs()

            val zipFile = File(baseDir, "$fps.zip")
            val extractDir = File(baseDir, "Extracted_Config")
            
            // 3. SMART CACHE & CORRUPT CHECK
            if (zipFile.exists()) {
                if (zipFile.length() < 1024L) {
                    onProgress("[CACHE] Corrupt/Empty file detected. Deleting...")
                    zipFile.delete()
                } else {
                    onProgress("[CACHE] Valid file found. Skipping download.")
                }
            }

            if (!zipFile.exists()) {
                onProgress("> DOWNLOADING v0.7 PAYLOAD...")
                downloadFile(downloadUrl, zipFile)
                
                if (!zipFile.exists() || zipFile.length() == 0L) {
                    zipFile.delete()
                    onProgress("> [ERROR] DOWNLOAD FAILED (INVALID URL OR NETWORK)")
                    return@withContext
                }
            }

            // 4. EXTRACTION
            onProgress("> EXTRACTING CONFIGURATION PAYLOAD...")
            if (extractDir.exists()) extractDir.deleteRecursively()
            extractDir.mkdirs()
            unzip(zipFile, extractDir)

            // VALIDATION: ZIP EXTRACTION VALIDATION
            val sourceFilesDir = findFilesDirectory(extractDir)
            if (sourceFilesDir == null || !sourceFilesDir.exists()) {
                onProgress("> [ERROR] 'Files' DIRECTORY NOT FOUND IN ZIP")
                return@withContext
            }
            
            val gameFilesPath = "/storage/emulated/0/Android/data/$targetPackage/files/"

            onProgress("> INJECTING 'Files' INTO TARGET GAME...")

            // a) Ensure target directory exists via Shizuku shell
            runShizukuCommand("mkdir -p \"$gameFilesPath\"")
            
            // b) Overwrite assets using explicit Shizuku copy command
            // We copy contents of Files/ into the game's files/ folder
            val injectCommand = "cp -rf \"${sourceFilesDir.absolutePath}/.\" \"$gameFilesPath\""
            val injectResult = runShizukuCommand(injectCommand)

            if (injectResult.success) {
                onProgress("[SUCCESS] Injection successful! System Overdrive Active.")
                
                // 5. FINAL CLEANUP
                onProgress("> PURGING TEMPORARY FILES...")
                extractDir.deleteRecursively()
                
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
