package com.workeasy.clockincompanion.domain.usecase

import com.workeasy.clockincompanion.domain.model.ClockEvent
import com.workeasy.clockincompanion.domain.publisher.ClockEventPublisher
import com.workeasy.clockincompanion.domain.store.ClockEventStore
import com.workeasy.clockincompanion.domain.store.OfflineFlushScheduler
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class HandleClockInUseCaseTest {

    private val store: ClockEventStore = mockk(relaxed = true)
    private val publisher: ClockEventPublisher = mockk()
    private val flushScheduler: OfflineFlushScheduler = mockk(relaxed = true)

    private val useCase = HandleClockInUseCase(store, publisher, flushScheduler)

    @Test
    fun `persists then publishes and marks synced on success`() = runTest {
        coEvery { publisher.publish(any()) } returns true

        val result = useCase(employeeId = 1, deviceId = "demo")

        assertTrue(result is ClockInResult.Published)
        coVerifyOrder {
            store.save(any())
            publisher.publish(any())
            store.markSynced(any())
        }
        verify(exactly = 0) { flushScheduler.schedule() }
    }

    @Test
    fun `persists and schedules flush when publish fails`() = runTest {
        coEvery { publisher.publish(any()) } returns false

        val result = useCase(employeeId = 3, deviceId = "demo")

        assertTrue(result is ClockInResult.Queued)
        coVerify { store.save(match { it.employeeId == 3 }) }
        coVerify(exactly = 0) { store.markSynced(any()) }
        verify(exactly = 1) { flushScheduler.schedule() }
    }
}
