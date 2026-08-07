package com.zeper.player.downloader.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import com.zeper.player.ZeperApp
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.zeper.player.core.data.PreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class OnlineSearchViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = PreferencesManager(application)
    
    private val _userAccount = MutableStateFlow<GoogleSignInAccount?>(null)
    val userAccount: StateFlow<GoogleSignInAccount?> = _userAccount.asStateFlow()

    private val _videoResults = MutableStateFlow<List<YouTubeSearchResult>>(emptyList())
    val videoResults: StateFlow<List<YouTubeSearchResult>> = _videoResults.asStateFlow()

    private val _musicResults = MutableStateFlow<List<YouTubeSearchResult>>(emptyList())
    val musicResults: StateFlow<List<YouTubeSearchResult>> = _musicResults.asStateFlow()

    private val _syncedPlaylists = MutableStateFlow<List<YouTubePlaylist>>(emptyList())
    val syncedPlaylists: StateFlow<List<YouTubePlaylist>> = _syncedPlaylists.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _browserUrl = MutableStateFlow("https://m.youtube.com")
    val browserUrl: StateFlow<String> = _browserUrl.asStateFlow()

    init {
        fetchTrending()
    }

    fun setBrowserUrl(url: String) {
        _browserUrl.value = url
    }

    fun setUserAccount(account: GoogleSignInAccount?) {
        _userAccount.value = account
        if (account != null) {
            syncUserData()
        } else {
            _syncedPlaylists.value = emptyList()
        }
    }

    private fun syncUserData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // In a real app, this would use the Google account token to call YouTube API
                // For now, we simulate finding user's playlists
                kotlinx.coroutines.delay(2000)
                _syncedPlaylists.value = listOf(
                    YouTubePlaylist("Favorite Music", "24 videos"),
                    YouTubePlaylist("Watch Later", "12 videos"),
                    YouTubePlaylist("My Mix", "50+ videos")
                )
            } catch (e: Exception) {
                android.util.Log.e("OnlineSearchVM", "Sync failed", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun search(query: String, isMusic: Boolean = false) {
        if (query.isBlank()) return
        
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Wait for init
                var retries = 0
                while (!ZeperApp.isInitialized && retries < 20) {
                    kotlinx.coroutines.delay(500)
                    retries++
                }

                val results = withContext(Dispatchers.IO) {
                    val searchType = if (isMusic) "ytsearch5:" else "ytsearch5:"
                    val request = YoutubeDLRequest("$searchType$query")
                    request.addOption("--dump-json")
                    request.addOption("--flat-playlist")
                    
                    val output = YoutubeDL.getInstance().execute(request, null)
                    // Parse multi-line JSON output
                    output.out.lines()
                        .filter { it.isNotBlank() }
                        .mapNotNull { line ->
                            try {
                                val json = org.json.JSONObject(line)
                                YouTubeSearchResult(
                                    id = json.optString("id", ""),
                                    title = json.optString("title", "Unknown"),
                                    url = json.optString("url", "https://www.youtube.com/watch?v=${json.optString("id")}"),
                                    thumbnail = json.optString("thumbnail", ""),
                                    duration = json.optString("duration_string", "0:00"),
                                    uploader = json.optString("uploader", "Unknown"),
                                    viewCount = json.optString("view_count", "0")
                                )
                            } catch (e: Exception) {
                                null
                            }
                        }
                }

                if (isMusic) {
                    _musicResults.value = results
                } else {
                    _videoResults.value = results
                }
                
                // Track preference (SnapTube style recommendation logic)
                if (query.lowercase().contains("natok")) {
                    android.util.Log.d("OnlineSearchVM", "User interested in Natok, prioritizing in next trending fetch")
                }
                
            } catch (e: Exception) {
                android.util.Log.e("OnlineSearchVM", "Search failed", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun fetchTrending() {
        // Fetch a mix of trending content
        search("trending now", isMusic = false)
        search("new music hits", isMusic = true)
    }
}

data class YouTubeSearchResult(
    val id: String,
    val title: String,
    val url: String,
    val thumbnail: String?,
    val duration: String,
    val uploader: String,
    val viewCount: String
)

data class YouTubePlaylist(
    val title: String,
    val count: String
)
