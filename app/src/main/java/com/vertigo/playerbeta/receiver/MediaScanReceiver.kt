//Detects new songs as they are downloaded and added to phone storage


package com.vertigo.playerbeta.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import android.provider.MediaStore

//this class listens for new music files added

class MediaScanReceiver : BroadcastReceiver() {


    //function to call when new file is found
    private var onMediaChanged: (() -> Unit)? = null

    fun setListner(callback: () -> Unit) {
        onMediaChanged = callback
    }

    //called when media files change
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Intent.ACTION_MEDIA_SCANNER_FINISHED || intent?.data?.toString()
                ?.contains(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI.toString()
                ) == true
        ) {
            onMediaChanged?.invoke()   //TELLS APP to refresh song list
        }
    }


    //register this receiver from the activity
    fun register(context: Context){
        val filter = IntentFilter().apply{
            addAction(Intent.ACTION_MEDIA_SCANNER_FINISHED)
            addDataScheme("file")
        }
        // Use ContextCompat to register the receiver with the NOT_EXPORTED flag safely across Android versions
        ContextCompat.registerReceiver(
            context,
            this,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    //unregister when app closes to save battery
    fun unregister(context: Context){
        try {
            context.unregisterReceiver(this)
        } catch (e: IllegalArgumentException) {
            // Receiver not registered
        }
    }

}
