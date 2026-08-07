package com.zeper.player.core.ui

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.zeper.player.music.data.MusicPlaybackManager
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.zeper.player.core.data.PreferencesManager
import com.zeper.player.downloader.ui.OnlineSearchScreen
import com.zeper.player.downloader.ui.DownloaderScreen
import com.zeper.player.games.ui.GamePlayScreen
import com.zeper.player.games.ui.GamesHubScreen
import com.zeper.player.music.ui.MusicListScreen
import com.zeper.player.music.ui.MusicPlayerScreen
import com.zeper.player.video.ui.VideoListScreen
import com.zeper.player.video.ui.HomeViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import java.util.Calendar
import kotlinx.coroutines.launch

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Videos : Screen("videos", "Videos", Icons.Default.VideoLibrary)
    object Music : Screen("music", "Music", Icons.Default.MusicNote)
    object Games : Screen("games", "Games", Icons.Default.VideogameAsset)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
    object ThemeSettings : Screen("theme_settings", "Themes", Icons.Default.Palette)
    object EyeCare : Screen("eye_care", "Eye Care", Icons.Default.Visibility)
    object FileSettings : Screen("file_settings", ".file", Icons.Default.FileOpen)
    object Downloader : Screen("downloader?url={url}", "Downloader", Icons.Default.Download)
    object About : Screen("about", "About", Icons.Default.Info)
    object GamePlay : Screen("game_play/{gameId}", "Play Game", Icons.Default.VideogameAsset)
    object MusicPlayer : Screen("music_player", "Music Player", Icons.Default.MusicNote)
    object AppIcon : Screen("app_icon", "App Icon", Icons.Default.Apps)
    object TrashBin : Screen("trash_bin", "Trash Bin", Icons.Default.Delete)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(initialSharedUrl: String? = null) {
    val context = LocalContext.current
    val playbackManager = remember { MusicPlaybackManager.getInstance(context) }
    val prefs = remember { PreferencesManager(context) }
    val navController = rememberNavController()
    val homeViewModel: HomeViewModel = viewModel()
    val selectedCategory by homeViewModel.selectedCategory.collectAsState()
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    LaunchedEffect(initialSharedUrl) {
        if (initialSharedUrl != null) {
            navController.navigate("downloader?url=$initialSharedUrl")
        }
    }

    val eyeCareEnabled by prefs.eyeCareEnabled.collectAsState(initial = false)
    val eyeCareIntensity by prefs.eyeCareIntensity.collectAsState(initial = 0.3f)
    val eyeCareScheduleEnabled by prefs.eyeCareScheduleEnabled.collectAsState(initial = false)
    val startHour by prefs.eyeCareStartHour.collectAsState(initial = 22)
    val startMinute by prefs.eyeCareStartMinute.collectAsState(initial = 0)
    val endHour by prefs.eyeCareEndHour.collectAsState(initial = 7)
    val endMinute by prefs.eyeCareEndMinute.collectAsState(initial = 0)

    val isEyeCareActive = remember(eyeCareEnabled, eyeCareScheduleEnabled, startHour, startMinute, endHour, endMinute) {
        if (!eyeCareEnabled) return@remember false
        if (!eyeCareScheduleEnabled) return@remember true
        val now = Calendar.getInstance()
        val currentHour = now.get(Calendar.HOUR_OF_DAY)
        val currentTime = currentHour * 60 + now.get(Calendar.MINUTE)
        val startTime = startHour * 60 + startMinute
        val endTime = endHour * 60 + endMinute
        if (startTime < endTime) currentTime in startTime until endTime
        else currentTime >= startTime || currentTime < endTime
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = true, // Enables standard edge swipe
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(300.dp),
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                drawerContentColor = MaterialTheme.colorScheme.onSurface
            ) {
                Spacer(Modifier.height(24.dp))
                // Full Logo instead of just text
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    AsyncImage(
                        model = com.zeper.player.R.drawable.zeper_rowend_light_logo,
                        contentDescription = "Zeper Logo",
                        modifier = Modifier
                            .height(60.dp)
                            .padding(start = 8.dp)
                    )
                }
                HorizontalDivider()
                
                NavigationDrawerItem(
                    label = { Text("Internet") },
                    selected = false,
                    onClick = { 
                        scope.launch { drawerState.close() }
                        navController.navigate(Screen.Downloader.route)
                    },
                    icon = { Icon(Icons.Default.Search, null) }
                )
                NavigationDrawerItem(
                    label = { Text("Settings") },
                    selected = false,
                    onClick = { 
                        scope.launch { drawerState.close() }
                        navController.navigate(Screen.Settings.route)
                    },
                    icon = { Icon(Icons.Default.Settings, null) }
                )
                NavigationDrawerItem(
                    label = { Text("Themes") },
                    selected = false,
                    onClick = { 
                        scope.launch { drawerState.close() }
                        navController.navigate(Screen.ThemeSettings.route)
                    },
                    icon = { Icon(Icons.Default.Palette, null) }
                )
                NavigationDrawerItem(
                    label = { Text("App Icon") },
                    selected = false,
                    onClick = { 
                        scope.launch { drawerState.close() }
                        navController.navigate(Screen.AppIcon.route)
                    },
                    icon = { Icon(Icons.Default.Apps, null) }
                )
                NavigationDrawerItem(
                    label = { Text("Trash Bin") },
                    selected = false,
                    onClick = { 
                        scope.launch { drawerState.close() }
                        navController.navigate(Screen.TrashBin.route)
                    },
                    icon = { Icon(Icons.Default.Delete, null) }
                )
                HorizontalDivider()
                NavigationDrawerItem(
                    label = { Text("About") },
                    selected = false,
                    onClick = { 
                        scope.launch { drawerState.close() }
                        navController.navigate(Screen.About.route)
                    },
                    icon = { Icon(Icons.Default.Info, null) }
                )
            }
        }
    ) {
        Box(modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                // Custom gesture to open drawer: Tap and slide right from any point on left side
                detectDragGestures { change, dragAmount ->
                    if (dragAmount.x > 50 && drawerState.isClosed) {
                        scope.launch { drawerState.open() }
                    }
                }
            }
            .drawWithContent {
                drawContent()
                if (isEyeCareActive) {
                    val filterColor = when {
                        eyeCareIntensity > 0.55f -> Color(0xFFFF9800).copy(alpha = (eyeCareIntensity - 0.5f) * 0.44f)
                        eyeCareIntensity < 0.45f -> Color(0xFF00FFFF).copy(alpha = (0.5f - eyeCareIntensity) * 0.34f)
                        else -> Color.Transparent
                    }
                    if (filterColor != Color.Transparent) drawRect(color = filterColor, size = size)
                }
            }
        ) {
            Scaffold(
                bottomBar = {
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = navBackStackEntry?.destination?.route
                    val isVideosTab = currentRoute == Screen.Videos.route
                    val shouldShowBottomBar = currentRoute in listOf(Screen.Videos.route, Screen.Music.route, Screen.Games.route)

                    val hideForSubCategory = isVideosTab && (selectedCategory == "Internet" || selectedCategory == "FM" || selectedCategory == "ZPR Share")

                    if (shouldShowBottomBar && !hideForSubCategory) {
                        Column {
                            MiniPlayer(playbackManager = playbackManager, onOpenPlayer = { navController.navigate(Screen.MusicPlayer.route) })
                            ZeperBottomNavigation(navController)
                        }
                    }
                }
            ) { innerPadding ->
                NavHost(
                    navController = navController,
                    startDestination = Screen.Videos.route,
                    modifier = Modifier.padding(innerPadding)
                ) {
                    composable(Screen.Videos.route) { 
                        VideoListScreen(
                            homeViewModel = homeViewModel
                        )
                    }
                    composable(Screen.Music.route) { MusicListScreen() }
                    composable(Screen.Games.route) {
                        GamesHubScreen(onGameClick = { game -> navController.navigate("game_play/${game.id}") })
                    }
                    composable(Screen.MusicPlayer.route) {
                        MusicPlayerScreen(playbackManager = playbackManager, onBack = { navController.popBackStack() })
                    }
                    composable(Screen.Settings.route) {
                        SettingsScreen(
                            onBack = { navController.popBackStack() },
                            onNavigateToThemes = { navController.navigate(Screen.ThemeSettings.route) },
                            onNavigateToEyeCare = { navController.navigate(Screen.EyeCare.route) },
                            onNavigateToFileSettings = { navController.navigate(Screen.FileSettings.route) },
                            onNavigateToAppIcon = { navController.navigate(Screen.AppIcon.route) }
                        )
                    }
                    composable(Screen.FileSettings.route) { FileSettingsScreen(onBack = { navController.popBackStack() }) }
                    composable(Screen.ThemeSettings.route) { ThemeSettingsScreen(onBack = { navController.popBackStack() }) }
                    composable(Screen.EyeCare.route) { EyeCareScreen(onBack = { navController.popBackStack() }) }
                    composable(Screen.AppIcon.route) { AppIconSettingsScreen(onBack = { navController.popBackStack() }) }
                    composable(Screen.TrashBin.route) { TrashBinScreen(onBack = { navController.popBackStack() }) }
                    composable(
                        Screen.Downloader.route,
                        arguments = listOf(navArgument("url") { type = NavType.StringType; nullable = true })
                    ) { backStackEntry ->
                        val initialUrl = backStackEntry.arguments?.getString("url")
                        OnlineSearchScreen(initialUrl = initialUrl, onBack = { navController.popBackStack() })
                    }
                    composable(Screen.About.route) { AboutScreen(onBack = { navController.popBackStack() }) }
                    composable(Screen.GamePlay.route) { backStackEntry ->
                        val gameId = backStackEntry.arguments?.getString("gameId")
                        GamePlayScreen(gameId = gameId ?: "", onBack = { navController.popBackStack() })
                    }
                }
            }
        }
    }
}

