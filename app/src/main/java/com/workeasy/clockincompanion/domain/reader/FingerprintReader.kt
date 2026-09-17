package com.workeasy.clockincompanion.domain.reader

import com.workeasy.clockincompanion.domain.model.ConnectionState
import com.workeasy.clockincompanion.domain.model.ScanEvent
import com.workeasy.clockincompanion.domain.model.ScanPhase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface FingerprintReader {
    val connectionState: StateFlow<ConnectionState>
    val scanPhase: StateFlow<ScanPhase>
    fun events(): Flow<ScanEvent>
    suspend fun connect()
    suspend fun disconnect()
}
