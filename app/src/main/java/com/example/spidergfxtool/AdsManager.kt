package com.example.spidergfxtool

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

object AdsManager {
    private const val TAG = "AdsManager"

    private var interstitialAd: InterstitialAd? = null

    fun init(context: Context) {
        MobileAds.initialize(context) {}
    }

    /**
     * Loads a standard Banner Ad with fixed height to prevent layout shift.
     */
    @Composable
    fun BannerAdView(modifier: Modifier = Modifier) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(50.dp) // Strictly fixed height to prevent UI jumping
        ) {
            AndroidView(
                modifier = Modifier.fillMaxWidth(),
                factory = { context ->
                    AdView(context).apply {
                        setAdSize(AdSize.BANNER)
                        adUnitId = context.getString(R.string.banner_ad_unit_id)
                        loadAd(AdRequest.Builder().build())
                        adListener = object : AdListener() {
                            override fun onAdFailedToLoad(error: LoadAdError) {
                                Log.e(TAG, "Banner failed to load: ${error.message}")
                            }
                        }
                    }
                }
            )
        }
    }

    /**
     * Pre-loads an Interstitial Ad using production ID.
     */
    fun loadInterstitial(context: Context) {
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            context,
            context.getString(R.string.interstitial_ad_unit_id),
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    Log.d(TAG, "Interstitial Ad Loaded")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                    Log.e(TAG, "Interstitial failed to load: ${error.message}")
                }
            }
        )
    }

    /**
     * Shows Interstitial Ad if ready, otherwise executes the callback immediately.
     */
    fun showInterstitial(activity: Activity, onAdDismissed: () -> Unit) {
        if (interstitialAd != null) {
            interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    interstitialAd = null
                    onAdDismissed()
                    loadInterstitial(activity) // Pre-load next ad
                }

                override fun onAdFailedToShowFullScreenContent(error: com.google.android.gms.ads.AdError) {
                    interstitialAd = null
                    onAdDismissed()
                }
            }
            interstitialAd?.show(activity)
        } else {
            // Ad not ready, execute action immediately
            onAdDismissed()
            loadInterstitial(activity) // Try loading for next time
        }
    }

    /**
     * Executes the action first, then opens the smart Ad link in the external browser.
     */
    fun executeWithAd(context: Context, action: () -> Unit) {
        val adUrl = "https://hankperryeugene.com/de7j79ut?key=4dd3674d2c81383319cd6dd656477690"
        
        // 1. Execute the tool's intended action first (so it's ready when they return)
        action.invoke()
        
        // 2. Open the Ad in the external browser
        try {
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(adUrl))
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
