package com.example.spidergfxtool.ui.components

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.graphics.drawable.toBitmap
import com.example.spidergfxtool.SoundEffectsManager

data class AppEntry(
    val name: String,
    val packageName: String,
    val icon: Drawable
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhitelistOverlay(
    onDismiss: () -> Unit,
    onWhitelistChanged: (Set<String>) -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val pm = context.packageManager
    val prefs = remember { context.getSharedPreferences("spider_gfx_prefs", Context.MODE_PRIVATE) }
    
    var whitelist by remember { 
        mutableStateOf(prefs.getStringSet("ram_whitelist", emptySet()) ?: emptySet()) 
    }
    
    var searchQuery by remember { mutableStateOf("") }
    var installedApps by remember { mutableStateOf<List<AppEntry>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 || (it.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0 }
            .map { 
                AppEntry(
                    name = it.loadLabel(pm).toString(),
                    packageName = it.packageName,
                    icon = it.loadIcon(pm)
                )
            }
            .sortedBy { it.name }
        installedApps = apps
        isLoading = false
    }

    val filteredApps = remember(searchQuery, installedApps) {
        if (searchQuery.isBlank()) installedApps
        else installedApps.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f),
            color = Color(0xFF0A0A0A),
            shape = CutCornerShape(topStart = 16.dp, bottomEnd = 16.dp),
            border = BorderStroke(1.dp, Color(0xFFFF3131).copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "NO FORCE STOP WHITELIST",
                    color = Color(0xFFFF3131),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
                
                Spacer(modifier = Modifier.height(16.dp))

                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search App...", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.White.copy(alpha = 0.05f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                        focusedIndicatorColor = Color(0xFFFF3131),
                        cursorColor = Color(0xFFFF3131)
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (isLoading) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFFFF3131))
                    }
                } else {
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(filteredApps) { app ->
                            val isChecked = app.packageName in whitelist
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        SoundEffectsManager.playOn()
                                        SoundEffectsManager.performHaptic(haptic)
                                        val newWhitelist = if (isChecked) whitelist - app.packageName else whitelist + app.packageName
                                        whitelist = newWhitelist
                                        prefs.edit().putStringSet("ram_whitelist", newWhitelist).apply()
                                        onWhitelistChanged(newWhitelist)
                                    }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Image(
                                    bitmap = app.icon.toBitmap().asImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = app.name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    Text(text = app.packageName, color = Color.Gray, fontSize = 10.sp)
                                }
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = null,
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = Color(0xFFFF3131),
                                        uncheckedColor = Color.DarkGray,
                                        checkmarkColor = Color.White
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        SoundEffectsManager.playOff()
                        SoundEffectsManager.performHaptic(haptic)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF3131)),
                    shape = CutCornerShape(4.dp)
                ) {
                    Text("CLOSE & SAVE", fontWeight = FontWeight.Black)
                }
            }
        }
    }
}
