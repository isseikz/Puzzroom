package tokyo.isseikuzumaki.unison.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            onFilePicked(it.toString())
        }
    }

    Button(
        onClick = {
            launcher.launch("audio/*")
        }
    ) {
        Icon(Icons.Default.AudioFile, contentDescription = "Pick Audio")
        Text(" Pick Audio File")
    }
}

@Composable
actual fun TranscriptionFilePickerButton(
    onFilePicked: (String) -> Unit
) {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            onFilePicked(it.toString())
        }
    }

    Button(
        onClick = {
            launcher.launch("text/*")
        }
    ) {
        Icon(Icons.Default.Description, contentDescription = "Pick Transcription")
        Text(" Pick Transcription File")
    }
}
