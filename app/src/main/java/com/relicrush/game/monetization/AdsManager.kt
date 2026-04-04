package com.relicrush.game.monetization

import android.app.Activity
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.relicrush.game.utils.GameConstants

class AdsManager(private val activity: Activity) {
    private var rewardedAd: RewardedAd? = null
    private var interstitialAd: InterstitialAd? = null
    private var isRewardLoading = false
    private var isInterstitialLoading = false

    fun preloadAds() {
        loadRewarded()
        loadInterstitial()
    }

    fun loadRewarded() {
        if (rewardedAd != null || isRewardLoading) {
            return
        }

        isRewardLoading = true
        RewardedAd.load(
            activity,
            GameConstants.TEST_REWARDED_AD_UNIT,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    isRewardLoading = false
                    rewardedAd = ad
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    isRewardLoading = false
                    rewardedAd = null
                }
            }
        )
    }

    fun loadInterstitial() {
        if (interstitialAd != null || isInterstitialLoading) {
            return
        }

        isInterstitialLoading = true
        InterstitialAd.load(
            activity,
            GameConstants.TEST_INTERSTITIAL_AD_UNIT,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    isInterstitialLoading = false
                    interstitialAd = ad
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    isInterstitialLoading = false
                    interstitialAd = null
                }
            }
        )
    }

    fun showRewardedRevive(
        onRewardEarned: () -> Unit,
        onClosed: (Boolean) -> Unit,
        onUnavailable: (String) -> Unit
    ) {
        val ad = rewardedAd
        if (ad == null) {
            loadRewarded()
            onUnavailable("Rewarded ad is still loading.")
            return
        }

        var earnedReward = false
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                rewardedAd = null
                loadRewarded()
                onClosed(earnedReward)
            }

            override fun onAdFailedToShowFullScreenContent(adError: com.google.android.gms.ads.AdError) {
                rewardedAd = null
                loadRewarded()
                onUnavailable("Rewarded ad failed to open.")
            }
        }

        ad.show(activity) {
            earnedReward = true
            onRewardEarned()
        }
    }

    fun showInterstitial(onClosed: () -> Unit) {
        val ad = interstitialAd ?: run {
            loadInterstitial()
            onClosed()
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                interstitialAd = null
                loadInterstitial()
                onClosed()
            }

            override fun onAdFailedToShowFullScreenContent(adError: com.google.android.gms.ads.AdError) {
                interstitialAd = null
                loadInterstitial()
                onClosed()
            }
        }

        ad.show(activity)
    }
}
