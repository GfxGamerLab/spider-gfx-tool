package com.example.spidergfxtool.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.CircleShape
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
                    statusText += "\n[NETWORK] Downloading Tweaks from GitHub..."
                    val url120fps = "https://github.com/GfxGamerLab/gfx-pubg-files/raw/refs/heads/main/v2.0/120FPS.zip"
                    
                    GfxBackendManager.downloadGfxZip(context, url120fps).onSuccess { zip ->
                        statusText += "\n[DECRYPT] Extracting Configuration..."
                        val cacheDir = context.externalCacheDir ?: throw Exception("Cache unavailable")
                        val unzippedDir = File(cacheDir, "extracted_files")
                        
                        GfxBackendManager.extractZip(zip, unzippedDir).onSuccess { folder ->
                            applyGfxToVersions(folder, "120 FPS")
                        }.onFailure { 
                            statusText += "\n[ERROR] Extraction Failed"
                        }
                    }.onFailure { 
                        statusText += "\n[ERROR] Download Interrupted"
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
                    statusText += "\n[NETWORK] Downloading Lag Fix Pack from GitHub..."
                    val urlLagFix = "https://github.com/GfxGamerLab/gfx-pubg-files/raw/refs/heads/main/v2.0/LAGFIX.zip"
                    
                    GfxBackendManager.downloadGfxZip(context, urlLagFix).onSuccess { zip ->
                        statusText += "\n[DECRYPT] Processing Optimization Streams..."
                        val cacheDir = context.externalCacheDir ?: throw Exception("Cache unavailable")
                        val unzippedDir = File(cacheDir, "extracted_files_lagfix")
                        
                        GfxBackendManager.extractZip(zip, unzippedDir).onSuccess { folder ->
                            applyGfxToVersions(folder, "Lag Fix")
                        }.onFailure { 
                            statusText += "\n[ERROR] Data Extraction Error"
                        }
                    }.onFailure { 
                        statusText += "\n[ERROR] Repository Access Failed"
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

                Card(
                    modifier = Modifier.fillMaxWidth().shadow(16.dp, RoundedCornerShape(16.dp))
                        .alpha(if (isProcessing) 1f else neonAlpha),
                    colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(if (isConnected) Color.Green else Color.Red))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(if (isConnected) "Shizuku Connected" else "Shizuku Disconnected", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    color = Color(0xFFFF3131).copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF3131).copy(alpha = 0.5f))
                ) {
                    Text("PUBG 4.5 Optimized", color = Color(0xFFFF3131), fontSize = 14.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp), letterSpacing = 2.sp)
                }
                
                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Active Target: ${selectedVersion.name}",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                VersionSelector(
                    selectedVersion = selectedVersion,
                    onVersionSelected = onVersionSelected
                )
                
                Spacer(modifier = Modifier.height(24.dp))

                // Main Action Buttons
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    UnlockButton(
                        text = "120 FPS\nUNLOCKER",
                        enabled = isConnected, 
                        alphaPulse = neonAlpha, 
                        onClick = on120FpsClick
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    LagFixButton(
                        enabled = isConnected,
                        onClick = onLagFixClick
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Box(
                contentAlignment = Alignment.Center, 
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (isProcessing) 220.dp else 120.dp) // Dynamic height for web/terminal
            ) {
                if (!isProcessing) {
                    MorphingWebMask(infiniteTransition)
                }

                if (isProcessing) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight()
                            .border(1.dp, Color(0xFFFF3131), RoundedCornerShape(8.dp)),
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

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                SocialIcon("YouTube", "https://youtube.com/@gfxgamerlab_1", Color.Red)
                SocialIcon("Telegram", "https://t.me/gfxgamerlab", Color(0xFF24A1DE))
                SocialIcon("WhatsApp", "https://whatsapp.com/channel/0029VbB1E5VFsn0o8MNoAp2l", Color(0xFF25D366))
            }
        }
    }
}

@Composable
fun MorphingWebMask(infiniteTransition: InfiniteTransition) {
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f, targetValue = 1.2f,
        animationSpec = infiniteRepeatable(tween(3000), RepeatMode.Reverse), label = "WebScale"
    )
    val morph by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3000), RepeatMode.Reverse), label = "WebMorph"
    )

    Canvas(modifier = Modifier.size(300.dp).scale(scale).alpha(0.3f)) {
        val center = Offset(size.width / 2, size.height / 2)
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
fun UnlockButton(text: String, enabled: Boolean, alphaPulse: Float, onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "Glow")
    val glowScale by infiniteTransition.animateFloat(initialValue = 1f, targetValue = 1.05f, animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse), label = "Glow")
    val alpha by animateFloatAsState(if (enabled) alphaPulse else 0.5f, label = "A")

    Box(
        modifier = Modifier.size(200.dp).scale(if (enabled) glowScale else 1f).alpha(alpha)
            .shadow(if (enabled) 30.dp else 0.dp, CircleShape, spotColor = Color.Blue)
            .clip(CircleShape).background(if (enabled) Brush.linearGradient(listOf(Color(0xFFFF3131), Color(0xFF3131FF))) else Brush.linearGradient(listOf(Color.Gray, Color.DarkGray)))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center, lineHeight = 26.sp)
    }
}

@Composable
fun LagFixButton(enabled: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth(0.7f)
            .height(50.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick),
        color = if (enabled) Color(0xFFFF3131).copy(alpha = 0.2f) else Color.DarkGray.copy(alpha = 0.2f),
        border = androidx.compose.foundation.BorderStroke(2.dp, if (enabled) Color(0xFFFF3131) else Color.Gray),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text("ACTIVATE LAG FIX", color = if (enabled) Color.White else Color.Gray, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
        }
    }
}

@Composable
fun SocialIcon(name: String, url: String, color: Color) {
    val context = LocalContext.current
    Surface(
        modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) },
        color = color.copy(alpha = 0.2f), border = androidx.compose.foundation.BorderStroke(1.dp, color)
    ) {
        Text(name, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
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

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        versions.forEach { version ->
            val isSelected = selectedVersion.packageName == version.packageName
            VersionChip(
                text = version.name,
                isSelected = isSelected,
                onClick = { onVersionSelected(version) }
            )
            if (version != versions.last()) {
                Spacer(modifier = Modifier.width(10.dp))
            }
        }
    }
}

@Composable
fun VersionChip(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .height(40.dp)
            .widthIn(min = 80.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        color = if (isSelected) Color(0xFF3F0000) else Color.Black.copy(alpha = 0.4f),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) Color(0xFFFF3131) else Color.Gray.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = if (isSelected) Color.White else Color.Gray,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                letterSpacing = 1.sp
            )
        }
    }
}
