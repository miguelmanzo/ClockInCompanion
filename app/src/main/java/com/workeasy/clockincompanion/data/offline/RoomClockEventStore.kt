package com.workeasy.clockincompanion.data.offline

import com.workeasy.clockincompanion.domain.model.ClockEvent
import com.workeasy.clockincompanion.domain.store.ClockEventStore
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomClockEventStore @Inject constructor(
    private val dao: ClockEventDao,
) : ClockEventStore {
    override suspend fun save(event: ClockEvent) {
        dao.insert(event.toEntity(synced = false))
    }

    override suspend fun markSynced(id: String) {
        dao.markSynced(id)
    }

    override suspend fun getPending(): List<ClockEvent> =
        dao.getPendingEvents().map { it.toClockEvent() }

    override fun observePendingCount(): Flow<Int> = dao.getPendingCountFlow()
}
