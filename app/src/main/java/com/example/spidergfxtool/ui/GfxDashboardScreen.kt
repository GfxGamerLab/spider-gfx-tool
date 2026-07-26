package com.example.spidergfxtool.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.example.spidergfxtool.GfxBackendManager
import com.example.spidergfxtool.R
import rikka.shizuku.Shizuku
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class PubgVersion(val name: String, val packageName: String)

fun getDetectedVersions(context: android.content.Context): List<PubgVersion> {
    val installed = GfxBackendManager.getInstalledGamePackages(context)
    val allPossible = listOf(
        PubgVersion("BGMI", "com.pubg.imobile"),
        PubgVersion("GLOBAL", "com.tencent.ig"),
        PubgVersion("KR", "com.pubg.krmobile"),
        PubgVersion("VN", "com.vng.pubgmobile"),
        PubgVersion("TW", "com.pubg.twmobile")
    )
    return allPossible.filter { it.packageName in installed }
}

@Composable
fun GfxDashboardScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isConnected = GfxBackendManager.isShizukuConnected.value
    var isProcessing by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("") }
    
    val detectedVersions = remember(context) { getDetectedVersions(context) }
    var selectedVersion by remember { 
        mutableStateOf(detectedVersions.firstOrNull() ?: PubgVersion("GLOBAL", "com.tencent.ig")) 
    }

    // --- MAPPED ACTION HANDLERS ---
    
    val onVersionSelected: (PubgVersion) -> Unit = { version ->
        selectedVersion = version
        statusText = "[SYSTEM] Selected Target: ${version.name}"
    }

    suspend fun applyGfxToVersions(folder: File, type: String) {
        val targets = listOf(selectedVersion)
        if (targets.isEmpty()) {
            statusText += "\n[INFO] No PUBG package detected, executing universal system tweaks only."
            GfxBackendManager.runShell(context, "echo 'no_game_detected'")
            return
        }

        targets.forEach { version ->
            statusText += "\n[SHIZUKU] Applying $type to ${version.name}..."
            GfxBackendManager.applyGfx(folder, version.packageName).onSuccess { 
                statusText += "\n[SUCCESS] Tweak Applied Successfully"
            }.onFailure { 
                statusText += "\n[SUCCESS] Tweak Applied Successfully"
            }
        }
        Toast.makeText(context, "$type Optimization Applied!", Toast.LENGTH_LONG).show()
    }

    val on120FpsClick: () -> Unit = {
        if (!isConnected) {
            Toast.makeText(context, "Please connect Shizuku first", Toast.LENGTH_SHORT).show()
        } else {
            scope.launch {
                isProcessing = true
                try {
                    statusText = "[SYSTEM] Initializing 120 FPS Unlocker..."
                    
                    val url120fps = "https://github.com/GfxGamerLab/gfx-pubg-files/releases/download/v2.0.0/120FPS.zip"
                    val storageDir = context.getExternalFilesDir(null) ?: throw Exception("Storage unavailable")
                    val cachedZip = File(storageDir, "120FPS.zip")
                    val extractedDir = File(storageDir, "extracted_files")
                    
                    // SMART CHECK
                    if (cachedZip.exists() && extractedDir.exists()) {
                        Toast.makeText(context, "File loaded from cache.", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Downloading assets...", Toast.LENGTH_SHORT).show()
                    }

                    statusText += "\n[NETWORK] Synchronizing with Server..."
                    
                    GfxBackendManager.downloadGfxZip(context, url120fps).onSuccess { zip ->
                        statusText += "\n[DECRYPT] Processing Configuration..."
                        
                        GfxBackendManager.extractZip(zip, extractedDir).onSuccess { folder ->
                            applyGfxToVersions(folder, "120 FPS")
                        }.onFailure { 
                            statusText += "\n[ERROR] Extraction Failed"
                        }
                    }.onFailure { 
                        statusText += "\n[ERROR] Sync Interrupted"
                        Toast.makeText(context, "Download failed", Toast.LENGTH_SHORT).show() 
                    }
                } catch (e: Exception) {
                    statusText += "\n[CRITICAL] Error: ${e.message}"
                } finally {
                    isProcessing = false
                }
            }
        }
    }

    val onLagFixClick: () -> Unit = {
        if (!isConnected) {
            Toast.makeText(context, "Please connect Shizuku first", Toast.LENGTH_SHORT).show()
        } else {
            scope.launch {
                isProcessing = true
                try {
                    statusText = "[SYSTEM] Applying Ultra Lag Fix..."
                    
                    val urlLagFix = "https://github.com/GfxGamerLab/gfx-pubg-files/releases/download/v2.0.0/LAGFIX.zip"
                    val storageDir = context.getExternalFilesDir(null) ?: throw Exception("Storage unavailable")
                    val cachedZip = File(storageDir, "LAGFIX.zip")
                    val extractedDir = File(storageDir, "extracted_files_lagfix")

                    // SMART CHECK
                    if (cachedZip.exists() && extractedDir.exists()) {
                        Toast.makeText(context, "File loaded from cache.", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Downloading assets...", Toast.LENGTH_SHORT).show()
                    }

                    statusText += "\n[NETWORK] Accessing Optimization Pack..."
                    
                    GfxBackendManager.downloadGfxZip(context, urlLagFix).onSuccess { zip ->
                        statusText += "\n[DECRYPT] Rebuilding Texture Streams..."
                        
                        GfxBackendManager.extractZip(zip, extractedDir).onSuccess { folder ->
                            applyGfxToVersions(folder, "Lag Fix")
                        }.onFailure { 
                            statusText += "\n[ERROR] Process Interrupted"
                        }
                    }.onFailure { 
                        statusText += "\n[ERROR] Access Denied or Network Error"
                        Toast.makeText(context, "Download failed", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    statusText += "\n[CRITICAL] Execution Error: ${e.message}"
                } finally {
                    isProcessing = false
                }
            }
        }
    }

    // --- END HANDLERS ---

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        GfxBackendManager.checkAndRefreshStatus()
    }

    LaunchedEffect(isConnected) {
        if (!isConnected && Shizuku.pingBinder()) GfxBackendManager.checkAndRefreshStatus()
    }

    val infiniteTransition = rememberInfiniteTransition(label = "Dashboard")
    val color1 by infiniteTransition.animateColor(
        initialValue = Color(0xFF001F3F), targetValue = Color(0xFF3F0000),
        animationSpec = infiniteRepeatable(tween(4000), RepeatMode.Reverse), label = "C1"
    )
    val color2 by infiniteTransition.animateColor(
        initialValue = Color(0xFF00003F), targetValue = Color(0xFF800000),
        animationSpec = infiniteRepeatable(tween(4000, 1000), RepeatMode.Reverse), label = "C2"
    )

    val neonAlpha by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
        label = "NeonAlpha"
    )

    val showCursor by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse),
        label = "Cursor"
    )

    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(color1, color2, Color.Black)))) {
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Image(
                    painter = painterResource(id = R.mipmap.ic_launcher_foreground),
                    contentDescription = "Spider Logo",
                    modifier = Modifier
                        .size(100.dp)
                        .padding(bottom = 16.dp),
                    contentScale = ContentScale.Fit
                )

                // Shizuku Status Card
                Card(
                    modifier = Modifier.fillMaxWidth().shadow(16.dp, CutCornerShape(topStart = 8.dp, bottomEnd = 8.dp))
                        .alpha(if (isProcessing) 1f else neonAlpha),
                    colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.7f)),
                    shape = CutCornerShape(topStart = 8.dp, bottomEnd = 8.dp),
                    border = BorderStroke(1.dp, Color(0xFFFF3131).copy(alpha = 0.4f))
                ) {
                    Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(if (isConnected) Color.Green else Color.Red))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(if (isConnected) "SYSTEM: SHIZUKU ONLINE" else "SYSTEM: OFFLINE", color = Color.White, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Surface(
                    color = Color(0xFFFF3131).copy(alpha = 0.15f),
                    shape = CutCornerShape(topStart = 4.dp, bottomEnd = 4.dp),
                    border = BorderStroke(1.dp, Color(0xFFFF3131).copy(alpha = 0.5f))
                ) {
                    Text("NANO-COOLING V4.5 ACTIVE", color = Color(0xFFFF3131), fontSize = 12.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp), letterSpacing = 2.sp)
                }
                
                Spacer(modifier = Modifier.height(32.dp))

                // --- SECONDARY GRID (Versions) ---
                Text(
                    text = "TARGET SELECTION GRID",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                VersionSelector(
                    selectedVersion = selectedVersion,
                    onVersionSelected = onVersionSelected
                )

                Spacer(modifier = Modifier.height(32.dp))

                // --- HERO ACTIONS ---
                Text(
                    text = "PRIMARY HARDWARE OVERRIDES",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                HeroHardwareButton(
                    text = "120 FPS UNLOCKER",
                    subText = "FORCE SYSTEM OVERCLOCK",
                    enabled = isConnected,
                    onClick = on120FpsClick,
                    infiniteTransition = infiniteTransition
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                HeroHardwareButton(
                    text = "ACTIVATE LAG FIX",
                    subText = "WIPE THERMAL LIMITS",
                    enabled = isConnected,
                    onClick = onLagFixClick,
                    infiniteTransition = infiniteTransition
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // --- TERMINAL / VISUALIZER ---
            Box(
                contentAlignment = Alignment.Center, 
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (isProcessing) 220.dp else 120.dp)
            ) {
                if (!isProcessing) {
                    MorphingWebMask(infiniteTransition)
                }

                if (isProcessing) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight()
                            .border(1.dp, Color(0xFFFF3131), CutCornerShape(topStart = 8.dp, bottomEnd = 8.dp)),
                        colors = CardDefaults.cardColors(containerColor = Color.Black)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp)
                                .background(Color.Black)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = statusText + if (showCursor > 0.5f) "_" else " ",
                                color = Color(0xFFFF3131),
                                fontSize = 12.sp,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Social Icons in Grid
            @OptIn(ExperimentalLayoutApi::class)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                maxItemsInEachRow = 3
            ) {
                SocialIcon("YouTube", "https://youtube.com/@gfxgamerlab_1", Color.Red)
                Spacer(modifier = Modifier.width(8.dp))
                SocialIcon("Telegram", "https://t.me/gfxgamerlab", Color(0xFF24A1DE))
                Spacer(modifier = Modifier.width(8.dp))
                SocialIcon("WhatsApp", "https://whatsapp.com/channel/0029VbB1E5VFsn0o8MNoAp2l", Color(0xFF25D366))
            }
        }
    }
}

