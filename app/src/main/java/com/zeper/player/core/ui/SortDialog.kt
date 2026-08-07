package com.zeper.player.core.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

/**
 * Custom Sort Dialog as per user request (matched to the provided photo).
 * Features two radio groups: Property (Name, Date, Size, Length) and Direction (Dependent on property).
 */
@Composable
fun SortDialog(
    currentSortOrder: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val properties = listOf("Name", "Date", "Size", "Length")
    val propertyKeys = listOf("name", "date", "size", "length")

    var selectedPropertyIndex by remember {
        val key = currentSortOrder.substringBefore("_")
        mutableIntStateOf(propertyKeys.indexOf(key).coerceAtLeast(0))
    }

    var isAscending by remember {
        val direction = currentSortOrder.substringAfter("_", "desc")
        mutableStateOf(direction == "asc")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sort by") },
        text = {
            Column(modifier = Modifier.selectableGroup()) {
                // Group 1: Property
                propertyKeys.forEachIndexed { index, key ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .selectable(
                                selected = (selectedPropertyIndex == index),
                                onClick = { selectedPropertyIndex = index },
                                role = Role.RadioButton
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (selectedPropertyIndex == index),
                            onClick = null
                        )
                        Text(
                            text = properties[index],
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // Group 2: Direction
                val currentKey = propertyKeys[selectedPropertyIndex]
                
                // Texts based on property
                val descText = when (currentKey) {
                    "name" -> "From Z to A"
                    "date" -> "From new to old"
                    "size" -> "From big to small"
                    "length" -> "From long to short"
                    else -> "Descending"
                }
                val ascText = when (currentKey) {
                    "name" -> "From A to Z"
                    "date" -> "From old to new"
                    "size" -> "From small to big"
                    "length" -> "From short to long"
                    else -> "Ascending"
                }

                // Descending row
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .selectable(
                            selected = !isAscending,
                            onClick = { isAscending = false },
                            role = Role.RadioButton
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = !isAscending, onClick = null)
                    Text(
                        text = descText,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                }

                // Ascending row
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .selectable(
                            selected = isAscending,
                            onClick = { isAscending = true },
                            role = Role.RadioButton
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = isAscending, onClick = null)
                    Text(
                        text = ascText,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val property = propertyKeys[selectedPropertyIndex]
                val direction = if (isAscending) "asc" else "desc"
                onConfirm("${property}_${direction}")
            }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL")
            }
        }
    )
}
