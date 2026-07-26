package com.example.spidergfxtool.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.spidergfxtool.GfxBackendManager
import com.example.spidergfxtool.HudOverlayService
import com.example.spidergfxtool.R
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

// Cyber Engine Overrides UI
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

    // Extreme Overclock States
    var thermalThrottling by remember { mutableStateOf(prefs.getBoolean("key_thermal_over", false)) }
    var artOverdrive by rememberSaveable { mutableStateOf(false) }
    var backgroundZero by remember { mutableStateOf(prefs.getBoolean("key_bg_zero", false)) }
    var tcpInjection by remember { mutableStateOf(prefs.getBoolean("key_tcp_inject", false)) }

    // Unified Background Spec from Dashboard
    val infiniteTransition = rememberInfiniteTransition(label = "TweakEngine")
    val color1 by infiniteTransition.animateColor(
        initialValue = Color(0xFF001F3F), targetValue = Color(0xFF3F0000),
        animationSpec = infiniteRepeatable(tween(4000), RepeatMode.Reverse), label = "C1"
    )
    val color2 by infiniteTransition.animateColor(
        initialValue = Color(0xFF00003F), targetValue = Color(0xFF800000),
        animationSpec = infiniteRepeatable(tween(4000, 1000), RepeatMode.Reverse), label = "C2"
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(color1, color2, Color.Black)))
    ) {
        // LAYER 0: Animated Spider Web (Matches Dashboard's atmosphere but kept for tweaks page)
        AnimatedSpiderWeb(infiniteTransition)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(70.dp))

            Text(
                text = "ENGINE OVERRIDE CENTER",
                color = Color(0xFFFF3131),
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 5.sp,
                textAlign = TextAlign.Center
            )
            
            Text(
                text = "STATUS: ${if(isConnected) "ENCRYPTED LINK ACTIVE" else "LINK OFFLINE"}",
                color = if(isConnected) Color.Green.copy(alpha = 0.6f) else Color.Red.copy(alpha = 0.6f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // --- 🚀 PERFORMANCE ENGINE ZONE ---
            CyberTweakCard(
                title = "PERFORMANCE ENGINE",
                icon = "🚀",
                statusHex = "0xAF32"
            ) {
                Column {
                    CyberTweakToggleRow("Battery Unrestrictor", batteryUnrestrictor, infiniteTransition) { 
                        prefs.edit().putBoolean("key_batt_unrestrict", it).apply()
                        playSwitchSound(context, if (it) R.raw.switch_on else R.raw.switch_off)
                        runTweak("Battery Unrestrictor", "dumpsys deviceidle whitelist +%s", it) { batteryUnrestrictor = it } 
                    }
                    CyberTweakToggleRow("Android Game Mode", gameMode, infiniteTransition) { 
                        prefs.edit().putBoolean("key_game_mode", it).apply()
                        playSwitchSound(context, if (it) R.raw.switch_on else R.raw.switch_off)
                        runTweak("Game Mode", "cmd game set --mode performance %s", it) { gameMode = it } 
                    }
                    CyberTweakToggleRow("Phantom Process Limit", phantomProcess, infiniteTransition) { 
                        prefs.edit().putBoolean("key_phantom_limiter", it).apply()
                        playSwitchSound(context, if (it) R.raw.switch_on else R.raw.switch_off)
                        runTweak("Phantom Limiter", "device_config put activity_manager max_phantom_processes 2147483647", it) { phantomProcess = it } 
                    }
                    CyberTweakToggleRow("ART Compiler Turbo", artCompiler, infiniteTransition) { 
                        prefs.edit().putBoolean("key_art_turbo", it).apply()
                        playSwitchSound(context, if (it) R.raw.switch_on else R.raw.switch_off)
                        runTweak("ART Turbo", "pm compile -m speed -f %s", it) { artCompiler = it } 
                    }
                    CyberTweakToggleRow("Thread Priority Lock", threadPriority, infiniteTransition) { 
                        prefs.edit().putBoolean("key_thread_pri", it).apply()
                        playSwitchSound(context, if (it) R.raw.switch_on else R.raw.switch_off)
                        runTweak("Thread Priority", "setprop debug.composition.type gpu", it) { threadPriority = it } 
                    }
                    CyberTweakToggleRow("LMK Protection", lmkProtection, infiniteTransition) { 
                        prefs.edit().putBoolean("key_lmk_prot", it).apply()
                        playSwitchSound(context, if (it) R.raw.switch_on else R.raw.switch_off)
                        runTweak("LMK Protection", "setprop ro.lmk.kill_heaviest_task false", it) { lmkProtection = it } 
                    }
                    CyberTweakToggleRow("Thermal Illusion (Fake Cold)", thermalThrottling, infiniteTransition) { 
                        prefs.edit().putBoolean("key_thermal_over", it).apply()
                        playSwitchSound(context, if (it) R.raw.switch_on else R.raw.switch_off)
                        val cmd = if (it) "cmd thermalservice override-status 0" else "cmd thermalservice reset"
                        runTweak("Thermal Illusion", cmd, it) { thermalThrottling = it } 
                    }
                    CyberTweakToggleRow("ART Engine Speed Overdrive", artOverdrive, infiniteTransition) { 
                        if (it && !isConnected) {
                            Toast.makeText(context, "Shizuku disconnected", Toast.LENGTH_SHORT).show()
                            return@CyberTweakToggleRow
                        }
                        
                        // FIX: Update state IMMEDIATELY and don't reset unless user manually toggles off
                        artOverdrive = it
                        if (it) {
                            playTweakSound(true)
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            
                            scope.launch(Dispatchers.IO) {
                                withContext(Dispatchers.Main) {
                                    terminalText = "[SYSTEM] Initializing ART Overdrive..."
                                    terminalText += "\n[INFO] Compiling ART, please wait..."
                                    Toast.makeText(context, "Optimizing ART Engine... Please wait.", Toast.LENGTH_LONG).show()
                                }
                                
                                val installed = GfxBackendManager.getInstalledGamePackages(context)
                                if (installed.isEmpty()) {
                                    withContext(Dispatchers.Main) {
                                        terminalText += "\n[ERROR] No target package detected."
                                        // Keeping it true as requested, but indicating error in terminal
                                    }
                                    return@launch
                                }
                                
                                installed.forEach { pkg ->
                                    withContext(Dispatchers.Main) { terminalText += "\n[PROCESS] Overclocking $pkg..." }
                                    GfxBackendManager.runShell(context, "cmd package compile -m speed -f $pkg")
                                }
                                
                                withContext(Dispatchers.Main) {
                                    terminalText += "\n[SUCCESS] ART Overdrive Complete."
                                    Toast.makeText(context, "ART Overdrive Applied!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } else {
                            playTweakSound(false)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // --- 🌐 NETWORK ACCELERATION ZONE ---
            CyberTweakCard(
                title = "NETWORK ACCELERATION",
                icon = "🌐",
                statusHex = "0xNW99"
            ) {
                Column {
                    CyberTweakToggleRow("Network Ping Stabilizer", pingStabilizer, infiniteTransition) { 
                        prefs.edit().putBoolean("key_ping_stab", it).apply()
                        playSwitchSound(context, if (it) R.raw.switch_on else R.raw.switch_off)
                        runTweak("Ping Stabilizer", "settings put global wifi_scan_always_enabled 0", it) { pingStabilizer = it } 
                    }
                    CyberTweakToggleRow("Ultra Network Buffer", ultraNetwork, infiniteTransition) { 
                        prefs.edit().putBoolean("key_ultra_net", it).apply()
                        playSwitchSound(context, if (it) R.raw.switch_on else R.raw.switch_off)
                        runTweak("Network Buffer", "setprop net.tcp.buffersize.wifi 524288,1048576,2097152", it) { ultraNetwork = it } 
                    }
                    CyberTweakToggleRow("TCP Window Size Injection", tcpInjection, infiniteTransition) { 
                        prefs.edit().putBoolean("key_tcp_inject", it).apply()
                        playSwitchSound(context, if (it) R.raw.switch_on else R.raw.switch_off)
                        val cmd = if (it) "settings put global tcp_default_init_rwnd 60" else "settings delete global tcp_default_init_rwnd"
                        runTweak("TCP Injection", cmd, it) { tcpInjection = it } 
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // --- 🕸️ HUD OVERLAY & VISUALS ZONE ---
            CyberTweakCard(
                title = "HUD OVERLAY & VISUALS",
                icon = "🕸️",
                statusHex = "0xV721"
            ) {
                Column {
                    CyberTweakToggleRow("Real-Time HUD Meter", hudOverlayEnabled, infiniteTransition) { 
                        if (!Settings.canDrawOverlays(context)) {
                            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
                            context.startActivity(intent)
                        } else {
                            hudOverlayEnabled = it
                            prefs.edit().putBoolean("key_hud_overlay", it).apply()
                            playSwitchSound(context, if (it) R.raw.switch_on else R.raw.switch_off)
                            if (it) context.startService(Intent(context, HudOverlayService::class.java)) 
                            else context.stopService(Intent(context, HudOverlayService::class.java))
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
                    CyberTweakToggleRow("Mechanical Feedback", mechanicalFeedback, infiniteTransition) { 
                        mechanicalFeedback = it
                        prefs.edit().putBoolean("key_mech_feedback", it).apply()
                        playSwitchSound(context, if (it) R.raw.switch_on else R.raw.switch_off)
                    }
                    CyberTweakToggleRow("Hardware Overlay Bypass", hardwareOverlayBypass, infiniteTransition) { 
                        prefs.edit().putBoolean("key_hw_overlay", it).apply()
                        playSwitchSound(context, if (it) R.raw.switch_on else R.raw.switch_off)
                        runTweak("Overlay Bypass", "service call SurfaceFlinger 1008 i32 1", it) { hardwareOverlayBypass = it } 
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // --- ⚙️ SYSTEM OPTIMIZATION ZONE ---
            CyberTweakCard(
                title = "SYSTEM OPTIMIZATION",
                icon = "⚙️",
                statusHex = "0xDB99"
            ) {
                Column {
                    CyberTweakToggleRow("UI Touch Speed (0.5x)", uiTouchSpeed, infiniteTransition) { 
                        prefs.edit().putBoolean("key_touch_speed", it).apply()
                        playSwitchSound(context, if (it) R.raw.switch_on else R.raw.switch_off)
                        runTweak("Touch Speed", "settings put global window_animation_scale 0.5", it) { uiTouchSpeed = it } 
                    }
                    CyberTweakToggleRow("High Precision Aim", precisionAim, infiniteTransition) { 
                        prefs.edit().putBoolean("key_precision_aim", it).apply()
                        playSwitchSound(context, if (it) R.raw.switch_on else R.raw.switch_off)
                        runTweak("Precision Aim", "setprop debug.input.velocity_tracker_strategy lsq2", it) { precisionAim = it } 
                    }
                    CyberTweakToggleRow("Frame Latency Fixer", frameLatency, infiniteTransition) { 
                        prefs.edit().putBoolean("key_frame_latency", it).apply()
                        playSwitchSound(context, if (it) R.raw.switch_on else R.raw.switch_off)
                        runTweak("Frame Latency", "setprop debug.sf.latch_unsignaled 1", it) { frameLatency = it } 
                    }
                    CyberTweakToggleRow("FIFO UI Governor", fifoUi, infiniteTransition) { 
                        prefs.edit().putBoolean("key_fifo_ui", it).apply()
                        playSwitchSound(context, if (it) R.raw.switch_on else R.raw.switch_off)
                        runTweak("FIFO UI", "setprop sys.use_fifo_ui 1", it) { fifoUi = it } 
                    }
                    CyberTweakToggleRow("Vulkan Skia Optimizer", vulkanSkia, infiniteTransition) { 
                        prefs.edit().putBoolean("key_vulkan_skia", it).apply()
                        playSwitchSound(context, if (it) R.raw.switch_on else R.raw.switch_off)
                        runTweak("Vulkan Skia", "setprop debug.hwui.renderer vulkan", it) { vulkanSkia = it } 
                    }
                    CyberTweakToggleRow("Memory Heap Flush", memoryHeapFlush, infiniteTransition) { 
                        prefs.edit().putBoolean("key_mem_heap", it).apply()
                        playSwitchSound(context, if (it) R.raw.switch_on else R.raw.switch_off)
                        runTweak("Heap Flush", "cmd activity send-trim-memory --user current %s RUNNING_CRITICAL", it) { memoryHeapFlush = it } 
                    }
                    CyberTweakToggleRow("Disable Backpressure", disableBackpressure, infiniteTransition) { 
                        prefs.edit().putBoolean("key_backpressure", it).apply()
                        playSwitchSound(context, if (it) R.raw.switch_on else R.raw.switch_off)
                        runTweak("Backpressure", "setprop debug.sf.disable_backpressure 1", it) { disableBackpressure = it } 
                    }
                    CyberTweakToggleRow("Background Zero-State", backgroundZero, infiniteTransition) { 
                        prefs.edit().putBoolean("key_bg_zero", it).apply()
                        playSwitchSound(context, if (it) R.raw.switch_on else R.raw.switch_off)
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        scope.launch {
                            val apps = listOf("com.facebook.katana", "com.instagram.android", "com.whatsapp")
                            val mode = if (it) "ignore" else "allow"
                            terminalText = "[PROCESS] Injecting Zero-State to Social Apps..."
                            apps.forEach { pkg ->
                                GfxBackendManager.runShell(context, "cmd appops set $pkg RUN_IN_BACKGROUND $mode")
                            }
                            terminalText += "\n[SUCCESS] Background Zero-State Active."
                            backgroundZero = it
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = onDeepCachePurge,
                        modifier = Modifier.fillMaxWidth().height(45.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF3131).copy(alpha = 0.2f)),
                        shape = CutCornerShape(topStart = 8.dp, bottomEnd = 8.dp),
                        border = BorderStroke(1.dp, Color(0xFFFF3131).copy(alpha = 0.5f))
                    ) {
                        Text("INITIATE DEEP CACHE PURGE", fontWeight = FontWeight.Black, fontSize = 12.sp, color = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Turbo Boost Reactor (Updated to match dashboard hero style)
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(160.dp)) {
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

            if (isTurboBoosting || terminalText.isNotEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))
                Card(
                    modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFFFF3131).copy(alpha = 0.5f), CutCornerShape(8.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.8f)),
                    shape = CutCornerShape(8.dp)
                ) {
                    Text(
                        text = terminalText,
                        color = Color(0xFFFF3131),
                        modifier = Modifier.padding(12.dp),
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
fun CyberTweakCard(
    title: String,
    icon: String,
    statusHex: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFFF3131).copy(alpha = 0.2f), CutCornerShape(topStart = 12.dp, bottomEnd = 12.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.4f)),
        shape = CutCornerShape(topStart = 12.dp, bottomEnd = 12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(icon, fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    )
                }
                Text(
                    text = "ID: $statusHex",
                    color = Color(0xFFFF3131).copy(alpha = 0.4f),
                    fontSize = 9.sp,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
            }
            
            // Decorative Divider Line
            Box(modifier = Modifier.padding(vertical = 12.dp).fillMaxWidth().height(1.dp).background(Brush.horizontalGradient(listOf(Color(0xFFFF3131).copy(alpha = 0.3f), Color.Transparent))))
            
            content()
            
            // Subtle tech footer
            Row(modifier = Modifier.padding(top = 12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Text("SECURE ENGINE PROTOCOL v2.0", color = Color.White.copy(alpha = 0.1f), fontSize = 8.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun CyberTweakToggleRow(
    label: String,
    checked: Boolean,
    infiniteTransition: InfiniteTransition,
    onCheckedChange: (Boolean) -> Unit
) {
    val glowAlphaState = infiniteTransition.animateFloat(
        initialValue = 0.5f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse), label = "ToggleGlow"
    )

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
                color = Color.Red,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.graphicsLayer {
                    alpha = if (checked) glowAlphaState.value else 0.4f
                }
            )
        }
        
        // Custom Sci-Fi Toggle with breathing glow optimized via graphicsLayer
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
                    spotColor = Color.Red
                )
                .border(
                    width = 1.dp,
                    color = if (checked) Color(0xFFFF3131) else Color.DarkGray,
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
                    .background(if (checked) Color(0xFFFF3131) else Color.Gray)
                    .clip(CutCornerShape(2.dp))
            )
        }
    }
}

@Composable
fun CyberTweakChip(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(CutCornerShape(topStart = 4.dp, bottomEnd = 4.dp))
            .background(if (isSelected) Color(0xFF330000) else Color.Transparent)
            .border(1.dp, if (isSelected) Color(0xFFFF3131) else Color.DarkGray, CutCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (isSelected) Color.White else Color.Gray,
            fontWeight = FontWeight.Black,
            fontSize = 11.sp
        )
    }
}

@Composable
fun AnimatedSpiderWeb(infiniteTransition: InfiniteTransition) {
    val webAlphaState = infiniteTransition.animateFloat(
        initialValue = 0.05f,
        targetValue = 0.15f,
        animationSpec = infiniteRepeatable(tween(3000), RepeatMode.Reverse),
        label = "WebPulse"
    )

    Canvas(modifier = Modifier
        .fillMaxSize()
        .graphicsLayer {
            alpha = webAlphaState.value
        }
    ) {
        val center = Offset(size.width / 2, size.height / 2)
        val webColor = Color(0xFFFF3131) // Opaque color, alpha handled by graphicsLayer
        
        for (i in 0 until 12) {
            val angle = (i * 30).toDouble() * Math.PI / 180
            drawLine(
                color = webColor,
                start = center,
                end = Offset(
                    center.x + (size.width * Math.cos(angle)).toFloat(),
                    center.y + (size.height * Math.sin(angle)).toFloat()
                ),
                strokeWidth = 1f
            )
        }
        
        for (i in 1..8) {
            val radius = (size.width / 6) * i
            val path = Path()
            for (j in 0 until 12) {
                val angle = (j * 30).toDouble() * Math.PI / 180
                val x = center.x + (radius * Math.cos(angle)).toFloat()
                val y = center.y + (radius * Math.sin(angle)).toFloat()
                if (j == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()
            drawPath(path, webColor, style = Stroke(width = 1f))
        }
    }
}
