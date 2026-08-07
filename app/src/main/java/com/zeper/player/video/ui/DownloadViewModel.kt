package com.zeper.player.video.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zeper.player.core.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class DownloadViewModel(application: Application) : AndroidViewModel(application) {
    private val manager = DownloadManager(application)
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()
    
    private val _statusFilter = MutableStateFlow<DownloadStatus?>(null)
    val statusFilter = _statusFilter.asStateFlow()

    private val _sortOrder = MutableStateFlow("date_desc") // "name_asc", "size_desc", "date_desc"
    val sortOrder = _sortOrder.asStateFlow()

    val downloads: StateFlow<List<DownloadEntity>> = combine(
        manager.getAllDownloads(),
        _searchQuery,
        _statusFilter,
        _sortOrder
    ) { list, query, status, sort ->
        var filteredList = list
        
        // Filter by Status
        if (status != null) {
            filteredList = filteredList.filter { it.status == status }
        }
        
        // Filter by Search Query (Name or Format)
        if (query.isNotEmpty()) {
            filteredList = filteredList.filter { 
                it.fileName.contains(query, ignoreCase = true) || it.format.contains(query, ignoreCase = true)
            }
        }
        
        // Apply Sorting
        when (sort) {
            "name_asc" -> filteredList.sortedBy { it.fileName.lowercase() }
            "name_desc" -> filteredList.sortedByDescending { it.fileName.lowercase() }
            "size_desc" -> filteredList.sortedByDescending { it.totalSize }
            "size_asc" -> filteredList.sortedBy { it.totalSize }
            "date_desc" -> filteredList.sortedByDescending { it.timestamp }
            "date_asc" -> filteredList.sortedBy { it.timestamp }
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

    fun pauseDownload(id: String) {
        viewModelScope.launch { manager.pauseDownload(id) }
    }

    fun resumeDownload(id: String) {
        viewModelScope.launch { manager.resumeDownload(id) }
    }

    fun cancelDownload(id: String) {
        viewModelScope.launch { manager.cancelDownload(id) }
    }

    fun deleteDownload(id: String) {
        viewModelScope.launch { manager.deleteDownload(id) }
    }

    fun retryDownload(id: String) {
        viewModelScope.launch { manager.retryDownload(id) }
    }
    
    fun startDummyDownload() {
        viewModelScope.launch {
            manager.startDownload(
                url = "https://www.learningcontainer.com/wp-content/uploads/2020/05/sample-mp4-file.mp4",
                fileName = "Sample Video ${System.currentTimeMillis()}.mp4",
                type = "video",
                resolution = "720p",
                format = "MP4"
            )
        }
    }
}
