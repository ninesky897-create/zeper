package com.zeper.player.core.data

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class MediaFile(
    val id: Long,
    val name: String,
    val path: String,
    val contentUri: String,
    val duration: Long,
    val size: Long,
    val type: String, // "video" or "audio"
    val dateAdded: Long = 0L,
    val artist: String? = null,
    val album: String? = null,
    val folder: String? = null,
    val isNew: Boolean = false
)

class MediaScanner(private val context: Context) {

    suspend fun scanMedia(
        scanHidden: Boolean,
        scanDot: Boolean,
        scanNoMedia: Boolean,
        hiddenFolders: List<String> = emptyList(),
        includeAudio: Boolean = true,
        includeVideo: Boolean = true
    ): List<MediaFile> = withContext(Dispatchers.IO) {
        val mediaList = mutableListOf<MediaFile>()
        
        if (includeVideo) {
            scanType(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, "video", scanHidden, scanDot, scanNoMedia, hiddenFolders, mediaList)
        }
        if (includeAudio) {
            scanType(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, "audio", scanHidden, scanDot, scanNoMedia, hiddenFolders, mediaList)
        }

        mediaList
    }

    private fun scanType(
        uri: android.net.Uri,
        type: String,
        scanHidden: Boolean,
        scanDot: Boolean,
        scanNoMedia: Boolean,
        hiddenFolders: List<String>,
        resultList: MutableList<MediaFile>
    ) {
        val projection = mutableListOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.DATA,
            MediaStore.MediaColumns.DURATION,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.DATE_ADDED
        ).apply {
            if (type == "audio") {
                add(MediaStore.Audio.AudioColumns.ARTIST)
                add(MediaStore.Audio.AudioColumns.ALBUM)
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                add(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME)
            }
        }.toTypedArray()

        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            val pathCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)
            val durCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DURATION)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
            
            val artistCol = if (type == "audio") cursor.getColumnIndex(MediaStore.Audio.AudioColumns.ARTIST) else -1
            val albumCol = if (type == "audio") cursor.getColumnIndex(MediaStore.Audio.AudioColumns.ALBUM) else -1
            val bucketCol = cursor.getColumnIndex(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME)

            val noMediaCache = mutableMapOf<String, Boolean>()

            while (cursor.moveToNext()) {
                try {
                    val path = cursor.getString(pathCol) ?: continue
                    
                    // Fast path: skip expensive File checks if defaults are used
                    if (!scanHidden || !scanDot || !scanNoMedia) {
                        val file = File(path)
                        if (!scanHidden && file.isHidden) continue
                        if (!scanDot && isDotFile(file)) continue
                        if (!scanNoMedia) {
                            val parentPath = file.parent ?: ""
                            val hasNoMedia = noMediaCache.getOrPut(parentPath) { hasNoMedia(file) }
                            if (hasNoMedia) continue
                        }
                    }

                    val folderName = if (bucketCol != -1) {
                        cursor.getString(bucketCol)
                    } else {
                        File(path).parentFile?.name
                    }

                    if (folderName != null && hiddenFolders.contains(folderName)) continue

                    val id = cursor.getLong(idCol)
                    val contentUri = ContentUris.withAppendedId(uri, id).toString()

                    resultList.add(
                        MediaFile(
                            id = id,
                            name = cursor.getString(nameCol) ?: "Unknown",
                            path = path,
                            contentUri = contentUri,
                            duration = cursor.getLong(durCol),
                            size = cursor.getLong(sizeCol),
                            type = type,
                            dateAdded = cursor.getLong(dateCol),
                            artist = if (artistCol != -1) cursor.getString(artistCol) else null,
                            album = if (albumCol != -1) cursor.getString(albumCol) else null,
                            folder = folderName ?: "Internal Storage"
                        )
                    )
                } catch (e: Exception) {
                    android.util.Log.e("MediaScanner", "Error scanning file", e)
                }
            }
        }
    }

    private fun isDotFile(file: File): Boolean {
        var current: File? = file
        while (current != null) {
            if (current.name.startsWith(".")) return true
            current = current.parentFile
        }
        return false
    }

    private fun hasNoMedia(file: File): Boolean {
        var current: File? = file.parentFile
        while (current != null) {
            if (File(current, ".nomedia").exists()) return true
            current = current.parentFile
        }
        return false
    }
}
