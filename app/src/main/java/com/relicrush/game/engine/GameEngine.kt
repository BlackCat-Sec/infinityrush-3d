package com.relicrush.game.engine

import android.content.Context
import android.graphics.Color
import com.relicrush.game.entities.Coin
import com.relicrush.game.entities.DailyRewardInfo
import com.relicrush.game.entities.EnvironmentTheme
import com.relicrush.game.entities.GameContent
import com.relicrush.game.entities.GameScreen
import com.relicrush.game.entities.MissionDefinition
import com.relicrush.game.entities.MissionType
import com.relicrush.game.entities.Obstacle
import com.relicrush.game.entities.ObstacleType
import com.relicrush.game.entities.Particle
import com.relicrush.game.entities.Player
import com.relicrush.game.entities.PowerUp
import com.relicrush.game.entities.PowerUpType
import com.relicrush.game.entities.RuntimeMessage
import com.relicrush.game.utils.GameConstants
import com.relicrush.game.utils.GameMath
import com.relicrush.game.utils.GamePreferences
import com.relicrush.game.utils.ObjectPool
import com.relicrush.game.utils.SoundManager
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

class GameEngine(context: Context) {
    private data class SpawnSpec(
        val lane: Int,
        val type: ObstacleType,
        val zOffset: Float = 0f
    )

    private val random = Random(System.currentTimeMillis())
    private val preferences = GamePreferences(context)
    val soundManager = SoundManager(context, preferences)

    val player = Player()
    val obstacles = mutableListOf<Obstacle>()
    val coins = mutableListOf<Coin>()
    val powerUps = mutableListOf<PowerUp>()
    val particles = mutableListOf<Particle>()

    private val obstaclePool = ObjectPool { Obstacle() }
    private val coinPool = ObjectPool { Coin() }
    private val powerUpPool = ObjectPool { PowerUp() }
    private val particlePool = ObjectPool { Particle() }

    var screen = GameScreen.HOME
        private set
    var viewportWidth = 0
        private set
    var viewportHeight = 0
        private set

    var highScore = preferences.getHighScore()
        private set
    var coinBank = preferences.getCoins()
        private set
    var totalDistance = preferences.getTotalDistance()
        private set
    var totalXp = preferences.getTotalXp()
        private set
    var gamesPlayed = preferences.getGamesPlayed()
        private set
    var selectedCharacterId = preferences.getSelectedCharacter()
        private set
    val unlockedCharacters = preferences.getUnlockedCharacters()
    var characterPreviewIndex = 0
        private set
    var removeAdsPurchased = preferences.isRemoveAdsPurchased()
        private set

    private var missionIndex = preferences.getMissionIndex()
    private var missionProgress = preferences.getMissionProgress()
    private var distanceThisRun = 0f
    private var scoreThisRun = 0
    private var coinsThisRun = 0
    private var dodgesThisRun = 0
    private var powerUpsThisRun = 0
    private var currentSpeed = GameConstants.BASE_SPEED
    private var obstacleTimer = 0f
    private var coinTimer = 0f
    private var powerUpTimer = 0f
    private var ambientTime = 0f
    private var runTime = 0f
    private var reviveUsed = false
    private var runFinalized = false
    private var pendingInterstitial = false
    private var lastPlayerHeight = 0f

    var statusMessage: RuntimeMessage? = null
        private set

    init {
        if (!unlockedCharacters.contains(selectedCharacterId)) {
            selectedCharacterId = GameConstants.DEFAULT_UNLOCKED_CHARACTER
            preferences.setSelectedCharacter(selectedCharacterId)
        }
        characterPreviewIndex = GameContent.characters.indexOfFirst { it.id == selectedCharacterId }.coerceAtLeast(0)
        player.reset(selectedCharacter())
    }

    fun configure(width: Int, height: Int) {
        viewportWidth = width
        viewportHeight = height
    }

