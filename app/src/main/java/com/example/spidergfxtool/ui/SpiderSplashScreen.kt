package com.example.spidergfxtool.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.ui.draw.blur
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.example.spidergfxtool.R
import kotlinx.coroutines.delay

@Composable
fun SpiderSplashScreen(onAnimationComplete: () -> Unit) {
    var startAnimation by remember { mutableStateOf(false) }
    val fullText = "SPIDER GFX ENGINE - INITIALIZING CORE..."
    
    val scale by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "LogoScale"
    )

    val alpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(1500),
        label = "ContentAlpha"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "Glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
        label = "GlowAlpha"
    )

    LaunchedEffect(Unit) {
        delay(300)
        startAnimation = true
        delay(2000) // 2 second cinematic delay
        onAnimationComplete()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF050505)),
        contentAlignment = Alignment.Center
    ) {
        WebPatternBackground(Modifier.fillMaxSize())

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.alpha(alpha)
        ) {
            Box(contentAlignment = Alignment.Center) {
                // Outer Neon Glow
                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .scale(scale)
                        .alpha(glowAlpha * 0.5f)
                        .blur(30.dp)
                        .background(Color(0xFFFF3131).copy(alpha = 0.3f), CircleShape)
                )
                
                SpiderLogo(
                    modifier = Modifier.size(220.dp).scale(scale),
                    glowIntensity = glowAlpha
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            Text(
                text = "SPIDER GFX",
                color = Color(0xFFFF3131),
                fontSize = 38.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 12.sp,
                modifier = Modifier.scale(scale)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Subtitle with smooth fade-in
            AnimatedVisibility(
                visible = startAnimation,
                enter = fadeIn(tween(1000, 500))
            ) {
                Text(
                    text = fullText,
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 3.sp,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
            }
            
            Spacer(modifier = Modifier.height(40.dp))
            
            CyberWaveform(
                modifier = Modifier.width(220.dp).height(50.dp),
                color = Color(0xFFFF3131).copy(alpha = alpha)
            )
        }
    }
}

@Composable
fun CyberWaveform(modifier: Modifier, color: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "Waveform")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing), RepeatMode.Restart),
        label = "Phase"
    )

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val centerY = height / 2
        val path = Path()
        
        path.moveTo(0f, centerY)
        for (x in 0..width.toInt() step 5) {
            val relativeX = x / width
            val sine = Math.sin((relativeX * 4 * Math.PI + phase).toDouble()).toFloat()
            val magnitude = Math.sin((relativeX * Math.PI).toDouble()).toFloat() // Envelope
            val y = centerY + sine * (height / 2) * magnitude
            path.lineTo(x.toFloat(), y)
        }
        
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 4f)
        )
    }
}

@Composable
fun SpiderLogo(modifier: Modifier = Modifier, glowIntensity: Float) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Pulsating Neon Ring
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = Color(0xFFFF3131).copy(alpha = 0.2f * glowIntensity),
                radius = size.minDimension / 2,
                style = Stroke(width = 8f)
            )
            drawCircle(
                color = Color(0xFFFF3131).copy(alpha = 0.5f * glowIntensity),
                radius = size.minDimension / 2.1f,
                style = Stroke(width = 2f)
            )
        }

        // Inner logo glow
        Box(
            modifier = Modifier
                .fillMaxSize(0.7f)
                .blur(15.dp)
                .background(Color(0xFFFF3131).copy(alpha = 0.3f * glowIntensity), CircleShape)
        )
        
        Image(
            painter = painterResource(id = R.mipmap.ic_launcher_foreground),
            contentDescription = "Spider Logo",
            modifier = Modifier
                .fillMaxSize(0.9f)
                .clip(CircleShape), // Fix: Apply circular clip
            contentScale = ContentScale.Crop // Fix: Fill the circle
        )
    }
}

@Composable
fun WebPatternBackground(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2, size.height / 2)
        val strokeColor = Color.White.copy(alpha = 0.04f)
        for (i in 0 until 12) {
            val angle = (i * 30).toDouble() * Math.PI / 180
            drawLine(strokeColor, center, Offset(center.x + (size.width * Math.cos(angle)).toFloat(), center.y + (size.height * Math.sin(angle)).toFloat()), 1f)
        }
        for (i in 1..8) {
            drawCircle(
                color = strokeColor,
                radius = (size.width / 6) * i,
                center = center,
                style = Stroke(width = 1f)
            )
        }
    }
}

@Preview
@Composable
fun SpiderSplashScreenPreview() {
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        SpiderSplashScreen(onAnimationComplete = {})
    }
}
