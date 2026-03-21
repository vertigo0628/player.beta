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
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.media.session.MediaButtonReceiver
import com.vertigo.playerbeta.MainActivity
import com.vertigo.playerbeta.R
import com.vertigo.playerbeta.data.AudioFile
import com.vertigo.playerbeta.player.MediaPlayerManager

//this service keeps music playing when app is closed/minimised
//also shows notification with playback control

class MusicService : Service() {
    private val binder = MusicBinder()
    private lateinit var playerManager: MediaPlayerManager
    private lateinit var mediaSession: MediaSessionCompat

    //channel id for android notifications
    companion object {
        const val CHANNEL_ID = "music_player_channel"
        const val NOTIFICATION_ID = 1
    }

    //callback for notification button pressed
    private var onNotificationAction: ((String) -> Unit)? = null

    fun setOnNotificationAction(callback: (String) -> Unit) {
        onNotificationAction = callback
    }

    //called when activity connects to this service
    inner class MusicBinder : Binder() {
        fun getService(): MusicService = this@MusicService
    }

    override fun onCreate() {
        super.onCreate()
        playerManager = MediaPlayerManager(this)
        createNotificationChannel() // required for android 8+
        initMediaSession()
    }

    //create media session -app appears in the system media controls
    private fun initMediaSession() {
        mediaSession = MediaSessionCompat(this, "MusicService").apply {
            //set flags so media controls work on lockscreen
            setFlags(MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS or MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS)
            
            //set callbacks for playback controls handles clicks from notification/lock screen
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() {
                    handleAction("PLAY_PAUSE")
                }

                override fun onPause() {
                    handleAction("PLAY_PAUSE")
                }

                override fun onSkipToNext() {
                    handleAction("NEXT")
                }

                override fun onSkipToPrevious() {
                    handleAction("PREVIOUS")
                }

                override fun onSeekTo(pos: Long) {
                    seekTo(pos)
                    updatePlaybackState(isPlaying())
                }
            })
            //active session so system knows its ready
            isActive = true
        }
    }

    //activity binds to service to control playback
    override fun onBind(intent: Intent): IBinder = binder

    //Service is starting -called by startservice
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Forward media button events to the MediaSession
        MediaButtonReceiver.handleIntent(mediaSession, intent)

        val action = intent?.action
        if (action != null && action != Intent.ACTION_MEDIA_BUTTON) {
            handleAction(action)
        }

        updateForeground()
        return START_STICKY //RESTARTS IF KILLED BY SYSTEM
    }

    //processes actions from notification or media buttons
    private fun handleAction(action: String) {
        when (action) {
            "PLAY", "PAUSE", "PLAY_PAUSE" -> {
                if (onNotificationAction != null) {
                    onNotificationAction?.invoke("PLAY_PAUSE")
                } else {
                    togglePlayPause()
                }
            }
            "NEXT" -> {
                onNotificationAction?.invoke("NEXT")
            }
            "PREVIOUS" -> {
                onNotificationAction?.invoke("PREVIOUS")
            }
        }
    }

    //makes this a foreground service.process so android doesnt kill it
    private fun updateForeground() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    //public methods that viewmodel can call through binder
    fun play(song: AudioFile) {
        playerManager.play(song)
        updateMediaSession(song) //update media session
        updatePlaybackState(true)
        updateNotification() //update notification text
    }

    fun togglePlayPause(): Boolean {
        val isPlaying = playerManager.togglePlayPause()
        updatePlaybackState(isPlaying)
        updateNotification()
        return isPlaying
    }

    //update metadata in media session
    private fun updateMediaSession(song: AudioFile) {
        val metadata = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, song.title)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, song.artist)
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, song.duration)
            .build()
        mediaSession.setMetadata(metadata)
    }

    //tell android if music is playing or paused
    private fun updatePlaybackState(isPlaying: Boolean) {
        val state = if (isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED
        val playbackState = PlaybackStateCompat.Builder()
            .setState(state, playerManager.getCurrentPosition(), 1.0f) //state,position,speed
            .setActions(
                PlaybackStateCompat.ACTION_PLAY or
                        PlaybackStateCompat.ACTION_PAUSE or
                        PlaybackStateCompat.ACTION_PLAY_PAUSE or
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                        PlaybackStateCompat.ACTION_SEEK_TO
            )
            .build()
        mediaSession.setPlaybackState(playbackState)
    }

    fun seekTo(position: Long) = playerManager.seekTo(position)
    fun getCurrentPosition() = playerManager.getCurrentPosition()
    fun getDuration() = playerManager.getDuration()
    fun isPlaying() = playerManager.isPlaying()
    fun getCurrentSong() = playerManager.getCurrentSong()
    fun setOnCompleteListener(callback: () -> Unit) = playerManager.setOnCompleteListener(callback)

    //create the notification channel -required for android 8+
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Music Playback", //name shown in settings
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                // low= no sound produced
                description = "Current playing song"
                setShowBadge(false) //dont show app icon in notification
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    //build media notification with album art and buttons
    private fun buildNotification(): Notification {
        val song = playerManager.getCurrentSong()
        val isPlaying = playerManager.isPlaying()

        //intent to open app when notification is tapped
        val openAppIntent = Intent(this, MainActivity::class.java)
        val contentIntent = PendingIntent.getActivity(
            this, 0, openAppIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val prevIntent = Intent(this, MusicService::class.java).apply { action = "PREVIOUS" }
        val prevPendingIntent = PendingIntent.getService(this, 2, prevIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val playPauseAction = if (isPlaying) "PAUSE" else "PLAY"
        val playPauseIntent = Intent(this, MusicService::class.java).apply { action = playPauseAction }
        val playPausePendingIntent = PendingIntent.getService(this, 1, playPauseIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val nextIntent = Intent(this, MusicService::class.java).apply { action = "NEXT" }
        val nextPendingIntent = PendingIntent.getService(this, 3, nextIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val playPauseIcon = if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play

        //build notification with song info and controls
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(song?.title ?: "No song playing")
            .setContentText(song?.artist ?: "Unknown artist")
            .setSmallIcon(R.drawable.vplayer_beta) //set icon that youll use in re/drawables/
            .setContentIntent(contentIntent)
            .setOnlyAlertOnce(true) //play sound once/ dont make sound every update
            .setOngoing(isPlaying) //cant swipe away while playing
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setMediaSession(mediaSession.sessionToken) //connects to session
                    .setShowActionsInCompactView(0, 1, 2) //shows 3 buttons when collapsed
            )
            .addAction(android.R.drawable.ic_media_previous, "Previous", prevPendingIntent)
            .addAction(playPauseIcon, playPauseAction, playPausePendingIntent)
            .addAction(android.R.drawable.ic_media_next, "Next", nextPendingIntent)
            .build()
    }

    //refresh notification when song changes
    private fun updateNotification() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification())
    }

    //clean up when service is destroyed
    override fun onDestroy() {
        mediaSession.release() //release media session
        playerManager.stop()
        super.onDestroy()
    }
}
