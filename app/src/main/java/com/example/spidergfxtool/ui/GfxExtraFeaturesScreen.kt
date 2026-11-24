package com.example.spidergfxtool.ui

import android.widget.Toast
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.spidergfxtool.GfxBackendManager
import com.example.spidergfxtool.SoundEffectsManager
import com.example.spidergfxtool.AdsManager
import com.example.spidergfxtool.SystemTelemetryViewModel
import com.example.spidergfxtool.ui.theme.TerminalTextStyle
import com.example.spidergfxtool.ui.components.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.geometry.Offset
import java.util.Locale

@Composable
fun GfxExtraFeaturesScreen() {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val telemetryViewModel = remember { SystemTelemetryViewModel() }
    val prefs = remember { context.getSharedPreferences("spider_gfx_prefs", android.content.Context.MODE_PRIVATE) }
    val scope = rememberCoroutineScope()
    val shizukuState by GfxBackendManager.shizukuState
    val (ringColor, statusLabel) = when (shizukuState.status) {
        com.example.spidergfxtool.ShizukuStatus.ONLINE -> Pair(ElectricCyan, "ONLINE")
        com.example.spidergfxtool.ShizukuStatus.PERMISSION_NEEDED -> Pair(Color(0xFFFF9100), "NEED PERM")
        com.example.spidergfxtool.ShizukuStatus.OFFLINE -> Pair(NeonCrimson, "OFFLINE")
    }
    val isConnected = shizukuState.isGranted
    val telemetryState by telemetryViewModel.state
    
    var isProcessing by remember { mutableStateOf(false) }
    
    var statusText by remember { mutableStateOf("IDLE: SYSTEM READY\n>> WAITING FOR INPUT...") }
    
    // Smooth Slider State (Hoisted to prevent jumps)
    var resScale by rememberSaveable { mutableStateOf(prefs.getFloat("ipad_view_scale", 1.0f)) }

    // Start telemetry monitoring
    DisposableEffect(Unit) {
        telemetryViewModel.startMonitoring(context)
        onDispose { telemetryViewModel.stopMonitoring() }
    }

    val triggerFeedback = { isOn: Boolean ->
        if (isOn) SoundEffectsManager.playOn() else SoundEffectsManager.playOff()
        SoundEffectsManager.performHaptic(haptic)
    }

    val infiniteTransition = rememberInfiniteTransition(label = "ExtraFeatures")
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

    val backgroundBrush = remember(color1, color2) { Brush.verticalGradient(listOf(color1, color2, Color.Black)) }

    Box(modifier = Modifier.fillMaxSize().background(backgroundBrush)) {
        HudGridBackground()
        // Reuse spider web for atmosphere
        AnimatedSpiderWeb(infiniteTransition)

        Row(modifier = Modifier.fillMaxSize()) {
            // --- LEFT COLUMN: EXTRA TWEAKS (Left Thumb) ---
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1.1f)
                    .padding(start = 12.dp, top = 54.dp, bottom = 12.dp, end = 12.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "ADVANCED HARDWARE MODS",
                    color = ElectricCyan.copy(alpha = 0.8f),
                    style = TerminalTextStyle,
                    fontSize = 12.sp,
                    letterSpacing = 2.sp
                )
                
                Spacer(modifier = Modifier.height(10.dp))

                // IPAD VIEW SECTION
                CyberTweakCard(title = "IPAD VIEW & ASPECT RATIO", icon = Icons.Outlined.AspectRatio, statusHex = "0xIPD1") {
                    Column(modifier = Modifier.padding(horizontal = 4.dp)) {
                        Text(
                            text = "IPAD VIEW SCALE: ${(resScale * 100).toInt()}%",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Slider(
                            value = resScale,
                            onValueChange = { resScale = it },
                            valueRange = 0.7f..1.0f,
                            colors = SliderDefaults.colors(
                                thumbColor = NeonCrimson,
                                activeTrackColor = NeonCrimson,
                                inactiveTrackColor = Color.DarkGray
                            ),
                            modifier = Modifier.fillMaxWidth().height(24.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Button(
                                onClick = {
                                    triggerFeedback(true)
                                    if (!isConnected) {
                                        Toast.makeText(context, "Shizuku not connected. Please connect Shizuku.", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    scope.launch {
                                        isProcessing = true
                                        statusText = "[SYSTEM] Applying Hardware Aspect Override..."
                                        GfxBackendManager.applyWidthIpadView(context, resScale) { log ->
                                            statusText += "\n$log"
                                        }
                                        prefs.edit().putFloat("ipad_view_scale", resScale).apply()
                                        isProcessing = false
                                        triggerFeedback(false)
                                    }
                                },
                                modifier = Modifier.weight(1f).height(38.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = NeonCrimson.copy(alpha = 0.8f)),
                                shape = CutCornerShape(8.dp)
                            ) {
                                Text("APPLY IPAD VIEW", style = TerminalTextStyle, fontSize = 9.sp)
                            }
                            
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            Button(
                                onClick = {
                                    triggerFeedback(true)
                                    if (!isConnected) {
                                        Toast.makeText(context, "Shizuku not connected. Please connect Shizuku.", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    scope.launch {
                                        isProcessing = true
                                        statusText = "[SYSTEM] Restoring Display Resolution & Density..."
                                        GfxBackendManager.globalReset()
                                        GfxBackendManager.runShell(null, "wm size reset && wm density reset")
                                        delay(500)
                                        statusText += "\n[SUCCESS] wm size reset & wm density reset executed."
                                        resScale = 1.0f
                                        prefs.edit().putFloat("ipad_view_scale", 1.0f).apply()
                                        isProcessing = false
                                        Toast.makeText(context, "Display Resolution & Density Reset to Stock!", Toast.LENGTH_SHORT).show()
                                        triggerFeedback(false)
                                    }
                                },
                                modifier = Modifier.weight(1f).height(38.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray.copy(alpha = 0.5f)),
                                shape = CutCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                            ) {
                                Text("RESET RESOLUTION", style = TerminalTextStyle, fontSize = 8.sp, textAlign = TextAlign.Center)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // --- ADVANCED SYSTEM DASHBOARD ---
                Text(
                    text = "ADVANCED SYSTEM DASHBOARD",
                    color = ElectricCyan.copy(alpha = 0.8f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Dashboard Grid (Expanded with new metrics)
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // 1. BATTERY ENGINE
                    TelemetryWideCard(
                        title = "ADVANCED BATTERY ENGINE",
                        icon = Icons.Outlined.BatteryStd,
                        value = "${telemetryState.batteryPercent}% (${telemetryState.batteryHealth})",
                        progress = telemetryState.batteryPercent / 100f,
                        label = "HEALTH: ${telemetryState.batteryHealth} | VOLT: ${telemetryState.batteryVoltage} | TECH: ${telemetryState.batteryTech}"
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        // 2. DISPLAY ENGINE
                        TelemetryMiniCard(
                            modifier = Modifier.weight(1f),
                            title = "DISPLAY ENGINE",
                            data = listOf(
                                "RESOLUTION" to telemetryState.resolution,
                                "DENSITY" to telemetryState.density,
                                "HDR" to telemetryState.hdrStatus
                            ),
                            icon = Icons.Outlined.Monitor
                        )
                        // 3. PERFORMANCE ENGINE
                        TelemetryMiniCard(
                            modifier = Modifier.weight(1f),
                            title = "PERFORMANCE",
                            data = listOf(
                                "LIVE FPS" to "${telemetryState.liveFps}",
                                "LATENCY" to "${telemetryState.livePingMs}ms",
                                "THROTTLE" to telemetryState.thermalStatus
                            ),
                            icon = Icons.Outlined.Speed,
                            highlight = true
                        )
                    }

                    // 4. CPU & GPU ENGINE
                    TelemetryWideCard(
                        title = "CPU & GPU HARDWARE ENGINE",
                        icon = Icons.Outlined.Hardware,
                        value = "${telemetryState.cpuCores} CORES ACTIVE",
                        progress = 1.0f,
                        label = "RENDERER: ${telemetryState.gpuRenderer} | ARCH: ${telemetryState.cpuArch}"
                    )

                    // 5. MEMORY MONITOR
                    TelemetryWideCard(
                        title = "MEMORY & RESOURCE MONITOR",
                        icon = Icons.Outlined.Memory,
                        value = "${String.format(Locale.US, "%.2f", telemetryState.totalRamGb - telemetryState.availRamGb)}GB / ${String.format(Locale.US, "%.2f", telemetryState.totalRamGb)}GB",
                        progress = telemetryState.ramUsagePercent,
                        label = "RAM USAGE | STORAGE FREE: ${telemetryState.freeStorageGb}GB"
                    )
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                GlobalCommunityFooter()

                Spacer(modifier = Modifier.height(100.dp))
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
                            color = ringColor,
                            startAngle = 0f,
                            sweepAngle = 280f,
                            useCenter = false,
                            style = Stroke(width = 4f, cap = StrokeCap.Round)
                        )
                    }
                    
                    Surface(
                        modifier = Modifier
                            .size(70.dp)
                            .shadow(20.dp, CircleShape, spotColor = ringColor)
                            .clickable { GfxBackendManager.handleCoreLinkClick(context) },
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.8f),
                        border = BorderStroke(2.dp, ringColor)
                    ) {
                        Column(
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = statusLabel,
                                color = ringColor,
                                fontSize = if (statusLabel.length > 6) 10.sp else 12.sp,
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
                        text = "SECURE ENGINE PROTOCOL v9.0",
                        color = Color.White.copy(alpha = 0.6f),
                        style = TerminalTextStyle,
                        fontSize = 8.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}

@Composable
fun TelemetryMiniCard(
    modifier: Modifier = Modifier,
    title: String,
    data: List<Pair<String, String>>,
    icon: ImageVector,
    highlight: Boolean = false
) {
    Card(
        modifier = modifier.border(1.dp, if (highlight) NeonCrimson else Color.White.copy(alpha = 0.1f), CutCornerShape(8.dp)),
        colors = CardDefaults.cardColors(containerColor = CyberCardDark.copy(alpha = 0.8f)),
        shape = CutCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = icon, contentDescription = null, tint = if (highlight) NeonCrimson else Color.White, modifier = Modifier.size(12.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = title, color = if (highlight) NeonCrimson else Color.White.copy(alpha = 0.6f), fontSize = 8.sp, fontWeight = FontWeight.Black)
            }
            Spacer(modifier = Modifier.height(4.dp))
            data.forEach { (label, value) ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = label, color = Color.Gray, fontSize = 7.sp, fontWeight = FontWeight.Bold)
                    Text(text = value, color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Black, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                }
            }
        }
    }
}

@Composable
fun TelemetryWideCard(
    title: String,
    icon: ImageVector,
    value: String,
    progress: Float,
    label: String
) {
    Card(
        modifier = Modifier.fillMaxWidth().border(1.dp, Color.White.copy(alpha = 0.1f), CutCornerShape(8.dp)),
        colors = CardDefaults.cardColors(containerColor = CyberCardDark.copy(alpha = 0.8f)),
        shape = CutCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = title, color = Color.White.copy(alpha = 0.6f), fontSize = 8.sp, fontWeight = FontWeight.Black)
                }
                Text(text = value, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = label, color = Color.Gray, fontSize = 7.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(3.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)),
                color = if (title.contains("BATTERY")) ElectricCyan else NeonCrimson,
                trackColor = Color.DarkGray
            )
        }
    }
}
