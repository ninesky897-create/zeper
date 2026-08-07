package com.zeper.player.core.ui

import android.app.TimePickerDialog
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.zeper.player.core.data.PreferencesManager
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EyeCareScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager(context) }
    val scope = rememberCoroutineScope()

    val enabled by prefs.eyeCareEnabled.collectAsState(initial = false)
    val intensity by prefs.eyeCareIntensity.collectAsState(initial = 0.3f)
    val scheduleEnabled by prefs.eyeCareScheduleEnabled.collectAsState(initial = false)
    val startHour by prefs.eyeCareStartHour.collectAsState(initial = 22)
    val startMinute by prefs.eyeCareStartMinute.collectAsState(initial = 0)
    val endHour by prefs.eyeCareEndHour.collectAsState(initial = 7)
    val endMinute by prefs.eyeCareEndMinute.collectAsState(initial = 0)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Eye Care") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            item {
                ListItem(
                    headlineContent = { Text("Eye Care Mode") },
                    supportingContent = { Text("Reduce eye strain by filtering blue light") },
                    trailingContent = {
                        Switch(
                            checked = enabled,
                            onCheckedChange = { scope.launch { prefs.setEyeCareEnabled(it) } }
                        )
                    }
                )
                HorizontalDivider()
            }

            if (enabled) {
                item {
                    SectionHeader("Settings")
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Cool", style = MaterialTheme.typography.bodyMedium)
                            Text("Default", style = MaterialTheme.typography.bodyMedium)
                            Text("Warm", style = MaterialTheme.typography.bodyMedium)
                        }
                        Slider(
                            value = intensity,
                            onValueChange = { scope.launch { prefs.setEyeCareIntensity(it) } },
                            valueRange = 0f..1f,
                            steps = 20
                        )
                        val statusText = when {
                            intensity > 0.55f -> "Warm: ${((intensity - 0.5f) * 200).toInt()}%"
                            intensity < 0.45f -> "Cool: ${((0.5f - intensity) * 200).toInt()}%"
                            else -> "Default (Neutral)"
                        }
                        Text(
                            statusText,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider()
                }

                item {
                    SectionHeader("Schedule")
                    ListItem(
                        headlineContent = { Text("Schedule") },
                        supportingContent = { Text("Automatically turn on Eye Care at a set time") },
                        trailingContent = {
                            Switch(
                                checked = scheduleEnabled,
                                onCheckedChange = { scope.launch { prefs.setEyeCareScheduleEnabled(it) } }
                            )
                        }
                    )

                    if (scheduleEnabled) {
                        ListItem(
                            headlineContent = { Text("Start Time") },
                            trailingContent = {
                                TextButton(onClick = {
                                    TimePickerDialog(context, { _, h, m ->
                                        scope.launch { prefs.setEyeCareStartTime(h, m) }
                                    }, startHour, startMinute, false).show()
                                }) {
                                    Text(String.format(Locale.getDefault(), "%02d:%02d", startHour, startMinute))
                                }
                            }
                        )
                        ListItem(
                            headlineContent = { Text("End Time") },
                            trailingContent = {
                                TextButton(onClick = {
                                    TimePickerDialog(context, { _, h, m ->
                                        scope.launch { prefs.setEyeCareEndTime(h, m) }
                                    }, endHour, endMinute, false).show()
                                }) {
                                    Text(String.format(Locale.getDefault(), "%02d:%02d", endHour, endMinute))
                                }
                            }
                        )
                    }
                    HorizontalDivider()
                }
            }

            item {
                SectionHeader("System Settings")
                ListItem(
                    headlineContent = { Text("System Night Light") },
                    supportingContent = { Text("Configure phone's native blue light filter") },
                    trailingContent = {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, modifier = Modifier.size(20.dp).padding(start = 12.dp)) // Placeholder for right arrow if desired, or just use clickable
                    },
                    modifier = Modifier.clickable {
                        try {
                            val intent = Intent(Settings.ACTION_NIGHT_DISPLAY_SETTINGS)
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            try {
                                val intent = Intent(Settings.ACTION_DISPLAY_SETTINGS)
                                context.startActivity(intent)
                            } catch (e2: Exception) {
                                // Fallback
                            }
                        }
                    }
                )
            }
        }
    }
}
