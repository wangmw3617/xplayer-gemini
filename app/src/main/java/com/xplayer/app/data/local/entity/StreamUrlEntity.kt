package com.xplayer.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "stream_urls",
    indices = [
        Index(value = ["url"], unique = true),
        Index(value = ["lastPlayedAt"])
    ]
)
data class StreamUrlEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val url: String,
    val customTitle: String = "",
    val headersJson: String = "",
    val lastPlayedAt: Long = System.currentTimeMillis()
)
