package com.workeasy.clockincompanion.domain.reader

/**
 * Debug-only controls for demos without hardware (and later for enroll UI).
 * Bound alongside [FingerprintReader]; serial impl can provide enroll-only methods later.
 */
interface DebugFingerprintControls {
    suspend fun simulateMatch(employeeId: Int = 1)
    suspend fun simulateNoMatch()
}
