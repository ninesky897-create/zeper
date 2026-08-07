package com.zeper.player.core.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zeper.player.core.data.MediaFile
import com.zeper.player.video.ui.formatDuration
import com.zeper.player.video.ui.formatSize
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MediaOptionsMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    mediaFile: MediaFile,
    isFavorite: Boolean,
    onFavoriteClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onVaultClick: (() -> Unit)? = null,
    onConvertClick: (() -> Unit)? = null,
    onShareClick: () -> Unit,
    onDetailsClick: () -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss
    ) {
        DropdownMenuItem(
            text = { Text(if (isFavorite) "Remove Favorite" else "Favorite") },
            onClick = {
                onFavoriteClick()
                onDismiss()
            },
            leadingIcon = {
                Icon(
                    if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = null,
                    tint = if (isFavorite) androidx.compose.ui.graphics.Color.Red else LocalContentColor.current
                )
            }
        )
        
        DropdownMenuItem(
            text = { Text("Details") },
            onClick = {
                onDetailsClick()
                onDismiss()
            },
            leadingIcon = { Icon(Icons.Default.Info, null) }
        )

        if (mediaFile.type == "video" && onConvertClick != null) {
            DropdownMenuItem(
                text = { Text("Video to Audio") },
                onClick = {
                    onConvertClick()
                    onDismiss()
                },
                leadingIcon = { Icon(Icons.Default.MusicNote, null) }
            )
        }

        DropdownMenuItem(
            text = { Text("Share") },
            onClick = {
                onShareClick()
                onDismiss()
            },
            leadingIcon = { Icon(Icons.Default.Share, null) }
        )

        HorizontalDivider()

        if (onVaultClick != null) {
            DropdownMenuItem(
                text = { Text("Lock in Vault") },
                onClick = {
                    onVaultClick()
                    onDismiss()
                },
                leadingIcon = { Icon(Icons.Default.Lock, null) }
            )
        }

        DropdownMenuItem(
            text = { Text("Delete", color = androidx.compose.ui.graphics.Color.Red) },
            onClick = {
                onDeleteClick()
                onDismiss()
            },
            leadingIcon = { Icon(Icons.Default.Delete, null, tint = androidx.compose.ui.graphics.Color.Red) }
        )
    }
}

@Composable
fun FileDetailsDialog(
    mediaFile: MediaFile,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("File Details") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DetailRow("Name", mediaFile.name)
                DetailRow("Path", mediaFile.path)
                DetailRow("Size", formatSize(mediaFile.size))
                DetailRow("Duration", formatDuration(mediaFile.duration))
                DetailRow("Type", mediaFile.type.uppercase())
                if (mediaFile.dateAdded > 0) {
                    val date = Date(mediaFile.dateAdded * 1000)
                    val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
                    DetailRow("Date Added", sdf.format(date))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
