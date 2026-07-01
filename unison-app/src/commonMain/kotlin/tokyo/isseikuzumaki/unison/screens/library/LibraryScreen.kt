package tokyo.isseikuzumaki.unison.screens.library

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel

/**
 * Platform-specific library screen wrapper
 */
@Composable
expect fun LibraryScreenPlatform(
    onNavigateToSession: (String, String) -> Unit,
    viewModel: LibraryViewModel = koinViewModel()
)

/**
 * Library Screen - Entry point of the app
 * Allows users to select an audio file and its transcription to start a shadowing session
 */
@Composable
internal fun LibraryScreen(
    onAudioFileSelected: () -> Unit,
    onTranscriptionFileSelected: () -> Unit,
    onStartSession: () -> Unit,
    onNavigateToSession: (String, String) -> Unit,
    viewModel: LibraryViewModel
) {
    val uiState by viewModel.uiState.collectAsState()

    // Handle navigation when files are validated
    LaunchedEffect(uiState) {
        if (uiState is LibraryUiState.Valid) {
            val state = uiState as LibraryUiState.Valid
            onNavigateToSession(state.audioUri, state.transcriptionUri)
            viewModel.clearError()
        }
    }

    val hasAudio = uiState is LibraryUiState.AudioSelected ||
                   uiState is LibraryUiState.BothSelected ||
                   uiState is LibraryUiState.Valid
    val hasTranscription = uiState is LibraryUiState.TranscriptionSelected ||
                           uiState is LibraryUiState.BothSelected ||
                           uiState is LibraryUiState.Valid
    val canStart = hasAudio && hasTranscription

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Unison - Shadowing Sync Player") }
            )
        },
        snackbarHost = {
            if (uiState is LibraryUiState.Error) {
                Snackbar(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text((uiState as LibraryUiState.Error).message)
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp),
                modifier = Modifier.padding(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    modifier = Modifier.size(120.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "Unison",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "Eliminate lag in shadowing practice by syncing your voice with the original audio",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Audio File Selection Button
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onAudioFileSelected
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AudioFile,
                                contentDescription = null,
                                tint = if (hasAudio) MaterialTheme.colorScheme.primary
                                      else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Column {
                                Text(
                                    text = "Audio File",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = if (hasAudio) "Selected" else "Tap to select",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (hasAudio) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Selected",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Transcription File Selection Button
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onTranscriptionFileSelected
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = null,
                                tint = if (hasTranscription) MaterialTheme.colorScheme.primary
                                      else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Column {
                                Text(
                                    text = "Transcription File",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = if (hasTranscription) "Selected" else "Tap to select",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (hasTranscription) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Selected",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Start Session Button
                Button(
                    onClick = onStartSession,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = canStart && uiState !is LibraryUiState.Validating
                ) {
                    if (uiState is LibraryUiState.Validating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Start Session")
                    }
                }

                Text(
                    text = "Audio: MP3, AAC, WAV • Transcription: TXT",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
