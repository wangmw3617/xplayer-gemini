package com.xplayer.app.player.engine

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.view.SurfaceView
import com.xplayer.app.data.datastore.EnginePolicy
import com.xplayer.app.data.datastore.EnginePrefs
import com.xplayer.app.data.datastore.SubtitlePrefs
import com.xplayer.app.data.local.entity.KernelType
import com.xplayer.app.data.local.entity.MpvShaderEntity
import com.xplayer.app.data.source.MediaSource
import com.xplayer.app.player.model.AspectRatioMode
import com.xplayer.app.player.model.PlayerUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class EngineRouter(
    private val context: Context,
    private val scope: CoroutineScope,
    private var enginePrefs: EnginePrefs,
    private var activeShaders: List<MpvShaderEntity> = emptyList()
) : PlayerEngine {

    private val _state = MutableStateFlow(PlayerUiState())
    override val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    override val engineType: KernelType
        get() = currentEngine?.engineType ?: KernelType.MEDIA3

    private var currentEngine: PlayerEngine? = null
    private var attachedSurfaceView: SurfaceView? = null
    private var currentSource: MediaSource? = null
    private var stateObservationJob: Job? = null

    fun updateConfig(newEnginePrefs: EnginePrefs, newShaders: List<MpvShaderEntity>) {
        this.enginePrefs = newEnginePrefs
        this.activeShaders = newShaders
    }

    private fun determineInitialEngine(source: MediaSource): KernelType {
        when (enginePrefs.enginePolicy) {
            EnginePolicy.FORCE_MEDIA3 -> return KernelType.MEDIA3
            EnginePolicy.FORCE_MPV -> return KernelType.MPV
            EnginePolicy.MEDIA3_FIRST -> {
                // Check if GLSL shaders are active or custom mpv.conf forced
                if (activeShaders.any { it.enabled } || (enginePrefs.customMpvConfEnabled && enginePrefs.customMpvConf.isNotBlank())) {
                    return KernelType.MPV
                }
                // Check ASS/SSA subtitles
                if (enginePrefs.autoSwitchAss && source is MediaSource.LocalFileSource) {
                    val hasAss = source.externalSubtitles.any { it.extension == "ass" || it.extension == "ssa" }
                    if (hasAss) {
                        return KernelType.MPV
                    }
                }
                return KernelType.MEDIA3
            }
        }
    }

    private fun createEngine(type: KernelType): PlayerEngine {
        return when (type) {
            KernelType.MEDIA3, KernelType.AUTO -> {
                Media3EngineImpl(
                    context = context,
                    scope = scope,
                    frameRateMatching = enginePrefs.frameRateMatching,
                    onErrorFallback = {
                        // Silent fallback to MPV on Media3 decoding error
                        if (enginePrefs.enginePolicy == EnginePolicy.MEDIA3_FIRST) {
                            switchEngine(KernelType.MPV)
                        }
                    }
                )
            }
            KernelType.MPV -> {
                MpvEngineImpl(
                    context = context,
                    scope = scope,
                    enginePrefs = enginePrefs,
                    activeShaders = activeShaders,
                    onErrorFallback = {
                        // If MPV fails, can fallback to Media3
                    }
                )
            }
        }
    }

    fun switchEngine(targetType: KernelType) {
        val oldEngine = currentEngine
        val source = currentSource ?: return
        val currentPosition = oldEngine?.state?.value?.currentPositionMs ?: _state.value.currentPositionMs
        val isPlaying = oldEngine?.state?.value?.isPlaying ?: _state.value.isPlaying
        val speed = oldEngine?.state?.value?.speed ?: _state.value.speed
        val aspect = oldEngine?.state?.value?.aspectRatioMode ?: _state.value.aspectRatioMode

        // Release old engine
        stateObservationJob?.cancel()
        oldEngine?.detachSurfaceView()
        oldEngine?.release()

        // Create new engine
        val newEngine = createEngine(targetType)
        currentEngine = newEngine

        attachedSurfaceView?.let { newEngine.attachSurfaceView(it) }
        newEngine.prepare(source, currentPosition)
        newEngine.setSpeed(speed)
        newEngine.setAspectRatio(aspect)
        if (!isPlaying) {
            newEngine.pause()
        }

        observeEngineState(newEngine)
    }

    private fun observeEngineState(engine: PlayerEngine) {
        stateObservationJob?.cancel()
        stateObservationJob = scope.launch {
            engine.state.collectLatest { engineState ->
                _state.value = engineState.copy(activeEngine = engine.engineType)
            }
        }
    }

    override fun prepare(source: MediaSource, startPositionMs: Long) {
        currentSource = source
        val targetType = determineInitialEngine(source)

        currentEngine?.let {
            stateObservationJob?.cancel()
            it.detachSurfaceView()
            it.release()
        }

        val engine = createEngine(targetType)
        currentEngine = engine
        attachedSurfaceView?.let { engine.attachSurfaceView(it) }
        engine.prepare(source, startPositionMs)

        observeEngineState(engine)
    }

    override fun play() {
        currentEngine?.play()
    }

    override fun pause() {
        currentEngine?.pause()
    }

    override fun seekTo(positionMs: Long) {
        currentEngine?.seekTo(positionMs)
    }

    override fun setSpeed(speed: Float) {
        currentEngine?.setSpeed(speed)
    }

    override fun selectAudioTrack(id: String?) {
        currentEngine?.selectAudioTrack(id)
    }

    override fun selectSubtitleTrack(id: String?) {
        currentEngine?.selectSubtitleTrack(id)
    }

    override fun addExternalSubtitle(uri: Uri, name: String) {
        currentEngine?.addExternalSubtitle(uri, name)
    }

    override fun setSubtitleDelay(delayMs: Long) {
        currentEngine?.setSubtitleDelay(delayMs)
    }

    override fun setSubtitleStyle(style: SubtitlePrefs) {
        currentEngine?.setSubtitleStyle(style)
    }

    override fun setAspectRatio(mode: AspectRatioMode) {
        currentEngine?.setAspectRatio(mode)
    }

    override fun captureScreenshot(callback: (Bitmap?) -> Unit) {
        currentEngine?.captureScreenshot(callback) ?: callback(null)
    }

    override fun attachSurfaceView(surfaceView: SurfaceView) {
        attachedSurfaceView = surfaceView
        currentEngine?.attachSurfaceView(surfaceView)
    }

    override fun detachSurfaceView() {
        currentEngine?.detachSurfaceView()
        attachedSurfaceView = null
    }

    override fun release() {
        stateObservationJob?.cancel()
        currentEngine?.detachSurfaceView()
        currentEngine?.release()
        currentEngine = null
        attachedSurfaceView = null
    }
}
