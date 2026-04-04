package com.infinityrush.game3d

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

class MainActivity : AppCompatActivity(), GameRenderer.UiListener {
    private lateinit var gameView: GameSurfaceView
    private lateinit var hudBar: LinearLayout
    private lateinit var settingsStrip: LinearLayout
    private lateinit var startOverlay: LinearLayout
    private lateinit var pauseOverlay: LinearLayout
    private lateinit var gameOverOverlay: LinearLayout
    private lateinit var scoreText: TextView
    private lateinit var coinsText: TextView
    private lateinit var speedText: TextView
    private lateinit var finalScoreText: TextView
    private lateinit var highScoreText: TextView
    private lateinit var pauseButton: ImageButton
    private lateinit var playButton: Button
    private lateinit var resumeButton: Button
    private lateinit var restartButton: Button
    private lateinit var musicSwitch: Switch
    private lateinit var sfxSwitch: Switch

    private var isUpdatingAudioToggles = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContentView(R.layout.activity_main)
        bindViews()
        bindEvents()
        gameView.setUiListener(this)
        hideSystemBars()
        renderSnapshot(
            GameSnapshot(
                state = RunnerState.START,
                score = 0,
                highScore = GamePreferences.getHighScore(this),
                coins = 0,
                speedKph = 0,
                musicEnabled = GamePreferences.isMusicEnabled(this),
                sfxEnabled = GamePreferences.isSfxEnabled(this)
            )
        )
    }

    override fun onResume() {
        super.onResume()
        hideSystemBars()
        gameView.resumeForLifecycle()
    }

    override fun onPause() {
        gameView.pauseForLifecycle()
        super.onPause()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemBars()
        }
    }

    override fun onSnapshot(snapshot: GameSnapshot) {
        renderSnapshot(snapshot)
    }

    private fun bindViews() {
        gameView = findViewById(R.id.gameView)
        hudBar = findViewById(R.id.hudBar)
        settingsStrip = findViewById(R.id.settingsStrip)
        startOverlay = findViewById(R.id.startOverlay)
        pauseOverlay = findViewById(R.id.pauseOverlay)
        gameOverOverlay = findViewById(R.id.gameOverOverlay)
        scoreText = findViewById(R.id.scoreText)
        coinsText = findViewById(R.id.coinsText)
        speedText = findViewById(R.id.speedText)
        finalScoreText = findViewById(R.id.finalScoreText)
        highScoreText = findViewById(R.id.highScoreText)
        pauseButton = findViewById(R.id.pauseButton)
        playButton = findViewById(R.id.playButton)
        resumeButton = findViewById(R.id.resumeButton)
        restartButton = findViewById(R.id.restartButton)
        musicSwitch = findViewById(R.id.musicSwitch)
        sfxSwitch = findViewById(R.id.sfxSwitch)
    }

    private fun bindEvents() {
        playButton.setOnClickListener { gameView.startRun() }
        pauseButton.setOnClickListener { gameView.pauseRun() }
        resumeButton.setOnClickListener { gameView.resumeRun() }
        restartButton.setOnClickListener { gameView.restartRun() }

        musicSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (!isUpdatingAudioToggles) {
                gameView.setMusicEnabled(isChecked)
            }
        }

        sfxSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (!isUpdatingAudioToggles) {
                gameView.setSfxEnabled(isChecked)
            }
        }
    }

    private fun renderSnapshot(snapshot: GameSnapshot) {
        scoreText.text = "${getString(R.string.hud_score)} ${snapshot.score}"
        coinsText.text = "${getString(R.string.hud_coins)} ${snapshot.coins}"
        speedText.text = "${getString(R.string.hud_speed)} ${snapshot.speedKph} km/h"
        finalScoreText.text = "Final score: ${snapshot.score}"
        highScoreText.text = "Best run: ${snapshot.highScore}"

        isUpdatingAudioToggles = true
        musicSwitch.isChecked = snapshot.musicEnabled
        sfxSwitch.isChecked = snapshot.sfxEnabled
        isUpdatingAudioToggles = false

        when (snapshot.state) {
            RunnerState.START -> {
                hudBar.visibility = View.GONE
                settingsStrip.visibility = View.VISIBLE
                startOverlay.visibility = View.VISIBLE
                pauseOverlay.visibility = View.GONE
                gameOverOverlay.visibility = View.GONE
            }

            RunnerState.RUNNING -> {
                hudBar.visibility = View.VISIBLE
                settingsStrip.visibility = View.GONE
                startOverlay.visibility = View.GONE
                pauseOverlay.visibility = View.GONE
                gameOverOverlay.visibility = View.GONE
            }

            RunnerState.PAUSED -> {
                hudBar.visibility = View.VISIBLE
                settingsStrip.visibility = View.VISIBLE
                startOverlay.visibility = View.GONE
                pauseOverlay.visibility = View.VISIBLE
                gameOverOverlay.visibility = View.GONE
            }

            RunnerState.GAME_OVER -> {
                hudBar.visibility = View.GONE
                settingsStrip.visibility = View.VISIBLE
                startOverlay.visibility = View.GONE
                pauseOverlay.visibility = View.GONE
                gameOverOverlay.visibility = View.VISIBLE
            }
        }
    }

    private fun hideSystemBars() {
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}
