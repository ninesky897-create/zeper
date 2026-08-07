package com.zeper.player.core.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.zeper.player.core.data.PreferencesManager

private val CyberCyanDark = darkColorScheme(
    primary = Color.White,
    onPrimary = Color.Black,
    background = Color.Black,
    onBackground = Color.White,
    surface = Color.Black,
    onSurface = Color.White,
    surfaceVariant = Color.Black,
    onSurfaceVariant = Color.White,
    outline = Color.White,
    primaryContainer = Color.Black,
    onPrimaryContainer = Color.White
)

private val ZeperOrangeLight = lightColorScheme(
    primary = Color.Black,
    onPrimary = Color.White,
    background = Color.White,
    onBackground = Color.Black,
    surface = Color.White,
    onSurface = Color.Black,
    surfaceVariant = Color.White,
    onSurfaceVariant = Color.Black,
    outline = Color.Black,
    primaryContainer = Color.White,
    onPrimaryContainer = Color.Black
)

@Composable
fun ZeperTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    prefs: PreferencesManager? = null,
    content: @Composable () -> Unit
) {
    val accentColorName = prefs?.accentColor?.collectAsState(initial = "orange")?.value ?: "orange"
    val customEnabled = prefs?.customThemeEnabled?.collectAsState(initial = false)?.value ?: false
    val customBg = prefs?.customBgColor?.collectAsState(initial = "#000000")?.value ?: "#000000"
    val customText = prefs?.customTextColor?.collectAsState(initial = "#00FFFF")?.value ?: "#00FFFF"
    val customPrimary = prefs?.customPrimaryColor?.collectAsState(initial = "#00FFFF")?.value ?: "#00FFFF"

    val colorScheme = remember(darkTheme, accentColorName, customEnabled, customBg, customText, customPrimary) {
        if (customEnabled) {
            val bg = parseColor(customBg)
            val text = parseColor(customText)
            val primary = parseColor(customPrimary)
            darkColorScheme(
                primary = primary,
                onPrimary = bg,
                background = bg,
                onBackground = text,
                surface = bg,
                onSurface = text,
                surfaceVariant = bg,
                onSurfaceVariant = text,
                outline = text,
                primaryContainer = bg,
                onPrimaryContainer = text
            )
        } else {
            if (darkTheme) CyberCyanDark else ZeperOrangeLight
        }
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

private fun parseColor(hex: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        Color.Black
    }
}
