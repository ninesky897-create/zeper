package com.zeper.player

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import android.content.Intent
import com.zeper.player.core.data.PreferencesManager
import com.zeper.player.core.ui.MainScreen
import com.zeper.player.core.ui.theme.ZeperTheme
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavHostController
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.zeper.player.core.data.TrashWorker
import java.util.concurrent.TimeUnit

import coil.ImageLoader
import coil.decode.VideoFrameDecoder
import coil.Coil

class MainActivity : ComponentActivity() {
    private var sharedUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_Zeper)
        super.onCreate(savedInstanceState)
        
        // Configure Coil to support video thumbnails
        val imageLoader = ImageLoader.Builder(this)
            .components {
                add(VideoFrameDecoder.Factory())
            }
            .crossfade(true)
            .build()
        Coil.setImageLoader(imageLoader)

        handleIntent(intent)
        scheduleTrashCleanup()
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val prefs = remember { PreferencesManager(context) }
            
            val themeMode by prefs.themeMode.collectAsState(initial = "auto")
            
            val darkTheme = when (themeMode) {
                "light" -> false
                "dark" -> true
                else -> androidx.compose.foundation.isSystemInDarkTheme()
            }

            var hasPermission by remember {
                mutableStateOf(checkStoragePermission(context))
            }

            val permissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) { permissions ->
                hasPermission = permissions.values.all { it }
            }

            // Automatically request permission on startup
            LaunchedEffect(Unit) {
                if (!hasPermission) {
                    val permissions = mutableListOf<String>()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissions.add(Manifest.permission.READ_MEDIA_VIDEO)
                        permissions.add(Manifest.permission.READ_MEDIA_AUDIO)
                        permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
                        permissions.add(Manifest.permission.POST_NOTIFICATIONS)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                            permissions.add(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
                        }
                    } else {
                        permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
                        permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    }
                    permissionLauncher.launch(permissions.toTypedArray())
                }
            }

            ZeperTheme(darkTheme = darkTheme, prefs = prefs) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.onBackground
                ) {
                    MainScreen(initialSharedUrl = sharedUrl)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh library when returning to app
        // We can't easily access the ViewModel here without more refactoring, 
        // but the DB flow should handle history.
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            sharedUrl = intent.getStringExtra(Intent.EXTRA_TEXT)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun checkStoragePermission(context: android.content.Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val video = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED
            val audio = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED
            val images = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val partial = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) == PackageManager.PERMISSION_GRANTED
                video && audio && (images || partial)
            } else {
                video && audio && images
            }
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun scheduleTrashCleanup() {
        val workRequest = PeriodicWorkRequestBuilder<TrashWorker>(1, TimeUnit.DAYS)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "TrashCleanup",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }
}
