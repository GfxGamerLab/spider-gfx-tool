package com.example.spidergfxtool.ui

import android.widget.Toast
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.spidergfxtool.GfxBackendManager
import com.example.spidergfxtool.SystemTelemetryViewModel
import com.example.spidergfxtool.ui.components.AnimatedSpiderWeb
import com.example.spidergfxtool.ui.components.CyberTweakCard
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.geometry.Offset
import java.util.Locale

@Composable
fun GfxExtraFeaturesScreen() {
    val context = LocalContext.current
    val telemetryViewModel = remember { SystemTelemetryViewModel() }
    val prefs = remember { context.getSharedPreferences("spider_gfx_prefs", android.content.Context.MODE_PRIVATE) }
    val scope = rememberCoroutineScope()
    val isConnected = GfxBackendManager.isShizukuConnected.value
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

    val neonCyan = remember { Color(0xFF00FFFF) }
    val neonRed = remember { Color(0xFFFF3131) }
    val backgroundBrush = remember(color1, color2) { Brush.verticalGradient(listOf(color1, color2, Color.Black)) }

    Box(modifier = Modifier.fillMaxSize().background(backgroundBrush)) {
        // Reuse spider web for atmosphere
        AnimatedSpiderWeb(infiniteTransition)

        Row(modifier = Modifier.fillMaxSize()) {
            // --- LEFT COLUMN: EXTRA TWEAKS (Left Thumb) ---
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1.1f)
                    .padding(start = 24.dp, top = 24.dp, bottom = 24.dp, end = 12.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "ADVANCED HARDWARE MODS",
                    color = neonCyan.copy(alpha = 0.8f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )
                
                Spacer(modifier = Modifier.height(16.dp))

                // IPAD VIEW SECTION
                CyberTweakCard(title = "IPAD VIEW & ASPECT RATIO", icon = "📐", statusHex = "0xIPD1") {
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
                                thumbColor = neonRed,
                                activeTrackColor = neonRed,
                                inactiveTrackColor = Color.DarkGray
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Button(
                                onClick = {
                                    if (!isConnected) {
                                        Toast.makeText(context, "Connect Shizuku first", Toast.LENGTH_SHORT).show()
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
                                    }
                                },
                                modifier = Modifier.weight(1f).height(50.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = neonRed.copy(alpha = 0.8f)),
                                shape = CutCornerShape(8.dp)
                            ) {
                                Text("APPLY IPAD VIEW", fontWeight = FontWeight.Black, fontSize = 11.sp)
                            }
                            
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            Button(
                                onClick = {
                                    if (!isConnected) {
                                        Toast.makeText(context, "Connect Shizuku first", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    scope.launch {
                                        isProcessing = true
                                        statusText = "[SYSTEM] Restoring Factory Specs..."
                                        GfxBackendManager.globalReset()
                                        delay(1000)
                                        statusText += "\n[SUCCESS] Matrix Reset Completed."
                                        resScale = 1.0f
                                        prefs.edit().putFloat("ipad_view_scale", 1.0f).apply()
                                        isProcessing = false
                                    }
                                },
                                modifier = Modifier.weight(1f).height(50.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray.copy(alpha = 0.5f)),
                                shape = CutCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                            ) {
                                Text("RESET RESOLUTION", fontWeight = FontWeight.Bold, fontSize = 10.sp, textAlign = TextAlign.Center)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // --- ADVANCED SYSTEM DASHBOARD ---
                Text(
                    text = "ADVANCED SYSTEM DASHBOARD",
                    color = neonCyan.copy(alpha = 0.8f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Dashboard Grid (2 Columns)
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TelemetryMiniCard(
                            modifier = Modifier.weight(1f),
                            title = "DEVICE IDENTITY",
                            data = listOf(
                                "BRAND" to telemetryState.brand,
                                "MODEL" to telemetryState.model,
                                "OS" to "Android ${telemetryState.androidVersion}"
                            ),
                            icon = "📱"
                        )
                        TelemetryMiniCard(
                            modifier = Modifier.weight(1f),
                            title = "PERFORMANCE",
                            data = listOf(
                                "LIVE FPS" to "${telemetryState.liveFps}",
                                "LATENCY" to "${telemetryState.livePingMs}ms",
                                "ARCH" to telemetryState.cpuArch
                            ),
                            icon = "🚀",
                            highlight = true
                        )
                    }

                    TelemetryWideCard(
                        title = "MEMORY & RESOURCE MONITOR",
                        icon = "🧠",
                        value = "${String.format(Locale.US, "%.2f", telemetryState.totalRamGb - telemetryState.availRamGb)}GB / ${String.format(Locale.US, "%.2f", telemetryState.totalRamGb)}GB",
                        progress = telemetryState.ramUsagePercent,
                        label = "RAM USAGE"
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TelemetryMiniCard(
                            modifier = Modifier.weight(1f),
                            title = "POWER STATUS",
                            data = listOf(
                                "BATTERY" to "${telemetryState.batteryPercent}%",
                                "TEMP" to "${telemetryState.batteryTemp}°C"
                            ),
                            icon = "🔋"
                        )
                        TelemetryMiniCard(
                            modifier = Modifier.weight(1f),
                            title = "HARDWARE",
                            data = listOf(
                                "STORAGE" to "${telemetryState.freeStorageGb}GB Free",
                                "REFRESH" to "${(context.getSystemService(android.content.Context.WINDOW_SERVICE) as android.view.WindowManager).defaultDisplay.refreshRate.toInt()}Hz"
                            ),
                            icon = "🖥️"
                        )
                    }
                }
                
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
                // MASTER STATUS RING (Identical to Dashboard)
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

                // SYSTEM TERMINAL (Identical to Dashboard)
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
                        text = "EXTRA MODULES SYNCED",
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
fun TelemetryMiniCard(
    modifier: Modifier = Modifier,
    title: String,
    data: List<Pair<String, String>>,
    icon: String,
    highlight: Boolean = false
) {
    val neonRed = remember { Color(0xFFFF3131) }
    Card(
        modifier = modifier.border(1.dp, if (highlight) neonRed else Color.White.copy(alpha = 0.1f), CutCornerShape(8.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.4f)),
        shape = CutCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(icon, fontSize = 14.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = title, color = if (highlight) neonRed else Color.White.copy(alpha = 0.6f), fontSize = 9.sp, fontWeight = FontWeight.Black)
            }
            Spacer(modifier = Modifier.height(8.dp))
            data.forEach { (label, value) ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = label, color = Color.Gray, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    Text(text = value, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
fun TelemetryWideCard(
    title: String,
    icon: String,
    value: String,
    progress: Float,
    label: String
) {
    val neonRed = remember { Color(0xFFFF3131) }
    Card(
        modifier = Modifier.fillMaxWidth().border(1.dp, Color.White.copy(alpha = 0.1f), CutCornerShape(8.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.4f)),
        shape = CutCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(icon, fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = title, color = Color.White.copy(alpha = 0.6f), fontSize = 9.sp, fontWeight = FontWeight.Black)
                }
                Text(text = value, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black)
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(text = label, color = Color.Gray, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                color = neonRed,
                trackColor = Color.DarkGray
            )
        }
    }
}
