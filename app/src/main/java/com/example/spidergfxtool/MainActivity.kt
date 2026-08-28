package com.example.spidergfxtool

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.material3.Scaffold
import com.example.spidergfxtool.ui.GfxActivationScreen
import com.example.spidergfxtool.ui.GfxDashboardScreen
import com.example.spidergfxtool.ui.GfxExtremeHudScreen
import com.example.spidergfxtool.ui.GfxExtraFeaturesScreen
import com.example.spidergfxtool.ui.AppHibernationScreen
import com.example.spidergfxtool.ui.SpiderSplashScreen
import com.example.spidergfxtool.ui.components.CyberDrawerOverlay
import com.example.spidergfxtool.ui.theme.SpiderGFXToolTheme
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import rikka.shizuku.Shizuku
import kotlinx.coroutines.*

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // --- IMMERSIVE FULLSCREEN MODE (GAME STYLE) ---
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        // --- SAFETY BOOT CHECK: RESET SYSTEM STATES ---
        // Ensures the device clears any "ghost" states (Battery, ZRAM, etc.) on app launch
        CoroutineScope(Dispatchers.IO).launch {
            GfxBackendManager.runMasterReset(this@MainActivity)
        }

        // Ads Initialization
        AdsManager.init(this)
        AdsManager.loadInterstitial(this)

        val prefs = getSharedPreferences("spider_gfx_prefs", android.content.Context.MODE_PRIVATE)
        SoundEffectsManager.isFeedbackEnabled = prefs.getBoolean("key_mech_feedback", true)

        // SAFE INIT: Prevents crash if Shizuku binder is unreachable on certain devices
        try {
            GfxBackendManager.initShizukuListener(this)
            SoundEffectsManager.init(this)
        } catch (e: Exception) {
            Toast.makeText(this, "Initialization failed", Toast.LENGTH_LONG).show()
        }
        
        AudioPlayerManager.playIntroMusic(this)

        setContent {
            // Check status on launch and show Toast if disconnected instead of crashing
            LaunchedEffect(Unit) {
                try {
                    if (!GfxBackendManager.checkAndRefreshStatus()) {
                        Toast.makeText(this@MainActivity, "Shizuku not running. Please open Shizuku app.", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@MainActivity, "Shizuku Connection Error", Toast.LENGTH_SHORT).show()
                }
            }

            SpiderGFXToolTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Safely refresh status when app returns to foreground
        try {
            GfxBackendManager.checkAndRefreshStatus()
        } catch (e: Exception) {
            // Silently handle background status refresh errors
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Ensure resources are released safely
        try {
            // FORCE BATTERY RESET ON SHUTDOWN
            CoroutineScope(Dispatchers.IO).launch {
                GfxBackendManager.runShell(null, "dumpsys battery reset")
                GfxBackendManager.globalReset() // AUTO-RESET ON APP CLOSE
                GfxBackendManager.resetOverclockingSafely(this@MainActivity) // REVERT EXPERIMENTAL TWEAKS
                GfxBackendManager.destroy()
            }
            AudioPlayerManager.stopAndRelease()
            SoundEffectsManager.release()
        } catch (e: Exception) {
            // Ignore destruction errors
        }
    }
}

enum class DashboardPage { GFX_TOOL, SYSTEM_ENGINE, EXTRA_FEATURES, HIBERNATION }

@Composable
fun AppNavigation() {
    var showSplash by remember { mutableStateOf(true) }
    var isActivated by remember { mutableStateOf(false) }
    var isDrawerOpen by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Scaffold(
        bottomBar = {
            if (!showSplash) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.Black,
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                ) {
                    AdsManager.BannerAdView()
                }
            }
        },
        containerColor = Color.Transparent
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Crossfade(
                targetState = if (showSplash) "splash" else if (!isActivated) "activation" else "dashboard",
                animationSpec = tween(durationMillis = 1000),
                label = "ScreenTransition"
            ) { screenState ->
                when (screenState) {
                    "splash" -> {
                        SpiderSplashScreen(onAnimationComplete = {
                            showSplash = false
                        })
                    }
                    "activation" -> {
                        GfxActivationScreen(onActivated = {
                            isActivated = true
                        })
                    }
                    "dashboard" -> {
                        DashboardNavigationWrapper()
                    }
                }
            }

            // Floating Hamburger Icon (Global)
            if (!showSplash) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .zIndex(150f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CyberResetIcon(
                        onClick = {
                            val scope = CoroutineScope(Dispatchers.Main)
                            scope.launch {
                                resetAllTweaks(context)
                            }
                        }
                    )
                    
                    CyberHamburgerIcon(
                        onClick = { isDrawerOpen = true }
                    )
                }
            }

            // Global Cyber Drawer
            CyberDrawerOverlay(
                isOpen = isDrawerOpen,
                onClose = { isDrawerOpen = false }
            )
        }
    }
}

