package com.relicrush.game

import android.app.Application
import com.google.android.gms.ads.MobileAds

class RelicRushApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // The Mobile Ads SDK should be initialized once at app start.
        Thread {
            MobileAds.initialize(this) { }
        }.start()
    }
}
