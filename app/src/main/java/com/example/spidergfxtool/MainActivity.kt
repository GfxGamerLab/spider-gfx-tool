package com.example.spidergfxtool

import android.os.Bundle
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
        
        GfxBackendManager.initShizukuListener(this)
        AudioPlayerManager.playIntroMusic(this)

        setContent {
            LaunchedEffect(Unit) {
                GfxBackendManager.checkAndRefreshStatus()
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
        GfxBackendManager.checkAndRefreshStatus()
    }

    override fun onDestroy() {
        super.onDestroy()
        GfxBackendManager.destroy()
        AudioPlayerManager.stopAndRelease()
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
