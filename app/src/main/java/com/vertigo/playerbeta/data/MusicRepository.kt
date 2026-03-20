//this gets songs from the device/phone storage

package com.vertigo.playerbeta.data

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

//this class finds music files stored on the phone
class MusicRepository (private val context: Context) {


    //this line below runs on the background thread so ui doesnt freeze
    suspend fun getSongs(): List<AudioFile> = withContext(Dispatchers.IO) {
        val songs = mutableListOf<AudioFile>()

        //info we want from each song file
        val columns = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION
        )
        //only get actual music files (IS_MUSIC != 0)
        val filter = "${MediaStore.Audio.Media.IS_MUSIC} != 0"

        //Query the phones media database
        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            columns,
            filter,
            null,
            "${MediaStore.Audio.Media.TITLE} ASC" //sort A-Z BY TITLE
        )?.use { cursor ->

            //get column positions
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)

            //go through every song found
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)

                songs.add(
                    AudioFile(
                        id = id,
                        title = cursor.getString(titleColumn) ?: "Unknown song",
                        artist = cursor.getString(artistColumn) ?: "Unknown artist",
                        album = cursor.getString(albumColumn) ?: "Unknown album",
                        duration = cursor.getLong(durationColumn),
                        uri = ContentUris.withAppendedId(
                            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                            id)
                    )
                )
            }
        }
        songs
    }
}
