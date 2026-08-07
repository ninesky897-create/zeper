package com.zeper.player.music.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zeper.player.core.data.MediaFile
import com.zeper.player.core.data.MediaScanner
import com.zeper.player.core.data.PreferencesManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

import com.zeper.player.core.data.TrashManager

class MusicViewModel(application: Application) : AndroidViewModel(application) {
    private val scanner = MediaScanner(application)
    private val prefs = PreferencesManager(application)
    private val db = com.zeper.player.core.data.ZeperDatabase.getInstance(application)
    private val dao = db.mediaDao()
    private val trashManager = TrashManager(application)

    private val _favorites = dao.getAllFavorites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteUris = _favorites.map { list -> list.map { it.uri }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    private val _songs = MutableStateFlow<List<MediaFile>>(emptyList())
    val songs: StateFlow<List<MediaFile>> = _songs.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _sortOrder = MutableStateFlow("name") // "name", "date", "artist"
    val sortOrder: StateFlow<String> = _sortOrder.asStateFlow()

    init {
        observeSettingsAndScan()
    }

    fun setSortOrder(order: String) {
        _sortOrder.value = order
    }

    private fun observeSettingsAndScan() {
        viewModelScope.launch {
            combine(
                prefs.scanHiddenFiles,
                prefs.scanDotFiles,
                prefs.scanNomediaFiles,
                _sortOrder
            ) { hidden, dot, nomedia, sort ->
                DataSettings(hidden, dot, nomedia, sort)
            }.collect { settings ->
                refresh(settings)
            }
        }
    }

    data class DataSettings(
        val hidden: Boolean,
        val dot: Boolean,
        val nomedia: Boolean,
        val sort: String
    )

    fun refresh(settings: DataSettings? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            val startTime = System.currentTimeMillis()

            val h = settings?.hidden ?: prefs.scanHiddenFiles.first()
            val d = settings?.dot ?: prefs.scanDotFiles.first()
            val n = settings?.nomedia ?: prefs.scanNomediaFiles.first()
            val s = settings?.sort ?: _sortOrder.value

            var list = scanner.scanMedia(
                scanHidden = h,
                scanDot = d,
                scanNoMedia = n,
                includeAudio = true,
                includeVideo = false
            ).filter { it.type == "audio" }

            // Apply Sorting
            list = when (s) {
                "name_asc" -> list.sortedBy { it.name.lowercase() }
                "name_desc" -> list.sortedByDescending { it.name.lowercase() }
                "date_desc" -> list.sortedByDescending { it.id }
                "date_asc" -> list.sortedBy { it.id }
                "size_desc" -> list.sortedByDescending { it.size }
                "size_asc" -> list.sortedBy { it.size }
                "length_desc" -> list.sortedByDescending { it.duration }
                "length_asc" -> list.sortedBy { it.duration }
                "artist" -> list.sortedBy { (it.artist ?: "").lowercase() }
                else -> list
            }

            _songs.value = list

            val elapsedTime = System.currentTimeMillis() - startTime
            if (elapsedTime < 1200) {
                delay(1200 - elapsedTime)
            }
            _isLoading.value = false
        }
    }

    fun toggleFavorite(song: MediaFile) {
        viewModelScope.launch {
            val isFav = favoriteUris.value.contains(song.path)
            if (isFav) {
                dao.removeFavorite(com.zeper.player.core.data.FavoriteEntity(song.path, song.name, "music"))
            } else {
                dao.addFavorite(com.zeper.player.core.data.FavoriteEntity(song.path, song.name, "music"))
            }
        }
    }

    fun deleteFile(context: android.content.Context, song: MediaFile) {
        viewModelScope.launch {
            if (trashManager.moveToTrash(song)) {
                android.widget.Toast.makeText(context, "Moved to Trash Bin", android.widget.Toast.LENGTH_SHORT).show()
                refresh()
            } else {
                android.widget.Toast.makeText(context, "Failed to move to Trash", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Derived views for UI
    val folders = _songs.map { list ->
        list.groupBy { it.folder ?: "Internal Storage" }
            .mapValues { (_, songs) -> songs.sortedBy { it.name.lowercase() } }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val artists = _songs.map { list ->
        list.groupBy { it.artist ?: "Unknown Artist" }
            .mapValues { (_, songs) -> songs.sortedBy { it.name.lowercase() } }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val albums = _songs.map { list ->
        list.groupBy { it.album ?: "Unknown Album" }
            .mapValues { (_, songs) -> songs.sortedBy { it.name.lowercase() } }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())
}
