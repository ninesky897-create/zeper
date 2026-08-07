package com.zeper.player.video.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zeper.player.core.data.MediaFile
import com.zeper.player.core.data.MediaScanner
import com.zeper.player.core.data.PreferencesManager
import com.zeper.player.core.data.ZeperDatabase
import com.zeper.player.core.data.HistoryEntity
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

import com.zeper.player.core.data.TrashManager

class VideoViewModel(application: Application) : AndroidViewModel(application) {
    private val scanner = MediaScanner(application)
    private val prefs = PreferencesManager(application)
    private val db = ZeperDatabase.getInstance(application)
    private val dao = db.mediaDao()
    private val trashManager = TrashManager(application)
    private val vaultManager = com.zeper.player.core.data.VaultManager(application)

    private val _favorites = dao.getAllFavorites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteUris = _favorites.map { list -> list.map { it.uri }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    private val _videos = MutableStateFlow<List<MediaFile>>(emptyList())
    val videos: StateFlow<List<MediaFile>> = _videos.asStateFlow()

    private val _lockedVideos = MutableStateFlow<List<MediaFile>>(emptyList())
    val lockedVideos: StateFlow<List<MediaFile>> = _lockedVideos.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _sortOrder = MutableStateFlow("date") // "name", "date", "size"
    val sortOrder: StateFlow<String> = _sortOrder.asStateFlow()

    val history = dao.getHistory("video")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        observeSettingsAndScan()
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
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
                _searchQuery,
                _sortOrder,
                prefs.hiddenFolders,
                prefs.lockedFolders
            ) { args: Array<Any> ->
                DataSettings(
                    hidden = args[0] as Boolean,
                    dot = args[1] as Boolean,
                    nomedia = args[2] as Boolean,
                    query = args[3] as String,
                    sort = args[4] as String,
                    hiddenFolders = args[5] as List<String>,
                    lockedFolders = args[6] as List<String>
                )
            }.collect { settings ->
                refresh(settings)
            }
        }
    }

    data class DataSettings(
        val hidden: Boolean,
        val dot: Boolean,
        val nomedia: Boolean,
        val query: String,
        val sort: String,
        val hiddenFolders: List<String>,
        val lockedFolders: List<String>
    )

    fun refresh(settings: DataSettings? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            val startTime = System.currentTimeMillis()
            
            val h = settings?.hidden ?: prefs.scanHiddenFiles.first()
            val d = settings?.dot ?: prefs.scanDotFiles.first()
            val n = settings?.nomedia ?: prefs.scanNomediaFiles.first()
            val q = settings?.query ?: _searchQuery.value
            val s = settings?.sort ?: _sortOrder.value
            val hf = settings?.hiddenFolders ?: prefs.hiddenFolders.first()
            val lf = settings?.lockedFolders ?: prefs.lockedFolders.first()

            val rawVideos = scanner.scanMedia(
                scanHidden = h,
                scanDot = d,
                scanNoMedia = n,
                includeAudio = false,
                includeVideo = true
            )
            
            val threeDaysAgo = System.currentTimeMillis() / 1000 - (3 * 24 * 60 * 60)
            val historyList = dao.getHistory("video").first()

            val processedData = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                val videos = rawVideos.map { video ->
                    val hasBeenPlayed = historyList.any { it.uri == video.path && it.lastPosition > 0 }
                    val isWithin3Days = video.dateAdded > threeDaysAgo
                    video.copy(isNew = isWithin3Days && !hasBeenPlayed)
                }
                
                var list = videos
                
                // Apply Folder Hiding
                if (hf.isNotEmpty()) {
                    list = list.filter { !hf.contains(it.folder ?: "Internal Storage") }
                }

                // Apply Folder Locking
                if (lf.isNotEmpty()) {
                    list = list.filter { !lf.contains(it.folder ?: "Internal Storage") }
                }
                
                // Apply Search
                if (q.isNotEmpty()) {
                    list = list.filter { it.name.contains(q, ignoreCase = true) }
                }

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
                    else -> list
                }

                // Locked Videos
                val lockedList = if (lf.isNotEmpty()) {
                    videos.filter { lf.contains(it.folder ?: "Internal Storage") }
                } else {
                    emptyList()
                }

                list to lockedList
            }

            _videos.value = processedData.first
            _lockedVideos.value = processedData.second

            val elapsedTime = System.currentTimeMillis() - startTime
            if (elapsedTime < 300) {
                delay(300 - elapsedTime)
            }
            _isLoading.value = false
        }
    }

    fun toggleFavorite(path: String, name: String) {
        viewModelScope.launch {
            val isFav = favoriteUris.value.contains(path)
            if (isFav) {
                dao.removeFavorite(com.zeper.player.core.data.FavoriteEntity(path, name, "video"))
            } else {
                dao.addFavorite(com.zeper.player.core.data.FavoriteEntity(path, name, "video"))
            }
        }
    }

    fun toggleFavorite(video: MediaFile) {
        toggleFavorite(video.path, video.name)
    }

    fun deleteFile(context: android.content.Context, video: MediaFile) {
        viewModelScope.launch {
            if (trashManager.moveToTrash(video)) {
                android.widget.Toast.makeText(context, "Moved to Trash Bin", android.widget.Toast.LENGTH_SHORT).show()
                refresh()
            } else {
                try {
                    val uri = android.net.Uri.parse(video.contentUri)
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                        val pendingIntent = android.provider.MediaStore.createDeleteRequest(
                            context.contentResolver, 
                            listOf(uri)
                        )
                        if (context is android.app.Activity) {
                            context.startIntentSenderForResult(
                                pendingIntent.intentSender, 
                                1001, 
                                null, 0, 0, 0
                            )
                        }
                    } else {
                        context.contentResolver.delete(uri, null, null)
                        refresh()
                    }
                } catch (e: Exception) {
                    android.widget.Toast.makeText(context, "Delete failed: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun moveToVault(context: android.content.Context, video: MediaFile) {
        viewModelScope.launch {
            if (vaultManager.moveToVault(video)) {
                android.widget.Toast.makeText(context, "Moved to Privacy Vault", android.widget.Toast.LENGTH_SHORT).show()
                refresh()
            } else {
                android.widget.Toast.makeText(context, "Failed to move to Vault", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun updateProgress(path: String, title: String, position: Long, duration: Long = 0, folder: String? = null) {
        viewModelScope.launch {
            dao.addToHistory(
                HistoryEntity(
                    uri = path, 
                    title = title, 
                    timestamp = System.currentTimeMillis(), 
                    type = "video", 
                    lastPosition = position,
                    duration = duration,
                    folder = folder
                )
            )
        }
    }

    suspend fun getSavedPosition(uri: String): Long {
        return dao.getHistory("video").first().find { it.uri == uri }?.lastPosition ?: 0L
    }
}
