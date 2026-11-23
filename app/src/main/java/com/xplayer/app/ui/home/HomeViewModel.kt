package com.xplayer.app.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xplayer.app.XPlayerApp
import com.xplayer.app.data.local.entity.PlaybackHistoryEntity
import com.xplayer.app.data.local.entity.StreamUrlEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as XPlayerApp
    private val historyDao = app.database.playbackHistoryDao()
    private val streamUrlDao = app.database.streamUrlDao()

    val continueWatching: StateFlow<PlaybackHistoryEntity?> = historyDao.getLatestPlaybackHistoryFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val recentPlayed: StateFlow<List<PlaybackHistoryEntity>> = historyDao.getRecentHistoryFlow(15)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val streamHistory: StateFlow<List<StreamUrlEntity>> = streamUrlDao.getAllStreamsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _streamInputUrl = MutableStateFlow("")
    val streamInputUrl: StateFlow<String> = _streamInputUrl.asStateFlow()

    fun updateStreamUrl(url: String) {
        _streamInputUrl.value = url
    }

    fun playStreamUrl(url: String, onPlay: (String) -> Unit) {
        val trimmed = url.trim()
        if (trimmed.isNotBlank()) {
            viewModelScope.launch {
                val entity = StreamUrlEntity(
                    url = trimmed,
                    customTitle = trimmed.substringAfterLast('/'),
                    lastPlayedAt = System.currentTimeMillis()
                )
                streamUrlDao.insertStream(entity)
                onPlay(trimmed)
            }
        }
    }

    fun removeHistory(mediaId: String) {
        viewModelScope.launch {
            historyDao.deleteByMediaId(mediaId)
        }
    }

    fun removeStream(id: Long) {
        viewModelScope.launch {
            streamUrlDao.deleteById(id)
        }
    }
}
