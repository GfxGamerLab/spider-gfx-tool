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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.graphics.drawscope.withTransform
import coil.compose.AsyncImage
import com.example.spidergfxtool.ui.theme.TerminalTextStyle
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
            .border(1.dp, NeonCrimson.copy(alpha = 0.2f), CutCornerShape(topStart = 16.dp, bottomEnd = 16.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = CutCornerShape(topStart = 16.dp, bottomEnd = 16.dp)
    ) {
        Box {
            // Glassmorphism Background
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.linearGradient(
                            listOf(Color.White.copy(alpha = 0.05f), Color.Transparent),
                            start = Offset(0f, 0f),
                            end = Offset(0f, 400f)
                        )
                    )
                    .background(CyberCardDark.copy(alpha = 0.7f))
                    .blur(10.dp)
            )

            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f, fill = false),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
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
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            maxLines = 2,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ID: $statusHex",
                        color = NeonCrimson.copy(alpha = 0.4f),
                        fontSize = 9.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        letterSpacing = 1.5.sp
                    )
                }
                
                val dividerBrush = remember { Brush.horizontalGradient(listOf(NeonCrimson.copy(alpha = 0.3f), Color.Transparent)) }
                Box(modifier = Modifier.padding(vertical = 12.dp).fillMaxWidth().height(1.dp).background(dividerBrush))
                
                content()
                
                Row(modifier = Modifier.padding(top = 12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Text("SECURE ENGINE PROTOCOL v9.0", color = Color.White.copy(alpha = 0.1f), fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                }
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

fun buildTerminalAnnotatedString(text: String, defaultColor: Color): androidx.compose.ui.text.AnnotatedString {
    return buildAnnotatedString {
        val lines = text.split("\n")
        lines.forEachIndexed { index, line ->
            var currentPos = 0
            while (currentPos < line.length) {
                when {
                    line.startsWith("[SUCCESS]", ignoreCase = true, startIndex = currentPos) -> {
                        withStyle(SpanStyle(color = Color(0xFF00FF00), fontWeight = FontWeight.Bold)) {
                            append("[SUCCESS]")
                        }
                        currentPos += "[SUCCESS]".length
                    }
                    line.startsWith("[SYSTEM]", ignoreCase = true, startIndex = currentPos) -> {
                        withStyle(SpanStyle(color = Color(0xFF00FFFF), fontWeight = FontWeight.Bold)) {
                            append("[SYSTEM]")
                        }
                        currentPos += "[SYSTEM]".length
                    }
                    line.startsWith("[INFO]", ignoreCase = true, startIndex = currentPos) -> {
                        withStyle(SpanStyle(color = Color(0xFF00FFFF), fontWeight = FontWeight.Bold)) {
                            append("[INFO]")
                        }
                        currentPos += "[INFO]".length
                    }
                    line.startsWith("[ERROR]", ignoreCase = true, startIndex = currentPos) -> {
                        withStyle(SpanStyle(color = Color(0xFFFF1744), fontWeight = FontWeight.Bold)) {
                            append("[ERROR]")
                        }
                        currentPos += "[ERROR]".length
                    }
                    line.startsWith("[WARNING]", ignoreCase = true, startIndex = currentPos) -> {
                        withStyle(SpanStyle(color = Color(0xFFFF9100), fontWeight = FontWeight.Bold)) {
                            append("[WARNING]")
                        }
                        currentPos += "[WARNING]".length
                    }
                    else -> {
                        withStyle(SpanStyle(color = defaultColor)) {
                            append(line[currentPos].toString())
                        }
                        currentPos++
                    }
                }
            }
            if (index < lines.size - 1) {
                append("\n")
            }
        }
    }
}

/**
 * Animated Typewriter Text for Hacker Console effect with Color-Coded Prefixes.
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

    val annotatedDisplay = remember(displayedText, cursorVisible, color) {
        buildTerminalAnnotatedString(displayedText + (if (cursorVisible) "_" else " "), color)
    }

    Text(
        text = annotatedDisplay,
        fontFamily = FontFamily.Monospace,
        fontSize = 10.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.5.sp,
        modifier = modifier.padding(8.dp)
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
    Box(
        modifier = Modifier
            .clip(CutCornerShape(4.dp))
            .background(if (isSelected) Color(0xFF330000) else Color.Transparent)
            .border(1.dp, if (isSelected) NeonCrimson else Color.DarkGray, CutCornerShape(4.dp))
            .tacticalPress {
                SoundEffectsManager.playOn()
                onClick()
            }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (isSelected) Color.White else Color.Gray,
            fontWeight = FontWeight.Black,
            fontSize = 11.sp,
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
            letterSpacing = 1.sp
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
fun GlobalCommunityFooter(modifier: Modifier = Modifier, showTutorials: Boolean = true) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- VIDEO TUTORIALS SECTION ---
        if (showTutorials) {
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
                    thumbnailUrl = "https://img.youtube.com/vi/JOMh3uegKVk/maxresdefault.jpg",
                    videoUrl = "https://youtu.be/JOMh3uegKVk"
                )
                TutorialThumbnailCard(
                    modifier = Modifier.weight(1f),
                    title = "Tutorial Video in Hindi/Urdu",
                    thumbnailUrl = "https://img.youtube.com/vi/HuuldKpzVsU/maxresdefault.jpg",
                    videoUrl = "https://youtu.be/HuuldKpzVsU"
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }

        // --- SOCIAL MEDIA LINKS SECTION ---
        Text(
            text = "JOIN OUR COMMUNITY",
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 2.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SocialFooterButton("YouTube", Color.Red, modifier = Modifier.weight(1f)) {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/@GfxGamerLab_1"))
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                SocialFooterButton("Telegram", Color(0xFF24A1DE), modifier = Modifier.weight(1f)) {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/gfxgamerlab"))
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SocialFooterButton("WhatsApp", Color(0xFF25D366), modifier = Modifier.weight(1f)) {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://whatsapp.com/channel/0029VbB1E5VFsn0o8MNoAp2l"))
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                SocialFooterButton("TikTok", Color(0xFFEE1D52), modifier = Modifier.weight(1f)) {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.tiktok.com/@gfxgamerlab?_r=1&_t=ZS-99gzTXWIINh"))
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "SECURE ENGINE PROTOCOL v9.0",
            color = Color.White.copy(alpha = 0.1f),
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Reusable modifier for a tactical scale-down effect on press with haptic feedback.
 */
@Composable
fun Modifier.tacticalPress(
    interactionSource: MutableInteractionSource? = null,
    onClick: (() -> Unit)? = null
): Modifier {
    val haptic = LocalHapticFeedback.current
    val isPressedInteraction by (interactionSource ?: remember { MutableInteractionSource() }).collectIsPressedAsState()
    
    var isPressedManual by remember { mutableStateOf(false) }
    val isPressed = isPressedInteraction || isPressedManual

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
        label = "PressScale"
    )

    val modifier = this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }

    return if (onClick != null) {
        modifier.pointerInput(onClick) {
            detectTapGestures(
                onPress = {
                    isPressedManual = true
                    try {
                        awaitRelease()
                    } finally {
                        isPressedManual = false
                    }
                },
                onTap = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onClick()
                }
            )
        }
    } else if (interactionSource == null) {
        modifier.pointerInput(Unit) {
            detectTapGestures(
                onPress = {
                    isPressedManual = true
                    try {
                        awaitRelease()
                    } finally {
                        isPressedManual = false
                    }
                }
            )
        }
    } else {
        modifier
    }
}

