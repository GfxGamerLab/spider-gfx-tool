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
import com.example.spidergfxtool.ui.SpiderSplashScreen
import com.example.spidergfxtool.ui.theme.SpiderGFXToolTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // SAFE INIT: Prevents crash if Shizuku binder is unreachable on certain devices
        try {
            GfxBackendManager.initShizukuListener(this)
        } catch (e: Exception) {
            Toast.makeText(this, "Shizuku initialization failed", Toast.LENGTH_LONG).show()
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
        } catch (e: Exception) {
            // Ignore destruction errors
        }
    }
}

@Composable
fun AppNavigation() {
    var showSplash by remember { mutableStateOf(true) }
    var isActivated by remember { mutableStateOf(false) }

    Crossfade(
        targetState = if (showSplash) "splash" else if (!isActivated) "activation" else "dashboard",
        animationSpec = tween(durationMillis = 800),
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
                GfxDashboardScreen()
            }
        }
    }
}