    fun update(deltaSeconds: Float) {
        ambientTime += deltaSeconds
        updateMessage(deltaSeconds)

        if (screen == GameScreen.RUNNING) {
            updateRun(deltaSeconds)
        } else {
            updateParticles(deltaSeconds)
            soundManager.setMusicIntensity(0.32f + abs(sin(ambientTime * 0.45f)) * 0.08f)
        }
    }

    fun selectedCharacter() = GameContent.getCharacter(selectedCharacterId)

    fun previewCharacter() = GameContent.characters[characterPreviewIndex]

    fun currentMission(): MissionDefinition {
        return GameContent.missions[missionIndex % GameContent.missions.size]
    }

    fun currentMissionProgress(): Int = missionProgress

    fun progressToNextMission(): Float {
        val mission = currentMission()
        return (missionProgress.toFloat() / mission.target.toFloat()).coerceIn(0f, 1f)
    }

    fun playerLevel(): Int = (totalXp / GameConstants.XP_PER_LEVEL) + 1

    fun progressToNextLevel(): Float {
        return (totalXp % GameConstants.XP_PER_LEVEL).toFloat() / GameConstants.XP_PER_LEVEL.toFloat()
    }

    fun currentTheme(): EnvironmentTheme {
        return when ((distanceThisRun / GameConstants.ZONE_LENGTH_METERS).toInt() % 3) {
            1 -> EnvironmentTheme.RUINS
            2 -> EnvironmentTheme.BRIDGE
            else -> EnvironmentTheme.JUNGLE
        }
    }

    fun dayNightBlend(): Float {
        val cycle = (runTime / GameConstants.DAY_NIGHT_SECONDS) * (Math.PI * 2f)
        return ((sin(cycle).toFloat() + 1f) * 0.5f).coerceIn(0f, 1f)
    }

    fun distanceInRun(): Float = distanceThisRun

    fun scoreInRun(): Int = scoreThisRun

    fun coinsInRun(): Int = coinsThisRun

    fun speedInRun(): Float = currentSpeed

    fun canRevive(): Boolean = screen == GameScreen.GAME_OVER && !reviveUsed

    fun hasPendingInterstitial(): Boolean = pendingInterstitial

    fun consumePendingInterstitial(): Boolean {
        val pending = pendingInterstitial
        pendingInterstitial = false
        return pending
    }

    fun isMusicEnabled(): Boolean = soundManager.isMusicEnabled()

    fun isSfxEnabled(): Boolean = soundManager.isSfxEnabled()

    fun dailyRewardInfo(): DailyRewardInfo {
        val lastClaim = preferences.getDailyRewardDay()
        val today = GameMath.todayKey()
        val currentStreak = preferences.getDailyRewardStreak()
        val nextStreak = when {
            lastClaim == null -> 1
            lastClaim == today -> maxOf(1, currentStreak)
            else -> {
                val previous = java.time.LocalDate.parse(lastClaim)
                val delta = java.time.temporal.ChronoUnit.DAYS.between(previous, java.time.LocalDate.now())
                if (delta == 1L) currentStreak + 1 else 1
            }
        }
        val rewardIndex = (nextStreak - 1) % GameContent.dailyRewards.size
        return DailyRewardInfo(
            streakDay = nextStreak,
            rewardCoins = GameContent.dailyRewards[rewardIndex],
            canClaim = lastClaim != today
        )
    }

    fun startRun() {
        resetRunState()
        screen = GameScreen.RUNNING
        soundManager.startMusic()
    }

    fun restartRun() {
        finalizeRunIfNeeded()
        startRun()
    }

    fun pauseRun() {
        if (screen == GameScreen.RUNNING) {
            screen = GameScreen.PAUSED
            soundManager.pauseMusic()
        }
    }

    fun resumeRun() {
        if (screen == GameScreen.PAUSED) {
            screen = GameScreen.RUNNING
            soundManager.resumeMusic()
        }
    }

