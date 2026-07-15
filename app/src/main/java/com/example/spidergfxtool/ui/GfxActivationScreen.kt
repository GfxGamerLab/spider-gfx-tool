package com.example.spidergfxtool.ui

import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.spidergfxtool.AudioPlayerManager
import com.example.spidergfxtool.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun GfxActivationScreen(onActivated: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var keyInput by remember { mutableStateOf("") }
    
    val infiniteTransition = rememberInfiniteTransition(label = "SpiderAnim")
    
    // Background Creeping Spiders Animation
    val spiderMove by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing), RepeatMode.Restart),
        label = "SpiderMove"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color.Black, Color(0xFF1A0000), Color.Black))),
        contentAlignment = Alignment.Center
    ) {
        // Dynamic Creeping Spiders Layer
        Box(Modifier.fillMaxSize()) {
            repeat(4) { index ->
                CreepingSpider(
                    modifier = Modifier.align(
                        when(index) {
                            0 -> Alignment.TopStart
                            1 -> Alignment.TopEnd
                            2 -> Alignment.BottomStart
                            else -> Alignment.BottomEnd
                        }
                    ),
                    progress = spiderMove,
                    index = index
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Pulsing Logo
            val pulse by infiniteTransition.animateFloat(
                initialValue = 1f, targetValue = 1.15f,
                animationSpec = infiniteRepeatable(tween(1500), RepeatMode.Reverse),
                label = "Pulse"
            )

            Box(
                modifier = Modifier
                    .size(120.dp)
                    .scale(pulse)
                    .shadow(30.dp, RoundedCornerShape(60.dp), spotColor = Color.Red)
                    .background(Brush.radialGradient(listOf(Color(0xFFFF3131), Color(0xFF8B0000))), shape = RoundedCornerShape(60.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("SPIDER", color = Color.White, fontWeight = FontWeight.Black, fontSize = 24.sp)
            }

            Spacer(modifier = Modifier.height(40.dp))

            Text("KEY ACTIVATION", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 6.sp)
            
            Spacer(modifier = Modifier.height(32.dp))

            // Cyberpunk Key Bracket
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .background(Color.Black)
                    .border(2.dp, Color(0xFFFF0033))
            ) {
                // Side accents for hacker feel
                Box(modifier = Modifier.fillMaxHeight().width(6.dp).background(Color(0xFFFF0033)).align(Alignment.CenterStart))
                Box(modifier = Modifier.fillMaxHeight().width(6.dp).background(Color(0xFFFF0033)).align(Alignment.CenterEnd))

                TextField(
                    value = keyInput,
                    onValueChange = { keyInput = it.uppercase() },
                    placeholder = { 
                        Text(
                            "SECURE ACCESS KEY", 
                            color = Color(0xFFFF0033).copy(alpha = 0.4f),
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        ) 
                    },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 4.sp,
                        fontSize = 18.sp
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

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    if (keyInput == "SPIDERGFXTOOL") {
                        // 1. Stop Intro Music
                        AudioPlayerManager.stopAndRelease()
                        
                        // 2. Play Success Sound
                        val successPlayer = MediaPlayer.create(context, R.raw.correct_key)
                        successPlayer?.apply {
                            setOnCompletionListener { it.release() }
                            start()
                        }
                        
                        // 3. Delayed Transition (3.5s) for cinematic experience
                        scope.launch {
                            delay(3500)
                            onActivated()
                        }
                    } else {
                        // Play Error Sound
                        val errorPlayer = android.media.MediaPlayer.create(context, com.example.spidergfxtool.R.raw.wrong_key)
                        errorPlayer?.apply {
                            setOnCompletionListener { mp -> mp.release() }
                            start()
                        }

                        Toast.makeText(context, "Invalid Key!", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp).shadow(10.dp, RoundedCornerShape(12.dp)),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF3131)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("ACTIVATE TOOL", fontWeight = FontWeight.Black, letterSpacing = 2.sp)
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Contact admin for key: @gfxgamerlab_1",
                color = Color.Red,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/gfxgamerlab_1")))
                }
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Social Row
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                SocialIcon("YT", "https://youtube.com/@gfxgamerlab_1", Color.Red)
                SocialIcon("TG", "https://t.me/gfxgamerlab", Color(0xFF24A1DE))
                SocialIcon("WA", "https://whatsapp.com/channel/0029VbB1E5VFsn0o8MNoAp2l", Color(0xFF25D366))
            }
        }
    }
}

@Composable
fun CreepingSpider(modifier: Modifier, progress: Float, index: Int) {
    val alpha = 0.2f
    val offset = when(index) {
        0 -> Offset(100f * progress, 100f * progress)
        1 -> Offset(-100f * progress, 100f * progress)
        2 -> Offset(100f * progress, -100f * progress)
        else -> Offset(-100f * progress, -100f * progress)
    }
    
    Canvas(modifier = modifier.size(60.dp).offset(offset.x.dp, offset.y.dp).alpha(alpha)) {
        drawCircle(Color.Red, radius = 10f) // Simplified spider body
        // Basic legs
        repeat(4) { i ->
            drawLine(Color.Red, Offset(0f,0f), Offset(20f, (i-1.5f)*15f), strokeWidth = 2f)
            drawLine(Color.Red, Offset(0f,0f), Offset(-20f, (i-1.5f)*15f), strokeWidth = 2f)
        }
    }
}
