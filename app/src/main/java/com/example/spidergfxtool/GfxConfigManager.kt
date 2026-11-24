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
 * Updated for GitHub 9.0 release assets with Zip Slip protection and robust validation.
 */
object GfxConfigManager {
    private const val TAG = "GfxConfigManager"
    private const val VERSION_FOLDER = "9.0_Version"
    
    private val client = OkHttpClient()

    private val fpsUrls = mapOf(
        "120FPS" to "https://raw.githubusercontent.com/GfxGamerLab/spider-gfx-tool/main/9.0/120FPS.zip",
        "90FPS" to "https://raw.githubusercontent.com/GfxGamerLab/spider-gfx-tool/main/9.0/90FPS.zip",
        "60FPS" to "https://raw.githubusercontent.com/GfxGamerLab/spider-gfx-tool/main/9.0/60FPS.zip"
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
     * Handles GitHub 9.0 external storage caching, secure unzipping, and robust Shizuku injection.
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
            if (!SmartEngine.isShizukuReady()) {
                onProgress("[ERROR] Shizuku engine offline. Please start Shizuku via Wireless Debugging.")
                return@withContext
            }

            // 1. SELF-CLEANING & VERSION CONTROL
            cleanupOldVersions(context)

            // 2. SETUP STORAGE
            val baseDir = File(context.getExternalFilesDir(null), VERSION_FOLDER)
            if (!baseDir.exists()) baseDir.mkdirs()

            val zipFile = File(baseDir, "$fps.zip")
            val extractDir = File(baseDir, "Extracted_Config_${System.currentTimeMillis()}")
            
            // 3. SMART CACHE & VALID SIGNATURE CHECK
            if (zipFile.exists()) {
                if (zipFile.length() < 1024L || !isValidZipFile(zipFile)) {
                    onProgress("[CACHE] Corrupt or invalid zip detected. Re-downloading...")
                    zipFile.delete()
                } else {
                    onProgress("[CACHE] Valid cached archive found for $fps.")
                }
            }

            if (!zipFile.exists()) {
                onProgress("[SYSTEM] Downloading $fps release package from GitHub 9.0...")
                downloadFile(downloadUrl, zipFile)
                
                if (!zipFile.exists() || zipFile.length() == 0L || !isValidZipFile(zipFile)) {
                    zipFile.delete()
                    onProgress("[ERROR] Download failed or returned invalid binary format.")
                    return@withContext
                }
            }

            // 4. EXTRACTION
            onProgress("[SYSTEM] Extracting archive securely...")
            if (extractDir.exists()) extractDir.deleteRecursively()
            extractDir.mkdirs()
            unzip(zipFile, extractDir)

            // VALIDATION: LOCATE 'Files' FOLDER
            val sourceFilesDir = findFilesDirectory(extractDir)
            if (sourceFilesDir == null || !sourceFilesDir.exists()) {
                onProgress("[ERROR] Required 'Files' directory not found in extracted archive.")
                extractDir.deleteRecursively()
                return@withContext
            }
            
            val destPath = "/storage/emulated/0/Android/data/$targetPackage/files"

            onProgress("[SYSTEM] Injecting payload into target package: $targetPackage...")

            val command = "mkdir -p '$destPath' && cp -rf '${sourceFilesDir.absolutePath}/.' '$destPath/'"
            val result = runShizukuCommand(command)

            if (result.success) {
                onProgress("[SUCCESS] $fps pack successfully injected into $targetPackage!")
                
                // 5. FINAL CLEANUP
                onProgress("[SYSTEM] Purging temporary staging files...")
                extractDir.deleteRecursively()
                
                showToast(context, "$fps Successfully Applied to $targetPackage!")
            } else {
                onProgress("[ERROR] Payload injection failed: ${result.error}")
            }

        } catch (e: Exception) {
            val errorMsg = e.localizedMessage ?: "Unknown Error"
            Log.e(TAG, "Process Failed", e)
            onProgress("[ERROR] GFX Engine Exception: $errorMsg")
        }
    }

    private fun findFilesDirectory(dir: File): File? {
        // Case-sensitive check for "Files" first
        dir.listFiles()?.forEach { file ->
            if (file.isDirectory && file.name == "Files") {
                return file
            }
        }
        // Case-insensitive fallback
        dir.listFiles()?.forEach { file ->
            if (file.isDirectory && file.name.equals("Files", ignoreCase = true)) {
                return file
            }
        }
        // Deep search recursively
        dir.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                val found = findFilesDirectory(file)
                if (found != null) return found
            }
        }
        return null
    }

    private fun isValidZipFile(file: File): Boolean {
        if (!file.exists() || file.length() < 4L) return false
        return try {
            FileInputStream(file).use { fis ->
                val header = ByteArray(4)
                if (fis.read(header) == 4) {
                    // ZIP file header signature: PK\003\004 (0x50, 0x4B, 0x03, 0x04)
                    header[0] == 0x50.toByte() && header[1] == 0x4B.toByte() && header[2] == 0x03.toByte() && header[3] == 0x04.toByte()
                } else false
            }
        } catch (e: Exception) {
            false
        }
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
        val destCanonicalPath = targetDir.canonicalPath
        ZipInputStream(FileInputStream(zipFile)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val newFile = File(targetDir, entry.name)
                val canonicalPath = newFile.canonicalPath
                if (!canonicalPath.startsWith(destCanonicalPath + File.separator) && canonicalPath != destCanonicalPath) {
                    throw IOException("Zip slip vulnerability detected in entry: ${entry.name}")
                }

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
            val sanitizedError = SmartEngine.sanitizeShizukuOutput(errorStream)
            
            if (exitCode != 0) {
                Log.e(TAG, "Command failed ($exitCode): $sanitizedError")
            }
            CommandResult(exitCode == 0, exitCode, sanitizedError)
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
