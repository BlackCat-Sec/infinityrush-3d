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
    BLOCKER
}

enum class PowerUpType {
    SHIELD,
    MAGNET
}

enum class RunnerSkin(
    val id: String,
    val displayName: String,
    val unlockCoins: Int,
    val suitColor: FloatArray,
    val trimColor: FloatArray,
    val accentColor: FloatArray
) {
    VANGUARD(
        id = "vanguard",
        displayName = "Vanguard",
        unlockCoins = 0,
        suitColor = floatArrayOf(0.20f, 0.64f, 0.92f, 1f),
        trimColor = floatArrayOf(0.95f, 0.98f, 1f, 1f),
        accentColor = floatArrayOf(0.08f, 0.14f, 0.24f, 1f)
    ),
    SOLARIS(
        id = "solaris",
        displayName = "Solaris",
        unlockCoins = 180,
        suitColor = floatArrayOf(0.98f, 0.59f, 0.20f, 1f),
        trimColor = floatArrayOf(1f, 0.92f, 0.58f, 1f),
        accentColor = floatArrayOf(0.34f, 0.17f, 0.07f, 1f)
    ),
    PHANTOM(
        id = "phantom",
        displayName = "Phantom",
        unlockCoins = 420,
        suitColor = floatArrayOf(0.56f, 0.42f, 0.96f, 1f),
        trimColor = floatArrayOf(0.90f, 0.87f, 1f, 1f),
        accentColor = floatArrayOf(0.14f, 0.11f, 0.28f, 1f)
    ),
    AURORA(
        id = "aurora",
        displayName = "Aurora",
        unlockCoins = 760,
        suitColor = floatArrayOf(0.16f, 0.84f, 0.66f, 1f),
        trimColor = floatArrayOf(0.84f, 1f, 0.97f, 1f),
        accentColor = floatArrayOf(0.04f, 0.17f, 0.17f, 1f)
    );

    companion object {
        fun fromId(id: String?): RunnerSkin {
            return entries.firstOrNull { it.id == id } ?: VANGUARD
        }
    }
}

data class GameSnapshot(
    val state: RunnerState,
    val score: Int,
    val highScore: Int,
    val coins: Int,
    val totalCoins: Int,
    val speedKph: Int,
    val selectedSkin: String,
    val nextUnlock: String,
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
    val depth: Float
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
