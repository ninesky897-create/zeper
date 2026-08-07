package com.zeper.player.downloader.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.zIndex
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.zeper.player.core.auth.GoogleAuthManager
import com.zeper.player.downloader.ui.BrowserTabContent
import com.zeper.player.R
import androidx.activity.compose.BackHandler
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun OnlineSearchScreen(
    initialUrl: String? = null,
    onBack: () -> Unit,
    viewModel: DownloaderViewModel = viewModel(),
    searchViewModel: OnlineSearchViewModel = viewModel()
) {
    val context = LocalContext.current
    val authManager = remember { GoogleAuthManager(context) }
    val userAccount by authManager.userAccount.collectAsState()

    LaunchedEffect(userAccount) {
        searchViewModel.setUserAccount(userAccount)
    }
    
    val tabs = listOf(
        stringResource(R.string.tab_search),
        stringResource(R.string.tab_youtube),
        stringResource(R.string.tab_music),
        stringResource(R.string.tab_more)
    )

    val pagerState = rememberPagerState(
        initialPage = if (initialUrl != null) 1 else 0,
        pageCount = { tabs.size }
    )
    val scope = rememberCoroutineScope()

    BackHandler(enabled = pagerState.currentPage != 0) {
        scope.launch { pagerState.animateScrollToPage(0) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
    ) {
        // --- Tab Row (Fixed) ---
        val tabRowHeight = 48.dp
        Box(
            modifier = Modifier
                .height(tabRowHeight)
                .fillMaxWidth()
                .background(Color.Black)
                .zIndex(1f)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                
                ScrollableTabRow(
                    selectedTabIndex = pagerState.currentPage,
                    containerColor = Color.Black,
                    contentColor = Color(0xFFF4B400),
                    edgePadding = 8.dp,
                    modifier = Modifier.weight(1f),
                    divider = {},
                    indicator = { tabPositions ->
                        if (pagerState.currentPage < tabPositions.size) {
                            TabRowDefaults.SecondaryIndicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                                color = Color(0xFFF4B400)
                            )
                        }
                    }
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = pagerState.currentPage == index,
                            onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                            text = {
                                Text(
                                    text = title,
                                    fontWeight = if (pagerState.currentPage == index) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 14.sp
                                )
                            }
                        )
                    }
                }
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.Top
        ) { page ->
            when (page) {
                0 -> HomeTabContent(
                    searchViewModel = searchViewModel,
                    onSiteClick = { url ->
                        // Navigate to specific browser tab or update URL
                        if (url.contains("music.youtube.com")) {
                            scope.launch { pagerState.animateScrollToPage(2) }
                        } else {
                            scope.launch { pagerState.animateScrollToPage(1) }
                        }
                    },
                    onSearch = { query -> 
                        searchViewModel.search(query)
                        scope.launch { pagerState.animateScrollToPage(1) }
                    }
                )
                1 -> YouTubeTabContent(viewModel, searchViewModel)
                2 -> MusicTabContent(viewModel, searchViewModel)
                3 -> OnlineDownloadsTabContent(viewModel)
                4 -> MoreTabContent(authManager)
            }
        }
    }
}

