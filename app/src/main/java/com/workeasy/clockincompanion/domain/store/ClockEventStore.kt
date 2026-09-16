package com.workeasy.clockincompanion.domain.store

import com.workeasy.clockincompanion.domain.model.ClockEvent
import kotlinx.coroutines.flow.Flow

interface ClockEventStore {
    suspend fun save(event: ClockEvent)
    suspend fun markSynced(id: String)
    suspend fun getPending(): List<ClockEvent>
    fun observePendingCount(): Flow<Int>
}
