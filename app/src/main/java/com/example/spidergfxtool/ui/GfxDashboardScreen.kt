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
import androidx.compose.ui.graphics.StrokeCap
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
import com.example.spidergfxtool.ui.components.AnimatedSpiderWeb
import com.example.spidergfxtool.ui.components.SocialIcon
import com.example.spidergfxtool.ui.components.WhitelistOverlay
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
    val prefs = remember { context.getSharedPreferences("spider_gfx_prefs", android.content.Context.MODE_PRIVATE) }
    val scope = rememberCoroutineScope()
    val isConnected = GfxBackendManager.isShizukuConnected.value
    var isProcessing by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("") }
    
    val detectedVersions = remember(context) { getDetectedVersions(context) }
    var selectedVersion by remember { 
        mutableStateOf(detectedVersions.firstOrNull() ?: PubgVersion("GLOBAL", "com.tencent.ig")) 
    }

    // RAM Sweeper States
    var showWhitelist by remember { mutableStateOf(false) }
    var includeSystemApps by remember { mutableStateOf(prefs.getBoolean("ram_include_system", false)) }
    var currentWhitelist by remember { 
        mutableStateOf(prefs.getStringSet("ram_whitelist", emptySet()) ?: emptySet()) 
    }

    // --- MAPPED ACTION HANDLERS ---
    
    val onVersionSelected: (PubgVersion) -> Unit = { version ->
        selectedVersion = version
        statusText = "[SYSTEM] Selected Target: ${version.name}"
    }

    val onApplyFps: (Int) -> Unit = { fps ->
        if (!isConnected) {
            Toast.makeText(context, "Please connect Shizuku first", Toast.LENGTH_SHORT).show()
        } else {
            scope.launch {
                isProcessing = true
                try {
                    val url = when(fps) {
                        60 -> "https://github.com/GfxGamerLab/gfx-pubg-files/releases/download/v2.0-fps/60FPS.zip"
                        90 -> "https://github.com/GfxGamerLab/gfx-pubg-files/releases/download/v2.0-fps/90FPS.zip"
                        else -> "https://github.com/GfxGamerLab/gfx-pubg-files/releases/download/v2.0-fps/120FPS.zip"
                    }
                    val fileName = "${fps}FPS.zip"
                    
                    statusText = "[SYSTEM] Initializing $fps FPS Engine..."
                    GfxBackendManager.applyFpsConfig(
                        context = context,
                        downloadUrl = url,
                        fileName = fileName,
                        targetPackage = selectedVersion.packageName
                    ) { log ->
                        statusText += "\n$log"
                    }.onSuccess {
                        statusText += "\n[SUCCESS] $fps FPS Engine Activated!"
                        Toast.makeText(context, "$fps FPS Optimized!", Toast.LENGTH_LONG).show()
                    }.onFailure { e ->
                        statusText += "\n[ERROR] Operation Failed: ${e.message}"
                        Toast.makeText(context, "Failed to apply $fps FPS", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    statusText += "\n[CRITICAL] Error: ${e.message}"
                } finally {
                    isProcessing = false
                }
            }
        }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        GfxBackendManager.checkAndRefreshStatus()
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

    val neonCyan = Color(0xFF00FFFF)
    val neonRed = Color(0xFFFF3131)

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(1500), RepeatMode.Reverse), label = "Neon"
    )

    // LANDSCAPE HUD ROOT
    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(color1, color2, Color.Black)))) {
        // Overlay Layer
        if (showWhitelist) {
            WhitelistOverlay(
                onDismiss = { showWhitelist = false },
                onWhitelistChanged = { currentWhitelist = it }
            )
        }

        Row(modifier = Modifier.fillMaxSize()) {
            // --- LEFT COLUMN: TACTICAL HUD (Left Thumb) ---
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1.1f)
                    .padding(start = 24.dp, top = 24.dp, bottom = 24.dp, end = 12.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // APP BRANDING LOGO
                Image(
                    painter = painterResource(id = R.mipmap.ic_launcher_foreground),
                    contentDescription = "Spider Logo",
                    modifier = Modifier
                        .size(80.dp)
                        .padding(bottom = 12.dp)
                        .align(Alignment.CenterHorizontally),
                    contentScale = ContentScale.Fit
                )

                Text(
                    text = "TACTICAL OVERRIDES",
                    color = neonCyan.copy(alpha = 0.8f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )
                
                Spacer(modifier = Modifier.height(16.dp))

                // Version Selector Grid
                VersionSelector(
                    selectedVersion = selectedVersion,
                    onVersionSelected = onVersionSelected
                )

                Spacer(modifier = Modifier.height(24.dp))

                // --- FPS SELECTION ENGINE ---
                Text(
                    text = "FPS SELECTION ENGINE",
                    color = neonCyan.copy(alpha = 0.8f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    HudTweakButton(
                        text = "APPLY 60 FPS",
                        subText = "STABLE SMOOTH PERFORMANCE",
                        glowColor = neonCyan,
                        alphaPulse = glowAlpha,
                        enabled = isConnected && !isProcessing,
                        onClick = { onApplyFps(60) }
                    )

                    HudTweakButton(
                        text = "APPLY 90 FPS",
                        subText = "HIGH REFRESH OVERDRIVE",
                        glowColor = neonCyan,
                        alphaPulse = glowAlpha,
                        enabled = isConnected && !isProcessing,
                        onClick = { onApplyFps(90) }
                    )

                    HudTweakButton(
                        text = "APPLY 120 FPS",
                        subText = "ULTIMATE ELITE FLUIDITY",
                        glowColor = neonCyan,
                        alphaPulse = glowAlpha,
                        enabled = isConnected && !isProcessing,
                        onClick = { onApplyFps(120) }
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // --- TACTICAL RAM SWEEPER ---
                Text(
                    text = "TACTICAL RAM SWEEPER",
                    color = neonCyan.copy(alpha = 0.8f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("INCLUDE SYSTEM APPS", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Switch(
                        checked = includeSystemApps,
                        onCheckedChange = { 
                            includeSystemApps = it
                            prefs.edit().putBoolean("ram_include_system", it).apply()
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color.Red)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = {
                            if (!isConnected) {
                                Toast.makeText(context, "Connect Shizuku first", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            scope.launch {
                                isProcessing = true
                                statusText = "[SYSTEM] Initiating Tactical RAM Sweep..."
                                GfxBackendManager.runRamSweeper(
                                    context = context,
                                    includeSystem = includeSystemApps,
                                    whitelist = currentWhitelist
                                ) { log ->
                                    statusText += "\n$log"
                                }
                                isProcessing = false
                            }
                        },
                        modifier = Modifier.weight(1f).height(50.dp).graphicsLayer {
                            alpha = if (isConnected && !isProcessing) 1f else 0.5f
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                        shape = CutCornerShape(topStart = 8.dp, bottomEnd = 8.dp)
                    ) {
                        Text("KILL APPS", fontWeight = FontWeight.Black)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = { showWhitelist = true },
                        modifier = Modifier.height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray.copy(alpha = 0.5f)),
                        shape = CutCornerShape(topStart = 4.dp, bottomEnd = 4.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
                    ) {
                        Text("WHITELIST", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
                
                Spacer(modifier = Modifier.height(40.dp))
                
                // Social Links in Small HUD Style
                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SocialIcon("YouTube", "https://youtube.com/@gfxgamerlab_1", Color.Red)
                    SocialIcon("Telegram", "https://t.me/gfxgamerlab", Color(0xFF24A1DE))
                }
            }

            // --- RIGHT COLUMN: MASTER CONSOLE (Right Thumb) ---
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(0.9f)
                    .padding(start = 12.dp, top = 24.dp, bottom = 24.dp, end = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // MASTER STATUS RING
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(140.dp)) {
                    Canvas(modifier = Modifier.fillMaxSize().graphicsLayer { rotationZ = glowAlpha * 360f }) {
                        drawArc(
                            color = if (isConnected) neonCyan else neonRed,
                            startAngle = 0f,
                            sweepAngle = 280f,
                            useCenter = false,
                            style = Stroke(width = 4f, cap = StrokeCap.Round)
                        )
                    }
                    
                    Surface(
                        modifier = Modifier.size(100.dp).shadow(20.dp, CircleShape, spotColor = if (isConnected) neonCyan else neonRed),
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.8f),
                        border = BorderStroke(2.dp, if (isConnected) neonCyan else neonRed)
                    ) {
                        Column(
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (isConnected) "ONLINE" else "OFFLINE",
                                color = if (isConnected) neonCyan else neonRed,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = "CORE LINK",
                                color = Color.White.copy(alpha = 0.4f),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // SYSTEM TERMINAL
                Card(
                    modifier = Modifier.fillMaxWidth().weight(1f).border(1.dp, neonRed.copy(alpha = 0.3f), CutCornerShape(topStart = 12.dp, bottomEnd = 12.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.6f)),
                    shape = CutCornerShape(topStart = 12.dp, bottomEnd = 12.dp)
                ) {
                    Column(modifier = Modifier.fillMaxSize().padding(12.dp).verticalScroll(rememberScrollState())) {
                        Text(
                            text = if (isProcessing) statusText + "_" else statusText,
                            color = neonRed,
                            fontSize = 10.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            lineHeight = 14.sp
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Status Badge
                Surface(
                    color = Color.White.copy(alpha = 0.05f),
                    shape = CutCornerShape(topStart = 4.dp, bottomEnd = 4.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                ) {
                    Text(
                        text = "NANO-SYSTEM V4.5 ACTIVE",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}

@Composable
fun HudTweakButton(
    text: String,
    subText: String,
    glowColor: Color,
    alphaPulse: Float,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(70.dp)
            .graphicsLayer {
                alpha = if (enabled) 1f else 0.5f
            }
            .shadow(if (enabled) (10 * alphaPulse).dp else 0.dp, CutCornerShape(topStart = 12.dp, bottomEnd = 12.dp), spotColor = glowColor),
        shape = CutCornerShape(topStart = 12.dp, bottomEnd = 12.dp),
        color = Color.White.copy(alpha = 0.05f),
        border = BorderStroke(1.dp, if (enabled) glowColor.copy(alpha = alphaPulse) else Color.Gray.copy(alpha = 0.3f))
    ) {
        Box(
            modifier = Modifier.background(
                Brush.linearGradient(listOf(Color.Black.copy(alpha = 0.8f), Color.Transparent))
            ).padding(horizontal = 20.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Column {
                Text(text, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black)
                Text(subText, color = glowColor.copy(alpha = 0.6f), fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }
            
            // Slanted Decorative Edge
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(2.dp)
                    .fillMaxHeight(0.5f)
                    .background(glowColor.copy(alpha = alphaPulse))
            )
        }
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
