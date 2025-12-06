package tokyo.isseikuzumaki.unison.screens.recorder

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.puzzroom.whisper.TranscriptionSegment
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import tokyo.isseikuzumaki.unison.screens.session.SessionUiState
import tokyo.isseikuzumaki.unison.screens.session.SessionViewModel

/**
 * Platform-specific permission handler
 * Returns a PermissionState that can be observed
 */
@Composable
expect fun rememberRecordAudioPermissionState(): RecordAudioPermissionState

/**
 * Permission state for record audio
 */
interface RecordAudioPermissionState {
    val isGranted: Boolean
    fun launchPermissionRequest()
}

/**
 * Platform-specific recorder screen wrapper with permission handling
 */
@Composable
fun RecorderScreenPlatform(
    uri: String,
    onNavigateBack: () -> Unit,
    onNavigateToEditor: () -> Unit
) {
    val permissionState = rememberRecordAudioPermissionState()

    LaunchedEffect(Unit) {
        if (!permissionState.isGranted) {
            permissionState.launchPermissionRequest()
        }
    }

    if (permissionState.isGranted) {
        RecorderScreen(
            uri = uri,
            onNavigateBack = onNavigateBack,
            onNavigateToEditor = onNavigateToEditor
        )
    } else {
        PermissionRequiredContent(
            onRequestPermission = { permissionState.launchPermissionRequest() }
        )
    }
}

/**
 * Permission required content - shown when microphone permission is not granted
 */
@Composable
private fun PermissionRequiredContent(
    onRequestPermission: () -> Unit
) {
    Surface {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Microphone Permission Required",
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = "This app needs microphone access to record your voice",
                    style = MaterialTheme.typography.bodyMedium
                )
                Button(onClick = onRequestPermission) {
                    Text("Grant Permission")
                }
            }
        }
    }
}

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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                    var currentPositionMs by remember { mutableStateOf(0L) }

                    RecorderContentWithTranscription(
                        isRecording = false,
                        durationMs = state.durationMs,
                        currentPositionMs = currentPositionMs,
                        transcription = state.transcription,
                        onRecordClick = { viewModel.startRecording() }
                    )
                }

                is SessionUiState.Recording -> {
                    var currentPositionMs by remember { mutableStateOf(0L) }

                    // Update current position in real-time
                    LaunchedEffect(Unit) {
                        while (isActive) {
                            currentPositionMs = viewModel.getCurrentPositionMs()
                            delay(100) // Update every 100ms
                        }
                    }

                    RecorderContentWithTranscription(
                        isRecording = true,
                        durationMs = state.durationMs,
                        currentPositionMs = currentPositionMs,
                        transcription = state.transcription,
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
    RecorderContentWithTranscription(
        isRecording = isRecording,
        durationMs = durationMs,
        currentPositionMs = currentPositionMs,
        transcription = emptyList(),
        onRecordClick = onRecordClick
    )
}

@Composable
private fun RecorderContentWithTranscription(
    isRecording: Boolean,
    durationMs: Long,
    currentPositionMs: Long,
    transcription: List<TranscriptionSegment>,
    onRecordClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
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

        // Transcription display
        if (transcription.isNotEmpty()) {
            TranscriptionList(
                transcription = transcription,
                currentPositionMs = currentPositionMs,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }

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
private fun TranscriptionList(
    transcription: List<TranscriptionSegment>,
    currentPositionMs: Long,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    // Auto-scroll to active segment
    LaunchedEffect(currentPositionMs) {
        val activeIndex = transcription.indexOfFirst { segment ->
            currentPositionMs in segment.startTimeMs..segment.endTimeMs
        }
        if (activeIndex >= 0) {
            listState.animateScrollToItem(activeIndex)
        }
    }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(transcription) { segment ->
                TranscriptionSegmentItem(
                    segment = segment,
                    isActive = currentPositionMs in segment.startTimeMs..segment.endTimeMs
                )
            }
        }
    }
}

@Composable
private fun TranscriptionSegmentItem(
    segment: TranscriptionSegment,
    isActive: Boolean
) {
    val backgroundColor = if (isActive) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }

    val textColor = if (isActive) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(
                text = "${formatTime(segment.startTimeMs)} - ${formatTime(segment.endTimeMs)}",
                style = MaterialTheme.typography.labelSmall,
                color = textColor.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = segment.text.trim(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                color = textColor
            )
        }
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

@Preview
@Composable
fun RecorderScreenPreview() {
    RecorderContent(
        isRecording = true,
        durationMs = 180000,
        currentPositionMs = 45000,
        onRecordClick = {}
    )
}