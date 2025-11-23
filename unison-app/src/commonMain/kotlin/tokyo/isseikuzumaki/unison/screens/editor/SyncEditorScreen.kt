package tokyo.isseikuzumaki.unison.screens.editor

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel
import tokyo.isseikuzumaki.unison.screens.session.SessionUiState
import tokyo.isseikuzumaki.unison.screens.session.SessionViewModel

/**
 * Sync Editor Screen - Adjust timing and mix balance
 * This is where users fine-tune synchronization for perfect unison playback
 */
@Composable
fun SyncEditorScreen(
    onNavigateBack: () -> Unit,
    onNavigateToRecorder: () -> Unit,
    onFinish: () -> Unit
) {
    val viewModel: SessionViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsState()
    val offsetMs by viewModel.offsetMs.collectAsState()
    val balance by viewModel.balance.collectAsState()

    var isPlaying by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    when (val state = uiState) {
                        is SessionUiState.Editing -> Text(state.fileName)
                        else -> Text("Sync Editor")
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onFinish) {
                        Icon(Icons.Default.Check, contentDescription = "Finish")
                    }
                }
            )
        }
    ) { padding ->
        when (val state = uiState) {
            is SessionUiState.Editing -> {
                EditorContent(
                    modifier = Modifier.padding(padding),
                    durationMs = state.durationMs,
                    offsetMs = offsetMs,
                    balance = balance,
                    isPlaying = isPlaying,
                    onOffsetChange = { viewModel.setOffset(it) },
                    onBalanceChange = { viewModel.setBalance(it) },
                    onPlayClick = {
                        if (isPlaying) {
                            viewModel.stopPreview()
                            isPlaying = false
                        } else {
                            viewModel.playPreview()
                            isPlaying = true
                        }
                    },
                    onRetryClick = {
                        viewModel.retryRecording()
                        onNavigateToRecorder()
                    }
                )
            }

            else -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@Composable
private fun EditorContent(
    modifier: Modifier = Modifier,
    durationMs: Long,
    offsetMs: Int,
    balance: Float,
    isPlaying: Boolean,
    onOffsetChange: (Int) -> Unit,
    onBalanceChange: (Float) -> Unit,
    onPlayClick: () -> Unit,
    onRetryClick: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(32.dp)
    ) {
        // Info card
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Fine-tune synchronization",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Adjust the offset to eliminate lag and balance the mix to hear both voices clearly.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Offset slider
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Sync Offset",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = "${if (offsetMs >= 0) "+" else ""}${offsetMs} ms",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Slider(
                    value = offsetMs.toFloat(),
                    onValueChange = { onOffsetChange(it.toInt()) },
                    valueRange = -2000f..500f,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Earlier",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Later",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = if (offsetMs < 0) {
                        "Your voice plays ${-offsetMs}ms earlier"
                    } else if (offsetMs > 0) {
                        "Your voice plays ${offsetMs}ms later"
                    } else {
                        "No offset - perfectly synced"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Balance slider
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Mix Balance",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = "${(balance * 100).toInt()}% Your Voice",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Slider(
                    value = balance,
                    onValueChange = onBalanceChange,
                    valueRange = 0f..1f,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Original Only",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Your Voice Only",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onRetryClick,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Retry")
            }

            Button(
                onClick = onPlayClick,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isPlaying) "Stop" else "Preview")
            }
        }
    }
}