    fun goHome() {
        finalizeRunIfNeeded()
        screen = GameScreen.HOME
        player.reset(selectedCharacter())
        clearRunObjects()
        soundManager.startMusic()
    }

    fun openCharacterSelect() {
        finalizeRunIfNeeded()
        screen = GameScreen.CHARACTER_SELECT
        characterPreviewIndex = GameContent.characters.indexOfFirst { it.id == selectedCharacterId }.coerceAtLeast(0)
    }

    fun backFromCharacterSelect() {
        screen = GameScreen.HOME
    }

    fun selectCharacter(index: Int) {
        characterPreviewIndex = index.coerceIn(0, GameContent.characters.lastIndex)
        val preview = previewCharacter()
        if (unlockedCharacters.contains(preview.id)) {
            selectedCharacterId = preview.id
            preferences.setSelectedCharacter(selectedCharacterId)
            player.reset(selectedCharacter())
        }
    }

    fun unlockOrSelectPreviewCharacter() {
        val preview = previewCharacter()
        if (unlockedCharacters.contains(preview.id)) {
            selectedCharacterId = preview.id
            preferences.setSelectedCharacter(selectedCharacterId)
            player.reset(selectedCharacter())
            showStatus("${preview.name} is ready for the next run.", Color.parseColor("#E8D98D"))
            return
        }

        if (coinBank < preview.unlockCost) {
            showStatus("Not enough coins to unlock ${preview.name}.", Color.parseColor("#FF8C82"))
            return
        }

        coinBank -= preview.unlockCost
        preferences.setCoins(coinBank)
        unlockedCharacters.add(preview.id)
        preferences.setUnlockedCharacters(unlockedCharacters)
        selectedCharacterId = preview.id
        preferences.setSelectedCharacter(selectedCharacterId)
        player.reset(selectedCharacter())
        showStatus("${preview.name} unlocked.", Color.parseColor("#9CF3C3"))
    }

    fun claimDailyReward() {
        val reward = dailyRewardInfo()
        if (!reward.canClaim) {
            showStatus("Daily reward already claimed today.", Color.parseColor("#E8D98D"))
            return
        }

        coinBank += reward.rewardCoins
        preferences.setCoins(coinBank)
        preferences.setDailyRewardDay(GameMath.todayKey())
        preferences.setDailyRewardStreak(reward.streakDay)
        showStatus("Daily reward +${reward.rewardCoins} coins.", Color.parseColor("#9CF3C3"))
    }

    fun toggleMusic() {
        soundManager.setMusicEnabled(!soundManager.isMusicEnabled())
        showStatus(
            if (soundManager.isMusicEnabled()) "Music on." else "Music muted.",
            Color.parseColor("#E8D98D")
        )
    }

    fun toggleSfx() {
        soundManager.setSfxEnabled(!soundManager.isSfxEnabled())
        showStatus(
            if (soundManager.isSfxEnabled()) "Sound effects on." else "Sound effects muted.",
            Color.parseColor("#E8D98D")
        )
    }

    fun onCoinPackPurchased(amount: Int) {
        coinBank += amount
        preferences.setCoins(coinBank)
        showStatus("+$amount coins added to your vault.", Color.parseColor("#9CF3C3"))
    }

    fun onRemoveAdsPurchased() {
        removeAdsPurchased = true
        preferences.setRemoveAdsPurchased(true)
        showStatus("Ads removed from future runs.", Color.parseColor("#9CF3C3"))
    }

    fun finalizeRunForExit(): Boolean {
        finalizeRunIfNeeded()
        return consumePendingInterstitial()
    }

    fun reviveRun() {
        if (!canRevive()) {
            return
        }

        reviveUsed = true
        screen = GameScreen.RUNNING
        player.revive(GameConstants.REVIVE_INVINCIBILITY)
        removeDangerNearPlayer()
        soundManager.startMusic()
        showStatus("Revive granted. Keep moving.", Color.parseColor("#9CF3C3"))
    }

