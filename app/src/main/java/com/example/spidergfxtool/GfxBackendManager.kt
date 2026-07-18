package com.example.spidergfxtool

import android.content.Context
import android.content.pm.PackageManager
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
                if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
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
        
        // SAFE CACHE CLEAN-UP: Always start fresh to avoid corrupt streams
        try {
            if (targetFile.exists()) {
                Log.d("GFX_DOWNLOAD", "Cleaning up old zip: $fileName")
                targetFile.delete()
            }
            if (extractedDir.exists()) {
                Log.d("GFX_DOWNLOAD", "Cleaning up old extraction cache: $extractedDirName")
                extractedDir.deleteRecursively()
            }
        } catch (e: Exception) {
            Log.e("GFX_DOWNLOAD", "Cleanup failed: ${e.message}")
        }

        val request = Request.Builder()
            .url(downloadUrl)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            .build()

        try {
            Log.d("GFX_DOWNLOAD", "Starting download from: $downloadUrl")
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
     * Refactored to suspend function to ensure it runs on IO dispatcher.
     */
    suspend fun extractZip(zipFile: File, outputDir: File): Result<File> = withContext(Dispatchers.IO) {
        try {
            val existingFilesFolder = findFilesFolder(outputDir)
            if (existingFilesFolder != null && existingFilesFolder.exists()) {
                Log.d("GFX_EXTRACT", "Extracted folder already exists, skipping extraction.")
                return@withContext Result.success(existingFilesFolder)
            }

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
                        // VERIFIED OVERWRITE STREAM: Using BufferedOutputStream for clean replacement
                        BufferedOutputStream(FileOutputStream(newFile)).use { bos -> 
                            zis.copyTo(bos) 
                        }
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
     * SAFE SHIZUKU COPY: Targeted merge-copy without deleting game directories.
     * Uses 'cp -rf' to merge content into destination, ensuring only files in 
     * the zip are overwritten while keeping all other game data intact.
     */
    suspend fun applyGfx(filesFolder: File, packageName: String): Result<String> = withContext(Dispatchers.IO) {
        val sourcePath = filesFolder.absolutePath
        val destPath = getDestPath(packageName)
        
        // CRITICAL SAFETY: No 'rm -rf'. 
        // We use 'cp -rf' which merges the 'files' directory from the source 
        // into the destination game path. This overwrites existing config files 
        // without touching unrelated data (maps, obb, etc).
        val command = "cp -rf \"$sourcePath\" \"$destPath\""
        
        Log.d("GFX_SHELL", "Executing SAFE Targeted Copy: $command")
        
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
                // Post-Execution Verification: Verify the specific folder exists at destination
                if (verifyInstallation(destPath + "files")) {
                    Log.d("GFX_SHELL", "Verified: Applied configuration exists.")
                    Result.success("120 FPS Applied Successfully!")
                } else {
                    Log.e("GFX_SHELL", "Verification Failed: Folder not found after copy.")
                    Result.failure(Exception("Apply failed: Verification check returned false."))
                }
            } else {
                Log.e("GFX_SHELL", "Copy Failed ($exitCode): $error")
                Result.failure(Exception("Apply failed (code $exitCode): $error"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Executes a shell command via Shizuku safely.
     */
    suspend fun runShell(command: String): Result<String> = withContext(Dispatchers.IO) {
        Log.d("GFX_SHELL", "Executing: $command")
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
            
            // Check if directory exists
            val verifyCommand = "[ -d \"$path\" ] && echo \"OK\""
            
            val process = newProcessMethod.invoke(
                null, 
                arrayOf("sh", "-c", verifyCommand), 
                null, 
                null
            ) as java.lang.Process

            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val result = reader.readLine()
            process.waitFor() == 0 && result == "OK"
        } catch (e: Exception) {
            false
        }
    }
}
