package tokyo.isseikuzumaki.unison.screens.recorder

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import tokyo.isseikuzumaki.unison.LocalPermissionHandler

/**
 * Android-specific wrapper for RecorderScreen that handles permissions
 */
@Composable
actual fun RecorderScreenPlatform(
    uri: String,
    onNavigateBack: () -> Unit,
    onNavigateToEditor: () -> Unit
) {
    val permissionHandler = LocalPermissionHandler.current
    var permissionGranted by remember { mutableStateOf(permissionHandler.hasRecordAudioPermission()) }

    LaunchedEffect(Unit) {
        if (!permissionGranted) {
            permissionHandler.requestRecordAudioPermission { granted ->
                permissionGranted = granted
            }
        }
    }

    if (permissionGranted) {
        RecorderScreen(
            uri = uri,
            onNavigateBack = onNavigateBack,
            onNavigateToEditor = onNavigateToEditor
        )
    } else {
        // Show permission required message
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
                    Button(
                        onClick = {
                            permissionHandler.requestRecordAudioPermission { granted ->
                                permissionGranted = granted
                            }
                        }
                    ) {
                        Text("Grant Permission")
                    }
                }
            }
        }
    }
}
