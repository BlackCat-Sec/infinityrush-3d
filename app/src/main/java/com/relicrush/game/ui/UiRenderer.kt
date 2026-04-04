package com.relicrush.game.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import com.relicrush.game.engine.GameEngine
import com.relicrush.game.entities.CharacterPalette
import com.relicrush.game.entities.Coin
import com.relicrush.game.entities.EnvironmentTheme
import com.relicrush.game.entities.GameContent
import com.relicrush.game.entities.GameScreen
import com.relicrush.game.entities.Obstacle
import com.relicrush.game.entities.ObstacleType
import com.relicrush.game.entities.Player
import com.relicrush.game.entities.PowerUp
import com.relicrush.game.entities.PowerUpType
import com.relicrush.game.utils.GameConstants
import com.relicrush.game.utils.GameMath
import kotlin.math.sin

class UiRenderer(context: Context) {
    private data class Projection(
        val x: Float,
        val y: Float,
        val scale: Float,
        val depth: Float
    )

    private val density = context.resources.displayMetrics.density
    private val tempPath = Path()

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f * density
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
    }
    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }
    private val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.LEFT
    }
    private val smallPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.LEFT
    }
    private val hudPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.LEFT
        isFakeBoldText = true
    }
    private val chipTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(80, 0, 0, 0)
    }
    private val particlePaint = Paint(Paint.ANTI_ALIAS_FLAG)

    fun draw(canvas: Canvas, engine: GameEngine, layout: UiLayout) {
        if (layout.width <= 0 || layout.height <= 0) {
            return
        }

        updateTextSizes(layout)
        drawWorld(canvas, engine, layout)

        when (engine.screen) {
            GameScreen.HOME -> drawHome(canvas, engine, layout)
            GameScreen.CHARACTER_SELECT -> drawCharacterSelect(canvas, engine, layout)
            GameScreen.RUNNING -> drawHud(canvas, engine, layout)
            GameScreen.PAUSED -> {
                drawHud(canvas, engine, layout)
                drawPauseOverlay(canvas, engine, layout)
            }

            GameScreen.GAME_OVER -> {
                drawHud(canvas, engine, layout)
                drawGameOver(canvas, engine, layout)
            }
        }

        drawRuntimeMessage(canvas, engine, layout)
    }

    private fun updateTextSizes(layout: UiLayout) {
        titlePaint.textSize = layout.height * 0.066f
        textPaint.textSize = layout.height * 0.031f
        bodyPaint.textSize = layout.height * 0.03f
        smallPaint.textSize = layout.height * 0.023f
        hudPaint.textSize = layout.height * 0.03f
        chipTextPaint.textSize = layout.height * 0.024f
    }

    private fun drawWorld(canvas: Canvas, engine: GameEngine, layout: UiLayout) {
        drawSky(canvas, engine, layout)
        drawParallax(canvas, engine, layout)
        drawTrack(canvas, engine, layout)
        drawWorldItems(canvas, engine, layout)
        drawPlayer(canvas, engine, layout)
        drawParticles(canvas, engine)
    }

    private fun drawSky(canvas: Canvas, engine: GameEngine, layout: UiLayout) {
        val theme = engine.currentTheme()
        val blend = engine.dayNightBlend()
        val skyTop = when (theme) {
            EnvironmentTheme.JUNGLE -> blendColor(Color.parseColor("#082E37"), Color.parseColor("#7FD8E6"), blend)
            EnvironmentTheme.RUINS -> blendColor(Color.parseColor("#1F1631"), Color.parseColor("#F2B86E"), blend)
            EnvironmentTheme.BRIDGE -> blendColor(Color.parseColor("#11242F"), Color.parseColor("#8AD0FF"), blend)
        }
        val skyBottom = when (theme) {
            EnvironmentTheme.JUNGLE -> blendColor(Color.parseColor("#173D25"), Color.parseColor("#FFE0A0"), blend)
            EnvironmentTheme.RUINS -> blendColor(Color.parseColor("#3C2832"), Color.parseColor("#FFD7A4"), blend)
            EnvironmentTheme.BRIDGE -> blendColor(Color.parseColor("#182C25"), Color.parseColor("#FFF0C0"), blend)
        }

        fillPaint.shader = LinearGradient(
            0f,
            0f,
            0f,
            layout.height.toFloat(),
            skyTop,
            skyBottom,
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, layout.width.toFloat(), layout.height.toFloat(), fillPaint)
        fillPaint.shader = null

        val sunColor = blendColor(Color.parseColor("#D8F2FF"), Color.parseColor("#FFE3A1"), blend)
        fillPaint.color = sunColor
        canvas.drawCircle(layout.width * 0.78f, layout.height * 0.18f, layout.height * 0.065f, fillPaint)
        fillPaint.color = Color.argb(35, 255, 255, 255)
        canvas.drawCircle(layout.width * 0.78f, layout.height * 0.18f, layout.height * 0.11f, fillPaint)
    }

    private fun drawParallax(canvas: Canvas, engine: GameEngine, layout: UiLayout) {
        val theme = engine.currentTheme()
        val horizon = layout.height * 0.25f
        val baseY = layout.height * 0.63f
        val scroll = engine.distanceInRun() * 0.26f

        fillPaint.color = when (theme) {
            EnvironmentTheme.JUNGLE -> Color.parseColor("#224A35")
            EnvironmentTheme.RUINS -> Color.parseColor("#584035")
            EnvironmentTheme.BRIDGE -> Color.parseColor("#294437")
        }

        var x = -(scroll % (layout.width * 0.45f)) - layout.width * 0.45f
        while (x < layout.width + layout.width * 0.45f) {
            canvas.drawOval(
                x,
                horizon + layout.height * 0.02f,
                x + layout.width * 0.62f,
                baseY,
                fillPaint
            )
            x += layout.width * 0.4f
        }

        fillPaint.color = when (theme) {
            EnvironmentTheme.JUNGLE -> Color.parseColor("#153423")
            EnvironmentTheme.RUINS -> Color.parseColor("#3F2D28")
            EnvironmentTheme.BRIDGE -> Color.parseColor("#183128")
        }
        x = -((scroll * 1.6f) % (layout.width * 0.25f)) - layout.width * 0.22f
        while (x < layout.width + layout.width * 0.22f) {
            when (theme) {
                EnvironmentTheme.JUNGLE -> {
                    canvas.drawRoundRect(
                        x,
                        horizon + layout.height * 0.05f,
                        x + layout.width * 0.08f,
                        baseY + layout.height * 0.05f,
                        layout.width * 0.02f,
                        layout.width * 0.02f,
                        fillPaint
                    )
                    fillPaint.color = Color.parseColor("#2D6A43")
                    canvas.drawCircle(x + layout.width * 0.04f, horizon + layout.height * 0.05f, layout.width * 0.08f, fillPaint)
                    fillPaint.color = Color.parseColor("#153423")
                }

                EnvironmentTheme.RUINS -> {
                    canvas.drawRect(x, horizon + layout.height * 0.02f, x + layout.width * 0.06f, baseY + layout.height * 0.04f, fillPaint)
                    fillPaint.color = Color.parseColor("#8F6A4A")
                    canvas.drawRect(x - layout.width * 0.01f, horizon + layout.height * 0.01f, x + layout.width * 0.07f, horizon + layout.height * 0.05f, fillPaint)
                    fillPaint.color = Color.parseColor("#3F2D28")
                }

                EnvironmentTheme.BRIDGE -> {
                    canvas.drawRect(x, horizon + layout.height * 0.07f, x + layout.width * 0.03f, baseY + layout.height * 0.02f, fillPaint)
                    canvas.drawRect(x + layout.width * 0.05f, horizon + layout.height * 0.07f, x + layout.width * 0.08f, baseY + layout.height * 0.02f, fillPaint)
                    canvas.drawRect(x, horizon + layout.height * 0.07f, x + layout.width * 0.08f, horizon + layout.height * 0.09f, fillPaint)
                }
            }
            x += layout.width * 0.2f
        }
    }

    private fun drawTrack(canvas: Canvas, engine: GameEngine, layout: UiLayout) {
        val topY = layout.height * 0.27f
        val bottomY = layout.height * 0.93f
        val nearWidth = layout.width * 0.92f
        val farWidth = layout.width * 0.22f
        val centerX = layout.width * 0.5f
        val edgeColor = when (engine.currentTheme()) {
            EnvironmentTheme.JUNGLE -> Color.parseColor("#503B23")
            EnvironmentTheme.RUINS -> Color.parseColor("#64432B")
            EnvironmentTheme.BRIDGE -> Color.parseColor("#70543B")
        }
        val roadColor = when (engine.currentTheme()) {
            EnvironmentTheme.JUNGLE -> Color.parseColor("#6A5130")
            EnvironmentTheme.RUINS -> Color.parseColor("#7B5A3E")
            EnvironmentTheme.BRIDGE -> Color.parseColor("#8B6947")
        }

        tempPath.reset()
        tempPath.moveTo(centerX - farWidth / 2f, topY)
        tempPath.lineTo(centerX + farWidth / 2f, topY)
        tempPath.lineTo(centerX + nearWidth / 2f, bottomY)
        tempPath.lineTo(centerX - nearWidth / 2f, bottomY)
        tempPath.close()
        fillPaint.color = edgeColor
        canvas.drawPath(tempPath, fillPaint)

        tempPath.reset()
        tempPath.moveTo(centerX - farWidth * 0.38f, topY + layout.height * 0.01f)
        tempPath.lineTo(centerX + farWidth * 0.38f, topY + layout.height * 0.01f)
        tempPath.lineTo(centerX + nearWidth * 0.38f, bottomY)
        tempPath.lineTo(centerX - nearWidth * 0.38f, bottomY)
        tempPath.close()
        fillPaint.color = roadColor
        canvas.drawPath(tempPath, fillPaint)

        fillPaint.color = Color.argb(130, 255, 232, 185)
        repeat(2) { divider ->
            val ratio = (divider + 1) / 3f
            val topX = GameMath.lerp(centerX - farWidth * 0.38f, centerX + farWidth * 0.38f, ratio)
            val bottomX = GameMath.lerp(centerX - nearWidth * 0.38f, centerX + nearWidth * 0.38f, ratio)
            var markerDepth = 0f
            while (markerDepth < 1f) {
                val nextDepth = (markerDepth + 0.09f).coerceAtMost(1f)
                val y1 = GameMath.lerp(topY + layout.height * 0.01f, bottomY, markerDepth)
                val y2 = GameMath.lerp(topY + layout.height * 0.01f, bottomY, nextDepth)
                val x1 = GameMath.lerp(topX, bottomX, markerDepth)
                val x2 = GameMath.lerp(topX, bottomX, nextDepth)
                strokePaint.color = Color.argb((80 + markerDepth * 100).toInt(), 255, 235, 200)
                strokePaint.strokeWidth = GameMath.lerp(1.5f * density, 8f * density, nextDepth)
                canvas.drawLine(x1, y1, x2, y2, strokePaint)
                markerDepth += 0.18f
            }
        }
    }

    private fun drawWorldItems(canvas: Canvas, engine: GameEngine, layout: UiLayout) {
        engine.obstacles.sortByDescending { it.z }
        engine.coins.sortByDescending { it.z }
        engine.powerUps.sortByDescending { it.z }
        engine.obstacles.forEach { drawObstacle(canvas, layout, it) }
        engine.coins.forEach { drawCoin(canvas, layout, it) }
        engine.powerUps.forEach { drawPowerUp(canvas, layout, it) }
    }

    private fun drawObstacle(canvas: Canvas, layout: UiLayout, obstacle: Obstacle) {
        val projection = project(layout, obstacle.laneFloat, obstacle.z)
        val shadowWidth = 28f * projection.scale * density
        canvas.drawOval(
            projection.x - shadowWidth,
            projection.y - 8f * density,
            projection.x + shadowWidth,
            projection.y + 8f * density,
            shadowPaint
        )

        when (obstacle.type) {
            ObstacleType.ROCK -> {
                fillPaint.color = Color.parseColor("#7E6551")
                canvas.drawRoundRect(projection.x - 26f * projection.scale, projection.y - 54f * projection.scale, projection.x + 28f * projection.scale, projection.y, 16f * projection.scale, 16f * projection.scale, fillPaint)
                fillPaint.color = Color.parseColor("#B19172")
                canvas.drawRoundRect(projection.x - 18f * projection.scale, projection.y - 42f * projection.scale, projection.x + 8f * projection.scale, projection.y - 14f * projection.scale, 14f * projection.scale, 14f * projection.scale, fillPaint)
            }

            ObstacleType.SPIKES -> {
                fillPaint.color = Color.parseColor("#CFD7D9")
                repeat(3) { index ->
                    tempPath.reset()
                    val offset = (index - 1) * 18f * projection.scale
                    tempPath.moveTo(projection.x + offset, projection.y - 58f * projection.scale)
                    tempPath.lineTo(projection.x - 12f * projection.scale + offset, projection.y)
                    tempPath.lineTo(projection.x + 12f * projection.scale + offset, projection.y)
                    tempPath.close()
                    canvas.drawPath(tempPath, fillPaint)
                }
            }

            ObstacleType.BOULDER -> {
                fillPaint.color = Color.parseColor("#86715F")
                canvas.drawCircle(projection.x, projection.y - 30f * projection.scale, 32f * projection.scale, fillPaint)
                fillPaint.color = Color.parseColor("#B09A84")
                canvas.drawCircle(projection.x - 10f * projection.scale, projection.y - 40f * projection.scale, 10f * projection.scale, fillPaint)
            }

            ObstacleType.SWING_TRAP -> {
                fillPaint.color = Color.parseColor("#6F5134")
                canvas.drawRect(projection.x - 4f * projection.scale, projection.y - 94f * projection.scale, projection.x + 4f * projection.scale, projection.y, fillPaint)
                strokePaint.color = Color.parseColor("#C9D2D5")
                strokePaint.strokeWidth = 3f * projection.scale
                canvas.drawLine(projection.x, projection.y - 90f * projection.scale, projection.x + 28f * projection.scale, projection.y - 38f * projection.scale, strokePaint)
                fillPaint.color = Color.parseColor("#D4DBDE")
                canvas.drawCircle(projection.x + 35f * projection.scale, projection.y - 30f * projection.scale, 16f * projection.scale, fillPaint)
            }

            ObstacleType.GAP -> {
                fillPaint.color = Color.parseColor("#1C1209")
                canvas.drawOval(projection.x - 42f * projection.scale, projection.y - 8f * projection.scale, projection.x + 42f * projection.scale, projection.y + 18f * projection.scale, fillPaint)
                fillPaint.color = Color.parseColor("#8F6A43")
                canvas.drawRect(projection.x - 48f * projection.scale, projection.y - 3f * projection.scale, projection.x - 14f * projection.scale, projection.y + 5f * projection.scale, fillPaint)
                canvas.drawRect(projection.x + 14f * projection.scale, projection.y - 3f * projection.scale, projection.x + 48f * projection.scale, projection.y + 5f * projection.scale, fillPaint)
            }

            ObstacleType.LOW_BRANCH -> {
                fillPaint.color = Color.parseColor("#6B482B")
                canvas.drawRect(projection.x - 6f * projection.scale, projection.y - 72f * projection.scale, projection.x + 6f * projection.scale, projection.y, fillPaint)
                canvas.drawRoundRect(projection.x - 48f * projection.scale, projection.y - 70f * projection.scale, projection.x + 50f * projection.scale, projection.y - 48f * projection.scale, 12f * projection.scale, 12f * projection.scale, fillPaint)
                fillPaint.color = Color.parseColor("#2F7F4A")
                canvas.drawCircle(projection.x - 34f * projection.scale, projection.y - 72f * projection.scale, 16f * projection.scale, fillPaint)
                canvas.drawCircle(projection.x + 34f * projection.scale, projection.y - 72f * projection.scale, 14f * projection.scale, fillPaint)
            }
        }
    }

    private fun drawCoin(canvas: Canvas, layout: UiLayout, coin: Coin) {
        val projection = project(layout, coin.laneIndex.toFloat(), coin.z, coin.verticalArc)
        fillPaint.color = Color.argb(80, 0, 0, 0)
        canvas.drawOval(projection.x - 16f * projection.scale, projection.y + 10f * projection.scale, projection.x + 16f * projection.scale, projection.y + 20f * projection.scale, fillPaint)

        fillPaint.color = Color.parseColor("#F6C853")
        canvas.drawCircle(projection.x, projection.y - 20f * projection.scale, 18f * projection.scale, fillPaint)
        strokePaint.color = Color.parseColor("#FFF4B1")
        strokePaint.strokeWidth = 2f * projection.scale
        canvas.drawCircle(projection.x, projection.y - 20f * projection.scale, 12f * projection.scale, strokePaint)
        fillPaint.color = Color.argb((120 + sin(coin.sparkle) * 70).toInt().coerceIn(50, 220), 255, 249, 194)
        canvas.drawCircle(projection.x + 12f * projection.scale, projection.y - 32f * projection.scale, 5f * projection.scale, fillPaint)
    }

    private fun drawPowerUp(canvas: Canvas, layout: UiLayout, powerUp: PowerUp) {
        val projection = project(layout, powerUp.laneIndex.toFloat(), powerUp.z, 0.92f)
        val color = when (powerUp.type) {
            PowerUpType.MAGNET -> Color.parseColor("#FF6B6B")
            PowerUpType.SHIELD -> Color.parseColor("#6EC7FF")
            PowerUpType.BOOST -> Color.parseColor("#FFAE42")
            PowerUpType.DOUBLE_SCORE -> Color.parseColor("#E48FFF")
        }

        canvas.drawOval(projection.x - 18f * projection.scale, projection.y + 14f * projection.scale, projection.x + 18f * projection.scale, projection.y + 24f * projection.scale, shadowPaint)
        fillPaint.color = color
        tempPath.reset()
        tempPath.moveTo(projection.x, projection.y - 42f * projection.scale)
        tempPath.lineTo(projection.x - 22f * projection.scale, projection.y - 18f * projection.scale)
        tempPath.lineTo(projection.x, projection.y + 4f * projection.scale)
        tempPath.lineTo(projection.x + 22f * projection.scale, projection.y - 18f * projection.scale)
        tempPath.close()
        canvas.drawPath(tempPath, fillPaint)

        chipTextPaint.color = Color.WHITE
        val label = when (powerUp.type) {
            PowerUpType.MAGNET -> "M"
            PowerUpType.SHIELD -> "S"
            PowerUpType.BOOST -> "B"
            PowerUpType.DOUBLE_SCORE -> "2X"
        }
        canvas.drawText(label, projection.x, projection.y - 8f * projection.scale, chipTextPaint)
    }

    private fun drawPlayer(canvas: Canvas, engine: GameEngine, layout: UiLayout) {
        val player = engine.player
        val x = layout.width * 0.5f + (player.lanePosition - 1f) * layout.width * if (layout.width > layout.height) 0.13f else 0.18f
        val groundY = layout.height * 0.83f
        val jumpOffset = player.height * layout.height * 0.07f
        val scale = if (layout.width > layout.height) 1f else 0.92f
        val slideFactor = if (player.isSliding()) 0.56f else 1f
        val palette = player.character?.palette ?: GameContent.characters.first().palette

        canvas.drawOval(x - 52f * scale, groundY - 12f, x + 52f * scale, groundY + 12f, shadowPaint)

        if (player.hasBoost()) {
            repeat(3) { index ->
                fillPaint.color = Color.argb(40 - index * 10, 255, 215, 120)
                val trailShift = (index + 1) * 16f * scale
                drawPlayerBody(canvas, x - trailShift, groundY - jumpOffset, scale * (1f - index * 0.04f), slideFactor, player, palette.skinColor, fillPaint.color, Color.argb(30, 255, 255, 255), palette.hairColor, true)
            }
        }

        if (player.isShielded()) {
            fillPaint.color = Color.argb(50, 110, 215, 255)
            canvas.drawCircle(x, groundY - jumpOffset - 94f * scale, 72f * scale, fillPaint)
        }
        if (player.hasMagnet()) {
            strokePaint.color = Color.argb(110, 255, 218, 120)
            strokePaint.strokeWidth = 3f * density
            canvas.drawCircle(x, groundY - jumpOffset - 94f * scale, 94f * scale, strokePaint)
        }

        drawPlayerBody(canvas, x, groundY - jumpOffset, scale, slideFactor, player, palette.skinColor, palette.primaryColor, palette.accentColor, palette.hairColor, false)
    }

    private fun drawPlayerBody(
        canvas: Canvas,
        x: Float,
        y: Float,
        scale: Float,
        slideFactor: Float,
        player: Player,
        paletteSkin: Int,
        palettePrimary: Int,
        paletteAccent: Int,
        paletteHair: Int,
        ghostAlpha: Boolean
    ) {
        val alphaMultiplier = if (ghostAlpha) 0.45f else 1f
        val bodyHeight = 118f * scale * slideFactor
        val squat = 1f + player.landingSquash * 0.12f
        val runSwing = sin(player.animationTime * 9.2f) * 16f * scale * if (player.height > 0.05f) 0.2f else 1f

        fillPaint.color = applyAlpha(palettePrimary, alphaMultiplier)
        canvas.drawRoundRect(x - 30f * scale, y - bodyHeight, x + 30f * scale, y - 32f * scale, 18f * scale, 18f * scale, fillPaint)
        fillPaint.color = applyAlpha(paletteAccent, alphaMultiplier)
        canvas.drawRoundRect(x - 34f * scale, y - bodyHeight + 18f * scale, x + 34f * scale, y - bodyHeight + 34f * scale, 12f * scale, 12f * scale, fillPaint)

        fillPaint.color = applyAlpha(paletteSkin, alphaMultiplier)
        canvas.drawCircle(x, y - bodyHeight - 18f * scale, 22f * scale, fillPaint)
        fillPaint.color = applyAlpha(paletteHair, alphaMultiplier)
        canvas.drawArc(x - 22f * scale, y - bodyHeight - 40f * scale, x + 22f * scale, y - bodyHeight + 2f * scale, 180f, 180f, true, fillPaint)

        fillPaint.color = applyAlpha(paletteAccent, alphaMultiplier)
        canvas.drawRoundRect(x + 18f * scale, y - bodyHeight + 26f * scale, x + 44f * scale, y - bodyHeight + 78f * scale, 10f * scale, 10f * scale, fillPaint)

        fillPaint.color = applyAlpha(palettePrimary, alphaMultiplier)
        canvas.drawRoundRect(x - 44f * scale, y - bodyHeight + 18f * scale, x - 18f * scale, y - bodyHeight + 34f * scale, 10f * scale, 10f * scale, fillPaint)
        canvas.drawRoundRect(x + 18f * scale, y - bodyHeight + 18f * scale, x + 44f * scale, y - bodyHeight + 34f * scale, 10f * scale, 10f * scale, fillPaint)

        fillPaint.color = applyAlpha(paletteSkin, alphaMultiplier)
        canvas.drawRoundRect(x - 44f * scale, y - bodyHeight + 32f * scale, x - 24f * scale, y - bodyHeight + 78f * scale + runSwing * 0.2f, 10f * scale, 10f * scale, fillPaint)
        canvas.drawRoundRect(x + 24f * scale, y - bodyHeight + 32f * scale, x + 44f * scale, y - bodyHeight + 78f * scale - runSwing * 0.2f, 10f * scale, 10f * scale, fillPaint)

        fillPaint.color = applyAlpha(palettePrimary, alphaMultiplier)
        canvas.drawRoundRect(x - 24f * scale, y - 34f * scale * squat, x - 4f * scale, y, 10f * scale, 10f * scale, fillPaint)
        canvas.drawRoundRect(x + 4f * scale, y - 34f * scale * squat, x + 24f * scale, y, 10f * scale, 10f * scale, fillPaint)

        fillPaint.color = applyAlpha(Color.parseColor("#2D2B2A"), alphaMultiplier)
        canvas.drawRoundRect(x - 28f * scale, y - 10f * scale, x - 2f * scale, y + 8f * scale, 10f * scale, 10f * scale, fillPaint)
        canvas.drawRoundRect(x + 2f * scale, y - 10f * scale, x + 28f * scale, y + 8f * scale, 10f * scale, 10f * scale, fillPaint)

        fillPaint.color = applyAlpha(paletteAccent, alphaMultiplier)
        tempPath.reset()
        tempPath.moveTo(x - 8f * scale, y - bodyHeight + 34f * scale)
        tempPath.lineTo(x + 4f * scale, y - bodyHeight + 44f * scale)
        tempPath.lineTo(x - 18f * scale, y - bodyHeight + 74f * scale + runSwing * 0.12f)
        tempPath.close()
        canvas.drawPath(tempPath, fillPaint)
    }

    private fun drawParticles(canvas: Canvas, engine: GameEngine) {
        engine.particles.forEach { particle ->
            particlePaint.color = Color.argb(particle.alpha(), Color.red(particle.color), Color.green(particle.color), Color.blue(particle.color))
            canvas.drawCircle(particle.x, particle.y, particle.size, particlePaint)
        }
    }

    private fun drawHud(canvas: Canvas, engine: GameEngine, layout: UiLayout) {
        fillPaint.color = Color.argb(110, 9, 12, 16)
        canvas.drawRoundRect(layout.hudProgressBar, 18f * density, 18f * density, fillPaint)
        fillPaint.color = Color.parseColor("#F3C66A")
        canvas.drawRoundRect(layout.hudProgressBar.left, layout.hudProgressBar.top, GameMath.lerp(layout.hudProgressBar.left, layout.hudProgressBar.right, ((engine.distanceInRun() % GameConstants.ZONE_LENGTH_METERS) / GameConstants.ZONE_LENGTH_METERS).coerceIn(0f, 1f)), layout.hudProgressBar.bottom, 18f * density, 18f * density, fillPaint)

        hudPaint.color = Color.WHITE
        canvas.drawText("Score ${engine.scoreInRun()}", layout.hudProgressBar.left, layout.hudProgressBar.top - layout.height * 0.012f, hudPaint)
        canvas.drawText("Coins ${engine.coinsInRun()}", layout.hudProgressBar.left, layout.hudProgressBar.bottom + layout.height * 0.05f, hudPaint)
        canvas.drawText("Distance ${engine.distanceInRun().toInt()}m", layout.hudProgressBar.left + layout.width * 0.28f, layout.hudProgressBar.bottom + layout.height * 0.05f, hudPaint)

        chipTextPaint.color = Color.WHITE
        fillPaint.color = Color.argb(120, 8, 10, 14)
        canvas.drawRoundRect(layout.pauseButton, layout.pauseButton.height() * 0.38f, layout.pauseButton.height() * 0.38f, fillPaint)
        strokePaint.color = Color.argb(100, 255, 235, 190)
        canvas.drawRoundRect(layout.pauseButton, layout.pauseButton.height() * 0.38f, layout.pauseButton.height() * 0.38f, strokePaint)
        fillPaint.color = Color.WHITE
        val barWidth = layout.pauseButton.width() * 0.12f
        canvas.drawRoundRect(layout.pauseButton.left + layout.pauseButton.width() * 0.32f, layout.pauseButton.top + layout.pauseButton.height() * 0.26f, layout.pauseButton.left + layout.pauseButton.width() * 0.32f + barWidth, layout.pauseButton.bottom - layout.pauseButton.height() * 0.26f, barWidth, barWidth, fillPaint)
        canvas.drawRoundRect(layout.pauseButton.right - layout.pauseButton.width() * 0.32f - barWidth, layout.pauseButton.top + layout.pauseButton.height() * 0.26f, layout.pauseButton.right - layout.pauseButton.width() * 0.32f, layout.pauseButton.bottom - layout.pauseButton.height() * 0.26f, barWidth, barWidth, fillPaint)

        drawPowerChip(canvas, layout, layout.hudProgressBar.right - layout.width * 0.22f, layout.height * 0.11f, "Shield", engine.player.shieldTimer, Color.parseColor("#7FD5FF"))
        drawPowerChip(canvas, layout, layout.hudProgressBar.right - layout.width * 0.11f, layout.height * 0.11f, "Boost", engine.player.boostTimer, Color.parseColor("#FFBC63"))
    }

    private fun drawPowerChip(canvas: Canvas, layout: UiLayout, centerX: Float, centerY: Float, label: String, timeLeft: Float, color: Int) {
        if (timeLeft <= 0f) {
            return
        }
        val rect = RectF(centerX - layout.width * 0.047f, centerY - layout.height * 0.03f, centerX + layout.width * 0.047f, centerY + layout.height * 0.03f)
        fillPaint.color = Color.argb(120, 8, 10, 14)
        canvas.drawRoundRect(rect, 18f * density, 18f * density, fillPaint)
        strokePaint.color = color
        canvas.drawRoundRect(rect, 18f * density, 18f * density, strokePaint)
        chipTextPaint.color = color
        canvas.drawText("$label ${timeLeft.toInt() + 1}", rect.centerX(), GameMath.centerTextY(chipTextPaint, rect), chipTextPaint)
    }

    private fun drawHome(canvas: Canvas, engine: GameEngine, layout: UiLayout) {
        val hero = engine.selectedCharacter()
        val daily = engine.dailyRewardInfo()
        titlePaint.color = Color.WHITE
        canvas.drawText("Relic Rush", layout.width * 0.5f, layout.height * 0.11f, titlePaint)
        textPaint.color = Color.parseColor("#F3E6C7")
        canvas.drawText("Temple sprinting with real progression, rewards, and lane-switch action.", layout.width * 0.5f, layout.height * 0.16f, textPaint)

        drawPanel(canvas, layout.heroPanel, Color.argb(165, 13, 18, 20), Color.argb(120, 255, 221, 148))
        bodyPaint.color = Color.WHITE
        bodyPaint.isFakeBoldText = true
        canvas.drawText("Hero: ${hero.name}  •  ${hero.title}", layout.heroPanel.left + layout.width * 0.04f, layout.heroPanel.top + layout.height * 0.07f, bodyPaint)
        bodyPaint.isFakeBoldText = false
        smallPaint.color = Color.parseColor("#F3E6C7")
        canvas.drawText("Level ${engine.playerLevel()}  •  Vault ${engine.coinBank} coins", layout.heroPanel.left + layout.width * 0.04f, layout.heroPanel.top + layout.height * 0.12f, smallPaint)
        canvas.drawText("High Score ${engine.highScore}  •  Lifetime ${engine.totalDistance}m", layout.heroPanel.left + layout.width * 0.04f, layout.heroPanel.top + layout.height * 0.16f, smallPaint)
        drawStatBar(canvas, layout.heroPanel.left + layout.width * 0.04f, layout.heroPanel.top + layout.height * 0.21f, layout.width * 0.34f, "Adventure Level", engine.progressToNextLevel(), Color.parseColor("#F3C66A"))
        drawStatBar(canvas, layout.heroPanel.left + layout.width * 0.04f, layout.heroPanel.top + layout.height * 0.29f, layout.width * 0.34f, "Mission Progress", engine.progressToNextMission(), Color.parseColor("#8EE0A1"))
        drawHeroPortrait(canvas, layout.heroPanel.right - layout.width * 0.18f, layout.heroPanel.top + layout.height * 0.22f, layout.height * 0.18f, hero.palette)

        drawPanel(canvas, layout.missionPanel, Color.argb(160, 15, 20, 24), Color.argb(120, 115, 211, 255))
        bodyPaint.color = Color.WHITE
        bodyPaint.isFakeBoldText = true
        canvas.drawText(engine.currentMission().title, layout.missionPanel.left + layout.width * 0.04f, layout.missionPanel.top + layout.height * 0.055f, bodyPaint)
        bodyPaint.isFakeBoldText = false
        smallPaint.color = Color.parseColor("#E9E1CB")
        canvas.drawText(engine.currentMission().description, layout.missionPanel.left + layout.width * 0.04f, layout.missionPanel.top + layout.height * 0.097f, smallPaint)
        canvas.drawText("Reward ${engine.currentMission().rewardCoins} coins", layout.missionPanel.left + layout.width * 0.04f, layout.missionPanel.top + layout.height * 0.14f, smallPaint)
        drawProgressBar(canvas, RectF(layout.missionPanel.left + layout.width * 0.04f, layout.missionPanel.bottom - layout.height * 0.05f, layout.missionPanel.right - layout.width * 0.04f, layout.missionPanel.bottom - layout.height * 0.024f), engine.progressToNextMission(), Color.parseColor("#6DDA86"))

        drawPanel(canvas, layout.storePanel, Color.argb(155, 17, 20, 22), Color.argb(120, 255, 206, 124))
        bodyPaint.color = Color.WHITE
        bodyPaint.isFakeBoldText = true
        canvas.drawText("Daily Reward Day ${daily.streakDay}", layout.storePanel.left + layout.width * 0.04f, layout.storePanel.top + layout.height * 0.05f, bodyPaint)
        bodyPaint.isFakeBoldText = false
        smallPaint.color = Color.parseColor("#F3E6C7")
        canvas.drawText(if (daily.canClaim) "Ready now: ${daily.rewardCoins} coins" else "Already claimed today. Come back tomorrow.", layout.storePanel.left + layout.width * 0.04f, layout.storePanel.top + layout.height * 0.095f, smallPaint)

        drawButton(canvas, layout.primaryButton, "Start Adventure", Color.parseColor("#F3C66A"), Color.parseColor("#1B140E"), true)
        drawButton(canvas, layout.secondaryButton, "Choose Heroes", Color.parseColor("#5B87B0"), Color.WHITE, false)
        drawButton(canvas, layout.tertiaryButton, if (daily.canClaim) "Claim Daily Reward" else "Daily Reward Claimed", if (daily.canClaim) Color.parseColor("#69C27D") else Color.parseColor("#42534A"), Color.WHITE, false)
        drawButton(canvas, layout.leftButton, if (engine.removeAdsPurchased) "Ads Removed" else "Remove Ads", if (engine.removeAdsPurchased) Color.parseColor("#6B785E") else Color.parseColor("#34495E"), Color.WHITE, false)
        drawButton(canvas, layout.rightButton, "Buy ${GameConstants.COIN_PACK_REWARD} Coins", Color.parseColor("#8D5DA7"), Color.WHITE, false)
    }

    private fun drawCharacterSelect(canvas: Canvas, engine: GameEngine, layout: UiLayout) {
        titlePaint.color = Color.WHITE
        canvas.drawText("Choose Your Runner", layout.width * 0.5f, layout.height * 0.11f, titlePaint)
        textPaint.color = Color.parseColor("#F3E6C7")
        canvas.drawText("Each hero has a slightly different speed and jump profile.", layout.width * 0.5f, layout.height * 0.16f, textPaint)

        GameContent.characters.forEachIndexed { index, hero ->
            val rect = layout.characterCards[index]
            val isSelected = index == engine.characterPreviewIndex
            val unlocked = engine.unlockedCharacters.contains(hero.id)
            drawPanel(canvas, rect, if (isSelected) Color.argb(190, 19, 26, 30) else Color.argb(145, 12, 15, 18), if (isSelected) hero.palette.glowColor else Color.argb(80, 255, 255, 255))
            drawHeroPortrait(canvas, rect.centerX(), rect.top + rect.height() * 0.36f, rect.height() * 0.36f, hero.palette)
            bodyPaint.color = Color.WHITE
            bodyPaint.isFakeBoldText = true
            bodyPaint.textAlign = Paint.Align.CENTER
            canvas.drawText(hero.name, rect.centerX(), rect.top + rect.height() * 0.66f, bodyPaint)
            bodyPaint.isFakeBoldText = false
            smallPaint.color = Color.parseColor("#F3E6C7")
            smallPaint.textAlign = Paint.Align.CENTER
            canvas.drawText(hero.title, rect.centerX(), rect.top + rect.height() * 0.75f, smallPaint)
            canvas.drawText(if (unlocked) "Unlocked" else "${hero.unlockCost} coins", rect.centerX(), rect.top + rect.height() * 0.84f, smallPaint)
            smallPaint.textAlign = Paint.Align.LEFT
            bodyPaint.textAlign = Paint.Align.LEFT
        }

        val preview = engine.previewCharacter()
        drawButton(canvas, layout.characterActionButton, if (engine.unlockedCharacters.contains(preview.id)) "Select ${preview.name}" else "Unlock ${preview.name} • ${preview.unlockCost} coins", if (engine.unlockedCharacters.contains(preview.id)) preview.palette.glowColor else Color.parseColor("#F3C66A"), Color.parseColor("#1B140E"), true)
        drawButton(canvas, layout.characterBackButton, "Back To Camp", Color.parseColor("#3F5360"), Color.WHITE, false)
    }

    private fun drawPauseOverlay(canvas: Canvas, engine: GameEngine, layout: UiLayout) {
        canvas.drawColor(Color.argb(120, 5, 7, 10))
        drawPanel(canvas, layout.heroPanel, Color.argb(200, 14, 18, 20), Color.argb(150, 255, 221, 148))
        titlePaint.color = Color.WHITE
        canvas.drawText("Camp Pause", layout.width * 0.5f, layout.heroPanel.top + layout.height * 0.09f, titlePaint)
        textPaint.color = Color.parseColor("#F3E6C7")
        canvas.drawText("Tune the run, then jump back into the temple path.", layout.width * 0.5f, layout.heroPanel.top + layout.height * 0.15f, textPaint)
        drawToggle(canvas, layout.musicToggle, "Music", engine.isMusicEnabled())
        drawToggle(canvas, layout.sfxToggle, "Sound Effects", engine.isSfxEnabled())
        drawButton(canvas, layout.primaryButton, "Resume", Color.parseColor("#F3C66A"), Color.parseColor("#1B140E"), true)
        drawButton(canvas, layout.secondaryButton, "Back Home", Color.parseColor("#3F5360"), Color.WHITE, false)
    }

    private fun drawGameOver(canvas: Canvas, engine: GameEngine, layout: UiLayout) {
        canvas.drawColor(Color.argb(120, 8, 6, 6))
        drawPanel(canvas, layout.heroPanel, Color.argb(205, 18, 12, 10), Color.argb(140, 255, 157, 115))
        titlePaint.color = Color.WHITE
        canvas.drawText("Run Over", layout.width * 0.5f, layout.heroPanel.top + layout.height * 0.09f, titlePaint)
        textPaint.color = Color.parseColor("#F3E6C7")
        canvas.drawText("You made it ${engine.distanceInRun().toInt()}m and banked ${engine.coinsInRun()} coins this run.", layout.width * 0.5f, layout.heroPanel.top + layout.height * 0.15f, textPaint)
        bodyPaint.color = Color.WHITE
        bodyPaint.textAlign = Paint.Align.CENTER
        bodyPaint.isFakeBoldText = true
        canvas.drawText("Score ${engine.scoreInRun()}  •  Best ${engine.highScore}", layout.width * 0.5f, layout.heroPanel.top + layout.height * 0.23f, bodyPaint)
        bodyPaint.isFakeBoldText = false
        bodyPaint.textAlign = Paint.Align.LEFT
        drawProgressBar(canvas, RectF(layout.heroPanel.left + layout.width * 0.08f, layout.heroPanel.bottom - layout.height * 0.1f, layout.heroPanel.right - layout.width * 0.08f, layout.heroPanel.bottom - layout.height * 0.07f), engine.progressToNextMission(), Color.parseColor("#6DDA86"))
        drawButton(canvas, layout.restartButton, "Run Again", Color.parseColor("#F3C66A"), Color.parseColor("#1B140E"), true)
        drawButton(canvas, layout.homeButton, "Back Home", Color.parseColor("#3F5360"), Color.WHITE, false)
        drawButton(canvas, layout.reviveButton, if (engine.canRevive()) "Watch Ad To Revive" else "Revive Used", if (engine.canRevive()) Color.parseColor("#6DDA86") else Color.parseColor("#45524A"), Color.WHITE, false)
    }

    private fun drawRuntimeMessage(canvas: Canvas, engine: GameEngine, layout: UiLayout) {
        val message = engine.statusMessage ?: return
        val rect = RectF(layout.width * 0.17f, layout.height * 0.03f, layout.width * 0.83f, layout.height * 0.095f)
        fillPaint.color = Color.argb(180, 9, 12, 15)
        canvas.drawRoundRect(rect, 18f * density, 18f * density, fillPaint)
        strokePaint.color = message.kindColor
        canvas.drawRoundRect(rect, 18f * density, 18f * density, strokePaint)
        chipTextPaint.color = Color.WHITE
        canvas.drawText(message.text, rect.centerX(), GameMath.centerTextY(chipTextPaint, rect), chipTextPaint)
    }

    private fun drawPanel(canvas: Canvas, rect: RectF, fillColor: Int, strokeColor: Int) {
        fillPaint.color = fillColor
        canvas.drawRoundRect(rect, 26f * density, 26f * density, fillPaint)
        strokePaint.color = strokeColor
        canvas.drawRoundRect(rect, 26f * density, 26f * density, strokePaint)
    }

    private fun drawButton(canvas: Canvas, rect: RectF, label: String, color: Int, textColor: Int, prominent: Boolean) {
        fillPaint.color = color
        canvas.drawRoundRect(rect, 22f * density, 22f * density, fillPaint)
        strokePaint.color = if (prominent) Color.argb(120, 255, 244, 222) else Color.argb(110, 255, 255, 255)
        canvas.drawRoundRect(rect, 22f * density, 22f * density, strokePaint)
        chipTextPaint.color = textColor
        canvas.drawText(label, rect.centerX(), GameMath.centerTextY(chipTextPaint, rect), chipTextPaint)
    }

    private fun drawToggle(canvas: Canvas, rect: RectF, label: String, enabled: Boolean) {
        drawPanel(canvas, rect, Color.argb(160, 12, 16, 18), Color.argb(120, 255, 221, 148))
        bodyPaint.color = Color.WHITE
        bodyPaint.isFakeBoldText = true
        canvas.drawText(label, rect.left + rect.width() * 0.08f, rect.centerY() + bodyPaint.textSize * 0.18f, bodyPaint)
        bodyPaint.isFakeBoldText = false
        val pill = RectF(rect.right - rect.width() * 0.28f, rect.top + rect.height() * 0.18f, rect.right - rect.width() * 0.06f, rect.bottom - rect.height() * 0.18f)
        fillPaint.color = if (enabled) Color.parseColor("#6DDA86") else Color.parseColor("#565F64")
        canvas.drawRoundRect(pill, pill.height() * 0.5f, pill.height() * 0.5f, fillPaint)
        chipTextPaint.color = Color.WHITE
        canvas.drawText(if (enabled) "On" else "Off", pill.centerX(), GameMath.centerTextY(chipTextPaint, pill), chipTextPaint)
    }

    private fun drawProgressBar(canvas: Canvas, rect: RectF, progress: Float, color: Int) {
        fillPaint.color = Color.argb(150, 11, 14, 18)
        canvas.drawRoundRect(rect, 14f * density, 14f * density, fillPaint)
        fillPaint.color = color
        canvas.drawRoundRect(rect.left, rect.top, GameMath.lerp(rect.left, rect.right, progress), rect.bottom, 14f * density, 14f * density, fillPaint)
    }

    private fun drawStatBar(canvas: Canvas, left: Float, top: Float, width: Float, label: String, progress: Float, color: Int) {
        smallPaint.color = Color.parseColor("#F3E6C7")
        canvas.drawText(label, left, top, smallPaint)
        drawProgressBar(canvas, RectF(left, top + 12f * density, left + width, top + 32f * density), progress, color)
    }

    private fun drawHeroPortrait(canvas: Canvas, centerX: Float, centerY: Float, size: Float, palette: CharacterPalette) {
        fillPaint.color = Color.argb(70, 0, 0, 0)
        canvas.drawCircle(centerX, centerY + size * 0.48f, size * 0.36f, fillPaint)
        fillPaint.color = palette.primaryColor
        canvas.drawRoundRect(centerX - size * 0.26f, centerY - size * 0.02f, centerX + size * 0.26f, centerY + size * 0.46f, size * 0.12f, size * 0.12f, fillPaint)
        fillPaint.color = palette.skinColor
        canvas.drawCircle(centerX, centerY - size * 0.24f, size * 0.2f, fillPaint)
        fillPaint.color = palette.hairColor
        canvas.drawArc(centerX - size * 0.2f, centerY - size * 0.42f, centerX + size * 0.2f, centerY - size * 0.06f, 180f, 180f, true, fillPaint)
        fillPaint.color = palette.accentColor
        canvas.drawRoundRect(centerX - size * 0.3f, centerY - size * 0.01f, centerX + size * 0.3f, centerY + size * 0.08f, size * 0.08f, size * 0.08f, fillPaint)
        canvas.drawRoundRect(centerX + size * 0.12f, centerY + size * 0.06f, centerX + size * 0.34f, centerY + size * 0.34f, size * 0.08f, size * 0.08f, fillPaint)
    }

    private fun project(layout: UiLayout, lanePosition: Float, z: Float, verticalHeight: Float = 0f): Projection {
        val depth = (1f - (z / GameConstants.FAR_Z)).coerceIn(0f, 1f)
        val eased = GameMath.easeOutCubic(depth)
        val horizonY = layout.height * 0.28f
        val groundY = layout.height * 0.88f
        val laneSpread = GameMath.lerp(layout.width * 0.08f, layout.width * 0.29f, eased)
        val x = layout.width * 0.5f + (lanePosition - 1f) * laneSpread
        val y = GameMath.lerp(horizonY, groundY, eased) - verticalHeight * layout.height * 0.09f * (0.3f + eased)
        val scale = GameMath.lerp(0.28f, 1.12f, eased)
        return Projection(x, y, scale, depth)
    }

    private fun blendColor(start: Int, end: Int, amount: Float): Int {
        val t = amount.coerceIn(0f, 1f)
        return Color.argb(
            GameMath.lerp(Color.alpha(start).toFloat(), Color.alpha(end).toFloat(), t).toInt(),
            GameMath.lerp(Color.red(start).toFloat(), Color.red(end).toFloat(), t).toInt(),
            GameMath.lerp(Color.green(start).toFloat(), Color.green(end).toFloat(), t).toInt(),
            GameMath.lerp(Color.blue(start).toFloat(), Color.blue(end).toFloat(), t).toInt()
        )
    }

    private fun applyAlpha(color: Int, alphaMultiplier: Float): Int {
        return Color.argb((Color.alpha(color) * alphaMultiplier).toInt().coerceIn(0, 255), Color.red(color), Color.green(color), Color.blue(color))
    }
}
