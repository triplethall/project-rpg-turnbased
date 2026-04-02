package ru.triplethall.rpgturnbased

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.Preferences

object SoundManager {
    private const val PREFS_NAME = "audio_settings"
    private const val MUSIC_VOLUME_KEY = "music_volume"
    private const val SOUND_VOLUME_KEY = "sound_volume"
    private const val DEFAULT_VOLUME = 0.7f

    @JvmStatic
    var musicVolume: Float = DEFAULT_VOLUME
        set(value) {
            field = value.coerceIn(0f, 1f)
            saveVolume()
            applyMusicVolume()
        }

    @JvmStatic
    var soundVolume: Float = DEFAULT_VOLUME
        set(value) {
            field = value.coerceIn(0f, 1f)
            saveVolume()
        }

    private val prefs: Preferences = Gdx.app.getPreferences(PREFS_NAME)
    private var currentMusic: Music? = null
    private val sounds = mutableMapOf<String, Sound>()

    init {
        loadVolume()
    }

    private fun loadVolume() {
        musicVolume = prefs.getFloat(MUSIC_VOLUME_KEY, DEFAULT_VOLUME)
        soundVolume = prefs.getFloat(SOUND_VOLUME_KEY, DEFAULT_VOLUME)
    }

    private fun saveVolume() {
        prefs.putFloat(MUSIC_VOLUME_KEY, musicVolume)
        prefs.putFloat(SOUND_VOLUME_KEY, soundVolume)
        prefs.flush()
    }

    private fun applyMusicVolume() {
        currentMusic?.volume = musicVolume
    }

    @JvmStatic
    @JvmOverloads
    fun playMusic(musicFile: String, looping: Boolean = true): Music? {
        return try {
            currentMusic?.stop()
            currentMusic = Gdx.audio.newMusic(Gdx.files.internal(musicFile))
            currentMusic?.isLooping = looping
            currentMusic?.volume = musicVolume
            currentMusic?.play()
            currentMusic
        } catch (e: Exception) {
            Gdx.app.error("SoundManager", "Failed to play music: $musicFile", e)
            null
        }
    }

    @JvmStatic
    fun stopMusic() {
        currentMusic?.stop()
        currentMusic?.dispose()
        currentMusic = null
    }

    @JvmStatic
    fun playSound(soundFile: String): Long {
        val sound = sounds.getOrPut(soundFile) {
            Gdx.audio.newSound(Gdx.files.internal(soundFile))
        }
        return sound.play(soundVolume)
    }

    @JvmStatic
    fun dispose() {
        currentMusic?.dispose()
        sounds.values.forEach { it.dispose() }
        sounds.clear()
    }
}
