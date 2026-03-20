//Detects new songs as they are downloaded and added to phone storage


package com.vertigo.playerbeta.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(this, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(this, filter)
        }

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