@Composable
fun HomeTabContent(
    searchViewModel: OnlineSearchViewModel,
    onSiteClick: (String) -> Unit, 
    onSearch: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val trendingResults by searchViewModel.videoResults.collectAsState()
    
    val sites = listOf(
        SiteItem("YouTube", "https://m.youtube.com", Icons.Default.PlayCircleFilled, Color(0xFFFF0000)),
        SiteItem("Facebook", "https://m.facebook.com", Icons.Default.Facebook, Color(0xFF1877F2)),
        SiteItem("Instagram", "https://www.instagram.com", Icons.Default.CameraAlt, Color(0xFFE4405F)),
        SiteItem("TikTok", "https://www.tiktok.com", Icons.Default.MusicNote, Color(0xFF000000)),
        SiteItem("Music", "https://music.youtube.com", Icons.Default.Headphones, Color(0xFFF4B400)),
        SiteItem("Radio", "https://radio.garden/visit/rajshahi/f09uPaXs", Icons.Default.Radio, Color(0xFF00FF00)),
        SiteItem("Twitter", "https://mobile.twitter.com", Icons.Default.Language, Color(0xFF1DA1F2)),
        SiteItem("DailyMotion", "https://www.dailymotion.com", Icons.Default.Tv, Color(0xFF0062FF)),
        SiteItem("Vimeo", "https://vimeo.com", Icons.Default.Movie, Color(0xFF1AB7EA))
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- Bold Search Bar ---
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(28.dp),
            color = Color(0xFF1E1E1E),
            border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFF4B400))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Search, null, tint = Color.Gray)
                Spacer(Modifier.width(12.dp))
                Box(modifier = Modifier.weight(1f)) {
                    if (searchQuery.isEmpty()) {
                        Text("Search or paste URL", color = Color.Gray)
                    }
                    androidx.compose.foundation.text.BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 16.sp),
                        modifier = Modifier.fillMaxWidth(),
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(Color(0xFFF4B400)),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            imeAction = androidx.compose.ui.text.input.ImeAction.Search
                        ),
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                            onSearch = { onSearch(searchQuery) }
                        )
                    )
                }
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, null, tint = Color.Gray)
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        Text(
            "Popular Sites",
            modifier = Modifier.fillMaxWidth(),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )

        Spacer(Modifier.height(16.dp))

        // --- Site Grid ---
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(sites) { site ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { onSiteClick(site.url) }
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(site.color.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = site.icon,
                            contentDescription = site.name,
                            tint = site.color,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = site.name,
                        fontSize = 12.sp,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        
        Spacer(Modifier.height(32.dp))
        
        // Trending Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "Hot Trending Videos",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            Text(
                "See All",
                color = Color(0xFFF4B400),
                fontSize = 14.sp,
                modifier = Modifier.clickable { /* See all */ }
            )
        }

        Spacer(Modifier.height(16.dp))

        // Horizontal Trending List
        if (trendingResults.isEmpty()) {
            // Placeholder
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                border = BorderStroke(1.dp, Color(0xFFF4B400).copy(alpha = 0.3f))
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.AutoMirrored.Filled.TrendingUp, 
                            null, 
                            tint = Color(0xFFF4B400), 
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Discover Trending", 
                            color = Color.White, 
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        } else {
            androidx.compose.foundation.lazy.LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(trendingResults.take(5)) { video ->
                    Card(
                        modifier = Modifier
                            .width(200.dp)
                            .clickable { onSiteClick(video.url) },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
                    ) {
                        Column {
                            Box(modifier = Modifier.height(112.dp).fillMaxWidth()) {
                                AsyncImage(
                                    model = video.thumbnail,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                Surface(
                                    modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp),
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color.Black.copy(alpha = 0.8f)
                                ) {
                                    Text(video.duration, color = Color.White, fontSize = 10.sp, modifier = Modifier.padding(2.dp))
                                }
                            }
                            Text(
                                video.title,
                                color = Color.White,
                                fontSize = 12.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

data class SiteItem(val name: String, val url: String, val icon: ImageVector, val color: Color)

@Composable
fun YouTubeTabContent(viewModel: DownloaderViewModel, searchViewModel: OnlineSearchViewModel) {
    val results by searchViewModel.videoResults.collectAsState()
    val isLoading by searchViewModel.isLoading.collectAsState()
    var selectedVideoUrl by remember { mutableStateOf<String?>(null) }
    var showQualitySheet by remember { mutableStateOf<String?>(null) }

    if (selectedVideoUrl != null) {
        Box(modifier = Modifier.fillMaxSize()) {
            BrowserTabContent(
                url = selectedVideoUrl!!,
                onUrlDetected = { url -> showQualitySheet = url }
            )
            
            // Back to Feed Button
            IconButton(
                onClick = { selectedVideoUrl = null },
                modifier = Modifier
                    .padding(12.dp)
                    .align(Alignment.TopStart)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
            }
        }
    } else {
        Box(modifier = Modifier.fillMaxSize()) {
            if (isLoading && results.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFFFF0000))
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    item {
                        Text(
                            "Trending Videos",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                    items(results) { video ->
                        YouTubeVideoCard(
                            video = video,
                            onClick = { selectedVideoUrl = video.url }
                        )
                        
                        // Add a sponsored item after every 3 videos
                        if (results.indexOf(video) % 3 == 2) {
                            SponsoredAdCard()
                        }
                    }
                }
            }
        }
    }

    if (showQualitySheet != null) {
        ResolutionSelectionSheet(
            url = showQualitySheet!!,
            onDismiss = { showQualitySheet = null },
            onOptionSelected = { isVideo, quality ->
                viewModel.startDownload(showQualitySheet!!, isVideo, quality)
                showQualitySheet = null
            }
        )
    }
}

@Composable
fun YouTubeVideoCard(video: YouTubeSearchResult, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(bottom = 16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16 / 9f)
                .background(Color.DarkGray)
        ) {
            AsyncImage(
                model = video.thumbnail,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            
            // Duration Overlay
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp),
                shape = RoundedCornerShape(4.dp),
                color = Color.Black.copy(alpha = 0.8f)
            ) {
                Text(
                    text = video.duration,
                    color = Color.White,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Channel Avatar Placeholder
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.Gray.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, null, tint = Color.Gray, modifier = Modifier.size(24.dp))
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = video.title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = video.uploader,
                        color = Color.Gray,
                        fontSize = 13.sp,
                        maxLines = 1
                    )
                    Text(" • ", color = Color.Gray)
                    Text(
                        text = "${video.viewCount} views",
                        color = Color.Gray,
                        fontSize = 13.sp,
                        maxLines = 1
                    )
                }
            }
            
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = { onClick() }) {
                    Icon(Icons.Default.Download, null, tint = Color(0xFFF4B400))
                }
                IconButton(onClick = { /* More actions */ }) {
                    Icon(Icons.Default.MoreVert, null, tint = Color.White)
                }
            }
        }
    }
}

