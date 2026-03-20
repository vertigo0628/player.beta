//connects everthing together

package com.vertigo.playerbeta.viewmodel

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vertigo.playerbeta.data.AudioFile
import com.vertigo.playerbeta.data.MusicRepository
import com.vertigo.playerbeta.receiver.MediaScanReceiver
import com.vertigo.playerbeta.services.MusicService
import com.vertigo.playerbeta.player.PlayerState
import com.vertigo.playerbeta.player.RepeatMode
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


//main controller -connects ui to music playback
class PlayerViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = MusicRepository(application)
    private val mediaScanReceiver = MediaScanReceiver()


    //Service connection binds background music service to this activity
    private var musicService: MusicService? = null
    private var serviceBound = false

    //CONNECTION B2WIN Viewmodel and musicservice
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MusicService.MusicBinder
            musicService = binder.getService()
            serviceBound = true

            //when song finishes naturally -play next
            musicService?.setOnCompleteListener { playNext() }
        }


        override fun onServiceDisconnected(name: ComponentName?) {
            musicService = null
            serviceBound = false
        }
    }


    //public state that UI observes
    var songs by mutableStateOf<List<AudioFile>>(emptyList())
        private set

    var playerState by mutableStateOf(PlayerState())
        private set


    private var progressJob: Job? = null   //updates progress bar every second

    init {
        loadSongs()
        startMusicService()
        setupMediaScannerListener()
    }

    //loads songs from device storage
    private fun loadSongs() {
        viewModelScope.launch {
            songs = repository.getSongs()
        }
    }

    //start and bind to background service
    private fun startMusicService() {
        val context = getApplication<Application>()

        //start service so it keeps running.
        context.startService(Intent(context, MusicService::class.java))

        //bind to it so we can control it
        context.bindService(
            Intent(context, MusicService::class.java),
            serviceConnection,
            Context.BIND_AUTO_CREATE
        )

    }

    //listen for when new songs are added to storage
    private fun setupMediaScannerListener() {
        mediaScanReceiver.setListner {
            loadSongs()
        }
        mediaScanReceiver.register(getApplication())

    }

    //user tapped a song -start playiing
    fun playSong(song: AudioFile) {
        musicService?.play(song)
        playerState = playerState.copy(currentSong = song, isPlaying = true)
        startProgressTracking()
    }

    //play/pause button tapped
    fun togglePlayPause() {
        val nowPlaying = musicService?.togglePlayPause() ?: false
        playerState = playerState.copy(isPlaying = nowPlaying)


        if (nowPlaying) startProgressTracking() else stopProgressTracking()
    }

    //next button -plays next song
    fun playNext() {
        val current = playerState.currentSong ?: return
        val currentIndex = songs.indexOf(current)

        val nextIndex = when {
            //shuffle on -random song
            playerState.isShuffling -> songs.indices.random()

            //repeat one -same song
            playerState.repeatMode == RepeatMode.ONE -> currentIndex

            //normal or repeat all -next song
            else -> if (currentIndex + 1 < songs.size) currentIndex + 1 else 0
        }
        playSong(songs[nextIndex])
    }

    //previous button
    fun playPrevious() {
        val current = playerState.currentSong ?: return
        val currentIndex = songs.indexOf(current)
        val prevIndex = if (currentIndex - 1 >= 0) currentIndex - 1 else songs.size - 1
        playSong(songs[prevIndex])
    }

    fun seekTo(position: Long) {
        musicService?.seekTo(position)
        playerState = playerState.copy(position = position)
    }

    fun toggleShuffle() {
        playerState = playerState.copy(isShuffling = !playerState.isShuffling)
    }

    //toggle repeat mode
    fun toggleRepeat() {
        val nextMode = when (playerState.repeatMode) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        playerState = playerState.copy(repeatMode = nextMode)
    }

    //update progress bar every second
    private fun startProgressTracking (){
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (true){
                musicService?.let{
                    playerState = playerState.copy(
                        position = it.getCurrentPosition(),
                        duration = it.getDuration()
                    )
                }
                delay(1000)  //wait for a second
            }
        }

    }

    private fun stopProgressTracking() {
        progressJob?.cancel()
    }

    //cleanup when viewmodel is destroyed
    override fun onCleared () {
        super.onCleared()
        stopProgressTracking()
        mediaScanReceiver.unregister(getApplication())
        if (serviceBound) {
            getApplication<Application>().unbindService(serviceConnection)
        }

    }


}
