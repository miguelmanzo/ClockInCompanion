package com.workeasy.clockincompanion.data.reader

import com.workeasy.clockincompanion.domain.model.ConnectionState
import com.workeasy.clockincompanion.domain.model.ScanEvent
import com.workeasy.clockincompanion.domain.reader.DebugFingerprintControls
import com.workeasy.clockincompanion.domain.reader.EnrollResult
import com.workeasy.clockincompanion.domain.reader.FingerprintReader
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SimulatedFingerprintReader @Inject constructor() :
    FingerprintReader,
    DebugFingerprintControls {

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _events = MutableSharedFlow<ScanEvent>(extraBufferCapacity = 8)
    override fun events(): Flow<ScanEvent> = _events.asSharedFlow()

    override val supportsSimulation: Boolean = true
    override val supportsEnroll: Boolean = true

    override suspend fun connect() {
        _connectionState.value = ConnectionState.CONNECTING
        delay(200)
        _connectionState.value = ConnectionState.CONNECTED
    }

    override suspend fun disconnect() {
        _connectionState.value = ConnectionState.DISCONNECTED
    }

    override suspend fun simulateMatch(employeeId: Int) {
        delay(800)
        _events.emit(ScanEvent.Matched(employeeId))
    }

    override suspend fun simulateNoMatch() {
        delay(800)
        _events.emit(ScanEvent.NoMatch)
    }

    override suspend fun enrollSlot(slot: Int): EnrollResult {
        if (slot !in 1..2) return EnrollResult.Failed("Slot must be 1 or 2")
        delay(1_200)
        return EnrollResult.Success
    }
}
