package com.zeper.player.video.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.UnstableApi
import com.zeper.player.core.data.*
import com.zeper.player.core.ui.FileDetailsDialog
import com.zeper.player.core.ui.MediaOptionsMenu
import com.zeper.player.core.ui.SectionHeader
import com.zeper.player.core.ui.SortDialog
import com.zeper.player.core.ui.theme.ZeperTheme
import kotlinx.coroutines.launch

@UnstableApi
class VideoListActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val folderName = intent.getStringExtra("folder_name") ?: "Videos"
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val prefs = remember { PreferencesManager(context) }
            val scope = rememberCoroutineScope()
            
            val themeMode by prefs.themeMode.collectAsState(initial = "auto")
            val darkTheme = when (themeMode) {
                "light" -> false
                "dark" -> true
                else -> androidx.compose.foundation.isSystemInDarkTheme()
            }
            
            val isGridView by prefs.isGridView.collectAsState(initial = false)

            ZeperTheme(darkTheme = darkTheme, prefs = prefs) {
                val videoViewModel: VideoViewModel = viewModel()
                val videos by videoViewModel.videos.collectAsState()
                val isLoading by videoViewModel.isLoading.collectAsState()
                val sortOrder by videoViewModel.sortOrder.collectAsState()
                val favoriteUris by videoViewModel.favoriteUris.collectAsState()

                var detailsTarget by remember { mutableStateOf<MediaFile?>(null) }

                Scaffold(
                    containerColor = MaterialTheme.colorScheme.background,
                    topBar = {
                        TopAppBar(
                            title = { Text(folderName) },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.background,
                                titleContentColor = MaterialTheme.colorScheme.onBackground,
                                navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                                actionIconContentColor = MaterialTheme.colorScheme.onBackground
                            ),
                            navigationIcon = {
                                IconButton(onClick = { finish() }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                                }
                            },
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
                                                videoViewModel.setSortOrder(it)
                                            }
                                        )
                                    }
                                }
                                IconButton(onClick = { 
                                    scope.launch { prefs.setGridView(!isGridView) }
                                }) {
                                    Icon(if (isGridView) Icons.AutoMirrored.Filled.List else Icons.Default.GridView, "Switch View")
                                }
                            }
                        )
                    }
                ) { innerPadding ->
                    PullToRefreshBox(
                        isRefreshing = isLoading,
                        onRefresh = { videoViewModel.refresh() },
                        modifier = Modifier.padding(innerPadding).fillMaxSize()
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            val folderVideos = videos.filter { 
                                (it.folder ?: "Internal Storage") == folderName || (folderName == "Recent Added" && videos.take(5).contains(it))
                            }

                            if (isGridView) {
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(2),
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(folderVideos) { video ->
                                        VideoGridItem(
                                            video = video,
                                            isFavorite = favoriteUris.contains(video.path),
                                            onFavoriteClick = { videoViewModel.toggleFavorite(video) },
                                            onDeleteClick = { videoViewModel.deleteFile(context, video) },
                                            onVaultClick = { videoViewModel.moveToVault(context, video) },
                                            onShareClick = { /* TODO */ },
                                            onDetailsClick = { detailsTarget = video },
                                            onClick = {
                                                val intent = android.content.Intent(this@VideoListActivity, VideoPlayerActivity::class.java).apply {
                                                    putExtra("video_uri", video.path)
                                                    putExtra("video_title", video.name)
                                                }
                                                startActivity(intent)
                                            }
                                        )
                                    }
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(folderVideos) { video ->
                                        VideoItem(
                                            video = video,
                                            isFavorite = favoriteUris.contains(video.path),
                                            onFavoriteClick = { videoViewModel.toggleFavorite(video) },
                                            onDeleteClick = { videoViewModel.deleteFile(context, video) },
                                            onVaultClick = { videoViewModel.moveToVault(context, video) },
                                            onShareClick = { /* TODO */ },
                                            onDetailsClick = { detailsTarget = video },
                                            onClick = {
                                                val intent = android.content.Intent(this@VideoListActivity, VideoPlayerActivity::class.java).apply {
                                                    putExtra("video_uri", video.path)
                                                    putExtra("video_title", video.name)
                                                }
                                                startActivity(intent)
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        detailsTarget?.let { video ->
                            FileDetailsDialog(
                                mediaFile = video,
                                onDismiss = { detailsTarget = null }
                            )
                        }
                    }
                }
            }
        }
    }
}

