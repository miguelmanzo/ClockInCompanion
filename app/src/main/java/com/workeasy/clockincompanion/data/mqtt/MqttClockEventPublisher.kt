package com.workeasy.clockincompanion.data.mqtt

import com.hivemq.client.mqtt.MqttClient
import com.hivemq.client.mqtt.datatypes.MqttQos
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient
import com.workeasy.clockincompanion.domain.model.ClockEvent
import com.workeasy.clockincompanion.domain.publisher.ClockEventPublisher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class MqttClockEventPublisher @Inject constructor(
    private val mqttConfig: MqttConfig,
) : ClockEventPublisher {

    private val mutex = Mutex()
    private var client: Mqtt5AsyncClient? = null
    private var connectedHost: String? = null

    override suspend fun publish(event: ClockEvent): Boolean = withContext(Dispatchers.IO) {
        val snapshot = mqttConfig.snapshot()
        if (snapshot.host.isBlank()) return@withContext false

        mutex.withLock {
            try {
                ensureConnected(snapshot)
                val activeClient = client ?: return@withLock false
                val payload = Json.encodeToString(
                    ClockEventPayload(
                        eventId = event.id,
                        employeeId = event.employeeId,
                        eventType = event.eventType,
                        deviceId = event.deviceId,
                        timestamp = event.timestampEpochMs,
                    ),
                )
                suspendCancellableCoroutine { cont ->
                    activeClient.publishWith()
                        .topic(snapshot.topic)
                        .qos(MqttQos.AT_LEAST_ONCE)
                        .payload(payload.toByteArray(Charsets.UTF_8))
                        .send()
                        .whenComplete { _, error ->
                            cont.resume(error == null)
                        }
                }
            } catch (_: Exception) {
                runCatching { client?.disconnect()?.get(1, TimeUnit.SECONDS) }
                client = null
                connectedHost = null
                false
            }
        }
    }

    private fun ensureConnected(snapshot: MqttConfig.Snapshot) {
        val existing = client
        if (existing != null &&
            existing.state.isConnected &&
            connectedHost == snapshot.host
        ) {
            return
        }

        runCatching { existing?.disconnect()?.get(1, TimeUnit.SECONDS) }

        val newClient = MqttClient.builder()
            .useMqttVersion5()
            .identifier("${snapshot.clientId}-${System.currentTimeMillis() % 10_000}")
            .serverHost(snapshot.host)
            .serverPort(snapshot.port)
            .buildAsync()

        newClient.connectWith()
            .cleanStart(true)
            .send()
            .get(5, TimeUnit.SECONDS)

        client = newClient
        connectedHost = snapshot.host
    }

    @Serializable
    private data class ClockEventPayload(
        val eventId: String,
        val employeeId: Int,
        val eventType: String,
        val deviceId: String,
        val timestamp: Long,
    )
}
