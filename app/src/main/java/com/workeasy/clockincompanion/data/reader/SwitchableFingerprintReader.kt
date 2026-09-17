package com.workeasy.clockincompanion.data.reader

import com.workeasy.clockincompanion.BuildConfig
import com.workeasy.clockincompanion.domain.model.ConnectionState
import com.workeasy.clockincompanion.domain.model.ScanEvent
import com.workeasy.clockincompanion.domain.model.ScanPhase
import com.workeasy.clockincompanion.domain.reader.DebugFingerprintControls
import com.workeasy.clockincompanion.domain.reader.EnrollResult
import com.workeasy.clockincompanion.domain.reader.FingerprintReader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/** Thin runtime toggle between simulated and USB readers. */
@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class SwitchableFingerprintReader @Inject constructor(
    private val simulated: SimulatedFingerprintReader,
    private val serial: SerialFingerprintReader,
) : FingerprintReader, DebugFingerprintControls {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mutex = Mutex()
    private val _useSimulated = MutableStateFlow(BuildConfig.USE_SIMULATED_READER)
    val useSimulated: StateFlow<Boolean> = _useSimulated.asStateFlow()

    private val active: FingerprintReader
        get() = if (_useSimulated.value) simulated else serial

    private val activeDebug: DebugFingerprintControls
        get() = if (_useSimulated.value) simulated else serial

    override val connectionState: StateFlow<ConnectionState> = _useSimulated
        .flatMapLatest { useSim ->
            if (useSim) simulated.connectionState else serial.connectionState
        }
        .stateIn(scope, SharingStarted.Eagerly, ConnectionState.DISCONNECTED)

    override val scanPhase: StateFlow<ScanPhase> = _useSimulated
        .flatMapLatest { useSim ->
            if (useSim) simulated.scanPhase else serial.scanPhase
        }
        .stateIn(scope, SharingStarted.Eagerly, ScanPhase.Idle)

    override fun events(): Flow<ScanEvent> = _useSimulated.flatMapLatest { useSim ->
        if (useSim) simulated.events() else serial.events()
    }

    override val supportsSimulation: Boolean
        get() = activeDebug.supportsSimulation

    override val supportsEnroll: Boolean
        get() = activeDebug.supportsEnroll

    override suspend fun connect() = active.connect()

    override suspend fun disconnect() = active.disconnect()

    override suspend fun simulateMatch(employeeId: Int) =
        activeDebug.simulateMatch(employeeId)

    override suspend fun simulateNoMatch() = activeDebug.simulateNoMatch()

    override suspend fun enrollSlot(slot: Int): EnrollResult =
        activeDebug.enrollSlot(slot)

    override suspend fun clearLibrary(): EnrollResult =
        activeDebug.clearLibrary()

    suspend fun setUseSimulated(enabled: Boolean) = mutex.withLock {
        if (_useSimulated.value == enabled) return
        active.disconnect()
        _useSimulated.value = enabled
        active.connect()
    }
}
