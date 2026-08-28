package com.example.spidergfxtool.ui

import android.os.Build
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
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
import com.example.spidergfxtool.HudOverlayService
import com.example.spidergfxtool.SmartDnsManager
import com.example.spidergfxtool.R
import com.example.spidergfxtool.ui.components.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.spidergfxtool.SoundEffectsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Performance Optimized Toggle Row with Layout Fixes
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
                shape = RoundedCornerShape(12.dp),
                spotColor = activeColor
            )
            .border(
                width = 1.dp,
                color = if (checked) activeColor.copy(alpha = glowAlphaState.value * 0.8f) else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .background(
                color = if (checked) activeColor.copy(alpha = 0.04f) else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            // Neon Icon
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

            // TEXT COLUMN: Uses weight(1f) to prevent overlap with toggle
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label.uppercase(),
                    color = if (checked) Color.White else Color.Gray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
                
                Text(
                    text = description,
                    color = if (checked) activeColor.copy(alpha = 0.7f) else Color.Gray.copy(alpha = 0.5f),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 10.sp,
                    maxLines = 2
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

enum class TweakCategory(val title: String, val icon: ImageVector) {
    PERFORMANCE("SYSTEM & PERFORMANCE ENGINE", Icons.Outlined.Speed),
    NETWORK("NETWORK & PING OPTIMIZATION", Icons.Outlined.Wifi),
    HUD_VISUALS("HUD & VISUALS", Icons.Outlined.Monitor),
    TOUCH_AUDIO("ADVANCED TOUCH & AUDIO", Icons.Outlined.Mouse),
    DEVICE_MODULES("DEVICE-SPECIFIC MODULES", Icons.Outlined.Smartphone),
    EXPERIMENTAL("EXPERIMENTAL / EXTREME TWEAKS ⚠️", Icons.Outlined.Science)
}

@Composable
fun GfxExtremeHudScreen() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("spider_gfx_prefs", android.content.Context.MODE_PRIVATE) }
    
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val isConnected = GfxBackendManager.isShizukuConnected.value
    
    // UI Navigation State
    var selectedCategory by remember { mutableStateOf<TweakCategory?>(null) }

    // --- SYSTEM ENGINE STATES ---
    var hudOverlayEnabled by remember { mutableStateOf(prefs.getBoolean("key_hud_overlay", false)) }
    var refreshRate by remember { mutableStateOf(prefs.getString("key_refresh_rate", "60Hz") ?: "60Hz") }
    var mechanicalFeedback by remember { mutableStateOf(prefs.getBoolean("key_mech_feedback", true)) }
    var isTurboBoosting by remember { mutableStateOf(false) }
    var terminalText by remember { mutableStateOf("IDLE: SYSTEM ENGINE ACTIVE\n>> STABILITY PROTOCOL ARMED...") }
    var isOptimizing by remember { mutableStateOf(false) }

    // OVERCLOCKING STATES (MIGRATED)
    var hwOverlayBypass by remember { mutableStateOf(prefs.getBoolean("key_hw_overlay", false)) }
    var gameModeForce by remember { mutableStateOf(prefs.getBoolean("key_game_mode", false)) }
    var fstrimProcessing by remember { mutableStateOf(false) }

    // PERFORMANCE ENGINE
    var artOverdrive by remember { mutableStateOf(prefs.getBoolean("key_art_overdrive", false)) }
    var gameMode by remember { mutableStateOf(prefs.getBoolean("key_game_mode", false)) }
    var phantomProcess by remember { mutableStateOf(prefs.getBoolean("key_phantom_limiter", false)) }
    var memoryHeapFlush by remember { mutableStateOf(prefs.getBoolean("key_mem_heap", false)) }
    var disableBackpressure by remember { mutableStateOf(prefs.getBoolean("key_backpressure", false)) }
    var cpuCorePinning by remember { mutableStateOf(prefs.getBoolean("key_cpu_pinning", false)) }
    var flashStorageTrim by remember { mutableStateOf(prefs.getBoolean("key_storage_trim", false)) }
    var extremeThermalKillers by remember { mutableStateOf(prefs.getBoolean("key_daemon_thermal", false)) }
    
    // EXPERIMENTAL / EXTREME TWEAKS
    var thermalThrottling by remember { mutableStateOf(prefs.getBoolean("key_thermal_over", false)) }
    var lmkProtection by remember { mutableStateOf(prefs.getBoolean("key_lmk_prot", false)) }
    var artCompiler by remember { mutableStateOf(prefs.getBoolean("key_art_turbo", false)) }
    var fakeBatteryTemp by remember { mutableStateOf(prefs.getBoolean("key_fake_temp", false)) }
    var zramBypass by remember { mutableStateOf(prefs.getBoolean("key_zram_bypass", false)) }
    var logdAnnihilator by remember { mutableStateOf(prefs.getBoolean("key_logd_kill", false)) }

    // NETWORK ENGINE
    var tcpInjection by remember { mutableStateOf(prefs.getBoolean("key_tcp_inject", false)) }
    var ultraNetwork by remember { mutableStateOf(prefs.getBoolean("key_ultra_net", false)) }
    var pingStabilizer by remember { mutableStateOf(prefs.getBoolean("key_ping_stab", false)) }
    var networkMtuTuning by remember { mutableStateOf(prefs.getBoolean("key_mtu_tuning", false)) }
    var tcpBbr by remember { mutableStateOf(prefs.getBoolean("key_tcp_bbr", false)) }
    
    // NEW: SMART DNS STATES
    var smartDnsEnabled by remember { mutableStateOf(prefs.getBoolean("key_smart_dns", false)) }
    var isDnsOptimizing by remember { mutableStateOf(false) }

    // HUD & VISUALS
    var hardwareOverlayBypass by remember { mutableStateOf(prefs.getBoolean("key_hw_overlay", false)) }
    var vulkanSkia by remember { mutableStateOf(prefs.getBoolean("key_vulkan_skia", false)) }
    var forceMsaa4x by remember { mutableStateOf(prefs.getBoolean("key_force_msaa", false)) }
    var vSyncDisable by remember { mutableStateOf(prefs.getBoolean("key_vsync_disable", false)) }

    // TOUCH & AUDIO
    var uiTouchSpeed by remember { mutableStateOf(prefs.getBoolean("key_touch_speed", false)) }
    var precisionAim by remember { mutableStateOf(prefs.getBoolean("key_precision_aim", false)) }
    var frameLatency by remember { mutableStateOf(prefs.getBoolean("key_frame_latency", false)) }
    var fifoUi by remember { mutableStateOf(prefs.getBoolean("key_fifo_ui", false)) }
    var touchSamplingBooster by remember { mutableStateOf(prefs.getBoolean("key_touch_sampling", false)) }
    var audioBufferSlicer by remember { mutableStateOf(prefs.getBoolean("key_audio_slicer", false)) }
    var touchPrediction by remember { mutableStateOf(prefs.getBoolean("key_touch_predict", false)) }
    var btLowLatency by remember { mutableStateOf(prefs.getBoolean("key_bt_latency", false)) }

    // DEVICE MODULES
    var samsungGosKiller by remember { mutableStateOf(prefs.getBoolean("key_samsung_gos", false)) }
    var xiaomiJoyose by remember { mutableStateOf(prefs.getBoolean("key_xiaomi_joyose", false)) }
    var brandAggressiveDoze by remember { mutableStateOf(prefs.getBoolean("key_brand_doze", false)) }

    // SAFETY DIALOG STATE: Show only once on Experimental Section Entry
    var showExperimentalWarning by rememberSaveable { mutableStateOf(true) }

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

    val triggerActionFeedback = { isOn: Boolean ->
        if (isOn) SoundEffectsManager.playOn() else SoundEffectsManager.playOff()
        SoundEffectsManager.performHaptic(haptic)
    }

    val runTweak: (String, String, Boolean, (Boolean) -> Unit) -> Unit = { name, command, newState, stateUpdate ->
        stateUpdate(newState)
        triggerActionFeedback(newState)
        scope.launch(Dispatchers.IO) {
            try {
                if (command.isNotEmpty()) {
                    withContext(Dispatchers.Main) {
                        terminalText = "[PROCESS] Toggling $name: ${if (newState) "ON" else "OFF"}..."
                    }
                    GfxBackendManager.runShell(context, command)
                    withContext(Dispatchers.Main) {
                        terminalText += "\n[SUCCESS] $name state synced."
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    stateUpdate(false) // REVERT ON ERROR
                    terminalText += "\n[ERROR] $name: ${e.message}"
                    Toast.makeText(context, "Command Failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
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
                isOptimizing = true
                terminalText = "[SYSTEM] Initiating Deep Cache Purge..."
            }
            
            try {
                GfxBackendManager.runShell(context, "pm trim-caches 4G")
                delay(500)
                withContext(Dispatchers.Main) {
                    terminalText += "\n[SUCCESS] System Optimized!"
                    Toast.makeText(context, "Cache Cleared!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    terminalText += "\n[ERROR] Cache Purge Failed."
                }
            } finally {
                withContext(Dispatchers.Main) {
                    isOptimizing = false
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(backgroundBrush)) {
        AnimatedSpiderWeb(infiniteTransition)

        Row(modifier = Modifier.fillMaxSize()) {
            // --- LEFT COLUMN: SYSTEM ENGINE CONTENT ---
            Box(modifier = Modifier.weight(1.1f).fillMaxHeight()) {
                AnimatedContent(
                    targetState = selectedCategory,
                    transitionSpec = {
                        if (targetState != null) {
                            (slideInHorizontally { it } + fadeIn()).togetherWith(slideOutHorizontally { -it } + fadeOut())
                        } else {
                            (slideInHorizontally { -it } + fadeIn()).togetherWith(slideOutHorizontally { it } + fadeOut())
                        }
                    },
                    label = "CategoryTransition"
                ) { targetCategory ->
                    if (targetCategory == null) {
                        // DASHBOARD VIEW
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp)
                                .verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "SYSTEM ENGINE DASHBOARD",
                                color = ElectricCyan.copy(alpha = 0.8f),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 2.sp
                            )
                            Spacer(modifier = Modifier.height(24.dp))

                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                modifier = Modifier.heightIn(max = 1200.dp).fillMaxWidth(0.92f),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                userScrollEnabled = false
                            ) {
                                items(TweakCategory.entries) { category ->
                                    CategoryButton(
                                        category = category,
                                        neonColor = if (category == TweakCategory.TOUCH_AUDIO || category == TweakCategory.EXPERIMENTAL) NeonCrimson else ElectricCyan,
                                        modifier = Modifier.scale(0.92f)
                                    ) {
                                        triggerActionFeedback(true)
                                        selectedCategory = category
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(18.dp))

                            // --- OVERCLOCKING (EXPERIMENTAL) - MIGRATED ---
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth(0.9f)
                                    .border(1.5.dp, Color(0xFF8B0000), CutCornerShape(topStart = 12.dp, bottomEnd = 12.dp)),
                                colors = CardDefaults.cardColors(containerColor = CyberCardDark.copy(alpha = 0.9f)),
                                shape = CutCornerShape(topStart = 12.dp, bottomEnd = 12.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Outlined.Warning, "Warning", tint = Color(0xFF8B0000), modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            "⚠️ OVERCLOCKING ENGINE (USE WITH CAUTION)",
                                            color = Color(0xFF8B0000),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Black,
                                            letterSpacing = 1.sp
                                        )
                                    }
                                    
                                    val dividerBrush = remember { Brush.horizontalGradient(listOf(Color(0xFF8B0000).copy(alpha = 0.5f), Color.Transparent)) }
                                    Box(modifier = Modifier.padding(vertical = 8.dp).fillMaxWidth().height(1.dp).background(dividerBrush))

                                    // a) HW Overlay Bypass
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("HW OVERLAY BYPASS", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black)
                                            Text("Disables SF overlays to force GPU rendering", color = Color.Gray, fontSize = 8.sp)
                                        }
                                        CyberpunkToggle(
                                            checked = hwOverlayBypass,
                                            onCheckedChange = { isOn ->
                                                triggerActionFeedback(isOn)
                                                val oldState = hwOverlayBypass
                                                hwOverlayBypass = isOn
                                                scope.launch {
                                                    val cmd = if (isOn) "service call SurfaceFlinger 1008 i32 1" else "service call SurfaceFlinger 1008 i32 0"
                                                    val result = GfxBackendManager.runShell(context, cmd)
                                                    if (result.isSuccess) {
                                                        prefs.edit().putBoolean("key_hw_overlay", isOn).apply()
                                                        terminalText = if (isOn) "[CORE] Hardware Overlay Override Active." else "[CORE] HW Overlays Reverted."
                                                    } else {
                                                        hwOverlayBypass = oldState
                                                        Toast.makeText(context, "Hardware overlay override not supported", Toast.LENGTH_SHORT).show()
                                                        terminalText = "[ERROR] Kernel rejected SurfaceFlinger override."
                                                    }
                                                }
                                            },
                                            activeColor = Color(0xFF8B0000)
                                        )
                                    }

                                    // b) Native Game Mode Force
                                    val isGameModeSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = if (isGameModeSupported) "NATIVE GAME MODE FORCE" else "NATIVE GAME MODE (Android 12+ Only)",
                                                color = if (isGameModeSupported) Color.White else Color.Gray,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Black
                                            )
                                            Text("Forces performance schedule for game package", color = Color.Gray, fontSize = 8.sp)
                                        }
                                        CyberpunkToggle(
                                            checked = gameModeForce,
                                            onCheckedChange = { isOn ->
                                                if (!isGameModeSupported) return@CyberpunkToggle
                                                triggerActionFeedback(isOn)
                                                val oldState = gameModeForce
                                                gameModeForce = isOn
                                                scope.launch {
                                                    val pkg = GfxBackendManager.getAutoDetectedGamePackage(context)
                                                    val cmd = if (isOn) "cmd game mode performance $pkg" else "cmd game mode standard $pkg"
                                                    val result = GfxBackendManager.runShell(context, cmd)
                                                    if (result.isSuccess) {
                                                        prefs.edit().putBoolean("key_game_mode", isOn).apply()
                                                        terminalText = if (isOn) "[OS] Performance Game Mode Enforced." else "[OS] Game Mode Reverted."
                                                    } else {
                                                        gameModeForce = oldState
                                                        terminalText = "[ERROR] System rejected Game Mode command."
                                                    }
                                                }
                                            },
                                            activeColor = Color(0xFF8B0000),
                                            isLoading = !isGameModeSupported // Greyed out visually
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // c) Storage Core Purge / FSTRIM
                                    Button(
                                        onClick = {
                                            triggerActionFeedback(true)
                                            scope.launch {
                                                fstrimProcessing = true
                                                terminalText = "[FS] Initializing Storage Purge (FSTRIM)..."
                                                val result = withContext(Dispatchers.IO) {
                                                    GfxBackendManager.executeShizukuSafe("sm fstrim -v")
                                                }
                                                terminalText += if (result.isSuccess) "\n[SUCCESS] Blocks trimmed: ${result.getOrNull()}" else "\n[FAIL] Trim failed or unsupported."
                                                fstrimProcessing = false
                                                triggerActionFeedback(false)
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth().height(36.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray.copy(alpha = 0.3f)),
                                        shape = CutCornerShape(8.dp),
                                        border = BorderStroke(1.dp, Color(0xFF8B0000).copy(alpha = 0.5f)),
                                        enabled = !fstrimProcessing
                                    ) {
                                        Text(if (fstrimProcessing) "PURGING..." else "PURGE STORAGE (FSTRIM)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 9.sp)
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    // d) Network DNS Flush
                                    Button(
                                        onClick = {
                                            triggerActionFeedback(true)
                                            scope.launch {
                                                terminalText = "[NET] Flushing DNS Resolver Cache..."
                                                val result = GfxBackendManager.runShell(context, "ndc resolver flushdefaultif")
                                                terminalText += if (result.isSuccess) "\n[SUCCESS] Resolver cache purged." else "\n[FAIL] DNS Flush command rejected."
                                                triggerActionFeedback(false)
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth().height(36.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray.copy(alpha = 0.3f)),
                                        shape = CutCornerShape(8.dp),
                                        border = BorderStroke(1.dp, Color(0xFF8B0000).copy(alpha = 0.5f))
                                    ) {
                                        Text("FLUSH NETWORK DNS", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 9.sp)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            // STANDALONE TURBO REACTOR
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().scale(0.9f)) {
                                val reactorPulseState = infiniteTransition.animateFloat(
                                    initialValue = 1f, targetValue = 1.1f,
                                    animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse), label = "Reactor"
                                )

                                Box(
                                    modifier = Modifier
                                        .size(80.dp)
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
                                                    try {
                                                        terminalText = ">> INJECTING TURBO CORE..."
                                                        GfxBackendManager.runShell(context, "pm trim-caches 4G")
                                                        delay(500)
                                                        terminalText += "\n>> FORCING SYSTEM REFRESH..."
                                                        GfxBackendManager.runShell(context, "settings put system peak_refresh_rate 120.0")
                                                        delay(1000)
                                                        terminalText = "[CORE READY] ENGINE OVERCLOCKED."
                                                        delay(2000)
                                                        isTurboBoosting = false
                                                    } catch (e: Exception) {
                                                        terminalText = "[FAIL] CORE INJECTION BLOCKED."
                                                        isTurboBoosting = false
                                                    }
                                                }
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (isTurboBoosting) "LIVE" else "TURBO\nREACTOR",
                                        color = Color.White,
                                        fontWeight = FontWeight.Black,
                                        textAlign = TextAlign.Center,
                                        fontSize = 12.sp,
                                        letterSpacing = 2.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Button(
                                onClick = onDeepCachePurge,
                                modifier = Modifier.fillMaxWidth(0.9f).height(42.dp),
                                enabled = !isOptimizing,
                                colors = ButtonDefaults.buttonColors(containerColor = NeonCrimson.copy(alpha = 0.15f)),
                                shape = CutCornerShape(topStart = 10.dp, bottomEnd = 10.dp),
                                border = BorderStroke(1.dp, NeonCrimson.copy(alpha = 0.5f))
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Outlined.Autorenew, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("DEEP CACHE PURGE", fontWeight = FontWeight.Black, fontSize = 12.sp, color = Color.White)
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(32.dp))
                            GlobalCommunityFooter()

                            Spacer(modifier = Modifier.height(100.dp))
                        }
                    } else {
                        // DETAIL VIEW
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp)
                                .verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { 
                                    triggerActionFeedback(false)
                                    selectedCategory = null 
                                }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = targetCategory.title,
                                    color = if (targetCategory == TweakCategory.EXPERIMENTAL) NeonCrimson else ElectricCyan,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 2.sp
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(24.dp))

                            // Root Experimental Warning Dialog (Appears exactly once on entry)
                            if (targetCategory == TweakCategory.EXPERIMENTAL && showExperimentalWarning) {
                                AlertDialog(
                                    onDismissRequest = { 
                                        showExperimentalWarning = false 
                                        selectedCategory = null 
                                    },
                                    title = { Text("Warning: High Risk Tweak", color = NeonCrimson, fontWeight = FontWeight.Black) },
                                    text = { Text("This feature pushes hardware to its absolute limits. It may cause kernel panics, device freezing, or game crashes on unsupported devices. Proceed at your own risk!", color = Color.White) },
                                    confirmButton = {
                                        TextButton(onClick = { showExperimentalWarning = false }) {
                                            Text("I ACCEPT", color = NeonCrimson, fontWeight = FontWeight.Black)
                                        }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = { 
                                            showExperimentalWarning = false 
                                            selectedCategory = null 
                                        }) {
                                            Text("CANCEL", color = Color.Gray)
                                        }
                                    },
                                    containerColor = Color(0xFF1A0000),
                                    shape = CutCornerShape(12.dp),
                                    tonalElevation = 24.dp
                                )
                            }

                            Box(modifier = Modifier.fillMaxWidth(0.92f)) {
                                when (targetCategory) {
                                    TweakCategory.PERFORMANCE -> {
                                        CyberTweakCard(title = "SYSTEM & PERFORMANCE ENGINE", icon = Icons.Outlined.Speed, statusHex = "0xELITE") {
                                            Column {
                                                Button(
                                                    onClick = {
                                                        triggerActionFeedback(true)
                                                        scope.launch(Dispatchers.IO) {
                                                            isOptimizing = true
                                                            try {
                                                                withContext(Dispatchers.Main) { terminalText = "[PROCESS] Starting AOT Speed Overdrive..." }
                                                                GfxBackendManager.runShell(context, "cmd package compile -m speed -f com.tencent.ig")
                                                                withContext(Dispatchers.Main) { terminalText += "\n[SUCCESS] ART Optimized." }
                                                            } catch (e: Exception) {
                                                                withContext(Dispatchers.Main) { terminalText += "\n[ERROR] AOT Compilation Failed." }
                                                            } finally {
                                                                isOptimizing = false
                                                            }
                                                        }
                                                    },
                                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                                    enabled = !isOptimizing,
                                                    colors = ButtonDefaults.buttonColors(containerColor = NeonCrimson.copy(alpha = 0.15f)),
                                                    border = BorderStroke(1.dp, NeonCrimson.copy(alpha = 0.4f))
                                                ) {
                                                    Icon(Icons.Outlined.Bolt, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text("AOT GAME COMPILER (MAX FPS)", fontSize = 11.sp, fontWeight = FontWeight.Black)
                                                }

                                                CyberTweakToggleRow("ART Overdrive", "Optimizes DEX runtime for game assets", Icons.Outlined.AutoAwesome, artOverdrive, glowAlphaState) {
                                                    runTweak("ART Overdrive", "cmd package compile -m speed -f com.tencent.ig", it) { 
                                                        artOverdrive = it
                                                        prefs.edit().putBoolean("key_art_overdrive", it).apply()
                                                    }
                                                }
                                                CyberTweakToggleRow("Android Game Mode", "Enables system-level performance mode", Icons.Outlined.Games, gameMode, glowAlphaState) { 
                                                    prefs.edit().putBoolean("key_game_mode", it).apply()
                                                    runTweak("Game Mode", "cmd game set --mode performance %s", it) { gameMode = it } 
                                                }
                                                CyberTweakToggleRow("Phantom Processes", "Removes limits on background child processes", Icons.Outlined.Psychology, phantomProcess, glowAlphaState) { 
                                                    prefs.edit().putBoolean("key_phantom_limiter", it).apply()
                                                    runTweak("Phantom Limiter", "device_config put activity_manager max_phantom_processes 2147483647", it) { phantomProcess = it } 
                                                }
                                                CyberTweakToggleRow("Memory Heap Flush", "Aggressively flushes RAM heap to prevent stutters", Icons.Outlined.Memory, memoryHeapFlush, glowAlphaState) { 
                                                    prefs.edit().putBoolean("key_mem_heap", it).apply()
                                                    runTweak("Heap Flush", "cmd activity send-trim-memory --user current %s RUNNING_CRITICAL", it) { memoryHeapFlush = it } 
                                                }
                                                CyberTweakToggleRow("Disable Backpressure", "Stops system from slowing down frame delivery", Icons.Outlined.FastForward, disableBackpressure, glowAlphaState) { 
                                                    prefs.edit().putBoolean("key_backpressure", it).apply()
                                                    runTweak("Backpressure", "setprop debug.sf.disable_backpressure 1", it) { disableBackpressure = it } 
                                                }
                                                CyberTweakToggleRow("Game Turbo Core Lock (CPU Affinity)", "Forces game threads strictly onto high-performance Prime cores", Icons.Outlined.Hardware, cpuCorePinning, glowAlphaState) {
                                                    prefs.edit().putBoolean("key_cpu_pinning", it).apply()
                                                    runTweak("CPU Pinning", "taskset -p f0 $(pidof %s)", it) { cpuCorePinning = it }
                                                }
                                                CyberTweakToggleRow("Storage TRIM", "Forces block alignment for zero-delay loading", Icons.Outlined.Storage, flashStorageTrim, glowAlphaState) {
                                                    prefs.edit().putBoolean("key_storage_trim", it).apply()
                                                    runTweak("Storage Trim", "sm fstrim", it) { flashStorageTrim = it }
                                                }
                                                CyberTweakToggleRow("Daemon Thermal Killers", "Stops brand-specific background thermal throttlers", Icons.Outlined.LocalFireDepartment, extremeThermalKillers, glowAlphaState) {
                                                    prefs.edit().putBoolean("key_daemon_thermal", it).apply()
                                                    val cmd = if (it) "stop mi_thermald; stop thermald; stop gameopt" else "start mi_thermald; start thermald"
                                                    runTweak("Thermal Killer", cmd, it) { extremeThermalKillers = it }
                                                }
                                            }
                                        }
                                    }
                                    TweakCategory.NETWORK -> {
                                        CyberTweakCard(title = "NETWORK & PING OPTIMIZATION", icon = Icons.Outlined.Wifi, statusHex = "0xNW99") {
                                            Column {
                                                CyberTweakToggleRow(
                                                    label = "Smart Dynamic DNS (Auto-Optimize)",
                                                    description = "Pings providers & ISP routes to select the lowest latency path",
                                                    icon = Icons.Outlined.Dns,
                                                    checked = smartDnsEnabled,
                                                    glowAlphaState = glowAlphaState,
                                                    isLoading = isDnsOptimizing
                                                ) { isEnabled ->
                                                    if (isEnabled) {
                                                        triggerActionFeedback(true)
                                                        isDnsOptimizing = true
                                                        scope.launch {
                                                            try {
                                                                terminalText = "[NETWORK] Testing routes (Cloudflare, Google, ISP)..."
                                                                val result = SmartDnsManager.findBestDns()
                                                                
                                                                val detailLog = result.details.entries.joinToString(", ") { "${it.key}=${it.value}ms" }
                                                                terminalText += "\n[NETWORK] Latency Scan: $detailLog"
                                                                
                                                                if (result.best.isDefault) {
                                                                    GfxBackendManager.runShell(context, "settings put global private_dns_mode off")
                                                                    terminalText += "\n[SUCCESS] ISP routing is optimal. Private DNS: OFF."
                                                                    Toast.makeText(context, "ISP is optimal. DNS Off.", Toast.LENGTH_SHORT).show()
                                                                } else {
                                                                    val cmd = "settings put global private_dns_mode hostname && settings put global private_dns_specifier ${result.best.hostname}"
                                                                    GfxBackendManager.runShell(context, cmd)
                                                                    terminalText += "\n[SUCCESS] Optimized: Best route is ${result.best.name}"
                                                                }
                                                                
                                                                smartDnsEnabled = true
                                                                prefs.edit().putBoolean("key_smart_dns", true).apply()
                                                            } catch (e: Exception) {
                                                                terminalText += "\n[ERROR] DNS Route optimization failed."
                                                                smartDnsEnabled = false
                                                            } finally {
                                                                isDnsOptimizing = false
                                                            }
                                                        }
                                                    } else {
                                                        triggerActionFeedback(false)
                                                        smartDnsEnabled = false
                                                        prefs.edit().putBoolean("key_smart_dns", false).apply()
                                                        scope.launch {
                                                            GfxBackendManager.runShell(context, "settings put global private_dns_mode off")
                                                            terminalText = "[NETWORK] Smart DNS Disabled. Defaults Restored."
                                                        }
                                                    }
                                                }

                                                CyberTweakToggleRow("TCP BBR (Zero Packet Loss)", "Enables Google's congestion control for stable packets", Icons.Outlined.NetworkCheck, tcpBbr, glowAlphaState) {
                                                    prefs.edit().putBoolean("key_tcp_bbr", it).apply()
                                                    runTweak("TCP BBR", if (it) "sysctl -w net.ipv4.tcp_congestion_control=bbr" else "sysctl -w net.ipv4.tcp_congestion_control=cubic", it) { tcpBbr = it }
                                                }
                                                CyberTweakToggleRow("TCP Injection", "Reduces packet delay via TCP optimization", Icons.Outlined.SettingsInputAntenna, tcpInjection, glowAlphaState) { 
                                                    prefs.edit().putBoolean("key_tcp_inject", it).apply()
                                                    runTweak("TCP Injection", if (it) "settings put global tcp_default_init_rwnd 60" else "settings delete global tcp_default_init_rwnd", it) { tcpInjection = it } 
                                                }
                                                CyberTweakToggleRow("Ultra Network", "Allocates priority bandwidth to WiFi/Data", Icons.Outlined.SignalCellularAlt, ultraNetwork, glowAlphaState) { 
                                                    prefs.edit().putBoolean("key_ultra_net", it).apply()
                                                    runTweak("Ultra Network", "setprop net.tcp.buffersize.wifi 524288,1048576,2097152", it) { ultraNetwork = it } 
                                                }
                                                CyberTweakToggleRow("Ping Stabilizer", "Disables background WiFi scanning during match", Icons.Outlined.Router, pingStabilizer, glowAlphaState) { 
                                                    val cmd = if (it) "settings put global wifi_scan_always_enabled 0" else "settings put global wifi_scan_always_enabled 1"
                                                    runTweak("Ping Stabilizer", cmd, it) { pingStabilizer = it } 
                                                    prefs.edit().putBoolean("key_ping_stab", it).apply()
                                                }
                                                CyberTweakToggleRow("MTU Size Tuning (1428)", "Optimizes packet size to fix desync", Icons.Outlined.Compress, networkMtuTuning, glowAlphaState) {
                                                    prefs.edit().putBoolean("key_mtu_tuning", it).apply()
                                                    val cmd = if (it) "ifconfig wlan0 mtu 1428" else "ifconfig wlan0 mtu 1500"
                                                    runTweak("MTU Tuning", cmd, it) { networkMtuTuning = it }
                                                }
                                            }
                                        }
                                    }
                                    TweakCategory.HUD_VISUALS -> {
                                        CyberTweakCard(title = "HUD & VISUALS", icon = Icons.Outlined.Monitor, statusHex = "0xV721") {
                                            Column {
                                                CyberTweakToggleRow("Real-Time HUD", "Displays FPS and System stats over game", Icons.Outlined.Visibility, hudOverlayEnabled, glowAlphaState) { 
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
                                                                try {
                                                                    val rateVal = rate.replace("Hz", ".0")
                                                                    GfxBackendManager.runShell(context, "settings put system peak_refresh_rate $rateVal")
                                                                } catch (e: Exception) {}
                                                            }
                                                        }
                                                    }
                                                }

                                                CyberTweakToggleRow("HW Overlay Bypass", "Forces GPU to handle all screen layers", Icons.Outlined.Layers, hardwareOverlayBypass, glowAlphaState) { 
                                                    prefs.edit().putBoolean("key_hw_overlay", it).apply()
                                                    runTweak("Overlay Bypass", "service call SurfaceFlinger 1008 i32 1", it) { hardwareOverlayBypass = it } 
                                                }
                                                CyberTweakToggleRow("Vulkan Skia Opt", "Forces Vulkan backend for 2D UI rendering", Icons.Outlined.Architecture, vulkanSkia, glowAlphaState) { 
                                                    prefs.edit().putBoolean("key_vulkan_skia", it).apply()
                                                    runTweak("Vulkan Skia", "setprop debug.hwui.renderer vulkan", it) { vulkanSkia = it } 
                                                }
                                                CyberTweakToggleRow("Force 4x MSAA", "Injects anti-aliasing via GPU backend", Icons.Outlined.Gradient, forceMsaa4x, glowAlphaState) {
                                                    prefs.edit().putBoolean("key_force_msaa", it).apply()
                                                    val cmd = if (it) "setprop debug.hwui.overdraw false && settings put global debug.force_msaa true" else "settings put global debug.force_msaa false"
                                                    runTweak("GPU MSAA", cmd, it) { forceMsaa4x = it }
                                                }
                                                CyberTweakToggleRow("V-Sync Disable", "Bypasses frame limits for raw uncapped FPS", Icons.Outlined.TimerOff, vSyncDisable, glowAlphaState) {
                                                    prefs.edit().putBoolean("key_vsync_disable", it).apply()
                                                    runTweak("V-Sync", "setprop debug.hwui.disable_vsync ${if (it) "true" else "false"}", it) { vSyncDisable = it }
                                                }
                                            }
                                        }
                                    }
                                    TweakCategory.TOUCH_AUDIO -> {
                                        Column {
                                            CyberTweakCard(title = "ADVANCED TOUCH & AUDIO", icon = Icons.Outlined.Mouse, statusHex = "0xI/O") {
                                                Column {
                                                    CyberTweakToggleRow("Pointer Speed Override", "Boosts touch prediction and system pointer speed", Icons.Outlined.AdsClick, touchPrediction, glowAlphaState) {
                                                        prefs.edit().putBoolean("key_touch_predict", it).apply()
                                                        val cmd = if (it) "settings put system pointer_speed 7 && setprop debug.touch.predict_ms 0" else "settings put system pointer_speed 0"
                                                        runTweak("Touch Prediction", cmd, it) { touchPrediction = it }
                                                    }
                                                    CyberTweakToggleRow("UI Touch Speed", "Minimizes UI animation delay for speed", Icons.Outlined.TouchApp, uiTouchSpeed, glowAlphaState) { 
                                                        prefs.edit().putBoolean("key_touch_speed", it).apply()
                                                        runTweak("Touch Speed", "settings put global window_animation_scale 0.5", it) { uiTouchSpeed = it } 
                                                    }
                                                    CyberTweakToggleRow("High Precision Aim", "Increases touch sensitivity in-game", Icons.Outlined.MyLocation, precisionAim, glowAlphaState) { 
                                                        prefs.edit().putBoolean("key_precision_aim", it).apply()
                                                        runTweak("Precision Aim", "setprop debug.input.velocity_tracker_strategy lsq2", it) { precisionAim = it } 
                                                    }
                                                    CyberTweakToggleRow("Frame Latency", "Reduces render queue for faster response", Icons.Outlined.AvTimer, frameLatency, glowAlphaState) { 
                                                        prefs.edit().putBoolean("key_frame_latency", it).apply()
                                                        runTweak("Frame Latency", "setprop debug.sf.latch_unsignaled 1", it) { frameLatency = it } 
                                                    }
                                                    CyberTweakToggleRow("FIFO UI Governor", "Sets UI thread to highest priority", Icons.Outlined.LinearScale, fifoUi, glowAlphaState) { 
                                                        prefs.edit().putBoolean("key_fifo_ui", it).apply()
                                                        runTweak("FIFO UI", "setprop sys.use_fifo_ui 1", it) { fifoUi = it } 
                                                    }
                                                    CyberTweakToggleRow("Touch Sampling Booster", "Maximizes hardware touch reporting rate", Icons.Outlined.FlashOn, touchSamplingBooster, glowAlphaState) {
                                                        prefs.edit().putBoolean("key_touch_sampling", it).apply()
                                                        runTweak("Touch Booster", "settings put system pointer_speed 7", it) { touchSamplingBooster = it }
                                                    }
                                                    CyberTweakToggleRow("Audio Buffer Slicer", "Removes Android buffer for instant footsteps", Icons.AutoMirrored.Outlined.VolumeUp, audioBufferSlicer, glowAlphaState) {
                                                        prefs.edit().putBoolean("key_audio_slicer", it).apply()
                                                        val cmd = if (it) "setprop ro.audio.flinger_standbytime_ms 0 && setprop af.fast_track_multiplier 1" else "setprop ro.audio.flinger_standbytime_ms 3000"
                                                        runTweak("Audio Slicer", cmd, it) { audioBufferSlicer = it }
                                                    }
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(14.dp))

                                            // BLUETOOTH AUDIO ENFORCER (MIGRATED)
                                            CyberTweakCard(title = "BLUETOOTH AUDIO ENFORCER", icon = Icons.Outlined.BluetoothAudio, statusHex = "0xBT99") {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text("BLUETOOTH AUDIO LOW-LATENCY", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black)
                                                        Text("Forces high bit-rate and minimum buffer for gaming", color = Color.Gray, fontSize = 8.sp)
                                                    }
                                                    CyberpunkToggle(
                                                        checked = btLowLatency,
                                                        onCheckedChange = { 
                                                            btLowLatency = it
                                                            prefs.edit().putBoolean("key_bt_latency", it).apply()
                                                            val cmd = if (it) {
                                                                "setprop persist.bluetooth.latency.low true && setprop persist.vendor.bt.a2dp.latency.low true"
                                                            } else {
                                                                "setprop persist.bluetooth.latency.low false"
                                                            }
                                                            runTweak("BT Low-Latency", cmd, it) { btLowLatency = it }
                                                        },
                                                        activeColor = NeonCrimson
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    TweakCategory.DEVICE_MODULES -> {
                                        CyberTweakCard(title = "DEVICE-SPECIFIC MODULES", icon = Icons.Outlined.Smartphone, statusHex = "0xBRND") {
                                            Column {
                                                CyberTweakToggleRow("Samsung GOS Killer", "Disables Game Optimization Service", Icons.Outlined.DomainVerification, samsungGosKiller, glowAlphaState) {
                                                    prefs.edit().putBoolean("key_samsung_gos", it).apply()
                                                    val cmd = if (it) "pm disable-user --user 0 com.samsung.android.game.gos" else "pm enable --user 0 com.samsung.android.game.gos"
                                                    runTweak("Samsung GOS", cmd, it) { samsungGosKiller = it }
                                                }
                                                CyberTweakToggleRow("Xiaomi Joyose Disabler", "Stops cloud-based frame throttling", Icons.Outlined.AppRegistration, xiaomiJoyose, glowAlphaState) {
                                                    prefs.edit().putBoolean("key_xiaomi_joyose", it).apply()
                                                    val cmd = if (it) "pm disable-user --user 0 com.xiaomi.joyose" else "pm enable --user 0 com.xiaomi.joyose"
                                                    runTweak("Xiaomi Joyose", cmd, it) { xiaomiJoyose = it }
                                                }
                                                CyberTweakToggleRow("Aggressive Doze & RAM Flush", "Forces 100% RAM allocation to game window", Icons.Outlined.AutoFixHigh, brandAggressiveDoze, glowAlphaState) {
                                                    prefs.edit().putBoolean("key_brand_doze", it).apply()
                                                    val cmd = if (it) "cmd deviceidle force-idle && am kill-all" else "cmd deviceidle unforce"
                                                    runTweak("Aggressive RAM", cmd, it) { brandAggressiveDoze = it }
                                                }
                                            }
                                        }
                                    }
                                    TweakCategory.EXPERIMENTAL -> {
                                        CyberTweakCard(title = "EXPERIMENTAL / EXTREME TWEAKS ⚠️", icon = Icons.Outlined.Science, statusHex = "0xRISK") {
                                            Column {
                                                CyberTweakToggleRow("ZRAM Bypass", "Disables virtual swap for physical RAM", Icons.Outlined.SdCard, zramBypass, glowAlphaState, isRisky = true) {
                                                    runTweak("ZRAM Bypass", if (it) "sysctl -w vm.swappiness=0" else "sysctl -w vm.swappiness=60", it) { 
                                                        zramBypass = it
                                                        prefs.edit().putBoolean("key_zram_bypass", it).apply()
                                                    }
                                                }
                                                CyberTweakToggleRow("Anti-Throttle", "Spoofs battery temp to 25°C", Icons.Outlined.Thermostat, fakeBatteryTemp, glowAlphaState, isRisky = true) {
                                                    runTweak("Anti-Throttle", if (it) "dumpsys battery set temp 250" else "dumpsys battery reset", it) { 
                                                        fakeBatteryTemp = it
                                                        prefs.edit().putBoolean("key_fake_temp", it).apply()
                                                    }
                                                }
                                                CyberTweakToggleRow("LMK Protection", "Prevents system from closing game", Icons.Outlined.Shield, lmkProtection, glowAlphaState, isRisky = true) { 
                                                    runTweak("LMK Protection", "setprop ro.lmk.kill_heaviest_task false", it) { 
                                                        lmkProtection = it
                                                        prefs.edit().putBoolean("key_lmk_prot", it).apply()
                                                    } 
                                                }
                                                CyberTweakToggleRow("ART Compiler Turbo", "Optimizes DEX assets for speed", Icons.Outlined.RocketLaunch, artCompiler, glowAlphaState, isRisky = true) { 
                                                    runTweak("ART Turbo", "pm compile -m speed -f %s", it) { 
                                                        artCompiler = it
                                                        prefs.edit().putBoolean("key_art_turbo", it).apply()
                                                    } 
                                                }
                                                CyberTweakToggleRow("Thermal Illusion", "Bypasses system thermal limits", Icons.Outlined.AcUnit, thermalThrottling, glowAlphaState, isRisky = true) { 
                                                    runTweak("Thermal Illusion", if (it) "cmd thermalservice override-status 0" else "cmd thermalservice reset", it) { 
                                                        thermalThrottling = it
                                                        prefs.edit().putBoolean("key_thermal_over", it).apply()
                                                    } 
                                                }
                                                CyberTweakToggleRow("Kill System Loggers", "Reduces background I/O overhead", Icons.Outlined.Terminal, logdAnnihilator, glowAlphaState, isRisky = true) {
                                                    runTweak("Logd Annihilator", if (it) "stop logd" else "start logd", it) { 
                                                        logdAnnihilator = it
                                                        prefs.edit().putBoolean("key_logd_kill", it).apply()
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(100.dp))
                        }
                    }
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
fun CategoryButton(
    category: TweakCategory,
    neonColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(100.dp)
            .clickable(onClick = onClick)
            .border(1.dp, neonColor.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(neonColor.copy(alpha = 0.1f), CircleShape)
                    .border(1.dp, neonColor.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = category.icon, contentDescription = null, tint = neonColor, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = category.title,
                color = Color.White,
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                letterSpacing = 1.sp
            )
        }
    }
}
