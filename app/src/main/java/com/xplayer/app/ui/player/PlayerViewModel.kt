package com.xplayer.app.ui.player

import android.app.Activity
import android.app.Application
import android.net.Uri
import android.view.SurfaceView
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xplayer.app.XPlayerApp
import com.xplayer.app.data.datastore.GesturePrefs
import com.xplayer.app.data.datastore.PlaybackPrefs
import com.xplayer.app.data.datastore.SubtitlePrefs
import com.xplayer.app.data.local.entity.KernelType
import com.xplayer.app.data.local.entity.PlaybackHistoryEntity
import com.xplayer.app.data.local.entity.SourceType
import com.xplayer.app.data.scanner.SubtitleMatcher
import com.xplayer.app.data.source.MediaSource
import com.xplayer.app.player.engine.EngineRouter
import com.xplayer.app.player.model.AspectRatioMode
import com.xplayer.app.player.model.PlayerUiState
import com.xplayer.app.ui.player.gesture.HudState
import com.xplayer.app.ui.player.util.ScreenshotHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as XPlayerApp
    private val db = app.database
    private val historyDao = db.playbackHistoryDao()
    private val shaderDao = db.mpvShaderDao()
    private val prefsRepo = app.preferencesRepository

    private var engineRouter: EngineRouter? = null
    private var positionSaveJob: Job? = null
    private var hudDismissJob: Job? = null
    private var currentMediaSource: MediaSource? = null

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private val _hudState = MutableStateFlow<HudState>(HudState.None)
    val hudState: StateFlow<HudState> = _hudState.asStateFlow()

    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage: SharedFlow<String> = _toastMessage.asSharedFlow()

    val gesturePrefs: StateFlow<GesturePrefs> = prefsRepo.gesturePrefsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GesturePrefs())

    val subtitlePrefs: StateFlow<SubtitlePrefs> = prefsRepo.subtitlePrefsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SubtitlePrefs())

    val playbackPrefs: StateFlow<PlaybackPrefs> = prefsRepo.playbackPrefsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PlaybackPrefs())

    init {
        viewModelScope.launch {
            val enginePrefs = prefsRepo.enginePrefsFlow.first()
            val shaders = shaderDao.getEnabledShaders()
            val router = EngineRouter(
                context = app,
                scope = viewModelScope,
                enginePrefs = enginePrefs,
                activeShaders = shaders
            )
            engineRouter = router

            launch {
                router.state.collect { state ->
                    _uiState.value = state
                }
            }
        }
    }

    fun attachSurface(surfaceView: SurfaceView) {
        engineRouter?.attachSurfaceView(surfaceView)
    }

    fun detachSurface() {
        engineRouter?.detachSurfaceView()
    }

    fun loadMedia(pathOrUrl: String) {
        viewModelScope.launch {
            val history = historyDao.getHistoryByMediaId(pathOrUrl)
            val startPosition = if (history != null && !history.isFinished) history.lastPositionMs else 0L

            val source: MediaSource = if (pathOrUrl.startsWith("http://") || pathOrUrl.startsWith("https://")) {
                MediaSource.NetworkUrlSource(
                    url = pathOrUrl,
                    title = pathOrUrl.substringAfterLast('/')
                )
            } else {
                val file = File(pathOrUrl)
                val subtitles = SubtitleMatcher.findSubtitlesForVideo(file)
                MediaSource.LocalFileSource(
                    path = pathOrUrl,
                    title = file.nameWithoutExtension,
                    sizeBytes = file.length(),
                    modifiedTime = file.lastModified(),
                    externalSubtitles = subtitles
                )
            }
            currentMediaSource = source

            // Wait until router ready
            while (engineRouter == null) {
                delay(50L)
            }

            engineRouter?.prepare(source, startPosition)

            startPeriodicPositionSave()
        }
    }

    fun play() {
        engineRouter?.play()
    }

    fun pause() {
        engineRouter?.pause()
        saveCurrentPosition()
    }

    fun seekTo(positionMs: Long) {
        engineRouter?.seekTo(positionMs)
    }

    fun setSpeed(speed: Float) {
        engineRouter?.setSpeed(speed)
    }

    fun switchKernel() {
        val current = _uiState.value.activeEngine
        val target = if (current == KernelType.MEDIA3) KernelType.MPV else KernelType.MEDIA3
        engineRouter?.switchEngine(target)
    }

    fun cycleAspectRatio() {
        val current = _uiState.value.aspectRatioMode
        val next = when (current) {
            AspectRatioMode.FIT -> AspectRatioMode.CROP
            AspectRatioMode.CROP -> AspectRatioMode.STRETCH
            AspectRatioMode.STRETCH -> AspectRatioMode.RATIO_16_9
            AspectRatioMode.RATIO_16_9 -> AspectRatioMode.RATIO_4_3
            AspectRatioMode.RATIO_4_3 -> AspectRatioMode.ORIGINAL
            AspectRatioMode.ORIGINAL -> AspectRatioMode.FIT
        }
        engineRouter?.setAspectRatio(next)
    }

    fun selectAudioTrack(id: String?) {
        engineRouter?.selectAudioTrack(id)
    }

    fun selectSubtitleTrack(id: String?) {
        engineRouter?.selectSubtitleTrack(id)
    }

    fun addExternalSubtitle(uri: Uri, name: String) {
        engineRouter?.addExternalSubtitle(uri, name)
    }

    fun adjustSubtitleDelay(deltaMs: Long) {
        val current = _uiState.value.subtitleDelayMs
        val newDelay = current + deltaMs
        engineRouter?.setSubtitleDelay(newDelay)
    }

    fun resetSubtitleDelay() {
        engineRouter?.setSubtitleDelay(0L)
    }

    fun showHud(hud: HudState, autoDismissMs: Long = 1500L) {
        _hudState.value = hud
        hudDismissJob?.cancel()
        if (autoDismissMs > 0) {
            hudDismissJob = viewModelScope.launch {
                delay(autoDismissMs)
                _hudState.value = HudState.None
            }
        }
    }

    fun captureScreenshot(context: Activity) {
        engineRouter?.captureScreenshot { bitmap ->
            if (bitmap != null) {
                viewModelScope.launch {
                    val uri = ScreenshotHelper.saveScreenshot(context, bitmap)
                    if (uri != null) {
                        _toastMessage.emit("截图已保存至相册")
                    } else {
                        _toastMessage.emit("截图保存失败")
                    }
                }
            } else {
                viewModelScope.launch {
                    _toastMessage.emit("截图失败：渲染帧不可用")
                }
            }
        }
    }

    private fun startPeriodicPositionSave() {
        positionSaveJob?.cancel()
        positionSaveJob = viewModelScope.launch {
            while (isActive) {
                delay(3000L)
                saveCurrentPosition()
            }
        }
    }

    private fun saveCurrentPosition() {
        val source = currentMediaSource ?: return
        val currentPos = _uiState.value.currentPositionMs
        val duration = _uiState.value.durationMs

        if (duration > 0 && currentPos > 0) {
            viewModelScope.launch {
                val entity = PlaybackHistoryEntity(
                    mediaId = source.id,
                    sourceType = if (source is MediaSource.LocalFileSource) SourceType.LOCAL else SourceType.NETWORK,
                    title = source.title,
                    lastPositionMs = currentPos,
                    durationMs = duration,
                    lastPlayedAt = System.currentTimeMillis(),
                    preferredEngine = _uiState.value.activeEngine,
                    subtitleDelayMs = _uiState.value.subtitleDelayMs,
                    audioTrackId = _uiState.value.selectedAudioTrackId,
                    subtitleTrackId = _uiState.value.selectedSubtitleTrackId
                )
                historyDao.insertOrUpdate(entity)
            }
        }
    }

    override fun onCleared() {
        saveCurrentPosition()
        positionSaveJob?.cancel()
        engineRouter?.release()
        engineRouter = null
        super.onCleared()
    }
}
