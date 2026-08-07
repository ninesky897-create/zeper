package com.zeper.player.core.data

import android.content.Context
import androidx.work.*
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class DownloadManager(private val context: Context) {
    private val db = ZeperDatabase.getInstance(context)
    private val dao = db.mediaDao()
    private val workManager = WorkManager.getInstance(context)

    fun getAllDownloads(): Flow<List<DownloadEntity>> = dao.getAllDownloads()

    fun getFilteredDownloads(status: DownloadStatus?): Flow<List<DownloadEntity>> {
        return if (status == null) dao.getAllDownloads() 
        else dao.getDownloadsByStatus(status.name)
    }

    suspend fun startDownload(
        url: String,
        fileName: String,
        type: String,
        resolution: String? = null,
        format: String = "MP4"
    ) {
        val id = UUID.randomUUID().toString()
        val download = DownloadEntity(
            id = id,
            url = url,
            fileName = fileName,
            status = DownloadStatus.QUEUED,
            type = type,
            resolution = resolution,
            format = format,
            timestamp = System.currentTimeMillis()
        )
        dao.updateDownload(download)

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val downloadWork = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setConstraints(constraints)
            .setInputData(workDataOf("download_id" to id))
            .addTag("download_$id")
            .build()

        workManager.enqueueUniqueWork(
            "download_$id",
            ExistingWorkPolicy.KEEP,
            downloadWork
        )
    }

    suspend fun pauseDownload(id: String) {
        workManager.cancelUniqueWork("download_$id")
        dao.updateDownloadStatus(id, DownloadStatus.PAUSED)
    }

    suspend fun resumeDownload(id: String) {
        val download = dao.getDownloadById(id) ?: return
        dao.updateDownloadStatus(id, DownloadStatus.QUEUED)
        
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val downloadWork = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setConstraints(constraints)
            .setInputData(workDataOf("download_id" to id))
            .addTag("download_$id")
            .build()

        workManager.enqueueUniqueWork(
            "download_$id",
            ExistingWorkPolicy.REPLACE,
            downloadWork
        )
    }

    suspend fun cancelDownload(id: String) {
        workManager.cancelUniqueWork("download_$id")
        dao.deleteDownloadById(id)
    }

    suspend fun retryDownload(id: String) {
        resumeDownload(id)
    }

    suspend fun deleteDownload(id: String, deleteFile: Boolean = true) {
        val download = dao.getDownloadById(id)
        if (deleteFile && download?.filePath != null) {
            val file = java.io.File(download.filePath)
            if (file.exists()) file.delete()
        }
        dao.deleteDownloadById(id)
    }

    suspend fun renameDownload(id: String, newName: String) {
        val download = dao.getDownloadById(id) ?: return
        if (download.filePath != null) {
            val oldFile = java.io.File(download.filePath)
            if (oldFile.exists()) {
                val newFile = java.io.File(oldFile.parent, newName)
                if (oldFile.renameTo(newFile)) {
                    dao.updateDownload(download.copy(fileName = newName, filePath = newFile.absolutePath))
                }
            }
        } else {
            dao.renameDownload(id, newName)
        }
    }
}
