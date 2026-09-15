package com.workeasy.clockincompanion.domain.publisher

import com.workeasy.clockincompanion.domain.model.ClockEvent

interface ClockEventPublisher {
    /** Returns true if MQTT publish completed successfully. */
    suspend fun publish(event: ClockEvent): Boolean
}
