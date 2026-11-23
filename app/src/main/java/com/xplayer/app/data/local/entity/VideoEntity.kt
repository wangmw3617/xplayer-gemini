package com.xplayer.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class HdrType {
    NONE,
    HDR10,
    HDR10_PLUS,
    DOLBY_VISION
}

@Entity(
    tableName = "videos",
    indices = [
        Index(value = ["path"], unique = true),
        Index(value = ["folderPath"]),
        Index(value = ["dateAdded"])
    ]
)
data class VideoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val path: String,
    val folderPath: String,
    val title: String,
    val durationMs: Long = 0L,
    val sizeBytes: Long = 0L,
    val dateAdded: Long = 0L,
    val dateModified: Long = 0L,
    val width: Int = 0,
    val height: Int = 0,
    val videoCodec: String = "",
    val hdrType: HdrType = HdrType.NONE,
    val hasAssSubtitle: Boolean = false,
    val thumbnailPath: String? = null
)
