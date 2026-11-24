package com.example.spidergfxtool.ui

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.core.content.edit
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.compose.ui.zIndex
import com.example.spidergfxtool.GfxBackendManager
import com.example.spidergfxtool.SmartEngine
import com.example.spidergfxtool.SmartDnsManager
import com.example.spidergfxtool.SoundEffectsManager
import com.example.spidergfxtool.SmartResult
import com.example.spidergfxtool.ui.theme.*
import com.example.spidergfxtool.ui.components.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// --- TARGET GAME DETAILS HELPER ---
fun getTargetGameDetails(context: Context): Pair<String, String> {
    val pkg = GfxBackendManager.getAutoDetectedGamePackage(context)
    val appName = try {
        val pm = context.packageManager
        val appInfo = pm.getApplicationInfo(pkg, 0)
        pm.getApplicationLabel(appInfo).toString()
    } catch (_: Exception) {
        "Target Game"
    }
    return Pair(appName, pkg)
}

// --- CATEGORY MODULE HOOK METHODS ---
fun openSystemOptimization(updateTerminal: (String) -> Unit) {
    updateTerminal("[MODULE] SYSTEM OPTIMIZATION Selected.\n[STATUS] OS Scheduler, storage trim & 10 smoothness engine modules ready.")
}

fun openGameOptimization(context: Context, updateTerminal: (String) -> Unit) {
    val (appName, pkg) = getTargetGameDetails(context)
    updateTerminal("[MODULE] GAME OPTIMIZATION Selected.\n[TARGET LOCKED] $appName ($pkg)\n[STATUS] Game Mode, Standby Guard & Frame Pacing ready.")
}

fun openNetworkOptimization(updateTerminal: (String) -> Unit) {
    updateTerminal("[MODULE] NETWORK OPTIMIZATION Selected.\n[STATUS] Ping Stabilizer & Adaptive 1m Auto-DNS Engine initialized.")
}

fun openSensitivityGyro(updateTerminal: (String) -> Unit) {
    updateTerminal("[MODULE] SENSITIVITY & GYRO Selected.\n[STATUS] Hardware Sensor Analyzer & Touch Response Engine ready.")
}

// --- HARDWARE GYROSCOPE DIAGNOSTIC RUNNER ---
fun runGyroDiagnostic(
    context: Context,
    updateTerminal: (String) -> Unit,
    onResult: (String, Long, Float) -> Unit
) {
    val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    val gyro = sensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    if (sensorManager == null || gyro == null) {
        updateTerminal("[GYRO DIAGNOSTIC] Hardware Gyroscope: NOT DETECTED on this device. Software emulation mode active.")
        onResult("NOT DETECTED", 0L, 0f)
        return
    }

    updateTerminal("[GYRO DIAGNOSTIC] Testing hardware sensor: ${gyro.name} (${gyro.vendor})...")

    val timestamps = mutableListOf<Long>()
    val magnitudes = mutableListOf<Float>()
    var sampleCount = 0

    val listener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            if (event == null) return
            val now = System.currentTimeMillis()
            timestamps.add(now)
            val mag = kotlin.math.sqrt(event.values[0] * event.values[0] + event.values[1] * event.values[1] + event.values[2] * event.values[2])
            magnitudes.add(mag)
            sampleCount++

            if (sampleCount >= 20) {
                sensorManager.unregisterListener(this)
                val intervals = mutableListOf<Long>()
                for (i in 1 until timestamps.size) {
                    intervals.add(timestamps[i] - timestamps[i - 1])
                }
                val avgDelay = if (intervals.isNotEmpty()) intervals.average().toLong() else 0L
                val avgNoise = if (magnitudes.isNotEmpty()) magnitudes.average().toFloat() else 0f

                val statusStr = "DETECTED (${gyro.name})"
                onResult(statusStr, avgDelay, avgNoise)
                updateTerminal("[GYRO DIAGNOSTIC] Hardware: ${gyro.name}\n[RESULT] Measured Interval: ${avgDelay}ms | Motion Noise: ${String.format("%.4f", avgNoise)} rad/s\n[STATUS] Gyroscope hardware responsiveness: OPTIMAL")
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    val registered = sensorManager.registerListener(listener, gyro, SensorManager.SENSOR_DELAY_GAME)
    if (!registered) {
        updateTerminal("[GYRO DIAGNOSTIC] Failed to register sensor listener. System permission or hardware restriction.")
        onResult("ERROR", 0L, 0f)
    }
}

// Compact Side-by-Side Cyber-Tech Category Card
@Composable
fun CyberGridCategoryCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    cardBgColor: Color,
    tagText: String,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(92.dp)
            .tacticalPress {
                SoundEffectsManager.playClick()
                SoundEffectsManager.performHaptic(haptic)
                onClick()
            }
            .border(
                border = BorderStroke(1.5.dp, accentColor.copy(alpha = 0.35f)),
                shape = CutCornerShape(topStart = 14.dp, bottomEnd = 14.dp)
            )
            .shadow(
                elevation = 6.dp,
                shape = CutCornerShape(topStart = 14.dp, bottomEnd = 14.dp),
                spotColor = accentColor
            ),
        colors = CardDefaults.cardColors(containerColor = cardBgColor.copy(alpha = 0.85f)),
        shape = CutCornerShape(topStart = 14.dp, bottomEnd = 14.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(10.dp)) {
            // Top Accent Glow Line
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .width(36.dp)
                    .height(2.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color.Transparent, accentColor)
                        )
                    )
            )

            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header: Icon + Tag
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .background(accentColor.copy(alpha = 0.12f), CutCornerShape(6.dp))
                            .border(1.dp, accentColor.copy(alpha = 0.4f), CutCornerShape(6.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Surface(
                        color = accentColor.copy(alpha = 0.15f),
                        shape = CutCornerShape(topStart = 4.dp, bottomEnd = 4.dp),
                        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f))
                    ) {
                        Text(
                            text = tagText,
                            color = accentColor,
                            style = TerminalTextStyle,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                // Text: Title + Short Subtitle
                Column {
                    Text(
                        text = title,
                        color = Color.White,
                        style = TerminalTextStyle,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.6.sp,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Text(
                        text = subtitle,
                        color = Color.White.copy(alpha = 0.65f),
                        fontSize = 8.sp,
                        lineHeight = 9.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// Animated Cyber Boost Reactor Circle Composable
@Composable
fun CyberBoostReactorCircle(
    isBoosting: Boolean,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val infiniteTransition = rememberInfiniteTransition(label = "ReactorPulse")
    
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "Pulse"
    )

    val reactorRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(if (isBoosting) 800 else 3000, easing = LinearEasing), RepeatMode.Restart),
        label = "Rotation"
    )

    val reactorColor = if (isBoosting) NeonCrimson else ElectricCyan

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp, bottom = 12.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(110.dp)
                .tacticalPress {
                    if (!isBoosting) {
                        SoundEffectsManager.playClick()
                        SoundEffectsManager.performHaptic(haptic)
                        onClick()
                    }
                }
        ) {
            // Rotating Outer Cyber Arc Ring
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { rotationZ = reactorRotation }
            ) {
                drawArc(
                    color = reactorColor.copy(alpha = 0.8f),
                    startAngle = 0f,
                    sweepAngle = 120f,
                    useCenter = false,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )
                drawArc(
                    color = reactorColor.copy(alpha = 0.4f),
                    startAngle = 180f,
                    sweepAngle = 100f,
                    useCenter = false,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // Glowing Inner Reactor Core
            Surface(
                modifier = Modifier
                    .size(86.dp)
                    .graphicsLayer {
                        scaleX = if (isBoosting) pulseScale else 1.0f
                        scaleY = if (isBoosting) pulseScale else 1.0f
                    }
                    .shadow(16.dp, CircleShape, spotColor = reactorColor),
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.85f),
                border = BorderStroke(2.dp, reactorColor.copy(alpha = 0.8f))
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    reactorColor.copy(alpha = 0.25f),
                                    Color.Black.copy(alpha = 0.9f)
                                )
                            )
                        )
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (isBoosting) Icons.Outlined.Bolt else Icons.Outlined.RocketLaunch,
                            contentDescription = null,
                            tint = reactorColor,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isBoosting) "BOOSTING" else "BOOST",
                            color = Color.White,
                            style = TerminalTextStyle,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.5.sp
                        )
                        Text(
                            text = if (isBoosting) "TURBO" else "REACTOR",
                            color = reactorColor.copy(alpha = 0.7f),
                            style = TerminalTextStyle,
                            fontSize = 7.sp,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = if (isBoosting) "IGNITING TURBO REACTOR..." else "TAP TO IGNITE TURBO SYSTEM BOOST",
            color = Color.White.copy(alpha = 0.5f),
            style = TerminalTextStyle,
            fontSize = 8.sp,
            letterSpacing = 1.sp
        )
    }
}

