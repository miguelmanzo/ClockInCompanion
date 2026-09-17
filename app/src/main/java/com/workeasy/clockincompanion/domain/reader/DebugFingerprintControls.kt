package com.workeasy.clockincompanion.domain.reader

sealed interface EnrollResult {
    data object Success : EnrollResult
    data class Failed(val message: String) : EnrollResult
}

/**
 * Debug-only controls: simulation (no hardware) and/or enroll into sensor slots 1–2.
 */
interface DebugFingerprintControls {
    val supportsSimulation: Boolean
    val supportsEnroll: Boolean

    suspend fun simulateMatch(employeeId: Int = 1)
    suspend fun simulateNoMatch()
    suspend fun enrollSlot(slot: Int): EnrollResult
    suspend fun clearLibrary(): EnrollResult
}
