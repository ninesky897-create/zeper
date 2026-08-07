package com.zeper.player.video.ui

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaController
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionToken
import android.content.ComponentName
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.zeper.player.video.data.VideoPlaybackService
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.os.Environment
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.zeper.player.core.data.MediaFile
import com.zeper.player.core.data.PreferencesManager
import com.zeper.player.core.ui.theme.ZeperTheme
import com.zeper.player.video.ui.VideoViewModel
import com.zeper.player.video.ui.formatDuration
import com.zeper.player.video.ui.formatSize
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.abs

@UnstableApi
@OptIn(ExperimentalMaterial3Api::class)
class VideoPlayerActivity : ComponentActivity() {
    private var player: ExoPlayer? = null


    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        val videoUri = intent.getStringExtra("video_uri") ?: intent.data?.toString() ?: ""
        val videoTitle = intent.getStringExtra("video_title") ?: getFileNameFromUri(videoUri) ?: "Video"

        loadVideoContent(videoUri, videoTitle)
    }

    private fun loadVideoContent(videoUri: String, videoTitle: String) {
        setContent {
            val prefs = remember { PreferencesManager(this) }
            val themeMode by prefs.themeMode.collectAsState(initial = "auto")
            val darkTheme = when (themeMode) {
                "light" -> false
                "dark" -> true
                else -> androidx.compose.foundation.isSystemInDarkTheme()
            }

            ZeperTheme(darkTheme = darkTheme, prefs = prefs) {
                PlayerScreen(videoUri, videoTitle)
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val videoUri = intent.getStringExtra("video_uri") ?: intent.data?.toString() ?: ""
        val videoTitle = intent.getStringExtra("video_title") ?: getFileNameFromUri(videoUri) ?: "Video"
        
        if (videoUri.isNotEmpty()) {
            loadVideoContent(videoUri, videoTitle)
        }
    }

    private fun getFileNameFromUri(uriString: String): String? {
        return try {
            val uri = android.net.Uri.parse(uriString)
            val path = uri.path
            path?.substring(path.lastIndexOf('/') + 1)
        } catch (e: Exception) {
            null
        }
    }

    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    @OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
    @Composable
    fun PlayerScreen(initialUri: String, initialTitle: String) {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val prefs = remember { PreferencesManager(context) }
        val videoViewModel: VideoViewModel = viewModel()
        val allVideos by videoViewModel.videos.collectAsState()

        var currentUri by remember { mutableStateOf(initialUri) }
        var currentTitle by remember { mutableStateOf(initialTitle) }

        var isLocked by remember { mutableStateOf(false) }
        var isAudioOnly by remember { mutableStateOf(false) }
        var playbackSpeed by remember { mutableStateOf(1.0f) }
        val autoPlayEnabled by prefs.autoPlayEnabled.collectAsState(initial = true)
        var showSpeedMenu by remember { mutableStateOf(false) }
        var showSettingsMenu by remember { mutableStateOf(false) }

        // Controls state
        var showControls by remember { mutableStateOf(true) }
        var isPlaying by remember { mutableStateOf(true) }
        var isFullscreen by remember { mutableStateOf(false) }
        var currentPosition by remember { mutableLongStateOf(0L) }
        var duration by remember { mutableLongStateOf(0L) }
        var videoAspectRatio by remember { mutableFloatStateOf(16 / 9f) }

        // Gesture state
        var gestureType by remember { mutableStateOf("none") }
        var gestureText by remember { mutableStateOf("") }
        var doubleTapSide by remember { mutableStateOf("none") } // "left" or "right"

        var dragSumX by remember { mutableFloatStateOf(0f) }
        var dragSumY by remember { mutableFloatStateOf(0f) }
        var initialSeekTime by remember { mutableLongStateOf(0L) }
        var seekOffsetSum by remember { mutableLongStateOf(0L) }
        var hasStartedGesture by remember { mutableStateOf(false) }

        // Playlist & Sorting state
        var selectedPlaylistFolder by remember { mutableStateOf<String?>(null) }
        var showFolderMenu by remember { mutableStateOf(false) }
        var showSortMenuPlaylist by remember { mutableStateOf(false) }
        var videoSortOrder by remember { mutableStateOf("name_asc") } 
        var isPlaylistGridView by remember { mutableStateOf(false) }
        
        // Playback Mode: 0=Seq, 1=RepeatAll, 2=RepeatOne, 3=Shuffle
        var playbackMode by remember { mutableIntStateOf(0) }

        // Minimization/Scale state
        var playerScale by remember { mutableFloatStateOf(1f) }
        var playerOffsetY by remember { mutableFloatStateOf(0f) }
        var isMinimized by remember { mutableStateOf(false) }

        val controllerFuture = remember {
            val sessionToken = SessionToken(context, ComponentName(context, VideoPlaybackService::class.java))
            MediaController.Builder(context, sessionToken).buildAsync()
        }
        var playerInstance by remember { mutableStateOf<Player?>(null) }

        DisposableEffect(controllerFuture) {
            controllerFuture.addListener({
                try {
                    playerInstance = controllerFuture.get()
                    // android.util.Log.d("VideoPlayerActivity", "MediaController connected: $playerInstance")
                } catch (e: Exception) {
                    android.util.Log.e("VideoPlayerActivity", "MediaController connection failed", e)
                    e.printStackTrace()
                }
            }, MoreExecutors.directExecutor())
            onDispose {
                MediaController.releaseFuture(controllerFuture)
            }
        }

        // Filtering videos by same folder
        val currentVideo = remember(currentUri, allVideos) { allVideos.find { it.path == currentUri } }
        
        // Initialize selectedPlaylistFolder if not set
        LaunchedEffect(currentVideo) {
            if (selectedPlaylistFolder == null && currentVideo != null) {
                selectedPlaylistFolder = currentVideo.folder
            }
        }

        val folders = remember(allVideos) {
            allVideos.mapNotNull { it.folder }.distinct().filter { it != "All Videos" && it != "All Files" }.sorted()
        }

        val playlistVideos = remember(selectedPlaylistFolder, allVideos, videoSortOrder) { 
            val baseList = if (selectedPlaylistFolder == null) {
                allVideos
            } else {
                allVideos.filter { it.folder == selectedPlaylistFolder }
            }
            
            when (videoSortOrder) {
                "name_asc" -> baseList.sortedBy { it.name }
                "name_desc" -> baseList.sortedByDescending { it.name }
                "date_asc" -> baseList.sortedBy { it.dateAdded }
                "date_desc" -> baseList.sortedByDescending { it.dateAdded }
                "size_asc" -> baseList.sortedBy { it.size }
                "size_desc" -> baseList.sortedByDescending { it.size }
                else -> baseList
            }
        }

        // Robust playback initialization
        LaunchedEffect(playerInstance, playlistVideos, currentUri) {
            val player = playerInstance ?: return@LaunchedEffect
            if (currentUri.isEmpty()) return@LaunchedEffect
            
            android.util.Log.d("VideoPlayerActivity", "Attempting to play: $currentUri")

            val isCurrentUriInPlayer = (0 until player.mediaItemCount).any {
                player.getMediaItemAt(it).mediaId == currentUri 
            }

            if (!isCurrentUriInPlayer) {
                if (playlistVideos.isNotEmpty()) {
                    val mediaItems = playlistVideos.map { video ->
                        MediaItem.Builder()
                            .setUri(video.contentUri)
                            .setMediaMetadata(MediaMetadata.Builder().setTitle(video.name).build())
                            .setMediaId(video.path)
                            .build()
                    }
                    player.setMediaItems(mediaItems)
                    val targetIndex = playlistVideos.indexOfFirst { it.path == currentUri }
                    if (targetIndex != -1) {
                        player.seekTo(targetIndex, 0)
                    }
                } else if (currentUri.isNotEmpty()) {
                    val targetVideo = allVideos.find { it.path == currentUri }
                    val playbackUri = targetVideo?.contentUri ?: currentUri

                    val mediaItem = MediaItem.Builder()
                        .setUri(playbackUri)
                        .setMediaMetadata(MediaMetadata.Builder().setTitle(currentTitle).build())
                        .setMediaId(currentUri)
                        .build()
                    player.setMediaItem(mediaItem)
                }
                player.prepare()
                player.play() // Force play
            } else {
                val currentIndexInPlayer = player.currentMediaItemIndex
                val targetIndex = (0 until player.mediaItemCount).indexOfFirst {
                    player.getMediaItemAt(it).mediaId == currentUri 
                }
                if (targetIndex != -1 && targetIndex != currentIndexInPlayer) {
                    player.seekTo(targetIndex, 0)
                }
                player.play() // Force play
            }
            
            // Resume playback logic
            val savedPos = videoViewModel.getSavedPosition(currentUri)
            if (savedPos > 0 && player.currentPosition < savedPos) {
                player.seekTo(savedPos)
            }
            
            // Save to history immediately
            videoViewModel.updateProgress(
                currentUri,
                currentTitle,
                player.currentPosition,
                player.duration.coerceAtLeast(0),
                currentVideo?.folder
            )
        }

        val currentPlaylistVideos by rememberUpdatedState(playlistVideos)
        val currentUriState by rememberUpdatedState(currentUri)

        LaunchedEffect(playerInstance) {
            val player = playerInstance ?: return@LaunchedEffect
            val listener = object : Player.Listener {
                override fun onVideoSizeChanged(videoSize: androidx.media3.common.VideoSize) {
                    if (videoSize.width > 0 && videoSize.height > 0) {
                        videoAspectRatio = videoSize.width.toFloat() / videoSize.height.toFloat()
                    }
                }

                override fun onIsPlayingChanged(isPlayingChanged: Boolean) {
                    isPlaying = isPlayingChanged
                    // Save history immediately when play/pause state changes
                    videoViewModel.updateProgress(
                        currentUriState,
                        currentTitle,
                        player.currentPosition,
                        player.duration.coerceAtLeast(0),
                        currentVideo?.folder
                    )
                }

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    mediaItem?.let {
                        val newUri = it.mediaId
                        if (newUri != currentUriState && newUri.isNotEmpty()) {
                            currentUri = newUri
                            currentTitle = it.mediaMetadata.title?.toString() ?: "Video"
                        }
                    }
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_ENDED) {
                        if (playbackMode == 0) { // Sequential
                             playNext(player, currentPlaylistVideos, currentUriState) { nextVideo ->
                                currentUri = nextVideo.path
                                currentTitle = nextVideo.name
                            }
                        }
                        // RepeatOne and RepeatAll are handled by ExoPlayer's repeatMode
                    }
                }

                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    android.util.Log.e("VideoPlayerActivity", "ExoPlayer Error: ${error.message}", error)
                    Toast.makeText(context, "Playback Error: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            }

            player.addListener(listener)
            // No need to remove listener here as the player life is managed by controllerFuture
        }

        player = playerInstance as? ExoPlayer // Keep for legacy if needed, but mostly null now
        val exoPlayer = playerInstance as? ExoPlayer

        // Auto-hide controls
        LaunchedEffect(showControls, isPlaying) {
            if (showControls && isPlaying) {
                delay(3000)
                showControls = false
            }
        }

        LaunchedEffect(playerInstance, currentUri, currentTitle) {
            while (true) {
                playerInstance?.let { p ->
                    if (p.isPlaying) {
                        currentPosition = p.currentPosition
                        duration = p.duration
                        videoViewModel.updateProgress(
                            currentUri,
                            currentTitle,
                            p.currentPosition,
                            p.duration.coerceAtLeast(0),
                            currentVideo?.folder
                        )
                    }
                }
                delay(2000) // Update every 2 seconds to save battery/DB writes
            }
        }

        DisposableEffect(playerInstance, currentUri, currentTitle) {
            onDispose {
                playerInstance?.let { p ->
                    videoViewModel.updateProgress(currentUri, currentTitle, p.currentPosition)
                    if (!isAudioOnly) {
                        try {
                            p.pause()
                        } catch (e: Exception) {
                            android.util.Log.e("VideoPlayerActivity", "Error pausing player on dispose", e)
                        }
                    }
                }
            }
        }

        BackHandler(isFullscreen) {
            isFullscreen = false
            (context as Activity).requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }

        Column(modifier = Modifier.fillMaxSize().background(Color(0xFF000000))) {
            // ── Smart Dynamic Video Player Frame ────────────────
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (isFullscreen) Modifier.weight(1f) 
                        else Modifier
                            .graphicsLayer {
                                scaleX = playerScale
                                scaleY = playerScale
                                translationY = playerOffsetY
                            }
                            .aspectRatio(videoAspectRatio.coerceIn(0.5f, 2.0f))
                    )
                    .clip(if (!isFullscreen && playerScale == 1f) RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp) else RoundedCornerShape(0.dp))
                    .background(Color.Black)
            ) {
                val width = constraints.maxWidth
                
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            this.player = playerInstance
                            useController = false
                            setBackgroundColor(android.graphics.Color.BLACK)
                        }
                    },
                    update = { view ->
                        view.player = playerInstance
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Interactions Overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(isLocked) {
                            if (!isLocked) {
                                detectTapGestures(
                                    onTap = { showControls = !showControls },
                                    onDoubleTap = { offset ->
                                        playerInstance?.let { player ->
                                            if (offset.x < width / 2) {
                                                player.seekTo((player.currentPosition - 10000).coerceAtLeast(0))
                                                doubleTapSide = "left"
                                            } else {
                                                player.seekTo((player.currentPosition + 10000).coerceAtMost(player.duration))
                                                doubleTapSide = "right"
                                            }
                                        }
                                        scope.launch {
                                            delay(600)
                                            doubleTapSide = "none"
                                        }
                                    }
                                )
                            }
                        }
                        .pointerInput(isLocked) {
                            if (!isLocked) {
                                detectDragGestures(
                                    onDragStart = {
                                        playerInstance?.let { player ->
                                            dragSumX = 0f
                                            dragSumY = 0f
                                            initialSeekTime = player.currentPosition
                                            hasStartedGesture = false
                                        }
                                    },
                                    onDragEnd = {
                                        if (hasStartedGesture) {
                                            if (gestureType == "seek") {
                                                playerInstance?.let { player ->
                                                    player.seekTo((initialSeekTime + seekOffsetSum).coerceIn(0L, player.duration))
                                                }
                                            }
                                        }

                                        // Swipe up to resize/minimize
                                        if (dragSumY < -150) {
                                            if (!isFullscreen) {
                                                playerScale = 0.6f
                                                playerOffsetY = -100f
                                                isMinimized = true
                                            }
                                        } else if (dragSumY > 150) {
                                            if (isMinimized) {
                                                playerScale = 1f
                                                playerOffsetY = 0f
                                                isMinimized = false
                                            } else if (!isFullscreen) {
                                                (context as Activity).finish()
                                            }
                                        }

                                        if (dragSumX > 300 && !isFullscreen) {
                                            isFullscreen = true
                                            (context as Activity).requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                                        }
                                        
                                        scope.launch {
                                            delay(1000)
                                            gestureType = "none"
                                        }
                                    },
                                    onDrag = { _, dragAmount ->
                                        dragSumX += dragAmount.x
                                        dragSumY += dragAmount.y

                                        if (!hasStartedGesture) {
                                            if (abs(dragSumX) > abs(dragSumY) && abs(dragSumX) > 50f) {
                                                gestureType = "seek"
                                                hasStartedGesture = true
                                            }
                                        }

                                        if (hasStartedGesture) {
                                            if (gestureType == "seek") {
                                                playerInstance?.let { player ->
                                                    val deltaX = dragSumX / 8f
                                                    seekOffsetSum = (deltaX * 1000).toLong()
                                                    val targetTime = (initialSeekTime + seekOffsetSum).coerceIn(0L, player.duration)
                                                    val diffSeconds = (targetTime - initialSeekTime) / 1000
                                                    gestureText = if (diffSeconds >= 0) "+${diffSeconds}s" else "${diffSeconds}s"
                                                }
                                            }
                                        }

                                        // Real-time scale feedback while dragging up
                                        if (dragSumY < 0 && !isFullscreen && !hasStartedGesture) {
                                            playerScale = (1f + (dragSumY / 1000f)).coerceIn(0.5f, 1f)
                                        }
                                    }
                                )
                            }
                        }
                )

                // Custom Controls Overlay
                if (showControls) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.3f))
                            .zIndex(1f)
                    ) {
                        // Top Bar
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp).align(Alignment.TopCenter),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = {
                                if (isFullscreen) {
                                    isFullscreen = false
                                    (context as Activity).requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                                } else {
                                    (context as Activity).finish()
                                }
                            }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                            }
                            Text(currentTitle, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                            IconButton(onClick = { (context as Activity).enterPictureInPictureMode() }) {
                                Icon(Icons.Default.PictureInPicture, null, tint = Color.White)
                            }

                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .combinedClickable(
                                        onClick = {
                                            isAudioOnly = !isAudioOnly
                                            Toast.makeText(context, if (isAudioOnly) "Background Play ON" else "Background Play OFF", Toast.LENGTH_SHORT).show()
                                        },
                                        onLongClick = {
                                            extractAudioToMusic(context, currentUri, currentTitle)
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Headphones,
                                    contentDescription = "Background Play",
                                    tint = if (isAudioOnly) Color(0xFF108ADC) else Color.White
                                )
                            }

                            IconButton(onClick = { showSpeedMenu = true }) {
                                Icon(Icons.Default.Speed, null, tint = Color.White)
                            }
                            IconButton(onClick = { showSettingsMenu = true }) {
                                Icon(Icons.Default.Settings, null, tint = Color.White)
                            }
                        }

                        // Middle Controls
                        Row(
                            modifier = Modifier.align(Alignment.Center),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(48.dp)
                        ) {
                            IconButton(onClick = { playPrevious(playerInstance, playlistVideos, currentUri) { currentUri = it.path; currentTitle = it.name } }, modifier = Modifier.size(48.dp)) {
                                Icon(Icons.Default.SkipPrevious, null, tint = Color.White, modifier = Modifier.size(36.dp))
                            }
                            IconButton(onClick = { if (isPlaying) playerInstance?.pause() else playerInstance?.play() }, modifier = Modifier.size(72.dp).background(Color.Black.copy(alpha = 0.5f), CircleShape)) {
                                Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(48.dp))
                            }
                            IconButton(onClick = { playNext(playerInstance, playlistVideos, currentUri) { currentUri = it.path; currentTitle = it.name } }, modifier = Modifier.size(48.dp)) {
                                Icon(Icons.Default.SkipNext, null, tint = Color.White, modifier = Modifier.size(36.dp))
                            }
                        }

                        // Bottom Progress and Time
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val timeText = "${formatDuration(currentPosition)} / ${formatDuration(duration)}"
                                Text(
                                    text = timeText,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Thick Red Seeker Slider matching the requested image
                            val sliderValue = if (duration > 0) currentPosition.toFloat() / duration else 0f
                            Slider(
                                value = sliderValue,
                                onValueChange = {
                                    if (duration > 0) {
                                        playerInstance?.seekTo((it * duration).toLong())
                                        currentPosition = (it * duration).toLong()
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(20.dp),
                                thumb = {},
                                track = { state ->
                                    SliderDefaults.Track(
                                        sliderState = state,
                                        modifier = Modifier.height(3.dp),
                                        colors = SliderDefaults.colors(
                                            activeTrackColor = Color.Red,
                                            inactiveTrackColor = Color.Gray.copy(alpha = 0.4f)
                                        ),
                                        thumbTrackGapSize = 0.dp,
                                        drawStopIndicator = null
                                    )
                                }
                            )
                        }
                    }
                }

                // Double Tap Feedback
                if (doubleTapSide != "none") {
                    Box(
                        modifier = Modifier
                            .align(if (doubleTapSide == "left") Alignment.CenterStart else Alignment.CenterEnd)
                            .fillMaxHeight()
                            .width(width.dp / 3)
                            .background(Color.White.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("10s", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Gesture Notification
                if (gestureType != "none") {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(gestureText, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (!isFullscreen) {
                // ── Current Media Information Section ─────────────
                Column(modifier = Modifier.background(Color(0xFF121212)).padding(16.dp).fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 🗑️ Delete Icon
                        IconButton(
                            onClick = {
                                allVideos.find { it.path == currentUri }?.let { video ->
                                    videoViewModel.deleteFile(context, video)
                                    finish()
                                }
                            },
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(Icons.Default.Delete, "Delete", tint = Color.LightGray)
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // 📝 Media Details
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = currentTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "SnapTube Video",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // ❤️ Favorite Button
                        val favoriteUris by videoViewModel.favoriteUris.collectAsState()
                        val isFavorite = favoriteUris.contains(currentUri)

                        IconButton(
                            onClick = { videoViewModel.toggleFavorite(currentUri, currentTitle) },
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                null,
                                tint = if (isFavorite) Color(0xFFFF9800) else Color.LightGray
                            )
                        }
                    }
                }

                HorizontalDivider(thickness = 0.5.dp, color = Color.Gray.copy(alpha = 0.2f))

                // ── Playlist Section: SnapTube Video ──────────────
                Column(modifier = Modifier.fillMaxWidth().weight(1f).background(Color(0xFF0A0A0B))) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { showFolderMenu = true },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Playlist: ${selectedPlaylistFolder ?: "All Videos"}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFB300) // Gold/Yellow like Snaptube
                            )
                            Icon(Icons.Default.ArrowDropDown, null, tint = Color(0xFFFFB300), modifier = Modifier.size(20.dp))

                            DropdownMenu(
                                expanded = showFolderMenu,
                                onDismissRequest = { showFolderMenu = false },
                                modifier = Modifier.background(Color(0xFF1E1E1E))
                            ) {
                                DropdownMenuItem(
                                    text = { Text("All Videos", color = Color.White) },
                                    onClick = {
                                        selectedPlaylistFolder = null
                                        showFolderMenu = false
                                    }
                                )
                                folders.forEach { folderName ->
                                    DropdownMenuItem(
                                        text = { Text(folderName, color = Color.White) },
                                        onClick = {
                                            selectedPlaylistFolder = folderName
                                            showFolderMenu = false
                                        }
                                    )
                                }
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    playbackMode = (playbackMode + 1) % 4
                                    playerInstance?.let { p ->
                                        when(playbackMode) {
                                            0 -> { p.repeatMode = Player.REPEAT_MODE_OFF; p.shuffleModeEnabled = false }
                                            1 -> { p.repeatMode = Player.REPEAT_MODE_ALL; p.shuffleModeEnabled = false }
                                            2 -> { p.repeatMode = Player.REPEAT_MODE_ONE; p.shuffleModeEnabled = false }
                                            3 -> { p.repeatMode = Player.REPEAT_MODE_OFF; p.shuffleModeEnabled = true }
                                        }
                                    }
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                val icon = when(playbackMode) {
                                    1 -> Icons.Default.Repeat
                                    2 -> Icons.Default.RepeatOne
                                    3 -> Icons.Default.Shuffle
                                    else -> Icons.Default.FormatListBulleted
                                }
                                Icon(icon, null, tint = if (playbackMode != 0) Color(0xFFFFB300) else Color.LightGray, modifier = Modifier.size(20.dp))
                            }
                            IconButton(onClick = { isPlaylistGridView = !isPlaylistGridView }, modifier = Modifier.size(36.dp)) {
                                Icon(
                                    if (isPlaylistGridView) Icons.AutoMirrored.Filled.ViewList else Icons.Default.GridView,
                                    null,
                                    tint = Color.LightGray,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Box {
                                IconButton(onClick = { showSortMenuPlaylist = true }, modifier = Modifier.size(36.dp)) {
                                    Icon(Icons.Default.Sort, null, tint = Color.LightGray, modifier = Modifier.size(20.dp))
                                }

                                DropdownMenu(
                                    expanded = showSortMenuPlaylist,
                                    onDismissRequest = { showSortMenuPlaylist = false },
                                    modifier = Modifier.background(Color(0xFF1E1E1E))
                                ) {
                                    val sortOptions = listOf(
                                        "name_asc" to "Name (A-Z)",
                                        "name_desc" to "Name (Z-A)",
                                        "date_desc" to "Newest First",
                                        "date_asc" to "Oldest First",
                                        "size_desc" to "Largest First",
                                        "size_asc" to "Smallest First"
                                    )
                                    sortOptions.forEach { (id, label) ->
                                        DropdownMenuItem(
                                            text = { Text(label, color = if (videoSortOrder == id) Color(0xFFFFB300) else Color.White) },
                                            onClick = {
                                                videoSortOrder = id
                                                showSortMenuPlaylist = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (isPlaylistGridView) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(items = playlistVideos) { video ->
                                RelatedVideoGridItem(
                                    video = video,
                                    currentUri = currentUri,
                                    onClick = {
                                        currentUri = video.path
                                        currentTitle = video.name
                                    }
                                )
                            }
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(items = playlistVideos) { video ->
                                RelatedVideoItem(
                                    video = video,
                                    currentUri = currentUri,
                                    onClick = {
                                        currentUri = video.path
                                        currentTitle = video.name
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showSpeedMenu) {
            AlertDialog(
                onDismissRequest = { showSpeedMenu = false },
                title = { Text("Playback Speed") },
                text = {
                    Column {
                        listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { speed ->
                            TextButton(onClick = {
                                playbackSpeed = speed
                                playerInstance?.playbackParameters = PlaybackParameters(speed)
                                showSpeedMenu = false
                            }) {
                                Text("${speed}x", color = if (playbackSpeed == speed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                },
                confirmButton = {}
            )
        }

        if (showSettingsMenu) {
            AlertDialog(
                onDismissRequest = { showSettingsMenu = false },
                title = { Text("Video Settings") },
                text = {
                    Column {
                        ListItem(
                            headlineContent = { Text("Playback Speed") },
                            trailingContent = { Text("${playbackSpeed}x") },
                            modifier = Modifier.clickable { showSettingsMenu = false; showSpeedMenu = true }
                        )
                        ListItem(
                            headlineContent = { Text("Screen Lock") },
                            supportingContent = { Text("Lock player controls") },
                            trailingContent = { Switch(checked = isLocked, onCheckedChange = { isLocked = it }) },
                            modifier = Modifier.clickable { isLocked = !isLocked }
                        )
                        ListItem(
                            headlineContent = { Text("Auto Play") },
                            supportingContent = { Text("Play next video automatically") },
                            trailingContent = { Switch(checked = autoPlayEnabled, onCheckedChange = { scope.launch { prefs.setAutoPlayEnabled(it) } }) },
                            modifier = Modifier.clickable { scope.launch { prefs.setAutoPlayEnabled(!autoPlayEnabled) } }
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showSettingsMenu = false }) { Text("Close") }
                }
            )
        }
    }

    private fun playNext(player: Player?, list: List<MediaFile>, currentUri: String, onNext: (MediaFile) -> Unit) {
        val currentIndex = list.indexOfFirst { it.path == currentUri }
        if (currentIndex != -1 && currentIndex < list.size - 1) {
            onNext(list[currentIndex + 1])
        }
    }

    private fun playPrevious(player: Player?, list: List<MediaFile>, currentUri: String, onPrev: (MediaFile) -> Unit) {
        val currentIndex = list.indexOfFirst { it.path == currentUri }
        if (currentIndex > 0) {
            onPrev(list[currentIndex - 1])
        }
    }

    @Composable
    fun RelatedVideoGridItem(video: MediaFile, currentUri: String, onClick: () -> Unit) {
        val isSelected = video.path == currentUri

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() },
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else Color.Transparent
            )
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16 / 10f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Gray.copy(alpha = 0.2f))
                ) {
                    AsyncImage(
                        model = video.path,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(6.dp)
                            .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = formatDuration(video.duration),
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = video.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                Text(
                    text = formatSize(video.size),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 8.dp)
                )
            }
        }
    }

    @Composable
    fun RelatedVideoItem(video: MediaFile, currentUri: String, onClick: () -> Unit) {
        val isSelected = video.path == currentUri

        ListItem(
            modifier = Modifier.fillMaxWidth().clickable { onClick() },
            colors = ListItemDefaults.colors(
                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else Color.Transparent
            ),
            leadingContent = {
                Box(modifier = Modifier.size(120.dp, 70.dp).clip(RoundedCornerShape(8.dp)).background(Color.Gray.copy(alpha = 0.2f))) {
                    AsyncImage(
                        model = video.path,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp).background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp)).padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(formatDuration(video.duration), color = Color.White, fontSize = 10.sp)
                    }
                }
            },
            headlineContent = {
                Text(video.name, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
            },
            supportingContent = {
                Text(formatSize(video.size), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        )
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        enterPictureInPictureMode()
    }

    private fun extractAudioToMusic(context: Context, videoPath: String, title: String) {
        Toast.makeText(context, "Extracting MP3...", Toast.LENGTH_SHORT).show()

        val scope = kotlinx.coroutines.MainScope()
        scope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val videoFile = java.io.File(videoPath)
                if (!videoFile.exists()) return@launch

                val musicDir = java.io.File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "Zeper/Music")
                if (!musicDir.exists()) musicDir.mkdirs()

                val outputFileName = title.substringBeforeLast(".") + ".mp3"
                val outputFile = java.io.File(musicDir, outputFileName)

                val extractor = MediaExtractor()
                extractor.setDataSource(videoPath)

                var audioTrackIndex = -1
                for (i in 0 until extractor.trackCount) {
                    val format = extractor.getTrackFormat(i)
                    val mime = format.getString(MediaFormat.KEY_MIME)
                    if (mime?.startsWith("audio/") == true) {
                        audioTrackIndex = i
                        break
                    }
                }

                if (audioTrackIndex != -1) {
                    extractor.selectTrack(audioTrackIndex)
                    val muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
                    val trackIndex = muxer.addTrack(extractor.getTrackFormat(audioTrackIndex))
                    muxer.start()

                    val bufferSize = 1024 * 1024
                    val buffer = java.nio.ByteBuffer.allocate(bufferSize)
                    val bufferInfo = android.media.MediaCodec.BufferInfo()

                    while (true) {
                        bufferInfo.offset = 0
                        bufferInfo.size = extractor.readSampleData(buffer, 0)
                        if (bufferInfo.size < 0) break

                        bufferInfo.presentationTimeUs = extractor.sampleTime
                        bufferInfo.flags = android.media.MediaCodec.BUFFER_FLAG_KEY_FRAME // Simple mapping for direct copy
                        muxer.writeSampleData(trackIndex, buffer, bufferInfo)
                        extractor.advance()
                    }

                    muxer.stop()
                    muxer.release()

                    // Scan the file so it appears in Music apps
                    android.media.MediaScannerConnection.scanFile(context, arrayOf(outputFile.absolutePath), null, null)

                    launch(kotlinx.coroutines.Dispatchers.Main) {
                        Toast.makeText(context, "MP3 saved to Zeper/Music", Toast.LENGTH_LONG).show()
                    }
                }
                extractor.release()
            } catch (e: Exception) {
                e.printStackTrace()
                launch(kotlinx.coroutines.Dispatchers.Main) {
                    Toast.makeText(context, "Extraction failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
