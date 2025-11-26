package tokyo.isseikuzumaki.audioscriptplayer.ui.organisms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import tokyo.isseikuzumaki.audioscriptplayer.data.AlignmentState
import tokyo.isseikuzumaki.audioscriptplayer.data.ModelState

/**
 * Header component displaying model status and process button.
 */
@Composable
fun PlayerHeader(
    modelState: ModelState,
    alignmentState: AlignmentState,
    onLoadModel: () -> Unit,
    onProcess: () -> Unit,
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
            // Model status row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ModelStatusIndicator(modelState)
                
                Button(
                    onClick = onLoadModel,
                    enabled = modelState == ModelState.NotLoaded || modelState == ModelState.Error
                ) {
                    Text(
                        text = when (modelState) {
                            ModelState.NotLoaded -> "Load Model"
                            ModelState.Loading -> "Loading..."
                            ModelState.Ready -> "Model Ready"
                            ModelState.Error -> "Retry"
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Process button row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AlignmentStatusIndicator(alignmentState)
                
                Button(
                    onClick = onProcess,
                    enabled = modelState == ModelState.Ready && 
                              alignmentState != AlignmentState.Processing
                ) {
                    if (alignmentState == AlignmentState.Processing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = when (alignmentState) {
                            AlignmentState.Idle -> "Process"
                            AlignmentState.Processing -> "Processing..."
                            AlignmentState.Completed -> "Re-Process"
                            AlignmentState.Error -> "Retry"
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ModelStatusIndicator(state: ModelState) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        val (icon, color, text) = when (state) {
            ModelState.NotLoaded -> Triple(
                Icons.Default.HourglassEmpty,
                MaterialTheme.colorScheme.outline,
                "Model not loaded"
            )
            ModelState.Loading -> Triple(
                Icons.Default.Refresh,
                MaterialTheme.colorScheme.primary,
                "Loading model..."
            )
            ModelState.Ready -> Triple(
                Icons.Default.CheckCircle,
                MaterialTheme.colorScheme.primary,
                "Model ready"
            )
            ModelState.Error -> Triple(
                Icons.Default.Error,
                MaterialTheme.colorScheme.error,
                "Model error"
            )
        }
        
        if (state == ModelState.Loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp
            )
        } else {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = color
        )
    }
}

@Composable
private fun AlignmentStatusIndicator(state: AlignmentState) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        val (icon, color, text) = when (state) {
            AlignmentState.Idle -> Triple(
                Icons.Default.HourglassEmpty,
                MaterialTheme.colorScheme.outline,
                "Not processed"
            )
            AlignmentState.Processing -> Triple(
                Icons.Default.Refresh,
                MaterialTheme.colorScheme.primary,
                "Processing..."
            )
            AlignmentState.Completed -> Triple(
                Icons.Default.CheckCircle,
                MaterialTheme.colorScheme.primary,
                "Alignment complete"
            )
            AlignmentState.Error -> Triple(
                Icons.Default.Error,
                MaterialTheme.colorScheme.error,
                "Alignment error"
            )
        }
        
        if (state == AlignmentState.Processing) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp
            )
        } else {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = color
        )
    }
}
