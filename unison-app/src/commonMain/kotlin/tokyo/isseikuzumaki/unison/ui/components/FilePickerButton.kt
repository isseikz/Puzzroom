package tokyo.isseikuzumaki.unison.ui.components

import androidx.compose.runtime.Composable

/**
 * File picker button for selecting audio files
 * @param onFilePicked Callback with the URI of the selected audio file
 */
@Composable
expect fun AudioFilePickerButton(
    onFilePicked: (String) -> Unit
)

/**
 * File picker button for selecting transcription files (text/json)
 * @param onFilePicked Callback with the URI of the selected transcription file
 */
@Composable
expect fun TranscriptionFilePickerButton(
    onFilePicked: (String) -> Unit
)
