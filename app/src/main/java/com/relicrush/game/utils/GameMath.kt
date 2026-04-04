package com.relicrush.game.utils

import android.content.Context
import android.graphics.Paint
import android.graphics.RectF
import kotlin.math.max
import kotlin.math.min

object GameMath {
    fun clamp(value: Float, minValue: Float, maxValue: Float): Float {
        return max(minValue, min(value, maxValue))
    }

    fun lerp(start: Float, end: Float, amount: Float): Float {
        return start + (end - start) * amount
    }

    fun smoothStep(start: Float, end: Float, amount: Float): Float {
        val t = clamp(amount, 0f, 1f)
        val eased = t * t * (3f - 2f * t)
        return lerp(start, end, eased)
    }

    fun easeOutCubic(value: Float): Float {
        val t = 1f - clamp(value, 0f, 1f)
        return 1f - t * t * t
    }

    fun dp(context: Context, value: Float): Float {
        return value * context.resources.displayMetrics.density
    }

    fun centerTextY(paint: Paint, rect: RectF): Float {
        return rect.centerY() - (paint.descent() + paint.ascent()) / 2f
    }

    fun todayKey(): String {
        return java.time.LocalDate.now().toString()
    }

    fun formatMeters(distance: Float): String {
        return "${distance.toInt()}m"
    }

    fun formatCoins(coins: Int): String {
        return coins.toString()
    }
}
