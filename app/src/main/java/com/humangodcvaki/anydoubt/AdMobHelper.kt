package com.humangodcvaki.anydoubt

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

class AdMobHelper(private val context: Context) {

    private var interstitialAd: InterstitialAd? = null
    private var isAdLoading = false

    companion object {
        private const val TAG = "AdMobHelper"

        // Test Ad Unit ID - Replace with your actual Ad Unit ID in production
        private const val AD_UNIT_ID = "ca-app-pub-3734613584513892/2522409832"
        // Get your actual Ad Unit ID from: https://apps.admob.com/
    }

    init {
        // Initialize Mobile Ads SDK
        MobileAds.initialize(context) { initializationStatus ->
            Log.d(TAG, "AdMob SDK initialized: ${initializationStatus.adapterStatusMap}")
        }
        loadInterstitialAd()
    }

    /**
     * Load the interstitial ad
     */
    private fun loadInterstitialAd() {
        if (isAdLoading) {
            Log.d(TAG, "Ad is already loading")
            return
        }

        isAdLoading = true
        val adRequest = AdRequest.Builder().build()

        InterstitialAd.load(
            context,
            AD_UNIT_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.e(TAG, "Ad failed to load: ${adError.message}")
                    interstitialAd = null
                    isAdLoading = false
                }

                override fun onAdLoaded(ad: InterstitialAd) {
                    Log.d(TAG, "Ad loaded successfully")
                    interstitialAd = ad
                    isAdLoading = false
                }
            }
        )
    }

    /**
     * Show the interstitial ad with callback
     * @param activity The activity context
     * @param onAdDismissed Callback when ad is dismissed or failed to show
     */
    fun showInterstitialAd(
        activity: Activity,
        onAdDismissed: () -> Unit
    ) {
        if (interstitialAd != null) {
            interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "Ad was dismissed")
                    interstitialAd = null
                    // Preload next ad
                    loadInterstitialAd()
                    // Execute the callback
                    onAdDismissed()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Log.e(TAG, "Ad failed to show: ${adError.message}")
                    interstitialAd = null
                    // Execute the callback even if ad fails
                    onAdDismissed()
                }

                override fun onAdShowedFullScreenContent() {
                    Log.d(TAG, "Ad showed fullscreen content")
                }
            }

            interstitialAd?.show(activity)
        } else {
            Log.d(TAG, "The interstitial ad wasn't ready yet")
            // If ad is not ready, try to load and execute callback immediately
            loadInterstitialAd()
            onAdDismissed()
        }
    }

    /**
     * Check if ad is ready to show
     */
    fun isAdReady(): Boolean {
        return interstitialAd != null
    }

    /**
     * Preload ad for next time
     */
    fun preloadAd() {
        if (interstitialAd == null && !isAdLoading) {
            loadInterstitialAd()
        }
    }
}