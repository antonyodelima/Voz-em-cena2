package com.sagun12.vozemcena.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface VoiceDao {
    @Query("SELECT * FROM voices ORDER BY isCustom DESC, name ASC")
    fun getAllVoices(): Flow<List<VoiceEntity>>

    @Query("SELECT * FROM voices WHERE isCustom = 1 ORDER BY createdAt DESC")
    fun getCustomVoices(): Flow<List<VoiceEntity>>

    @Query("SELECT * FROM voices WHERE id = :voiceId")
    suspend fun getVoiceById(voiceId: String): VoiceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVoices(voices: List<VoiceEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVoice(voice: VoiceEntity)

    @Update
    suspend fun updateVoice(voice: VoiceEntity)

    @Delete
    suspend fun deleteVoice(voice: VoiceEntity)

    @Query("DELETE FROM voices WHERE id = :voiceId")
    suspend fun deleteVoiceById(voiceId: String)

    @Query("SELECT COUNT(*) FROM voices")
    suspend fun getCount(): Int
}
