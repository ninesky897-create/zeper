package com.zeper.player.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zeper.player.core.data.PreferencesManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager(context) }
    val scope = rememberCoroutineScope()

    val themeMode by prefs.themeMode.collectAsState(initial = "auto")
    val customBg by prefs.customBgColor.collectAsState(initial = "#000000")
    val customText by prefs.customTextColor.collectAsState(initial = "#00FFFF")
    val customEnabled by prefs.customThemeEnabled.collectAsState(initial = false)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Themes") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            // 1. MAIN THEMES (TOP)
            SectionHeader("Main Themes")
            
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Pitch Black
                Box(modifier = Modifier.weight(1f)) {
                    ThemePreviewCard(
                        preset = ThemePreset("Pitch Black", "#000000", "#FFFFFF"),
                        isSelected = !customEnabled && themeMode == "dark",
                        onClick = {
                            scope.launch {
                                prefs.setCustomThemeEnabled(false)
                                prefs.setThemeMode("dark")
                            }
                        }
                    )
                }
                // Snow White
                Box(modifier = Modifier.weight(1f)) {
                    ThemePreviewCard(
                        preset = ThemePreset("Snow White", "#FFFFFF", "#000000"),
                        isSelected = !customEnabled && themeMode == "light",
                        onClick = {
                            scope.launch {
                                prefs.setCustomThemeEnabled(false)
                                prefs.setThemeMode("light")
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            ThemeSelector(themeMode) { mode ->
                scope.launch { 
                    prefs.setCustomThemeEnabled(false)
                    prefs.setThemeMode(mode) 
                }
            }
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // 2. THEME GALLERY (Photos/Presets)
            SectionHeader("Theme Gallery")
            
            val presets = listOf(
                ThemePreset("Cyber Cyan", "#000000", "#00FFFF"),
                ThemePreset("Zeper Orange", "#FEFDFD", "#FF8C00"),
                ThemePreset("Deep Sea", "#001F3F", "#00FFFF"),
                ThemePreset("Forest", "#00332E", "#4CAF50"),
                ThemePreset("Blood Red", "#1A0000", "#F44336"),
                ThemePreset("Neon Pink", "#000000", "#FF00FF"),
                ThemePreset("Muted Gray", "#121212", "#9E9E9E"),
                ThemePreset("Gold Edition", "#000000", "#FFD700")
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(presets) { preset ->
                    ThemePreviewCard(
                        preset = preset,
                        isSelected = customEnabled && customBg.lowercase() == preset.bg.lowercase() && customText.lowercase() == preset.text.lowercase(),
                        onClick = {
                            scope.launch {
                                prefs.setCustomThemeEnabled(true)
                                prefs.setCustomBgColor(preset.bg)
                                prefs.setCustomTextColor(preset.text)
                                prefs.setCustomPrimaryColor(preset.text)
                            }
                        }
                    )
                }
            }

            // Reset Button
            TextButton(
                onClick = {
                    scope.launch {
                        prefs.setCustomThemeEnabled(false)
                        prefs.setThemeMode("auto")
                    }
                },
                modifier = Modifier.padding(16.dp).fillMaxWidth()
            ) {
                Text("Reset to System Default")
            }
        }
    }
}

data class ThemePreset(val name: String, val bg: String, val text: String)

@Composable
fun ThemePreviewCard(preset: ThemePreset, isSelected: Boolean, onClick: () -> Unit) {
    val bgColor = try { Color(android.graphics.Color.parseColor(preset.bg)) } catch (e: Exception) { Color.Black }
    val textColor = try { Color(android.graphics.Color.parseColor(preset.text)) } catch (e: Exception) { Color.White }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp)
            .clickable { onClick() }
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) Color.Red else Color.Gray.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    preset.name,
                    color = textColor,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Sample Text",
                    color = textColor.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.bodySmall
                )
                // Opposites logic preview icon
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Bg block
                    Box(modifier = Modifier.size(16.dp, 12.dp).clip(RoundedCornerShape(2.dp)).background(bgColor).border(1.dp, textColor, RoundedCornerShape(2.dp)))
                    Spacer(modifier = Modifier.width(4.dp))
                    // Text block
                    Box(modifier = Modifier.size(16.dp, 12.dp).clip(RoundedCornerShape(2.dp)).background(textColor))
                }
            }
            
            if (isSelected) {
                Surface(
                    shape = CircleShape,
                    color = Color.Red,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(18.dp)
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.padding(2.dp),
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun ThemeSelector(current: String, onSelected: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listOf("auto", "light", "dark").forEach { mode ->
            val isSelected = current == mode
            Button(
                onClick = { onSelected(mode) },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                ),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(mode.uppercase(), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
