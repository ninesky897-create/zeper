package com.zeper.player.downloader.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "liked_videos")
data class LikedVideoEntity(
    @PrimaryKey val videoId: String,
    val title: String,
    val thumbnail: String,
    val channelTitle: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "playback_history")
data class HistoryEntity(
    @PrimaryKey val videoId: String,
    val title: String,
    val thumbnail: String,
    val channelTitle: String,
    val duration: String,
    val lastPlayedTime: Long = System.currentTimeMillis()
)
