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
    private const val CONFIG_VERSION_DIR = "config_v0_5"
    
    private val client = OkHttpClient()

    private val fpsUrls = mapOf(
        "120FPS" to "https://raw.githubusercontent.com/GfxGamerLab/gfx-pubg-files/main/0.5version/120FPS.zip",
        "90FPS" to "https://raw.githubusercontent.com/GfxGamerLab/gfx-pubg-files/main/0.5version/90FPS.zip",
        "60FPS" to "https://raw.githubusercontent.com/GfxGamerLab/gfx-pubg-files/main/0.5version/60FPS.zip"
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

    /**
     * Core function to apply FPS configuration.
     * Handles v0.5 local storage caching, unzipping, and robust Shizuku injection.
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

            // 1. SETUP LOCAL STORAGE
            val localDir = context.getExternalFilesDir(CONFIG_VERSION_DIR) ?: throw IOException("App storage unreachable")
            if (!localDir.exists()) localDir.mkdirs()

            // Cleanup legacy data
            cleanupLegacyConfigs(context)

            val zipFile = File(localDir, "$fps.zip")
            val extractDir = File(localDir, "extracted_$fps")
            
            // 2. SMART CACHE & DOWNLOAD
            if (zipFile.exists() && zipFile.length() > 0) {
                onProgress("[CACHE HIT] Local v0.5 config archive found. Applying offline...")
            } else {
                onProgress("[DOWNLOAD] Fetching v0.5 config archive from GitHub...")
                downloadFile(downloadUrl, zipFile)
                onProgress("[DOWNLOAD] Downloaded v0.5 archive to local app data folder.")
            }

            // 3. PURE KOTLIN UNZIP
            if (extractDir.exists()) extractDir.deleteRecursively()
            extractDir.mkdirs()

            onProgress("[UNZIP] Extracting configuration structure...")
            unzip(zipFile, extractDir)
            onProgress("[UNZIP] Extracted files successfully in app storage.")

            // 4. ROBUST SHIZUKU INJECTION
            val sourceFilesDir = findFilesDirectory(extractDir) ?: throw IOException("Inner 'Files' structure not found in zip")
            val gameFilesPath = "/storage/emulated/0/Android/data/$targetPackage/files"

            onProgress("[INJECT] Copying files via Shizuku to $gameFilesPath/...")

            // a) Ensure target directory exists
            runShizukuCommand("mkdir -p \"$gameFilesPath\"")
            
            // b) Grant permissions to local extracted files so Shizuku shell can read them
            runShizukuCommand("chmod -R 777 \"${extractDir.absolutePath}\"")
            
            // c) Force-copy extracted contents directly into game path
            val injectResult = runShizukuCommand("cp -rf \"${sourceFilesDir.absolutePath}/\"* \"$gameFilesPath/\"")

            // 5. CLEANUP extraction residue
            extractDir.deleteRecursively()

            if (injectResult) {
                onProgress("[SUCCESS] GFX Config v0.5 Applied Successfully!")
                showToast(context, "Config applied successfully!")
            } else {
                onProgress("[ERROR] Shizuku injection command failed.")
            }

        } catch (e: Exception) {
            val errorMsg = e.localizedMessage ?: "Unknown Error"
            Log.e(TAG, "Process Failed", e)
            onProgress("[ERROR] Process Failed: $errorMsg")
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

    private fun runShizukuCommand(command: String): Boolean {
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
            if (exitCode != 0) {
                val error = process.errorStream.bufferedReader().readText()
                Log.e(TAG, "Command failed ($exitCode): $error")
            }
            exitCode == 0
        } catch (e: Exception) {
            Log.e(TAG, "Shizuku Exec Error", e)
            false
        }
    }

    private fun showToast(context: Context, message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
}
