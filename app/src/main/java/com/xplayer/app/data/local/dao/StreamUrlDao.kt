package com.xplayer.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.xplayer.app.data.local.entity.StreamUrlEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StreamUrlDao {
    @Query("SELECT * FROM stream_urls ORDER BY lastPlayedAt DESC")
    fun getAllStreamsFlow(): Flow<List<StreamUrlEntity>>

    @Query("SELECT * FROM stream_urls WHERE url = :url LIMIT 1")
    suspend fun getStreamByUrl(url: String): StreamUrlEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStream(stream: StreamUrlEntity): Long

    @Update
    suspend fun updateStream(stream: StreamUrlEntity)

    @Query("DELETE FROM stream_urls WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM stream_urls WHERE url = :url")
    suspend fun deleteByUrl(url: String)
}
