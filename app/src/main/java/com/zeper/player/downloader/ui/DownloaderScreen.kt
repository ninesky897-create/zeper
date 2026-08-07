package com.zeper.player.downloader.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.valentinilk.shimmer.shimmer
import com.zeper.player.core.data.DownloadEntity
import com.zeper.player.core.data.DownloadStatus
import com.zeper.player.downloader.data.model.YouTubeSearchItem
import com.zeper.player.downloader.ui.YouTubeViewModel
import com.zeper.player.downloader.ui.YouTubeUiState
import com.zeper.player.video.ui.formatSize
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DownloaderScreen(
    initialUrl: String? = null,
    viewModel: DownloaderViewModel = viewModel(),
    ytViewModel: YouTubeViewModel = viewModel(),
    onBack: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(1) } // Default to YouTube tab
    val tabs = listOf("Search", "YouTube", "Music", "Downloads")
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                Spacer(Modifier.height(8.dp))
                
                // Modern Search Section - Row with Search Bar, Download Icon, and Login Icon
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    var searchQuery by remember { mutableStateOf("") }
                    val suggestions by ytViewModel.suggestions.collectAsState()
                    var active by remember { mutableStateOf(false) }

                    // Search Bar taking the remaining space
                    Box(modifier = Modifier.weight(1f)) {
                        SearchBar(
                            query = searchQuery,
                            onQueryChange = { 
                                searchQuery = it
                                ytViewModel.updateSuggestions(it)
                            },
                            onSearch = { 
                                ytViewModel.searchVideos(it)
                                ytViewModel.searchMusic(it)
                                active = false
                            },
                            active = active,
                            onActiveChange = { active = it },
                            placeholder = { Text("Search YouTube or Music...", fontSize = 14.sp) },
                            leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(20.dp)) },
                            trailingIcon = { 
                                if(searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = ""; ytViewModel.updateSuggestions("") }) {
                                        Icon(Icons.Default.Clear, null, modifier = Modifier.size(20.dp))
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            LazyColumn {
                                items(suggestions) { suggestion ->
                                    ListItem(
                                        headlineContent = { Text(suggestion) },
                                        leadingContent = { Icon(Icons.Default.History, null) },
                                        modifier = Modifier.clickable {
                                            searchQuery = suggestion
                                            ytViewModel.searchVideos(suggestion)
                                            ytViewModel.searchMusic(suggestion)
                                            active = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.width(8.dp))

                    // Login Icon exactly as requested: [Search Bar] [Login Icon]
                    IconButton(
                        onClick = { /* TODO: Google Login Implementation */ },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "Login",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    edgePadding = 16.dp,
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.primary,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = MaterialTheme.colorScheme.primary
                        )
                    },
                    divider = {}
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            when (selectedTab) {
                0 -> SearchTabContent(ytViewModel)
                1 -> YouTubeTabContent(ytViewModel)
                2 -> MusicTabContent(ytViewModel)
                3 -> YouTubeDownloadsTabContent(viewModel)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchTabContent(viewModel: YouTubeViewModel) {
    val trending by viewModel.trendingSearches.collectAsState()
    
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Trending Searches", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
        Spacer(modifier = Modifier.height(12.dp))
        
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            trending.forEach { tag ->
                AssistChip(
                    onClick = { viewModel.searchVideos(tag) },
                    label = { Text(tag) },
                    shape = RoundedCornerShape(16.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        Text("Popular Categories", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
        Spacer(modifier = Modifier.height(12.dp))
        
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            SiteIcon("Gaming", Icons.Default.VideogameAsset, Color(0xFF4CAF50))
            SiteIcon("News", Icons.Default.Newspaper, Color(0xFF2196F3))
            SiteIcon("Learning", Icons.Default.School, Color(0xFFFF9800))
            SiteIcon("Tech", Icons.Default.Devices, Color(0xFF9C27B0))
        }
    }
}

@Composable
fun SiteIcon(name: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            shape = CircleShape,
            color = color.copy(alpha = 0.1f),
            modifier = Modifier.size(56.dp)
        ) {
            Icon(icon, null, modifier = Modifier.padding(16.dp).size(24.dp), tint = color)
        }
        Text(name, fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
fun YouTubeTabContent(viewModel: YouTubeViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    
    when (val state = uiState) {
        is YouTubeUiState.Idle -> {
            LaunchedEffect(Unit) { viewModel.searchVideos("") }
            YouTubeSkeleton()
        }
        is YouTubeUiState.Loading -> YouTubeSkeleton()
        is YouTubeUiState.Success -> {
            LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
                items(state.videos) { video ->
                    YouTubeVideoCard(video, viewModel)
                }
            }
        }
        is YouTubeUiState.Error -> ErrorView(state.message) { viewModel.searchVideos("") }
    }
}

@Composable
fun YouTubeSkeleton() {
    Column(modifier = Modifier.shimmer()) {
        repeat(3) {
            Box(modifier = Modifier.fillMaxWidth().height(200.dp).background(Color.Gray.copy(alpha = 0.3f)))
            Spacer(modifier = Modifier.height(12.dp))
            Row(Modifier.padding(12.dp)) {
                Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(Color.Gray.copy(alpha = 0.3f)))
                Spacer(Modifier.width(12.dp))
                Column {
                    Box(modifier = Modifier.fillMaxWidth(0.7f).height(16.dp).background(Color.Gray.copy(alpha = 0.3f)))
                    Spacer(Modifier.height(4.dp))
                    Box(modifier = Modifier.fillMaxWidth(0.4f).height(12.dp).background(Color.Gray.copy(alpha = 0.3f)))
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun YouTubeVideoCard(video: YouTubeSearchItem, viewModel: YouTubeViewModel) {
    var showDownloadSheet by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp).clickable {
            val intent = android.content.Intent(context, YouTubePlayerActivity::class.java).apply {
                putExtra("VIDEO_ID", video.id.videoId)
                putExtra("TITLE", video.snippet.title)
                putExtra("CHANNEL", video.snippet.channelTitle)
            }
            context.startActivity(intent)
        },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().height(220.dp)) {
                AsyncImage(
                    model = video.snippet.thumbnails.high.url,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Surface(
                    modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp),
                    shape = RoundedCornerShape(4.dp),
                    color = Color.Black.copy(alpha = 0.8f)
                ) {
                    Text("10:24", color = Color.White, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 4.dp))
                }
            }
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                Surface(shape = CircleShape, modifier = Modifier.size(36.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)) {
                    Icon(Icons.Default.Person, null, modifier = Modifier.padding(8.dp), tint = MaterialTheme.colorScheme.primary)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(video.snippet.title, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurface)
                    Text("${video.snippet.channelTitle} • 1.2M views • ${video.snippet.publishedAt.take(10)}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                }
            }
            
            // Action Buttons
            Row(modifier = Modifier.padding(horizontal = 12.dp).padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = { viewModel.toggleLike(video) }) {
                    Icon(Icons.Default.ThumbUp, null, tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = { /* TODO: Favorite */ }) {
                    Icon(Icons.Default.FavoriteBorder, null, tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = { 
                    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(android.content.Intent.EXTRA_TEXT, "https://www.youtube.com/watch?v=${video.id.videoId}")
                    }
                    context.startActivity(android.content.Intent.createChooser(intent, "Share Video"))
                }) {
                    Icon(Icons.Default.Share, null, tint = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { showDownloadSheet = true }) {
                    Icon(Icons.Default.DownloadForOffline, null, tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
    
    if (showDownloadSheet) {
        DownloadBottomSheet(onDismiss = { showDownloadSheet = false })
    }
}

@Composable
fun MusicTabContent(viewModel: YouTubeViewModel) {
    val uiState by viewModel.musicUiState.collectAsState()
    
    when (val state = uiState) {
        is YouTubeUiState.Idle -> {
            LaunchedEffect(Unit) { viewModel.searchMusic("") }
            MusicSkeleton()
        }
        is YouTubeUiState.Loading -> MusicSkeleton()
        is YouTubeUiState.Success -> {
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(state.videos) { video ->
                    MusicListItem(video)
                }
            }
        }
        is YouTubeUiState.Error -> ErrorView(state.message) { viewModel.searchMusic("") }
    }
}

@Composable
fun MusicSkeleton() {
    Column(modifier = Modifier.shimmer().padding(16.dp)) {
        repeat(5) {
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Box(modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)).background(Color.Gray.copy(alpha = 0.3f)))
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Box(modifier = Modifier.fillMaxWidth(0.6f).height(16.dp).background(Color.Gray.copy(alpha = 0.3f)))
                    Spacer(Modifier.height(4.dp))
                    Box(modifier = Modifier.fillMaxWidth(0.3f).height(12.dp).background(Color.Gray.copy(alpha = 0.3f)))
                }
            }
        }
    }
}

@Composable
fun MusicListItem(video: YouTubeSearchItem) {
    var showDownloadSheet by remember { mutableStateOf(false) }
    val context = LocalContext.current
    
    Row(
        modifier = Modifier.fillMaxWidth().clickable {
            val intent = android.content.Intent(context, YouTubePlayerActivity::class.java).apply {
                putExtra("VIDEO_ID", video.id.videoId)
                putExtra("TITLE", video.snippet.title)
                putExtra("CHANNEL", video.snippet.channelTitle)
            }
            context.startActivity(intent)
        },
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = video.snippet.thumbnails.medium.url,
            contentDescription = null,
            modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(video.snippet.title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurface)
            Text("${video.snippet.channelTitle} • Artist", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        }
        IconButton(onClick = { showDownloadSheet = true }) {
            Icon(Icons.Default.FileDownload, null, tint = MaterialTheme.colorScheme.primary)
        }
    }
    
    if (showDownloadSheet) {
        DownloadBottomSheet(onDismiss = { showDownloadSheet = false })
    }
}

@Composable
fun YouTubeDownloadsTabContent(viewModel: DownloaderViewModel) {
    val downloads by viewModel.downloads.collectAsState()

    if (downloads.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No downloads yet")
        }
    } else {
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(downloads) { download ->
                DownloadItemView(
                    download = download,
                    onPause = { viewModel.pauseDownload(it) },
                    onResume = { viewModel.resumeDownload(it) },
                    onCancel = { viewModel.cancelDownload(it) },
                    onDelete = { viewModel.deleteDownload(it) },
                    onRetry = { viewModel.retryDownload(it) },
                    onRename = { /* TODO */ }
                )
            }
        }
    }
}

@Composable
fun SearchHistoryTab(viewModel: YouTubeViewModel) {
    val history by viewModel.searchHistory.collectAsState()
    
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Search History", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
            TextButton(onClick = { viewModel.clearHistory() }) {
                Text("Clear All", color = MaterialTheme.colorScheme.primary)
            }
        }
        
        LazyColumn {
            items(history) { item ->
                ListItem(
                    headlineContent = { Text(item.query) },
                    leadingContent = { Icon(Icons.Default.History, null) },
                    trailingContent = {
                        IconButton(onClick = { viewModel.deleteSearch(item.query) }) {
                            Icon(Icons.Default.Close, null)
                        }
                    },
                    modifier = Modifier.clickable { viewModel.searchVideos(item.query) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DownloadBottomSheet(onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState()
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(modifier = Modifier.padding(16.dp).padding(bottom = 32.dp)) {
            Text("Download Quality", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            Spacer(modifier = Modifier.height(16.dp))
            
            Text("Video", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            val videoQualities = listOf("1080p", "720p", "480p", "360p", "240p", "144p")
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                videoQualities.forEach { q ->
                    FilterChip(
                        selected = false,
                        onClick = { /* TODO: UI Only */ },
                        label = { Text(q) }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            Text("Audio", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            val audioQualities = listOf("MP3 (320kbps)", "AAC", "M4A")
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                audioQualities.forEach { q ->
                    FilterChip(
                        selected = false,
                        onClick = { /* TODO: UI Only */ },
                        label = { Text(q) }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Download Now")
            }
        }
    }
}

@Composable
fun ErrorView(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.ErrorOutline, null, modifier = Modifier.size(80.dp), tint = Color.Red)
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.titleMedium,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground
        )
        if (message.contains("Invalid API Key")) {
            Text(
                "Please add YOUTUBE_API_KEY to your local.properties file.",
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onRetry,
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Retry Now")
        }
    }
}

@Composable
fun DownloadItemView(
    download: DownloadEntity,
    onPause: (String) -> Unit,
    onResume: (String) -> Unit,
    onCancel: (String) -> Unit,
    onDelete: (String) -> Unit,
    onRetry: (String) -> Unit,
    onRename: (DownloadEntity) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(4.dp), modifier = Modifier.size(40.dp), color = Color.LightGray) {}
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(download.fileName, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${(download.progress * 100).toInt()}% • ${formatSize(download.totalSize)}", fontSize = 10.sp)
                }
                IconButton(onClick = { if (download.status == DownloadStatus.DOWNLOADING) onPause(download.id) else onResume(download.id) }) {
                    Icon(if (download.status == DownloadStatus.DOWNLOADING) Icons.Default.PauseCircleFilled else Icons.Default.PlayCircleFilled, null)
                }
            }
            if (download.status == DownloadStatus.DOWNLOADING) {
                LinearProgressIndicator(
                    progress = { download.progress },
                    modifier = Modifier.fillMaxWidth().height(4.dp).padding(top = 8.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
