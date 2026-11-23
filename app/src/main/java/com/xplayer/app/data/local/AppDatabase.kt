package com.xplayer.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.xplayer.app.data.local.dao.FolderBlacklistDao
import com.xplayer.app.data.local.dao.MpvShaderDao
import com.xplayer.app.data.local.dao.PlaybackHistoryDao
import com.xplayer.app.data.local.dao.StreamUrlDao
import com.xplayer.app.data.local.dao.VideoDao
import com.xplayer.app.data.local.entity.FolderBlacklistEntity
import com.xplayer.app.data.local.entity.MpvShaderEntity
import com.xplayer.app.data.local.entity.PlaybackHistoryEntity
import com.xplayer.app.data.local.entity.StreamUrlEntity
import com.xplayer.app.data.local.entity.VideoEntity

@Database(
    entities = [
        VideoEntity::class,
        PlaybackHistoryEntity::class,
        FolderBlacklistEntity::class,
        StreamUrlEntity::class,
        MpvShaderEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun videoDao(): VideoDao
    abstract fun playbackHistoryDao(): PlaybackHistoryDao
    abstract fun folderBlacklistDao(): FolderBlacklistDao
    abstract fun streamUrlDao(): StreamUrlDao
    abstract fun mpvShaderDao(): MpvShaderDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "xplayer.db"
                ).fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
