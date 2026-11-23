package com.xplayer.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.xplayer.app.data.local.entity.FolderBlacklistEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FolderBlacklistDao {
    @Query("SELECT * FROM folder_blacklist ORDER BY addedAt DESC")
    fun getAllBlacklistedFoldersFlow(): Flow<List<FolderBlacklistEntity>>

    @Query("SELECT folderPath FROM folder_blacklist")
    suspend fun getAllBlacklistedPaths(): List<String>

    @Query("SELECT EXISTS(SELECT 1 FROM folder_blacklist WHERE folderPath = :folderPath LIMIT 1)")
    suspend fun isBlacklisted(folderPath: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBlacklist(blacklist: FolderBlacklistEntity)

    @Query("DELETE FROM folder_blacklist WHERE folderPath = :folderPath")
    suspend fun deleteBlacklist(folderPath: String)
}
