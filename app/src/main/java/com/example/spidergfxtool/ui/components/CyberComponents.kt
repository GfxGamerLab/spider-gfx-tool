package com.example.spidergfxtool.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.geometry.Offset
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import com.example.spidergfxtool.SoundEffectsManager

@Composable
fun CyberTweakCard(
    title: String,
    icon: String,
    statusHex: String,
    content: @Composable () -> Unit
) {
    val neonRed = remember { Color(0xFFFF3131) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, neonRed.copy(alpha = 0.2f), CutCornerShape(topStart = 12.dp, bottomEnd = 12.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.4f)),
        shape = CutCornerShape(topStart = 12.dp, bottomEnd = 12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(icon, fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    )
                }
                Text(
                    text = "ID: $statusHex",
                    color = neonRed.copy(alpha = 0.4f),
                    fontSize = 9.sp,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
            }
            
            val dividerBrush = remember { Brush.horizontalGradient(listOf(Color(0xFFFF3131).copy(alpha = 0.3f), Color.Transparent)) }
            Box(modifier = Modifier.padding(vertical = 12.dp).fillMaxWidth().height(1.dp).background(dividerBrush))
            
            content()
            
            Row(modifier = Modifier.padding(top = 12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Text("SECURE ENGINE PROTOCOL v2.0", color = Color.White.copy(alpha = 0.1f), fontSize = 8.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun AnimatedSpiderWeb(infiniteTransition: InfiniteTransition) {
    val webAlphaState = infiniteTransition.animateFloat(
        initialValue = 0.05f,
        targetValue = 0.15f,
        animationSpec = infiniteRepeatable(tween(3000), RepeatMode.Reverse),
        label = "WebPulse"
    )

    Canvas(modifier = Modifier.fillMaxSize().graphicsLayer { alpha = webAlphaState.value }) {
        val center = Offset(size.width / 2, size.height / 2)
        val webColor = Color(0xFFFF3131)
        
        for (i in 0 until 12) {
            val angle = (i * 30).toDouble() * Math.PI / 180
            drawLine(
                color = webColor,
                start = center,
                end = Offset(
                    center.x + (size.width * Math.cos(angle)).toFloat(),
                    center.y + (size.height * Math.sin(angle)).toFloat()
                ),
                strokeWidth = 1f
            )
        }
        
        for (i in 1..8) {
            val radius = (size.width / 6) * i
            val path = Path()
            for (j in 0 until 12) {
                val angle = (j * 30).toDouble() * Math.PI / 180
                val x = center.x + (radius * Math.cos(angle)).toFloat()
                val y = center.y + (radius * Math.sin(angle)).toFloat()
                if (j == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()
            drawPath(path, webColor, style = Stroke(width = 1f))
        }
    }
}

@Composable
fun CyberTweakChip(text: String, isSelected: Boolean, onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    Box(
        modifier = Modifier
            .clip(CutCornerShape(topStart = 4.dp, bottomEnd = 4.dp))
            .background(if (isSelected) Color(0xFF330000) else Color.Transparent)
            .border(1.dp, if (isSelected) Color(0xFFFF3131) else Color.DarkGray, CutCornerShape(4.dp))
            .clickable {
                SoundEffectsManager.playOn()
                SoundEffectsManager.performHaptic(haptic)
                onClick()
            }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (isSelected) Color.White else Color.Gray,
            fontWeight = FontWeight.Black,
            fontSize = 11.sp
        )
    }
}

@Composable
fun SocialIcon(name: String, url: String, color: Color) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    Surface(
        onClick = { 
            SoundEffectsManager.playOn()
            SoundEffectsManager.performHaptic(haptic)
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) 
        },
        modifier = Modifier
            .padding(vertical = 4.dp)
            .clip(CutCornerShape(topStart = 4.dp, bottomEnd = 4.dp)),
        color = Color.Black.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.5f)),
        shape = CutCornerShape(topStart = 4.dp, bottomEnd = 4.dp)
    ) {
        Text(
            text = name,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp
        )
    }
}
