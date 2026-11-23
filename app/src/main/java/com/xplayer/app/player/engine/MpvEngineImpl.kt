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
import dev.jdtech.mpv.MPVLib
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

    private var mpv: MPVLib? = null
    private var attachedSurfaceView: SurfaceView? = null
    private var progressJob: Job? = null
    private var isInitialized = false
    private var pendingStartPositionMs: Long = 0L

    init {
        initMpv()
    }

    private fun initMpv() {
        runCatching {
            val instance = MPVLib.create(context.applicationContext) ?: error("Failed to create MPVLib")
            mpv = instance
            applyMpvOptions(instance)
            instance.init()
            observeProperties(instance)
            instance.addObserver(this)
            isInitialized = true
        }.onFailure { e ->
            e.printStackTrace()
            _state.update { it.copy(error = e.localizedMessage) }
            onErrorFallback?.invoke(e.localizedMessage ?: "mpv init failed")
        }
    }

    private fun applyMpvOptions(instance: MPVLib) {
        // HWDEC
        val hwdec = if (activeShaders.isNotEmpty()) {
            "mediacodec-copy"
        } else {
            enginePrefs.mpvHwdec.value
        }
        instance.setOptionString("hwdec", hwdec)
        instance.setOptionString("vo", "gpu")
        instance.setOptionString("gpu-context", "android")
        instance.setOptionString("sub-auto", "all")
        instance.setOptionString("sub-font-provider", "none")
        instance.setOptionString("keep-open", "yes")

        // Custom mpv.conf parsing if enabled
        if (enginePrefs.customMpvConfEnabled && enginePrefs.customMpvConf.isNotBlank()) {
            enginePrefs.customMpvConf.lines().forEach { line ->
                val trimmed = line.trim()
                if (trimmed.isNotEmpty() && !trimmed.startsWith("#") && trimmed.contains("=")) {
                    val key = trimmed.substringBefore("=").trim()
                    val value = trimmed.substringAfter("=").trim()
                    instance.setOptionString(key, value)
                }
            }
        }

        // Apply external GLSL shaders if present
        if (activeShaders.isNotEmpty()) {
            val shaderList = activeShaders.filter { it.enabled }.map { it.filePath }
            if (shaderList.isNotEmpty()) {
                val shadersJoined = shaderList.joinToString(":")
                instance.setOptionString("glsl-shaders", shadersJoined)
            }
        }
    }

    private fun observeProperties(instance: MPVLib) {
        instance.observeProperty("time-pos", MPVLib.MpvFormat.MPV_FORMAT_DOUBLE)
        instance.observeProperty("duration", MPVLib.MpvFormat.MPV_FORMAT_DOUBLE)
        instance.observeProperty("pause", MPVLib.MpvFormat.MPV_FORMAT_FLAG)
        instance.observeProperty("track-list", MPVLib.MpvFormat.MPV_FORMAT_NONE)
        instance.observeProperty("video-params/w", MPVLib.MpvFormat.MPV_FORMAT_INT64)
        instance.observeProperty("video-params/h", MPVLib.MpvFormat.MPV_FORMAT_INT64)
        instance.observeProperty("eof-reached", MPVLib.MpvFormat.MPV_FORMAT_FLAG)
    }

    override fun prepare(source: MediaSource, startPositionMs: Long) {
        if (!isInitialized) return
        pendingStartPositionMs = startPositionMs

        runCatching {
            mpv?.command(arrayOf("loadfile", source.playbackUri))

            if (startPositionMs > 0) {
                val secs = startPositionMs / 1000.0
                mpv?.setPropertyDouble("playback-time", secs)
            }

            // Auto-mount external subtitles
            if (source is MediaSource.LocalFileSource) {
                source.externalSubtitles.forEach { sub ->
                    mpv?.command(arrayOf("sub-add", sub.path, "auto"))
                }
            }

            mpv?.setPropertyBoolean("pause", false)
            _state.update {
                it.copy(
                    isPlaying = true,
                    currentPositionMs = startPositionMs,
                    error = null,
                    isBuffering = false
                )
            }
            startProgressTracker()
        }.onFailure { e ->
            _state.update { it.copy(error = e.localizedMessage) }
            onErrorFallback?.invoke(e.localizedMessage ?: "mpv prepare error")
        }
    }

    override fun play() {
        mpv?.setPropertyBoolean("pause", false)
        _state.update { it.copy(isPlaying = true) }
    }

    override fun pause() {
        mpv?.setPropertyBoolean("pause", true)
        _state.update { it.copy(isPlaying = false) }
    }

    override fun seekTo(positionMs: Long) {
        val seconds = positionMs / 1000.0
        mpv?.command(arrayOf("seek", seconds.toString(), "absolute"))
        _state.update { it.copy(currentPositionMs = positionMs) }
    }

    override fun setSpeed(speed: Float) {
        mpv?.setPropertyDouble("speed", speed.toDouble())
        _state.update { it.copy(speed = speed) }
    }

    override fun selectAudioTrack(id: String?) {
        val aid = id ?: "no"
        mpv?.setPropertyString("aid", aid)
        _state.update { it.copy(selectedAudioTrackId = id) }
    }

    override fun selectSubtitleTrack(id: String?) {
        val sid = id ?: "no"
        mpv?.setPropertyString("sid", sid)
        _state.update { it.copy(selectedSubtitleTrackId = id) }
    }

    override fun addExternalSubtitle(uri: Uri, name: String) {
        val path = uri.path ?: uri.toString()
        mpv?.command(arrayOf("sub-add", path, "select"))
        refreshTracks()
    }

    override fun setSubtitleDelay(delayMs: Long) {
        val delaySec = delayMs / 1000.0
        mpv?.setPropertyDouble("sub-delay", delaySec)
        _state.update { it.copy(subtitleDelayMs = delayMs) }
    }

    override fun setSubtitleStyle(style: SubtitlePrefs) {
        mpv?.setPropertyDouble("sub-scale", style.fontSizeScale.toDouble().coerceIn(0.5, 3.0))
    }

    override fun setAspectRatio(mode: AspectRatioMode) {
        when (mode) {
            AspectRatioMode.RATIO_16_9 -> mpv?.setPropertyDouble("video-aspect-override", 16.0 / 9.0)
            AspectRatioMode.RATIO_4_3 -> mpv?.setPropertyDouble("video-aspect-override", 4.0 / 3.0)
            AspectRatioMode.ORIGINAL -> mpv?.setPropertyDouble("video-aspect-override", -1.0)
            else -> mpv?.setPropertyDouble("video-aspect-override", -1.0)
        }
        _state.update { it.copy(aspectRatioMode = mode) }
    }

    override fun captureScreenshot(callback: (Bitmap?) -> Unit) {
        val sv = attachedSurfaceView
        if (sv != null && sv.holder.surface.isValid) {
            val bitmap = Bitmap.createBitmap(sv.width, sv.height, Bitmap.Config.ARGB_8888)
            try {
                PixelCopy.request(
                    sv,
                    bitmap,
                    { result ->
                        if (result == PixelCopy.SUCCESS) {
                            callback(bitmap)
                        } else {
                            callback(null)
                        }
                    },
                    Handler(Looper.getMainLooper())
                )
            } catch (e: Exception) {
                callback(null)
            }
        } else {
            callback(null)
        }
    }

    override fun attachSurfaceView(surfaceView: SurfaceView) {
        attachedSurfaceView = surfaceView
        surfaceView.holder.addCallback(this)
        if (surfaceView.holder.surface.isValid) {
            mpv?.attachSurface(surfaceView.holder.surface)
        }
    }

    override fun detachSurfaceView() {
        attachedSurfaceView?.holder?.removeCallback(this)
        attachedSurfaceView = null
        mpv?.detachSurface()
    }

    // SurfaceHolder.Callback
    override fun surfaceCreated(holder: SurfaceHolder) {
        mpv?.attachSurface(holder.surface)
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        runCatching { mpv?.setPropertyString("android-surface-size", "${width}x$height") }
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        mpv?.detachSurface()
    }

    // MPVLib.EventObserver callbacks
    override fun eventProperty(property: String) {
        when (property) {
            "time-pos" -> {
                val posSec = mpv?.getPropertyDouble("time-pos") ?: 0.0
                _state.update { it.copy(currentPositionMs = (posSec * 1000).toLong()) }
            }
            "duration" -> {
                val durSec = mpv?.getPropertyDouble("duration") ?: 0.0
                _state.update { it.copy(durationMs = (durSec * 1000).toLong()) }
            }
            "pause" -> {
                val paused = mpv?.getPropertyBoolean("pause") ?: false
                _state.update { it.copy(isPlaying = !paused) }
            }
            "track-list" -> {
                refreshTracks()
            }
            "video-params/w", "video-params/h" -> {
                val w = mpv?.getPropertyInt("video-params/w") ?: 0
                val h = mpv?.getPropertyInt("video-params/h") ?: 0
                if (w > 0 && h > 0) {
                    _state.update { it.copy(videoWidth = w, videoHeight = h) }
                }
            }
            "eof-reached" -> {
                val eof = mpv?.getPropertyBoolean("eof-reached") ?: false
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

    override fun eventProperty(property: String, value: Double) {
        eventProperty(property)
    }

    override fun event(eventId: Int) {
        // MPV event handling
    }

    private fun refreshTracks() {
        val trackCount = mpv?.getPropertyInt("track-list/count") ?: 0
        val audioList = mutableListOf<TrackInfo>()
        val subList = mutableListOf<TrackInfo>()
        var currentAid: String? = null
        var currentSid: String? = null

        for (i in 0 until trackCount) {
            val type = mpv?.getPropertyString("track-list/$i/type") ?: continue
            val id = mpv?.getPropertyString("track-list/$i/id") ?: "$i"
            val title = mpv?.getPropertyString("track-list/$i/title")
            val lang = mpv?.getPropertyString("track-list/$i/lang")
            val codec = mpv?.getPropertyString("track-list/$i/codec")
            val selected = mpv?.getPropertyBoolean("track-list/$i/selected") ?: false

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
                    val posSec = mpv?.getPropertyDouble("time-pos") ?: 0.0
                    val durSec = mpv?.getPropertyDouble("duration") ?: 0.0
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
                mpv?.removeObserver(this)
                mpv?.destroy()
            }
            isInitialized = false
            mpv = null
        }
        attachedSurfaceView = null
    }
}
