package com.xplayer.app.player.engine

import android.graphics.Bitmap
import android.net.Uri
import android.view.SurfaceView
import com.xplayer.app.data.datastore.SubtitlePrefs
import com.xplayer.app.data.local.entity.KernelType
import com.xplayer.app.data.source.MediaSource
import com.xplayer.app.player.model.AspectRatioMode
import com.xplayer.app.player.model.PlayerUiState
import kotlinx.coroutines.flow.StateFlow

interface PlayerEngine {
    val engineType: KernelType
    val state: StateFlow<PlayerUiState>

    fun prepare(source: MediaSource, startPositionMs: Long = 0L)
    fun play()
    fun pause()
    fun seekTo(positionMs: Long)
    fun setSpeed(speed: Float)

    fun selectAudioTrack(id: String?)
    fun selectSubtitleTrack(id: String?)
    fun addExternalSubtitle(uri: Uri, name: String)
    fun setSubtitleDelay(delayMs: Long)
    fun setSubtitleStyle(style: SubtitlePrefs)

    fun setAspectRatio(mode: AspectRatioMode)
    fun captureScreenshot(callback: (Bitmap?) -> Unit)

    fun attachSurfaceView(surfaceView: SurfaceView)
    fun detachSurfaceView()

    fun release()
}
