package com.relicrush.game.entities

import android.graphics.Color
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

enum class GameScreen {
    HOME,
    CHARACTER_SELECT,
    RUNNING,
    PAUSED,
    GAME_OVER
}

enum class SwipeDirection {
    LEFT,
    RIGHT,
    UP,
    DOWN
}

enum class EnvironmentTheme {
    JUNGLE,
    RUINS,
    BRIDGE
}

enum class ObstacleType {
    ROCK,
    SPIKES,
    BOULDER,
    SWING_TRAP,
    GAP,
    LOW_BRANCH
}

enum class PowerUpType {
    MAGNET,
    SHIELD,
    BOOST,
    DOUBLE_SCORE
}

enum class MissionType {
    DISTANCE,
    COINS,
    DODGES,
    POWER_UPS
}

data class CharacterPalette(
    val skinColor: Int,
    val primaryColor: Int,
    val accentColor: Int,
    val hairColor: Int,
    val glowColor: Int
)

data class CharacterDefinition(
    val id: String,
    val name: String,
    val title: String,
    val unlockCost: Int,
    val speedMultiplier: Float,
    val jumpMultiplier: Float,
    val palette: CharacterPalette
)

data class MissionDefinition(
    val type: MissionType,
    val title: String,
    val description: String,
    val target: Int,
    val rewardCoins: Int
)

data class DailyRewardInfo(
    val streakDay: Int,
    val rewardCoins: Int,
    val canClaim: Boolean
)

data class RuntimeMessage(
    val text: String,
    val kindColor: Int,
    var timeLeft: Float
)

class Player {
    var character: CharacterDefinition? = null
        private set

    var laneIndex = 1
        private set
    var lanePosition = 1f
        private set
    var height = 0f
        private set
    var verticalVelocity = 0f
        private set
    var slideTimer = 0f
        private set
    var shieldTimer = 0f
        private set
    var magnetTimer = 0f
        private set
    var boostTimer = 0f
        private set
    var doubleScoreTimer = 0f
        private set
    var animationTime = 0f
        private set
    var landingSquash = 0f
        private set

    fun reset(definition: CharacterDefinition) {
        character = definition
        laneIndex = 1
        lanePosition = 1f
        height = 0f
        verticalVelocity = 0f
        slideTimer = 0f
        shieldTimer = 0f
        magnetTimer = 0f
        boostTimer = 0f
        doubleScoreTimer = 0f
        animationTime = 0f
        landingSquash = 0f
    }

    fun update(deltaSeconds: Float, gravity: Float) {
        animationTime += deltaSeconds

        lanePosition += (laneIndex - lanePosition) * minOf(1f, deltaSeconds * 10f)
        verticalVelocity -= gravity * deltaSeconds
        height += verticalVelocity * deltaSeconds

        if (height <= 0f) {
            if (verticalVelocity < -6f) {
                landingSquash = 1f
            }
            height = 0f
            verticalVelocity = 0f
        }

        slideTimer = (slideTimer - deltaSeconds).coerceAtLeast(0f)
        shieldTimer = (shieldTimer - deltaSeconds).coerceAtLeast(0f)
        magnetTimer = (magnetTimer - deltaSeconds).coerceAtLeast(0f)
        boostTimer = (boostTimer - deltaSeconds).coerceAtLeast(0f)
        doubleScoreTimer = (doubleScoreTimer - deltaSeconds).coerceAtLeast(0f)
        landingSquash = (landingSquash - deltaSeconds * 3.8f).coerceAtLeast(0f)
    }

    fun moveLeft() {
        laneIndex = (laneIndex - 1).coerceAtLeast(0)
    }

    fun moveRight() {
        laneIndex = (laneIndex + 1).coerceAtMost(2)
    }

    fun jump(baseVelocity: Float): Boolean {
        if (height > 0.08f || slideTimer > 0f) {
            return false
        }

        val jumpVelocity = baseVelocity * (character?.jumpMultiplier ?: 1f)
        verticalVelocity = jumpVelocity
        return true
    }

    fun slide(): Boolean {
        if (height > 0.12f) {
            return false
        }

        slideTimer = 0.74f
        return true
    }

    fun grantPowerUp(type: PowerUpType, duration: Float) {
        when (type) {
            PowerUpType.MAGNET -> magnetTimer = duration
            PowerUpType.SHIELD -> shieldTimer = duration
            PowerUpType.BOOST -> boostTimer = duration
            PowerUpType.DOUBLE_SCORE -> doubleScoreTimer = duration
        }
    }

    fun revive(invincibilityDuration: Float) {
        height = 0f
        verticalVelocity = 0f
        slideTimer = 0f
        shieldTimer = invincibilityDuration
        boostTimer = 0f
    }

    fun isSliding(): Boolean = slideTimer > 0f
    fun isShielded(): Boolean = shieldTimer > 0f
    fun hasMagnet(): Boolean = magnetTimer > 0f
    fun hasBoost(): Boolean = boostTimer > 0f
    fun hasDoubleScore(): Boolean = doubleScoreTimer > 0f
}

class Obstacle {
    var active = false
        private set
    lateinit var type: ObstacleType
        private set
    var laneIndex = 1
        private set
    var z = 0f
        private set
    var laneFloat = 1f
        private set
    private var moveAmplitude = 0f
    private var moveSpeed = 0f
    private var phase = 0f
    var dodgeCounted = false

