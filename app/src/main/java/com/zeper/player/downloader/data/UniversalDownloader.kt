package com.zeper.player.downloader.data

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import com.zeper.player.core.data.DownloadEntity
import com.zeper.player.core.data.DownloadStatus
import com.zeper.player.core.data.MediaDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class UniversalDownloader(
    private val context: Context,
    private val dao: MediaDao
) {
    private val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    fun enqueueDownload(url: String, fileName: String, type: String = "video") {
        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle(fileName)
            .setDescription("Downloading $type...")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "Zeper/$fileName")
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        val downloadId = downloadManager.enqueue(request)
        
        CoroutineScope(Dispatchers.IO).launch {
            // Check if we already have an entity (e.g. from a previous attempt)
            val existing = dao.getDownloadById(downloadId.toString())
            val entity = existing?.copy(
                status = DownloadStatus.DOWNLOADING,
                progress = 0f,
                type = type,
                timestamp = System.currentTimeMillis()
            ) ?: DownloadEntity(
                id = downloadId.toString(),
                url = url,
                fileName = fileName,
                status = DownloadStatus.DOWNLOADING,
                progress = 0f,
                type = type
            )
            dao.updateDownload(entity)
            trackProgress(downloadId, url, fileName, type)
        }
    }

    fun pauseDownload(id: String) {
        // System DownloadManager doesn't have a direct "pause" method that works across all versions easily
        // Usually, it handles connectivity-based pausing automatically.
        // For manual pause, we'd often need a custom downloader, but we can update the status in DB.
        CoroutineScope(Dispatchers.IO).launch {
            dao.updateDownloadStatus(id, DownloadStatus.PAUSED)
        }
    }

    fun cancelDownload(id: String) {
        val downloadId = id.toLongOrNull() ?: return
        downloadManager.remove(downloadId)
        CoroutineScope(Dispatchers.IO).launch {
            dao.deleteDownloadById(id)
        }
    }

    private fun trackProgress(downloadId: Long, url: String, fileName: String, type: String) {
        CoroutineScope(Dispatchers.IO).launch {
            var downloading = true
            while (downloading) {
                val query = DownloadManager.Query().setFilterById(downloadId)
                val cursor = downloadManager.query(query)
                if (cursor != null && cursor.moveToFirst()) {
                    val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                    val bytesDownloaded = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                    val bytesTotal = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                    val localUri = cursor.getString(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI))

                    val progress = if (bytesTotal > 0) (bytesDownloaded.toFloat() / bytesTotal) else 0f
                    
                    val statusEnum = when (status) {
                        DownloadManager.STATUS_SUCCESSFUL -> DownloadStatus.COMPLETED
                        DownloadManager.STATUS_FAILED -> DownloadStatus.FAILED
                        DownloadManager.STATUS_PENDING -> DownloadStatus.PENDING
                        DownloadManager.STATUS_PAUSED -> DownloadStatus.PAUSED
                        else -> DownloadStatus.DOWNLOADING
                    }

                    val existing = dao.getDownloadById(downloadId.toString())
                    val updatedEntity = (existing ?: DownloadEntity(
                        id = downloadId.toString(),
                        url = url,
                        fileName = fileName,
                        type = type
                    )).copy(
                        status = statusEnum,
                        progress = progress,
                        downloadedSize = bytesDownloaded,
                        totalSize = if (bytesTotal > 0) bytesTotal else 0,
                        filePath = localUri?.replace("file://", "")
                    )

                    dao.updateDownload(updatedEntity)
                    
                    if (status == DownloadManager.STATUS_SUCCESSFUL || status == DownloadManager.STATUS_FAILED) {
                        downloading = false
                    }
                }
                cursor?.close()
                delay(1000)
            }
        }
    }
}