    fun moveLeft() {
        if (screen == GameScreen.RUNNING) {
            player.moveLeft()
        }
    }

    fun moveRight() {
        if (screen == GameScreen.RUNNING) {
            player.moveRight()
        }
    }

    fun jump() {
        if (screen != GameScreen.RUNNING) {
            return
        }

        if (player.jump(GameConstants.BASE_JUMP_VELOCITY)) {
            soundManager.playJump()
            spawnDustAtPlayer(Color.parseColor("#D2B48C"), 8)
        }
    }

    fun slide() {
        if (screen != GameScreen.RUNNING) {
            return
        }

        if (player.slide()) {
            spawnDustAtPlayer(Color.parseColor("#C09663"), 6)
        }
    }

    fun onHostPause() {
        if (screen == GameScreen.RUNNING) {
            screen = GameScreen.PAUSED
        }
        if (screen == GameScreen.GAME_OVER) {
            finalizeRunIfNeeded()
        }
        soundManager.pauseMusic()
    }

    fun onHostResume() {
        if (screen != GameScreen.RUNNING) {
            soundManager.startMusic()
        }
    }

    fun release() {
        finalizeRunIfNeeded()
        soundManager.release()
    }

    fun showStatus(text: String, color: Int) {
        statusMessage = RuntimeMessage(text = text, kindColor = color, timeLeft = 2.6f)
    }

    private fun updateMessage(deltaSeconds: Float) {
        statusMessage?.let { message ->
            message.timeLeft -= deltaSeconds
            if (message.timeLeft <= 0f) {
                statusMessage = null
            }
        }
    }

    private fun updateRun(deltaSeconds: Float) {
        runTime += deltaSeconds
        currentSpeed = (
            GameConstants.BASE_SPEED * selectedCharacter().speedMultiplier +
                runTime * GameConstants.SPEED_RAMP_PER_SECOND +
                if (player.hasBoost()) GameConstants.BOOST_SPEED_BONUS else 0f
            ).coerceAtMost(GameConstants.MAX_SPEED)

        distanceThisRun += currentSpeed * deltaSeconds
        scoreThisRun = (
            distanceThisRun * GameConstants.SCORE_PER_METER * if (player.hasDoubleScore()) 2f else 1f
            ).toInt() + coinsThisRun * 10

        val wasGrounded = lastPlayerHeight <= 0.01f
        player.update(deltaSeconds, GameConstants.GRAVITY)
        if (!wasGrounded && player.height == 0f) {
            spawnDustAtPlayer(Color.parseColor("#B58B61"), 7)
        }
        lastPlayerHeight = player.height

        obstacleTimer -= deltaSeconds
        coinTimer -= deltaSeconds
        powerUpTimer -= deltaSeconds

        if (obstacleTimer <= 0f) {
            spawnObstacleWave()
            val difficulty = (runTime / 12f).coerceAtMost(1.2f)
            obstacleTimer = (GameConstants.BASE_SPAWN_INTERVAL - difficulty * 0.48f)
                .coerceAtLeast(GameConstants.MIN_SPAWN_INTERVAL)
        }

        if (coinTimer <= 0f) {
            spawnCoinTrail()
            coinTimer = GameConstants.COIN_INTERVAL + random.nextFloat() * 0.22f
        }

        if (powerUpTimer <= 0f) {
            spawnPowerUp()
            powerUpTimer = GameConstants.POWER_UP_INTERVAL + random.nextFloat() * 2.4f
        }

        updateObstacles(deltaSeconds)
        updateCoins(deltaSeconds)
        updatePowerUps(deltaSeconds)
        updateParticles(deltaSeconds)
        soundManager.setMusicIntensity(0.38f + ((currentSpeed - GameConstants.BASE_SPEED) / 40f).coerceIn(0f, 0.45f))
    }

