package com.example.spidergfxtool.ui

import android.content.Intent
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
import com.example.spidergfxtool.ui.components.SocialIcon
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val ACCESS_KEY = "GFXGAMERLAB"

@Composable
fun GfxActivationScreen(onActivated: () -> Unit) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    var keyInput by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
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
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .shadow(30.dp, CircleShape, spotColor = Color.Red)
                        .background(
                            Brush.radialGradient(listOf(Color(0xFFFF3131), Color(0xFF660000))), 
                            shape = CircleShape
                        )
                        .border(2.dp, Color.White.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.mipmap.ic_launcher_foreground),
                        contentDescription = "Logo",
                        modifier = Modifier.fillMaxSize(0.8f),
                        contentScale = ContentScale.Fit
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // TITLE
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(tween(800, 200)) + slideInVertically(initialOffsetY = { 20 }, animationSpec = tween(800, 200))
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "CORE ACTIVATION", 
                        color = Color.White, 
                        fontSize = 26.sp, 
                        fontWeight = FontWeight.Black, 
                        letterSpacing = 8.sp
                    )
                    Text(
                        text = "V3.0 STABILITY PROTOCOL", 
                        color = Color(0xFFFF3131).copy(alpha = 0.7f), 
                        fontSize = 11.sp, 
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // INPUT FIELD
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(tween(800, 400)) + slideInVertically(initialOffsetY = { 40 }, animationSpec = tween(800, 400))
            ) {
                OutlinedTextField(
                    value = keyInput,
                    onValueChange = { keyInput = it },
                    label = { Text("PROTOCOL KEY", color = Color.Gray, fontSize = 12.sp) },
                    placeholder = { Text("Enter Encryption Key...", color = Color.DarkGray) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(10.dp, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        val icon = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                        IconButton(onClick = { 
                            SoundEffectsManager.playOn()
                            SoundEffectsManager.performHaptic(haptic)
                            passwordVisible = !passwordVisible 
                        }) {
                            Icon(imageVector = icon, contentDescription = null, tint = Color(0xFFFF3131))
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFFF3131),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                        cursorColor = Color(0xFFFF3131),
                        focusedContainerColor = Color.Black.copy(alpha = 0.6f),
                        unfocusedContainerColor = Color.Black.copy(alpha = 0.4f)
                    )
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // BUTTON
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(tween(800, 600)) + slideInVertically(initialOffsetY = { 60 }, animationSpec = tween(800, 600))
            ) {
                ElevatedButton(
                    onClick = {
                        SoundEffectsManager.playOn()
                        SoundEffectsManager.performHaptic(haptic)
                        // Robust multi-lingual validation logic
                        val cleanedInput = keyInput.trim().filter { !it.isWhitespace() }.uppercase()
                        
                        if (cleanedInput == ACCESS_KEY) {
                            val mp = MediaPlayer.create(context, R.raw.correct_key)
                            mp?.start()
                            mp?.setOnCompletionListener { it.release() }
                            scope.launch {
                                delay(2500)
                                onActivated()
                            }
                        } else {
                            val mp = MediaPlayer.create(context, R.raw.wrong_key)
                            mp?.start()
                            mp?.setOnCompletionListener { it.release() }
                            Toast.makeText(context, "Encryption Key Rejected!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                        .shadow(20.dp, RoundedCornerShape(16.dp), spotColor = glowColor),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.elevatedButtonColors(
                        containerColor = Color(0xFFFF3131),
                        contentColor = Color.White
                    ),
                    elevation = ButtonDefaults.elevatedButtonElevation(defaultElevation = 12.dp)
                ) {
                    Text(
                        text = "INITIALIZE ENGINE", 
                        fontWeight = FontWeight.Black, 
                        letterSpacing = 2.sp,
                        fontSize = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // FOOTER JOIN
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(tween(1000, 1000))
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("join channel for key", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Surface(
                        onClick = {
                            SoundEffectsManager.playOn()
                            SoundEffectsManager.performHaptic(haptic)
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/gfxgamerlab"))
                            context.startActivity(intent)
                        },
                        modifier = Modifier.height(34.dp).width(75.dp),
                        color = Color.White.copy(alpha = 0.05f),
                        border = BorderStroke(1.dp, Color(0xFFFF3131).copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("JOIN", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // SOCIAL ICONS
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(tween(1000, 1200))
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    SocialIcon("YT", "https://youtube.com/@gfxgamerlab_1", Color.Red)
                    SocialIcon("TG", "https://t.me/gfxgamerlab", Color(0xFF24A1DE))
                    SocialIcon("WA", "https://whatsapp.com/channel/0029VbB1E5VFsn0o8MNoAp2l", Color(0xFF25D366))
                }
            }
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
