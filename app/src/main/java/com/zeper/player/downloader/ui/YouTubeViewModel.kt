package com.zeper.player.downloader.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zeper.player.downloader.data.local.YouTubeDatabase
import com.zeper.player.downloader.data.model.YouTubeSearchItem
import com.zeper.player.downloader.data.remote.NetworkModule
import com.zeper.player.downloader.data.repository.YouTubeRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class YouTubeUiState {
    object Idle : YouTubeUiState()
    object Loading : YouTubeUiState()
    data class Success(val videos: List<YouTubeSearchItem>, val nextPageToken: String?) : YouTubeUiState()
    data class Error(val message: String) : YouTubeUiState()
}

class YouTubeViewModel(application: Application) : AndroidViewModel(application) {

    private val db = YouTubeDatabase.getDatabase(application)
    private val repository: YouTubeRepository = YouTubeRepository(
        NetworkModule.youtubeService,
        db.searchHistoryDao(),
        db.youtubeDao()
    )

    private val _uiState = MutableStateFlow<YouTubeUiState>(YouTubeUiState.Idle)
    val uiState: StateFlow<YouTubeUiState> = _uiState.asStateFlow()

    private val _musicUiState = MutableStateFlow<YouTubeUiState>(YouTubeUiState.Idle)
    val musicUiState: StateFlow<YouTubeUiState> = _musicUiState.asStateFlow()

    val searchHistory = repository.getRecentSearches().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    private var currentQuery = ""
    private var currentMusicQuery = ""

    private val _suggestions = MutableStateFlow<List<String>>(emptyList())
    val suggestions: StateFlow<List<String>> = _suggestions.asStateFlow()

    fun updateSuggestions(query: String) {
        if (query.isEmpty()) {
            _suggestions.value = emptyList()
            return
        }
        val history = searchHistory.value.map { it.query }
        val trending = listOf("New Songs 2024", "Movie Trailers", "Cricket Highlights", "Tech Reviews", "ASMR")
        _suggestions.value = (history + trending).filter { it.contains(query, ignoreCase = true) }.distinct().take(5)
    }

    fun searchVideos(query: String) {
        val searchQuery = if (query.isEmpty()) "Trending music 2024" else query
        if (searchQuery == currentQuery && _uiState.value is YouTubeUiState.Success) return
        currentQuery = searchQuery
        viewModelScope.launch {
            _uiState.value = YouTubeUiState.Loading
            try {
                val response = repository.searchVideos(searchQuery)
                if (response.isSuccessful) {
                    val body = response.body()
                    _uiState.value = YouTubeUiState.Success(body?.items ?: emptyList(), body?.nextPageToken)
                    if (query.isNotEmpty()) repository.insertSearch(query)
                } else {
                    handleError(response.code(), _uiState)
                }
            } catch (e: Exception) {
                _uiState.value = YouTubeUiState.Error("No Internet Connection")
            }
        }
    }

    fun searchMusic(query: String) {
        if (query == currentMusicQuery && _musicUiState.value is YouTubeUiState.Success) return
        currentMusicQuery = query
        viewModelScope.launch {
            _musicUiState.value = YouTubeUiState.Loading
            try {
                val response = repository.searchMusic(query)
                if (response.isSuccessful) {
                    val body = response.body()
                    _musicUiState.value = YouTubeUiState.Success(body?.items ?: emptyList(), body?.nextPageToken)
                } else {
                    handleError(response.code(), _musicUiState)
                }
            } catch (e: Exception) {
                _musicUiState.value = YouTubeUiState.Error("No Internet Connection")
            }
        }
    }

    private fun handleError(code: Int, stateFlow: MutableStateFlow<YouTubeUiState>) {
        val message = when (code) {
            403 -> "Quota Exceeded or Invalid API Key"
            404 -> "Not Found"
            in 500..599 -> "Server Error"
            else -> "Unexpected Error (Code: $code)"
        }
        stateFlow.value = YouTubeUiState.Error(message)
    }

    fun deleteSearch(query: String) {
        viewModelScope.launch {
            repository.deleteSearch(query)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }
    
    // Placeholder for Trending
    val trendingSearches = flowOf(listOf("New Songs 2024", "Movie Trailers", "Cricket Highlights", "Tech Reviews", "ASMR"))
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val likedVideos = repository.getLikedVideos().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleLike(video: YouTubeSearchItem) {
        viewModelScope.launch {
            repository.insertLiked(video)
        }
    }
}
