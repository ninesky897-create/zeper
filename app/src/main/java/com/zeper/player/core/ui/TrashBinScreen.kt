package com.zeper.player.core.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zeper.player.core.data.TrashEntity
import com.zeper.player.core.data.TrashManager
import com.zeper.player.video.ui.HomeViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashBinScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val homeViewModel: HomeViewModel = viewModel()
    val db = com.zeper.player.core.data.ZeperDatabase.getInstance(context)
    val trashItems by db.mediaDao().getTrashItems().collectAsState(initial = emptyList())
    val trashManager = remember { TrashManager(context) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Trash Bin") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (trashItems.isEmpty()) {
            Box(modifier = Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Trash Bin is empty", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
                item {
                    Text(
                        "Items will be permanently deleted after 30 days.",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                items(trashItems) { item ->
                    TrashItemRow(
                        item = item,
                        onRestore = {
                            scope.launch {
                                if (trashManager.restoreFromTrash(item)) {
                                    homeViewModel.refreshData()
                                }
                            }
                        },
                        onDelete = {
                            scope.launch {
                                trashManager.permanentlyDelete(item)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun TrashItemRow(
    item: TrashEntity,
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {
    val sdf = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
    val dateStr = sdf.format(Date(item.deleteTimestamp))

    ListItem(
        headlineContent = { Text(item.fileName, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = { Text("Deleted: $dateStr") },
        trailingContent = {
            Row {
                IconButton(onClick = onRestore) {
                    Icon(Icons.Default.Restore, "Restore", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.DeleteForever, "Delete Permanently", tint = Color.Red)
                }
            }
        }
    )
}
