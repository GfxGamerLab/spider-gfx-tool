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
import com.example.spidergfxtool.ui.GfxActivationScreen
import com.example.spidergfxtool.ui.GfxDashboardScreen
import com.example.spidergfxtool.ui.GfxExtremeHudScreen
import com.example.spidergfxtool.ui.SpiderSplashScreen
import com.example.spidergfxtool.ui.theme.SpiderGFXToolTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
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
            GfxBackendManager.destroy()
            AudioPlayerManager.stopAndRelease()
            SoundEffectsManager.release()
        } catch (e: Exception) {
            // Ignore destruction errors
        }
    }
}

enum class DashboardPage { SYSTEM_ENGINE, EXTREME_HUD }

@Composable
fun AppNavigation() {
    var showSplash by remember { mutableStateOf(true) }
    var isActivated by remember { mutableStateOf(false) }

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
                DashboardPage.SYSTEM_ENGINE -> GfxDashboardScreen()
                DashboardPage.EXTREME_HUD -> GfxExtremeHudScreen()
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
            .border(1.dp, Color(0xFFFF3131).copy(alpha = 0.5f), RoundedCornerShape(24.dp))
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SwitcherItem(
            text = "SYSTEM ENGINE",
            isSelected = currentPage == DashboardPage.SYSTEM_ENGINE,
            onClick = { onPageSelected(DashboardPage.SYSTEM_ENGINE) }
        )
        SwitcherItem(
            text = "EXTREME HUD",
            isSelected = currentPage == DashboardPage.EXTREME_HUD,
            onClick = { onPageSelected(DashboardPage.EXTREME_HUD) }
        )
    }
}

@Composable
fun SwitcherItem(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (isSelected) Color(0xFFFF3131) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (isSelected) Color.White else Color.Gray,
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp
        )
    }
}
