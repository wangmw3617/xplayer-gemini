package com.xplayer.app.player.model

enum class AspectRatioMode {
    FIT,         // 适应屏幕（保持比例，黑边填充）
    CROP,        // 裁剪填满（消除带鱼屏黑边）
    STRETCH,     // 拉伸全屏
    RATIO_16_9,  // 强制 16:9
    RATIO_4_3,   // 强制 4:3
    ORIGINAL     // 原始视频尺寸
}

data class TrackInfo(
    val id: String,
    val name: String,
    val language: String? = null,
    val codec: String? = null,
    val channels: String? = null,
    val isSelected: Boolean = false,
    val isAss: Boolean = false
)

data class PlayerUiState(
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val isEnded: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val bufferedPositionMs: Long = 0L,
    val speed: Float = 1.0f,
    val videoWidth: Int = 0,
    val videoHeight: Int = 0,
    val audioTracks: List<TrackInfo> = emptyList(),
    val selectedAudioTrackId: String? = null,
    val subtitleTracks: List<TrackInfo> = emptyList(),
    val selectedSubtitleTrackId: String? = null,
    val subtitleDelayMs: Long = 0L,
    val aspectRatioMode: AspectRatioMode = AspectRatioMode.FIT,
    val activeEngine: com.xplayer.app.data.local.entity.KernelType = com.xplayer.app.data.local.entity.KernelType.MEDIA3,
    val error: String? = null
)
