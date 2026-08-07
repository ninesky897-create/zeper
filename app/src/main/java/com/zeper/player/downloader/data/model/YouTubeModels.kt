package com.zeper.player.downloader.data.model

import com.google.gson.annotations.SerializedName

data class YouTubeSearchResponse(
    val items: List<YouTubeSearchItem>?,
    val nextPageToken: String?
)

data class YouTubeSearchItem(
    val id: YouTubeId,
    val snippet: YouTubeSnippet
)

data class YouTubeId(
    val kind: String,
    val videoId: String?,
    val channelId: String?,
    val playlistId: String?
)

data class YouTubeSnippet(
    val publishedAt: String,
    val channelId: String,
    val title: String,
    val description: String,
    val thumbnails: YouTubeThumbnails,
    val channelTitle: String,
    val liveBroadcastContent: String?
)

data class YouTubeThumbnails(
    val default: ThumbnailDetails,
    val medium: ThumbnailDetails,
    val high: ThumbnailDetails
)

data class ThumbnailDetails(
    val url: String,
    val width: Int,
    val height: Int
)

data class YouTubeVideoResponse(
    val items: List<YouTubeVideoItem>?
)

data class YouTubeVideoItem(
    val id: String,
    val snippet: YouTubeSnippet,
    val contentDetails: YouTubeContentDetails?,
    val statistics: YouTubeStatistics?
)

data class YouTubeContentDetails(
    val duration: String,
    val dimension: String,
    val definition: String,
    val caption: String,
    val licensedContent: Boolean
)

data class YouTubeStatistics(
    val viewCount: String,
    val likeCount: String,
    val favoriteCount: String,
    val commentCount: String
)

data class YouTubeChannelResponse(
    val items: List<YouTubeChannelItem>?
)

data class YouTubeChannelItem(
    val id: String,
    val snippet: YouTubeChannelSnippet,
    val statistics: YouTubeChannelStatistics?
)

data class YouTubeChannelSnippet(
    val title: String,
    val description: String,
    val thumbnails: YouTubeThumbnails
)

data class YouTubeChannelStatistics(
    val subscriberCount: String,
    val videoCount: String,
    val viewCount: String
)
