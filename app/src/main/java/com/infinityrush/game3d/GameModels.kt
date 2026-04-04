package com.infinityrush.game3d

enum class RunnerState {
    START,
    RUNNING,
    PAUSED,
    GAME_OVER
}

enum class SwipeDirection {
    LEFT,
    RIGHT,
    DOWN
}

enum class ObstacleType {
    HURDLE,
    GATE,
    BLOCKER,
    SPIKE,
    BOULDER
}

enum class PowerUpType {
    SHIELD,
    MAGNET
}

data class GameSnapshot(
    val state: RunnerState,
    val score: Int,
    val highScore: Int,
    val coins: Int,
    val totalCoins: Int,
    val speedKph: Int,
    val selectedHero: String,
    val selectedHeroTitle: String,
    val nextUnlock: String,
    val activeMission: String,
    val activeMissionReward: String,
    val activePowerUp: String,
    val musicEnabled: Boolean,
    val sfxEnabled: Boolean
)

data class Obstacle(
    val lane: Int,
    val type: ObstacleType,
    var z: Float,
    val width: Float,
    val height: Float,
    val depth: Float,
    var spin: Float = 0f
)

data class Coin(
    val lane: Int,
    var z: Float,
    val height: Float,
    var collected: Boolean = false
)

data class PowerUp(
    val lane: Int,
    val type: PowerUpType,
    var z: Float,
    val height: Float,
    var collected: Boolean = false
)
