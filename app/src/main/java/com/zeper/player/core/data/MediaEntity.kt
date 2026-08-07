package com.zeper.player.core.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val uri: String,
    val title: String,
    val type: String // "video" or "music"
)

@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey val uri: String,
    val title: String,
    val timestamp: Long,
    val type: String,
    val lastPosition: Long = 0,
    val duration: Long = 0,
    val folder: String? = null
)

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey val id: String, // Typically the download ID or URL hash
    val url: String,
    val fileName: String,
    val filePath: String? = null,
    val status: DownloadStatus = DownloadStatus.PENDING,
    val progress: Float = 0f,
    val downloadedSize: Long = 0,
    val totalSize: Long = 0,
    val downloadSpeed: String = "0 KB/s",
    val eta: String = "--",
    val type: String = "video", // "video" or "audio"
    val resolution: String? = null, // e.g., "1080p", "720p"
    val format: String = "MP4",
    val thumbnailPath: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val errorMessage: String? = null
)

enum class DownloadStatus {
    PENDING, DOWNLOADING, PAUSED, COMPLETED, FAILED, QUEUED
}

@Entity(tableName = "trash")
data class TrashEntity(
    @PrimaryKey val originalPath: String,
    val trashPath: String,
    val fileName: String,
    val deleteTimestamp: Long,
    val mediaType: String // "video" or "audio"
)

@Entity(tableName = "vault")
data class VaultEntity(
    @PrimaryKey val originalPath: String,
    val vaultPath: String,
    val fileName: String,
    val timestamp: Long,
    val mediaType: String // "video" or "audio"
)
