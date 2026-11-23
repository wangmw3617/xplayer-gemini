package com.xplayer.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.xplayer.app.data.local.entity.MpvShaderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MpvShaderDao {
    @Query("SELECT * FROM mpv_shaders ORDER BY orderIndex ASC")
    fun getAllShadersFlow(): Flow<List<MpvShaderEntity>>

    @Query("SELECT * FROM mpv_shaders WHERE enabled = 1 ORDER BY orderIndex ASC")
    suspend fun getEnabledShaders(): List<MpvShaderEntity>

    @Query("SELECT * FROM mpv_shaders WHERE id = :id LIMIT 1")
    suspend fun getShaderById(id: Long): MpvShaderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShader(shader: MpvShaderEntity): Long

    @Update
    suspend fun updateShader(shader: MpvShaderEntity)

    @Query("DELETE FROM mpv_shaders WHERE id = :id")
    suspend fun deleteById(id: Long)
}