/**
 * Reusable modifier for a slow-moving animated neon border for active states.
 * Upgraded with BREATHING glow effect.
 */
@Composable
fun Modifier.animatedNeonBorder(
    enabled: Boolean,
    color1: Color = ElectricCyan,
    color2: Color = Color.Transparent,
    shape: Shape = CutCornerShape(12.dp),
    borderWidth: androidx.compose.ui.unit.Dp = 1.5.dp
): Modifier {
    if (!enabled) return this

    val infiniteTransition = rememberInfiniteTransition(label = "NeonBorder")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Restart),
        label = "Angle"
    )

    // BREATHING EFFECT: Animate opacity from 40% to 100%
    val breathingAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse),
        label = "Breathing"
    )

    val brush = Brush.sweepGradient(
        listOf(color1.copy(alpha = breathingAlpha), color2, color1.copy(alpha = breathingAlpha)),
        center = Offset.Unspecified
    )

    return this
        .graphicsLayer {
            rotationZ = angle
        }
        .border(borderWidth, brush, shape)
        .graphicsLayer {
            rotationZ = -angle
        }
}

/**
 * Technical HUD Grid Background with subtle dotted pattern.
 */
@Composable
fun HudGridBackground(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val strokeColor = Color.White.copy(alpha = 0.05f)
        val gridSize = 40.dp.toPx()
        val dotSize = 1.dp.toPx()

        for (x in 0..size.width.toInt() step gridSize.toInt()) {
            for (y in 0..size.height.toInt() step gridSize.toInt()) {
                drawCircle(
                    color = strokeColor,
                    radius = dotSize,
                    center = Offset(x.toFloat(), y.toFloat())
                )
            }
        }
    }
}

/**
 * Tactical Target Checkbox for sci-fi list interaction.
 */
