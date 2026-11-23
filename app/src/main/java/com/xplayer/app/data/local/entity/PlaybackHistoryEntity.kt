package com.xplayer.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class SourceType {
    LOCAL,
    NETWORK
}

enum class KernelType {
    AUTO,
    MEDIA3,
    MPV
}

@Entity(
    tableName = "playback_history",
    indices = [
        Index(value = ["mediaId"], unique = true),
        Index(value = ["lastPlayedAt"])
    ]
)
data class PlaybackHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val mediaId: String, // File path or stream URL
    val sourceType: SourceType = SourceType.LOCAL,
    val title: String,
    val lastPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val lastPlayedAt: Long = System.currentTimeMillis(),
    val preferredEngine: KernelType = KernelType.AUTO,
    val subtitleDelayMs: Long = 0L,
    val audioTrackId: String? = null,
    val subtitleTrackId: String? = null
) {
    val progressRatio: Float
        get() = if (durationMs > 0) (lastPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f

    val isFinished: Boolean
        get() = durationMs > 0 && lastPositionMs >= (durationMs * 0.95f)
}
