package com.zeper.player.downloader.data.remote

import com.zeper.player.downloader.data.model.YouTubeChannelResponse
import com.zeper.player.downloader.data.model.YouTubeSearchResponse
import com.zeper.player.downloader.data.model.YouTubeVideoResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface YouTubeApiService {

    @GET("search")
    suspend fun searchVideos(
        @Query("part") part: String = "snippet",
        @Query("q") query: String,
        @Query("type") type: String = "video",
        @Query("maxResults") maxResults: Int = 20,
        @Query("pageToken") pageToken: String? = null,
        @Query("videoCategoryId") categoryId: String? = null,
        @Query("key") apiKey: String
    ): Response<YouTubeSearchResponse>

    @GET("videos")
    suspend fun getTrendingVideos(
        @Query("part") part: String = "snippet,contentDetails,statistics",
        @Query("chart") chart: String = "mostPopular",
        @Query("regionCode") regionCode: String = "US",
        @Query("videoCategoryId") categoryId: String? = null,
        @Query("key") apiKey: String
    ): Response<YouTubeVideoResponse>

    @GET("videos")
    suspend fun getVideoDetails(
        @Query("part") part: String = "snippet,contentDetails,statistics",
        @Query("id") videoId: String,
        @Query("key") apiKey: String
    ): Response<YouTubeVideoResponse>

    @GET("channels")
    suspend fun getChannelDetails(
        @Query("part") part: String = "snippet,statistics",
        @Query("id") channelId: String,
        @Query("key") apiKey: String
    ): Response<YouTubeChannelResponse>

    companion object {
        const val BASE_URL = "https://www.googleapis.com/youtube/v3/"
    }
}
