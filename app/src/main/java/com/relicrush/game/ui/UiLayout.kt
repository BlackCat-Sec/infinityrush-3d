package com.relicrush.game.ui

import android.graphics.RectF
import com.relicrush.game.utils.GameMath

class UiLayout {
    var width = 0
        private set
    var height = 0
        private set

    val heroPanel = RectF()
    val missionPanel = RectF()
    val storePanel = RectF()
    val primaryButton = RectF()
    val secondaryButton = RectF()
    val tertiaryButton = RectF()
    val leftButton = RectF()
    val rightButton = RectF()
    val pauseButton = RectF()
    val musicToggle = RectF()
    val sfxToggle = RectF()
    val restartButton = RectF()
    val homeButton = RectF()
    val reviveButton = RectF()
    val characterActionButton = RectF()
    val characterBackButton = RectF()
    val characterCards = MutableList(4) { RectF() }
    val hudProgressBar = RectF()

    fun update(width: Int, height: Int) {
        this.width = width
        this.height = height

        val isLandscape = width > height
        val sideInset = width * if (isLandscape) 0.06f else 0.07f
        val topInset = height * if (isLandscape) 0.06f else 0.05f
        val buttonHeight = height * if (isLandscape) 0.08f else 0.07f
        val buttonWidth = width * if (isLandscape) 0.25f else 0.36f

        heroPanel.set(
            sideInset,
            topInset + height * 0.08f,
            width - sideInset,
            topInset + height * if (isLandscape) 0.48f else 0.42f
        )

        missionPanel.set(
            sideInset,
            heroPanel.bottom + height * 0.03f,
            width - sideInset,
            heroPanel.bottom + height * if (isLandscape) 0.21f else 0.19f
        )

        storePanel.set(
            sideInset,
            missionPanel.bottom + height * 0.025f,
            width - sideInset,
            missionPanel.bottom + height * if (isLandscape) 0.17f else 0.16f
        )

        primaryButton.set(
            width * 0.5f - buttonWidth / 2f,
            height * 0.78f,
            width * 0.5f + buttonWidth / 2f,
            height * 0.78f + buttonHeight
        )

        secondaryButton.set(
            width * 0.5f - buttonWidth / 2f,
            primaryButton.bottom + height * 0.018f,
            width * 0.5f + buttonWidth / 2f,
            primaryButton.bottom + height * 0.018f + buttonHeight
        )

        tertiaryButton.set(
            width * 0.5f - buttonWidth / 2f,
            secondaryButton.bottom + height * 0.018f,
            width * 0.5f + buttonWidth / 2f,
            secondaryButton.bottom + height * 0.018f + buttonHeight
        )

        leftButton.set(
            sideInset,
            storePanel.bottom - buttonHeight,
            sideInset + buttonWidth * 0.94f,
            storePanel.bottom
        )

        rightButton.set(
            width - sideInset - buttonWidth * 0.94f,
            storePanel.bottom - buttonHeight,
            width - sideInset,
            storePanel.bottom
        )

        val pauseSize = minOf(width, height) * 0.11f
        pauseButton.set(
            width - sideInset - pauseSize,
            topInset,
            width - sideInset,
            topInset + pauseSize
        )

        hudProgressBar.set(
            sideInset,
            topInset + pauseSize * 0.2f,
            width - sideInset - pauseSize - width * 0.03f,
            topInset + pauseSize * 0.58f
        )

        musicToggle.set(
            width * 0.18f,
            height * 0.43f,
            width * 0.82f,
            height * 0.50f
        )
        sfxToggle.set(
            width * 0.18f,
            musicToggle.bottom + height * 0.025f,
            width * 0.82f,
            musicToggle.bottom + height * 0.095f
        )

        restartButton.set(primaryButton)
        homeButton.set(secondaryButton)
        reviveButton.set(tertiaryButton)

        val cardGap = width * 0.03f
        val cardWidth = if (isLandscape) (width - sideInset * 2 - cardGap * 3) / 4f else (width - sideInset * 2 - cardGap) / 2f
        val cardHeight = height * if (isLandscape) 0.38f else 0.24f
        val cardTop = topInset + height * 0.1f

        repeat(4) { index ->
            val row = if (isLandscape) 0 else index / 2
            val column = if (isLandscape) index else index % 2
            val left = sideInset + column * (cardWidth + cardGap)
            val top = cardTop + row * (cardHeight + height * 0.03f)
            characterCards[index].set(left, top, left + cardWidth, top + cardHeight)
        }

        characterActionButton.set(
            width * 0.18f,
            if (isLandscape) height * 0.74f else height * 0.67f,
            width * 0.82f,
            if (isLandscape) height * 0.82f else height * 0.75f
        )

        characterBackButton.set(
            width * 0.18f,
            characterActionButton.bottom + height * 0.02f,
            width * 0.82f,
            characterActionButton.bottom + height * 0.09f
        )
    }

    fun contains(rect: RectF, x: Float, y: Float): Boolean {
        return rect.contains(x, y)
    }

    fun inset(rect: RectF, amount: Float): RectF {
        return RectF(rect.left + amount, rect.top + amount, rect.right - amount, rect.bottom - amount)
    }

    fun centerTextY(rect: RectF, ascent: Float, descent: Float): Float {
        return rect.centerY() - (descent + ascent) / 2f
    }

    fun radius(base: Float): Float = GameMath.lerp(base * 0.16f, base * 0.5f, 0.4f)
}
