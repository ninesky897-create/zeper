package com.zeper.player.video.ui

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.UnstableApi
import coil.compose.AsyncImage
import androidx.activity.compose.BackHandler
import com.zeper.player.core.data.*
import com.zeper.player.core.ui.FileDetailsDialog
import com.zeper.player.core.ui.MediaOptionsMenu
import com.zeper.player.core.ui.SectionHeader
import com.zeper.player.core.ui.SortDialog
import com.zeper.player.core.ui.theme.*
import com.zeper.player.downloader.ui.DownloaderScreen
import com.zeper.player.downloader.ui.OnlineSearchScreen
import kotlinx.coroutines.launch

@OptIn(androidx.media3.common.util.UnstableApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun VideoListScreen(
    homeViewModel: HomeViewModel = viewModel(),
) {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager(context) }
    val scope = rememberCoroutineScope()
    
    val selectedCategory by homeViewModel.selectedCategory.collectAsState()
    val historyItems by homeViewModel.historyItems.collectAsState()
    val folders by homeViewModel.folders.collectAsState()
    val allVideos by homeViewModel.allVideos.collectAsState()
    val isLoading by homeViewModel.isLoading.collectAsState()
    val sortOrder by homeViewModel.sortOrder.collectAsState()
    val favoriteUris by homeViewModel.favoriteUris.collectAsState()
    val favoriteVideos by homeViewModel.favoriteVideos.collectAsState()
    val isShowingAllFiles by homeViewModel.isShowingAllFiles.collectAsState()
    val isGridView by prefs.isGridView.collectAsState(initial = false)

    var detailsTarget by remember { mutableStateOf<MediaFile?>(null) }

    BackHandler(enabled = selectedCategory != "All Videos") {
        homeViewModel.selectCategory("All Videos")
        Toast.makeText(context, "Returning to All Videos", Toast.LENGTH_SHORT).show()
    }

    PullToRefreshBox(
        isRefreshing = isLoading,
        onRefresh = { homeViewModel.refreshData() },
        modifier = Modifier.fillMaxSize()
    ) {
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Category Chips as the Header
                val isSubScreen = selectedCategory == "Internet" || selectedCategory == "FM" || selectedCategory == "ZPR Share"
                
                if (!isSubScreen) {
                    Box(
                        modifier = Modifier
                            .statusBarsPadding()
                            .padding(top = 8.dp)
                    ) {
                        CategoryChipsRow(
                            categories = homeViewModel.categories.map { 
                                if (it == "All Videos" && isShowingAllFiles) "All Files" else it 
                            },
                            selectedCategory = if (isShowingAllFiles && selectedCategory == "All Files") "All Files" else selectedCategory,
                            onCategorySelected = { 
                                homeViewModel.selectCategory(it)
                            }
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .then(if (isSubScreen) Modifier.statusBarsPadding() else Modifier)
                ) {
                    when (selectedCategory) {
                        "FM" -> GeoFMScreen(onBack = { homeViewModel.selectCategory("All Videos") })
                        "Internet" -> OnlineSearchScreen(onBack = { homeViewModel.selectCategory("All Videos") })
                        //"Privacy" -> com.zeper.player.core.ui.PrivacyScreen(onBack = { homeViewModel.selectCategory("All Videos") })
                        "ZPR Share" -> com.zeper.player.core.ui.ZprScreen(onBack = { homeViewModel.selectCategory("All Videos") })
                        else -> {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(bottom = 32.dp)
                            ) {
                                // 2. History
                                if (selectedCategory == "All Videos" || selectedCategory == "Recent" || selectedCategory == "All Files") {
                                    item {
                                        HistorySection(
                                            historyItems = historyItems,
                                            onVideoClick = { video ->
                                                val intent = Intent(context, VideoPlayerActivity::class.java).apply {
                                                    putExtra("video_uri", video.path)
                                                    putExtra("video_title", video.name)
                                                }
                                                context.startActivity(intent)
                                            }
                                        )
                                    }
                                }

                                // 3. Folders or Files
                                if (selectedCategory == "All Videos" || selectedCategory == "All Files") {
                                    if (isShowingAllFiles) {
                                        item {
                                            FolderSectionHeader(
                                                folderCount = allVideos.size,
                                                label = "ALL FILES",
                                                isGridView = isGridView,
                                                onToggleView = { scope.launch { prefs.setGridView(!isGridView) } },
                                                currentSortOrder = sortOrder,
                                                onSortSelected = { homeViewModel.setSortOrder(it) }
                                            )
                                        }

                                        if (isGridView) {
                                            item {
                                                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                                                    allVideos.chunked(2).forEach { rowVideos ->
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                                        ) {
                                                            rowVideos.forEach { video ->
                                                                Box(modifier = Modifier.weight(1f)) {
                                                                    VideoGridItem(
                                                                        video = video,
                                                                        isFavorite = favoriteUris.contains(video.path),
                                                                        onFavoriteClick = { homeViewModel.toggleFavorite(video) },
                                                                        onDeleteClick = { homeViewModel.deleteFile(context, video) },
                                                                        onVaultClick = { homeViewModel.moveToVault(context, video) },
                                                                        onShareClick = { /* TODO */ },
                                                                        onDetailsClick = { detailsTarget = video },
                                                                        onClick = {
                                                                            val intent = Intent(context, VideoPlayerActivity::class.java).apply {
                                                                                putExtra("video_uri", video.path)
                                                                                putExtra("video_title", video.name)
                                                                            }
                                                                            context.startActivity(intent)
                                                                        }
                                                                    )
                                                                }
                                                            }
                                                            if (rowVideos.size == 1) {
                                                                Spacer(modifier = Modifier.weight(1f))
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        } else {
                                            items(allVideos) { video ->
                                                VideoItem(
                                                    video = video,
                                                    isFavorite = favoriteUris.contains(video.path),
                                                    onFavoriteClick = { homeViewModel.toggleFavorite(video) },
                                                    onDeleteClick = { homeViewModel.deleteFile(context, video) },
                                                    onVaultClick = { homeViewModel.moveToVault(context, video) },
                                                    onShareClick = { /* TODO */ },
                                                    onDetailsClick = { detailsTarget = video },
                                                    onClick = {
                                                        val intent = Intent(context, VideoPlayerActivity::class.java).apply {
                                                            putExtra("video_uri", video.path)
                                                            putExtra("video_title", video.name)
                                                        }
                                                        context.startActivity(intent)
                                                    }
                                                )
                                            }
                                        }
                                    } else {
                                        item {
                                            FolderSectionHeader(
                                                folderCount = folders.size,
                                                label = "FOLDERS",
                                                isGridView = false,
                                                onToggleView = {},
                                                currentSortOrder = sortOrder,
                                                onSortSelected = { homeViewModel.setSortOrder(it) }
                                            )
                                        }

                                        items(folders) { folder ->
                                            FolderListItem(
                                                folder = folder,
                                                onClick = {
                                                    val intent = Intent(context, VideoListActivity::class.java).apply {
                                                        putExtra("folder_name", folder.name)
                                                    }
                                                    context.startActivity(intent)
                                                }
                                            )
                                        }
                                    }
                                }

                                // 4. Favorites & Privacy
                                if (selectedCategory == "Favorites") {
                                    if (favoriteVideos.isEmpty()) {
                                        item {
                                            EmptyStateCard(
                                                icon = Icons.Default.FavoriteBorder,
                                                title = "No Favorites Yet",
                                                subtitle = "Add videos to favorites using the menu"
                                            )
                                        }
                                    } else {
                                        item {
                                            FolderSectionHeader(
                                                folderCount = favoriteVideos.size,
                                                label = "FAVORITES",
                                                isGridView = isGridView,
                                                onToggleView = { scope.launch { prefs.setGridView(!isGridView) } },
                                                currentSortOrder = sortOrder,
                                                onSortSelected = { homeViewModel.setSortOrder(it) }
                                            )
                                        }

                                        if (isGridView) {
                                            item {
                                                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                                                    favoriteVideos.chunked(2).forEach { rowVideos ->
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                                        ) {
                                                            rowVideos.forEach { video ->
                                                                Box(modifier = Modifier.weight(1f)) {
                                                                    VideoGridItem(
                                                                        video = video,
                                                                        isFavorite = true,
                                                                        onFavoriteClick = { homeViewModel.toggleFavorite(video) },
                                                                        onDeleteClick = { homeViewModel.deleteFile(context, video) },
                                                                        onVaultClick = { homeViewModel.moveToVault(context, video) },
                                                                        onShareClick = { /* TODO */ },
                                                                        onDetailsClick = { detailsTarget = video },
                                                                        onClick = {
                                                                            val intent = Intent(context, VideoPlayerActivity::class.java).apply {
                                                                                putExtra("video_uri", video.path)
                                                                                putExtra("video_title", video.name)
                                                                            }
                                                                            context.startActivity(intent)
                                                                        }
                                                                    )
                                                                }
                                                            }
                                                            if (rowVideos.size == 1) {
                                                                Spacer(modifier = Modifier.weight(1f))
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        } else {
                                            items(favoriteVideos) { video ->
                                                VideoItem(
                                                    video = video,
                                                    isFavorite = true,
                                                    onFavoriteClick = { homeViewModel.toggleFavorite(video) },
                                                    onDeleteClick = { homeViewModel.deleteFile(context, video) },
                                                    onVaultClick = { homeViewModel.moveToVault(context, video) },
                                                    onShareClick = { /* TODO */ },
                                                    onDetailsClick = { detailsTarget = video },
                                                    onClick = {
                                                        val intent = Intent(context, VideoPlayerActivity::class.java).apply {
                                                            putExtra("video_uri", video.path)
                                                            putExtra("video_title", video.name)
                                                        }
                                                        context.startActivity(intent)
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                                if (selectedCategory == "Privacy") {
                                    item {
                                        EmptyStateCard(
                                            icon = Icons.Default.Lock,
                                            title = "Privacy Vault",
                                            subtitle = "Lock folders to keep your videos private"
                                        )
                                    }
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

@Composable
private fun FolderSectionHeader(
    folderCount: Int,
    label: String,
    isGridView: Boolean,
    onToggleView: () -> Unit,
    currentSortOrder: String,
    onSortSelected: (String) -> Unit
) {
    var showSortDialog by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (folderCount > 0) "$folderCount $label" else label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 1.sp
        )
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (label == "ALL FILES") {
                IconButton(onClick = onToggleView) {
                    Icon(
                        if (isGridView) Icons.AutoMirrored.Filled.ViewList else Icons.Default.GridView,
                        contentDescription = "Toggle View",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Box {
                IconButton(onClick = { showSortDialog = true }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.AutoMirrored.Filled.Sort, "Sort", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                
                if (showSortDialog) {
                    SortDialog(
                        currentSortOrder = currentSortOrder,
                        onDismiss = { showSortDialog = false },
                        onConfirm = { 
                            showSortDialog = false
                            onSortSelected(it)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryChipsRow(
    categories: List<String>,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(categories) { category ->
            CategoryChip(
                label = category,
                icon = getCategoryIcon(category),
                isSelected = category == selectedCategory,
                onClick = { onCategorySelected(category) }
            )
        }
    }
}

@Composable
private fun CategoryChip(
    label: String,
    icon: ImageVector?,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val displayLabel = when (label) {
        "All Videos" -> stringResource(com.zeper.player.R.string.category_all_videos)
        "All Files" -> stringResource(com.zeper.player.R.string.category_all_files)
        "Internet" -> stringResource(com.zeper.player.R.string.category_online_search)
        "FM" -> "FM"
        "Favorites" -> stringResource(com.zeper.player.R.string.category_favorites)
        "Privacy" -> stringResource(com.zeper.player.R.string.category_privacy)
        "Manage scan list" -> stringResource(com.zeper.player.R.string.category_manage_scan_list)
        else -> label
    }

    val isIconOnly = label !in listOf("All Videos", "All Files")
    val isYouTube = label == "Internet"

    val bgColor by animateColorAsState(
        if (isSelected) {
            if (isYouTube) Color(0xFFFF0000) else MaterialTheme.colorScheme.primary 
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        }, 
        label = "chipBg"
    )
    val contentColor by animateColorAsState(if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface, label = "chipContent")
    val scale by animateFloatAsState(if (isSelected) 1.1f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "chipScale")

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = bgColor,
        border = if (!isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)) else null,
        modifier = Modifier
            .height(52.dp)
            .scale(scale)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = if (isIconOnly) 18.dp else 22.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon, 
                    contentDescription = null, 
                    modifier = Modifier.size(if (isYouTube) 26.dp else 22.dp), 
                    tint = contentColor
                )
            }
            if (!isIconOnly) {
                if (icon != null) Spacer(modifier = Modifier.width(8.dp))
                Text(displayLabel, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.ExtraBold, color = contentColor)
            }
        }
    }
}

private fun getCategoryIcon(category: String): ImageVector? {
    return when (category) {
        "All Videos", "All Files" -> null
        "Internet" -> Icons.Default.Public 
        "FM" -> Icons.Default.Radio
        "ZPR Share" -> Icons.Default.Share
        "Privacy" -> Icons.Default.Security
        "Favorites" -> Icons.Default.Favorite
        "Recent" -> Icons.Default.History
        "Manage scan list" -> Icons.Default.ManageSearch
        else -> Icons.Default.VideoLibrary
    }
}

@Composable
private fun HistorySection(historyItems: List<DummyHistoryItem>, onVideoClick: (DummyVideo) -> Unit) {
    if (historyItems.isEmpty()) return
    Column {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("History", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(bottom = 16.dp)) {
            items(historyItems) { historyItem ->
                HistoryVideoCard(historyItem = historyItem, onClick = { onVideoClick(historyItem.video) })
            }
        }
    }
}

@Composable
private fun HistoryVideoCard(historyItem: DummyHistoryItem, onClick: () -> Unit) {
    val video = historyItem.video
    Column(modifier = Modifier.width(180.dp).clickable { onClick() }) {
        Box(modifier = Modifier.fillMaxWidth().height(105.dp).clip(RoundedCornerShape(12.dp)).shadow(4.dp, RoundedCornerShape(12.dp))) {
            AsyncImage(model = video.path, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
            Icon(Icons.Default.PlayCircleFilled, null, modifier = Modifier.size(36.dp).align(Alignment.Center), tint = Color.White.copy(alpha = 0.7f))
            Box(modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp).background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                Text(formatDuration(video.durationMs), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            if (historyItem.progressPercent > 0f) {
                LinearProgressIndicator(progress = { historyItem.progressPercent }, modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(3.dp), color = MaterialTheme.colorScheme.primary, trackColor = Color.Black.copy(alpha = 0.3f))
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = video.name.removeSuffix(".mp4"), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(horizontal = 2.dp))
    }
}

@Composable
private fun FolderListItem(folder: DummyFolder, onClick: () -> Unit) {
    ListItem(
        leadingContent = {
            Box(modifier = Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                Icon(if (folder.isExternal) Icons.Default.SdCard else Icons.Default.Folder, null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
            }
        },
        headlineContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(folder.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.width(8.dp))
                if (folder.subtitle != null) {
                    Text(
                        text = folder.subtitle, 
                        style = MaterialTheme.typography.labelSmall, 
                        color = Color(0xFF4CAF50), // Green
                        modifier = Modifier.padding(start = 4.dp)
                    )
                } else {
                    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)) {
                        Text(text = folder.videoCount.toString(), modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        },
        trailingContent = { Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
        modifier = Modifier.clickable { onClick() }
    )
}

@Composable
private fun EmptyStateCard(icon: ImageVector, title: String, subtitle: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(48.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(modifier = Modifier.size(72.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, null, modifier = Modifier.size(36.dp), tint = MaterialTheme.colorScheme.primary)
        }
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 16.dp))
    }
}
