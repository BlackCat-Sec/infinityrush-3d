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
    private val vpMatrix = FloatArray(16)
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
    private var selectedSkin = GamePreferences.getSelectedSkin(context)

    private var score = 0
    private var coinsCollected = 0
    private var speed = 18f
    private var distance = 0f
    private var runCoinsBanked = false

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

    private val obstacles = mutableListOf<Obstacle>()
    private val coins = mutableListOf<Coin>()
    private val powerUps = mutableListOf<PowerUp>()

    private var lastFrameNanos = 0L
    private var lastSnapshotDispatchTime = 0L

    private val laneWidth = 2.35f
    private val groundY = -1.15f
    private val spawnZ = -72f
    private val fogColor = floatArrayOf(0.07f, 0.11f, 0.18f, 1f)
    private val lightDirection = floatArrayOf(-0.35f, 0.9f, 0.25f)

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
        ensureSelectedSkinUnlocked()
        soundManager.startMusic()
        dispatchSnapshot(force = true)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        val aspectRatio = width.toFloat() / height.toFloat()
        Matrix.perspectiveM(projectionMatrix, 0, 56f, aspectRatio, 0.1f, 180f)
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
        ensureSelectedSkinUnlocked()
        runnerState = RunnerState.RUNNING
        score = 0
        coinsCollected = 0
        speed = 18f
        distance = 0f
        runCoinsBanked = false
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
        val unlockedSkins = unlockedSkins()
        if (unlockedSkins.isEmpty()) {
            return
        }

        val currentIndex = unlockedSkins.indexOf(selectedSkin).let { if (it == -1) 0 else it }
        selectedSkin = unlockedSkins[(currentIndex + 1) % unlockedSkins.size]
        GamePreferences.saveSelectedSkin(context, selectedSkin)
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
        coinSpin += deltaSeconds * 140f
        worldPulse += deltaSeconds * 0.8f
    }

    private fun updateRunning(deltaSeconds: Float) {
        runAnimationTime += deltaSeconds * (speed * 0.22f)
        coinSpin += deltaSeconds * 190f
        worldPulse += deltaSeconds * (1f + speed * 0.015f)
        distance += speed * deltaSeconds
        speed = min(42f, speed + deltaSeconds * 0.55f)
        score = max(score, (distance * 6.5f).toInt() + coinsCollected * 20)

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
    }

    private fun updatePatterns(deltaSeconds: Float) {
        spawnTimer -= deltaSeconds
        if (spawnTimer > 0f || minActiveZ() <= -54f) {
            return
        }

        val recommendedLane = spawnPattern()
        maybeSpawnPowerUp(recommendedLane)

        val baseInterval = (2.2f - (speed - 18f) * 0.025f).coerceAtLeast(1.2f)
        spawnTimer = baseInterval + random.nextFloat() * 0.35f
    }

    private fun spawnPattern(): Int {
        val roll = random.nextInt(100)
        return when {
            roll < 20 -> spawnSingleBlocker()
            roll < 38 -> spawnHurdleRow()
            roll < 54 -> spawnGateRow()
            roll < 70 -> spawnSplitLanePattern()
            roll < 86 -> spawnCoinTunnel()
            else -> spawnStaggeredMix()
        }
    }

    private fun spawnSingleBlocker(): Int {
        val lane = randomLane()
        val safeLane = openLaneExcluding(setOf(lane))
        obstacles += Obstacle(lane, ObstacleType.BLOCKER, spawnZ, 1.65f, 2.6f, 1.3f)
        spawnCoinsInLane(safeLane, spawnZ - 10f, 4, 6f, 0.7f)
        return safeLane
    }

    private fun spawnHurdleRow(): Int {
        val lane = randomLane()
        obstacles += Obstacle(lane, ObstacleType.HURDLE, spawnZ, 1.45f, 1.05f, 1.0f)
        spawnCoinsInLane(lane, spawnZ - 9f, 5, 5.8f, 0.85f)
        return lane
    }

    private fun spawnGateRow(): Int {
        val lane = randomLane()
        obstacles += Obstacle(lane, ObstacleType.GATE, spawnZ, 1.65f, 2.25f, 1.05f)
        spawnCoinsInLane(lane, spawnZ - 12f, 4, 6f, 0.55f)
        return lane
    }

    private fun spawnSplitLanePattern(): Int {
        val safeLane = randomLane()
        (-1..1).filter { it != safeLane }.forEach { lane ->
            obstacles += Obstacle(
                lane = lane,
                type = ObstacleType.BLOCKER,
                z = spawnZ - if (lane < safeLane) 0f else 6f,
                width = 1.65f,
                height = 2.6f,
                depth = 1.3f
            )
        }
        spawnCoinsInLane(safeLane, spawnZ - 6f, 6, 5.5f, 0.7f)
        return safeLane
    }

    private fun spawnCoinTunnel(): Int {
        val lane = randomLane()
        obstacles += Obstacle(lane, ObstacleType.HURDLE, spawnZ, 1.45f, 1.05f, 1.0f)
        obstacles += Obstacle(lane, ObstacleType.GATE, spawnZ - 18f, 1.65f, 2.25f, 1.05f)
        spawnCoinsInLane(lane, spawnZ - 5f, 7, 4.8f, 0.8f)
        return lane
    }

    private fun spawnStaggeredMix(): Int {
        val firstLane = randomLane()
        val secondLane = if (firstLane == 0) listOf(-1, 1).random(random) else 0
        val safeLane = openLaneExcluding(setOf(firstLane, secondLane))
        obstacles += Obstacle(firstLane, ObstacleType.BLOCKER, spawnZ, 1.65f, 2.6f, 1.3f)
        obstacles += Obstacle(
            secondLane,
            if (random.nextBoolean()) ObstacleType.HURDLE else ObstacleType.GATE,
            spawnZ - 14f,
            1.5f,
            2.1f,
            1.05f
        )
        spawnCoinsInLane(safeLane, spawnZ - 8f, 5, 5.6f, 0.75f)
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
        powerUpSpawnTimer = 8.5f + random.nextFloat() * 4.5f
    }

    private fun updateObstacles(deltaSeconds: Float) {
        val iterator = obstacles.iterator()
        while (iterator.hasNext()) {
            val obstacle = iterator.next()
            obstacle.z += speed * deltaSeconds
            if (obstacle.z > 10f) {
                iterator.remove()
                continue
            }

            if (abs(obstacle.z) < obstacle.depth * 1.4f && abs(runnerX - laneToX(obstacle.lane)) < laneWidth * 0.4f) {
                val collisionIsActive = when (obstacle.type) {
                    ObstacleType.HURDLE -> runnerY < obstacle.height + 0.25f
                    ObstacleType.GATE -> slideTimer <= 0f
                    ObstacleType.BLOCKER -> true
                }

                if (!collisionIsActive) {
                    continue
                }

                if (shieldTimer > 0f) {
                    shieldTimer = 0f
                    iterator.remove()
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
            PowerUpType.MAGNET -> magnetTimer = 8.5f
        }
        score += 120
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
            }
        }
    }

    private fun handleCrash() {
        if (runnerState == RunnerState.GAME_OVER) {
            return
        }

        persistRunCoinsIfNeeded()
        runnerState = RunnerState.GAME_OVER
        soundManager.playCrash()
        soundManager.pauseMusic()

        if (score > highScore) {
            highScore = score
            GamePreferences.saveHighScore(context, highScore)
        }

        ensureSelectedSkinUnlocked()
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
        val cameraX = runnerX * 0.26f
        val cameraY = if (slideTimer > 0f) 2.95f else 3.35f + runnerY * 0.12f
        Matrix.setLookAtM(
            viewMatrix,
            0,
            cameraX,
            cameraY + cameraBob,
            8.8f,
            runnerX * 0.18f,
            groundY + 1.2f + runnerY * 0.12f,
            -13f,
            0f,
            1f,
            0f
        )
        Matrix.multiplyMM(vpMatrix, 0, projectionMatrix, 0, viewMatrix, 0)

        drawTrack()
        drawEnvironment()
        drawCoins()
        drawPowerUps()
        drawObstacles()
        drawRunner()
    }

    private fun drawTrack() {
        val segmentLength = 10f
        val trackWidth = laneWidth * 3.3f
        val scroll = distance % segmentLength

        for (index in 0..16) {
            val centerZ = scroll - index * segmentLength - 6f
            val accentColor = if (index % 2 == 0) color(0.15f, 0.20f, 0.31f) else color(0.12f, 0.16f, 0.27f)
            drawCube(0f, groundY - 0.12f, centerZ, trackWidth, 0.14f, segmentLength, accentColor)
            drawCube(-laneWidth, groundY - 0.02f, centerZ, 0.05f, 0.03f, segmentLength, color(0.25f, 0.70f, 0.82f))
            drawCube(0f, groundY - 0.02f, centerZ, 0.05f, 0.03f, segmentLength, color(0.25f, 0.70f, 0.82f))
            drawCube(laneWidth, groundY - 0.02f, centerZ, 0.05f, 0.03f, segmentLength, color(0.25f, 0.70f, 0.82f))
            drawCube(-trackWidth * 0.52f, groundY + 0.45f, centerZ, 0.18f, 0.9f, segmentLength, color(0.07f, 0.11f, 0.2f))
            drawCube(trackWidth * 0.52f, groundY + 0.45f, centerZ, 0.18f, 0.9f, segmentLength, color(0.07f, 0.11f, 0.2f))
        }
    }

    private fun drawEnvironment() {
        val treeSpacing = 14f
        val treeScroll = (distance * 0.65f) % treeSpacing
        for (index in 0..10) {
            val z = treeScroll - index * treeSpacing - 8f
            drawTree(-7f, z, false)
            drawTree(7f, z - 4f, true)
        }

        val archSpacing = 22f
        val archScroll = (distance * 0.88f) % archSpacing
        for (index in 0..7) {
            val z = archScroll - index * archSpacing - 15f
            drawTrackArch(z)
        }

        val skylineSpacing = 26f
        val skylineScroll = (distance * 0.52f) % skylineSpacing
        for (index in 0..8) {
            val z = skylineScroll - index * skylineSpacing - 24f
            drawSkylineTower(-12.5f, z, 4.4f + (index % 3) * 0.9f, warm = index % 2 == 0)
            drawSkylineTower(12.5f, z - 6f, 5.0f + (index % 4) * 0.8f, warm = index % 2 != 0)
        }
    }

    private fun drawTree(x: Float, z: Float, warmTint: Boolean) {
        val trunkColor = if (warmTint) color(0.30f, 0.20f, 0.14f) else color(0.22f, 0.17f, 0.11f)
        val canopyColor = if (warmTint) color(0.30f, 0.48f, 0.34f) else color(0.20f, 0.42f, 0.38f)
        drawCube(x, groundY + 1.1f, z, 0.48f, 2.2f, 0.48f, trunkColor)
        drawCube(x, groundY + 2.75f, z, 1.8f, 1.55f, 1.8f, canopyColor)
        drawCube(x, groundY + 3.55f, z, 1.25f, 0.95f, 1.25f, canopyColor)
    }

    private fun drawTrackArch(z: Float) {
        val frameColor = color(0.18f, 0.29f, 0.44f)
        val beamColor = color(0.22f, 0.78f, 0.88f, 0.72f)
        drawCube(-4.25f, groundY + 2.2f, z, 0.24f, 3.3f, 0.24f, frameColor)
        drawCube(4.25f, groundY + 2.2f, z, 0.24f, 3.3f, 0.24f, frameColor)
        drawCube(0f, groundY + 3.7f, z, 4.55f, 0.18f, 0.24f, frameColor)
        drawCube(0f, groundY + 3.18f, z, 3.7f, 0.08f, 0.08f, beamColor)
    }

    private fun drawSkylineTower(x: Float, z: Float, height: Float, warm: Boolean) {
        val bodyColor = if (warm) color(0.11f, 0.15f, 0.24f) else color(0.09f, 0.13f, 0.20f)
        val lightColor = if (warm) color(0.92f, 0.62f, 0.28f, 0.68f) else color(0.32f, 0.82f, 0.92f, 0.68f)
        drawCube(x, groundY + height * 0.5f, z, 2.4f, height, 2.1f, bodyColor)
        drawCube(x, groundY + height - 0.6f, z + 0.1f, 1.9f, 0.12f, 0.1f, lightColor)
        drawCube(x, groundY + height - 1.3f, z + 0.1f, 1.9f, 0.12f, 0.1f, lightColor)
    }

    private fun drawCoins() {
        for (coin in coins) {
            val pulse = 0.38f + sin((coinSpin + coin.z * 12f) * 0.03f) * 0.04f
            drawCube(
                x = laneToX(coin.lane),
                y = groundY + coin.height,
                z = coin.z,
                scaleX = pulse,
                scaleY = pulse,
                scaleZ = 0.12f,
                color = color(0.98f, 0.80f, 0.29f),
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
            val x = laneToX(obstacle.lane)
            when (obstacle.type) {
                ObstacleType.HURDLE -> {
                    drawCube(x, groundY + 0.52f, obstacle.z, 1.3f, 1.04f, 0.72f, color(0.96f, 0.47f, 0.34f))
                    drawCube(x, groundY + 0.95f, obstacle.z, 1.45f, 0.16f, 1.0f, color(1f, 0.77f, 0.57f))
                }

                ObstacleType.GATE -> {
                    drawCube(x, groundY + 1.75f, obstacle.z, 1.55f, 0.28f, 0.86f, color(0.54f, 0.47f, 0.98f))
                    drawCube(x - 0.62f, groundY + 0.95f, obstacle.z, 0.24f, 1.85f, 0.24f, color(0.39f, 0.32f, 0.86f))
                    drawCube(x + 0.62f, groundY + 0.95f, obstacle.z, 0.24f, 1.85f, 0.24f, color(0.39f, 0.32f, 0.86f))
                }

                ObstacleType.BLOCKER -> {
                    drawCube(x, groundY + 1.15f, obstacle.z, 1.5f, 2.3f, 1.0f, color(0.83f, 0.20f, 0.28f))
                    drawCube(x, groundY + 2.25f, obstacle.z, 1.15f, 0.35f, 1.15f, color(1.0f, 0.72f, 0.30f))
                }
            }
        }
    }

    private fun drawRunner() {
        val baseY = groundY + runnerY
        val sideLean = (laneToX(targetLane) - runnerX) * 8f
        val stride = sin(runAnimationTime) * 24f
        val suitColor = selectedSkin.suitColor
        val trimColor = selectedSkin.trimColor
        val accentColor = selectedSkin.accentColor

        if (shieldTimer > 0f) {
            drawCube(
                runnerX,
                baseY + 1.55f,
                0f,
                1.28f,
                1.95f,
                1.18f,
                color(0.56f, 0.95f, 1f, 0.20f),
                rotationY = worldPulse * 82f
            )
        }

        if (magnetTimer > 0f) {
            val magnetOffset = sin(worldPulse * 4.4f) * 0.18f
            drawCube(runnerX - 0.78f, baseY + 1.32f, magnetOffset, 0.14f, 0.95f, 0.14f, color(0.99f, 0.58f, 0.29f, 0.62f))
            drawCube(runnerX + 0.78f, baseY + 1.32f, -magnetOffset, 0.14f, 0.95f, 0.14f, color(0.34f, 0.92f, 0.98f, 0.62f))
        }

        if (slideTimer > 0f) {
            drawCube(runnerX, baseY + 0.52f, 0f, 1.18f, 0.62f, 1.95f, suitColor, rotationY = sideLean)
            drawCube(runnerX - 0.48f, baseY + 0.72f, 0.18f, 0.42f, 0.42f, 0.42f, trimColor, rotationY = sideLean)
            drawCube(runnerX + 0.35f, baseY + 0.2f, 0.35f, 0.75f, 0.2f, 0.55f, accentColor, rotationY = sideLean)
        } else {
            drawCube(runnerX, baseY + 1.45f, 0f, 0.86f, 1.16f, 0.6f, suitColor, rotationY = sideLean)
            drawCube(runnerX, baseY + 2.42f, 0.05f, 0.62f, 0.62f, 0.62f, trimColor, rotationY = sideLean)
            drawCube(runnerX - 0.5f, baseY + 1.48f, 0f, 0.18f, 0.95f, 0.18f, accentColor, rotationX = -stride, rotationY = sideLean)
            drawCube(runnerX + 0.5f, baseY + 1.48f, 0f, 0.18f, 0.95f, 0.18f, accentColor, rotationX = stride, rotationY = sideLean)
            drawCube(runnerX - 0.22f, baseY + 0.52f, 0f, 0.22f, 1.02f, 0.22f, accentColor, rotationX = stride * 1.2f, rotationY = sideLean)
            drawCube(runnerX + 0.22f, baseY + 0.52f, 0f, 0.22f, 1.02f, 0.22f, accentColor, rotationX = -stride * 1.2f, rotationY = sideLean)
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

    private fun laneToX(lane: Int): Float = lane * laneWidth

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

    private fun unlockedSkins(): List<RunnerSkin> {
        val totalCoins = effectiveTotalCoins()
        return RunnerSkin.entries.filter { totalCoins >= it.unlockCoins }
    }

    private fun ensureSelectedSkinUnlocked() {
        if (effectiveTotalCoins() >= selectedSkin.unlockCoins) {
            return
        }

        selectedSkin = unlockedSkins().lastOrNull() ?: RunnerSkin.VANGUARD
        GamePreferences.saveSelectedSkin(context, selectedSkin)
    }

    private fun effectiveTotalCoins(): Int {
        return bankedCoins + if (runCoinsBanked) 0 else coinsCollected
    }

    private fun nextUnlockLabel(): String {
        val totalCoins = effectiveTotalCoins()
        val nextSkin = RunnerSkin.entries.firstOrNull { totalCoins < it.unlockCoins }
        return if (nextSkin == null) {
            "All suits unlocked"
        } else {
            "${nextSkin.displayName} at ${nextSkin.unlockCoins} coins (${nextSkin.unlockCoins - totalCoins} left)"
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
        return if (statuses.isEmpty()) "No active boost" else statuses.joinToString("  ")
    }

    private fun color(r: Float, g: Float, b: Float, a: Float = 1f): FloatArray {
        return floatArrayOf(r, g, b, a)
    }

    private fun dispatchSnapshot(force: Boolean) {
        val now = System.currentTimeMillis()
        if (!force && now - lastSnapshotDispatchTime < 100L) {
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
            selectedSkin = selectedSkin.displayName,
            nextUnlock = nextUnlockLabel(),
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
