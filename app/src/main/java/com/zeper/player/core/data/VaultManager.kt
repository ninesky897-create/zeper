package com.zeper.player.core.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.*

class VaultManager(private val context: Context) {
    private val db = ZeperDatabase.getInstance(context)
    private val dao = db.mediaDao()
    private val vaultDir = File(context.getExternalFilesDir(null), ".vault").apply { 
        if (!exists()) mkdirs() 
        File(this, ".nomedia").createNewFile()
    }

    suspend fun moveToVault(mediaFile: MediaFile): Boolean = withContext(Dispatchers.IO) {
        try {
            val sourceFile = File(mediaFile.path)
            if (!sourceFile.exists()) return@withContext false

            val vaultFile = File(vaultDir, "${UUID.randomUUID()}_${sourceFile.name}")
            
            if (sourceFile.renameTo(vaultFile)) {
                return@withContext saveToVaultDb(mediaFile, vaultFile)
            }
            
            try {
                sourceFile.inputStream().use { input ->
                    vaultFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                if (sourceFile.delete()) {
                    return@withContext saveToVaultDb(mediaFile, vaultFile)
                } else {
                    vaultFile.delete()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext false
    }

    private suspend fun saveToVaultDb(mediaFile: MediaFile, vaultFile: File): Boolean {
        val vaultEntry = VaultEntity(
            originalPath = mediaFile.path,
            vaultPath = vaultFile.absolutePath,
            fileName = mediaFile.name,
            timestamp = System.currentTimeMillis(),
            mediaType = mediaFile.type
        )
        dao.addToVault(vaultEntry)
        
        try {
            val uri = android.net.Uri.parse(mediaFile.contentUri)
            context.contentResolver.delete(uri, null, null)
        } catch (e: Exception) {
        }
        
        return true
    }

    suspend fun restoreFromVault(item: VaultEntity): Boolean = withContext(Dispatchers.IO) {
        try {
            val vaultFile = File(item.vaultPath)
            if (!vaultFile.exists()) {
                dao.removeFromVault(item)
                return@withContext false
            }

            val originalFile = File(item.originalPath)
            originalFile.parentFile?.mkdirs()

            if (vaultFile.renameTo(originalFile)) {
                dao.removeFromVault(item)
                android.media.MediaScannerConnection.scanFile(
                    context, 
                    arrayOf(originalFile.absolutePath), 
                    null, 
                    null
                )
                return@withContext true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext false
    }

    suspend fun deletePermanently(item: VaultEntity): Boolean = withContext(Dispatchers.IO) {
        try {
            val vaultFile = File(item.vaultPath)
            if (vaultFile.exists()) {
                vaultFile.delete()
            }
            dao.removeFromVault(item)
            return@withContext true
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext false
    }
}
