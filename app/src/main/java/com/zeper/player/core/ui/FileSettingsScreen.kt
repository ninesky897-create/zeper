package com.zeper.player.core.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zeper.player.core.data.PreferencesManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager(context) }
    val scope = rememberCoroutineScope()
    
    val scanHidden by prefs.scanHiddenFiles.collectAsState(initial = false)
    val scanNomedia by prefs.scanNomediaFiles.collectAsState(initial = false)
    val retention by prefs.trashRetentionDays.collectAsState(initial = 30)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage scan list") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
            item {
                Text(
                    text = "VIDEO",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                SwitchPreference(
                    title = "Recognize hidden files",
                    description = "Recognize hidden files starting with \".\"(dot). It may take more time to scan.",
                    checked = scanHidden,
                    onCheckedChange = { scope.launch { prefs.setScanHiddenFiles(it) } }
                )
                SwitchPreference(
                    title = "Recognize .nomedia",
                    description = "Recognize file under the folder which contains \".nomedia\" file",
                    checked = scanNomedia,
                    onCheckedChange = { scope.launch { prefs.setScanNomediaFiles(it) } }
                )
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                Text(
                    text = "MUSIC",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                
                SwitchPreference(
                    title = "Recognize hidden files",
                    description = "Scan music files in hidden folders",
                    checked = scanHidden,
                    onCheckedChange = { scope.launch { prefs.setScanHiddenFiles(it) } }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                ListItem(
                    headlineContent = { Text("Trash Bin Retention") },
                    supportingContent = { Text("Items are deleted after $retention days") },
                    trailingContent = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { scope.launch { prefs.setTrashRetentionDays((retention - 1).coerceAtLeast(1)) } }) {
                                Icon(Icons.Default.Remove, null)
                            }
                            Text("$retention", fontWeight = FontWeight.Bold)
                            IconButton(onClick = { scope.launch { prefs.setTrashRetentionDays((retention + 1).coerceAtMost(365)) } }) {
                                Icon(Icons.Default.Add, null)
                            }
                        }
                    }
                )
            }
        }
    }
}
