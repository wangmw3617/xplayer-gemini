package com.xplayer.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "mpv_shaders")
data class MpvShaderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val filePath: String,
    val enabled: Boolean = true,
    val orderIndex: Int = 0
)
