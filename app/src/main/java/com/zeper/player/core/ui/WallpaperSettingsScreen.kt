package com.zeper.player.core.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.zeper.player.core.data.PreferencesManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WallpaperSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager(context) }
    val scope = rememberCoroutineScope()
    
    val wallpaperMode by prefs.wallpaperMode.collectAsState(initial = "auto")
    val wallpaperBlur by prefs.wallpaperBlur.collectAsState(initial = false)
    
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            uri?.let {
                scope.launch {
                    prefs.setManualWallpaperUri(it.toString())
                    prefs.setWallpaperMode("manual")
                }
            }
        }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Wallpaper Engine") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            Text("Mode", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            
            ListItem(
                headlineContent = { Text("Auto Wallpaper Mode") },
                supportingContent = { Text("Changes with system theme") },
                trailingContent = { RadioButton(selected = wallpaperMode == "auto", onClick = { scope.launch { prefs.setWallpaperMode("auto") } }) },
                modifier = Modifier.clickable { scope.launch { prefs.setWallpaperMode("auto") } }
            )
            
            ListItem(
                headlineContent = { Text("Manual Selection") },
                supportingContent = { Text("Fixed wallpaper selection") },
                trailingContent = { RadioButton(selected = wallpaperMode == "manual", onClick = { scope.launch { prefs.setWallpaperMode("manual") } }) },
                modifier = Modifier.clickable { scope.launch { prefs.setWallpaperMode("manual") } }
            )
            
            ListItem(
                headlineContent = { Text("Blur Effect") },
                supportingContent = { Text("Apply blur to wallpaper") },
                trailingContent = { Switch(checked = wallpaperBlur, onCheckedChange = { scope.launch { prefs.setWallpaperBlur(it) } }) }
            )
            
            HorizontalDivider()
            
            Text("Sources", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { galleryLauncher.launch("image/*") }, modifier = Modifier.weight(1f)) {
                    Text("Gallery")
                }
                Button(onClick = { /* TODO: Online Wallpapers */ }, modifier = Modifier.weight(1f)) {
                    Text("Online")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text("Default Wallpapers", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            
            val builtIn = listOf(
                "android.resource://${context.packageName}/drawable/zeper_light",
                "android.resource://${context.packageName}/drawable/zeper_dark"
            )
            
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(builtIn) { uri ->
                    Card(
                        modifier = Modifier
                            .aspectRatio(9f / 16f)
                            .clickable {
                                scope.launch {
                                    prefs.setManualWallpaperUri(uri)
                                    prefs.setWallpaperMode("manual")
                                }
                            }
                    ) {
                        AsyncImage(
                            model = uri,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
        }
    }
}
