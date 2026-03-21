//connects everthing together

package com.vertigo.playerbeta.viewmodel

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.IBinder
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vertigo.playerbeta.MainActivity
import com.vertigo.playerbeta.MusicPlayerApplication
import com.vertigo.playerbeta.data.AudioFile
import com.vertigo.playerbeta.data.MusicRepository
import com.vertigo.playerbeta.data.Playlist
import com.vertigo.playerbeta.data.PlaylistRepository
import com.vertigo.playerbeta.receiver.MediaScanReceiver
import com.vertigo.playerbeta.player.PlayerState
import com.vertigo.playerbeta.player.RepeatMode
import com.vertigo.playerbeta.services.MusicService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Data class to represent the current state of the Equalizer and Audio Effects
data class EqState(
    val enabled: Boolean = false, // Is the EQ currently active
    val presets: List<String> = emptyList(), // Available system EQ presets
    val currentPreset: Short = -1, // Currently selected preset index (-1 for manual)
    val bandLevels: Map<Short, Short> = emptyMap(), // Map of band index to gain level in mB
    val bassBoostStrength: Short = 0, // Strength of the bass boost (0-1000)
    val numberOfBands: Short = 0, // Number of frequency bands supported by the device
    val bandCenterFreqs: Map<Short, Int> = emptyMap(), // Center frequency for each band in mHz
    val levelRange: ShortArray = shortArrayOf(-1500, 1500) // Supported gain range (min/max)
)