    private fun updateObstacles(deltaSeconds: Float) {
        val iterator = obstacles.iterator()
        while (iterator.hasNext()) {
            val obstacle = iterator.next()
            obstacle.update(deltaSeconds, currentSpeed)

            if (!obstacle.dodgeCounted && obstacle.z < -1.1f) {
                obstacle.dodgeCounted = true
                dodgesThisRun += 1
            }

            if (obstacle.collidesWith(player)) {
                if (player.isShielded()) {
                    obstacle.deactivate()
                    iterator.remove()
                    obstaclePool.recycle(obstacle)
                    spawnBreakBurst(obstacle.laneFloat, Color.parseColor("#9EE6FF"))
                    showStatus("Shield saved the run.", Color.parseColor("#9EE6FF"))
                    continue
                }

                triggerCrash()
                return
            }

            if (obstacle.isExpired(GameConstants.DESPAWN_Z)) {
                obstacle.deactivate()
                iterator.remove()
                obstaclePool.recycle(obstacle)
            }
        }
    }

    private fun updateCoins(deltaSeconds: Float) {
        val iterator = coins.iterator()
        while (iterator.hasNext()) {
            val coin = iterator.next()
            coin.update(deltaSeconds, currentSpeed)

            if (coin.shouldCollect(player)) {
                coinsThisRun += if (player.hasDoubleScore()) 2 else 1
                soundManager.playCoin()
                spawnCoinSparkles(coin.laneIndex.toFloat())
                coin.deactivate()
                iterator.remove()
                coinPool.recycle(coin)
                continue
            }

            if (coin.isExpired(GameConstants.DESPAWN_Z)) {
                coin.deactivate()
                iterator.remove()
                coinPool.recycle(coin)
            }
        }
    }

    private fun updatePowerUps(deltaSeconds: Float) {
        val iterator = powerUps.iterator()
        while (iterator.hasNext()) {
            val powerUp = iterator.next()
            powerUp.update(deltaSeconds, currentSpeed)

            if (powerUp.shouldCollect(player)) {
                val duration = when (powerUp.type) {
                    PowerUpType.MAGNET -> GameConstants.MAGNET_DURATION
                    PowerUpType.SHIELD -> GameConstants.SHIELD_DURATION
                    PowerUpType.BOOST -> GameConstants.BOOST_DURATION
                    PowerUpType.DOUBLE_SCORE -> GameConstants.DOUBLE_SCORE_DURATION
                }
                player.grantPowerUp(powerUp.type, duration)
                powerUpsThisRun += 1
                soundManager.playPowerUp()
                spawnBreakBurst(powerUp.laneIndex.toFloat(), Color.parseColor("#FFE27A"))
                showStatus("${powerUp.type.name.replace('_', ' ')} activated.", Color.parseColor("#FFE27A"))
                powerUp.deactivate()
                iterator.remove()
                powerUpPool.recycle(powerUp)
                continue
            }

            if (powerUp.isExpired(GameConstants.DESPAWN_Z)) {
                powerUp.deactivate()
                iterator.remove()
                powerUpPool.recycle(powerUp)
            }
        }
    }

    private fun updateParticles(deltaSeconds: Float) {
        val iterator = particles.iterator()
        while (iterator.hasNext()) {
            val particle = iterator.next()
            particle.update(deltaSeconds)
            if (!particle.active) {
                iterator.remove()
                particlePool.recycle(particle)
            }
        }
    }

    private fun triggerCrash() {
        screen = GameScreen.GAME_OVER
        soundManager.playCrash()
        spawnBreakBurst(player.lanePosition, Color.parseColor("#FF9161"))
        showStatus("The relic trail fought back.", Color.parseColor("#FF9161"))
    }

    private fun removeDangerNearPlayer() {
        val iterator = obstacles.iterator()
        while (iterator.hasNext()) {
            val obstacle = iterator.next()
            if (obstacle.z < 10f) {
                obstacle.deactivate()
                iterator.remove()
                obstaclePool.recycle(obstacle)
            }
        }
    }

