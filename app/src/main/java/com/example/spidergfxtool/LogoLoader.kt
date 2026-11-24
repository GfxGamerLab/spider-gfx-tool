package com.example.spidergfxtool

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import java.io.InputStream

/**
 * Safe Asset & Drawable Brand Logo Loader with zero-crash fallbacks.
 */
object LogoLoader {
    private const val TAG = "LogoLoader"
    private val candidateAssets = listOf("brand_logo.png", "logo.png", "profile.png", "spider.png", "ic_launcher.png")

    fun loadBitmap(context: Context): Bitmap? {
        for (assetName in candidateAssets) {
            var inputStream: InputStream? = null
            try {
                inputStream = context.assets.open(assetName)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                if (bitmap != null) {
                    return bitmap
                }
            } catch (e: Throwable) {
                Log.d(TAG, "Asset $assetName not loaded: ${e.message}")
            } finally {
                try {
                    inputStream?.close()
                } catch (_: Throwable) {}
            }
        }
        return null
    }
}

@Composable
fun rememberBrandLogoPainter(): Painter {
    val context = LocalContext.current
    val bitmapPainter = remember {
        try {
            val bitmap = LogoLoader.loadBitmap(context)
            if (bitmap != null) {
                BitmapPainter(bitmap.asImageBitmap())
            } else {
                null
            }
        } catch (e: Throwable) {
            Log.e("LogoLoader", "Painter error", e)
            null
        }
    }

    if (bitmapPainter != null) {
        return bitmapPainter
    }

    return painterResource(id = R.drawable.ic_launcher_foreground)
}
