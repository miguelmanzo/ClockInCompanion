package com.workeasy.clockincompanion.data.offline

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ClockEventDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: ClockEventEntity)

    @Query("SELECT * FROM clock_events WHERE synced = 0 ORDER BY timestamp ASC")
    suspend fun getPendingEvents(): List<ClockEventEntity>

    @Query("UPDATE clock_events SET synced = 1 WHERE id = :id")
    suspend fun markSynced(id: String)

    @Query("SELECT COUNT(*) FROM clock_events WHERE synced = 0")
    fun getPendingCountFlow(): Flow<Int>
}
