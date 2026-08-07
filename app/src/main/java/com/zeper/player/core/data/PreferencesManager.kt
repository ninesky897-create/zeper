package com.zeper.player.core.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "zeper_preferences")

class PreferencesManager(private val context: Context) {

    companion object {
        val THEME_MODE = stringPreferencesKey("theme_mode") // "auto", "light", "dark"
        
        // V2 File Scanner Toggles
        val SCAN_HIDDEN_FILES = booleanPreferencesKey("scan_hidden_files")
        val SCAN_DOT_FILES = booleanPreferencesKey("scan_dot_files")
        val SCAN_NOMEDIA_FILES = booleanPreferencesKey("scan_nomedia_files")
        
        // V2 Wallpaper Settings
        val WALLPAPER_MODE = stringPreferencesKey("wallpaper_mode") // "auto", "manual"
        val MANUAL_WALLPAPER_URI = stringPreferencesKey("manual_wallpaper_uri")
        val WALLPAPER_BLUR = booleanPreferencesKey("wallpaper_blur")

        // V2 Theme Extension
        val ACCENT_COLOR = stringPreferencesKey("accent_color") // "orange", "blue", "red", "green", "purple", "cyan"
        val CUSTOM_THEME_ENABLED = booleanPreferencesKey("custom_theme_enabled")
        val CUSTOM_BG_COLOR = stringPreferencesKey("custom_bg_color") // Hex
        val CUSTOM_TEXT_COLOR = stringPreferencesKey("custom_text_color") // Hex
        val CUSTOM_PRIMARY_COLOR = stringPreferencesKey("custom_primary_color") // Hex

        // V2 Privacy Settings
        val PRIVACY_PIN = stringPreferencesKey("privacy_pin")
        val HIDDEN_FOLDERS = stringPreferencesKey("hidden_folders") // Comma separated list
        val LOCKED_FOLDERS = stringPreferencesKey("locked_folders") // Comma separated list

        // V2 Eye Care Settings
        val EYE_CARE_ENABLED = booleanPreferencesKey("eye_care_enabled")
        val EYE_CARE_INTENSITY = floatPreferencesKey("eye_care_intensity") // 0.0f to 1.0f
        val EYE_CARE_SCHEDULE_ENABLED = booleanPreferencesKey("eye_care_schedule_enabled")
        val EYE_CARE_START_HOUR = intPreferencesKey("eye_care_start_hour")
        val EYE_CARE_START_MINUTE = intPreferencesKey("eye_care_start_minute")
        val EYE_CARE_END_HOUR = intPreferencesKey("eye_care_end_hour")
        val EYE_CARE_END_MINUTE = intPreferencesKey("eye_care_end_minute")

        // V2 View Settings
        val IS_GRID_VIEW = booleanPreferencesKey("is_grid_view")

        // Player Settings
        val AUTO_PLAY_ENABLED = booleanPreferencesKey("auto_play_enabled")

        // App Logo Settings
        val SELECTED_LOGO_TYPE = stringPreferencesKey("selected_logo_type") // "rowend", "socer"

        // Trash Bin Settings
        val TRASH_RETENTION_DAYS = intPreferencesKey("trash_retention_days")

        // Snake Game Save
        val SNAKE_SAVE_DATA = stringPreferencesKey("snake_save_data")
    }