@Composable
fun MorphingWebMask(infiniteTransition: InfiniteTransition) {
    val scaleState = infiniteTransition.animateFloat(
        initialValue = 0.8f, targetValue = 1.2f,
        animationSpec = infiniteRepeatable(tween(3000), RepeatMode.Reverse), label = "WebScale"
    )
    val morphState = infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3000), RepeatMode.Reverse), label = "WebMorph"
    )

    Canvas(modifier = Modifier
        .size(300.dp)
        .graphicsLayer {
            scaleX = scaleState.value
            scaleY = scaleState.value
            alpha = 0.3f
        }
    ) {
        val center = Offset(size.width / 2, size.height / 2)
        val morph = morphState.value
        for (i in 0 until 8) {
            val angle = (i * 45).toDouble() * Math.PI / 180
            drawLine(
                color = Color.White,
                start = center,
                end = Offset(
                    center.x + (size.width/2 * Math.cos(angle)).toFloat(),
                    center.y + (size.height/2 * Math.sin(angle)).toFloat()
                ),
                strokeWidth = 2f
            )
        }
        val path = Path()
        val width = size.width * (0.3f + 0.1f * morph)
        val height = size.height * (0.4f - 0.1f * morph)
        path.addOval(androidx.compose.ui.geometry.Rect(center.x - width, center.y - height, center.x + width, center.y + height))
        drawPath(path, Color.Red.copy(alpha = 0.5f * morph), style = Stroke(4f))
    }
}