@Composable
fun ZeperBottomNavigation(navController: NavHostController) {
    val items = listOf(Screen.Videos, Screen.Music, Screen.Games)
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.onSurface) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route
        items.forEach { screen ->
            NavigationBarItem(
                icon = { Icon(screen.icon, contentDescription = screen.title) },
                label = { Text(screen.title, fontWeight = if (currentRoute == screen.route) FontWeight.Bold else FontWeight.Normal) },
                selected = currentRoute == screen.route,
                onClick = {
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

@Composable
fun MiniPlayer(playbackManager: MusicPlaybackManager, onOpenPlayer: () -> Unit) {
    val currentSong by playbackManager.currentSong.collectAsState()
    val isPlaying by playbackManager.isPlaying.collectAsState()
    val currentPosition by playbackManager.currentPosition.collectAsState()
    val duration by playbackManager.duration.collectAsState()
    if (currentSong == null) return
    val progress = if (duration > 0) currentPosition.toFloat() / duration else 0f
    Surface(
        modifier = Modifier.fillMaxWidth().height(72.dp).clickable { onOpenPlayer() },
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.Gray.copy(alpha = 0.2f))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(2.dp).align(Alignment.TopCenter), color = MaterialTheme.colorScheme.primary, trackColor = Color.Transparent)
            Row(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { playbackManager.stop() }) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Surface(modifier = Modifier.size(48.dp), shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(imageVector = Icons.Default.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = currentSong!!.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                    Text(text = "${formatTime(currentPosition)} / ${formatTime(duration)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = { playbackManager.playPause() }) {
                    Icon(imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = "Play/Pause", modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.onSurface)
                }
                IconButton(onClick = { /* Open Playlist */ }) {
                    Icon(imageVector = Icons.Default.Menu, contentDescription = "Playlist", tint = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return java.util.Locale.getDefault().let { String.format(it, "%02d:%02d", minutes, seconds) }
}
