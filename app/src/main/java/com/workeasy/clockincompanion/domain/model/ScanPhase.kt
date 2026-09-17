package com.workeasy.clockincompanion.domain.model

/** Live identify progress for the scan card (hardware or simulate). */
enum class ScanPhase {
    /** No finger / not identifying. */
    Idle,

    /** GetImage succeeded — finger is on the sensor. */
    FingerDetected,

    /** Building template / searching library. */
    Matching,
}
