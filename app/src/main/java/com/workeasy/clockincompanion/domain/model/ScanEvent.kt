package com.workeasy.clockincompanion.domain.model

sealed interface ScanEvent {
    data class Matched(val employeeId: Int) : ScanEvent
    data object NoMatch : ScanEvent
    data class Error(val message: String) : ScanEvent
}
