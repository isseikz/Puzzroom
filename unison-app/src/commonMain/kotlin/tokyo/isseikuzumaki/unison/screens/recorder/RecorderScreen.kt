package tokyo.isseikuzumaki.unison.screens.recorder

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import tokyo.isseikuzumaki.unison.screens.session.SessionUiState
import tokyo.isseikuzumaki.unison.screens.session.SessionViewModel

/**
 * Platform-specific recorder screen wrapper
 */
@Composable
expect fun RecorderScreenPlatform(
    uri: String,
    onNavigateBack: () -> Unit,
    onNavigateToEditor: () -> Unit
)

/**
 * Recorder Screen - Records user's voice while playing original audio
 */
@Composable
internal fun RecorderScreen(
    uri: String,
    onNavigateBack: () -> Unit,
    onNavigateToEditor: () -> Unit
) {
    val viewModel: SessionViewModel = koinViewModel { parametersOf(uri) }
    val uiState by viewModel.uiState.collectAsState()

    // Auto-navigate to editor when recording is complete
    LaunchedEffect(uiState) {
        if (uiState is SessionUiState.Editing) {
            onNavigateToEditor()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    when (val state = uiState) {
                        is SessionUiState.Ready -> Text(state.fileName)
                        is SessionUiState.Recording -> Text(state.fileName)
                        else -> Text("Recording")
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            when (val state = uiState) {
                is SessionUiState.Loading -> {
                    CircularProgressIndicator()
                }

                is SessionUiState.Ready -> {
                    RecorderContent(
                        isRecording = false,
                        durationMs = state.durationMs,
                        currentPositionMs = 0,
                        onRecordClick = { viewModel.startRecording() }
                    )
                }

                is SessionUiState.Recording -> {
                    RecorderContent(
                        isRecording = true,
                        durationMs = state.durationMs,
                        currentPositionMs = viewModel.getCurrentPositionMs(),
                        onRecordClick = { viewModel.stopRecording() }
                    )
                }

                is SessionUiState.Error -> {
                    ErrorContent(
                        message = state.message,
                        onRetry = onNavigateBack
                    )
                }

                else -> {}
            }
        }
    }
}

@Composable
private fun RecorderContent(
    isRecording: Boolean,
    durationMs: Long,
    currentPositionMs: Long,
    onRecordClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(32.dp),
        modifier = Modifier.padding(32.dp)
    ) {
        // Recording status indicator
        if (isRecording) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.FiberManualRecord,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Recording...",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
        } else {
            Text(
                text = "Ready to record",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Progress bar
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LinearProgressIndicator(
                progress = { if (durationMs > 0) currentPositionMs.toFloat() / durationMs else 0f },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${formatTime(currentPositionMs)} / ${formatTime(durationMs)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Record/Stop button
        FloatingActionButton(
            onClick = onRecordClick,
            modifier = Modifier.size(80.dp),
            containerColor = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
        ) {
            Icon(
                imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.FiberManualRecord,
                contentDescription = if (isRecording) "Stop" else "Record",
                modifier = Modifier.size(40.dp)
            )
        }

        Text(
            text = if (isRecording) "Tap to stop recording" else "Tap to start recording",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.padding(32.dp)
    ) {
        Text(
            text = "Error",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.error
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Button(onClick = onRetry) {
            Text("Go Back")
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}
