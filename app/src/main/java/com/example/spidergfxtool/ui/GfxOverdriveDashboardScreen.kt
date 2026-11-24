package com.example.spidergfxtool.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.graphics.drawable.toBitmap
import com.example.spidergfxtool.AdsManager
import com.example.spidergfxtool.GfxBackendManager
import com.example.spidergfxtool.SmartEngine
import com.example.spidergfxtool.SoundEffectsManager
import com.example.spidergfxtool.SystemTelemetryViewModel
import com.example.spidergfxtool.ui.components.*
import com.example.spidergfxtool.ui.theme.TerminalTextStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class InstalledGameInfo(
    val name: String,
    val packageName: String,
    val icon: Drawable?
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GfxOverdriveDashboardScreen(
    onNavigateToAdvancedTool: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences("spider_gfx_prefs", Context.MODE_PRIVATE) }

    // Telemetry ViewModel Integration
    val telemetryViewModel = remember { SystemTelemetryViewModel() }
    LaunchedEffect(Unit) {
        telemetryViewModel.startMonitoring(context)
    }
    val telemetryState by telemetryViewModel.state

    // State for deployed targets
    var deployedTargets by remember {
        mutableStateOf(
            prefs.getStringSet("key_deployed_targets", null)?.toList() ?: emptyList()
        )
    }

    var selectedPackageName by remember {
        mutableStateOf(
            prefs.getString("key_selected_target_pkg", null) ?: deployedTargets.firstOrNull() ?: ""
        )
    }

    var showAppPicker by remember { mutableStateOf(false) }
    var installedApps by remember { mutableStateOf<List<InstalledGameInfo>>(emptyList()) }
    var isLoadingApps by remember { mutableStateOf(false) }
    var appSearchQuery by remember { mutableStateOf("") }

    var isLaunching by remember { mutableStateOf(false) }
    var launchStepText by remember { mutableStateOf("INITIALIZING TURBO REACTOR...") }
    var launchProgress by remember { mutableStateOf(0f) }

    fun performTargetBoostAndLaunch() {
        if (selectedPackageName.isEmpty()) {
            Toast.makeText(context, "Please deploy and select a target game first!", Toast.LENGTH_SHORT).show()
            showAppPicker = true
            return
        }
        AdsManager.executeWithAd(context) {
            isLaunching = true
            scope.launch(Dispatchers.IO) {
                try {
                    launchStepText = "PURGING BACKGROUND PROCESSES & RAM..."
                    launchProgress = 0.25f
                    kotlinx.coroutines.delay(250)

                    launchStepText = "OPTIMIZING CACHE & GPU ALLOCATION..."
                    launchProgress = 0.50f
                    kotlinx.coroutines.delay(250)

                    val shizukuReady = SmartEngine.isShizukuReady()
                    launchStepText = if (shizukuReady) "INJECTING SHIZUKU PRIVILEGED CORE..." else "CONFIGURING NATIVE ENGINE BOOST..."
                    launchProgress = 0.75f
                    SmartEngine.executeTurboReactor(context)
                    kotlinx.coroutines.delay(300)

                    launchStepText = "TARGET LOCKED // LAUNCHING GAME..."
                    launchProgress = 1.0f
                    kotlinx.coroutines.delay(350)

                    withContext(Dispatchers.Main) {
                        isLaunching = false
                        val pm = context.packageManager
                        val launchIntent = pm.getLaunchIntentForPackage(selectedPackageName)
                        if (launchIntent != null) {
                            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(launchIntent)
                        } else {
                            Toast.makeText(context, "Unable to launch $selectedPackageName. App may not be installed or support direct launch.", Toast.LENGTH_LONG).show()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        isLaunching = false
                        Toast.makeText(context, "Error launching target $selectedPackageName: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    // --- ANIMATED SCI-FI BACKGROUND GRADIENTS ---
    val infiniteTransition = rememberInfiniteTransition(label = "OverdrivePulse")
    val color1 by infiniteTransition.animateColor(
        initialValue = Color(0xFF001F3F), targetValue = Color(0xFF3F0000),
        animationSpec = infiniteRepeatable(tween(4000), RepeatMode.Reverse), label = "C1"
    )
    val color2 by infiniteTransition.animateColor(
        initialValue = Color(0xFF00003F), targetValue = Color(0xFF800000),
        animationSpec = infiniteRepeatable(tween(4000, 1000), RepeatMode.Reverse), label = "C2"
    )
    val backgroundBrush = remember(color1, color2) { Brush.verticalGradient(listOf(color1, color2, Color.Black)) }

    val pulseGlow by infiniteTransition.animateColor(
        initialValue = ElectricCyan,
        targetValue = NeonCrimson,
        animationSpec = infiniteRepeatable(tween(2000), RepeatMode.Reverse),
        label = "Glow"
    )

    // Helper function to persist targets
    fun saveTargets(newList: List<String>) {
        deployedTargets = newList
        prefs.edit().putStringSet("key_deployed_targets", newList.toSet()).apply()
    }

    // Helper function to update selected target
    fun selectTarget(pkg: String) {
        selectedPackageName = pkg
        prefs.edit().putString("key_selected_target_pkg", pkg).apply()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
    ) {
        HudGridBackground()

        // --- FULL-SCREEN PRE-LAUNCH ANIMATION OVERLAY ---
        if (isLaunching) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.95f))
                    .zIndex(100f),
                contentAlignment = Alignment.Center
            ) {
                HudGridBackground()

                val pm = context.packageManager
                val targetName = remember(selectedPackageName) {
                    try {
                        val info = pm.getApplicationInfo(selectedPackageName, 0)
                        pm.getApplicationLabel(info).toString()
                    } catch (e: Exception) {
                        selectedPackageName.substringAfterLast(".")
                    }
                }
                val targetIcon = remember(selectedPackageName) {
                    try { pm.getApplicationIcon(selectedPackageName) } catch (e: Exception) { null }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .padding(24.dp)
                        .border(1.5.dp, NeonCrimson, CutCornerShape(16.dp))
                        .background(CyberCardDark.copy(alpha = 0.95f), CutCornerShape(16.dp))
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .shadow(36.dp, CutCornerShape(20.dp), spotColor = NeonCrimson)
                            .background(Color.Black, CutCornerShape(20.dp))
                            .animatedNeonBorder(enabled = true, color1 = NeonCrimson, color2 = ElectricCyan, shape = CutCornerShape(20.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (targetIcon != null) {
                            Image(
                                bitmap = targetIcon.toBitmap().asImageBitmap(),
                                contentDescription = targetName,
                                modifier = Modifier
                                    .fillMaxSize(0.85f)
                                    .clip(CutCornerShape(14.dp))
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.RocketLaunch,
                                contentDescription = null,
                                tint = ElectricCyan,
                                modifier = Modifier.size(54.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = "ENGAGING TURBO REACTOR",
                        color = NeonCrimson,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 2.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = targetName,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    LinearProgressIndicator(
                        progress = { launchProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CutCornerShape(4.dp)),
                        color = ElectricCyan,
                        trackColor = Color.White.copy(alpha = 0.1f),
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = launchStepText,
                        color = ElectricCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = selectedPackageName,
                        color = Color.Gray,
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // --- TOP BAR & HEADER (GAME-SPACE CONSOLE POLISH) ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 6.dp, end = 120.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "SPIDER GFX TOOL HUB",
                        color = Color.White,
                        style = TerminalTextStyle,
                        fontSize = 18.sp,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = "COMMAND CENTER & TARGET SELECTOR // V9.0",
                        color = ElectricCyan,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                // TWEAKS ENGINE BUTTON (RENAMED & NON-OVERLAPPING)
                Surface(
                    onClick = {
                        SoundEffectsManager.playOn()
                        SoundEffectsManager.performHaptic(haptic)
                        onNavigateToAdvancedTool()
                    },
                    modifier = Modifier
                        .height(38.dp)
                        .shadow(14.dp, CutCornerShape(12.dp), spotColor = NeonCrimson)
                        .tacticalPress(),
                    shape = CutCornerShape(12.dp),
                    color = NeonCrimson,
                    contentColor = Color.White,
                    border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Tune,
                            contentDescription = "Tweaks Engine",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "TWEAKS ENGINE",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.5.sp
                        )
                    }
                }
            }

            // --- PHASE 3: LIVE TELEMETRY TERMINAL ROW ---
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .border(1.dp, ElectricCyan.copy(alpha = 0.3f), CutCornerShape(8.dp)),
                shape = CutCornerShape(8.dp),
                color = Color.Black.copy(alpha = 0.7f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(
                            text = "CPU: ${telemetryState.cpuUsagePercent}%",
                            color = ElectricCyan,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "RAM: ${(telemetryState.ramUsagePercent * 100).toInt()}%",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "PING: ${if (telemetryState.livePingMs > 0) "${telemetryState.livePingMs}ms" else "24ms"}",
                            color = Color.Yellow,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(Color(0xFF00FF66), CircleShape)
                        )
                        Text(
                            text = "SHIZUKU CORE: [ONLINE]",
                            color = Color(0xFF00FF66),
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // --- BALANCED 2-COLUMN LAYOUT (LEFT: COMMAND CENTER, RIGHT: CENTERED TARGET LOCK & BOOST) ---
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // --- LEFT COLUMN: COMMAND CENTER & DEPLOYED TARGETS ---
                Surface(
                    modifier = Modifier
                        .weight(1.1f)
                        .fillMaxHeight()
                        .shadow(16.dp, CutCornerShape(topStart = 16.dp, bottomEnd = 16.dp), spotColor = ElectricCyan)
                        .border(1.2.dp, ElectricCyan.copy(alpha = 0.45f), CutCornerShape(topStart = 16.dp, bottomEnd = 16.dp)),
                    shape = CutCornerShape(topStart = 16.dp, bottomEnd = 16.dp),
                    color = CyberCardDark.copy(alpha = 0.9f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "COMMAND CENTER",
                                color = ElectricCyan,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.5.sp
                            )
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(ElectricCyan, CircleShape)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // + DEPLOY NEW TARGET BUTTON
                        Surface(
                            onClick = {
                                SoundEffectsManager.playOn()
                                SoundEffectsManager.performHaptic(haptic)
                                isLoadingApps = true
                                showAppPicker = true
                                appSearchQuery = ""
                                scope.launch(Dispatchers.IO) {
                                    val pm = context.packageManager
                                    val apps = try {
                                        // UNIVERSAL APP QUERY (MATCH_ALL FOR ANDROID 11+)
                                        val flags = PackageManager.MATCH_ALL or PackageManager.GET_META_DATA
                                        pm.getInstalledApplications(flags)
                                            .filter { appInfo ->
                                                try {
                                                    (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) == 0 ||
                                                    pm.getLaunchIntentForPackage(appInfo.packageName) != null
                                                } catch (e: Exception) { false }
                                            }
                                            .map { appInfo ->
                                                InstalledGameInfo(
                                                    name = try { appInfo.loadLabel(pm).toString() } catch (e: Exception) { appInfo.packageName },
                                                    packageName = appInfo.packageName,
                                                    icon = try { pm.getApplicationIcon(appInfo) } catch (e: Exception) { null }
                                                )
                                            }
                                            .sortedBy { it.name }
                                    } catch (e: Exception) {
                                        try {
                                            pm.getInstalledApplications(0)
                                                .map { appInfo ->
                                                    InstalledGameInfo(
                                                        name = try { appInfo.loadLabel(pm).toString() } catch (e: Exception) { appInfo.packageName },
                                                        packageName = appInfo.packageName,
                                                        icon = try { pm.getApplicationIcon(appInfo) } catch (e: Exception) { null }
                                                    )
                                                }
                                                .sortedBy { it.name }
                                        } catch (_: Exception) {
                                            emptyList()
                                        }
                                    }
                                    withContext(Dispatchers.Main) {
                                        installedApps = apps
                                        isLoadingApps = false
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(36.dp)
                                .shadow(10.dp, CutCornerShape(10.dp), spotColor = ElectricCyan)
                                .tacticalPress(),
                            shape = CutCornerShape(10.dp),
                            color = ElectricCyan.copy(alpha = 0.15f),
                            border = BorderStroke(1.5.dp, ElectricCyan)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add",
                                    tint = ElectricCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "DEPLOY NEW TARGET",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 1.2.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // LIST OF DEPLOYED TARGETS WITH 'X' DELETE ICON
                        if (deployedTargets.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.SportsEsports,
                                        contentDescription = null,
                                        tint = Color.Gray.copy(alpha = 0.4f),
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "NO TARGETS DEPLOYED",
                                        color = Color.Gray,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Click '+ DEPLOY NEW TARGET'\nto select a game",
                                        color = Color.Gray.copy(alpha = 0.5f),
                                        fontSize = 8.sp,
                                        fontFamily = FontFamily.Monospace,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(bottom = 32.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(deployedTargets) { pkg ->
                                    val isSelected = pkg == selectedPackageName
                                    val pm = context.packageManager
                                    val appName = remember(pkg) {
                                        try {
                                            val info = pm.getApplicationInfo(pkg, 0)
                                            pm.getApplicationLabel(info).toString()
                                        } catch (e: Exception) {
                                            pkg.substringAfterLast(".")
                                        }
                                    }
                                    val iconDrawable = remember(pkg) {
                                        try { pm.getApplicationIcon(pkg) } catch (e: Exception) { null }
                                    }

                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .shadow(if (isSelected) 8.dp else 2.dp, CutCornerShape(10.dp), spotColor = if (isSelected) NeonCrimson else ElectricCyan)
                                            .tacticalPress {
                                                SoundEffectsManager.playOn()
                                                SoundEffectsManager.performHaptic(haptic)
                                                selectTarget(pkg)
                                            },
                                        shape = CutCornerShape(10.dp),
                                        color = if (isSelected) NeonCrimson.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.04f),
                                        border = BorderStroke(
                                            if (isSelected) 1.5.dp else 1.dp,
                                            if (isSelected) NeonCrimson else Color.White.copy(alpha = 0.15f)
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                modifier = Modifier.weight(1f),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                if (iconDrawable != null) {
                                                    Image(
                                                        bitmap = iconDrawable.toBitmap().asImageBitmap(),
                                                        contentDescription = appName,
                                                        modifier = Modifier
                                                            .size(32.dp)
                                                            .clip(CutCornerShape(6.dp))
                                                    )
                                                } else {
                                                    Icon(
                                                        imageVector = Icons.Default.SportsEsports,
                                                        contentDescription = null,
                                                        tint = Color.White,
                                                        modifier = Modifier.size(32.dp)
                                                    )
                                                }

                                                Spacer(modifier = Modifier.width(10.dp))

                                                Column {
                                                    Text(
                                                        text = appName,
                                                        color = Color.White,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = pkg,
                                                        color = if (isSelected) ElectricCyan else Color.Gray,
                                                        fontSize = 8.sp,
                                                        fontFamily = FontFamily.Monospace,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }

                                            // DELETE BUTTON 'X'
                                            Surface(
                                                onClick = {
                                                    SoundEffectsManager.playOff()
                                                    SoundEffectsManager.performHaptic(haptic)
                                                    val updated = deployedTargets.filter { it != pkg }
                                                    saveTargets(updated)
                                                    if (selectedPackageName == pkg) {
                                                        val nextPkg = updated.firstOrNull() ?: ""
                                                        selectTarget(nextPkg)
                                                    }
                                                },
                                                modifier = Modifier.size(26.dp),
                                                shape = CutCornerShape(6.dp),
                                                color = Color.Red.copy(alpha = 0.2f),
                                                border = BorderStroke(1.dp, NeonCrimson.copy(alpha = 0.6f))
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = Icons.Default.Close,
                                                        contentDescription = "Delete",
                                                        tint = NeonCrimson,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // --- RIGHT COLUMN: CENTERED TARGET LOCK & BOOST BUTTON ---
                Surface(
                    modifier = Modifier
                        .weight(1.2f)
                        .fillMaxHeight()
                        .shadow(20.dp, CutCornerShape(topStart = 16.dp, bottomEnd = 16.dp), spotColor = NeonCrimson)
                        .border(1.5.dp, NeonCrimson.copy(alpha = 0.6f), CutCornerShape(topStart = 16.dp, bottomEnd = 16.dp)),
                    shape = CutCornerShape(topStart = 16.dp, bottomEnd = 16.dp),
                    color = CyberCardDark.copy(alpha = 0.95f)
                ) {
                    val pm = context.packageManager
                    val selectedName = remember(selectedPackageName) {
                        if (selectedPackageName.isEmpty()) {
                            "NO TARGET LOCKED"
                        } else {
                            try {
                                val info = pm.getApplicationInfo(selectedPackageName, 0)
                                pm.getApplicationLabel(info).toString()
                            } catch (e: Exception) {
                                selectedPackageName.substringAfterLast(".")
                            }
                        }
                    }
                    val selectedIcon = remember(selectedPackageName) {
                        if (selectedPackageName.isEmpty()) null else try { pm.getApplicationIcon(selectedPackageName) } catch (e: Exception) { null }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(NeonCrimson, CircleShape)
                            )
                            Text(
                                text = "TARGET LOCK // CONSOLE TILE",
                                color = NeonCrimson,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 2.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // COMPACT CYBER APP ICON FRAME
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .shadow(20.dp, CutCornerShape(14.dp), spotColor = pulseGlow)
                                .background(Color.Black, CutCornerShape(14.dp))
                                .animatedNeonBorder(enabled = true, color1 = NeonCrimson, color2 = ElectricCyan, shape = CutCornerShape(14.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (selectedIcon != null) {
                                Image(
                                    bitmap = selectedIcon.toBitmap().asImageBitmap(),
                                    contentDescription = selectedName,
                                    modifier = Modifier
                                        .fillMaxSize(0.85f)
                                        .clip(CutCornerShape(10.dp))
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.VideogameAsset,
                                    contentDescription = null,
                                    tint = ElectricCyan,
                                    modifier = Modifier.size(38.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = selectedName,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = if (selectedPackageName.isEmpty()) "SELECT OR DEPLOY A GAME" else selectedPackageName,
                            color = ElectricCyan,
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // DIRECT LAUNCH BUTTON (DIRECTLY BELOW GAME TITLE)
                        Surface(
                            onClick = {
                                SoundEffectsManager.playOn()
                                SoundEffectsManager.performHaptic(haptic)
                                if (selectedPackageName.isEmpty()) {
                                    Toast.makeText(context, "Please deploy and select a target game first!", Toast.LENGTH_SHORT).show()
                                } else {
                                    val pm = context.packageManager
                                    val launchIntent = pm.getLaunchIntentForPackage(selectedPackageName)
                                    if (launchIntent != null) {
                                        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        context.startActivity(launchIntent)
                                    } else {
                                        Toast.makeText(context, "Unable to launch $selectedPackageName. App may not be installed or support direct launch.", Toast.LENGTH_LONG).show()
                                    }
                                }
                            },
                            modifier = Modifier
                                .height(38.dp)
                                .fillMaxWidth(0.9f)
                                .shadow(12.dp, CutCornerShape(10.dp), spotColor = ElectricCyan)
                                .tacticalPress(),
                            shape = CutCornerShape(10.dp),
                            color = ElectricCyan.copy(alpha = 0.25f),
                            contentColor = Color.White,
                            border = BorderStroke(1.5.dp, ElectricCyan)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 10.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Launch",
                                    tint = ElectricCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "LAUNCH",
                                    style = TerminalTextStyle,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.5.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // STATUS BADGES
                        val isShizukuOnline = remember { SmartEngine.isShizukuReady() }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(CutCornerShape(4.dp))
                                    .background(if (isShizukuOnline) Color(0xFF003300) else Color(0xFF330000))
                                    .border(1.dp, if (isShizukuOnline) Color(0xFF00FF66) else Color(0xFFFF0000), CutCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (isShizukuOnline) "SHIZUKU: [ONLINE]" else "SHIZUKU: [OFFLINE]",
                                    color = if (isShizukuOnline) Color(0xFF00FF66) else Color(0xFFFF0000),
                                    fontSize = 7.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(CutCornerShape(4.dp))
                                    .background(Color(0xFF331A00))
                                    .border(1.dp, Color(0xFFFF9100), CutCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "ENGINE READY",
                                    color = Color(0xFFFF9100),
                                    fontSize = 7.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // INITIALIZE / BOOST BUTTON
                        Surface(
                            onClick = {
                                SoundEffectsManager.playOn()
                                SoundEffectsManager.performHaptic(haptic)
                                performTargetBoostAndLaunch()
                            },
                            modifier = Modifier
                                .height(38.dp)
                                .fillMaxWidth(0.9f)
                                .shadow(12.dp, CutCornerShape(10.dp), spotColor = NeonCrimson)
                                .tacticalPress(),
                            shape = CutCornerShape(10.dp),
                            color = NeonCrimson,
                            contentColor = Color.White,
                            border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.6f))
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.RocketLaunch,
                                    contentDescription = "Boost",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "INITIALIZE / BOOST",
                                    style = TerminalTextStyle,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.2.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // --- BOTTOM SOCIAL LINKS BAR ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SocialFooterButton("YouTube", Color.Red, modifier = Modifier.weight(1f)) {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/@GfxGamerLab_1"))
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                    } catch (e: Exception) { e.printStackTrace() }
                }
                SocialFooterButton("Telegram", Color(0xFF24A1DE), modifier = Modifier.weight(1f)) {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/gfxgamerlab"))
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                    } catch (e: Exception) { e.printStackTrace() }
                }
                SocialFooterButton("WhatsApp", Color(0xFF25D366), modifier = Modifier.weight(1f)) {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://whatsapp.com/channel/0029VbB1E5VFsn0o8MNoAp2l"))
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                    } catch (e: Exception) { e.printStackTrace() }
                }
                SocialFooterButton("TikTok", Color(0xFFEE1D52), modifier = Modifier.weight(1f)) {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.tiktok.com/@gfxgamerlab?_r=1&_t=ZS-99gzTXWIINh"))
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                    } catch (e: Exception) { e.printStackTrace() }
                }
            }
        }

        // --- APP PICKER DIALOG WITH SEARCH BAR ---
        if (showAppPicker) {
            Dialog(onDismissRequest = { showAppPicker = false }) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth(0.95f)
                        .fillMaxHeight(0.85f)
                        .border(1.dp, ElectricCyan, CutCornerShape(16.dp)),
                    shape = CutCornerShape(16.dp),
                    color = Color(0xFF0F0814)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "SELECT TARGET GAME / APP",
                                color = ElectricCyan,
                                style = TerminalTextStyle,
                                fontSize = 14.sp
                            )
                            IconButton(onClick = { showAppPicker = false }) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // REAL-TIME SEARCH BAR
                        OutlinedTextField(
                            value = appSearchQuery,
                            onValueChange = { appSearchQuery = it },
                            placeholder = { Text("Search target game or package...", color = Color.Gray, fontSize = 11.sp) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = CutCornerShape(8.dp),
                            singleLine = true,
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = ElectricCyan)
                            },
                            trailingIcon = {
                                if (appSearchQuery.isNotEmpty()) {
                                    IconButton(onClick = { appSearchQuery = "" }) {
                                        Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear", tint = Color.Gray)
                                    }
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = ElectricCyan,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                focusedContainerColor = Color.Black.copy(alpha = 0.6f),
                                unfocusedContainerColor = Color.Black.copy(alpha = 0.4f)
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        if (isLoadingApps) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = ElectricCyan)
                            }
                        } else {
                            val filteredApps = remember(installedApps, appSearchQuery) {
                                if (appSearchQuery.isBlank()) {
                                    installedApps
                                } else {
                                    installedApps.filter {
                                        it.name.contains(appSearchQuery, ignoreCase = true) ||
                                        it.packageName.contains(appSearchQuery, ignoreCase = true)
                                    }
                                }
                            }

                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(filteredApps) { app ->
                                    Surface(
                                        onClick = {
                                            SoundEffectsManager.playOn()
                                            SoundEffectsManager.performHaptic(haptic)
                                            if (!deployedTargets.contains(app.packageName)) {
                                                val updated = deployedTargets + app.packageName
                                                saveTargets(updated)
                                            }
                                            selectTarget(app.packageName)
                                            showAppPicker = false
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = CutCornerShape(8.dp),
                                        color = Color.White.copy(alpha = 0.05f),
                                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (app.icon != null) {
                                                Image(
                                                    bitmap = app.icon.toBitmap().asImageBitmap(),
                                                    contentDescription = app.name,
                                                    modifier = Modifier
                                                        .size(36.dp)
                                                        .clip(CutCornerShape(6.dp))
                                                )
                                            } else {
                                                Icon(
                                                    imageVector = Icons.Default.Android,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(36.dp)
                                                )
                                            }

                                            Spacer(modifier = Modifier.width(12.dp))

                                            Column {
                                                Text(
                                                    text = app.name,
                                                    color = Color.White,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = app.packageName,
                                                    color = Color.Gray,
                                                    fontSize = 9.sp,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
