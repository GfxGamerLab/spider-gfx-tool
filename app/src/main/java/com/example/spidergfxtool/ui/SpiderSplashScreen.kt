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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.delay

@Composable
fun SpiderSplashScreen(onAnimationComplete: () -> Unit) {
    var startAnimation by remember { mutableStateOf(false) }
    
    val scale by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.3f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "LogoScale"
    )

    val alpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(1000),
        label = "ContentAlpha"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "Glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse),
        label = "GlowAlpha"
    )

    LaunchedEffect(Unit) {
        startAnimation = true
        delay(2500)
        onAnimationComplete()
    }

    SpiderSplashScreenContent(scale, alpha, glowAlpha)
}

@Composable
fun SpiderSplashScreenContent(scale: Float, alpha: Float, glowAlpha: Float) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0A)),
        contentAlignment = Alignment.Center
    ) {
        WebPatternBackground(Modifier.fillMaxSize())

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.alpha(alpha)
        ) {
            SpiderLogo(
                modifier = Modifier.size(200.dp).scale(scale),
                glowIntensity = glowAlpha
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "SPIDER GFX",
                color = Color(0xFFFF3131),
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 8.sp,
                modifier = Modifier.scale(scale)
            )
        }
    }
}

@Composable
fun SpiderLogo(modifier: Modifier = Modifier, glowIntensity: Float) {
    val spiderColor = Color(0xFF8B0000)
    val neonColor = Color(0xFFFF3131)

    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2, size.height / 2)
        val bodyWidth = size.width * 0.3f
        val bodyHeight = size.height * 0.4f

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(neonColor.copy(alpha = 0.4f * glowIntensity), Color.Transparent),
                center = center,
                radius = size.width / 1.2f
            )
        )

        val legPath = Path()
        val legOffsets = listOf(
            Offset(-0.2f, -0.4f) to Offset(-0.8f, -0.8f),
            Offset(0.2f, -0.4f) to Offset(0.8f, -0.8f),
            Offset(-0.3f, -0.1f) to Offset(-0.9f, -0.3f),
            Offset(0.3f, -0.1f) to Offset(0.9f, -0.3f),
            Offset(-0.3f, 0.1f) to Offset(-0.9f, 0.3f),
            Offset(0.3f, 0.1f) to Offset(0.9f, 0.3f),
            Offset(-0.2f, 0.4f) to Offset(-0.7f, 0.9f),
            Offset(0.2f, 0.4f) to Offset(0.7f, 0.9f)
        )

        legOffsets.forEach { (start, end) ->
            legPath.moveTo(center.x + (start.x * size.width / 2), center.y + (start.y * size.height / 2))
            legPath.quadraticTo(
                center.x + (end.x * size.width / 2), center.y + (start.y * size.height / 2),
                center.x + (end.x * size.width / 2), center.y + (end.y * size.height / 2)
            )
        }

        drawPath(path = legPath, color = spiderColor, style = Stroke(width = 10f))
        drawOval(spiderColor, Offset(center.x - bodyWidth / 3, center.y - bodyHeight / 2), androidx.compose.ui.geometry.Size(bodyWidth / 1.5f, bodyHeight / 2.5f))
        drawOval(spiderColor, Offset(center.x - bodyWidth / 2, center.y - bodyHeight / 8), androidx.compose.ui.geometry.Size(bodyWidth, bodyHeight / 1.5f))
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
    SpiderSplashScreenContent(scale = 1f, alpha = 1f, glowAlpha = 1f)
}
