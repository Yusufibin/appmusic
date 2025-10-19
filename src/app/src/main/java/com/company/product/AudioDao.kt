package com.company.product

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface AudioDao {
    @Query("SELECT * FROM presets")
    suspend fun getAllPresets(): List<Preset>

    @Insert
    suspend fun insertPreset(preset: Preset)

    @Query("SELECT * FROM logs ORDER BY timestamp DESC LIMIT 5")
    suspend fun getRecentLogs(): List<LogEntry>

    @Insert
    suspend fun insertLog(log: LogEntry)

    @Query("DELETE FROM logs WHERE id = :id")
    suspend fun deleteLog(id: Long)
}