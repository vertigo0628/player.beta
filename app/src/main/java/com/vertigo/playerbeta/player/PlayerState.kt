//status handler

package com.vertigo.playerbeta.player

import com.vertigo.playerbeta.data.AudioFile

//keeps track of what is currently happening with/in the player

data class PlayerState (
    val currentSong : AudioFile? = null,  //which is song is playing (null=none)
    val isPlaying: Boolean = false, //true= playing false=paused
    val position: Long = 0,         //current time
    val duration: Long = 0,        //song length
    val isShuffling: Boolean = false,  //random order on/off
    val repeatMode: RepeatMode = RepeatMode.OFF  //repeat options
)

//ways of repeating
enum class RepeatMode {
    OFF,  //no repeat
    ALL,   //repeat all songs
    ONE //repeat current song
}
