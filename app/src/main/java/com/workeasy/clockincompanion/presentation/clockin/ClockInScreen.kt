package com.workeasy.clockincompanion.presentation.clockin

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        ConnectionStatusBar(state = connectionState)

        ScanResultCard(
            event = lastEvent,
            isScanning = isScanning,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(vertical = 24.dp),
        )

        if (BuildConfig.DEBUG) {
            DebugControls(
                enabled = !isScanning && connectionState == ConnectionState.CONNECTED,
                onSimulateMatch = viewModel::onSimulateMatch,
                onSimulateNoMatch = viewModel::onSimulateNoMatch,
            )
        }
    }
}

@Composable
private fun ConnectionStatusBar(state: ConnectionState) {
    val (color, label) = when (state) {
        ConnectionState.DISCONNECTED -> Color.Gray to "Disconnected"
        ConnectionState.CONNECTING -> NoMatchAmber to "Connecting"
        ConnectionState.CONNECTED -> MatchGreen to "Connected"
        ConnectionState.ERROR -> ErrorRed to "Error"
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.semantics {
            contentDescription = "Sensor status: $label"
        },
    ) {
        Box(
            modifier = Modifier
                .size(14.dp)
                .clip(CircleShape)
                .background(color),
        )
        Text(
            text = "Sensor: $label",
            style = MaterialTheme.typography.titleLarge,
        )
    }
}

@Composable
private fun ScanResultCard(
    event: ScanEvent?,
    isScanning: Boolean,
    modifier: Modifier = Modifier,
) {
    val (bg, primary, secondary) = when {
        isScanning -> Triple(
            MaterialTheme.colorScheme.surfaceVariant,
            "Scanning…",
            "Please wait",
        )
        event == null -> Triple(
            MaterialTheme.colorScheme.surfaceVariant,
            "Place finger on sensor",
            "Waiting for scan",
        )
        event is ScanEvent.Matched -> Triple(
            MatchGreen.copy(alpha = 0.15f),
            "Clocked In",
            "Employee #${event.employeeId}",
        )
        event is ScanEvent.NoMatch -> Triple(
            NoMatchAmber.copy(alpha = 0.2f),
            "No Match",
            "Try again",
        )
        event is ScanEvent.Error -> Triple(
            ErrorRed.copy(alpha = 0.15f),
            "Sensor Error",
            event.message,
        )
        else -> Triple(
            MaterialTheme.colorScheme.surfaceVariant,
            "Place finger on sensor",
            "",
        )
    }

    Surface(
        modifier = modifier.semantics {
            contentDescription = "$primary. $secondary"
        },
        shape = RoundedCornerShape(16.dp),
        color = bg,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AnimatedContent(targetState = primary, label = "scanPrimary") { text ->
                Text(
                    text = text,
                    style = MaterialTheme.typography.headlineMedium,
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = secondary,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )
        }
    }
}

@Composable
private fun DebugControls(
    enabled: Boolean,
    onSimulateMatch: () -> Unit,
    onSimulateNoMatch: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Debug",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        )
        Button(
            onClick = onSimulateMatch,
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
        ) {
            Text("Simulate Scan (Match)")
        }
        OutlinedButton(
            onClick = onSimulateNoMatch,
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
        ) {
            Text("Simulate No Match")
        }
    }
}
