//background playback and notification

package com.vertigo.playerbeta.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.vertigo.playerbeta.MainActivity
import com.vertigo.playerbeta.R
import com.vertigo.playerbeta.data.AudioFile
import com.vertigo.playerbeta.player.MediaPlayerManager

//this service keeps music playing when app is closed/minimised
//also shows notification with playback control


class MusicService : Service() {
    private val binder = MusicBinder()
    private lateinit var playerManager: MediaPlayerManager

    //channel id for android notifications
    companion object {
        const val CHANNEL_ID = "music_player_channel"
        const val NOTIFICATION_ID = 1
    }

    //called when activity connects to this service
    inner class MusicBinder : Binder() {
        fun getService(): MusicService = this@MusicService
    }

    override fun onCreate() {
        super.onCreate()
        playerManager = MediaPlayerManager(this)
        createNotificationChannel() // required for android 8+

    }

    //activity binds to service to control playback
    override fun onBind(intent: Intent): IBinder = binder

    //Service is starting -called by startservice
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        //makes this a foreground service.process so android doesnt kill it
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                buildNotification(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(NOTIFICATION_ID, buildNotification())
        }
        return START_STICKY  //RESTARTS IF KILLED BY SYSTEM
    }

    //public methods that viewmodel can call through binder
    fun play(song: AudioFile) {
        playerManager.play(song)
        updateNotification() //update notification text
    }

    fun togglePlayPause() = playerManager.togglePlayPause()

    fun seekTo(position: Long) = playerManager.seekTo(position)

    fun getCurrentPosition() = playerManager.getCurrentPosition()

    fun getDuration() = playerManager.getDuration()

    fun isPlaying() = playerManager.isPlaying()

    fun getCurrentSong() = playerManager.getCurrentSong()

    fun setOnCompleteListener(callback: () -> Unit) {
        playerManager.setOnCompleteListener(callback)
    }

//create the notification channel -required for android 8+
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Music Playback", //name shown in settings
                NotificationManager.IMPORTANCE_LOW).apply {
                // low= no sound produced
                    description = "Current playing song"
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    //build notification with song info and controls

    private fun buildNotification(): Notification {
        val song = playerManager.getCurrentSong()

        //intent to open app when notification is tapped
        val openAppIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(song?.title ?: "No song playing")
            .setContentText(song?.artist ?: "Unknown artist")
            .setSmallIcon(R.drawable.ic_launcher_background)  //set icon that youll use in re/drawables/
            .setContentIntent(pendingIntent)
            .setOngoing(true)    //cant be swiped away while playing
            .build()
    }

    //refresh notification when song changes

    private fun updateNotification() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification())
    }

    //clean up when service is destroyed
    override fun onDestroy() {
        playerManager.stop()
        super.onDestroy()

    }
}
