//controls audio playback

package com.vertigo.playerbeta.player

import android.content.Context
import android.media.MediaPlayer
import android.media.audiofx.Equalizer
import android.media.audiofx.BassBoost
import com.vertigo.playerbeta.data.AudioFile

//class manages actual audio playback -androids media player
class MediaPlayerManager(private val context: Context) {

    private var player: MediaPlayer? = null //androids build in player
    private var nextPlayer: MediaPlayer? = null // for gapless playback
    private var currentSong: AudioFile? = null //whats currently loaded
    private var nextSong: AudioFile? = null
    private var onSongFinished: (() -> Unit)? = null //callback when song ends
    
    // Audio Effects components from Android's audiofx framework
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    
    // Internal cache for EQ settings to persist across session changes
    private var eqEnabled = false // Master toggle for all effects
    private var currentPreset: Short = -1 // Current selected preset (-1 for manual)
    private var bassBoostStrength: Short = 0 // Offset strength (0-1000)
    private val bandLevels = mutableMapOf<Short, Short>() // Manual gain levels per band

    //called when song naturally finishes playing
    fun setOnCompleteListener(callback: () -> Unit) {
        onSongFinished = callback
    }

    //start playing a specific song
    fun play(song: AudioFile) {
        //stop and clear any existing playbacks -stop the current playing song
        player?.release()
        currentSong = song

        //create a new player instance
        player = MediaPlayer().apply {
            setDataSource(context, song.uri) //load file
            prepare() //get ready to play
            
            // Initialize audio effects with the new session ID
            setupAudioEffects(audioSessionId)
            
            start() //begin

            //when song ends naturally trigger callback
            setOnCompletionListener {
                handleSongCompletion()
            }
        }
    }

    // Internal handler for when a song ends naturally
    private fun handleSongCompletion() {
        if (nextPlayer != null) {
            // Gapless transition: Swap the next player into the primary slot
            player?.release()
            player = nextPlayer
            currentSong = nextSong
            nextPlayer = null
            nextSong = null
            
            // Re-attach the completion listener to the new primary player
            player?.setOnCompletionListener {
                handleSongCompletion()
            }
            // Notify the service/viewmodel that we've moved to the next track
            onSongFinished?.invoke()
        } else {
            // Standard end of queue
            onSongFinished?.invoke()
        }
    }

    // Pre-loads the next song into a background MediaPlayer for gapless transitions
    fun setNextSong(song: AudioFile?) {
        nextSong = song
        nextPlayer?.release()
        nextPlayer = null

        if (song != null) {
            try {
                // Initialize the next player but don't start it yet
                nextPlayer = MediaPlayer().apply {
                    setDataSource(context, song.uri)
                    prepare()
                }
                // Inform the current player about its successor
                player?.setNextMediaPlayer(nextPlayer)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            // Clear the transition link if no next song is available
            player?.setNextMediaPlayer(null)
        }
    }

    private fun setupAudioEffects(sessionId: Int) {
        try {
            equalizer?.release()
            bassBoost?.release()

            equalizer = Equalizer(0, sessionId).apply {
                enabled = eqEnabled
                if (currentPreset >= 0 && currentPreset < numberOfPresets) {
                    usePreset(currentPreset)
                } else {
                    bandLevels.forEach { (band, level) ->
                        setBandLevel(band, level)
                    }
                }
            }

            bassBoost = BassBoost(0, sessionId).apply {
                enabled = eqEnabled
                setStrength(bassBoostStrength)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setEqualizerEnabled(enabled: Boolean) {
        eqEnabled = enabled
        equalizer?.enabled = enabled
        bassBoost?.enabled = enabled
    }

    fun getEqualizerPresets(): List<String> {
        val count = equalizer?.numberOfPresets?.toInt() ?: 0
        return (0 until count).map { equalizer?.getPresetName(it.toShort()) ?: "Preset $it" }
    }

    fun usePreset(presetIndex: Short) {
        currentPreset = presetIndex
        equalizer?.usePreset(presetIndex)
        // Cache levels after applying preset
        val bands = equalizer?.numberOfBands ?: 0
        for (i in 0 until bands) {
            val band = i.toShort()
            bandLevels[band] = equalizer?.getBandLevel(band) ?: 0
        }
    }

    fun setBandLevel(band: Short, level: Short) {
        currentPreset = -1 // Manual mode
        bandLevels[band] = level
        equalizer?.setBandLevel(band, level)
    }

    fun setBassBoost(strength: Short) {
        bassBoostStrength = strength
        bassBoost?.setStrength(strength)
    }

    fun getBandLevel(band: Short): Short = bandLevels[band] ?: 0
    fun getBassBoostStrength(): Short = bassBoostStrength
    fun isEqualizerEnabled(): Boolean = eqEnabled
    fun getNumberOfBands(): Short = equalizer?.numberOfBands ?: 0
    fun getBandCenterFreq(band: Short): Int = equalizer?.getCenterFreq(band) ?: 0
    fun getBandLevelRange(): ShortArray = equalizer?.bandLevelRange ?: shortArrayOf(-1500, 1500)
    fun getCurrentPreset(): Short = currentPreset

    //pause or resume playback
    fun togglePlayPause(): Boolean {
        player?.let {
            if (it.isPlaying) {
                it.pause()
                return false //now paused
            } else {
                it.start()
                return true //now playing
            }
        }
        return false
    }

    //jump to specific time eg when slider is dragged
    fun seekTo(positionMs: Long) {
        player?.seekTo(positionMs.toInt())
    }

    //stop everything and free memory
    fun stop() {
        player?.release()
        equalizer?.release()
        bassBoost?.release()
        player = null
        equalizer = null
        bassBoost = null
    }

    //get current playback position
    fun getCurrentPosition(): Long = player?.currentPosition?.toLong() ?: 0

    //GET total song length
    fun getDuration(): Long = player?.duration?.toLong() ?: 0

    //check if song is currently playing
    fun isPlaying(): Boolean = player?.isPlaying ?: false

    //get current loaded songs
    fun getCurrentSong(): AudioFile? = currentSong
}
