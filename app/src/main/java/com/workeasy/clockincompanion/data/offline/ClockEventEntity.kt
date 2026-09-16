package com.workeasy.clockincompanion.data.offline

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.workeasy.clockincompanion.domain.model.ClockEvent

@Entity(tableName = "clock_events")
data class ClockEventEntity(
    @PrimaryKey val id: String,
    val employeeId: Int,
    val eventType: String,
    val deviceId: String,
    val timestamp: Long,
    val synced: Boolean = false,
)

fun ClockEvent.toEntity(synced: Boolean = false) = ClockEventEntity(
    id = id,
    employeeId = employeeId,
    eventType = eventType,
    deviceId = deviceId,
    timestamp = timestampEpochMs,
    synced = synced,
)

fun ClockEventEntity.toClockEvent() = ClockEvent(
    id = id,
    employeeId = employeeId,
    eventType = eventType,
    deviceId = deviceId,
    timestampEpochMs = timestamp,
)
