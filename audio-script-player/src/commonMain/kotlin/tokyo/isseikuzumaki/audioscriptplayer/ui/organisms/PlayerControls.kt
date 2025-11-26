package tokyo.isseikuzumaki.audioscriptplayer.ui.organisms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Player control component with play/pause button and seek bar.
 */
@Composable
fun PlayerControls(
    isPlaying: Boolean,
    currentPositionMs: Long,
    durationMs: Long,
    enabled: Boolean,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Seek bar
            Slider(
                value = if (durationMs > 0) {
                    currentPositionMs.toFloat() / durationMs.toFloat()
                } else {
                    0f
                },
                onValueChange = { fraction ->
                    if (durationMs > 0) {
                        onSeek((fraction * durationMs).toLong())
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = enabled && durationMs > 0
            )
            
            // Time display
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatTime(currentPositionMs),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
                Text(
                    text = formatTime(durationMs),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Play/Pause button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Restart button
                IconButton(
                    onClick = { onSeek(0L) },
                    enabled = enabled && durationMs > 0
                ) {
                    Icon(
                        imageVector = Icons.Default.Replay,
                        contentDescription = "Restart",
                        modifier = Modifier.size(32.dp),
                        tint = if (enabled && durationMs > 0) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outline
                        }
                    )
                }
                
                // Play/Pause button
                IconButton(
                    onClick = onPlayPause,
                    enabled = enabled && durationMs > 0
                ) {
                    Icon(
                        imageVector = if (isPlaying) {
                            Icons.Default.Pause
                        } else {
                            Icons.Default.PlayArrow
                        },
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        modifier = Modifier.size(48.dp),
                        tint = if (enabled && durationMs > 0) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outline
                        }
                    )
                }
            }
        }
    }
}

/**
 * Format milliseconds as MM:SS.
 */
private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}