    private fun finalizeRunIfNeeded() {
        if (runFinalized || distanceThisRun <= 0f) {
            return
        }

        runFinalized = true
        gamesPlayed += 1
        totalDistance += distanceThisRun.toInt()
        totalXp += (distanceThisRun / 2.8f).toInt() + coinsThisRun * 8
        coinBank += coinsThisRun

        if (scoreThisRun > highScore) {
            highScore = scoreThisRun
            preferences.setHighScore(highScore)
        }

        val mission = currentMission()
        missionProgress += when (mission.type) {
            MissionType.DISTANCE -> distanceThisRun.toInt()
            MissionType.COINS -> coinsThisRun
            MissionType.DODGES -> dodgesThisRun
            MissionType.POWER_UPS -> powerUpsThisRun
        }

        if (missionProgress >= mission.target) {
            coinBank += mission.rewardCoins
            missionIndex = (missionIndex + 1) % GameContent.missions.size
            missionProgress = 0
            showStatus("Mission complete. +${mission.rewardCoins} coins.", Color.parseColor("#9CF3C3"))
        }

        preferences.setCoins(coinBank)
        preferences.setTotalDistance(totalDistance)
        preferences.setTotalXp(totalXp)
        preferences.setGamesPlayed(gamesPlayed)
        preferences.setMissionIndex(missionIndex)
        preferences.setMissionProgress(missionProgress)

        pendingInterstitial =
            !removeAdsPurchased &&
                screen == GameScreen.GAME_OVER &&
                gamesPlayed % GameConstants.INTERSTITIAL_EVERY_GAME_OVERS == 0
    }

    private fun resetRunState() {
        clearRunObjects()
        player.reset(selectedCharacter())
        distanceThisRun = 0f
        scoreThisRun = 0
        coinsThisRun = 0
        dodgesThisRun = 0
        powerUpsThisRun = 0
        currentSpeed = GameConstants.BASE_SPEED
        obstacleTimer = 0.55f
        coinTimer = 0.25f
        powerUpTimer = GameConstants.POWER_UP_INTERVAL
        runTime = 0f
        reviveUsed = false
        runFinalized = false
        pendingInterstitial = false
        lastPlayerHeight = 0f
        statusMessage = null
    }

    private fun clearRunObjects() {
        obstaclePool.recycleAll(obstacles)
        coinPool.recycleAll(coins)
        powerUpPool.recycleAll(powerUps)
        particlePool.recycleAll(particles)
    }

    private fun spawnObstacleWave() {
        val difficulty = (runTime / 14f).toInt().coerceAtMost(4)
        val patterns = mutableListOf<List<SpawnSpec>>(
            listOf(SpawnSpec(randomLane(), ObstacleType.ROCK)),
            listOf(SpawnSpec(randomLane(), ObstacleType.SPIKES)),
            listOf(SpawnSpec(randomLane(), ObstacleType.LOW_BRANCH)),
            listOf(SpawnSpec(randomLane(), ObstacleType.GAP))
        )

        if (difficulty >= 1) {
            patterns += listOf(
                SpawnSpec(0, ObstacleType.ROCK),
                SpawnSpec(2, ObstacleType.SPIKES, 5.4f)
            )
            patterns += listOf(SpawnSpec(1, ObstacleType.SWING_TRAP))
        }

        if (difficulty >= 2) {
            patterns += listOf(
                SpawnSpec(0, ObstacleType.SPIKES),
                SpawnSpec(1, ObstacleType.GAP, 5.8f)
            )
            patterns += listOf(
                SpawnSpec(1, ObstacleType.BOULDER),
                SpawnSpec(2, ObstacleType.LOW_BRANCH, 7.0f)
            )
        }

        if (difficulty >= 3) {
            patterns += listOf(
                SpawnSpec(0, ObstacleType.ROCK),
                SpawnSpec(2, ObstacleType.ROCK),
                SpawnSpec(1, ObstacleType.LOW_BRANCH, 7.8f)
            )
        }

        val pattern = patterns.random(random)
        pattern.forEach { spec ->
            val obstacle = obstaclePool.obtain()
            obstacle.spawn(spec.type, spec.lane, GameConstants.FAR_Z + spec.zOffset, random)
            obstacles += obstacle
        }
    }

