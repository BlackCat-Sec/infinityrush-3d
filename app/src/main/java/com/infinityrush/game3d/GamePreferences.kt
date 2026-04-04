package com.infinityrush.game3d

import android.content.Context

object GamePreferences {
    private const val PREFS_NAME = "infinity_rush_3d_prefs"
    private const val HIGH_SCORE_KEY = "high_score"
    private const val MUSIC_ENABLED_KEY = "music_enabled"
    private const val SFX_ENABLED_KEY = "sfx_enabled"

    fun getHighScore(context: Context): Int {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(HIGH_SCORE_KEY, 0)
    }

    fun saveHighScore(context: Context, highScore: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(HIGH_SCORE_KEY, highScore)
            .apply()
    }

    fun isMusicEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(MUSIC_ENABLED_KEY, true)
    }

    fun saveMusicEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(MUSIC_ENABLED_KEY, enabled)
            .apply()
    }

    fun isSfxEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(SFX_ENABLED_KEY, true)
    }

    fun saveSfxEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(SFX_ENABLED_KEY, enabled)
            .apply()
    }
}

