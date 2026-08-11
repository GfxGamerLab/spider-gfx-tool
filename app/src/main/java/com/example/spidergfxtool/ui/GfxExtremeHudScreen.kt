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
import androidx.compose.ui.hapticfeedback.HapticFeedback
import com.example.spidergfxtool.SoundEffectsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Performance Optimized Toggle Row
@Composable
fun CyberTweakToggleRow(
    label: String,
    description: String,
    icon: String,
    checked: Boolean,
    glowAlphaState: State<Float>,
    onCheckedChange: (Boolean) -> Unit
) {
    val neonRed = remember { Color(0xFFFF3131) }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
            .graphicsLayer {
                if (checked) {
                    shadowElevation = (glowAlphaState.value * 12).dp.toPx()
                }
            }
            .shadow(
                elevation = if (checked) (glowAlphaState.value * 8).dp else 0.dp,
                shape = RoundedCornerShape(12.dp),
                spotColor = neonRed
            )
            .border(
                width = 1.dp,
                color = if (checked) neonRed.copy(alpha = glowAlphaState.value) else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .background(
                color = if (checked) neonRed.copy(alpha = 0.05f) else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            // Neon Icon
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        color = if (checked) neonRed.copy(alpha = 0.2f) else Color.DarkGray.copy(alpha = 0.2f),
                        shape = CircleShape
                    )
                    .border(
                        1.dp,
                        if (checked) neonRed.copy(alpha = 0.5f) else Color.Gray.copy(alpha = 0.3f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(text = icon, fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = label.uppercase(),
                    color = if (checked) Color.White else Color.Gray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
                Text(
                    text = description,
                    color = if (checked) neonRed.copy(alpha = 0.7f) else Color.Gray.copy(alpha = 0.5f),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 10.sp
                )
            }
        }
        
        Switch(
            checked = checked,
            onCheckedChange = { onCheckedChange(it) },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = neonRed,
                uncheckedThumbColor = Color.Gray,
                uncheckedTrackColor = Color.DarkGray.copy(alpha = 0.5f)
            )
        )
    }
}