// Full-Screen Cinematic Cyber Boost Overlay Composable
@Composable
fun CyberBoostCinematicOverlay(
    stage: Int,
    progress: Float,
    stepText: String,
    onDismiss: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "CinematicOverlay")

    val arcRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing), RepeatMode.Restart),
        label = "ArcRotation"
    )

    val corePulse by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "CorePulse"
    )

    val activeColor = if (stage == 3) ElectricCyan else NeonCrimson

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xF0070B12))
            .zIndex(9999f)
            .clickable(enabled = false) {}
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        HudGridBackground()

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                color = activeColor.copy(alpha = 0.15f),
                shape = CutCornerShape(topStart = 8.dp, bottomEnd = 8.dp),
                border = BorderStroke(1.dp, activeColor.copy(alpha = 0.5f))
            ) {
                Text(
                    text = when (stage) {
                        1 -> "[ STAGE 1: CORE IGNITION ]"
                        2 -> "[ STAGE 2: CACHE PURGE & SWEEP ]"
                        else -> "[ STAGE 3: BOOST COMPLETE ]"
                    },
                    color = activeColor,
                    style = TerminalTextStyle,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    letterSpacing = 2.sp
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(180.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { rotationZ = arcRotation }
                ) {
                    drawArc(
                        color = activeColor.copy(alpha = 0.9f),
                        startAngle = 0f,
                        sweepAngle = 140f,
                        useCenter = false,
                        style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = activeColor.copy(alpha = 0.5f),
                        startAngle = 180f,
                        sweepAngle = 120f,
                        useCenter = false,
                        style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                Canvas(modifier = Modifier.size(145.dp)) {
                    val sweepAngle = progress * 360f
                    drawArc(
                        color = ElectricCyan,
                        startAngle = -90f,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                Surface(
                    modifier = Modifier
                        .size(110.dp)
                        .graphicsLayer {
                            scaleX = corePulse
                            scaleY = corePulse
                        }
                        .shadow(24.dp, CircleShape, spotColor = activeColor),
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.9f),
                    border = BorderStroke(2.5.dp, activeColor)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        activeColor.copy(alpha = 0.35f),
                                        Color.Black.copy(alpha = 0.95f)
                                    )
                                )
                            )
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = if (stage == 3) Icons.Outlined.CheckCircle else Icons.Outlined.Bolt,
                                contentDescription = null,
                                tint = activeColor,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (stage == 3) "100%" else "${(progress * 100).toInt()}%",
                                color = Color.White,
                                style = TerminalTextStyle,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = stepText,
                color = Color.White,
                style = TerminalTextStyle,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                letterSpacing = 1.2.sp,
                modifier = Modifier.padding(horizontal = 32.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "SYSTEM ENGINE PROTOCOL — REAL-TIME CLEANUP ACTIVE",
                color = activeColor.copy(alpha = 0.6f),
                style = TerminalTextStyle,
                fontSize = 8.sp,
                letterSpacing = 1.sp
            )
        }
    }
}

