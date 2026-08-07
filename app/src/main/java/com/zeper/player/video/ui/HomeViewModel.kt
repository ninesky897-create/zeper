package com.zeper.player.video.ui

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zeper.player.core.data.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * HomeViewModel — MVVM ViewModel for the Home Screen.
 *
 * Now connected to MediaScanner for real data.
 */
import com.zeper.player.core.data.TrashManager

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val scanner = MediaScanner(application)
    private val prefs = PreferencesManager(application)
    private val db = ZeperDatabase.getInstance(application)
    private val dao = db.mediaDao()
    private val trashManager = TrashManager(application)
    private val vaultManager = VaultManager(application)

    // ── Category Selection ──────────────────────────────────────
    val categories = listOf("FM", "Internet", "All Videos", "ZPR Share", "Favorites", "Recent", "Privacy")

    private val _selectedCategory = MutableStateFlow("All Videos")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _isShowingAllFiles = MutableStateFlow(false)
    val isShowingAllFiles: StateFlow<Boolean> = _isShowingAllFiles.asStateFlow()

    private val _sortOrder = MutableStateFlow("date_desc") // Default to newest first
    val sortOrder: StateFlow<String> = _sortOrder.asStateFlow()

    fun selectCategory(category: String) {
        if (category == "All Videos" || category == "All Files") {
            if (_selectedCategory.value == "All Videos" || _selectedCategory.value == "All Files") {
                _isShowingAllFiles.value = !_isShowingAllFiles.value
            } else {
                _isShowingAllFiles.value = false
            }
            _selectedCategory.value = if (_isShowingAllFiles.value) "All Files" else "All Videos"
        } else {
            _selectedCategory.value = category
            _isShowingAllFiles.value = false
        }
    }

    fun setSortOrder(order: String) {
        _sortOrder.value = order
        refreshData()
    }

    // ── Real Data Flows ─────────────────────────────────────────
    private val _allVideos = MutableStateFlow<List<MediaFile>>(emptyList())
    val allVideos: StateFlow<List<MediaFile>> = _allVideos.asStateFlow()

    private val _folders = MutableStateFlow<List<DummyFolder>>(emptyList())
    val folders: StateFlow<List<DummyFolder>> = _folders.asStateFlow()

    private val _historyItems = MutableStateFlow<List<DummyHistoryItem>>(emptyList())
    val historyItems: StateFlow<List<DummyHistoryItem>> = _historyItems.asStateFlow()

    private val _lastPlayedFolder = MutableStateFlow<String?>(null)
    val lastPlayedFolder: StateFlow<String?> = _lastPlayedFolder.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _favorites = dao.getAllFavorites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteUris = _favorites.map { list -> list.map { it.uri }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val favoriteVideos = combine(_allVideos, _favorites) { videos, favEntities ->
        val videoFavs = favEntities.filter { it.type == "video" }
        videoFavs.map { entity ->
            videos.find { it.path == entity.uri } ?: MediaFile(
                id = 0,
                name = entity.title,
                path = entity.uri,
                contentUri = entity.uri,
                duration = 0,
                size = 0,
                type = "video",
                folder = "Favorite"
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        refreshData()
        observeHistory()
    }

    fun toggleFavorite(path: String, name: String) {
        viewModelScope.launch {
            val isFav = favoriteUris.value.contains(path)
            if (isFav) {
                dao.removeFavorite(FavoriteEntity(path, name, "video"))
            } else {
                dao.addFavorite(FavoriteEntity(path, name, "video"))
            }
        }
    }

    fun toggleFavorite(video: MediaFile) {
        toggleFavorite(video.path, video.name)
    }

    fun deleteFile(context: android.content.Context, video: MediaFile) {
        viewModelScope.launch {
            if (trashManager.moveToTrash(video)) {
                Toast.makeText(context, "Moved to Trash Bin", Toast.LENGTH_SHORT).show()
                refreshData()
            } else {
                try {
                    // Modern Android deletion logic fallback
                    val uri = android.net.Uri.parse(video.contentUri)
                    val deleted = context.contentResolver.delete(uri, null, null)
                    if (deleted > 0) {
                        Toast.makeText(context, "File deleted", Toast.LENGTH_SHORT).show()
                        refreshData()
                    } else {
                        // Try direct file deletion if MediaStore delete didn't report success
                        val file = java.io.File(video.path)
                        if (file.exists() && file.delete()) {
                            Toast.makeText(context, "File deleted", Toast.LENGTH_SHORT).show()
                            refreshData()
                        } else {
                            Toast.makeText(context, "Delete failed (Permission denied)", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Delete failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun moveToVault(context: android.content.Context, video: MediaFile) {
        viewModelScope.launch {
            if (vaultManager.moveToVault(video)) {
                Toast.makeText(context, "Moved to Privacy Vault", Toast.LENGTH_SHORT).show()
                refreshData()
            } else {
                Toast.makeText(context, "Failed to move to Vault", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun observeHistory() {
        viewModelScope.launch {
            combine(dao.getHistory("video"), _allVideos) { historyEntities, videos ->
                historyEntities.map { entity ->
                    val video = videos.find { it.path == entity.uri }
                    if (video != null) {
                        DummyHistoryItem(
                            video = DummyVideo(
                                id = video.id,
                                name = video.name,
                                durationMs = video.duration,
                                sizeBytes = video.size,
                                folderName = video.folder ?: "Internal Storage",
                                path = video.path
                            ),
                            progressPercent = if (video.duration > 0) entity.lastPosition.toFloat() / video.duration else 0f
                        )
                    } else {
                        // Fallback to database info if video not in current scan
                        DummyHistoryItem(
                            video = DummyVideo(
                                id = 0,
                                name = entity.title,
                                durationMs = entity.duration,
                                sizeBytes = 0,
                                folderName = entity.folder ?: "Unknown",
                                path = entity.uri
                            ),
                            progressPercent = if (entity.duration > 0) entity.lastPosition.toFloat() / entity.duration else 0f
                        )
                    }
                }
            }.collect {
                _historyItems.value = it
            }
        }
    }

    fun refreshData() {
        viewModelScope.launch {
            _isLoading.value = true
            val startTime = System.currentTimeMillis()
            
            // Collect current preferences
            val scanHidden = prefs.scanHiddenFiles.first()
            val scanDot = prefs.scanDotFiles.first()
            val scanNoMedia = prefs.scanNomediaFiles.first()
            val hiddenFolders = prefs.hiddenFolders.first()
            
            // Scan only videos for the Video Tab to improve speed
            val rawVideos = scanner.scanMedia(
                scanHidden = scanHidden,
                scanDot = scanDot,
                scanNoMedia = scanNoMedia,
                hiddenFolders = hiddenFolders,
                includeAudio = false,
                includeVideo = true
            )
            
            val threeDaysAgo = System.currentTimeMillis() / 1000 - (3 * 24 * 60 * 60)
            val historyList = dao.getHistory("video").first()

            // Process on Default dispatcher to keep UI responsive
            val processedData = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                val videos = rawVideos.map { video ->
                    val hasBeenPlayed = historyList.any { it.uri == video.path && it.lastPosition > 0 }
                    val isWithin3Days = video.dateAdded > threeDaysAgo
                    video.copy(isNew = isWithin3Days && !hasBeenPlayed)
                }

                // Apply Sorting
                val sortedVideos = when (_sortOrder.value) {
                    "name_asc" -> videos.sortedBy { it.name.lowercase() }
                    "name_desc" -> videos.sortedByDescending { it.name.lowercase() }
                    "date_desc" -> videos.sortedByDescending { it.dateAdded }
                    "date_asc" -> videos.sortedBy { it.dateAdded }
                    "size_desc" -> videos.sortedByDescending { it.size }
                    "size_asc" -> videos.sortedBy { it.size }
                    "length_desc" -> videos.sortedByDescending { it.duration }
                    "length_asc" -> videos.sortedBy { it.duration }
                    else -> videos.sortedByDescending { it.dateAdded }
                }

                // Group by Folder
                val folderMap = sortedVideos.groupBy { it.folder ?: "Internal Storage" }.toMutableMap()
                
                val foldersList = folderMap.map { (name, list) ->
                    val firstPath = list.firstOrNull()?.path ?: ""
                    val isExternal = !firstPath.startsWith("/storage/emulated/0") && firstPath.startsWith("/storage/")
                    DummyFolder(name = name, videoCount = list.size, isExternal = isExternal)
                }.toMutableList()

                // Add virtual "Recent Added" folder
                val recentVideos = videos.filter { it.dateAdded > threeDaysAgo }
                if (recentVideos.isNotEmpty()) {
                    foldersList.add(0, DummyFolder(
                        name = "Recent Added", 
                        videoCount = recentVideos.size, 
                        isExternal = false,
                        subtitle = "${recentVideos.size} videos added"
                    ))
                }

                sortedVideos to foldersList.sortedBy { if (it.name == "Recent Added") "" else it.name }
            }

            _allVideos.value = processedData.first
            _folders.value = processedData.second

            // Artificial delay reduced to fix "laggy" feel
            val elapsedTime = System.currentTimeMillis() - startTime
            if (elapsedTime < 300) {
                delay(300 - elapsedTime)
            }

            _isLoading.value = false
        }
    }

    val folderCount: Int get() = _folders.value.size
    val totalVideoCount: Int get() = _allVideos.value.size
}
