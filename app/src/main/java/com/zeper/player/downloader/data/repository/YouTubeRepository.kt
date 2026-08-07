package com.zeper.player.downloader.data.repository

import com.zeper.player.BuildConfig
import com.zeper.player.downloader.data.local.YouTubeDao
import com.zeper.player.downloader.data.local.LikedVideoEntity
import com.zeper.player.downloader.data.local.SearchHistoryDao
import com.zeper.player.downloader.data.local.SearchHistoryEntity
import com.zeper.player.downloader.data.model.*
import com.zeper.player.downloader.data.remote.YouTubeApiService
import kotlinx.coroutines.flow.Flow
import retrofit2.Response

class YouTubeRepository(
    private val apiService: YouTubeApiService,
    private val searchHistoryDao: SearchHistoryDao,
    private val youtubeDao: YouTubeDao
) {
    private val apiKey = BuildConfig.YOUTUBE_API_KEY

    suspend fun getTrendingVideos(region: String = "US", categoryId: String? = null): Response<YouTubeVideoResponse> {
        return apiService.getTrendingVideos(regionCode = region, categoryId = categoryId, apiKey = apiKey)
    }

    suspend fun insertLiked(video: YouTubeSearchItem) {
        youtubeDao.insertLiked(LikedVideoEntity(
            videoId = video.id.videoId ?: "",
            title = video.snippet.title,
            thumbnail = video.snippet.thumbnails.high.url,
            channelTitle = video.snippet.channelTitle
        ))
    }

    fun getLikedVideos() = youtubeDao.getLikedVideos()

    suspend fun searchVideos(query: String, pageToken: String? = null): Response<YouTubeSearchResponse> {
        return apiService.searchVideos(query = query, pageToken = pageToken, apiKey = apiKey)
    }

    suspend fun searchMusic(query: String, pageToken: String? = null): Response<YouTubeSearchResponse> {
        // Category 10 is Music
        return apiService.searchVideos(query = query, categoryId = "10", pageToken = pageToken, apiKey = apiKey)
    }

    suspend fun getVideoDetails(videoId: String): Response<YouTubeVideoResponse> {
        return apiService.getVideoDetails(videoId = videoId, apiKey = apiKey)
    }

    suspend fun getChannelDetails(channelId: String): Response<YouTubeChannelResponse> {
        return apiService.getChannelDetails(channelId = channelId, apiKey = apiKey)
    }

    fun getRecentSearches(): Flow<List<SearchHistoryEntity>> {
        return searchHistoryDao.getRecentSearches()
    }

    suspend fun insertSearch(query: String) {
        searchHistoryDao.insertSearch(SearchHistoryEntity(query))
    }

    suspend fun deleteSearch(query: String) {
        searchHistoryDao.deleteSearch(query)
    }

    suspend fun clearHistory() {
        searchHistoryDao.clearHistory()
    }
}
