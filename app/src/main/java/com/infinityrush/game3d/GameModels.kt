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

data class GameSnapshot(
    val state: RunnerState,
    val score: Int,
    val highScore: Int,
    val coins: Int,
    val speedKph: Int,
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

