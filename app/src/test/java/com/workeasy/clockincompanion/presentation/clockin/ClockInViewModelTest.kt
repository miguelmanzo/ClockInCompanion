package com.workeasy.clockincompanion.presentation.clockin

import app.cash.turbine.test
import com.workeasy.clockincompanion.domain.model.ConnectionState
import com.workeasy.clockincompanion.domain.model.ScanEvent
import com.workeasy.clockincompanion.domain.reader.DebugFingerprintControls
import com.workeasy.clockincompanion.domain.reader.FingerprintReader
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ClockInViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    private val events = MutableSharedFlow<ScanEvent>(extraBufferCapacity = 8)

    private val fingerprintReader: FingerprintReader = mockk(relaxed = true)
    private val debugControls: DebugFingerprintControls = mockk(relaxed = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { fingerprintReader.connectionState } returns connectionState
        every { fingerprintReader.events() } returns events
        coEvery { fingerprintReader.connect() } coAnswers {
            connectionState.value = ConnectionState.CONNECTED
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = ClockInViewModel(fingerprintReader, debugControls)

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
    }
}
