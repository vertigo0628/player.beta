// song data model


package com.vertigo.playerbeta.data

import android.net.Uri

//this file holds data about the song playing
data class AudioFile(
    val id: Long,  // Unique identifier for the audio file
    val title: String,  //song name
    val artist: String,  //name of artist
    val album: String,  //name of album
    val duration: Long,  //length of song
    val uri: Uri  //location where the song is stored

)
