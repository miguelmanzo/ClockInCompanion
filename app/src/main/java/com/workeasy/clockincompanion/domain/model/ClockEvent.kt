package com.workeasy.clockincompanion.domain.model

import java.util.UUID

data class ClockEvent(
    val id: String = UUID.randomUUID().toString(),
    val employeeId: Int,
    val eventType: String = "CLOCK_IN",
    val deviceId: String,
    val timestampEpochMs: Long = System.currentTimeMillis(),
)
