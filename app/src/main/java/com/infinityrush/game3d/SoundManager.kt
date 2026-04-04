package com.infinityrush.game3d

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool

class SoundManager(context: Context) {
    private val appContext = context.applicationContext
    private var musicEnabled = GamePreferences.isMusicEnabled(appContext)
    private var sfxEnabled = GamePreferences.isSfxEnabled(appContext)

    private val soundPool = SoundPool.Builder()
        .setMaxStreams(4)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private val jumpSoundId = soundPool.load(appContext, R.raw.jump, 1)
    private val crashSoundId = soundPool.load(appContext, R.raw.crash, 1)

    private var mediaPlayer: MediaPlayer? = MediaPlayer.create(appContext, R.raw.bgm_loop)?.apply {
        isLooping = true
        setVolume(0.38f, 0.38f)
    }

    fun startMusic() {
        if (!musicEnabled) {
            return
        }

        val player = mediaPlayer ?: return
        if (!player.isPlaying) {
            player.start()
        }
    }

    fun pauseMusic() {
        val player = mediaPlayer ?: return
        if (player.isPlaying) {
            player.pause()
        }
    }

    fun playJump() {
        if (sfxEnabled) {
            soundPool.play(jumpSoundId, 0.85f, 0.85f, 1, 0, 1.05f)
        }
    }

    fun playCrash() {
        if (sfxEnabled) {
            soundPool.play(crashSoundId, 0.92f, 0.92f, 1, 0, 0.94f)
        }
    }

    fun isMusicEnabled(): Boolean = musicEnabled

    fun isSfxEnabled(): Boolean = sfxEnabled

    fun setMusicEnabled(enabled: Boolean) {
        musicEnabled = enabled
        GamePreferences.saveMusicEnabled(appContext, enabled)
        if (enabled) {
            startMusic()
        } else {
            pauseMusic()
        }
    }

    fun setSfxEnabled(enabled: Boolean) {
        sfxEnabled = enabled
        GamePreferences.saveSfxEnabled(appContext, enabled)
    }

    fun release() {
        mediaPlayer?.release()
        mediaPlayer = null
        soundPool.release()
    }
}

