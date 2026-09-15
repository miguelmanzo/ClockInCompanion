package com.workeasy.clockincompanion.domain.usecase

import com.workeasy.clockincompanion.domain.model.ClockEvent
import com.workeasy.clockincompanion.domain.publisher.ClockEventPublisher
import javax.inject.Inject

class HandleClockInUseCase @Inject constructor(
    private val publisher: ClockEventPublisher,
) {
    suspend operator fun invoke(employeeId: Int, deviceId: String): ClockInResult {
        val event = ClockEvent(
            employeeId = employeeId,
            deviceId = deviceId,
        )
        val published = publisher.publish(event)
        return if (published) {
            ClockInResult.Published(event)
        } else {
            ClockInResult.PublishFailed(event)
        }
    }
}

sealed interface ClockInResult {
    val event: ClockEvent

    data class Published(override val event: ClockEvent) : ClockInResult
    data class PublishFailed(override val event: ClockEvent) : ClockInResult
}
