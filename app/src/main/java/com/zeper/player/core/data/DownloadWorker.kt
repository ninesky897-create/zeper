package com.zeper.player.core.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

class DownloadWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val db = ZeperDatabase.getInstance(context)
    private val dao = db.mediaDao()
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val downloadId = inputData.getString("download_id") ?: return@withContext Result.failure()
        var download = dao.getDownloadById(downloadId) ?: return@withContext Result.failure()

        try {
            setForeground(createForegroundInfo(downloadId, download.fileName, 0))
            
            val url = URL(download.url)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 15000
            connection.readTimeout = 15000

            // Support for resume
            val file = File(context.getExternalFilesDir(null), download.fileName)
            var downloadedBytes = 0L
            if (file.exists() && download.status == DownloadStatus.PAUSED) {
                downloadedBytes = file.length()
                connection.setRequestProperty("Range", "bytes=$downloadedBytes-")
            }

            connection.connect()

            if (connection.responseCode != HttpURLConnection.HTTP_OK && connection.responseCode != HttpURLConnection.HTTP_PARTIAL) {
                dao.updateDownload(download.copy(status = DownloadStatus.FAILED, errorMessage = "Server error: ${connection.responseCode}"))
                return@withContext Result.failure()
            }

            val totalSize = connection.contentLength + downloadedBytes
            val inputStream = connection.inputStream
            val outputStream = FileOutputStream(file, downloadedBytes > 0)

            val buffer = ByteArray(8192)
            var bytesRead: Int
            var lastUpdate = System.currentTimeMillis()
            var bytesInInterval = 0L
            var startTime = System.currentTimeMillis()

            dao.updateDownload(download.copy(status = DownloadStatus.DOWNLOADING, totalSize = totalSize, filePath = file.absolutePath))

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                if (isStopped) {
                    outputStream.close()
                    inputStream.close()
                    dao.updateDownloadStatus(downloadId, DownloadStatus.PAUSED)
                    return@withContext Result.retry()
                }

                outputStream.write(buffer, 0, bytesRead)
                downloadedBytes += bytesRead
                bytesInInterval += bytesRead

                val currentTime = System.currentTimeMillis()
                if (currentTime - lastUpdate > 1000) {
                    val progress = (downloadedBytes.toFloat() / totalSize)
                    val speedBytesPerSec = bytesInInterval * 1000 / (currentTime - lastUpdate)
                    val speedText = formatSpeed(speedBytesPerSec)
                    val eta = if (speedBytesPerSec > 0) formatEta((totalSize - downloadedBytes) / speedBytesPerSec) else "--"

                    dao.updateDownload(download.copy(
                        status = DownloadStatus.DOWNLOADING,
                        progress = progress,
                        downloadedSize = downloadedBytes,
                        downloadSpeed = speedText,
                        eta = eta
                    ))
                    
                    updateNotification(downloadId, download.fileName, (progress * 100).toInt())
                    
                    lastUpdate = currentTime
                    bytesInInterval = 0
                }
            }

            outputStream.flush()
            outputStream.close()
            inputStream.close()

            dao.updateDownload(download.copy(
                status = DownloadStatus.COMPLETED,
                progress = 1f,
                downloadedSize = totalSize,
                downloadSpeed = "0 KB/s",
                eta = "Done"
            ))
            
            showCompletionNotification(downloadId, download.fileName)
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            dao.updateDownload(download.copy(status = DownloadStatus.FAILED, errorMessage = e.message))
            Result.failure()
        }
    }

    private fun formatSpeed(bytesPerSec: Long): String {
        return when {
            bytesPerSec >= 1024 * 1024 -> String.format("%.1f MB/s", bytesPerSec / (1024f * 1024f))
            bytesPerSec >= 1024 -> String.format("%d KB/s", bytesPerSec / 1024)
            else -> "$bytesPerSec B/s"
        }
    }

    private fun formatEta(seconds: Long): String {
        if (seconds > 3600) return String.format("%dh %dm", seconds / 3600, (seconds % 3600) / 60)
        if (seconds > 60) return String.format("%dm %ds", seconds / 60, seconds % 60)
        return "${seconds}s"
    }

    private fun createForegroundInfo(id: String, fileName: String, progress: Int): ForegroundInfo {
        val channelId = "downloader_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Downloads", NotificationManager.IMPORTANCE_LOW)
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle("Downloading $fileName")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .setProgress(100, progress, false)
            .build()

        return ForegroundInfo(id.hashCode(), notification)
    }

    private fun updateNotification(id: String, fileName: String, progress: Int) {
        val notification = NotificationCompat.Builder(context, "downloader_channel")
            .setContentTitle("Downloading $fileName")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .setProgress(100, progress, false)
            .build()
        notificationManager.notify(id.hashCode(), notification)
    }

    private fun showCompletionNotification(id: String, fileName: String) {
        val notification = NotificationCompat.Builder(context, "downloader_channel")
            .setContentTitle("Download Completed")
            .setContentText(fileName)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(id.hashCode(), notification)
    }
}
