package com.example.spidergfxtool.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.example.spidergfxtool.GfxBackendManager
import com.example.spidergfxtool.R
import rikka.shizuku.Shizuku
import kotlinx.coroutines.launch
import java.io.File

data class PubgVersion(val name: String, val packageName: String)

val pubgVersions = listOf(
    PubgVersion("Global", "com.tencent.ig"),
    PubgVersion("Korea", "com.pubg.krmobile"),
    PubgVersion("Taiwan", "com.rekoo.pubgm"),
    PubgVersion("Vietnam", "com.vng.pubgmobile"),
    PubgVersion("BGMI", "com.pubg.imobile")
)

@Composable
fun GfxDashboardScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isConnected = GfxBackendManager.isShizukuConnected.value
    var isProcessing by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("") }
    var selectedVersion by remember { mutableStateOf(pubgVersions[0]) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        GfxBackendManager.checkAndRefreshStatus()
    }

    LaunchedEffect(isConnected) {
        if (!isConnected && Shizuku.pingBinder()) GfxBackendManager.checkAndRefreshStatus()
    }

    val infiniteTransition = rememberInfiniteTransition(label = "Dashboard")
    val color1 by infiniteTransition.animateColor(
        initialValue = Color(0xFF001F3F), targetValue = Color(0xFF3F0000),
        animationSpec = infiniteRepeatable(tween(4000), RepeatMode.Reverse), label = "C1"
    )
    val color2 by infiniteTransition.animateColor(
        initialValue = Color(0xFF00003F), targetValue = Color(0xFF800000),
        animationSpec = infiniteRepeatable(tween(4000, 1000), RepeatMode.Reverse), label = "C2"
    )

    val neonAlpha by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
        label = "NeonAlpha"
    )

    val showCursor by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse),
        label = "Cursor"
    )

    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(color1, color2, Color.Black)))) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Dashboard Header Branding
                Image(
                    painter = painterResource(id = R.mipmap.ic_launcher_foreground),
                    contentDescription = "Spider Logo",
                    modifier = Modifier
                        .size(100.dp)
                        .padding(bottom = 16.dp),
                    contentScale = ContentScale.Fit
                )

                Card(
                    modifier = Modifier.fillMaxWidth().shadow(16.dp, RoundedCornerShape(16.dp))
                        .alpha(if (isProcessing) 1f else neonAlpha),
                    colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(if (isConnected) Color.Green else Color.Red))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(if (isConnected) "Shizuku Connected" else "Shizuku Disconnected", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    color = Color(0xFFFF3131).copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF3131).copy(alpha = 0.5f))
                ) {
                    Text("PUBG 4.5 Optimized", color = Color(0xFFFF3131), fontSize = 14.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp), letterSpacing = 2.sp)
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Version Selector Grid
                VersionSelector(
                    selectedVersion = selectedVersion,
                    onVersionSelected = { selectedVersion = it }
                )
            }

            // Main Action Area with Web/Mask Animation
            Box(contentAlignment = Alignment.Center, modifier = Modifier.weight(1f)) {
                if (!isProcessing) {
                    MorphingWebMask(infiniteTransition)
                }

                if (isProcessing) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .padding(16.dp)
                            .border(1.dp, Color(0xFFFF3131), RoundedCornerShape(8.dp)),
                        colors = CardDefaults.cardColors(containerColor = Color.Black)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp)
                                .background(Color.Black)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = statusText + if (showCursor > 0.5f) "_" else " ",
                                color = Color(0xFFFF3131),
                                fontSize = 12.sp,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                lineHeight = 18.sp
                            )
                        }
                    }
                } else {
                    UnlockButton(enabled = isConnected, alphaPulse = neonAlpha, onClick = {
                        if (!isConnected) {
                            Toast.makeText(context, "Please connect Shizuku first", Toast.LENGTH_SHORT).show()
                        } else {
                            scope.launch {
                                isProcessing = true
                                statusText = "[SYSTEM] Initiating Cyber Security Bypass..."
                                kotlinx.coroutines.delay(1000)
                                
                                val extCache = context.externalCacheDir ?: File("/sdcard/Android/data/${context.packageName}/cache")
                                val unzippedDir = File(extCache, "extracted_files")
                                
                                statusText += "\n[DOWNLOAD] Pulling Node Stream from GitHub..."
                                GfxBackendManager.downloadGfxZip(context).onSuccess { zip ->
                                    statusText += "\n[DECRYPT] Validating PUBG 4.5 Configuration Structure..."
                                    kotlinx.coroutines.delay(800)
                                    
                                    GfxBackendManager.extractZip(zip, unzippedDir).onSuccess { folder ->
                                        statusText += "\n[SYSTEM] Injecting GFX Patches to ${selectedVersion.name}..."
                                        GfxBackendManager.applyGfx(folder, selectedVersion.packageName).onSuccess { 
                                            statusText += "\n[SUCCESS] ${selectedVersion.name} Stream Patched Successfully!"
                                            kotlinx.coroutines.delay(1500)
                                            Toast.makeText(context, it, Toast.LENGTH_LONG).show() 
                                        }.onFailure { 
                                            statusText += "\n[ERROR] Injection Failed"
                                            Toast.makeText(context, "Apply failed", Toast.LENGTH_SHORT).show() 
                                        }
                                    }.onFailure { 
                                        statusText += "\n[ERROR] Decryption Error"
                                        Toast.makeText(context, "Extract failed", Toast.LENGTH_SHORT).show() 
                                    }
                                }.onFailure { 
                                    statusText += "\n[ERROR] Download Stream Interrupted"
                                    Toast.makeText(context, "Download failed", Toast.LENGTH_SHORT).show() 
                                }
                                isProcessing = false
                            }
                        }
                    })
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                SocialIcon("YouTube", "https://youtube.com/@gfxgamerlab_1", Color.Red)
                SocialIcon("Telegram", "https://t.me/gfxgamerlab", Color(0xFF24A1DE))
                SocialIcon("WhatsApp", "https://whatsapp.com/channel/0029VbB1E5VFsn0o8MNoAp2l", Color(0xFF25D366))
            }
        }
    }
}

