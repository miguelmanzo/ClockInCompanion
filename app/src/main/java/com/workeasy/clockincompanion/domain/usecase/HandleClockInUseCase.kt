package com.workeasy.clockincompanion.domain.usecase

import com.workeasy.clockincompanion.domain.model.ClockEvent
import com.workeasy.clockincompanion.domain.publisher.ClockEventPublisher
import com.workeasy.clockincompanion.domain.store.ClockEventStore
import com.workeasy.clockincompanion.domain.store.OfflineFlushScheduler
import javax.inject.Inject

class HandleClockInUseCase @Inject constructor(
    private val store: ClockEventStore,
    private val publisher: ClockEventPublisher,
    private val flushScheduler: OfflineFlushScheduler,
) {
    /**
     * Persist-first: the local queue is the source of truth.
     * On publish failure, schedule a flush when network returns.
     */
    suspend operator fun invoke(employeeId: Int, deviceId: String): ClockInResult {
        val event = ClockEvent(
            employeeId = employeeId,
            deviceId = deviceId,
        )
        store.save(event)

        val published = publisher.publish(event)
        return if (published) {
            store.markSynced(event.id)
            ClockInResult.Published(event)
        } else {
            flushScheduler.schedule()
            ClockInResult.Queued(event)
        }
    }
}

sealed interface ClockInResult {
    val event: ClockEvent

    data class Published(override val event: ClockEvent) : ClockInResult
    data class Queued(override val event: ClockEvent) : ClockInResult
}
