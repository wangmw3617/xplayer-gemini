package com.xplayer.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.xplayer.app.data.local.entity.PlaybackHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaybackHistoryDao {
    @Query("SELECT * FROM playback_history ORDER BY lastPlayedAt DESC LIMIT :limit")
    fun getRecentHistoryFlow(limit: Int = 15): Flow<List<PlaybackHistoryEntity>>

    @Query("SELECT * FROM playback_history ORDER BY lastPlayedAt DESC LIMIT 1")
    fun getLatestPlaybackHistoryFlow(): Flow<PlaybackHistoryEntity?>

    @Query("SELECT * FROM playback_history WHERE mediaId = :mediaId LIMIT 1")
    suspend fun getHistoryByMediaId(mediaId: String): PlaybackHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(history: PlaybackHistoryEntity)

    @Update
    suspend fun update(history: PlaybackHistoryEntity)

    @Query("DELETE FROM playback_history WHERE mediaId = :mediaId")
    suspend fun deleteByMediaId(mediaId: String)

    @Query("DELETE FROM playback_history")
    suspend fun clearAll()
}
