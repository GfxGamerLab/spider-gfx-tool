package com.example.spidergfxtool

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

object AdsManager {
    private const val TAG = "AdsManager"
    
    // Ad Unit IDs
    private const val BANNER_AD_UNIT_ID = "ca-app-pub-3834589609578692/6944671506"
    private const val INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3834589609578692/3089186977"
    private const val TEST_DEVICE_ID = "7d37cb69-a705-4461-8f0e-10ca1ad1abe1"

    private var interstitialAd: InterstitialAd? = null

    fun init(context: Context) {
        MobileAds.initialize(context) {}
        
        // Configure Test Device
        val configuration = RequestConfiguration.Builder()
            .setTestDeviceIds(listOf(TEST_DEVICE_ID))
            .build()
        MobileAds.setRequestConfiguration(configuration)
    }

    /**
     * Loads a standard Banner Ad.
     */
    @Composable
    fun BannerAdView(modifier: Modifier = Modifier) {
        AndroidView(
            modifier = modifier.fillMaxWidth(),
            factory = { context ->
                AdView(context).apply {
                    setAdSize(AdSize.BANNER)
                    adUnitId = BANNER_AD_UNIT_ID
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

    /**
     * Pre-loads an Interstitial Ad.
     */
    fun loadInterstitial(context: Context) {
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            context,
            INTERSTITIAL_AD_UNIT_ID,
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
}
