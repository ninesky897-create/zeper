package com.zeper.player.core.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.zeper.player.core.data.PreferencesManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onNavigateToThemes: () -> Unit,
    onNavigateToEyeCare: () -> Unit,
    onNavigateToFileSettings: () -> Unit,
    onNavigateToAppIcon: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager(context) }
    
    val eyeCareEnabled by prefs.eyeCareEnabled.collectAsState(initial = false)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
            // General Settings Group
            item {
                ListItem(
                    headlineContent = { Text("Themes") },
                    supportingContent = { Text("Change app colors and custom theme") },
                    trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) },
                    modifier = Modifier.clickable { onNavigateToThemes() }
                )
                
                ListItem(
                    headlineContent = { Text("App icon") },
                    supportingContent = { Text("Customize home screen icon") },
                    trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) },
                    modifier = Modifier.clickable { onNavigateToAppIcon() }
                )

                ListItem(
                    headlineContent = { Text("Eye Care") },
                    supportingContent = { Text(if (eyeCareEnabled) "On" else "Off") },
                    trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) },
                    modifier = Modifier.clickable { onNavigateToEyeCare() }
                )
                
                ListItem(
                    headlineContent = { Text("Manage scan list") },
                    supportingContent = { Text("Hidden files, dot files, and .nomedia") },
                    trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) },
                    modifier = Modifier.clickable { onNavigateToFileSettings() }
                )

                HorizontalDivider()
            }
            
            item {
                SectionHeader("App Maintenance")
                ListItem(
                    headlineContent = { Text("Clear Cache") },
                    modifier = Modifier.clickable { /* TODO */ }
                )
                ListItem(
                    headlineContent = { Text("Refresh Library") },
                    modifier = Modifier.clickable { /* TODO */ }
                )
            }
        }
    }
}