    private fun spawnCoinTrail() {
        val arc = random.nextInt(3)
        when (arc) {
            0 -> {
                val lane = randomLane()
                repeat(6) { index ->
                    spawnCoin(lane, GameConstants.FAR_Z + index * 5.4f, if (index in 2..3) 1.15f else 0.6f)
                }
            }

            1 -> {
                val lanes = listOf(0, 1, 2).shuffled(random)
                repeat(6) { index ->
                    spawnCoin(lanes[index % lanes.size], GameConstants.FAR_Z + index * 4.6f, 0.82f)
                }
            }

            else -> {
                repeat(5) { index ->
                    spawnCoin(index % 3, GameConstants.FAR_Z + index * 5.2f, 0.95f + sin(index.toFloat()) * 0.28f)
                }
            }
        }
    }

    private fun spawnCoin(lane: Int, z: Float, arcHeight: Float) {
        val coin = coinPool.obtain()
        coin.spawn(lane, z, arcHeight)
        coins += coin
    }

    private fun spawnPowerUp() {
        val powerUp = powerUpPool.obtain()
        val type = PowerUpType.entries.random(random)
        powerUp.spawn(type, randomLane(), GameConstants.FAR_Z + 10f)
        powerUps += powerUp
    }

    private fun spawnDustAtPlayer(color: Int, count: Int) {
        repeat(count) {
            val particle = particlePool.obtain()
            particle.spawn(
                x = laneToScreenX(player.lanePosition) + random.nextFloat() * 48f - 24f,
                y = playerGroundY() + random.nextFloat() * 18f,
                velocityX = random.nextFloat() * 120f - 60f,
                velocityY = -(120f + random.nextFloat() * 100f),
                size = 4f + random.nextFloat() * 7f,
                color = color,
                life = 0.35f + random.nextFloat() * 0.25f
            )
            particles += particle
        }
    }

    private fun spawnCoinSparkles(lanePosition: Float) {
        repeat(7) {
            val particle = particlePool.obtain()
            particle.spawn(
                x = laneToScreenX(lanePosition) + random.nextFloat() * 30f - 15f,
                y = playerGroundY() - 90f + random.nextFloat() * 24f,
                velocityX = random.nextFloat() * 160f - 80f,
                velocityY = -(160f + random.nextFloat() * 120f),
                size = 4f + random.nextFloat() * 6f,
                color = Color.parseColor("#FFD76B"),
                life = 0.32f + random.nextFloat() * 0.2f
            )
            particles += particle
        }
    }

    private fun spawnBreakBurst(lanePosition: Float, color: Int) {
        repeat(14) {
            val particle = particlePool.obtain()
            particle.spawn(
                x = laneToScreenX(lanePosition) + random.nextFloat() * 70f - 35f,
                y = playerGroundY() - 80f + random.nextFloat() * 40f,
                velocityX = random.nextFloat() * 260f - 130f,
                velocityY = -(200f + random.nextFloat() * 160f),
                size = 6f + random.nextFloat() * 10f,
                color = color,
                life = 0.42f + random.nextFloat() * 0.3f
            )
            particles += particle
        }
    }

    private fun randomLane(): Int = random.nextInt(GameConstants.LANE_COUNT)

    private fun laneToScreenX(lanePosition: Float): Float {
        val spread = if (viewportWidth > viewportHeight) viewportWidth * 0.13f else viewportWidth * 0.18f
        return viewportWidth * 0.5f + (lanePosition - 1f) * spread
    }

    private fun playerGroundY(): Float = viewportHeight * 0.83f
}
