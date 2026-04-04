package com.relicrush.game.utils

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import com.relicrush.game.R

class SoundManager(
    context: Context,
    private val preferences: GamePreferences
) {
    private val appContext = context.applicationContext

    private val soundPool = SoundPool.Builder()
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .setMaxStreams(6)
        .build()

    private val jumpSoundId = soundPool.load(appContext, R.raw.jump, 1)
    private val crashSoundId = soundPool.load(appContext, R.raw.crash, 1)
    private val coinSoundId = soundPool.load(appContext, R.raw.coin, 1)

    private var musicEnabled = preferences.isMusicEnabled()
    private var sfxEnabled = preferences.isSfxEnabled()
    private var musicIntensity = 0.45f

    private val mediaPlayer = MediaPlayer.create(appContext, R.raw.bgm_loop)?.apply {
        isLooping = true
        setVolume(musicIntensity, musicIntensity)
    }

    fun startMusic() {
        if (!musicEnabled) {
            return
        }

        mediaPlayer?.let { player ->
            if (!player.isPlaying) {
                player.start()
            }
        }
    }

    fun pauseMusic() {
        mediaPlayer?.let { player ->
            if (player.isPlaying) {
                player.pause()
            }
        }
    }

    fun resumeMusic() {
        startMusic()
    }

    fun setMusicEnabled(enabled: Boolean) {
        musicEnabled = enabled
        preferences.setMusicEnabled(enabled)
        if (enabled) {
            startMusic()
            setMusicIntensity(musicIntensity)
        } else {
            pauseMusic()
        }
    }

    fun setSfxEnabled(enabled: Boolean) {
        sfxEnabled = enabled
        preferences.setSfxEnabled(enabled)
    }

    fun isMusicEnabled(): Boolean = musicEnabled
    fun isSfxEnabled(): Boolean = sfxEnabled

    fun setMusicIntensity(intensity: Float) {
        musicIntensity = GameMath.clamp(intensity, 0.18f, 1f)
        if (musicEnabled) {
            mediaPlayer?.setVolume(musicIntensity, musicIntensity)
        }
    }

    fun playJump() {
        play(jumpSoundId, 0.75f)
    }

    fun playCrash() {
        play(crashSoundId, 1f)
    }

    fun playCoin() {
        play(coinSoundId, 0.64f)
    }

    fun playPowerUp() {
        play(coinSoundId, 0.88f, 1.18f)
    }

    private fun play(soundId: Int, volume: Float, rate: Float = 1f) {
        if (!sfxEnabled) {
            return
        }

        soundPool.play(soundId, volume, volume, 1, 0, rate)
    }

    fun release() {
        mediaPlayer?.release()
        soundPool.release()
    }
}
