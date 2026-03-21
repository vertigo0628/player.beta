//main UI

package com.vertigo.playerbeta.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vertigo.playerbeta.MainActivity
import com.vertigo.playerbeta.data.AudioFile
import com.vertigo.playerbeta.data.Playlist
import com.vertigo.playerbeta.player.RepeatMode
import com.vertigo.playerbeta.viewmodel.PlayerViewModel
import kotlinx.coroutines.launch

//main screen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(viewModel: PlayerViewModel, activity: MainActivity) {
    LaunchedEffect(Unit) {
        viewModel.checkForPendingImports(activity)
    }
    val state = viewModel.playerState
    var showSleepTimerSheet by remember { mutableStateOf(false) }
    var showEqSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    
    var showAddToPlaylistSheet by remember { mutableStateOf<AudioFile?>(null) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var selectedPlaylist by remember { mutableStateOf<Playlist?>(null) }

    // EQ BOTTOM SHEET
    if (showEqSheet) {
        ModalBottomSheet(
            onDismissRequest = { showEqSheet = false },
            sheetState = rememberModalBottomSheetState()
        ) {
            EqualizerSheetContent(viewModel)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("V-Player(Beta)") },
                actions = {
                    //new import button
                    IconButton(onClick = { activity.openFilePicker() }) {
                        Icon(Icons.Default.Add, contentDescription = "Import from files")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            //TOP SECTION - PLAYER CONTROLS
            state.currentSong?.let { song ->
                PlayerControls(
                    song = song,
                    isPlaying = state.isPlaying,
                    duration = state.duration,
                    position = state.position,
                    isShuffling = state.isShuffling,
                    repeatMode = state.repeatMode,
                    onPlayPause = { viewModel.togglePlayPause() },
                    onNext = { viewModel.playNext() },
                    onPrevious = { viewModel.playPrevious() },
                    onSeek = { viewModel.seekTo(it) },
                    onToggleShuffle = { viewModel.toggleShuffle() },
                    onToggleRepeat = { viewModel.toggleRepeat() },
                    onSleepTimerClick = { showSleepTimerSheet = true },
                    isSleepTimerActive = viewModel.sleepTimerEndTime > 0,
                    isFavorite = viewModel.favorites.contains(song.id),
                    onToggleFavorite = { viewModel.toggleFavorite(song.id) },
                    onEqClick = { showEqSheet = true }
                )
            }

            if (showSleepTimerSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showSleepTimerSheet = false },
                    sheetState = sheetState
                ) {
                    SleepTimerSheetContent(
                        onSelect = { minutes ->
                            viewModel.startSleepTimer(minutes)
                            scope.launch { sheetState.hide() }.invokeOnCompletion {
                                if (!sheetState.isVisible) showSleepTimerSheet = false
                            }
                        },
                        onStop = {
                            viewModel.stopSleepTimer()
                            scope.launch { sheetState.hide() }.invokeOnCompletion {
                                if (!sheetState.isVisible) showSleepTimerSheet = false
                            }
                        },
                        isActive = viewModel.sleepTimerEndTime > 0,
                        endTime = viewModel.sleepTimerEndTime
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 🔍 SEARCH BAR: Filters the allSongs list in the ViewModel
            OutlinedTextField(
                value = viewModel.searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                placeholder = { Text("Search songs, artists, albums...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (viewModel.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = MaterialTheme.shapes.medium
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 📂 LIBRARY TABS: Navigation between different organizational views
            TabRow(
                selectedTabIndex = viewModel.libraryTab.ordinal,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                PlayerViewModel.LibraryTab.values().forEach { tab ->
                    Tab(
                        selected = viewModel.libraryTab == tab,
                        onClick = { viewModel.updateLibraryTab(tab) },
                        text = { Text(tab.name) },
                        icon = {
                            Icon(
                                imageVector = when (tab) {
                                    PlayerViewModel.LibraryTab.Songs -> Icons.Default.MusicNote
                                    PlayerViewModel.LibraryTab.Albums -> Icons.Default.Album
                                    PlayerViewModel.LibraryTab.Artists -> Icons.Default.Person
                                    PlayerViewModel.LibraryTab.Folders -> Icons.Default.Folder
                                    PlayerViewModel.LibraryTab.Playlists -> Icons.Default.QueueMusic
                                },
                                contentDescription = null
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 📄 MAIN CONTENT AREA: Renders the list based on the selected organizational tab
            LazyColumn(modifier = Modifier.weight(1f)) {
                when (viewModel.libraryTab) {
                    PlayerViewModel.LibraryTab.Songs -> {
                        renderSongList(viewModel, state, onAddToPlaylist = { showAddToPlaylistSheet = it })
                    }
                    PlayerViewModel.LibraryTab.Albums -> {
                        renderGroupedList(viewModel.groupedByAlbum, viewModel, state, onAddToPlaylist = { showAddToPlaylistSheet = it })
                    }
                    PlayerViewModel.LibraryTab.Artists -> {
                        renderGroupedList(viewModel.groupedByArtist, viewModel, state, onAddToPlaylist = { showAddToPlaylistSheet = it })
                    }
                    PlayerViewModel.LibraryTab.Folders -> {
                        renderGroupedList(viewModel.groupedByFolder, viewModel, state, onAddToPlaylist = { showAddToPlaylistSheet = it })
                    }
                    PlayerViewModel.LibraryTab.Playlists -> {
                        if (selectedPlaylist == null) {
                            renderPlaylistList(
                                playlists = viewModel.playlists,
                                favoritesCount = viewModel.favorites.size,
                                recentlyAddedCount = viewModel.recentlyAddedSongs.size,
                                recentlyPlayedCount = viewModel.recentlyPlayedSongs.size,
                                onCreateClick = { showCreatePlaylistDialog = true },
                                onPlaylistClick = { selectedPlaylist = it },
                                onDeleteClick = { viewModel.deletePlaylist(it.id) },
                                onSmartPlaylistClick = { type ->
                                    selectedPlaylist = when(type) {
                                        "Favorites" -> Playlist("favorites", "Favorites", viewModel.favorites.toList())
                                        "Recently Added" -> Playlist("recently_added", "Recently Added", viewModel.recentlyAddedSongs.map { it.id })
                                        "Recently Played" -> Playlist("recently_played", "Recently Played", viewModel.recentlyPlayedSongs.map { it.id })
                                        else -> null
                                    }
                                }
                            )
                        } else {
                            renderPlaylistSongs(
                                playlist = selectedPlaylist!!,
                                viewModel = viewModel,
                                state = state,
                                onBackClick = { selectedPlaylist = null },
                                onRemoveSong = { songId -> 
                                    if (selectedPlaylist!!.id == "favorites") {
                                        viewModel.toggleFavorite(songId)
                                    } else {
                                        viewModel.removeSongFromPlaylist(songId, selectedPlaylist!!.id)
                                    }
                                    // Refresh the temporary playlist object
                                    selectedPlaylist = when(selectedPlaylist!!.id) {
                                        "favorites" -> Playlist("favorites", "Favorites", viewModel.favorites.toList())
                                        "recently_added" -> Playlist("recently_added", "Recently Added", viewModel.recentlyAddedSongs.map { it.id })
                                        "recently_played" -> Playlist("recently_played", "Recently Played", viewModel.recentlyPlayedSongs.map { it.id })
                                        else -> viewModel.playlists.find { it.id == selectedPlaylist!!.id }
                                    }
                                }
                            )
                        }
                    }
                }
            }

            if (showCreatePlaylistDialog) {
                var name by remember { mutableStateOf("") }
                AlertDialog(
                    onDismissRequest = { showCreatePlaylistDialog = false },
                    title = { Text("New Playlist") },
                    text = {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Playlist Name") },
                            singleLine = true
                        )
                    },
                    confirmButton = {
                        Button(onClick = {
                            if (name.isNotBlank()) {
                                viewModel.createPlaylist(name)
                                showCreatePlaylistDialog = false
                            }
                        }) { Text("Create") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showCreatePlaylistDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            if (showAddToPlaylistSheet != null) {
                ModalBottomSheet(onDismissRequest = { showAddToPlaylistSheet = null }) {
                    AddToPlaylistContent(
                        playlists = viewModel.playlists,
                        onPlaylistSelect = { playlist ->
                            viewModel.addSongToPlaylist(showAddToPlaylistSheet!!.id, playlist.id)
                            showAddToPlaylistSheet = null
                        },
                        onCreateNew = {
                            showCreatePlaylistDialog = true
                            // We don't null showAddToPlaylistSheet here so we can come back? 
                            // Actually it's better to just close it.
                            showAddToPlaylistSheet = null
                        }
                    )
                }
            }
        }
    }
}

private fun LazyListScope.renderSongList(
    viewModel: PlayerViewModel,
    state: com.vertigo.playerbeta.player.PlayerState,
    onAddToPlaylist: (AudioFile) -> Unit
) {
    val filtered = viewModel.filteredSongs
    if (filtered.isEmpty()) {
        item {
            Text(
                "No songs found",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyLarge
            )
        }
    } else {
        items(filtered) { song: AudioFile ->
            SongListItem(
                song = song,
                isCurrentSong = state.currentSong?.id == song.id,
                isPlaying = state.isPlaying && state.currentSong?.id == song.id,
                onSongClick = { viewModel.playSong(song) },
                onRemove = if (song.source != "Device") {
                    { viewModel.removedImportedSong(song) }
                } else null,
                onAddToPlaylist = { onAddToPlaylist(song) },
                isFavorite = viewModel.favorites.contains(song.id),
                onToggleFavorite = { viewModel.toggleFavorite(song.id) }
            )
        }
    }
}

private fun LazyListScope.renderPlaylistList(
    playlists: List<Playlist>,
    favoritesCount: Int,
    recentlyAddedCount: Int,
    recentlyPlayedCount: Int,
    onCreateClick: () -> Unit,
    onPlaylistClick: (Playlist) -> Unit,
    onDeleteClick: (Playlist) -> Unit,
    onSmartPlaylistClick: (String) -> Unit
) {
    item {
        Text("Smart Playlists", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(16.dp))
    }

    item {
        ListItem(
            headlineContent = { Text("Favorites") },
            supportingContent = { Text("$favoritesCount songs") },
            leadingContent = { Icon(Icons.Default.Favorite, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            modifier = Modifier.clickable { onSmartPlaylistClick("Favorites") }
        )
    }

    item {
        ListItem(
            headlineContent = { Text("Recently Added") },
            supportingContent = { Text("$recentlyAddedCount songs") },
            leadingContent = { Icon(Icons.Default.NewReleases, contentDescription = null) },
            modifier = Modifier.clickable { onSmartPlaylistClick("Recently Added") }
        )
    }

    item {
        ListItem(
            headlineContent = { Text("Recently Played") },
            supportingContent = { Text("$recentlyPlayedCount songs") },
            leadingContent = { Icon(Icons.Default.History, contentDescription = null) },
            modifier = Modifier.clickable { onSmartPlaylistClick("Recently Played") }
        )
    }

    item {
        Text("My Playlists", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(16.dp))
    }

    item {
        ListItem(
            headlineContent = { Text("Create New Playlist", color = MaterialTheme.colorScheme.primary) },
            leadingContent = { Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            modifier = Modifier.clickable { onCreateClick() }
        )
    }

    if (playlists.isEmpty()) {
        item {
            Text("No playlists yet", modifier = Modifier.padding(16.dp))
        }
    } else {
        items(playlists) { playlist ->
            ListItem(
                headlineContent = { Text(playlist.name) },
                supportingContent = { Text("${playlist.songIds.size} songs") },
                leadingContent = { Icon(Icons.Default.QueueMusic, contentDescription = null) },
                trailingContent = {
                    IconButton(onClick = { onDeleteClick(playlist) }) {
                        Icon(Icons.Default.Close, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                },
                modifier = Modifier.clickable { onPlaylistClick(playlist) }
            )
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.renderPlaylistSongs(
    playlist: Playlist,
    viewModel: PlayerViewModel,
    state: com.vertigo.playerbeta.player.PlayerState,
    onBackClick: () -> Unit,
    onRemoveSong: (Long) -> Unit
) {
    item {
        ListItem(
            headlineContent = { Text("Back to Playlists", color = MaterialTheme.colorScheme.primary) },
            leadingContent = { Icon(Icons.Default.SkipPrevious, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            modifier = Modifier.clickable { onBackClick() }
        )
    }
    
    item {
        Text(
            text = playlist.name,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(16.dp)
        )
    }

    val songs = viewModel.allSongs.filter { playlist.songIds.contains(it.id) }
    
    if (songs.isEmpty()) {
        item {
            Text("No songs in this playlist", modifier = Modifier.padding(16.dp))
        }
    } else {
        items(songs) { song ->
            SongListItem(
                song = song,
                isCurrentSong = state.currentSong?.id == song.id,
                isPlaying = state.isPlaying && state.currentSong?.id == song.id,
                onSongClick = { viewModel.playSong(song) },
                onRemove = { onRemoveSong(song.id) }
            )
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.renderGroupedList(
    groupedSongs: Map<String, List<AudioFile>>,
    viewModel: PlayerViewModel,
    state: com.vertigo.playerbeta.player.PlayerState,
    onAddToPlaylist: (AudioFile) -> Unit
) {
    if (groupedSongs.isEmpty()) {
        item {
            Text(
                "No items found",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyLarge
            )
        }
    } else {
        groupedSongs.forEach { (groupName, songs) ->
            item {
                Text(
                    text = groupName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(16.dp, 8.dp)
                )
            }
            items(songs) { song ->
                SongListItem(
                    song = song,
                    isCurrentSong = state.currentSong?.id == song.id,
                    isPlaying = state.isPlaying && state.currentSong?.id == song.id,
                    onSongClick = { viewModel.playSong(song) },
                    onRemove = if (song.source != "Device") {
                        { viewModel.removedImportedSong(song) }
                    } else null,
                    onAddToPlaylist = { onAddToPlaylist(song) },
                    isFavorite = viewModel.favorites.contains(song.id),
                    onToggleFavorite = { viewModel.toggleFavorite(song.id) }
                )
            }
        }
    }
}

//player card with album art, button and progress bar
@Composable
fun PlayerControls(
    song: AudioFile,
    isPlaying: Boolean,
    duration: Long,
    position: Long,
    isShuffling: Boolean,
    repeatMode: RepeatMode,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onSleepTimerClick: () -> Unit,
    isSleepTimerActive: Boolean,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onEqClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            //placeholder for album art
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = "Album Art",
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            //song title and artist
            Text(
                text = song.title,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            //progress bar slider
            Slider(
                value = position.toFloat(),
                onValueChange = { onSeek(it.toLong()) },
                valueRange = 0f..duration.toFloat().coerceAtLeast(1f),
                modifier = Modifier.fillMaxWidth()
            )

            //time labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(formatTime(position), style = MaterialTheme.typography.bodySmall)
                Text(formatTime(duration), style = MaterialTheme.typography.bodySmall)
            }

            Spacer(modifier = Modifier.height(16.dp))

            //main control buttons prev/play/next
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(32.dp)
            ) {
                IconButton(onClick = onPrevious) {
                    Icon(
                        Icons.Default.SkipPrevious,
                        contentDescription = "Previous",
                        modifier = Modifier.size(48.dp)
                    )
                }
                //Big play/pause button
                FilledIconButton(
                    onClick = onPlayPause,
                    modifier = Modifier.size(64.dp)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        modifier = Modifier.size(32.dp)
                    )
                }

                IconButton(onClick = onNext) {
                    Icon(
                        Icons.Default.SkipNext,
                        contentDescription = "Next",
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            //shuffle and repeat buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                //Shuffle button
                IconButton(onClick = onToggleShuffle) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (isShuffling) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                //REPEAT BUTTON
                IconButton(onClick = onToggleRepeat) {
                    Icon(
                        imageVector = when (repeatMode) {
                            RepeatMode.OFF -> Icons.Default.Repeat
                            RepeatMode.ALL -> Icons.Default.Repeat
                            RepeatMode.ONE -> Icons.Default.RepeatOne
                        },
                        contentDescription = "Repeat",
                        tint = if (repeatMode != RepeatMode.OFF) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // SLEEP TIMER BUTTON
                IconButton(onClick = onSleepTimerClick) {
                    Icon(
                        imageVector = if (isSleepTimerActive) Icons.Default.Timer else Icons.Default.TimerOff,
                        contentDescription = "Sleep Timer",
                        tint = if (isSleepTimerActive) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // FAVORITE BUTTON
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (isFavorite) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // EQ BUTTON
                IconButton(onClick = onEqClick) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Equalizer"
                    )
                }
            }
        }
    }
}

@Composable
fun SleepTimerSheetContent(
    onSelect: (Int) -> Unit,
    onStop: () -> Unit,
    isActive: Boolean,
    endTime: Long
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Sleep Timer", style = MaterialTheme.typography.titleLarge)
        
        if (isActive) {
            val remaining = (endTime - System.currentTimeMillis()) / 1000
            val minutes = remaining / 60
            val seconds = remaining % 60
            Text(
                "Active: ${minutes}:${seconds.toString().padStart(2, '0')} remaining",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            FilledIconButton(
                onClick = onStop,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Stop Timer")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        
        Text("Turn off music in:", style = MaterialTheme.typography.bodyMedium)
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            TimerOption(15, onSelect)
            TimerOption(30, onSelect)
            TimerOption(60, onSelect)
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun TimerOption(minutes: Int, onSelect: (Int) -> Unit) {
    Card(
        onClick = { onSelect(minutes) },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        modifier = Modifier.size(80.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text("${minutes}m", style = MaterialTheme.typography.titleMedium)
        }
    }
}

//Single row in the song list
@Composable
fun SongListItem(
    song: AudioFile,
    isCurrentSong: Boolean,
    isPlaying: Boolean,
    onSongClick: () -> Unit,
    onRemove: (() -> Unit)? = null,
    onAddToPlaylist: (() -> Unit)? = null,
    isFavorite: Boolean = false,
    onToggleFavorite: (() -> Unit)? = null
) {
    ListItem(
        headlineContent = {
            Text(
                text = song.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (isCurrentSong) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface
            )
        },
        supportingContent = {
            Text(
                text = "${song.artist} - ${formatTime(song.duration)}",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        trailingContent = {
            Row {
                onToggleFavorite?.let {
                    IconButton(onClick = it) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (isFavorite) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                onAddToPlaylist?.let {
                    IconButton(onClick = it) {
                        Icon(Icons.Default.PlaylistAdd, contentDescription = "Add to playlist")
                    }
                }
                onRemove?.let {
                    IconButton(onClick = it) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Remove",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        },
        leadingContent = {
            Icon(
                imageVector = if (isPlaying) Icons.Default.PlayArrow else Icons.Default.MusicNote,
                contentDescription = null,
                tint = if (isCurrentSong) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        modifier = Modifier.clickable { onSongClick() }
    )
}

@Composable
fun AddToPlaylistContent(
    playlists: List<Playlist>,
    onPlaylistSelect: (Playlist) -> Unit,
    onCreateNew: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text("Add to Playlist", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 16.dp))
        
        ListItem(
            headlineContent = { Text("Create New Playlist") },
            leadingContent = { Icon(Icons.Default.Add, contentDescription = null) },
            modifier = Modifier.clickable { onCreateNew() }
        )
        
        LazyColumn(modifier = Modifier.height(300.dp)) {
            items(playlists) { playlist ->
                ListItem(
                    headlineContent = { Text(playlist.name) },
                    supportingContent = { Text("${playlist.songIds.size} songs") },
                    leadingContent = { Icon(Icons.Default.QueueMusic, contentDescription = null) },
                    modifier = Modifier.clickable { onPlaylistSelect(playlist) }
                )
            }
        }
    }
}

@Composable
fun EqualizerSheetContent(viewModel: PlayerViewModel) {
    val eqState = viewModel.eqState
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Equalizer", style = MaterialTheme.typography.titleLarge)
            Switch(
                checked = eqState.enabled,
                onCheckedChange = { viewModel.toggleEqualizer() }
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (eqState.enabled) {
            // PRESETS
            Text("Presets", style = MaterialTheme.typography.titleMedium)
            LazyRow(modifier = Modifier.padding(vertical = 8.dp)) {
                itemsIndexed(eqState.presets) { index, preset ->
                    FilterChip(
                        selected = eqState.currentPreset == index.toShort(),
                        onClick = { viewModel.setPreset(index.toShort()) },
                        label = { Text(preset) },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // BANDS
            Text("Bands", style = MaterialTheme.typography.titleMedium)
            eqState.bandLevels.forEach { (band, level) ->
                val freq = eqState.bandCenterFreqs[band] ?: 0
                val freqLabel = if (freq >= 1000000) "${freq / 1000000} kHz" else "${freq / 1000} Hz"
                
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(freqLabel, style = MaterialTheme.typography.bodySmall)
                        Text("${level / 100} dB", style = MaterialTheme.typography.bodySmall)
                    }
                    Slider(
                        value = level.toFloat(),
                        onValueChange = { viewModel.setBandLevel(band, it.toInt().toShort()) },
                        valueRange = eqState.levelRange[0].toFloat()..eqState.levelRange[1].toFloat()
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // BASS BOOST
            Text("Bass Boost", style = MaterialTheme.typography.titleMedium)
            Slider(
                value = eqState.bassBoostStrength.toFloat(),
                onValueChange = { viewModel.setBassBoost(it.toInt().toShort()) },
                valueRange = 0f..1000f
            )
        } else {
            Text("Equalizer is disabled", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}

//converts milliseconds to minutes and seconds format
private fun formatTime(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}