@Composable
fun CyberHamburgerIcon(modifier: Modifier = Modifier, onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    Surface(
        onClick = {
            SoundEffectsManager.playOn()
            SoundEffectsManager.performHaptic(haptic)
            onClick()
        },
        modifier = modifier
            .size(45.dp)
            .shadow(10.dp, CutCornerShape(8.dp), spotColor = Color(0xFFFF1744)),
        color = Color.Black.copy(alpha = 0.7f),
        shape = CutCornerShape(8.dp),
        border = BorderStroke(1.dp, Color(0xFFFF1744).copy(alpha = 0.5f))
    ) {
        Box(contentAlignment = Alignment.Center) {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                repeat(3) {
                    Box(modifier = Modifier.size(width = 20.dp, height = 2.dp).background(Color.White))
                }
            }
        }
    }
}

@Composable
fun DashboardNavigationWrapper() {
    var currentPage by remember { mutableStateOf(DashboardPage.SYSTEM_ENGINE) }

    Box(modifier = Modifier.fillMaxSize()) {
        // TOP NAVIGATION SWITCHER (Overlaid)
        CyberNavigationSwitcher(
            currentPage = currentPage,
            onPageSelected = { currentPage = it },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp)
                .zIndex(10f)
        )

        Crossfade(
            targetState = currentPage,
            animationSpec = tween(durationMillis = 500),
            label = "DashboardTransition"
        ) { page ->
            when (page) {
                DashboardPage.GFX_TOOL -> GfxDashboardScreen()
                DashboardPage.SYSTEM_ENGINE -> GfxExtremeHudScreen()
                DashboardPage.EXTRA_FEATURES -> GfxExtraFeaturesScreen()
                DashboardPage.HIBERNATION -> AppHibernationScreen { 
                    currentPage = DashboardPage.SYSTEM_ENGINE 
                }
            }
        }
    }
}

@Composable
fun CyberNavigationSwitcher(
    currentPage: DashboardPage,
    onPageSelected: (DashboardPage) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Color.Black.copy(alpha = 0.8f))
            .border(1.dp, Color(0xFFFF1744).copy(alpha = 0.5f), RoundedCornerShape(24.dp))
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SwitcherItem(
            text = "GFX TOOL",
            isSelected = currentPage == DashboardPage.GFX_TOOL,
            onClick = { onPageSelected(DashboardPage.GFX_TOOL) }
        )
        SwitcherItem(
            text = "SYSTEM ENGINE",
            isSelected = currentPage == DashboardPage.SYSTEM_ENGINE,
            onClick = { onPageSelected(DashboardPage.SYSTEM_ENGINE) }
        )
        SwitcherItem(
            text = "EXTRA FEATURES",
            isSelected = currentPage == DashboardPage.EXTRA_FEATURES,
            onClick = { onPageSelected(DashboardPage.EXTRA_FEATURES) }
        )
        SwitcherItem(
            text = "HIBERNATION",
            isSelected = currentPage == DashboardPage.HIBERNATION,
            onClick = { onPageSelected(DashboardPage.HIBERNATION) }
        )
    }
}

@Composable
fun SwitcherItem(text: String, isSelected: Boolean, onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (isSelected) Color(0xFFFF1744) else Color.Transparent)
            .clickable {
                SoundEffectsManager.playOn()
                SoundEffectsManager.performHaptic(haptic)
                onClick()
            }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (isSelected) Color.White else Color.Gray,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp
        )
    }
}

@Composable
fun CyberResetIcon(modifier: Modifier = Modifier, onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    Surface(
        onClick = {
            SoundEffectsManager.playOn()
            SoundEffectsManager.performHaptic(haptic)
            onClick()
        },
        modifier = modifier
            .size(45.dp)
            .shadow(10.dp, CutCornerShape(8.dp), spotColor = Color(0xFF00E5FF)),
        color = Color.Black.copy(alpha = 0.7f),
        shape = CutCornerShape(8.dp),
        border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.5f))
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Reset",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

suspend fun resetAllTweaks(context: android.content.Context) {
    val prefs = context.getSharedPreferences("spider_gfx_prefs", android.content.Context.MODE_PRIVATE)
    prefs.edit().clear().apply()
    
    // Play Welcome Sound
    try {
        android.media.MediaPlayer.create(context, R.raw.spider_intro).apply {
            setVolume(1.0f, 1.0f)
            start()
            setOnCompletionListener { release() }
        }
    } catch (e: Exception) {
        // Silently fail if audio issues
    }
    
    // Run Master Shizuku Reset
    GfxBackendManager.runMasterReset(context)
    
    // Show Toast
    android.widget.Toast.makeText(context, "All settings reset to default!", android.widget.Toast.LENGTH_SHORT).show()
}
