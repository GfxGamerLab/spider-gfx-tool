package com.example.spidergfxtool.ui

import android.media.MediaPlayer
import android.net.Uri
import android.view.animation.OvershootInterpolator
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.spidergfxtool.AudioPlayerManager
import com.example.spidergfxtool.R
import com.example.spidergfxtool.SoundEffectsManager
import com.example.spidergfxtool.ui.theme.TerminalTextStyle
import com.example.spidergfxtool.ui.components.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun GfxActivationScreen(onActivated: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isVisible by remember { mutableStateOf(false) }
    
    val infiniteTransition = rememberInfiniteTransition(label = "DashboardGlow")
    
    val glowColor by infiniteTransition.animateColor(
        initialValue = Color(0xFF330000),
        targetValue = Color(0xFFFF3131),
        animationSpec = infiniteRepeatable(tween(2000), RepeatMode.Reverse),
        label = "Glow"
    )

    LaunchedEffect(Unit) {
        isVisible = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0A0A0A), Color(0xFF1A0000), Color.Black)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        HudGridBackground()
        // LAYER 1: Animated Background Pattern
        Box(modifier = Modifier.fillMaxSize()) {
            val spiderMove by infiniteTransition.animateFloat(
                initialValue = 0f, targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing), RepeatMode.Restart),
                label = "SpiderMove"
            )
            repeat(4) { index ->
                CreepingSpider(
                    modifier = Modifier.align(when(index) {
                        0 -> Alignment.TopStart
                        1 -> Alignment.TopEnd
                        2 -> Alignment.BottomStart
                        else -> Alignment.BottomEnd
                    }),
                    progress = spiderMove,
                    index = index
                )
            }
        }

        // LAYER 2: Main Content with Staggered Animations
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // LOGO
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(tween(800)) + scaleIn(tween(800, easing = OvershootInterpolator(1.5f).toEasing()))
            ) {
                CyberHudEmblem(modifier = Modifier.size(110.dp))
            }

            Spacer(modifier = Modifier.height(20.dp))

            // TITLE
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(tween(800, 200)) + slideInVertically(initialOffsetY = { 20 }, animationSpec = tween(800, 200))
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "CORE ACTIVATION", 
                        color = Color.White, 
                        style = TerminalTextStyle,
                        fontSize = 24.sp,
                        letterSpacing = 8.sp
                    )
                    Text(
                        text = "V9.0 STABILITY PROTOCOL", 
                        color = Color(0xFFFF3131).copy(alpha = 0.7f), 
                        style = TerminalTextStyle,
                        fontSize = 10.sp,
                        letterSpacing = 2.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // CONTINUE BUTTON
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(tween(800, 600)) + slideInVertically(initialOffsetY = { 60 }, animationSpec = tween(800, 600))
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .shadow(20.dp, CutCornerShape(16.dp), spotColor = glowColor)
                        .tacticalPress {
                            try {
                                val mp = MediaPlayer.create(context, R.raw.correct_key)
                                mp?.start()
                                mp?.setOnCompletionListener { it.release() }
                            } catch (e: Exception) { /* Fallback if audio fails */ }
                            
                            scope.launch {
                                delay(300)
                                onActivated()
                            }
                        },
                    shape = CutCornerShape(16.dp),
                    color = Color(0xFFFF3131),
                    contentColor = Color.White,
                    tonalElevation = 12.dp
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Text(
                            text = "CONTINUE", 
                            style = TerminalTextStyle,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 4.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            GlobalCommunityFooter(showTutorials = false)
        }
    }
}

// Helper to bridge Interpolator to Compose Easing
private fun android.view.animation.Interpolator.toEasing() = Easing { x -> getInterpolation(x) }

@Composable
fun CreepingSpider(modifier: Modifier, progress: Float, index: Int) {
    val offset = when(index) {
        0 -> Offset(100f * progress, 100f * progress)
        1 -> Offset(-100f * progress, 100f * progress)
        2 -> Offset(100f * progress, -100f * progress)
        else -> Offset(-100f * progress, -100f * progress)
    }
    
    Canvas(
        modifier = modifier
            .size(60.dp)
            .offset(offset.x.dp, offset.y.dp)
            .alpha(0.15f)
    ) {
        drawCircle(Color.Red, radius = 10f)
        repeat(4) { i ->
            drawLine(Color.Red, Offset(0f, 0f), Offset(20f, (i - 1.5f) * 15f), strokeWidth = 2f)
            drawLine(Color.Red, Offset(0f, 0f), Offset(-20f, (i - 1.5f) * 15f), strokeWidth = 2f)
        }
    }
}
