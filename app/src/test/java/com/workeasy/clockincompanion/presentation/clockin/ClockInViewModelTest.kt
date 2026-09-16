package com.workeasy.clockincompanion.presentation.clockin

import app.cash.turbine.test
import com.workeasy.clockincompanion.data.mqtt.MqttConfig
import com.workeasy.clockincompanion.domain.model.ClockEvent
import com.workeasy.clockincompanion.domain.model.ConnectionState
import com.workeasy.clockincompanion.domain.model.ScanEvent
import com.workeasy.clockincompanion.domain.reader.DebugFingerprintControls
import com.workeasy.clockincompanion.domain.reader.EnrollResult
import com.workeasy.clockincompanion.domain.reader.FingerprintReader
import com.workeasy.clockincompanion.domain.store.ClockEventStore
import com.workeasy.clockincompanion.domain.usecase.ClockInResult
import com.workeasy.clockincompanion.domain.usecase.HandleClockInUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ClockInViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    private val events = MutableSharedFlow<ScanEvent>(extraBufferCapacity = 8)

    private val fingerprintReader: FingerprintReader = mockk(relaxed = true)
    private val debugControls: DebugFingerprintControls = mockk(relaxed = true)
    private val handleClockIn: HandleClockInUseCase = mockk()
    private val clockEventStore: ClockEventStore = mockk(relaxed = true)
    private val mqttConfig = MqttConfig()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { fingerprintReader.connectionState } returns connectionState
        every { fingerprintReader.events() } returns events
        every { debugControls.supportsSimulation } returns true
        every { debugControls.supportsEnroll } returns true
        every { clockEventStore.observePendingCount() } returns flowOf(0)
        coEvery { fingerprintReader.connect() } coAnswers {
            connectionState.value = ConnectionState.CONNECTED
        }
        coEvery { handleClockIn(any(), any()) } answers {
            ClockInResult.Published(
                ClockEvent(employeeId = firstArg(), deviceId = secondArg()),
            )
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = ClockInViewModel(
        fingerprintReader,
        debugControls,
        handleClockIn,
        mqttConfig,
        clockEventStore,
    )

    @Test
    fun `connects on init and exposes connected state`() = runTest(dispatcher) {
        val vm = viewModel()

        vm.connectionState.test {
            assertEquals(ConnectionState.DISCONNECTED, awaitItem())
            advanceUntilIdle()
            assertEquals(ConnectionState.CONNECTED, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        coVerify(exactly = 1) { fingerprintReader.connect() }
    }

    @Test
    fun `simulate match updates lastEvent when reader emits Matched`() = runTest(dispatcher) {
        coEvery { debugControls.simulateMatch(any()) } coAnswers {
            events.emit(ScanEvent.Matched(1))
        }

        val vm = viewModel()
        advanceUntilIdle()

        vm.lastEvent.test {
            assertEquals(null, awaitItem())

            vm.onSimulateMatch()
            advanceUntilIdle()

            assertEquals(ScanEvent.Matched(1), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        coVerify(exactly = 1) { debugControls.simulateMatch(any()) }
        assertFalse(vm.isScanning.value)
    }

    @Test
    fun `matched scan triggers mqtt publish use case`() = runTest(dispatcher) {
        coEvery { debugControls.simulateMatch(any()) } coAnswers {
            events.emit(ScanEvent.Matched(7))
        }

        val vm = viewModel()
        advanceUntilIdle()

        vm.onSimulateMatch()
        advanceUntilIdle()

        coVerify(exactly = 1) { handleClockIn(7, MqttConfig.DEVICE_ID) }
        assertTrue(vm.publishStatus.value?.contains("Published") == true)
    }

    @Test
    fun `queued clock-in shows offline status`() = runTest(dispatcher) {
        coEvery { debugControls.simulateMatch(any()) } coAnswers {
            events.emit(ScanEvent.Matched(2))
        }
        coEvery { handleClockIn(any(), any()) } answers {
            ClockInResult.Queued(ClockEvent(employeeId = firstArg(), deviceId = secondArg()))
        }

        val vm = viewModel()
        advanceUntilIdle()
        vm.onSimulateMatch()
        advanceUntilIdle()

        assertTrue(vm.publishStatus.value?.contains("queued", ignoreCase = true) == true)
    }

    @Test
    fun `simulate no match updates lastEvent when reader emits NoMatch`() = runTest(dispatcher) {
        coEvery { debugControls.simulateNoMatch() } coAnswers {
            events.emit(ScanEvent.NoMatch)
        }

        val vm = viewModel()
        advanceUntilIdle()

        vm.onSimulateNoMatch()
        advanceUntilIdle()

        assertEquals(ScanEvent.NoMatch, vm.lastEvent.value)
        coVerify(exactly = 1) { debugControls.simulateNoMatch() }
        coVerify(exactly = 0) { handleClockIn(any(), any()) }
    }

    @Test
    fun `enroll slot success updates enroll status`() = runTest(dispatcher) {
        coEvery { debugControls.enrollSlot(1) } returns EnrollResult.Success

        val vm = viewModel()
        advanceUntilIdle()

        vm.onEnrollSlot(1)
        advanceUntilIdle()

        assertEquals("Stored in slot 1", vm.enrollStatus.value)
        assertFalse(vm.isEnrolling.value)
        coVerify(exactly = 1) { debugControls.enrollSlot(1) }
    }
}
