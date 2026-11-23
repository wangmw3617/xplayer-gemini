package com.xplayer.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "folder_blacklist")
data class FolderBlacklistEntity(
    @PrimaryKey
    val folderPath: String,
    val addedAt: Long = System.currentTimeMillis()
)
