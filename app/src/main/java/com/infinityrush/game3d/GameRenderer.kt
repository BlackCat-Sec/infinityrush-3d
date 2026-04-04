package com.infinityrush.game3d

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.os.Handler
import android.os.Looper
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sign
import kotlin.math.sin
import kotlin.random.Random
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class GameRenderer(private val context: Context) : GLSurfaceView.Renderer {
    interface UiListener {
        fun onSnapshot(snapshot: GameSnapshot)
    }

    var uiListener: UiListener? = null

    private val mainHandler = Handler(Looper.getMainLooper())
    private val random = Random(System.currentTimeMillis())
    private val soundManager = SoundManager(context)

    private val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)
    private val tempMatrix = FloatArray(16)

    private lateinit var cubeMesh: CubeMesh
    private var shaderProgram = 0
    private var positionHandle = 0
    private var normalHandle = 0
    private var mvpHandle = 0
    private var modelHandle = 0
    private var colorHandle = 0
    private var fogColorHandle = 0
    private var lightDirectionHandle = 0

    private var runnerState = RunnerState.START
    private var highScore = GamePreferences.getHighScore(context)
    private var bankedCoins = GamePreferences.getTotalCoins(context)
    private var selectedHero = GamePreferences.getSelectedHero(context)
    private var missionIndex = GamePreferences.getMissionIndex(context)
    private var missionProgress = GamePreferences.getMissionProgress(context)
    private var lastPersistedMissionProgress = missionProgress

    private var score = 0
    private var coinsCollected = 0
    private var speed = 18f
    private var distance = 0f
    private var runCoinsBanked = false
    private var currentLevel = 1
    private var screenAspectRatio = 16f / 9f

    private var runnerLane = 0
    private var targetLane = 0
    private var runnerX = 0f
    private var runnerY = 0f
    private var runnerVelocityY = 0f
    private var slideTimer = 0f
    private var jumpBufferTimer = 0f
    private var slideBufferTimer = 0f
    private var runAnimationTime = 0f
    private var coinSpin = 0f
    private var worldPulse = 0f
    private var spawnTimer = 1.7f
    private var powerUpSpawnTimer = 7.5f
    private var shieldTimer = 0f
    private var magnetTimer = 0f
    private var rewardBannerTimer = 0f
    private var rewardBanner = ""

    private val obstacles = mutableListOf<Obstacle>()
    private val coins = mutableListOf<Coin>()
    private val powerUps = mutableListOf<PowerUp>()

    private var lastFrameNanos = 0L
    private var lastSnapshotDispatchTime = 0L

    private val laneWidth = 2.35f
    private val groundY = -1.15f
    private val spawnZ = -74f
    private val fogColor = floatArrayOf(0.08f, 0.10f, 0.08f, 1f)
    private val lightDirection = floatArrayOf(-0.28f, 0.9f, 0.22f)

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glEnable(GLES20.GL_CULL_FACE)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
        GLES20.glClearColor(0.03f, 0.05f, 0.1f, 1f)

        shaderProgram = OpenGlUtils.createProgram(VERTEX_SHADER, FRAGMENT_SHADER)
        positionHandle = GLES20.glGetAttribLocation(shaderProgram, "aPosition")
        normalHandle = GLES20.glGetAttribLocation(shaderProgram, "aNormal")
        mvpHandle = GLES20.glGetUniformLocation(shaderProgram, "uMvpMatrix")
        modelHandle = GLES20.glGetUniformLocation(shaderProgram, "uModelMatrix")
        colorHandle = GLES20.glGetUniformLocation(shaderProgram, "uColor")
        fogColorHandle = GLES20.glGetUniformLocation(shaderProgram, "uFogColor")
        lightDirectionHandle = GLES20.glGetUniformLocation(shaderProgram, "uLightDirection")

        cubeMesh = CubeMesh()
        normalizeMissionState()
        ensureSelectedHeroUnlocked()
        soundManager.startMusic()
        dispatchSnapshot(force = true)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        screenAspectRatio = width.toFloat() / height.toFloat()
        val fieldOfView = if (screenAspectRatio < 1f) 66f else 56f
        Matrix.perspectiveM(projectionMatrix, 0, fieldOfView, screenAspectRatio, 0.1f, 180f)
    }

    override fun onDrawFrame(gl: GL10?) {
        val now = System.nanoTime()
        if (lastFrameNanos == 0L) {
            lastFrameNanos = now
        }

        val deltaSeconds = ((now - lastFrameNanos) / 1_000_000_000f).coerceAtMost(0.05f)
        lastFrameNanos = now

        when (runnerState) {
            RunnerState.RUNNING -> updateRunning(deltaSeconds)
            RunnerState.START,
            RunnerState.PAUSED,
            RunnerState.GAME_OVER -> updatePresentation(deltaSeconds)
        }

        renderWorld()
        dispatchSnapshot(force = false)
    }

    fun startRun() {
        ensureSelectedHeroUnlocked()
        runnerState = RunnerState.RUNNING
        score = 0
        coinsCollected = 0
        speed = 18f
        distance = 0f
        runCoinsBanked = false
        currentLevel = 1
        runnerLane = 0
        targetLane = 0
        runnerX = 0f
        runnerY = 0f
        runnerVelocityY = 0f
        slideTimer = 0f
        jumpBufferTimer = 0f
        slideBufferTimer = 0f
        runAnimationTime = 0f
        coinSpin = 0f
        worldPulse = 0f
        spawnTimer = 1.7f
        powerUpSpawnTimer = 7.5f
        shieldTimer = 0f
        magnetTimer = 0f
        rewardBannerTimer = 0f
        rewardBanner = ""
        obstacles.clear()
        coins.clear()
        powerUps.clear()
        lastFrameNanos = 0L
        soundManager.startMusic()
        dispatchSnapshot(force = true)
    }

    fun pauseRunFromUi() {
        if (runnerState == RunnerState.RUNNING) {
            runnerState = RunnerState.PAUSED
            soundManager.pauseMusic()
            persistMissionState(force = true)
            dispatchSnapshot(force = true)
        }
    }

    fun resumeRunFromUi() {
        if (runnerState == RunnerState.PAUSED) {
            runnerState = RunnerState.RUNNING
            lastFrameNanos = 0L
            soundManager.startMusic()
            dispatchSnapshot(force = true)
        }
    }

    fun cycleSkin() {
        val unlockedHeroes = unlockedHeroes()
        if (unlockedHeroes.isEmpty()) {
            return
        }

        val currentIndex = unlockedHeroes.indexOf(selectedHero).let { if (it == -1) 0 else it }
        selectedHero = unlockedHeroes[(currentIndex + 1) % unlockedHeroes.size]
        GamePreferences.saveSelectedHero(context, selectedHero)
        dispatchSnapshot(force = true)
    }

    fun setMusicEnabled(enabled: Boolean) {
        soundManager.setMusicEnabled(enabled)
        dispatchSnapshot(force = true)
    }

    fun setSfxEnabled(enabled: Boolean) {
        soundManager.setSfxEnabled(enabled)
        dispatchSnapshot(force = true)
    }

    fun onHostPause() {
        if (runnerState == RunnerState.RUNNING) {
            runnerState = RunnerState.PAUSED
        }
        soundManager.pauseMusic()
        persistMissionState(force = true)
        dispatchSnapshot(force = true)
    }

    fun onHostResume() {
        lastFrameNanos = 0L
        if (runnerState == RunnerState.START || runnerState == RunnerState.GAME_OVER) {
            soundManager.startMusic()
        }
        dispatchSnapshot(force = true)
    }

    fun onTap() {
        if (runnerState == RunnerState.RUNNING) {
            jumpBufferTimer = 0.18f
        }
    }

    fun onSwipe(direction: SwipeDirection) {
        if (runnerState != RunnerState.RUNNING) {
            return
        }

        when (direction) {
            SwipeDirection.LEFT -> targetLane = max(-1, targetLane - 1)
            SwipeDirection.RIGHT -> targetLane = min(1, targetLane + 1)
            SwipeDirection.DOWN -> slideBufferTimer = 0.2f
        }
    }

    private fun updatePresentation(deltaSeconds: Float) {
        runAnimationTime += deltaSeconds * 4f
        coinSpin += deltaSeconds * 120f
        worldPulse += deltaSeconds * 0.75f
        rewardBannerTimer = (rewardBannerTimer - deltaSeconds).coerceAtLeast(0f)
    }

    private fun updateRunning(deltaSeconds: Float) {
        runAnimationTime += deltaSeconds * (speed * 0.22f)
        coinSpin += deltaSeconds * 170f
        worldPulse += deltaSeconds * (1f + speed * 0.015f)
        rewardBannerTimer = (rewardBannerTimer - deltaSeconds).coerceAtLeast(0f)
        distance += speed * deltaSeconds
        val reachedLevel = levelForDistance(distance)
        if (reachedLevel > currentLevel) {
            advanceToLevel(reachedLevel)
        }

        speed = min(46f, speed + deltaSeconds * (0.58f + currentLevel * 0.015f))
        score = max(score, (distance * 6.8f).toInt() + coinsCollected * 20 + currentLevel * 12)

        updateMissionAbsolute(MissionType.SURVIVE_DISTANCE, distance.toInt())

        powerUpSpawnTimer = (powerUpSpawnTimer - deltaSeconds).coerceAtLeast(0f)
        shieldTimer = (shieldTimer - deltaSeconds).coerceAtLeast(0f)
        magnetTimer = (magnetTimer - deltaSeconds).coerceAtLeast(0f)

        val desiredX = laneToX(targetLane)
        val deltaX = desiredX - runnerX
        val laneShiftSpeed = 8.4f
        val laneStep = laneShiftSpeed * deltaSeconds
        runnerX = if (abs(deltaX) <= laneStep) {
            desiredX
        } else {
            runnerX + sign(deltaX) * laneStep
        }
        runnerLane = when {
            runnerX < -laneWidth * 0.5f -> -1
            runnerX > laneWidth * 0.5f -> 1
            else -> 0
        }

        jumpBufferTimer = (jumpBufferTimer - deltaSeconds).coerceAtLeast(0f)
        slideBufferTimer = (slideBufferTimer - deltaSeconds).coerceAtLeast(0f)

        if (runnerY <= 0.001f && jumpBufferTimer > 0f) {
            slideTimer = 0f
            runnerVelocityY = 7.85f
            jumpBufferTimer = 0f
            soundManager.playJump()
        }

        if (runnerY <= 0.001f && slideBufferTimer > 0f && slideTimer <= 0f) {
            slideTimer = 0.68f
            slideBufferTimer = 0f
        }

        if (slideTimer > 0f) {
            slideTimer = (slideTimer - deltaSeconds).coerceAtLeast(0f)
        }

        runnerVelocityY -= 20f * deltaSeconds
        runnerY += runnerVelocityY * deltaSeconds
        if (runnerY < 0f) {
            runnerY = 0f
            runnerVelocityY = 0f
        }

        updatePatterns(deltaSeconds)
        updateObstacles(deltaSeconds)
        updatePowerUps(deltaSeconds)
        updateCoins(deltaSeconds)
        persistMissionState(force = false)
    }

    private fun updatePatterns(deltaSeconds: Float) {
        spawnTimer -= deltaSeconds
        if (spawnTimer > 0f || minActiveZ() <= -54f) {
            return
        }

        val recommendedLane = spawnPattern()
        maybeSpawnPowerUp(recommendedLane)

        val baseInterval = (2.2f - (speed - 18f) * 0.025f - (currentLevel - 1) * 0.035f).coerceAtLeast(0.95f)
        spawnTimer = baseInterval + random.nextFloat() * 0.35f
    }

    private fun spawnPattern(): Int {
        val roll = random.nextInt(100)
        return when {
            roll < 16 -> spawnSingleBlocker()
            roll < 30 -> spawnHurdleRow()
            roll < 42 -> spawnGateRow()
            roll < 56 -> spawnSpikeField()
            roll < 70 -> spawnBoulderCharge()
            roll < 82 -> spawnSplitLanePattern()
            roll < 92 -> spawnTempleCombo()
            else -> spawnStaggeredMix()
        }
    }

    private fun spawnSingleBlocker(): Int {
        val lane = randomLane()
        val safeLane = openLaneExcluding(setOf(lane))
        obstacles += Obstacle(lane, ObstacleType.BLOCKER, spawnZ, 1.65f, 2.7f, 1.3f)
        spawnCoinsInLane(safeLane, spawnZ - 10f, 4, 6f, 0.7f)
        return safeLane
    }

    private fun spawnHurdleRow(): Int {
        val lane = randomLane()
        obstacles += Obstacle(lane, ObstacleType.HURDLE, spawnZ, 1.5f, 1.15f, 1.0f)
        spawnCoinsInLane(lane, spawnZ - 10f, 5, 5.7f, 0.9f)
        return lane
    }

    private fun spawnGateRow(): Int {
        val lane = randomLane()
        obstacles += Obstacle(lane, ObstacleType.GATE, spawnZ, 1.7f, 2.3f, 1.05f)
        spawnCoinsInLane(lane, spawnZ - 12f, 4, 6f, 0.55f)
        return lane
    }

    private fun spawnSpikeField(): Int {
        val safeLane = randomLane()
        (-1..1).filter { it != safeLane }.forEachIndexed { index, lane ->
            obstacles += Obstacle(lane, ObstacleType.SPIKE, spawnZ - index * 5.5f, 1.7f, 0.95f, 1.1f)
        }
        spawnCoinsInLane(safeLane, spawnZ - 6f, 5, 5.4f, 0.78f)
        return safeLane
    }

    private fun spawnBoulderCharge(): Int {
        val lane = randomLane()
        val safeLane = openLaneExcluding(setOf(lane))
        obstacles += Obstacle(lane, ObstacleType.BOULDER, spawnZ, 1.55f, 1.55f, 1.55f)
        spawnCoinsInLane(safeLane, spawnZ - 9f, 5, 5.6f, 0.82f)
        return safeLane
    }

    private fun spawnSplitLanePattern(): Int {
        val safeLane = randomLane()
        (-1..1).filter { it != safeLane }.forEach { lane ->
            obstacles += Obstacle(
                lane = lane,
                type = if (random.nextBoolean()) ObstacleType.BLOCKER else ObstacleType.SPIKE,
                z = spawnZ - if (lane < safeLane) 0f else 6f,
                width = 1.65f,
                height = if (lane < safeLane) 2.6f else 0.95f,
                depth = 1.2f
            )
        }
        spawnCoinsInLane(safeLane, spawnZ - 6f, 6, 5.4f, 0.72f)
        return safeLane
    }

    private fun spawnTempleCombo(): Int {
        val lane = randomLane()
        val supportLane = openLaneExcluding(setOf(lane))
        obstacles += Obstacle(lane, ObstacleType.GATE, spawnZ, 1.7f, 2.3f, 1.05f)
        obstacles += Obstacle(supportLane, ObstacleType.SPIKE, spawnZ - 12f, 1.7f, 0.95f, 1.1f)
        obstacles += Obstacle(lane, ObstacleType.BOULDER, spawnZ - 22f, 1.55f, 1.55f, 1.55f)
        spawnCoinsInLane(oppositeLane(supportLane), spawnZ - 8f, 5, 5.1f, 0.76f)
        return oppositeLane(supportLane)
    }

    private fun spawnStaggeredMix(): Int {
        val firstLane = randomLane()
        val secondLane = if (firstLane == 0) listOf(-1, 1).random(random) else 0
        val safeLane = openLaneExcluding(setOf(firstLane, secondLane))
        obstacles += Obstacle(firstLane, ObstacleType.BLOCKER, spawnZ, 1.65f, 2.6f, 1.3f)
        obstacles += Obstacle(
            secondLane,
            if (random.nextBoolean()) ObstacleType.HURDLE else ObstacleType.SPIKE,
            spawnZ - 13f,
            1.5f,
            if (secondLane == 0) 1.0f else 0.95f,
            1.05f
        )
        spawnCoinsInLane(safeLane, spawnZ - 8f, 5, 5.5f, 0.78f)
        return safeLane
    }

    private fun maybeSpawnPowerUp(lane: Int) {
        if (powerUpSpawnTimer > 0f) {
            return
        }

        val preferredType = when {
            shieldTimer <= 0f && magnetTimer <= 0f -> if (random.nextBoolean()) PowerUpType.SHIELD else PowerUpType.MAGNET
            shieldTimer <= 0f -> PowerUpType.SHIELD
            else -> PowerUpType.MAGNET
        }

        powerUps += PowerUp(
            lane = lane,
            type = preferredType,
            z = spawnZ - 18f,
            height = if (preferredType == PowerUpType.SHIELD) 1.1f else 0.95f
        )
        powerUpSpawnTimer = 8.8f + random.nextFloat() * 4.4f
    }

    private fun updateObstacles(deltaSeconds: Float) {
        val iterator = obstacles.iterator()
        while (iterator.hasNext()) {
            val obstacle = iterator.next()
            obstacle.z += speed * deltaSeconds
            if (obstacle.type == ObstacleType.BOULDER) {
                obstacle.spin += deltaSeconds * 260f
            }

            if (obstacle.z > 10f) {
                iterator.remove()
                updateMissionIncrement(MissionType.DODGE_HAZARDS, 1)
                continue
            }

            if (abs(obstacle.z) < obstacle.depth * 1.45f && abs(runnerX - obstacleX(obstacle)) < laneWidth * 0.4f) {
                val collisionIsActive = when (obstacle.type) {
                    ObstacleType.HURDLE -> runnerY < obstacle.height + 0.3f
                    ObstacleType.GATE -> slideTimer <= 0f
                    ObstacleType.SPIKE -> runnerY < obstacle.height + 0.28f
                    ObstacleType.BLOCKER,
                    ObstacleType.BOULDER -> true
                }

                if (!collisionIsActive) {
                    continue
                }

                if (shieldTimer > 0f) {
                    shieldTimer = 0f
                    iterator.remove()
                    rewardBanner = "Shield blocked a fatal hit"
                    rewardBannerTimer = 2.2f
                    continue
                }

                handleCrash()
                return
            }
        }
    }

    private fun updatePowerUps(deltaSeconds: Float) {
        val iterator = powerUps.iterator()
        while (iterator.hasNext()) {
            val powerUp = iterator.next()
            powerUp.z += speed * deltaSeconds
            if (powerUp.z > 10f || powerUp.collected) {
                iterator.remove()
                continue
            }

            if (abs(powerUp.z) < 1.2f && abs(runnerX - laneToX(powerUp.lane)) < laneWidth * 0.36f) {
                powerUp.collected = true
                activatePowerUp(powerUp.type)
                iterator.remove()
            }
        }
    }

    private fun activatePowerUp(type: PowerUpType) {
        when (type) {
            PowerUpType.SHIELD -> shieldTimer = 7.5f
            PowerUpType.MAGNET -> magnetTimer = 8.8f
        }
        score += 120
        updateMissionIncrement(MissionType.USE_POWER_UPS, 1)
        rewardBanner = if (type == PowerUpType.SHIELD) "Shield relic activated" else "Magnet relic activated"
        rewardBannerTimer = 2.4f
    }

    private fun updateCoins(deltaSeconds: Float) {
        val iterator = coins.iterator()
        while (iterator.hasNext()) {
            val coin = iterator.next()
            coin.z += speed * deltaSeconds
            if (coin.z > 9f || coin.collected) {
                iterator.remove()
                continue
            }

            val pickupRadius = if (magnetTimer > 0f) laneWidth * 1.35f else laneWidth * 0.34f
            val pickupDepth = if (magnetTimer > 0f) 6.4f else 1.1f
            if (abs(coin.z) < pickupDepth && abs(runnerX - laneToX(coin.lane)) < pickupRadius) {
                coin.collected = true
                coinsCollected += 1
                score += 25
                updateMissionIncrement(MissionType.COLLECT_COINS, 1)
            }
        }
    }

    private fun handleCrash() {
        if (runnerState == RunnerState.GAME_OVER) {
            return
        }

        persistRunCoinsIfNeeded()
        persistMissionState(force = true)
        runnerState = RunnerState.GAME_OVER
        soundManager.playCrash()
        soundManager.pauseMusic()

        if (score > highScore) {
            highScore = score
            GamePreferences.saveHighScore(context, highScore)
        }

        ensureSelectedHeroUnlocked()
        dispatchSnapshot(force = true)
    }

    private fun persistRunCoinsIfNeeded() {
        if (runCoinsBanked) {
            return
        }

        bankedCoins += coinsCollected
        GamePreferences.saveTotalCoins(context, bankedCoins)
        runCoinsBanked = true
    }

    private fun renderWorld() {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        GLES20.glUseProgram(shaderProgram)
        GLES20.glUniform4fv(fogColorHandle, 1, fogColor, 0)
        GLES20.glUniform3fv(lightDirectionHandle, 1, lightDirection, 0)

        val cameraBob = sin(runAnimationTime * 0.55f) * 0.08f
        val portraitCamera = screenAspectRatio < 1f
        val cameraDistance = if (portraitCamera) 11.6f else 9.0f
        val cameraX = runnerX * if (portraitCamera) 0.18f else 0.24f
        val cameraY = (if (slideTimer > 0f) 3.0f else 3.42f + runnerY * 0.12f) + if (portraitCamera) 0.16f else 0f
        val lookAheadZ = if (portraitCamera) -15.5f else -13f
        Matrix.setLookAtM(
            viewMatrix,
            0,
            cameraX,
            cameraY + cameraBob,
            cameraDistance,
            runnerX * 0.18f,
            groundY + 1.22f + runnerY * 0.12f,
            lookAheadZ,
            0f,
            1f,
            0f
        )

        drawTrack()
        drawEnvironment()
        drawCoins()
        drawPowerUps()
        drawObstacles()
        drawRunner()
    }

    private fun drawTrack() {
        val segmentLength = 10f
        val trackWidth = laneWidth * 3.35f
        val scroll = distance % segmentLength

        for (index in 0..16) {
            val centerZ = scroll - index * segmentLength - 6f
            val stoneColor = if (index % 2 == 0) color(0.23f, 0.22f, 0.18f) else color(0.20f, 0.19f, 0.16f)
            drawCube(0f, groundY - 0.12f, centerZ, trackWidth, 0.14f, segmentLength, stoneColor)
            drawCube(-laneWidth, groundY - 0.02f, centerZ, 0.07f, 0.03f, segmentLength, color(0.68f, 0.58f, 0.32f))
            drawCube(0f, groundY - 0.02f, centerZ, 0.07f, 0.03f, segmentLength, color(0.68f, 0.58f, 0.32f))
            drawCube(laneWidth, groundY - 0.02f, centerZ, 0.07f, 0.03f, segmentLength, color(0.68f, 0.58f, 0.32f))
            drawCube(-trackWidth * 0.52f, groundY + 0.5f, centerZ, 0.26f, 1.0f, segmentLength, color(0.12f, 0.11f, 0.09f))
            drawCube(trackWidth * 0.52f, groundY + 0.5f, centerZ, 0.26f, 1.0f, segmentLength, color(0.12f, 0.11f, 0.09f))
        }
    }

    private fun drawEnvironment() {
        val pillarSpacing = 16f
        val pillarScroll = (distance * 0.72f) % pillarSpacing
        for (index in 0..10) {
            val z = pillarScroll - index * pillarSpacing - 10f
            drawTemplePillar(-7.2f, z, damaged = index % 2 == 0)
            drawTemplePillar(7.2f, z - 4.5f, damaged = index % 2 != 0)
            drawTorch(-5.4f, z + 2f)
            drawTorch(5.4f, z - 1f)
        }

        val archSpacing = 24f
        val archScroll = (distance * 0.85f) % archSpacing
        for (index in 0..7) {
            val z = archScroll - index * archSpacing - 16f
            drawTempleArch(z)
        }

        val cliffSpacing = 30f
        val cliffScroll = (distance * 0.48f) % cliffSpacing
        for (index in 0..8) {
            val z = cliffScroll - index * cliffSpacing - 28f
            drawCliff(-13.5f, z, 4.8f + (index % 3) * 1.1f)
            drawCliff(13.5f, z - 6f, 5.2f + (index % 4) * 0.8f)
        }
    }

    private fun drawTemplePillar(x: Float, z: Float, damaged: Boolean) {
        val stone = color(0.33f, 0.30f, 0.24f)
        drawCube(x, groundY + 1.4f, z, 0.72f, 2.8f, 0.72f, stone)
        drawCube(x, groundY + 2.95f, z, 0.92f, 0.26f, 0.92f, color(0.40f, 0.36f, 0.29f))
        drawCube(x, groundY + 0.06f, z, 1.08f, 0.14f, 1.08f, color(0.27f, 0.24f, 0.19f))
        if (damaged) {
            drawCube(x + 0.16f, groundY + 2.5f, z + 0.14f, 0.32f, 0.85f, 0.32f, color(0.20f, 0.18f, 0.14f), rotationZ = 14f)
        }
    }

    private fun drawTempleArch(z: Float) {
        val stone = color(0.38f, 0.33f, 0.25f)
        drawCube(-4.4f, groundY + 2.35f, z, 0.32f, 3.55f, 0.32f, stone)
        drawCube(4.4f, groundY + 2.35f, z, 0.32f, 3.55f, 0.32f, stone)
        drawCube(0f, groundY + 4.0f, z, 4.78f, 0.22f, 0.32f, stone)
        drawCube(0f, groundY + 3.4f, z, 3.4f, 0.12f, 0.08f, color(0.83f, 0.58f, 0.16f, 0.65f))
    }

    private fun drawTorch(x: Float, z: Float) {
        drawCube(x, groundY + 0.8f, z, 0.16f, 1.6f, 0.16f, color(0.23f, 0.17f, 0.10f))
        val flamePulse = 0.28f + sin(worldPulse * 6.0f + z) * 0.04f
        drawCube(x, groundY + 1.75f, z, flamePulse, 0.38f, flamePulse, color(0.98f, 0.58f, 0.16f, 0.88f))
        drawCube(x, groundY + 1.96f, z, flamePulse * 0.62f, 0.22f, flamePulse * 0.62f, color(1f, 0.88f, 0.56f, 0.82f))
    }

    private fun drawCliff(x: Float, z: Float, height: Float) {
        drawCube(x, groundY + height * 0.5f, z, 3.0f, height, 2.8f, color(0.15f, 0.16f, 0.12f))
        drawCube(x + sign(x) * 0.3f, groundY + height - 0.7f, z, 2.0f, 0.28f, 1.8f, color(0.24f, 0.22f, 0.16f))
    }

    private fun drawCoins() {
        for (coin in coins) {
            val pulse = 0.40f + sin((coinSpin + coin.z * 12f) * 0.03f) * 0.05f
            drawCube(
                x = laneToX(coin.lane),
                y = groundY + coin.height,
                z = coin.z,
                scaleX = pulse,
                scaleY = pulse,
                scaleZ = 0.12f,
                color = color(0.97f, 0.79f, 0.28f),
                rotationY = coinSpin + coin.z * 1.5f,
                rotationZ = 18f
            )
        }
    }

    private fun drawPowerUps() {
        for (powerUp in powerUps) {
            val x = laneToX(powerUp.lane)
            when (powerUp.type) {
                PowerUpType.SHIELD -> {
                    val wobble = sin(worldPulse * 3.8f + powerUp.z) * 10f
                    drawCube(x, groundY + powerUp.height, powerUp.z, 0.62f, 0.62f, 0.62f, color(0.28f, 0.86f, 0.98f), rotationY = coinSpin + wobble)
                    drawCube(x, groundY + powerUp.height, powerUp.z, 0.9f, 0.08f, 0.9f, color(0.70f, 0.97f, 1f, 0.55f), rotationY = coinSpin * 1.3f)
                }

                PowerUpType.MAGNET -> {
                    drawCube(x - 0.22f, groundY + powerUp.height, powerUp.z, 0.18f, 0.72f, 0.18f, color(1.0f, 0.56f, 0.30f), rotationY = coinSpin)
                    drawCube(x + 0.22f, groundY + powerUp.height, powerUp.z, 0.18f, 0.72f, 0.18f, color(0.34f, 0.92f, 0.98f), rotationY = coinSpin)
                    drawCube(x, groundY + powerUp.height + 0.32f, powerUp.z, 0.52f, 0.18f, 0.18f, color(0.95f, 0.95f, 1f), rotationY = coinSpin)
                }
            }
        }
    }

    private fun drawObstacles() {
        for (obstacle in obstacles) {
            val x = obstacleX(obstacle)
            when (obstacle.type) {
                ObstacleType.HURDLE -> {
                    drawCube(x, groundY + 0.52f, obstacle.z, 1.34f, 1.06f, 0.74f, color(0.63f, 0.26f, 0.16f))
                    drawCube(x, groundY + 0.96f, obstacle.z, 1.48f, 0.16f, 1.04f, color(0.91f, 0.70f, 0.42f))
                }

                ObstacleType.GATE -> {
                    drawCube(x, groundY + 1.78f, obstacle.z, 1.62f, 0.30f, 0.90f, color(0.45f, 0.40f, 0.18f))
                    drawCube(x - 0.64f, groundY + 0.96f, obstacle.z, 0.26f, 1.9f, 0.26f, color(0.31f, 0.25f, 0.13f))
                    drawCube(x + 0.64f, groundY + 0.96f, obstacle.z, 0.26f, 1.9f, 0.26f, color(0.31f, 0.25f, 0.13f))
                }

                ObstacleType.BLOCKER -> {
                    drawCube(x, groundY + 1.18f, obstacle.z, 1.58f, 2.34f, 1.06f, color(0.45f, 0.35f, 0.22f))
                    drawCube(x, groundY + 2.28f, obstacle.z, 1.18f, 0.36f, 1.18f, color(0.82f, 0.63f, 0.30f))
                    drawCube(x, groundY + 1.16f, obstacle.z + 0.56f, 0.88f, 0.88f, 0.10f, color(0.65f, 0.16f, 0.12f))
                }

                ObstacleType.SPIKE -> {
                    drawCube(x, groundY + 0.22f, obstacle.z, 1.72f, 0.14f, 1.10f, color(0.26f, 0.23f, 0.18f))
                    drawCube(x - 0.46f, groundY + 0.54f, obstacle.z, 0.22f, 0.66f, 0.22f, color(0.72f, 0.72f, 0.70f), rotationZ = -8f)
                    drawCube(x, groundY + 0.62f, obstacle.z, 0.24f, 0.82f, 0.24f, color(0.74f, 0.74f, 0.72f))
                    drawCube(x + 0.46f, groundY + 0.54f, obstacle.z, 0.22f, 0.66f, 0.22f, color(0.72f, 0.72f, 0.70f), rotationZ = 8f)
                }

                ObstacleType.BOULDER -> {
                    drawCube(x, groundY + 0.88f, obstacle.z, 1.22f, 1.22f, 1.22f, color(0.38f, 0.35f, 0.31f), rotationX = obstacle.spin, rotationZ = obstacle.spin * 0.7f)
                    drawCube(x, groundY + 0.88f, obstacle.z, 0.72f, 0.72f, 0.72f, color(0.50f, 0.46f, 0.40f), rotationX = obstacle.spin, rotationY = obstacle.spin * 0.5f)
                }
            }
        }
    }

    private fun drawRunner() {
        val baseY = groundY + runnerY
        val sideLean = (laneToX(targetLane) - runnerX) * 8f
        val stride = sin(runAnimationTime) * 24f
        val skinTone = selectedHero.skinTone
        val hairColor = selectedHero.hairColor
        val primary = selectedHero.outfitPrimary
        val secondary = selectedHero.outfitSecondary
        val gear = selectedHero.gearColor

        if (shieldTimer > 0f) {
            drawCube(runnerX, baseY + 1.62f, 0f, 1.34f, 2.04f, 1.22f, color(0.56f, 0.95f, 1f, 0.18f), rotationY = worldPulse * 80f)
        }

        if (magnetTimer > 0f) {
            val magnetOffset = sin(worldPulse * 4.4f) * 0.18f
            drawCube(runnerX - 0.82f, baseY + 1.42f, magnetOffset, 0.14f, 1.02f, 0.14f, color(0.99f, 0.58f, 0.29f, 0.62f))
            drawCube(runnerX + 0.82f, baseY + 1.42f, -magnetOffset, 0.14f, 1.02f, 0.14f, color(0.34f, 0.92f, 0.98f, 0.62f))
        }

        if (slideTimer > 0f) {
            drawCube(runnerX, baseY + 0.56f, 0f, 1.24f, 0.64f, 1.98f, primary, rotationY = sideLean)
            drawCube(runnerX, baseY + 0.72f, -0.14f, 0.64f, 0.22f, 1.12f, secondary, rotationY = sideLean)
            drawCube(runnerX - 0.50f, baseY + 0.78f, 0.18f, 0.44f, 0.44f, 0.44f, skinTone, rotationY = sideLean)
            drawCube(runnerX + 0.35f, baseY + 0.22f, 0.35f, 0.78f, 0.22f, 0.58f, gear, rotationY = sideLean)
        } else {
            drawCube(runnerX, baseY + 1.54f, 0f, 0.92f, 1.22f, 0.64f, primary, rotationY = sideLean)
            drawCube(runnerX, baseY + 1.72f, -0.22f, 0.68f, 0.18f, 0.26f, secondary, rotationY = sideLean)
            drawCube(runnerX, baseY + 1.46f, -0.38f, 0.52f, 0.64f, 0.26f, gear, rotationY = sideLean)
            drawCube(runnerX, baseY + 2.48f, 0.04f, 0.60f, 0.62f, 0.58f, skinTone, rotationY = sideLean)
            drawCube(runnerX, baseY + 2.82f, -0.02f, 0.66f, 0.18f, 0.50f, hairColor, rotationY = sideLean)
            drawCube(runnerX - 0.52f, baseY + 1.56f, 0.02f, 0.18f, 0.96f, 0.18f, gear, rotationX = -stride, rotationY = sideLean)
            drawCube(runnerX + 0.52f, baseY + 1.56f, 0.02f, 0.18f, 0.96f, 0.18f, gear, rotationX = stride, rotationY = sideLean)
            drawCube(runnerX - 0.22f, baseY + 0.56f, 0f, 0.24f, 1.08f, 0.24f, primary, rotationX = stride * 1.2f, rotationY = sideLean)
            drawCube(runnerX + 0.22f, baseY + 0.56f, 0f, 0.24f, 1.08f, 0.24f, primary, rotationX = -stride * 1.2f, rotationY = sideLean)
            drawCube(runnerX - 0.22f, baseY + 0.02f, 0.08f, 0.28f, 0.26f, 0.40f, gear, rotationY = sideLean)
            drawCube(runnerX + 0.22f, baseY + 0.02f, 0.08f, 0.28f, 0.26f, 0.40f, gear, rotationY = sideLean)
        }
    }

    private fun drawCube(
        x: Float,
        y: Float,
        z: Float,
        scaleX: Float,
        scaleY: Float,
        scaleZ: Float,
        color: FloatArray,
        rotationX: Float = 0f,
        rotationY: Float = 0f,
        rotationZ: Float = 0f
    ) {
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, x, y, z)
        if (rotationX != 0f) Matrix.rotateM(modelMatrix, 0, rotationX, 1f, 0f, 0f)
        if (rotationY != 0f) Matrix.rotateM(modelMatrix, 0, rotationY, 0f, 1f, 0f)
        if (rotationZ != 0f) Matrix.rotateM(modelMatrix, 0, rotationZ, 0f, 0f, 1f)
        Matrix.scaleM(modelMatrix, 0, scaleX, scaleY, scaleZ)

        Matrix.multiplyMM(tempMatrix, 0, viewMatrix, 0, modelMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, tempMatrix, 0)

        GLES20.glUniformMatrix4fv(modelHandle, 1, false, modelMatrix, 0)
        GLES20.glUniformMatrix4fv(mvpHandle, 1, false, mvpMatrix, 0)
        GLES20.glUniform4fv(colorHandle, 1, color, 0)
        cubeMesh.draw(positionHandle, normalHandle)
    }

    private fun spawnCoinsInLane(lane: Int, startZ: Float, count: Int, spacing: Float, height: Float) {
        repeat(count) { index ->
            coins += Coin(lane = lane, z = startZ - index * spacing, height = height)
        }
    }

    private fun currentMission(): MissionDefinition {
        return AdventureContent.missions[missionIndex % AdventureContent.missions.size]
    }

    private fun laneToX(lane: Int): Float = lane * laneWidth

    private fun obstacleX(obstacle: Obstacle): Float = laneToX(obstacle.lane)

    private fun minActiveZ(): Float {
        val obstacleMin = obstacles.minOfOrNull { it.z } ?: Float.POSITIVE_INFINITY
        val coinMin = coins.minOfOrNull { it.z } ?: Float.POSITIVE_INFINITY
        val powerUpMin = powerUps.minOfOrNull { it.z } ?: Float.POSITIVE_INFINITY
        return min(obstacleMin, min(coinMin, powerUpMin))
    }

    private fun randomLane(): Int = listOf(-1, 0, 1).random(random)

    private fun openLaneExcluding(blockedLanes: Set<Int>): Int {
        val openLanes = (-1..1).filterNot { blockedLanes.contains(it) }
        return if (openLanes.isEmpty()) 0 else openLanes.random(random)
    }

    private fun oppositeLane(lane: Int): Int = when (lane) {
        -1 -> 1
        1 -> -1
        else -> if (random.nextBoolean()) -1 else 1
    }

    private fun unlockedHeroes(): List<RunnerCharacter> {
        val totalCoins = effectiveTotalCoins()
        return RunnerCharacter.entries.filter { totalCoins >= it.unlockCoins }
    }

    private fun ensureSelectedHeroUnlocked() {
        if (effectiveTotalCoins() >= selectedHero.unlockCoins) {
            return
        }

        selectedHero = unlockedHeroes().lastOrNull() ?: RunnerCharacter.ARIA
        GamePreferences.saveSelectedHero(context, selectedHero)
    }

    private fun effectiveTotalCoins(): Int {
        return bankedCoins + if (runCoinsBanked) 0 else coinsCollected
    }

    private fun normalizeMissionState() {
        if (AdventureContent.missions.isEmpty()) {
            missionIndex = 0
            missionProgress = 0
            lastPersistedMissionProgress = 0
            return
        }

        missionIndex = missionIndex.mod(AdventureContent.missions.size)
        missionProgress = missionProgress.coerceAtLeast(0)
        lastPersistedMissionProgress = missionProgress
    }

    private fun updateMissionIncrement(type: MissionType, amount: Int) {
        val mission = currentMission()
        if (mission.type != type) {
            return
        }

        val updatedProgress = (missionProgress + amount).coerceAtMost(mission.target)
        if (updatedProgress == missionProgress) {
            return
        }

        missionProgress = updatedProgress
        if (missionProgress >= mission.target) {
            completeMission()
        }
    }

    private fun updateMissionAbsolute(type: MissionType, value: Int) {
        val mission = currentMission()
        if (mission.type != type) {
            return
        }

        val updatedProgress = value.coerceAtMost(mission.target)
        if (updatedProgress <= missionProgress) {
            return
        }

        missionProgress = updatedProgress
        if (missionProgress >= mission.target) {
            completeMission()
        }
    }

    private fun completeMission() {
        val finishedMission = currentMission()
        bankedCoins += finishedMission.rewardCoins
        GamePreferences.saveTotalCoins(context, bankedCoins)
        rewardBanner = "Quest complete: +${finishedMission.rewardCoins} coins"
        rewardBannerTimer = 3.6f

        missionIndex = (missionIndex + 1) % AdventureContent.missions.size
        missionProgress = 0
        lastPersistedMissionProgress = 0
        GamePreferences.saveMissionIndex(context, missionIndex)
        GamePreferences.saveMissionProgress(context, missionProgress)
        ensureSelectedHeroUnlocked()
    }

    private fun persistMissionState(force: Boolean) {
        if (missionProgress == lastPersistedMissionProgress) {
            return
        }

        val mission = currentMission()
        val shouldPersist = force || mission.type != MissionType.SURVIVE_DISTANCE || missionProgress - lastPersistedMissionProgress >= 10
        if (!shouldPersist) {
            return
        }

        GamePreferences.saveMissionIndex(context, missionIndex)
        GamePreferences.saveMissionProgress(context, missionProgress)
        lastPersistedMissionProgress = missionProgress
    }

    private fun levelForDistance(distanceCovered: Float): Int {
        return (distanceCovered / 140f).toInt() + 1
    }

    private fun advanceToLevel(newLevel: Int) {
        while (currentLevel < newLevel) {
            currentLevel += 1
            val rewardCoins = 12 + currentLevel * 4
            bankedCoins += rewardCoins
            GamePreferences.saveTotalCoins(context, bankedCoins)
            rewardBanner = "Level $currentLevel reached in ${zoneNameForLevel(currentLevel)}: +$rewardCoins coins"
            rewardBannerTimer = 3.4f
        }
        ensureSelectedHeroUnlocked()
    }

    private fun zoneNameForLevel(level: Int): String {
        return when (level) {
            1, 2 -> "Temple Gate"
            3, 4 -> "Sunfire Causeway"
            5, 6 -> "Relic Vault"
            7, 8 -> "Moonlit Quarry"
            9, 10 -> "Storm Bridge"
            else -> "Sky Ruins"
        }
    }

    private fun nextUnlockLabel(): String {
        val totalCoins = effectiveTotalCoins()
        val nextHero = RunnerCharacter.entries.firstOrNull { totalCoins < it.unlockCoins }
        return if (nextHero == null) {
            "All heroes unlocked"
        } else {
            "${nextHero.displayName} at ${nextHero.unlockCoins} coins (${nextHero.unlockCoins - totalCoins} left)"
        }
    }

    private fun activeMissionLabel(): String {
        val mission = currentMission()
        return "Quest: ${mission.title} ${missionProgress}/${mission.target}"
    }

    private fun activeMissionRewardLabel(): String {
        return if (rewardBannerTimer > 0f && rewardBanner.isNotBlank()) {
            rewardBanner
        } else {
            "Reward: +${currentMission().rewardCoins} coins"
        }
    }

    private fun activePowerUpLabel(): String {
        val statuses = mutableListOf<String>()
        if (shieldTimer > 0f) {
            statuses += "Shield ${shieldTimer.toInt() + 1}s"
        }
        if (magnetTimer > 0f) {
            statuses += "Magnet ${magnetTimer.toInt() + 1}s"
        }
        return if (statuses.isEmpty()) "No active relic" else statuses.joinToString("  ")
    }

    private fun color(r: Float, g: Float, b: Float, a: Float = 1f): FloatArray {
        return floatArrayOf(r, g, b, a)
    }

    private fun dispatchSnapshot(force: Boolean) {
        val now = System.currentTimeMillis()
        if (!force && now - lastSnapshotDispatchTime < 140L) {
            return
        }

        lastSnapshotDispatchTime = now
        val snapshot = GameSnapshot(
            state = runnerState,
            score = score,
            highScore = highScore,
            coins = coinsCollected,
            totalCoins = effectiveTotalCoins(),
            speedKph = (speed * 11.5f).toInt(),
            level = currentLevel,
            zoneName = zoneNameForLevel(currentLevel),
            selectedHero = selectedHero.displayName,
            selectedHeroTitle = selectedHero.title,
            nextUnlock = nextUnlockLabel(),
            activeMission = activeMissionLabel(),
            activeMissionReward = activeMissionRewardLabel(),
            activePowerUp = activePowerUpLabel(),
            musicEnabled = soundManager.isMusicEnabled(),
            sfxEnabled = soundManager.isSfxEnabled()
        )

        mainHandler.post {
            uiListener?.onSnapshot(snapshot)
        }
    }

    companion object {
        private const val VERTEX_SHADER = """
            uniform mat4 uMvpMatrix;
            uniform mat4 uModelMatrix;
            uniform vec3 uLightDirection;
            attribute vec3 aPosition;
            attribute vec3 aNormal;
            varying float vLight;
            varying float vFog;

            void main() {
                vec4 worldPosition = uModelMatrix * vec4(aPosition, 1.0);
                vec3 worldNormal = normalize(mat3(uModelMatrix) * aNormal);
                float diffuse = max(dot(normalize(uLightDirection), worldNormal), 0.0);
                vLight = 0.28 + diffuse * 0.72;
                vFog = clamp((abs(worldPosition.z) - 16.0) / 92.0, 0.0, 1.0);
                gl_Position = uMvpMatrix * vec4(aPosition, 1.0);
            }
        """

        private const val FRAGMENT_SHADER = """
            precision mediump float;
            uniform vec4 uColor;
            uniform vec4 uFogColor;
            varying float vLight;
            varying float vFog;

            void main() {
                vec4 litColor = vec4(uColor.rgb * vLight, uColor.a);
                gl_FragColor = vec4(mix(litColor.rgb, uFogColor.rgb, vFog), litColor.a);
            }
        """
    }
}

