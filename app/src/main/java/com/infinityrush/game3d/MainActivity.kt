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
    private lateinit var missionText: TextView
    private lateinit var powerText: TextView
    private lateinit var finalScoreText: TextView
    private lateinit var highScoreText: TextView
    private lateinit var selectedSkinText: TextView
    private lateinit var startBankText: TextView
    private lateinit var startSkinText: TextView
    private lateinit var startUnlockText: TextView
    private lateinit var startMissionText: TextView
    private lateinit var startRewardText: TextView
    private lateinit var totalCoinsText: TextView
    private lateinit var gameOverSkinText: TextView
    private lateinit var gameOverUnlockText: TextView
    private lateinit var gameOverMissionText: TextView
    private lateinit var gameOverRewardText: TextView
    private lateinit var pauseButton: ImageButton
    private lateinit var playButton: Button
    private lateinit var resumeButton: Button
    private lateinit var restartButton: Button
    private lateinit var skinButton: Button
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
        val hero = GamePreferences.getSelectedHero(this)
        val mission = currentMission()
        renderSnapshot(
            GameSnapshot(
                state = RunnerState.START,
                score = 0,
                highScore = GamePreferences.getHighScore(this),
                coins = 0,
                totalCoins = GamePreferences.getTotalCoins(this),
                speedKph = 0,
                selectedHero = hero.displayName,
                selectedHeroTitle = hero.title,
                nextUnlock = nextUnlockText(GamePreferences.getTotalCoins(this)),
                activeMission = getString(
                    R.string.quest_progress_format,
                    mission.title,
                    GamePreferences.getMissionProgress(this),
                    mission.target
                ),
                activeMissionReward = getString(R.string.quest_reward_format, mission.rewardCoins),
                activePowerUp = getString(R.string.power_up_none),
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
        missionText = findViewById(R.id.missionText)
        powerText = findViewById(R.id.powerText)
        finalScoreText = findViewById(R.id.finalScoreText)
        highScoreText = findViewById(R.id.highScoreText)
        selectedSkinText = findViewById(R.id.selectedSkinText)
        startBankText = findViewById(R.id.startBankText)
        startSkinText = findViewById(R.id.startSkinText)
        startUnlockText = findViewById(R.id.startUnlockText)
        startMissionText = findViewById(R.id.startMissionText)
        startRewardText = findViewById(R.id.startRewardText)
        totalCoinsText = findViewById(R.id.totalCoinsText)
        gameOverSkinText = findViewById(R.id.gameOverSkinText)
        gameOverUnlockText = findViewById(R.id.gameOverUnlockText)
        gameOverMissionText = findViewById(R.id.gameOverMissionText)
        gameOverRewardText = findViewById(R.id.gameOverRewardText)
        pauseButton = findViewById(R.id.pauseButton)
        playButton = findViewById(R.id.playButton)
        resumeButton = findViewById(R.id.resumeButton)
        restartButton = findViewById(R.id.restartButton)
        skinButton = findViewById(R.id.skinButton)
        musicSwitch = findViewById(R.id.musicSwitch)
        sfxSwitch = findViewById(R.id.sfxSwitch)
    }

    private fun bindEvents() {
        playButton.setOnClickListener { gameView.startRun() }
        pauseButton.setOnClickListener { gameView.pauseRun() }
        resumeButton.setOnClickListener { gameView.resumeRun() }
        restartButton.setOnClickListener { gameView.restartRun() }
        skinButton.setOnClickListener { gameView.cycleSkin() }

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
        missionText.text = snapshot.activeMission
        powerText.text = snapshot.activePowerUp
        finalScoreText.text = getString(R.string.final_score_format, snapshot.score)
        highScoreText.text = getString(R.string.best_run_format, snapshot.highScore)
        selectedSkinText.text = heroText(snapshot.selectedHero, snapshot.selectedHeroTitle)
        startBankText.text = getString(R.string.total_coins_format, snapshot.totalCoins)
        startSkinText.text = heroText(snapshot.selectedHero, snapshot.selectedHeroTitle)
        startUnlockText.text = getString(R.string.next_unlock_format, snapshot.nextUnlock)
        startMissionText.text = snapshot.activeMission
        startRewardText.text = snapshot.activeMissionReward
        totalCoinsText.text = getString(R.string.total_coins_format, snapshot.totalCoins)
        gameOverSkinText.text = heroText(snapshot.selectedHero, snapshot.selectedHeroTitle)
        gameOverUnlockText.text = getString(R.string.next_unlock_format, snapshot.nextUnlock)
        gameOverMissionText.text = snapshot.activeMission
        gameOverRewardText.text = snapshot.activeMissionReward

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

    private fun nextUnlockText(totalCoins: Int): String {
        val nextHero = RunnerCharacter.entries.firstOrNull { totalCoins < it.unlockCoins }
        return if (nextHero == null) {
            getString(R.string.all_suits_unlocked)
        } else {
            getString(
                R.string.next_unlock_value_format,
                nextHero.displayName,
                nextHero.unlockCoins,
                nextHero.unlockCoins - totalCoins
            )
        }
    }

    private fun currentMission(): MissionDefinition {
        val missions = AdventureContent.missions
        val index = GamePreferences.getMissionIndex(this).mod(missions.size)
        return missions[index]
    }

    private fun heroText(name: String, title: String): String {
        return getString(R.string.current_hero_format, name, title)
    }
}
