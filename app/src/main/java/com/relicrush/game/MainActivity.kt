package com.relicrush.game

import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.relicrush.game.engine.GameView
import com.relicrush.game.monetization.AdsManager
import com.relicrush.game.monetization.BillingManager

class MainActivity : AppCompatActivity(), BillingManager.Listener {
    private lateinit var gameView: GameView
    private lateinit var adsManager: AdsManager
    private lateinit var billingManager: BillingManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        adsManager = AdsManager(this)
        billingManager = BillingManager(this, this)
        gameView = GameView(this, adsManager, billingManager)
        setContentView(gameView)
        hideSystemBars()
    }

    override fun onResume() {
        super.onResume()
        hideSystemBars()
        gameView.onHostResume()
    }

    override fun onPause() {
        gameView.onHostPause()
        super.onPause()
    }

    override fun onDestroy() {
        gameView.release()
        billingManager.end()
        super.onDestroy()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemBars()
        }
    }

    override fun onRemoveAdsPurchased() {
        gameView.onRemoveAdsPurchased()
    }

    override fun onCoinPackPurchased(amount: Int) {
        gameView.onCoinPackPurchased(amount)
    }

    override fun onStoreMessage(message: String) {
        gameView.onStoreMessage(message)
    }

    private fun hideSystemBars() {
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}
