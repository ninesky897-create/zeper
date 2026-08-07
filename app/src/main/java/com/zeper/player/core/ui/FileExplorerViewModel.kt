package com.zeper.player.core.ui

import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File

data class FileItem(
    val file: File,
    val isDirectory: Boolean,
    val size: Long,
    val name: String = file.name,
    val path: String = file.absolutePath,
    val hasVideo: Boolean = false
)

class FileExplorerViewModel : ViewModel() {
    private val root = Environment.getExternalStorageDirectory()
    private val _currentDirectory = MutableStateFlow(root)
    val currentDirectory: StateFlow<File> = _currentDirectory

    private val _files = MutableStateFlow<List<FileItem>>(emptyList())
    val files: StateFlow<List<FileItem>> = _files

    init {
        loadFiles(root)
    }

    fun navigateTo(directory: File) {
        if (directory.isDirectory) {
            _currentDirectory.value = directory
            loadFiles(directory)
        }
    }

    fun navigateUp(): Boolean {
        val parent = _currentDirectory.value.parentFile
        if (parent != null && parent.absolutePath.startsWith(root.absolutePath)) {
            navigateTo(parent)
            return true
        }
        return false
    }

    private fun loadFiles(directory: File) {
        viewModelScope.launch {
            val fileList = directory.listFiles()?.filter { file ->
                !file.isHidden && 
                !file.name.startsWith(".") && 
                !file.name.endsWith(".file")
            }?.map {
                val hasVideo = if (it.isDirectory) {
                    it.listFiles()?.any { child -> isVideoFile(child) } ?: false
                } else {
                    isVideoFile(it)
                }
                FileItem(
                    file = it,
                    isDirectory = it.isDirectory,
                    size = if (it.isDirectory) 0 else it.length(),
                    name = it.name,
                    path = it.absolutePath,
                    hasVideo = hasVideo
                )
            }?.sortedWith(compareByDescending<FileItem> { it.hasVideo }.thenBy { !it.isDirectory }.thenBy { it.name.lowercase() }) ?: emptyList()
            
            _files.value = fileList
        }
    }

    private fun isVideoFile(file: File): Boolean {
        if (file.isDirectory) return false
        val extensions = listOf("mp4", "mkv", "webm", "avi", "mov", "3gp")
        return extensions.any { file.name.lowercase().endsWith(it) }
    }
}
