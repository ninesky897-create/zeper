package com.zeper.player.downloader.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zeper.player.core.data.*
import com.zeper.player.downloader.data.UniversalDownloader
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class DownloaderViewModel(application: Application) : AndroidViewModel(application) {
    private val db = ZeperDatabase.getInstance(application)
    private val dao = db.mediaDao()
    private val manager = DownloadManager(application)
    private val universalDownloader = UniversalDownloader(application, dao)
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()
    
    private val _statusFilter = MutableStateFlow<DownloadStatus?>(null)
    val statusFilter = _statusFilter.asStateFlow()

    private val _sortOrder = MutableStateFlow("date_desc")
    val sortOrder = _sortOrder.asStateFlow()

    val downloads: StateFlow<List<DownloadEntity>> = combine(
        manager.getAllDownloads(),
        _searchQuery,
        _statusFilter,
        _sortOrder
    ) { list, query, status, sort ->
        var filteredList = list
        
        if (status != null) {
            filteredList = filteredList.filter { it.status == status }
        }
        
        if (query.isNotEmpty()) {
            filteredList = filteredList.filter { 
                it.fileName.contains(query, ignoreCase = true) || it.format.contains(query, ignoreCase = true)
            }
        }
        
        when (sort) {
            "name_asc" -> filteredList.sortedBy { it.fileName.lowercase() }
            "size_desc" -> filteredList.sortedByDescending { it.totalSize }
            "date_desc" -> filteredList.sortedByDescending { it.timestamp }
            else -> filteredList.sortedByDescending { it.timestamp }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFilter(status: DownloadStatus?) {
        _statusFilter.value = status
    }

    fun setSortOrder(order: String) {
        _sortOrder.value = order
    }

    fun startDownload(url: String, isVideo: Boolean, resolution: String? = null) {
        val extension = if (isVideo) ".mp4" else ".mp3"
        val type = if (isVideo) "video" else "audio"
        val fileName = "Zeper_${System.currentTimeMillis()}$extension"
        
        universalDownloader.enqueueDownload(url, fileName, type)
    }

    fun pauseDownload(id: String) {
        universalDownloader.pauseDownload(id)
    }

    fun resumeDownload(id: String) {
        // For System DownloadManager, we might just re-enqueue or the system handles it.
        // For now, let's treat it as start if it was paused.
        // But system DownloadManager doesn't easily expose "resume" for a specific ID if manually stopped.
    }

    fun cancelDownload(id: String) {
        universalDownloader.cancelDownload(id)
    }

    fun deleteDownload(id: String) {
        viewModelScope.launch { manager.deleteDownload(id) }
    }

    fun retryDownload(id: String) {
        viewModelScope.launch { manager.retryDownload(id) }
    }

    fun renameDownload(id: String, newName: String) {
        viewModelScope.launch { manager.renameDownload(id, newName) }
    }

    val sampleVideos = listOf(
        "Bunny" to "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
        "Elephants" to "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
        "Sintel" to "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4"
    )
}
