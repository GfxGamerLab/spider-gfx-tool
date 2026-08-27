package com.example.spidergfxtool.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.example.spidergfxtool.SoundEffectsManager
import kotlinx.coroutines.delay

// --- Cyberpunk Theme Colors ---
val CyberDeepDark = Color(0xFF060308)
val CyberCardDark = Color(0xFF120808)
val ElectricCyan = Color(0xFF00E5FF)
val NeonCrimson = Color(0xFFFF1744)

@Composable
fun CyberTweakCard(
    title: String,
    icon: ImageVector,
    statusHex: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, NeonCrimson.copy(alpha = 0.2f), CutCornerShape(topStart = 12.dp, bottomEnd = 12.dp)),
        colors = CardDefaults.cardColors(containerColor = CyberCardDark.copy(alpha = 0.8f)),
        shape = CutCornerShape(topStart = 12.dp, bottomEnd = 12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp).shadow(4.dp, spotColor = Color.White)
                    )
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
                    color = NeonCrimson.copy(alpha = 0.4f),
                    fontSize = 9.sp,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
            }
            
            val dividerBrush = remember { Brush.horizontalGradient(listOf(NeonCrimson.copy(alpha = 0.3f), Color.Transparent)) }
            Box(modifier = Modifier.padding(vertical = 12.dp).fillMaxWidth().height(1.dp).background(dividerBrush))
            
            content()
            
            Row(modifier = Modifier.padding(top = 12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Text("SECURE ENGINE PROTOCOL v0.5", color = Color.White.copy(alpha = 0.1f), fontSize = 8.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * Premium Cyberpunk UI Toggle with Neon Crimson/Cyan glow and sleek design.
 */
@Composable
fun CyberpunkToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    activeColor: Color = ElectricCyan,
    isLoading: Boolean = false
) {
    val transition = updateTransition(targetState = checked, label = "Toggle")
    val thumbOffset by transition.animateDp(
        transitionSpec = { spring(stiffness = Spring.StiffnessMediumLow) },
        label = "Offset"
    ) { if (it) 22.dp else 0.dp }

    val trackColor by transition.animateColor(label = "Color") { 
        if (it) activeColor.copy(alpha = 0.15f) else Color.DarkGray.copy(alpha = 0.2f) 
    }

    Box(
        modifier = Modifier
            .width(48.dp)
            .height(24.dp)
            .clip(CutCornerShape(topStart = 6.dp, bottomEnd = 6.dp))
            .background(trackColor)
            .border(1.dp, if (checked) activeColor.copy(alpha = 0.8f) else Color.Gray.copy(alpha = 0.3f), CutCornerShape(topStart = 6.dp, bottomEnd = 6.dp))
            .clickable(enabled = !isLoading) { onCheckedChange(!checked) }
            .padding(3.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(18.dp)
                    .padding(2.dp),
                color = activeColor,
                strokeWidth = 2.dp
            )
        } else {
            Box(
                modifier = Modifier
                    .offset(x = thumbOffset)
                    .size(18.dp)
                    .shadow(if (checked) 6.dp else 0.dp, spotColor = activeColor)
                    .background(if (checked) activeColor else Color.Gray, CutCornerShape(topStart = 4.dp, bottomEnd = 4.dp))
            )
        }
    }
}

/**
 * Animated Typewriter Text for Hacker Console effect.
 */
@Composable
fun TypewriterText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Red,
    scrollState: ScrollState? = null
) {
    var displayedText by remember { mutableStateOf("") }
    var cursorVisible by remember { mutableStateOf(true) }

    LaunchedEffect(text) {
        if (text.length >= 150) {
            displayedText = text
        } else {
            displayedText = ""
            text.forEach { char ->
                displayedText += char
                delay(2) 
            }
        }
    }

    LaunchedEffect(displayedText) {
        scrollState?.animateScrollTo(scrollState.maxValue)
    }

    LaunchedEffect(Unit) {
        while (true) {
            cursorVisible = !cursorVisible
            delay(500)
        }
    }

    Text(
        text = displayedText + (if (cursorVisible) "_" else " "),
        color = color,
        fontSize = 10.sp,
        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
        lineHeight = 14.sp,
        modifier = modifier
    )
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
        val webColor = NeonCrimson
        
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
            .border(1.dp, if (isSelected) NeonCrimson else Color.DarkGray, CutCornerShape(4.dp))
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

@Composable
fun GlobalCommunityFooter(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- VIDEO TUTORIALS SECTION ---
        Text(
            text = "VIDEO TUTORIALS",
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 2.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TutorialThumbnailCard(
                modifier = Modifier.weight(1f),
                title = "Tutorial Video in English",
                thumbnailUrl = "https://img.youtube.com/vi/jzFuYRNmG_o/hqdefault.jpg",
                videoUrl = "https://youtu.be/jzFuYRNmG_o?si=OrYVXMlu__wB76UU"
            )
            TutorialThumbnailCard(
                modifier = Modifier.weight(1f),
                title = "Tutorial Video in Hindi/Urdu",
                thumbnailUrl = "https://img.youtube.com/vi/wLs7fkdlGqI/hqdefault.jpg",
                videoUrl = "https://youtu.be/wLs7fkdlGqI?si=e_O2aMgHRJAH7hac"
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // --- SOCIAL MEDIA LINKS SECTION ---
        Text(
            text = "JOIN OUR COMMUNITY",
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 2.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            SocialFooterButton("YouTube", Color.Red) {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/@GfxGamerLab_1")))
            }
            SocialFooterButton("Telegram", Color(0xFF24A1DE)) {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/gfxgamerlab")))
            }
            SocialFooterButton("WhatsApp", Color(0xFF25D366)) {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://whatsapp.com/channel/0029VbB1E5VFsn0o8MNoAp2l")))
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "SECURE ENGINE PROTOCOL v0.5",
            color = Color.White.copy(alpha = 0.1f),
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun TutorialThumbnailCard(
    modifier: Modifier = Modifier,
    title: String,
    thumbnailUrl: String,
    videoUrl: String
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    Card(
        modifier = modifier
            .clickable {
                SoundEffectsManager.playOn()
                SoundEffectsManager.performHaptic(haptic)
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(videoUrl)))
            }
            .border(1.dp, ElectricCyan.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = CyberCardDark),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column {
            AsyncImage(
                model = thumbnailUrl,
                contentDescription = title,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                contentScale = ContentScale.Crop
            )
            Text(
                text = title,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                maxLines = 2,
                lineHeight = 13.sp,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun SocialFooterButton(
    text: String,
    color: Color,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    Surface(
        onClick = {
            SoundEffectsManager.playOn()
            SoundEffectsManager.performHaptic(haptic)
            onClick()
        },
        modifier = Modifier
            .height(36.dp)
            .widthIn(min = 90.dp),
        color = color.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.5f)),
        shape = CutCornerShape(topStart = 4.dp, bottomEnd = 4.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 8.dp)) {
            Text(text = text, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black)
        }
    }
}
