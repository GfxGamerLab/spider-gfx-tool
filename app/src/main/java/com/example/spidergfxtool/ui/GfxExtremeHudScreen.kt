package com.example.spidergfxtool.ui

import android.os.Build
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
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
import com.example.spidergfxtool.SmartEngine
import com.example.spidergfxtool.SmartResult
import com.example.spidergfxtool.TweakStatus
import com.example.spidergfxtool.RealTimeHudService
import com.example.spidergfxtool.CrosshairService
import com.example.spidergfxtool.OverlayStateManager
import com.example.spidergfxtool.SmartDnsManager
import com.example.spidergfxtool.R
import com.example.spidergfxtool.ui.theme.TerminalTextStyle
import com.example.spidergfxtool.ui.components.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.spidergfxtool.SoundEffectsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
    val isConnected = SmartEngine.isShizukuReady()
    
    // UI Navigation State
    var selectedCategory by remember { mutableStateOf<TweakCategory?>(null) }

    // --- SYSTEM ENGINE STATES ---
    val hudActive by OverlayStateManager.isHudActive.collectAsState()
    val crosshairActive by OverlayStateManager.isCrosshairActive.collectAsState()

    var hudOverlayEnabled by remember(hudActive) { mutableStateOf(hudActive) }
    var floatingOverlayEnabled by remember(crosshairActive) { mutableStateOf(crosshairActive) }
    var refreshRate by remember { mutableStateOf(prefs.getString("key_refresh_rate", "60Hz") ?: "60Hz") }
    var isTurboBoosting by remember { mutableStateOf(false) }
    
    // SHORTENED PROFESSIONAL IDLE TEXT
    var terminalText by remember { mutableStateOf("[SYSTEM] Ready. Awaiting command...") }
    var isOptimizing by remember { mutableStateOf(false) }

    // OVERCLOCKING STATES
    var hwOverlayBypass by remember { mutableStateOf(prefs.getBoolean("key_hw_overlay", false)) }
    var gameModeForce by remember { mutableStateOf(prefs.getBoolean("key_game_mode", false)) }
    var fstrimProcessing by remember { mutableStateOf(false) }

    // PERFORMANCE ENGINE
    var artOverdrive by remember { mutableStateOf(prefs.getBoolean("key_art_overdrive", false)) }
    var gameMode by remember { mutableStateOf(prefs.getBoolean("key_game_mode", false)) }
    var phantomProcess by remember { mutableStateOf(prefs.getBoolean("key_phantom_limiter", false)) }
    var memoryHeapFlush by remember { mutableStateOf(prefs.getBoolean("key_mem_heap", false)) }
    var cpuCorePinning by remember { mutableStateOf(prefs.getBoolean("key_cpu_pinning", false)) }
    var flashStorageTrim by remember { mutableStateOf(prefs.getBoolean("key_storage_trim", false)) }
    var extremeThermalKillers by remember { mutableStateOf(prefs.getBoolean("key_daemon_thermal", false)) }
    var smartZram by remember { mutableStateOf(prefs.getBoolean("key_smart_zram", false)) }
    var readAheadOpt by remember { mutableStateOf(prefs.getBoolean("key_read_ahead_opt", false)) }

    // EXPERIMENTAL / EXTREME TWEAKS
    var thermalThrottling by remember { mutableStateOf(prefs.getBoolean("key_thermal_over", false)) }
    var lmkProtection by remember { mutableStateOf(prefs.getBoolean("key_lmk_prot", false)) }
    var artCompiler by remember { mutableStateOf(prefs.getBoolean("key_art_turbo", false)) }
    var fakeBatteryTemp by remember { mutableStateOf(prefs.getBoolean("key_fake_temp", false)) }
    var zramBypass by remember { mutableStateOf(prefs.getBoolean("key_zram_bypass", false)) }
    var logdAnnihilator by remember { mutableStateOf(prefs.getBoolean("key_logd_kill", false)) }

    // NETWORK ENGINE
    var smartNetStabilizer by remember { mutableStateOf(prefs.getBoolean("key_smart_net_stab", false)) }
    var tcpBbr by remember { mutableStateOf(prefs.getBoolean("key_tcp_bbr", false)) }
    var dynamicMtu by remember { mutableStateOf(prefs.getBoolean("key_dynamic_mtu", false)) }
    var fqCodel by remember { mutableStateOf(prefs.getBoolean("key_fq_codel", false)) }

    // SMART DNS STATES
    var smartDnsEnabled by remember { mutableStateOf(prefs.getBoolean("key_smart_dns", false)) }
    var isDnsOptimizing by remember { mutableStateOf(false) }

    // HUD & VISUALS
    var hardwareOverlayBypass by remember { mutableStateOf(prefs.getBoolean("key_hw_overlay", false)) }
    var vulkanSkia by remember { mutableStateOf(prefs.getBoolean("key_vulkan_skia", false)) }
    var forceMsaa4x by remember { mutableStateOf(prefs.getBoolean("key_force_msaa", false)) }
    var vSyncDisable by remember { mutableStateOf(prefs.getBoolean("key_vsync_disable", false)) }

    // TOUCH & INPUT OPTIMIZATION
    var smartTouchOptimizer by remember { mutableStateOf(prefs.getBoolean("key_smart_touch_opt", false)) }
    var cpuInputBoost by remember { mutableStateOf(prefs.getBoolean("key_cpu_input_boost", false)) }
    var uiTouchSpeed by remember { mutableStateOf(prefs.getBoolean("key_touch_speed", false)) }
    var precisionAim by remember { mutableStateOf(prefs.getBoolean("key_precision_aim", false)) }
    var frameLatency by remember { mutableStateOf(prefs.getBoolean("key_frame_latency", false)) }
    var fifoUi by remember { mutableStateOf(prefs.getBoolean("key_fifo_ui", false)) }
    var audioBufferSlicer by remember { mutableStateOf(prefs.getBoolean("key_audio_slicer", false)) }
    var btLowLatency by remember { mutableStateOf(prefs.getBoolean("key_bt_latency", false)) }

    // DEVICE MODULES
    var samsungGosKiller by remember { mutableStateOf(prefs.getBoolean("key_samsung_gos", false)) }
    var xiaomiJoyose by remember { mutableStateOf(prefs.getBoolean("key_xiaomi_joyose", false)) }
    var brandAggressiveDoze by remember { mutableStateOf(prefs.getBoolean("key_brand_doze", false)) }

    // SAFETY DIALOG STATE
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

    // AI AGENT RESULT PROCESSOR WITH EXACT SOUND MAPPING & UI AUTO-REVERT
    val processSmartResult = { res: SmartResult, enableRequested: Boolean, stateUpdate: (Boolean) -> Unit, prefKey: String ->
        if (res.status == TweakStatus.SUCCESS) {
            if (enableRequested) {
                SoundEffectsManager.playOn()
            } else {
                SoundEffectsManager.playOff()
            }
            stateUpdate(enableRequested)
            prefs.edit { putBoolean(prefKey, enableRequested) }
        } else {
            // UNSUPPORTED/FAILED: Play R.raw.wrong_key ONLY (do not play switch_off)
            SoundEffectsManager.playError()
            // Visually animate back to OFF state on UI thread
            stateUpdate(false)
            prefs.edit { putBoolean(prefKey, false) }
        }
        terminalText = "${res.logMessage}\n$terminalText"
    }

    // AI AGENT GENERIC TWEAK RUNNER
    val runAiAgentTweak = { featureName: String, purpose: String, commandOn: String, commandOff: String, enable: Boolean, preCheck: suspend () -> Boolean, stateUpdate: (Boolean) -> Unit, prefKey: String ->
        SoundEffectsManager.performHaptic(haptic)
        scope.launch(Dispatchers.IO) {
            val res = SmartEngine.runAiAgentTweak(
                featureName = featureName,
                purpose = purpose,
                preCheckCondition = preCheck,
                commandOn = commandOn,
                commandOff = commandOff,
                enable = enable
            )
            withContext(Dispatchers.Main) {
                processSmartResult(res, enable, stateUpdate, prefKey)
            }
        }
    }

    // ONE-TIME ACTION: STORAGE PURGE (FSTRIM)
    val onPurgeStorageFstrim: () -> Unit = {
        SoundEffectsManager.playClick()
        SoundEffectsManager.performHaptic(haptic)
        scope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                fstrimProcessing = true
            }
            val res = SmartEngine.executePurgeStorageFstrim()
            withContext(Dispatchers.Main) {
                fstrimProcessing = false
                if (res.status == TweakStatus.SUCCESS) {
                    SoundEffectsManager.playOn()
                } else {
                    SoundEffectsManager.playError()
                }
                terminalText = "${res.logMessage}\n$terminalText"
            }
        }
    }

    // ONE-TIME ACTION: SOCKET & DNS CACHE FLUSH
    val onSocketDnsCacheFlush: () -> Unit = {
        SoundEffectsManager.playClick()
        SoundEffectsManager.performHaptic(haptic)
        scope.launch(Dispatchers.IO) {
            val res = SmartEngine.executeSocketDnsCacheFlush()
            withContext(Dispatchers.Main) {
                if (res.status == TweakStatus.SUCCESS) {
                    SoundEffectsManager.playOn()
                } else {
                    SoundEffectsManager.playError()
                }
                terminalText = "${res.logMessage}\n$terminalText"
            }
        }
    }

    // ONE-TIME ACTION: AOT GAME COMPILER
    val onAotGameCompiler: () -> Unit = {
        SoundEffectsManager.playClick()
        SoundEffectsManager.performHaptic(haptic)
        scope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                isOptimizing = true
            }
            val res = SmartEngine.applyArtOverdrive(context, true)
            withContext(Dispatchers.Main) {
                isOptimizing = false
                if (res.status == TweakStatus.SUCCESS) {
                    SoundEffectsManager.playOn()
                } else {
                    SoundEffectsManager.playError()
                }
                terminalText = "${res.logMessage}\n$terminalText"
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(backgroundBrush)) {
        HudGridBackground()
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
                        // DASHBOARD VIEW (Cleaned up: Duplicate Deep Cache Purge button removed)
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp)
                                .verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "SMART ENGINE DASHBOARD v0.8",
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
                                        // Silent navigation for category cards
                                        SoundEffectsManager.performHaptic(haptic)
                                        selectedCategory = category
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(18.dp))

                            // --- OVERCLOCKING (EXPERIMENTAL) ---
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth(0.9f)
                                    .border(1.5.dp, Color(0xFF8B0000), CutCornerShape(topStart = 16.dp, bottomEnd = 16.dp)),
                                colors = CardDefaults.cardColors(containerColor = CyberCardDark.copy(alpha = 0.9f)),
                                shape = CutCornerShape(topStart = 16.dp, bottomEnd = 16.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Outlined.Warning, "Warning", tint = Color(0xFF8B0000), modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            "⚠️ SMART OVERCLOCKING ENGINE (USE WITH CAUTION)",
                                            color = Color(0xFF8B0000),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Black,
                                            letterSpacing = 1.sp
                                        )
                                    }
                                    
                                    val dividerBrush = remember { Brush.horizontalGradient(listOf(Color(0xFF8B0000).copy(alpha = 0.5f), Color.Transparent)) }
                                    Box(modifier = Modifier.padding(vertical = 8.dp).fillMaxWidth().height(1.dp).background(dividerBrush))

                                    // TOP ACTION BUTTON IN OVERCLOCKING (Cleaned up: Flush Network DNS duplicate removed)
                                    TopActionButton(
                                        text = "PURGE STORAGE (FSTRIM)",
                                        subText = "Trims and aligns SSD blocks for zero-lag loading",
                                        icon = Icons.Outlined.Storage,
                                        accentColor = Color(0xFF8B0000),
                                        isLoading = fstrimProcessing,
                                        onClick = onPurgeStorageFstrim
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

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
                                                SoundEffectsManager.performHaptic(haptic)
                                                scope.launch(Dispatchers.IO) {
                                                    val res = SmartEngine.applyHwOverlayBypass(isOn)
                                                    withContext(Dispatchers.Main) {
                                                        processSmartResult(res, isOn, { hwOverlayBypass = it }, "key_hw_overlay")
                                                    }
                                                }
                                            },
                                            activeColor = Color(0xFF8B0000)
                                        )
                                    }

                                    // b) Native Game Mode Force
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "GAME MODE PERFORMANCE TURBO",
                                                color = Color.White,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Black
                                            )
                                            Text("Forces active CPU schedule & unrestricted power", color = Color.Gray, fontSize = 8.sp)
                                        }
                                        CyberpunkToggle(
                                            checked = gameModeForce,
                                            onCheckedChange = { isOn ->
                                                SoundEffectsManager.performHaptic(haptic)
                                                scope.launch(Dispatchers.IO) {
                                                    val res = SmartEngine.applyAndroidGameMode(context, isOn)
                                                    withContext(Dispatchers.Main) {
                                                        processSmartResult(res, isOn, { gameModeForce = it }, "key_game_mode")
                                                    }
                                                }
                                            },
                                            activeColor = Color(0xFF8B0000),
                                            isLoading = false
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            // STANDALONE TURBO REACTOR (REAL DEEP RAM & CPU OPTIMIZER)
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
                                                SoundEffectsManager.playClick()
                                                SoundEffectsManager.performHaptic(haptic)
                                                scope.launch(Dispatchers.IO) {
                                                    val res = SmartEngine.executeTurboReactor()
                                                    withContext(Dispatchers.Main) {
                                                        if (res.status == TweakStatus.SUCCESS) {
                                                            SoundEffectsManager.playOn()
                                                        } else {
                                                            SoundEffectsManager.playError()
                                                        }
                                                        terminalText = "${res.logMessage}\n$terminalText"
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

                            Spacer(modifier = Modifier.height(32.dp))
                            GlobalCommunityFooter()

                            Spacer(modifier = Modifier.height(100.dp))
                        }
                    } else {
                        // DETAIL VIEW FOR SELECTED CATEGORY
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp)
                                .verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { 
                                    SoundEffectsManager.playOff()
                                    SoundEffectsManager.performHaptic(haptic)
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
                            
                            Spacer(modifier = Modifier.height(16.dp))

                            // Root Experimental Warning Dialog
                            if (targetCategory == TweakCategory.EXPERIMENTAL && showExperimentalWarning) {
                                AlertDialog(
                                    onDismissRequest = { 
                                        showExperimentalWarning = false 
                                        selectedCategory = null 
                                    },
                                    title = { Text("Warning: High Risk Tweak", color = NeonCrimson, fontWeight = FontWeight.Black) },
                                    text = { Text("This feature pushes hardware to its absolute limits. It may cause kernel panics, device freezing, or game crashes on unsupported devices. Proceed at your own risk!", color = Color.White) },
                                    confirmButton = {
                                        TextButton(onClick = { 
                                            SoundEffectsManager.playOn()
                                            showExperimentalWarning = false 
                                        }) {
                                            Text("I ACCEPT", color = NeonCrimson, fontWeight = FontWeight.Black)
                                        }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = { 
                                            SoundEffectsManager.playOff()
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
                                                // TOP ACTION BUTTONS AT VERY TOP OF PERFORMANCE CATEGORY
                                                TopActionButton(
                                                    text = "AOT GAME COMPILER (MAX FPS)",
                                                    subText = "Pre-compiles game DEX assets into native machine code",
                                                    icon = Icons.Outlined.Bolt,
                                                    accentColor = NeonCrimson,
                                                    isLoading = isOptimizing,
                                                    onClick = onAotGameCompiler
                                                )

                                                TopActionButton(
                                                    text = "STORAGE CORE PURGE (FSTRIM)",
                                                    subText = "Trims and aligns SSD blocks for zero-lag texture loading",
                                                    icon = Icons.Outlined.Storage,
                                                    accentColor = NeonCrimson,
                                                    isLoading = fstrimProcessing,
                                                    onClick = onPurgeStorageFstrim
                                                )

                                                Spacer(modifier = Modifier.height(8.dp))

                                                CyberTweakToggleRow("ART Overdrive", "Optimizes DEX runtime for game assets", Icons.Outlined.AutoAwesome, artOverdrive, glowAlphaState) { isOn ->
                                                    SoundEffectsManager.performHaptic(haptic)
                                                    scope.launch(Dispatchers.IO) {
                                                        val res = SmartEngine.applyArtOverdrive(context, isOn)
                                                        withContext(Dispatchers.Main) {
                                                            processSmartResult(res, isOn, { artOverdrive = it }, "key_art_overdrive")
                                                        }
                                                    }
                                                }

                                                CyberTweakToggleRow("Android Game Mode", "Enables system-level performance mode schedule", Icons.Outlined.Games, gameMode, glowAlphaState) { isOn -> 
                                                    SoundEffectsManager.performHaptic(haptic)
                                                    scope.launch(Dispatchers.IO) {
                                                        val res = SmartEngine.applyAndroidGameMode(context, isOn)
                                                        withContext(Dispatchers.Main) {
                                                            processSmartResult(res, isOn, { gameMode = it }, "key_game_mode")
                                                        }
                                                    }
                                                }

                                                CyberTweakToggleRow("Phantom Processes", "Removes limits on background child processes", Icons.Outlined.Psychology, phantomProcess, glowAlphaState) { isOn -> 
                                                    SoundEffectsManager.performHaptic(haptic)
                                                    scope.launch(Dispatchers.IO) {
                                                        val res = SmartEngine.applyPhantomProcessLimiter(isOn)
                                                        withContext(Dispatchers.Main) {
                                                            processSmartResult(res, isOn, { phantomProcess = it }, "key_phantom_limiter")
                                                        }
                                                    }
                                                }

                                                CyberTweakToggleRow("Memory Heap Flush", "Aggressively flushes RAM heap to prevent stutters", Icons.Outlined.Memory, memoryHeapFlush, glowAlphaState) { isOn -> 
                                                    SoundEffectsManager.performHaptic(haptic)
                                                    scope.launch(Dispatchers.IO) {
                                                        val res = SmartEngine.applyMemoryHeapFlush(context, isOn)
                                                        withContext(Dispatchers.Main) {
                                                            processSmartResult(res, isOn, { memoryHeapFlush = it }, "key_mem_heap")
                                                        }
                                                    }
                                                }

                                                CyberTweakToggleRow("Game Turbo Core Lock (CPU Affinity)", "Forces game threads strictly onto high-performance Prime cores", Icons.Outlined.Hardware, cpuCorePinning, glowAlphaState) { isOn ->
                                                    val pkg = GfxBackendManager.getAutoDetectedGamePackage(context)
                                                    val cmdOn = "PID=\$(pidof $pkg); renice -n -20 -p \$PID; taskset -p f0 \$PID; for i in 4 5 6 7; do echo performance > /sys/devices/system/cpu/cpu\$i/cpufreq/scaling_governor 2>/dev/null; done"
                                                    val cmdOff = "for i in 4 5 6 7; do echo schedutil > /sys/devices/system/cpu/cpu\$i/cpufreq/scaling_governor 2>/dev/null; done"
                                                    runAiAgentTweak(
                                                        "CPU Pinning & Affinity",
                                                        "Pins game threads to high-performance CPU Prime cores.",
                                                        cmdOn,
                                                        cmdOff,
                                                        isOn,
                                                        { SmartEngine.verifyNodeExists("/sys/devices/system/cpu/cpu4/cpufreq/scaling_governor") },
                                                        { cpuCorePinning = it },
                                                        "key_cpu_pinning"
                                                    )
                                                }

                                                CyberTweakToggleRow("Storage TRIM", "Forces block alignment for zero-delay loading", Icons.Outlined.Storage, flashStorageTrim, glowAlphaState) { isOn ->
                                                    runAiAgentTweak(
                                                        "Storage TRIM",
                                                        "Executes kernel level fstrim block alignment.",
                                                        "sm fstrim",
                                                        "",
                                                        isOn,
                                                        { true },
                                                        { flashStorageTrim = it },
                                                        "key_storage_trim"
                                                    )
                                                }

                                                CyberTweakToggleRow("Smart ZRAM Compression Check", "Verifies & applies ZSTD swap compression with LZ4 fallback", Icons.Outlined.Compress, smartZram, glowAlphaState) { isOn ->
                                                    SoundEffectsManager.performHaptic(haptic)
                                                    scope.launch(Dispatchers.IO) {
                                                        val res = SmartEngine.applySmartZramCompression(isOn)
                                                        withContext(Dispatchers.Main) {
                                                            processSmartResult(res, isOn, { smartZram = it }, "key_smart_zram")
                                                        }
                                                    }
                                                }

                                                CyberTweakToggleRow("Storage Read-Ahead Optimization", "Scales block read-ahead buffer to 2048KB for texturing", Icons.Outlined.Storage, readAheadOpt, glowAlphaState) { isOn ->
                                                    val cmdOn = "if [ -d /sys/block/sda/queue ]; then echo 2048 > /sys/block/sda/queue/read_ahead_kb; fi; if [ -d /sys/block/mmcblk0/queue ]; then echo 2048 > /sys/block/mmcblk0/queue/read_ahead_kb; fi"
                                                    val cmdOff = "if [ -d /sys/block/sda/queue ]; then echo 128 > /sys/block/sda/queue/read_ahead_kb; fi; if [ -d /sys/block/mmcblk0/queue ]; then echo 128 > /sys/block/mmcblk0/queue/read_ahead_kb; fi"
                                                    runAiAgentTweak(
                                                        "Storage Read-Ahead",
                                                        "Increases flash memory block read-ahead buffer size to 2048KB.",
                                                        cmdOn,
                                                        cmdOff,
                                                        isOn,
                                                        { SmartEngine.verifyNodeExists("/sys/block/sda/queue") || SmartEngine.verifyNodeExists("/sys/block/mmcblk0/queue") },
                                                        { readAheadOpt = it },
                                                        "key_read_ahead_opt"
                                                    )
                                                }

                                                CyberTweakToggleRow("Daemon Thermal Killers", "Spoofs battery temp & stops thermal throttlers", Icons.Outlined.LocalFireDepartment, extremeThermalKillers, glowAlphaState) { isOn ->
                                                    SoundEffectsManager.performHaptic(haptic)
                                                    scope.launch(Dispatchers.IO) {
                                                        val res = SmartEngine.applyThermalAntiThrottle(isOn)
                                                        withContext(Dispatchers.Main) {
                                                            processSmartResult(res, isOn, { extremeThermalKillers = it }, "key_daemon_thermal")
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    TweakCategory.NETWORK -> {
                                        CyberTweakCard(title = "NETWORK & PING OPTIMIZATION", icon = Icons.Outlined.Wifi, statusHex = "0xNW99") {
                                            Column {
                                                // TOP ACTION BUTTON AT VERY TOP OF NETWORK CATEGORY
                                                TopActionButton(
                                                    text = "SOCKET & DNS CACHE FLUSH",
                                                    subText = "Flushes TCP socket buffers and DNS resolver cache to fix ping spikes",
                                                    icon = Icons.Outlined.WifiTethering,
                                                    accentColor = ElectricCyan,
                                                    onClick = onSocketDnsCacheFlush
                                                )

                                                Spacer(modifier = Modifier.height(8.dp))

                                                CyberTweakToggleRow(
                                                    label = "Smart Dynamic DNS (Auto-Optimize)",
                                                    description = "Pings providers & ISP routes to select the lowest latency path",
                                                    icon = Icons.Outlined.Dns,
                                                    checked = smartDnsEnabled,
                                                    glowAlphaState = glowAlphaState,
                                                    isLoading = isDnsOptimizing
                                                ) { isEnabled ->
                                                    if (isEnabled) {
                                                        SoundEffectsManager.performHaptic(haptic)
                                                        isDnsOptimizing = true
                                                        scope.launch(Dispatchers.IO) {
                                                            try {
                                                                withContext(Dispatchers.Main) {
                                                                    terminalText = "[AI AGENT] Analyzing Dynamic DNS Routes...\n" +
                                                                            "[INFO] Purpose: Pings Cloudflare, Google, and ISP resolvers to select lowest ping path.\n" +
                                                                            "[CHECK] Testing DNS latency...\n$terminalText"
                                                                }
                                                                val result = SmartDnsManager.findBestDns()
                                                                val detailLog = result.details.entries.joinToString(", ") { "${it.key}=${it.value}ms" }
                                                                
                                                                withContext(Dispatchers.Main) {
                                                                    terminalText = "[PROCESS] Route Latency Scan: $detailLog\n$terminalText"
                                                                }
                                                                
                                                                if (result.best.isDefault) {
                                                                    SmartEngine.executeRaw("settings put global private_dns_mode off")
                                                                    withContext(Dispatchers.Main) {
                                                                        SoundEffectsManager.playOn()
                                                                        terminalText = "[SUCCESS] ISP routing is optimal. Private DNS set to OFF.\n$terminalText"
                                                                        Toast.makeText(context, "ISP route is optimal. DNS Off.", Toast.LENGTH_SHORT).show()
                                                                    }
                                                                } else {
                                                                    val cmd = "settings put global private_dns_mode hostname && settings put global private_dns_specifier ${result.best.hostname}"
                                                                    SmartEngine.executeRaw(cmd)
                                                                    withContext(Dispatchers.Main) {
                                                                        SoundEffectsManager.playOn()
                                                                        terminalText = "[SUCCESS] DNS Optimized: Best route is ${result.best.name} (${result.best.hostname})\n$terminalText"
                                                                    }
                                                                }
                                                                
                                                                withContext(Dispatchers.Main) {
                                                                    smartDnsEnabled = true
                                                                    prefs.edit { putBoolean("key_smart_dns", true) }
                                                                }
                                                            } catch (_: Exception) {
                                                                withContext(Dispatchers.Main) {
                                                                    SoundEffectsManager.playError()
                                                                    terminalText = "[UNSUPPORTED] Feature blocked. Reason: Device restriction or missing kernel module.\n$terminalText"
                                                                    smartDnsEnabled = false
                                                                    prefs.edit { putBoolean("key_smart_dns", false) }
                                                                }
                                                            } finally {
                                                                withContext(Dispatchers.Main) {
                                                                    isDnsOptimizing = false
                                                                }
                                                            }
                                                        }
                                                    } else {
                                                        SoundEffectsManager.playOff()
                                                        SoundEffectsManager.performHaptic(haptic)
                                                        smartDnsEnabled = false
                                                        prefs.edit { putBoolean("key_smart_dns", false) }
                                                        scope.launch(Dispatchers.IO) {
                                                            SmartEngine.executeRaw("settings put global private_dns_mode off")
                                                            withContext(Dispatchers.Main) {
                                                                terminalText = "[AI AGENT] Smart DNS Disabled. Defaults Restored.\n$terminalText"
                                                            }
                                                        }
                                                    }
                                                }

                                                CyberTweakToggleRow("TCP BBR Congestion Control", "Enables Google's BBR for zero packet loss if supported", Icons.Outlined.NetworkCheck, tcpBbr, glowAlphaState) { isOn ->
                                                    SoundEffectsManager.performHaptic(haptic)
                                                    scope.launch(Dispatchers.IO) {
                                                        val res = SmartEngine.applyTcpBbr(isOn)
                                                        withContext(Dispatchers.Main) {
                                                            processSmartResult(res, isOn, { tcpBbr = it }, "key_tcp_bbr")
                                                        }
                                                    }
                                                }

                                                CyberTweakToggleRow("Dynamic MTU Interface Tuning", "Tunes active network interface MTU to 1428 to fix packet fragmentation", Icons.Outlined.Compress, dynamicMtu, glowAlphaState) { isOn ->
                                                    SoundEffectsManager.performHaptic(haptic)
                                                    scope.launch(Dispatchers.IO) {
                                                        val res = SmartEngine.applySmartMtuTuning(isOn)
                                                        withContext(Dispatchers.Main) {
                                                            processSmartResult(res, isOn, { dynamicMtu = it }, "key_dynamic_mtu")
                                                        }
                                                    }
                                                }

                                                CyberTweakToggleRow("fq_codel Queue Discipline", "Enables fq_codel qdisc to fix bufferbloat and ping spikes", Icons.Outlined.Speed, fqCodel, glowAlphaState) { isOn ->
                                                    SoundEffectsManager.performHaptic(haptic)
                                                    scope.launch(Dispatchers.IO) {
                                                        val res = SmartEngine.applyFqCodel(isOn)
                                                        withContext(Dispatchers.Main) {
                                                            processSmartResult(res, isOn, { fqCodel = it }, "key_fq_codel")
                                                        }
                                                    }
                                                }

                                                CyberTweakToggleRow("Smart Network & Ping Stabilizer", "Disables background WiFi scanning & tunes TCP buffers", Icons.Outlined.Router, smartNetStabilizer, glowAlphaState) { isOn ->
                                                    SoundEffectsManager.performHaptic(haptic)
                                                    scope.launch(Dispatchers.IO) {
                                                        val res = SmartEngine.applySmartNetworkAndPingStabilizer(isOn)
                                                        withContext(Dispatchers.Main) {
                                                            processSmartResult(res, isOn, { smartNetStabilizer = it }, "key_smart_net_stab")
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    TweakCategory.HUD_VISUALS -> {
                                        CyberTweakCard(title = "HUD & VISUALS", icon = Icons.Outlined.Monitor, statusHex = "0xV721") {
                                            Column {
                                                CyberTweakToggleRow("Real-Time HUD", "Displays FPS and System stats over game", Icons.Outlined.Visibility, hudOverlayEnabled, glowAlphaState) { isOn -> 
                                                    if (!Settings.canDrawOverlays(context)) {
                                                        SoundEffectsManager.playError()
                                                        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, "package:${context.packageName}".toUri())
                                                        context.startActivity(intent)
                                                    } else {
                                                        prefs.edit { putBoolean("key_hud_overlay", isOn) }
                                                        if (isOn) {
                                                            SoundEffectsManager.playOn()
                                                            context.startService(Intent(context, RealTimeHudService::class.java)) 
                                                        } else {
                                                            SoundEffectsManager.playOff()
                                                            context.stopService(Intent(context, RealTimeHudService::class.java))
                                                        }
                                                    }
                                                }

                                                CyberTweakToggleRow("Smart Floating Overlay", "Draggable crosshair and tactical FPS HUD", Icons.Outlined.CenterFocusWeak, floatingOverlayEnabled, glowAlphaState) { isOn ->
                                                    if (!Settings.canDrawOverlays(context)) {
                                                        SoundEffectsManager.playError()
                                                        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, "package:${context.packageName}".toUri())
                                                        context.startActivity(intent)
                                                    } else {
                                                        prefs.edit { putBoolean("key_floating_overlay", isOn) }
                                                        if (isOn) {
                                                            SoundEffectsManager.playOn()
                                                            context.startService(Intent(context, CrosshairService::class.java))
                                                        } else {
                                                            SoundEffectsManager.playOff()
                                                            context.stopService(Intent(context, CrosshairService::class.java))
                                                        }
                                                    }
                                                }
                                                
                                                Text("DYNAMIC REFRESH RATE", color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp, bottom = 8.dp))
                                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                    listOf("60Hz", "90Hz", "120Hz").forEach { rate ->
                                                        val isSelected = refreshRate == rate
                                                        CyberTweakChip(rate, isSelected) {
                                                            refreshRate = rate
                                                            prefs.edit { putString("key_refresh_rate", rate) }
                                                            SoundEffectsManager.playClick()
                                                            SoundEffectsManager.performHaptic(haptic)
                                                            scope.launch(Dispatchers.IO) {
                                                                try {
                                                                    if (SmartEngine.checkDynamicRefreshRate()) {
                                                                        val rateVal = rate.replace("Hz", ".0")
                                                                        SmartEngine.executeRaw("settings put system peak_refresh_rate $rateVal")
                                                                    }
                                                                } catch (_: Exception) {}
                                                            }
                                                        }
                                                    }
                                                }

                                                CyberTweakToggleRow("HW Overlay Bypass", "Forces GPU to handle all screen layers safely", Icons.Outlined.Layers, hardwareOverlayBypass, glowAlphaState) { isOn -> 
                                                    SoundEffectsManager.performHaptic(haptic)
                                                    scope.launch(Dispatchers.IO) {
                                                        val res = SmartEngine.applyHwOverlayBypass(isOn)
                                                        withContext(Dispatchers.Main) {
                                                            processSmartResult(res, isOn, { hardwareOverlayBypass = it }, "key_hw_overlay")
                                                        }
                                                    }
                                                }
                                                CyberTweakToggleRow("Vulkan Skia Opt", "Forces Vulkan backend for 2D UI rendering", Icons.Outlined.Architecture, vulkanSkia, glowAlphaState) { isOn -> 
                                                    SoundEffectsManager.performHaptic(haptic)
                                                    scope.launch(Dispatchers.IO) {
                                                        val res = SmartEngine.applyVulkanSkia(isOn)
                                                        withContext(Dispatchers.Main) {
                                                            processSmartResult(res, isOn, { vulkanSkia = it }, "key_vulkan_skia")
                                                        }
                                                    }
                                                }
                                                CyberTweakToggleRow("Force 4x MSAA", "Injects anti-aliasing via GPU backend", Icons.Outlined.Gradient, forceMsaa4x, glowAlphaState) { isOn ->
                                                    SoundEffectsManager.performHaptic(haptic)
                                                    scope.launch(Dispatchers.IO) {
                                                        val res = SmartEngine.applyForceMsaa4x(isOn)
                                                        withContext(Dispatchers.Main) {
                                                            processSmartResult(res, isOn, { forceMsaa4x = it }, "key_force_msaa")
                                                        }
                                                    }
                                                }
                                                CyberTweakToggleRow("V-Sync Disable", "Bypasses frame limits for raw uncapped FPS", Icons.Outlined.TimerOff, vSyncDisable, glowAlphaState) { isOn ->
                                                    SoundEffectsManager.performHaptic(haptic)
                                                    scope.launch(Dispatchers.IO) {
                                                        val res = SmartEngine.applyVSyncDisable(isOn)
                                                        withContext(Dispatchers.Main) {
                                                            processSmartResult(res, isOn, { vSyncDisable = it }, "key_vsync_disable")
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    TweakCategory.TOUCH_AUDIO -> {
                                        Column {
                                            CyberTweakCard(title = "ADVANCED TOUCH & AUDIO", icon = Icons.Outlined.Mouse, statusHex = "0xI/O") {
                                                Column {
                                                    CyberTweakToggleRow("Smart Touch Optimizer", "Consolidates Pointer Speed, Touch Sampling & Polling Boosters", Icons.Outlined.AdsClick, smartTouchOptimizer, glowAlphaState) { isOn ->
                                                        SoundEffectsManager.performHaptic(haptic)
                                                        scope.launch(Dispatchers.IO) {
                                                            val res = SmartEngine.applySmartTouchOptimizer(isOn)
                                                            withContext(Dispatchers.Main) {
                                                                processSmartResult(res, isOn, { smartTouchOptimizer = it }, "key_smart_touch_opt")
                                                            }
                                                        }
                                                    }

                                                    CyberTweakToggleRow("CPU Input Boost", "Triggers CPU boost on user touch inputs for lag-free response", Icons.Outlined.FlashOn, cpuInputBoost, glowAlphaState) { isOn ->
                                                        SoundEffectsManager.performHaptic(haptic)
                                                        scope.launch(Dispatchers.IO) {
                                                            val res = SmartEngine.applyCpuInputBoost(isOn)
                                                            withContext(Dispatchers.Main) {
                                                                processSmartResult(res, isOn, { cpuInputBoost = it }, "key_cpu_input_boost")
                                                            }
                                                        }
                                                    }

                                                    CyberTweakToggleRow("UI Touch Speed", "Minimizes UI animation delay for speed", Icons.Outlined.TouchApp, uiTouchSpeed, glowAlphaState) { isOn -> 
                                                        SoundEffectsManager.performHaptic(haptic)
                                                        scope.launch(Dispatchers.IO) {
                                                            val res = SmartEngine.applyUiTouchSpeed(isOn)
                                                            withContext(Dispatchers.Main) {
                                                                processSmartResult(res, isOn, { uiTouchSpeed = it }, "key_touch_speed")
                                                            }
                                                        }
                                                    }
                                                    CyberTweakToggleRow("High Precision Aim", "Increases touch sensitivity in-game", Icons.Outlined.MyLocation, precisionAim, glowAlphaState) { isOn -> 
                                                        SoundEffectsManager.performHaptic(haptic)
                                                        scope.launch(Dispatchers.IO) {
                                                            val res = SmartEngine.applyHighPrecisionAim(isOn)
                                                            withContext(Dispatchers.Main) {
                                                                processSmartResult(res, isOn, { precisionAim = it }, "key_precision_aim")
                                                            }
                                                        }
                                                    }
                                                    CyberTweakToggleRow("Frame Latency", "Reduces render queue for faster response", Icons.Outlined.AvTimer, frameLatency, glowAlphaState) { isOn -> 
                                                        SoundEffectsManager.performHaptic(haptic)
                                                        scope.launch(Dispatchers.IO) {
                                                            val res = SmartEngine.applyFrameLatency(isOn)
                                                            withContext(Dispatchers.Main) {
                                                                processSmartResult(res, isOn, { frameLatency = it }, "key_frame_latency")
                                                            }
                                                        }
                                                    }
                                                    CyberTweakToggleRow("FIFO UI Governor", "Sets UI thread to highest priority", Icons.Outlined.LinearScale, fifoUi, glowAlphaState) { isOn -> 
                                                        SoundEffectsManager.performHaptic(haptic)
                                                        scope.launch(Dispatchers.IO) {
                                                            val res = SmartEngine.applyFifoUiGovernor(isOn)
                                                            withContext(Dispatchers.Main) {
                                                                processSmartResult(res, isOn, { fifoUi = it }, "key_fifo_ui")
                                                            }
                                                        }
                                                    }
                                                    CyberTweakToggleRow("Audio Buffer Slicer", "Removes Android buffer for instant footsteps", Icons.AutoMirrored.Outlined.VolumeUp, audioBufferSlicer, glowAlphaState) { isOn ->
                                                        SoundEffectsManager.performHaptic(haptic)
                                                        scope.launch(Dispatchers.IO) {
                                                            val res = SmartEngine.applyAudioBufferSlicer(isOn)
                                                            withContext(Dispatchers.Main) {
                                                                processSmartResult(res, isOn, { audioBufferSlicer = it }, "key_audio_slicer")
                                                            }
                                                        }
                                                    }
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(14.dp))

                                            // BLUETOOTH AUDIO ENFORCER
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
                                                        onCheckedChange = { isOn -> 
                                                            val cmdOn = "setprop persist.bluetooth.latency.low true && setprop persist.vendor.bt.a2dp.latency.low true"
                                                            val cmdOff = "setprop persist.bluetooth.latency.low false"
                                                            runAiAgentTweak(
                                                                "Bluetooth Low Latency",
                                                                "Enforces low-latency Bluetooth A2DP audio mode for wireless headsets.",
                                                                cmdOn,
                                                                cmdOff,
                                                                isOn,
                                                                { SmartEngine.checkAudioBufferSlicerAndBluetooth() },
                                                                { btLowLatency = it },
                                                                "key_bt_latency"
                                                            )
                                                        },
                                                        activeColor = NeonCrimson
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    TweakCategory.DEVICE_MODULES -> {
                                        CyberTweakCard(title = "DEVICE-SPECIFIC MODULES (OEM VERIFIED)", icon = Icons.Outlined.Smartphone, statusHex = "0xBRND") {
                                            Column {
                                                CyberTweakToggleRow("Samsung GOS Killer", "Disables Game Optimization Service (Samsung Devices Only)", Icons.Outlined.DomainVerification, samsungGosKiller, glowAlphaState) { isOn ->
                                                    SoundEffectsManager.performHaptic(haptic)
                                                    scope.launch(Dispatchers.IO) {
                                                        val res = SmartEngine.applySamsungGosKiller(isOn)
                                                        withContext(Dispatchers.Main) {
                                                            processSmartResult(res, isOn, { samsungGosKiller = it }, "key_samsung_gos")
                                                        }
                                                    }
                                                }
                                                CyberTweakToggleRow("Xiaomi Joyose Disabler", "Stops cloud-based frame throttling (Xiaomi/Redmi/Poco Only)", Icons.Outlined.AppRegistration, xiaomiJoyose, glowAlphaState) { isOn ->
                                                    SoundEffectsManager.performHaptic(haptic)
                                                    scope.launch(Dispatchers.IO) {
                                                        val res = SmartEngine.applyXiaomiJoyoseDisabler(isOn)
                                                        withContext(Dispatchers.Main) {
                                                            processSmartResult(res, isOn, { xiaomiJoyose = it }, "key_xiaomi_joyose")
                                                        }
                                                    }
                                                }
                                                CyberTweakToggleRow("Aggressive Doze & RAM Flush", "Forces 100% RAM allocation to game window", Icons.Outlined.AutoFixHigh, brandAggressiveDoze, glowAlphaState) { isOn ->
                                                    runAiAgentTweak(
                                                        "Aggressive Doze & RAM Flush",
                                                        "Forces device idle doze state and terminates background apps for max RAM.",
                                                        "cmd deviceidle force-idle && am kill-all",
                                                        "cmd deviceidle unforce",
                                                        isOn,
                                                        { SmartEngine.checkStorageCorePurgeAndTrim() },
                                                        { brandAggressiveDoze = it },
                                                        "key_brand_doze"
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    TweakCategory.EXPERIMENTAL -> {
                                        CyberTweakCard(title = "EXPERIMENTAL / EXTREME TWEAKS ⚠️", icon = Icons.Outlined.Science, statusHex = "0xRISK") {
                                            Column {
                                                CyberTweakToggleRow("ZRAM Bypass", "Disables virtual swap for physical RAM", Icons.Outlined.SdCard, zramBypass, glowAlphaState, isRisky = true) { isOn ->
                                                    SoundEffectsManager.performHaptic(haptic)
                                                    scope.launch(Dispatchers.IO) {
                                                        val res = SmartEngine.applyZramBypass(isOn)
                                                        withContext(Dispatchers.Main) {
                                                            processSmartResult(res, isOn, { zramBypass = it }, "key_zram_bypass")
                                                        }
                                                    }
                                                }
                                                CyberTweakToggleRow("Anti-Throttle", "Spoofs battery temp to 25°C", Icons.Outlined.Thermostat, fakeBatteryTemp, glowAlphaState, isRisky = true) { isOn ->
                                                    SoundEffectsManager.performHaptic(haptic)
                                                    scope.launch(Dispatchers.IO) {
                                                        val res = SmartEngine.applyThermalAntiThrottle(isOn)
                                                        withContext(Dispatchers.Main) {
                                                            processSmartResult(res, isOn, { fakeBatteryTemp = it }, "key_fake_temp")
                                                        }
                                                    }
                                                }
                                                CyberTweakToggleRow("LMK Protection", "Prevents system from closing game", Icons.Outlined.Shield, lmkProtection, glowAlphaState, isRisky = true) { isOn -> 
                                                    SoundEffectsManager.performHaptic(haptic)
                                                    scope.launch(Dispatchers.IO) {
                                                        val res = SmartEngine.applyLmkProtection(isOn)
                                                        withContext(Dispatchers.Main) {
                                                            processSmartResult(res, isOn, { lmkProtection = it }, "key_lmk_prot")
                                                        }
                                                    }
                                                }
                                                CyberTweakToggleRow("ART Compiler Turbo", "Optimizes DEX assets for speed", Icons.Outlined.RocketLaunch, artCompiler, glowAlphaState, isRisky = true) { isOn -> 
                                                    SoundEffectsManager.performHaptic(haptic)
                                                    scope.launch(Dispatchers.IO) {
                                                        val res = SmartEngine.applyArtOverdrive(context, isOn)
                                                        withContext(Dispatchers.Main) {
                                                            processSmartResult(res, isOn, { artCompiler = it }, "key_art_turbo")
                                                        }
                                                    }
                                                }
                                                CyberTweakToggleRow("Thermal Illusion", "Bypasses system thermal limits", Icons.Outlined.AcUnit, thermalThrottling, glowAlphaState, isRisky = true) { isOn -> 
                                                    runAiAgentTweak(
                                                        "Thermal Illusion",
                                                        "Overrides thermalservice status to 0 (NONE) to bypass thermal throttling.",
                                                        "cmd thermalservice override-status 0",
                                                        "cmd thermalservice reset",
                                                        isOn,
                                                        { SmartEngine.checkThermalAntiThrottle() },
                                                        { thermalThrottling = it },
                                                        "key_thermal_over"
                                                    )
                                                }
                                                CyberTweakToggleRow("Kill System Loggers", "Reduces background I/O overhead", Icons.Outlined.Terminal, logdAnnihilator, glowAlphaState, isRisky = true) { isOn ->
                                                    runAiAgentTweak(
                                                        "Kill System Loggers",
                                                        "Stops system logd daemon to eliminate log writing storage I/O lag.",
                                                        "stop logd",
                                                        "start logd",
                                                        isOn,
                                                        { SmartEngine.checkKillSystemLoggers() },
                                                        { logdAnnihilator = it },
                                                        "key_logd_kill"
                                                    )
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
                                style = TerminalTextStyle,
                                fontSize = 12.sp
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
                        text = "SECURE ENGINE PROTOCOL v0.8",
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
            .tacticalPress { onClick() }
            .border(1.dp, neonColor.copy(alpha = 0.3f), CutCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = CutCornerShape(16.dp)
    ) {
        Box {
            // Glassmorphism Background
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.White.copy(alpha = 0.05f), Color.Transparent)
                        )
                    )
                    .background(Color.Black.copy(alpha = 0.4f))
                    .blur(8.dp)
            )

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
                    style = TerminalTextStyle,
                    fontSize = 9.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
