package com.xplayer.app.ui.videos

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xplayer.app.XPlayerApp
import com.xplayer.app.data.local.entity.VideoEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

enum class VideoSortOrder {
    DATE_DESC,
    NAME_ASC,
    SIZE_DESC,
    DURATION_DESC
}

class VideosViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as XPlayerApp
    private val videoDao = app.database.videoDao()
    private val scanner = app.mediaScanner

    val isScanning: StateFlow<Boolean> = scanner.isScanning

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _sortOrder = MutableStateFlow(VideoSortOrder.DATE_DESC)
    val sortOrder: StateFlow<VideoSortOrder> = _sortOrder.asStateFlow()

    private val _isGridView = MutableStateFlow(false)
    val isGridView: StateFlow<Boolean> = _isGridView.asStateFlow()

    val videos: StateFlow<List<VideoEntity>> = combine(
        videoDao.getAllVideosFlow(),
        _searchQuery,
        _sortOrder
    ) { allVideos, query, sort ->
        val filtered = if (query.isBlank()) {
            allVideos
        } else {
            allVideos.filter { it.title.contains(query, ignoreCase = true) || it.path.contains(query, ignoreCase = true) }
        }

        when (sort) {
            VideoSortOrder.DATE_DESC -> filtered.sortedByDescending { it.dateAdded }
            VideoSortOrder.NAME_ASC -> filtered.sortedBy { it.title.lowercase() }
            VideoSortOrder.SIZE_DESC -> filtered.sortedByDescending { it.sizeBytes }
            VideoSortOrder.DURATION_DESC -> filtered.sortedByDescending { it.durationMs }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSortOrder(order: VideoSortOrder) {
        _sortOrder.value = order
    }

    fun toggleViewMode() {
        _isGridView.value = !_isGridView.value
    }

    fun scanVideos() {
        viewModelScope.launch {
            scanner.scanMedia()
        }
    }

    fun deleteVideo(video: VideoEntity, onComplete: () -> Unit) {
        viewModelScope.launch {
            runCatching {
                val file = File(video.path)
                if (file.exists()) {
                    file.delete()
                }
            }
            videoDao.deleteByPath(video.path)
            onComplete()
        }
    }

    fun renameVideo(video: VideoEntity, newName: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            val file = File(video.path)
            val parent = file.parentFile ?: return@launch
            val extension = file.extension
            val newFile = File(parent, "$newName.$extension")

            if (!newFile.exists() && file.renameTo(newFile)) {
                videoDao.deleteByPath(video.path)
                val updated = video.copy(
                    path = newFile.absolutePath,
                    title = newName,
                    dateModified = newFile.lastModified()
                )
                videoDao.insertVideo(updated)
            }
            onComplete()
        }
    }
}
