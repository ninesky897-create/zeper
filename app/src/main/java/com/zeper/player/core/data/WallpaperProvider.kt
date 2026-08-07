package com.zeper.player.core.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

data class ZeperWallpaper(
    val uri: String?,
    val isBlur: Boolean = false
)

class WallpaperProvider(private val context: Context, private val prefs: PreferencesManager) {

    // Default Zeper Wallpapers from res/drawable
    private val defaultLightWallpaper = "android.resource://${context.packageName}/drawable/zeper_light"
    private val defaultDarkWallpaper = "android.resource://${context.packageName}/drawable/zeper_dark"

    val currentWallpaper: Flow<ZeperWallpaper> = combine(
        prefs.themeMode,
        prefs.wallpaperMode,
        prefs.manualWallpaperUri
    ) { theme, mode, manualUri ->
        if (mode == "manual" && manualUri != null) {
            ZeperWallpaper(manualUri)
        } else {
            // Auto mode: follow system theme
            val isDark = when (theme) {
                "dark" -> true
                "light" -> false
                else -> {
                    // Detect system dark mode
                    (context.resources.configuration.uiMode and 
                     android.content.res.Configuration.UI_MODE_NIGHT_MASK) == 
                     android.content.res.Configuration.UI_MODE_NIGHT_YES
                }
            }
            ZeperWallpaper(if (isDark) defaultDarkWallpaper else defaultLightWallpaper)
        }
    }
}
