package com.example.spidergfxtool.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.example.spidergfxtool.R
import kotlinx.coroutines.delay

@Composable
fun SpiderSplashScreen(onAnimationComplete: () -> Unit) {
    var startAnimation by remember { mutableStateOf(false) }
    var displayedText by remember { mutableStateOf("") }
    val fullText = "INITIALIZING CYBER CORE..."
    
    val scale by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.5f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
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
        startAnimation = true
        // Typewriter effect
        fullText.forEachIndexed { index, _ ->
            displayedText = fullText.substring(0, index + 1)
            delay(80)
        }
        delay(1500)
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
            SpiderLogo(
                modifier = Modifier.size(220.dp).scale(scale),
                glowIntensity = glowAlpha
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "SPIDER GFX",
                color = Color(0xFFFF3131),
                fontSize = 36.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 10.sp,
                modifier = Modifier.scale(scale)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = displayedText,
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )
            
            Spacer(modifier = Modifier.height(40.dp))
            
            CyberWaveform(
                modifier = Modifier.width(200.dp).height(40.dp),
                color = Color(0xFFFF3131)
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
            style = Stroke(width = 3f)
        )
        
        // Add some glowing dots
        repeat(5) { i ->
            val dotX = (width / 6) * (i + 1)
            val dotSine = Math.sin((dotX / width * 4 * Math.PI + phase).toDouble()).toFloat()
            val dotMagnitude = Math.sin((dotX / width * Math.PI).toDouble()).toFloat()
            val dotY = centerY + dotSine * (height / 2) * dotMagnitude
            drawCircle(color, radius = 4f, center = Offset(dotX, dotY))
        }
    }
}

@Composable
fun SpiderLogo(modifier: Modifier = Modifier, glowIntensity: Float) {
    Box(
        modifier = modifier.graphicsLayer {
            alpha = 0.6f + (0.4f * glowIntensity)
        },
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.mipmap.ic_launcher_foreground),
            contentDescription = "Spider Logo",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
fun WebPatternBackground(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2, size.height / 2)
        val strokeColor = Color.White.copy(alpha = 0.05f)
        for (i in 0 until 12) {
            val angle = (i * 30).toDouble() * Math.PI / 180
            drawLine(strokeColor, center, Offset(center.x + (size.width * Math.cos(angle)).toFloat(), center.y + (size.height * Math.sin(angle)).toFloat()), 1f)
        }
        for (i in 1..6) {
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
