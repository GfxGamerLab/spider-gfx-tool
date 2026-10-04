package com.example.spidergfxtool.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.spidergfxtool.SoundEffectsManager

@Composable
fun CyberDrawerOverlay(
    isOpen: Boolean,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val infiniteTransition = rememberInfiniteTransition(label = "DrawerGlow")
    val glowColor by infiniteTransition.animateColor(
        initialValue = Color(0xFF330000),
        targetValue = Color(0xFFFF3131),
        animationSpec = infiniteRepeatable(tween(2000), RepeatMode.Reverse),
        label = "Glow"
    )

    AnimatedVisibility(
        visible = isOpen,
        enter = slideInHorizontally(initialOffsetX = { -it }) + fadeIn(),
        exit = slideOutHorizontally(targetOffsetX = { -it }) + fadeOut(),
        modifier = Modifier.zIndex(100f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
                .clickable { onClose() }
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.85f)
                    .border(
                        width = 1.dp,
                        brush = Brush.horizontalGradient(listOf(glowColor, Color.Transparent)),
                        shape = CutCornerShape(topEnd = 24.dp, bottomEnd = 24.dp)
                    )
                    .clickable(enabled = false) {},
                color = Color(0xFF0A0A0A),
                shape = CutCornerShape(topEnd = 24.dp, bottomEnd = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                ) {
                    Text(
                        text = "SYSTEM MENU",
                        color = Color(0xFFFF3131),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 4.sp
                    )
                    
                    Box(modifier = Modifier.padding(vertical = 16.dp).fillMaxWidth().height(1.dp).background(glowColor.copy(alpha = 0.5f)))

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // SECTION: ABOUT
                        item {
                            CyberDrawerSectionHeader("ABOUT SPIDER GFX TOOL")
                            CyberGlassCard {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "Spider GFX Tool is an advanced system utility and performance optimizer. It utilizes secure local APIs (via Shizuku) to manage thermal throttling limits, synchronize local configuration files, and optimize display refresh rates for maximum hardware efficiency. Strictly operates locally without remote code execution. 100% Safe and Policy Compliant.",
                                        color = Color.White.copy(alpha = 0.7f),
                                        fontSize = 11.sp,
                                        lineHeight = 16.sp,
                                        textAlign = TextAlign.Justify
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    CyberDrawerButton("Privacy Policy") { 
                                        SoundEffectsManager.playOn()
                                        SoundEffectsManager.performHaptic(haptic)
                                    }
                                }
                            }
                        }

                        // SECTION: TUTORIALS
                        item {
                            CyberDrawerSectionHeader("OFFICIAL TUTORIALS & GUIDES")
                        }
                        
                        val tutorials = listOf(
                            TutorialItem(
                                "(PINNED) Access Android/Data Folder 2026",
                                "https://youtu.be/uRiIhKCKWAE",
                                isPinned = true
                            ),
                            TutorialItem(
                                "PUBG & BGMI 4.5 Lag Fix 2026",
                                "https://youtu.be/s64sRbinHtE"
                            ),
                            TutorialItem(
                                "Secret GFX to Unlock 120 FPS",
                                "https://youtu.be/tqgYYlkWFUQ"
                            ),
                            TutorialItem(
                                "Shizuku Game Booster Speed Up",
                                "https://youtu.be/s7vtalg_ruw"
                            )
                        )

                        items(tutorials) { item ->
                            TutorialCard(item) {
                                SoundEffectsManager.playOn()
                                SoundEffectsManager.performHaptic(haptic)
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(item.url)))
                            }
                        }

                        // SECTION: CONNECT
                        item {
                            CyberDrawerSectionHeader("CONNECT WITH DEVELOPER")
                            SocialGrid()
                        }
                    }
                    
                    Text(
                        text = "VERSION 0.8 STABLE",
                        color = Color.White.copy(alpha = 0.2f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CyberDrawerSectionHeader(title: String) {
    Text(
        text = title,
        color = Color.White,
        fontSize = 12.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
fun CyberGlassCard(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White.copy(alpha = 0.05f),
        shape = CutCornerShape(8.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
    ) {
        content()
    }
}

@Composable
fun CyberDrawerButton(text: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(40.dp),
        color = Color(0xFFFF3131).copy(alpha = 0.1f),
        border = BorderStroke(1.dp, Color(0xFFFF3131).copy(alpha = 0.5f)),
        shape = CutCornerShape(4.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

data class TutorialItem(val title: String, val url: String, val isPinned: Boolean = false)

@Composable
fun TutorialCard(item: TutorialItem, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        color = if (item.isPinned) Color(0xFF2A0000) else Color.White.copy(alpha = 0.03f),
        shape = CutCornerShape(topStart = 8.dp, bottomEnd = 8.dp),
        border = BorderStroke(1.dp, if (item.isPinned) Color(0xFFFF3131) else Color.White.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(if (item.isPinned) "📌" else "▶️", fontSize = 14.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = item.title,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2
            )
        }
    }
}

@Composable
fun SocialGrid() {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val socials = listOf(
        SocialData("YouTube", "https://youtube.com/@gfxgamerlab_1", Color.Red),
        SocialData("Telegram", "https://t.me/gfxgamerlab", Color(0xFF24A1DE)),
        SocialData("WhatsApp", "https://whatsapp.com/channel/0029VbB1E5VFsn0o8MNoAp2l", Color(0xFF25D366)),
        SocialData("Facebook", "https://www.facebook.com/share/1VRENXmyXD/", Color(0xFF1877F2))
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        socials.forEach { social ->
            Surface(
                onClick = { 
                    SoundEffectsManager.playOn()
                    SoundEffectsManager.performHaptic(haptic)
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(social.url))) 
                },
                modifier = Modifier.fillMaxWidth().height(45.dp),
                color = social.color.copy(alpha = 0.1f),
                border = BorderStroke(1.dp, social.color.copy(alpha = 0.4f)),
                shape = CutCornerShape(topStart = 4.dp, bottomEnd = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(social.name, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black)
                    Spacer(modifier = Modifier.weight(1f))
                    Text("CONNECT >", color = social.color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

data class SocialData(val name: String, val url: String, val color: Color)
