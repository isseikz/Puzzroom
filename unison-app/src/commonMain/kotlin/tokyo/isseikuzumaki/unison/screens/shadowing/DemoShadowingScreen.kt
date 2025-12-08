package tokyo.isseikuzumaki.unison.screens.shadowing

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import tokyo.isseikuzumaki.shared.ui.atoms.HorizontalSpacer
import tokyo.isseikuzumaki.shared.ui.atoms.VerticalSpacer
import tokyo.isseikuzumaki.shared.ui.atoms.AppText
import tokyo.isseikuzumaki.shared.ui.theme.WarmError
import tokyo.isseikuzumaki.shared.ui.theme.WarmPrimary
import tokyo.isseikuzumaki.unison.screens.session.DemoSessionViewModel

/**
 * Demo version of ShadowingScreen that uses dummy data
 * For testing and demonstration purposes
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DemoShadowingScreen(
    modifier: Modifier = Modifier
) {
    val viewModel: DemoSessionViewModel = viewModel()
    val shadowingData by viewModel.shadowingData.collectAsState()

    var isPlaying by remember { mutableStateOf(false) }
    var isRecording by remember { mutableStateOf(false) }
    var seekPosition by remember { mutableStateOf(0f) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Shadowing Practice (Demo)") }
            )
        },
        floatingActionButton = {
            if (shadowingData != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Seek bar
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Slider(
                            value = seekPosition,
                            onValueChange = { seekPosition = it },
                            valueRange = 0f..shadowingData!!.durationMs.toFloat(),
                            colors = SliderDefaults.colors(
                                thumbColor = WarmPrimary,
                                activeTrackColor = WarmPrimary,
                                inactiveTrackColor = WarmPrimary.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Time display
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            AppText(
                                text = formatDuration(seekPosition.toLong()),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            AppText(
                                text = formatDuration(shadowingData!!.durationMs),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Control buttons
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Play/Stop audio button
                        FloatingActionButton(
                            onClick = {
                                if (isPlaying) {
                                    viewModel.stopPreview()
                                    isPlaying = false
                                } else {
                                    viewModel.playPreview()
                                    isPlaying = true
                                }
                            },
                            containerColor = WarmPrimary
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Stop" else "Play",
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        // Record button
                        FloatingActionButton(
                            onClick = {
                                if (isRecording) {
                                    viewModel.stopRecording()
                                    isRecording = false
                                } else {
                                    viewModel.startRecording()
                                    isRecording = true
                                }
                            },
                            containerColor = if (isRecording) WarmError else MaterialTheme.colorScheme.secondary
                        ) {
                            if (isRecording) {
                                val infiniteTransition = rememberInfiniteTransition()
                                val alpha by infiniteTransition.animateFloat(
                                    initialValue = 1f,
                                    targetValue = 0.3f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(500, easing = LinearEasing),
                                        repeatMode = RepeatMode.Reverse
                                    )
                                )
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = "Recording",
                                    modifier = Modifier
                                        .size(24.dp)
                                        .alpha(alpha)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = "Record",
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        modifier = modifier
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                shadowingData == null -> {
                    // Loading state
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                        VerticalSpacer(height = 16.dp)
                        AppText(
                            text = "Loading demo data...",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
                else -> {
                    // Content state
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        // Demo banner
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = WarmPrimary.copy(alpha = 0.1f)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp)
                            ) {
                                AppText(
                                    text = "🎭 DEMO MODE",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = WarmPrimary
                                )
                                VerticalSpacer(height = 4.dp)
                                AppText(
                                    text = "This is a demonstration with dummy data. Audio playback and recording are simulated.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        VerticalSpacer(height = 16.dp)

                        // File name
                        AppText(
                            text = shadowingData!!.fileName,
                            style = MaterialTheme.typography.headlineSmall,
                            color = WarmPrimary
                        )

                        VerticalSpacer(height = 8.dp)

                        // Duration
                        AppText(
                            text = "Duration: ${formatDuration(shadowingData!!.durationMs)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        VerticalSpacer(height = 24.dp)

                        // Transcript card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                AppText(
                                    text = "Transcript",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = WarmPrimary
                                )

                                VerticalSpacer(height = 12.dp)

                                AppText(
                                    text = shadowingData!!.transcript,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        VerticalSpacer(height = 16.dp)

                        // Instructions card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                AppText(
                                    text = "How to Practice",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )

                                VerticalSpacer(height = 8.dp)

                                AppText(
                                    text = "1. Press play to listen to the audio\n" +
                                            "2. Read along with the transcript\n" +
                                            "3. Press record to practice speaking\n" +
                                            "4. Try to match the timing and pronunciation",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }

                        // Recording indicator
                        if (isRecording) {
                            VerticalSpacer(height = 16.dp)

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = WarmError.copy(alpha = 0.1f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .background(WarmError, CircleShape)
                                    )
                                    HorizontalSpacer(width = 8.dp)
                                    AppText(
                                        text = "Recording in progress... (simulated)",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = WarmError
                                    )
                                }
                            }
                        }

                        // Bottom spacing for FABs
                        Spacer(modifier = Modifier.height(100.dp))
                    }
                }
            }
        }
    }
}

/**
 * Format duration in milliseconds to MM:SS format
 */
private fun formatDuration(durationMs: Long): String {
    val totalSeconds = (durationMs / 1000).toInt()
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
