package com.workeasy.clockincompanion.data.reader

import com.workeasy.clockincompanion.data.usb.As608Protocol
import com.workeasy.clockincompanion.data.usb.UsbSerialManager
import com.workeasy.clockincompanion.domain.model.ConnectionState
import com.workeasy.clockincompanion.domain.model.ScanEvent
import com.workeasy.clockincompanion.domain.reader.DebugFingerprintControls
import com.workeasy.clockincompanion.domain.reader.EnrollResult
import com.workeasy.clockincompanion.domain.reader.FingerprintReader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SerialFingerprintReader @Inject constructor(
    private val usbSerialManager: UsbSerialManager,
) : FingerprintReader, DebugFingerprintControls {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val commandMutex = Mutex()

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _events = MutableSharedFlow<ScanEvent>(extraBufferCapacity = 8)
    override fun events(): Flow<ScanEvent> = _events.asSharedFlow()

    override val supportsSimulation: Boolean = false
    override val supportsEnroll: Boolean = true

    @Volatile
    private var enrollInProgress = false
    private var scanJob: Job? = null

    override suspend fun connect() {
        _connectionState.value = ConnectionState.CONNECTING
        try {
            usbSerialManager.open()
            _connectionState.value = ConnectionState.CONNECTED
            startScanLoop()
        } catch (e: Exception) {
            _connectionState.value = ConnectionState.ERROR
            _events.tryEmit(ScanEvent.Error(e.message ?: "USB connect failed"))
        }
    }

    override suspend fun disconnect() {
        scanJob?.cancel()
        scanJob = null
        usbSerialManager.close()
        _connectionState.value = ConnectionState.DISCONNECTED
    }

    override suspend fun simulateMatch(employeeId: Int) {
        error("Simulation not available on serial reader")
    }

    override suspend fun simulateNoMatch() {
        error("Simulation not available on serial reader")
    }

    override suspend fun enrollSlot(slot: Int): EnrollResult {
        if (slot !in 1..2) return EnrollResult.Failed("Slot must be 1 or 2")
        if (_connectionState.value != ConnectionState.CONNECTED) {
            return EnrollResult.Failed("Sensor not connected")
        }

        enrollInProgress = true
        return try {
            commandMutex.withLock {
                waitForFinger()
                image2Tz(1)
                waitForFingerRemoved()
                waitForFinger()
                image2Tz(2)
                regModel()
                store(slot)
            }
            EnrollResult.Success
        } catch (e: Exception) {
            EnrollResult.Failed(e.message ?: "Enroll failed")
        } finally {
            enrollInProgress = false
        }
    }

    private fun startScanLoop() {
        scanJob?.cancel()
        scanJob = scope.launch {
            while (isActive) {
                delay(SCAN_INTERVAL_MS)
                if (enrollInProgress) continue
                if (_connectionState.value != ConnectionState.CONNECTED) continue
                runCatching { triggerIdentify() }
            }
        }
    }

    private suspend fun triggerIdentify() {
        commandMutex.withLock {
            val response = usbSerialManager.transact(
                As608Protocol.buildAutoIdentifyCommand(),
                timeoutMs = 3_000L,
            )
            val event = As608Protocol.parseAutoIdentifyResponse(response)
            // Ignore "no finger" noise during polling.
            if (event is ScanEvent.Error && event.message.contains("No finger", ignoreCase = true)) {
                return
            }
            val confirm = As608Protocol.confirmationCode(response)
            if (confirm == As608Protocol.CONFIRM_NO_FINGER) return
            _events.emit(event)
        }
    }

    private suspend fun waitForFinger(timeoutMs: Long = 15_000L) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            val response = usbSerialManager.transact(As608Protocol.buildGetImageCommand())
            when (As608Protocol.confirmationCode(response)) {
                As608Protocol.CONFIRM_OK -> return
                As608Protocol.CONFIRM_NO_FINGER -> delay(200)
                else -> delay(200)
            }
        }
        error("Timed out waiting for finger")
    }

    private suspend fun waitForFingerRemoved(timeoutMs: Long = 15_000L) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            val response = usbSerialManager.transact(As608Protocol.buildGetImageCommand())
            when (As608Protocol.confirmationCode(response)) {
                As608Protocol.CONFIRM_NO_FINGER -> return
                else -> delay(200)
            }
        }
        error("Timed out waiting for finger to be removed")
    }

    private suspend fun image2Tz(bufferId: Int) {
        val response = usbSerialManager.transact(As608Protocol.buildImage2TzCommand(bufferId))
        val code = As608Protocol.confirmationCode(response)
        if (code != As608Protocol.CONFIRM_OK) {
            error("Image2Tz failed: 0x${code?.toString(16)}")
        }
    }

    private suspend fun regModel() {
        val response = usbSerialManager.transact(As608Protocol.buildRegModelCommand())
        val code = As608Protocol.confirmationCode(response)
        if (code != As608Protocol.CONFIRM_OK) {
            error("RegModel failed: 0x${code?.toString(16)}")
        }
    }

    private suspend fun store(pageId: Int) {
        val response = usbSerialManager.transact(As608Protocol.buildStoreCommand(pageId))
        val code = As608Protocol.confirmationCode(response)
        if (code != As608Protocol.CONFIRM_OK) {
            error("Store failed: 0x${code?.toString(16)}")
        }
    }

    companion object {
        private const val SCAN_INTERVAL_MS = 2_000L
    }
}
