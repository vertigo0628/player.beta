package com.vertigo.playerbeta.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class PlaylistRepository(private val context: Context) {
    private val playlistFile = File(context.filesDir, "playlists.json")
    private val favoritesFile = File(context.filesDir, "favorites.json")

    fun getFavorites(): Set<Long> {
        if (!favoritesFile.exists()) return emptySet()
        return try {
            val jsonArray = JSONArray(favoritesFile.readText())
            val favorites = mutableSetOf<Long>()
            for (i in 0 until jsonArray.length()) {
                favorites.add(jsonArray.getLong(i))
            }
            favorites
        } catch (e: Exception) {
            emptySet()
        }
    }

    fun saveFavorites(favorites: Set<Long>) {
        try {
            val jsonArray = JSONArray()
            favorites.forEach { jsonArray.put(it) }
            favoritesFile.writeText(jsonArray.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getPlaylists(): List<Playlist> {
        if (!playlistFile.exists()) return emptyList()
        
        return try {
            val jsonString = playlistFile.readText()
            val jsonArray = JSONArray(jsonString)
            val playlists = mutableListOf<Playlist>()
            
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val songIdsArray = obj.getJSONArray("songIds")
                val songIds = mutableListOf<Long>()
                for (j in 0 until songIdsArray.length()) {
                    songIds.add(songIdsArray.getLong(j))
                }
                playlists.add(
                    Playlist(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        songIds = songIds
                    )
                )
            }
            playlists
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun savePlaylists(playlists: List<Playlist>) {
        try {
            val jsonArray = JSONArray()
            playlists.forEach { playlist ->
                val obj = JSONObject()
                obj.put("id", playlist.id)
                obj.put("name", playlist.name)
                
                val songIdsArray = JSONArray()
                playlist.songIds.forEach { songIdsArray.put(it) }
                obj.put("songIds", songIdsArray)
                
                jsonArray.put(obj)
            }
            playlistFile.writeText(jsonArray.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