//main controller -connects ui to music playback
class PlayerViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = MusicRepository(application)
    private val playlistRepository = PlaylistRepository(application)
    private val mediaScanReceiver = MediaScanReceiver()


    //Service connection binds background music service to this activity
    private var musicService: MusicService? = null
    private var serviceBound = false

    //CONNECTION B2WIN Viewmodel and musicservice
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MusicService.MusicBinder
            val serviceInstance = binder.getService()
            musicService = serviceInstance
            serviceBound = true

            // SYNC STATE: Check if music is already playing in the background (e.g. returning from notification)
            val current = serviceInstance.getCurrentSong()
            if (current != null) {
                playerState = playerState.copy(
                    currentSong = current,
                    isPlaying = serviceInstance.isPlaying()
                )
                if (serviceInstance.isPlaying()) {
                    startProgressTracking()
                }
            }

            //CONNECT NOTIFICATION BUTTONS TO VIEWMODEL
            musicService?.setOnNotificationAction { action ->
                handleNotificationAction(action)
            }


            //when song finishes naturally -play next
            musicService?.setOnCompleteListener { playNext() }
            updateEqState()
        }


        override fun onServiceDisconnected(name: ComponentName?) {
            musicService = null
            serviceBound = false
        }
    }

    //called when user presses buttons on notification
    private fun handleNotificationAction(action: String) {
        when (action) {
            "PLAY_PAUSE" -> togglePlayPause()
            "NEXT" -> playNext()
            "PREVIOUS" -> playPrevious()
        }
    }


    //public state that UI observes
    var deviceSongs by mutableStateOf<List<AudioFile>>(emptyList())
        private set
    var importedSongs by mutableStateOf<List<AudioFile>>(emptyList())
        private set
    var sharedSongs by mutableStateOf<List<AudioFile>>(emptyList())
        private set


    //combined list for display
    val allSongs: List<AudioFile>
        get() = deviceSongs + importedSongs + sharedSongs

    //public state that UI observes



    var playerState by mutableStateOf(PlayerState())
        private set
    private var progressJob: Job? = null   //updates progress bar every second

    // 🔍 SEARCH & FILTERING STATE
    // User's current search string
    var searchQuery by mutableStateOf("")
        private set

    // Calculated list of songs based on search query (case-insensitive)
    val filteredSongs: List<AudioFile>
        get() = if (searchQuery.isBlank()) {
            allSongs
        } else {
            allSongs.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                it.artist.contains(searchQuery, ignoreCase = true) ||
                it.album.contains(searchQuery, ignoreCase = true)
            }
        }

    fun updateSearchQuery(query: String) {
        searchQuery = query
    }

    // 📂 LIBRARY VIEW STATE
    // Currently selected tab (Songs, Albums, Artists, Folders, Playlists)
    var libraryTab by mutableStateOf(LibraryTab.Songs)
        private set

    fun updateLibraryTab(tab: LibraryTab) {
        libraryTab = tab
    }

    // Grouping logic for the various library views
    val groupedByArtist: Map<String, List<AudioFile>>
        get() = filteredSongs.groupBy { it.artist }

    val groupedByAlbum: Map<String, List<AudioFile>>
        get() = filteredSongs.groupBy { it.album }

    val groupedByFolder: Map<String, List<AudioFile>>
        get() = filteredSongs.groupBy { song ->
            val path = song.uri.path ?: ""
            val segments = path.split("/")
            // Extract the parent folder name from the file path
            if (segments.size > 1) segments[segments.size - 2] else "Internal"
        }

    // Navigation enum for the library tabs
    enum class LibraryTab {
        Songs, Albums, Artists, Folders, Playlists
    }

    // PLAYLIST STATE
    var playlists by mutableStateOf<List<Playlist>>(emptyList())
        private set

    // SMART PLAYLIST STATE
    // ❤️ FAVORITES & SMART PLAYLISTS
    // Set of song IDs that are favorited
    var favorites by mutableStateOf<Set<Long>>(emptySet())
        private set

    // Compute lists for smart playlists
    val favoriteSongs: List<AudioFile>
        get() = allSongs.filter { it.id in favorites }

    val recentlyAddedSongs: List<AudioFile>
        get() = allSongs.sortedByDescending { it.dateAdded }.take(20)

    // Using mutableStateListOf to ensure UI updates when history changes
    private val _recentlyPlayedSongs = mutableStateListOf<AudioFile>()
    val recentlyPlayedSongs: List<AudioFile> get() = _recentlyPlayedSongs

    // 🎚️ EQUALIZER & AUDIO EFFECTS
    var eqState by mutableStateOf(EqState())
        private set

    // Toggle the active state of the EQ
    fun toggleEqualizer() {
        val newState = !eqState.enabled
        musicService?.setEqualizerEnabled(newState)
        eqState = eqState.copy(enabled = newState)
    }

    // Apply a specific system preset
    fun setPreset(index: Short) {
        musicService?.usePreset(index)
        updateEqState()
    }

    // Manually set a gain level for a specific frequency band
    fun setBandLevel(band: Short, level: Short) {
        musicService?.setBandLevel(band, level)
        updateEqState()
    }

    // Set the strength of the bass boost effect
    fun setBassBoost(strength: Short) {
        musicService?.setBassBoost(strength)
        updateEqState()
    }

    // Sync the internal eqState with the service's current effect levels
    private fun updateEqState() {
        musicService?.let { service ->
            val bands = service.getNumberOfBands()
            val levels = mutableMapOf<Short, Short>()
            val freqs = mutableMapOf<Short, Int>()
            for (i in 0 until bands) {
                val band = i.toShort()
                levels[band] = service.getBandLevel(band)
                freqs[band] = service.getBandCenterFreq(band)
            }
            eqState = EqState(
                enabled = service.isEqualizerEnabled(),
                presets = service.getEqualizerPresets(),
                currentPreset = service.getCurrentPreset(),
                bandLevels = levels,
                bassBoostStrength = service.getBassBoostStrength(),
                numberOfBands = bands,
                bandCenterFreqs = freqs,
                levelRange = service.getBandLevelRange()
            )
        }
    }

    fun toggleFavorite(songId: Long) {
        favorites = if (favorites.contains(songId)) {
            favorites - songId
        } else {
            favorites + songId
        }
        playlistRepository.saveFavorites(favorites)
    }

    fun loadPlaylists() {
        playlists = playlistRepository.getPlaylists()
        favorites = playlistRepository.getFavorites()
    }

    fun createPlaylist(name: String) {
        val newPlaylist = Playlist(
            id = System.currentTimeMillis().toString(),
            name = name,
            songIds = emptyList()
        )
        playlists = playlists + newPlaylist
        playlistRepository.savePlaylists(playlists)
    }

    fun addSongToPlaylist(songId: Long, playlistId: String) {
        playlists = playlists.map {
            if (it.id == playlistId) {
                if (it.songIds.contains(songId)) it
                else it.copy(songIds = it.songIds + songId)
            } else it
        }
        playlistRepository.savePlaylists(playlists)
    }

    fun removeSongFromPlaylist(songId: Long, playlistId: String) {
        playlists = playlists.map {
            if (it.id == playlistId) {
                it.copy(songIds = it.songIds - songId)
            } else it
        }
        playlistRepository.savePlaylists(playlists)
    }

    fun deletePlaylist(playlistId: String) {
        playlists = playlists.filter { it.id != playlistId }
        playlistRepository.savePlaylists(playlists)
    }

    // SLEEP TIMER STATE
    var sleepTimerEndTime by mutableStateOf(0L)
        private set

    private var sleepTimerJob: Job? = null

    fun startSleepTimer(minutes: Int) {
        stopSleepTimer()
        val durationMillis = minutes * 60 * 1000L
        sleepTimerEndTime = System.currentTimeMillis() + durationMillis
        
        sleepTimerJob = viewModelScope.launch {
            while (System.currentTimeMillis() < sleepTimerEndTime) {
                delay(1000)
            }
            if (playerState.isPlaying) {
                togglePlayPause()
            }
            sleepTimerEndTime = 0L
        }
    }

    fun stopSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerEndTime = 0L
    }

    init {
        loadSongs()
        loadPlaylists()
        startMusicService()
        setupMediaScannerListener()
    }

    //loads songs from device storage
    private fun loadSongs() {
        viewModelScope.launch {
            deviceSongs = repository.getSongs()
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
    private fun updateNextSong() {
        val songs = allSongs
        if (songs.isEmpty()) {
            musicService?.setNextSong(null)
            return
        }

        val current = playerState.currentSong ?: return
        val currentIndex = songs.indexOf(current)
        if (currentIndex == -1) {
            musicService?.setNextSong(null)
            return
        }

        val nextIndex = when {
            playerState.isShuffling -> songs.indices.random()
            playerState.repeatMode == RepeatMode.ONE -> currentIndex
            else -> if (currentIndex + 1 < songs.size) currentIndex + 1 else 0
        }
        
        musicService?.setNextSong(songs[nextIndex])
    }

    fun playSong(song: AudioFile) {
        // Track recently played
        if (_recentlyPlayedSongs.contains(song)) {
            _recentlyPlayedSongs.remove(song)
        }
        _recentlyPlayedSongs.add(0, song)
        if (_recentlyPlayedSongs.size > 20) _recentlyPlayedSongs.removeAt(_recentlyPlayedSongs.size - 1)

        musicService?.play(song)
        playerState = playerState.copy(currentSong = song, isPlaying = true)
        updateNextSong()
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
        val songs = allSongs
        if (songs.isEmpty()) return
        
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
        val songs = allSongs
        if (songs.isEmpty()) return
        
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
        updateNextSong()
    }

    //toggle repeat mode
    fun toggleRepeat() {
        val nextMode = when (playerState.repeatMode) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        playerState = playerState.copy(repeatMode = nextMode)
        updateNextSong()
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
    
    fun checkForPendingImports(activity: MainActivity) {
        //checks picked file
        getApplication<MusicPlayerApplication>().pickedUri?.let { uri ->
            importUri(uri, "Imported")
            getApplication<MusicPlayerApplication>().pickedUri = null
        }

        //checks thr shared files
        activity.sharedUris?.let { uris ->
            if (uris.isNotEmpty()) {
                importSharedUris(uris)
                activity.sharedUris = emptyList()
            }
        }
    }
    
    //import files share from telegram
    fun importSharedUris(uris: List<Uri>){
        val context = getApplication<Application>()
        uris.forEach { uri ->
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (e: Exception) { }

            repository.getAudioFromUri(uri, "shared")?.let {song ->
                if (sharedSongs.none {it.uri == song.uri}) {
                    sharedSongs = sharedSongs + song
                }
            }
        }
        if (playerState.currentSong == null && sharedSongs.isNotEmpty()) {
            playSong(sharedSongs.last())
        }
    }

    //import single file
    private fun importUri(uri: Uri, source: String) {
        val context = getApplication<Application>()
        try {
            context.contentResolver.takePersistableUriPermission(
                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (e: Exception) {
        }
        repository.getAudioFromUri(uri, source)?.let { song ->
            if (importedSongs.none { it.uri == song.uri }) {
                importedSongs = importedSongs + song
                playSong(song)
            }
        }
    }

    //REMOVE IMPORTED/SHARED SONGS FROM LIST
    fun removedImportedSong(song: AudioFile) {
        importedSongs = importedSongs.filter { it.id != song.id }
        sharedSongs = sharedSongs.filter { it.id != song.id }
        if (playerState.currentSong?.id == song.id) {
            playerState = playerState.copy(currentSong = null, isPlaying = false)
        }

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
