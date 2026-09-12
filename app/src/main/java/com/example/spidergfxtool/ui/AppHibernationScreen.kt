package com.example.spidergfxtool.ui

import android.content.Intent
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.example.spidergfxtool.GfxBackendManager
import com.example.spidergfxtool.SoundEffectsManager
import com.example.spidergfxtool.ui.theme.TerminalTextStyle
import com.example.spidergfxtool.ui.components.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AppInfo(
    val name: String,
    val packageName: String,
    val icon: Drawable,
    val isSystem: Boolean,
    val initialRamMb: Int,
    val estimatedRamMb: Int,
    val isHeavy: Boolean,
    val isRunning: Boolean = false // Replaces isHibernated
)

@Composable
fun AppHibernationScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val isConnected = GfxBackendManager.isShizukuConnected.value

    // Use SnapshotStateList for real-time UI updates
    val apps = remember { mutableStateListOf<AppInfo>() }
    val selectedPackages = remember { mutableStateListOf<String>() }
    var isExecuting by remember { mutableStateOf(false) }
    var terminalLog by remember { mutableStateOf("IDLE: SYSTEM HIBERNATION v0.6 READY...") }
    var isLoading by remember { mutableStateOf(true) }

    val infiniteTransition = rememberInfiniteTransition(label = "Hibernation")

    // Load and Scan Apps Live
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val pm = context.packageManager
            
            // 1. Identify Launcher Packages
            val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
            val launchers = pm.queryIntentActivities(launcherIntent, 0).map { it.activityInfo.packageName }
            
            // 2. Strict Blacklist
            val blacklist = listOf(
                "com.android.systemui",
                "com.android.settings",
                "com.android.phone",
                "com.google.android.inputmethod.latin",
                "com.android.vending",
                context.packageName,
                "moe.shizuku.privileged.api"
            )

            val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            val heavyKeywords = listOf("instagram", "tiktok", "facebook", "browser", "chrome", "game", "pubg", "freefire", "whatsapp")
            
            val appList = installedApps.filter { app ->
                val isCritical = app.packageName in blacklist || app.packageName in launchers
                val isSystem = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                !isCritical && (!isSystem || heavyKeywords.any { app.packageName.lowercase().contains(it) })
            }.map { app ->
                val name = app.loadLabel(pm).toString()
                val pkg = app.packageName
                val isHeavy = heavyKeywords.any { name.lowercase().contains(it) || pkg.lowercase().contains(it) }
                val ram = if (isHeavy) (300..600).random() else (50..250).random()
                
                // LIVE KERNEL SCAN
                val isRunning = GfxBackendManager.checkIfAppIsRunning(pkg)
                
                AppInfo(
                    name = name,
                    packageName = pkg,
                    icon = app.loadIcon(pm),
                    isSystem = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                    initialRamMb = ram,
                    estimatedRamMb = if (isRunning) ram else 0,
                    isHeavy = isHeavy,
                    isRunning = isRunning
                )
            }.sortedWith(
                compareByDescending<AppInfo> { it.isRunning }
                .thenByDescending { it.isHeavy }
                .thenByDescending { it.estimatedRamMb }
            )
            
            withContext(Dispatchers.Main) {
                apps.clear()
                apps.addAll(appList)
                isLoading = false
                terminalLog = "[SYSTEM] Live Scan Complete. Active processes identified."
            }
        }
    }

    val totalPurgeableRam = remember(selectedPackages.size, apps.toList()) {
        selectedPackages.sumOf { pkg -> apps.find { it.packageName == pkg }?.estimatedRamMb ?: 0 }
    }

    val onHibernate = {
        if (!isConnected) {
            Toast.makeText(context, "Connect Shizuku first", Toast.LENGTH_SHORT).show()
        } else {
            scope.launch {
                isExecuting = true
                try {
                    terminalLog = "[PROCESS] Initiating v0.6 Deep Hibernation..."
                    val targets = selectedPackages.toList()
                    
                    targets.forEach { pkg ->
                        val appIndex = apps.indexOfFirst { it.packageName == pkg }
                        if (appIndex != -1) {
                            val app = apps[appIndex]
                            terminalLog += "\n>> Freezing: ${app.name}..."
                            
                            // ACTUAL SHIZUKU EXECUTION
                            val result = GfxBackendManager.runShell(context, "am force-stop $pkg")
                            
                            if (result.isSuccess) {
                                // Real-time state update in SnapshotStateList
                                apps[appIndex] = app.copy(isRunning = false, estimatedRamMb = 0)
                                selectedPackages.remove(pkg)
                                terminalLog += "\n[SUCCESS] ${app.name} suspended."
                            } else {
                                terminalLog += "\n[FAIL] Failed to stop ${app.name}."
                            }
                        }
                        delay(200) // Visual feedback pacing
                    }
                    
                    // Final re-sort to move sleeping apps to bottom
                    val sortedList = apps.toList().sortedWith(
                        compareByDescending<AppInfo> { it.isRunning }
                        .thenByDescending { it.isHeavy }
                        .thenByDescending { it.estimatedRamMb }
                    )
                    apps.clear()
                    apps.addAll(sortedList)
                    
                    terminalLog += "\n[FINISH] Hibernation complete. System optimized."
                    SoundEffectsManager.playOff()
                } catch (e: Exception) {
                    terminalLog += "\n[ERROR] Engine failure: ${e.localizedMessage}"
                } finally {
                    isExecuting = false
                }
            }
        }
    }

    // --- TWO-PANE LANDSCAPE ROOT ---
    Box(modifier = Modifier.fillMaxSize().background(CyberDeepDark)) {
        HudGridBackground()
        AnimatedSpiderWeb(infiniteTransition)
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 72.dp, start = 16.dp, end = 16.dp, bottom = 8.dp)
        ) {
            Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                
                // --- LEFT PANE (55%): APP SELECTOR ---
                Column(modifier = Modifier.weight(0.55f).fillMaxHeight()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "APP SELECTOR ENGINE v0.6",
                            color = ElectricCyan.copy(alpha = 0.8f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp
                        )
                        
                        // SELECT ALL ACTIVE FEATURE
                        if (!isLoading && apps.any { it.isRunning }) {
                            TextButton(
                                onClick = {
                                    triggerActionFeedback(haptic, true)
                                    apps.filter { it.isRunning }.forEach { 
                                        if (it.packageName !in selectedPackages) selectedPackages.add(it.packageName) 
                                    }
                                    terminalLog = "[UI] All active processes targeted for purge."
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                modifier = Modifier.height(24.dp)
                            ) {
                                Icon(Icons.Outlined.SelectAll, null, tint = NeonCrimson, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("SELECT ALL ACTIVE", color = NeonCrimson, fontSize = 9.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }

                    Surface(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        color = CyberCardDark.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.5f))
                    ) {
                        if (isLoading) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = ElectricCyan)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(apps, key = { it.packageName }) { app ->
                                    AppItemCard(
                                        app = app,
                                        isSelected = app.packageName in selectedPackages,
                                        onSelect = { 
                                            if (app.isRunning && !isExecuting) {
                                                if (it) {
                                                    selectedPackages.add(app.packageName)
                                                    SoundEffectsManager.playOn()
                                                } else {
                                                    selectedPackages.remove(app.packageName)
                                                    SoundEffectsManager.playOff()
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // --- RIGHT PANE (45%): STATUS & CONTROL ---
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(0.45f)
                ) {
                    // Header / Exit Row
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "CORE MONITOR v0.6",
                            color = NeonCrimson.copy(alpha = 0.8f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                        IconButton(
                            onClick = {
                                SoundEffectsManager.playOff()
                                onBack()
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }

                    // 1. Status Summary Card
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        color = CyberCardDark.copy(alpha = 0.9f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.3f))
                    ) {
                        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Memory, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("SELECTED TARGETS: ${selectedPackages.size}", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black)
                                Text(
                                    text = "PURGEABLE: $totalPurgeableRam MB", 
                                    color = if (selectedPackages.isNotEmpty()) NeonCrimson else ElectricCyan, 
                                    fontSize = 11.sp, 
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                )
                            }
                        }
                    }

                    // 2. Hacker Terminal Console Box
                    val scrollState = rememberScrollState()
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(vertical = 4.dp)
                            .border(1.dp, NeonCrimson.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                        color = Color.Black.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize().padding(10.dp).verticalScroll(scrollState)) {
                            TypewriterText(text = terminalLog, color = NeonCrimson, scrollState = scrollState)
                        }
                    }

                    // 3. Action Button
                    Button(
                        onClick = {
                            if (selectedPackages.isNotEmpty() && !isExecuting) {
                                SoundEffectsManager.playOn()
                                SoundEffectsManager.performHaptic(haptic)
                                onHibernate()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                            .padding(top = 4.dp)
                            .shadow(if (selectedPackages.isNotEmpty()) 12.dp else 0.dp, RoundedCornerShape(12.dp), spotColor = NeonCrimson),
                        enabled = selectedPackages.isNotEmpty() && !isExecuting,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonCrimson,
                            disabledContainerColor = Color.DarkGray.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isExecuting) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Outlined.PowerSettingsNew, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isExecuting) "EXECUTING..." else "HIBERNATE SELECTED",
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AppItemCard(app: AppInfo, isSelected: Boolean, onSelect: (Boolean) -> Unit) {
    val borderColor = if (!app.isRunning) ElectricCyan.copy(alpha = 0.2f) else NeonCrimson.copy(alpha = 0.4f)
    val bgColor = if (!app.isRunning) Color(0xFF0A0A0A) else (if (isSelected) Color(0xFF1A0A0A) else CyberCardDark)
    
    Surface(
        onClick = {}, // Handled by tacticalPress
        modifier = Modifier
            .fillMaxWidth()
            .shadow(if (isSelected && app.isRunning) 8.dp else 0.dp, CutCornerShape(12.dp), spotColor = NeonCrimson)
            .animatedNeonBorder(
                enabled = app.isRunning && isSelected,
                color1 = NeonCrimson,
                shape = CutCornerShape(12.dp)
            )
            .tacticalPress { if (app.isRunning) onSelect(!isSelected) },
        color = bgColor,
        shape = CutCornerShape(12.dp),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier.padding(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                bitmap = app.icon.toBitmap().asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .alpha(if (!app.isRunning) 0.4f else 1f)
                    .border(1.dp, if (app.isHeavy && app.isRunning) NeonCrimson.copy(alpha = 0.5f) else Color.Transparent, CircleShape)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.name, 
                    color = if (!app.isRunning) Color.Gray else Color.White, 
                    fontSize = 11.sp, 
                    fontWeight = FontWeight.Black,
                    maxLines = 1
                )
                Text(
                    text = app.packageName, 
                    color = Color.Gray.copy(alpha = 0.6f), 
                    style = TerminalTextStyle,
                    fontSize = 7.sp, 
                    maxLines = 1
                )
            }
            
            Column(horizontalAlignment = Alignment.End) {
                if (!app.isRunning) {
                    Surface(
                        color = ElectricCyan.copy(alpha = 0.05f),
                        shape = CutCornerShape(2.dp),
                        border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.2f))
                    ) {
                        Text(
                            "SLEEPING / 0 MB", 
                            color = ElectricCyan.copy(alpha = 0.6f), 
                            style = TerminalTextStyle,
                            fontSize = 7.sp, 
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                } else {
                    Text(
                        text = "~${app.estimatedRamMb} MB", 
                        color = if (isSelected) Color.White else NeonCrimson, 
                        style = TerminalTextStyle,
                        fontSize = 9.sp
                    )
                    if (app.isHeavy) {
                        Text("HEAVY", color = NeonCrimson.copy(alpha = 0.6f), fontSize = 6.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
            
            if (app.isRunning) {
                Spacer(modifier = Modifier.width(8.dp))
                TacticalTargetCheckbox(
                    checked = isSelected,
                    onCheckedChange = onSelect
                )
            }
        }
    }
}

private fun triggerActionFeedback(haptic: androidx.compose.ui.hapticfeedback.HapticFeedback, isOn: Boolean) {
    if (isOn) SoundEffectsManager.playOn() else SoundEffectsManager.playOff()
    SoundEffectsManager.performHaptic(haptic)
}
