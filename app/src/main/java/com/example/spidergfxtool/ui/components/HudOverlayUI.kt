package com.example.spidergfxtool.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.spidergfxtool.ui.theme.TerminalTextStyle

@Composable
fun HudOverlayUI(
    fps: Int,
    temp: Float
) {
    Box(
        modifier = Modifier
            .wrapContentSize()
            .padding(8.dp)
            .background(Color.Black.copy(alpha = 0.5f), CutCornerShape(topStart = 8.dp, bottomEnd = 8.dp))
            .border(1.dp, ElectricCyan.copy(alpha = 0.3f), CutCornerShape(topStart = 8.dp, bottomEnd = 8.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Column(horizontalAlignment = Alignment.Start) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "FPS:",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    style = TerminalTextStyle
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "$fps",
                    color = ElectricCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    style = TerminalTextStyle
                )
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "TEMP:",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    style = TerminalTextStyle
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${"%.1f".format(temp)}°C",
                    color = NeonCrimson,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    style = TerminalTextStyle
                )
            }
        }
    }
}
