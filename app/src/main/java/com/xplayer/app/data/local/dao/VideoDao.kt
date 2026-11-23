package com.xplayer.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.xplayer.app.data.local.entity.VideoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VideoDao {
    @Query("SELECT * FROM videos ORDER BY dateAdded DESC")
    fun getAllVideosFlow(): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos WHERE folderPath = :folderPath ORDER BY title ASC")
    fun getVideosByFolderFlow(folderPath: String): Flow<List<VideoEntity>>

    @Query("SELECT DISTINCT folderPath FROM videos")
    fun getAllFolderPathsFlow(): Flow<List<String>>

    @Query("SELECT * FROM videos WHERE title LIKE '%' || :query || '%' OR path LIKE '%' || :query || '%' ORDER BY title ASC")
    fun searchVideosFlow(query: String): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos WHERE path = :path LIMIT 1")
    suspend fun getVideoByPath(path: String): VideoEntity?

    @Query("SELECT * FROM videos WHERE id = :id LIMIT 1")
    suspend fun getVideoById(id: Long): VideoEntity?

    @Query("SELECT * FROM videos")
    suspend fun getAllVideos(): List<VideoEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideos(videos: List<VideoEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideo(video: VideoEntity): Long

    @Update
    suspend fun updateVideo(video: VideoEntity)

    @Query("DELETE FROM videos WHERE path = :path")
    suspend fun deleteByPath(path: String)

    @Query("DELETE FROM videos WHERE path NOT IN (:existingPaths)")
    suspend fun deleteOldVideos(existingPaths: List<String>)

    @Query("DELETE FROM videos")
    suspend fun clearAll()
}