@Composable
fun MusicTabContent(viewModel: DownloaderViewModel, searchViewModel: OnlineSearchViewModel) {
    val results by searchViewModel.musicResults.collectAsState()
    val isLoading by searchViewModel.isLoading.collectAsState()
    var selectedMusicUrl by remember { mutableStateOf<String?>(null) }
    var showQualitySheet by remember { mutableStateOf<String?>(null) }

    if (selectedMusicUrl != null) {
        Box(modifier = Modifier.fillMaxSize()) {
            BrowserTabContent(
                url = selectedMusicUrl!!,
                onUrlDetected = { url -> showQualitySheet = url }
            )
            IconButton(
                onClick = { selectedMusicUrl = null },
                modifier = Modifier
                    .padding(12.dp)
                    .align(Alignment.TopStart)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
            }
        }
    } else {
        Box(modifier = Modifier.fillMaxSize()) {
            if (isLoading && results.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFFF4B400))
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item(span = { GridItemSpan(2) }) {
                        Text(
                            "Top Music Hits",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }
                    items(results) { music ->
                        MusicCard(
                            result = music,
                            onDownloadClick = { /* Handled via click to browser */ },
                            modifier = Modifier.clickable { selectedMusicUrl = music.url }
                        )
                    }
                }
            }
        }
    }

    if (showQualitySheet != null) {
        ResolutionSelectionSheet(
            url = showQualitySheet!!,
            onDismiss = { showQualitySheet = null },
            onOptionSelected = { isVideo, quality ->
                viewModel.startDownload(showQualitySheet!!, isVideo, quality)
                showQualitySheet = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResolutionSelectionSheet(
    url: String,
    onDismiss: () -> Unit,
    onOptionSelected: (isVideo: Boolean, resolution: String) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            // Video Title / URL Info
            Text(
                text = "Zeper Video Downloader",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = url,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(20.dp))

            // Audio Section
            SectionHeader(title = "Music", icon = Icons.Default.MusicNote)
            Spacer(modifier = Modifier.height(12.dp))
            
            val audioOptions = listOf(
                "MP3 70K" to "4.2 MB",
                "MP3 128K" to "8.5 MB",
                "M4A 128K" to "7.9 MB",
                "MP3 160K" to "10.1 MB"
            )

            Row(modifier = Modifier.fillMaxWidth()) {
                audioOptions.take(3).forEach { (label, size) ->
                    QualityGridItem(
                        label = label,
                        size = size,
                        onClick = { onOptionSelected(false, label) },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                QualityGridItem(
                    label = audioOptions[3].first,
                    size = audioOptions[3].second,
                    onClick = { onOptionSelected(false, audioOptions[3].first) },
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.weight(2f))
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Video Section
            SectionHeader(title = "Video", icon = Icons.Default.VideoLibrary)
            Spacer(modifier = Modifier.height(12.dp))

            val videoOptions = listOf(
                "144p" to "12.4 MB",
                "240p" to "28.1 MB",
                "360p" to "45.6 MB",
                "480p" to "82.3 MB",
                "720p HD" to "156.4 MB",
                "1080p HD" to "312.8 MB",
                "2K HD" to "850.2 MB",
                "4K HD" to "1.8 GB"
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                videoOptions.chunked(3).forEach { row ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        row.forEachIndexed { index, (label, size) ->
                            QualityGridItem(
                                label = label,
                                size = size,
                                onClick = { onOptionSelected(true, label) },
                                modifier = Modifier.weight(1f)
                            )
                            if (index < row.size - 1) Spacer(Modifier.width(8.dp))
                        }
                        // Fill remaining space in the row
                        repeat(3 - row.size) {
                            Spacer(Modifier.weight(1f))
                            Spacer(Modifier.width(8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            icon,
            contentDescription = null,
            tint = Color(0xFFE91E63),
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun QualityGridItem(
    label: String,
    size: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(0.5.dp, Color.Gray.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = size,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun EmptyDownloadsState() {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.CloudDownload,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "No Downloads Yet",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            "Paste a link above to start downloading your favorite videos and music.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
fun MusicCard(result: YouTubeSearchResult, modifier: Modifier = Modifier, onDownloadClick: () -> Unit) {
    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.DarkGray)
        ) {
            AsyncImage(
                model = result.thumbnail,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop
            )
            IconButton(modifier = Modifier.align(Alignment.BottomEnd), onClick = onDownloadClick) {
                Icon(Icons.Default.Download, null, tint = Color(0xFFF4B400))
            }
        }
        Text(result.title, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(result.uploader, color = Color.Gray, fontSize = 11.sp, maxLines = 1)
    }
}

@Composable
fun OnlineDownloadsTabContent(viewModel: DownloaderViewModel) {
    val downloads by viewModel.downloads.collectAsState()

    if (downloads.isEmpty()) {
        EmptyDownloadsState()
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 32.dp)) {
            items(downloads, key = { it.id }) { download ->
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
fun MoreTabContent(authManager: GoogleAuthManager) {
    val userAccount by authManager.userAccount.collectAsState()
    val context = LocalContext.current
    
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        authManager.handleSignInResult(task)
    }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp).padding(bottom = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF1E1E1E)
        ) {
            Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                if (userAccount == null) {
                    Icon(Icons.Default.AccountCircle, null, modifier = Modifier.size(64.dp), tint = Color.Gray)
                    Text("Guest Mode Active", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(top = 12.dp))
                    Text(
                        "You can search and download any video without a Google account. It's fully functional!",
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Sign in only if you want to sync your YouTube playlists and subscriptions directly into Zeper.",
                        color = Color(0xFFF4B400).copy(alpha = 0.8f),
                        textAlign = TextAlign.Center,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(20.dp))
                    Button(
                        onClick = { launcher.launch(authManager.getSignInIntent()) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF4B400)),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Connect Gmail for Sync", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                } else {
                    AsyncImage(
                        model = userAccount?.photoUrl,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp).clip(CircleShape),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                    Text(userAccount?.displayName ?: "User", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(top = 12.dp))
                    Text(userAccount?.email ?: "", color = Color.Gray, fontSize = 13.sp)
                    Spacer(Modifier.height(20.dp))
                    OutlinedButton(
                        onClick = { authManager.signOut { } },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF4B400))
                    ) {
                        Text("Sign Out", color = Color(0xFFF4B400))
                    }
                }
            }
        }
        
        Spacer(Modifier.height(32.dp))
        
        ListItem(
            headlineContent = { Text(stringResource(R.string.menu_settings), color = Color.White) },
            leadingContent = { Icon(Icons.Default.Settings, null, tint = Color.Gray) },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )
        ListItem(
            headlineContent = { Text("ডাউনলোড পাথ (Download Path)", color = Color.White) },
            leadingContent = { Icon(Icons.Default.Folder, null, tint = Color.Gray) },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )
    }
}

@Composable
fun SponsoredAdCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = Color(0xFFF4B400),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        "Ad",
                        color = Color.Black,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    "Try Zeper Premium for No Ads",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Enjoy unlimited high-speed downloads and background playback with our Premium subscription.",
                color = Color.Gray,
                fontSize = 12.sp
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = { /* Upgrade logic */ },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF4B400)),
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text("Upgrade Now", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}
