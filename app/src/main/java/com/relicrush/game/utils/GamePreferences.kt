package com.relicrush.game.utils

import android.content.Context

class GamePreferences(context: Context) {
    private val prefs = context.getSharedPreferences(GameConstants.PREFS_NAME, Context.MODE_PRIVATE)

    fun getHighScore(): Int = prefs.getInt("high_score", 0)
    fun setHighScore(value: Int) = prefs.edit().putInt("high_score", value).apply()

    fun getCoins(): Int = prefs.getInt("coins", 0)
    fun setCoins(value: Int) = prefs.edit().putInt("coins", value).apply()

    fun getTotalDistance(): Int = prefs.getInt("total_distance", 0)
    fun setTotalDistance(value: Int) = prefs.edit().putInt("total_distance", value).apply()

    fun getTotalXp(): Int = prefs.getInt("total_xp", 0)
    fun setTotalXp(value: Int) = prefs.edit().putInt("total_xp", value).apply()

    fun getGamesPlayed(): Int = prefs.getInt("games_played", 0)
    fun setGamesPlayed(value: Int) = prefs.edit().putInt("games_played", value).apply()

    fun getSelectedCharacter(): String {
        return prefs.getString("selected_character", GameConstants.DEFAULT_UNLOCKED_CHARACTER)
            ?: GameConstants.DEFAULT_UNLOCKED_CHARACTER
    }

    fun setSelectedCharacter(id: String) {
        prefs.edit().putString("selected_character", id).apply()
    }

    fun getUnlockedCharacters(): MutableSet<String> {
        return prefs.getStringSet("unlocked_characters", setOf(GameConstants.DEFAULT_UNLOCKED_CHARACTER))
            ?.toMutableSet()
            ?: mutableSetOf(GameConstants.DEFAULT_UNLOCKED_CHARACTER)
    }

    fun setUnlockedCharacters(ids: Set<String>) {
        prefs.edit().putStringSet("unlocked_characters", ids).apply()
    }

    fun isMusicEnabled(): Boolean = prefs.getBoolean("music_enabled", true)
    fun setMusicEnabled(enabled: Boolean) = prefs.edit().putBoolean("music_enabled", enabled).apply()

    fun isSfxEnabled(): Boolean = prefs.getBoolean("sfx_enabled", true)
    fun setSfxEnabled(enabled: Boolean) = prefs.edit().putBoolean("sfx_enabled", enabled).apply()

    fun isRemoveAdsPurchased(): Boolean = prefs.getBoolean("remove_ads", false)
    fun setRemoveAdsPurchased(purchased: Boolean) {
        prefs.edit().putBoolean("remove_ads", purchased).apply()
    }

    fun getMissionIndex(): Int = prefs.getInt("mission_index", 0)
    fun setMissionIndex(index: Int) = prefs.edit().putInt("mission_index", index).apply()

    fun getMissionProgress(): Int = prefs.getInt("mission_progress", 0)
    fun setMissionProgress(progress: Int) = prefs.edit().putInt("mission_progress", progress).apply()

    fun getDailyRewardDay(): String? = prefs.getString("daily_reward_day", null)
    fun setDailyRewardDay(day: String) = prefs.edit().putString("daily_reward_day", day).apply()

    fun getDailyRewardStreak(): Int = prefs.getInt("daily_reward_streak", 0)
    fun setDailyRewardStreak(streak: Int) = prefs.edit().putInt("daily_reward_streak", streak).apply()
}