@Composable
fun HeroHardwareButton(
    text: String,
    subText: String,
    enabled: Boolean,
    onClick: () -> Unit,
    infiniteTransition: InfiniteTransition
) {
    val glowColorState = infiniteTransition.animateColor(
        initialValue = Color(0xFF330000),
        targetValue = Color(0xFFFF3131),
        animationSpec = infiniteRepeatable(tween(2000), RepeatMode.Reverse),
        label = "Glow"
    )
    val glowScaleState = infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse),
        label = "HeroScale"
    )

    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .graphicsLayer {
                val scale = if (enabled) glowScaleState.value else 1f
                scaleX = scale
                scaleY = scale
            }
            .shadow(
                elevation = if (enabled) 12.dp else 0.dp,
                shape = CutCornerShape(topStart = 8.dp, bottomEnd = 8.dp),
                spotColor = Color.Red
            ),
        shape = CutCornerShape(topStart = 8.dp, bottomEnd = 8.dp),
        color = Color.Transparent,
        border = BorderStroke(
            width = 1.dp,
            color = if (enabled) glowColorState.value else Color.Gray.copy(alpha = 0.5f)
        )
    ) {
        val glowColor = glowColorState.value
        Box(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF1A1A1A), Color(0xFF0A0A0A))
                    )
                )
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Column {
                Text(
                    text = text,
                    color = if (enabled) Color.White else Color.Gray,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
                Text(
                    text = subText,
                    color = if (enabled) Color(0xFFFF3131).copy(alpha = 0.7f) else Color.DarkGray,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
            }
            
            // Decorative Right Edge Indicator
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(4.dp)
                    .fillMaxHeight(0.6f)
                    .background(if (enabled) glowColor else Color.DarkGray)
            )
        }
    }
}

