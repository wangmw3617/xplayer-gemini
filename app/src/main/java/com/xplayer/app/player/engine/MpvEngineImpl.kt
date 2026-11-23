package com.xplayer.app.player.engine

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.xplayer.app.data.datastore.EnginePrefs
import com.xplayer.app.data.datastore.SubtitlePrefs
import com.xplayer.app.data.local.entity.KernelType
import com.xplayer.app.data.local.entity.MpvShaderEntity
import com.xplayer.app.data.source.MediaSource
import com.xplayer.app.player.model.AspectRatioMode
import com.xplayer.app.player.model.PlayerUiState
import com.xplayer.app.player.model.TrackInfo
import is.xyz.mpv.MPVLib
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

class MpvEngineImpl(
    private val context: Context,
    private val scope: CoroutineScope,
    private val enginePrefs: EnginePrefs,
    private val activeShaders: List<MpvShaderEntity> = emptyList(),
    private val onErrorFallback: ((String) -> Unit)? = null
) : PlayerEngine, MPVLib.EventObserver, SurfaceHolder.Callback {

    override val engineType: KernelType = KernelType.MPV

    private val _state = MutableStateFlow(PlayerUiState(activeEngine = KernelType.MPV))
    override val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    private var attachedSurfaceView: SurfaceView? = null
    private var progressJob: Job? = null
    private var isInitialized = false
    private var pendingStartPositionMs: Long = 0L

    init {
        initMpv()
    }

    private fun initMpv() {
        runCatching {
            MPVLib.create(context.applicationContext)
            applyMpvOptions()
            MPVLib.init()
            MPVLib.addObserver(this)
            isInitialized = true
        }.onFailure { e ->
            e.printStackTrace()
            _state.update { it.copy(error = e.localizedMessage) }
            onErrorFallback?.invoke(e.localizedMessage ?: "mpv init failed")
        }
    }

    private fun applyMpvOptions() {
        // HWDEC
        val hwdec = if (activeShaders.isNotEmpty()) {
            "mediacodec-copy"
        } else {
            enginePrefs.mpvHwdec.value
        }
        MPVLib.setOptionString("hwdec", hwdec)
        MPVLib.setOptionString("vo", "gpu")
        MPVLib.setOptionString("gpu-context", "android")
        MPVLib.setOptionString("sub-auto", "all")
        MPVLib.setOptionString("sub-font-provider", "none")
        MPVLib.setOptionString("keep-open", "yes")

        // Custom mpv.conf parsing if enabled
        if (enginePrefs.customMpvConfEnabled && enginePrefs.customMpvConf.isNotBlank()) {
            enginePrefs.customMpvConf.lines().forEach { line ->
                val trimmed = line.trim()
                if (trimmed.isNotEmpty() && !trimmed.startsWith("#") && trimmed.contains("=")) {
                    val key = trimmed.substringBefore("=").trim()
                    val value = trimmed.substringAfter("=").trim()
                    MPVLib.setOptionString(key, value)
                }
            }
        }

        // Apply external GLSL shaders if present
        if (activeShaders.isNotEmpty()) {
            val shaderList = activeShaders.filter { it.enabled }.map { it.filePath }
            if (shaderList.isNotEmpty()) {
                val shadersJoined = shaderList.joinToString(":")
                MPVLib.setOptionString("glsl-shaders", shadersJoined)
            }
        }
    }

    override fun prepare(source: MediaSource, startPositionMs: Long) {
        if (!isInitialized) return
        pendingStartPositionMs = startPositionMs

        runCatching {
            MPVLib.command(arrayOf("loadfile", source.playbackUri))

            if (startPositionMs > 0) {
                val secs = startPositionMs / 1000.0
                MPVLib.setPropertyDouble("playback-time", secs)
            }

            // Auto-mount external subtitles
            if (source is MediaSource.LocalFileSource) {
                source.externalSubtitles.forEach { sub ->
                    MPVLib.command(arrayOf("sub-add", sub.path, "auto"))
                }
            }

            MPVLib.setPropertyBoolean("pause", false)
            _state.update {
                it.copy(
                    isPlaying = true,
                    currentPositionMs = startPositionMs,
                    error = null
                )
            }
            startProgressTracker()
        }.onFailure { e ->
            e.printStackTrace()
            _state.update { it.copy(error = e.localizedMessage) }
        }
    }

    override fun play() {
        if (!isInitialized) return
        MPVLib.setPropertyBoolean("pause", false)
        _state.update { it.copy(isPlaying = true) }
        startProgressTracker()
    }

    override fun pause() {
        if (!isInitialized) return
        MPVLib.setPropertyBoolean("pause", true)
        _state.update { it.copy(isPlaying = false) }
        stopProgressTracker()
    }

    override fun seekTo(positionMs: Long) {
        if (!isInitialized) return
        val seconds = positionMs / 1000.0
        MPVLib.command(arrayOf("seek", seconds.toString(), "absolute"))
        _state.update { it.copy(currentPositionMs = positionMs) }
    }

    override fun setSpeed(speed: Float) {
        if (!isInitialized) return
        MPVLib.setPropertyDouble("speed", speed.toDouble())
        _state.update { it.copy(speed = speed) }
    }

    override fun selectAudioTrack(id: String?) {
        if (!isInitialized) return
        val aid = id ?: "no"
        MPVLib.setPropertyString("aid", aid)
        _state.update { it.copy(selectedAudioTrackId = id) }
    }

    override fun selectSubtitleTrack(id: String?) {
        if (!isInitialized) return
        val sid = id ?: "no"
        MPVLib.setPropertyString("sid", sid)
        _state.update { it.copy(selectedSubtitleTrackId = id) }
    }

    override fun addExternalSubtitle(uri: Uri, name: String) {
        if (!isInitialized) return
        val path = uri.path ?: return
        MPVLib.command(arrayOf("sub-add", path, "select"))
        refreshTracks()
    }

    override fun setSubtitleDelay(delayMs: Long) {
        if (!isInitialized) return
        val delaySec = delayMs / 1000.0
        MPVLib.setPropertyDouble("sub-delay", delaySec)
        _state.update { it.copy(subtitleDelayMs = delayMs) }
    }

    override fun setSubtitleStyle(style: SubtitlePrefs) {
        if (!isInitialized) return
        val fontScale = style.fontSizeScale.toDouble()
        MPVLib.setPropertyDouble("sub-scale", fontScale)
    }

    override fun setAspectRatio(mode: AspectRatioMode) {
        if (!isInitialized) return
        when (mode) {
            AspectRatioMode.RATIO_16_9 -> MPVLib.setPropertyDouble("video-aspect-override", 16.0 / 9.0)
            AspectRatioMode.RATIO_4_3 -> MPVLib.setPropertyDouble("video-aspect-override", 4.0 / 3.0)
            AspectRatioMode.ORIGINAL -> MPVLib.setPropertyDouble("video-aspect-override", -1.0)
            else -> MPVLib.setPropertyDouble("video-aspect-override", -1.0)
        }
        _state.update { it.copy(aspectRatioMode = mode) }
    }

    override fun captureScreenshot(callback: (Bitmap?) -> Unit) {
        val surfaceView = attachedSurfaceView
        if (surfaceView == null || !surfaceView.holder.surface.isValid) {
            callback(null)
            return
        }

        val width = surfaceView.width.coerceAtLeast(1)
        val height = surfaceView.height.coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        runCatching {
            PixelCopy.request(
                surfaceView,
                bitmap,
                { copyResult ->
                    if (copyResult == PixelCopy.SUCCESS) {
                        callback(bitmap)
                    } else {
                        callback(null)
                    }
                },
                Handler(Looper.getMainLooper())
            )
        }.onFailure {
            callback(null)
        }
    }

    override fun attachSurfaceView(surfaceView: SurfaceView) {
        attachedSurfaceView = surfaceView
        surfaceView.holder.addCallback(this)
        if (surfaceView.holder.surface.isValid) {
            MPVLib.attachSurface(surfaceView.holder.surface)
        }
    }

    override fun detachSurfaceView() {
        attachedSurfaceView?.holder?.removeCallback(this)
        attachedSurfaceView = null
        if (isInitialized) {
            MPVLib.detachSurface()
        }
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        if (isInitialized) {
            MPVLib.attachSurface(holder.surface)
        }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        // Surface resized
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        if (isInitialized) {
            MPVLib.detachSurface()
        }
    }

    // MPVLib.EventObserver callbacks
    override fun eventProperty(property: String) {
        when (property) {
            "time-pos" -> {
                val posSec = MPVLib.getPropertyDouble("time-pos") ?: 0.0
                _state.update { it.copy(currentPositionMs = (posSec * 1000).toLong()) }
            }
            "duration" -> {
                val durSec = MPVLib.getPropertyDouble("duration") ?: 0.0
                _state.update { it.copy(durationMs = (durSec * 1000).toLong()) }
            }
            "pause" -> {
                val paused = MPVLib.getPropertyBoolean("pause") ?: false
                _state.update { it.copy(isPlaying = !paused) }
            }
            "track-list" -> {
                refreshTracks()
            }
            "video-params/w", "video-params/h" -> {
                val w = MPVLib.getPropertyInt("video-params/w") ?: 0
                val h = MPVLib.getPropertyInt("video-params/h") ?: 0
                if (w > 0 && h > 0) {
                    _state.update { it.copy(videoWidth = w, videoHeight = h) }
                }
            }
            "eof-reached" -> {
                val eof = MPVLib.getPropertyBoolean("eof-reached") ?: false
                if (eof) {
                    _state.update { it.copy(isEnded = true, isPlaying = false) }
                }
            }
        }
    }

    override fun eventProperty(property: String, value: Long) {
        eventProperty(property)
    }

    override fun eventProperty(property: String, value: Boolean) {
        eventProperty(property)
    }

    override fun eventProperty(property: String, value: String) {
        eventProperty(property)
    }

    override fun event(eventId: Int) {
        // MPV event handling
    }

    private fun refreshTracks() {
        val trackCount = MPVLib.getPropertyInt("track-list/count") ?: 0
        val audioList = mutableListOf<TrackInfo>()
        val subList = mutableListOf<TrackInfo>()
        var currentAid: String? = null
        var currentSid: String? = null

        for (i in 0 until trackCount) {
            val type = MPVLib.getPropertyString("track-list/$i/type") ?: continue
            val id = MPVLib.getPropertyString("track-list/$i/id") ?: "$i"
            val title = MPVLib.getPropertyString("track-list/$i/title")
            val lang = MPVLib.getPropertyString("track-list/$i/lang")
            val codec = MPVLib.getPropertyString("track-list/$i/codec")
            val selected = MPVLib.getPropertyBoolean("track-list/$i/selected") ?: false

            val displayName = title ?: lang ?: "${type.replaceFirstChar { it.uppercase() }} #$id"

            if (type == "audio") {
                audioList.add(
                    TrackInfo(
                        id = id,
                        name = displayName,
                        language = lang,
                        codec = codec,
                        isSelected = selected
                    )
                )
                if (selected) currentAid = id
            } else if (type == "sub") {
                val isAss = codec?.lowercase()?.contains("ass") == true || codec?.lowercase()?.contains("ssa") == true
                subList.add(
                    TrackInfo(
                        id = id,
                        name = displayName,
                        language = lang,
                        codec = codec,
                        isSelected = selected,
                        isAss = isAss
                    )
                )
                if (selected) currentSid = id
            }
        }

        _state.update {
            it.copy(
                audioTracks = audioList,
                selectedAudioTrackId = currentAid,
                subtitleTracks = subList,
                selectedSubtitleTrackId = currentSid
            )
        }
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch(Dispatchers.Main) {
            while (isActive) {
                if (isInitialized) {
                    val posSec = MPVLib.getPropertyDouble("time-pos") ?: 0.0
                    val durSec = MPVLib.getPropertyDouble("duration") ?: 0.0
                    _state.update {
                        it.copy(
                            currentPositionMs = (posSec * 1000).toLong().coerceAtLeast(0L),
                            durationMs = (durSec * 1000).toLong().coerceAtLeast(0L)
                        )
                    }
                }
                delay(300L)
            }
        }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
        progressJob = null
    }

    override fun release() {
        stopProgressTracker()
        if (isInitialized) {
            runCatching {
                MPVLib.removeObserver(this)
                MPVLib.destroy()
            }
            isInitialized = false
        }
        attachedSurfaceView = null
    }
}
