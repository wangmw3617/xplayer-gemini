package com.xplayer.app.player.engine

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import android.view.Surface
import android.view.SurfaceView
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.SeekParameters
import com.xplayer.app.data.datastore.SubtitlePrefs
import com.xplayer.app.data.local.entity.KernelType
import com.xplayer.app.data.source.MediaSource
import com.xplayer.app.player.model.AspectRatioMode
import com.xplayer.app.player.model.PlayerUiState
import com.xplayer.app.player.model.TrackInfo
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

@OptIn(UnstableApi::class)
class Media3EngineImpl(
    private val context: Context,
    private val scope: CoroutineScope,
    private val frameRateMatching: Boolean = true,
    private val onErrorFallback: ((PlaybackException) -> Unit)? = null
) : PlayerEngine {

    override val engineType: KernelType = KernelType.MEDIA3

    private val _state = MutableStateFlow(PlayerUiState(activeEngine = KernelType.MEDIA3))
    override val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    private var exoPlayer: ExoPlayer? = null
    private var attachedSurfaceView: SurfaceView? = null
    private var progressJob: Job? = null
    private var currentMediaSource: MediaSource? = null

    init {
        initPlayer()
    }

    private fun initPlayer() {
        val renderersFactory = DefaultRenderersFactory(context)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
            .setEnableDecoderFallback(true)

        val player = ExoPlayer.Builder(context, renderersFactory)
            .setSeekParameters(SeekParameters.DEFAULT)
            .build()

        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                val isBuffering = playbackState == Player.STATE_BUFFERING
                val isEnded = playbackState == Player.STATE_ENDED
                _state.update {
                    it.copy(
                        isBuffering = isBuffering,
                        isEnded = isEnded,
                        durationMs = player.duration.coerceAtLeast(0L)
                    )
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _state.update { it.copy(isPlaying = isPlaying) }
                if (isPlaying) {
                    startProgressTracker()
                } else {
                    stopProgressTracker()
                }
            }

            override fun onVideoSizeChanged(videoSize: VideoSize) {
                _state.update {
                    it.copy(
                        videoWidth = videoSize.width,
                        videoHeight = videoSize.height
                    )
                }
                updateFrameRateMatching(videoSize)
            }

            override fun onTracksChanged(tracks: Tracks) {
                updateTracks(tracks)
            }

            override fun onPlayerError(error: PlaybackException) {
                _state.update { it.copy(error = error.localizedMessage) }
                onErrorFallback?.invoke(error)
            }
        })

        exoPlayer = player
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch(Dispatchers.Main) {
            while (isActive) {
                exoPlayer?.let { player ->
                    val pos = player.currentPosition.coerceAtLeast(0L)
                    val dur = player.duration.coerceAtLeast(0L)
                    val buf = player.bufferedPosition.coerceAtLeast(0L)
                    _state.update {
                        it.copy(
                            currentPositionMs = pos,
                            durationMs = dur,
                            bufferedPositionMs = buf
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

    private fun updateTracks(tracks: Tracks) {
        val audioList = mutableListOf<TrackInfo>()
        val subList = mutableListOf<TrackInfo>()
        var selectedAudioId: String? = null
        var selectedSubId: String? = null

        for (group in tracks.groups) {
            val trackType = group.type
            val trackGroup = group.mediaTrackGroup

            for (i in 0 until trackGroup.length) {
                val format = trackGroup.getFormat(i)
                val trackId = "${trackGroup.id}_$i"
                val isSelected = group.isTrackSelected(i)

                if (trackType == C.TRACK_TYPE_AUDIO) {
                    val channelsStr = when (format.channelCount) {
                        6 -> "5.1"
                        8 -> "7.1"
                        2 -> "Stereo"
                        1 -> "Mono"
                        else -> "${format.channelCount}ch"
                    }
                    val name = format.label ?: format.language ?: "Audio ${audioList.size + 1}"
                    val info = TrackInfo(
                        id = trackId,
                        name = name,
                        language = format.language,
                        codec = format.sampleMimeType,
                        channels = channelsStr,
                        isSelected = isSelected
                    )
                    audioList.add(info)
                    if (isSelected) selectedAudioId = trackId
                } else if (trackType == C.TRACK_TYPE_TEXT) {
                    val isAss = format.sampleMimeType == MimeTypes.TEXT_SSA ||
                            format.label?.lowercase()?.contains("ass") == true
                    val name = format.label ?: format.language ?: "Subtitle ${subList.size + 1}"
                    val info = TrackInfo(
                        id = trackId,
                        name = name,
                        language = format.language,
                        codec = format.sampleMimeType,
                        isSelected = isSelected,
                        isAss = isAss
                    )
                    subList.add(info)
                    if (isSelected) selectedSubId = trackId
                }
            }
        }

        _state.update {
            it.copy(
                audioTracks = audioList,
                selectedAudioTrackId = selectedAudioId,
                subtitleTracks = subList,
                selectedSubtitleTrackId = selectedSubId
            )
        }
    }

    private fun updateFrameRateMatching(videoSize: VideoSize) {
        if (!frameRateMatching || Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return
        val surface = attachedSurfaceView?.holder?.surface ?: return
        if (surface.isValid) {
            // Default 24fps or detect from format
            runCatching {
                surface.setFrameRate(24f, Surface.FRAME_RATE_COMPATIBILITY_DEFAULT)
            }
        }
    }

    override fun prepare(source: MediaSource, startPositionMs: Long) {
        currentMediaSource = source
        val player = exoPlayer ?: return

        val mediaItemBuilder = MediaItem.Builder().setUri(source.playbackUri)

        // Attach local external subtitles if available
        if (source is MediaSource.LocalFileSource) {
            val subConfigs = source.externalSubtitles.map { subFile ->
                val mimeType = if (subFile.extension == "ass" || subFile.extension == "ssa") {
                    MimeTypes.TEXT_SSA
                } else if (subFile.extension == "vtt") {
                    MimeTypes.TEXT_VTT
                } else {
                    MimeTypes.APPLICATION_SUBRIP
                }
                MediaItem.SubtitleConfiguration.Builder(Uri.fromFile(File(subFile.path)))
                    .setMimeType(mimeType)
                    .setLabel(subFile.name)
                    .setLanguage("und")
                    .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                    .build()
            }
            if (subConfigs.isNotEmpty()) {
                mediaItemBuilder.setSubtitleConfigurations(subConfigs)
            }
        }

        val mediaItem = mediaItemBuilder.build()
        player.setMediaItem(mediaItem, startPositionMs.coerceAtLeast(0L))
        player.prepare()
        player.playWhenReady = true

        _state.update {
            it.copy(
                currentPositionMs = startPositionMs,
                error = null
            )
        }
    }

    override fun play() {
        exoPlayer?.play()
    }

    override fun pause() {
        exoPlayer?.pause()
    }

    override fun seekTo(positionMs: Long) {
        exoPlayer?.seekTo(positionMs)
        _state.update { it.copy(currentPositionMs = positionMs) }
    }

    override fun setSpeed(speed: Float) {
        exoPlayer?.playbackParameters = PlaybackParameters(speed)
        _state.update { it.copy(speed = speed) }
    }

    override fun selectAudioTrack(id: String?) {
        val player = exoPlayer ?: return
        val tracks = player.currentTracks

        if (id == null) return
        for (group in tracks.groups) {
            if (group.type == C.TRACK_TYPE_AUDIO) {
                for (i in 0 until group.mediaTrackGroup.length) {
                    val trackId = "${group.mediaTrackGroup.id}_$i"
                    if (trackId == id) {
                        player.trackSelectionParameters = player.trackSelectionParameters
                            .buildUpon()
                            .setOverrideForType(
                                TrackSelectionOverride(group.mediaTrackGroup, listOf(i))
                            )
                            .build()
                        _state.update { it.copy(selectedAudioTrackId = id) }
                        return
                    }
                }
            }
        }
    }

    override fun selectSubtitleTrack(id: String?) {
        val player = exoPlayer ?: return
        if (id == null) {
            // Disable subtitles
            player.trackSelectionParameters = player.trackSelectionParameters
                .buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                .build()
            _state.update { it.copy(selectedSubtitleTrackId = null) }
            return
        }

        val tracks = player.currentTracks
        for (group in tracks.groups) {
            if (group.type == C.TRACK_TYPE_TEXT) {
                for (i in 0 until group.mediaTrackGroup.length) {
                    val trackId = "${group.mediaTrackGroup.id}_$i"
                    if (trackId == id) {
                        player.trackSelectionParameters = player.trackSelectionParameters
                            .buildUpon()
                            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                            .setOverrideForType(
                                TrackSelectionOverride(group.mediaTrackGroup, listOf(i))
                            )
                            .build()
                        _state.update { it.copy(selectedSubtitleTrackId = id) }
                        return
                    }
                }
            }
        }
    }

    override fun addExternalSubtitle(uri: Uri, name: String) {
        val player = exoPlayer ?: return
        val ext = uri.lastPathSegment?.substringAfterLast('.', "")?.lowercase() ?: ""
        val mimeType = when (ext) {
            "ass", "ssa" -> MimeTypes.TEXT_SSA
            "vtt" -> MimeTypes.TEXT_VTT
            else -> MimeTypes.APPLICATION_SUBRIP
        }
        val subConfig = MediaItem.SubtitleConfiguration.Builder(uri)
            .setMimeType(mimeType)
            .setLabel(name)
            .setLanguage("und")
            .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
            .build()

        val currentItem = player.currentMediaItem ?: return
        val currentSubs = currentItem.localConfiguration?.subtitleConfigurations ?: emptyList()
        val newSubs = currentSubs + subConfig
        val newItem = currentItem.buildUpon().setSubtitleConfigurations(newSubs).build()

        val pos = player.currentPosition
        player.setMediaItem(newItem, pos)
        player.prepare()
    }

    override fun setSubtitleDelay(delayMs: Long) {
        _state.update { it.copy(subtitleDelayMs = delayMs) }
        // Media3 applies subtitle delay through track output or player state
    }

    override fun setSubtitleStyle(style: SubtitlePrefs) {
        // Styled on Compose SubtitleView or Media3 SubtitleView
    }

    override fun setAspectRatio(mode: AspectRatioMode) {
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
        exoPlayer?.setVideoSurfaceView(surfaceView)
    }

    override fun detachSurfaceView() {
        exoPlayer?.clearVideoSurfaceView(attachedSurfaceView)
        attachedSurfaceView = null
    }

    override fun release() {
        stopProgressTracker()
        exoPlayer?.release()
        exoPlayer = null
        attachedSurfaceView = null
    }
}
