package com.zeper.player.music.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zeper.player.core.data.MediaFile
import com.zeper.player.music.data.MusicPlaybackManager
import com.zeper.player.core.ui.FileDetailsDialog
import com.zeper.player.core.ui.MediaOptionsMenu
import com.zeper.player.core.ui.SectionHeader
import com.zeper.player.core.ui.SortDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicListScreen(viewModel: MusicViewModel = viewModel()) {
    val context = LocalContext.current
    val playbackManager = remember { MusicPlaybackManager.getInstance(context) }
    
    val songs by viewModel.songs.collectAsState()
    val folders by viewModel.folders.collectAsState()
    val artists by viewModel.artists.collectAsState()
    val albums by viewModel.albums.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()
    val favoriteUris by viewModel.favoriteUris.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Songs", "Artists", "Albums", "Folders")

    var activeGroupKey by remember { mutableStateOf<String?>(null) }
    var activeGroupSongs by remember { mutableStateOf<List<MediaFile>>(emptyList()) }

    var menuTarget by remember { mutableStateOf<MediaFile?>(null) }
    var detailsTarget by remember { mutableStateOf<MediaFile?>(null) }

    LaunchedEffect(selectedTab) {
        activeGroupKey = null
        activeGroupSongs = emptyList()
    }

    Scaffold(
        topBar = {
            Column {
                if (activeGroupKey == null) {
                    CenterAlignedTopAppBar(
                        title = { Text("Music Library", fontWeight = FontWeight.Bold) },
                        actions = {
                            var showSortDialog by remember { mutableStateOf(false) }
                            
                            Box {
                                IconButton(onClick = { showSortDialog = true }) {
                                    Icon(Icons.AutoMirrored.Filled.Sort, "Sort")
                                }
                                
                                if (showSortDialog) {
                                    SortDialog(
                                        currentSortOrder = sortOrder,
                                        onDismiss = { showSortDialog = false },
                                        onConfirm = {
                                            showSortDialog = false
                                            viewModel.setSortOrder(it)
                                        }
                                    )
                                }
                            }
                        }
                    )
                } else {
                    TopAppBar(
                        title = { Text(activeGroupKey!!, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        navigationIcon = {
                            IconButton(onClick = { activeGroupKey = null }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                            }
                        }
                    )
                }
                
                if (activeGroupKey == null) {
                    SecondaryScrollableTabRow(
                        selectedTabIndex = selectedTab,
                        edgePadding = 16.dp,
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary,
                        divider = {}
                    ) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTab == index,
                                onClick = { selectedTab = index },
                                text = { Text(title, style = MaterialTheme.typography.labelLarge) }
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = isLoading,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier.padding(innerPadding).fillMaxSize()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (isLoading && activeGroupKey == null && songs.isEmpty()) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else if (activeGroupKey != null) {
                    SongList(
                        songs = activeGroupSongs,
                        favoriteUris = favoriteUris,
                        onMenuClick = { menuTarget = it },
                        onSongClick = { clickedSong ->
                            playbackManager.playSong(clickedSong, activeGroupSongs)
                        }
                    )
                } else {
                    when (selectedTab) {
                        0 -> SongList(
                            songs = songs,
                            favoriteUris = favoriteUris,
                            onMenuClick = { menuTarget = it },
                            onSongClick = { clickedSong ->
                                playbackManager.playSong(clickedSong, songs)
                            }
                        )
                        1 -> GroupedList(artists, Icons.Default.Person) { key, groupSongs ->
                            activeGroupKey = key
                            activeGroupSongs = groupSongs
                        }
                        2 -> GroupedList(albums, Icons.Default.Album) { key, groupSongs ->
                            activeGroupKey = key
                            activeGroupSongs = groupSongs
                        }
                        3 -> GroupedList(folders, Icons.Default.Folder) { key, groupSongs ->
                            activeGroupKey = key
                            activeGroupSongs = groupSongs
                        }
                    }
                }

                // Dialogs
                menuTarget?.let { song ->
                    MediaOptionsMenu(
                        expanded = true,
                        onDismiss = { menuTarget = null },
                        mediaFile = song,
                        isFavorite = favoriteUris.contains(song.path),
                        onFavoriteClick = { viewModel.toggleFavorite(song) },
                        onDeleteClick = { viewModel.deleteFile(context, song) },
                        onShareClick = { /* TODO */ },
                        onDetailsClick = { detailsTarget = song }
                    )
                }

                detailsTarget?.let { song ->
                    FileDetailsDialog(
                        mediaFile = song,
                        onDismiss = { detailsTarget = null }
                    )
                }
            }
        }
    }
}

@Composable
fun SongList(
    songs: List<MediaFile>, 
    favoriteUris: Set<String>,
    onMenuClick: (MediaFile) -> Unit,
    onSongClick: (MediaFile) -> Unit
) {
    if (songs.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No music found", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(songs) { song ->
                val isFavorite = favoriteUris.contains(song.path)
                ListItem(
                    modifier = Modifier.clickable { onSongClick(song) },
                    headlineContent = { 
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isFavorite) {
                                Icon(Icons.Default.Favorite, null, tint = Color.Red, modifier = Modifier.size(14.dp).padding(end = 4.dp))
                            }
                            Text(
                                song.name, 
                                maxLines = 1, 
                                overflow = TextOverflow.Ellipsis,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    },
                    supportingContent = { 
                        Text(
                            "${song.artist ?: "Unknown Artist"} • ${song.album ?: "Unknown Album"}",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodySmall
                        ) 
                    },
                    leadingContent = {
                        Surface(
                            modifier = Modifier.size(48.dp),
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.MusicNote, 
                                    null, 
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    },
                    trailingContent = {
                        IconButton(onClick = { onMenuClick(song) }) {
                            Icon(Icons.Default.MoreVert, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun GroupedList(
    groups: Map<String, List<MediaFile>>, 
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onGroupClick: (String, List<MediaFile>) -> Unit
) {
    if (groups.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Nothing here", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            groups.forEach { (name, songs) ->
                item {
                    ListItem(
                        modifier = Modifier.clickable { onGroupClick(name, songs) },
                        headlineContent = { Text(name, fontWeight = FontWeight.Medium) },
                        supportingContent = { Text("${songs.size} songs", style = MaterialTheme.typography.bodySmall) },
                        leadingContent = {
                            Surface(
                                modifier = Modifier.size(48.dp),
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        },
                        trailingContent = {
                            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.outline)
                        }
                    )
                }
            }
        }
    }
}
