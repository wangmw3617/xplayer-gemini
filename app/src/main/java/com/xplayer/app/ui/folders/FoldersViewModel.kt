package com.xplayer.app.ui.folders

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xplayer.app.XPlayerApp
import com.xplayer.app.data.local.entity.FolderBlacklistEntity
import com.xplayer.app.data.local.entity.VideoEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

data class FolderItem(
    val path: String,
    val name: String,
    val videoCount: Int,
    val totalSizeBytes: Long,
    val videos: List<VideoEntity>
)

class FoldersViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as XPlayerApp
    private val videoDao = app.database.videoDao()
    private val blacklistDao = app.database.folderBlacklistDao()
    private val scanner = app.mediaScanner

    private val _selectedFolder = MutableStateFlow<FolderItem?>(null)
    val selectedFolder: StateFlow<FolderItem?> = _selectedFolder.asStateFlow()

    val blacklistedFolders: StateFlow<List<FolderBlacklistEntity>> = blacklistDao.getAllBlacklistedFoldersFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val folderItems: StateFlow<List<FolderItem>> = combine(
        videoDao.getAllVideosFlow(),
        blacklistDao.getAllBlacklistedFoldersFlow()
    ) { videos, blacklists ->
        val blacklistPaths = blacklists.map { it.folderPath }.toSet()
        val validVideos = videos.filter { v -> !blacklistPaths.any { v.folderPath.startsWith(it) } }

        validVideos.groupBy { it.folderPath }.map { (folderPath, vList) ->
            val folderName = folderPath.substringAfterLast(File.separator).ifEmpty { folderPath }
            val totalSize = vList.sumOf { it.sizeBytes }
            FolderItem(
                path = folderPath,
                name = folderName,
                videoCount = vList.size,
                totalSizeBytes = totalSize,
                videos = vList
            )
        }.sortedBy { it.name.lowercase() }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectFolder(folder: FolderItem?) {
        _selectedFolder.value = folder
    }

    fun blacklistFolder(folderPath: String) {
        viewModelScope.launch {
            blacklistDao.insertBlacklist(FolderBlacklistEntity(folderPath = folderPath))
            scanner.scanMedia()
        }
    }

    fun unhideFolder(folderPath: String) {
        viewModelScope.launch {
            blacklistDao.deleteBlacklist(folderPath)
            scanner.scanMedia()
        }
    }
}
