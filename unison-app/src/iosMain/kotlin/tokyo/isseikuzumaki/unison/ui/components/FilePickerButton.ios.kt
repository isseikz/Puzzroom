package tokyo.isseikuzumaki.unison.ui.components

import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Description
import androidx.compose.runtime.Composable

@Composable
actual fun AudioFilePickerButton(
    onFilePicked: (String) -> Unit
) {
    // TODO: Implement iOS file picker for audio files
    // For now, this is a placeholder that will need native iOS implementation
    Button(
        onClick = {
            println("iOS audio file picker not yet implemented")
        }
    ) {
        Icon(Icons.Default.AudioFile, contentDescription = "Pick Audio")
        Text(" Pick Audio File (iOS TODO)")
    }
}

@Composable
actual fun TranscriptionFilePickerButton(
    onFilePicked: (String) -> Unit
) {
    // TODO: Implement iOS file picker for transcription files
    // For now, this is a placeholder that will need native iOS implementation
    Button(
        onClick = {
            println("iOS transcription file picker not yet implemented")
        }
    ) {
        Icon(Icons.Default.Description, contentDescription = "Pick Transcription")
        Text(" Pick Transcription File (iOS TODO)")
    }
}
