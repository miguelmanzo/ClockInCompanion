package com.workeasy.clockincompanion.data.mqtt

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MqttConfig @Inject constructor() {
    private val _brokerHost = MutableStateFlow(DEFAULT_HOST)
    val brokerHost: StateFlow<String> = _brokerHost.asStateFlow()

    fun updateBrokerHost(host: String) {
        _brokerHost.value = host.trim()
    }

    fun snapshot(): Snapshot = Snapshot(
        host = _brokerHost.value,
        port = BROKER_PORT,
        topic = TOPIC,
        qos = QOS,
        deviceId = DEVICE_ID,
        clientId = CLIENT_ID,
    )

    data class Snapshot(
        val host: String,
        val port: Int,
        val topic: String,
        val qos: Int,
        val deviceId: String,
        val clientId: String,
    )

    companion object {
        /** Empty until set in debug UI to the laptop hotspot IP (e.g. 192.168.43.10). */
        const val DEFAULT_HOST = ""
        const val BROKER_PORT = 1883
        const val TOPIC = "workeasy/demo/clockevents"
        const val QOS = 1
        const val DEVICE_ID = "demo-device-001"
        const val CLIENT_ID = "clock-in-companion-android"
    }
}
