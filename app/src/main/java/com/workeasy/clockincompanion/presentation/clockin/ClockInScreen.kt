package com.workeasy.clockincompanion.presentation.clockin

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.workeasy.clockincompanion.BuildConfig
import com.workeasy.clockincompanion.domain.model.ConnectionState
import com.workeasy.clockincompanion.domain.model.ScanEvent
import com.workeasy.clockincompanion.presentation.theme.ErrorRed
import com.workeasy.clockincompanion.presentation.theme.MatchGreen
import com.workeasy.clockincompanion.presentation.theme.NoMatchAmber

@Composable
fun ClockInScreen(
    viewModel: ClockInViewModel = hiltViewModel(),
) {
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val lastEvent by viewModel.lastEvent.collectAsStateWithLifecycle()
    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()
    val isEnrolling by viewModel.isEnrolling.collectAsStateWithLifecycle()
    val enrollStatus by viewModel.enrollStatus.collectAsStateWithLifecycle()
    val publishStatus by viewModel.publishStatus.collectAsStateWithLifecycle()
    val brokerHost by viewModel.brokerHost.collectAsStateWithLifecycle()
    val pendingCount by viewModel.pendingCount.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Clock-In Companion",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.semantics {
                contentDescription = "Clock-In Companion"
            },
        )
        Text(
            text = "Fingerprint clock-in over USB serial, MQTT, and offline queue",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
        )

        ConnectionStatusBar(state = connectionState)

        ScanResultCard(
            event = lastEvent,
            isBusy = isScanning || isEnrolling,
            enrollStatus = enrollStatus,
            publishStatus = publishStatus,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 220.dp),
        )

        if (pendingCount > 0) {
            PendingSyncBanner(count = pendingCount)
        }

        if (BuildConfig.DEBUG) {
            DebugControls(
                enabled = !isScanning && !isEnrolling &&
                    connectionState == ConnectionState.CONNECTED,
                supportsSimulation = viewModel.supportsSimulation,
                supportsEnroll = viewModel.supportsEnroll,
                brokerHost = brokerHost,
                onBrokerHostChanged = viewModel::onBrokerHostChanged,
                onSimulateMatch = viewModel::onSimulateMatch,
                onSimulateNoMatch = viewModel::onSimulateNoMatch,
                onEnrollSlot1 = { viewModel.onEnrollSlot(1) },
                onEnrollSlot2 = { viewModel.onEnrollSlot(2) },
            )
        }
    }
}

@Composable
private fun PendingSyncBanner(count: Int) {
    val label = if (count == 1) {
        "1 event pending sync"
    } else {
        "$count events pending sync"
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = label },
        shape = RoundedCornerShape(12.dp),
        color = NoMatchAmber.copy(alpha = 0.28f),
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            style = MaterialTheme.typography.titleLarge,
        )
    }
}

@Composable
private fun ConnectionStatusBar(state: ConnectionState) {
    val (color, label) = when (state) {
        ConnectionState.DISCONNECTED -> Color(0xFF757575) to "Disconnected"
        ConnectionState.CONNECTING -> NoMatchAmber to "Connecting"
        ConnectionState.CONNECTED -> MatchGreen to "Connected"
        ConnectionState.ERROR -> ErrorRed to "Error"
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Sensor status: $label" },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(color),
            )
            Text(
                text = "Sensor: $label",
                style = MaterialTheme.typography.titleLarge,
            )
        }
    }
}

@Composable
private fun ScanResultCard(
    event: ScanEvent?,
    isBusy: Boolean,
    enrollStatus: String?,
    publishStatus: String?,
    modifier: Modifier = Modifier,
) {
    val (bg, primary, secondary) = when {
        isBusy && enrollStatus != null -> Triple(
            MaterialTheme.colorScheme.surfaceVariant,
            "Enrolling…",
            enrollStatus,
        )
        isBusy -> Triple(
            MaterialTheme.colorScheme.surfaceVariant,
            "Scanning…",
            "Please wait",
        )
        event is ScanEvent.Matched -> Triple(
            MatchGreen.copy(alpha = 0.18f),
            "Clocked In",
            buildString {
                append("Employee #${event.employeeId}")
                if (!publishStatus.isNullOrBlank()) {
                    append("\n")
                    append(publishStatus)
                }
            },
        )
        event is ScanEvent.NoMatch -> Triple(
            NoMatchAmber.copy(alpha = 0.22f),
            "No Match",
            "Try again",
        )
        event is ScanEvent.Error -> Triple(
            ErrorRed.copy(alpha = 0.16f),
            "Sensor Error",
            event.message,
        )
        !enrollStatus.isNullOrBlank() -> Triple(
            MaterialTheme.colorScheme.surfaceVariant,
            "Enrollment",
            enrollStatus,
        )
        else -> Triple(
            MaterialTheme.colorScheme.surfaceVariant,
            "Place finger on sensor",
            "Waiting for scan",
        )
    }

    Surface(
        modifier = modifier.semantics {
            contentDescription = "$primary. $secondary"
        },
        shape = RoundedCornerShape(20.dp),
        color = bg,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AnimatedContent(
                targetState = primary to secondary,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "scanResult",
            ) { (title, subtitle) ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun DebugControls(
    enabled: Boolean,
    supportsSimulation: Boolean,
    supportsEnroll: Boolean,
    brokerHost: String,
    onBrokerHostChanged: (String) -> Unit,
    onSimulateMatch: () -> Unit,
    onSimulateNoMatch: () -> Unit,
    onEnrollSlot1: () -> Unit,
    onEnrollSlot2: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Demo controls",
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = if (brokerHost.isBlank()) {
                    "Set the MQTT broker host to your laptop IP before publishing."
                } else {
                    "Broker: $brokerHost:1883"
                },
                style = MaterialTheme.typography.bodyLarge,
                color = if (brokerHost.isBlank()) {
                    ErrorRed
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                },
            )

            OutlinedTextField(
                value = brokerHost,
                onValueChange = onBrokerHostChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "MQTT broker host" },
                singleLine = true,
                label = { Text("MQTT broker host") },
                placeholder = { Text("192.168.x.x") },
                supportingText = {
                    Text("Topic workeasy/demo/clockevents · port 1883")
                },
            )

            HorizontalDivider()

            if (supportsSimulation) {
                Button(
                    onClick = onSimulateMatch,
                    enabled = enabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp),
                ) {
                    Text("Simulate Scan (Match)")
                }
                OutlinedButton(
                    onClick = onSimulateNoMatch,
                    enabled = enabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp),
                ) {
                    Text("Simulate No Match")
                }
            }

            if (supportsEnroll) {
                OutlinedButton(
                    onClick = onEnrollSlot1,
                    enabled = enabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp),
                ) {
                    Text("Enroll slot 1")
                }
                OutlinedButton(
                    onClick = onEnrollSlot2,
                    enabled = enabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp),
                ) {
                    Text("Enroll slot 2")
                }
            }

            Text(
                text = if (BuildConfig.USE_SIMULATED_READER) {
                    "Reader: simulated — set USE_SIMULATED_READER=false for USB hardware"
                } else {
                    "Reader: USB serial (CP2102 @ 57600)"
                },
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            )
        }
    }
}
