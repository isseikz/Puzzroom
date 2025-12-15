package tokyo.isseikuzumaki.unison.screens.review

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import tokyo.isseikuzumaki.shared.ui.atoms.HorizontalSpacer
import tokyo.isseikuzumaki.shared.ui.atoms.VerticalSpacer
import tokyo.isseikuzumaki.shared.ui.atoms.AppText
import tokyo.isseikuzumaki.shared.ui.theme.WarmPrimary
import tokyo.isseikuzumaki.shared.ui.theme.WarmSecondary
import tokyo.isseikuzumaki.shared.ui.theme.WarmError

/**
 * Format duration in milliseconds to MM:SS format
 */
private fun formatDuration(durationMs: Long): String {
    val totalSeconds = (durationMs / 1000).toInt()
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}

/**
 * Screen for reviewing recorded voice after shadowing practice
 *
 * @param recordingDurationMs Duration of the recording in milliseconds
 * @param originalDurationMs Duration of the original audio in milliseconds
 * @param originalTranscript The original transcript for reference
 * @param onNavigateBack Callback when back button is pressed
 * @param onAcceptRecording Callback when user accepts the recording
 * @param onRetryRecording Callback when user wants to retry recording
 * @param modifier Modifier for the screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordingReviewScreen(
    recordingDurationMs: Long,
    originalDurationMs: Long,
    originalTranscript: String,
    onNavigateBack: () -> Unit,
    onAcceptRecording: () -> Unit,
    onRetryRecording: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isPlaying by remember { mutableStateOf(false) }
    var seekPosition by remember { mutableStateOf(0f) }
    var timingOffset by remember { mutableStateOf(0f) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Review Your Recording") },
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
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Slider(
                            value = seekPosition,
                            onValueChange = { seekPosition = it },
                            valueRange = 0f..recordingDurationMs.toFloat(),
                            colors = SliderDefaults.colors(
                                thumbColor = WarmPrimary,
                                activeTrackColor = WarmPrimary,
                                inactiveTrackColor = WarmPrimary.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

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
                                text = formatDuration(recordingDurationMs),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    HorizontalSpacer(width = 16.dp)

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FloatingActionButton(
                            onClick = {
                                isPlaying = !isPlaying
                            },
                            containerColor = WarmPrimary
                        ) {
                            if (isPlaying) {
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
                                    imageVector = Icons.Default.Stop,
                                    contentDescription = "Stop",
                                    modifier = Modifier
                                        .size(24.dp)
                                        .alpha(alpha)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Play",
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        FloatingActionButton(
                            onClick = onRetryRecording,
                            containerColor = WarmError
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Retry",
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        FloatingActionButton(
                            onClick = onAcceptRecording,
                            containerColor = WarmSecondary
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Accept",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                VerticalSpacer(height = 8.dp)

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.95f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        AppText(
                            text = "Timing Adjustment",
                            style = MaterialTheme.typography.labelMedium,
                            color = WarmPrimary
                        )

                        VerticalSpacer(height = 8.dp)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            AppText(
                                text = "Earlier",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )

                            Slider(
                                value = timingOffset,
                                onValueChange = { timingOffset = it },
                                valueRange = -2000f..2000f,
                                colors = SliderDefaults.colors(
                                    thumbColor = WarmPrimary,
                                    activeTrackColor = WarmPrimary,
                                    inactiveTrackColor = WarmPrimary.copy(alpha = 0.3f)
                                ),
                                modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                            )

                            AppText(
                                text = "Later",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            val offsetText = if (timingOffset >= 0) {
                                "+" + String.format("%.1f", timingOffset / 1000f) + "s"
                            } else {
                                String.format("%.1f", timingOffset / 1000f) + "s"
                            }
                            AppText(
                                text = "Offset: $offsetText",
                                style = MaterialTheme.typography.labelSmall,
                                color = WarmPrimary
                            )
                        }
                    }
                }
            }
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = WarmPrimary.copy(alpha = 0.1f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    AppText(
                        text = "Recording Complete!",
                        style = MaterialTheme.typography.titleMedium,
                        color = WarmPrimary
                    )

                    VerticalSpacer(height = 8.dp)

                    val durationText = "Duration: " + formatDuration(recordingDurationMs)
                    AppText(
                        text = durationText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            VerticalSpacer(height = 24.dp)

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
                        text = "Original Transcript",
                        style = MaterialTheme.typography.titleMedium,
                        color = WarmPrimary
                    )

                    VerticalSpacer(height = 12.dp)

                    AppText(
                        text = originalTranscript,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            VerticalSpacer(height = 16.dp)

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
                        text = "Next Steps",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )

                    VerticalSpacer(height = 8.dp)

                    AppText(
                        text = "• Press play to hear both original and your recording simultaneously\n• Adjust timing offset to synchronize the playback\n• Compare with the original transcript\n• Press ✓ to accept or ✗ to retry",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}
