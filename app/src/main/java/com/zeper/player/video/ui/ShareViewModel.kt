package com.zeper.player.video.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.yausername.youtubedl_android.YoutubeDL
import com.zeper.player.ZeperApp
import com.zeper.player.core.data.DownloadManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ShareViewModel(application: Application) : AndroidViewModel(application) {
    private val downloadManager = DownloadManager(application)
    
    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private var currentUrl: String? = null
    private var videoTitle: String = "Video"

    fun fetchInfo(url: String) {
        currentUrl = url
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                // Ensure initialized
                var retries = 0
                while (!ZeperApp.isInitialized && retries < 20) {
                    kotlinx.coroutines.delay(1000)
                    retries++
                }

                if (!ZeperApp.isInitialized) {
                    throw IllegalStateException("Video Downloader engine failed to start. Please restart the app.")
                }

                val info = withContext(Dispatchers.IO) {
                    YoutubeDL.getInstance().getInfo(url)
                }
                videoTitle = info.title ?: "Video"
                
                val videoOptions = listOf(
                    QualityOption("144p", "approx. 5MB", "144"),
                    QualityOption("360p", "approx. 15MB", "360"),
                    QualityOption("720p HD", "approx. 50MB", "720"),
                    QualityOption("1080p HD", "approx. 120MB", "1080")
                )
                
                val audioOptions = listOf(
                    QualityOption("MP3 128K", "approx. 4MB", "128"),
                    QualityOption("MP3 320K", "approx. 10MB", "320")
                )
                
                _uiState.value = UiState.Success(
                    title = videoTitle,
                    thumbnail = info.thumbnail,
                    videoOptions = videoOptions,
                    audioOptions = audioOptions
                )
            } catch (e: Throwable) {
                _uiState.value = UiState.Error(e.message ?: e.toString())
            }
        }
    }

    fun startDownload(option: QualityOption, type: String) {
        val url = currentUrl ?: return
        viewModelScope.launch {
            val fileName = if (type == "audio") "${videoTitle}_${System.currentTimeMillis()}.mp3" else "${videoTitle}_${System.currentTimeMillis()}.mp4"
            downloadManager.startDownload(
                url = url,
                fileName = fileName,
                type = type,
                resolution = if (type == "video") option.label else null,
                format = if (type == "audio") "MP3" else "MP4"
            )
        }
    }

    data class QualityOption(val label: String, val size: String, val value: String)

    sealed class UiState {
        object Loading : UiState()
        data class Success(
            val title: String,
            val thumbnail: String?,
            val videoOptions: List<QualityOption>,
            val audioOptions: List<QualityOption>
        ) : UiState()
        data class Error(val message: String) : UiState()
    }
}