    val snakeSaveData: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[SNAKE_SAVE_DATA]
    }

    suspend fun saveSnakeGame(data: String?) {
        context.dataStore.edit { preferences ->
            if (data == null) {
                preferences.remove(SNAKE_SAVE_DATA)
            } else {
                preferences[SNAKE_SAVE_DATA] = data
            }
        }
    }

    val trashRetentionDays: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[TRASH_RETENTION_DAYS] ?: 30
    }

    suspend fun setTrashRetentionDays(days: Int) {
        context.dataStore.edit { preferences ->
            preferences[TRASH_RETENTION_DAYS] = days
        }
    }

    val hiddenFolders: Flow<List<String>> = context.dataStore.data.map { preferences ->
        preferences[HIDDEN_FOLDERS]?.split(",")?.filter { it.isNotEmpty() } ?: emptyList()
    }

    val lockedFolders: Flow<List<String>> = context.dataStore.data.map { preferences ->
        preferences[LOCKED_FOLDERS]?.split(",")?.filter { it.isNotEmpty() } ?: emptyList()
    }

    suspend fun toggleFolderLock(folderName: String) {
        context.dataStore.edit { preferences ->
            val current = preferences[LOCKED_FOLDERS]?.split(",")?.filter { it.isNotEmpty() } ?: emptyList()
            val updated = if (current.contains(folderName)) {
                current.filter { it != folderName }
            } else {
                current + folderName
            }
            preferences[LOCKED_FOLDERS] = updated.joinToString(",")
        }
    }

    suspend fun toggleFolderVisibility(folderName: String) {
        context.dataStore.edit { preferences ->
            val current = preferences[HIDDEN_FOLDERS]?.split(",")?.filter { it.isNotEmpty() } ?: emptyList()
            val updated = if (current.contains(folderName)) {
                current.filter { it != folderName }
            } else {
                current + folderName
            }
            preferences[HIDDEN_FOLDERS] = updated.joinToString(",")
        }
    }

    val privacyPin: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[PRIVACY_PIN]
    }

    suspend fun setPrivacyPin(pin: String?) {
        context.dataStore.edit { preferences ->
            if (pin == null) {
                preferences.remove(PRIVACY_PIN)
            } else {
                // Securely hash the PIN before storing
                val hashedPin = hashPin(pin)
                preferences[PRIVACY_PIN] = hashedPin
            }
        }
    }

    private fun hashPin(pin: String): String {
        return try {
            val md = java.security.MessageDigest.getInstance("SHA-256")
            val digest = md.digest(pin.toByteArray())
            digest.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            pin // Fallback to plaintext if hashing fails (should not happen)
        }
    }

    fun verifyPin(input: String, storedHash: String?): Boolean {
        if (storedHash == null) return false
        return hashPin(input) == storedHash
    }

    val themeMode: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[THEME_MODE] ?: "auto"
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { preferences ->
            preferences[THEME_MODE] = mode
        }
    }

    val scanHiddenFiles: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[SCAN_HIDDEN_FILES] ?: false
    }

    suspend fun setScanHiddenFiles(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SCAN_HIDDEN_FILES] = enabled
        }
    }

    val scanDotFiles: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[SCAN_DOT_FILES] ?: false
    }

    suspend fun setScanDotFiles(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SCAN_DOT_FILES] = enabled
        }
    }

    val scanNomediaFiles: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[SCAN_NOMEDIA_FILES] ?: false
    }

    suspend fun setScanNomediaFiles(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SCAN_NOMEDIA_FILES] = enabled
        }
    }

    val wallpaperMode: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[WALLPAPER_MODE] ?: "auto"
    }

    suspend fun setWallpaperMode(mode: String) {
        context.dataStore.edit { preferences ->
            preferences[WALLPAPER_MODE] = mode
        }
    }

    val wallpaperBlur: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[WALLPAPER_BLUR] ?: false
    }

    suspend fun setWallpaperBlur(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[WALLPAPER_BLUR] = enabled
        }
    }

    val manualWallpaperUri: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[MANUAL_WALLPAPER_URI]
    }

    suspend fun setManualWallpaperUri(uri: String?) {
        context.dataStore.edit { preferences ->
            if (uri == null) {
                preferences.remove(MANUAL_WALLPAPER_URI)
            } else {
                preferences[MANUAL_WALLPAPER_URI] = uri
            }
        }
    }

    // Theme Extension Accessors
    val accentColor: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[ACCENT_COLOR] ?: "orange"
    }

    suspend fun setAccentColor(color: String) {
        context.dataStore.edit { preferences ->
            preferences[ACCENT_COLOR] = color
        }
    }

    val customThemeEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[CUSTOM_THEME_ENABLED] ?: false
    }

    suspend fun setCustomThemeEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[CUSTOM_THEME_ENABLED] = enabled
        }
    }

    val customBgColor: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[CUSTOM_BG_COLOR] ?: "#000000"
    }

    suspend fun setCustomBgColor(color: String) {
        context.dataStore.edit { preferences ->
            preferences[CUSTOM_BG_COLOR] = color
        }
    }

    val customTextColor: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[CUSTOM_TEXT_COLOR] ?: "#00FFFF"
    }

    suspend fun setCustomTextColor(color: String) {
        context.dataStore.edit { preferences ->
            preferences[CUSTOM_TEXT_COLOR] = color
        }
    }

    val customPrimaryColor: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[CUSTOM_PRIMARY_COLOR] ?: "#00FFFF"
    }

    suspend fun setCustomPrimaryColor(color: String) {
        context.dataStore.edit { preferences ->
            preferences[CUSTOM_PRIMARY_COLOR] = color
        }
    }

    // Eye Care Accessors
    val eyeCareEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[EYE_CARE_ENABLED] ?: false
    }

    suspend fun setEyeCareEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[EYE_CARE_ENABLED] = enabled
        }
    }

    val eyeCareIntensity: Flow<Float> = context.dataStore.data.map { preferences ->
        preferences[EYE_CARE_INTENSITY] ?: 0.5f
    }

    suspend fun setEyeCareIntensity(intensity: Float) {
        context.dataStore.edit { preferences ->
            preferences[EYE_CARE_INTENSITY] = intensity
        }
    }

    val eyeCareScheduleEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[EYE_CARE_SCHEDULE_ENABLED] ?: false
    }

    suspend fun setEyeCareScheduleEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[EYE_CARE_SCHEDULE_ENABLED] = enabled
        }
    }

    val eyeCareStartHour: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[EYE_CARE_START_HOUR] ?: 22 // 10 PM
    }

    val eyeCareStartMinute: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[EYE_CARE_START_MINUTE] ?: 0
    }

    suspend fun setEyeCareStartTime(hour: Int, minute: Int) {
        context.dataStore.edit { preferences ->
            preferences[EYE_CARE_START_HOUR] = hour
            preferences[EYE_CARE_START_MINUTE] = minute
        }
    }

    val eyeCareEndHour: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[EYE_CARE_END_HOUR] ?: 7 // 7 AM
    }

    val eyeCareEndMinute: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[EYE_CARE_END_MINUTE] ?: 0
    }

    suspend fun setEyeCareEndTime(hour: Int, minute: Int) {
        context.dataStore.edit { preferences ->
            preferences[EYE_CARE_END_HOUR] = hour
            preferences[EYE_CARE_END_MINUTE] = minute
        }
    }

    val isGridView: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[IS_GRID_VIEW] ?: false
    }

    suspend fun setGridView(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_GRID_VIEW] = enabled
        }
    }

    val autoPlayEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[AUTO_PLAY_ENABLED] ?: true
    }

    suspend fun setAutoPlayEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[AUTO_PLAY_ENABLED] = enabled
        }
    }

    val selectedLogoType: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[SELECTED_LOGO_TYPE] ?: "logo1"
    }

    suspend fun setSelectedLogoType(type: String) {
        context.dataStore.edit { preferences ->
            preferences[SELECTED_LOGO_TYPE] = type
        }
    }
}
