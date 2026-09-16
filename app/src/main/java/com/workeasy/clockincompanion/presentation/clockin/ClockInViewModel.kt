package com.workeasy.clockincompanion.presentation.clockin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workeasy.clockincompanion.data.mqtt.MqttConfig
import com.workeasy.clockincompanion.domain.model.ConnectionState
import com.workeasy.clockincompanion.domain.model.ScanEvent
import com.workeasy.clockincompanion.domain.reader.DebugFingerprintControls
import com.workeasy.clockincompanion.domain.reader.EnrollResult
import com.workeasy.clockincompanion.domain.reader.FingerprintReader
import com.workeasy.clockincompanion.domain.store.ClockEventStore
import com.workeasy.clockincompanion.domain.usecase.ClockInResult
import com.workeasy.clockincompanion.domain.usecase.HandleClockInUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ClockInViewModel @Inject constructor(
    private val fingerprintReader: FingerprintReader,
    private val debugControls: DebugFingerprintControls,
    private val handleClockIn: HandleClockInUseCase,
    private val mqttConfig: MqttConfig,
    clockEventStore: ClockEventStore,
) : ViewModel() {

    private val _lastEvent = MutableStateFlow<ScanEvent?>(null)
    private val _isScanning = MutableStateFlow(false)
    private val _enrollStatus = MutableStateFlow<String?>(null)
    private val _isEnrolling = MutableStateFlow(false)
    private val _publishStatus = MutableStateFlow<String?>(null)

    val connectionState: StateFlow<ConnectionState> = fingerprintReader.connectionState
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ConnectionState.DISCONNECTED,
        )

    val lastEvent: StateFlow<ScanEvent?> = _lastEvent.asStateFlow()
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()
    val enrollStatus: StateFlow<String?> = _enrollStatus.asStateFlow()
    val isEnrolling: StateFlow<Boolean> = _isEnrolling.asStateFlow()
    val publishStatus: StateFlow<String?> = _publishStatus.asStateFlow()
    val brokerHost: StateFlow<String> = mqttConfig.brokerHost

    val pendingCount: StateFlow<Int> = clockEventStore.observePendingCount()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = 0,
        )

    val supportsSimulation: Boolean = debugControls.supportsSimulation
    val supportsEnroll: Boolean = debugControls.supportsEnroll

    init {
        viewModelScope.launch {
            fingerprintReader.connect()
        }
        viewModelScope.launch {
            fingerprintReader.events().collect { event ->
                _lastEvent.value = event
                _isScanning.value = false
                if (event is ScanEvent.Matched) {
                    publishClockIn(event.employeeId)
                }
            }
        }
    }

    fun onBrokerHostChanged(host: String) {
        mqttConfig.updateBrokerHost(host)
    }

    fun onSimulateMatch() {
        if (!supportsSimulation || _isScanning.value || _isEnrolling.value) return
        viewModelScope.launch {
            _isScanning.value = true
            debugControls.simulateMatch()
        }
    }

    fun onSimulateNoMatch() {
        if (!supportsSimulation || _isScanning.value || _isEnrolling.value) return
        viewModelScope.launch {
            _isScanning.value = true
            debugControls.simulateNoMatch()
        }
    }

    fun onEnrollSlot(slot: Int) {
        if (!supportsEnroll || _isEnrolling.value || _isScanning.value) return
        viewModelScope.launch {
            _isEnrolling.value = true
            _enrollStatus.value = "Place finger for slot $slot…"
            when (val result = debugControls.enrollSlot(slot)) {
                EnrollResult.Success -> {
                    _enrollStatus.value = "Stored in slot $slot"
                }
                is EnrollResult.Failed -> {
                    _enrollStatus.value = "Failed: ${result.message}"
                }
            }
            _isEnrolling.value = false
        }
    }

    private suspend fun publishClockIn(employeeId: Int) {
        _publishStatus.value = "Publishing…"
        when (val result = handleClockIn(employeeId, MqttConfig.DEVICE_ID)) {
            is ClockInResult.Published -> {
                _publishStatus.value = "Published to ${MqttConfig.TOPIC}"
            }
            is ClockInResult.Queued -> {
                _publishStatus.value = "Offline — queued for sync"
            }
        }
    }
}
