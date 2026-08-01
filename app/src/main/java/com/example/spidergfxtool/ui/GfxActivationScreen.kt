package com.example.spidergfxtool.ui

import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.spidergfxtool.AudioPlayerManager
import com.example.spidergfxtool.R
import com.example.spidergfxtool.ui.components.SocialIcon
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val SPIDERMAN = "SPIDERMAN"

@Composable
fun GfxActivationScreen(onActivated: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var keyInput by remember { mutableStateOf("") }
    
    val infiniteTransition = rememberInfiniteTransition(label = "SpiderAnim")
    
    val spiderMove by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "SpiderMove"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color.Black, Color(0xFF1A0000), Color.Black))),
        contentAlignment = Alignment.Center
    ) {
        // Background Spiders
        Box(modifier = Modifier.fillMaxSize()) {
            repeat(4) { index ->
                val alignment = when(index) {
                    0 -> Alignment.TopStart
                    1 -> Alignment.TopEnd
                    2 -> Alignment.BottomStart
                    else -> Alignment.BottomEnd
                }
                CreepingSpider(
                    modifier = Modifier.align(alignment),
                    progress = spiderMove,
                    index = index
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 32.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            val pulse by infiniteTransition.animateFloat(
                initialValue = 1f, targetValue = 1.15f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1500),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "Pulse"
            )

            Box(
                modifier = Modifier
                    .size(100.dp)
                    .scale(pulse)
                    .shadow(30.dp, CircleShape, spotColor = Color.Red)
                    .background(
                        brush = Brush.radialGradient(listOf(Color(0xFFFF3131), Color(0xFF8B0000))), 
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.mipmap.ic_launcher_foreground),
                    contentDescription = "App Logo",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "KEY ACTIVATION", 
                color = Color.White, 
                fontSize = 22.sp, 
                fontWeight = FontWeight.ExtraBold, 
                letterSpacing = 4.sp
            )
            
            Spacer(modifier = Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .background(Color.Black)
                    .border(2.dp, Color(0xFFFF0033))
            ) {
                Box(modifier = Modifier.fillMaxHeight().width(6.dp).background(Color(0xFFFF0033)).align(Alignment.CenterStart))
                Box(modifier = Modifier.fillMaxHeight().width(6.dp).background(Color(0xFFFF0033)).align(Alignment.CenterEnd))

                TextField(
                    value = keyInput,
                    onValueChange = { newValue -> keyInput = newValue.uppercase() },
                    placeholder = { 
                        Text(
                            text = "SECURE ACCESS KEY", 
                            color = Color(0xFFFF0033).copy(alpha = 0.4f),
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp,
                            fontSize = 14.sp
                        ) 
                    },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 4.sp,
                        fontSize = 16.sp
                    ),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = Color(0xFFFF0033)
                    )
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (keyInput == SPIDERMAN) {
                        val successPlayer = MediaPlayer.create(context, R.raw.correct_key)
                        successPlayer?.apply {
                            setOnCompletionListener { mp -> mp.release() }
                            start()
                        }
                        scope.launch {
                            delay(3500)
                            onActivated()
                        }
                    } else {
                        val errorPlayer = MediaPlayer.create(context, R.raw.wrong_key)
                        errorPlayer?.apply {
                            setOnCompletionListener { mp -> mp.release() }
                            start()
                        }
                        Toast.makeText(context, "Invalid Key!", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp).shadow(10.dp, RoundedCornerShape(12.dp)),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF3131)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "VERIFY & CONTINUE", 
                    fontWeight = FontWeight.Black, 
                    letterSpacing = 2.sp
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "join channel for key",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(12.dp))
                Surface(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/gfxgamerlab"))
                        context.startActivity(intent)
                    },
                    modifier = Modifier
                        .height(34.dp)
                        .width(75.dp)
                        .shadow(12.dp, RoundedCornerShape(6.dp), spotColor = Color.Red),
                    color = Color(0xFF1C1C1C),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, Color(0xFFFF3131))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "JOIN",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(), 
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SocialIcon("YT", "https://youtube.com/@gfxgamerlab_1", Color.Red)
                SocialIcon("TG", "https://t.me/gfxgamerlab", Color(0xFF24A1DE))
                SocialIcon("WA", "https://whatsapp.com/channel/0029VbB1E5VFsn0o8MNoAp2l", Color(0xFF25D366))
            }
        }
    }
}

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
            .alpha(0.2f)
    ) {
        drawCircle(Color.Red, radius = 10f)
        repeat(4) { i ->
            drawLine(Color.Red, Offset(0f, 0f), Offset(20f, (i - 1.5f) * 15f), strokeWidth = 2f)
            drawLine(Color.Red, Offset(0f, 0f), Offset(-20f, (i - 1.5f) * 15f), strokeWidth = 2f)
        }
    }
}
