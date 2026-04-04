package com.relicrush.game.engine

import android.annotation.SuppressLint
import android.graphics.Canvas
import android.os.SystemClock
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.appcompat.app.AppCompatActivity
import com.relicrush.game.entities.GameScreen
import com.relicrush.game.monetization.AdsManager
import com.relicrush.game.monetization.BillingManager
import com.relicrush.game.ui.UiLayout
import com.relicrush.game.ui.UiRenderer
import com.relicrush.game.utils.GameConstants
import com.relicrush.game.utils.GameMath

class GameView(
    private val activity: AppCompatActivity,
    private val adsManager: AdsManager,
    private val billingManager: BillingManager
) : SurfaceView(activity), SurfaceHolder.Callback, Runnable {

    private val engine = GameEngine(activity)
    private val layout = UiLayout()
    private val renderer = UiRenderer(activity)
    private val engineLock = Any()

    @Volatile
    private var loopRunning = false

    private var gameThread: Thread? = null
    private var touchDownX = 0f
    private var touchDownY = 0f
    private var touchStartTime = 0L
    private var gestureConsumed = false

    init {
        holder.addCallback(this)
        isFocusable = true
        keepScreenOn = true
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        startLoop()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        synchronized(engineLock) {
            layout.update(width, height)
            engine.configure(width, height)
        }
        startLoop()
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        stopLoop()
    }

    override fun run() {
        var previousFrameTime = SystemClock.elapsedRealtime()
        while (loopRunning) {
            val frameStart = SystemClock.elapsedRealtime()
            val deltaSeconds = ((frameStart - previousFrameTime).coerceAtMost(40L)) / 1000f
            previousFrameTime = frameStart

            val canvas = holder.lockCanvas()
            if (canvas != null) {
                try {
                    synchronized(engineLock) {
                        engine.update(deltaSeconds)
                        renderer.draw(canvas, engine, layout)
                    }
                } finally {
                    holder.unlockCanvasAndPost(canvas)
                }
            }

            val elapsed = SystemClock.elapsedRealtime() - frameStart
            val sleepTime = (GameConstants.FRAME_TIME_MS - elapsed).coerceAtLeast(2L)
            try {
                Thread.sleep(sleepTime)
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }
    }

    fun onHostResume() {
        synchronized(engineLock) {
            engine.onHostResume()
        }
        adsManager.preloadAds()
        billingManager.start()
        if (holder.surface.isValid) {
            startLoop()
        }
    }

    fun onHostPause() {
        synchronized(engineLock) {
            engine.onHostPause()
        }
        stopLoop()
    }

    fun release() {
        stopLoop()
        synchronized(engineLock) {
            engine.release()
        }
    }

    fun onCoinPackPurchased(amount: Int) {
        synchronized(engineLock) {
            engine.onCoinPackPurchased(amount)
        }
    }

    fun onRemoveAdsPurchased() {
        synchronized(engineLock) {
            engine.onRemoveAdsPurchased()
        }
    }

    fun onStoreMessage(message: String) {
        synchronized(engineLock) {
            engine.showStatus(message, android.graphics.Color.parseColor("#E8D98D"))
        }
    }

    private fun startLoop() {
        if (loopRunning || layout.width <= 0 || layout.height <= 0) {
            return
        }

        loopRunning = true
        gameThread = Thread(this, "RelicRushLoop").apply { start() }
    }

    private fun stopLoop() {
        if (!loopRunning) {
            return
        }

        loopRunning = false
        gameThread?.interrupt()
        try {
            gameThread?.join(400)
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
        }
        gameThread = null
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                touchDownX = event.x
                touchDownY = event.y
                touchStartTime = SystemClock.elapsedRealtime()
                gestureConsumed = false
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                synchronized(engineLock) {
                    if (engine.screen == GameScreen.RUNNING && !gestureConsumed) {
                        handleSwipeGesture(event.x, event.y)
                    }
                }
                return true
            }

            MotionEvent.ACTION_UP -> {
                performClick()
                synchronized(engineLock) {
                    if (!gestureConsumed) {
                        handleTap(event.x, event.y)
                    }
                }
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    private fun handleSwipeGesture(x: Float, y: Float) {
        val threshold = GameMath.dp(context, 38f)
        val deltaX = x - touchDownX
        val deltaY = y - touchDownY
        if (kotlin.math.abs(deltaX) < threshold && kotlin.math.abs(deltaY) < threshold) {
            return
        }

        gestureConsumed = true
        when {
            kotlin.math.abs(deltaX) > kotlin.math.abs(deltaY) && deltaX > threshold -> engine.moveRight()
            kotlin.math.abs(deltaX) > kotlin.math.abs(deltaY) && deltaX < -threshold -> engine.moveLeft()
            deltaY < -threshold -> engine.jump()
            deltaY > threshold -> engine.slide()
        }
    }

    private fun handleTap(x: Float, y: Float) {
        when (engine.screen) {
            GameScreen.HOME -> handleHomeTap(x, y)
            GameScreen.CHARACTER_SELECT -> handleCharacterTap(x, y)
            GameScreen.RUNNING -> if (layout.contains(layout.pauseButton, x, y)) engine.pauseRun()
            GameScreen.PAUSED -> handlePauseTap(x, y)
            GameScreen.GAME_OVER -> handleGameOverTap(x, y)
        }
    }

    private fun handleHomeTap(x: Float, y: Float) {
        when {
            layout.contains(layout.primaryButton, x, y) -> engine.startRun()
            layout.contains(layout.secondaryButton, x, y) -> engine.openCharacterSelect()
            layout.contains(layout.tertiaryButton, x, y) -> engine.claimDailyReward()
            layout.contains(layout.leftButton, x, y) -> {
                if (engine.removeAdsPurchased) {
                    engine.showStatus("Ads are already removed.", android.graphics.Color.parseColor("#9CF3C3"))
                } else {
                    billingManager.launchPurchase(GameConstants.PRODUCT_REMOVE_ADS)
                }
            }

            layout.contains(layout.rightButton, x, y) -> {
                billingManager.launchPurchase(GameConstants.PRODUCT_COIN_PACK)
            }
        }
    }

    private fun handleCharacterTap(x: Float, y: Float) {
        layout.characterCards.forEachIndexed { index, rect ->
            if (layout.contains(rect, x, y)) {
                engine.selectCharacter(index)
                return
            }
        }

        when {
            layout.contains(layout.characterActionButton, x, y) -> engine.unlockOrSelectPreviewCharacter()
            layout.contains(layout.characterBackButton, x, y) -> engine.backFromCharacterSelect()
        }
    }

    private fun handlePauseTap(x: Float, y: Float) {
        when {
            layout.contains(layout.musicToggle, x, y) -> engine.toggleMusic()
            layout.contains(layout.sfxToggle, x, y) -> engine.toggleSfx()
            layout.contains(layout.primaryButton, x, y) -> engine.resumeRun()
            layout.contains(layout.secondaryButton, x, y) -> engine.goHome()
        }
    }

    private fun handleGameOverTap(x: Float, y: Float) {
        when {
            layout.contains(layout.restartButton, x, y) -> exitGameOverAndThen { engine.startRun() }
            layout.contains(layout.homeButton, x, y) -> exitGameOverAndThen { engine.goHome() }
            layout.contains(layout.reviveButton, x, y) && engine.canRevive() -> {
                if (engine.removeAdsPurchased) {
                    engine.reviveRun()
                } else {
                    adsManager.showRewardedRevive(
                        onRewardEarned = {
                            synchronized(engineLock) {
                                engine.reviveRun()
                            }
                        },
                        onClosed = { rewarded ->
                            if (!rewarded) {
                                synchronized(engineLock) {
                                    engine.showStatus("Rewarded ad was skipped.", android.graphics.Color.parseColor("#FF9A7A"))
                                }
                            }
                        },
                        onUnavailable = { message ->
                            synchronized(engineLock) {
                                engine.showStatus(message, android.graphics.Color.parseColor("#FFB86C"))
                            }
                        }
                    )
                }
            }
        }
    }

    private fun exitGameOverAndThen(action: () -> Unit) {
        val shouldShowInterstitial = engine.finalizeRunForExit()
        if (shouldShowInterstitial) {
            adsManager.showInterstitial {
                synchronized(engineLock) {
                    action()
                }
            }
        } else {
            action()
        }
    }
}
