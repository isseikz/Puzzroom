package tokyo.isseikuzumaki.unison.screens.fileselection

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import tokyo.isseikuzumaki.unison.ui.components.AudioFilePickerButton
import tokyo.isseikuzumaki.unison.ui.components.TranscriptionFilePickerButton

/**
 * Screen for selecting audio and transcription files for shadowing practice
 *
 * @param onNavigateBack Callback when back button is pressed
 * @param onFilesSelected Callback when both files are selected and confirmed
 * @param modifier Modifier for the screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileSelectionScreen(
    onNavigateBack: () -> Unit,
    onFilesSelected: (audioUri: String, transcriptionUri: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var audioFileUri by remember { mutableStateOf<String?>(null) }
    var transcriptionFileUri by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Select Files for Shadowing") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            // Show confirm button only when both files are selected
            if (audioFileUri != null && transcriptionFileUri != null) {
                ExtendedFloatingActionButton(
                    onClick = {
                        onFilesSelected(audioFileUri!!, transcriptionFileUri!!)
                    },
                    icon = {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Confirm"
                        )
                    },
                    text = { Text("Start Shadowing") }
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Instructions
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "How to use:",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "1. Select an audio file to practice with",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "2. Select the corresponding transcription file",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "3. Press 'Start Shadowing' to begin",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Audio File Selection
            FileSelectionCard(
                title = "Audio File",
                fileUri = audioFileUri,
                onClearFile = { audioFileUri = null }
            ) {
                AudioFilePickerButton(
                    onFilePicked = { uri ->
                        audioFileUri = uri
                    }
                )
            }

            // Transcription File Selection
            FileSelectionCard(
                title = "Transcription File",
                fileUri = transcriptionFileUri,
                onClearFile = { transcriptionFileUri = null }
            ) {
                TranscriptionFilePickerButton(
                    onFilePicked = { uri ->
                        transcriptionFileUri = uri
                    }
                )
            }

            // Status Message
            if (audioFileUri != null && transcriptionFileUri != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Ready",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "All files selected! Tap 'Start Shadowing' to begin.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }
}

/**
 * Card component for displaying file selection status
 */
@Composable
private fun FileSelectionCard(
    title: String,
    fileUri: String?,
    onClearFile: () -> Unit,
    pickerButton: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )

            if (fileUri != null) {
                // Show selected file info
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Selected:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = fileUri.substringAfterLast("/"),
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        TextButton(onClick = onClearFile) {
                            Text("Change")
                        }
                    }
                }
            } else {
                // Show picker button
                pickerButton()
            }
        }
    }
}
