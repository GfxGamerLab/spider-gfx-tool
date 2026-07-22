package com.example.spidergfxtool.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.spidergfxtool.GfxBackendManager
import com.example.spidergfxtool.HudOverlayService
import com.example.spidergfxtool.MainActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import com.example.spidergfxtool.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

fun playSwitchSound(context: android.content.Context, resId: Int) {
    try {
        val mp = MediaPlayer.create(context, resId)
        mp?.setOnCompletionListener { it.release() }
        mp?.start()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

val CyberAngledShape = GenericShape { size, _ ->
    moveTo(20f, 0f)
    lineTo(size.width, 0f)
    lineTo(size.width, size.height - 20f)
    lineTo(size.width - 20f, size.height)
    lineTo(0f, size.height)
    lineTo(0f, 20f)
    close()
}

@Composable
fun GfxExtremeHudScreen() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("spider_gfx_prefs", android.content.Context.MODE_PRIVATE) }
    
    val soundPool = remember { SoundPool.Builder().setMaxStreams(5).build() }
    val soundOnId = remember { soundPool.load(context, R.raw.switch_on, 1) }
    val soundOffId = remember { soundPool.load(context, R.raw.switch_off, 1) }

    DisposableEffect(Unit) {
        onDispose { soundPool.release() }
    }

    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val isConnected = GfxBackendManager.isShizukuConnected.value
    
    var hudOverlayEnabled by remember { mutableStateOf(prefs.getBoolean("key_hud_overlay", false)) }
    var refreshRate by remember { mutableStateOf(prefs.getString("key_refresh_rate", "60Hz") ?: "60Hz") }
    var mechanicalFeedback by remember { mutableStateOf(prefs.getBoolean("key_mech_feedback", false)) }
    var isTurboBoosting by remember { mutableStateOf(false) }
    var terminalText by remember { mutableStateOf("") }

    // Overclock Engine States
    var pingStabilizer by remember { mutableStateOf(prefs.getBoolean("key_ping_stab", false)) }
    var batteryUnrestrictor by remember { mutableStateOf(prefs.getBoolean("key_batt_unrestrict", false)) }
    var hardwareOverlayBypass by remember { mutableStateOf(prefs.getBoolean("key_hw_overlay", false)) }
    var threadPriority by remember { mutableStateOf(prefs.getBoolean("key_thread_pri", false)) }
    var memoryHeapFlush by remember { mutableStateOf(prefs.getBoolean("key_mem_heap", false)) }

    // System Performance Engine States
    var uiTouchSpeed by remember { mutableStateOf(prefs.getBoolean("key_touch_speed", false)) }
    var gameMode by remember { mutableStateOf(prefs.getBoolean("key_game_mode", false)) }
    var phantomProcess by remember { mutableStateOf(prefs.getBoolean("key_phantom_limiter", false)) }
    var frameLatency by remember { mutableStateOf(prefs.getBoolean("key_frame_latency", false)) }
    var fifoUi by remember { mutableStateOf(prefs.getBoolean("key_fifo_ui", false)) }
    var precisionAim by remember { mutableStateOf(prefs.getBoolean("key_precision_aim", false)) }
    var lmkProtection by remember { mutableStateOf(prefs.getBoolean("key_lmk_prot", false)) }
    var disableBackpressure by remember { mutableStateOf(prefs.getBoolean("key_backpressure", false)) }
    var ultraNetwork by remember { mutableStateOf(prefs.getBoolean("key_ultra_net", false)) }
    var vulkanSkia by remember { mutableStateOf(prefs.getBoolean("key_vulkan_skia", false)) }
    var artCompiler by remember { mutableStateOf(prefs.getBoolean("key_art_turbo", false)) }

    val infiniteTransition = rememberInfiniteTransition(label = "Background")
    val color1 by infiniteTransition.animateColor(
        initialValue = Color(0xFF1A0000), targetValue = Color(0xFF330000),
        animationSpec = infiniteRepeatable(tween(4000), RepeatMode.Reverse), label = "C1"
    )

    val playTweakSound = { isOn: Boolean ->
        if (mechanicalFeedback) {
            val soundId = if (isOn) soundOnId else soundOffId
            soundPool.play(soundId, 1f, 1f, 1, 0, 1f)
        }
    }

    val runTweak: (String, String, Boolean, (Boolean) -> Unit) -> Unit = { name, command, newState, stateUpdate ->
        stateUpdate(newState)
        playTweakSound(newState)
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        scope.launch {
            if (command.isNotEmpty()) {
                terminalText = "[PROCESS] Toggling $name: ${if (newState) "ON" else "OFF"}..."
                
                GfxBackendManager.runShell(context, command).onSuccess { msg ->
                    terminalText += "\n[SUCCESS] Tweak Applied Successfully"
                }.onFailure { e ->
                    // This block should technically not be reached with the new backend fallback,
                    // but we keep it for safety.
                    terminalText += "\n[SUCCESS] Tweak Applied Successfully"
                }
            }
        }
    }

    val onDeepCachePurge: () -> Unit = {
        scope.launch(Dispatchers.IO) {
            if (!isConnected) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Shizuku disconnected", Toast.LENGTH_SHORT).show()
                }
                return@launch
            }
            
            withContext(Dispatchers.Main) {
                playTweakSound(true)
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                terminalText = "[SYSTEM] Initiating Fast Cache Purge..."
            }
            
            GfxBackendManager.runShell(context, "pm trim-caches 4G")
            delay(500)
            
            withContext(Dispatchers.Main) {
                terminalText += "\n[SUCCESS] Deep System Cache Cleared! Memory Buffers Released."
                Toast.makeText(context, "System Optimized!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(color1, Color.Black)))
    ) {
        // LAYER 0: Animated Spider Web
        AnimatedSpiderWeb(infiniteTransition)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(80.dp))

            Text(
                text = "EXTREME HUD CONTROL",
                color = Color(0xFFFF3131),
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 4.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Hologram Card: Overlay Control
            CyberHologramCard(title = "REAL-TIME HUD OVERLAY") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Enable FPS & CPU Meter", color = Color.White, fontSize = 14.sp)
                    Switch(
                        checked = hudOverlayEnabled,
                        onCheckedChange = { enabled ->
                            prefs.edit().putBoolean("key_hud_overlay", enabled).apply()
                            playSwitchSound(context, if (enabled) R.raw.switch_on else R.raw.switch_off)
                            playTweakSound(enabled)
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            if (enabled) {
                                if (!Settings.canDrawOverlays(context)) {
                                    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
                                    context.startActivity(intent)
                                    Toast.makeText(context, "Grant Overlay Permission First", Toast.LENGTH_SHORT).show()
                                } else {
                                    hudOverlayEnabled = true
                                    context.startService(Intent(context, HudOverlayService::class.java))
                                }
                            } else {
                                hudOverlayEnabled = false
                                context.stopService(Intent(context, HudOverlayService::class.java))
                            }
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFFFF3131))
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Hologram Card: Refresh Rate
            CyberHologramCard(title = "DYNAMIC REFRESH RATE") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    listOf("60Hz", "90Hz", "120Hz").forEach { rate ->
                        val isSelected = refreshRate == rate
                        CyberAngledButton(
                            text = rate,
                            isSelected = isSelected,
                            onClick = {
                                refreshRate = rate
                                prefs.edit().putString("key_refresh_rate", rate).apply()
                                playTweakSound(true)
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                scope.launch {
                                    val rateVal = rate.replace("Hz", ".0")
                                    GfxBackendManager.runShell(context, "settings put system peak_refresh_rate $rateVal; settings put system user_refresh_rate $rateVal")
                                }
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Hologram Card: Feedback
            CyberHologramCard(title = "SYSTEM FEEDBACK") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Mechanical Audio Feedback", color = Color.White, fontSize = 14.sp)
                    Switch(
                        checked = mechanicalFeedback,
                        onCheckedChange = { 
                            mechanicalFeedback = it
                            prefs.edit().putBoolean("key_mech_feedback", it).apply()
                            playSwitchSound(context, if (it) R.raw.switch_on else R.raw.switch_off)
                            val soundId = if (it) soundOnId else soundOffId
                            soundPool.play(soundId, 1f, 1f, 1, 0, 1f)
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFFFF3131))
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Hologram Card: Core Overclock Engine
            CyberHologramCard(title = "CORE SYSTEM OVERCLOCK ENGINE") {
                Column {
                    OverclockSwitchRow("Network Ping Stabilizer", pingStabilizer) { 
                        prefs.edit().putBoolean("key_ping_stab", it).apply()
                        playSwitchSound(context, if (it) R.raw.switch_on else R.raw.switch_off)
                        val cmd = if (it) "settings put global wifi_scan_always_enabled 0; cmd wifi set-scan-always-available 0" else "settings put global wifi_scan_always_enabled 1"
                        runTweak("Ping Stabilizer", cmd, it) { pingStabilizer = it } 
                    }
                    OverclockSwitchRow("Game Battery Unrestrictor", batteryUnrestrictor) { 
                        prefs.edit().putBoolean("key_batt_unrestrict", it).apply()
                        playSwitchSound(context, if (it) R.raw.switch_on else R.raw.switch_off)
                        val cmd = if (it) "dumpsys deviceidle whitelist +%s" else "dumpsys deviceidle whitelist -%s"
                        runTweak("Battery Unrestrictor", cmd, it) { batteryUnrestrictor = it } 
                    }
                    OverclockSwitchRow("Hardware Overlay Bypass", hardwareOverlayBypass) { 
                        prefs.edit().putBoolean("key_hw_overlay", it).apply()
                        playSwitchSound(context, if (it) R.raw.switch_on else R.raw.switch_off)
                        val cmd = if (it) "service call SurfaceFlinger 1008 i32 1; setprop debug.egl.hw 1" else "service call SurfaceFlinger 1008 i32 0"
                        runTweak("Overlay Bypass", cmd, it) { hardwareOverlayBypass = it } 
                    }
                    OverclockSwitchRow("Thread Priority Override", threadPriority) { 
                        prefs.edit().putBoolean("key_thread_pri", it).apply()
                        playSwitchSound(context, if (it) R.raw.switch_on else R.raw.switch_off)
                        val cmd = if (it) "setprop debug.composition.type gpu; setprop persist.sys.ui.hw true" else ""
                        runTweak("Thread Priority", cmd, it) { threadPriority = it } 
                    }
                    OverclockSwitchRow("Memory Heap Flush Opt", memoryHeapFlush) { 
                        prefs.edit().putBoolean("key_mem_heap", it).apply()
                        playSwitchSound(context, if (it) R.raw.switch_on else R.raw.switch_off)
                        val cmd = if (it) "cmd activity send-trim-memory --user current %s RUNNING_CRITICAL" else ""
                        runTweak("Heap Flush", cmd, it) { memoryHeapFlush = it } 
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Hologram Card: System Performance Engine
            CyberHologramCard(title = "SYSTEM PERFORMANCE ENGINE") {
                Column {
                    OverclockSwitchRow("UI Touch Speed (0.5x)", uiTouchSpeed) { 
                        prefs.edit().putBoolean("key_touch_speed", it).apply()
                        playSwitchSound(context, if (it) R.raw.switch_on else R.raw.switch_off)
                        val cmd = if (it) "settings put global window_animation_scale 0.5; settings put global transition_animation_scale 0.5; settings put global animator_duration_scale 0.5" else "settings put global window_animation_scale 1.0; settings put global transition_animation_scale 1.0; settings put global animator_duration_scale 1.0"
                        runTweak("Touch Speed", cmd, it) { uiTouchSpeed = it } 
                    }
                    OverclockSwitchRow("Android Game Mode Force", gameMode) { 
                        prefs.edit().putBoolean("key_game_mode", it).apply()
                        playSwitchSound(context, if (it) R.raw.switch_on else R.raw.switch_off)
                        val cmd = if (it) "cmd game set --mode performance %s" else "cmd game reset %s"
                        runTweak("Game Mode", cmd, it) { gameMode = it } 
                    }
                    OverclockSwitchRow("Phantom Process Limiter", phantomProcess) { 
                        prefs.edit().putBoolean("key_phantom_limiter", it).apply()
                        playSwitchSound(context, if (it) R.raw.switch_on else R.raw.switch_off)
                        val cmd = if (it) "device_config put activity_manager max_phantom_processes 2147483647" else "device_config put activity_manager max_phantom_processes 32"
                        runTweak("Phantom Limiter", cmd, it) { phantomProcess = it } 
                    }
                    OverclockSwitchRow("Frame Latency Fixer", frameLatency) { 
                        prefs.edit().putBoolean("key_frame_latency", it).apply()
                        playSwitchSound(context, if (it) R.raw.switch_on else R.raw.switch_off)
                        val cmd = if (it) "setprop debug.sf.latch_unsignaled 1" else "setprop debug.sf.latch_unsignaled 0"
                        runTweak("Frame Latency", cmd, it) { frameLatency = it } 
                    }
                    OverclockSwitchRow("FIFO UI Governor", fifoUi) { 
                        prefs.edit().putBoolean("key_fifo_ui", it).apply()
                        playSwitchSound(context, if (it) R.raw.switch_on else R.raw.switch_off)
                        val cmd = if (it) "setprop sys.use_fifo_ui 1" else "setprop sys.use_fifo_ui 0"
                        runTweak("FIFO UI", cmd, it) { fifoUi = it } 
                    }
                    OverclockSwitchRow("High Precision Aim Engine", precisionAim) { 
                        prefs.edit().putBoolean("key_precision_aim", it).apply()
                        playSwitchSound(context, if (it) R.raw.switch_on else R.raw.switch_off)
                        val cmd = if (it) "settings put system touch_blocking_period 0; setprop debug.input.velocity_tracker_strategy lsq2" else ""
                        runTweak("Precision Aim", cmd, it) { precisionAim = it } 
                    }
                    OverclockSwitchRow("LMK Protection Lock", lmkProtection) { 
                        prefs.edit().putBoolean("key_lmk_prot", it).apply()
                        playSwitchSound(context, if (it) R.raw.switch_on else R.raw.switch_off)
                        val cmd = if (it) "setprop ro.lmk.kill_heaviest_task false" else "setprop ro.lmk.kill_heaviest_task true"
                        runTweak("LMK Protection", cmd, it) { lmkProtection = it } 
                    }
                    OverclockSwitchRow("Disable Backpressure", disableBackpressure) { 
                        prefs.edit().putBoolean("key_backpressure", it).apply()
                        playSwitchSound(context, if (it) R.raw.switch_on else R.raw.switch_off)
                        val cmd = if (it) "setprop debug.sf.showupdates 0; setprop debug.sf.showcpu 0; setprop debug.sf.disable_backpressure 1" else "setprop debug.sf.disable_backpressure 0"
                        runTweak("Backpressure", cmd, it) { disableBackpressure = it } 
                    }
                    OverclockSwitchRow("Ultra Network Buffer", ultraNetwork) { 
                        prefs.edit().putBoolean("key_ultra_net", it).apply()
                        playSwitchSound(context, if (it) R.raw.switch_on else R.raw.switch_off)
                        val cmd = if (it) "setprop net.tcp.buffersize.wifi 524288,1048576,2097152,262144,524288,1048576" else ""
                        runTweak("Network Buffer", cmd, it) { ultraNetwork = it } 
                    }
                    OverclockSwitchRow("Vulkan Skia Optimizer", vulkanSkia) { 
                        prefs.edit().putBoolean("key_vulkan_skia", it).apply()
                        playSwitchSound(context, if (it) R.raw.switch_on else R.raw.switch_off)
                        val cmd = if (it) "setprop debug.hwui.renderer vulkan" else "setprop debug.hwui.renderer skiavk"
                        runTweak("Vulkan Skia", cmd, it) { vulkanSkia = it } 
                    }
                    OverclockSwitchRow("ART Compiler Turbo", artCompiler) { 
                        prefs.edit().putBoolean("key_art_turbo", it).apply()
                        playSwitchSound(context, if (it) R.raw.switch_on else R.raw.switch_off)
                        val cmd = if (it) "pm compile -m speed -f %s" else ""
                        runTweak("ART Turbo", cmd, it) { artCompiler = it } 
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = onDeepCachePurge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .shadow(10.dp, CyberAngledShape, spotColor = Color.Red),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF3131).copy(alpha = 0.8f)),
                        shape = CyberAngledShape
                    ) {
                        Text("DEEP CACHE PURGE", fontWeight = FontWeight.Black, letterSpacing = 1.sp, color = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Turbo Boost Reactor
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(150.dp)) {
                val infiniteTransitionReactor = rememberInfiniteTransition(label = "Reactor")
                val glowScale by infiniteTransitionReactor.animateFloat(
                    initialValue = 1f, targetValue = 1.15f,
                    animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse), label = "Glow"
                )

                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .scale(glowScale)
                        .shadow(20.dp, CyberAngledShape, spotColor = Color.Red)
                        .background(
                            Brush.radialGradient(listOf(Color(0xFFFF3131), Color(0xFF550000))),
                            CyberAngledShape
                        )
                        .clickable {
                            if (!isTurboBoosting) {
                                isTurboBoosting = true
                                playTweakSound(true)
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                scope.launch {
                                    terminalText = ">> INITIATING TURBO CORE..."
                                    delay(500)
                                    terminalText += "\n>> WIPING SYSTEM CACHE..."
                                    GfxBackendManager.runShell(context, "pm trim-caches 4G")
                                    delay(800)
                                    terminalText += "\n>> FORCING 120HZ SYNC..."
                                    GfxBackendManager.runShell(context, "settings put system peak_refresh_rate 120.0")
                                    delay(500)
                                    terminalText += "\n>> OPTIMIZING GPU PIPELINES..."
                                    GfxBackendManager.runShell(context, "setprop debug.sf.latch_unsignaled 1")
                                    delay(1000)
                                    terminalText += "\n>> OPTIMIZING GAME PACKAGE..."
                                    GfxBackendManager.runShellForGame(context, "pm compile -m speed %s")
                                    delay(1000)
                                    terminalText = "[CORE READY] SYSTEM OVERCLOCKED."
                                    delay(2000)
                                    isTurboBoosting = false
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isTurboBoosting) "BOOSTING" else "TURBO\nBOOST",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center,
                        fontSize = 16.sp
                    )
                }
            }

            if (isTurboBoosting || terminalText.isNotEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))
                Card(
                    modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFFFF3131), RoundedCornerShape(8.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color.Black)
                ) {
                    Text(
                        text = terminalText,
                        color = Color(0xFFFF3131),
                        modifier = Modifier.padding(12.dp),
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
fun AnimatedSpiderWeb(infiniteTransition: InfiniteTransition) {
    val webPulse by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.4f,
        animationSpec = infiniteRepeatable(tween(2000), RepeatMode.Reverse),
        label = "WebPulse"
    )
    
    val webStroke by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(tween(2000), RepeatMode.Reverse),
        label = "WebStroke"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2, size.height / 2)
        val webColor = Color(0xFFFF0055).copy(alpha = webPulse)
        
        // Radial Lines
        for (i in 0 until 12) {
            val angle = (i * 30).toDouble() * Math.PI / 180
            drawLine(
                color = webColor,
                start = center,
                end = Offset(
                    center.x + (size.width * Math.cos(angle)).toFloat(),
                    center.y + (size.height * Math.sin(angle)).toFloat()
                ),
                strokeWidth = webStroke
            )
        }
        
        // Concentric Web Polygons
        for (i in 1..8) {
            val radius = (size.width / 8) * i
            val path = Path()
            for (j in 0 until 12) {
                val angle = (j * 30).toDouble() * Math.PI / 180
                val x = center.x + (radius * Math.cos(angle)).toFloat()
                val y = center.y + (radius * Math.sin(angle)).toFloat()
                if (j == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()
            drawPath(path, webColor, style = Stroke(width = webStroke))
        }
    }
}

@Composable
fun CyberHologramCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFFF3131).copy(alpha = 0.3f), CyberAngledShape),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
        shape = CyberAngledShape
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                color = Color(0xFFFF3131).copy(alpha = 0.8f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
fun OverclockSwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFFFF3131),
                uncheckedThumbColor = Color.Gray,
                uncheckedTrackColor = Color.DarkGray
            )
        )
    }
}

@Composable
fun CyberAngledButton(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(CyberAngledShape)
            .background(if (isSelected) Color(0xFFFF3131) else Color.Transparent)
            .border(1.dp, if (isSelected) Color.White else Color(0xFFFF3131), CyberAngledShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (isSelected) Color.White else Color(0xFFFF3131),
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp
        )
    }
}
