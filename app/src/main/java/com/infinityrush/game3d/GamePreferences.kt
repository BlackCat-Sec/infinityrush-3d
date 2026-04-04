package com.infinityrush.game3d

import android.content.Context

object GamePreferences {
    private const val PREFS_NAME = "infinity_rush_3d_prefs"
    private const val HIGH_SCORE_KEY = "high_score"
    private const val TOTAL_COINS_KEY = "total_coins"
    private const val MUSIC_ENABLED_KEY = "music_enabled"
    private const val SFX_ENABLED_KEY = "sfx_enabled"
    private const val SELECTED_HERO_KEY = "selected_hero"
    private const val MISSION_INDEX_KEY = "mission_index"
    private const val MISSION_PROGRESS_KEY = "mission_progress"

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

    fun getTotalCoins(context: Context): Int {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(TOTAL_COINS_KEY, 0)
    }

    fun saveTotalCoins(context: Context, totalCoins: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(TOTAL_COINS_KEY, totalCoins)
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

    fun getSelectedHero(context: Context): RunnerCharacter {
        val id = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(SELECTED_HERO_KEY, RunnerCharacter.ARIA.id)
        return RunnerCharacter.fromId(id)
    }

    fun saveSelectedHero(context: Context, hero: RunnerCharacter) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(SELECTED_HERO_KEY, hero.id)
            .apply()
    }

    fun getMissionIndex(context: Context): Int {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(MISSION_INDEX_KEY, 0)
    }

    fun saveMissionIndex(context: Context, missionIndex: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(MISSION_INDEX_KEY, missionIndex)
            .apply()
    }

    fun getMissionProgress(context: Context): Int {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(MISSION_PROGRESS_KEY, 0)
    }

    fun saveMissionProgress(context: Context, missionProgress: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(MISSION_PROGRESS_KEY, missionProgress)
            .apply()
    }
}