@Composable
fun MorphingWebMask(infiniteTransition: InfiniteTransition) {
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f, targetValue = 1.2f,
        animationSpec = infiniteRepeatable(tween(3000), RepeatMode.Reverse), label = "WebScale"
    )
    val morph by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3000), RepeatMode.Reverse), label = "WebMorph"
    )

    Canvas(modifier = Modifier.size(300.dp).scale(scale).alpha(0.3f)) {
        val center = Offset(size.width / 2, size.height / 2)
        // Web Lines
        for (i in 0 until 8) {
            val angle = (i * 45).toDouble() * Math.PI / 180
            drawLine(
                color = Color.White,
                start = center,
                end = Offset(
                    center.x + (size.width/2 * Math.cos(angle)).toFloat(),
                    center.y + (size.height/2 * Math.sin(angle)).toFloat()
                ),
                strokeWidth = 2f
            )
        }
        // Morphing Mask Shape (Simplified silhouette)
        val path = Path()
        val width = size.width * (0.3f + 0.1f * morph)
        val height = size.height * (0.4f - 0.1f * morph)
        path.addOval(androidx.compose.ui.geometry.Rect(center.x - width, center.y - height, center.x + width, center.y + height))
        drawPath(path, Color.Red.copy(alpha = 0.5f * morph), style = Stroke(4f))
    }
}

@Composable
fun UnlockButton(enabled: Boolean, alphaPulse: Float, onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "Glow")
    val glowScale by infiniteTransition.animateFloat(initialValue = 1f, targetValue = 1.05f, animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse), label = "Glow")
    val alpha by animateFloatAsState(if (enabled) alphaPulse else 0.5f, label = "A")

    Box(
        modifier = Modifier.size(240.dp).scale(if (enabled) glowScale else 1f).alpha(alpha)
            .shadow(if (enabled) 30.dp else 0.dp, CircleShape, spotColor = Color.Blue)
            .clip(CircleShape).background(if (enabled) Brush.linearGradient(listOf(Color(0xFFFF3131), Color(0xFF3131FF))) else Brush.linearGradient(listOf(Color.Gray, Color.DarkGray)))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text("120 FPS\nUNLOCKER", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center, lineHeight = 30.sp)
    }
}

@Composable
fun SocialIcon(name: String, url: String, color: Color) {
    val context = LocalContext.current
    Surface(
        modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) },
        color = color.copy(alpha = 0.2f), border = androidx.compose.foundation.BorderStroke(1.dp, color)
    ) {
        Text(name, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VersionSelector(selectedVersion: PubgVersion, onVersionSelected: (PubgVersion) -> Unit) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        maxItemsInEachRow = 3
    ) {
        pubgVersions.forEach { version ->
            val isSelected = selectedVersion == version
            Surface(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onVersionSelected(version) },
                color = if (isSelected) Color(0xFFFF3131).copy(alpha = 0.3f) else Color.Black.copy(alpha = 0.4f),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp, 
                    if (isSelected) Color(0xFFFF3131) else Color.Gray.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = version.name,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    color = if (isSelected) Color.White else Color.Gray,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}
