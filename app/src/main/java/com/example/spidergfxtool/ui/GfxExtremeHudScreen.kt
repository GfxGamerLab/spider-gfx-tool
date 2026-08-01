package com.example.spidergfxtool.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
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
import com.example.spidergfxtool.R
import com.example.spidergfxtool.ui.components.AnimatedSpiderWeb
import com.example.spidergfxtool.ui.components.CyberTweakCard
import com.example.spidergfxtool.ui.components.CyberTweakChip
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.media.MediaPlayer
import android.media.SoundPool
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

// Performance Optimized Toggle Row
@Composable
fun CyberTweakToggleRow(
    label: String,
    checked: Boolean,
    glowAlphaState: State<Float>,
    onCheckedChange: (Boolean) -> Unit
) {
    val neonRed = remember { Color(0xFFFF3131) }
    
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label.uppercase(),
                color = if (checked) Color.White else Color.Gray,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
            Text(
                text = if (checked) "STATUS: ACTIVE" else "STATUS: STANDBY",
                color = neonRed,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.graphicsLayer {
                    alpha = if (checked) glowAlphaState.value else 0.4f
                }
            )
        }
        
        Box(
            modifier = Modifier
                .size(width = 50.dp, height = 24.dp)
                .clip(CutCornerShape(topStart = 6.dp, bottomEnd = 6.dp))
                .background(if (checked) Color(0xFF2A0000) else Color(0xFF111111))
                .graphicsLayer {
                    val glow = glowAlphaState.value
                    shadowElevation = if (checked) (8 * glow).dp.toPx() else 0f
                    alpha = if (checked) 1f else 0.8f
                }
                .shadow(
                    elevation = 0.dp, 
                    shape = CutCornerShape(topStart = 6.dp, bottomEnd = 6.dp),
                    spotColor = neonRed
                )
                .border(
                    width = 1.dp,
                    color = if (checked) neonRed else Color.DarkGray,
                    shape = CutCornerShape(topStart = 6.dp, bottomEnd = 6.dp)
                )
                .clickable { onCheckedChange(!checked) },
            contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .padding(3.dp)
                    .fillMaxHeight()
                    .width(20.dp)
                    .background(if (checked) neonRed else Color.Gray)
                    .clip(CutCornerShape(2.dp))
            )
        }
    }
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
    
    // States (Optimized)
    var hudOverlayEnabled by remember { mutableStateOf(prefs.getBoolean("key_hud_overlay", false)) }
    var refreshRate by remember { mutableStateOf(prefs.getString("key_refresh_rate", "60Hz") ?: "60Hz") }
    var mechanicalFeedback by remember { mutableStateOf(prefs.getBoolean("key_mech_feedback", false)) }
    var isTurboBoosting by remember { mutableStateOf(false) }
    var terminalText by remember { mutableStateOf("IDLE: SYSTEM READY\n>> WAITING FOR INPUT...") }

    var pingStabilizer by remember { mutableStateOf(prefs.getBoolean("key_ping_stab", false)) }
    var batteryUnrestrictor by remember { mutableStateOf(prefs.getBoolean("key_batt_unrestrict", false)) }
    var hardwareOverlayBypass by remember { mutableStateOf(prefs.getBoolean("key_hw_overlay", false)) }
    var threadPriority by remember { mutableStateOf(prefs.getBoolean("key_thread_pri", false)) }
    var memoryHeapFlush by remember { mutableStateOf(prefs.getBoolean("key_mem_heap", false)) }

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

    var thermalThrottling by remember { mutableStateOf(prefs.getBoolean("key_thermal_over", false)) }
    var artOverdrive by rememberSaveable { mutableStateOf(false) }
    var backgroundZero by remember { mutableStateOf(prefs.getBoolean("key_bg_zero", false)) }
    var tcpInjection by remember { mutableStateOf(prefs.getBoolean("key_tcp_inject", false)) }

    // Visual Transitions (Optimized)
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
    
    val neonCyan = remember { Color(0xFF00FFFF) }
    val neonRed = remember { Color(0xFFFF3131) }
    val backgroundBrush = remember(color1, color2) { Brush.verticalGradient(listOf(color1, color2, Color.Black)) }

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
        scope.launch(Dispatchers.IO) {
            if (command.isNotEmpty()) {
                withContext(Dispatchers.Main) {
                    terminalText = "[PROCESS] Toggling $name: ${if (newState) "ON" else "OFF"}..."
                }
                GfxBackendManager.runShell(context, command)
                withContext(Dispatchers.Main) {
                    terminalText += "\n[SUCCESS] $name state synced."
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
                terminalText = "[SYSTEM] Initiating Deep Cache Purge..."
            }
            
            GfxBackendManager.runShell(context, "pm trim-caches 4G")
            delay(500)
            
            withContext(Dispatchers.Main) {
                terminalText += "\n[SUCCESS] System Optimized!"
                Toast.makeText(context, "Cache Cleared!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(backgroundBrush)) {
        // LAYER 0: Animated Spider Web
        AnimatedSpiderWeb(infiniteTransition)

        Row(modifier = Modifier.fillMaxSize()) {
            // --- LEFT COLUMN: SYSTEM TWEAKS (Left Thumb) ---
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1.1f)
                    .padding(start = 24.dp, top = 24.dp, bottom = 24.dp, end = 12.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "SYSTEM OVERRIDES",
                    color = neonCyan.copy(alpha = 0.8f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )
                
                Spacer(modifier = Modifier.height(16.dp))

                // ULTIMATE SMOOTHNESS & PERFORMANCE
                CyberTweakCard(title = "ULTIMATE SMOOTHNESS & PERFORMANCE", icon = "⚡", statusHex = "0xELITE") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = {
                                scope.launch(Dispatchers.IO) {
                                    withContext(Dispatchers.Main) { terminalText = "[PROCESS] Injecting 2X UI Smoothness..." }
                                    GfxBackendManager.runShell(context, "settings put global window_animation_scale 0.5 && settings put global transition_animation_scale 0.5 && settings put global animator_duration_scale 0.5")
                                    withContext(Dispatchers.Main) { terminalText += "\n[SYSTEM ENGINE] 2X UI Smoothness Applied." }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = neonRed.copy(alpha = 0.2f)),
                            shape = CutCornerShape(8.dp),
                            border = BorderStroke(1.dp, neonRed.copy(alpha = 0.5f))
                        ) {
                            Text("ENABLE 2X UI SMOOTHNESS", fontSize = 11.sp, fontWeight = FontWeight.Black)
                        }

                        Button(
                            onClick = {
                                scope.launch(Dispatchers.IO) {
                                    withContext(Dispatchers.Main) { terminalText = "[PROCESS] Starting AOT Speed Overdrive..." }
                                    val targets = listOf("com.tencent.ig", "com.pubg.imobile")
                                    targets.forEach { pkg ->
                                        withContext(Dispatchers.Main) { terminalText += "\n>> Compiling: $pkg..." }
                                        GfxBackendManager.runShell(context, "cmd package compile -m speed -f $pkg")
                                    }
                                    withContext(Dispatchers.Main) { terminalText += "\n[SYSTEM ENGINE] ART Compiler executed. Game optimized for Max FPS." }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = neonRed.copy(alpha = 0.2f)),
                            shape = CutCornerShape(8.dp),
                            border = BorderStroke(1.dp, neonRed.copy(alpha = 0.5f))
                        ) {
                            Text("AOT GAME COMPILER (MAX FPS)", fontSize = 11.sp, fontWeight = FontWeight.Black)
                        }

                        Button(
                            onClick = {
                                scope.launch(Dispatchers.IO) {
                                    withContext(Dispatchers.Main) { terminalText = "[PROCESS] Triggering Kernel Cache Purge..." }
                                    GfxBackendManager.runShell(context, "echo 3 > /proc/sys/vm/drop_caches")
                                    withContext(Dispatchers.Main) { terminalText += "\n[SYSTEM ENGINE] Deep caches dropped. RAM freed." }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = neonRed.copy(alpha = 0.2f)),
                            shape = CutCornerShape(8.dp),
                            border = BorderStroke(1.dp, neonRed.copy(alpha = 0.5f))
                        ) {
                            Text("FORCE DROP CACHES (RAM CLEANUP)", fontSize = 11.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 🚀 PERFORMANCE ENGINE
                CyberTweakCard(title = "PERFORMANCE ENGINE", icon = "🚀", statusHex = "0xAF32") {
                    Column {
                        CyberTweakToggleRow("Thermal Illusion", thermalThrottling, glowAlphaState) { 
                            prefs.edit().putBoolean("key_thermal_over", it).apply()
                            runTweak("Thermal Illusion", if (it) "cmd thermalservice override-status 0" else "cmd thermalservice reset", it) { thermalThrottling = it } 
                        }
                        CyberTweakToggleRow("ART Overdrive", artOverdrive, glowAlphaState) {
                            if (it && !isConnected) return@CyberTweakToggleRow
                            artOverdrive = it
                            if (it) {
                                playTweakSound(true)
                                scope.launch(Dispatchers.IO) {
                                    withContext(Dispatchers.Main) { terminalText = "[PROCESS] Compiling ART Assets..." }
                                    GfxBackendManager.runShell(context, "cmd package compile -m speed -f com.tencent.ig")
                                    withContext(Dispatchers.Main) { terminalText += "\n[SUCCESS] ART Optimized." }
                                }
                            }
                        }
                        CyberTweakToggleRow("Android Game Mode", gameMode, glowAlphaState) { 
                            prefs.edit().putBoolean("key_game_mode", it).apply()
                            runTweak("Game Mode", "cmd game set --mode performance %s", it) { gameMode = it } 
                        }
                        CyberTweakToggleRow("Battery Unrestrict", batteryUnrestrictor, glowAlphaState) { 
                            prefs.edit().putBoolean("key_batt_unrestrict", it).apply()
                            runTweak("Battery", "dumpsys deviceidle whitelist +%s", it) { batteryUnrestrictor = it } 
                        }
                        CyberTweakToggleRow("Phantom Processes", phantomProcess, glowAlphaState) { 
                            prefs.edit().putBoolean("key_phantom_limiter", it).apply()
                            runTweak("Phantom Limiter", "device_config put activity_manager max_phantom_processes 2147483647", it) { phantomProcess = it } 
                        }
                        CyberTweakToggleRow("ART Turbo", artCompiler, glowAlphaState) { 
                            prefs.edit().putBoolean("key_art_turbo", it).apply()
                            runTweak("ART Turbo", "pm compile -m speed -f %s", it) { artCompiler = it } 
                        }
                        CyberTweakToggleRow("Thread Priority", threadPriority, glowAlphaState) { 
                            prefs.edit().putBoolean("key_thread_pri", it).apply()
                            runTweak("Thread Priority", "setprop debug.composition.type gpu", it) { threadPriority = it } 
                        }
                        CyberTweakToggleRow("LMK Protection", lmkProtection, glowAlphaState) { 
                            prefs.edit().putBoolean("key_lmk_prot", it).apply()
                            runTweak("LMK Protection", "setprop ro.lmk.kill_heaviest_task false", it) { lmkProtection = it } 
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 🌐 NETWORK ACCELERATION
                CyberTweakCard(title = "NETWORK ACCELERATION", icon = "🌐", statusHex = "0xNW99") {
                    Column {
                        CyberTweakToggleRow("TCP Injection", tcpInjection, glowAlphaState) { 
                            prefs.edit().putBoolean("key_tcp_inject", it).apply()
                            runTweak("TCP Injection", if (it) "settings put global tcp_default_init_rwnd 60" else "settings delete global tcp_default_init_rwnd", it) { tcpInjection = it } 
                        }
                        CyberTweakToggleRow("Ultra Network", ultraNetwork, glowAlphaState) { 
                            prefs.edit().putBoolean("key_ultra_net", it).apply()
                            runTweak("Ultra Network", "setprop net.tcp.buffersize.wifi 524288,1048576,2097152", it) { ultraNetwork = it } 
                        }
                        CyberTweakToggleRow("Ping Stabilizer", pingStabilizer, glowAlphaState) { 
                            prefs.edit().putBoolean("key_ping_stab", it).apply()
                            runTweak("Ping Stabilizer", "settings put global wifi_scan_always_enabled 0", it) { pingStabilizer = it } 
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 🕸️ HUD & VISUALS
                CyberTweakCard(title = "HUD & VISUALS", icon = "🕸️", statusHex = "0xV721") {
                    Column {
                        CyberTweakToggleRow("Real-Time HUD", hudOverlayEnabled, glowAlphaState) { 
                            if (!Settings.canDrawOverlays(context)) {
                                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
                                context.startActivity(intent)
                            } else {
                                hudOverlayEnabled = it
                                prefs.edit().putBoolean("key_hud_overlay", it).apply()
                                if (it) {
                                    playSwitchSound(context, R.raw.switch_on)
                                    context.startService(Intent(context, HudOverlayService::class.java)) 
                                } else {
                                    playSwitchSound(context, R.raw.switch_off)
                                    context.stopService(Intent(context, HudOverlayService::class.java))
                                }
                            }
                        }
                        
                        Text("DYNAMIC REFRESH RATE", color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp, bottom = 8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            listOf("60Hz", "90Hz", "120Hz").forEach { rate ->
                                val isSelected = refreshRate == rate
                                CyberTweakChip(rate, isSelected) {
                                    refreshRate = rate
                                    prefs.edit().putString("key_refresh_rate", rate).apply()
                                    playTweakSound(true)
                                    scope.launch {
                                        val rateVal = rate.replace("Hz", ".0")
                                        GfxBackendManager.runShell(context, "settings put system peak_refresh_rate $rateVal")
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        CyberTweakToggleRow("Mech Feedback", mechanicalFeedback, glowAlphaState) { 
                            mechanicalFeedback = it
                            prefs.edit().putBoolean("key_mech_feedback", it).apply()
                            playSwitchSound(context, if (it) R.raw.switch_on else R.raw.switch_off)
                        }
                        CyberTweakToggleRow("HW Overlay Bypass", hardwareOverlayBypass, glowAlphaState) { 
                            prefs.edit().putBoolean("key_hw_overlay", it).apply()
                            runTweak("Overlay Bypass", "service call SurfaceFlinger 1008 i32 1", it) { hardwareOverlayBypass = it } 
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // ⚙️ SYSTEM OPTIMIZATION
                CyberTweakCard(title = "SYSTEM OPTIMIZATION", icon = "⚙️", statusHex = "0xDB99") {
                    Column {
                        CyberTweakToggleRow("UI Touch Speed", uiTouchSpeed, glowAlphaState) { 
                            prefs.edit().putBoolean("key_touch_speed", it).apply()
                            runTweak("Touch Speed", "settings put global window_animation_scale 0.5", it) { uiTouchSpeed = it } 
                        }
                        CyberTweakToggleRow("High Precision Aim", precisionAim, glowAlphaState) { 
                            prefs.edit().putBoolean("key_precision_aim", it).apply()
                            runTweak("Precision Aim", "setprop debug.input.velocity_tracker_strategy lsq2", it) { precisionAim = it } 
                        }
                        CyberTweakToggleRow("Frame Latency", frameLatency, glowAlphaState) { 
                            prefs.edit().putBoolean("key_frame_latency", it).apply()
                            runTweak("Frame Latency", "setprop debug.sf.latch_unsignaled 1", it) { frameLatency = it } 
                        }
                        CyberTweakToggleRow("FIFO UI Governor", fifoUi, glowAlphaState) { 
                            prefs.edit().putBoolean("key_fifo_ui", it).apply()
                            runTweak("FIFO UI", "setprop sys.use_fifo_ui 1", it) { fifoUi = it } 
                        }
                        CyberTweakToggleRow("Vulkan Skia Opt", vulkanSkia, glowAlphaState) { 
                            prefs.edit().putBoolean("key_vulkan_skia", it).apply()
                            runTweak("Vulkan Skia", "setprop debug.hwui.renderer vulkan", it) { vulkanSkia = it } 
                        }
                        CyberTweakToggleRow("Memory Flush", memoryHeapFlush, glowAlphaState) { 
                            prefs.edit().putBoolean("key_mem_heap", it).apply()
                            runTweak("Heap Flush", "cmd activity send-trim-memory --user current %s RUNNING_CRITICAL", it) { memoryHeapFlush = it } 
                        }
                        CyberTweakToggleRow("Disable Backpress", disableBackpressure, glowAlphaState) { 
                            prefs.edit().putBoolean("key_backpressure", it).apply()
                            runTweak("Backpressure", "setprop debug.sf.disable_backpressure 1", it) { disableBackpressure = it } 
                        }
                        CyberTweakToggleRow("Zero-State BG", backgroundZero, glowAlphaState) { 
                            prefs.edit().putBoolean("key_bg_zero", it).apply()
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            scope.launch(Dispatchers.IO) {
                                withContext(Dispatchers.Main) { terminalText = "[PROCESS] Freezing Background Apps..." }
                                GfxBackendManager.runShell(context, "cmd appops set com.facebook.katana RUN_IN_BACKGROUND ignore")
                                withContext(Dispatchers.Main) {
                                    terminalText += "\n[SUCCESS] Backgrounds Suppressed."
                                    backgroundZero = it
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))

                // MASTER ACTIONS
                Button(
                    onClick = onDeepCachePurge,
                    modifier = Modifier.fillMaxWidth().height(45.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF3131).copy(alpha = 0.2f)),
                    shape = CutCornerShape(topStart = 8.dp, bottomEnd = 8.dp),
                    border = BorderStroke(1.dp, Color(0xFFFF3131).copy(alpha = 0.5f))
                ) {
                    Text("DEEP CACHE PURGE", fontWeight = FontWeight.Black, fontSize = 12.sp, color = Color.White)
                }

                Spacer(modifier = Modifier.height(40.dp))

                // Turbo Boost Reactor
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(160.dp).align(Alignment.CenterHorizontally)) {
                    val reactorPulseState = infiniteTransition.animateFloat(
                        initialValue = 1f, targetValue = 1.1f,
                        animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse), label = "Reactor"
                    )

                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .graphicsLayer {
                                val pulse = reactorPulseState.value
                                scaleX = pulse
                                scaleY = pulse
                            }
                            .shadow(20.dp, CutCornerShape(12.dp), spotColor = Color.Red)
                            .background(
                                Brush.radialGradient(listOf(Color(0xFFFF3131).copy(alpha = 0.8f), Color(0xFF2A0000))),
                                CutCornerShape(12.dp)
                            )
                            .clickable {
                                if (!isTurboBoosting) {
                                    isTurboBoosting = true
                                    playTweakSound(true)
                                    scope.launch {
                                        terminalText = ">> INJECTING TURBO CORE..."
                                        GfxBackendManager.runShell(context, "pm trim-caches 4G")
                                        delay(500)
                                        terminalText += "\n>> FORCING SYSTEM REFRESH..."
                                        GfxBackendManager.runShell(context, "settings put system peak_refresh_rate 120.0")
                                        delay(1000)
                                        terminalText = "[CORE READY] ENGINE OVERCLOCKED."
                                        delay(2000)
                                        isTurboBoosting = false
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isTurboBoosting) "INJECTING" else "TURBO\nREACTOR",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.Center,
                            fontSize = 14.sp,
                            letterSpacing = 2.sp
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
                    Canvas(modifier = Modifier.fillMaxSize().graphicsLayer { rotationZ = glowAlphaState.value * 360f }) {
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
                            text = terminalText,
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
                        text = "CORE ENGINE SYNCED",
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

