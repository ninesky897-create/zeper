package com.zeper.player.core.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.flow.first
import java.io.File

class TrashWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): androidx.work.ListenableWorker.Result {
        val prefs = PreferencesManager(applicationContext)
        val db = ZeperDatabase.getInstance(applicationContext)
        val dao = db.mediaDao()
        
        val retentionDays = prefs.trashRetentionDays.first()
        val expirationTime = System.currentTimeMillis() - (retentionDays.toLong() * 24 * 60 * 60 * 1000)
        
        val items = dao.getTrashItems().first()
        items.forEach { item ->
            if (item.deleteTimestamp < expirationTime) {
                val file = File(item.trashPath)
                if (file.exists()) {
                    file.delete()
                }
                dao.removeFromTrash(item)
            }
        }
        
        return androidx.work.ListenableWorker.Result.success()
    }
}
