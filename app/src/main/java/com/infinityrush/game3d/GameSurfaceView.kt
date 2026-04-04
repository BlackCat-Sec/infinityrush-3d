package com.infinityrush.game3d

import android.content.Context
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import android.view.MotionEvent
import kotlin.math.abs

class GameSurfaceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : GLSurfaceView(context, attrs) {
    private val rendererImpl = GameRenderer(context.applicationContext)

    private var touchDownX = 0f
    private var touchDownY = 0f
    private var touchDownTime = 0L
    private var gestureHandled = false

    init {
        setEGLContextClientVersion(2)
        preserveEGLContextOnPause = true
        setRenderer(rendererImpl)
        renderMode = RENDERMODE_CONTINUOUSLY
    }

    fun setUiListener(listener: GameRenderer.UiListener) {
        rendererImpl.uiListener = listener
    }

    fun startRun() {
        queueEvent { rendererImpl.startRun() }
    }

    fun restartRun() {
        queueEvent { rendererImpl.startRun() }
    }

    fun pauseRun() {
        queueEvent { rendererImpl.pauseRunFromUi() }
    }

    fun resumeRun() {
        queueEvent { rendererImpl.resumeRunFromUi() }
    }

    fun setMusicEnabled(enabled: Boolean) {
        queueEvent { rendererImpl.setMusicEnabled(enabled) }
    }

    fun setSfxEnabled(enabled: Boolean) {
        queueEvent { rendererImpl.setSfxEnabled(enabled) }
    }

    fun pauseForLifecycle() {
        queueEvent { rendererImpl.onHostPause() }
        onPause()
    }

    fun resumeForLifecycle() {
        onResume()
        queueEvent { rendererImpl.onHostResume() }
    }

    @Suppress("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                touchDownX = event.x
                touchDownY = event.y
                touchDownTime = event.eventTime
                gestureHandled = false
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val deltaX = event.x - touchDownX
                val deltaY = event.y - touchDownY
                if (!gestureHandled && abs(deltaY) > abs(deltaX) && deltaY > 120f) {
                    queueEvent { rendererImpl.onSwipe(SwipeDirection.DOWN) }
                    gestureHandled = true
                }
                return true
            }

            MotionEvent.ACTION_UP -> {
                val deltaX = event.x - touchDownX
                val deltaY = event.y - touchDownY
                val duration = event.eventTime - touchDownTime

                if (!gestureHandled) {
                    when {
                        abs(deltaX) > abs(deltaY) && abs(deltaX) > 110f -> {
                            queueEvent {
                                rendererImpl.onSwipe(
                                    if (deltaX > 0f) SwipeDirection.RIGHT else SwipeDirection.LEFT
                                )
                            }
                        }

                        duration < 220L && abs(deltaY) < 90f -> {
                            queueEvent { rendererImpl.onTap() }
                        }
                    }
                }

                performClick()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }
}