@Composable
fun TacticalTargetCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "TargetLock")
    val lockAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
        label = "LockAlpha"
    )

    Box(
        modifier = modifier
            .size(30.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onCheckedChange(!checked) },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val color = if (checked) NeonCrimson else Color.Gray.copy(alpha = 0.4f)
            val alpha = if (checked) lockAlpha else 1f
            val sizePx = size.width
            val thickness = 1.5.dp.toPx()

            // Draw Brackets [ ]
            // Top Left
            drawLine(color.copy(alpha = alpha), Offset(0f, 0f), Offset(sizePx / 4, 0f), thickness)
            drawLine(color.copy(alpha = alpha), Offset(0f, 0f), Offset(0f, sizePx / 4), thickness)
            
            // Top Right
            drawLine(color.copy(alpha = alpha), Offset(sizePx, 0f), Offset(sizePx * 0.75f, 0f), thickness)
            drawLine(color.copy(alpha = alpha), Offset(sizePx, 0f), Offset(sizePx, sizePx / 4), thickness)

            // Bottom Left
            drawLine(color.copy(alpha = alpha), Offset(0f, sizePx), Offset(sizePx / 4, sizePx), thickness)
            drawLine(color.copy(alpha = alpha), Offset(0f, sizePx), Offset(0f, sizePx * 0.75f), thickness)

            // Bottom Right
            drawLine(color.copy(alpha = alpha), Offset(sizePx, sizePx), Offset(sizePx * 0.75f, sizePx), thickness)
            drawLine(color.copy(alpha = alpha), Offset(sizePx, sizePx), Offset(sizePx, sizePx * 0.75f), thickness)

            if (checked) {
                // Draw Crosshair in center
                drawLine(color.copy(alpha = alpha), Offset(sizePx / 2, sizePx / 4), Offset(sizePx / 2, sizePx * 0.75f), thickness)
                drawLine(color.copy(alpha = alpha), Offset(sizePx / 4, sizePx / 2), Offset(sizePx * 0.75f, sizePx / 2), thickness)
            }
        }
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
            .tacticalPress {
                SoundEffectsManager.playOn()
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(videoUrl)))
            }
            .border(1.dp, ElectricCyan.copy(alpha = 0.3f), CutCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = CyberCardDark),
        shape = CutCornerShape(12.dp)
    ) {
        Column {
            AsyncImage(
                model = thumbnailUrl,
                contentDescription = title,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(CutCornerShape(topStart = 12.dp, topEnd = 12.dp)),
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
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(30.dp)
            .tacticalPress {
                SoundEffectsManager.playOn()
                onClick()
            },
        color = color.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.5f)),
        shape = CutCornerShape(4.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)) {
            Text(text = text, color = Color.White, fontSize = 8.5.sp, fontWeight = FontWeight.Black, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, letterSpacing = 0.5.sp)
        }
    }
}

@Composable
fun CyberHudEmblem(modifier: Modifier = Modifier, glowIntensity: Float = 1f) {
    val infiniteTransition = rememberInfiniteTransition(label = "HudEmblem")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(10000, easing = LinearEasing), RepeatMode.Restart),
        label = "Rotation"
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().graphicsLayer { rotationZ = rotation }) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = size.minDimension / 2 * 0.9f

            // Outer glowing ring
            drawCircle(
                color = NeonCrimson.copy(alpha = 0.5f * glowIntensity),
                radius = radius,
                style = Stroke(width = 6f)
            )

            // Inner cyan precision ring
            drawCircle(
                color = ElectricCyan.copy(alpha = 0.7f * glowIntensity),
                radius = radius * 0.75f,
                style = Stroke(width = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f), 0f))
            )

            // Precision ticks
            for (i in 0 until 8) {
                val angle = (i * 45).toDouble() * Math.PI / 180
                val startX = center.x + (radius * 0.85f * Math.cos(angle)).toFloat()
                val startY = center.y + (radius * 0.85f * Math.sin(angle)).toFloat()
                val endX = center.x + (radius * 0.95f * Math.cos(angle)).toFloat()
                val endY = center.y + (radius * 0.95f * Math.sin(angle)).toFloat()
                drawLine(
                    color = NeonCrimson,
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = 3f
                )
            }
        }

        // Counter-rotating inner HUD core
        Canvas(modifier = Modifier.fillMaxSize().graphicsLayer { rotationZ = -rotation * 1.5f }) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = size.minDimension / 2 * 0.5f

            // Hexagonal geometric core
            val path = Path().apply {
                for (i in 0 until 6) {
                    val angle = (i * 60).toDouble() * Math.PI / 180
                    val x = center.x + (radius * Math.cos(angle)).toFloat()
                    val y = center.y + (radius * Math.sin(angle)).toFloat()
                    if (i == 0) moveTo(x, y) else lineTo(x, y)
                }
                close()
            }
            drawPath(path, color = ElectricCyan.copy(alpha = 0.6f), style = Stroke(width = 2f))

            // Crosshair lines
            drawLine(NeonCrimson, Offset(center.x - radius, center.y), Offset(center.x + radius, center.y), 2f)
            drawLine(NeonCrimson, Offset(center.x, center.y - radius), Offset(center.x, center.y + radius), 2f)
        }

        // Center glowing core dot
        Box(
            modifier = Modifier
                .size(18.dp)
                .background(NeonCrimson, CircleShape)
                .border(2.dp, ElectricCyan, CircleShape)
        )
    }
}