@Composable
fun GfxExtremeHudScreen() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("spider_gfx_prefs", android.content.Context.MODE_PRIVATE) }
    
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val isConnected = GfxBackendManager.isShizukuConnected.value
    
    // States (Optimized)
    var hudOverlayEnabled by remember { mutableStateOf(prefs.getBoolean("key_hud_overlay", false)) }
    var refreshRate by remember { mutableStateOf(prefs.getString("key_refresh_rate", "60Hz") ?: "60Hz") }
    
    // MECH FEEDBACK - Default to TRUE
    var mechanicalFeedback by remember { mutableStateOf(prefs.getBoolean("key_mech_feedback", true)) }
    
    var isTurboBoosting by remember { mutableStateOf(false) }
    var terminalText by remember { mutableStateOf("IDLE: SYSTEM ENGINE ACTIVE\n>> STABILITY PROTOCOL ARMED...") }

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

    // BRAND-SPECIFIC TWEAK STATES
    var samsungGosKiller by remember { mutableStateOf(prefs.getBoolean("key_samsung_gos", false)) }
    var xiaomiJoyose by remember { mutableStateOf(prefs.getBoolean("key_xiaomi_joyose", false)) }
    var brandAggressiveDoze by remember { mutableStateOf(prefs.getBoolean("key_brand_doze", false)) }

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

    val triggerActionFeedback = { isOn: Boolean ->
        if (isOn) SoundEffectsManager.playOn() else SoundEffectsManager.playOff()
        SoundEffectsManager.performHaptic(haptic)
    }

    val runTweak: (String, String, Boolean, (Boolean) -> Unit) -> Unit = { name, command, newState, stateUpdate ->
        stateUpdate(newState)
        triggerActionFeedback(newState)
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
                triggerActionFeedback(true)
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
                                triggerActionFeedback(true)
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
                                triggerActionFeedback(true)
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
                                triggerActionFeedback(true)
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
                        CyberTweakToggleRow("Thermal Illusion", "Bypasses system thermal limits for stable FPS", "🌡️", thermalThrottling, glowAlphaState) { 
                            prefs.edit().putBoolean("key_thermal_over", it).apply()
                            runTweak("Thermal Illusion", if (it) "cmd thermalservice override-status 0" else "cmd thermalservice reset", it) { thermalThrottling = it } 
                        }
                        CyberTweakToggleRow("ART Overdrive", "Optimizes Android Runtime for game assets", "💎", artOverdrive, glowAlphaState) {
                            if (it && !isConnected) return@CyberTweakToggleRow
                            artOverdrive = it
                            if (it) {
                                triggerActionFeedback(true)
                                scope.launch(Dispatchers.IO) {
                                    withContext(Dispatchers.Main) { terminalText = "[PROCESS] Compiling ART Assets..." }
                                    GfxBackendManager.runShell(context, "cmd package compile -m speed -f com.tencent.ig")
                                    withContext(Dispatchers.Main) { terminalText += "\n[SUCCESS] ART Optimized." }
                                }
                            }
                        }
                        CyberTweakToggleRow("Android Game Mode", "Enables system-level performance mode", "🎮", gameMode, glowAlphaState) { 
                            prefs.edit().putBoolean("key_game_mode", it).apply()
                            runTweak("Game Mode", "cmd game set --mode performance %s", it) { gameMode = it } 
                        }
                        CyberTweakToggleRow("Battery Unrestrict", "Lifts power-saving caps on game process", "🔋", batteryUnrestrictor, glowAlphaState) { 
                            prefs.edit().putBoolean("key_batt_unrestrict", it).apply()
                            runTweak("Battery", "dumpsys deviceidle whitelist +%s", it) { batteryUnrestrictor = it } 
                        }
                        CyberTweakToggleRow("Phantom Processes", "Optimizes background process management", "👻", phantomProcess, glowAlphaState) { 
                            prefs.edit().putBoolean("key_phantom_limiter", it).apply()
                            runTweak("Phantom Limiter", "device_config put activity_manager max_phantom_processes 2147483647", it) { phantomProcess = it } 
                        }
                        CyberTweakToggleRow("LMK Protection", "Prevents Low Memory Killer from closing game", "🛡️", lmkProtection, glowAlphaState) { 
                            prefs.edit().putBoolean("key_lmk_prot", it).apply()
                            runTweak("LMK Protection", "setprop ro.lmk.kill_heaviest_task false", it) { lmkProtection = it } 
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 📱 BRAND-SPECIFIC HARDWARE MODULES
                CyberTweakCard(title = "DEVICE-SPECIFIC HARDWARE MODULES", icon = "📱", statusHex = "0xBRND") {
                    Column {
                        // SAMSUNG
                        CyberTweakToggleRow("Samsung GOS Killer", "Disables Game Optimization Service to stop throttling", "🇰🇷", samsungGosKiller, glowAlphaState) {
                            prefs.edit().putBoolean("key_samsung_gos", it).apply()
                            val cmd = if (it) "pm disable-user --user 0 com.samsung.android.game.gos" else "pm enable --user 0 com.samsung.android.game.gos"
                            runTweak("Samsung GOS", cmd, it) { samsungGosKiller = it }
                        }
                        // XIAOMI
                        CyberTweakToggleRow("Xiaomi Joyose Disabler", "Stops frame-drops caused by Xiaomi's cloud limiter", "🇨🇳", xiaomiJoyose, glowAlphaState) {
                            prefs.edit().putBoolean("key_xiaomi_joyose", it).apply()
                            val cmd = if (it) "pm disable-user --user 0 com.xiaomi.joyose" else "pm enable --user 0 com.xiaomi.joyose"
                            runTweak("Xiaomi Joyose", cmd, it) { xiaomiJoyose = it }
                        }
                        // UNIVERSAL (OPPO/INFINIX/ETC)
                        CyberTweakToggleRow("Aggressive Doze & RAM Flush", "Forces 100% RAM allocation to game window", "🐉", brandAggressiveDoze, glowAlphaState) {
                            prefs.edit().putBoolean("key_brand_doze", it).apply()
                            val cmd = if (it) "cmd deviceidle force-idle && am kill-all" else "cmd deviceidle unforce"
                            runTweak("Aggressive RAM", cmd, it) { brandAggressiveDoze = it }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 🌐 NETWORK ACCELERATION
                CyberTweakCard(title = "NETWORK ACCELERATION", icon = "🌐", statusHex = "0xNW99") {
                    Column {
                        CyberTweakToggleRow("TCP Injection", "Reduces packet delay via TCP window optimization", "🛰️", tcpInjection, glowAlphaState) { 
                            prefs.edit().putBoolean("key_tcp_inject", it).apply()
                            runTweak("TCP Injection", if (it) "settings put global tcp_default_init_rwnd 60" else "settings delete global tcp_default_init_rwnd", it) { tcpInjection = it } 
                        }
                        CyberTweakToggleRow("Ultra Network", "Allocates priority bandwidth to WiFi/Data", "📶", ultraNetwork, glowAlphaState) { 
                            prefs.edit().putBoolean("key_ultra_net", it).apply()
                            runTweak("Ultra Network", "setprop net.tcp.buffersize.wifi 524288,1048576,2097152", it) { ultraNetwork = it } 
                        }
                        CyberTweakToggleRow("Ping Stabilizer", "Disables background WiFi scanning during match", "📡", pingStabilizer, glowAlphaState) { 
                            prefs.edit().putBoolean("key_ping_stab", it).apply()
                            runTweak("Ping Stabilizer", "settings put global wifi_scan_always_enabled 0", it) { pingStabilizer = it } 
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 🕸️ HUD & VISUALS
                CyberTweakCard(title = "HUD & VISUALS", icon = "🕸️", statusHex = "0xV721") {
                    Column {
                        CyberTweakToggleRow("Real-Time HUD", "Displays FPS and System stats over game", "👓", hudOverlayEnabled, glowAlphaState) { 
                            if (!Settings.canDrawOverlays(context)) {
                                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
                                context.startActivity(intent)
                            } else {
                                hudOverlayEnabled = it
                                prefs.edit().putBoolean("key_hud_overlay", it).apply()
                                if (it) {
                                    SoundEffectsManager.playOn()
                                    context.startService(Intent(context, HudOverlayService::class.java)) 
                                } else {
                                    SoundEffectsManager.playOff()
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
                                    triggerActionFeedback(true)
                                    scope.launch {
                                        val rateVal = rate.replace("Hz", ".0")
                                        GfxBackendManager.runShell(context, "settings put system peak_refresh_rate $rateVal")
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        CyberTweakToggleRow("Mech Feedback", "Activates haptic switch sounds for tactile feel", "🔊", mechanicalFeedback, glowAlphaState) { 
                            if (!it) triggerActionFeedback(false) // Feedback before disabling
                            SoundEffectsManager.isFeedbackEnabled = it
                            mechanicalFeedback = it
                            prefs.edit().putBoolean("key_mech_feedback", it).apply()
                            if (it) triggerActionFeedback(true) // Feedback after enabling
                        }
                        CyberTweakToggleRow("HW Overlay Bypass", "Forces GPU to handle all screen layers", "🔳", hardwareOverlayBypass, glowAlphaState) { 
                            prefs.edit().putBoolean("key_hw_overlay", it).apply()
                            runTweak("Overlay Bypass", "service call SurfaceFlinger 1008 i32 1", it) { hardwareOverlayBypass = it } 
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // ⚙️ SYSTEM OPTIMIZATION
                CyberTweakCard(title = "SYSTEM OPTIMIZATION", icon = "⚙️", statusHex = "0xDB99") {
                    Column {
                        CyberTweakToggleRow("UI Touch Speed", "Minimizes UI animation delay for speed", "🖱️", uiTouchSpeed, glowAlphaState) { 
                            prefs.edit().putBoolean("key_touch_speed", it).apply()
                            runTweak("Touch Speed", "settings put global window_animation_scale 0.5", it) { uiTouchSpeed = it } 
                        }
                        CyberTweakToggleRow("High Precision Aim", "Increases touch sensitivity in-game", "🎯", precisionAim, glowAlphaState) { 
                            prefs.edit().putBoolean("key_precision_aim", it).apply()
                            runTweak("Precision Aim", "setprop debug.input.velocity_tracker_strategy lsq2", it) { precisionAim = it } 
                        }
                        CyberTweakToggleRow("Frame Latency", "Reduces frame render queue for faster response", "⌛", frameLatency, glowAlphaState) { 
                            prefs.edit().putBoolean("key_frame_latency", it).apply()
                            runTweak("Frame Latency", "setprop debug.sf.latch_unsignaled 1", it) { frameLatency = it } 
                        }
                        CyberTweakToggleRow("FIFO UI Governor", "Sets UI thread to highest real-time priority", "🧵", fifoUi, glowAlphaState) { 
                            prefs.edit().putBoolean("key_fifo_ui", it).apply()
                            runTweak("FIFO UI", "setprop sys.use_fifo_ui 1", it) { fifoUi = it } 
                        }
                        CyberTweakToggleRow("Vulkan Skia Opt", "Forces Vulkan GPU backend for 2D UI rendering", "🌋", vulkanSkia, glowAlphaState) { 
                            prefs.edit().putBoolean("key_vulkan_skia", it).apply()
                            runTweak("Vulkan Skia", "setprop debug.hwui.renderer vulkan", it) { vulkanSkia = it } 
                        }
                        CyberTweakToggleRow("Memory Heap Flush", "Aggressively flushes RAM heap to prevent stutters", "🧠", memoryHeapFlush, glowAlphaState) { 
                            prefs.edit().putBoolean("key_mem_heap", it).apply()
                            runTweak("Heap Flush", "cmd activity send-trim-memory --user current %s RUNNING_CRITICAL", it) { memoryHeapFlush = it } 
                        }
                        CyberTweakToggleRow("Disable Backpressure", "Stops system from slowing down frame delivery", "⏩", disableBackpressure, glowAlphaState) { 
                            prefs.edit().putBoolean("key_backpressure", it).apply()
                            runTweak("Backpressure", "setprop debug.sf.disable_backpressure 1", it) { disableBackpressure = it } 
                        }
                        CyberTweakToggleRow("ART Compiler Turbo", "Optimizes DEX assets for maximum execution speed", "🚀", artCompiler, glowAlphaState) { 
                            prefs.edit().putBoolean("key_art_turbo", it).apply()
                            runTweak("ART Turbo", "pm compile -m speed -f %s", it) { artCompiler = it } 
                        }
                        CyberTweakToggleRow("Thread Priority GPU", "Elevates render thread priority on hardware level", "🦾", threadPriority, glowAlphaState) { 
                            prefs.edit().putBoolean("key_thread_pri", it).apply()
                            runTweak("Thread Priority", "setprop debug.composition.type gpu", it) { threadPriority = it } 
                        }
                        CyberTweakToggleRow("Zero-State BG", "Deeply suppresses non-game background activity", "🧊", backgroundZero, glowAlphaState) { 
                            prefs.edit().putBoolean("key_bg_zero", it).apply()
                            triggerActionFeedback(it)
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
                                    triggerActionFeedback(true)
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
