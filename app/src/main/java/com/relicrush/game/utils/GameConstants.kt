package com.relicrush.game.utils

object GameConstants {
    const val TARGET_FPS = 60L
    const val FRAME_TIME_MS = 1000L / TARGET_FPS

    const val LANE_COUNT = 3
    const val FAR_Z = 135f
    const val DESPAWN_Z = -8f
    const val BASE_SPEED = 27f
    const val MAX_SPEED = 58f
    const val SPEED_RAMP_PER_SECOND = 0.42f
    const val BOOST_SPEED_BONUS = 15f
    const val BASE_SPAWN_INTERVAL = 1.35f
    const val MIN_SPAWN_INTERVAL = 0.6f
    const val POWER_UP_INTERVAL = 9.5f
    const val COIN_INTERVAL = 0.82f
    const val GRAVITY = 44f
    const val BASE_JUMP_VELOCITY = 17.8f
    const val SLIDE_DURATION = 0.74f
    const val SHIELD_DURATION = 7.5f
    const val MAGNET_DURATION = 7.5f
    const val BOOST_DURATION = 4.8f
    const val DOUBLE_SCORE_DURATION = 6.8f
    const val REVIVE_INVINCIBILITY = 2.4f
    const val DAY_NIGHT_SECONDS = 95f
    const val ZONE_LENGTH_METERS = 520f
    const val XP_PER_LEVEL = 1200
    const val SCORE_PER_METER = 4f
    const val COIN_PACK_REWARD = 2500
    const val INTERSTITIAL_EVERY_GAME_OVERS = 2

    const val PREFS_NAME = "relic_rush_prefs"
    const val DEFAULT_UNLOCKED_CHARACTER = "maya"
    const val PRODUCT_REMOVE_ADS = "remove_ads"
    const val PRODUCT_COIN_PACK = "coin_pack"

    const val TEST_REWARDED_AD_UNIT = "ca-app-pub-3940256099942544/5224354917"
    const val TEST_INTERSTITIAL_AD_UNIT = "ca-app-pub-3940256099942544/1033173712"
}
