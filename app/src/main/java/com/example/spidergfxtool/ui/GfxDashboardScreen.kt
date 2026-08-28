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
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.example.spidergfxtool.GfxBackendManager
import com.example.spidergfxtool.GfxConfigManager
import com.example.spidergfxtool.R
import com.example.spidergfxtool.SoundEffectsManager
import com.example.spidergfxtool.AdsManager
import com.example.spidergfxtool.ui.components.*
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
    val haptic = LocalHapticFeedback.current
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

    // Central Feedback Helper
    val triggerFeedback = { isOn: Boolean ->
        if (isOn) SoundEffectsManager.playOn() else SoundEffectsManager.playOff()
        SoundEffectsManager.performHaptic(haptic)
    }

    // --- MAPPED ACTION HANDLERS ---
    
    val onVersionSelected: (PubgVersion) -> Unit = { version ->
        triggerFeedback(true)
        selectedVersion = version
        statusText = "[SYSTEM] Selected Target: ${version.name}"
    }

    val onApplyFps: (Int) -> Unit = { fps ->
        triggerFeedback(true)
        if (!isConnected) {
            Toast.makeText(context, "Please connect Shizuku first", Toast.LENGTH_SHORT).show()
        } else {
            scope.launch {
                isProcessing = true
                try {
                    val fpsLabel = "${fps}FPS"
                    statusText = "[SYSTEM] Initializing $fpsLabel v0.5 Engine..."
                    
                    GfxConfigManager.applyFpsConfig(
                        fps = fpsLabel,
                        targetPackage = selectedVersion.packageName,
                        context = context
                    ) { log ->
                        statusText += "\n$log"
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

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(1500), RepeatMode.Reverse), label = "Neon"
    )

    val radarRotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Restart), label = "Radar"
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
                    .padding(start = 24.dp, top = 12.dp, bottom = 12.dp, end = 12.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // APP BRANDING LOGO
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(bottom = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Subtle Glow
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .blur(20.dp)
                            .background(Color(0xFFFF3131).copy(alpha = 0.2f), CircleShape)
                    )
                    
                    Image(
                        painter = painterResource(id = R.mipmap.ic_launcher_foreground),
                        contentDescription = "Spider Logo",
                        modifier = Modifier.size(60.dp),
                        contentScale = ContentScale.Fit
                    )
                }

                Text(
                    text = "TACTICAL OVERRIDES",
                    color = ElectricCyan.copy(alpha = 0.8f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )
                
                Spacer(modifier = Modifier.height(10.dp))

                // Version Selector Grid
                VersionSelector(
                    selectedVersion = selectedVersion,
                    onVersionSelected = onVersionSelected
                )

                Spacer(modifier = Modifier.height(14.dp))

                // --- FPS SELECTION ENGINE ---
                Text(
                    text = "FPS SELECTION ENGINE",
                    color = ElectricCyan.copy(alpha = 0.8f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    HudTweakButton(
                        text = "APPLY 60 FPS",
                        subText = "STABLE SMOOTH PERFORMANCE",
                        glowColor = ElectricCyan,
                        alphaPulse = glowAlpha,
                        enabled = isConnected && !isProcessing,
                        onClick = { onApplyFps(60) }
                    )

                    HudTweakButton(
                        text = "APPLY 90 FPS",
                        subText = "HIGH REFRESH OVERDRIVE",
                        glowColor = ElectricCyan,
                        alphaPulse = glowAlpha,
                        enabled = isConnected && !isProcessing,
                        onClick = { onApplyFps(90) }
                    )

                    HudTweakButton(
                        text = "APPLY 120 FPS",
                        subText = "ULTIMATE ELITE FLUIDITY",
                        glowColor = ElectricCyan,
                        alphaPulse = glowAlpha,
                        enabled = isConnected && !isProcessing,
                        onClick = { onApplyFps(120) }
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // --- TACTICAL RAM SWEEPER ---
                Text(
                    text = "TACTICAL RAM SWEEPER",
                    color = ElectricCyan.copy(alpha = 0.8f),
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
                    CyberpunkToggle(
                        checked = includeSystemApps,
                        onCheckedChange = { 
                            triggerFeedback(it)
                            includeSystemApps = it
                            prefs.edit().putBoolean("ram_include_system", it).apply()
                        },
                        activeColor = NeonCrimson
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = {
                            AdsManager.showInterstitial(context as android.app.Activity) {
                                triggerFeedback(true)
                                if (!isConnected) {
                                    Toast.makeText(context, "Connect Shizuku first", Toast.LENGTH_SHORT).show()
                                    return@showInterstitial
                                }
                                scope.launch {
                                    isProcessing = true
                                    try {
                                        statusText = "[SYSTEM] Initiating Tactical RAM Sweep..."
                                        GfxBackendManager.runRamSweeper(
                                            context = context,
                                            includeSystem = includeSystemApps,
                                            whitelist = currentWhitelist
                                        ) { log ->
                                            statusText += "\n$log"
                                        }
                                        SoundEffectsManager.playOff() // Success/Finish sound
                                    } catch (e: Exception) {
                                        statusText += "\n[ERROR] RAM Sweep Failed: ${e.message}"
                                    } finally {
                                        isProcessing = false
                                    }
                                }
                            }
                        },
                        modifier = Modifier.weight(1f).height(50.dp).graphicsLayer {
                            alpha = if (isConnected && !isProcessing) 1f else 0.5f
                        },
                        enabled = isConnected && !isProcessing,
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCrimson),
                        shape = CutCornerShape(topStart = 8.dp, bottomEnd = 8.dp)
                    ) {
                        Text("KILL APPS", fontWeight = FontWeight.Black)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = { 
                            triggerFeedback(true)
                            showWhitelist = true 
                        },
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

                Spacer(modifier = Modifier.height(32.dp))
                GlobalCommunityFooter()
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
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(100.dp)) {
                    Canvas(modifier = Modifier.fillMaxSize().graphicsLayer { rotationZ = radarRotation }) {
                        drawArc(
                            color = if (isConnected) ElectricCyan else NeonCrimson,
                            startAngle = 0f,
                            sweepAngle = 280f,
                            useCenter = false,
                            style = Stroke(width = 4f, cap = StrokeCap.Round)
                        )
                    }
                    
                    Surface(
                        modifier = Modifier.size(70.dp).shadow(20.dp, CircleShape, spotColor = if (isConnected) ElectricCyan else NeonCrimson),
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.8f),
                        border = BorderStroke(2.dp, if (isConnected) ElectricCyan else NeonCrimson)
                    ) {
                        Column(
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (isConnected) "ONLINE" else "OFFLINE",
                                color = if (isConnected) ElectricCyan else NeonCrimson,
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

                Spacer(modifier = Modifier.height(14.dp))

                // SYSTEM TERMINAL
                val terminalScrollState = rememberScrollState()
                Card(
                    modifier = Modifier.fillMaxWidth().weight(1f).border(1.dp, NeonCrimson.copy(alpha = 0.3f), CutCornerShape(topStart = 12.dp, bottomEnd = 12.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.6f)),
                    shape = CutCornerShape(topStart = 12.dp, bottomEnd = 12.dp)
                ) {
                    Column(modifier = Modifier.fillMaxSize().padding(12.dp).verticalScroll(terminalScrollState)) {
                        TypewriterText(
                            text = statusText,
                            color = NeonCrimson,
                            scrollState = terminalScrollState
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
                        text = "SECURE ENGINE PROTOCOL v0.5",
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
            .height(56.dp)
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
            .height(36.dp)
            .widthIn(min = 90.dp)
            .clip(CutCornerShape(topStart = 6.dp, bottomEnd = 6.dp))
            .clickable { onClick() },
        color = if (isSelected) Color(0xFF2A0000) else Color.Black.copy(alpha = 0.6f),
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) NeonCrimson else Color.Gray.copy(alpha = 0.3f)
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
