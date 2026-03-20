//controls audio playback

package com.vertigo.playerbeta.player

import android.content.Context
import android.media.MediaPlayer
import com.vertigo.playerbeta.data.AudioFile

//class manages actual audio playback -androids media player
class MediaPlayerManager(private val context: Context) {

    private var player: MediaPlayer? = null //androids build in player
    private var currentSong: AudioFile? = null //whats currently loaded
    private var onSongFinished: (() -> Unit)? = null //callback when song ends

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
            start() //begin

            //when song ends naturally trigger callback
            setOnCompletionListener {
                onSongFinished?.invoke()
            }
        }
    }

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
        player = null
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