    fun spawn(type: ObstacleType, laneIndex: Int, z: Float, random: Random) {
        active = true
        this.type = type
        this.laneIndex = laneIndex
        this.z = z
        this.laneFloat = laneIndex.toFloat()
        this.dodgeCounted = false
        when (type) {
            ObstacleType.BOULDER -> {
                moveAmplitude = 0.28f + random.nextFloat() * 0.35f
                moveSpeed = 2.1f + random.nextFloat() * 1.4f
                phase = random.nextFloat() * 6.28f
            }

            ObstacleType.SWING_TRAP -> {
                moveAmplitude = 0.18f + random.nextFloat() * 0.24f
                moveSpeed = 3.0f + random.nextFloat() * 1.5f
                phase = random.nextFloat() * 6.28f
            }

            else -> {
                moveAmplitude = 0f
                moveSpeed = 0f
                phase = 0f
            }
        }
    }

    fun update(deltaSeconds: Float, speed: Float) {
        if (!active) {
            return
        }

        z -= speed * deltaSeconds
        phase += moveSpeed * deltaSeconds
        laneFloat = laneIndex + sin(phase) * moveAmplitude
    }

    fun isExpired(despawnZ: Float): Boolean = active && z < despawnZ

    fun deactivate() {
        active = false
    }

    fun collidesWith(player: Player): Boolean {
        if (!active || z !in -1.5f..3.2f) {
            return false
        }

        val laneDistance = abs(laneFloat - player.lanePosition)
        if (laneDistance > 0.52f) {
            return false
        }

        return when (type) {
            ObstacleType.SPIKES, ObstacleType.GAP -> player.height < 1.25f
            ObstacleType.LOW_BRANCH, ObstacleType.SWING_TRAP -> !player.isSliding()
            ObstacleType.ROCK, ObstacleType.BOULDER -> player.height < 0.9f
        }
    }
}

class Coin {
    var active = false
        private set
    var laneIndex = 1
        private set
    var z = 0f
        private set
    var verticalArc = 0f
        private set
    var sparkle = 0f
        private set

    fun spawn(laneIndex: Int, z: Float, verticalArc: Float) {
        active = true
        this.laneIndex = laneIndex
        this.z = z
        this.verticalArc = verticalArc
        sparkle = 0f
    }

    fun update(deltaSeconds: Float, speed: Float) {
        if (!active) {
            return
        }

        z -= speed * deltaSeconds
        sparkle += deltaSeconds * 5.4f
    }

    fun shouldCollect(player: Player): Boolean {
        if (!active || z !in -1.2f..4.2f) {
            return false
        }

        val laneDistance = abs(laneIndex.toFloat() - player.lanePosition)
        val magnetRange = if (player.hasMagnet()) 1.2f else 0.34f
        val heightMatch = abs(verticalArc - player.height) < if (player.hasMagnet()) 1.8f else 0.9f
        return laneDistance <= magnetRange && heightMatch
    }

    fun isExpired(despawnZ: Float): Boolean = active && z < despawnZ

    fun deactivate() {
        active = false
    }
}

class PowerUp {
    var active = false
        private set
    lateinit var type: PowerUpType
        private set
    var laneIndex = 1
        private set
    var z = 0f
        private set
    var spin = 0f
        private set

    fun spawn(type: PowerUpType, laneIndex: Int, z: Float) {
        active = true
        this.type = type
        this.laneIndex = laneIndex
        this.z = z
        this.spin = 0f
    }

    fun update(deltaSeconds: Float, speed: Float) {
        if (!active) {
            return
        }

        z -= speed * deltaSeconds
        spin += deltaSeconds * 4.3f
    }

    fun shouldCollect(player: Player): Boolean {
        if (!active || z !in -1.2f..4.2f) {
            return false
        }

        return abs(laneIndex.toFloat() - player.lanePosition) <= 0.44f
    }

    fun isExpired(despawnZ: Float): Boolean = active && z < despawnZ

    fun deactivate() {
        active = false
    }
}

class Particle {
    var active = false
        private set
    var x = 0f
        private set
    var y = 0f
        private set
    var velocityX = 0f
        private set
    var velocityY = 0f
        private set
    var size = 0f
        private set
    var color = Color.WHITE
        private set
    var life = 0f
        private set
    var maxLife = 0f
        private set

    fun spawn(
        x: Float,
        y: Float,
        velocityX: Float,
        velocityY: Float,
        size: Float,
        color: Int,
        life: Float
    ) {
        active = true
        this.x = x
        this.y = y
        this.velocityX = velocityX
        this.velocityY = velocityY
        this.size = size
        this.color = color
        this.life = life
        this.maxLife = life
    }

    fun update(deltaSeconds: Float) {
        if (!active) {
            return
        }

        life -= deltaSeconds
        if (life <= 0f) {
            active = false
            return
        }

        x += velocityX * deltaSeconds
        y += velocityY * deltaSeconds
        velocityY += 420f * deltaSeconds
        velocityX *= 0.97f
    }

    fun alpha(): Int {
        if (!active || maxLife <= 0f) {
            return 0
        }
        return (255f * (life / maxLife).coerceIn(0f, 1f)).toInt()
    }

    fun deactivate() {
        active = false
    }
}