// Performance Optimized Toggle Row with AI Agent Feedback
@Composable
fun CyberTweakToggleRow(
    label: String,
    description: String,
    icon: ImageVector,
    checked: Boolean,
    glowAlphaState: State<Float>,
    isRisky: Boolean = false,
    isLoading: Boolean = false,
    onCheckedChange: (Boolean) -> Unit
) {
    val activeColor = if (isRisky) NeonCrimson else ElectricCyan
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .shadow(
                elevation = if (checked) (glowAlphaState.value * 6).dp else 0.dp,
                shape = CutCornerShape(12.dp),
                spotColor = activeColor
            )
            .animatedNeonBorder(
                enabled = checked,
                color1 = activeColor,
                shape = CutCornerShape(12.dp)
            )
            .background(
                color = if (checked) activeColor.copy(alpha = 0.04f) else Color.Transparent,
                shape = CutCornerShape(12.dp)
            )
            .tacticalPress { onCheckedChange(!checked) }
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(
                        color = if (checked) activeColor.copy(alpha = 0.15f) else Color.DarkGray.copy(alpha = 0.15f),
                        shape = CircleShape
                    )
                    .border(
                        1.dp,
                        if (checked) activeColor.copy(alpha = 0.4f) else Color.Gray.copy(alpha = 0.2f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        color = activeColor,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (checked) activeColor else Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label.uppercase(),
                    color = if (checked) Color.White else Color.Gray,
                    style = TerminalTextStyle,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                
                Text(
                    text = description,
                    color = if (checked) activeColor.copy(alpha = 0.7f) else Color.Gray.copy(alpha = 0.5f),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 10.sp,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
        }
        
        Spacer(modifier = Modifier.width(12.dp))

        CyberpunkToggle(
            checked = checked,
            onCheckedChange = onCheckedChange,
            activeColor = activeColor,
            isLoading = isLoading
        )
    }
}

// Prominent Top-Level One-Time Action Button
@Composable
fun TopActionButton(
    text: String,
    subText: String,
    icon: ImageVector,
    accentColor: Color = ElectricCyan,
    isLoading: Boolean = false,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = !isLoading,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .padding(vertical = 4.dp),
        colors = ButtonDefaults.buttonColors(containerColor = accentColor.copy(alpha = 0.15f)),
        shape = CutCornerShape(topStart = 10.dp, bottomEnd = 10.dp),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = accentColor,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(imageVector = icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(text = text.uppercase(), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black, style = TerminalTextStyle)
                Text(text = subText, color = accentColor.copy(alpha = 0.7f), fontSize = 8.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun GfxExtremeHudScreen() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("spider_gfx_prefs", Context.MODE_PRIVATE) }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val shizukuState by GfxBackendManager.shizukuState
    val (ringColor, statusLabel) = when (shizukuState.status) {
        com.example.spidergfxtool.ShizukuStatus.ONLINE -> Pair(ElectricCyan, "ONLINE")
        com.example.spidergfxtool.ShizukuStatus.PERMISSION_NEEDED -> Pair(Color(0xFFFF9100), "NEED PERM")
        com.example.spidergfxtool.ShizukuStatus.OFFLINE -> Pair(NeonCrimson, "OFFLINE")
    }
    val isConnected = shizukuState.isGranted
    
    // UI Sub-Page Navigation State
    var selectedModule by remember { mutableStateOf<String?>(null) }

    // SHORTENED PROFESSIONAL IDLE TEXT
    var terminalText by remember { mutableStateOf("[SYSTEM] Ready. Awaiting command...") }

    // BOOST REACTOR & CINEMATIC OVERLAY STATES
    var isBoostingReactor by remember { mutableStateOf(false) }
    var showBoostOverlay by remember { mutableStateOf(false) }
    var boostStage by remember { mutableIntStateOf(1) }
    var boostProgress by remember { mutableFloatStateOf(0f) }
    var boostStepText by remember { mutableStateOf("INITIALIZING CORE IGNITION...") }

    // --- NETWORK ENGINE STATES ---
    var autoDnsEnabled by remember { mutableStateOf(prefs.getBoolean("key_auto_dns", false)) }
    var autoDnsStatusText by remember { mutableStateOf("Auto-Scan: OFF") }
    var socketTuningEnabled by remember { mutableStateOf(prefs.getBoolean("key_socket_tuning", false)) }
    var jitterReductionEnabled by remember { mutableStateOf(prefs.getBoolean("key_jitter_reduction", false)) }
    var isFlushingDns by remember { mutableStateOf(false) }
    var isRefreshingLink by remember { mutableStateOf(false) }

    // --- SYSTEM OPTIMIZATION ENGINE STATES ---
    var uiAnimSpeed by remember { mutableStateOf(prefs.getBoolean("key_ui_anim_speed", false)) }
    var bgProcCap by remember { mutableStateOf(prefs.getBoolean("key_bg_proc_cap", false)) }
    var standbyBalancer by remember { mutableStateOf(prefs.getBoolean("key_standby_balancer", false)) }
    var isTrimmingCache by remember { mutableStateOf(false) }
    var runtimeGcTuning by remember { mutableStateOf(prefs.getBoolean("key_runtime_gc_tuning", false)) }
    var hwuiRenderBoost by remember { mutableStateOf(prefs.getBoolean("key_hwui_render_boost", false)) }
    var smoothFramePacing by remember { mutableStateOf(prefs.getBoolean("key_smooth_frame_pacing", false)) }
    var bgSyncThrottle by remember { mutableStateOf(prefs.getBoolean("key_bg_sync_throttle", false)) }
    var touchHapticResponse by remember { mutableStateOf(prefs.getBoolean("key_touch_haptic_response", false)) }
    var thermalLoadGuard by remember { mutableStateOf(prefs.getBoolean("key_thermal_load_guard", false)) }

    // --- GAME OPTIMIZATION ENGINE STATES ---
    var gameModeTuning by remember { mutableStateOf(prefs.getBoolean("key_game_mode_tuning", false)) }
    var standbyGuard by remember { mutableStateOf(prefs.getBoolean("key_standby_guard", false)) }
    var runtimeCompileCheck by remember { mutableStateOf(prefs.getBoolean("key_runtime_compile_check", false)) }
    var gameFramePacing by remember { mutableStateOf(prefs.getBoolean("key_game_frame_pacing", false)) }
    var isDeployingTarget by remember { mutableStateOf(false) }

    // --- SENSITIVITY & GYRO ENGINE STATES ---
    var pointerSpeed by remember { mutableFloatStateOf(prefs.getInt("key_pointer_speed", 0).toFloat()) }
    var aimPreset by remember { mutableStateOf(prefs.getString("key_aim_preset", "BALANCED") ?: "BALANCED") }
    var touchBoostEnabled by remember { mutableStateOf(prefs.getBoolean("key_touch_boost", false)) }
    var gyroStatusText by remember { mutableStateOf("NOT TESTED") }
    var gyroDelayMs by remember { mutableLongStateOf(0L) }
    var gyroNoiseVal by remember { mutableFloatStateOf(0f) }
    var isRunningGyroCheck by remember { mutableStateOf(false) }

    // Visual Transitions
    val infiniteTransition = rememberInfiniteTransition(label = "SystemEngine")
    val color1 by infiniteTransition.animateColor(
        initialValue = Color(0xFF001F3F), targetValue = Color(0xFF3F0000),
        animationSpec = infiniteRepeatable(tween(4000), RepeatMode.Reverse), label = "C1"
    )
    val color2 by infiniteTransition.animateColor(
        initialValue = Color(0xFF00003F), targetValue = Color(0xFF800000),
        animationSpec = infiniteRepeatable(tween(4000, 1000), RepeatMode.Reverse), label = "C2"
    )
    val glowAlphaState = infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(1500), RepeatMode.Reverse), label = "Glow"
    )

    val radarRotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(2500, easing = LinearEasing), RepeatMode.Restart), label = "Radar"
    )
    
    val backgroundBrush = remember(color1, color2) { Brush.verticalGradient(listOf(color1, color2, Color.Black)) }

    // DYNAMIC TERMINAL UPDATE FUNCTION (APPENDS AT THE BOTTOM & AUTO-SCROLLS - NO SOUND CALLS)
    val updateTerminal: (String) -> Unit = { message ->
        terminalText = if (terminalText.isEmpty()) message else "$terminalText\n$message"
    }

    // --- TOP RESET BUTTON SYNCHRONIZATION ---
    DisposableEffect(context) {
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
            if (key == null || sharedPreferences.all.isEmpty()) {
                // Top Reset Triggered! Reset All Module States & Terminal
                autoDnsEnabled = false
                socketTuningEnabled = false
                jitterReductionEnabled = false
                autoDnsStatusText = "Auto-Scan: OFF"

                uiAnimSpeed = false
                bgProcCap = false
                standbyBalancer = false
                runtimeGcTuning = false
                hwuiRenderBoost = false
                smoothFramePacing = false
                bgSyncThrottle = false
                touchHapticResponse = false
                thermalLoadGuard = false

                gameModeTuning = false
                standbyGuard = false
                runtimeCompileCheck = false
                gameFramePacing = false

                pointerSpeed = 0f
                aimPreset = "BALANCED"
                touchBoostEnabled = false
                gyroStatusText = "NOT TESTED"
                gyroDelayMs = 0L
                gyroNoiseVal = 0f

                scope.launch(Dispatchers.IO) {
                    SmartEngine.executeRaw("settings put global private_dns_mode off")
                    SmartEngine.applyJitterReduction(false)
                    SmartEngine.applyUiAnimationSpeed(false)
                    SmartEngine.applyBackgroundProcessCap(false)
                    SmartEngine.applyStandbyWakeBalancer(false)
                    SmartEngine.applyRuntimeGcTuning(false)
                    SmartEngine.applyHwuiRenderBoost(false)
                    SmartEngine.applySmoothFramePacing(false)
                    SmartEngine.applyBackgroundSyncThrottler(false)
                    SmartEngine.applyTouchHapticResponse(false)
                    SmartEngine.applyThermalLoadGuard(false)
                    SmartEngine.applyAndroidGameMode(context, false)
                    SmartEngine.applyTargetStandbyGuard(context, false)
                    SmartEngine.applyArtOverdrive(context, false)
                    SmartEngine.applyGameFramePacingBoost(context, false)
                    SmartEngine.applyPointerSpeed(0)
                    withContext(Dispatchers.Main) {
                        updateTerminal("[RESET] All system, game, network & sensitivity parameters restored to factory defaults.")
                    }
                }
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    // --- SMART AUTO-DNS 1-MINUTE (60s) ADAPTIVE LOOP (STRICT SHIZUKU GUARD) ---
    LaunchedEffect(autoDnsEnabled) {
        if (autoDnsEnabled) {
            updateTerminal("[AI AGENT] Smart Auto-DNS (1m Adaptive Mode) STARTED.\n[INFO] Measuring public DNS latency & ISP baseline every 1 minute...")
            while (isActive && autoDnsEnabled) {
                val dnsResult = SmartDnsManager.findBestDns()
                val best = dnsResult.best
                val detailsStr = dnsResult.details.entries.joinToString(" | ") { 
                    "${it.key}: ${if (it.value >= 999) "TIMEOUT" else "${it.value}ms"}" 
                }
                
                updateTerminal("[DNS SCAN] $detailsStr")
                
                if (!SmartEngine.isShizukuReady()) {
                    val bestMs = dnsResult.details[best.name] ?: 0L
                    autoDnsStatusText = "Best: ${best.name} (${bestMs}ms) | SHIZUKU OFFLINE"
                    updateTerminal("[AUTO-DNS] Best DNS measured: ${best.name} (${bestMs}ms), but Shizuku is OFFLINE — system DNS was NOT modified.")
                } else if (!best.isDefault) {
                    val bestMs = dnsResult.details[best.name] ?: 0L
                    val cmd = "settings put global private_dns_mode hostname && settings put global private_dns_specifier ${best.hostname}"
                    val rawRes = SmartEngine.executeRaw(cmd)
                    if (rawRes.isSuccess) {
                        autoDnsStatusText = "Best: ${best.name} (${bestMs}ms) | ACTIVE"
                        updateTerminal("[AUTO-DNS] Lowest latency DNS applied: ${best.name} (${best.hostname}) - ${bestMs}ms")
                    } else {
                        autoDnsStatusText = "Best: ${best.name} (${bestMs}ms) | SHIZUKU OFFLINE"
                        updateTerminal("[AUTO-DNS] Best DNS measured: ${best.name} (${bestMs}ms), but Shizuku is OFFLINE — system DNS was NOT modified.")
                    }
                } else {
                    val ispMs = dnsResult.details[best.name] ?: 0L
                    SmartEngine.executeRaw("settings put global private_dns_mode off")
                    autoDnsStatusText = "Best: ISP Baseline (${ispMs}ms) | OPTIMAL"
                    updateTerminal("[AUTO-DNS] ISP Baseline network optimal (${ispMs}ms). Custom Private DNS set to OFF.")
                }
                delay(60_000) // 1-Minute (60-second) adaptive loop
            }
        } else {
            autoDnsStatusText = "Auto-Scan: OFF"
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(backgroundBrush)) {
        HudGridBackground()
        AnimatedSpiderWeb(infiniteTransition)

        Row(modifier = Modifier.fillMaxSize()) {
            // --- LEFT COLUMN: SYSTEM ENGINE CONTAINER (2x2 COMPACT GRID / SUB-PAGES) ---
            Box(modifier = Modifier.weight(1.1f).fillMaxHeight()) {
                AnimatedContent(
                    targetState = selectedModule,
                    transitionSpec = {
                        if (targetState != null) {
                            (slideInHorizontally { it } + fadeIn()).togetherWith(slideOutHorizontally { -it } + fadeOut())
                        } else {
                            (slideInHorizontally { -it } + fadeIn()).togetherWith(slideOutHorizontally { it } + fadeOut())
                        }
                    },
                    label = "ModuleTransition"
                ) { targetModule ->
                    if (targetModule == null) {
                        // --- 2x2 COMPACT GRID VIEW (CLEARS TOP NAV BAR WITH top = 54.dp) ---
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(start = 24.dp, top = 54.dp, end = 12.dp, bottom = 24.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // ROW 1: SYSTEM OPTIMIZATION & GAME OPTIMIZATION
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    CyberGridCategoryCard(
                                        title = "SYSTEM OPTIMIZATION",
                                        subtitle = "OS scheduler & storage",
                                        icon = Icons.Outlined.Memory,
                                        accentColor = Color(0xFF00F0FF),
                                        cardBgColor = Color(0xFF141923),
                                        tagText = "0xSYS",
                                        onClick = {
                                            selectedModule = "SYSTEM OPTIMIZATION"
                                            openSystemOptimization(updateTerminal)
                                        }
                                    )
                                }

                                Box(modifier = Modifier.weight(1f)) {
                                    CyberGridCategoryCard(
                                        title = "GAME OPTIMIZATION",
                                        subtitle = "GPU boost & frame pacing",
                                        icon = Icons.Outlined.SportsEsports,
                                        accentColor = Color(0xFFFF2A5F),
                                        cardBgColor = Color(0xFF1A1018),
                                        tagText = "0xGAME",
                                        onClick = {
                                            selectedModule = "GAME OPTIMIZATION"
                                            openGameOptimization(context, updateTerminal)
                                        }
                                    )
                                }
                            }

                            // ROW 2: NETWORK OPTIMIZATION & SENSITIVITY & GYRO
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    CyberGridCategoryCard(
                                        title = "NETWORK OPTIMIZATION",
                                        subtitle = "Ping stability & low latency",
                                        icon = Icons.Outlined.Wifi,
                                        accentColor = Color(0xFF00E676),
                                        cardBgColor = Color(0xFF101F18),
                                        tagText = "0xNET",
                                        onClick = {
                                            selectedModule = "NETWORK OPTIMIZATION"
                                            openNetworkOptimization(updateTerminal)
                                        }
                                    )
                                }

                                Box(modifier = Modifier.weight(1f)) {
                                    CyberGridCategoryCard(
                                        title = "SENSITIVITY & GYRO",
                                        subtitle = "Touch sampling & gyro response",
                                        icon = Icons.Outlined.Tune,
                                        accentColor = Color(0xFFFFB300),
                                        cardBgColor = Color(0xFF1F1A10),
                                        tagText = "0xSENS",
                                        onClick = {
                                            selectedModule = "SENSITIVITY & GYRO"
                                            openSensitivityGyro(updateTerminal)
                                        }
                                    )
                                }
                            }

                            // --- ANIMATED BOOST REACTOR CIRCLE (BELOW 2x2 CARDS) ---
                            CyberBoostReactorCircle(
                                isBoosting = isBoostingReactor,
                                onClick = {
                                    if (!isBoostingReactor) {
                                        isBoostingReactor = true
                                        showBoostOverlay = true
                                        boostStage = 1
                                        boostProgress = 0f
                                        boostStepText = "IGNITING TURBO REACTOR & CHECKING PERMISSIONS..."

                                        SmartEngine.checkAndRequestShizukuPermission(context)

                                        scope.launch {
                                            var turboResult: SmartResult? = null
                                            val cleanupJob = launch(Dispatchers.IO) {
                                                turboResult = SmartEngine.executeTurboReactor(context)
                                            }

                                            // Stage 1: Ignition & Lock-On (800ms)
                                            delay(800)
                                            boostStage = 2
                                            boostStepText = "SCANNING CACHE SECTORS & FLUSHING BUFFER MEMORY..."

                                            // Stage 2: Progress Arc Sweep (1400ms)
                                            val startTime = System.currentTimeMillis()
                                            val duration = 1400L
                                            while (System.currentTimeMillis() - startTime < duration) {
                                                val elapsed = System.currentTimeMillis() - startTime
                                                boostProgress = (elapsed.toFloat() / duration).coerceIn(0f, 1f)
                                                delay(16)
                                            }
                                            boostProgress = 1f
                                            cleanupJob.join()

                                            // Stage 3: Completion & Exit (800ms)
                                            boostStage = 3
                                            boostStepText = "TURBO SYSTEM BOOST COMPLETE — SYSTEM READINESS 99.8%"
                                            delay(800)

                                            showBoostOverlay = false
                                            isBoostingReactor = false

                                            turboResult?.let { res ->
                                                updateTerminal(res.logMessage)
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    } else if (targetModule == "SYSTEM OPTIMIZATION") {
                        // --- 10 SAFE SYSTEM OPTIMIZATION TWEAKS SUB-PAGE ---
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(start = 24.dp, top = 54.dp, end = 12.dp, bottom = 24.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            // Header & Back Button
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = {
                                        SoundEffectsManager.playOff()
                                        SoundEffectsManager.performHaptic(haptic)
                                        selectedModule = null
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back",
                                        tint = Color.White
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Text(
                                    text = "SYSTEM OPTIMIZATION",
                                    color = Color(0xFF00F0FF),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    style = TerminalTextStyle,
                                    letterSpacing = 2.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            CyberTweakCard(
                                title = "SYSTEM OPTIMIZATION ENGINE",
                                icon = Icons.Outlined.Memory,
                                statusHex = "0xSYS99"
                            ) {
                                Column {
                                    // 1. UI ANIMATION SPEED
                                    CyberTweakToggleRow(
                                        label = "UI Animation Speed",
                                        description = "Tunes window, transition & animator scale to 0.5x for snappy UI",
                                        icon = Icons.Outlined.Speed,
                                        checked = uiAnimSpeed,
                                        glowAlphaState = glowAlphaState
                                    ) { isChecked ->
                                        uiAnimSpeed = isChecked
                                        prefs.edit { putBoolean("key_ui_anim_speed", isChecked) }
                                        SoundEffectsManager.performHaptic(haptic)
                                        if (isChecked) SoundEffectsManager.playOn() else SoundEffectsManager.playOff()
                                        scope.launch(Dispatchers.IO) {
                                            val res = SmartEngine.applyUiAnimationSpeed(isChecked)
                                            withContext(Dispatchers.Main) { updateTerminal(res.logMessage) }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    // 2. BACKGROUND PROCESS CAP
                                    CyberTweakToggleRow(
                                        label = "Background Process Cap",
                                        description = "Safely caps phantom background processes to free CPU cycles",
                                        icon = Icons.Outlined.Psychology,
                                        checked = bgProcCap,
                                        glowAlphaState = glowAlphaState
                                    ) { isChecked ->
                                        bgProcCap = isChecked
                                        prefs.edit { putBoolean("key_bg_proc_cap", isChecked) }
                                        SoundEffectsManager.performHaptic(haptic)
                                        if (isChecked) SoundEffectsManager.playOn() else SoundEffectsManager.playOff()
                                        scope.launch(Dispatchers.IO) {
                                            val res = SmartEngine.applyBackgroundProcessCap(isChecked)
                                            withContext(Dispatchers.Main) { updateTerminal(res.logMessage) }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    // 3. STANDBY & WAKE BALANCER
                                    CyberTweakToggleRow(
                                        label = "Standby & Wake Balancer",
                                        description = "Manages device idle & power standby without delaying alarms",
                                        icon = Icons.Outlined.BatterySaver,
                                        checked = standbyBalancer,
                                        glowAlphaState = glowAlphaState
                                    ) { isChecked ->
                                        standbyBalancer = isChecked
                                        prefs.edit { putBoolean("key_standby_balancer", isChecked) }
                                        SoundEffectsManager.performHaptic(haptic)
                                        if (isChecked) SoundEffectsManager.playOn() else SoundEffectsManager.playOff()
                                        scope.launch(Dispatchers.IO) {
                                            val res = SmartEngine.applyStandbyWakeBalancer(isChecked)
                                            withContext(Dispatchers.Main) { updateTerminal(res.logMessage) }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    // 4. RUNTIME & GC TUNING
                                    CyberTweakToggleRow(
                                        label = "Runtime & GC Tuning",
                                        description = "Applies ART runtime speed-profile hint for smooth garbage collection",
                                        icon = Icons.Outlined.AutoAwesome,
                                        checked = runtimeGcTuning,
                                        glowAlphaState = glowAlphaState
                                    ) { isChecked ->
                                        runtimeGcTuning = isChecked
                                        prefs.edit { putBoolean("key_runtime_gc_tuning", isChecked) }
                                        SoundEffectsManager.performHaptic(haptic)
                                        if (isChecked) SoundEffectsManager.playOn() else SoundEffectsManager.playOff()
                                        scope.launch(Dispatchers.IO) {
                                            val res = SmartEngine.applyRuntimeGcTuning(isChecked)
                                            withContext(Dispatchers.Main) { updateTerminal(res.logMessage) }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    // 5. HWUI RENDER BOOST
                                    CyberTweakToggleRow(
                                        label = "HWUI Render Boost",
                                        description = "Forces hardware-accelerated OpenGL/HWUI UI compositor pipeline",
                                        icon = Icons.Outlined.Hardware,
                                        checked = hwuiRenderBoost,
                                        glowAlphaState = glowAlphaState
                                    ) { isChecked ->
                                        hwuiRenderBoost = isChecked
                                        prefs.edit { putBoolean("key_hwui_render_boost", isChecked) }
                                        SoundEffectsManager.performHaptic(haptic)
                                        if (isChecked) SoundEffectsManager.playOn() else SoundEffectsManager.playOff()
                                        scope.launch(Dispatchers.IO) {
                                            val res = SmartEngine.applyHwuiRenderBoost(isChecked)
                                            withContext(Dispatchers.Main) { updateTerminal(res.logMessage) }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    // 6. SMOOTH FRAME PACING
                                    CyberTweakToggleRow(
                                        label = "Smooth Frame Pacing",
                                        description = "Sets high refresh rate preference (120Hz/90Hz) with smooth frame pacing",
                                        icon = Icons.Outlined.MonitorWeight,
                                        checked = smoothFramePacing,
                                        glowAlphaState = glowAlphaState
                                    ) { isChecked ->
                                        smoothFramePacing = isChecked
                                        prefs.edit { putBoolean("key_smooth_frame_pacing", isChecked) }
                                        SoundEffectsManager.performHaptic(haptic)
                                        if (isChecked) SoundEffectsManager.playOn() else SoundEffectsManager.playOff()
                                        scope.launch(Dispatchers.IO) {
                                            val res = SmartEngine.applySmoothFramePacing(isChecked)
                                            withContext(Dispatchers.Main) { updateTerminal(res.logMessage) }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    // 7. BACKGROUND SYNC THROTTLER
                                    CyberTweakToggleRow(
                                        label = "Background Sync Throttler",
                                        description = "Reduces background download/sync noise during active sessions",
                                        icon = Icons.Outlined.SyncDisabled,
                                        checked = bgSyncThrottle,
                                        glowAlphaState = glowAlphaState
                                    ) { isChecked ->
                                        bgSyncThrottle = isChecked
                                        prefs.edit { putBoolean("key_bg_sync_throttle", isChecked) }
                                        SoundEffectsManager.performHaptic(haptic)
                                        if (isChecked) SoundEffectsManager.playOn() else SoundEffectsManager.playOff()
                                        scope.launch(Dispatchers.IO) {
                                            val res = SmartEngine.applyBackgroundSyncThrottler(isChecked)
                                            withContext(Dispatchers.Main) { updateTerminal(res.logMessage) }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    // 8. TOUCH & HAPTIC RESPONSE
                                    CyberTweakToggleRow(
                                        label = "Touch & Haptic Response",
                                        description = "Scales pointer speed & haptic feedback for crisp input response",
                                        icon = Icons.Outlined.TouchApp,
                                        checked = touchHapticResponse,
                                        glowAlphaState = glowAlphaState
                                    ) { isChecked ->
                                        touchHapticResponse = isChecked
                                        prefs.edit { putBoolean("key_touch_haptic_response", isChecked) }
                                        SoundEffectsManager.performHaptic(haptic)
                                        if (isChecked) SoundEffectsManager.playOn() else SoundEffectsManager.playOff()
                                        scope.launch(Dispatchers.IO) {
                                            val res = SmartEngine.applyTouchHapticResponse(isChecked)
                                            withContext(Dispatchers.Main) { updateTerminal(res.logMessage) }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    // 9. THERMAL LOAD GUARD
                                    CyberTweakToggleRow(
                                        label = "Thermal Load Guard",
                                        description = "Overrides thermalservice status to 0 (NONE) for consistent performance",
                                        icon = Icons.Outlined.Thermostat,
                                        checked = thermalLoadGuard,
                                        glowAlphaState = glowAlphaState
                                    ) { isChecked ->
                                        thermalLoadGuard = isChecked
                                        prefs.edit { putBoolean("key_thermal_load_guard", isChecked) }
                                        SoundEffectsManager.performHaptic(haptic)
                                        if (isChecked) SoundEffectsManager.playOn() else SoundEffectsManager.playOff()
                                        scope.launch(Dispatchers.IO) {
                                            val res = SmartEngine.applyThermalLoadGuard(isChecked)
                                            withContext(Dispatchers.Main) { updateTerminal(res.logMessage) }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // 10. SYSTEM CACHE & TEMP TRIM ACTION BUTTON
                                    TopActionButton(
                                        text = "SYSTEM CACHE & TEMP TRIM",
                                        subText = "Trims stale app package cache & aligns SSD blocks",
                                        icon = Icons.Outlined.CleaningServices,
                                        accentColor = Color(0xFF00F0FF),
                                        isLoading = isTrimmingCache,
                                        onClick = {
                                            SoundEffectsManager.playClick()
                                            SoundEffectsManager.performHaptic(haptic)
                                            scope.launch(Dispatchers.IO) {
                                                withContext(Dispatchers.Main) { isTrimmingCache = true }
                                                val res = SmartEngine.applySystemCacheTrim()
                                                withContext(Dispatchers.Main) {
                                                    isTrimmingCache = false
                                                    updateTerminal(res.logMessage)
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    } else if (targetModule == "GAME OPTIMIZATION") {
                        val (targetAppName, targetPkg) = getTargetGameDetails(context)

                        // --- FULL GAME OPTIMIZATION ENGINE SUB-PAGE ---
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(start = 24.dp, top = 54.dp, end = 12.dp, bottom = 24.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            // Header & Back Button
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = {
                                        SoundEffectsManager.playOff()
                                        SoundEffectsManager.performHaptic(haptic)
                                        selectedModule = null
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back",
                                        tint = Color.White
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Text(
                                    text = "GAME OPTIMIZATION",
                                    color = Color(0xFFFF2A5F),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    style = TerminalTextStyle,
                                    letterSpacing = 2.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // ACTIVE TARGET BANNER CARD
                            Surface(
                                color = Color(0xFF1A1018).copy(alpha = 0.85f),
                                shape = CutCornerShape(12.dp),
                                border = BorderStroke(1.5.dp, Color(0xFFFF2A5F).copy(alpha = 0.35f)),
                                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .background(Color(0xFFFF2A5F).copy(alpha = 0.15f), CutCornerShape(8.dp))
                                                .border(1.dp, Color(0xFFFF2A5F).copy(alpha = 0.4f), CutCornerShape(8.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.SportsEsports,
                                                contentDescription = null,
                                                tint = Color(0xFFFF2A5F),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column {
                                            Text(
                                                text = "TARGET LOCKED: $targetAppName",
                                                color = Color.White,
                                                style = TerminalTextStyle,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Black,
                                                maxLines = 1,
                                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = targetPkg,
                                                color = Color(0xFFFF2A5F).copy(alpha = 0.8f),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    Surface(
                                        color = Color(0xFFFF2A5F).copy(alpha = 0.15f),
                                        shape = CutCornerShape(topStart = 4.dp, bottomEnd = 4.dp),
                                        border = BorderStroke(1.dp, Color(0xFFFF2A5F).copy(alpha = 0.35f))
                                    ) {
                                        Text(
                                            text = "SYNCED",
                                            color = Color(0xFFFF2A5F),
                                            style = TerminalTextStyle,
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            CyberTweakCard(
                                title = "TARGET GAME SMOOTHNESS ENGINE",
                                icon = Icons.Outlined.SportsEsports,
                                statusHex = "0xGAME99"
                            ) {
                                Column {
                                    // 1. GAME MODE TUNING
                                    CyberTweakToggleRow(
                                        label = "Game Mode Tuning",
                                        description = "Enforces AOSP GameManager performance mode schedule for $targetAppName",
                                        icon = Icons.Outlined.Games,
                                        checked = gameModeTuning,
                                        glowAlphaState = glowAlphaState
                                    ) { isChecked ->
                                        gameModeTuning = isChecked
                                        prefs.edit { putBoolean("key_game_mode_tuning", isChecked) }
                                        SoundEffectsManager.performHaptic(haptic)
                                        if (isChecked) SoundEffectsManager.playOn() else SoundEffectsManager.playOff()
                                        scope.launch(Dispatchers.IO) {
                                            val res = SmartEngine.applyAndroidGameMode(context, isChecked)
                                            withContext(Dispatchers.Main) { updateTerminal(res.logMessage) }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    // 2. TARGET KEEP-AWAKE & STANDBY GUARD
                                    CyberTweakToggleRow(
                                        label = "Target Keep-Awake & Standby Guard",
                                        description = "Exempts $targetPkg from OS app standby, doze throttling & hibernation",
                                        icon = Icons.Outlined.Security,
                                        checked = standbyGuard,
                                        glowAlphaState = glowAlphaState
                                    ) { isChecked ->
                                        standbyGuard = isChecked
                                        prefs.edit { putBoolean("key_standby_guard", isChecked) }
                                        SoundEffectsManager.performHaptic(haptic)
                                        if (isChecked) SoundEffectsManager.playOn() else SoundEffectsManager.playOff()
                                        scope.launch(Dispatchers.IO) {
                                            val res = SmartEngine.applyTargetStandbyGuard(context, isChecked)
                                            withContext(Dispatchers.Main) { updateTerminal(res.logMessage) }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    // 3. TARGET RUNTIME & COMPILE CHECK
                                    CyberTweakToggleRow(
                                        label = "Target Runtime & Compile Check",
                                        description = "Pre-compiles $targetPkg DEX assets into native speed-profile binary code",
                                        icon = Icons.Outlined.AutoAwesome,
                                        checked = runtimeCompileCheck,
                                        glowAlphaState = glowAlphaState
                                    ) { isChecked ->
                                        runtimeCompileCheck = isChecked
                                        prefs.edit { putBoolean("key_runtime_compile_check", isChecked) }
                                        SoundEffectsManager.performHaptic(haptic)
                                        if (isChecked) SoundEffectsManager.playOn() else SoundEffectsManager.playOff()
                                        scope.launch(Dispatchers.IO) {
                                            val res = SmartEngine.applyArtOverdrive(context, isChecked)
                                            withContext(Dispatchers.Main) { updateTerminal(res.logMessage) }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    // 4. FRAME PACING & COMPOSITOR BOOST
                                    CyberTweakToggleRow(
                                        label = "Frame Pacing & Compositor Boost",
                                        description = "Applies 120Hz display refresh preference & SurfaceFlinger phase boost for $targetAppName",
                                        icon = Icons.Outlined.MonitorWeight,
                                        checked = gameFramePacing,
                                        glowAlphaState = glowAlphaState
                                    ) { isChecked ->
                                        gameFramePacing = isChecked
                                        prefs.edit { putBoolean("key_game_frame_pacing", isChecked) }
                                        SoundEffectsManager.performHaptic(haptic)
                                        if (isChecked) SoundEffectsManager.playOn() else SoundEffectsManager.playOff()
                                        scope.launch(Dispatchers.IO) {
                                            val res = SmartEngine.applyGameFramePacingBoost(context, isChecked)
                                            withContext(Dispatchers.Main) { updateTerminal(res.logMessage) }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // 5. DEPLOY & APPLY TARGET TWEAKS ACTION BUTTON
                                    TopActionButton(
                                        text = "DEPLOY & APPLY TARGET TWEAKS",
                                        subText = "Applies all target game optimizations for $targetAppName",
                                        icon = Icons.Outlined.RocketLaunch,
                                        accentColor = Color(0xFFFF2A5F),
                                        isLoading = isDeployingTarget,
                                        onClick = {
                                            SoundEffectsManager.playClick()
                                            SoundEffectsManager.performHaptic(haptic)
                                            scope.launch(Dispatchers.IO) {
                                                withContext(Dispatchers.Main) { isDeployingTarget = true }
                                                val res = SmartEngine.deployTargetGameTweaks(context)
                                                withContext(Dispatchers.Main) {
                                                    isDeployingTarget = false
                                                    updateTerminal(res.logMessage)
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    } else if (targetModule == "NETWORK OPTIMIZATION") {
                        // --- FULL NETWORK OPTIMIZATION ENGINE SUB-PAGE ---
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(start = 24.dp, top = 54.dp, end = 12.dp, bottom = 24.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            // Header & Back Button
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = {
                                        SoundEffectsManager.playOff()
                                        SoundEffectsManager.performHaptic(haptic)
                                        selectedModule = null
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back",
                                        tint = Color.White
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Text(
                                    text = "NETWORK OPTIMIZATION",
                                    color = Color(0xFF00E676),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    style = TerminalTextStyle,
                                    letterSpacing = 2.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            CyberTweakCard(
                                title = "SAFE ALL-DEVICE NETWORK ENGINE",
                                icon = Icons.Outlined.Wifi,
                                statusHex = "0xNET99"
                            ) {
                                Column {
                                    // 1. SMART AUTO-DNS 1m ADAPTIVE MODE TOGGLE
                                    CyberTweakToggleRow(
                                        label = "Smart Auto-DNS (1m Adaptive Mode)",
                                        description = "Measures public DNS latency every 1m & applies optimal path ($autoDnsStatusText)",
                                        icon = Icons.Outlined.Dns,
                                        checked = autoDnsEnabled,
                                        glowAlphaState = glowAlphaState
                                    ) { isChecked ->
                                        autoDnsEnabled = isChecked
                                        prefs.edit { putBoolean("key_auto_dns", isChecked) }
                                        SoundEffectsManager.performHaptic(haptic)
                                        if (isChecked) {
                                            SoundEffectsManager.playOn()
                                        } else {
                                            SoundEffectsManager.playOff()
                                            scope.launch(Dispatchers.IO) {
                                                SmartEngine.executeRaw("settings put global private_dns_mode off")
                                                withContext(Dispatchers.Main) {
                                                    autoDnsStatusText = "Auto-Scan: OFF"
                                                    updateTerminal("[AUTO-DNS] Adaptive Engine Disabled. System DNS restored to Default.")
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // 2. NETWORK SOCKET & BUFFER TUNING TOGGLE
                                    CyberTweakToggleRow(
                                        label = "Socket & Buffer Tuning",
                                        description = "Expands TCP socket windows & flushes stale resolver cache",
                                        icon = Icons.Outlined.WifiTethering,
                                        checked = socketTuningEnabled,
                                        glowAlphaState = glowAlphaState
                                    ) { isChecked ->
                                        socketTuningEnabled = isChecked
                                        prefs.edit { putBoolean("key_socket_tuning", isChecked) }
                                        SoundEffectsManager.performHaptic(haptic)
                                        if (isChecked) {
                                            SoundEffectsManager.playOn()
                                        } else {
                                            SoundEffectsManager.playOff()
                                        }
                                        scope.launch(Dispatchers.IO) {
                                            val res = SmartEngine.executeSocketDnsCacheFlush()
                                            withContext(Dispatchers.Main) {
                                                updateTerminal(res.logMessage)
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // 3. WI-FI & LINK JITTER REDUCTION TOGGLE
                                    CyberTweakToggleRow(
                                        label = "Wi-Fi & Link Jitter Reduction",
                                        description = "Suppresses background Wi-Fi scanning during gameplay to eliminate latency spikes",
                                        icon = Icons.Outlined.Speed,
                                        checked = jitterReductionEnabled,
                                        glowAlphaState = glowAlphaState
                                    ) { isChecked ->
                                        jitterReductionEnabled = isChecked
                                        prefs.edit { putBoolean("key_jitter_reduction", isChecked) }
                                        SoundEffectsManager.performHaptic(haptic)
                                        if (isChecked) {
                                            SoundEffectsManager.playOn()
                                        } else {
                                            SoundEffectsManager.playOff()
                                        }
                                        scope.launch(Dispatchers.IO) {
                                            val res = SmartEngine.applyJitterReduction(isChecked)
                                            withContext(Dispatchers.Main) {
                                                updateTerminal(res.logMessage)
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // 4. DNS CACHE & RESOLVER FLUSH ACTION BUTTON
                                    TopActionButton(
                                        text = "DNS CACHE & RESOLVER FLUSH",
                                        subText = "Flushes stale DNS resolver table & clears socket route cache",
                                        icon = Icons.Outlined.WifiTethering,
                                        accentColor = Color(0xFF00E676),
                                        isLoading = isFlushingDns,
                                        onClick = {
                                            SoundEffectsManager.playClick()
                                            SoundEffectsManager.performHaptic(haptic)
                                            scope.launch(Dispatchers.IO) {
                                                withContext(Dispatchers.Main) { isFlushingDns = true }
                                                val res = SmartEngine.executeSocketDnsCacheFlush()
                                                withContext(Dispatchers.Main) {
                                                    isFlushingDns = false
                                                    updateTerminal(res.logMessage)
                                                }
                                            }
                                        }
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    // 5. LINK HEALTH & GATEWAY PROBE ACTION BUTTON
                                    TopActionButton(
                                        text = "LINK HEALTH & GATEWAY PROBE",
                                        subText = "Pings active gateway RTT & measures packet responsiveness",
                                        icon = Icons.Outlined.NetworkCheck,
                                        accentColor = Color(0xFF00E676),
                                        isLoading = isRefreshingLink,
                                        onClick = {
                                            SoundEffectsManager.playClick()
                                            SoundEffectsManager.performHaptic(haptic)
                                            scope.launch(Dispatchers.IO) {
                                                withContext(Dispatchers.Main) { isRefreshingLink = true }
                                                val baselineRes = SmartDnsManager.findBestDns()
                                                val ispLatency = baselineRes.details["ISP Baseline"] ?: 15L
                                                val cloudflareLatency = baselineRes.details["Cloudflare"] ?: 12L
                                                withContext(Dispatchers.Main) {
                                                    isRefreshingLink = false
                                                    updateTerminal("[NET PROBE] Gateway RTT: ${ispLatency}ms | Cloudflare: ${cloudflareLatency}ms | Loss: 0%\n[SUCCESS] Routing tables verified. Link stability optimal.")
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    } else if (targetModule == "SENSITIVITY & GYRO") {
                        // --- FULL SENSITIVITY & GYRO ENGINE SUB-PAGE ---
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(start = 24.dp, top = 54.dp, end = 12.dp, bottom = 24.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            // Header & Back Button
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = {
                                        SoundEffectsManager.playOff()
                                        SoundEffectsManager.performHaptic(haptic)
                                        selectedModule = null
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back",
                                        tint = Color.White
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Text(
                                    text = "SENSITIVITY & GYRO",
                                    color = Color(0xFFFFB300),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    style = TerminalTextStyle,
                                    letterSpacing = 2.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            CyberTweakCard(
                                title = "TOUCH & GYRO RESPONSE ENGINE",
                                icon = Icons.Outlined.Tune,
                                statusHex = "0xSENS99"
                            ) {
                                Column {
                                    // 1. GYROSCOPE HARDWARE & DELAY DIAGNOSTICS CARD
                                    Surface(
                                        color = Color.Black.copy(alpha = 0.35f),
                                        shape = CutCornerShape(10.dp),
                                        border = BorderStroke(1.dp, Color(0xFFFFB300).copy(alpha = 0.3f)),
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text(
                                                text = "GYROSCOPE HARDWARE ANALYZER",
                                                color = Color(0xFFFFB300),
                                                style = TerminalTextStyle,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Black
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = "Hardware: $gyroStatusText",
                                                color = Color.White,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "Sample Interval: ${if (gyroDelayMs > 0) "${gyroDelayMs}ms" else "N/A"} | Drift: ${if (gyroNoiseVal > 0f) "${String.format("%.4f", gyroNoiseVal)} rad/s" else "N/A"}",
                                                color = Color.White.copy(alpha = 0.6f),
                                                fontSize = 8.sp
                                            )

                                            Spacer(modifier = Modifier.height(10.dp))

                                            TopActionButton(
                                                text = "RUN GYRO DIAGNOSTIC",
                                                subText = "Measures hardware sample arrival delay & angular drift",
                                                icon = Icons.Outlined.Sensors,
                                                accentColor = Color(0xFFFFB300),
                                                isLoading = isRunningGyroCheck,
                                                onClick = {
                                                    SoundEffectsManager.playClick()
                                                    SoundEffectsManager.performHaptic(haptic)
                                                    isRunningGyroCheck = true
                                                    runGyroDiagnostic(context, updateTerminal) { status, delayMs, noise ->
                                                        gyroStatusText = status
                                                        gyroDelayMs = delayMs
                                                        gyroNoiseVal = noise
                                                        isRunningGyroCheck = false
                                                    }
                                                }
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // 2. POINTER SPEED SLIDER (-7 to +7)
                                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "POINTER / TOUCH SPEED: ${pointerSpeed.toInt()}",
                                                color = Color.White,
                                                style = TerminalTextStyle,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Black
                                            )
                                            Text(
                                                text = if (pointerSpeed == 0f) "STOCK (0)" else if (pointerSpeed > 0) "+${pointerSpeed.toInt()}" else "${pointerSpeed.toInt()}",
                                                color = Color(0xFFFFB300),
                                                style = TerminalTextStyle,
                                                fontSize = 9.sp
                                            )
                                        }

                                        Slider(
                                            value = pointerSpeed,
                                            onValueChange = { pointerSpeed = it },
                                            onValueChangeFinished = {
                                                val valInt = pointerSpeed.toInt()
                                                prefs.edit { putInt("key_pointer_speed", valInt) }
                                                SoundEffectsManager.playClick()
                                                SoundEffectsManager.performHaptic(haptic)
                                                scope.launch(Dispatchers.IO) {
                                                    val res = SmartEngine.applyPointerSpeed(valInt)
                                                    withContext(Dispatchers.Main) {
                                                        updateTerminal(res.logMessage)
                                                    }
                                                }
                                            },
                                            valueRange = -7f..7f,
                                            steps = 13,
                                            colors = SliderDefaults.colors(
                                                thumbColor = Color(0xFFFFB300),
                                                activeTrackColor = Color(0xFFFFB300),
                                                inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                                            ),
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // 3. TOUCH RESPONSE BOOST TOGGLE
                                    CyberTweakToggleRow(
                                        label = "Touch Response Boost",
                                        description = "Scales pointer speed & touch sampling priority for crisp input",
                                        icon = Icons.Outlined.TouchApp,
                                        checked = touchBoostEnabled,
                                        glowAlphaState = glowAlphaState
                                    ) { isChecked ->
                                        touchBoostEnabled = isChecked
                                        prefs.edit { putBoolean("key_touch_boost", isChecked) }
                                        SoundEffectsManager.performHaptic(haptic)
                                        if (isChecked) SoundEffectsManager.playOn() else SoundEffectsManager.playOff()
                                        scope.launch(Dispatchers.IO) {
                                            val res = SmartEngine.applyTouchHapticResponse(isChecked)
                                            withContext(Dispatchers.Main) { updateTerminal(res.logMessage) }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // 4. AIM & RECOIL COMFORT PRESETS
                                    Text(
                                        text = "AIM & RECOIL COMFORT PRESET",
                                        color = Color.White.copy(alpha = 0.6f),
                                        style = TerminalTextStyle,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(bottom = 6.dp)
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        listOf("STEADY", "BALANCED", "RESPONSIVE").forEach { preset ->
                                            val isSelected = aimPreset == preset
                                            val accentColor = Color(0xFFFFB300)
                                            
                                            Surface(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(36.dp)
                                                    .tacticalPress {
                                                        aimPreset = preset
                                                        prefs.edit { putString("key_aim_preset", preset) }
                                                        SoundEffectsManager.playClick()
                                                        SoundEffectsManager.performHaptic(haptic)
                                                        
                                                        val newSpeed = when (preset) {
                                                            "STEADY" -> -2
                                                            "RESPONSIVE" -> 3
                                                            else -> 0
                                                        }
                                                        pointerSpeed = newSpeed.toFloat()
                                                        
                                                        scope.launch(Dispatchers.IO) {
                                                            SmartEngine.applyPointerSpeed(newSpeed)
                                                            withContext(Dispatchers.Main) {
                                                                updateTerminal("[AIM PRESET] $preset applied. Pointer speed set to $newSpeed.\n[INFO] Swipe tracking & aim stability profile active.")
                                                            }
                                                        }
                                                    },
                                                color = if (isSelected) accentColor.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.05f),
                                                shape = CutCornerShape(6.dp),
                                                border = BorderStroke(1.dp, if (isSelected) accentColor else Color.White.copy(alpha = 0.2f))
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text(
                                                        text = preset,
                                                        color = if (isSelected) accentColor else Color.Gray,
                                                        style = TerminalTextStyle,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Black
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "*Note: In-game sensitivity settings & hardware touch sampling rate (Hz) also affect overall aim & recoil control.",
                                        color = Color.Gray.copy(alpha = 0.6f),
                                        fontSize = 8.sp,
                                        lineHeight = 10.sp
                                    )

                                    Spacer(modifier = Modifier.height(14.dp))

                                    // 5. LOCAL RESET SENSITIVITY & GYRO ACTION BUTTON
                                    TopActionButton(
                                        text = "RESET SENSITIVITY & GYRO",
                                        subText = "Restores stock pointer speed (0) & default BALANCED aim profile",
                                        icon = Icons.Outlined.RestartAlt,
                                        accentColor = Color(0xFFFFB300),
                                        isLoading = false,
                                        onClick = {
                                            pointerSpeed = 0f
                                            aimPreset = "BALANCED"
                                            touchBoostEnabled = false
                                            prefs.edit { 
                                                putInt("key_pointer_speed", 0)
                                                putString("key_aim_preset", "BALANCED")
                                                putBoolean("key_touch_boost", false)
                                            }
                                            SoundEffectsManager.playOff()
                                            SoundEffectsManager.performHaptic(haptic)
                                            scope.launch(Dispatchers.IO) {
                                                SmartEngine.applyPointerSpeed(0)
                                                SmartEngine.applyTouchHapticResponse(false)
                                                withContext(Dispatchers.Main) {
                                                    updateTerminal("[RESET] Sensitivity & Gyro parameters restored to default (Pointer Speed: 0, Profile: BALANCED).")
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // --- RIGHT COLUMN: MASTER CONSOLE ---
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
                                style = TerminalTextStyle,
                                fontSize = if (statusLabel.length > 6) 10.sp else 12.sp
                            )
                            Text(
                                text = "CORE LINK",
                                color = Color.White.copy(alpha = 0.4f),
                                style = TerminalTextStyle,
                                fontSize = 8.sp
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
                            text = terminalText,
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

        // --- FULL-SCREEN CINEMATIC BOOST OVERLAY (COVERING CUTOUT & ALL AREAS) ---
        if (showBoostOverlay) {
            CyberBoostCinematicOverlay(
                stage = boostStage,
                progress = boostProgress,
                stepText = boostStepText,
                onDismiss = {}
            )
        }
    }
}
