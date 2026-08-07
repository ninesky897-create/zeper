package com.zeper.player.core.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.*

class TrashManager(private val context: Context) {
    private val db = ZeperDatabase.getInstance(context)
    private val dao = db.mediaDao()
    private val trashDir = File(context.getExternalFilesDir(null), ".trash").apply { if (!exists()) mkdirs() }

    suspend fun moveToTrash(mediaFile: MediaFile): Boolean = withContext(Dispatchers.IO) {
        try {
            val sourceFile = File(mediaFile.path)
            if (!sourceFile.exists()) return@withContext false

            val trashFile = File(trashDir, "${UUID.randomUUID()}_${sourceFile.name}")
            
            // Try rename first
            if (sourceFile.renameTo(trashFile)) {
                return@withContext saveToTrashDb(mediaFile, trashFile)
            }
            
            // If rename fails (e.g. cross-volume), try copy and delete
            try {
                sourceFile.inputStream().use { input ->
                    trashFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                if (sourceFile.delete()) {
                    return@withContext saveToTrashDb(mediaFile, trashFile)
                } else {
                    trashFile.delete() // Clean up trash if original couldn't be deleted
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext false
    }

    private suspend fun saveToTrashDb(mediaFile: MediaFile, trashFile: File): Boolean {
        val trashEntry = TrashEntity(
            originalPath = mediaFile.path,
            trashPath = trashFile.absolutePath,
            fileName = mediaFile.name,
            deleteTimestamp = System.currentTimeMillis(),
            mediaType = mediaFile.type
        )
        dao.addToTrash(trashEntry)
        
        // Also try to notify MediaScanner to remove the old path
        try {
            val file = File(mediaFile.path)
            if (file.exists()) {
                file.delete()
            }
            val uri = android.net.Uri.parse(mediaFile.contentUri)
            context.contentResolver.delete(uri, null, null)
        } catch (e: Exception) {
            // Ignore failure, shared files need createDeleteRequest on Android 11+
        }
        
        return true
    }

    suspend fun restoreFromTrash(item: TrashEntity): Boolean = withContext(Dispatchers.IO) {
        try {
            val trashFile = File(item.trashPath)
            if (!trashFile.exists()) {
                dao.removeFromTrash(item)
                return@withContext false
            }

            val originalFile = File(item.originalPath)
            originalFile.parentFile?.mkdirs()

            if (trashFile.renameTo(originalFile)) {
                dao.removeFromTrash(item)
                return@withContext true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext false
    }

    suspend fun permanentlyDelete(item: TrashEntity): Boolean = withContext(Dispatchers.IO) {
        try {
            val trashFile = File(item.trashPath)
            if (trashFile.exists()) {
                trashFile.delete()
            }
            dao.removeFromTrash(item)
            return@withContext true
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext false
    }
}
