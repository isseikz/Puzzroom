package tokyo.isseikuzumaki.unison.screens.library

import androidx.compose.runtime.Composable
import tokyo.isseikuzumaki.unison.LocalFilePicker
import tokyo.isseikuzumaki.unison.LocalPermissionHandler
import org.koin.compose.viewmodel.koinViewModel

/**
 * Android-specific wrapper for LibraryScreen that integrates file picker and permissions
 */
@Composable
actual fun LibraryScreenPlatform(
    onNavigateToSession: (String, String) -> Unit,
    viewModel: LibraryViewModel
) {
    val filePicker = LocalFilePicker.current
    val permissionHandler = LocalPermissionHandler.current

    LibraryScreen(
        onAudioFileSelected = {
            // Check and request permission if needed, then open audio file picker
            if (permissionHandler.hasRecordAudioPermission()) {
                filePicker.pickAudioFile { uri ->
                    uri?.let { viewModel.onAudioFileSelected(it.toString()) }
                }
            } else {
                permissionHandler.requestRecordAudioPermission { granted ->
                    if (granted) {
                        filePicker.pickAudioFile { uri ->
                            uri?.let { viewModel.onAudioFileSelected(it.toString()) }
                        }
                    }
                }
            }
        },
        onTranscriptionFileSelected = {
            // Open text file picker for transcription
            filePicker.pickTextFile { uri ->
                uri?.let { viewModel.onTranscriptionFileSelected(it.toString()) }
            }
        },
        onStartSession = {
            viewModel.onStartSession()
        },
        onNavigateToSession = onNavigateToSession,
        viewModel = viewModel
    )
}
