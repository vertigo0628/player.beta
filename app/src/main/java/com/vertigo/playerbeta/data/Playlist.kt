package com.vertigo.playerbeta.data

data class Playlist(
    val id: String,
    val name: String,
    val songIds: List<Long> = emptyList()
)