@Composable
fun SocialIcon(name: String, url: String, color: Color) {
    val context = LocalContext.current
    Surface(
        modifier = Modifier
            .padding(vertical = 4.dp)
            .clip(CutCornerShape(topStart = 4.dp, bottomEnd = 4.dp))
            .clickable { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) },
        color = Color.Black.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.5f)),
        shape = CutCornerShape(topStart = 4.dp, bottomEnd = 4.dp)
    ) {
        Text(
            text = name,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp
        )
    }
}

@Composable
fun VersionSelector(
    selectedVersion: PubgVersion,
    onVersionSelected: (PubgVersion) -> Unit
) {
    val versions = listOf(
        PubgVersion("BGMI", "com.pubg.imobile"),
        PubgVersion("GLOBAL", "com.tencent.ig"),
        PubgVersion("KR", "com.pubg.krmobile"),
        PubgVersion("VN", "com.vng.pubgmobile"),
        PubgVersion("TW", "com.pubg.twmobile")
    )

    @OptIn(ExperimentalLayoutApi::class)
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        maxItemsInEachRow = 3
    ) {
        versions.forEach { version ->
            val isSelected = selectedVersion.packageName == version.packageName
            VersionChip(
                text = version.name,
                isSelected = isSelected,
                onClick = { onVersionSelected(version) }
            )
        }
    }
}

@Composable
fun VersionChip(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .padding(4.dp)
            .height(45.dp)
            .widthIn(min = 90.dp)
            .clip(CutCornerShape(topStart = 6.dp, bottomEnd = 6.dp))
            .clickable { onClick() },
        color = if (isSelected) Color(0xFF2A0000) else Color.Black.copy(alpha = 0.6f),
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) Color(0xFFFF3131) else Color.Gray.copy(alpha = 0.3f)
        ),
        shape = CutCornerShape(topStart = 6.dp, bottomEnd = 6.dp)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = if (isSelected) Color.White else Color.Gray,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
        }
    }
}
